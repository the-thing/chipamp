package com.github.thething.chipgroove.mod;

final class Channel {

    int period;
    int sampleNumber;
    double samplePosition;
    double sampleIncrement;
    int volume;

    Effect effect;
    ExtendedEffect extendedEffect;
    int effectArgumentX;
    int effectArgumentY;

    int maxPeriod;

    Channel() {
        reset();
    }

    void reset() {
        period = 0;
        sampleNumber = 0;
        samplePosition = 0.0;
        sampleIncrement = 0.0;
        volume = 0;
        effect = Effect.NONE;
        extendedEffect = ExtendedEffect.NONE;
        effectArgumentX = 0;
        effectArgumentY = 0;
        maxPeriod = 0;
    }

    float nextSample(Mod mod) {
        if (sampleNumber <= 0) {
            return 0.0f;
        }

        Sample sample = mod.getSample(sampleNumber - 1);

        if (sample.isEmpty()) {
            return 0.0f;
        }

        int samplePosition = (int) this.samplePosition;
        float out = 0.0f;

        if (samplePosition < sample.getDataLength()) {
            out = sample.getData(samplePosition) / 128.0f;
        }

        this.samplePosition += sampleIncrement;

        if (sample.isLoopEnabled()) {
            double loopEnd = sample.getLoopStart() + sample.getLoopLength();

            if (this.samplePosition >= loopEnd) {
                this.samplePosition = sample.getLoopStart() + (this.samplePosition - loopEnd) % sample.getLoopLength();
            }
        } else {
            if (this.samplePosition >= sample.getDataLength()) {
                this.samplePosition = sample.getDataLength();
                sampleIncrement = 0.0;
            }
        }

        return out * (volume / 64.0f);
    }
}
