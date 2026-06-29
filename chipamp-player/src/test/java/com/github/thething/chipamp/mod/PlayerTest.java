package com.github.thething.chipamp.mod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayerTest {

    private Player underTest;
    private ModLoader modLoader;

    @BeforeEach
    void setUp() {
        underTest = new Player();
        modLoader = new ModLoader(true);
    }

    // TODO remove later
    @Test
    public void foo() throws IOException, LineUnavailableException {
        underTest = new Player();
        underTest.setLoggingEnabled(true);
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
        assertThat(audio.length).isEqualTo(74_889_920);
    }

    @Test
    public void shouldReadModWithInfiniteLoop() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.setMod(mod);

        byte[] audio = underTest.read();
        assertThat(audio.length).isEqualTo(25_743_360);
    }

}
