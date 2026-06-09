package com.github.thething.chipgroove.common;

import com.github.thething.chipgroove.io.Resources;
import com.github.thething.chipgroove.mod.Mod;
import com.github.thething.chipgroove.mod.ModLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

class FormattersTest {

    @Test
    void shouldReturnFormattedPatterns() throws IOException {
        String expected = Resources.readText("DJ Metune - Axel F.txt");
        Mod mod = ModLoader.load("DJ Metune - Axel F.mod");
        String formatted = Formatters.formatPatterns(mod);

        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void shouldReturnFormattedHexByte() {
        assertThat(Formatters.formatHexByte(0)).isEqualTo("00");
        assertThat(Formatters.formatHexByte(1)).isEqualTo("01");
        assertThat(Formatters.formatHexByte(15)).isEqualTo("0F");
        assertThat(Formatters.formatHexByte(255)).isEqualTo("FF");
    }

    @Test
    void shouldReturnFormattedHexInt() {
        assertThat(Formatters.formatHexInt(0)).isEqualTo("00000000");
        assertThat(Formatters.formatHexInt(1)).isEqualTo("00000001");
        assertThat(Formatters.formatHexInt(15)).isEqualTo("0000000F");
        assertThat(Formatters.formatHexInt(255)).isEqualTo("000000FF");
        assertThat(Formatters.formatHexInt(Integer.MIN_VALUE)).isEqualTo("80000000");
        assertThat(Formatters.formatHexInt(Integer.MAX_VALUE)).isEqualTo("7FFFFFFF");
    }
}