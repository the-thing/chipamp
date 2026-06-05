package com.github.thething.chipgroove;

import com.github.thething.chipgroove.io.Resources;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

public final class Main {

    public static void main(String[] args) throws IOException {
        try (DataInputStream in = new DataInputStream(Resources.getResourceAsStream("h0ffman-eon.mod"))) {
            ModLoader.load((DataInput) in);
        }
    }
}
