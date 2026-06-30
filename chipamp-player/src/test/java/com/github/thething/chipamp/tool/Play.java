package com.github.thething.chipamp.tool;

import com.github.thething.chipamp.mod.Mod;
import com.github.thething.chipamp.mod.ModLoader;
import com.github.thething.chipamp.mod.Mods;
import com.github.thething.chipamp.mod.Player;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

// TODO remove later
public class Play {

    @Test
    public void play() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/Popcorn.mod");

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
    @Disabled
    public void dupa() throws IOException {
        for (File file : new File("C:\\Users\\Marcin\\Downloads\\mod").listFiles()) {
            if (!file.getName().endsWith(".mod")) {
                continue;
            }

            ModLoader modLoader = new ModLoader(true);

            System.out.println("Loading module: " + file.getName());
            Mod mod = modLoader.load(file);
            assertThat(mod).isNotNull();
        }
    }

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(Play.class))
                .build();

        Launcher launcher = LauncherFactory.create();
        launcher.execute(request);
    }
}
