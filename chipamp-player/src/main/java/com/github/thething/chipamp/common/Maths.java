package com.github.thething.chipamp.common;

public final class Maths {

    private Maths() {
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    public static int roundUpPow2(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    public static int roundDownPow2(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Value must be positive: " + value);
        }

        return Integer.highestOneBit(value);
    }
}
