package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Maths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.github.thething.chipamp.common.Requirements.requireInRange;

/**
 * Utility class for MOD (Module) file format operations and constants.
 * <p>
 * This class provides functionality for working with ProTracker MOD files, including period-to-frequency conversion,
 * note handling, effect processing, and waveform generation. It contains period tables, fine-tuning calculations, and
 * various helper methods for analyzing and manipulating MOD data.
 */
public final class Mods {

    /**
     * PAL system clock frequency in Hertz (3,546,895 Hz). Used for period-to-frequency conversion in PAL systems.
     */
    public static final int PAL_CLOCK_HZ = 3_546_895;

    /**
     * NTSC system clock frequency in Hertz (3,579,545 Hz). Used for period-to-frequency conversion in NTSC systems.
     */
    public static final int NTSC_CLOCK_HZ = 3_579_545;

    /**
     * Minimum valid period value (B-4, the highest note).
     */
    public static final int MIN_PERIOD = 57;

    /**
     * Maximum valid period value (C-0, the lowest note).
     */
    public static final int MAX_PERIOD = 1712;

    /**
     * ModPlug Tracker left pan position (92/255).
     */
    public static final float MPT_LEFT_PAN = Maths.round(92.0f / 255.0f, 2);

    /**
     * ModPlug Tracker right pan position (192/255).
     */
    public static final float MPT_RIGHT_PAN = Maths.round(192.0f / 255.0f, 2);

    /**
     * ProTracker period table. Octaves 0 and 4 are non-standard.
     *
     * <pre>
     *  C     C#    D     D#    E     F     F#    G     G#    A     A#   B
     *  1712, 1616, 1525, 1440, 1357, 1281, 1209, 1141, 1077, 1017, 961, 907 | Octave 0
     *   856,  808, 762,  720,  678,  640,  604,  570,  538,  508,  480, 453 | Octave 1
     *   428,  404, 381,  360,  339,  320,  302,  285,  269,  254,  240, 226 | Octave 2
     *   214,  202, 190,  180,  170,  160,  151,  143,  135,  127,  120, 113 | Octave 3
     *   107,  101,  95,   90,   85,   80,   76,   71,   67,   64,   60,  57 | Octave 4
     *  </pre>
     */
    private static final String[] NOTES = new String[1713];

    /**
     * Sorted array of all valid period values used for binary search operations.
     */
    private static final int[] PERIODS;

    /**
     * Custom note names for non-standard periods, indexed by the first character of the note name.
     */
    private static final String[] CUSTOM_NOTES = new String[]{
            "A-X", "B-X", "C-X", "D-X", "E-X", "F-X", "G-X"
    };

    /**
     * Sine wave lookup table for waveform generation (32 values, 0-255 range).
     */
    private static final int[] SINE_TABLE = {
            0, 24, 49, 74, 97, 120, 141, 161, 180,
            197, 212, 224, 235, 244, 250, 253, 255,
            253, 250, 244, 235, 224, 212, 197, 180,
            161, 141, 120, 97, 74, 49, 24
    };

    /**
     * Fine-tune period tables for all 16 fine-tune values (-8 to +7). Each row contains 36 period values (3 octaves ×
     * 12 notes).
     */
    private static final int[][] FINE_TUNE_PERIODS = {
            // fine -8
            {907, 856, 808, 762, 720, 678, 640, 604, 570, 538, 508, 480,
                    453, 428, 404, 381, 360, 339, 320, 302, 285, 269, 254, 240,
                    226, 214, 202, 190, 180, 170, 160, 151, 143, 135, 127, 120},
            // fine -7
            {900, 850, 802, 757, 715, 674, 637, 601, 567, 535, 505, 477,
                    450, 425, 401, 379, 357, 337, 318, 300, 284, 268, 253, 239,
                    225, 213, 201, 189, 179, 169, 159, 150, 142, 134, 126, 119},
            // fine -6
            {894, 844, 796, 752, 709, 670, 632, 597, 563, 532, 502, 474,
                    447, 422, 398, 376, 355, 335, 316, 298, 282, 266, 251, 237,
                    223, 211, 199, 188, 177, 167, 158, 149, 141, 133, 125, 118},
            // fine -5
            {887, 838, 791, 746, 704, 665, 628, 592, 559, 528, 498, 470,
                    444, 419, 395, 373, 352, 332, 314, 296, 280, 264, 249, 235,
                    222, 209, 198, 187, 176, 166, 157, 148, 140, 132, 125, 118},
            // fine -4
            {881, 832, 785, 741, 699, 660, 623, 588, 555, 524, 494, 467,
                    441, 416, 392, 370, 350, 330, 312, 294, 278, 262, 247, 233,
                    220, 208, 196, 185, 175, 165, 156, 147, 139, 131, 124, 117},
            // fine -3
            {875, 826, 779, 736, 694, 655, 619, 584, 551, 520, 491, 463,
                    437, 413, 390, 368, 347, 328, 309, 292, 276, 260, 245, 232,
                    219, 206, 195, 184, 174, 164, 155, 146, 138, 130, 123, 116},
            // fine -2
            {868, 820, 774, 730, 689, 651, 614, 580, 547, 516, 487, 460,
                    434, 410, 387, 365, 345, 325, 307, 290, 274, 258, 244, 230,
                    217, 205, 193, 183, 172, 163, 154, 145, 137, 129, 122, 115},
            // fine -1
            {862, 814, 768, 725, 684, 646, 610, 575, 543, 513, 484, 457,
                    431, 407, 384, 363, 342, 323, 305, 288, 272, 256, 242, 228,
                    216, 203, 192, 181, 171, 161, 152, 144, 136, 128, 121, 114},
            // fine 0 (default)
            {856, 808, 762, 720, 678, 640, 604, 570, 538, 508, 480, 453,
                    428, 404, 381, 360, 339, 320, 302, 285, 269, 254, 240, 226,
                    214, 202, 190, 180, 170, 160, 151, 143, 135, 127, 120, 113},
            // fine +1
            {850, 802, 757, 715, 674, 637, 601, 567, 535, 505, 477, 450,
                    425, 401, 379, 357, 337, 318, 300, 284, 268, 253, 239, 225,
                    213, 201, 189, 179, 169, 159, 150, 142, 134, 126, 119, 113},
            // fine +2
            {844, 796, 752, 709, 670, 632, 597, 563, 532, 502, 474, 447,
                    422, 398, 376, 355, 335, 316, 298, 282, 266, 251, 237, 223,
                    211, 199, 188, 177, 167, 158, 149, 141, 133, 125, 118, 112},
            // fine +3
            {838, 791, 746, 704, 665, 628, 592, 559, 528, 498, 470, 444,
                    419, 395, 373, 352, 332, 314, 296, 280, 264, 249, 235, 222,
                    209, 198, 187, 176, 166, 157, 148, 140, 132, 125, 118, 111},
            // fine +4
            {832, 785, 741, 699, 660, 623, 588, 555, 524, 494, 467, 441,
                    416, 392, 370, 350, 330, 312, 294, 278, 262, 247, 233, 220,
                    208, 196, 185, 175, 165, 156, 147, 139, 131, 124, 117, 110},
            // fine +5
            {826, 779, 736, 694, 655, 619, 584, 551, 520, 491, 463, 437,
                    413, 390, 368, 347, 328, 309, 292, 276, 260, 245, 232, 219,
                    206, 195, 184, 174, 164, 155, 146, 138, 130, 123, 116, 109},
            // fine +6
            {820, 774, 730, 689, 651, 614, 580, 547, 516, 487, 460, 434,
                    410, 387, 365, 345, 325, 307, 290, 274, 258, 244, 230, 217,
                    205, 193, 183, 172, 163, 154, 145, 137, 129, 122, 115, 109},
            // fine +7
            {814, 768, 725, 684, 646, 610, 575, 543, 513, 484, 457, 431,
                    407, 384, 363, 342, 323, 305, 288, 272, 256, 242, 228, 216,
                    203, 192, 181, 171, 161, 152, 144, 136, 128, 121, 114, 108}};

    /**
     * Funk repeat (invert loop) speed table for the EFx effect.
     */
    private static final int[] FUNK_TABLE = {
            0, 5, 6, 7, 8, 10, 11, 13, 16, 19, 22, 26, 32, 43, 64, 128
    };

    static {
        // C
        NOTES[1712] = "C-0";
        NOTES[856] = "C-1";
        NOTES[428] = "C-2";
        NOTES[214] = "C-3";
        NOTES[107] = "C-4";
        // C#
        NOTES[1616] = "C#0";
        NOTES[808] = "C#1";
        NOTES[404] = "C#2";
        NOTES[202] = "C#3";
        NOTES[101] = "C#4";
        // D
        NOTES[1525] = "D-0";
        NOTES[762] = "D-1";
        NOTES[381] = "D-2";
        NOTES[190] = "D-3";
        NOTES[95] = "D-4";
        // D#
        NOTES[1440] = "D#0";
        NOTES[720] = "D#1";
        NOTES[360] = "D#2";
        NOTES[180] = "D#3";
        NOTES[90] = "D#4";
        // E
        NOTES[1357] = "E-0";
        NOTES[678] = "E-1";
        NOTES[339] = "E-2";
        NOTES[170] = "E-3";
        NOTES[85] = "E-4";
        // F
        NOTES[1281] = "F-0";
        NOTES[640] = "F-1";
        NOTES[320] = "F-2";
        NOTES[160] = "F-3";
        NOTES[80] = "F-4";
        // F#
        NOTES[1209] = "F#0";
        NOTES[604] = "F#1";
        NOTES[302] = "F#2";
        NOTES[151] = "F#3";
        NOTES[76] = "F#4";
        // G
        NOTES[1141] = "G-0";
        NOTES[570] = "G-1";
        NOTES[285] = "G-2";
        NOTES[143] = "G-3";
        NOTES[71] = "G-4";
        // G#
        NOTES[1077] = "G#0";
        NOTES[538] = "G#1";
        NOTES[269] = "G#2";
        NOTES[135] = "G#3";
        NOTES[67] = "G#4";
        // A
        NOTES[1017] = "A-0";
        NOTES[508] = "A-1";
        NOTES[254] = "A-2";
        NOTES[127] = "A-3";
        NOTES[64] = "A-4";
        // A#
        NOTES[961] = "A#0";
        NOTES[480] = "A#1";
        NOTES[240] = "A#2";
        NOTES[120] = "A#3";
        NOTES[60] = "A#4";
        // B
        NOTES[907] = "B-0";
        NOTES[453] = "B-1";
        NOTES[226] = "B-2";
        NOTES[113] = "B-3";
        NOTES[57] = "B-4";

        List<Integer> notes = new ArrayList<>();

        for (int i = 0; i < NOTES.length; i++) {
            if (NOTES[i] != null) {
                notes.add(i);
            }
        }

        PERIODS = notes.stream().mapToInt(Integer::intValue).sorted().toArray();
    }

    private Mods() {
    }

    /**
     * Gets the note name for a given period value.
     *
     * @param period the period value
     * @return the note name (e.g., "C-3", "A#2") or null if the period is invalid or non-standard
     */
    public static String getNote(int period) {
        if (period > NOTES.length) {
            return null;
        }

        return NOTES[period];
    }

    /**
     * Applies fine-tuning to a period value.
     * <p>
     * Fine-tuning allows adjustment of pitch by ±8 steps, where each step is 1/8 of a semitone.
     *
     * @param period             the base period value
     * @param fineTune           the fine-tune value (-8 to +7)
     * @param roundNearestPeriod whether to round non-standard periods to the nearest valid period
     * @return the fine-tuned period value
     * @throws IllegalArgumentException if fine-tune is not in range [-8, 7]
     */
    public static int getFineTunePeriod(int period, int fineTune, boolean roundNearestPeriod) {
        requireInRange(fineTune, -8, 7);

        int periodIndex = getPeriodIndex(period);

        if (periodIndex == -1) {
            // this happens when mods use non-standard periods outside of range e.g. 90s_house_project.mod
            if (roundNearestPeriod) {
                return findNearestPeriod(period, fineTune);
            } else {
                return getCustomFineTunePeriod(period, fineTune);
            }
        }

        fineTune += 8;

        return FINE_TUNE_PERIODS[fineTune][periodIndex];
    }

    /**
     * Calculates a custom fine-tuned period using a mathematical formula.
     * <p>
     * Used for non-standard periods that don't exist in the standard period table. The formula applies exponential
     * scaling: period * 2^(-fineTune/96)
     *
     * @param period   the base period value
     * @param fineTune the fine-tune value (-8 to +7)
     * @return the fine-tuned period value, rounded to the nearest integer
     * @throws IllegalArgumentException if fine-tune is not in range [-8, 7]
     */
    public static int getCustomFineTunePeriod(int period, int fineTune) {
        requireInRange(fineTune, -8, 7);

        if (fineTune == 0) {
            return period;
        }

        double factor = Math.pow(2.0, -fineTune / 96.0);

        return (int) Math.round(period * factor);
    }

    /**
     * Gets the index of a period in the period table with the default fine-tuning (0).
     *
     * @param period the period value to look up
     * @return the index (0-35) or -1 if the period is not found in the table
     */
    public static int getPeriodIndex(int period) {
        return getPeriodIndex(period, 0);
    }

    /**
     * Gets the index of a period in the fine-tuned period table.
     *
     * @param period   the period value to look up
     * @param fineTune the fine-tune value (-8 to +7)
     * @return the index (0-35) or -1 if the period is not found in the table
     * @throws IllegalArgumentException if fine-tune is not in range [-8, 7]
     */
    public static int getPeriodIndex(int period, int fineTune) {
        requireInRange(fineTune, -8, 7);

        int[] periods = FINE_TUNE_PERIODS[fineTune + 8];

        for (int i = 0; i < periods.length; i++) {
            if (period == periods[i]) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Shifts a period up or down by a specified number of semitones.
     * <p>
     * Positive semitones shift the period up (lower pitch), negative semitones shift down (higher pitch). The result is
     * clamped to the valid period range.
     *
     * @param period    the base period value
     * @param fineTune  the fine-tune value (-8 to +7)
     * @param semitones the number of semitones to shift (can be negative)
     * @return the shifted period value, or the original period if it's not found in the table
     * @throws IllegalArgumentException if fine-tune is not in range [-8, 7]
     */
    public static int shiftUpPeriodBySemitones(int period, int fineTune, int semitones) {
        requireInRange(fineTune, -8, 7);

        if (semitones == 0) {
            return period;
        }

        int[] periods = FINE_TUNE_PERIODS[fineTune + 8];
        int index = getPeriodIndex(period, fineTune);

        if (index == -1) {
            // TODO check if this ever happens
            return period;
        }

        int newIndex = Maths.clamp(index + semitones, 0, periods.length - 1);

        return periods[newIndex];
    }

    /**
     * Finds the nearest valid period to a target period value in the fine-tuned period table.
     *
     * @param targetPeriod the target period value
     * @param fineTune     the fine-tune value (-8 to +7)
     * @return the nearest valid period value
     * @throws IllegalArgumentException if fine-tune is not in range [-8, 7]
     */
    public static int findNearestPeriod(int targetPeriod, int fineTune) {
        requireInRange(fineTune, -8, 7);

        int[] periods = FINE_TUNE_PERIODS[fineTune + 8];
        int nearest = periods[0];
        int minDiff = Math.abs(targetPeriod - nearest);

        for (int i = 1; i < periods.length; i++) {
            int diff = Math.abs(targetPeriod - periods[i]);

            if (diff < minDiff) {
                minDiff = diff;
                nearest = periods[i];
            }
        }

        return nearest;
    }

    /**
     * Finds the nearest note name for a given period.
     * <p>
     * If the exact period is not found, returns the note name of the closest period.
     *
     * @param period the period value
     * @return the closest note name (e.g., "C-3", "A#2")
     */
    public static String findNearestNote(int period) {
        int index = Arrays.binarySearch(PERIODS, period);

        if (index >= 0) {
            return NOTES[PERIODS[index]];
        }

        index = -index - 1;

        if (index == PERIODS.length) {
            return NOTES[PERIODS[PERIODS.length - 1]];
        }

        int diff1 = Math.abs(period - PERIODS[index - 1]);
        int diff2 = Math.abs(period - PERIODS[index]);

        if (diff1 <= diff2) {
            return NOTES[PERIODS[index - 1]];
        } else {
            return NOTES[PERIODS[index]];
        }
    }

    /**
     * Gets a custom note name for non-standard periods.
     * <p>
     * Returns a note name like "A-X", "B-X", etc., based on the closest standard note.
     *
     * @param period the period value
     * @return the custom note name
     */
    public static String getCustomNote(int period) {
        String note = findNearestNote(period);
        return CUSTOM_NOTES[note.charAt(0) - 'A'];
    }

    /**
     * Gets a waveform value for a given waveform type and position.
     * <p>
     * Used for vibrato and tremolo effects. The position wraps around every 64 steps.
     *
     * @param type     the waveform type (SINE, SAWTOOTH, or SQUARE)
     * @param position the position in the waveform (0-63, wraps automatically)
     * @return the waveform value in the range [-255, 255]
     */
    public static int getWaveformValue(WaveformType type, int position) {
        position = position & 63;
        int raw = SINE_TABLE[position & 31];

        switch (type) {
            case SAWTOOTH -> {
                return (position < 32) ? (255 - position * 8) : -(255 - (position - 32) * 8);
            }
            case SQUARE -> {
                return (position < 32) ? 255 : -255;
            }

            default -> {
                return (position < 32) ? raw : -raw;
            }
        }
    }

    /**
     * Collects all unique effect types used in a MOD file.
     *
     * @param mod the MOD file to analyze
     * @return a set of all unique effect types found in the MOD
     */
    public static Set<EffectType> getUniqueEffects(Mod mod) {
        EnumSet<EffectType> effectTypes = EnumSet.noneOf(EffectType.class);

        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
                for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                    Instrument instrument = mod.getInstrument(channelIndex, patternIndex, rowIndex);
                    EffectType effectType = instrument.effectType();

                    if (effectType != EffectType.NONE && effectType != EffectType.EXTENDED_EFFECT) {
                        effectTypes.add(effectType);
                    }
                }
            }
        }

        return effectTypes;
    }

    /**
     * Collects all unique extended effect types used in a MOD file.
     *
     * @param mod the MOD file to analyze
     * @return a set of all unique extended effect types found in the MOD
     */
    public static Set<ExtendedEffectType> getUniqueExtendedEffects(Mod mod) {
        EnumSet<ExtendedEffectType> extendedEffectTypes = EnumSet.noneOf(ExtendedEffectType.class);

        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
                for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                    Instrument instrument = mod.getInstrument(channelIndex, patternIndex, rowIndex);
                    ExtendedEffectType extendedEffectType = instrument.extendedEffectType();

                    if (extendedEffectType != ExtendedEffectType.NONE) {
                        extendedEffectTypes.add(extendedEffectType);
                    }
                }
            }
        }

        return extendedEffectTypes;
    }

    /**
     * Checks if a specific effect type is used anywhere in a MOD file.
     *
     * @param mod        the MOD file to check
     * @param effectType the effect type to search for
     * @return true if the effect is found at least once, false otherwise
     */
    public static boolean isEffectPresent(Mod mod, EffectType effectType) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
                for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                    Instrument instrument = mod.getInstrument(channelIndex, patternIndex, rowIndex);

                    if (instrument.effectType() == effectType) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if a specific extended effect type is used anywhere in a MOD file.
     *
     * @param mod        the MOD file to check
     * @param effectType the extended effect type to search for
     * @return true if the extended effect is found at least once, false otherwise
     */
    public static boolean isExtendedEffectPresent(Mod mod, ExtendedEffectType effectType) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
                for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                    Instrument instrument = mod.getInstrument(channelIndex, patternIndex, rowIndex);

                    if (instrument.extendedEffectType() == effectType) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Converts a period value to a playback frequency in Hertz.
     * <p>
     * Uses the formula: frequency = clockHz / period
     * <p>
     * For example, period 428 (middle C, C-3) produces 8287 Hz on PAL systems. The mixer then resamples this to the
     * output sample rate.
     *
     * @param period the period value
     * @param clockHz  the clockHz frequency in Hz (typically PAL_CLOCK_HZ or NTSC_CLOCK_HZ)
     * @return the frequency in Hz, or 0 if the period is non-positive
     */
    public static float convertPeriodToHz(int period, float clockHz) {
        if (period <= 0) {
            return 0.0f;
        }

        return clockHz / period;
    }

    /**
     * Checks if a MOD file contains any non-standard period values (custom notes).
     *
     * @param mod the MOD file to check
     * @return true if the MOD contains at least one custom note, false otherwise
     */
    public static boolean isCustomNotePresent(Mod mod) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
                for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                    Instrument instrument = mod.getInstrument(channelIndex, patternIndex, rowIndex);
                    int period = instrument.period();

                    if (period == 0) {
                        continue;
                    }

                    if (getNote(period) == null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Finds the first sequence position that references a given pattern index.
     *
     * @param mod          the MOD file to search
     * @param patternIndex the pattern index to find
     * @return the sequence index (0-127) or -1 if the pattern is not found in the sequence
     */
    public static int findSequenceIndex(Mod mod, int patternIndex) {
        for (int sequenceIndex = 0; sequenceIndex < mod.getLength(); sequenceIndex++) {
            if (mod.getPatternIndex(sequenceIndex) == patternIndex) {
                return sequenceIndex;
            }
        }

        return -1;
    }

    /**
     * Gets the funk repeat (invert loop) speed value for a given index.
     * <p>
     * Used by the EFx (invert loop) effect to determine sample manipulation speed.
     *
     * @param index the funk table index (0-15)
     * @return the funk speed value
     */
    public static int getFunk(int index) {
        return FUNK_TABLE[index];
    }
}
