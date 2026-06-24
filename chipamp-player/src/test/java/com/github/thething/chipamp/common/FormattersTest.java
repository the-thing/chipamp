package com.github.thething.chipamp.common;

import com.github.thething.chipamp.io.Resources;
import com.github.thething.chipamp.mod.Mod;
import com.github.thething.chipamp.mod.ModLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class FormattersTest {

    private ModLoader modLoader;

    @BeforeEach
    void setUp() {
        modLoader = new ModLoader();
    }

    @Test
    void shouldReturnFormattedPatterns() throws IOException {
        String expected = Resources.readText("axel-patterns.txt");
        Mod mod = modLoader.load("DJ Metune - Axel F.mod");
        String formatted = Formatters.formatPatterns(mod);

        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void shouldReturnFormattedPattern() throws IOException {
        String expected = Resources.readText("axel-pattern-2.txt");
        Mod mod = modLoader.load("DJ Metune - Axel F.mod");
        String formatted = Formatters.formatPattern(mod, 2);

        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void shouldReturnFormattedRow() throws IOException {
        Mod mod = modLoader.load("DJ Metune - Axel F.mod");
        String formatted = Formatters.formatRow(mod, 5, 10);

        assertThat(formatted).isEqualTo("0005 | 10 | A-3 12 --- | F-3 05 A01 | --- -- --- | --- -- EB1 |");
    }

    @Test
    void shouldFormatEmptyModule() throws IOException {
        String expected = Resources.readText("empty-patterns.txt");
        Mod mod = modLoader.load("chip/empty.mod");
        String formatted = Formatters.formatPatterns(mod);

        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void shouldReturnFormattedHexByte() {
        assertThat(Formatters.formatHexByte(0)).isEqualTo("00");
        assertThat(Formatters.formatHexByte(1)).isEqualTo("01");
        assertThat(Formatters.formatHexByte(15)).isEqualTo("0F");
        assertThat(Formatters.formatHexByte(255)).isEqualTo("FF");

        assertThat(Formatters.formatHexByte(0, 0)).isEqualTo("00");
        assertThat(Formatters.formatHexByte(0, 15)).isEqualTo("0F");
        assertThat(Formatters.formatHexByte(15, 0)).isEqualTo("F0");
        assertThat(Formatters.formatHexByte(15, 15)).isEqualTo("FF");
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