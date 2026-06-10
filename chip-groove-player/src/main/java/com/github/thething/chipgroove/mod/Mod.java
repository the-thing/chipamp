package com.github.thething.chipgroove.mod;

import static com.github.thething.chipgroove.common.Requirements.requireInRange;
import static java.util.Objects.requireNonNull;

public class Mod {

    public static final int ROW_COUNT = 64;

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 128;
    private static final int MIN_CHANNEL_COUNT = 4;
    private static final int MAX_CHANNEL_COUNT = 8;
    private static final int SAMPLE_COUNT = 31;

    /**
     * The module's title. Original ProTracker wrote letters only in uppercase.
     */
    private final String title;

    /**
     * Number of song positions / length in pattern count (ie. number of patterns played throughout the song). Legal
     * values are 1..128.
     */
    private final int length;

    /**
     * Samples.
     */
    private final Sample[] samples;

    /**
     * Pattern table: patterns to play in each song position (0..127) Each byte has a legal value of 0..63. The highest
     * value in this table is the highest pattern stored, no patterns above this value are stored.
     */
    private final int[] patternSequences;

    /**
     * Tracker identifier. Startrekker puts "FLT4" or "FLT8" here to indicate the # of channels. If there are more than
     * 64 patterns, ProTracker will put "M!K!" here. You might also find: "4CHN", "6CHN" or "8CHN" which indicates 4, 6
     * or 8 channels respectively. If no letters are here, then this is the start of the pattern data, and only 15
     * samples were present.
     */
    private final String trackerId;

    /**
     * Number of channels. In most case 4.
     */
    private final int channelCount;

    /**
     * Pattern sheet. [patternCount][rowCount][channelCount]
     */
    private final Instrument[][][] patterns;

    /**
     * Number of different patterns.
     */
    private final int patternCount;

    public Mod(
            String title, int length, Sample[] samples, int[] patternSequences, String trackerId,
            Instrument[][][] patterns, int patternCount, int channelCount) {
        this.title = requireNonNull(title);
        this.length = requireInRange(length, MIN_LENGTH, MAX_LENGTH);
        this.samples = checkSamples(samples);
        this.patternSequences = checkPatternSequences(patternSequences);
        this.trackerId = requireNonNull(trackerId);
        this.patternCount = patternCount;
        this.patterns = patterns;
        this.channelCount = requireInRange(channelCount, MIN_CHANNEL_COUNT, MAX_CHANNEL_COUNT);
    }

    private Sample[] checkSamples(Sample[] samples) {
        if (samples.length != SAMPLE_COUNT) {
            throw new IllegalArgumentException("Invalid sample count: " + samples.length);
        }

        for (int i = 0; i < samples.length; i++) {
            if (samples[i] == null) {
                throw new IllegalArgumentException("Invalid sample: " + samples[i]);
            }
        }

        return samples;
    }

    private int[] checkPatternSequences(int[] patternSequences) {

        for (int i = 0; i < patternSequences.length; i++) {

        }

        // TODO make sure that patterns are valid and exist
        return patternSequences;
    }

    public String getTitle() {
        return title;
    }

    public int getLength() {
        return length;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getSampleCount() {
        return samples.length;
    }

    public Sample getSample(int sample) {
        return samples[sample];
    }

    public int getPatternSequenceCount() {
        return patternSequences.length;
    }

    public int getPatternSequence(int position) {
        return patternSequences[position];
    }

    public String getTrackerId() {
        return trackerId;
    }

    public int getPatternCount() {
        return patternCount;
    }

    public Instrument getInstrument(int patternIndex, int rowIndex, int channelIndex) {
        return patterns[patternIndex][rowIndex][channelIndex];
    }
}
