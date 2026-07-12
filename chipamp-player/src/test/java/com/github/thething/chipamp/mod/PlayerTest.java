package com.github.thething.chipamp.mod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.util.function.Function;

import static java.util.Objects.checkFromIndexSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerTest {

    private Sampler sampler;
    private ModLoader modLoader;
    private CapturingSourceDataLine sourceDataLine;
    @Mock
    private Function<AudioFormat, SourceDataLine> sourceDataLineFactory;
    private Player underTest;

    @SuppressWarnings("resource")
    @BeforeEach
    void setUp() {
        sampler = new Sampler();
        modLoader = new ModLoader();
        sourceDataLine = spy(new CapturingSourceDataLine());

        doReturn(sourceDataLine).when(sourceDataLineFactory).apply(any(AudioFormat.class));

        underTest = new Player(sampler, sourceDataLineFactory);
    }

    @Test
    void shouldCreatePlayer() {
        new Player(sampler);
    }

    @Test
    void shouldPlayWholeSong() throws IOException, LineUnavailableException {
        Mod mod = modLoader.load("chip/Angelwings - 1995.mod");
        sampler.updateMod(mod);

        underTest.play();
        assertThat(sourceDataLine.totalBytesWritten).isEqualTo(7_368_964);

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }

    @Test
    void shouldPlayToEndSequence() throws IOException, LineUnavailableException {
        Mod mod = modLoader.load("chip/Angelwings - 1995.mod");
        sampler.updateMod(mod);
        sampler.seekSequence(1);

        underTest.play(3);
        assertThat(sourceDataLine.totalBytesWritten).isEqualTo(1_474_560);

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }

    @Test
    void shouldPlayFromToSequence() throws IOException, LineUnavailableException {
        Mod mod = modLoader.load("chip/Angelwings - 1995.mod");
        sampler.updateMod(mod);

        underTest.play(5, 8);
        assertThat(sourceDataLine.totalBytesWritten).isEqualTo(2_211_840);

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }

    @Test
    void shouldPlayPatterns() throws IOException, LineUnavailableException {
        Mod mod = modLoader.load("chip/Angelwings - 1995.mod");
        sampler.updateMod(mod);

        int patternCount = underTest.playPatterns(3);

        assertThat(patternCount).isEqualTo(3);
        assertThat(sourceDataLine.totalBytesWritten).isEqualTo(2_208_004);

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }

    @Test
    void shouldPlayRows() throws LineUnavailableException, IOException {
        Mod mod = modLoader.load("chip/Angelwings - 1995.mod");
        sampler.updateMod(mod);

        int rowCount = underTest.playRows(3435);

        assertThat(rowCount).isEqualTo(640);
        assertThat(sourceDataLine.totalBytesWritten).isEqualTo(7_368_964);

        verify(sourceDataLine).open(any(AudioFormat.class));
        verify(sourceDataLine).start();
        verify(sourceDataLine).drain();
        verify(sourceDataLine).close();
    }

    private static class CapturingSourceDataLine implements SourceDataLine {

        private int totalBytesWritten;

        @Override
        public void open(AudioFormat format, int bufferSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void open(AudioFormat format) {
        }

        @Override
        public int write(byte[] buffer, int offset, int length) {
            checkFromIndexSize(offset, length, buffer.length);
            totalBytesWritten += length;
            return length;
        }

        @Override
        public void drain() {
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRunning() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public AudioFormat getFormat() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getBufferSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int available() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getFramePosition() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLongFramePosition() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getMicrosecondPosition() {
            throw new UnsupportedOperationException();
        }

        @Override
        public float getLevel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Line.Info getLineInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void open() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isOpen() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Control[] getControls() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isControlSupported(Control.Type control) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Control getControl(Control.Type control) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addLineListener(LineListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeLineListener(LineListener listener) {
            throw new UnsupportedOperationException();
        }
    }
}