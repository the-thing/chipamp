package com.github.thething.chipgroove.common;

public final class ChipArrays {

    private ChipArrays() {
    }

    public static int indexOf(byte[] values, byte value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }

        return -1;
    }

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
