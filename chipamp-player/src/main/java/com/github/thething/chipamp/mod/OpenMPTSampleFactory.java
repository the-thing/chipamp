package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Strings;

import java.util.Arrays;

public final class OpenMPTSampleFactory implements SampleFactory {

    public static final OpenMPTSampleFactory INSTANCE = new OpenMPTSampleFactory();

    /**
     * {@inheritDoc}
     */
    public Sample createSample(ModLoader.SampleHeader header, String trackerId, byte[] sampleData) {
        int loopStart = header.loopStart();
        int loopLength = header.loopLength();
        int sampleLength = sampleData.length;

        if (loopStart + loopLength > sampleLength) {
            // TODO remove later
            System.out.println("kupa: " + header.name() + " / " + header.loopStart() + " / " + header.loopLength() + " / " + sampleLength + ", trackerId=" + trackerId);

            if (sampleLength == 0) {
                loopStart = 0;
                loopLength = 0;
            } else if (loopStart == 0) {
                sampleData = Arrays.copyOf(sampleData, loopLength);
            } else if (Strings.equals(trackerId, "M.K.")) {
                loopStart >>= 1;

                if (loopStart + loopLength > sampleLength) {
                    // if still breaches the sample, just shorten the length
                    loopStart <<= 1;
                    loopLength -= loopStart;
                }
            } else {
                sampleLength -= loopStart >> 1;
                loopStart = 0;
            }

            System.out.println("After, loopStart=" + loopStart + ", loopLength=" + loopLength + ", sampleLength=" + sampleLength);
        }

        return new Sample(header.name(), header.fineTune(), header.volume(), loopStart, loopLength, sampleData);
    }
}
