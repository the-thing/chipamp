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

import static com.github.thething.chipgroove.common.Requirements.requireInRange;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.requireNonNull;

// TODO add volume mulitplier
public final class Player {

    public static final int PAL_CLOCK_HZ = 3_546_895;
    public static final int NTSC_CLOCK_HZ = 3_579_545;

    private static final int CHANNEL_COUNT = 8;
    private static final byte[] TMP_BUFFER = new byte[4];

    private final Channel[] channels;
    private final Config config;
    private final Context context;

    private Mod mod;

    private int patternSequenceIndex;
    private int rowIndex;
    private int tickIndex;
    private int samplesRemainingInCurrentTick;

    public Player() {
        this.channels = new Channel[CHANNEL_COUNT];

        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channels[i] = new Channel();
        }

        this.config = new Config();
        this.context = new Context(config.samplingRate);

        samplesRemainingInCurrentTick = context.samplesPerTick;
    }

    private void reset() {
        context.reset(config.samplingRate);
        samplesRemainingInCurrentTick = context.samplesPerTick;

        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channels[i].reset();
        }
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
            // TODO disable logging regardless of setting when advancing patterns
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
        return config.stereo ? 4 : 2;
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

                if (config.logRowEnabled) {
                    config.logger.println(Formatters.formatRow(mod, patternIndex, rowIndex));
                }

                handleNewRow(mod, config.clockHz, config.samplingRate);
            } else {
                applyMidRowEffects();
            }

            tickIndex++;

            if (tickIndex >= context.speed) {
                tickIndex = 0;
                advanceRow(mod);
            }

            samplesRemainingInCurrentTick = context.samplesPerTick;
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
        if (context.jumpPending || context.breakPending) {
            // Resolve Bxx + Dxx interaction (ProTracker rule):
            // Bxx alone  → jump to order jumpOrder, row 0
            // Dxx alone  → jump to orderPos+1, row breakRow
            // Both       → jump to order jumpOrder, row breakRow

            if (context.jumpPending && !context.breakPending) {
                patternSequenceIndex = context.jumpOrder;
                rowIndex = 0;
            } else if (context.breakPending && !context.jumpPending) {
                patternSequenceIndex = Math.min(patternSequenceIndex + 1, mod.getLength() - 1);
                rowIndex = context.breakRow;
            } else {
                // Both: Bxx sets order, Dxx sets row
                patternSequenceIndex = context.jumpOrder;
                rowIndex = context.breakRow;
            }

            context.jumpPending = false;
            context.breakPending = false;
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

            // copy data to previous fields before applying new changes
            channel.previousPeriod = channel.period;
            channel.previousSamplePosition = channel.samplePosition;
            channel.previousEffectType = channel.effectType;
            channel.previousExtendedEffectType = channel.extendedEffectType;
            channel.previousEffectArgumentX = channel.effectArgumentX;
            channel.previousEffectArgumentY = channel.effectArgumentY;

            Sample sample = instrument.sampleNumber() > 0 ? mod.getSample(instrument.sampleNumber() - 1) : null;

            // TODO this needs to be split per sample and instrument
            if (sample != null && instrument.period() > 0) {
                channel.updatePeriod(instrument.period(), clockHz, samplingRate);
                channel.sampleNumber = instrument.sampleNumber();
                channel.samplePosition = 0.0;
                channel.volume = sample.getVolume();
                channel.tremoloPosition = 0;
            }

            if (sample != null) {
                channel.volume = sample.getVolume();
            }

            channel.effectType = instrument.effectType();
            channel.extendedEffectType = instrument.extendedEffectType();
            channel.effectArgumentX = instrument.effectArgumentX();
            channel.effectArgumentY = instrument.effectArgumentY();
        }

        applyNewRowEffects();
    }

    private void applyNewRowEffects() {
        // TODO effects that affect global channels should be taken from highest channel

        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Channel channel = channels[channelIndex];
            channel.effectType.onNewRow(channel, context, config);
        }
    }

    private void applyMidRowEffects() {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Channel channel = channels[channelIndex];
            channel.effectType.onMidRow(channel, context, config);
        }
    }

    public int getSamplingRate() {
        return config.samplingRate;
    }

    public void setSamplingRate(int samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("samplingRate must be greater than zero");
        }

        config.samplingRate = samplingRate;
    }

    public void setMuted(int channelIndex, boolean muted) {
        channels[channelIndex].muted = muted;
    }

    public AudioFormat getCompatibleAudioFormat() {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, config.samplingRate, 16,
                config.stereo ? 2 : 1, 4, config.samplingRate, false);
    }

    public static void main(String[] args) throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader();
        Mod mod = modLoader.load("Hoffman - Eon.mod");

        Player player = new Player();
        player.setMod(mod);
          player.changePositionToPattern(4);
//        player.setMuted(0, true);
//        player.setMuted(1, false);
//        player.setMuted(2, true);
//        player.setMuted(3, true);
        player.play();

        byte[] buffer = new byte[1024 * 1024 * 1024];

        AudioFormat format = player.getCompatibleAudioFormat();
        int readCount = player.read(buffer);

        System.out.println(readCount);

        Resources.saveAudio(new File("axel.wav"), format, buffer, 0, readCount);
    }
}
