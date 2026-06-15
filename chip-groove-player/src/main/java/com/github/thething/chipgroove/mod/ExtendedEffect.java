package com.github.thething.chipgroove.mod;

import java.util.Arrays;

public enum ExtendedEffect {

    SET_FILTER(0x00),
    FINE_SLIDE_UP(0x01),
    FINE_SLIDE_DOWN(0x02),
    SET_GLISSANDO(0x03),
    SET_VIBRATO_WAVEFORM(0x04),
    SET_FINE_TUNE_VALUE(0x05),
    LOOP_PATTERN(0x06),
    SET_TREMOLO_WAVEFORM(0x07),
    ROUGH_PANNING(0x08),
    RETRIGGER_SAMPLE(0x09),
    FINE_VOLUME_SLIDE_UP(0x0A),
    FINE_VOLUME_SLIDE_DOWN(0x0B),
    CUT_SAMPLE(0x0C),
    DELAY_SAMPLE(0x0D),
    DELAY_PATTERN(0x0E),
    INVERT_LOOP(0x0F),
    NONE(0xFF);

    private static final ExtendedEffect[] EFFECT_BY_CODE;

    static {
        EFFECT_BY_CODE = new ExtendedEffect[256];
        Arrays.fill(EFFECT_BY_CODE, 16, EFFECT_BY_CODE.length, NONE);
        System.arraycopy(values(), 0, EFFECT_BY_CODE, 0, values().length);
    }

    private final int code;

    ExtendedEffect(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ExtendedEffect valueOf(int code) {
        return EFFECT_BY_CODE[code];
    }
}
