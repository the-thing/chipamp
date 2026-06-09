package com.github.thething.chipgroove.mod;

public class Mod {

    private final String title;
    private final Sample[] samples;
    private final int[] patternSequences;
    private final String trackerId;
    private final Pattern[][][] patterns;
    private final int patternCount;
    private final int rowCount;
    private final int channelCount;

    // TODO validate
    public Mod(
            String title, Sample[] samples, int[] patternSequences, String trackerId,
            Pattern[][][] patterns, int patternCount, int rowCount, int channelCount) {
        this.title = title;
        this.samples = samples;
        this.patternSequences = patternSequences;
        this.trackerId = trackerId;
        this.patternCount = patternCount;
        this.patterns = patterns;
        this.rowCount = rowCount;
        this.channelCount = channelCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public String getTitle() {
        return title;
    }

    public int getSampleCount() {
        return samples.length;
    }

    public Sample[] getSamples() {
        return samples;
    }

    public int getPatternSequenceCount() {
        return patternSequences.length;
    }

    public int getPatternSequence(int index) {
        return patterns[index].length;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public int getPatternCount() {
        return patternCount;
    }

    public Pattern getPattern(int patternIndex, int rowIndex, int channelIndex) {
        return patterns[patternIndex][rowIndex][channelIndex];
    }
}
