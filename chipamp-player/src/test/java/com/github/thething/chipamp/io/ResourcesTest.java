package com.github.thething.chipamp.io;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
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
        assertThat(files.length).isEqualTo(17);
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
        assertThat(bytes[0]).isEqualTo((byte) 48);
        assertThat(bytes[95679]).isEqualTo((byte) 10);
    }

    @Test
    void shouldReturnText() throws IOException {
        String text = Resources.readText("axel-patterns.txt");

        assertThat(text.length()).isEqualTo(95680);
        assertThat(text).startsWith("0000 | 00 | A-3 12 --- | F-3 08 A03 | --- -- --- | A-3 0A C08 |");
    }

    @Test
    void shouldGetAudioData() throws UnsupportedAudioFileException, IOException {
        byte[] audio = Resources.readAudio("wav/axel-stereo-48kHz-pal.wav");

        assertThat(audio.length).isEqualTo(35_337_984);
    }

    @Test
    void shouldSaveAudio() throws IOException {
        byte[] audio = new byte[]{1, 2, 3, 4};
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44_000, 8, 2, 4, 44_000, false);

        Resources.saveAudio("test1.wav", format, audio);
        Resources.saveAudio("test2.wav", format, audio, 0, audio.length);
    }
}