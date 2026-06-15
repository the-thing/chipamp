package com.github.thething.chipgroove.mod;

import java.io.IOException;

public class ProtoPlayer {

    private static final int CHANNEL_COUNT = 8;

    private final Channel[] channels;

    private int speed;
    private int tempo;

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
