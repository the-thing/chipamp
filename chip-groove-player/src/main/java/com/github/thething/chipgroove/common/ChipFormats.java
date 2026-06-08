package com.github.thething.chipgroove.common;

import com.github.thething.chipgroove.Pattern;

public final class ChipFormats {

    private ChipFormats() {
    }

    public static String format(Pattern[][][] patterns) {
        StringBuilder out = new StringBuilder();
        format(patterns, out);
        return out.toString();
    }

    public static void format(Pattern[][][] patterns, StringBuilder out) {
        for (int pattern = 0; pattern < patterns.length; pattern++) {
            for (int row = 0; row < patterns[pattern].length; row++) {
                for (int channel = 0; channel < patterns[pattern][row].length; channel++) {
                    out.append(patterns[pattern][row][channel]);
                }
            }
        }
    }
}
