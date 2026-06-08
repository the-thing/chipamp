package com.github.thething.chipgroove.common;

/**
 * General-purpose string utility methods.
 */
public final class Strings {

    private Strings() {
    }

    /**
     * Returns a string produced by left-padding {@code value} to at least {@code length} characters using
     * {@code paddingCharacter}.  If {@code value} is already at least {@code length} characters long it is returned
     * as-is.
     *
     * @param value            the string to pad
     * @param length           the minimum desired length
     * @param paddingCharacter the character to use for padding
     * @return the padded string
     */
    public static String padLeft(CharSequence value, int length, char paddingCharacter) {
        StringBuilder out = new StringBuilder(Math.max(length, value.length()));
        padLeft(out, value, length, paddingCharacter);
        return out.toString();
    }

    /**
     * Appends {@code value} left-padded to at least {@code length} characters to {@code out}.
     *
     * @param out              the builder to append to
     * @param value            the string to pad
     * @param length           the minimum desired length
     * @param paddingCharacter the character to use for padding
     */
    public static void padLeft(StringBuilder out, CharSequence value, int length, char paddingCharacter) {
        int diff = length - value.length();

        if (diff < 0) {
            out.append(value);
            return;
        }

        for (int i = 0; i < diff; i++) {
            out.append(paddingCharacter);
        }

        out.append(value);
    }
}
