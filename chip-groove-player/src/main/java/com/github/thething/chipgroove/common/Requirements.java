package com.github.thething.chipgroove.common;

public final class Requirements {

    private Requirements() {
    }

    /**
     * Verifies that {@code value} lies within the closed interval [{@code min}, {@code max}].
     *
     * @param value the value to validate
     * @param min   the minimum allowed value (inclusive)
     * @param max   the maximum allowed value (inclusive)
     * @return {@code value} unchanged when it is within range
     * @throws IllegalArgumentException if {@code min > max}, or if {@code value} is out of range
     */
    public static int requireInRange(int value, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Invalid range [" + min + "," + max + "]");
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException("Value must be in range [" + min + "," + max + "]: " + value);
        }

        return value;
    }

    /**
     * Verifies that {@code value} lies within the closed interval [{@code min}, {@code max}].
     *
     * @param value the value to validate
     * @param min   the minimum allowed value (inclusive)
     * @param max   the maximum allowed value (inclusive)
     * @return {@code value} unchanged when it is within range
     * @throws IllegalArgumentException if {@code min > max}, or if {@code value} is out of range
     */
    public static float requireInRange(float value, float min, float max) {
        if (min > max) {
            throw new IllegalArgumentException("Invalid range [" + min + "," + max + "]");
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException("Value must be in range [" + min + "," + max + "]: " + value);
        }

        return value;
    }
}
