package com.github.thething.chipamp.mod;

final class Snapshot {

    private final Channel[] channels;
    private final Context context;

    private Mod mod;
    private int sampleCount;
    private int sequenceIndex;
    private int rowIndex;
    private int tickIndex;
    private int sampleIndex;

    Snapshot(int samplingRate) {
        this.channels = new Channel[Mod.MAX_CHANNEL_COUNT];
        this.context = new Context(samplingRate);
    }
}
