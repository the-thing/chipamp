package com.github.thething.chipgroove.mod;

final class Channel {

    int period;
    int previousPeriod;
    int sampleNumber;
    double samplePosition;
    double previousSamplePosition;
    double sampleIncrement;
    int volume;
    // TODO maybe this should be a part of config?
    boolean left; // hardware panning, left == true -> 100% goes to left channel, otherwise 100% goes to right channel

    EffectType effectType;
    ExtendedEffectType extendedEffectType;
    int effectArgumentX;
    int effectArgumentY;

    /**
     * List of effect that are related to specific effects, but they also persist and should carry over even if there
     * are effects in between.
     */

    int portamentoPeriod;
    int volumeSlide; // volume recorded when hitting first row with volume slide or vibrato / tremolo with volume side

    int vibratoPosition;
    int vibratoSpeed;
    int vibratoAmplitude;
    int vibratoPeriod; // period recorded when hitting first row in vibrato
    WaveformType vibratoWaveformType;

    int tremoloPosition;
    int tremoloSpeed;
    int tremoloAmplitude;
    int tremoloVolume; // volume recorded when hitting first row with tremolo
    WaveformType tremoloWaveformType;

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

        effectType = EffectType.NONE;
        extendedEffectType = ExtendedEffectType.NONE;
        effectArgumentX = 0;
        effectArgumentY = 0;

        portamentoPeriod = 0;
        volumeSlide = 0;

        vibratoPosition = 0;
        vibratoSpeed = 0;
        vibratoAmplitude = 0;
        vibratoPeriod = 0;
        vibratoWaveformType = WaveformType.SINE;

        tremoloPosition = 0;
        tremoloSpeed = 0;
        tremoloAmplitude = 0;
        tremoloVolume = 0;
        tremoloWaveformType = WaveformType.SINE;
    }

    void resetOnNewSample() {
        vibratoPosition = 0;
        tremoloPosition = 0;
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
