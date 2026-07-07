package com.github.thething.chipamp.tool;

import com.github.thething.chipamp.mod.AsyncSourceDataLine;
import com.github.thething.chipamp.mod.Mod;
import com.github.thething.chipamp.mod.ModLoader;
import com.github.thething.chipamp.mod.Mods;
import com.github.thething.chipamp.mod.Player;
import com.github.thething.chipamp.mod.Sampler;
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
    public void playAsync() throws IOException, LineUnavailableException, InterruptedException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/Angelwings - 1995.mod");

        Sampler sampler = new Sampler();
        sampler.loadMod(mod);

        AudioFormat format = sampler.getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            byte[] writeBuffer = new byte[1024 * 64];
            ThreadFactory factory = (runnable) -> new Thread(runnable, "AsyncSourceDataLine");

            try (AsyncSourceDataLine asyncLine = AsyncSourceDataLine.launch(line, 4096 * 4, factory)) {
                while (sampler.getSequenceIndex() < mod.getLength()) {
                    int bytesPerRow = sampler.getBytesPerRow();
                    int bytesPerSample = sampler.getBytesPerSample();
                    // int writeLength = bytesPerRow - player.getBytesPerSample(); // read less than a row sample
                    // int writeLength = player.getBytesPerTick();
                    int writeLength = 1024;

                    if (asyncLine.size() < writeLength) {

                        int readCount = sampler.read(writeBuffer);

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
    void playSync() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/Tip - Stardust Memories.mod");

        Sampler sampler = new Sampler();
        sampler.loadMod(mod);

        Player player = new Player(sampler);
        player.play();
    }

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(Play.class))
                .build();

        Launcher launcher = LauncherFactory.create();
        launcher.execute(request);
    }
}
