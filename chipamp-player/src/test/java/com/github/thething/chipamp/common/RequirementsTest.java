package com.github.thething.chipamp.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequirementsTest {

    @Test
    void shouldReturnValueWhenInRange() {
        assertThat(Requirements.requireInRange(10, 0, 20)).isEqualTo(10);
        assertThat(Requirements.requireInRange(0, 0, 20)).isEqualTo(0);
        assertThat(Requirements.requireInRange(20, 0, 20)).isEqualTo(20);
        assertThat(Requirements.requireInRange(30, 30, 30)).isEqualTo(30);

        assertThat(Requirements.requireInRange(10.0f, 0.0f, 20.0f)).isEqualTo(10.0f);
        assertThat(Requirements.requireInRange(0.0f, 0.0f, 20.0f)).isEqualTo(0.0f);
        assertThat(Requirements.requireInRange(20.0f, 0.0f, 20.0f)).isEqualTo(20.0f);
        assertThat(Requirements.requireInRange(30.0f, 30.0f, 30.0f)).isEqualTo(30.0f);
    }

    @Test
    void shouldThrowExceptionWhenOutOfRange() {
        assertThatThrownBy(() -> Requirements.requireInRange(-10, 0, 10))
                .hasMessage("Value must be in range [0,10]: -10")
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause();

        assertThatThrownBy(() -> Requirements.requireInRange(20, 0, 10))
                .hasMessage("Value must be in range [0,10]: 20")
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause();

        assertThatThrownBy(() -> Requirements.requireInRange(-10.0f, 0.0f, 10.0f))
                .hasMessage("Value must be in range [0.0,10.0]: -10.0")
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause();

        assertThatThrownBy(() -> Requirements.requireInRange(20.0f, 0.0f, 10.0f))
                .hasMessage("Value must be in range [0.0,10.0]: 20.0")
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause();
    }

    @Test
    void shouldThrowExceptionWhenRangeIsInvalid() {
        assertThatThrownBy(() -> Requirements.requireInRange(10, 20, 10))
                .hasMessage("Invalid range [20,10]")
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause();

        assertThatThrownBy(() -> Requirements.requireInRange(10.0f, 20.0f, 10.0f))
                .hasMessage("Invalid range [20.0,10.0]")
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause();
    }
}