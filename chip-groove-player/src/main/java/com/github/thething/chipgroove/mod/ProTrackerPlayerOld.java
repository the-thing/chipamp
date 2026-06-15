package com.github.thething.chipgroove.mod;

import javax.sound.sampled.*;
import java.util.Arrays;

/**
 * Amiga ProTracker-compatible play loop in Java.
 *
 * Key Amiga timing facts reproduced here:
 *  - PAL  clock: 3_546_895 Hz  (most MODs are mastered on PAL)
 *  - NTSC clock: 3_579_545 Hz
 *  - Default BPM: 125, default Speed (ticks/row): 6
 *  - CIA timer tick interval (µs) = 2_500_000 / BPM   (Protracker formula)
 *  - Rows per pattern: 64, channels: 4
 *
 * Output: any sample rate you choose (e.g. 44100, 48000, 96000 Hz).
 * Mixing is done in software at that rate; period→frequency conversion uses
 * the real Amiga clock so pitch is always correct regardless of output rate.
 */
public class ProTrackerPlayerOld {

    // -----------------------------------------------------------------------
    // Amiga hardware constants
    // -----------------------------------------------------------------------

    /** PAL Amiga Paula clock (Hz). Use NTSC_CLOCK for NTSC machines. */
    public static final double PAL_CLOCK  = 3_546_895.0;
    public static final double NTSC_CLOCK = 3_579_545.0;

    /**
     * Convert an Amiga period value to a playback frequency (Hz).
     *
     * frequency = amigaClock / period
     *
     * Period 428 → middle C (C-3) ≈ 8287 Hz on PAL.
     * The mixer then re-samples this to whatever output rate you have chosen.
     */
    public static double periodToHz(int period, double amigaClock) {
        if (period <= 0) return 0.0;
        return amigaClock / period;
    }

    // -----------------------------------------------------------------------
    // Timing engine  (CIA timer / BPM / ticks)
    // -----------------------------------------------------------------------

    /**
     * Compute the length of one CIA tick in *output samples*.
     *
     * ProTracker CIA formula:
     *   tickDuration (µs) = 2_500_000 / BPM
     *
     * Converted to output samples:
     *   samplesPerTick = outputRate * tickDuration_µs / 1_000_000
     *                  = outputRate * 2_500_000 / (BPM * 1_000_000)
     *                  = outputRate * 2.5 / BPM
     *
     * At BPM=125, outputRate=44100:  44100 * 2.5 / 125 = 882 samples/tick  ✓
     */
    public static int samplesPerTick(int bpm, int outputRate) {
        return (int) Math.round((double) outputRate * 2_500_000.0 / (bpm * 1_000_000.0));
    }

    // -----------------------------------------------------------------------
    // Minimal data structures
    // -----------------------------------------------------------------------

    static class Sample {
        String name;
        byte[] data;         // signed 8-bit PCM, Amiga native format
        int    loopStart;    // in bytes
        int    loopLength;   // in bytes (0 = no loop / one-shot)
        int    volume;       // 0..64
        int    fineTune;     // -8..+7  (stored as 0..15 in .mod, convert: v>7 ? v-16 : v)

        Sample(int length) { data = new byte[length]; volume = 64; }
    }

    static class Note {
        int sampleNumber; // 1-based (0 = no sample)
        int period;       // Amiga period (0 = no note)
        int effect;       // 0x0..0xF
        int effectParam;  // 0x00..0xFF
    }

    static class Pattern {
        // [row][channel]
        Note[][] rows;
        Pattern() {
            rows = new Note[64][4];
            for (Note[] row : rows)
                Arrays.fill(row, new Note());
        }
    }

    // -----------------------------------------------------------------------
    // Channel state (Paula voice equivalent)
    // -----------------------------------------------------------------------

    static class Channel {
        // Current playback
        Sample sample;
        double samplePos;   // fractional position inside sample.data
        double increment;   // samples to advance per output sample
        int    volume;      // 0..64  (current, after volume effects)

        // For effects that need the "previous" period
        int    period;

        // Arpeggio / vibrato / etc. tick state
        int    effectMemory;

        void trigger(Sample s, int period, double amigaClock, int outputRate) {
            this.sample    = s;
            this.volume    = s != null ? s.volume : 0;
            this.period    = period;
            this.samplePos = 0.0;
            updateIncrement(period, amigaClock, outputRate);
        }

        void updateIncrement(int period, double amigaClock, int outputRate) {
            double noteHz  = periodToHz(period, amigaClock);
            this.increment = (outputRate > 0 && noteHz > 0) ? noteHz / outputRate : 0.0;
        }

        /** Mix one output sample from this channel (returns float in [-1,1]). */
        float nextSample() {
            if (sample == null || sample.data.length == 0) return 0f;

            int idx = (int) samplePos;
            float out = 0f;

            if (idx < sample.data.length) {
                out = sample.data[idx] / 128f;   // signed byte → [-1,1]
            }

            samplePos += increment;

            // Loop handling
            boolean looping = sample.loopLength > 2;
            if (looping) {
                double loopEnd = sample.loopStart + sample.loopLength;
                if (samplePos >= loopEnd)
                    samplePos = sample.loopStart + (samplePos - loopEnd) % sample.loopLength;
            } else {
                if (samplePos >= sample.data.length) {
                    samplePos = sample.data.length;   // silence after end
                    increment = 0;
                }
            }

            return out * (volume / 64f);
        }
    }

    // -----------------------------------------------------------------------
    // Main play loop
    // -----------------------------------------------------------------------

    static class PlayerState {
        // Song data
        Sample[]  samples;
        Pattern[] patterns;
        int[]     orderTable;   // sequence of pattern indices
        int       songLength;

        // Playback position
        int  orderPos   = 0;    // index into orderTable
        int  patternRow = 0;    // 0..63
        int  tick       = 0;    // current tick within row (0..speed-1)

        // Global timing
        int  speed      = 6;    // ticks per row (Fxx effect with x<=1F)
        int  bpm        = 125;  // beats per minute (Fxx effect with x>=20)

        Channel[] channels = new Channel[4];

        PlayerState() {
            for (int i = 0; i < 4; i++) channels[i] = new Channel();
        }
    }

    /**
     * The heart of the play loop.
     *
     * Call this once to fill one output buffer.  It advances the song
     * position by however many ticks fit in the requested number of samples.
     *
     * @param state        current player state (mutated in place)
     * @param outBuffer    stereo interleaved float[] (L,R,L,R,…)
     * @param numFrames    how many stereo frames to render
     * @param outputRate   output sample rate in Hz (e.g. 44100)
     * @param amigaClock   PAL_CLOCK or NTSC_CLOCK
     */
    public static void renderAudio(
            PlayerState state,
            float[]     outBuffer,
            int         numFrames,
            int         outputRate,
            double      amigaClock)
    {
        int sampTick = samplesPerTick(state.bpm, outputRate);
        int samplesLeftInTick = sampTick - (state.tick * sampTick); // simplified start offset
        // In a real implementation you'd persist "samplesRemainingInTick" across calls.

        int frameIdx = 0;

        while (frameIdx < numFrames) {

            // --- Process row/tick events if we're at a tick boundary ---
            if (samplesLeftInTick <= 0) {

                if (state.tick == 0) {
                    // --- Row trigger: read notes from the current pattern row ---
                    processRow(state, amigaClock, outputRate);
                } else {
                    // --- Intra-row ticks: apply effects that run every tick ---
                    processEffectTicks(state, amigaClock, outputRate);
                }

                state.tick++;
                if (state.tick >= state.speed) {
                    state.tick = 0;
                    advanceRow(state);
                }

                samplesLeftInTick = sampTick;
            }

            // --- Mix up to samplesLeftInTick frames (or buffer end) ---
            int toMix = Math.min(samplesLeftInTick, numFrames - frameIdx);

            for (int s = 0; s < toMix; s++) {
                float left  = 0f;
                float right = 0f;

                // Amiga hard panning: ch 0,3 → left, ch 1,2 → right
                left  += state.channels[0].nextSample();
                left  += state.channels[3].nextSample();
                right += state.channels[1].nextSample();
                right += state.channels[2].nextSample();

                // Soft-clip & store interleaved stereo
                outBuffer[(frameIdx + s) * 2]     = clamp(left  * 0.5f);
                outBuffer[(frameIdx + s) * 2 + 1] = clamp(right * 0.5f);
            }

            frameIdx           += toMix;
            samplesLeftInTick  -= toMix;
        }
    }

    // -----------------------------------------------------------------------
    // Row / effect processing
    // -----------------------------------------------------------------------

    private static void processRow(PlayerState st, double clock, int rate) {
        Pattern pat = st.patterns[st.orderTable[st.orderPos]];
        Note[]  row = pat.rows[st.patternRow];

        for (int ch = 0; ch < 4; ch++) {
            Note    n   = row[ch];
            Channel chn = st.channels[ch];
            Sample  smp = (n.sampleNumber > 0) ? st.samples[n.sampleNumber - 1] : null;

            // Trigger note (if period specified)
            if (n.period > 0 && smp != null) {
                chn.trigger(smp, n.period, clock, rate);
            } else if (smp != null) {
                // Sample number without new note: just reset volume
                chn.volume = smp.volume;
            }

            // Tick-0 effects
            switch (n.effect) {
                case 0xC -> chn.volume = Math.min(64, n.effectParam);         // Cxx Set Volume
                case 0xD -> handlePatternBreak(st, n.effectParam);             // Dxx Pattern Break
                case 0xF -> handleSetSpeedBpm(st, n.effectParam);              // Fxx Set Speed/BPM
                case 0xB -> handlePositionJump(st, n.effectParam);             // Bxx Jump to Order
                case 0xA -> chn.effectMemory = n.effectParam;                  // Axx Volume Slide (init)
                // … add more effects as needed
            }
        }
    }

    private static void processEffectTicks(PlayerState st, double clock, int rate) {
        Pattern pat = st.patterns[st.orderTable[st.orderPos]];
        Note[]  row = pat.rows[st.patternRow];

        for (int ch = 0; ch < 4; ch++) {
            Note    n   = row[ch];
            Channel chn = st.channels[ch];

            switch (n.effect) {
                case 0x0 -> applyArpeggio(chn, n.effectParam, st.tick, clock, rate);   // 0xy Arpeggio
                case 0x1 -> applyPortaUp(chn, n.effectParam, clock, rate);             // 1xx Porta Up
                case 0x2 -> applyPortaDown(chn, n.effectParam, clock, rate);           // 2xx Porta Down
                case 0xA -> applyVolumeSlide(chn, n.effectParam);                      // Axx Vol Slide
            }
        }
    }

    // -----------------------------------------------------------------------
    // Individual effect implementations
    // -----------------------------------------------------------------------

    private static void applyArpeggio(Channel ch, int param, int tick, double clock, int rate) {
        // tick 0 = normal, tick 1 = +semitone_x, tick 2 = +semitone_y, repeat
        int x = (param >> 4) & 0xF;
        int y =  param       & 0xF;
        int semitones = switch (tick % 3) {
            case 1 -> x;
            case 2 -> y;
            default -> 0;
        };
        // Shift period by semitones: period ÷ 2^(n/12)
        int newPeriod = (int) Math.round(ch.period / Math.pow(2.0, semitones / 12.0));
        ch.updateIncrement(newPeriod, clock, rate);
    }

    private static void applyPortaUp(Channel ch, int param, double clock, int rate) {
        ch.period = Math.max(113, ch.period - param);   // clamp to highest Amiga note
        ch.updateIncrement(ch.period, clock, rate);
    }

    private static void applyPortaDown(Channel ch, int param, double clock, int rate) {
        ch.period = Math.min(856, ch.period + param);   // clamp to lowest Amiga note
        ch.updateIncrement(ch.period, clock, rate);
    }

    private static void applyVolumeSlide(Channel ch, int param) {
        int up   = (param >> 4) & 0xF;
        int down =  param       & 0xF;
        ch.volume = Math.max(0, Math.min(64, ch.volume + up - down));
    }

    private static void handleSetSpeedBpm(PlayerState st, int param) {
        if (param == 0) return;           // 0 is ignored by ProTracker
        if (param < 0x20)
            st.speed = param;             // 1..31  → ticks per row
        else
            st.bpm   = param;             // 32..255 → BPM
    }

    private static void handlePatternBreak(PlayerState st, int param) {
        // BCD-encoded row:  high nibble*10 + low nibble
        int targetRow = ((param >> 4) * 10) + (param & 0xF);
        st.orderPos   = Math.min(st.orderPos + 1, st.songLength - 1);
        st.patternRow = Math.min(targetRow, 63);
        st.tick       = st.speed;         // force row advance skip
    }

    private static void handlePositionJump(PlayerState st, int param) {
        st.orderPos   = Math.min(param, st.songLength - 1);
        st.patternRow = 0;
        st.tick       = st.speed;
    }

    private static void advanceRow(PlayerState st) {
        st.patternRow++;
        if (st.patternRow >= 64) {
            st.patternRow = 0;
            st.orderPos++;
            if (st.orderPos >= st.songLength)
                st.orderPos = 0;   // loop song
        }
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    private static float clamp(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }

    // -----------------------------------------------------------------------
    // Demo: wire up javax.sound and play silence (replace with real .mod data)
    // -----------------------------------------------------------------------

    public static void main(String[] args) throws LineUnavailableException, InterruptedException {
        final int    OUTPUT_RATE  = 44100;
        final double AMIGA_CLOCK  = PAL_CLOCK;
        final int    BUFFER_FRAMES = 1024;   // frames per callback

        // Build a trivial one-pattern "song" with a single beep sample
        PlayerState state = new PlayerState();
        state.songLength = 1;
        state.orderTable = new int[]{0};
        state.patterns   = new Pattern[]{new Pattern()};
        state.samples    = new Sample[1];

        // Synthesise a 440 Hz sine wave as a looping Amiga 8-bit sample
        Sample testSample = new Sample(256);
        testSample.loopStart  = 0;
        testSample.loopLength = 256;
        testSample.volume     = 64;
        for (int i = 0; i < 256; i++)
            testSample.data[i] = (byte)(Math.sin(2 * Math.PI * i / 256.0) * 127);
        state.samples[0] = testSample;

        // C-3 on PAL = period 428  →  3546895/428 ≈ 8287 Hz
        // To get 440 Hz: period = 3546895/440 ≈ 8061
        int period440 = (int) Math.round(AMIGA_CLOCK / 440.0);

        Note testNote = new Note();
        testNote.sampleNumber = 1;
        testNote.period       = period440;
        state.patterns[0].rows[0][0] = testNote;  // channel 0, row 0

        // PCM_SIGNED 16-bit stereo — supported on every JVM/OS without fallback
        // 2 channels × 2 bytes = 4 bytes/frame, big-endian matches ShortBuffer easily
        AudioFormat fmt = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            OUTPUT_RATE, 16, 2, 4, OUTPUT_RATE, false);  // false = little-endian

        DataLine.Info info  = new DataLine.Info(SourceDataLine.class, fmt);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(fmt, BUFFER_FRAMES * 4 * 4);   // 4 buffer lengths
        line.start();

        float[]  floatBuf = new float[BUFFER_FRAMES * 2];   // L,R interleaved
        byte[]   byteBuf  = new byte [BUFFER_FRAMES * 4];   // 16-bit × 2 ch × 2 bytes

        System.out.printf("Output rate : %d Hz%n", OUTPUT_RATE);
        System.out.printf("Amiga clock : %.0f Hz (PAL)%n", AMIGA_CLOCK);
        System.out.printf("BPM         : %d%n", state.bpm);
        System.out.printf("Speed       : %d ticks/row%n", state.speed);
        System.out.printf("Tick length : %d samples  (%.2f ms)%n",
            samplesPerTick(state.bpm, OUTPUT_RATE),
            samplesPerTick(state.bpm, OUTPUT_RATE) * 1000.0 / OUTPUT_RATE);
        System.out.printf("Period 440Hz: %d  → %.2f Hz%n",
            period440, periodToHz(period440, AMIGA_CLOCK));

        long endTime = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < endTime) {
            renderAudio(state, floatBuf, BUFFER_FRAMES, OUTPUT_RATE, AMIGA_CLOCK);

            // Convert float [-1,1] → signed 16-bit little-endian bytes
            for (int i = 0; i < BUFFER_FRAMES * 2; i++) {
                short s = (short)(floatBuf[i] * 32767f);
                byteBuf[i * 2]     = (byte)( s       & 0xFF);  // low byte
                byteBuf[i * 2 + 1] = (byte)((s >> 8) & 0xFF);  // high byte
            }
            line.write(byteBuf, 0, byteBuf.length);
        }

        line.drain();
        line.close();
    }
}
