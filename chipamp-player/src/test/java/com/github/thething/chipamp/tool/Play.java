package com.github.thething.chipamp.tool;

import com.github.thething.chipamp.mod.AsyncSourceDataLine;
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadFactory;

import static org.assertj.core.api.Assertions.assertThat;

// TODO remove later
public class Play {

    @Test
    public void play() throws IOException, LineUnavailableException, InterruptedException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");

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

        System.out.println("getBytesPerTick: " + player.getBytesPerTick());
        System.out.println("getBytesPerRow: " + player.getBytesPerRow());

        AudioFormat format = player.getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        ThreadFactory factory = (runnable) -> new Thread(runnable, "AsyncSourceDataLine");

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            byte[] writeBuffer = new byte[1024 * 64];

            try (AsyncSourceDataLine asyncLine = AsyncSourceDataLine.launch(line, 4096 * 4, factory)) {
                while (player.getSequenceIndex() < mod.getLength()) {
                    int bytesPerRow = player.getBytesPerRow();
                    int bytesPerSample = player.getBytesPerSample();
                    // int writeLength = bytesPerRow - player.getBytesPerSample(); // read less than a row sample
                    // int writeLength = player.getBytesPerTick();
                    int writeLength = 1024;

                    if (asyncLine.size() < writeLength) {

                        int readCount = player.read(writeBuffer);

//                        if (writeLength != readCount) {
//                            System.out.println("readCount: " + readCount + ", writeLength = " + writeLength);
//                        }

                        int dupa = asyncLine.write(writeBuffer, 0, readCount);

                        if (dupa != readCount) {
                            System.out.println("dupa: " + dupa + ", readCount = " + readCount);
                        }

                    } else {
                        Thread.sleep(1);
                    }


                }
            }
        }
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
