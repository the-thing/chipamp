package com.github.thething.chipgroove.mod;

public final class ModTables {

    /**
     * ProTracker period table.
     *
     * <pre>
     *  C    C#   D    D#   E    F    F#   G    G#   A    A#   B
     *  856, 808, 762, 720, 678, 640, 604, 570, 538, 508, 480, 453 // octave 2
     *  428, 404, 381, 360, 339, 320, 302, 285, 269, 254, 240, 226 // octave 3
     *  214, 202, 190, 180, 170, 160, 151, 143, 135, 127, 120, 113 // octave 4
     *  </pre>
     */
    private static final String[] NOTES = new String[1024];

    static {
        // C
        NOTES[856] = "C-1";
        NOTES[428] = "C-2";
        NOTES[214] = "C-3";
        // C#
        NOTES[808] = "C#1";
        NOTES[404] = "C#2";
        NOTES[202] = "C#3";
        // D
        NOTES[762] = "D-1";
        NOTES[381] = "D-2";
        NOTES[190] = "D-3";
        // D#
        NOTES[720] = "D#1";
        NOTES[360] = "D#2";
        NOTES[180] = "D#3";
        // E
        NOTES[678] = "E-1";
        NOTES[339] = "E-2";
        NOTES[170] = "E-3";
        // F
        NOTES[640] = "F-1";
        NOTES[320] = "F-2";
        NOTES[160] = "F-3";
        // F#
        NOTES[604] = "F#1";
        NOTES[302] = "F#2";
        NOTES[151] = "F#3";
        // G
        NOTES[570] = "G-1";
        NOTES[285] = "G-2";
        NOTES[143] = "G-3";
        // G#
        NOTES[538] = "G#1";
        NOTES[269] = "G#2";
        NOTES[135] = "G#3";
        // A
        NOTES[508] = "A-1";
        NOTES[254] = "A-2";
        NOTES[127] = "A-3";
        // A#
        NOTES[480] = "A#1";
        NOTES[240] = "A#2";
        NOTES[120] = "A#3";
        // B
        NOTES[453] = "B-1";
        NOTES[226] = "B-2";
        NOTES[113] = "B-3";
    }

    private ModTables() {
    }

    public static String getNote(int period) {
        return NOTES[period];
    }
}
