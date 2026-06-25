package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.ExtraArrays;
import com.github.thething.chipamp.io.Resources;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public final class ModLoader {

    private static final int INSTRUMENT_LENGTH = 4;
    private static final int SAMPLE_HEADER_LENGTH = 30;
    private static final Set<String> DEFAULT_TRACKER_IDS = Set.of(
            "M.K.", "M!K!", "FLT4", "FLT8",
            "4CHN", "6CHN", "8CHN", "CD81",
            "OCTA");

    private final Set<String> knownTrackerIds;

    public ModLoader() {
        this(DEFAULT_TRACKER_IDS);
    }

    public ModLoader(Set<String> knownTrackerIds) {
        this.knownTrackerIds = requireNonNull(knownTrackerIds);
    }

    public Mod load(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] bytes = Resources.readBytes(in);
            return load(bytes);
        }
    }

    public Mod load(String name) throws IOException {
        return load(Resources.readBytes(name));
    }

    public Mod load(DataInputStream in) throws IOException {
        String title = loadTitle(in);
        SampleHeader[] sampleHeaders = loadSampleHeaders(in);

        int length = in.readUnsignedByte();

        // Historically set to 127, but can be safely ignored.
        // NoiseTracker uses this byte to indicate restart position.
        // This has been made redundant by the 'Position Jump' effect.
        in.skipBytes(1);

        int[] patternSequences = loadPatternSequences(in);
        String trackerId = loadTrackerId(in);
        int channelCount = detectChannelCount(trackerId);
        int patternCount = ExtraArrays.max(patternSequences) + 1;

        Instrument[][][] patterns = loadPatterns(in, patternCount, channelCount);
        Sample[] samples = loadSamples(in, sampleHeaders);

        return new Mod(title, length, samples, patternSequences, trackerId, patterns);
    }

    public Mod load(byte[] data) {
        int offset = 0;

        String title = loadTitle(data);
        offset += 20;

        int probeOffset = 20 + 31 * SAMPLE_HEADER_LENGTH + 2 + Mod.PATTERN_SEQUENCE_COUNT;
        String probeTrackerId = new String(data, probeOffset, 4, StandardCharsets.US_ASCII);
        int sampleCount;

        if (knownTrackerIds.contains(probeTrackerId)) {
            sampleCount = Mod.SAMPLE_COUNT_1;
        } else {
            sampleCount = Mod.SAMPLE_COUNT_2;
        }

        SampleHeader[] sampleHeaders = loadSampleHeaders(data, offset, sampleCount);
        offset += sampleHeaders.length * SAMPLE_HEADER_LENGTH;

        int length = data[offset] & 0xFF;
        offset++;

        // Historically set to 127, but can be safely ignored.
        // NoiseTracker uses this byte to indicate restart position.
        // This has been made redundant by the 'Position Jump' effect.
        offset++;

        int[] patternSequences = loadPatternSequences(data, offset);
        offset += patternSequences.length;

        String trackerId;

        if (sampleCount == Mod.SAMPLE_COUNT_1) {
            trackerId = new String(data, offset, 4, StandardCharsets.US_ASCII);
            offset += 4;
        } else {
            trackerId = new String("");
        }

        int patternCount = ExtraArrays.max(patternSequences) + 1;
        int channelCount = detectChannelCount(probeTrackerId);

        Instrument[][][] patterns = loadPatterns(data, offset, patternCount, channelCount);
        offset += patterns.length * Mod.ROW_COUNT * channelCount * INSTRUMENT_LENGTH;

        Sample[] samples = loadSamples(data, offset, sampleHeaders);

        for (int i = 0; i < samples.length; i++) {
            // TODO this is wrong
            offset += samples[i].actualLength();
        }

        if (offset != data.length) {
            throw new RuntimeException("Unexpected data length: " + offset + " != " + data.length);
        }

        return new Mod(title, length, samples, patternSequences, trackerId, patterns);
    }

    private static int detectChannelCount(String trackerId) {
        return switch (trackerId) {
            case "FLT8", "8CHN" -> 8;
            case "6CHN" -> 6;
            default -> 4;
        };
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

    private static String loadTitle(byte[] bytes) {
        int index = ExtraArrays.indexOf(bytes, 0, 20, (byte) 0);

        if (index == -1) {
            return new String(bytes, 0, 20, StandardCharsets.US_ASCII);
        } else {
            return new String(bytes, 0, index, StandardCharsets.US_ASCII);
        }
    }

    private static int[] loadPatternSequences(DataInput in) throws IOException {
        byte[] bytes = new byte[Mod.PATTERN_SEQUENCE_COUNT];
        in.readFully(bytes);

        int[] patternSequences = new int[Mod.PATTERN_SEQUENCE_COUNT];

        for (int i = 0; i < bytes.length; i++) {
            patternSequences[i] = Byte.toUnsignedInt(bytes[i]);
        }

        return patternSequences;
    }

    private static int[] loadPatternSequences(byte[] data, int offset) {
        int[] patternSequences = new int[Mod.PATTERN_SEQUENCE_COUNT];

        for (int i = 0; i < Mod.PATTERN_SEQUENCE_COUNT; i++) {
            patternSequences[i] = data[i + offset] & 0xFF;
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

        for (int i = 0; i < samples.length; i++) {
            byte[] sampleData = new byte[sampleHeaders[i].length()];
            in.readFully(sampleData);

            SampleHeader sampleHeader = sampleHeaders[i];
            samples[i] = new Sample(sampleHeader.name, sampleHeader.fineTune, sampleHeader.volume,
                    sampleHeader.loopStart, sampleHeader.loopLength, sampleData, sampleHeader.length);
        }

        return samples;
    }

    private static Sample[] loadSamples(byte[] data, int offset, SampleHeader[] sampleHeaders) {
        Sample[] samples = new Sample[sampleHeaders.length];

        System.out.println("data.length = " + data.length);

        for (int i = 0; i < samples.length; i++) {
            System.out.println("Loading sample, offset = " + offset + ", length = " + sampleHeaders[i].length() + ", name = " + sampleHeaders[i].name);

            int actualLength = Math.min(sampleHeaders[i].length(), data.length - offset);

            if (actualLength != sampleHeaders[i].length()) {
                System.out.println("actualLength = " + actualLength);
                System.out.println("Warning: sample length mismatch");
            }

            byte[] sampleData = new byte[sampleHeaders[i].length()];
            System.arraycopy(data, offset, sampleData, 0, actualLength);
            offset += actualLength;

            SampleHeader sampleHeader = sampleHeaders[i];
            samples[i] = new Sample(sampleHeader.name, sampleHeader.fineTune, sampleHeader.volume,
                    sampleHeader.loopStart, sampleHeader.loopLength, sampleData, actualLength);
        }

        return samples;
    }

    private static SampleHeader[] loadSampleHeaders(DataInput in) throws IOException {
        SampleHeader[] sampleHeaders = new SampleHeader[Mod.SAMPLE_COUNT_1];

        for (int i = 0; i < Mod.SAMPLE_COUNT_1; i++) {
            sampleHeaders[i] = loadSampleHeader(in);
        }

        return sampleHeaders;
    }

    private static SampleHeader loadSampleHeader(DataInput in) throws IOException {
        String name = loadSampleName(in);

        int length = in.readUnsignedShort() << 1; // in words - multiply by 2
        int fineTune = in.readByte();
        int volume = in.readUnsignedByte();

        // some mods store it as unsigned, so we have to do additional conversion: signed / unsigned -> signed -> unsigned
        fineTune = (fineTune << 28) >> 28;

        int loopStart = in.readUnsignedShort() << 1; // in words - multiply by 2
        int loopLength = in.readUnsignedShort() << 1; // in words - multiply by 2

        System.out.println("loopStart: " + loopStart + ", loopLength: " + loopLength);

        // there are some mods where loop length is larger than actual data length
        // currently we limit it to length
        if (loopStart + loopLength > length) {
            loopLength = length - loopStart;
        }

        return new SampleHeader(name, length, fineTune, volume, loopStart, loopLength);
    }

    private static SampleHeader[] loadSampleHeaders(byte[] data, int offset, int sampleCount) {
        SampleHeader[] sampleHeaders = new SampleHeader[sampleCount];

        for (int i = 0; i < sampleHeaders.length; i++) {
            sampleHeaders[i] = loadSampleheader(data, offset);
            offset += SAMPLE_HEADER_LENGTH;
        }

        return sampleHeaders;
    }

    private static SampleHeader loadSampleheader(byte[] data, int offset) {
        String name = loadSampleName(data, offset);
        offset += 22;

        int length = ExtraArrays.getBigEndianUnsignedShort(data, offset) << 1; // in words - multiply by 2
        offset += 2;

        int fineTune = data[offset];
        offset++;

        // some mods store fineTune as unsigned
        fineTune = (fineTune << 28) >> 28;

        int volume = data[offset] & 0xFF;
        offset++;

        int loopStart = ExtraArrays.getBigEndianUnsignedShort(data, offset) << 1; // in words - multiply by 2
        offset += 2;

        int loopLength = ExtraArrays.getBigEndianUnsignedShort(data, offset) << 1; // in words - multiply by 2

        System.out.println("name: " + name);
        System.out.println("loopStart: " + loopStart + ", loopLength: " + loopLength + ", length: " + length);

        // there are some mods where loop length is larger than actual data length
        // currently we limit it to length
        // TODO move this somewhere else
        if (loopStart + loopLength > length) {
            System.out.println("Warning: loop length is larger than actual data length");
            loopLength = length - loopStart;
        }

        return new SampleHeader(name, length, fineTune, volume, loopStart, loopLength);
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

    private static String loadSampleName(byte[] data, int offset) {
        int index = ExtraArrays.indexOf(data, offset, 22, (byte) 0);

        if (index == -1) {
            return new String(data, offset, 22, StandardCharsets.US_ASCII);
        } else {
            return new String(data, offset, index, StandardCharsets.US_ASCII);
        }
    }

    private static Instrument[][][] loadPatterns(DataInput in, int patternCount, int channelCount) throws IOException {
        Instrument[][][] patterns = new Instrument[patternCount][Mod.ROW_COUNT][channelCount];

        for (int i = 0; i < patternCount; i++) {
            for (int j = 0; j < Mod.ROW_COUNT; j++) {
                for (int k = 0; k < channelCount; k++) {
                    patterns[i][j][k] = loadPattern(in);
                }
            }
        }

        return patterns;
    }

    public static Instrument loadPattern(DataInput in) throws IOException {
        int b0 = in.readUnsignedByte();
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();

        int sampleNumber = (b0 & 0xF0) | (b2 >> 4);
        int period = ((b0 & 0x0F) << 8) | b1;
        int effectCode = b2 & 0x0F;
        int effectArgumentX = b3 >> 4;
        int effectArgumentY = b3 & 0x0F;

        EffectType effectType;
        ExtendedEffectType extendedEffectType;

        if (effectCode == 0 && effectArgumentX == 0 && effectArgumentY == 0) {
            effectType = EffectType.NONE;
            extendedEffectType = ExtendedEffectType.NONE;
        } else {
            effectType = EffectType.valueOf(effectCode);

            if (effectType == EffectType.EXTENDED_EFFECT) {
                extendedEffectType = ExtendedEffectType.valueOf(effectArgumentX);
            } else {
                extendedEffectType = ExtendedEffectType.NONE;
            }
        }

        return new Instrument(sampleNumber, period, effectType, extendedEffectType, effectArgumentX, effectArgumentY);
    }

    private static Instrument[][][] loadPatterns(byte[] data, int offset, int patternCount, int channelCount) {
        Instrument[][][] patterns = new Instrument[patternCount][Mod.ROW_COUNT][channelCount];

        for (int i = 0; i < patternCount; i++) {
            for (int j = 0; j < Mod.ROW_COUNT; j++) {
                for (int k = 0; k < channelCount; k++) {
                    patterns[i][j][k] = loadPattern(data, offset);
                    offset += INSTRUMENT_LENGTH;
                }
            }
        }

        return patterns;
    }

    public static Instrument loadPattern(byte[] data, int offset) {
        int b0 = data[offset] & 0xFF;
        int b1 = data[offset + 1] & 0xFF;
        int b2 = data[offset + 2] & 0xFF;
        int b3 = data[offset + 3] & 0xFF;

        int sampleNumber = (b0 & 0xF0) | (b2 >> 4);
        int period = ((b0 & 0x0F) << 8) | b1;
        int effectCode = b2 & 0x0F;
        int effectArgumentX = b3 >> 4;
        int effectArgumentY = b3 & 0x0F;

        EffectType effectType;
        ExtendedEffectType extendedEffectType;

        if (effectCode == 0 && effectArgumentX == 0 && effectArgumentY == 0) {
            effectType = EffectType.NONE;
            extendedEffectType = ExtendedEffectType.NONE;
        } else {
            effectType = EffectType.valueOf(effectCode);

            if (effectType == EffectType.EXTENDED_EFFECT) {
                extendedEffectType = ExtendedEffectType.valueOf(effectArgumentX);
            } else {
                extendedEffectType = ExtendedEffectType.NONE;
            }
        }

        return new Instrument(sampleNumber, period, effectType, extendedEffectType, effectArgumentX, effectArgumentY);
    }

    private record SampleHeader(String name, int length, int fineTune, int volume, int loopStart, int loopLength) {
    }
}
