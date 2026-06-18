package com.github.thething.chipgroove.mod;

import java.io.PrintStream;

final class Config {

    private static final PrintStream DEFAULT_LOG_STREAM = System.out;
    private static final boolean DEFAULT_LOG_ROW_ENABLED = true;
    private static final int DEFAULT_SAMPLING_RATE = 44_100;
    private static final boolean DEFAULT_STEREO = true;

    int clockHz;
    int samplingRate;
    int minPeriod;
    int maxPeriod;
    boolean stereo;
    PrintStream logger;
    boolean logRowEnabled;

    Config() {
        this.clockHz = Player.PAL_CLOCK_HZ;
        this.samplingRate = DEFAULT_SAMPLING_RATE;
        this.minPeriod = 113;
        this.maxPeriod = 856;
        this.stereo = DEFAULT_STEREO;
        this.logger = DEFAULT_LOG_STREAM;
        this.logRowEnabled = DEFAULT_LOG_ROW_ENABLED;
    }
}
