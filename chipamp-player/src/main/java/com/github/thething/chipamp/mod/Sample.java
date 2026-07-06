package com.github.thething.chipamp.mod;

import java.util.Arrays;

import static com.github.thething.chipamp.common.Requirements.requireInRange;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.requireNonNull;

public final class Sample {

    private static final int MIN_DATA_LENGTH = 0;
    private static final int MAX_DATA_LENGTH = 131_072;
    private static final int MIN_FINE_TUNE = -8;
    private static final int MAX_FINE_TUNE = 7;
    private static final int MIN_VOLUME = 0;
    private static final int MAX_VOLUME = 64;

    /**
     * Sample's name. If a name begins with a '#', it is assumed not to be an instrument name, and is probably a
     * message.
     */
    private final String name;

    /**
     * The lowest four bits represent a signed nibble (-8..7) which is the fine tune value for the sample. Each fine
     * tune step changes the note 1/8th of a semitone. Implemented by switching to a different table of period-values
     * for each fine-tune value.
     */
    private final int fineTune;

    /**
     * Volume of sample. Legal values are 0..64. Volume is the linear difference between sound intensities. 64 is full
     * volume, and the change in decibels can be calculated with 20 * log10(volume / 64).
     */
    private final int volume;

    /**
     * Start of the sample repeat offset in bytes. Once the sample has been played all the way through, it will loop if
     * the repeat length is greater than one. It repeats by jumping to this position in the sample and playing for the
     * repeat length, then jumping back to this position and playing for the repeat length, etc.
     */
    private final int loopStart;

    /**
     * Length of sample repeat in bytes. Only loop if greater than 2 (due to conversion from words to
     */
    private final int loopLength;

    /**
     * loopStart + loopLength
     */
    private final int loopEnd;

    /**
     * Sample data.
     */
    private final byte[] data;

    public Sample(String name, int fineTune, int volume, int loopStart, int loopLength, byte[] data) {
        this.name = requireNonNull(name);
        this.fineTune = requireInRange(fineTune, MIN_FINE_TUNE, MAX_FINE_TUNE);
        this.volume = requireInRange(volume, MIN_VOLUME, MAX_VOLUME);
        this.loopStart = requireInRange(loopStart, MIN_DATA_LENGTH, MAX_DATA_LENGTH);
        checkFromIndexSize(loopStart, loopLength, data.length);
        this.loopLength = loopLength;
        this.loopEnd = loopStart + loopLength;
        requireInRange(data.length, MIN_DATA_LENGTH, MAX_DATA_LENGTH);
        this.data = data;
    }

    public boolean isLoopEnabled() {
        return loopLength > 0;
    }

    public byte getData(int index) {
        return data[index];
    }

    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }

    public int getDataLength() {
        return data.length;
    }

    public boolean isEmpty() {
        return data.length == 0;
    }

    public String getName() {
        return name;
    }

    public int getFineTune() {
        return fineTune;
    }

    public int getVolume() {
        return volume;
    }

    public int getLoopStart() {
        return loopStart;
    }

    public int getLoopLength() {
        return loopLength;
    }

    public int getLoopEnd() {
        return loopEnd;
    }
}
