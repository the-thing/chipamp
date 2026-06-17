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
public class ProTrackerPlayer07 {

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

    /**
     * ProTracker finetune period table.
     *
     * ProTracker does NOT apply finetune with floating-point math — it looks up
     * the pre-tuned period from this table, exactly as the original source does.
     *
     * Layout: FINETUNE_PERIODS[fineTune+8][note]
     *   fineTune : -8..+7  → index 0..15   (stored nibble in .mod: 0..15, sign-extended: >7 → -16+v)
     *   note     : 0..35   (3 octaves: C-1..B-3, the range ProTracker supports)
     *
     * Row  0 = finetune -8
     * Row  8 = finetune  0  (no detuning)
     * Row 15 = finetune +7
     *
     * Source: ProTracker 2.3D source, mt_PeriodTable
     */
    public static final int[][] FINETUNE_PERIODS = {
        // finetune -8
        { 907,856,808,762,720,678,640,604,570,538,508,480,
          453,428,404,381,360,339,320,302,285,269,254,240,
          226,214,202,190,180,170,160,151,143,135,127,120 },
        // finetune -7
        { 900,850,802,757,715,675,636,601,567,535,505,477,
          450,425,401,379,357,337,318,300,284,268,253,238,
          225,212,200,189,179,169,159,150,142,134,126,119 },
        // finetune -6
        { 894,844,796,752,709,670,632,597,563,532,502,474,
          447,422,398,376,355,335,316,298,282,266,251,237,
          223,211,199,188,177,167,158,149,141,133,125,118 },
        // finetune -5
        { 887,838,791,746,704,665,628,592,559,528,498,470,
          444,419,395,373,352,332,314,296,280,264,249,235,
          222,209,198,187,176,166,157,148,140,132,125,118 },
        // finetune -4
        { 881,832,785,741,699,660,623,588,555,524,494,467,
          441,416,392,370,350,330,312,294,278,262,247,233,
          220,208,196,185,175,165,156,147,139,131,123,117 },
        // finetune -3
        { 875,826,779,736,694,655,619,584,551,520,491,463,
          437,413,390,368,347,328,309,292,276,260,245,232,
          219,206,195,184,174,164,155,146,138,130,123,116 },
        // finetune -2
        { 868,820,774,730,689,651,614,580,547,516,487,460,
          434,410,387,365,345,325,307,290,274,258,244,230,
          217,205,193,183,172,163,154,145,137,129,122,115 },
        // finetune -1
        { 862,814,768,725,684,646,610,575,543,513,484,457,
          431,407,384,363,342,323,305,288,272,256,242,228,
          216,204,192,181,171,161,152,144,136,128,121,114 },
        // finetune  0  (no detuning — the "normal" row)
        { 856,808,762,720,678,640,604,570,538,508,480,453,
          428,404,381,360,339,320,302,285,269,254,240,226,
          214,202,190,180,170,160,151,143,135,127,120,113 },
        // finetune +1
        { 850,802,757,715,674,637,601,567,535,505,477,450,
          425,401,379,357,337,318,300,284,268,253,238,225,
          212,200,189,179,169,159,150,142,134,126,119,113 },
        // finetune +2
        { 844,796,752,709,670,632,597,563,532,502,474,447,
          422,398,376,355,335,316,298,282,266,251,237,224,
          211,199,188,177,167,158,149,141,133,125,118,112 },
        // finetune +3
        { 838,791,746,704,665,628,592,559,528,498,470,444,
          419,395,373,352,332,314,296,280,264,249,235,222,
          209,198,187,176,166,157,148,140,132,125,118,111 },
        // finetune +4
        { 832,785,741,699,660,623,588,555,524,494,467,441,
          416,392,370,350,330,312,294,278,262,247,233,220,
          208,196,185,175,165,156,147,139,131,123,117,110 },
        // finetune +5
        { 826,779,736,694,655,619,584,551,520,491,463,437,
          413,390,368,347,328,309,292,276,260,245,232,219,
          206,195,184,174,164,155,146,138,130,123,116,110 },
        // finetune +6
        { 820,774,730,689,651,614,580,547,516,487,460,434,
          410,387,365,345,325,307,290,274,258,244,230,217,
          205,193,183,172,163,154,145,137,129,122,115,109 },
        // finetune +7
        { 814,768,725,684,646,610,575,543,513,484,457,431,
          407,384,363,342,323,305,288,272,256,242,228,216,
          204,192,181,171,161,152,144,136,128,121,114,108 },
    };

    /**
     * Look up the finetune-adjusted period for a given raw note period.
     *
     * ProTracker matches the raw period against the finetune-0 row to find the
     * note index, then returns the value from the correct finetune row.
     *
     * @param rawPeriod  period value from the pattern data
     * @param fineTune   -8..+7
     * @return           finetune-adjusted period, or rawPeriod if not found
     */
    public static int applyFineTune(int rawPeriod, int fineTune) {
        if (rawPeriod <= 0) return rawPeriod;
        int[] baseRow = FINETUNE_PERIODS[8];   // finetune 0
        for (int n = 0; n < baseRow.length; n++) {
            if (baseRow[n] == rawPeriod) {
                int ftIndex = fineTune + 8;    // -8..+7 → 0..15
                return FINETUNE_PERIODS[ftIndex][n];
            }
        }
        return rawPeriod;  // non-standard period: leave as-is
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
    /**
     * Exact samples-per-tick as a rational number numerator/denominator.
     * Use samplesPerTickAccurate() with a running accumulator to avoid drift.
     *
     * Exact value = outputRate * 2_500_000 / (BPM * 1_000_000)
     *             = outputRate * 5 / (BPM * 2)
     *
     * E.g. BPM=125, rate=44100: 44100*5 / 250 = 220500/250 = 882.0 exactly — lucky!
     * But BPM=120, rate=44100: 44100*5 / 240 = 220500/240 = 918.75 — fractional.
     * Rounding 918.75 to 919 every tick = +0.25 sample/tick error → song runs 0.027% slow.
     * Over a 3-minute song at speed=6: ~3500 rows * 6 ticks = 21000 ticks * 0.25 = 5250
     * extra samples = ~119 ms. Fine for most songs, but some trackers are more sensitive.
     *
     * The CIA-accurate fix: keep a sub-sample accumulator and alternate floor/ceil.
     */
    public static int samplesPerTick(int bpm, int outputRate) {
        // Kept for simple cases and effect-tick recompute where BPM just changed.
        // For the main loop use nextTickSamples() with the state accumulator.
        return (int) Math.round((double) outputRate * 2_500_000.0 / (bpm * 1_000_000.0));
    }

    /**
     * Return the integer sample count for this tick and advance the sub-sample
     * accumulator stored in PlayerState.  This is the Bresenham / DDA approach:
     * we keep the fractional remainder and add it each tick, emitting an extra
     * sample whenever it overflows 1.0 — exactly like the CIA timer counts.
     *
     * tickNumerator   = outputRate * 5
     * tickDenominator = BPM * 2
     * (derived from: outputRate * 2_500_000 / (BPM * 1_000_000) = outputRate*5/(BPM*2))
     */
    public static int nextTickSamples(PlayerState state, int outputRate) {
        // Recompute exact rational each time in case BPM changed this tick
        long num = (long) outputRate * 5;
        long den = (long) state.bpm * 2;
        long whole = num / den;
        // accumulator tracks fractional remainder * den to stay in integer arithmetic
        state.tickFracAccum += (num % den);
        if (state.tickFracAccum >= den) {
            state.tickFracAccum -= den;
            whole++;
        }
        return (int) whole;
    }

    /**
     * ProTracker sine table used by both vibrato and tremolo.
     * 32 entries covering a quarter... actually ProTracker uses a full
     * 32-step table representing one half cycle 0..255, mirrored for the
     * other half. This is the exact table from the original source.
     */
    public static final int[] SINE_TABLE = {
        0, 24, 49, 74, 97, 120, 141, 161, 180, 197, 212, 224, 235, 244, 250, 253,
        255, 253, 250, 244, 235, 224, 212, 197, 180, 161, 141, 120, 97, 74, 49, 24
    };

    /**
     * Returns the waveform value (-255..255) for a given waveform type and
     * position (0..63, wrapping). Shared by vibrato (effect 4xy) and
     * tremolo (effect 7xy) — both use the same waveform tables, set
     * independently via E4x (vibrato waveform) and E7x (tremolo waveform).
     */
    public static int waveformValue(int waveform, int pos) {
        pos = pos & 63;
        int idx = pos & 31;          // 0..31 index into the table
        int raw = SINE_TABLE[idx];
        switch (waveform & 3) {
            case 0: // sine
                return (pos < 32) ? raw : -raw;
            case 1: // ramp down (sawtooth)
                return (pos < 32) ? (255 - pos * 8) : -(255 - (pos - 32) * 8);
            case 2: // square
                return (pos < 32) ? 255 : -255;
            default:
                return (pos < 32) ? raw : -raw;
        }
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

        // Tremolo state (7xy effect)
        int    tremoloPos;     // 0..63, position in the waveform table
        int    tremoloSpeed;   // x nibble: how much tremoloPos advances per tick
        int    tremoloDepth;   // y nibble: amplitude of the volume swing
        int    tremoloWaveform; // 0=sine, 1=ramp down, 2=square (set via E7x, default 0)
        int    volumeBeforeTremolo; // the "real" volume, tremolo offsets from this temporarily

        void trigger(Sample s, int period, double amigaClock, int outputRate) {
            this.sample    = s;
            this.volume    = s != null ? s.volume : 0;
            this.samplePos = 0.0;
            // Apply sample finetune: look up the detuned period from the table.
            // fineTune is stored as 0..15 in the .mod nibble; values >7 mean negative
            // (-8..-1), so the Sample loader must sign-extend: v > 7 ? v - 16 : v
            int tunedPeriod = (s != null && s.fineTune != 0)
                ? applyFineTune(period, s.fineTune)
                : period;
            this.period = tunedPeriod;
            updateIncrement(tunedPeriod, amigaClock, outputRate);
            // Reset tremolo waveform position on a new note. Real ProTracker
            // only suppresses this when E7x bit 2 ("don't retrigger") is set —
            // not implemented here; add a `tremoloNoRetrig` flag if needed.
            this.tremoloPos = 0;
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
                // Signed 8-bit: -128..+127. Divide by 128 so -128 → -1.0
                // and +127 → +0.9921. Using 128 (not 127) matches Amiga Paula
                // which treats the byte as a signed 8-bit DAC with range -128..127.
                out = sample.data[idx] / 128f;
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

            // Volume 0..64: Paula hardware volume register is 6-bit (0..63) but
            // ProTracker allows 64 as a special "full volume" value = same as 63.
            // Clamp to 64 and divide by 64 so volume=64 → 1.0 (no attenuation).
            return out * (Math.min(volume, 64) / 64f);
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

        // Persisted across renderAudio() calls — how many output samples remain
        // in the current tick. Must survive buffer boundaries or timing drifts.
        int  samplesRemainingInTick = 0;
        // Sub-sample accumulator for drift-free tick timing (Bresenham DDA).
        // Stores the fractional remainder * denominator in integer form.
        long tickFracAccum = 0;

        // Deferred jump state — set during processRow(), applied in advanceRow().
        // ProTracker processes Bxx then Dxx; Dxx overrides the order increment from Bxx.
        boolean jumpPending      = false;  // Bxx position jump pending
        int     jumpOrder        = 0;
        boolean breakPending     = false;  // Dxx pattern break pending
        int     breakRow         = 0;

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
        // samplesRemainingInTick is persisted in state across buffer calls.
        // Initialise on very first call (tick==0, remaining==0).
        if (state.samplesRemainingInTick <= 0) {
            // Very first call: process row 0 immediately and start counting
            // the first real tick. All subsequent ticks are handled by the
            // main loop below.
            processRow(state, amigaClock, outputRate);
            state.tick = 1;
            state.samplesRemainingInTick = nextTickSamples(state, outputRate);
        }

        int frameIdx = 0;

        while (frameIdx < numFrames) {

            // --- Process row/tick events if we're at a tick boundary ---
            if (state.samplesRemainingInTick <= 0) {

                if (state.tick == 0) {
                    // Tick 0: read notes from the current pattern row
                    processRow(state, amigaClock, outputRate);
                } else {
                    // Ticks 1..speed-1: run per-tick effects only
                    processEffectTicks(state, amigaClock, outputRate);
                }

                state.tick++;
                if (state.tick >= state.speed) {
                    state.tick = 0;
                    advanceRow(state);
                }

                // Recompute tick length — BPM may have changed via Fxx.
                // nextTickSamples() advances the sub-sample accumulator to avoid drift.
                state.samplesRemainingInTick = nextTickSamples(state, outputRate);
            }

            // --- Mix up to samplesRemainingInTick frames (or buffer end) ---
            int toMix = Math.min(state.samplesRemainingInTick, numFrames - frameIdx);

            for (int s = 0; s < toMix; s++) {
                float left  = 0f;
                float right = 0f;

                // Amiga hard panning: ch 0,3 → left, ch 1,2 → right
                left  += state.channels[0].nextSample();
                left  += state.channels[3].nextSample();
                right += state.channels[1].nextSample();
                right += state.channels[2].nextSample();

                // Each side is the sum of 2 channels, each in [-1, 1],
                // so the theoretical max is 2.0. Dividing by 2.0 is mathematically
                // safe but wastes half the output dynamic range — most mod players
                // do NOT pre-attenuate and instead let the clamp handle rare peaks.
                // We match that behaviour: output at full range, clamp only on overflow.
                // If you hear distortion on a specific MOD, lower MASTER_VOLUME below.
                final float MASTER_VOLUME = 1.0f; // reduce if clipping occurs
                outBuffer[(frameIdx + s) * 2]     = clamp(left  * MASTER_VOLUME);
                outBuffer[(frameIdx + s) * 2 + 1] = clamp(right * MASTER_VOLUME);
            }

            frameIdx                       += toMix;
            state.samplesRemainingInTick -= toMix;
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
                case 0xA -> { if (n.effectParam != 0) chn.effectMemory = n.effectParam; } // Axx: store param, keep last if A00
                case 0x7 -> {                                                  // 7xy Tremolo (init on tick 0)
                    int x = (n.effectParam >> 4) & 0xF;
                    int y =  n.effectParam       & 0xF;
                    if (x != 0) chn.tremoloSpeed = x;   // 0 means "keep last speed"
                    if (y != 0) chn.tremoloDepth = y;   // 0 means "keep last depth"
                    // Remember the volume tremolo should oscillate around.
                    // A fresh note trigger already set chn.volume to the sample's
                    // base volume above, so capture it before any offset is applied.
                    chn.volumeBeforeTremolo = chn.volume;
                }
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
                case 0xA -> applyVolumeSlide(chn, chn.effectMemory);                    // Axx Vol Slide (uses stored param, supports A00 continue)
                case 0x7 -> applyTremolo(chn);                                          // 7xy Tremolo (ticks 1+)
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

    /**
     * 7xy Tremolo — oscillates volume around the channel's base volume.
     *
     * ProTracker formula:
     *   delta  = (depth * waveformValue(waveform, pos)) / 64
     *   volume = clamp(volumeBeforeTremolo + delta, 0, 64)
     *
     * Like vibrato, this NEVER permanently changes chn.volume's "real" value —
     * volumeBeforeTremolo is the value Cxx/Axx set, and tremolo offsets from
     * it fresh every tick so a Cxx on a later row isn't fighting stale tremolo
     * state. The position counter advances every tick (not just when the
     * effect is reapplied) and only resets on a new note trigger.
     */
    private static void applyTremolo(Channel ch) {
        int waveform = ch.tremoloWaveform;
        int raw      = waveformValue(waveform, ch.tremoloPos);   // -255..255
        int delta    = (ch.tremoloDepth * raw) / 64;
        ch.volume    = Math.max(0, Math.min(64, ch.volumeBeforeTremolo + delta));
        ch.tremoloPos += ch.tremoloSpeed;
        // tremoloPos wraps naturally via & 63 inside waveformValue()
    }

    private static void handleSetSpeedBpm(PlayerState st, int param) {
        if (param == 0) return;           // 0 is ignored by ProTracker
        if (param < 0x20)
            st.speed = param;             // 1..31  → ticks per row
        else
            st.bpm   = param;             // 32..255 → BPM
    }

    private static void handlePatternBreak(PlayerState st, int param) {
        // BCD-encoded row: high nibble*10 + low nibble
        int targetRow = ((param >> 4) * 10) + (param & 0xF);
        st.breakPending = true;
        st.breakRow     = Math.min(targetRow, 63);
        // Note: do NOT touch st.tick here — advanceRow() will handle the jump
        // at the natural end of the row (after all speed ticks are consumed).
    }

    private static void handlePositionJump(PlayerState st, int param) {
        st.jumpPending = true;
        st.jumpOrder   = Math.min(param, st.songLength - 1);
        // Dxx on the same row will override the order target set here.
    }

    private static void advanceRow(PlayerState st) {
        if (st.jumpPending || st.breakPending) {
            // Resolve Bxx + Dxx interaction (ProTracker rule):
            //   Bxx alone  → jump to order jumpOrder, row 0
            //   Dxx alone  → jump to orderPos+1, row breakRow
            //   Both       → jump to order jumpOrder, row breakRow
            if (st.jumpPending && !st.breakPending) {
                st.orderPos   = st.jumpOrder;
                st.patternRow = 0;
            } else if (st.breakPending && !st.jumpPending) {
                st.orderPos   = Math.min(st.orderPos + 1, st.songLength - 1);
                st.patternRow = st.breakRow;
            } else {
                // Both: Bxx sets order, Dxx sets row
                st.orderPos   = st.jumpOrder;
                st.patternRow = st.breakRow;
            }
            if (st.orderPos >= st.songLength) st.orderPos = 0;
            st.jumpPending  = false;
            st.breakPending = false;
        } else {
            st.patternRow++;
            if (st.patternRow >= 64) {
                st.patternRow = 0;
                st.orderPos++;
                if (st.orderPos >= st.songLength)
                    st.orderPos = 0;
            }
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
