package com.github.thething.chipamp.mod;

import static com.github.thething.chipamp.common.Requirements.requireInRange;

final class Channel {

    /**
     * Hardware panning left or right.
     */
    final boolean right;

    Sample sample;
    int period;
    float samplePosition;
    float sampleIncrement;
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

    int arpeggioTickIndex;
    int arpeggioPeriod;
    int portamentoTargetPeriod;
    int portamentoSpeed;
    int retriggerTickIndex;
    int cutSampleIndex;
    boolean glissandoEnabled;
    boolean periodTriggered;
    int volumeSlide;
    int vibratoPosition;
    int vibratoSpeed;
    int vibratoAmplitude;
    int vibratoPeriod;
    boolean vibratoRetrigger;
    WaveformType vibratoWaveformType;
    int tremoloPosition;
    int tremoloSpeed;
    int tremoloAmplitude;
    int tremoloVolume;
    boolean tremoloRetrigger;
    WaveformType tremoloWaveformType;
    int delayedTickIndex;
    int delayedTriggerTickIndex;
    int delayedPeriod;
    Sample delayedSample;
    int loopRowIndex;
    int loopCounter;

    Channel(boolean right) {
        this.right = right;
        reset();
    }

    void reset() {
        sample = null;
        period = 0;
        periodTriggered = false;
        samplePosition = 0.0f;
        sampleIncrement = 0.0f;
        volume = 64;
        leftPanning = right ? 0.0f : 1.0f;
        rightPanning = right ? 1.0f : 0.0f;

        effectType = EffectType.NONE;
        extendedEffectType = ExtendedEffectType.NONE;
        effectArgumentX = 0;
        effectArgumentY = 0;

        arpeggioTickIndex = 0;
        arpeggioPeriod = 0;
        portamentoTargetPeriod = 0;
        portamentoSpeed = 0;
        retriggerTickIndex = 0;
        cutSampleIndex = 0;
        glissandoEnabled = false;
        volumeSlide = 0;
        vibratoPosition = 0;
        vibratoSpeed = 0;
        vibratoAmplitude = 0;
        vibratoPeriod = 0;
        vibratoRetrigger = true;
        vibratoWaveformType = WaveformType.SINE;
        tremoloPosition = 0;
        tremoloSpeed = 0;
        tremoloAmplitude = 0;
        tremoloVolume = 0;
        tremoloRetrigger = true;
        tremoloWaveformType = WaveformType.SINE;
        loopRowIndex = 0;
        loopCounter = 0;
    }

    void resetOnNewSampleWithPeriod() {
        if (vibratoRetrigger) {
            vibratoPosition = 0;
        }

        if (tremoloRetrigger) {
            tremoloPosition = 0;
        }
    }

    float nextSample() {
        if (sample == null || sample.isEmpty()) {
            return 0.0f;
        }

        float samplePosition = this.samplePosition;
        float out = 0.0f;

        if (samplePosition < sample.getDataLength()) {
            out = sample.getData((int) samplePosition) / 128.0f;
        }

        this.samplePosition += sampleIncrement;

        if (sample.isLoopEnabled()) {
            float loopEnd = sample.getLoopStart() + sample.getLoopLength();

            if (this.samplePosition >= loopEnd) {
                this.samplePosition = sample.getLoopStart() + (this.samplePosition - loopEnd) % sample.getLoopLength();
            }
        } else {
            if (this.samplePosition >= sample.getDataLength()) {
                this.samplePosition = sample.getDataLength();
                this.sampleIncrement = 0.0f;
            }
        }

        return out * (volume / 64.0f);
    }

    void updatePeriodAndIncrement(int period, int clockHz, int samplingRate) {
        this.period = period;

        float noteHz = Mods.periodToHz(period, clockHz);
        sampleIncrement = (samplingRate > 0 && noteHz > 0) ? noteHz / samplingRate : 0.0f;
    }

    void updateIncrement(int period, int clockHz, int samplingRate) {
        float noteHz = Mods.periodToHz(period, clockHz);
        this.sampleIncrement = (samplingRate > 0 && noteHz > 0) ? noteHz / samplingRate : 0.0f;
    }

    void setPanning(float right) {
        this.rightPanning = requireInRange(right, 0.0f, 1.0f);
        this.leftPanning = 1.0f - right;
    }

    void copyFrom(Channel other) {
        sample = other.sample;
        period = other.period;
        samplePosition = other.samplePosition;
        sampleIncrement = other.sampleIncrement;
        volume = other.volume;
        leftPanning = other.leftPanning;
        rightPanning = other.rightPanning;

        effectType = other.effectType;
        extendedEffectType = other.extendedEffectType;
        effectArgumentX = other.effectArgumentX;
        effectArgumentY = other.effectArgumentY;

        arpeggioTickIndex = other.arpeggioTickIndex;
        arpeggioPeriod = other.arpeggioPeriod;
        portamentoTargetPeriod = other.portamentoTargetPeriod;
        portamentoSpeed = other.portamentoSpeed;
        retriggerTickIndex = other.retriggerTickIndex;
        cutSampleIndex = other.cutSampleIndex;
        glissandoEnabled = other.glissandoEnabled;
        periodTriggered = other.periodTriggered;
        volumeSlide = other.volumeSlide;
        vibratoPosition = other.vibratoPosition;
        vibratoSpeed = other.vibratoSpeed;
        vibratoAmplitude = other.vibratoAmplitude;
        vibratoPeriod = other.vibratoPeriod;
        vibratoRetrigger = other.vibratoRetrigger;
        vibratoWaveformType = other.vibratoWaveformType;
        tremoloPosition = other.tremoloPosition;
        tremoloSpeed = other.tremoloSpeed;
        tremoloAmplitude = other.tremoloAmplitude;
        tremoloVolume = other.tremoloVolume;
        tremoloRetrigger = other.tremoloRetrigger;
        tremoloWaveformType = other.tremoloWaveformType;
        delayedTickIndex = other.delayedTickIndex;
        delayedTriggerTickIndex = other.delayedTriggerTickIndex;
        delayedPeriod = other.delayedPeriod;
        delayedSample = other.delayedSample;
        loopRowIndex = other.loopRowIndex;
        loopCounter = other.loopCounter;
    }
}
