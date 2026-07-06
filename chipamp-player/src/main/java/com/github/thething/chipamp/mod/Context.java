package com.github.thething.chipamp.mod;

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

    boolean hardwareFilterEnabled;
    float hardwareFilterDelta;
    float hardwareFilterLeft;
    float hardwareFilterRight;

    Context(int samplingRate) {
        reset(samplingRate);
    }

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

        hardwareFilterEnabled = false;
        hardwareFilterDelta = 0.0f;
        hardwareFilterLeft = 0.0f;
        hardwareFilterRight = 0.0f;
    }

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

        hardwareFilterEnabled = other.hardwareFilterEnabled;
        hardwareFilterDelta = other.hardwareFilterDelta;
        hardwareFilterLeft = other.hardwareFilterLeft;
        hardwareFilterRight = other.hardwareFilterRight;
    }

    void updateTempoAndSamplesPerTick(int tempo, int samplingRate) {
        this.tempo = tempo;
        this.samplesPerTick = samplesPerTick(tempo, samplingRate);
    }

    void updateHardwareFilterDelta(int samplingRate) {
        float cutoffHz = 4900.0f;
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoffHz);
        float dt = 1.0f / samplingRate;
        hardwareFilterDelta = dt / (rc + dt);
    }

    /**
     * Compute the length of one CIA tick in output samples.
     * <p>
     * ProTracker CIA formula: tickDuration (µs) = 2_500_000 / BPM
     * <p>
     * Converted to output samples: samplesPerTick = samplingRate * tickDuration_µs / 1_000_000 = samplingRate *
     * 2_500_000 / (BPM * 1_000_000) = samplingRate * 2.5 / BPM
     * <p>
     */
    private static int samplesPerTick(int tempo, int samplingRate) {
        return (int) Math.round((double) samplingRate * 2_500_000.0 / (tempo * 1_000_000.0));
    }

    @Override
    public String toString() {
        return "Context{" +
                "speed=" + speed +
                ", tempo=" + tempo +
                ", samplesPerTick=" + samplesPerTick +
                ", jumpPending=" + jumpPending +
                ", jumpSequenceIndex=" + jumpSequenceIndex +
                ", breakPending=" + breakPending +
                ", breakRowIndex=" + breakRowIndex +
                ", loopPending=" + loopPending +
                ", loopRowIndex=" + loopRowIndex +
                ", loopCounter=" + loopCounter +
                ", hardwareFilterEnabled=" + hardwareFilterEnabled +
                ", hardwareFilterDelta=" + hardwareFilterDelta +
                ", hardwareFilterLeft=" + hardwareFilterLeft +
                ", hardwareFilterRight=" + hardwareFilterRight +
                '}';
    }
}
