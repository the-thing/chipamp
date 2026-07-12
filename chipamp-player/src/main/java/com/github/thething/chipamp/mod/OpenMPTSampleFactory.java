package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Strings;

import java.util.Arrays;

/**
 * Sample factory implementation that applies OpenMPT-style loop parameter corrections. This factory handles various
 * edge cases where loop start and loop length parameters exceed the actual sample length, applying different fix
 * strategies depending on the tracker ID and specific conditions.
 * <p>
 * The correction strategies include:
 * <ul>
 *   <li>Disabling loops for zero-length samples</li>
 *   <li>Expanding sample data to match loop length when loop start is zero</li>
 *   <li>Adjusting loop start by halving it for M.K. tracker files</li>
 *   <li>Truncating samples and adjusting loop parameters for other trackers</li>
 * </ul>
 */
public final class OpenMPTSampleFactory implements SampleFactory {

    public static final OpenMPTSampleFactory INSTANCE = new OpenMPTSampleFactory();

    /**
     * Creates a sample with OpenMPT-compatible loop parameter corrections. When the loop start plus loop length exceeds
     * the sample length, applies tracker-specific fixes to ensure valid loop parameters.
     *
     * @param header     the sample header containing metadata and loop parameters
     * @param trackerId  the tracker identifier used to determine correction strategy
     * @param sampleData the raw PCM sample data
     * @return a new Sample instance with corrected loop parameters
     */
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
