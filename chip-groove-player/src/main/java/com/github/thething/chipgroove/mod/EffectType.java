package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.common.Maths;

import java.util.Arrays;

public enum EffectType implements Effect {

    ARPEGGIO(0x00) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            channel.arpeggioPosition = 0;
            channel.arpeggioPeriod = channel.period;

            // it possible that the later ARPEGGIO rows do not have the note so we have to reset the increment based
            // on the current (last) note
            // A#5 06 037
            // ... 08 037
            channel.updateIncrement(channel.period, config.clockHz, config.samplingRate);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            channel.arpeggioPosition++;

            int semitones = switch (channel.arpeggioPosition % 3) {
                case 1 -> channel.effectArgumentX;
                case 2 -> channel.effectArgumentY;
                default -> 0;
            };

            int newPeriod = ModTables.shiftPeriodBySemitones(channel.arpeggioPeriod, channel.sample.fineTune(), semitones);
            channel.updateIncrement(newPeriod, config.clockHz, config.samplingRate);
        }
    },

    SLIDE_UP(0x01) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {

        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            int adjustment = (channel.effectArgumentX << 4) | channel.effectArgumentY;
            int newPeriod = Maths.clamp(channel.period - adjustment, config.minPeriod, config.maxPeriod);
            channel.updatePeriodAndIncrement(newPeriod, config.clockHz, config.samplingRate);
        }
    },

    SLIDE_DOWN(0x02) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            int adjustment = (channel.effectArgumentX << 4) | channel.effectArgumentY;
            int newPeriod = Maths.clamp(channel.period + adjustment, config.minPeriod, config.maxPeriod);
            channel.updatePeriodAndIncrement(newPeriod, config.clockHz, config.samplingRate);
        }
    },

    TONE_PORTAMENTO(0x03) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int portamentoSpeed = (channel.effectArgumentX << 4) | channel.effectArgumentY;

            if (portamentoSpeed != 0) {
                channel.portamentoSpeed = portamentoSpeed;
            }
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            EffectType.applyTonePortamento(channel, config);
        }
    },

    VIBRATO(0x04) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // continue with previous vibrato speed if not provided
            if (channel.effectArgumentX != 0) {
                channel.vibratoSpeed = channel.effectArgumentX;
            }

            // continue with previous vibrato amplitude if not provided
            if (channel.effectArgumentY != 0) {
                channel.vibratoAmplitude = channel.effectArgumentY;
            }

            channel.vibratoPeriod = channel.period;
            // supposedly waveforms are not reset on effect start
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            EffectType.applyToneVibrato(channel, config);
        }
    },

    TONE_PORTAMENTO_WITH_VOLUME_SLIDE(0x05) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // tone portamento with volume slide doesn't store portamento arguments
            // it just continues the old portamento if present
            EffectType.storeVolumeSlide(channel, config.volumeSlideDeltaEnabled);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            EffectType.applyTonePortamento(channel, config);
            EffectType.applyVolumeSlide(channel);
        }
    },

    VIBRATO_WITH_VOLUME_SLIDE(0x06) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            EffectType.storeVolumeSlide(channel, config.volumeSlideDeltaEnabled);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            EffectType.applyToneVibrato(channel, config);
            EffectType.applyVolumeSlide(channel);
        }
    },

    TREMOLO(0x07) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // use old tremolo speed if not specified
            if (channel.effectArgumentX != 0) {
                channel.tremoloSpeed = channel.effectArgumentX;
            }

            // use old tremolo depth if not specified
            if (channel.effectArgumentY != 0) {
                channel.tremoloAmplitude = channel.effectArgumentY;
            }

            channel.tremoloVolume = channel.volume;
            // supposedly waveforms are not reset on effect start
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            WaveformType tremoloWaveformType = channel.tremoloWaveformType;
            int waveformValue = ModTables.getWaveformValue(tremoloWaveformType, channel.tremoloPosition);
            int delta = (channel.tremoloAmplitude * waveformValue) / 64;

            channel.volume = Maths.clamp(channel.tremoloVolume + delta, 0, 64);
            channel.tremoloPosition += channel.tremoloSpeed;
        }
    },

    SET_PANNING_POSITION(0x08) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // original amiga doesn't support this effect
            int panningPosition = (channel.effectArgumentX) << 4 | channel.effectArgumentY;
            channel.setPanning(panningPosition / 255.0f);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    SET_SAMPLE_OFFSET(0x09) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            channel.samplePosition = ((channel.effectArgumentX << 4) | channel.effectArgumentY) * 256;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {

        }
    },

    VOLUME_SLIDE(0x0A) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            EffectType.storeVolumeSlide(channel, config.volumeSlideDeltaEnabled);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            EffectType.applyVolumeSlide(channel);
        }
    },

    POSITION_JUMP(0x0B) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int jumpSequenceIndex = (channel.effectArgumentX << 4) | channel.effectArgumentY;
            context.jumpPending = true;
            context.jumpSequenceIndex = jumpSequenceIndex;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    SET_VOLUME(0x0C) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int volume = (channel.effectArgumentX << 4) | channel.effectArgumentY;
            channel.volume = Maths.clamp(volume, 0, 64);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {

        }
    },

    PATTERN_BREAK(0x0D) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int row = channel.effectArgumentX * 10 + channel.effectArgumentY;
            context.breakPending = true;
            context.breakRowIndex = Maths.clamp(row, 0, 63);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {

        }
    },

    EXTENDED_EFFECT(0x0E) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            channel.extendedEffectType.onNewRow(channel, context, config);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            channel.extendedEffectType.onMidRow(channel, context, config);
        }
    },

    SET_SPEED(0x0F) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int speedOrTempo = (channel.effectArgumentX << 4) | channel.effectArgumentY;

            if (speedOrTempo < 0x20) {
                context.speed = speedOrTempo;
            } else {
                context.updateTempoAndSamplesPerTick(speedOrTempo, config.samplingRate);
            }
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {

        }
    },

    NONE(0xFF) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {

        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {

        }
    };

    private static final EffectType[] EFFECT_TYPE_BY_CODE;

    static {
        EFFECT_TYPE_BY_CODE = new EffectType[256];
        Arrays.fill(EFFECT_TYPE_BY_CODE, 16, EFFECT_TYPE_BY_CODE.length, NONE);
        System.arraycopy(values(), 0, EFFECT_TYPE_BY_CODE, 0, values().length);
    }

    private final int code;

    EffectType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static EffectType valueOf(int code) {
        return EFFECT_TYPE_BY_CODE[code];
    }

    private static void applyTonePortamento(Channel channel, Config config) {
        if (channel.portamentoTargetPeriod == 0 || channel.period == channel.portamentoTargetPeriod) {
            return;
        }

        int newPeriod;

        if (channel.period > channel.portamentoTargetPeriod) {
            newPeriod = Math.max(channel.portamentoTargetPeriod, channel.period - channel.portamentoSpeed);
        } else {
            newPeriod = Math.min(channel.portamentoTargetPeriod, channel.period + channel.portamentoSpeed);
        }

        newPeriod = Maths.clamp(newPeriod, config.minPeriod, config.maxPeriod);
        channel.updatePeriodAndIncrement(newPeriod, config.clockHz, config.samplingRate);
    }

    private static void applyToneVibrato(Channel channel, Config config) {
        WaveformType vibratoWaveformType = channel.vibratoWaveformType;
        int waveformValue = ModTables.getWaveformValue(vibratoWaveformType, channel.vibratoPosition);
        int delta = (channel.vibratoAmplitude * waveformValue) / 64;

        channel.period = Maths.clamp(channel.vibratoPeriod + delta, config.minPeriod, config.maxPeriod);
        channel.vibratoPosition += channel.vibratoSpeed;
    }

    /**
     * Decide which parameter to use for the volume slide based on configuration.
     */
    private static void storeVolumeSlide(Channel channel, boolean volumeSlideDelta) {
        if (channel.effectArgumentX != 0 && channel.effectArgumentY != 0) {
            // it is possible that both arguments are not zero
            if (volumeSlideDelta) {
                // compute delta
                channel.volumeSlide = channel.effectArgumentX - channel.effectArgumentY;
            } else {
                // prioritize x over delta (x - y)
                channel.volumeSlide = channel.effectArgumentX;
            }
        } else if (channel.effectArgumentX != 0) {
            channel.volumeSlide = channel.effectArgumentX;
        } else if (channel.effectArgumentY != 0) {
            channel.volumeSlide = -channel.effectArgumentY;
        }

        // when both arguments are zero, we retain old volume slide
    }

    private static void applyVolumeSlide(Channel channel) {
        channel.volume = Maths.clamp(channel.volume + channel.volumeSlide, 0, 64);
    }
}
