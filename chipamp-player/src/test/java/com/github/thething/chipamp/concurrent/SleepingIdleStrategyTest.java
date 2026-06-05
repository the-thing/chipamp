package com.github.thething.chipamp.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SleepingIdleStrategyTest {

    private SleepingIdleStrategy underTest;

    @BeforeEach
    void setUp() {
        underTest = new SleepingIdleStrategy(10L);
    }

    @Test
    void shouldCreateWithDefaultArgument() {
        new SleepingIdleStrategy();
    }

    @Test
    void shouldIdle() {
        long timeNs = System.nanoTime();
        underTest.idle();
        long deltaNs = System.nanoTime() - timeNs;

        assertThat(deltaNs).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(10));

        timeNs = System.nanoTime();
        underTest.idle(0);
        deltaNs = System.nanoTime() - timeNs;

        assertThat(deltaNs).isGreaterThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos(10));

        timeNs = System.nanoTime();
        underTest.idle(1);
        deltaNs = System.nanoTime() - timeNs;

        assertThat(deltaNs).isLessThan(TimeUnit.MILLISECONDS.toNanos(10));
    }

    @Test
    void shouldSetInterruptFlagWhenInterrupted() {
        Thread.currentThread().interrupt();
        underTest.idle();

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        assertThat(Thread.interrupted()).isTrue();
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenSleepTimeIsNegative() {
        assertThatThrownBy(() -> new SleepingIdleStrategy(-10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Sleep time must be greater than or equal to zero: -10");
    }
}