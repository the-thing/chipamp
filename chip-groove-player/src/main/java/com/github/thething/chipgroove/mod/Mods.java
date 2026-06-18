package com.github.thething.chipgroove.mod;

import java.util.EnumSet;
import java.util.Set;

public final class Mods {

    private Mods() {
    }

    public static Set<EffectType> getUniqueEffects(Mod mod) {
        EnumSet<EffectType> effectTypes = EnumSet.noneOf(EffectType.class);

        for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
            for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                    Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
                    EffectType effectType = instrument.effectType();

                    if (effectType != EffectType.NONE && effectType != EffectType.EXTENDED_EFFECT) {
                        effectTypes.add(effectType);
                    }
                }
            }
        }

        return effectTypes;
    }

    public static Set<ExtendedEffectType> getUniqueExtendedEffects(Mod mod) {
        EnumSet<ExtendedEffectType> extendedEffectTypes = EnumSet.noneOf(ExtendedEffectType.class);

        for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
            for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                    Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
                    ExtendedEffectType extendedEffectType = instrument.extendedEffectType();

                    if (extendedEffectType != ExtendedEffectType.NONE) {
                        extendedEffectTypes.add(extendedEffectType);
                    }
                }
            }
        }

        return extendedEffectTypes;
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
