package com.github.thething.chipgroove;

public final class Sample {

    private final String name;
    private final int length;
    private final int finetune;
    private final int volume;
    private final int loopStart;
    private final int loopLength;
    private final byte[] data;

    // TODO validate
    public Sample(String name, int length, int finetune, int volume, int loopStart, int loopLength, byte[] data) {
        this.name = name;
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
}
