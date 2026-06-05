package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Strings;

import java.util.Set;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Detects whether a tracker ID string corresponds to a valid MOD tracker format.
 * <p>
 * This detector checks tracker IDs against known tracker signatures and common patterns used in MOD music files. It can
 * be used to validate module files by recognizing their tracker ID markers.
 */
public final class DefaultTrackerDetector implements Predicate<String> {

    private static final Set<String> DEFAULT_TRACKER_IDS = Set.of("M.K.", "M!K!", "M&K!", "N.T.", "NSMS", "LARD", "OKTA", "OCTA");

    public static final DefaultTrackerDetector INSTANCE = new DefaultTrackerDetector();

    private final Set<String> knownTrackerIds;

    public DefaultTrackerDetector() {
        this(DEFAULT_TRACKER_IDS);
    }

    /**
     * Creates a new tracker detector with a custom set of known tracker IDs.
     *
     * @param knownTrackerIds the set of known tracker ID strings to recognize
     * @throws NullPointerException if knownTrackerIds is null
     */
    public DefaultTrackerDetector(Set<String> knownTrackerIds) {
        this.knownTrackerIds = requireNonNull(knownTrackerIds);
    }

    /**
     * Tests whether the given tracker ID corresponds to a valid MOD tracker format.
     *
     * @param trackerId the tracker ID string to test (typically 4 characters)
     * @return {@code true} if the tracker ID is recognized, {@code false} otherwise
     */
    @Override
    public boolean test(String trackerId) {
        if (knownTrackerIds.contains(trackerId)) {
            return true;
        }

        // (XX)CH
        if (Strings.isDigit(trackerId, 0) && Strings.isDigit(trackerId, 1) && trackerId.charAt(2) == 'C' && trackerId.charAt(3) == 'H') {
            return true;
        }

        // (X)CHN
        if (Strings.isDigit(trackerId, 0) && trackerId.charAt(1) == 'C' && trackerId.charAt(2) == 'H' && trackerId.charAt(3) == 'N') {
            return true;
        }

        // TDZ(X)
        if (trackerId.charAt(0) == 'T' && trackerId.charAt(1) == 'D' && trackerId.charAt(2) == 'Z' && Strings.isDigit(trackerId, 3)) {
            return true;
        }

        // FA0(X)
        if (trackerId.charAt(0) == 'F' && trackerId.charAt(1) == 'A' && trackerId.charAt(2) == '0' && Strings.isDigit(trackerId, 3)) {
            return true;
        }

        // FLT(X)
        if (trackerId.charAt(0) == 'F' && trackerId.charAt(1) == 'L' && trackerId.charAt(2) == 'T' && Strings.isDigit(trackerId, 3)) {
            return true;
        }

        // EX0(X)
        if (trackerId.charAt(0) == 'E' && trackerId.charAt(1) == 'X' && trackerId.charAt(2) == '0' && Strings.isDigit(trackerId, 3)) {
            return true;
        }

        // CD(X)1
        return trackerId.charAt(0) == 'C' && trackerId.charAt(1) == 'D' && Strings.isDigit(trackerId, 2) && trackerId.charAt(3) == '1';
    }
}
