package com.github.thething.chipamp.mod;

import static com.github.thething.chipamp.common.Requirements.requireInRange;
import static java.util.Objects.requireNonNull;

public final class Mod {

    public static final int ROW_COUNT = 64;
    public static final int PATTERN_SEQUENCE_COUNT = 128;
    public static final int MIN_LENGTH = 1;
    public static final int MAX_LENGTH = 128;
    public static final int MIN_CHANNEL_COUNT = 1;
    public static final int MAX_CHANNEL_COUNT = 16;
    public static final int SAMPLE_COUNT_1 = 31;
    public static final int SAMPLE_COUNT_2 = 15;
    public static final int MIN_PATTERN_COUNT = 1;
    public static final int MAX_PATTERN_COUNT = 128;

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
     * value in this table is the highest pattern stored. No patterns above this value are stored.
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
     * Pattern sheet. [channel][pattern][row]
     */
    private final Instrument[][][] patterns;

    public Mod(String title, int length, Sample[] samples, int[] patternSequences, String trackerId, Instrument[][][] patterns) {
        this.title = requireNonNull(title);
        this.length = requireInRange(length, MIN_LENGTH, MAX_LENGTH);
        this.samples = checkSamples(samples);
        this.patternSequences = checkPatternSequences(patternSequences);
        this.trackerId = requireNonNull(trackerId);
        this.patterns = checkPatterns(patterns);
    }

    private Sample[] checkSamples(Sample[] samples) {
        if (samples.length != SAMPLE_COUNT_1 && samples.length != SAMPLE_COUNT_2) {
            throw new IllegalArgumentException("Invalid sample length: " + samples.length);
        }

        for (Sample sample : samples) {
            requireNonNull(sample);
        }

        return samples;
    }

    private int[] checkPatternSequences(int[] patternSequences) {
        if (patternSequences.length != PATTERN_SEQUENCE_COUNT) {
            throw new IllegalArgumentException("Invalid pattern sequences length: " + patternSequences.length);
        }

        for (int patternSequence : patternSequences) {
            requireInRange(patternSequence, 0, MAX_LENGTH);
        }

        return patternSequences;
    }

    private Instrument[][][] checkPatterns(Instrument[][][] patterns) {
        requireInRange(patterns.length, MIN_CHANNEL_COUNT, MAX_CHANNEL_COUNT);
        requireInRange(patterns[0].length, MIN_PATTERN_COUNT, MAX_PATTERN_COUNT);

        if (patterns[0][0].length != ROW_COUNT) {
            throw new IllegalArgumentException("Invalid row count: " + patterns[0][0].length);
        }

        return patterns;
    }

    public String getTitle() {
        return title;
    }

    public int getLength() {
        return length;
    }

    public int getChannelCount() {
        return patterns.length;
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

    public int getPatternIndex(int sequenceIndex) {
        return patternSequences[sequenceIndex];
    }

    public String getTrackerId() {
        return trackerId;
    }

    public int getPatternCount() {
        return patterns[0].length;
    }

    public Instrument getInstrument(int channelIndex, int patternIndex, int rowIndex) {
        return patterns[channelIndex][patternIndex][rowIndex];
    }
}
