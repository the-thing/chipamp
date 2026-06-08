package com.github.thething.chipgroove.mod;

import static com.github.thething.chipgroove.common.Requirements.requireInRange;

public record Pattern(int sample, int pitch, int effect, int effectArgument) {

    public Pattern(int sample, int pitch, int effect, int effectArgument) {
        this.sample = requireInRange(sample, 0, 255);
        this.pitch = requireInRange(pitch, 0, 32768);
        this.effect = requireInRange(effect, 0, 255);
        this.effectArgument = requireInRange(effectArgument, 0, 255);
    }
}
