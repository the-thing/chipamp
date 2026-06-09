package com.github.thething.chipgroove.mod;

import static com.github.thething.chipgroove.common.Requirements.requireInRange;

public record Instrument(int sampleNumber, int period, int effect, int effectArgument) {

    public Instrument(int sampleNumber, int period, int effect, int effectArgument) {
        this.sampleNumber = requireInRange(sampleNumber, 0, 255);
        this.period = requireInRange(period, 0, Short.MAX_VALUE);
        this.effect = requireInRange(effect, 0, 255);
        this.effectArgument = requireInRange(effectArgument, 0, 255);
    }
}
