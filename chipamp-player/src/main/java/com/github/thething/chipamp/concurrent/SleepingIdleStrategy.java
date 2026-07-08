package com.github.thething.chipamp.concurrent;

public final class SleepingIdleStrategy implements IdleStrategy {

    private static final long DEFAULT_SLEEP_TIME_MILLIS = 1L;

    private final long sleepTimeMillis;

    public SleepingIdleStrategy() {
        this(DEFAULT_SLEEP_TIME_MILLIS);
    }

    public SleepingIdleStrategy(long sleepTimeMillis) {
        if (sleepTimeMillis < 0) {
            throw new IllegalArgumentException("sleepTimeMillis must be greater than or equal to zero: " + sleepTimeMillis);
        }

        this.sleepTimeMillis = sleepTimeMillis;
    }

    @Override
    public void idle() {
        try {
            Thread.sleep(sleepTimeMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
