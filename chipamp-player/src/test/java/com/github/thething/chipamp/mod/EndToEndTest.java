package com.github.thething.chipamp.mod;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

// TODO remove later
public class EndToEndTest {

    @Test
    public void play() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");

        Player player = new Player();
        player.setClockHz(Mods.PAL_CLOCK_HZ);
        player.setSamplingRate(48_000);
//        player.setMinPeriod(Mods.MIN_PERIOD);
//        player.setMaxPeriod(Mods.MAX_PERIOD);

        player.setMinPeriod(113);
        player.setMaxPeriod(856);
        player.setVolumeMultiplier(1.0f);
        player.setStereoEnabled(true);
        player.setStereoFoldDownEnabled(true);
        player.setVolumeSlideDeltaEnabled(false);
        player.setLoopDetectionEnabled(true);
        player.setLoggingEnabled(true);
        player.setMod(mod);

        player.setMuted(0, true);
        // player.setMuted(1, true);
        player.setMuted(2, true);
        player.setMuted(3, true);

//        player.seekPattern(1);
//        player.playPatterns(1);

        player.seekPattern(1);
        player.playPatterns(1);

//        byte[] audio = player.read();
//        AudioFormat format = player.getCompatibleAudioFormat();
//        Resources.saveAudio(new File("Chipamp - H0ffman - Eon.mod.wav"), format, audio);
    }
}
