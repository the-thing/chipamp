package com.github.thething.chipamp.tool;

import com.github.thething.chipamp.mod.AsyncSourceDataLine;
import com.github.thething.chipamp.mod.Mod;
import com.github.thething.chipamp.mod.ModLoader;
import com.github.thething.chipamp.mod.Player;
import com.github.thething.chipamp.mod.Sampler;
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

class PlayTool {

    @Test
    public void playAsync() throws IOException, LineUnavailableException, InterruptedException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/Slawomir Mrozek - Franko End.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);

        AudioFormat format = sampler.getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            byte[] buffer = new byte[1024 * 64];
            ThreadFactory factory = (runnable) -> new Thread(runnable, "AsyncSourceDataLine");

            int writeLength = 1024;
            int readLength = 1024;

            try (AsyncSourceDataLine asyncLine = AsyncSourceDataLine.launch(line, buffer.length, readLength, factory)) {
                while (sampler.getSequenceIndex() < mod.getLength()) {
                    if (asyncLine.size() < writeLength) {
                        int readCount = sampler.read(buffer);
                        int writeCount = asyncLine.write(buffer, 0, readCount);

                        if (writeCount != readCount) {
                            throw new RuntimeException("Unable to write all samples");
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
        Mod mod = modLoader.load("chip/H0ffman - Eon.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);
        sampler.setStereoFoldDownEnabled(false);

        sampler.seekPattern(60);

        Player player = new Player(sampler);
        player.play();
    }

    @Test
    void playStrangeSound() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/static_-_hardcore.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.seekSequence(2);
        sampler.setLoggingEnabled(true);

        // sequence 2 (pattern 0), row 50 it sounds strange

        Player player = new Player(sampler);
        player.playPatterns(3);
    }

    @Test
    void playCutSample() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/adventure_in.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.seekSequence(2);
        sampler.setLoggingEnabled(true);

        // sequence 2 (pattern 0), row 14

        Player player = new Player(sampler);
        player.playPatterns(1);
    }

    @Test
    void playDelayPattern() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/emotional_extacy.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.seekSequence(1);
        sampler.setLoggingEnabled(true);

        // sequence 1 (pattern 7), row 62

        Player player = new Player(sampler);
        player.playPatterns(1);
    }

    @Test
    void playInvertLoop() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/euroremix.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);

        // there are plenty of invert loop effects there

        Player player = new Player(sampler);
        player.play();
    }

    @Test
    void playCustomPeriods() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/90s_house_project.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setRoundNearestPeriodEnabled(false);
        sampler.setLoggingEnabled(true);

        sampler.setMuted(0, true);
        sampler.setMuted(1, true);
        sampler.setMuted(2, true);
        sampler.setMuted(3, false);

        sampler.seekSequence(12);

        // pattern 12 is full of custom notes on channel 3

        Player player = new Player(sampler);
        player.playPatterns(1);
    }

    // TODO remove
    @Test
    void test() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/elgpasa.mod");

        // System.out.println(Formatters.formatSamples(mod));
        // System.out.println(Formatters.formatPatterns(mod));

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);

        Player player = new Player(sampler);
        player.play();
    }

    // TODO remove
    @Test
    void foo() throws IOException {
        ModLoader modLoader = new ModLoader(true);
        Sampler sampler = new Sampler();

        for (File file : new File("C:\\Users\\Marcin\\Downloads\\mod").listFiles()) {
            if (!file.getName().endsWith(".mod")) {
                continue;
            }

            System.out.println("Loading: " + file.getName());
            Mod mod = modLoader.load(file);

            sampler.updateMod(mod);

            while (sampler.getSequenceIndex() < mod.getLength()) {
                sampler.read();
            }
        }
    }

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(PlayTool.class))
                .build();

        Launcher launcher = LauncherFactory.create();
        launcher.execute(request);
    }
}
