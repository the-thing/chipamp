package com.github.thething.chipgroove.mod;

final class Channel {

    int period;
    int sampleNumber;
    int volume;
    long samplePosition;
    long sampleIncrement;
    Effect effect;
    ExtendedEffect extendedEffect;
    int effectArgumentX;
    int effectArgumentY;
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
        effect = Effect.NONE;
        extendedEffect = ExtendedEffect.NONE;
        effectArgumentX = 0;
        effectArgumentY = 0;
    }
}
