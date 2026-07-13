package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Formatters;
import com.github.thething.chipamp.common.Maths;
import com.github.thething.chipamp.common.VisibleForTesting;

import javax.sound.sampled.AudioFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.github.thething.chipamp.common.Requirements.requireInRange;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.checkIndex;
import static java.util.Objects.requireNonNull;

/**
 * Renders a {@link Mod} tracker module into raw 16-bit PCM audio, one tick at a time.
 * <p>
 * A {@code Sampler} walks the module's pattern sequence row by row and tick by tick, dispatching note and effect data
 * to a set of {@link Channel}s and mixing their output into interleaved little-endian PCM samples. It supports seeking
 * and skipping by pattern or row, muting individual channels, enabling/disabling individual effects, and tuning
 * playback parameters such as sampling rate, clock frequency, and panning.
 * <p>
 * When a module is loaded via {@link #updateMod(Mod)}, the sampler performs a silent pre-pass over the whole module to
 * record the channel and context state at the start of every sequence/row combination, and to detect infinite pattern
 * loops. This allows {@link #seekSequence(int, int)} to jump directly to any row without having to replay the module
 * from the beginning, and allows {@link #getModLength(TimeUnit)} to report the total playback duration.
 * <p>
 * Instances are stateful and mutable and are not safe for concurrent use.
 */
public final class Sampler {

    private static final State INITIAL_STATE = new State(0, 0, false, 0, 0);
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final byte[] EMPTY_BUFFER = new byte[0];
    private static final byte[] TMP_BUFFER = new byte[4];

    private final Config config;
    private final Channel[] channels;
    private final Context context;

    /**
     * Sequence/row/loop signatures seen so far this playthrough, used to detect infinite pattern loops.
     */
    private final Set<State> visited;

    /**
     * Channel state snapshots indexed by {@code sequenceIndex * Mod.ROW_COUNT + rowIndex}, captured by
     * {@link #recalculateMeta()} for fast seeking.
     */
    private final Channel[][] channelsBySequenceRow;

    /**
     * Context snapshots indexed by {@code sequenceIndex * Mod.ROW_COUNT + rowIndex}, captured by
     * {@link #recalculateMeta()} for fast seeking.
     */
    private final Context[] contextBySequenceRow;

    /**
     * Loop-detection visited-set snapshots indexed by {@code sequenceIndex * Mod.ROW_COUNT + rowIndex}, captured by
     * {@link #recalculateMeta()} for fast seeking.
     */
    private final Set<State>[] visitedBySequenceRow;

    private Mod mod;
    private int sampleCount;
    private int sequenceIndex;
    private int rowIndex;
    private int tickIndex;
    private int sampleIndex;

    /**
     * Creates a new sampler with default playback settings and no module loaded. Call {@link #updateMod(Mod)} before
     * reading any audio.
     */
    public Sampler() {
        this.config = new Config(Mod.MAX_CHANNEL_COUNT);
        this.channels = new Channel[Mod.MAX_CHANNEL_COUNT];

        for (int i = 0; i < channels.length; i++) {
            boolean right = (i & 3) == 1 || (i & 3) == 2; // (LRRL) repeating pattern
            this.channels[i] = new Channel(config, right);
        }

        this.context = new Context(config.samplingRate);
        this.visited = new HashSet<>();

        this.channelsBySequenceRow = new Channel[Mod.PATTERN_SEQUENCE_COUNT * Mod.ROW_COUNT][Mod.MAX_CHANNEL_COUNT];

        for (int i = 0; i < channelsBySequenceRow.length; i++) {
            for (int channelIndex = 0; channelIndex < channelsBySequenceRow[i].length; channelIndex++) {
                boolean right = (i & 3) == 1 || (i & 3) == 2; // (LRRL) repeating pattern
                this.channelsBySequenceRow[i][channelIndex] = new Channel(config, right);
            }
        }

        this.contextBySequenceRow = new Context[Mod.PATTERN_SEQUENCE_COUNT * Mod.ROW_COUNT];

        for (int i = 0; i < contextBySequenceRow.length; i++) {
            this.contextBySequenceRow[i] = new Context(config.samplingRate);
        }

        this.visitedBySequenceRow = new HashSet[Mod.PATTERN_SEQUENCE_COUNT * Mod.ROW_COUNT];

        for (int i = 0; i < visitedBySequenceRow.length; i++) {
            this.visitedBySequenceRow[i] = new HashSet<>();
        }
    }

    /**
     * Rewinds playback to the very first tick of the module (sequence 0, row 0), resetting all channels and the
     * playback context, and re-arming loop detection.
     */
    public void reset() {
        context.reset(config.samplingRate);

        visited.clear();
        visited.add(INITIAL_STATE);

        for (Channel channel : channels) {
            channel.reset(config);
        }

        sequenceIndex = 0;
        rowIndex = 0;
        tickIndex = 0;
        sampleIndex = context.samplesPerTick;
    }

    /**
     * Seeks to row 0 of the given position in the pattern sequence.
     *
     * @param sequenceIndex the position in the pattern sequence to seek to
     * @throws NullPointerException      if no module has been loaded
     * @throws IndexOutOfBoundsException if {@code sequenceIndex} is out of range
     */
    public void seekSequence(int sequenceIndex) {
        seekSequence(sequenceIndex, 0);
    }

    /**
     * Seeks to a specific row at a specific position in the pattern sequence, restoring channel and context state from
     * the snapshot captured for that row by {@link #recalculateMeta()}. Seeking to sequence 0, row 0 is equivalent to
     * calling {@link #reset()}.
     *
     * @param sequenceIndex the position in the pattern sequence to seek to
     * @param rowIndex      the row within that pattern to seek to
     * @throws NullPointerException      if no module has been loaded
     * @throws IndexOutOfBoundsException if {@code sequenceIndex} or {@code rowIndex} is out of range
     */
    public void seekSequence(int sequenceIndex, int rowIndex) {
        requireNonNull(mod);
        checkIndex(sequenceIndex, mod.getLength());
        checkIndex(rowIndex, Mod.ROW_COUNT);

        int index = sequenceIndex * Mod.ROW_COUNT + rowIndex;
        this.context.copyFrom(contextBySequenceRow[index]);

        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            this.channels[channelIndex].copyFrom(channelsBySequenceRow[index][channelIndex]);
        }

        this.visited.clear();
        this.visited.addAll(visitedBySequenceRow[index]);

        if (sequenceIndex == 0 && rowIndex == 0) {
            reset();
        } else {
            this.sequenceIndex = sequenceIndex;
            this.rowIndex = rowIndex;
            this.tickIndex = 0;
            this.sampleIndex = 1;
        }
    }

    /**
     * Seeks to the first position in the pattern sequence that plays the given pattern.
     * <p>
     * Unlike {@link #seekSequence(int)}, which seeks to a position in the sequence, this seeks to a pattern number,
     * which may appear zero, one, or multiple times in the sequence.
     *
     * @param patternIndex the pattern number to seek to
     * @return the resolved position in the pattern sequence, or {@code -1} if the pattern does not appear in the
     * sequence, in which case playback position is left unchanged
     * @throws NullPointerException     if no module has been loaded
     * @throws IllegalArgumentException if {@code patternIndex} is out of range
     */
    public int seekPattern(int patternIndex) {
        requireNonNull(mod);
        requireInRange(patternIndex, 0, mod.getLength());

        int sequenceIndex = Mods.findSequenceIndex(mod, patternIndex);

        if (sequenceIndex < 0) {
            return -1;
        }

        seekSequence(sequenceIndex);

        return sequenceIndex;
    }

    /**
     * Advances playback silently until {@code patternCount} pattern-sequence boundaries have been crossed, or the end
     * of the module is reached. Rendered audio is discarded; logging is temporarily suppressed while skipping.
     *
     * @param patternCount the number of pattern-sequence boundaries to advance past
     * @return the number of boundaries actually crossed, which may be less than {@code patternCount} if the module ends
     * first
     * @throws NullPointerException if no module has been loaded
     */
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
            while (skippedPatternCount < patternCount) {
                boolean ticked = tick(TMP_BUFFER, 0);

                if (!ticked) {
                    break;
                }

                if (sequenceIndex != lastSequenceIndex) {
                    skippedPatternCount++;
                    lastSequenceIndex = sequenceIndex;
                }
            }
        } finally {
            config.loggingEnabled = logInfoEnabled;
        }

        return skippedPatternCount;
    }

    /**
     * Advances playback silently until {@code rowCount} rows have been crossed, or the end of the module is reached.
     * Rendered audio is discarded; logging is temporarily suppressed while skipping.
     *
     * @param rowCount the number of rows to advance past
     * @return the number of rows actually crossed, which may be less than {@code rowCount} if the module ends first
     * @throws NullPointerException if no module has been loaded
     */
    public int skipRows(int rowCount) {
        requireNonNull(mod);

        int skippedRowCount = 0;
        int lastSequenceIndex = sequenceIndex;
        int lastRowIndex = rowIndex;

        boolean logInfoEnabled = config.loggingEnabled;
        config.loggingEnabled = false;

        try {
            while (skippedRowCount < rowCount) {
                boolean ticked = tick(TMP_BUFFER, 0);

                if (!ticked) {
                    break;
                }

                if (rowIndex != lastRowIndex || sequenceIndex != lastSequenceIndex) {
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

    /**
     * Renders the remainder of the module to a newly allocated buffer, from the current playback position through the
     * end of the module.
     *
     * @return the rendered PCM audio, sized exactly to the number of bytes produced
     * @throws NullPointerException if no module has been loaded
     */
    public byte[] readAll() {
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

    /**
     * Renders audio from the current playback position until {@code patternCount} pattern-sequence boundaries have been
     * crossed, or the end of the module is reached.
     *
     * @param patternCount the number of pattern-sequence boundaries to render past
     * @return the rendered PCM audio, sized exactly to the number of bytes produced; empty if {@code patternCount} is
     * negative
     * @throws NullPointerException if no module has been loaded
     */
    public byte[] readPatterns(int patternCount) {
        requireNonNull(mod);

        if (patternCount < 0) {
            return EMPTY_BUFFER;
        }

        int bytesPerSample = getBytesPerSample();
        byte[] buffer = new byte[bytesPerSample];

        int offset = 0;
        int lastSequenceIndex = sequenceIndex;
        int readPatternCount = 0;

        while (readPatternCount < patternCount) {
            boolean ticked = tick(buffer, offset);

            if (!ticked) {
                break;
            }

            offset += bytesPerSample;

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
     * Renders audio from the current playback position until {@code rowCount} rows have been crossed, or the end of the
     * module is reached.
     *
     * @param rowCount the number of rows to render past
     * @return the rendered PCM audio, sized exactly to the number of bytes produced; empty if {@code rowCount} is
     * negative
     * @throws NullPointerException if no module has been loaded
     */
    public byte[] readRows(int rowCount) {
        requireNonNull(mod);

        if (rowCount < 0) {
            return EMPTY_BUFFER;
        }

        int bytesPerSample = getBytesPerSample();
        byte[] buffer = new byte[bytesPerSample];

        int offset = 0;
        int lastSequenceIndex = sequenceIndex;
        int lastRowIndex = rowIndex;
        int readRowCount = 0;

        while (readRowCount < rowCount) {
            boolean ticked = tick(buffer, offset);

            if (!ticked) {
                break;
            }

            offset += bytesPerSample;

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

    /**
     * Renders audio into the given buffer, filling as much of it as possible.
     *
     * @param output the buffer to render into
     * @return the number of bytes written, which will be a multiple of {@link #getBytesPerSample()}; may be less than
     * {@code output.length} if the module ends first, or {@code 0} if playback has already finished
     * @throws NullPointerException if no module has been loaded
     */
    public int read(byte[] output) {
        return read(output, 0, output.length);
    }

    /**
     * Renders audio into a region of the given buffer, filling as much of that region as possible.
     *
     * @param output the buffer to render into
     * @param offset the offset in {@code output} to start writing at
     * @param length the maximum number of bytes to write
     * @return the number of bytes written, which will be a multiple of {@link #getBytesPerSample()}; may be less than
     * {@code length} if the module ends first, or {@code 0} if playback has already finished
     * @throws NullPointerException      if no module has been loaded
     * @throws IndexOutOfBoundsException if {@code offset} and {@code length} are out of bounds for {@code output}
     */
    public int read(byte[] output, int offset, int length) {
        checkFromIndexSize(offset, length, output.length);

        int bytesPerSample = getBytesPerSample();

        if (length < bytesPerSample) {
            return 0;
        }

        int readCount = 0;
        int remaining = length;

        while (remaining >= bytesPerSample) {
            boolean ticked = tick(output, offset);

            if (!ticked) {
                break;
            }

            readCount += bytesPerSample;
            offset += bytesPerSample;
            remaining -= bytesPerSample;
        }

        return readCount;
    }

    /**
     * Renders a single output sample and advances playback by one sample's worth of time, crossing tick and row
     * boundaries as needed.
     *
     * @param output the buffer to write the rendered sample into
     * @param offset the offset in {@code output} to write at; must have room for {@link #getBytesPerSample()} bytes
     * @return {@code true} if a sample was rendered, {@code false} if the end of the module has been reached
     */
    private boolean tick(byte[] output, int offset) {
        if (sequenceIndex >= mod.getLength() && sampleIndex >= context.samplesPerTick) {
            return false;
        }

        if (sampleIndex >= context.samplesPerTick) {
            // first tick of a row triggers new notes/effects; later ticks only apply per-tick effect updates
            if (tickIndex == 0) {
                int patternIndex = mod.getPatternIndex(sequenceIndex);

                if (config.loggingEnabled) {
                    System.out.print(Formatters.formatRow(mod, patternIndex, rowIndex));
                    System.out.print(' ');
                    System.out.println(Formatters.formatEffects(mod, patternIndex, rowIndex));
                }

                handleNewRow();
            } else {
                applyMidRowEffects();
            }

            tickIndex++;

            if (tickIndex >= context.speed + context.extraDelay) {
                tickIndex = 0;
                context.extraDelay = 0;
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

            left += sample * channels[i].leftPan;
            right += sample * channels[i].rightPan;
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

        return true;
    }

    /**
     * Resolves the next sequence/row position at the end of a row, applying any pending loop, position-jump or
     * pattern-break effect in priority order (loop, then jump+break combined, then jump alone, then break alone, then a
     * plain single-row advance), and detects infinite pattern loops if enabled.
     */
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
                // in theory we could possibly continue util the end of the current pattern (especially if this is the
                // last pattern in mod), but it seems that open MPT doesn't do it either
                nextSequenceIndex = mod.getPatternSequenceCount() + 1;
                nextRowIndex = 0;
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

        // we explicitly DO NOT want to clear
        // loop counter
        // hardware filter
        // extra delay
    }

    /**
     * Loads each channel's note/instrument/effect data for the current row, triggers new samples via each effect's
     * pre-effect hook, and then applies each channel's new-row effect logic.
     */
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

    /**
     * Applies each channel's active effect for the first tick of the current row, skipping channels with no effect or
     * whose effect type is disabled in the configuration.
     */
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

    /**
     * Applies each channel's active effect for a tick other than the first tick of the row.
     */
    private void applyMidRowEffects() {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Channel channel = channels[channelIndex];
            channel.effectType.onMidRow(channel, context, config);
        }
    }

    /**
     * Returns the total playback duration of the currently loaded module, as computed by {@link #recalculateMeta()}
     * when the module was loaded.
     *
     * @param unit the time unit to express the duration in
     * @return the module's total playback duration in the requested unit
     */
    public long getModLength(TimeUnit unit) {
        long milliseconds = sampleCount * 1_000L / config.samplingRate * 1_000L;
        return unit.convert(milliseconds, TimeUnit.MICROSECONDS);
    }

    /**
     * Returns the total number of output samples in the currently loaded module.
     *
     * @return the sample count, or {@code 0} if no module has been loaded
     */
    public int getSampleCount() {
        return sampleCount;
    }

    /**
     * Returns the currently loaded module.
     *
     * @return the current module, or {@code null} if none has been loaded
     */
    public Mod getMod() {
        return mod;
    }

    /**
     * Loads a new module for playback, resetting playback position and pre-computing seek/loop metadata for it.
     *
     * @param mod the module to load
     * @throws IllegalArgumentException if {@code mod} uses more channels than this sampler supports
     */
    public void updateMod(Mod mod) {
        if (mod.getChannelCount() > channels.length) {
            throw new IllegalArgumentException("Mod has more channels than the player supports");
        }

        this.mod = mod;

        reset();
        recalculateMeta();
        reset();
    }

    /**
     * Sets the left output channel's panning level, re-deriving every channel's effective left/right pan from its
     * hardware default panning.
     *
     * @param leftPan the left panning level, from {@code 0.0} to {@code 1.0}
     * @throws IllegalArgumentException if {@code leftPan} is out of range
     */
    public void setLeftPan(float leftPan) {
        requireInRange(leftPan, 0.0f, 1.0f);
        config.leftPan = leftPan;

        for (Channel channel : channels) {
            channel.updatePanning(leftPan, config.rightPan);
        }
    }

    /**
     * Sets the right output channel's panning level, re-deriving every channel's effective left/right pan from its
     * hardware default panning.
     *
     * @param rightPan the right panning level, from {@code 0.0} to {@code 1.0}
     * @throws IllegalArgumentException if {@code rightPan} is out of range
     */
    public void setRightPan(float rightPan) {
        requireInRange(rightPan, 0.0f, 1.0f);
        config.rightPan = rightPan;

        for (Channel channel : channels) {
            channel.updatePanning(config.leftPan, rightPan);
        }
    }

    /**
     * Mutes or unmutes a single channel. A muted channel's sample position still advances during playback, but its
     * output is excluded from the mix.
     *
     * @param channelIndex the channel to mute or unmute
     * @param muted        {@code true} to mute the channel, {@code false} to unmute it
     */
    public void setMuted(int channelIndex, boolean muted) {
        config.muted[channelIndex] = muted;
    }

    /**
     * Returns whether a single channel is muted.
     *
     * @param channelIndex the channel to check
     * @return {@code true} if the channel is muted, {@code false} otherwise
     */
    public boolean isMuted(int channelIndex) {
        return config.muted[channelIndex];
    }

    /**
     * Enables or disables processing of a single effect type across all channels.
     *
     * @param effectType the effect type to enable or disable
     * @param enabled    {@code true} to enable the effect, {@code false} to disable it
     * @throws IllegalArgumentException if {@code effectType} is {@code null} or {@link EffectType#NONE}
     */
    public void setEffectEnabled(EffectType effectType, boolean enabled) {
        if (effectType == null || effectType == EffectType.NONE) {
            throw new IllegalArgumentException("Effect type must not be null or NONE");
        }

        config.effectEnabled[effectType.getCode()] = enabled;
    }

    /**
     * Checks if a single effect type is enabled across all channels.
     *
     * @param effectType the effect type to check
     * @return {@code true} if the effect is enabled, {@code false} otherwise
     * @throws IllegalArgumentException if {@code effectType} is {@code null} or {@link EffectType#NONE}
     */
    public boolean isEffectEnabled(EffectType effectType) {
        if (effectType == null || effectType == EffectType.NONE) {
            throw new IllegalArgumentException("Effect type must not be null or NONE");
        }

        return config.effectEnabled[effectType.getCode()];
    }

    /**
     * Enables or disables processing of a single extended effect type across all channels.
     *
     * @param extendedEffectType the extended effect type to enable or disable
     * @param enabled            {@code true} to enable the effect, {@code false} to disable it
     * @throws IllegalArgumentException if {@code extendedEffectType} is {@code null} or
     *                                  {@link ExtendedEffectType#NONE}
     */
    public void setExtendedEffectEnabled(ExtendedEffectType extendedEffectType, boolean enabled) {
        if (extendedEffectType == null || extendedEffectType == ExtendedEffectType.NONE) {
            throw new IllegalArgumentException("Extended effect must not be null or NONE");
        }

        config.extendedEffectEnabled[extendedEffectType.getCode()] = enabled;
    }

    /**
     * Checks if a single extended effect type is enabled across all channels.
     *
     * @param extendedEffectType the extended effect type to check
     * @return {@code true} if the extended effect is enabled, {@code false} otherwise
     * @throws IllegalArgumentException if {@code extendedEffectType} is {@code null} or
     *                                  {@link ExtendedEffectType#NONE}
     */
    public boolean isExtendedEffectEnabled(ExtendedEffectType extendedEffectType) {
        if (extendedEffectType == null || extendedEffectType == ExtendedEffectType.NONE) {
            throw new IllegalArgumentException("Extended effect must not be null or NONE");
        }

        return config.extendedEffectEnabled[extendedEffectType.getCode()];
    }

    /**
     * Enables or disables processing of all effect types across all channels.
     *
     * @param enabled {@code true} to enable all effects, {@code false} to disable all effects
     */
    public void setAllEffectsEnabled(boolean enabled) {
        Arrays.fill(config.effectEnabled, enabled);
    }

    /**
     * Enables or disables processing of all extended effect types across all channels.
     *
     * @param enabled {@code true} to enable all extended effects, {@code false} to disable all extended effects
     */
    public void setAllExtendedEffectsEnabled(boolean enabled) {
        Arrays.fill(config.extendedEffectEnabled, enabled);
    }

    /**
     * Sets the module's clock frequency, used to convert note periods to playback frequencies, and immediately
     * recalculates every channel's sample increment to reflect it.
     *
     * @param clockHz the clock frequency in Hz, e.g. {@code Mods.PAL_CLOCK_HZ} or {@code Mods.NTSC_CLOCK_HZ}
     * @throws IllegalArgumentException if {@code clockHz} is not positive
     */
    public void setClockHz(int clockHz) {
        if (clockHz <= 0) {
            throw new IllegalArgumentException("clockHz must be greater than zero");
        }

        this.config.clockHz = clockHz;

        if (mod != null) {
            recalculatePeriods();
        }
    }

    /**
     * Sets the output sampling rate, and immediately recalculates every channel's sample increment, the hardware filter
     * coefficient, and the seek/loop metadata to reflect it.
     *
     * @param samplingRate the output sampling rate in Hz
     * @throws IllegalArgumentException if {@code samplingRate} is not positive
     */
    public void setSamplingRate(int samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("samplingRate must be greater than zero");
        }

        config.samplingRate = samplingRate;

        if (mod != null) {
            recalculatePeriods();
            recalculateHardwareFilter();
            recalculateMeta();
        }
    }

    /**
     * Recomputes each channel's sample increment from its currently stored period, using the current clock frequency
     * and sampling rate. Called after either changes.
     */
    private void recalculatePeriods() {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            channels[channelIndex].updatePeriodAndIncrement(channels[channelIndex].period, config.clockHz, config.samplingRate);
        }
    }

    /**
     * Recomputes the hardware low-pass filter coefficient for the current sampling rate, if the filter is enabled.
     */
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

            visitedBySequenceRow[0].clear();
            visitedBySequenceRow[0].addAll(this.visited);

            while (true) {
                boolean ticked = tick(TMP_BUFFER, 0);

                if (!ticked) {
                    break;
                }

                sampleCount++;

                int index = sequenceIndex * Mod.ROW_COUNT + rowIndex;

                // when dealing with LOOP_PATTERN effect, it is possible that already visited row could be revisited
                // we only store the state once per row - first time we visit it
                if (index < visited.length && !visited[index]) {
                    visited[index] = true;
                    contextBySequenceRow[index].copyFrom(context);

                    for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
                        channelsBySequenceRow[index][channelIndex].copyFrom(channels[channelIndex]);
                    }

                    visitedBySequenceRow[index].clear();
                    visitedBySequenceRow[index].addAll(this.visited);
                }
            }

            this.sampleCount = sampleCount;
        } finally {
            config.loopDetectionEnabled = loopDetectionEnabled;
            config.loggingEnabled = loggingEnabled;
        }
    }

    /**
     * Sets the lower-bound clamp applied to note periods.
     *
     * @param minPeriod the minimum allowed period
     * @throws IllegalArgumentException if {@code minPeriod} is negative
     */
    public void setMinPeriod(int minPeriod) {
        if (minPeriod < 0) {
            throw new IllegalArgumentException("minPeriod must be greater than zero");
        }

        this.config.minPeriod = minPeriod;
    }

    /**
     * Sets the upper bound clamp applied to note periods.
     *
     * @param maxPeriod the maximum allowed period
     * @throws IllegalArgumentException if {@code maxPeriod} is negative
     */
    public void setMaxPeriod(int maxPeriod) {
        if (maxPeriod < 0) {
            throw new IllegalArgumentException("maxPeriod must be greater than zero");
        }

        this.config.maxPeriod = maxPeriod;
    }

    /**
     * Sets the overall output volume multiplier applied to the mixed signal.
     *
     * @param volumeMultiplier the volume multiplier, where {@code 1.0} is unity gain
     * @throws IllegalArgumentException if {@code volumeMultiplier} is negative
     */
    public void setVolumeMultiplier(float volumeMultiplier) {
        if (volumeMultiplier < 0.0f) {
            throw new IllegalArgumentException("volumeMultiplier must be greater than or equal to zero");
        }

        config.volumeMultiplier = volumeMultiplier;
    }

    /**
     * Enables or disables stereo output. When disabled, output is rendered as mono.
     *
     * @param stereoEnabled {@code true} to render stereo output, {@code false} to render mono
     */
    public void setStereoEnabled(boolean stereoEnabled) {
        this.config.stereoEnabled = stereoEnabled;
    }

    /**
     * Enables or disables stereo fold-down, which averages the left and right channels into an identical signal on
     * both, while still producing stereo-shaped output.
     *
     * @param stereoFoldDownEnabled {@code true} to fold stereo down to a centered signal, {@code false} to keep full
     *                              stereo separation
     */
    public void setStereoFoldDownEnabled(boolean stereoFoldDownEnabled) {
        this.config.stereoFoldDownEnabled = stereoFoldDownEnabled;
    }

    /**
     * Enables or disables delta-based volume sliding.
     *
     * @param volumeSlideDeltaEnabled {@code true} to enable delta-based volume sliding, {@code false} to disable it
     */
    public void setVolumeSlideDeltaEnabled(boolean volumeSlideDeltaEnabled) {
        this.config.volumeSlideDeltaEnabled = volumeSlideDeltaEnabled;
    }

    /**
     * Enables or disables rounding periods to the nearest supported value.
     *
     * @param roundNearestPeriodEnabled {@code true} to round periods to the nearest supported value, {@code false} to
     *                                  leave them unrounded
     */
    public void setRoundNearestPeriodEnabled(boolean roundNearestPeriodEnabled) {
        this.config.roundNearestPeriodEnabled = roundNearestPeriodEnabled;
    }

    /**
     * Enables or disables detection of infinite pattern loops during playback and metadata pre-computation.
     *
     * @param loopDetectionEnabled {@code true} to detect infinite loops, {@code false} to disable detection
     */
    public void setLoopDetectionEnabled(boolean loopDetectionEnabled) {
        this.config.loopDetectionEnabled = loopDetectionEnabled;
    }

    /**
     * Enables or disables logging of each row's notes and effects to standard output as it is played.
     *
     * @param enabled {@code true} to enable logging, {@code false} to disable it
     */
    public void setLoggingEnabled(boolean enabled) {
        this.config.loggingEnabled = enabled;
    }

    /**
     * Returns an {@link AudioFormat} describing the PCM audio produced by this sampler's {@code read}/{@code readXxx}
     * methods: 16-bit signed, little-endian, at the current sampling rate, mono or stereo depending on
     * {@link #setStereoEnabled(boolean)}.
     *
     * @return an audio format compatible with this sampler's current output
     */
    public AudioFormat getCompatibleAudioFormat() {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, config.samplingRate, 16,
                config.stereoEnabled ? 2 : 1, config.stereoEnabled ? 4 : 2, config.samplingRate, false);
    }

    /**
     * Returns the current position in the pattern sequence.
     *
     * @return the current sequence index
     */
    public int getSequenceIndex() {
        return sequenceIndex;
    }

    /**
     * Returns the current row within the current pattern.
     *
     * @return the current row index
     */
    public int getRowIndex() {
        return rowIndex;
    }

    /**
     * Returns the current tick within the current row.
     *
     * @return the current tick index
     */
    public int getTickIndex() {
        return tickIndex;
    }

    /**
     * Returns the current sample within the current tick.
     *
     * @return the current sample index
     */
    public int getSampleIndex() {
        return sampleIndex;
    }

    /**
     * Returns the number of bytes needed to encode one output sample: {@code 4} for 16-bit stereo, {@code 2} for 16-bit
     * mono.
     *
     * @return the number of bytes per output sample
     */
    public int getBytesPerSample() {
        return config.stereoEnabled ? 4 : 2;
    }

    /**
     * Returns the number of output bytes rendered per tick at the current sampling rate and tempo.
     *
     * @return the number of bytes per tick
     */
    public int getBytesPerTick() {
        return getBytesPerSample() * context.samplesPerTick;
    }

    /**
     * Returns the number of output bytes rendered per row at the current sampling rate, tempo and speed.
     *
     * @return the number of bytes per row
     */
    public int getBytesPerRow() {
        return getBytesPerTick() * context.speed;
    }

    /**
     * Returns the number of output samples rendered per tick at the current sampling rate and tempo.
     *
     * @return the number of samples per tick
     */
    public int getSamplesPerTick() {
        return context.samplesPerTick;
    }

    /**
     * Returns the number of output samples rendered per row at the current sampling rate, tempo and speed.
     *
     * @return the number of samples per row
     */
    public int getSamplesPerRow() {
        return context.speed * context.samplesPerTick;
    }

    /**
     * Returns a channel's current effective left panning level.
     *
     * @param channelIndex the channel to query
     * @return the channel's left panning level
     */
    public float getLeftPan(int channelIndex) {
        return channels[channelIndex].leftPan;
    }

    /**
     * Returns a channel's current effective right panning level.
     *
     * @param channelIndex the channel to query
     * @return the channel's right panning level
     */
    public float getRightPan(int channelIndex) {
        return channels[channelIndex].rightPan;
    }

    @VisibleForTesting
    Context getContext() {
        return context;
    }

    /**
     * A playback position and loop-bookkeeping signature, used as a visited-set key to detect infinite pattern loops:
     * if the exact same signature recurs, the module is looping without ever reaching its end.
     */
    private record State(int patternIndex, int rowIndex, boolean loopPending, int loopRowIndex, int loopCounter) {
    }
}
