package com.github.thething.chipamp.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MathsTest {

    @Test
    void shouldReturnClampedValue() {
        assertThat(Maths.clamp(10, 20, 30)).isEqualTo(20);
        assertThat(Maths.clamp(10, 30, 30)).isEqualTo(30);
        assertThat(Maths.clamp(-10, 0, 100)).isEqualTo(0);
        assertThat(Maths.clamp(101, 0, 100)).isEqualTo(100);

        assertThat(Maths.clamp(10.0f, 20.0f, 30.0f)).isEqualTo(20.0f);
        assertThat(Maths.clamp(10.0f, 30.0f, 30.0f)).isEqualTo(30.0f);
        assertThat(Maths.clamp(-10.0f, 0.0f, 100.0f)).isEqualTo(0.0f);
        assertThat(Maths.clamp(101.0f, 0.0f, 100.0f)).isEqualTo(100.0f);
    }

    @Test
    void shouldReturnRoundedValueToNextPowerOf2() {
        assertThat(Maths.roundUpPow2(0)).isEqualTo(1);
        assertThat(Maths.roundUpPow2(1)).isEqualTo(1);
        assertThat(Maths.roundUpPow2(2)).isEqualTo(2);
        assertThat(Maths.roundUpPow2(3)).isEqualTo(4);
        assertThat(Maths.roundUpPow2(15)).isEqualTo(16);
        assertThat(Maths.roundUpPow2(16)).isEqualTo(16);
        assertThat(Maths.roundUpPow2(1073741824)).isEqualTo(1073741824);
        assertThat(Maths.roundUpPow2(1073741825)).isEqualTo(-2147483648);
        assertThat(Maths.roundUpPow2(Integer.MAX_VALUE)).isEqualTo(-2147483648);
        assertThat(Maths.roundUpPow2(-1)).isEqualTo(1);
        assertThat(Maths.roundUpPow2(-20)).isEqualTo(1);
        assertThat(Maths.roundUpPow2(Integer.MIN_VALUE)).isEqualTo(-2147483648);
    }

    @Test
    void shouldReturnRoundedValueToPreviousPowerOf2() {
        assertThat(Maths.roundDownPow2(1)).isEqualTo(1);
        assertThat(Maths.roundDownPow2(2)).isEqualTo(2);
        assertThat(Maths.roundDownPow2(3)).isEqualTo(2);
        assertThat(Maths.roundDownPow2(15)).isEqualTo(8);
        assertThat(Maths.roundDownPow2(16)).isEqualTo(16);
        assertThat(Maths.roundDownPow2(Integer.MAX_VALUE)).isEqualTo(1073741824);
        assertThat(Maths.roundDownPow2(-1)).isEqualTo(-2147483648);
        assertThat(Maths.roundDownPow2(-20)).isEqualTo(-2147483648);
        assertThat(Maths.roundDownPow2(Integer.MIN_VALUE)).isEqualTo(-2147483648);
    }

    @Test
    void shouldReturnRoundedValue() {
        assertThat(Maths.round(3.14159f, 2)).isEqualTo(3.14f);
        assertThat(Maths.round(123.456f, 1)).isEqualTo(123.5f);
        assertThat(Maths.round(0.12345f, 3)).isEqualTo(0.123f);
    }
}