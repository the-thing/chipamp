package com.github.thething.chipamp.io;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourcesTest {

    @Test
    void shouldReturnInputStream() throws IOException {
        assertThat(Resources.getResourceAsStream("chip/DJ Metune - Axel F.mod")).isNotNull();
    }

    @SuppressWarnings("resource")
    @Test
    void shouldThrowExceptionWhenResourceIsNotFound() {
        assertThatThrownBy(() -> Resources.getResourceAsStream("chip/non-existent.mod"))
                .isInstanceOf(IOException.class)
                .hasNoCause()
                .hasMessage("Resource not found: chip/non-existent.mod");
    }

    @Test
    void shouldReturnFileName() throws IOException {
        assertThat(Resources.getFileName("chip/DJ Metune - Axel F.mod")).isEqualTo("DJ Metune - Axel F.mod");
    }

    @Test
    void shouldThrowExceptionWhenFileIsNotFound() {
        assertThatThrownBy(() -> Resources.getFileName("chip/non-existent.mod"))
                .isInstanceOf(IOException.class)
                .hasNoCause()
                .hasMessage("Resource not found: chip/non-existent.mod");
    }

    @Test
    void shouldReturnFilesFromDirectory() throws IOException {
        File[] files = Resources.listFiles("chip/other");
        assertThat(files.length).isEqualTo(15);
    }

    @Test
    void shouldThrowExceptionWhenDirectoryIsNotFound() {
        assertThatThrownBy(() -> Resources.listFiles("chip/non-existent"))
                .isInstanceOf(IOException.class)
                .hasNoCause()
                .hasMessage("Resource not found: chip/non-existent");
    }

    @Test
    void shouldThrowExceptionWhenIsNotDirectory() {
        assertThatThrownBy(() -> Resources.listFiles("axel-patterns.txt"))
                .isInstanceOf(RuntimeException.class)
                .hasNoCause()
                .hasMessageContaining("Not a directory: ");
    }

    @Test
    void shouldReturnAllBytes() throws IOException {
        byte[] bytes = Resources.readBytes("axel-patterns.txt");

        assertThat(bytes.length).isEqualTo(95680);
        assertThat(bytes[0]).isEqualTo((byte)48);
        assertThat(bytes[95679]).isEqualTo(0x00);
    }
}