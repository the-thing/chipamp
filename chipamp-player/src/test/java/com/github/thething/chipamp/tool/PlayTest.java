package com.github.thething.chipamp.tool;

import com.github.thething.chipamp.concurrent.IdleStrategy;
import com.github.thething.chipamp.concurrent.SleepingIdleStrategy;
import com.github.thething.chipamp.mod.AsyncSourceDataLine;
import com.github.thething.chipamp.mod.Mod;
import com.github.thething.chipamp.mod.ModLoader;
import com.github.thething.chipamp.mod.Player;
import com.github.thething.chipamp.mod.Sampler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;

class PlayTest {

    @Test
    @Disabled
    public void playAsync() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/H0ffman - Eon.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);
        sampler.setStereoEnabled(false);

        AudioFormat format = sampler.getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        IdleStrategy idleStrategy = new SleepingIdleStrategy(100L);

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            int length = sampler.getBytesPerRow();
            byte[] buffer = new byte[1024 * 64];

            try (AsyncSourceDataLine asyncLine = AsyncSourceDataLine.launch(line, length)) {
                while (sampler.getSequenceIndex() < mod.getLength()) {
                    int diff = length - asyncLine.size();

                    if (diff > 0) {
                        int readCount = sampler.read(buffer, 0, diff);

                        if (readCount <= 0) {
                            throw new RuntimeException("Unexpected end of audio");
                        }

                        int writeCount = asyncLine.write(buffer, 0, readCount);

                        if (writeCount != readCount) {
                            throw new RuntimeException("Unable to write all samples: " + readCount + " != " + writeCount);
                        }
                    } else {
                        idleStrategy.idle();
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                }
            }
        }
    }

    @Test
    // @Disabled
    void playSync() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/Captain - Space Debris.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);

        Player player = new Player(sampler);
        player.play();
    }

    @Test
    @Disabled
    void playStrangeSound() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/static_-_hardcore.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);

        // sequence 2 (pattern 0), row 50 it sounds strange
        sampler.seekSequence(2);

        Player player = new Player(sampler);
        player.playPatterns(1);
    }

    @Test
    @Disabled
    void playCutSample() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/adventure_in.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);

        sampler.seekSequence(2);
        // sequence 2 (pattern 0), row 14

        Player player = new Player(sampler);
        player.playPatterns(1);
    }

    @Test
    @Disabled
    void playDelayPattern() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/emotional_extacy.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);

        // sequence 1 (pattern 7), row 62
        sampler.seekSequence(1);

        Player player = new Player(sampler);
        player.playPatterns(1);
    }

    @Test
    @Disabled
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
    @Disabled
    void playCustomPeriods() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/90s_house_project.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setRoundNearestPeriodEnabled(false);
        sampler.setLoggingEnabled(true);

        // pattern 12 is full of custom notes on channel 3
        sampler.seekSequence(12);

        Player player = new Player(sampler);
        player.playPatterns(1);
    }

    @Test
    @Disabled
    void playFilter() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/alexel_-_synthetic_dreams.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);

        sampler.seekPattern(8);

        // pattern 8, row 36
        Player player = new Player(sampler);
        player.playPatterns(1);
    }

    @Test
    @Disabled
    void playSetPanning() throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader(true);
        Mod mod = modLoader.load("chip/other/afrigan_gagga.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);
        sampler.setLoggingEnabled(true);

        // sequence 15, row 11
        sampler.seekSequence(15);

        Player player = new Player(sampler);
        player.playPatterns(1);
    }
}
