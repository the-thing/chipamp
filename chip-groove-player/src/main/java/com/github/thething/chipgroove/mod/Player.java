package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.common.Maths;
import com.github.thething.chipgroove.io.Resources;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class Player {

    private static final int CHANNEL_COUNT = 8;

    public static final int PAL_CLOCK_HZ = 3_546_895;
    public static final int NTSC_CLOCK_HZ = 3_579_545;

    private static final int DEFAULT_SPEED = 6;
    private static final int DEFAULT_TEMPO = 126;
    private static final int DEFAULT_OUTPUT_RATE = 44_100;

    private final Channel[] channels;

    private int clockHz;
    private int speed; // ticks per row
    private int tempo; // beats per minute
    private int samplesPerTick;
    private int samplesRemainingInCurrentTick;
    private long tickFracAccum;

    private boolean jumpPending = false;
    private int jumpOrder = 0;
    private boolean breakPending = false;
    private int breakRow = 0;

    private int patternSequenceIndex;
    private int rowIndex;
    private int tickIndex;

    private int outputRate;

    public Player() {
        this.channels = new Channel[CHANNEL_COUNT];

        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channels[i] = new Channel();
        }

        speed = DEFAULT_SPEED;
        tempo = DEFAULT_TEMPO;
        clockHz = PAL_CLOCK_HZ;
        outputRate = DEFAULT_OUTPUT_RATE;
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

    // TODO not sure if this is required
    public int nextTickSamples(int tempo, int outputRate) {
        // Recompute exact rational each time in case BPM changed this tick
        long num = (long) outputRate * 5;
        long den = (long) tempo * 2;
        long whole = num / den;

        // accumulator tracks fractional remainder * den to stay in integer arithmetic

        tickFracAccum += (num % den);

        if (tickFracAccum >= den) {
            tickFracAccum -= den;
            whole++;
        }

        return (int) whole;
    }

    /**
     * Convert a period value to a playback frequency (Hz).
     * <p>
     * frequency = clock / period
     * <p>
     * Period 428 → middle C (C-3) = 8287 Hz on PAL. The mixer then re-samples this to whatever output rate you have
     * chosen.
     */
    public static double periodToHz(int period, double clock) {
        if (period <= 0) {
            return 0.0;
        }

        return clock / period;
    }

    public void play(Mod mod, ByteBuffer buffer) throws LineUnavailableException {
        resetChannels();

        // PCM_SIGNED 16-bit stereo — supported on every JVM/OS without fallback
        // 2 channels × 2 bytes = 4 bytes/frame, big-endian matches ShortBuffer easily
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, outputRate, 16, 2, 4, outputRate, false);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        // TODO check if we can replace sample tick
        samplesPerTick = samplesPerTick(tempo, outputRate);
        samplesRemainingInCurrentTick = samplesPerTick;

        while (patternSequenceIndex < mod.getLength()) {

            if (samplesRemainingInCurrentTick <= 0) {
                if (tickIndex == 0) {
                    handleNewRow(mod, clockHz, outputRate);
                } else {
                    handleMidRow(mod);
                }

                tickIndex++;

                if (tickIndex >= speed) {
                    tickIndex = 0;
                    advanceRow(mod.getLength());
                }

                samplesRemainingInCurrentTick = samplesPerTick;
            }

            float left = 0.0f;
            float right = 0.0f;

            left += channels[0].nextSample(mod);
            left += channels[3].nextSample(mod);
            right += channels[1].nextSample(mod);
            right += channels[2].nextSample(mod);

            left = Maths.clamp(left, -1.0f, 1.0f);
            right = Maths.clamp(right, -1.0f, 1.0f);

            short lefty = (short) (left * 32767.0f);
            short righty = (short) (right * 32767.0f);
            byte[] data = new byte[4];

            data[0] = (byte) (lefty & 0xFF);
            data[1] = (byte) ((lefty >> 8) & 0xFF);
            data[2] = (byte) (righty & 0xFF);
            data[3] = (byte) ((righty >> 8) & 0xFF);

            buffer.put(data[0]);
            buffer.put(data[1]);
            buffer.put(data[2]);
            buffer.put(data[3]);

            line.write(data, 0, 4);

            samplesRemainingInCurrentTick--;
        }

        line.drain();
        line.close();
    }

    private void advanceRow(int modLength) {
        if (jumpPending || breakPending) {
            // Resolve Bxx + Dxx interaction (ProTracker rule):
            // Bxx alone  → jump to order jumpOrder, row 0
            // Dxx alone  → jump to orderPos+1, row breakRow
            // Both       → jump to order jumpOrder, row breakRow

            if (jumpPending && !breakPending) {
                patternSequenceIndex = jumpOrder;
                rowIndex = 0;
            } else if (breakPending && !jumpPending) {
                patternSequenceIndex = Math.min(patternSequenceIndex + 1, modLength - 1);
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

                System.out.println("Pattern sequence index: " + patternSequenceIndex);
            }
        }
    }

    private void handleNewRow(Mod mod, double clock, int rate) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            int patternIndex = mod.getPatternIndex(patternSequenceIndex);
            Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
            Channel channel = channels[channelIndex];
            Sample sample = instrument.sampleNumber() > 0 ? mod.getSample(instrument.sampleNumber() - 1) : null;
            int previousSampleNumber = channel.sampleNumber;

            if (sample != null && instrument.period() > 0) {
                channel.sampleNumber = instrument.sampleNumber();
                channel.samplePosition = 0.0;
                channel.volume = sample.getVolume();
                channel.period = instrument.period();

                double noteHz = periodToHz(instrument.period(), clock);
                channel.sampleIncrement = (rate > 0 && noteHz > 0) ? noteHz / rate : 0;
            }

            if (sample != null) {
                channel.volume = sample.getVolume();
            }

            // TODO refactor to handle effects in separate methods

            Effect previousEffect = channel.effect;
            ExtendedEffect previousExtendedEffect = channel.extendedEffect;
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

                case TONE_PORTAMENTO -> {
                    // TODO
                    System.out.println("UNKNOWN EFFECT: " + instrument.effect());
                }

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

                case TREMOLO -> {
                    // TODO
                    System.out.println("UNKNOWN EFFECT: " + instrument.effect());
                }

                case SET_PANNING_POSITION -> {
                    // TODO
                    System.out.println("UNKNOWN EFFECT: " + instrument.effect());
                }

                case SET_SAMPLE_OFFSET ->
                        effectSetSampleOffset(channel, sample, previousSampleNumber, instrument.effectArgumentX(), instrument.effectArgumentY());

                case VOLUME_SLIDE ->
                        effectVolumeSlideNewRow(channel, previousEffect, prevEffectArgumentX, prevEffectArgumentY, instrument.effectArgumentX(), instrument.effectArgumentY());

                case POSITION_JUMP -> {
                    // TODO
                    System.out.println("UNKNOWN EFFECT: " + instrument.effect());
                }

                case SET_VOLUME -> effectSetVolume(channel, instrument.effectArgumentX(), instrument.effectArgumentY());

                case PATTERN_BREAK -> effectPatternBreak(instrument.effectArgumentX(), instrument.effectArgumentY());

                case EXTENDED_EFFECT -> {
                    // TODO extract
                    switch (instrument.extendedEffect()) {
                        case FINE_VOLUME_SLIDE_UP -> effectFineVolumeSlideUp(channel, instrument.effectArgumentY());
                        case FINE_VOLUME_SLIDE_DOWN -> effectFineVolumeSlideDown(channel, instrument.effectArgumentY());
                    }
                }

                case SET_SPEED -> effectSetSpeed(instrument.effectArgumentX(), instrument.effectArgumentY());
            }
        }
    }

    private void handleMidRow(Mod mod) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            Channel channel = channels[channelIndex];

            switch (channel.effect) {
                case SLIDE_UP -> effectSlideUp(channel);
                case VOLUME_SLIDE -> effectVolumeSlide(channel);
            }
        }
    }

    private void effectSlideUp(Channel channel) {
        int adjustment = (channel.effectArgumentX << 4) | channel.effectArgumentY;
        channel.period = Maths.clamp(channel.period - adjustment, 57, 1712);
    }

    private void effectSlideDown(Channel channel) {
        // TODO
    }

    private void effectSetSampleOffset(Channel channel, Sample sample, int previousSampleNumber, int argX, int argY) {
        int offset = ((argX << 4) | argY) * 256;
        int wordOffset = argX * 4096 + argY * 256;

        System.out.println("SET SAMPLE OFFSET " + offset + " / " + wordOffset + " / " + sample.getDataLength());
        // System.out.println("Sample: " + sample.ge);

        if (sample == null) {
            // if sample was not provided retrigger last sample
            System.out.println("Retrigger last sample");
            channel.sampleNumber = previousSampleNumber;
            // TOOD we probably need to set offset and increment
        }

        channel.samplePosition = offset;
    }

    /**
     * If the current effect is A00 (volume slide with both arguments equal to zero) and the previous effect was a
     * volume slide than inherit arguments from previous slide.
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
        // System.out.println("SET VOLUME " + arg);
        channel.volume = arg;
    }

    private void effectPatternBreak(int argX, int argY) {
        int arg = argX * 10 + argY;
        Math.min(arg, 63);
        // System.out.println("PATTERN BREAK " + arg);
        breakPending = true;
        breakRow = arg;
    }

    // TODO speed at zero should probably stop playing
    private void effectSetSpeed(int argX, int argY) {
        int arg = (argX << 4) | argY;

        if (arg < 0x20) {
            // System.out.println("EFFECT SET SPEED: " + arg);
            speed = arg;
        } else {
            // System.out.println("EFFECT SET TEMPO: " + arg);
            tempo = arg;
            samplesPerTick = samplesPerTick(tempo, outputRate);
        }
    }

    private void effectFineVolumeSlideUp(Channel channel, int argY) {
        channel.volume = Maths.clamp(channel.volume + argY, 0, 64);
    }

    private void effectFineVolumeSlideDown(Channel channel, int argY) {
        channel.volume = Maths.clamp(channel.volume - argY, 0, 64);
    }

    public static void main(String[] args) throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader();
        Mod mod = modLoader.load("Jogeir Liljedahl - Nearly There.mod");

        System.out.println("length " + mod.getLength() + " / " + mod.getPatternSequenceCount());

        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 1024);

        Player player = new Player();
        player.play(mod, buffer);

        buffer.flip();

        byte[] audio = new byte[buffer.remaining()];
        buffer.get(audio);

        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, DEFAULT_OUTPUT_RATE, 16, 2, 4, DEFAULT_OUTPUT_RATE, false);
        Resources.saveAudio(new File("nearly.wav"), format, audio);
    }
}
