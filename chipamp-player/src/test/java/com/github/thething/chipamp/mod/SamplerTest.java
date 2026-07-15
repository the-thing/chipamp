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

        byte[] audio = underTest.readAll();
        assertThat(audio.length).isEqualTo(74_889_920);
    }

    @Test
    void shouldReadModWithInfiniteLoop() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.updateMod(mod);

        byte[] audio = underTest.readAll();
        assertThat(audio.length).isEqualTo(25_743_360);
    }

    @Test
    void shouldMoveOutsideOfPatternRangeOnInfiniteLoopSeek() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.updateMod(mod);

        // move to the infinite loop position jump
        underTest.seekSequence(18, 15);

        assertThat(underTest.getSequenceIndex()).isEqualTo(18);
        assertThat(underTest.getRowIndex()).isEqualTo(15);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);

        // if loop detection is enabled
        underTest.skipRows(1);

        assertThat(underTest.getSequenceIndex()).isEqualTo(129);
        assertThat(underTest.getRowIndex()).isEqualTo(0);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);
    }

    @Test
    void shouldAllowReadingAfterSeekingOutsideOfLoopDetection() throws IOException {
        Mod mod = modLoader.load("chip/Allister Brimble - Superfrog Intro.mod");
        underTest.updateMod(mod);

        // move to after the infinite loop position jump
        underTest.seekSequence(18, 16);

        assertThat(underTest.getSequenceIndex()).isEqualTo(18);
        assertThat(underTest.getRowIndex()).isEqualTo(16);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);

        // we seeked outside of loop so we can actually read some empty rows
        int skippedRowCount = underTest.skipRows(100);

        assertThat(skippedRowCount).isEqualTo(48);
        assertThat(underTest.getSequenceIndex()).isEqualTo(19);
        assertThat(underTest.getRowIndex()).isEqualTo(0);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);
    }

    @Test
    void shouldGenerateAudioFile() throws IOException, UnsupportedAudioFileException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        byte[] expectedAudio = Resources.readAudio("wav/axel-stereo-48kHz-pal.wav");
        byte[] audio = underTest.readAll();

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

        assertThat(sampleCount).isEqualTo(397_056);
        assertThat(sequenceIndex).isEqualTo(1);
        assertThat(rowIndex).isEqualTo(0);
        assertThat(tickIndex).isEqualTo(0);
        assertThat(sampleIndex).isEqualTo(0);
    }

    @Test
    void shouldSeekSequence() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");
        underTest.updateMod(mod);

        underTest.seekSequence(11);

        assertThat(underTest.getSequenceIndex()).isEqualTo(11);
        assertThat(underTest.getRowIndex()).isEqualTo(0);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);

        underTest.seekSequence(11, 33);

        assertThat(underTest.getSequenceIndex()).isEqualTo(11);
        assertThat(underTest.getRowIndex()).isEqualTo(33);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);

        underTest.seekSequence(0);

        assertThat(underTest.getSequenceIndex()).isEqualTo(0);
        assertThat(underTest.getRowIndex()).isEqualTo(0);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);
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
        assertThat(underTest.getSampleIndex()).isEqualTo(0);

        sequenceIndex = underTest.seekPattern(2);
        assertThat(sequenceIndex).isEqualTo(0);

        assertThat(underTest.getSequenceIndex()).isEqualTo(0);
        assertThat(underTest.getRowIndex()).isEqualTo(0);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);
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

    @Test
    void shouldMuteAndUnmuteChannel() {
        assertThat(underTest.isMuted(0)).isFalse();
        assertThat(underTest.isMuted(1)).isFalse();
        assertThat(underTest.isMuted(2)).isFalse();
        assertThat(underTest.isMuted(3)).isFalse();

        underTest.setMuted(1, true);

        assertThat(underTest.isMuted(0)).isFalse();
        assertThat(underTest.isMuted(1)).isTrue();
        assertThat(underTest.isMuted(2)).isFalse();
        assertThat(underTest.isMuted(3)).isFalse();
    }

    @Test
    void shouldEnableAndDisableEffects() {
        assertThat(underTest.isEffectEnabled(EffectType.SLIDE_UP)).isTrue();

        underTest.setEffectEnabled(EffectType.SLIDE_UP, false);

        assertThat(underTest.isEffectEnabled(EffectType.SLIDE_UP)).isFalse();

        underTest.setEffectEnabled(EffectType.SLIDE_UP, true);

        assertThat(underTest.isEffectEnabled(EffectType.SLIDE_UP)).isTrue();
    }

    @Test
    void shouldEnableAndDisableExtendedEffects() {
        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.ROUGH_PANNING)).isTrue();

        underTest.setExtendedEffectEnabled(ExtendedEffectType.ROUGH_PANNING, false);

        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.ROUGH_PANNING)).isFalse();

        underTest.setExtendedEffectEnabled(ExtendedEffectType.ROUGH_PANNING, true);

        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.ROUGH_PANNING)).isTrue();
    }

    @Test
    void shouldEnableAndDisableAllEffects() {
        assertThat(underTest.isEffectEnabled(EffectType.SLIDE_UP)).isTrue();
        assertThat(underTest.isEffectEnabled(EffectType.EXTENDED_EFFECT)).isTrue();
        assertThat(underTest.isEffectEnabled(EffectType.POSITION_JUMP)).isTrue();

        underTest.setAllEffectsEnabled(false);

        assertThat(underTest.isEffectEnabled(EffectType.SLIDE_UP)).isFalse();
        assertThat(underTest.isEffectEnabled(EffectType.EXTENDED_EFFECT)).isFalse();
        assertThat(underTest.isEffectEnabled(EffectType.POSITION_JUMP)).isFalse();

        underTest.setAllEffectsEnabled(true);

        assertThat(underTest.isEffectEnabled(EffectType.SLIDE_UP)).isTrue();
        assertThat(underTest.isEffectEnabled(EffectType.EXTENDED_EFFECT)).isTrue();
        assertThat(underTest.isEffectEnabled(EffectType.POSITION_JUMP)).isTrue();
    }

    @Test
    void shouldEnableAndDisableAllExtendedEffects() {
        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.ROUGH_PANNING)).isTrue();
        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.CUT_SAMPLE)).isTrue();
        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.FINE_SLIDE_UP)).isTrue();

        underTest.setAllExtendedEffectsEnabled(false);

        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.ROUGH_PANNING)).isFalse();
        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.CUT_SAMPLE)).isFalse();
        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.FINE_SLIDE_UP)).isFalse();

        underTest.setAllExtendedEffectsEnabled(true);

        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.ROUGH_PANNING)).isTrue();
        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.CUT_SAMPLE)).isTrue();
        assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.FINE_SLIDE_UP)).isTrue();
    }

    /**
     * 3 x 32 rows of a pattern loop (pattern 1, row 0).
     *
     * <pre>
     * 0001 | 00 | F-3 0B E60 |
     * 0001 | 01 | --- -- --- |
     * 0001 | 02 | F-3 03 --- |
     * 0001 | 03 | --- -- --- |
     * 0001 | 04 | F-3 0B --- |
     * 0001 | 05 | --- -- --- |
     * 0001 | 06 | F-3 03 --- |
     * 0001 | 07 | --- -- --- |
     * 0001 | 08 | F-3 0B --- |
     * 0001 | 09 | --- -- --- |
     * 0001 | 10 | F-3 03 --- |
     * 0001 | 11 | F-3 02 --- |
     * 0001 | 12 | F-3 01 --- |
     * 0001 | 13 | F-3 02 --- |
     * 0001 | 14 | F-3 03 --- |
     * 0001 | 15 | --- -- --- |
     * 0001 | 16 | F-3 0B --- |
     * 0001 | 17 | --- -- --- |
     * 0001 | 18 | F-3 03 --- |
     * 0001 | 19 | --- -- --- |
     * 0001 | 20 | F-3 0B --- |
     * 0001 | 21 | --- -- --- |
     * 0001 | 22 | F-3 03 --- |
     * 0001 | 23 | --- -- --- |
     * 0001 | 24 | F-3 0B --- |
     * 0001 | 25 | --- -- --- |
     * 0001 | 26 | F-3 03 --- |
     * 0001 | 27 | F-3 02 --- |
     * 0001 | 28 | F-3 01 --- |
     * 0001 | 29 | F-3 02 --- |
     * 0001 | 30 | F-3 03 --- |
     * 0001 | 31 | --- -- E62 |
     * </pre>
     */
    @Test
    void shouldExecuteSameLoopMultipleTimesAfterSeek() throws IOException {
        Mod mod = modLoader.load("chip/other/euroremix.mod");

        Sampler sampler = new Sampler();
        sampler.updateMod(mod);

        for (int i = 0; i < 5; i++) {
            sampler.seekSequence(8, 0);

            assertThat(sampler.getContext().loopPending).isEqualTo(false);
            assertThat(sampler.getContext().loopRowIndex).isEqualTo(0);
            assertThat(sampler.getContext().loopCounter).isEqualTo(0);

            sampler.skipRows(32);

            assertThat(sampler.getSequenceIndex()).isEqualTo(8);
            assertThat(sampler.getRowIndex()).isEqualTo(0);
            assertThat(sampler.getTickIndex()).isEqualTo(0);
            assertThat(sampler.getSampleIndex()).isEqualTo(0);

            assertThat(sampler.getContext().loopPending).isEqualTo(false);
            assertThat(sampler.getContext().loopRowIndex).isEqualTo(0);
            assertThat(sampler.getContext().loopCounter).isEqualTo(2);

            sampler.skipRows(32);

            assertThat(sampler.getSequenceIndex()).isEqualTo(8);
            assertThat(sampler.getRowIndex()).isEqualTo(0);
            assertThat(sampler.getTickIndex()).isEqualTo(0);
            assertThat(sampler.getSampleIndex()).isEqualTo(0);

            assertThat(sampler.getContext().loopPending).isEqualTo(false);
            assertThat(sampler.getContext().loopRowIndex).isEqualTo(0);
            assertThat(sampler.getContext().loopCounter).isEqualTo(1);

            sampler.skipRows(32);

            assertThat(sampler.getSequenceIndex()).isEqualTo(8);
            assertThat(sampler.getRowIndex()).isEqualTo(32);
            assertThat(sampler.getTickIndex()).isEqualTo(0);
            assertThat(sampler.getSampleIndex()).isEqualTo(0);

            assertThat(sampler.getContext().loopPending).isEqualTo(false);
            assertThat(sampler.getContext().loopRowIndex).isEqualTo(0);
            assertThat(sampler.getContext().loopCounter).isEqualTo(0);
        }

        // should do the same even if we advance in the middle of the loop

        for (int i = 0; i < 2; i++) {
            sampler.seekSequence(8, 1);

            assertThat(sampler.getContext().loopPending).isEqualTo(false);
            assertThat(sampler.getContext().loopRowIndex).isEqualTo(0);
            assertThat(sampler.getContext().loopCounter).isEqualTo(0);

            sampler.skipRows(32);

            assertThat(sampler.getSequenceIndex()).isEqualTo(8);
            assertThat(sampler.getRowIndex()).isEqualTo(1);
            assertThat(sampler.getTickIndex()).isEqualTo(0);
            assertThat(sampler.getSampleIndex()).isEqualTo(0);

            assertThat(sampler.getContext().loopPending).isEqualTo(false);
            assertThat(sampler.getContext().loopRowIndex).isEqualTo(0);
            assertThat(sampler.getContext().loopCounter).isEqualTo(2);

            sampler.skipRows(32);

            assertThat(sampler.getSequenceIndex()).isEqualTo(8);
            assertThat(sampler.getRowIndex()).isEqualTo(1);
            assertThat(sampler.getTickIndex()).isEqualTo(0);
            assertThat(sampler.getSampleIndex()).isEqualTo(0);

            assertThat(sampler.getContext().loopPending).isEqualTo(false);
            assertThat(sampler.getContext().loopRowIndex).isEqualTo(0);
            assertThat(sampler.getContext().loopCounter).isEqualTo(1);

            sampler.skipRows(32);

            assertThat(sampler.getSequenceIndex()).isEqualTo(8);
            assertThat(sampler.getRowIndex()).isEqualTo(33);
            assertThat(sampler.getTickIndex()).isEqualTo(0);
            assertThat(sampler.getSampleIndex()).isEqualTo(0);

            assertThat(sampler.getContext().loopPending).isEqualTo(false);
            assertThat(sampler.getContext().loopRowIndex).isEqualTo(0);
            assertThat(sampler.getContext().loopCounter).isEqualTo(0);
        }
    }

    @Test
    void shouldLooseMidTickWhenSamplingRateIsChanged() throws IOException {
        Mod mod = modLoader.load("chip/other/euroremix.mod");
        underTest.updateMod(mod);

        assertThat(underTest.getBytesPerRow()).isEqualTo(23040);
        assertThat(underTest.getBytesPerTick()).isEqualTo(3840);
        assertThat(underTest.getBytesPerSample()).isEqualTo(4);

        byte[] buffer = new byte[underTest.getBytesPerRow() - underTest.getBytesPerSample()];
        int readCount = underTest.read(buffer);

        assertThat(readCount).isEqualTo(buffer.length);
        assertThat(underTest.getSequenceIndex()).isEqualTo(0);
        assertThat(underTest.getRowIndex()).isEqualTo(1);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(959);

        underTest.setSamplingRate(41_000);

        assertThat(readCount).isEqualTo(buffer.length);
        assertThat(underTest.getSequenceIndex()).isEqualTo(0);
        assertThat(underTest.getRowIndex()).isEqualTo(1);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);

        assertThat(underTest.getBytesPerRow()).isEqualTo(16400);
        assertThat(underTest.getBytesPerTick()).isEqualTo(3280);
        assertThat(underTest.getBytesPerSample()).isEqualTo(4);
    }

    @Test
    void shouldLooseMidTickWhenClockIsChanged() throws IOException {
        Mod mod = modLoader.load("chip/other/euroremix.mod");
        underTest.updateMod(mod);

        assertThat(underTest.getBytesPerRow()).isEqualTo(23040);
        assertThat(underTest.getBytesPerTick()).isEqualTo(3840);
        assertThat(underTest.getBytesPerSample()).isEqualTo(4);

        byte[] buffer = new byte[underTest.getBytesPerRow() - underTest.getBytesPerSample()];
        int readCount = underTest.read(buffer);

        assertThat(readCount).isEqualTo(buffer.length);
        assertThat(underTest.getSequenceIndex()).isEqualTo(0);
        assertThat(underTest.getRowIndex()).isEqualTo(1);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(959);

        underTest.setClockHz(Mods.NTSC_CLOCK_HZ);

        assertThat(readCount).isEqualTo(buffer.length);
        assertThat(underTest.getSequenceIndex()).isEqualTo(0);
        assertThat(underTest.getRowIndex()).isEqualTo(1);
        assertThat(underTest.getTickIndex()).isEqualTo(0);
        assertThat(underTest.getSampleIndex()).isEqualTo(0);

        assertThat(underTest.getBytesPerRow()).isEqualTo(19200);
        assertThat(underTest.getBytesPerTick()).isEqualTo(3840);
        assertThat(underTest.getBytesPerSample()).isEqualTo(4);
    }

    @Test
    void shouldPermanentlyDestroySampleData() throws IOException {
        Mod mod = modLoader.load("chip/other/euroremix.mod");

        Sample sample = mod.getSample(9);
        assertThat(sample.getName()).isEqualTo("productions, but");

        byte[] subData = sample.copyOfData(40, 6);
        assertThat(subData).containsExactly(new byte[]{-11, -11, -18, 17, 26, 26});

        underTest.updateMod(mod);

        subData = sample.copyOfData(40, 6);
        assertThat(subData).containsExactly(new byte[]{-11, -11, -18, 17, 26, 26});

        underTest.seekSequence(3, 1);
        underTest.skipRows(1);

        subData = sample.copyOfData(40, 6);
        assertThat(subData).containsExactly(new byte[]{-11, 11, 18, -17, -26, 26});

        underTest.seekSequence(3, 1);
        underTest.skipRows(1);

        subData = sample.copyOfData(40, 6);
        assertThat(subData).containsExactly(new byte[]{-11, -11, -18, 17, 26, 26});
    }

    @Test
    void shouldBuildIndexWithAllEffects() throws IOException {
        Mod mod = modLoader.load("chip/DJ Metune - Axel F.mod");

        for (int i = 0; i < Mods.EFFECT_COUNT; i++) {
            underTest.setEffectEnabled(EffectType.valueOf(i), false);
            underTest.setExtendedEffectEnabled(ExtendedEffectType.valueOf(i), false);
        }

        // should re-enable all effects except invert loop when rebuilding index
        underTest.updateMod(mod);

        // all effects are still disabled
        for (int i = 0; i < Mods.EFFECT_COUNT; i++) {
            assertThat(underTest.isEffectEnabled(EffectType.valueOf(i))).isFalse();
            assertThat(underTest.isExtendedEffectEnabled(ExtendedEffectType.valueOf(i))).isFalse();
        }

        // tempo / speed at row 0 (default)
        assertThat(underTest.getContext().speed).isEqualTo(6);
        assertThat(underTest.getContext().tempo).isEqualTo(125);

        underTest.seekSequence(0, 1);

        // index was rebuild with SET_SPEED effect enabled (even when explicitely disabled) so the valid tempo was set
        assertThat(underTest.getContext().speed).isEqualTo(6);
        assertThat(underTest.getContext().tempo).isEqualTo(116);
    }
}
