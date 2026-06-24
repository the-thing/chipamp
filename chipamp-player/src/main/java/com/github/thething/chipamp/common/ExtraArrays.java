package com.github.thething.chipamp.common;

/**
 * Utility class for array operations.
 */
public final class ExtraArrays {

    private ExtraArrays() {
    }

    /**
     * Returns the index of the first occurrence of the specified value in the array.
     *
     * @param values the array to search
     * @param value  the value to find
     * @return the index of the first occurrence of the value, or -1 if not found
     */
    public static int indexOf(byte[] values, byte value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the maximum value in the array.
     *
     * @param values the array to search
     * @return the maximum value in the array
     * @throws ArrayIndexOutOfBoundsException if the array is empty
     */
    public static int max(int[] values) {
        int max = values[0];

        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }

        return max;
    }
}
