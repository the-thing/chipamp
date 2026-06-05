package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Strings;

import java.util.function.ToIntFunction;

/**
 * Detects the number of audio channels based on a tracker ID string.
 * <p>
 * This detector recognizes various tracker ID patterns commonly found in MOD files to determine the number of audio
 * channels. If no recognized pattern is found, it defaults to 4 channels.
 */
public final class DefaultChannelDetector implements ToIntFunction<String> {

    public static final DefaultChannelDetector INSTANCE = new DefaultChannelDetector();

    /**
     * Detects the number of channels from the given tracker ID string.
     * <p>
     * The method checks the tracker ID against known patterns to determine the channel count. If no pattern matches, it
     * returns 4 as the default channel count.
     *
     * @param trackerId the tracker ID string (typically 4 characters)
     * @return the number of audio channels (defaults to 4 if no pattern is recognized)
     */
    @Override
    public int applyAsInt(String trackerId) {
        // (XX)CH
        if (Strings.isDigit(trackerId, 0) && Strings.isDigit(trackerId, 1) && trackerId.charAt(2) == 'C' && trackerId.charAt(3) == 'H') {
            return Integer.parseInt(trackerId.substring(0, 2));
        }

        // (X)CHN
        if (Strings.isDigit(trackerId, 0) && trackerId.charAt(1) == 'C' && trackerId.charAt(2) == 'H' && trackerId.charAt(3) == 'N') {
            return trackerId.charAt(0) - '0';
        }

        // TDZ(X)
        if (trackerId.charAt(0) == 'T' && trackerId.charAt(1) == 'D' && trackerId.charAt(2) == 'Z' && Strings.isDigit(trackerId, 3)) {
            return trackerId.charAt(3) - '0';
        }

        // FA0(X)
        if (trackerId.charAt(0) == 'F' && trackerId.charAt(1) == 'A' && trackerId.charAt(2) == '0' && Strings.isDigit(trackerId, 3)) {
            return trackerId.charAt(3) - '0';
        }

        // FLT(X)
        if (trackerId.charAt(0) == 'F' && trackerId.charAt(1) == 'L' && trackerId.charAt(2) == 'T' && Strings.isDigit(trackerId, 3)) {
            return trackerId.charAt(3) - '0';
        }

        // EX0(X)
        if (trackerId.charAt(0) == 'E' && trackerId.charAt(1) == 'X' && trackerId.charAt(2) == '0' && Strings.isDigit(trackerId, 3)) {
            return trackerId.charAt(3) - '0';
        }

        // CD(X)1
        if (trackerId.charAt(0) == 'C' && trackerId.charAt(1) == 'D' && Strings.isDigit(trackerId, 2) && trackerId.charAt(3) == '1') {
            return trackerId.charAt(2) - '0';
        }

        // OKTA, OCTA
        if (Strings.equals(trackerId, "OKTA") || Strings.equals(trackerId, "OCTA")) {
            return 8;
        }

        return 4;
    }
}
