package com.github.thething.chipamp.mod;

import java.util.Arrays;

import static com.github.thething.chipamp.common.Requirements.requireInRange;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.requireNonNull;

/**
 * A single sample (instrument sound) from a MOD file, along with its playback metadata.
 * <p>
 * A sample's raw {@code data} bytes may be mutated in place via {@link #invertData(int)}, which some tracker effects
 * (invert loop) rely on; all other states are immutable.
 */
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
     * Volume of sample. Legal values are 0..64. Volume is the linear difference between sound intensities. 64 is the
     * full volume.
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

    /**
     * Creates a sample.
     *
     * @param name       the sample's name, or a message if it begins with '#'
     * @param fineTune   the signed fine tune value (-8..7)
     * @param volume     the playback volume (0..64)
     * @param loopStart  the start of the repeat offset in bytes, must be within {@code data}
     * @param loopLength the length of the repeat in bytes; looping is disabled if not greater than 0
     * @param data       the sample data
     * @throws NullPointerException      if {@code name} or {@code data} is {@code null}
     * @throws IllegalArgumentException  if {@code fineTune} or {@code volume} is out of range, or {@code data} exceeds
     *                                   the maximum sample data length
     * @throws IndexOutOfBoundsException if {@code loopStart} and {@code loopLength} do not describe a valid range
     *                                   within {@code data}
     */
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

    /**
     * Returns whether this sample loops during playback.
     *
     * @return {@code true} if {@link #getLoopLength()} is greater than 0
     */
    public boolean isLoopEnabled() {
        return loopLength > 0;
    }

    /**
     * Returns the raw sample byte at the given index.
     *
     * @param index the index into the sample data
     * @return the byte at {@code index}
     */
    public byte getData(int index) {
        return data[index];
    }

    /**
     * Returns a copy of a range of the sample data.
     *
     * @param offset the start index within the sample data (inclusive)
     * @param length the number of bytes to copy
     * @return a new byte array containing the specified range of sample data
     * @throws IndexOutOfBoundsException if {@code offset} and {@code length} do not describe a valid range within the
     *                                   sample data
     */
    public byte[] copyOfData(int offset, int length) {
        checkFromIndexSize(offset, length, data.length);
        return Arrays.copyOfRange(data, offset, offset + length);
    }

    /**
     * Negates the sample byte at the given index in place.
     *
     * @param index the index into the sample data to invert
     */
    public void invertData(int index) {
        data[index] = (byte) -data[index];
    }

    /**
     * Returns the length of the sample data in bytes.
     *
     * @return the length of {@link #getData(int)}'s backing array
     */
    public int getDataLength() {
        return data.length;
    }

    /**
     * Returns whether this sample has no data.
     *
     * @return {@code true} if the sample data length is 0
     */
    public boolean isEmpty() {
        return data.length == 0;
    }

    /**
     * Returns the sample's name.
     *
     * @return the name, or a message if it begins with '#'
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the sample's fine-tune value.
     *
     * @return the signed fine tune value (-8..7)
     */
    public int getFineTune() {
        return fineTune;
    }

    /**
     * Returns the sample's playback volume.
     *
     * @return the volume (0..64)
     */
    public int getVolume() {
        return volume;
    }

    /**
     * Returns the start of the sample's repeat offset.
     *
     * @return the loop start offset in bytes
     */
    public int getLoopStart() {
        return loopStart;
    }

    /**
     * Returns the length of the sample's repeat.
     *
     * @return the loop length in bytes
     */
    public int getLoopLength() {
        return loopLength;
    }

    /**
     * Returns the end of the sample's repeat.
     *
     * @return {@link #getLoopStart()} plus {@link #getLoopLength()}
     */
    public int getLoopEnd() {
        return loopEnd;
    }
}
