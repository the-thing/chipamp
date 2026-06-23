package com.github.thething.chipgroove.mod;

import static com.github.thething.chipgroove.common.Requirements.requireInRange;
import static java.util.Objects.requireNonNull;

public record Instrument(int sampleNumber, int period,
                         EffectType effectType, ExtendedEffectType extendedEffectType, int effectArgumentX,
                         int effectArgumentY) {

    public Instrument(
            int sampleNumber, int period,
            EffectType effectType, ExtendedEffectType extendedEffectType, int effectArgumentX, int effectArgumentY) {
        this.sampleNumber = requireInRange(sampleNumber, 0, 255);
        this.period = requireInRange(period, 0, Short.MAX_VALUE);
        this.effectType = requireNonNull(effectType);
        this.extendedEffectType = requireNonNull(extendedEffectType);
        this.effectArgumentX = requireInRange(effectArgumentX, 0, 15);
        this.effectArgumentY = requireInRange(effectArgumentY, 0, 15);
    }
}
