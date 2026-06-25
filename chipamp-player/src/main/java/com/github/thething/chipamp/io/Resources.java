package com.github.thething.chipamp.io;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.util.Objects.checkFromIndexSize;

public final class Resources {

    private static final int DEFAULT_BUFFER_LENGTH = 4096;
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private Resources() {
    }

    public static InputStream getResourceAsStream(String name) throws IOException {
        InputStream in = Resources.class.getClassLoader().getResourceAsStream(name);

        if (in == null) {
            throw new IOException("Unable to find resource: " + name);
        }

        return in;
    }

    public static byte[] readBytes(String name) throws IOException {
        try (InputStream in = getResourceAsStream(name)) {
            return readBytes(in);
        }
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_LENGTH];
        int offset = 0;
        int readCount;

        while ((readCount = in.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += readCount;

            if (offset == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length << 1);
            }
        }

        return Arrays.copyOf(buffer, offset);
    }

    public static String readText(String name) throws IOException {
        return readText(name, DEFAULT_CHARSET);
    }

    public static String readText(String name, Charset charset) throws IOException {
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[DEFAULT_BUFFER_LENGTH];

        try (Reader reader = new InputStreamReader(getResourceAsStream(name), charset)) {
            int readCount;

            while ((readCount = reader.read(buffer)) != -1) {
                text.append(buffer, 0, readCount);
            }
        }

        return text.toString();
    }

    public static void saveAudio(File file, AudioFormat format, byte[] audio) throws IOException {
        saveAudio(file, format, audio, 0, audio.length);
    }

    public static void saveAudio(File file, AudioFormat format, byte[] audio, int offset, int length) throws IOException {
        checkFromIndexSize(offset, length, audio.length);

        try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(audio, offset, length), format, length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, file);
        }
    }
}
