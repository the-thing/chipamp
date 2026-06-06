package com.github.thething.chipgroove;

import com.github.thething.chipgroove.common.ChipArrays;
import org.jspecify.annotations.NonNull;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ModLoader {

    private static final int SAMPLE_COUNT = 31;
    private static final int PATTERN_COUNT = 128;
    private static final int CHANNEL_COUNT = 4;

    public static void load(InputStream in) throws IOException {
        load((DataInput) new DataInputStream(in));
    }

    public static void load(DataInput in) throws IOException {
        StringBuilder builder = new StringBuilder(20);

        byte[] name = new byte[20];
        in.readFully(name, 0, 20);

        SampleHeader[] sampleHeaders = new SampleHeader[SAMPLE_COUNT];

        for (int i = 0; i < SAMPLE_COUNT; i++) {
            sampleHeaders[i] = loadSampleHeader(in);
            System.out.println(sampleHeaders[i]);
        }

        int patternCount = in.readUnsignedByte();
        int songEnd = in.readUnsignedByte();

        int[] patternSequence = getPatternSequences(in);
        String trackerFormat = getTrackerFormat(in);

        int truePatternCount = ChipArrays.max(patternSequence) + 1;
        byte[][] sampleData = new byte[SAMPLE_COUNT][];

//        for (int i = 0; i < SAMPLE_COUNT; i++) {
//            sampleData[i] = new byte[sampleHeaders[i].length()];
//            in.readFully(sampleData[i]);
//        }
    }

    private static int[] getPatternSequences(DataInput in) throws IOException {
        byte[] bytes = new byte[PATTERN_COUNT];
        in.readFully(bytes);

        int[] patternSequences = new int[PATTERN_COUNT];

        for (int i = 0; i < bytes.length; i++) {
            patternSequences[i] = Byte.toUnsignedInt(bytes[i]);
        }

        return patternSequences;
    }

    private static String getTrackerFormat(DataInput in) throws IOException {
        byte[] trackerData = new byte[4];
        in.readFully(trackerData, 0, 4);

        return new String(trackerData, StandardCharsets.US_ASCII);
    }

    public static SampleHeader loadSampleHeader(DataInput in) throws IOException {
        byte[] nameArray = new byte[22];
        in.readFully(nameArray, 0, nameArray.length);

        int index = ChipArrays.indexOf(nameArray, (byte) 0);
        String name;

        if (index == -1) {
            name = new String(nameArray, StandardCharsets.US_ASCII);
        } else {
            name = new String(nameArray, 0, index, StandardCharsets.US_ASCII);
        }

        // sample length is double
        int length = in.readUnsignedShort() * 2;
        int finetune = in.readByte();
        int volume = in.readUnsignedByte();

        // TODO rename these
        int repeatOffset = in.readUnsignedShort();
        int repeatLength = in.readUnsignedShort();

        return new SampleHeader(name, length, finetune, volume, repeatOffset, repeatLength);
    }

    private record SampleHeader(String name, int length, int finetune, int volume, int repeatOffset, int repeatLength) {
    }
}
