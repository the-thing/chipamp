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

/**
 * Parses raw ProTracker MOD file data into a {@link Mod}.
 * <p>
 * The loader reads the title, sample headers, pattern sequence table, tracker ID, patterns, and sample data from the
 * MOD binary layout, delegating channel-count and tracker-ID detection to the configured strategies so that both
 * 15-sample (M.K.-less) and 31-sample MOD variants can be handled.
 */
// TODO add support for PowerPacker (PP20) decompression
public final class ModLoader {

    private static final int INSTRUMENT_LENGTH = 4;
    private static final int SAMPLE_HEADER_LENGTH = 30;
    private static final boolean DEFAULT_LOGGING_ENABLED = false;

    private final Predicate<String> trackerDetector;
    private final ToIntFunction<String> channelDetector;
    private final SampleFactory sampleFactory;
    private final boolean loggingEnabled;

    /**
     * Creates a loader using the default tracker detector, channel detector, and sample factory, with logging
     * disabled.
     */
    public ModLoader() {
        this(DefaultTrackerDetector.INSTANCE, DefaultChannelDetector.INSTANCE, OpenMPTSampleFactory.INSTANCE, DEFAULT_LOGGING_ENABLED);
    }

    /**
     * Creates a loader using the default tracker detector, channel detector, and sample factory.
     *
     * @param loggingEnabled whether warnings about malformed or unexpected data should be logged to {@code System.err}
     */
    public ModLoader(boolean loggingEnabled) {
        this(DefaultTrackerDetector.INSTANCE, DefaultChannelDetector.INSTANCE, OpenMPTSampleFactory.INSTANCE, loggingEnabled);
    }

    /**
     * Creates a loader with fully customizable detection and sample creation strategies.
     *
     * @param trackerDetector identifies whether a probed 4-byte tracker ID belongs to a known 31-sample tracker format
     * @param channelDetector determines the channel count of the module from the probed tracker ID
     * @param sampleFactory   creates {@link Sample} instances from raw sample headers and data
     * @param loggingEnabled  whether warnings about malformed or unexpected data should be logged to
     *                        {@code System.err}
     */
    public ModLoader(Predicate<String> trackerDetector, ToIntFunction<String> channelDetector, SampleFactory sampleFactory, boolean loggingEnabled) {
        this.trackerDetector = requireNonNull(trackerDetector);
        this.channelDetector = requireNonNull(channelDetector);
        this.sampleFactory = requireNonNull(sampleFactory);
        this.loggingEnabled = loggingEnabled;
    }

    /**
     * Loads a MOD file from the disk.
     *
     * @param file the MOD file to read
     * @return the parsed module
     * @throws IOException if the file cannot be read
     */
    public Mod load(File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            return load(Resources.readBytes(in));
        }
    }

    /**
     * Loads a MOD file from a named classpath resource.
     *
     * @param resourceName the resource name to read
     * @return the parsed module
     * @throws IOException if the resource cannot be read
     */
    public Mod load(String resourceName) throws IOException {
        return load(Resources.readBytes(resourceName));
    }

    /**
     * Parses a MOD module from raw bytes.
     *
     * @param data the raw MOD file contents
     * @return the parsed module
     */
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

        Instrument[][][] patterns = loadPatterns(data, offset, channelCount, patternCount, sampleCount);
        offset += patternCount * Mod.ROW_COUNT * channelCount * INSTRUMENT_LENGTH;

        Sample[] samples = new Sample[sampleCount];
        offset = loadSamples(data, offset, sampleHeaders, trackerId, samples);

        int remaining = data.length - offset;

        if (loggingEnabled && remaining > 0) {
            System.err.println("Warning: " + remaining + " bytes are not used");
        }

        return new Mod(title, length, samples, patternSequences, trackerId, patterns);
    }

    /**
     * Reads the module title from the first 20 bytes of the file.
     *
     * @param bytes the raw MOD file contents
     * @return the null-terminated (or full 20-byte) title, decoded as US-ASCII
     */
    private static String loadTitle(byte[] bytes) {
        int index = ExtraArrays.indexOf(bytes, 0, 20, (byte) 0);

        if (index == -1) {
            return new String(bytes, 0, 20, StandardCharsets.US_ASCII);
        } else {
            return new String(bytes, 0, index, StandardCharsets.US_ASCII);
        }
    }

    /**
     * Reads the pattern sequence (song position) table.
     *
     * @param data   the raw MOD file contents
     * @param offset the offset at which the sequence table begins
     * @return the pattern index referenced by each song position
     */
    private static int[] loadPatternSequences(byte[] data, int offset) {
        int[] patternSequences = new int[Mod.PATTERN_SEQUENCE_COUNT];

        for (int i = 0; i < Mod.PATTERN_SEQUENCE_COUNT; i++) {
            patternSequences[i] = data[i + offset] & 0xFF;
        }

        return patternSequences;
    }

    /**
     * Reads sample data for each sample header and creates the corresponding {@link Sample} instances.
     * <p>
     * A file may cut off abruptly, leaving less data than a header's declared length promises; in that case the missing
     * tail is treated as silence and, if {@link #loggingEnabled}, a warning is logged.
     *
     * @param data          the raw MOD file contents
     * @param offset        the offset at which the first sample's data begins
     * @param sampleHeaders the previously parsed sample headers, in sample order
     * @param trackerId     the detected tracker ID, passed through to the sample factory
     * @param out           the array to populate with one {@link Sample} per header
     * @return the offset immediately following the last sample's data
     */
    private int loadSamples(byte[] data, int offset, SampleHeader[] sampleHeaders, String trackerId, Sample[] out) {
        for (int i = 0; i < sampleHeaders.length; i++) {
            // it is possible that file cuts off abruptly and expected length from header is different from the actual
            int expectedLength = sampleHeaders[i].length();
            int actualLength = Math.min(expectedLength, data.length - offset);
            int offsetLength = actualLength;

            if (actualLength == expectedLength && expectedLength == 2) {
                // sample of length 2 is still empty
                actualLength = 0;
                expectedLength = 0;
            }

            if (loggingEnabled && actualLength != expectedLength) {
                System.err.println("Warning: sample '" + sampleHeaders[i].name() + "' / " + (i + 1) + " has length " + actualLength + " instead of " + expectedLength);
            }

            // it is possible that actual and expected length are different
            byte[] sampleData = new byte[expectedLength];

            System.arraycopy(data, offset, sampleData, 0, actualLength);
            offset += offsetLength;

            SampleHeader sampleHeader = sampleHeaders[i];

            out[i] = sampleFactory.createSample(sampleHeader, trackerId, sampleData);
        }

        return offset;
    }

    /**
     * Reads the fixed-size sample header table.
     *
     * @param data        the raw MOD file contents
     * @param offset      the offset at which the first sample header begins
     * @param sampleCount the number of sample headers to read (15 or 31 depending on the detected tracker format)
     * @return the parsed sample headers, in sample order
     */
    private static SampleHeader[] loadSampleHeaders(byte[] data, int offset, int sampleCount) {
        SampleHeader[] sampleHeaders = new SampleHeader[sampleCount];

        for (int i = 0; i < sampleHeaders.length; i++) {
            sampleHeaders[i] = loadSampleHeader(data, offset);
            offset += SAMPLE_HEADER_LENGTH;
        }

        return sampleHeaders;
    }

    /**
     * Reads a single 30-byte sample header.
     *
     * @param data   the raw MOD file contents
     * @param offset the offset at which this sample header begins
     * @return the parsed sample header
     */
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

    /**
     * Reads a sample name from a 22-byte, null-terminated field.
     *
     * @param data   the raw MOD file contents
     * @param offset the offset at which the name field begins
     * @return the null-terminated (or full 22-byte) name, decoded as US-ASCII
     */
    private static String loadSampleName(byte[] data, int offset) {
        int index = ExtraArrays.indexOf(data, offset, 22, (byte) 0);

        if (index == -1) {
            return new String(data, offset, 22, StandardCharsets.US_ASCII);
        } else {
            return new String(data, offset, index, StandardCharsets.US_ASCII);
        }
    }

    /**
     * Reads all pattern data, indexed by channel, pattern, and row.
     *
     * @param data         the raw MOD file contents
     * @param offset       the offset at which the first pattern's data begins
     * @param channelCount the number of channels in the module
     * @param patternCount the number of distinct patterns referenced by the sequence table
     * @param sampleCount  the number of samples in the module, used to validate/wrap out-of-range sample numbers
     * @return a {@code [channel][pattern][row]} array of instrument entries
     */
    private static Instrument[][][] loadPatterns(byte[] data, int offset, int channelCount, int patternCount, int sampleCount) {
        Instrument[][][] patterns = new Instrument[channelCount][patternCount][Mod.ROW_COUNT];

        for (int patternIndex = 0; patternIndex < patternCount; patternIndex++) {
            for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
                    patterns[channelIndex][patternIndex][rowIndex] = loadPattern(data, offset, sampleCount);
                    offset += INSTRUMENT_LENGTH;
                }
            }
        }

        return patterns;
    }

    /**
     * Decodes a single 4-byte instrument entry (one pattern cell) into an {@link Instrument}.
     *
     * @param data        the raw MOD file contents
     * @param offset      the offset at which this instrument entry begins
     * @param sampleCount the number of samples in the module; if the decoded sample number exceeds this, it is wrapped
     *                    modulo {@code sampleCount + 1}
     * @return the decoded instrument entry
     */
    public static Instrument loadPattern(byte[] data, int offset, int sampleCount) {
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

        if (sampleNumber > sampleCount) {
            sampleNumber %= sampleCount + 1;
        }

        return new Instrument(sampleNumber, period, effectType, extendedEffectType, effectArgumentX, effectArgumentY);
    }

    /**
     * A parsed sample header, as read from the MOD file's sample header table.
     *
     * @param name       the sample name
     * @param length     the sample data length in bytes
     * @param fineTune   the fine-tune value (-8 to +7)
     * @param volume     the default volume (0-64)
     * @param loopStart  the loop start offset in bytes
     * @param loopLength the loop length in bytes, or 0 if looping is disabled
     */
    public record SampleHeader(String name, int length, int fineTune, int volume, int loopStart, int loopLength) {
    }
}
