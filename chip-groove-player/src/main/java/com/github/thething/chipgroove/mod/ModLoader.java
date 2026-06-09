package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.common.ExtraArrays;
import com.github.thething.chipgroove.io.Resources;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ModLoader {

    private static final int SAMPLE_COUNT = 31;
    private static final int PATTERN_COUNT = 128;
    private static final int ROW_COUNT = 64;
    private static final int CHANNEL_COUNT = 4;

    public static Mod load(String name) throws IOException {
        try (DataInputStream in = new DataInputStream(Resources.getResourceAsStream(name))) {
            return load((DataInput) in);
        }
    }

    public static Mod load(InputStream in) throws IOException {
        return load((DataInput) new DataInputStream(in));
    }

    public static Mod load(DataInput in) throws IOException {
        String title = loadTitle(in);
        SampleHeader[] sampleHeaders = loadSampleHeaders(in);

        // skip 2 unused bytes
        // 1 - song length in patterns (0-80h)
        // 2 - restart byte for song looping (NoiseTracker)
        in.skipBytes(2);

        int[] patternSequences = loadPatternSequences(in);
        String trackerId = loadTrackerId(in);

        int patternCount = ExtraArrays.max(patternSequences) + 1;

        Pattern[][][] patterns = loadPatterns(in, patternCount);
        Sample[] samples = loadSamples(in, sampleHeaders);

        return new Mod(title, samples, patternSequences, trackerId, patterns, patternCount, ROW_COUNT, CHANNEL_COUNT);
    }

    private static String loadTitle(DataInput in) throws IOException {
        byte[] title = new byte[20];
        in.readFully(title, 0, 20);

        int index = ExtraArrays.indexOf(title, (byte) 0);

        if (index == -1) {
            return new String(title, StandardCharsets.US_ASCII);
        } else {
            return new String(title, 0, index, StandardCharsets.US_ASCII);
        }
    }

    private static int[] loadPatternSequences(DataInput in) throws IOException {
        byte[] bytes = new byte[PATTERN_COUNT];
        in.readFully(bytes);

        int[] patternSequences = new int[PATTERN_COUNT];

        for (int i = 0; i < bytes.length; i++) {
            patternSequences[i] = Byte.toUnsignedInt(bytes[i]);
        }

        return patternSequences;
    }

    private static String loadTrackerId(DataInput in) throws IOException {
        byte[] trackerData = new byte[4];
        in.readFully(trackerData, 0, 4);
        return new String(trackerData, StandardCharsets.US_ASCII);
    }

    private static Sample[] loadSamples(DataInput in, SampleHeader[] sampleHeaders) throws IOException {
        Sample[] samples = new Sample[sampleHeaders.length];

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            byte[] sampleData = new byte[sampleHeaders[i].length()];
            in.readFully(sampleData);
        }

        return samples;
    }

    private static SampleHeader[] loadSampleHeaders(DataInput in) throws IOException {
        SampleHeader[] sampleHeaders = new SampleHeader[SAMPLE_COUNT];

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            sampleHeaders[i] = loadSampleHeader(in);
        }

        return sampleHeaders;
    }

    private static SampleHeader loadSampleHeader(DataInput in) throws IOException {
        String name = loadSampleName(in);

        // sample length is double
        int length = in.readUnsignedShort() << 1;
        int finetune = in.readByte();
        int volume = in.readUnsignedByte();

        int loopStart = in.readUnsignedShort() << 1;
        int loopLength = in.readUnsignedShort() << 1;

        return new SampleHeader(name, length, finetune, volume, loopStart, loopLength);
    }

    private static String loadSampleName(DataInput in) throws IOException {
        byte[] nameArray = new byte[22];
        in.readFully(nameArray, 0, nameArray.length);

        int index = ExtraArrays.indexOf(nameArray, (byte) 0);

        if (index == -1) {
            return new String(nameArray, StandardCharsets.US_ASCII);
        } else {
            return new String(nameArray, 0, index, StandardCharsets.US_ASCII);
        }
    }

    private static Pattern[][][] loadPatterns(DataInput in, int patternCount) throws IOException {
        Pattern[][][] patterns = new Pattern[patternCount][ROW_COUNT][CHANNEL_COUNT];

        for (int i = 0; i < patternCount; i++) {
            for (int j = 0; j < ROW_COUNT; j++) {
                for (int k = 0; k < CHANNEL_COUNT; k++) {
                    patterns[i][j][k] = loadPattern(in);
                }
            }
        }

        return patterns;
    }

    public static Pattern loadPattern(DataInput in) throws IOException {
        int b0 = in.readUnsignedByte();
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();

        int sample = (b0 & 0xF0) | (b2 >> 4);
        int pitch = ((b0 & 0x0F) << 8) | b1;
        int effect = b2 & 0x0F;
        int effectArgument = b3 & 0xFF;

        return new Pattern(sample, pitch, effect, effectArgument);
    }

    private record SampleHeader(String name, int length, int finetune, int volume, int loopStart, int loopLength) {
    }
}
