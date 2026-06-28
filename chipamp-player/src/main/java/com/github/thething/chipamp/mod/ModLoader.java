package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.ExtraArrays;
import com.github.thething.chipamp.common.Strings;
import com.github.thething.chipamp.io.Resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public final class ModLoader {

    private static final int INSTRUMENT_LENGTH = 4;
    private static final int SAMPLE_HEADER_LENGTH = 30;
    private static final Set<String> DEFAULT_TRACKER_IDS = Set.of("M.K.", "M!K!", "N.T.", "NSMS", "LARD", "OKTA", "OCTA");

    private final Set<String> knownTrackerIds;
    private final SampleFactory sampleFactory;

    public ModLoader() {
        this(DEFAULT_TRACKER_IDS, OpenMPTSampleFactory.INSTANCE);
    }

    public ModLoader(Set<String> knownTrackerIds, SampleFactory sampleFactory) {
        this.knownTrackerIds = requireNonNull(knownTrackerIds);
        this.sampleFactory = requireNonNull(sampleFactory);
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

    // TODO add support for PP20 PowerPacker
    public Mod load(byte[] data) {
        int offset = 0;

        String title = loadTitle(data);
        offset += 20;

        int probeOffset = 20 + 31 * SAMPLE_HEADER_LENGTH + 2 + Mod.PATTERN_SEQUENCE_COUNT;
        String probeTrackerId = new String(data, probeOffset, 4, StandardCharsets.US_ASCII);
        int sampleCount;

        boolean knownTackerId = isKnownTrackerId(probeTrackerId);

        if (knownTackerId) {
            sampleCount = Mod.SAMPLE_COUNT_1;
        } else {
            sampleCount = Mod.SAMPLE_COUNT_2;
        }

        System.out.println("probeTrackerId: " + probeTrackerId);

        SampleHeader[] sampleHeaders = loadSampleHeaders(data, offset, sampleCount);
        offset += sampleHeaders.length * SAMPLE_HEADER_LENGTH;

         System.out.println("Sample headers: " + Arrays.toString(sampleHeaders));

        int length = data[offset] & 0xFF;
        offset++;

        // Historically set to 127, but can be safely ignored.
        // NoiseTracker uses this byte to indicate restart position.
        // This has been made redundant by the 'Position Jump' effect.
        offset++;

        int[] patternSequences = loadPatternSequences(data, offset);
        offset += patternSequences.length;

        // TODO remove
        // System.out.println("Pattern sequences: " + Arrays.toString(patternSequences));

        String trackerId;

        if (knownTackerId) {
            trackerId = probeTrackerId;
            offset += 4;
        } else {
            trackerId = "\0\0\0\0";
        }

        int patternCount = ExtraArrays.max(patternSequences) + 1;
        int channelCount = detectChannelCount(probeTrackerId);

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

    private static int detectChannelCount(String trackerId) {
        // (XX)CH
        if (Strings.isDigit(trackerId, 0) && Strings.isDigit(trackerId, 1) && trackerId.charAt(2) == 'C' && trackerId.charAt(3) == 'H') {
            return Integer.parseInt(trackerId.substring(0, 2));
        }

        // (X)CHN
        if (Strings.isDigit(trackerId, 0) && trackerId.charAt(1) == 'C' && trackerId.charAt(2) == 'H' && trackerId.charAt(3) == 'N') {
            return trackerId.charAt(0) - '0';
        }

        // TDZ(X)
        if (trackerId.charAt(0) == 'T' && trackerId.charAt(1) == 'D' && trackerId.charAt(2) == 'Z' && Strings.isDigit(trackerId, 3)) {
            return trackerId.charAt(3) - '0';
        }

        // FA0(X)
        if (trackerId.charAt(0) == 'F' && trackerId.charAt(1) == 'A' && trackerId.charAt(2) == '0' && Strings.isDigit(trackerId, 3)) {
            return trackerId.charAt(3) - '0';
        }

        // FLT(X)
        if (trackerId.charAt(0) == 'F' && trackerId.charAt(1) == 'L' && trackerId.charAt(2) == 'T' && Strings.isDigit(trackerId, 3)) {
            return trackerId.charAt(3) - '0';
        }

        // EX0(X)
        if (trackerId.charAt(0) == 'E' && trackerId.charAt(1) == 'X' && trackerId.charAt(2) == '0' && Strings.isDigit(trackerId, 3)) {
            return trackerId.charAt(3) - '0';
        }

        // CD(X)1
        if (trackerId.charAt(0) == 'C' && trackerId.charAt(1) == 'D' && Strings.isDigit(trackerId, 2) && trackerId.charAt(3) == '1') {
            return trackerId.charAt(2) - '0';
        }

        // OKTA, OCTA
        if (Strings.equals(trackerId, "OKTA") || Strings.equals(trackerId, "OCTA")) {
            return 8;
        }

        return 4;
    }

    private boolean isKnownTrackerId(String trackerId) {
        if (knownTrackerIds.contains(trackerId)) {
            return true;
        }

        // (XX)CH
        if (Strings.isDigit(trackerId, 0) && Strings.isDigit(trackerId, 1) && trackerId.charAt(2) == 'C' && trackerId.charAt(3) == 'H') {
            return true;
        }

        // (X)CHN
        if (Strings.isDigit(trackerId, 0) && trackerId.charAt(1) == 'C' && trackerId.charAt(2) == 'H' && trackerId.charAt(3) == 'N') {
            return true;
        }

        // TDZ(X)
        if (trackerId.charAt(0) == 'T' && trackerId.charAt(1) == 'D' && trackerId.charAt(2) == 'Z' && Strings.isDigit(trackerId, 3)) {
            return true;
        }

        // FA0(X)
        if (trackerId.charAt(0) == 'F' && trackerId.charAt(1) == 'A' && trackerId.charAt(2) == '0' && Strings.isDigit(trackerId, 3)) {
            return true;
        }

        // FLT(X)
        if (trackerId.charAt(0) == 'F' && trackerId.charAt(1) == 'L' && trackerId.charAt(2) == 'T' && Strings.isDigit(trackerId, 3)) {
            return true;
        }

        // EX0(X)
        if (trackerId.charAt(0) == 'E' && trackerId.charAt(1) == 'X' && trackerId.charAt(2) == '0' && Strings.isDigit(trackerId, 3)) {
            return true;
        }

        // TODO CD(X)1

        return false;
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
            int actualLength = Math.min(sampleHeaders[i].length(), data.length - offset);
            int expectedLength = sampleHeaders[i].length();

            if (actualLength != expectedLength) {
                System.err.println("Warning: sample " + ( i + 1) + " is shorter than expected: " + expectedLength + ", trackerId = " + trackerId);
            }

            byte[] sampleData = new byte[sampleHeaders[i].length()];

            System.arraycopy(data, offset, sampleData, 0, actualLength);
            offset += actualLength;

            SampleHeader sampleHeader = sampleHeaders[i];
            System.out.println("sample index: " + (i + 1));

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
