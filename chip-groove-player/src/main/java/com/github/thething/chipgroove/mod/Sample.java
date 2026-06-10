package com.github.thething.chipgroove.mod;

import static java.util.Objects.requireNonNull;

public final class Sample {

    /**
     * Sample's name. If a name begins with a '#', it is assumed not to be an instrument name, and is probably a
     * message.
     */
    private final String name;

    /**
     * Sample length in words (1 word = 2 bytes). The first word of the sample is overwritten by the tracker, so a
     * length of 1 still means an empty sample.
     */
    private final int length;

    /**
     * Lowest four bits represent a signed nibble (-8..7) which is the finetune value for the sample. Each finetune step
     * changes the note 1/8th of a semitone. Implemented by switching to a different table of period-values for each
     * finetune value.
     */
    private final int finetune;
    private final int volume;
    private final int loopStart;
    private final int loopLength;
    private final byte[] data;

    // TODO validate
    public Sample(String name, int length, int finetune, int volume, int loopStart, int loopLength, byte[] data) {
        this.name = requireNonNull(name);
        this.length = length;
        this.finetune = finetune;
        this.volume = volume;
        this.loopStart = loopStart;
        this.loopLength = loopLength;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public int getLength() {
        return length;
    }

    public int getFinetune() {
        return finetune;
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

    public byte getData(int index) {
        return data[index];
    }

    public byte[] getData() {
        return data;
    }
}
