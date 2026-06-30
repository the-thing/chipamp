package com.github.thething.chipamp.mod;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

// TODO remove later
public class EndToEndTest {

    @Test
    // @Disabled
    public void play() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/broken/janes.mod");

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

    @Test
    public void dupa() throws IOException {
        for (File file : new File("C:\\Users\\Marcin\\Downloads\\mod").listFiles()) {
            if (!file.getName().endsWith(".mod")) {
                continue;
            }

            ModLoader modLoader = new ModLoader(true);

            System.out.println("Loading module: " + file.getName());
            Mod mod = modLoader.load(file);
            assertThat(mod).isNotNull();

            if (mod.getChannelCount() > 4) {
                System.out.println("Channel count: " + mod.getChannelCount());
            }
        }
    }
}
