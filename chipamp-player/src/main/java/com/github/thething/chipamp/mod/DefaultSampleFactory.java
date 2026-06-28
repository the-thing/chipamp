package com.github.thething.chipamp.mod;

public final class DefaultSampleFactory implements SampleFactory {

    public static final DefaultSampleFactory INSTANCE = new DefaultSampleFactory();

    /**
     * {@inheritDoc}
     */
    public Sample createSample(ModLoader.SampleHeader header, String trackerId, byte[] sampleData) {
        int loopStart = header.loopStart();
        int loopLength = header.loopLength();
        int sampleLength = sampleData.length;

        // TODO remove later
        if (header.loopStart() > sampleLength) {
            System.out.println("dupa: " + header.name() + " / " + header.loopStart() + " / " + header.loopLength() + " / " + sampleLength);
        }

        // TODO remove later
        if (header.loopStart() + header.loopLength() > sampleLength) {
            System.out.println("kupa: " + header.name() + " / " + header.loopStart() + " / " + header.loopLength() + " / " + sampleLength);
        }

        return new Sample(header.name(), header.fineTune(), header.volume(), loopStart, loopLength, sampleData);
    }
}
