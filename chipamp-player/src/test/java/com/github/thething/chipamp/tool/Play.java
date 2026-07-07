package com.github.thething.chipamp.tool;

import com.github.thething.chipamp.mod.AsyncSourceDataLine;
import com.github.thething.chipamp.mod.ExtendedEffectType;
import com.github.thething.chipamp.mod.Instrument;
import com.github.thething.chipamp.mod.Mod;
import com.github.thething.chipamp.mod.ModLoader;
import com.github.thething.chipamp.mod.Mods;
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

class Play {

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

            byte[] buffer = new byte[1024 * 64];
            ThreadFactory factory = (runnable) -> new Thread(runnable, "AsyncSourceDataLine");

            try (AsyncSourceDataLine asyncLine = AsyncSourceDataLine.launch(line, 4096 * 4, factory)) {
                while (sampler.getSequenceIndex() < mod.getLength()) {
                    int writeLength = 1024;

                    if (asyncLine.size() < writeLength) {
                        int readCount = sampler.read(buffer);

                        if (writeLength != readCount) {
                            System.out.println("readCount: " + readCount + ", writeLength = " + writeLength);
                        }

                        int dupa = asyncLine.write(buffer, 0, readCount);

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

    @Test
    void playStrangeSound() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/effect/static_-_hardcore.mod");

        Sampler sampler = new Sampler();
        sampler.loadMod(mod);
        sampler.seekSequence(2);
        sampler.setLoggingEnabled(true);

        sampler.setMuted(1, true);
        sampler.setMuted(2, true);
        sampler.setMuted(3, true);

        // sequence 2 (pattern 0), row 50 it sounds strange

        Player player = new Player(sampler);
        player.playPatterns(1);
    }

    // TODO remove
    @Test
    void foo() throws IOException {
        ModLoader loader = new ModLoader(true);

        main:
        for (File file : new File("C:\\Users\\Marcin\\Downloads\\mod").listFiles()) {
            if (!file.getName().endsWith(".mod")) {
                continue;
            }

            // System.out.println("Loading file: " + file.getName());
            Mod mod = loader.load(file);

            if (Mods.isExtendedEffectPresent(mod, ExtendedEffectType.SET_FILTER)) {

                for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                    for (int patternIndex = 0; patternIndex < mod.getPatternCount(); patternIndex++) {
                        for (int rowIndex = 0; rowIndex < Mod.ROW_COUNT; rowIndex++) {
                            Instrument instrument = mod.getInstrument(channelIndex, patternIndex, rowIndex);

                            if (instrument.extendedEffectType() == ExtendedEffectType.SET_FILTER) {
                                if ((instrument.effectArgumentY() & 1) == 0) {
                                    System.out.println("SET FILTER present: " + file.getName());
                                    continue main;
                                }
                            }
                        }
                    }
                }
            }
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
