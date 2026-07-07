package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ModLoaderTest {

    private ModLoader underTest;

    @BeforeEach
    void setUp() {
        underTest = new ModLoader(true);
    }

    @Test
    void shouldLoadModFiles() throws IOException {
        Mod mod;

        mod = underTest.load("chip/DJ Metune - Axel F.mod");
        assertThat(mod.getTitle()).isEqualTo("axel f - dj metune");
        assertThat(mod.getLength()).isEqualTo(24);
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M.K.");
        assertThat(mod.getPatternCount()).isEqualTo(23);

        mod = underTest.load("chip/H0ffman - Eon.mod");
        assertThat(mod.getTitle()).isEqualTo("eon");
        assertThat(mod.getLength()).isEqualTo(92);
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M!K!");
        assertThat(mod.getPatternCount()).isEqualTo(77);

        mod = underTest.load("chip/Captain - Space Debris.mod");
        assertThat(mod.getTitle()).isEqualTo("space_debris");
        assertThat(mod.getLength()).isEqualTo(42);
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M.K.");
        assertThat(mod.getPatternCount()).isEqualTo(41);
    }

    @Test
    void shouldLoadEmptyMod() throws IOException {
        Mod mod = underTest.load("chip/effect/empty.mod");
        assertThat(mod.getTitle()).isEqualTo("");
        assertThat(mod.getLength()).isEqualTo(1);
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M.K.");
        assertThat(mod.getPatternCount()).isEqualTo(1);
    }

    @Test
    public void shouldLoadAllModules() throws IOException {
        for (File file : Resources.listFiles("chip")) {
            if (!file.getName().endsWith(".mod")) {
                continue;
            }

            Mod mod = underTest.load(file);
            assertThat(mod).isNotNull();
        }
    }

    @Test
    void shouldLoadBrokenModules() throws IOException {
        for (File file : Resources.listFiles("chip/broken")) {
            if (!file.getName().endsWith(".mod")) {
                continue;
            }

            System.out.println("Loading module: " + file.getName());
            Mod mod = underTest.load(file);
            assertThat(mod).isNotNull();
        }
    }

    @Test
    public void shouldLoadModWithShortenedSample() throws IOException {
        Mod mod = underTest.load("chip/effect/elmstreet.mod");
        assertThat(mod.getChannelCount()).isEqualTo(4);
        assertThat(mod.getSampleCount()).isEqualTo(31);

        Sample sample = mod.getSample(22);
        assertThat(sample.getName()).isEqualTo("st-06:fred9");
        assertThat(sample.getDataLength()).isEqualTo(15_094);
        assertThat(sample.getLoopStart()).isEqualTo(0);
        assertThat(sample.getLoopEnd()).isEqualTo(0);
        assertThat(sample.getLoopLength()).isEqualTo(0);
        assertThat(sample.getVolume()).isEqualTo(64);
    }

    @Test
    public void shouldTrimSampleLengthOfTwoToZero() throws IOException {
        Mod mod = underTest.load("chip/Enjoy the Happiness.mod");
        assertThat(mod.getChannelCount()).isEqualTo(4);
        assertThat(mod.getSampleCount()).isEqualTo(31);

        Sample sample = mod.getSample(10);
        assertThat(sample.getName()).isEqualTo("PO33 2RR, ENGLAND.");
        assertThat(sample.getDataLength()).isEqualTo(0);
        assertThat(sample.getLoopStart()).isEqualTo(0);
        assertThat(sample.getLoopEnd()).isEqualTo(0);
        assertThat(sample.getLoopLength()).isEqualTo(0);
        assertThat(sample.getVolume()).isEqualTo(0);
    }

    @Test
    public void shouldTrimLoopLengthOfTwoToZero() throws IOException {
        Mod mod = underTest.load("chip/effect/eisenzeit.mod");
        assertThat(mod.getChannelCount()).isEqualTo(4);
        assertThat(mod.getSampleCount()).isEqualTo(31);

        Sample sample = mod.getSample(0);
        assertThat(sample.getName()).isEqualTo(".(c).1995.r^c^p.!....");
        assertThat(sample.getDataLength()).isEqualTo(8_588);
        assertThat(sample.getLoopStart()).isEqualTo(0);
        assertThat(sample.getLoopEnd()).isEqualTo(0);
        assertThat(sample.getLoopLength()).isEqualTo(0);
        assertThat(sample.getVolume()).isEqualTo(64);
    }

    @Test
    public void shouldLoadShorterSample() throws IOException {
        Mod mod = underTest.load("chip/broken/janes.mod");
        assertThat(mod.getChannelCount()).isEqualTo(4);
        assertThat(mod.getSampleCount()).isEqualTo(31);

        Sample sample = mod.getSample(14);
        assertThat(sample.getName()).isEqualTo("st-01:FunBass");
        assertThat(sample.getDataLength()).isEqualTo(6500);
        assertThat(sample.getLoopStart()).isEqualTo(0);
        assertThat(sample.getLoopEnd()).isEqualTo(0);
        assertThat(sample.getLoopLength()).isEqualTo(0);
        assertThat(sample.getVolume()).isEqualTo(64);
    }

    @Test
    void shouldLoadModWith16Channels() throws IOException {
        Mod mod = underTest.load("chip/effect/di_-_demo.mod");
        assertThat(mod.getChannelCount()).isEqualTo(16);
        assertThat(mod.getSampleCount()).isEqualTo(31);
    }
}