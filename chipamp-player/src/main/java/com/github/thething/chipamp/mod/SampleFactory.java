package com.github.thething.chipamp.mod;

public interface SampleFactory {

    Sample createSample(ModLoader.SampleHeader header, String trackerId, byte[] sampleData);
}
