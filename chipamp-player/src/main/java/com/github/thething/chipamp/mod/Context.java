package com.github.thething.chipamp.mod;

/**
 * Playback context for MOD music tracking. Maintains tempo, speed, pattern flow control, and hardware filter state.
 */
final class Context {

    private static final int DEFAULT_SPEED = 6;
    private static final int DEFAULT_TEMPO = 125;

    int speed; // ticks per row
    int tempo; // beats per minute
    int samplesPerTick;
    boolean jumpPending;
    int jumpSequenceIndex;
    boolean breakPending;
    int breakRowIndex;
    boolean loopPending;
    int loopRowIndex;
    int loopCounter;
    int extraDelay;
    boolean hardwareFilterEnabled;
    float hardwareFilterDelta;
    float hardwareFilterLeft;
    float hardwareFilterRight;

    /**
     * Creates a new playback context with default settings.
     *
     * @param samplingRate the output sampling rate in Hz
     */
    Context(int samplingRate) {
        reset(samplingRate);
    }

    /**
     * Resets all context fields to their default values.
     *
     * @param samplingRate the output sampling rate in Hz
     */
    void reset(int samplingRate) {
        speed = DEFAULT_SPEED;
        updateTempoAndSamplesPerTick(DEFAULT_TEMPO, samplingRate);

        jumpPending = false;
        jumpSequenceIndex = 0;

        breakPending = false;
        breakRowIndex = 0;

        loopPending = false;
        loopRowIndex = 0;
        loopCounter = 0;

        extraDelay = 0;

        hardwareFilterEnabled = false;
        hardwareFilterDelta = 0.0f;
        hardwareFilterLeft = 0.0f;
        hardwareFilterRight = 0.0f;
    }

    /**
     * Copies all fields from another context.
     *
     * @param other the source context to copy from
     */
    void copyFrom(Context other) {
        speed = other.speed;
        tempo = other.tempo;
        samplesPerTick = other.samplesPerTick;

        jumpPending = other.jumpPending;
        jumpSequenceIndex = other.jumpSequenceIndex;

        breakPending = other.breakPending;
        breakRowIndex = other.breakRowIndex;

        loopPending = other.loopPending;
        loopRowIndex = other.loopRowIndex;
        loopCounter = other.loopCounter;

        extraDelay = other.extraDelay;

        hardwareFilterEnabled = other.hardwareFilterEnabled;
        hardwareFilterDelta = other.hardwareFilterDelta;
        hardwareFilterLeft = other.hardwareFilterLeft;
        hardwareFilterRight = other.hardwareFilterRight;
    }

    /**
     * Updates the tempo and recalculates samples per tick.
     *
     * @param tempo        the new tempo in beats per minute
     * @param samplingRate the output sampling rate in Hz
     */
    void updateTempoAndSamplesPerTick(int tempo, int samplingRate) {
        this.tempo = tempo;
        this.samplesPerTick = samplesPerTick(tempo, samplingRate);
    }

    /**
     * Recalculates the hardware filter coefficient based on the sampling rate. Uses a 4900 Hz cutoff frequency to
     * emulate the Amiga 500 low-pass filter.
     *
     * @param samplingRate the output sampling rate in Hz
     */
    void updateHardwareFilterDelta(int samplingRate) {
        float cutoffHz = 4900.0f;
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoffHz);
        float dt = 1.0f / samplingRate;
        hardwareFilterDelta = dt / (rc + dt);
    }

    /**
     * Computes the length of one CIA tick in output samples.
     * <p>
     * ProTracker CIA formula: tickDuration (µs) = 2_500_000 / BPM
     * <p>
     * Converted to output samples: samplesPerTick = samplingRate * tickDuration_µs / 1_000_000 = samplingRate *
     * 2_500_000 / (BPM * 1_000_000) = samplingRate * 2.5 / BPM
     *
     * @param tempo        the tempo in beats per minute
     * @param samplingRate the output sampling rate in Hz
     * @return the number of samples per tick
     */
    private static int samplesPerTick(int tempo, int samplingRate) {
        return (int) Math.round((double) samplingRate * 2_500_000.0 / (tempo * 1_000_000.0));
    }
}
