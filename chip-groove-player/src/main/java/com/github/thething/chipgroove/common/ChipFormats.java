package com.github.thething.chipgroove.common;

import com.github.thething.chipgroove.Pattern;

public final class ChipFormats {

    private ChipFormats() {
    }

    // TODO format mod file

    public static String formatPatterns(Pattern[][][] patterns) {
        StringBuilder out = new StringBuilder();
        formatPatterns(patterns, out);
        return out.toString();
    }

    public static void formatPatterns(Pattern[][][] patterns, StringBuilder out) {
        for (int pattern = 0; pattern < patterns.length; pattern++) {
            for (int row = 0; row < patterns[pattern].length; row++) {
                for (int channel = 0; channel < patterns[pattern][row].length; channel++) {
                    out.append(patterns[pattern][row][channel].pitch());
                    out.append(" ");
                }

                out.append('\n');
            }
        }
    }
}
