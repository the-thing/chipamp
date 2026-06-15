package com.github.thething.chipgroove.mod;

final class Channel {

    int period;
    int sampleNumber;
    int volume;
    long samplePosition;
    long sampleIncrement;
    int effect;
    int effectArgument;
    boolean muted;

    Channel() {
        reset();
    }

    void reset() {
        period = 0;
        sampleNumber = 0;
        volume = 0;
        samplePosition = 0L;
        sampleIncrement = 0L;
        effect = 0;
        effectArgument = 0;
    }
}
