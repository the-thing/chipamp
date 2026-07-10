package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SamplerTest {

    private Sampler underTest;
    private ModLoader modLoader;

    @BeforeEach
    void setUp() {
        underTest = new Sampler();
        underTest.setClockHz(Mods.PAL_CLOCK_HZ);
        underTest.setSamplingRate(48_000);
        underTest.setMinPeriod(Mods.MIN_PERIOD);
        underTest.setMaxPeriod(Mods.MAX_PERIOD);
        underTest.setVolumeMultiplier(0.5f);
        underTest.setLeftPan(Mods.MPT_LEFT_PAN);
        underTest.setRightPan(Mods.MPT_RIGHT_PAN);
        underTest.setStereoEnabled(true);
        underTest.setStereoFoldDownEnabled(true);
        underTest.setVolumeSlideDeltaEnabled(false);
        underTest.setRoundNearestPeriodEnabled(true);
        underTest.setLoopDetectionEnabled(true);
        underTest.setLoggingEnabled(false);

        modLoader = new ModLoader(true);
    }

    @Test
    void shouldReadModWithLoopEffect() throws IOException {
        Mod mod = modLoader.load("chip/Jogeir Liljedahl - Nearly There.mod");
        underTest.updateMod(mod);

        byte[] audio = underTest.read();
        assertThat(audio.length).isEqualTo(74_889_920);
    }

    @Test
    void shouldReadModWithInfiniteLoop() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.updateMod(mod);

        byte[] audio = underTest.read();
        assertThat(audio.length).isEqualTo(25_743_360);
    }

    @Test
    void shouldGenerateAudioFile() throws IOException, UnsupportedAudioFileException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        byte[] expectedAudio = Resources.readAudio("wav/axel-stereo-48kHz-pal.wav");
        byte[] audio = underTest.read();

        assertThat(audio).containsExactly(expectedAudio);
    }

    @Test
    void shouldGenerateSinglePattern() throws IOException, UnsupportedAudioFileException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        int sequenceIndex = underTest.skipPatterns(4);
        assertThat(sequenceIndex).isEqualTo(4);

        byte[] audio = underTest.readPatterns(1);
        byte[] expectedAudio = Resources.readAudio("wav/axel-stereo-48kHz-pal-pattern-1.wav");

        assertThat(audio).containsExactly(expectedAudio);
    }

    @Test
    void shouldSkipRows() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        int skippedRowCount = underTest.skipRows(23);
        assertThat(skippedRowCount).isEqualTo(23);

        skippedRowCount = underTest.skipRows(10);
        assertThat(skippedRowCount).isEqualTo(10);

        underTest.seekSequence(23);

        assertThat(underTest.getSequenceIndex()).isEqualTo(23);
        assertThat(underTest.getRowIndex()).isEqualTo(0);

        skippedRowCount = underTest.skipRows(4);
        assertThat(skippedRowCount).isEqualTo(4);

        skippedRowCount = underTest.skipRows(100);
        assertThat(skippedRowCount).isEqualTo(28);
    }

    @Test
    void shouldZeroStateAfterSequenceEnd() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        byte[] sample = new byte[underTest.getBytesPerSample()];
        int sampleCount = 0;

        int sequenceIndex;
        int rowIndex;
        int tickIndex;
        int sampleIndex;

        while (true) {
            sequenceIndex = underTest.getSequenceIndex();
            rowIndex = underTest.getRowIndex();
            tickIndex = underTest.getTickIndex();
            sampleIndex = underTest.getSampleIndex();

            if (sequenceIndex != 0) {
                break;
            }

            int readCount = underTest.read(sample);
            assertThat(readCount).isEqualTo(sample.length);

            sampleCount++;
        }

        assertThat(sampleCount).isEqualTo(396_023);
        assertThat(sequenceIndex).isEqualTo(1);
        assertThat(rowIndex).isEqualTo(0);
        assertThat(tickIndex).isEqualTo(0);
        assertThat(sampleIndex).isEqualTo(1);
    }

    @Test
    void shouldSeekSequence() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        underTest.seekSequence(11);

        assertThat(underTest.getSequenceIndex()).isEqualTo(11);
        assertThat(underTest.getRowIndex()).isEqualTo(0);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(1);

        underTest.seekSequence(11, 33);

        assertThat(underTest.getSequenceIndex()).isEqualTo(11);
        assertThat(underTest.getRowIndex()).isEqualTo(33);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(1);

        underTest.seekSequence(0);

        assertThat(underTest.getSequenceIndex()).isEqualTo(0);
        assertThat(underTest.getRowIndex()).isEqualTo(0);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(960);
    }

    @Test
    void shouldSeekPattern() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        int sequenceIndex = underTest.seekPattern(11);
        assertThat(sequenceIndex).isEqualTo(11);

        assertThat(underTest.getSequenceIndex()).isEqualTo(11);
        assertThat(underTest.getRowIndex()).isEqualTo(0);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(1);

        sequenceIndex = underTest.seekPattern(2);
        assertThat(sequenceIndex).isEqualTo(0);

        assertThat(underTest.getSequenceIndex()).isEqualTo(0);
        assertThat(underTest.getRowIndex()).isEqualTo(0);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(960);
    }

    @Test
    void shouldMatchStateWhenSkippingAndReading() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.updateMod(mod);

        underTest.reset();
        underTest.skipPatterns(1);

        int sequenceIndex = underTest.getSequenceIndex();
        int rowIndex = underTest.getRowIndex();
        int tickIndex = underTest.getTickIndex();
        int sampleIndex = underTest.getSampleIndex();

        underTest.reset();
        underTest.readPatterns(1);

        assertThat(underTest.getSequenceIndex()).isEqualTo(sequenceIndex);
        assertThat(underTest.getRowIndex()).isEqualTo(rowIndex);
        assertThat(underTest.getTickIndex()).isEqualTo(tickIndex);
        assertThat(underTest.getSampleIndex()).isEqualTo(sampleIndex);
    }

    @Test
    void shouldMatchStateWhenSkippingAndSeeking() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.updateMod(mod);

        underTest.reset();
        underTest.skipPatterns(1);

        int sequenceIndex = underTest.getSequenceIndex();
        int rowIndex = underTest.getRowIndex();
        int tickIndex = underTest.getTickIndex();
        int sampleIndex = underTest.getSampleIndex();

        underTest.reset();
        underTest.seekSequence(1);

        assertThat(underTest.getSequenceIndex()).isEqualTo(sequenceIndex);
        assertThat(underTest.getRowIndex()).isEqualTo(rowIndex);
        assertThat(underTest.getTickIndex()).isEqualTo(tickIndex);
        assertThat(underTest.getSampleIndex()).isEqualTo(sampleIndex);
    }

    @Test
    void shouldReadSameAudionWhenReadingAndSkippingPatterns() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.updateMod(mod);

        for (int sequenceIndex = 0; sequenceIndex < mod.getLength(); sequenceIndex++) {
            underTest.reset();
            underTest.skipPatterns(sequenceIndex);
            byte[] audio1 = underTest.readPatterns(1);

            underTest.reset();
            underTest.readPatterns(sequenceIndex);
            byte[] audio2 = underTest.readPatterns(1);

            assertThat(audio1).containsExactly(audio2);
        }
    }

    @Test
    public void shouldReturnReadRows() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        underTest.seekSequence(1);

        byte[] audio = underTest.readRows(2);
        assertThat(audio.length).isEqualTo(underTest.getBytesPerRow() * 2);
    }

    @Test
    void shouldGenerateSameAudioForSkipAndSeek() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.updateMod(mod);

        for (int sequenceIndex = 1; sequenceIndex < 2; sequenceIndex++) {
            underTest.reset();
            underTest.skipPatterns(sequenceIndex);
            byte[] audio1 = underTest.readPatterns(1);

            underTest.reset();
            underTest.seekSequence(sequenceIndex);
            byte[] audio2 = underTest.readPatterns(1);

            assertThat(audio1).containsExactly(audio2);
        }
    }

    @Test
    void shouldReturnSongLengthInMillis() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        assertThat(underTest.getSampleCount()).isEqualTo(8_834_496);
        assertThat(underTest.getModLength(TimeUnit.SECONDS)).isEqualTo(184L);
        assertThat(underTest.getModLength(TimeUnit.MILLISECONDS)).isEqualTo(184_052L);
    }

    @Test
    void shouldReturnBytesPerSample() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        assertThat(underTest.getBytesPerSample()).isEqualTo(4);

        underTest.setStereoEnabled(false);

        assertThat(underTest.getBytesPerSample()).isEqualTo(2);
    }

    @Test
    void shouldReturnBytesPerTick() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        assertThat(underTest.getBytesPerTick()).isEqualTo(3840);

        underTest.setStereoEnabled(false);

        assertThat(underTest.getBytesPerTick()).isEqualTo(1920);
    }

    @Test
    void shouldReturnSamplesPerTick() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        assertThat(underTest.getSamplesPerTick()).isEqualTo(960);
    }

    @Test
    void shouldReturnSamplesPerRow() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        assertThat(underTest.getSamplesPerRow()).isEqualTo(5760);
    }

    @Test
    void shouldLoadModWithEmptyDelayedSample() throws IOException {
        // empty pattern 0, row 17
        Mod mod = modLoader.load("chip/other/entertainer_pizcon.mod");
        underTest.updateMod(mod);
    }

    @Test
    void shouldLoadModWithFilter() throws IOException {
        // pattern 8, row 36
        Mod mod = modLoader.load("chip/other/alexel_-_synthetic_dreams.mod");
        underTest.updateMod(mod);
    }

    @Test
    void shouldChangeChannelPanning() throws IOException {
        Mod mod = modLoader.load("chip/other/afrigan_gagga.mod");
        underTest.updateMod(mod);

        assertThat(underTest.getLeftPan(5)).isEqualTo(0.25f);
        assertThat(underTest.getRightPan(5)).isEqualTo(0.75f);

        underTest.seekSequence(15, 11);

        assertThat(underTest.getLeftPan(5)).isEqualTo(0.25f);
        assertThat(underTest.getRightPan(5)).isEqualTo(0.75f);

        underTest.seekSequence(15, 12);

        assertThat(underTest.getLeftPan(5)).isEqualTo(0.0f);
        assertThat(underTest.getRightPan(5)).isEqualTo(1.0f);
    }

    @Test
    void shouldSampleModWithRoughPanningExtendedEffect() throws IOException {
        underTest.setLeftPan(1.0f);
        underTest.setRightPan(1.0f);

        Mod mod = modLoader.load("chip/other/eternal_vortex-1.mod");
        assertThat(Mods.isExtendedEffectPresent(mod, ExtendedEffectType.ROUGH_PANNING)).isTrue();

        underTest.updateMod(mod);

        assertThat(underTest.getLeftPan(1)).isEqualTo(0.0f);
        assertThat(underTest.getRightPan(1)).isEqualTo(1.0f);

        underTest.seekSequence(0, 1);

        assertThat(underTest.getLeftPan(1)).isEqualTo(0.0f);
        assertThat(underTest.getRightPan(1)).isEqualTo(1.0f);

        // effect takes place on sequence 0, row 1
        underTest.seekSequence(0, 2);

        assertThat(underTest.getLeftPan(1)).isEqualTo(0.53f);
        assertThat(underTest.getRightPan(1)).isEqualTo(0.47f);
    }
}
