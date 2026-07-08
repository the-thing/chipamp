package com.github.thething.chipamp.common;

import static java.util.Objects.checkFromIndexSize;

/**
 * Utility class for array operations.
 */
public final class ExtraArrays {

    private ExtraArrays() {
    }

    public static int indexOf(byte[] values, int offset, int length, byte value) {
        checkFromIndexSize(offset, length, values.length);

        for (int i = 0; i < length; i++) {
            if (values[i + offset] == value) {
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

    public static int getBigEndianUnsignedShort(byte[] buffer, int offset) {
        return ((buffer[offset] << 8) | (buffer[offset + 1] & 0xFF)) & 0xFFFF;
    }
}
