package com.github.thething.chipgroove.mod;

import java.util.Arrays;

import static com.github.thething.chipgroove.common.Requirements.requireInRange;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.requireNonNull;

/**
 * @param name       Sample's name. If a name begins with a '#', it is assumed not to be an instrument name, and is
 *                   probably a message.
 * @param fineTune   The lowest four bits represent a signed nibble (-8..7) which is the fine tune value for the sample.
 *                   Each fine tune step changes the note 1/8th of a semitone. Implemented by switching to a different
 *                   table of period-values for each fine-tune value.
 * @param volume     Volume of sample. Legal values are 0..64. Volume is the linear difference between sound
 *                   intensities. 64 is full volume, and the change in decibels can be calculated with 20 * log10(volume
 *                   / 64).
 * @param loopStart  Start of the sample repeat offset in bytes. Once the sample has been played all the way through, it
 *                   will loop if the repeat length is greater than one. It repeats by jumping to this position in the
 *                   sample and playing for the repeat length, then jumping back to this position and playing for the
 *                   repeat length, etc.
 * @param loopLength Length of sample repeat in bytes. Only loop if greater than 2 (due to conversion from words to
 *                   bytes).
 * @param data       Sample data.
 */
public record Sample(String name, int fineTune, int volume, int loopStart, int loopLength, byte[] data) {

    private static final int MIN_DATA_LENGTH = 0;
    private static final int MAX_DATA_LENGTH = 131_072;
    private static final int MIN_FINE_TUNE = -8;
    private static final int MAX_FINE_TUNE = 7;
    private static final int MIN_VOLUME = 0;
    private static final int MAX_VOLUME = 64;

    public Sample(String name, int fineTune, int volume, int loopStart, int loopLength, byte[] data) {
        this.name = requireNonNull(name);
        this.fineTune = requireInRange(fineTune, MIN_FINE_TUNE, MAX_FINE_TUNE);
        this.volume = requireInRange(volume, MIN_VOLUME, MAX_VOLUME);
        this.loopStart = requireInRange(loopStart, MIN_DATA_LENGTH, MAX_DATA_LENGTH);
        // some mods store loop length as 0 and not 1 world (2 bytes)
        checkFromIndexSize(loopStart, Math.max(0, loopLength - 2), data.length);
        this.loopLength = loopLength;
        requireInRange(data.length, MIN_DATA_LENGTH, MAX_DATA_LENGTH);
        this.data = data;
    }

    /**
     * Loop enabled flag. The tracker overwrites the first word of the sample, so a length of 2 still means an empty
     * sample.
     *
     * @return sample length in bytes
     */
    public boolean isLoopEnabled() {
        return loopLength > 2;
    }

    public byte getData(int index) {
        return data[index];
    }

    @Override
    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Sample length in bytes. The tracker overwrites the first word of the sample, so a length of 2 still means an
     * empty sample.
     *
     * @return sample length in bytes
     */
    public int getDataLength() {
        return data.length;
    }

    public boolean isEmpty() {
        return data.length <= 2;
    }
}
