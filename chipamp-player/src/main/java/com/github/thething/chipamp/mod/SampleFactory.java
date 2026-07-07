package com.github.thething.chipamp.mod;

/**
 * Factory interface for creating {@link Sample} instances from MOD file data. Implementations can apply different
 * strategies for handling sample loop parameters and fixing inconsistencies in MOD files created by different
 * trackers.
 */
public interface SampleFactory {

    /**
     * Creates a sample from the provided header, tracker ID, and sample data.
     *
     * @param header     the sample header containing metadata such as name, fine-tune, volume, and loop parameters
     * @param trackerId  the tracker identifier (e.g., "M.K.") that created the MOD file
     * @param sampleData the raw PCM sample data
     * @return a new Sample instance configured according to the factory's implementation strategy
     */
    Sample createSample(ModLoader.SampleHeader header, String trackerId, byte[] sampleData);
}
