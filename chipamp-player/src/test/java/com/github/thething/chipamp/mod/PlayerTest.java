package com.github.thething.chipamp.mod;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayerTest {

    private Player underTest;
    private ModLoader modLoader;

    @BeforeEach
    void setUp() {
        underTest = new Player();
        underTest.setLoggingEnabled(true);

        modLoader = new ModLoader(true);
    }

    // TODO remove later
    @Test
    public void foo() throws IOException, LineUnavailableException {
        Mod mod = modLoader.load("chip/Jogeir Liljedahl - Nearly There.mod");

        Player player = new Player();
        player.setLoggingEnabled(true);
        player.setMod(mod);
        // player.setMuted(0, true);
        // player.setMuted(1, true);
        // player.setMuted(2, true);
        // player.setMuted(3, true);

        // player.seekPattern(13);
        player.play();

//        byte[] audio = player.read();
//        AudioFormat format = player.getCompatibleAudioFormat();
//        Resources.saveAudio(new File("Chipamp - H0ffman - Eon.mod.wav"), format, audio);
    }

    @Test
    public void shouldReadModWithLoopEffect() throws IOException {
        Mod mod = modLoader.load("chip/Jogeir Liljedahl - Nearly There.mod");
        underTest.setMod(mod);

        byte[] audio = underTest.read();
        assertThat(audio.length).isEqualTo(1921024);
//
//        underTest.reset();
//        underTest.setLoopDetectionEnabled(false);
//
//        audio = underTest.read();
//        assertThat(audio.length).isEqualTo(1921024);
    }

}
