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

    // TODO get effects for a specific row
}
