package com.github.thething.chipgroove.mod;

import static com.github.thething.chipgroove.common.Requirements.requireInRange;

final class Channel {

    final boolean right;

    Sample sample;
    int period;
    double samplePosition; // TODO change to float
    double sampleIncrement; // TODO change to flaot
    int volume;
    float leftPanning;
    float rightPanning;

    /**
     * Effect data for the current row.
     */

    EffectType effectType;
    ExtendedEffectType extendedEffectType;
    int effectArgumentX;
    int effectArgumentY;

    /**
     * List of effect that are related to specific effects, but they also persist and should carry over even if there
     * are different effects in between.
     */

    int arpeggioPosition;
    int arpeggioPeriod;

    int portamentoTargetPeriod;
    int portamentoSpeed;

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

    Channel(boolean right) {
        this.right = right;
        reset();
    }

    void reset() {
        period = 0;
        sample = null;
        samplePosition = 0.0;
        sampleIncrement = 0.0;
        volume = 0;
        leftPanning = right ? 0.0f : 1.0f;
        rightPanning = right ? 1.0f : 0.0f;

        effectType = EffectType.NONE;
        extendedEffectType = ExtendedEffectType.NONE;
        effectArgumentX = 0;
        effectArgumentY = 0;

        arpeggioPosition = 0;
        arpeggioPeriod = 0;

        portamentoTargetPeriod = 0;
        portamentoSpeed = 0;

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

    void resetOnNewSampleWithPeriod() {
        vibratoPosition = 0;
        tremoloPosition = 0;
    }

    float nextSample() {
        if (sample == null || sample.isEmpty()) {
            return 0.0f;
        }

        int samplePosition = (int) this.samplePosition;
        float out = 0.0f;

        if (samplePosition < sample.getDataLength()) {
            out = sample.getData(samplePosition) / 128.0f;
        }

        this.samplePosition += sampleIncrement;

        if (sample.isLoopEnabled()) {
            double loopEnd = sample.loopStart() + sample.loopLength();

            if (this.samplePosition >= loopEnd) {
                this.samplePosition = sample.loopStart() + (this.samplePosition - loopEnd) % sample.loopLength();
            }
        } else {
            if (this.samplePosition >= sample.getDataLength()) {
                this.samplePosition = sample.getDataLength();
                sampleIncrement = 0.0;
            }
        }

        return out * (volume / 64.0f);
    }

    void updatePeriodAndIncrement(int period, int clockHz, int samplingRate) {
        this.period = period;

        double noteHz = Mods.periodToHz(period, clockHz);
        sampleIncrement = (samplingRate > 0 && noteHz > 0) ? noteHz / samplingRate : 0;
    }

    void updateIncrement(int period, double clockHz, int samplingRate) {
        double noteHz = Mods.periodToHz(period, clockHz);
        this.sampleIncrement = (samplingRate > 0 && noteHz > 0) ? noteHz / samplingRate : 0.0;
    }

    void setPanning(float right) {
        this.rightPanning = requireInRange(right, 0.0f, 1.0f);
        this.leftPanning = 1.0f - right;
    }
}
