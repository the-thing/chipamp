package com.github.thething.chipamp.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtraArraysTest {

    @Test
    void shouldReturnIndexOfTheValueInTheArrayWhenFound() {
        assertThat(ExtraArrays.indexOf(new byte[]{-128, 0, 127}, (byte) -128)).isEqualTo(0);
        assertThat(ExtraArrays.indexOf(new byte[]{-128, 0, 127}, (byte) 0)).isEqualTo(1);
        assertThat(ExtraArrays.indexOf(new byte[]{-128, 0, 127}, (byte) 127)).isEqualTo(2);
    }

    @Test
    void shouldReturnMinusOneWhenValueIsNotInTheArray() {
        assertThat(ExtraArrays.indexOf(new byte[]{-128, 0, 127}, (byte) 5)).isEqualTo(-1);
    }

    @Test
    void shouldReturnMaxValueFromArray() {
        assertThat(ExtraArrays.max(new int[]{-100, 10, 56678, 30, 345,})).isEqualTo(56678);
    }

    @Test
    void shouldReturnBigEndianUnsignedShortFromArray() {
        assertThat(ExtraArrays.getBigEndianUnsignedShort(new byte[]{0x0A, 0x0B, 0x0C}, 0)).isEqualTo(0x0A0B);
        assertThat(ExtraArrays.getBigEndianUnsignedShort(new byte[]{0x0A, 0x0B, 0x0C}, 1)).isEqualTo(0x0B0C);
    }
}