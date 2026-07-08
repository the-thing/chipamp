package com.github.thething.chipamp.common;

import static java.util.Objects.checkFromIndexSize;

/**
 * Utility class for array operations.
 */
public final class ExtraArrays {

    /**
     * Private constructor to prevent instantiation.
     */
    private ExtraArrays() {
    }

    /**
     * Searches for the first occurrence of a value in a subrange of a byte array.
     * Returns the relative index from the start of the subrange, not the absolute index in the array.
     *
     * @param values the byte array to search
     * @param offset the starting position in the array
     * @param length the number of elements to search
     * @param value  the value to search for
     * @return the relative index of the first occurrence, or {@code -1} if not found
     * @throws IndexOutOfBoundsException if the subrange is out of bounds
     */
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

    /**
     * Reads an unsigned 16-bit integer from a byte array in big-endian byte order.
     *
     * @param buffer the byte array to read from
     * @param offset the starting position in the array
     * @return the unsigned short value as an int (0-65535)
     * @throws ArrayIndexOutOfBoundsException if there are not enough bytes at the offset
     */
    public static int getBigEndianUnsignedShort(byte[] buffer, int offset) {
        return ((buffer[offset] << 8) | (buffer[offset + 1] & 0xFF)) & 0xFFFF;
    }
}
