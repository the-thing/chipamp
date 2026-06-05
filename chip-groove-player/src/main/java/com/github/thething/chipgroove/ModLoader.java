package com.github.thething.chipgroove;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ModLoader {

    public static void load(InputStream in) throws IOException {
        load((DataInput) new DataInputStream(in));
    }

    public static void load(DataInput in) throws IOException {
        StringBuilder builder = new StringBuilder(20);

        byte[] name = new byte[20];
        in.readFully(name, 0, 20);

        loadSample(in);
    }

    public static int indexOf(byte[] array, byte c) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == c) {
                return i;
            }
        }

        return -1;
    }

    public static void loadSample(DataInput in) throws IOException {
        byte[] nameArray = new byte[22];
        in.readFully(nameArray, 0, nameArray.length);

        int index = indexOf(nameArray, (byte) 0);
        String name;

        if (index == -1) {
            name = new String(nameArray, StandardCharsets.US_ASCII);
        } else {
            name = new String(nameArray, 0, index, StandardCharsets.US_ASCII);
        }

        int length = in.readUnsignedShort();
        int finetune = in.readByte();
        int volume = in.readUnsignedByte();
        int repeatOffset = in.readUnsignedShort();
        int repeatLength = in.readUnsignedShort();
    }
}
