package com.github.thething.chipgroove.common;

public final class Maths {

    private Maths() {
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
