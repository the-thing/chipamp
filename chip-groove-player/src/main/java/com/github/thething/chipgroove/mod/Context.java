package com.github.thething.chipgroove.mod;

final class Context {

    private static final int DEFAULT_SPEED = 6;
    private static final int DEFAULT_TEMPO = 126;

    int speed; // ticks per row
    int tempo; // beats per minute
    int samplesPerTick;
    boolean jumpPending;
    int jumpOrder;
    boolean breakPending;
    int breakRow;

    void reset(int samplingRate) {
        speed = DEFAULT_SPEED;
        tempo = DEFAULT_TEMPO;
        samplesPerTick = samplesPerTick(tempo, samplingRate);
        jumpPending = false;
        jumpOrder = 0;
        breakPending = false;
        breakRow = 0;
    }

    void updateTempo(int tempo, int samplingRate) {
        this.tempo = tempo;
        this.samplesPerTick = samplesPerTick(tempo, samplingRate);
    }

    private static int samplesPerTick(int tempo, int samplingRate) {
        return (int) Math.round((double) samplingRate * 2_500_000.0 / (tempo * 1_000_000.0));
    }
}
