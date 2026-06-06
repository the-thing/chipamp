package com.github.thething.chipgroove;

public class Sample {

    private final String name;
    private final int length;
    private final int finetune;
    private final int volume;
    private final int repeatOffset;

    public Sample(String name, int length, int finetune, int volume, int repeatOffset) {
        this.name = name;
        this.length = length;
        this.finetune = finetune;
        this.volume = volume;
        this.repeatOffset = repeatOffset;
    }
}
