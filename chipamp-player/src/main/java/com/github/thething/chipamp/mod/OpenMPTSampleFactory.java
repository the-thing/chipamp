package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Strings;

import java.util.Arrays;

public final class OpenMPTSampleFactory implements SampleFactory {

    public static final OpenMPTSampleFactory INSTANCE = new OpenMPTSampleFactory();

    @Override
    public Sample createSample(ModLoader.SampleHeader header, String trackerId, byte[] sampleData) {
        int loopStart = header.loopStart();
        int loopLength = header.loopLength();
        int sampleLength = sampleData.length;

        if (loopStart + loopLength > sampleLength) {
            if (sampleLength == 0) {
                // disable loop if length is zero
                loopStart = 0;
                loopLength = 0;
            } else if (loopStart == 0) {
                // expand to loop length if start is zero
                sampleData = Arrays.copyOf(sampleData, loopLength);
            } else if (Strings.equals(trackerId, "M.K.")) {
                // for M.K. tracker decrease loop start by half
                loopStart >>= 1;

                if (loopStart + loopLength > sampleLength) {
                    // if still breaches the sample, just shorten the loopLength
                    loopStart <<= 1;
                    loopLength -= loopStart;
                }
            } else {
                // truncate sample length to loopStart / 2 and set loopStart to zero
                sampleLength -= loopStart >> 1;
                sampleData = Arrays.copyOf(sampleData, sampleLength);
                loopStart = 0;

                if (loopStart + loopLength > sampleLength) {
                    // if still breaches, truncate the sample
                    sampleData = Arrays.copyOf(sampleData, loopLength);
                }
            }
        }

        return new Sample(header.name(), header.fineTune(), header.volume(), loopStart, loopLength, sampleData);
    }
}
