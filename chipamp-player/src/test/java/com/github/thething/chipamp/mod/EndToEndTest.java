package com.github.thething.chipamp.mod;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

// TODO remove later
public class EndToEndTest {

    @Test
    @Disabled
    public void play() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/Captain - Space Debris.mod");

        Player player = new Player();
        player.setClockHz(Mods.PAL_CLOCK_HZ);
        player.setSamplingRate(48_000);
        player.setMinPeriod(Mods.MIN_PERIOD);
        player.setMaxPeriod(Mods.MAX_PERIOD);
        player.setVolumeMultiplier(0.5f);
        player.setStereoEnabled(true);
        player.setStereoFoldDownEnabled(false);
        player.setVolumeSlideDeltaEnabled(false);
        player.setLoopDetectionEnabled(true);
        player.setLoggingEnabled(true);
        player.setMod(mod);

        player.setOpenMPTPanning();

        // player.setMuted(0, true);
        // player.setMuted(1, true);
        // player.setMuted(2, true);
        // player.setMuted(3, true);

        player.play();
    }
}
