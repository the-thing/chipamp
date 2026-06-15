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

public class ProtoPlayer {

    private static final int CHANNEL_COUNT = 8;
    private static final int PAL_CLOCK_HZ = 3_546_895;
    private static final int NTSC_CLOCK_HZ = 3_579_545;

    private static final int DEFAULT_SPEED = 6;
    private static final int DEFAULT_TEMPO = 126;
    private static final int DEFAULT_OUTPUT_RATE = 44_100;

    private final Channel[] channels;

    private int clock;
    private int speed; // ticks per row
    private int tempo; // beats per minute
    private int samplesPerTick;
    private int samplesLeftInTick;

    private int patternSequenceIndex;
    private int rowIndex;
    private int tickIndex;

    private int outputRate;
    private boolean stereo;

    public ProtoPlayer() {
        this.channels = new Channel[CHANNEL_COUNT];

        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channels[i] = new Channel();
        }

        speed = DEFAULT_SPEED;
        tempo = DEFAULT_TEMPO;
        clock = PAL_CLOCK_HZ;
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

    /**
     * Convert an Amiga period value to a playback frequency (Hz).
     * <p>
     * frequency = amigaClock / period
     * <p>
     * Period 428 → middle C (C-3) ≈ 8287 Hz on PAL. The mixer then re-samples this to whatever output rate you have
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

        samplesPerTick = samplesPerTick(tempo, outputRate);
        samplesLeftInTick = samplesPerTick;

        while (patternSequenceIndex < mod.getLength()) {

            if (samplesLeftInTick <= 0) {
                if (tickIndex == 0) {
                    processNewRow(mod, clock, outputRate);
                } else {
                    processTick();
                }

                tickIndex++;

                if (tickIndex >= speed) {
                    tickIndex = 0;
                    rowIndex++;

                    if (rowIndex >= 64) {
                        rowIndex = 0;
                        patternSequenceIndex++;

                        if (patternSequenceIndex < mod.getLength()) {
                            int patternIndex = mod.getPatternIndex(patternSequenceIndex);
                            System.out.println("new pattern: " + patternIndex);
                        }
                    }
                }

                samplesLeftInTick = samplesPerTick;
            }

            float left = 0.0f;
            float right = 0.0f;

            left += channels[0].nextSample(mod);
            left += channels[3].nextSample(mod);
            right += channels[1].nextSample(mod);
            right += channels[2].nextSample(mod);

            left = Maths.clamp(left * 0.5f, -1.0f, 1.0f);
            right = Maths.clamp(right * 0.5f, -1.0f, 1.0f);

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

            // line.write(data, 0, 4);

            samplesLeftInTick--;
        }

        line.drain();
        line.close();
    }

    private void processNewRow(Mod mod, double clock, int rate) {
        for (int channelIndex = 0; channelIndex < mod.getChannelCount(); channelIndex++) {
            int patternIndex = mod.getPatternIndex(patternSequenceIndex);
            Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);
            Channel channel = channels[channelIndex];
            Sample sample = instrument.sampleNumber() > 0 ? mod.getSample(instrument.sampleNumber() - 1) : null;

            if (sample != null && instrument.period() > 0) {
                channel.sampleNumber = instrument.sampleNumber();
                channel.samplePositionDouble = 0.0;
                channel.volume = sample.getVolume();
                channel.period = instrument.period();

                double noteHz = periodToHz(instrument.period(), clock);
                channel.sampleIncrementDouble = (rate > 0 && noteHz > 0) ? noteHz / rate : 0;
            }

            if (sample != null) {
                channel.volume = sample.getVolume();
            }

            // TODO process some effects
            switch (instrument.effect()) {
                case SET_SPEED -> effectSetSpeed(instrument.effectArgumentX(), instrument.effectArgumentY());
            }
        }
    }

    private void effectSetSpeed(int argX, int argY) {
        int arg = (argX << 4) | argY;

        if (arg < 0x20) {
            System.out.println("EFFECT SET SPEED: " + arg);
            speed = arg;
        } else {
            System.out.println("EFFECT SET TEMPO: " + arg);
            tempo = arg;
            samplesPerTick = samplesPerTick(tempo, outputRate);
        }
    }

    private void processTick() {
    }

    public static void main(String[] args) throws IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader();
        Mod mod = modLoader.load("Hoffman - Eon.mod");

        System.out.println("Mod length = " + mod.getLength());

        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 1024);

        ProtoPlayer player = new ProtoPlayer();
        player.play(mod, buffer);

        buffer.flip();

        byte[] audio = new byte[buffer.remaining()];
        buffer.get(audio);

        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, DEFAULT_OUTPUT_RATE, 16, 2, 4, DEFAULT_OUTPUT_RATE, false);
        Resources.saveAudio(new File("eon.wav"), format, audio);
    }
}
