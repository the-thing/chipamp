package com.github.thething.chipamp.common;

/**
 * General-purpose string utility methods.
 */
public final class Strings {

    /**
     * Private constructor to prevent instantiation.
     */
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

    /**
     * Checks if the character at the specified index in the string is a digit (0-9).
     *
     * @param str   the string to check
     * @param index the index of the character to check
     * @return {@code true} if the character is a digit, {@code false} otherwise
     */
    public static boolean isDigit(String str, int index) {
        char c = str.charAt(index);
        return c >= '0' && c <= '9';
    }

    /**
     * Compares two strings for equality, handling null values safely. Returns {@code true} if both strings are null or
     * have identical content.
     *
     * @param str1 the first string to compare
     * @param str2 the second string to compare
     * @return {@code true} if the strings are equal, {@code false} otherwise
     */
    public static boolean equals(String str1, String str2) {
        if (str1 == str2) {
            return true;
        }

        if (str1 == null || str2 == null) {
            return false;
        }

        if (str1.length() != str2.length()) {
            return false;
        }

        for (int i = 0; i < str1.length(); i++) {
            if (str1.charAt(i) != str2.charAt(i)) {
                return false;
            }
        }

        return true;
    }
}
