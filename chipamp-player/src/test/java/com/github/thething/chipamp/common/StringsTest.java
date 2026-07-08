package com.github.thething.chipamp.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringsTest {

    @Test
    void shouldReturnPaddedString() {
        assertThat(Strings.padLeft("123", 5, '0')).isEqualTo("00123");
        assertThat(Strings.padLeft("123", 3, '0')).isEqualTo("123");
        assertThat(Strings.padLeft("123", 0, '0')).isEqualTo("123");
    }

    @Test
    void shouldReturnTrueWhenCharIsDigit() {
        assertThat(Strings.isDigit("a1c+", 1)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenCharIsNotDigit() {
        assertThat(Strings.isDigit("a1c+", 0)).isFalse();
        assertThat(Strings.isDigit("a1c+", 2)).isFalse();
        assertThat(Strings.isDigit("a1c+", 3)).isFalse();
    }

    @Test
    void shouldReturnTrueWhenStringsAreEqual() {
        assertThat(Strings.equals("abc", "abc")).isTrue();
        assertThat(Strings.equals("abc", new String("abc"))).isTrue();
        assertThat(Strings.equals("", "")).isTrue();
        assertThat(Strings.equals(null, null)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenStringsAreNotEqual() {
        assertThat(Strings.equals("a", "abc")).isFalse();
        assertThat(Strings.equals("abc", "a")).isFalse();
        assertThat(Strings.equals("abc", "abd")).isFalse();
        assertThat(Strings.equals("", null)).isFalse();
        assertThat(Strings.equals(null, "")).isFalse();
    }
}