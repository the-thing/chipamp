package com.github.thething.chipgroove.mod;

public class Mod {

    private final String title;
    private final int length;
    private final Sample[] samples;
    private final int[] patternSequences;
    private final String trackerId;
    private final Instrument[][][] patterns;
    private final int patternCount;
    private final int rowCount;
    private final int channelCount;

    public Mod(
            String title, int length, Sample[] samples, int[] patternSequences, String trackerId,
            Instrument[][][] patterns, int patternCount, int rowCount, int channelCount) {
        this.title = title;
        this.length = length;
        this.samples = samples;
        this.patternSequences = patternSequences;
        this.trackerId = trackerId;
        this.patternCount = patternCount;
        this.patterns = patterns;
        this.rowCount = rowCount;
        this.channelCount = channelCount;
    }

    public String getTitle() {
        return title;
    }

    public int getLength() {
        return length;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getSampleCount() {
        return samples.length;
    }

    public Sample[] getSamples() {
        return samples;
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
