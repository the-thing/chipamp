package com.github.thething.chipgroove.mod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        Mod mod = underTest.load("empty.mod");
        assertThat(mod.getTitle()).isEqualTo("");
        assertThat(mod.getLength()).isEqualTo(1);
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M.K.");
        assertThat(mod.getPatternCount()).isEqualTo(1);
    }
}