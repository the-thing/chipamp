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
        underTest = new ModLoader();
    }

    @Test
    void shouldLoadModFile() throws IOException {
        Mod mod;

        mod = underTest.load("DJ Metune - Axel F.mod");
        assertThat(mod.getTitle()).isEqualTo("axel f - dj metune");
        assertThat(mod.getLength()).isEqualTo(24);
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M.K.");
        assertThat(mod.getPatternCount()).isEqualTo(23);

        mod = underTest.load("H0ffman - Eon.mod");
        assertThat(mod.getTitle()).isEqualTo("eon");
        assertThat(mod.getLength()).isEqualTo(92);
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M!K!");
        assertThat(mod.getPatternCount()).isEqualTo(77);

        mod = underTest.load("Captain - Space Debris.mod");
        assertThat(mod.getTitle()).isEqualTo("space_debris");
        assertThat(mod.getLength()).isEqualTo(42);
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M.K.");
        assertThat(mod.getPatternCount()).isEqualTo(41);
    }

    @Test
    void shouldLoadEmptyAmigaModule() throws IOException {
        Mod mod = underTest.load("chip/empty.mod");
        assertThat(mod.getTitle()).isEqualTo("");
        assertThat(mod.getLength()).isEqualTo(1);
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M.K.");
        assertThat(mod. getPatternCount()).isEqualTo(1);
    }

    @Test
    void test() throws IOException {
        Mod mod = underTest.load("chip/broken2/katherina_part1.mod");
    }

    // TODO find mods that have sample length == 2

    @Test
    void shouldLoadBrokenModules() throws IOException {
        for (File file : Resources.listFiles("chip/broken")) {

            if (!file.getName().endsWith(".mod")) {
                continue;
            }

            System.out.println("Loading MOD: " + file.getName() + ", file size = " + file.length());
            Mod mod = underTest.load(file);

            for (int i = 0; i < mod.getSampleCount(); i++) {
                Sample sample = mod.getSample(i);

                if (sample.loopStart() != 0) {
                    System.out.println(sample.name() + " = " + sample.loopStart());
                }
            }
        }
    }

    @Test
    void shouldLoadAllModules() throws IOException {
        for (File file : new File("C:\\Users\\Marcin\\Downloads\\dupa").listFiles()) {
            if (!file.getName().endsWith(".mod")) {
                continue;
            }

            System.out.println("Loading MOD: " + file.getName() + ", file size = " + file.length());
            Mod mod = underTest.load(file);

            for (int i = 0; i < mod.getSampleCount(); i++) {
                Sample sample = mod.getSample(i);

                if (sample.loopStart() != 0) {
                    System.out.println(sample.name() + " = " + sample.loopStart());
                }
            }
        }
    }

    // TODO how many modules are compressed by PP20
}