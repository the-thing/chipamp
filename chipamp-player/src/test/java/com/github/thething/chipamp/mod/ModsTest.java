package com.github.thething.chipamp.mod;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModsTest {

    @Test
    void shouldReturnNoteForPeriod() {
        assertThat(Mods.getNote(720)).isEqualTo("D#1");
        assertThat(Mods.getNote(1209)).isEqualTo("F#0");
    }

    @Test
    void shouldReturnNullWhenNoteIsNotFound() {
        assertThat(Mods.getNote(1234)).isNull();
        assertThat(Mods.getNote(10_000)).isNull();
    }

    @Test
    void shouldReturnCustomNoteForPeriod() {
        assertThat(Mods.getCustomNote(720)).isEqualTo("D-X");
        assertThat(Mods.getCustomNote(1209)).isEqualTo("F-X");
    }

    @Test
    void shouldReturnNearestFineTunePeriod() {
        assertThat(Mods.getFineTunePeriod(844, -6, true)).isEqualTo(844);
        assertThat(Mods.getFineTunePeriod(844, -6, true)).isEqualTo(844);
        assertThat(Mods.getFineTunePeriod(855, -6, true)).isEqualTo(844);
        assertThat(Mods.getFineTunePeriod(795, -6, true)).isEqualTo(796);
        assertThat(Mods.getFineTunePeriod(720, 5, true)).isEqualTo(694);
    }

    @Test
    void shouldReturnCustomFineTunePeriod() {
        assertThat(Mods.getFineTunePeriod(844, -6, false)).isEqualTo(881);
        assertThat(Mods.getCustomFineTunePeriod(844, -6)).isEqualTo(881);
        assertThat(Mods.getCustomFineTunePeriod(844, 0)).isEqualTo(844);
    }

    @Test
    void shouldReturnPeriodIndexWhenFound() {
        assertThat(Mods.getPeriodIndex(226)).isEqualTo(23);
        assertThat(Mods.getPeriodIndex(214)).isEqualTo(24);
    }

    @Test
    void shouldReturnMinusOneWhenPeriodIndexIsNotFound() {
        assertThat(Mods.getPeriodIndex(227)).isEqualTo(-1);
    }

    @Test
    void shouldReturnShiftedPeriod() {
        assertThat(Mods.shiftUpPeriodBySemitones(881, -4, 10)).isEqualTo(494);
        assertThat(Mods.shiftUpPeriodBySemitones(124, -4, 1)).isEqualTo(117);
        assertThat(Mods.shiftUpPeriodBySemitones(124, -4, 2)).isEqualTo(117);
        assertThat(Mods.shiftUpPeriodBySemitones(124, -4, 0)).isEqualTo(124);
    }

    @Test
    void shouldReturnNearestNote() {
        assertThat(Mods.findNearestNote(359)).isEqualTo("D#2");
        assertThat(Mods.findNearestNote(360)).isEqualTo("D#2");
        assertThat(Mods.findNearestNote(361)).isEqualTo("D#2");
        assertThat(Mods.findNearestNote(2_000)).isEqualTo("C-0");
        assertThat(Mods.findNearestNote(1713)).isEqualTo("C-0");
        assertThat(Mods.findNearestNote(1712)).isEqualTo("C-0");
        assertThat(Mods.findNearestNote(58)).isEqualTo("B-4");
    }

    @Test
    void shouldReturnWaveformValue() {
        assertThat(Mods.getWaveformValue(WaveformType.SAWTOOTH, 20)).isEqualTo(95);
        assertThat(Mods.getWaveformValue(WaveformType.SAWTOOTH, 100)).isEqualTo(-223);
        assertThat(Mods.getWaveformValue(WaveformType.SINE, 20)).isEqualTo(235);
        assertThat(Mods.getWaveformValue(WaveformType.SINE, 100)).isEqualTo(-97);
        assertThat(Mods.getWaveformValue(WaveformType.SQUARE, 20)).isEqualTo(255);
        assertThat(Mods.getWaveformValue(WaveformType.SQUARE, 100)).isEqualTo(-255);
    }

    @Test
    void shouldReturnUniqueEffects() throws IOException {
        ModLoader loader = new ModLoader();
        Mod mod = loader.load("chip/DJ Metune - Axel F.mod");

        Set<EffectType> uniqueEffects = Mods.getUniqueEffects(mod);
        assertThat(uniqueEffects).containsExactlyInAnyOrder(
                EffectType.SLIDE_UP,
                EffectType.SLIDE_DOWN,
                EffectType.TONE_PORTAMENTO,
                EffectType.VIBRATO,
                EffectType.TONE_PORTAMENTO_WITH_VOLUME_SLIDE,
                EffectType.VIBRATO_WITH_VOLUME_SLIDE,
                EffectType.TREMOLO,
                EffectType.SET_SAMPLE_OFFSET,
                EffectType.VOLUME_SLIDE,
                EffectType.SET_VOLUME,
                EffectType.PATTERN_BREAK,
                EffectType.SET_SPEED);
    }

    @Test
    void shouldReturnUniqueExtendedEffects() throws IOException {
        ModLoader loader = new ModLoader();
        Mod mod = loader.load("chip/DJ Metune - Axel F.mod");

        Set<ExtendedEffectType> uniqueEffects = Mods.getUniqueExtendedEffects(mod);
        assertThat(uniqueEffects).containsExactlyInAnyOrder(
                ExtendedEffectType.FINE_VOLUME_SLIDE_UP,
                ExtendedEffectType.FINE_VOLUME_SLIDE_DOWN
        );
    }
}