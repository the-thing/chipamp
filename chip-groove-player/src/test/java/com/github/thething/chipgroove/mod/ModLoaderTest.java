package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.io.Resources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ModLoaderTest {

    @Test
    void shouldLoadModFile() throws IOException {
        Mod mod;

        try (InputStream in = Resources.getResourceAsStream("DJ Metune - Axel F.mod")) {
            mod = ModLoader.load(in);
        }

        assertThat(mod.getTitle()).isEqualTo("axel f - dj metune");
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M.K.");
        assertThat(mod.getPatternCount()).isEqualTo(23);

        try (InputStream in = Resources.getResourceAsStream("Hoffman - Eon.mod")) {
            mod = ModLoader.load(in);
        }

        assertThat(mod.getTitle()).isEqualTo("eon");
        assertThat(mod.getSampleCount()).isEqualTo(31);
        assertThat(mod.getPatternSequenceCount()).isEqualTo(128);
        assertThat(mod.getTrackerId()).isEqualTo("M!K!");
        assertThat(mod.getPatternCount()).isEqualTo(77);
    }
}