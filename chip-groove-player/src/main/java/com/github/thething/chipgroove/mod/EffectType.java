package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.common.Maths;

import java.util.Arrays;

public enum EffectType implements Effect {

    ARPEGGIO(0x00) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("ARPEGGIO not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
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
            channel.updatePeriod(newPeriod, config.clockHz, config.samplingRate);
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
            channel.updatePeriod(newPeriod, config.clockHz, config.samplingRate);
        }
    },

    TONE_PORTAMENTO(0x03) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // store new period as portamento period
            channel.portamentoPeriod = channel.period;

            // restore period and sample position from previous row
            channel.updatePeriod(channel.previousPeriod, config.clockHz, config.samplingRate);
            channel.samplePosition = channel.previousSamplePosition;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            int periodIncrement = (channel.effectArgumentX << 4) | channel.effectArgumentY;
            int maxPeriod = Math.min(channel.portamentoPeriod, config.maxPeriod);

            int newPeriod = Maths.clamp(channel.period + periodIncrement, config.minPeriod, maxPeriod);
            channel.updatePeriod(newPeriod, config.clockHz, config.samplingRate);
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
            WaveformType vibratoWaveformType = channel.vibratoWaveformType;
            int waveformValue = ModTables.getWaveformValue(vibratoWaveformType, channel.vibratoPosition);
            int delta = (channel.vibratoAmplitude * waveformValue) / 64;

            channel.period = Maths.clamp(channel.vibratoPeriod + delta, config.minPeriod, config.maxPeriod);
            channel.vibratoPosition += channel.vibratoSpeed;
        }
    },

    TONE_PORTAMENTO_WITH_VOLUME_SLIDE(0x05) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("TONE_PORTAMENTO_WITH_VOLUME_SLIDE not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    VIBRATO_WITH_VOLUME_SLIDE(0x06) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("VIBRATO_WITH_VOLUME_SLIDE not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    TREMOLO(0x07) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // use old tremolo speed if not specified
            if (channel.effectArgumentX != 0) {
                channel.tremoloSpeed = channel.previousEffectArgumentX;
            }

            // use old tremolo depth if not specified
            if (channel.effectArgumentY != 0) {
                channel.tremoloAmplitude = channel.previousEffectArgumentY;
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
            // TODO
            System.out.println("SET_PANNING_POSITION not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    SET_SAMPLE_OFFSET(0x09) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int offset = ((channel.effectArgumentX << 4) | channel.effectArgumentY) * 256;
            channel.samplePosition = offset;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {

        }
    },

    VOLUME_SLIDE(0x0A) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // if the current effect is A00 (volume slide with both arguments equal to zero) and the previous effect
            // was also a volume side then inherit arguments from previous slide.
            if (channel.effectArgumentX == 0 && channel.effectArgumentY == 0 && channel.previousEffectType == EffectType.VOLUME_SLIDE) {
                channel.effectArgumentX = channel.previousEffectArgumentX;
                channel.effectArgumentY = channel.previousEffectArgumentY;
            }
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            int delta;

            // TODO if both set, some trackers prioritize X argument, some compute delta
            if (channel.effectArgumentX != 0 && channel.effectArgumentY != 0) {
                // config.logger.println("VOLUME_SLIDE with both arguments not equal to zero: " + channel.effectArgumentX + ", " + channel.effectArgumentY);
            }

            if (channel.effectArgumentX != 0) {
                delta = channel.effectArgumentX;
            } else {
                delta = -channel.effectArgumentY;
            }

            channel.volume = Maths.clamp(channel.volume + delta, 0, 64);
        }
    },

    POSITION_JUMP(0x0B) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("POSITION_JUMP not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
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
            context.breakRow = Maths.clamp(row, 0, 63);
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
                context.updateTempo(speedOrTempo, config.samplingRate);
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
}
