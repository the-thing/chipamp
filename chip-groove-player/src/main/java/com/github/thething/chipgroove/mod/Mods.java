package com.github.thething.chipgroove.mod;

import java.util.EnumSet;
import java.util.Set;

public final class Mods {

    private Mods() {
    }

    public static Set<Effect> getUniqueEffects(Mod mod) {
        EnumSet<Effect> effects = EnumSet.noneOf(Effect.class);

        for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
            for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                    Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
                    Effect effect = instrument.effect();

                    if (effect != Effect.NONE && effect != Effect.EXTENDED_EFFECT) {
                        effects.add(effect);
                    }
                }
            }
        }

        return effects;
    }

    public static Set<ExtendedEffect> getUniqueExtendedEffects(Mod mod) {
        EnumSet<ExtendedEffect> extendedEffects = EnumSet.noneOf(ExtendedEffect.class);

        for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
            for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                    Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
                    ExtendedEffect extendedEffect = instrument.extendedEffect();

                    if (extendedEffect != ExtendedEffect.NONE) {
                        extendedEffects.add(extendedEffect);
                    }
                }
            }
        }

        return extendedEffects;
    }


    /**
     * Convert a period value to a playback frequency (Hz).
     * <p>
     * frequency = clock / period
     * <p>
     * Period 428 → middle C (C-3) = 8287 Hz on PAL. The mixer then re-samples this to whatever output rate you have
     * chosen.
     */
    public static double periodToHz(int period, double clock) {
        if (period <= 0) {
            return 0.0;
        }

        return clock / period;
    }
}
