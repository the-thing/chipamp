package com.github.thething.chipgroove.mod;

public final class ModTables {

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
     * ProTracker sine table used by both vibrato and tremolo. 32 entries covering a quarter... actually ProTracker uses
     * a full 32-step table representing one half cycle 0..255, mirrored for the other half. This is the exact table
     * from the original source.
     */
    // TODO convert to hex
    public static final int[] SINE_TABLE = {
            0, 24, 49, 74, 97, 120, 141, 161, 180,
            197, 212, 224, 235, 244, 250, 253, 255,
            253, 250, 244, 235, 224, 212, 197, 180,
            161, 141, 120, 97, 74, 49, 24
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
    }

    private ModTables() {
    }

    public static String getNote(int period) {
        return NOTES[period];
    }

    public static int getWaveformValue(WaveformType type, int position) {
        position = position & 63;
        int raw = SINE_TABLE[position & 31];

        switch (type) {
            case SINE -> {
                return (position < 32) ? raw : -raw;
            }
            case SAWTOOTH -> {
                return (position < 32) ? (255 - position * 8) : -(255 - (position - 32) * 8);
            }
            case SQUARE -> {
                return (position < 32) ? 255 : -255;
            }
        }

        return 0;
    }
}
