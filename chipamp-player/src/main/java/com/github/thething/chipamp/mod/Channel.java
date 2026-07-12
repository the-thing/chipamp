package com.github.thething.chipamp.mod;

/**
 * Represents a single audio channel in a MOD tracker module.
 */
final class Channel {

    // default hardware panning for that channel
    final boolean right;

    Sample sample;
    int volume;
    int fineTune;
    int period;
    float samplePosition;
    float sampleIncrement;
    float leftPan;
    float rightPan;

    /**
     * Effect data for the current row.
     */

    EffectType effectType;
    ExtendedEffectType extendedEffectType;
    int effectArgumentX;
    int effectArgumentY;

    /**
     * Effect state that persists across rows and carries over even when different effects are active.
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
    int invertLoopPosition;
    int invertLoopAccumulator;

    /**
     * Creates a new channel with the specified configuration and panning.
     *
     * @param config the configuration settings
     * @param right  {@code true} if this is a right-panned channel, {@code false} for left
     */
    Channel(Config config, boolean right) {
        this.right = right;
        reset(config);
    }

    /**
     * Resets the channel to its initial state using the provided configuration.
     *
     * @param config the configuration settings
     */
    void reset(Config config) {
        sample = null;
        volume = 64;
        fineTune = 0;
        period = 0;
        samplePosition = 0.0f;
        sampleIncrement = 0.0f;

        updatePanning(config.leftPan, config.rightPan);

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
        periodTriggered = false;
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
        invertLoopPosition = 0;
        invertLoopAccumulator = 0;
    }

    /**
     * Copies all state from another channel to this channel.
     *
     * @param other the channel to copy from
     */
    void copyFrom(Channel other) {
        sample = other.sample;
        volume = other.volume;
        fineTune = other.fineTune;
        period = other.period;
        samplePosition = other.samplePosition;
        sampleIncrement = other.sampleIncrement;
        leftPan = other.leftPan;
        rightPan = other.rightPan;

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
        invertLoopPosition = other.invertLoopPosition;
        invertLoopAccumulator = other.invertLoopAccumulator;
    }

    /**
     * Resets effect state when a new sample with a period is triggered. This resets vibrato and tremolo positions if
     * retriggering is enabled.
     */
    void resetOnNewSampleWithPeriod() {
        if (vibratoRetrigger) {
            vibratoPosition = 0;
        }

        if (tremoloRetrigger) {
            tremoloPosition = 0;
        }
    }

    /**
     * Generates the next audio sample from this channel.
     * <p>
     * This method advances the sample position and handles looping if enabled. The returned value is scaled by the
     * channel's current volume.
     *
     * @return the next audio sample value, or 0.0 if no sample is playing
     */
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

    /**
     * Updates the channel's period and recalculates the sample increment.
     *
     * @param period       the new period value
     * @param clockHz      the module's clock frequency in Hz
     * @param samplingRate the output sampling rate
     */
    void updatePeriodAndIncrement(int period, int clockHz, int samplingRate) {
        this.period = period;

        float noteHz = Mods.convertPeriodToHz(period, clockHz);
        sampleIncrement = (samplingRate > 0 && noteHz > 0) ? noteHz / samplingRate : 0.0f;
    }

    /**
     * Recalculates the sample increment without updating the stored period.
     *
     * @param period       the period value to use for calculation
     * @param clockHz      the module's clock frequency in Hz
     * @param samplingRate the output sampling rate
     */
    void updateIncrement(int period, int clockHz, int samplingRate) {
        float noteHz = Mods.convertPeriodToHz(period, clockHz);
        this.sampleIncrement = (samplingRate > 0 && noteHz > 0) ? noteHz / samplingRate : 0.0f;
    }

    /**
     * Updates the channel with a new sample and its properties.
     *
     * @param sample the new sample to use
     */
    void updateSample(Sample sample) {
        this.sample = sample;
        this.volume = sample.getVolume();
        this.fineTune = sample.getFineTune();
    }

    /**
     * Updates the channel's panning based on the hardware default and pan settings.
     *
     * @param leftPan  the left panning level (0.0-1.0)
     * @param rightPan the right panning level (0.0-1.0)
     */
    void updatePanning(float leftPan, float rightPan) {
        if (right) {
            this.leftPan = 1.0f - rightPan;
            this.rightPan = rightPan;
        } else {
            this.leftPan = leftPan;
            this.rightPan = 1.0f - leftPan;
        }
    }
}
