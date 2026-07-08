package com.github.thething.chipamp.common;

/**
 * Utility class providing mathematical operations and helper functions.
 */
public final class Maths {

    /**
     * Precomputed multipliers for float rounding (powers of 10 from 10^0 to 10^9). Used to avoid repeated calculations
     * in the round() method.
     */
    private static final float[] FLOAT_MULTIPLIERS;

    static {
        FLOAT_MULTIPLIERS = new float[10];

        float value = 1.0f;

        for (int i = 0; i < FLOAT_MULTIPLIERS.length; i++) {
            FLOAT_MULTIPLIERS[i] = value;
            value *= 10.0f;
        }
    }

    private Maths() {
    }

    /**
     * Clamps an integer value to the specified range.
     * <p>
     * If the value is less than min, returns min. If greater than max, returns max. Otherwise, returns the value
     * unchanged.
     *
     * @param value the value to clamp
     * @param min   the minimum boundary (inclusive)
     * @param max   the maximum boundary (inclusive)
     * @return the clamped value in the range [min, max]
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Clamps a float value to the specified range.
     * <p>
     * If the value is less than min, returns min. If greater than max, returns max. Otherwise, returns the value
     * unchanged.
     *
     * @param value the value to clamp
     * @param min   the minimum boundary (inclusive)
     * @param max   the maximum boundary (inclusive)
     * @return the clamped value in the range [min, max]
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Rounds an integer up to the next power of two.
     * <p>
     * For example:
     * <ul>
     *   <li>1 → 1</li>
     *   <li>2 → 2</li>
     *   <li>3 → 4</li>
     *   <li>5 → 8</li>
     *   <li>100 → 128</li>
     * </ul>
     *
     * @param value the value to round up (must be positive)
     * @return the smallest power of two greater than or equal to the value
     */
    public static int roundUpPow2(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    /**
     * Rounds an integer down to the previous power of two.
     * <p>
     * For example:
     * <ul>
     *   <li>1 → 1</li>
     *   <li>2 → 2</li>
     *   <li>3 → 2</li>
     *   <li>5 → 4</li>
     *   <li>100 → 64</li>
     * </ul>
     *
     * @param value the value to round down (must be positive)
     * @return the largest power of two less than or equal to the value
     * @throws IllegalArgumentException if value is not positive
     */
    public static int roundDownPow2(int value) {
        return Integer.highestOneBit(value);
    }

    /**
     * Rounds a float value to a specified number of decimal places.
     * <p>
     * For example:
     * <ul>
     *   <li>round(3.14159f, 2) → 3.14</li>
     *   <li>round(123.456f, 1) → 123.5</li>
     *   <li>round(0.12345f, 3) → 0.123</li>
     * </ul>
     *
     * @param value the value to round
     * @param scale the number of decimal places (0-9)
     * @return the rounded value
     */
    public static float round(float value, int scale) {
        return Math.round(value * FLOAT_MULTIPLIERS[scale]) / FLOAT_MULTIPLIERS[scale];
    }
}
