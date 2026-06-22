package com.github.thething.chipgroove.mod;

import java.io.PrintStream;

final class Config {

    private static final PrintStream DEFAULT_LOG_STREAM = System.out;
    private static final boolean DEFAULT_LOG_ENABLED = false;
    private static final boolean DEFAULT_LOG_ROW_ENABLED = false;
    private static final int DEFAULT_SAMPLING_RATE = 48_000;
    private static final boolean DEFAULT_STEREO_ENABLED = true;
    private static final boolean DEFAULT_STEREO_FOLD_DOWN_ENABLED = true;
    private static final boolean DEFAULT_VOLUME_SLIDE_DELTA_ENABLED = false;
    private static final int DEFAULT_MIN_PERIOD = 108; // minimum standard octave with fine tune +7
    private static final int DEFAULT_MAX_PERIOD = 907; // maximum standard octave with fine tune -8

    final boolean[] muted;
    int clockHz;
    int samplingRate;
    int minPeriod;
    int maxPeriod;
    boolean stereoEnabled;
    boolean stereoFoldDownEnabled;
    boolean volumeSlideDeltaEnabled;
    PrintStream logger;
    boolean logInfoEnabled;
    boolean logErrorEnabled;

    Config(int channelCount) {
        this.muted = new boolean[channelCount];
        reset();
    }

    void reset() {
        this.clockHz = ModTables.PAL_CLOCK_HZ;
        this.samplingRate = DEFAULT_SAMPLING_RATE;
        this.minPeriod = DEFAULT_MIN_PERIOD;
        this.maxPeriod = DEFAULT_MAX_PERIOD;
        this.stereoEnabled = DEFAULT_STEREO_ENABLED;
        this.stereoFoldDownEnabled = DEFAULT_STEREO_FOLD_DOWN_ENABLED;
        this.volumeSlideDeltaEnabled = DEFAULT_VOLUME_SLIDE_DELTA_ENABLED;
        this.logger = DEFAULT_LOG_STREAM;
        this.logInfoEnabled = DEFAULT_LOG_ENABLED;
        this.logErrorEnabled = DEFAULT_LOG_ROW_ENABLED;
    }
}
