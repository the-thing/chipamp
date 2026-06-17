package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.common.Formatters;
import com.github.thething.chipgroove.common.Maths;
import com.github.thething.chipgroove.io.Resources;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import static com.github.thething.chipgroove.common.Requirements.requireInRange;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.requireNonNull;

// TODO add volume mulitplier
public final class Player {

    public static final int PAL_CLOCK_HZ = 3_546_895;
    public static final int NTSC_CLOCK_HZ = 3_579_545;

    private static final int CHANNEL_COUNT = 8;
    private static final byte[] TMP_BUFFER = new byte[4];

    private static final PrintStream DEFAULT_LOG_STREAM = System.out;
    private static final boolean DEFAULT_LOG_ROW_ENABLED = true;
    private static final int DEFAULT_SPEED = 6;
    private static final int DEFAULT_TEMPO = 126;
    private static final int DEFAULT_SAMPLING_RATE = 44_100;
    private static final boolean DEFAULT_STEREO = true;

    private final Channel[] channels;

    private Mod mod;

    private PrintStream logStream;
    private boolean logRowEnabled;

    private int outputSamplingRate;
    private boolean outputStereo;

    private int clockHz;
    private int speed; // ticks per row
    private int tempo; // beats per minute
    private int samplesPerTick;
    private int samplesRemainingInCurrentTick;

    private boolean jumpPending = false;
    private int jumpOrder = 0;
    private boolean breakPending = false;
    private int breakRow = 0;

    private int patternSequenceIndex;
    private int rowIndex;
    private int tickIndex;

    public Player() {
        this.channels = new Channel[CHANNEL_COUNT];

        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channels[i] = new Channel();
        }

        logStream = DEFAULT_LOG_STREAM;
        logRowEnabled = DEFAULT_LOG_ROW_ENABLED;
        outputSamplingRate = DEFAULT_SAMPLING_RATE;
        outputStereo = DEFAULT_STEREO;
        clockHz = PAL_CLOCK_HZ;

        reset();
    }

    private void reset() {
        speed = DEFAULT_SPEED;
        tempo = DEFAULT_TEMPO;
        samplesPerTick = samplesPerTick(tempo, outputSamplingRate);
        samplesRemainingInCurrentTick = samplesPerTick;
        resetChannels();
    }

    private void resetChannels() {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channels[i].reset();
        }
    }

    /**
     * Compute the length of one CIA tick in output samples.
     * <p>
     * ProTracker CIA formula: tickDuration (µs) = 2_500_000 / BPM
     * <p>
     * Converted to output samples: samplesPerTick = outputRate * tickDuration_µs / 1_000_000 = outputRate * 2_500_000 /
     * (BPM * 1_000_000) = outputRate * 2.5 / BPM
     * <p>
     */
    private static int samplesPerTick(int tempo, int outputRate) {
        return (int) Math.round((double) outputRate * 2_500_000.0 / (tempo * 1_000_000.0));
    }

    public void setMod(Mod mod) {
        this.mod = mod;
        reset();
    }

    public void changePositionToSequence(int patternSequenceIndex) {
        requireNonNull(mod);
        requireInRange(patternSequenceIndex, 0, mod.getLength() - 1);

        reset();

        while (this.patternSequenceIndex < patternSequenceIndex) {
            int readCount = read(TMP_BUFFER);

            if (readCount <= 0) {
                throw new RuntimeException("Unexpected end of audio");
            }
        }
    }

    public void changePositionToPattern(int patternIndex) {
        requireNonNull(mod);
        requireInRange(patternIndex, 0, mod.getPatternCount());

        int sequenceIndex = 0;

        while (sequenceIndex < mod.getPatternSequenceCount()) {
            if (mod.getPatternIndex(sequenceIndex) == patternIndex) {
                break;
            }

            sequenceIndex++;
        }

        changePositionToSequence(sequenceIndex);
    }

    public int getBytesPerTick() {
        return outputStereo ? 4 : 2;
    }

    public void play() throws LineUnavailableException {
        byte[] buffer = new byte[4];

        AudioFormat format = getCompatibleAudioFormat();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        int readCount;

        while ((readCount = read(buffer)) > 0) {
            line.write(buffer, 0, readCount);
        }

        line.drain();
        line.close();
    }

    public int read(byte[] output) {
        return read(output, 0, output.length);
    }

    public int read(byte[] output, int offset, int length) {
        checkFromIndexSize(offset, length, output.length);

        int bytesPerTick = getBytesPerTick();

        if (length < bytesPerTick) {
            throw new IllegalArgumentException("Buffer too small: " + length + " < " + bytesPerTick);
        }

        int readCount = 0;

        while (patternSequenceIndex < mod.getLength() && length - offset >= bytesPerTick) {
            tick(output, offset);
            readCount += bytesPerTick;
            offset += bytesPerTick;
        }

        return readCount;
    }

    private void tick(byte[] output, int offset) {
        if (samplesRemainingInCurrentTick <= 0) {
            if (tickIndex == 0) {
                int patternIndex = mod.getPatternIndex(patternSequenceIndex);

                if (logRowEnabled) {
                    logStream.print(Formatters.formatRow(mod, patternIndex, rowIndex));

                    logStream.print(' ');
                    // logStream.print(Formatters.formatEffects(mod, patternIndex, rowIndex));
                    logStream.print(mod.getInstrument(patternIndex, rowIndex, 3).effect());
                    logStream.print(' ');
                    logStream.println();
                }

                handleNewRow(mod, clockHz, outputSamplingRate);
            } else {
                applyMidRowEffects(mod);
            }

            tickIndex++;

            if (tickIndex >= speed) {
                tickIndex = 0;
                advanceRow(mod);
            }

            samplesRemainingInCurrentTick = samplesPerTick;
        }

        float left = 0.0f;
        float right = 0.0f;

        if (!channels[0].muted) {
            left += channels[0].nextSample(mod);
        }

        if (!channels[3].muted) {
            left += channels[3].nextSample(mod);
        }

        if (!channels[1].muted) {
            right += channels[1].nextSample(mod);
        }

        if (!channels[2].muted) {
            right += channels[2].nextSample(mod);
        }

        left = Maths.clamp(left, -1.0f, 1.0f);
        right = Maths.clamp(right, -1.0f, 1.0f);

        short lefty = (short) (left * 32767.0f);
        short righty = (short) (right * 32767.0f);

        output[offset] = (byte) (lefty & 0xFF);
        output[offset + 1] = (byte) ((lefty >> 8) & 0xFF);
        output[offset + 2] = (byte) (righty & 0xFF);
        output[offset + 3] = (byte) ((righty >> 8) & 0xFF);

        samplesRemainingInCurrentTick--;
    }

    private void advanceRow(Mod mod) {
        if (jumpPending || breakPending) {
            // Resolve Bxx + Dxx interaction (ProTracker rule):
            // Bxx alone  → jump to order jumpOrder, row 0
            // Dxx alone  → jump to orderPos+1, row breakRow
            // Both       → jump to order jumpOrder, row breakRow

            if (jumpPending && !breakPending) {
                patternSequenceIndex = jumpOrder;
                rowIndex = 0;
            } else if (breakPending && !jumpPending) {
                patternSequenceIndex = Math.min(patternSequenceIndex + 1, mod.getLength() - 1);
                rowIndex = breakRow;
            } else {
                // Both: Bxx sets order, Dxx sets row
                patternSequenceIndex = jumpOrder;
                rowIndex = breakRow;
            }

            jumpPending = false;
            breakPending = false;
        } else {
            rowIndex++;

            if (rowIndex >= 64) {
                rowIndex = 0;
                patternSequenceIndex++;
            }

            if (patternSequenceIndex < mod.getLength()) {
                int patternIndex = mod.getPatternIndex(patternSequenceIndex);
            }
        }
    }

    private void handleNewRow(Mod mod, int clockHz, int samplingRate) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            int patternIndex = mod.getPatternIndex(patternSequenceIndex);
            Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
            Channel channel = channels[channelIndex];
            Sample sample = instrument.sampleNumber() > 0 ? mod.getSample(instrument.sampleNumber() - 1) : null;
            int previousSampleNumber = channel.sampleNumber;

            if (sample != null && instrument.period() > 0) {
                channel.sampleNumber = instrument.sampleNumber();
                channel.volume = sample.getVolume();
                channel.tremoloPosition = 0;

                // TODO write comment
                if (instrument.effect() != Effect.TONE_PORTAMENTO) {
                    channel.samplePosition = 0.0;
                    channel.updatePeriod(instrument.period(), clockHz, samplingRate);
                }
            }

            if (sample != null) {
                channel.volume = sample.getVolume();
            }

            applyNewRowEffects(channel, instrument, sample, previousSampleNumber);
        }
    }

    private void applyNewRowEffects(Channel channel, Instrument instrument, Sample sample, int previousSampleNumber) {
        Effect previousEffect = channel.effect;
        int prevEffectArgumentX = channel.effectArgumentX;
        int prevEffectArgumentY = channel.effectArgumentY;

        channel.effect = instrument.effect();
        channel.extendedEffect = instrument.extendedEffect();
        channel.effectArgumentX = instrument.effectArgumentX();
        channel.effectArgumentY = instrument.effectArgumentY();

        switch (instrument.effect()) {

            case ARPEGGIO -> {
                //  TODO
                System.out.println("UNKNOWN EFFECT: " + instrument.effect());
            }

            case SLIDE_UP, SLIDE_DOWN -> {
                // handled mid-row
            }

            case TONE_PORTAMENTO -> effectTonePortamentoNewRow(channel, instrument);

            case VIBRATO -> {
                // TODO
                System.out.println("UNKNOWN EFFECT: " + instrument.effect());
            }

            case TONE_PORTAMENTO_WITH_VOLUME_SLIDE -> {
                // TODO
                System.out.println("UNKNOWN EFFECT: " + instrument.effect());
            }

            case VIBRATO_WITH_VOLUME_SLIDE -> {
                // TODO
                System.out.println("UNKNOWN EFFECT: " + instrument.effect());
            }

            case TREMOLO -> effectTremoloNewRow(channel);

            case SET_PANNING_POSITION -> {
                // TODO
                System.out.println("UNKNOWN EFFECT: " + instrument.effect());
            }

            case SET_SAMPLE_OFFSET -> effectSetSampleOffset(channel, sample, instrument, previousSampleNumber,
                    clockHz, outputSamplingRate, instrument.effectArgumentX(), instrument.effectArgumentY());

            case VOLUME_SLIDE ->
                    effectVolumeSlideNewRow(channel, previousEffect, prevEffectArgumentX, prevEffectArgumentY, instrument.effectArgumentX(), instrument.effectArgumentY());

            case POSITION_JUMP -> {
                // TODO
                System.out.println("UNKNOWN EFFECT: " + instrument.effect());
            }

            case SET_VOLUME -> effectSetVolume(channel, instrument.effectArgumentX(), instrument.effectArgumentY());

            case PATTERN_BREAK -> effectPatternBreak(instrument.effectArgumentX(), instrument.effectArgumentY());

            case EXTENDED_EFFECT -> applyExtendedEffects(channel, instrument);

            case SET_SPEED -> effectSetSpeed(instrument.effectArgumentX(), instrument.effectArgumentY());
        }
    }

    private void applyExtendedEffects(Channel channel, Instrument instrument) {
        switch (instrument.extendedEffect()) {

            case SET_FILTER -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case FINE_SLIDE_UP -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case FINE_SLIDE_DOWN -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case SET_GLISSANDO -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case SET_VIBRATO_WAVEFORM -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case SET_FINE_TUNE_VALUE -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case LOOP_PATTERN -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case SET_TREMOLO_WAVEFORM -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case ROUGH_PANNING -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case RETRIGGER_SAMPLE -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case FINE_VOLUME_SLIDE_UP -> effectFineVolumeSlideUp(channel, instrument.effectArgumentY());
            case FINE_VOLUME_SLIDE_DOWN -> effectFineVolumeSlideDown(channel, instrument.effectArgumentY());

            case CUT_SAMPLE -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case DELAY_SAMPLE -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case DELAY_PATTERN -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }

            case INVERT_LOOP -> {
                //TODO
                System.out.println("Unknown extended effect: " + instrument.extendedEffect());
            }
        }
    }

    private void applyMidRowEffects(Mod mod) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Channel channel = channels[channelIndex];

            switch (channel.effect) {
                case SLIDE_UP -> effectSlideUp(channel);
                case SLIDE_DOWN -> effectSlideDown(channel);
                case TONE_PORTAMENTO -> effectTonePortamento(channel);
                case TREMOLO -> effectTremolo(channel);
                case VOLUME_SLIDE -> effectVolumeSlide(channel);
            }
        }
    }

    // TODO min / max might be configurable
    private void effectSlideUp(Channel channel) {
        int adjustment = (channel.effectArgumentX << 4) | channel.effectArgumentY;
        int newPeriod = Maths.clamp(channel.getPeriod() - adjustment, 113, 856);
        channel.updatePeriod(newPeriod, clockHz, outputSamplingRate);
    }

    // TODO min / max might be configurable
    private void effectSlideDown(Channel channel) {
        int adjustment = (channel.effectArgumentX << 4) | channel.effectArgumentY;
        int newPeriod = Maths.clamp(channel.getPeriod() + adjustment, 113, 856);
        channel.updatePeriod(newPeriod, clockHz, outputSamplingRate);
    }

    private void effectTonePortamentoNewRow(Channel channel, Instrument instrument) {
        channel.maxPeriod = instrument.period();
    }

    private void effectTonePortamento(Channel channel) {
        int periodIncrement = (channel.effectArgumentX << 4) | channel.effectArgumentY;
        int newPeriod = Maths.clamp(channel.getPeriod() + periodIncrement, 113, Math.min(channel.maxPeriod, 856));
        channel.updatePeriod(newPeriod, clockHz, outputSamplingRate);
    }

    private static void effectTremoloNewRow(Channel channel) {
        // use old tremolo speed if not specified
        if (channel.effectArgumentX != 0) {
            channel.tremoloSpeed = channel.effectArgumentX;
        }

        // use old tremolo depth if not specified
        if (channel.effectArgumentY != 0) {
            channel.tremoloDepth = channel.effectArgumentY;
        }

        channel.tremoloVolume = channel.volume;
    }

    private static void effectTremolo(Channel channel) {
        WaveformType tremoloWaveformType = channel.tremoloWaveformType;
        int waveformValue = ModTables.getWaveformValue(tremoloWaveformType, channel.tremoloPosition);
        int delta = (channel.tremoloDepth * waveformValue) / 64;

        channel.volume = Maths.clamp(channel.volume + delta, 0, 64);
        channel.tremoloPosition += channel.tremoloSpeed;
    }

    /**
     * Set sample offset. According to some documents when effect is provided with no sample it is supposed to retrigger
     * last sample.
     */
    private void effectSetSampleOffset(
            Channel channel, Sample sample, Instrument instrument, int previousSampleNumber,
            int clockHz, int samplingRate, int argX, int argY) {
        int offset = ((argX << 4) | argY) * 256;

        // System.out.println("SET SAMPLE OFFSET " + offset + " / " + sample.getDataLength());
        channel.samplePosition = offset;

        if (sample == null) {
            System.out.println("No previous sample during SET_SAMPLE_OFFSET. Retrigger last sample");
            channel.sampleNumber = previousSampleNumber;
            channel.updatePeriod(instrument.period(), clockHz, samplingRate);
        }
    }

    /**
     * If the current effect is A00 (volume slide with both arguments equal to zero) and the previous effect was a
     * volume slide than inherit arguments from previous slide.
     * <p>
     * // TODO find source of this comment
     */
    private void effectVolumeSlideNewRow(Channel channel, Effect prevEffect, int prevArgX, int prevArgY, int argX, int argY) {
        if (argX == 0 && argY == 0 && prevEffect == Effect.VOLUME_SLIDE) {
            channel.effectArgumentX = prevArgX;
            channel.effectArgumentY = prevArgY;
        }
    }

    private void effectVolumeSlide(Channel channel) {
        int delta;

        if (channel.effectArgumentX != 0) {
            delta = channel.effectArgumentX;
        } else {
            delta = -channel.effectArgumentY;
        }

        channel.volume = Maths.clamp(channel.volume + delta, 0, 64);
    }

    private void effectSetVolume(Channel channel, int argX, int argY) {
        int arg = (argX << 4) | argY;
        arg = Math.min(64, arg);
        channel.volume = arg;
    }

    private void effectPatternBreak(int argX, int argY) {
        int arg = argX * 10 + argY;
        Math.min(arg, 63);
        breakPending = true;
        breakRow = arg;
    }

    // TODO speed at zero should probably stop playing
    private void effectSetSpeed(int argX, int argY) {
        int arg = (argX << 4) | argY;

        if (arg < 0x20) {
            speed = arg;
        } else {
            tempo = arg;
            samplesPerTick = samplesPerTick(tempo, outputSamplingRate);
        }
    }

    private void effectFineVolumeSlideUp(Channel channel, int argY) {
        channel.volume = Maths.clamp(channel.volume + argY, 0, 64);
    }

    private void effectFineVolumeSlideDown(Channel channel, int argY) {
        channel.volume = Maths.clamp(channel.volume - argY, 0, 64);
    }

    public int getOutputSamplingRate() {
        return outputSamplingRate;
    }

    public void setMuted(int channelIndex, boolean muted) {
        channels[channelIndex].muted = muted;
    }

    public AudioFormat getCompatibleAudioFormat() {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, outputSamplingRate, 16,
                outputStereo ? 2 : 1, 4, outputSamplingRate, false);
    }

    public static void main(String[] args) throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader();
        Mod mod = modLoader.load("DJ Metune - Axel F.mod");

        Player player = new Player();
        player.setMod(mod);
        player.changePositionToPattern(8);
//        player.setMuted(0, true);
//        player.setMuted(1, true);
//        player.setMuted(2, true);
//        player.setMuted(3, false);
        player.play();

        // AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, outputSamplingRate, 16,
        //                outputStereo ? 2 : 1, 4, outputSamplingRate, false);

        // ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);

        byte[] buffer = new byte[1024 * 1024 * 1024];

        AudioFormat format = player.getCompatibleAudioFormat();
        int readCount = player.read(buffer);

        System.out.println(readCount);

        Resources.saveAudio(new File("axel.wav"), format, buffer, 0, readCount);
    }
}
