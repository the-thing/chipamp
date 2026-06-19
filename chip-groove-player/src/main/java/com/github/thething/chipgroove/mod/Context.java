package com.github.thething.chipgroove.mod;

final class Context {

    private static final int DEFAULT_SPEED = 6;
    private static final int DEFAULT_TEMPO = 125;

    int speed; // ticks per row
    int tempo; // beats per minute
    int samplesPerTick;
    boolean jumpPending;
    int jumpOrder;
    boolean breakPending;
    int breakRow;

    Context(int samplingRate) {
        reset(samplingRate);
    }

    void reset(int samplingRate) {
        speed = DEFAULT_SPEED;
        updateTempo(DEFAULT_TEMPO, samplingRate);
        jumpPending = false;
        jumpOrder = 0;
        breakPending = false;
        breakRow = 0;
    }

    void updateTempo(int tempo, int samplingRate) {
        this.tempo = tempo;
        this.samplesPerTick = samplesPerTick(tempo, samplingRate);
    }

    /**
     * Compute the length of one CIA tick in output samples.
     * <p>
     * ProTracker CIA formula: tickDuration (µs) = 2_500_000 / BPM
     * <p>
     * Converted to output samples: samplesPerTick = outputRate * tickDuration_µs / 1_000_000 = outputRate * 2_500_000 /
     * (BPM * 1_000_000) = outputRate * 2.5 / BPM
     * <p>
     */
    private static int samplesPerTick(int tempo, int samplingRate) {
        return (int) Math.round((double) samplingRate * 2_500_000.0 / (tempo * 1_000_000.0));
    }
}
