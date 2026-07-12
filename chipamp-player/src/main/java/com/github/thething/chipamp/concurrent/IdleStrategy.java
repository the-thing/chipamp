package com.github.thething.chipamp.concurrent;

public interface IdleStrategy {

    void idle();

    default void idle(int workCount) {
        if (workCount <= 0) {
            idle();
        }
    }
}
