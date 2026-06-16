package com.github.thething.chipgroove.mod;

final class Channel {

    int period;
    int sampleNumber;
    int volume;
//    long samplePosition;
//    long sampleIncrement;
    Effect effect;
    ExtendedEffect extendedEffect;
    int effectArgumentX;
    int effectArgumentY;
    boolean muted;

    double samplePositionDouble;
    double sampleIncrementDouble;

    Channel() {
        reset();
    }

    void reset() {
        period = 0;
        sampleNumber = 0;
        volume = 0;
//        samplePosition = 0L;
//        sampleIncrement = 0L;
        effect = Effect.NONE;
        extendedEffect = ExtendedEffect.NONE;
        effectArgumentX = 0;
        effectArgumentY = 0;
    }

    float nextSample(Mod mod) {
        if (sampleNumber <= 0) {
            return 0.0f;
        }

        Sample sample = mod.getSample(sampleNumber - 1);

        if (sample.isEmpty()) {
            return 0.0f;
        }

        int samplePosition = (int) samplePositionDouble;
        float out = 0.0f;

        if (samplePosition < sample.getDataLength()) {
            out = sample.getData(samplePosition) / 128.0f;
        }

        samplePositionDouble += sampleIncrementDouble;

        if (sample.isLoopEnabled()) {
            double loopEnd = sample.getLoopStart() + sample.getLoopLength();

            if (samplePositionDouble >= loopEnd) {
                samplePositionDouble = sample.getLoopStart() + (samplePositionDouble - loopEnd) % sample.getLoopLength();
            }
        } else {
            if (samplePositionDouble >= sample.getDataLength()) {
                samplePositionDouble = sample.getDataLength();
                sampleIncrementDouble = 0.0;
            }
        }

        return out * (volume / 64.0f);
    }
}
