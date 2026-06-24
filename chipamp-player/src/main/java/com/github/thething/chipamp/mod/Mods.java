package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Maths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class Mods {

    public static final int PAL_CLOCK_HZ = 3_546_895;

    public static final int NTSC_CLOCK_HZ = 3_579_545;

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

    private static final String[] CUSTOM_NOTES = new String[]{
            "A-X", "B-X", "C-X", "D-X", "E-X", "F-X", "G-X"
    };

    private static final int[] PERIODS;

    public static final int[] SINE_TABLE = {
            0, 24, 49, 74, 97, 120, 141, 161, 180,
            197, 212, 224, 235, 244, 250, 253, 255,
            253, 250, 244, 235, 224, 212, 197, 180,
            161, 141, 120, 97, 74, 49, 24
    };

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

    public static String getNote(int period) {
        return NOTES[period];
    }

    public static int getFineTunePeriod(int period, int fineTune) {
        int periodIndex = getPeriodIndex(period);
        fineTune += 8;
        return FINE_TUNE_PERIODS[fineTune][periodIndex];
    }

    public static int getPeriodIndex(int period) {
        return getPeriodIndex(period, 0);
    }

    public static int getPeriodIndex(int period, int fineTune) {
        int[] periods = FINE_TUNE_PERIODS[fineTune + 8];

        for (int i = 0; i < periods.length; i++) {
            if (period == periods[i]) {
                return i;
            }
        }

        return -1;
    }

    public static int shiftPeriodBySemitones(int period, int fineTune, int semitones) {
        if (semitones == 0) {
            return period;
        }

        int[] periods = FINE_TUNE_PERIODS[fineTune + 8];
        int index = getPeriodIndex(period, fineTune);

        if (index == -1) {
            return period;
        }

        int newIndex = Maths.clamp(index + semitones, 0, periods.length - 1);
        return periods[newIndex];
    }

    public static String findClosestNote(int period) {
        int index = Arrays.binarySearch(PERIODS, period);

        if (index >= 0) {
            return NOTES[PERIODS[index]];
        }

        index = -index - 1;

        if (index == 0) {
            return NOTES[PERIODS[0]];
        }

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

    public static String getCustomNote(int period) {
        String note = findClosestNote(period);
        return CUSTOM_NOTES[note.charAt(0) - 'A'];
    }

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

    public static Set<EffectType> getUniqueEffects(Mod mod) {
        EnumSet<EffectType> effectTypes = EnumSet.noneOf(EffectType.class);

        for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
            for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                    Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
                    EffectType effectType = instrument.effectType();

                    if (effectType != EffectType.NONE && effectType != EffectType.EXTENDED_EFFECT) {
                        effectTypes.add(effectType);
                    }
                }
            }
        }

        return effectTypes;
    }

    public static Set<ExtendedEffectType> getUniqueExtendedEffects(Mod mod) {
        EnumSet<ExtendedEffectType> extendedEffectTypes = EnumSet.noneOf(ExtendedEffectType.class);

        for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
            for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                    Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
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
     * Convert a period value to a playback frequency (Hz).
     * <p>
     * frequency = clock / period
     * <p>
     * Period 428 → middle C (C-3) = 8287 Hz on PAL. The mixer then re-samples this to whatever output rate you have
     * chosen.
     */
    public static float periodToHz(int period, float clock) {
        if (period <= 0) {
            return 0.0f;
        }

        return clock / period;
    }
}
