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
import static java.util.Objects.checkFromToIndex;
import static java.util.Objects.requireNonNull;

public final class Player {

    private static final int CHANNEL_COUNT = 8;
    private static final byte[] TMP_BUFFER = new byte[4];

    private final Channel[] channels;
    private final Config config;
    private final Context context;

    private Mod mod;

    private int sequenceIndex;
    private int rowIndex;
    private int tickIndex;
    private int sampleIndex;

    public Player() {
        this.channels = new Channel[CHANNEL_COUNT];
        this.channels[0] = new Channel(false);
        this.channels[1] = new Channel(true);
        this.channels[2] = new Channel(true);
        this.channels[3] = new Channel(false);
        this.channels[4] = new Channel(false);
        this.channels[5] = new Channel(true);
        this.channels[6] = new Channel(true);
        this.channels[7] = new Channel(false);

        this.config = new Config(CHANNEL_COUNT);
        this.context = new Context(config.samplingRate);
    }

    private void reset() {
        context.reset(config.samplingRate);

        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channels[i].reset();
        }

        sequenceIndex = 0;
        rowIndex = 0;
        sampleIndex = 0;
    }

    public void setMod(Mod mod) {
        if (mod.getChannelCount() > CHANNEL_COUNT) {
            throw new IllegalArgumentException("Mod has more channels than the player supports");
        }

        this.mod = mod;
        reset();
    }

    public void changePositionSequence(int sequenceIndex) {
        requireNonNull(mod);
        requireInRange(sequenceIndex, 0, mod.getLength() - 1);

        reset();

        // disable logging regardless of config setting, we don't want to log skipped patterns
        boolean logEnabled = config.logInfoEnabled;
        config.logInfoEnabled = false;

        try {
            while (this.sequenceIndex < sequenceIndex) {
                int readCount = read(TMP_BUFFER);

                if (readCount <= 0) {
                    throw new RuntimeException("Unexpected end of audio");
                }
            }
        } finally {
            config.logInfoEnabled = logEnabled;
        }
    }

    public void changePositionPattern(int patternIndex) {
        requireNonNull(mod);
        requireInRange(patternIndex, 0, mod.getPatternCount());

        int sequenceIndex = 0;

        while (sequenceIndex < mod.getPatternSequenceCount()) {
            if (mod.getPatternIndex(sequenceIndex) == patternIndex) {
                break;
            }

            sequenceIndex++;
        }

        changePositionSequence(sequenceIndex);
    }

    public int getBytesPerTick() {
        return config.stereoEnabled ? 4 : 2;
    }

    public void play() throws LineUnavailableException {
        play(mod.getLength());
    }

    public void play(int endSequenceIndex) throws LineUnavailableException {
        checkFromToIndex(0, endSequenceIndex, mod.getLength());

        byte[] buffer = new byte[4];
        AudioFormat format = getCompatibleAudioFormat();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        while (sequenceIndex < endSequenceIndex) {
            int readCount = read(buffer);

            if (readCount <= 0) {
                throw new RuntimeException("Unexpected end of audio");
            }

            line.write(buffer, 0, readCount);
        }

        line.drain();
        line.close();
    }

    public int read(byte[] output) {
        return read(output, 0, output.length);
    }

    public int read(byte[] output, int offset, int length) {
        return read(output, offset, length, 0, mod.getLength());
    }

    public int read(byte[] output, int offset, int length, int startSequenceIndex, int endSequenceIndex) {
        checkFromIndexSize(offset, length, output.length);
        checkFromToIndex(startSequenceIndex, endSequenceIndex, mod.getLength());

        int bytesPerTick = getBytesPerTick();

        if (length < bytesPerTick) {
            throw new IllegalArgumentException("Buffer too small: " + length + " < " + bytesPerTick);
        }

        int readCount = 0;

        while (sequenceIndex >= startSequenceIndex && sequenceIndex < endSequenceIndex && length - offset >= bytesPerTick) {
            tick(output, offset);
            readCount += bytesPerTick;
            offset += bytesPerTick;
        }

        return readCount;
    }

    private void tick(byte[] output, int offset) {
        if (sampleIndex >= context.samplesPerTick) {
            if (tickIndex == 0) {
                int patternIndex = mod.getPatternIndex(sequenceIndex);

                if (config.logInfoEnabled && config.logErrorEnabled) {
                    config.logger.println(Formatters.formatRow(mod, patternIndex, rowIndex));
                }

                handleNewRow(mod, config.clockHz, config.samplingRate);
            } else {
                applyMidRowEffects();
            }

            tickIndex++;

            if (tickIndex >= context.speed) {
                tickIndex = 0;
                advanceRow();
            }

            sampleIndex = 0;
        }

        float left = 0.0f;
        float right = 0.0f;

        for (int i = 0; i < mod.getChannelCount(); i++) {
            // we increment the sample even if the channel is muted (need to push the sample position)
            float sample = channels[i].nextSample();

            if (config.muted[i]) {
                continue;
            }

            left += sample * channels[i].leftPanning;
            right += sample * channels[i].rightPanning;
        }

        left *= config.volumeMultiplier;
        right *= config.volumeMultiplier;

        left = Maths.clamp(left, -1.0f, 1.0f);
        right = Maths.clamp(right, -1.0f, 1.0f);

        if (config.stereoFoldDownEnabled) {
            left = (left + right) * 0.5f;
            right = left;
        }

        short lefty = (short) (left * 32767.0f);
        short righty = (short) (right * 32767.0f);

        if (config.stereoEnabled) {
            output[offset] = (byte) (lefty & 0xFF);
            output[offset + 1] = (byte) ((lefty >> 8) & 0xFF);
            output[offset + 2] = (byte) (righty & 0xFF);
            output[offset + 3] = (byte) ((righty >> 8) & 0xFF);
        } else {
            short mono = (short) ((lefty + righty) / 2);
            output[offset] = (byte) (mono & 0xFF);
            output[offset + 1] = (byte) ((mono >> 8) & 0xFF);
        }

        sampleIndex++;
    }

    private void advanceRow() {
        // make sure we don't loop forever and stop the song at any jump statement in the last pattern
        if (config.ignoreLastSequenceJumpStatementEnabled && context.jumpPending && sequenceIndex == mod.getLength() - 1) {
            context.jumpPending = false;
            context.jumpSequenceIndex = 0;
            context.breakPending = false;
            context.breakRowIndex = 0;

            // reset all channels
            for (int i = 0; i < mod.getChannelCount(); i++) {
                channels[i].reset();
            }

            // move pointers beyond the song
            sequenceIndex++;
            rowIndex = 0;

            return;
        }

        if (context.jumpPending && context.breakPending) {
            // both position jump and pattern break effects are pending
            sequenceIndex = context.jumpSequenceIndex;
            rowIndex = context.breakRowIndex;
        } else if (context.jumpPending) {
            // jump to row 0 of specific pattern
            sequenceIndex = context.jumpSequenceIndex;
            rowIndex = 0;
        } else if (context.breakPending) {
            // jump to next pattern's specific row
            sequenceIndex = sequenceIndex + 1;
            rowIndex = context.breakRowIndex;
        } else {
            // advance single row
            rowIndex++;

            if (rowIndex >= 64) {
                rowIndex = 0;
                sequenceIndex++;
            }
        }

        context.jumpPending = false;
        context.jumpSequenceIndex = 0;
        context.breakPending = false;
        context.breakRowIndex = 0;
    }

    private void handleNewRow(Mod mod, int clockHz, int samplingRate) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            int patternIndex = mod.getPatternIndex(sequenceIndex);
            Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
            Channel channel = channels[channelIndex];

            Sample sample = instrument.sampleNumber() > 0 ? mod.getSample(instrument.sampleNumber() - 1) : null;
            int period = instrument.period();

            if (sample != null) {
                channel.sample = sample;
                channel.volume = sample.volume();
            }

            if (period > 0) {
                Sample activeSample = channel.sample;

                if (activeSample != null && activeSample.fineTune() != 0) {
                    period = ModTables.getFineTunePeriod(period, activeSample.fineTune());
                }

                boolean portamento = instrument.effectType() == EffectType.TONE_PORTAMENTO ||
                        instrument.effectType() == EffectType.TONE_PORTAMENTO_WITH_VOLUME_SLIDE;

                if (portamento) {
                    // for portamento, we only set target period
                    channel.portamentoTargetPeriod = period;
                } else if (activeSample != null) {
                    channel.updatePeriodAndIncrement(period, clockHz, samplingRate);
                    channel.samplePosition = 0.0f;
                    channel.resetOnNewSampleWithPeriod();
                }
            }

            channel.effectType = instrument.effectType();
            channel.extendedEffectType = instrument.extendedEffectType();
            channel.effectArgumentX = instrument.effectArgumentX();
            channel.effectArgumentY = instrument.effectArgumentY();
        }

        applyNewRowEffects();
    }

    private void applyNewRowEffects() {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Channel channel = channels[channelIndex];

            if (channel.effectType == EffectType.NONE) {
                continue;
            }

            if (!config.effectEnabled[channel.effectType.getCode()]) {
                continue;
            }

            channel.effectType.onNewRow(channel, context, config);
        }
    }

    private void applyMidRowEffects() {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Channel channel = channels[channelIndex];
            channel.effectType.onMidRow(channel, context, config);
        }
    }

    public void setMuted(int channelIndex, boolean muted) {
        config.muted[channelIndex] = muted;
    }

    public void setEffectEnabled(EffectType effectType, boolean enabled) {
        if (effectType == null || effectType == EffectType.NONE) {
            throw new IllegalArgumentException("effectType must not be null or NONE");
        }

        config.effectEnabled[effectType.getCode()] = enabled;
    }

    public void setExtendedEffectEnabled(ExtendedEffectType extendedEffectType, boolean enabled) {
        if (extendedEffectType == null || extendedEffectType == ExtendedEffectType.NONE) {
            throw new IllegalArgumentException("extendedEffectType must not be null or NONE");
        }

        config.extendedEffectEnabled[extendedEffectType.getCode()] = enabled;
    }

    public void setClockHz(int clockHz) {
        if (clockHz <= 0) {
            throw new IllegalArgumentException("clockHz must be greater than zero");
        }

        this.config.clockHz = clockHz;
        recalculatePeriods();
    }

    public void setSamplingRate(int samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("samplingRate must be greater than zero");
        }

        config.samplingRate = samplingRate;
        recalculatePeriods();
    }

    private void recalculatePeriods() {
        if (mod != null) {
            for (int i = 0; i < mod.getChannelCount(); i++) {
                channels[i].updatePeriodAndIncrement(channels[i].period, config.clockHz, config.samplingRate);
            }
        }
    }

    public void setMinPeriod(int minPeriod) {
        if (minPeriod < 0) {
            throw new IllegalArgumentException("minPeriod must be greater than zero");
        }

        this.config.minPeriod = minPeriod;
    }

    public void setMaxPeriod(int maxPeriod) {
        if (maxPeriod < 0) {
            throw new IllegalArgumentException("maxPeriod must be greater than zero");
        }

        this.config.maxPeriod = maxPeriod;
    }

    public void setVolumeMultiplier(float volumeMultiplier) {
        if (volumeMultiplier < 0.0f) {
            throw new IllegalArgumentException("volumeMultiplier must be greater than or equal to zero");
        }

        config.volumeMultiplier = volumeMultiplier;
    }

    public void setStereoEnabled(boolean stereoEnabled) {
        this.config.stereoEnabled = stereoEnabled;
    }

    public void setStereoFoldDownEnabled(boolean stereoFoldDownEnabled) {
        this.config.stereoFoldDownEnabled = stereoFoldDownEnabled;
    }

    public void setVolumeSlideDeltaEnabled(boolean volumeSlideDeltaEnabled) {
        this.config.volumeSlideDeltaEnabled = volumeSlideDeltaEnabled;
    }

    public void setIgnoreLastSequenceJumpStatementEnabled(boolean ignoreLastSequenceJumpStatementEnabled) {
        this.config.ignoreLastSequenceJumpStatementEnabled = ignoreLastSequenceJumpStatementEnabled;
    }

    public void setLogger(PrintStream logger) {
        this.config.logger = requireNonNull(logger);
    }

    public void setLogInfoEnabled(boolean enabled) {
        this.config.logInfoEnabled = enabled;
    }

    public void setLogErrorEnabled(boolean enabled) {
        this.config.logErrorEnabled = enabled;
    }

    public AudioFormat getCompatibleAudioFormat() {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, config.samplingRate, 16,
                config.stereoEnabled ? 2 : 1, 4, config.samplingRate, false);
    }

    public static void main(String[] args) throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader();
        Mod mod = modLoader.load("Angelwings - 1995.mod");

        Player player = new Player();
        player.setStereoFoldDownEnabled(true);
        player.setLogInfoEnabled(true);
        player.setLogErrorEnabled(true);
        player.setMod(mod);
        // player.setMuted(0, true);
        // player.setMuted(1, true);
        // player.setMuted(2, true);
        // player.setMuted(3, true);

        player.setEffectEnabled(EffectType.ARPEGGIO, true);
        player.play();

        // TODO add support for dynamic array
//        byte[] buffer = new byte[1024 * 1024 * 100];
//        AudioFormat format = player.getCompatibleAudioFormat();
//        int readCount = player.read(buffer);
//        Resources.saveAudio(new File("Allister Brimble - Superfrog World 1.wav"), format, buffer, 0, readCount);
    }
}
