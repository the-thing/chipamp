package com.github.thething.chipgroove.mod;

import java.io.IOException;

public class ProtoPlayer {

    private static final int CHANNEL_COUNT = 8;
    private static final int PAL_CLOCK_HZ = 3_546_895;
    private static final int NTSC_CLOCK_HZ = 3_579_545;

    private final Channel[] channels;

    private int speed; //
    private int tempo; // beats per minute

    public ProtoPlayer() {
        this.channels = new Channel[CHANNEL_COUNT];
    }

    private void resetChannels() {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channels[i].reset();
        }
    }

    public void play(Mod mod) {
    }

    public static void main(String[] args) throws IOException {
        ModLoader modLoader = new ModLoader();
        Mod mod = modLoader.load("DJ Metune - Axel F.mod");

        ProtoPlayer player = new ProtoPlayer();
        player.play(mod);
    }
}
