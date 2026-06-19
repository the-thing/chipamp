package com.github.thething.chipgroove.mod;

import java.io.PrintStream;

final class Config {

    private static final PrintStream DEFAULT_LOG_STREAM = System.out;
    private static final boolean DEFAULT_LOG_ENABLED = true;
    private static final boolean DEFAULT_LOG_ROW_ENABLED = true;
    private static final int DEFAULT_SAMPLING_RATE = 48_000;
    private static final boolean DEFAULT_STEREO = true;
    private static final boolean DEFAULT_VOLUME_SLIDE_DELTA = false;

    int clockHz;
    int samplingRate;
    int minPeriod;
    int maxPeriod;
    boolean stereo;
    boolean volumeSlideDelta;
    PrintStream logger;
    boolean logEnabled;
    boolean logRowEnabled;

    Config() {
        reset();
    }

    void reset() {
        this.clockHz = Player.PAL_CLOCK_HZ;
        this.samplingRate = DEFAULT_SAMPLING_RATE;
        // TODO
        this.minPeriod = 113;
        this.maxPeriod = 856;
        this.stereo = DEFAULT_STEREO;
        this.volumeSlideDelta = DEFAULT_VOLUME_SLIDE_DELTA;
        this.logger = DEFAULT_LOG_STREAM;
        this.logEnabled = DEFAULT_LOG_ENABLED;
        this.logRowEnabled = DEFAULT_LOG_ROW_ENABLED;
    }
}
