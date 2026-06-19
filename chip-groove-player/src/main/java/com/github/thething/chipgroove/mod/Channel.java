package com.github.thething.chipgroove.mod;

final class Channel {

    int period;
    int previousPeriod;
    int sampleNumber;
    double samplePosition;
    double previousSamplePosition;
    double sampleIncrement;
    int volume;
    boolean muted;
    boolean left;

    EffectType effectType;
    EffectType previousEffectType;
    ExtendedEffectType extendedEffectType;
    ExtendedEffectType previousExtendedEffectType;
    int effectArgumentX;
    int effectArgumentY;
    int previousEffectArgumentX;
    int previousEffectArgumentY;

    int portamentoPeriod;
    int tremoloPosition;
    int tremoloSpeed;
    int tremoloDepth;
    WaveformType tremoloWaveformType;
    int tremoloVolume;

    Channel(boolean left) {
        this.left = left;
        reset();
    }

    void reset() {
        period = 0;
        previousPeriod = 0;
        sampleNumber = 0;
        samplePosition = 0.0;
        previousSamplePosition = 0.0;
        sampleIncrement = 0.0;
        volume = 0;
        muted = false;

        effectType = EffectType.NONE;
        previousEffectType = EffectType.NONE;
        extendedEffectType = ExtendedEffectType.NONE;
        previousExtendedEffectType = ExtendedEffectType.NONE;
        effectArgumentX = 0;
        effectArgumentY = 0;

        portamentoPeriod = 0;
        tremoloPosition = 0;
        tremoloSpeed = 0;
        tremoloDepth = 0;
        tremoloWaveformType = WaveformType.SINE;
        tremoloVolume = 0;
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

    void updatePeriod(int period, int clockHz, int samplingRate) {
        this.period = period;

        double noteHz = Mods.periodToHz(period, clockHz);
        sampleIncrement = (samplingRate > 0 && noteHz > 0) ? noteHz / samplingRate : 0;
    }
}
