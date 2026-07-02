package com.github.thething.chipamp.common;

public final class Maths {

    private final int MAX_INT_POWER_OF_2 = 1_073_741_824;

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

    public static void main(String[] args) {
        System.out.println(roundUpPow2(-1));
        System.out.println(roundUpPow2(0));
        System.out.println(roundUpPow2(4194304));
        System.out.println(roundUpPow2(4194304 * 2));
        System.out.println(roundUpPow2(4194304 * 4));
        System.out.println(roundUpPow2(33554432));
        System.out.println(roundUpPow2(1073741824));
    }
}
