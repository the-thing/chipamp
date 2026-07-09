package com.github.thething.chipamp.mod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlayerTest {

    private Sampler sampler;
    private ModLoader modLoader;
    private Player underTest;
    @Mock
    private Function<AudioFormat, SourceDataLine> sourceDataLineFactory;
    @Mock
    private SourceDataLine sourceDataLine;

    @SuppressWarnings("resource")
    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            // return the length of written bytes
            return invocation.getArgument(2);
        }).when(sourceDataLine).write(any(byte[].class), anyInt(), anyInt());

        sampler = new Sampler();
        doReturn(sourceDataLine).when(sourceDataLineFactory).apply(any(AudioFormat.class));

        modLoader = new ModLoader();

        underTest = new Player(sampler, sourceDataLineFactory);
    }

    @Test
    void shouldPlayWholeSong() throws IOException, LineUnavailableException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        sampler.updateMod(mod);

        underTest.play();

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine, times(8627)).write(any(byte[].class), eq(0), eq(1024 * 4));
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }

    @Test
    void shouldPlayToEndSequence() throws IOException, LineUnavailableException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        sampler.updateMod(mod);
        sampler.seekSequence(1);

        underTest.play(3);

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine, times(776)).write(any(byte[].class), eq(0), eq(1024 * 4));
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }

    @Test
    void shouldPlayFromToSequence() throws IOException, LineUnavailableException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        sampler.updateMod(mod);

        underTest.play(5, 8);

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine, times(1164)).write(any(byte[].class), eq(0), eq(1024 * 4));
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }

    @Test
    void shouldPlayPatterns() throws IOException, LineUnavailableException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        sampler.updateMod(mod);

        underTest.playPatterns(3);

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine, times(1163)).write(any(byte[].class), eq(0), eq(1024 * 4));
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }

    @Test
    void shouldPlayRows() throws LineUnavailableException, IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        sampler.updateMod(mod);

        underTest.playRows(3435);

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine, times(8627)).write(any(byte[].class), eq(0), eq(1024 * 4));
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }
}