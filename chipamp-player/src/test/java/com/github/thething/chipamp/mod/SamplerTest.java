package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.io.Resources;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class SamplerTest {

    private Sampler underTest;
    private ModLoader modLoader;

    @BeforeEach
    void setUp() {
        underTest = new Sampler();
        modLoader = new ModLoader(true);
    }

    @Test
    void shouldReadModWithLoopEffect() throws IOException {
        Mod mod = modLoader.load("chip/Jogeir Liljedahl - Nearly There.mod");
        underTest.setMod(mod);

        byte[] audio = underTest.read();
        assertThat(audio.length).isEqualTo(74_889_920);
    }

    @Test
    void shouldReadModWithInfiniteLoop() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.setMod(mod);

        byte[] audio = underTest.read();
        assertThat(audio.length).isEqualTo(25_743_360);
    }

    @Test
    void shouldGenerateAudioFile() throws IOException, UnsupportedAudioFileException {
        underTest.setClockHz(Mods.PAL_CLOCK_HZ);
        underTest.setSamplingRate(48_000);
        underTest.setMinPeriod(Mods.MIN_PERIOD);
        underTest.setMaxPeriod(Mods.MAX_PERIOD);
        underTest.setVolumeMultiplier(0.5f);
        underTest.setStereoEnabled(true);
        underTest.setStereoFoldDownEnabled(true);
        underTest.setVolumeSlideDeltaEnabled(false);
        underTest.setLoopDetectionEnabled(true);
        underTest.setLoggingEnabled(false);

        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.setMod(mod);

        byte[] expectedAudio = Resources.readAudio("wav/axel-stereo-48kHz-pal.wav");
        byte[] audio = underTest.read();

        assertThat(audio).containsExactly(expectedAudio);
    }

    @Test
    void shouldGenerateSinglePattern() throws IOException, UnsupportedAudioFileException {
        underTest.setClockHz(Mods.PAL_CLOCK_HZ);
        underTest.setSamplingRate(48_000);
        underTest.setMinPeriod(Mods.MIN_PERIOD);
        underTest.setMaxPeriod(Mods.MAX_PERIOD);
        underTest.setVolumeMultiplier(0.5f);
        underTest.setStereoEnabled(true);
        underTest.setStereoFoldDownEnabled(true);
        underTest.setVolumeSlideDeltaEnabled(false);
        underTest.setLoopDetectionEnabled(true);
        underTest.setLoggingEnabled(false);

        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.setMod(mod);

        int sequenceIndex = underTest.setPattern(1);
        assertThat(sequenceIndex).isEqualTo(4);

        byte[] expectedAudio = Resources.readAudio("wav/axel-stereo-48kHz-pal-pattern-1.wav");
        byte[] audio = underTest.readPatterns(1);

        assertThat(audio).containsExactly(expectedAudio);
    }

    @Test
    void shouldReturnSongLengthInMillis() throws IOException {
        underTest.setClockHz(Mods.PAL_CLOCK_HZ);
        underTest.setSamplingRate(48_000);
        underTest.setMinPeriod(Mods.MIN_PERIOD);
        underTest.setMaxPeriod(Mods.MAX_PERIOD);
        underTest.setVolumeMultiplier(0.5f);
        underTest.setStereoEnabled(true);
        underTest.setStereoFoldDownEnabled(true);
        underTest.setVolumeSlideDeltaEnabled(false);
        underTest.setLoopDetectionEnabled(true);
        underTest.setLoggingEnabled(false);

        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.setMod(mod);

        assertThat(underTest.getSampleCount()).isEqualTo(8_834_496);
        assertThat(underTest.getModLength(TimeUnit.SECONDS)).isEqualTo(184L);
        assertThat(underTest.getModLength(TimeUnit.MILLISECONDS)).isEqualTo(184_052L);
    }
}
