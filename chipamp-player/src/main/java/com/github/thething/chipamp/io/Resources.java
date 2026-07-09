package com.github.thething.chipamp.io;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;

import static java.util.Objects.checkFromIndexSize;

/**
 * Utility class for loading and saving resources from the classpath.
 */
public final class Resources {

    private static final int DEFAULT_BUFFER_LENGTH = 4096;
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private Resources() {
    }

    /**
     * Opens an input stream for the specified classpath resource.
     *
     * @param resourceName the name of the resource to load
     * @return an input stream for reading the resource
     * @throws IOException if the resource is not found
     */
    public static InputStream getResourceAsStream(String resourceName) throws IOException {
        InputStream in = Resources.class.getClassLoader().getResourceAsStream(resourceName);

        if (in == null) {
            throw new IOException("Resource not found: " + resourceName);
        }

        return in;
    }

    public static URL getResource(String resourceName) throws IOException {
        URL resource = Resources.class.getClassLoader().getResource(resourceName);

        if (resource == null) {
            throw new IOException("Resource not found: " + resourceName);
        }

        return resource;
    }

    /**
     * Gets the file name of the specified classpath resource.
     *
     * @param resourceName the name of the resource
     * @return the file name portion of the resource path
     * @throws IOException if the resource is not found or cannot be converted to a URI
     */
    public static String getFileName(String resourceName) throws IOException {
        URI uri;

        try {
            uri = getResource(resourceName).toURI();
        } catch (URISyntaxException e) {
            throw new IOException("Failed to convert resource to URI: " + resourceName, e);
        }

        return Paths.get(uri).getFileName().toString();
    }

    /**
     * Lists all files in the specified resource directory.
     *
     * @param resourceName the name of the directory resource
     * @return an array of files in the directory, or null if the directory is empty
     * @throws java.io.IOException if the resource is not found, does not exist, or is not a directory
     */
    public static File[] listFiles(String resourceName) throws IOException {
        File file = new File(getResource(resourceName).getFile());

        if (!file.isDirectory()) {
            throw new RuntimeException("Not a directory: " + file.getAbsolutePath());
        }

        return file.listFiles();
    }

    /**
     * Reads all bytes from the specified classpath resource.
     *
     * @param resourceName the name of the resource to read
     * @return a byte array containing all bytes from the resource
     * @throws IOException if an I/O error occurs or the resource is not found
     */
    public static byte[] readBytes(String resourceName) throws IOException {
        try (InputStream in = getResourceAsStream(resourceName)) {
            return readBytes(in);
        }
    }

    /**
     * Reads all bytes from the specified input stream.
     *
     * @param in the input stream to read from
     * @return a byte array containing all bytes from the stream
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Reads the entire contents of a text resource using UTF-8 encoding.
     *
     * @param name the name of the text resource to read
     * @return the contents of the resource as a string
     * @throws IOException if an I/O error occurs or the resource is not found
     */
    public static String readText(String name) throws IOException {
        return readText(name, DEFAULT_CHARSET);
    }

    /**
     * Reads the entire contents of a text resource using the specified character encoding.
     *
     * @param name    the name of the text resource to read
     * @param charset the character encoding to use
     * @return the contents of the resource as a string
     * @throws IOException if an I/O error occurs or the resource is not found
     */
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

    /**
     * Saves audio data to a WAV file.
     *
     * @param fileName the name of the file to create
     * @param format   the audio format of the audio data
     * @param audio    the audio data to save
     * @throws IOException if an I/O error occurs while writing the file
     */
    public static void saveAudio(String fileName, AudioFormat format, byte[] audio) throws IOException {
        saveAudio(new File(fileName), format, audio, 0, audio.length);
    }

    /**
     * Saves audio data to a WAV file.
     *
     * @param file   the file to create
     * @param format the audio format of the audio data
     * @param audio  the audio data to save
     * @throws IOException if an I/O error occurs while writing the file
     */
    public static void saveAudio(File file, AudioFormat format, byte[] audio) throws IOException {
        saveAudio(file, format, audio, 0, audio.length);
    }

    /**
     * Saves a portion of audio data to a WAV file.
     *
     * @param file   the file to create
     * @param format the audio format of the audio data
     * @param audio  the audio data array
     * @param offset the starting position in the audio array
     * @param length the number of bytes to write
     * @throws IOException               if an I/O error occurs while writing the file
     * @throws IndexOutOfBoundsException if offset and length are invalid for the audio array
     */
    public static void saveAudio(File file, AudioFormat format, byte[] audio, int offset, int length) throws IOException {
        checkFromIndexSize(offset, length, audio.length);

        try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(audio, offset, length), format, length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, file);
        }
    }

    /**
     * Reads audio data from a classpath resource.
     * <p>
     * The audio format is automatically detected by the Java Sound API.
     * </p>
     *
     * @param name the name of the audio resource to read
     * @return a byte array containing the audio data
     * @throws IOException                   if an I/O error occurs or the resource is not found
     * @throws UnsupportedAudioFileException if the audio file format is not supported
     */
    public static byte[] readAudio(String name) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(getResourceAsStream(name))) {
            return readBytes(in);
        }
    }
}
