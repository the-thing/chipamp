package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.common.Formatters;
import com.github.thething.chipgroove.common.Maths;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;

import static com.github.thething.chipgroove.common.Requirements.requireInRange;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.checkFromToIndex;
import static java.util.Objects.requireNonNull;

// TODO add a global volume multiplier
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
    private int samplesRemainingInCurrentTick; // TODO this culd be sample index (we have to test speed change)

    public Player() {
        this.channels = new Channel[CHANNEL_COUNT];
        this.channels[0] = new Channel(true);
        this.channels[1] = new Channel(false);
        this.channels[2] = new Channel(false);
        this.channels[3] = new Channel(true);
        this.channels[4] = new Channel(true);
        this.channels[5] = new Channel(false);
        this.channels[6] = new Channel(false);
        this.channels[7] = new Channel(true);

        this.config = new Config(CHANNEL_COUNT);
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

    public void changePositionSequence(int sequenceIndex) {
        requireNonNull(mod);
        requireInRange(sequenceIndex, 0, mod.getLength() - 1);

        reset();

        // disable logging regardless of config setting, we don't want to log skipped patterns
        boolean logEnabled = config.logInfoEnabled;
        config.logInfoEnabled = false;

        try {
            while (this.patternSequenceIndex < sequenceIndex) {
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

        while (patternSequenceIndex < endSequenceIndex) {
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

        while (patternSequenceIndex >= startSequenceIndex && patternSequenceIndex < endSequenceIndex && length - offset >= bytesPerTick) {
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

            samplesRemainingInCurrentTick = context.samplesPerTick;
        }

        float left = 0.0f;
        float right = 0.0f;

        for (int i = 0; i < mod.getChannelCount(); i++) {
            // we increment the sample even if the channel is muted (need to push the sample position)
            float sample = channels[i].nextSample(mod);

            if (config.muted[i]) {
                continue;
            }

            if (channels[i].left) {
                left += sample;
            } else {
                right += sample;
            }
        }

        left = Maths.clamp(left, -1.0f, 1.0f);
        right = Maths.clamp(right, -1.0f, 1.0f);

        if (config.stereoFoldDownEnabled) {
            left = (left + right) * 0.5f;
            right = left;
        }

        short lefty = (short) (left * 32767.0f);
        short righty = (short) (right * 32767.0f);

        output[offset] = (byte) (lefty & 0xFF);
        output[offset + 1] = (byte) ((lefty >> 8) & 0xFF);
        output[offset + 2] = (byte) (righty & 0xFF);
        output[offset + 3] = (byte) ((righty >> 8) & 0xFF);

        samplesRemainingInCurrentTick--;
    }

    private void advanceRow() {
        if (context.jumpPending && context.breakPending) {
            // both position jump and pattern break effects are pending
            patternSequenceIndex = context.jumpSequenceIndex;
            rowIndex = context.breakRowIndex;
        } else if (context.jumpPending) {
            // jump to row 0 of specific pattern
            patternSequenceIndex = context.jumpSequenceIndex;
            rowIndex = 0;
        } else if (context.breakPending) {
            // jump to next pattern's specific row
            patternSequenceIndex = patternSequenceIndex + 1;
            rowIndex = context.breakRowIndex;
        } else {
            // advance single row
            rowIndex++;

            if (rowIndex >= 64) {
                rowIndex = 0;
                patternSequenceIndex++;
            }
        }

        context.jumpPending = false;
        context.jumpSequenceIndex = 0;
        context.breakPending = false;
        context.breakRowIndex = 0;
    }

    private void handleNewRow(Mod mod, int clockHz, int samplingRate) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            int patternIndex = mod.getPatternIndex(patternSequenceIndex);
            Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
            Channel channel = channels[channelIndex];

            Sample sample = instrument.sampleNumber() > 0 ? mod.getSample(instrument.sampleNumber() - 1) : null;
            int period = instrument.period();

            if (sample != null) {
                channel.sampleNumber = instrument.sampleNumber();
                channel.volume = sample.volume();
            }

            if (period > 0) {
                Sample activeSample = channel.sampleNumber > 0 ? mod.getSample(channel.sampleNumber - 1) : null;

                if (activeSample != null && activeSample.fineTune() != 0) {
                    period = ModTables.getFineTunePeriod(period, activeSample.fineTune());
                }

                boolean portamento = instrument.effectType() == EffectType.TONE_PORTAMENTO ||
                        instrument.effectType() == EffectType.TONE_PORTAMENTO_WITH_VOLUME_SLIDE;

                if (portamento) {
                    // for portamento, we only set target
                    channel.portamentoTargetPeriod = period;
                } else if (activeSample != null) {
                    channel.updatePeriod(period, clockHz, samplingRate);
                    channel.samplePosition = 0.0;
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
        // TODO decided what to do with effects that modify global state

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

    public void setClockHz(int clockHz) {
        this.config.clockHz = clockHz;
        // TODO this needs to trigger updates
    }

    public void setSamplingRate(int samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("samplingRate must be greater than zero");
        }

        config.samplingRate = samplingRate;
        // TODO this needs to trigger updates for current states
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

    public void setMuted(int channelIndex, boolean muted) {
        config.muted[channelIndex] = muted;
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
        Mod mod = modLoader.load("Captain - Space Debris.mod");

        Player player = new Player();
        player.setStereoFoldDownEnabled(true);
        player.setLogInfoEnabled(true);
        player.setLogErrorEnabled(true);
        player.setMod(mod);
        player.setMuted(0, true);
        player.setMuted(1, true);
        // player.setMuted(2, true);
        player.setMuted(3, true);

        player.changePositionSequence(0);
        player.play(1);
        // player.play();

        // TODO add suport for dynamic array
//        byte[] buffer = new byte[1024 * 1024 * 1024];
//        AudioFormat format = player.getCompatibleAudioFormat();
//        int readCount = player.read(buffer);
//        Resources.saveAudio(new File("space debris.wav"), format, buffer, 0, readCount);
    }
}
