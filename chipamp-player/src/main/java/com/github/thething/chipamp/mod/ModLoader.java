package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.ExtraArrays;
import com.github.thething.chipamp.io.Resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static java.util.Objects.requireNonNull;

// TODO add support for PP20 PowerPacker decompression
public final class ModLoader {

    private static final int INSTRUMENT_LENGTH = 4;
    private static final int SAMPLE_HEADER_LENGTH = 30;

    private final Predicate<String> trackerDetector;
    private final ToIntFunction<String> channelDetector;
    private final SampleFactory sampleFactory;

    public ModLoader() {
        this(DefaultTrackerDetector.INSTANCE, DefaultChannelDetector.INSTANCE, OpenMPTSampleFactory.INSTANCE);
    }

    public ModLoader(Predicate<String> trackerDetector, ToIntFunction<String> channelDetector, SampleFactory sampleFactory) {
        this.trackerDetector = requireNonNull(trackerDetector);
        this.channelDetector = requireNonNull(channelDetector);
        this.sampleFactory = requireNonNull(sampleFactory);
    }

    public Mod load(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            return load(Resources.readBytes(in));
        }
    }

    public Mod load(String name) throws IOException {
        return load(Resources.readBytes(name));
    }

    public Mod load(byte[] data) {
        int offset = 0;

        String title = loadTitle(data);
        offset += 20;

        int probeOffset = 20 + 31 * SAMPLE_HEADER_LENGTH + 2 + Mod.PATTERN_SEQUENCE_COUNT;
        String probeTrackerId = new String(data, probeOffset, 4, StandardCharsets.US_ASCII);
        int sampleCount;

        boolean knownTackerId = trackerDetector.test(probeTrackerId);

        if (knownTackerId) {
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

        if (knownTackerId) {
            trackerId = probeTrackerId;
            offset += 4;
        } else {
            trackerId = "\0\0\0\0";
        }

        int patternCount = ExtraArrays.max(patternSequences) + 1;
        int channelCount = channelDetector.applyAsInt(probeTrackerId);

        Instrument[][][] patterns = loadPatterns(data, offset, patternCount, channelCount);
        offset += patterns.length * Mod.ROW_COUNT * channelCount * INSTRUMENT_LENGTH;

        Sample[] samples = new Sample[sampleCount];
        offset = loadSamples(data, offset, sampleHeaders, trackerId, samples);

        int remaining = data.length - offset;

        if (remaining > 0) {
            System.err.println("Warning: " + remaining + " bytes are not used");
        }

        return new Mod(title, length, samples, patternSequences, trackerId, patterns);
    }

    private static String loadTitle(byte[] bytes) {
        int index = ExtraArrays.indexOf(bytes, 0, 20, (byte) 0);

        if (index == -1) {
            return new String(bytes, 0, 20, StandardCharsets.US_ASCII);
        } else {
            return new String(bytes, 0, index, StandardCharsets.US_ASCII);
        }
    }

    private static int[] loadPatternSequences(byte[] data, int offset) {
        int[] patternSequences = new int[Mod.PATTERN_SEQUENCE_COUNT];

        for (int i = 0; i < Mod.PATTERN_SEQUENCE_COUNT; i++) {
            patternSequences[i] = data[i + offset] & 0xFF;
        }

        return patternSequences;
    }

    private int loadSamples(byte[] data, int offset, SampleHeader[] sampleHeaders, String trackerId, Sample[] out) {
        for (int i = 0; i < sampleHeaders.length; i++) {
            // there might be corrupted samples that are shorter than the header says (end of file)
            int expectedLength = sampleHeaders[i].length();
            int actualLength = Math.min(expectedLength, data.length - offset);

            if (expectedLength != actualLength) {
                System.err.println("Warning: sample " + ( i + 1) + " is shorter than expected (" + expectedLength + " vs " + actualLength + ")");

                if (Math.abs(expectedLength - actualLength) < 5) {
                    System.out.println("dudfsdf");
                }
            }

            // it is possible that actual and expected length are different
            byte[] sampleData = new byte[sampleHeaders[i].length()];

            System.arraycopy(data, offset, sampleData, 0, actualLength);
            offset += actualLength;

            SampleHeader sampleHeader = sampleHeaders[i];

            out[i] = sampleFactory.createSample(sampleHeader, trackerId, sampleData);
        }

        return offset;
    }

    private static SampleHeader[] loadSampleHeaders(byte[] data, int offset, int sampleCount) {
        SampleHeader[] sampleHeaders = new SampleHeader[sampleCount];

        for (int i = 0; i < sampleHeaders.length; i++) {
            sampleHeaders[i] = loadSampleHeader(data, offset);
            offset += SAMPLE_HEADER_LENGTH;
        }

        return sampleHeaders;
    }

    private static SampleHeader loadSampleHeader(byte[] data, int offset) {
        String name = loadSampleName(data, offset);
        offset += 22;

        int length = ExtraArrays.getBigEndianUnsignedShort(data, offset) << 1; // in words - multiply by 2
        offset += 2;

        int fineTune = (data[offset] << 28) >> 28;
        offset++;

        int volume = data[offset] & 0xFF;
        offset++;

        int loopStart = ExtraArrays.getBigEndianUnsignedShort(data, offset) << 1; // in words - multiply by 2
        offset += 2;

        int loopLength = ExtraArrays.getBigEndianUnsignedShort(data, offset) << 1; // in words - multiply by 2

        if (loopLength == 2) {
            // loop length equal to 0 or 2 bytes (1 in words) still means loop is disabled
            loopLength = 0;
        }

        return new SampleHeader(name, length, fineTune, volume, loopStart, loopLength);
    }

    private static String loadSampleName(byte[] data, int offset) {
        int index = ExtraArrays.indexOf(data, offset, 22, (byte) 0);

        if (index == -1) {
            return new String(data, offset, 22, StandardCharsets.US_ASCII);
        } else {
            return new String(data, offset, index, StandardCharsets.US_ASCII);
        }
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

    public record SampleHeader(String name, int length, int fineTune, int volume, int loopStart, int loopLength) {
    }
}
