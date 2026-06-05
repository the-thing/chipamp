package com.github.thething.chipamp.mod;

/**
 * The shape of a waveform used to generate an audio signal.
 */
public enum WaveformType {

    /**
     * A smooth, periodic waveform.
     */
    SINE,

    /**
     * A waveform that ramps linearly before dropping sharply.
     */
    SAWTOOTH,

    /**
     * A waveform that alternates between two fixed levels.
     */
    SQUARE
}
