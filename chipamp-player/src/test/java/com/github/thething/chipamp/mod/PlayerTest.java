package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.io.Resources;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayerTest {

    private Player underTest;
    private ModLoader modLoader;

    @BeforeEach
    void setUp() {
        underTest = new Player();
        modLoader = new ModLoader(true);
    }

    @Test
    public void shouldReadModWithLoopEffect() throws IOException {
        Mod mod = modLoader.load("chip/Jogeir Liljedahl - Nearly There.mod");
        underTest.setMod(mod);

        byte[] audio = underTest.read();
        assertThat(audio.length).isEqualTo(74_889_920);
    }

    @Test
    public void shouldReadModWithInfiniteLoop() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.setMod(mod);

        byte[] audio = underTest.read();
        assertThat(audio.length).isEqualTo(25_743_360);
    }

    @Test
    public void shouldRead() throws IOException {
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

        byte[] expectedAudio = Resources.readBytes("wav/axel-stereo-48kHz-pal.wav");
        byte[] audio = underTest.read();

        System.out.println(audio.length);
        System.out.println(expectedAudio.length);

         assertThat(audio).containsExactly(expectedAudio);

        // Resources.saveAudio(new File("axel-stereo-48kHz-pal.wav"), underTest.getCompatibleAudioFormat(), audio);
    }
}
