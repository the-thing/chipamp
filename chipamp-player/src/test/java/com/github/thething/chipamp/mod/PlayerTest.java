package com.github.thething.chipamp.mod;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;

public class PlayerTest {

    // TODO remove later
    @Test
    public void foo() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader();
        Mod mod = modLoader.load(new File("chip/Jogeir Liljedahl - Nearly There.mod"));

        Player player = new Player();
        player.setLoggingEnabled(true);
        player.setMod(mod);
        // player.setMuted(0, true);
        // player.setMuted(1, true);
        // player.setMuted(2, true);
        // player.setMuted(3, true);

        player.seekPattern(13);
        player.play();

//        byte[] audio = player.read();
//        AudioFormat format = player.getCompatibleAudioFormat();
//        Resources.saveAudio(new File("Chipamp - H0ffman - Eon.mod.wav"), format, audio);
    }

}
