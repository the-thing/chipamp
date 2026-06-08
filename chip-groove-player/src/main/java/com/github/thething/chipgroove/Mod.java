package com.github.thething.chipgroove;

public class Mod {

    private final String title;
    private final Sample[] samples;
    private final int[] patternSequences;
    private final String trackerId;
    private final int patternCount;
    private final Pattern[][][] patterns;

    // TODO validate
    public Mod(String title, Sample[] samples, int[] patternSequences, String trackerId, int patternCount, Pattern[][][] patterns) {
        this.title = title;
        this.samples = samples;
        this.patternSequences = patternSequences;
        this.trackerId = trackerId;
        this.patternCount = patternCount;
        this.patterns = patterns;
    }

    public String getTitle() {
        return title;
    }

    public Sample[] getSamples() {
        return samples;
    }

    public int[] getPatternSequences() {
        return patternSequences;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public int getPatternCount() {
        return patternCount;
    }

    public Pattern[][][] getPatterns() {
        return patterns;
    }
}
