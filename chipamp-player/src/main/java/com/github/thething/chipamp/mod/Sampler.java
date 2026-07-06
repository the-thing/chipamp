package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Formatters;
import com.github.thething.chipamp.common.Maths;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.github.thething.chipamp.common.Requirements.requireInRange;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.checkFromToIndex;
import static java.util.Objects.checkIndex;
import static java.util.Objects.requireNonNull;

public final class Sampler {

    private static final State INITIAL_STATE = new State(0, 0, false, 0, 0);
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final byte[] EMPTY_BUFFER = new byte[0];
    private static final byte[] TMP_BUFFER = new byte[4];

    private final Channel[] channels;
    private final Config config;
    private final Context context;
    private final Set<State> visited;
    private final Channel[][] channelsBySequenceRow;
    private final Context[] contextBySequenceRow;

    private Mod mod;
    private int sampleCount;
    private int sequenceIndex;
    private int rowIndex;
    private int tickIndex;
    private int sampleIndex;

    public Sampler() {
        this.channels = new Channel[Mod.MAX_CHANNEL_COUNT];

        for (int i = 0; i < channels.length; i++) {
            boolean right = (i & 3) == 1 || (i & 3) == 2; // (LRRL) repeating pattern
            this.channels[i] = new Channel(right);
        }

        this.config = new Config(channels.length);
        this.context = new Context(config.samplingRate);
        this.visited = new HashSet<>();

        this.channelsBySequenceRow = new Channel[Mod.PATTERN_SEQUENCE_COUNT * Mod.ROW_COUNT][Mod.MAX_CHANNEL_COUNT];

        for (int i = 0; i < channelsBySequenceRow.length; i++) {
            for (int channelIndex = 0; channelIndex < channelsBySequenceRow[i].length; channelIndex++) {
                boolean right = (i & 3) == 1 || (i & 3) == 2; // (LRRL) repeating pattern
                this.channelsBySequenceRow[i][channelIndex] = new Channel(right);
            }
        }

        this.contextBySequenceRow = new Context[Mod.PATTERN_SEQUENCE_COUNT * Mod.ROW_COUNT];

        for (int i = 0; i < contextBySequenceRow.length; i++) {
            contextBySequenceRow[i] = new Context(config.samplingRate);
        }
    }

    public void reset() {
        context.reset(config.samplingRate);

        visited.clear();
        visited.add(INITIAL_STATE);

        for (int i = 0; i < channels.length; i++) {
            channels[i].reset();
        }

        sequenceIndex = 0;
        rowIndex = 0;
        tickIndex = 0;
        sampleIndex = context.samplesPerTick;
    }

    public void seek(int sequenceIndex) {
        seek(sequenceIndex, 0);
    }

    public void seek(int sequenceIndex, int rowIndex) {
        requireNonNull(mod);
        checkIndex(sequenceIndex, mod.getLength());
        checkIndex(rowIndex, Mod.ROW_COUNT);

        int index = sequenceIndex * Mod.ROW_COUNT + rowIndex;
        this.context.copyFrom(contextBySequenceRow[index]);

        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            this.channels[channelIndex].copyFrom(channelsBySequenceRow[index][channelIndex]);
        }

        if (sequenceIndex == 0 && rowIndex == 0) {
            reset();
        } else {
            this.sequenceIndex = sequenceIndex;
            this.rowIndex = rowIndex;
            this.tickIndex = 0;
            this.sampleIndex = 1;
        }
    }

    public int seekPattern(int patternIndex) {
        requireNonNull(mod);
        requireInRange(patternIndex, 0, mod.getLength());

        int sequenceIndex = Mods.findSequenceIndex(mod, patternIndex);

        if (sequenceIndex < 0) {
            return -1;
        }

        seek(sequenceIndex);

        return sequenceIndex;
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

    public int playPatterns(int patternCount) throws LineUnavailableException {
        AudioFormat format = getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        int playedPatternCount = playPatterns(line, patternCount);

        line.drain();
        line.close();

        return playedPatternCount;
    }

    public int playPatterns(SourceDataLine line, int patternCount) {
        requireNonNull(mod);

        if (patternCount < 0) {
            return 0;
        }

        byte[] buffer = new byte[4];
        int lastSequenceIndex = sequenceIndex;
        int playedPatternCount = 0;

        while (sequenceIndex < mod.getLength() && playedPatternCount < patternCount) {
            int readCount = read(buffer);

            if (readCount <= 0) {
                throw new RuntimeException("Unexpected end of audio");
            }

            line.write(buffer, 0, readCount);

            if (lastSequenceIndex != sequenceIndex) {
                playedPatternCount++;
                lastSequenceIndex = sequenceIndex;
            }
        }

        return playedPatternCount;
    }

    public int playRows(int rowCount) throws LineUnavailableException {
        AudioFormat format = getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        int playedRowCount = playRows(line, rowCount);

        line.drain();
        line.close();

        return playedRowCount;
    }

    public int playRows(SourceDataLine line, int rowCount) {
        requireNonNull(mod);

        if (rowCount < 0) {
            return 0;
        }

        byte[] buffer = new byte[4];

        int lastRow = rowIndex;
        int lastSequenceIndex = sequenceIndex;
        int playedRowCount = 0;

        while (sequenceIndex < mod.getLength() && playedRowCount < rowCount) {
            int readCount = read(buffer);

            if (readCount <= 0) {
                throw new RuntimeException("Unexpected end of audio");
            }

            int writeCount = line.write(buffer, 0, readCount);

            if (writeCount != readCount) {
                throw new RuntimeException("Written less audio data than expected");
            }

            if (rowIndex != lastRow || lastSequenceIndex != sequenceIndex) {
                playedRowCount++;
                lastRow = rowIndex;
                lastSequenceIndex = sequenceIndex;
            }
        }

        return playedRowCount;
    }

    public int skipPatterns(int patternCount) {
        requireNonNull(mod);

        if (patternCount <= 0) {
            return 0;
        }

        int skippedPatternCount = 0;
        int lastSequenceIndex = sequenceIndex;

        boolean logInfoEnabled = config.loggingEnabled;
        config.loggingEnabled = false;

        try {
            while (sequenceIndex < mod.getLength() && skippedPatternCount < patternCount) {
                tick(TMP_BUFFER, 0);

                if (lastSequenceIndex != sequenceIndex) {
                    skippedPatternCount++;
                    lastSequenceIndex = sequenceIndex;
                }
            }
        } finally {
            config.loggingEnabled = logInfoEnabled;
        }

        return skippedPatternCount;
    }

    public int skipRows(int rowCount) {
        requireNonNull(mod);
        int skippedRowCount = 0;

        int lastSequenceIndex = sequenceIndex;
        int lastRowIndex = rowIndex;

        boolean logInfoEnabled = config.loggingEnabled;
        config.loggingEnabled = false;

        try {
            while (sequenceIndex < mod.getLength() && skippedRowCount < rowCount) {
                tick(TMP_BUFFER, 0);

                if (rowIndex != lastRowIndex || lastSequenceIndex != sequenceIndex) {
                    skippedRowCount++;
                    lastRowIndex = rowIndex;
                    lastSequenceIndex = sequenceIndex;
                }
            }
        } finally {
            config.loggingEnabled = logInfoEnabled;
        }

        return skippedRowCount;
    }

    public byte[] read() {
        requireNonNull(mod);

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        int offset = 0;
        int readCount;

        while ((readCount = read(buffer, offset, buffer.length - offset)) > 0) {
            offset += readCount;

            if (offset == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length << 1);
            }
        }

        return Arrays.copyOf(buffer, offset);
    }

    public byte[] readPatterns(int patternCount) {
        requireNonNull(mod);

        if (patternCount < 0) {
            return EMPTY_BUFFER;
        }

        int bytesPerTick = getBytesPerSample();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        int offset = 0;
        int lastSequenceIndex = sequenceIndex;
        int readPatternCount = 0;

        while (sequenceIndex < mod.getLength() && readPatternCount < patternCount) {
            tick(buffer, offset);
            offset += bytesPerTick;

            if (offset == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length << 1);
            }

            if (sequenceIndex != lastSequenceIndex) {
                readPatternCount++;
                lastSequenceIndex = sequenceIndex;
            }
        }

        return Arrays.copyOf(buffer, offset);
    }

    /**
     * public int skipPatterns(int patternCount) { requireNonNull(mod);
     * <p>
     * if (patternCount <= 0) { return 0; }
     * <p>
     * int skippedPatternCount = 0; int lastSequenceIndex = sequenceIndex;
     * <p>
     * boolean logInfoEnabled = config.loggingEnabled; config.loggingEnabled = false;
     * <p>
     * try { while (sequenceIndex < mod.getLength() && skippedPatternCount < patternCount) { tick(TMP_BUFFER, 0);
     * <p>
     * if (lastSequenceIndex != sequenceIndex) { skippedPatternCount++; lastSequenceIndex = sequenceIndex; } } } finally
     * { config.loggingEnabled = logInfoEnabled; }
     * <p>
     * return skippedPatternCount; }
     *
     * @param rowCount
     * @return
     */

    public byte[] readRows(int rowCount) {
        requireNonNull(mod);

        if (rowCount < 0) {
            return EMPTY_BUFFER;
        }

        int bytesPerTick = getBytesPerSample();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        int offset = 0;
        int lastSequenceIndex = sequenceIndex;
        int lastRowIndex = rowIndex;
        int readRowCount = 0;

        while (sequenceIndex < mod.getLength() && readRowCount < rowCount) {
            tick(buffer, offset);
            offset += bytesPerTick;

            if (offset == buffer.length) {
                buffer = Arrays.copyOf(buffer, buffer.length << 1);
            }

            if (sequenceIndex != lastSequenceIndex || rowIndex != lastRowIndex) {
                readRowCount++;
                lastSequenceIndex = sequenceIndex;
                lastRowIndex = rowIndex;
            }
        }

        return Arrays.copyOf(buffer, offset);
    }

    public int read(byte[] output) {
        return read(output, 0, output.length);
    }

    public int read(byte[] output, int offset, int length) {
        return read(output, offset, length, mod.getLength());
    }

    public int read(byte[] output, int offset, int length, int endSequenceIndex) {
        checkFromIndexSize(offset, length, output.length);
        checkFromToIndex(0, endSequenceIndex, mod.getLength());

        int bytesPerTick = getBytesPerSample();

        if (length < bytesPerTick) {
            return 0;
        }

        int end = offset + length;
        int readCount = 0;

        while ((sequenceIndex < endSequenceIndex || sampleIndex < context.samplesPerTick) && end - offset >= bytesPerTick) {
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

                if (config.loggingEnabled) {
                    System.out.print(Formatters.formatRow(mod, patternIndex, rowIndex));
                    System.out.print(" {");
                    System.out.print(Formatters.formatEffects(mod, patternIndex, rowIndex));
                    System.out.println("}");
                }

                handleNewRow();
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

        if (context.hardwareFilterEnabled) {
            context.hardwareFilterLeft += context.hardwareFilterDelta * (left - context.hardwareFilterLeft);
            context.hardwareFilterRight += context.hardwareFilterDelta * (right - context.hardwareFilterRight);
            left = context.hardwareFilterLeft;
            right = context.hardwareFilterRight;
        }

        if (config.stereoFoldDownEnabled) {
            left = (left + right) * 0.5f;
            right = left;
        }

        left = Maths.clamp(left, -1.0f, 1.0f);
        right = Maths.clamp(right, -1.0f, 1.0f);

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

    private static float computeFilterAlpha(int samplingRate) {
        float cutoffHz = 4900.0f; // approximate — pick a value matching your reference player
        float rc = 1.0f / (2.0f * (float) Math.PI * cutoffHz);
        float dt = 1.0f / samplingRate;
        return dt / (rc + dt);
    }

    private void advanceRow() {
        int nextSequenceIndex = sequenceIndex;
        int nextRowIndex;
        boolean nextLoopPending = context.loopPending;
        int nextLoopRowIndex = context.loopRowIndex;
        int nextLoopCounter = context.loopCounter;

        if (context.loopPending) {
            // looping takes priority above jumps and breaks
            nextRowIndex = context.loopRowIndex;
        } else if (context.jumpPending && context.breakPending) {
            // both position jump and pattern break effects are pending
            nextSequenceIndex = context.jumpSequenceIndex;
            nextRowIndex = context.breakRowIndex;

        } else if (context.jumpPending) {
            // jump to row 0 of specific pattern
            nextSequenceIndex = context.jumpSequenceIndex;
            nextRowIndex = 0;

        } else if (context.breakPending) {
            // jump to next pattern's specific row
            nextSequenceIndex = sequenceIndex + 1;
            nextRowIndex = context.breakRowIndex;
        } else {
            // advance single row
            nextRowIndex = rowIndex + 1;

            if (nextRowIndex >= 64) {
                nextRowIndex = 0;
                nextSequenceIndex = sequenceIndex + 1;
            }
        }

        if (config.loopDetectionEnabled) {
            State next = new State(nextSequenceIndex, nextRowIndex, nextLoopPending, nextLoopRowIndex, nextLoopCounter);
            boolean added = visited.add(next);

            if (!added) {
                if (config.loggingEnabled) {
                    System.err.println("Warning: infinite loop detected at row " + rowIndex + " of pattern " + mod.getPatternIndex(sequenceIndex));
                }

                // break the infinite loop by advancing outside of pattern sequences
                nextSequenceIndex = mod.getPatternSequenceCount() + 1;
                nextRowIndex = 0;

                // TODO do we want to continue till the end on the past pattern when the loop is ignored?
            }
        }

        sequenceIndex = nextSequenceIndex;
        rowIndex = nextRowIndex;

        context.jumpPending = false;
        context.jumpSequenceIndex = 0;
        context.breakPending = false;
        context.breakRowIndex = 0;
        context.loopPending = false;
        context.loopRowIndex = 0;
        // we explicitly do not want to clear the loop counter
    }

    private void handleNewRow() {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            int patternIndex = mod.getPatternIndex(sequenceIndex);
            Instrument instrument = mod.getInstrument(channelIndex, patternIndex, rowIndex);
            Channel channel = channels[channelIndex];

            Sample sample = instrument.sampleNumber() > 0 ? mod.getSample(instrument.sampleNumber() - 1) : null;
            int period = instrument.period();

            channel.effectType = instrument.effectType();
            channel.extendedEffectType = instrument.extendedEffectType();
            channel.effectArgumentX = instrument.effectArgumentX();
            channel.effectArgumentY = instrument.effectArgumentY();

            instrument.effectType().onPreEffect(channel, config, period, sample);
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

            channel.effectType.onNewRow(channel, context, config, rowIndex);
        }
    }

    private void applyMidRowEffects() {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Channel channel = channels[channelIndex];
            channel.effectType.onMidRow(channel, context, config);
        }
    }

    public long getModLength(TimeUnit unit) {
        long milliseconds = sampleCount * 1_000L / config.samplingRate * 1_000L;
        return unit.convert(milliseconds, TimeUnit.MICROSECONDS);
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public Mod getMod() {
        return mod;
    }

    public void loadMod(Mod mod) {
        if (mod.getChannelCount() > channels.length) {
            throw new IllegalArgumentException("Mod has more channels than the player supports");
        }

        this.mod = mod;

        reset();
        recalculateMeta();
        reset();
    }

    public void setPanning(int channelIndex, float right) {
        channels[channelIndex].setPanning(requireInRange(right, 0.0f, 1.0f));
    }

    public void setOpenMPTPanning() {
        for (int i = 0; i < channels.length; i++) {
            channels[i].setPanning(channels[i].right ? Mods.MPT_PAN_RIGHT : Mods.MPT_PAN_LEFT);
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

    public void setAllEffectsEnabled(boolean enabled) {
        for (int i = 0; i < config.effectEnabled.length; i++) {
            config.effectEnabled[i] = enabled;
        }
    }

    public void setAllExtendedEffectsEnabled(boolean enabled) {
        for (int i = 0; i < config.extendedEffectEnabled.length; i++) {
            config.extendedEffectEnabled[i] = enabled;
        }
    }

    public void setClockHz(int clockHz) {
        if (clockHz <= 0) {
            throw new IllegalArgumentException("clockHz must be greater than zero");
        }

        this.config.clockHz = clockHz;

        if (mod != null) {
            recalculatePeriods();
        }
    }

    public void setSamplingRate(int samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("samplingRate must be greater than zero");
        }

        config.samplingRate = samplingRate;

        // TODO do we want to rebuild metadata when changing sampling rate

        if (mod != null) {
            recalculatePeriods();
            recalculateHardwareFilter();
            recalculateMeta();
        }
    }

    private void recalculatePeriods() {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            channels[channelIndex].updatePeriodAndIncrement(channels[channelIndex].period, config.clockHz, config.samplingRate);
        }
    }

    private void recalculateHardwareFilter() {
        if (context.hardwareFilterEnabled) {
            context.updateHardwareFilterDelta(config.samplingRate);
        }
    }

    /**
     * Builds context and channel data for each sequence and row for fast traversal. Also calculates total amount of
     * samples. It always detects infinite loops.
     */
    private void recalculateMeta() {
        boolean loopDetectionEnabled = config.loopDetectionEnabled;
        boolean loggingEnabled = config.loggingEnabled;

        config.loopDetectionEnabled = true;
        config.loggingEnabled = false;

        try {
            int sampleCount = 0;

            boolean[] visited = new boolean[Mod.PATTERN_SEQUENCE_COUNT * Mod.ROW_COUNT];
            visited[0] = true;

            contextBySequenceRow[0].copyFrom(context);

            for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                channelsBySequenceRow[0][channelIndex].copyFrom(channels[channelIndex]);
            }

            while (sequenceIndex < mod.getLength() || sampleIndex < context.samplesPerTick) {
                tick(TMP_BUFFER, 0);
                sampleCount++;

                int index = sequenceIndex * Mod.ROW_COUNT + rowIndex;

                if (index < visited.length && !visited[index]) {
                    if (tickIndex != 0 || sampleIndex != 1) {
                        throw new RuntimeException("invalid state, tickIndex = " + tickIndex + ", sampleIndex = " + sampleIndex);
                    }

                    visited[index] = true;
                    contextBySequenceRow[index].copyFrom(context);

                    for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                        channelsBySequenceRow[index][channelIndex].copyFrom(channels[channelIndex]);
                    }
                }
            }

            this.sampleCount = sampleCount;
        } finally {
            config.loopDetectionEnabled = loopDetectionEnabled;
            config.loggingEnabled = loggingEnabled;
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

    public void setLoopDetectionEnabled(boolean loopDetectionEnabled) {
        this.config.loopDetectionEnabled = loopDetectionEnabled;
    }

    public void setLoggingEnabled(boolean enabled) {
        this.config.loggingEnabled = enabled;
    }

    public AudioFormat getCompatibleAudioFormat() {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, config.samplingRate, 16,
                config.stereoEnabled ? 2 : 1, config.stereoEnabled ? 4 : 2, config.samplingRate, false);
    }

    public int getSequenceIndex() {
        return sequenceIndex;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public int getTickIndex() {
        return tickIndex;
    }

    public int getSampleIndex() {
        return sampleIndex;
    }

    public int getSpeed() {
        return context.speed;
    }

    public int getBytesPerSample() {
        return config.stereoEnabled ? 4 : 2;
    }

    public int getBytesPerTick() {
        return getBytesPerSample() * context.samplesPerTick;
    }

    public int getBytesPerRow() {
        return getBytesPerTick() * context.speed;
    }

    public int getSamplesPerTick() {
        return context.samplesPerTick;
    }

    public int getSamplesPerRow() {
        return context.speed * context.samplesPerTick;
    }

    private record State(int patternIndex, int rowIndex, boolean loopPending, int loopRowIndex, int loopCounter) {
    }
}
