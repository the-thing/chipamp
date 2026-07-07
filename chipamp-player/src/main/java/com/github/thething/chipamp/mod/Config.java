package com.github.thething.chipamp.mod;

import java.util.Arrays;

// TODO add configuration for fine tune period lookup (if should calculate or find nearest)
final class Config {

    private static final boolean DEFAULT_LOGGING_ENABLED = false;
    private static final boolean DEFAULT_LOOP_DETECTION_ENABLED = true;
    private static final int DEFAULT_SAMPLING_RATE = 48_000;
    private static final boolean DEFAULT_STEREO_ENABLED = true;
    private static final boolean DEFAULT_STEREO_FOLD_DOWN_ENABLED = false;
    private static final boolean DEFAULT_VOLUME_SLIDE_DELTA_ENABLED = false;
    private static final boolean DEFAULT_EFFECT_ENABLED = true;
    private static final int DEFAULT_MIN_PERIOD = Mods.MIN_PERIOD;
    private static final int DEFAULT_MAX_PERIOD = Mods.MAX_PERIOD;
    private static final int DEFAULT_CLOCK_HZ = Mods.PAL_CLOCK_HZ;
    private static final float DEFAULT_VOLUME_MULTIPLIER = 0.5f;
    private static final float DEFAULT_LEFT_PAN = Mods.MPT_LEFT_PAN;
    private static final float DEFAULT_RIGHT_PAN = Mods.MPT_RIGHT_PAN;
    private static final boolean DEFAULT_ROUND_NEAREST_PERIOD_ENABLED = true;

    final boolean[] muted;
    final boolean[] effectEnabled;
    final boolean[] extendedEffectEnabled;
    int clockHz;
    int samplingRate;
    int minPeriod;
    int maxPeriod;
    float volumeMultiplier;
    float leftPan;
    float rightPan;
    boolean stereoEnabled;
    boolean stereoFoldDownEnabled;
    boolean volumeSlideDeltaEnabled;
    boolean roundNearestPeriodEnabled;
    boolean loopDetectionEnabled;
    boolean loggingEnabled;

    Config(int channelCount) {
        this.muted = new boolean[channelCount];
        this.effectEnabled = new boolean[16];
        Arrays.fill(this.effectEnabled, DEFAULT_EFFECT_ENABLED);
        this.extendedEffectEnabled = new boolean[16];
        Arrays.fill(this.extendedEffectEnabled, DEFAULT_EFFECT_ENABLED);
        reset();
    }

    void reset() {
        Arrays.fill(this.muted, false);
        Arrays.fill(this.effectEnabled, DEFAULT_EFFECT_ENABLED);
        Arrays.fill(this.extendedEffectEnabled, DEFAULT_EFFECT_ENABLED);
        this.clockHz = DEFAULT_CLOCK_HZ;
        this.samplingRate = DEFAULT_SAMPLING_RATE;
        this.minPeriod = DEFAULT_MIN_PERIOD;
        this.maxPeriod = DEFAULT_MAX_PERIOD;
        this.volumeMultiplier = DEFAULT_VOLUME_MULTIPLIER;
        this.leftPan = DEFAULT_LEFT_PAN;
        this.rightPan = DEFAULT_RIGHT_PAN;
        this.stereoEnabled = DEFAULT_STEREO_ENABLED;
        this.stereoFoldDownEnabled = DEFAULT_STEREO_FOLD_DOWN_ENABLED;
        this.volumeSlideDeltaEnabled = DEFAULT_VOLUME_SLIDE_DELTA_ENABLED;
        this.roundNearestPeriodEnabled = DEFAULT_ROUND_NEAREST_PERIOD_ENABLED;
        this.loopDetectionEnabled = DEFAULT_LOOP_DETECTION_ENABLED;
        this.loggingEnabled = DEFAULT_LOGGING_ENABLED;
    }
}
