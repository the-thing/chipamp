package com.github.thething.chipgroove.mod;

import java.util.Arrays;

public enum Effect {

    ARPEGGIO(0x00),
    SLIDE_UP(0x01),
    SLIDE_DOWN(0x02),
    TONE_PORTAMENTO(0x03),
    VIBRATO(0x04),
    TONE_PORTAMENTO_WITH_VOLUME_SLIDE(0x05),
    VIBRATO_WITH_VOLUME_SLIDE(0x06),
    TREMOLO(0x07),
    SET_PANNING_POSITION(0x08),
    SET_SAMPLE_OFFSET(0x09),
    VOLUME_SLIDE(0x0A),
    POSITION_JUMP(0x0B),
    SET_VOLUME(0x0C),
    PATTERN_BREAK(0x0D),
    EXTENDED_EFFECT(0x0E),
    SET_SPEED(0x0F),
    NONE(0xFF);

    private static final Effect[] EFFECT_BY_CODE;

    static {
        EFFECT_BY_CODE = new Effect[256];
        Arrays.fill(EFFECT_BY_CODE, 16, EFFECT_BY_CODE.length, NONE);
        System.arraycopy(values(), 0, EFFECT_BY_CODE, 0, values().length);
    }

    private final int code;

    Effect(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static Effect valueOf(int code) {
        return EFFECT_BY_CODE[code];
    }
}
