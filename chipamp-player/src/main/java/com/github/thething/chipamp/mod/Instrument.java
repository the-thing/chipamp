package com.github.thething.chipamp.mod;

import static com.github.thething.chipamp.common.Requirements.requireInRange;
import static java.util.Objects.requireNonNull;

/**
 * Represents a single instrument (note) entry in a MOD tracker pattern.
 * <p>
 * Each instrument entry contains information about which sample to play, at what period (pitch), and what effect to
 * apply. This corresponds to one cell in a tracker pattern row.
 *
 * @param sampleNumber       the sample index (0-31) to play, or 0 for no sample
 * @param period             the period value determining the pitch (0 for no change)
 * @param effectType         the type of effect to apply
 * @param extendedEffectType the type of extended effect when effectType is EXTENDED
 * @param effectArgumentX    the high nibble (0-15) of the effect parameter
 * @param effectArgumentY    the low nibble (0-15) of the effect parameter
 */
public record Instrument(int sampleNumber, int period,
                         EffectType effectType, ExtendedEffectType extendedEffectType,
                         int effectArgumentX, int effectArgumentY) {

    public Instrument(
            int sampleNumber, int period,
            EffectType effectType, ExtendedEffectType extendedEffectType,
            int effectArgumentX, int effectArgumentY) {
        this.sampleNumber = requireInRange(sampleNumber, 0, 31);
        this.period = requireInRange(period, 0, Short.MAX_VALUE);
        this.effectType = requireNonNull(effectType);
        this.extendedEffectType = requireNonNull(extendedEffectType);
        this.effectArgumentX = requireInRange(effectArgumentX, 0, 15);
        this.effectArgumentY = requireInRange(effectArgumentY, 0, 15);
    }
}
