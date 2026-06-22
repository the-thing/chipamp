package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.common.Maths;

import java.util.Arrays;

public enum ExtendedEffectType implements Effect {

    SET_FILTER(0x00) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO some sort of Amiga hardware filter
            if (config.logErrorEnabled) {
                config.logger.println("SET_FILTER not supported");
            }
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    FINE_SLIDE_UP(0x01) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int adjustment = channel.effectArgumentY;
            channel.period = Maths.clamp(channel.period - adjustment, config.minPeriod, config.maxPeriod);
            channel.updatePeriodAndIncrement(channel.period, config.clockHz, config.samplingRate);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    FINE_SLIDE_DOWN(0x02) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int adjustment = channel.effectArgumentY;
            channel.period = Maths.clamp(channel.period + adjustment, config.minPeriod, config.maxPeriod);
            channel.updatePeriodAndIncrement(channel.period, config.clockHz, config.samplingRate);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    SET_GLISSANDO(0x03) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("SET_GLISSANDO not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    SET_VIBRATO_WAVEFORM(0x04) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int form = channel.effectArgumentY;
            boolean retrigger = form < 4;

            WaveformType waveformType = switch (form % 4) {
                case 1 -> WaveformType.SAWTOOTH;
                case 2 -> WaveformType.SQUARE;
                default -> WaveformType.SINE;
            };

            channel.vibratoRetrigger = retrigger;
            channel.vibratoWaveformType = waveformType;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    SET_FINE_TUNE_VALUE(0x05) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("SET_FINE_TUNE_VALUE not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    LOOP_PATTERN(0x06) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("LOOP_PATTERN not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    SET_TREMOLO_WAVEFORM(0x07) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            int form = channel.effectArgumentY;
            boolean retrigger = form < 4;

            WaveformType waveformType = switch (form % 4) {
                case 1 -> WaveformType.SAWTOOTH;
                case 2 -> WaveformType.SQUARE;
                default -> WaveformType.SINE;
            };

            channel.tremoloRetrigger = retrigger;
            channel.tremoloWaveformType = waveformType;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    ROUGH_PANNING(0x08) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            channel.setPanning(channel.effectArgumentY / 16.0f);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    RETRIGGER_SAMPLE(0x09) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            channel.retriggerPosition = 0;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            channel.retriggerPosition++;

            int interval = channel.effectArgumentY;

            if (interval > 0 && channel.retriggerPosition % interval == 0) {
                channel.samplePosition = 0.0f;
            }
        }
    },

    FINE_VOLUME_SLIDE_UP(0x0A) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            channel.volume = Maths.clamp(channel.volume + channel.effectArgumentY, 0, 64);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    FINE_VOLUME_SLIDE_DOWN(0x0B) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            channel.volume = Maths.clamp(channel.volume - channel.effectArgumentY, 0, 64);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    CUT_SAMPLE(0x0C) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("CUT_SAMPLE not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    DELAY_SAMPLE(0x0D) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("DELAY_SAMPLE not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    DELAY_PATTERN(0x0E) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("DELAY_PATTERN not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
        }
    },

    INVERT_LOOP(0x0F) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config) {
            // TODO
            System.out.println("INVERT_LOOP not supported");
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            // TODO
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

    private static final ExtendedEffectType[] EFFECT_TYPE_BY_CODE;

    static {
        EFFECT_TYPE_BY_CODE = new ExtendedEffectType[256];
        Arrays.fill(EFFECT_TYPE_BY_CODE, 16, EFFECT_TYPE_BY_CODE.length, NONE);
        System.arraycopy(values(), 0, EFFECT_TYPE_BY_CODE, 0, values().length);
    }

    private final int code;

    ExtendedEffectType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ExtendedEffectType valueOf(int code) {
        return EFFECT_TYPE_BY_CODE[code];
    }
}
