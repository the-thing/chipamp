package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Maths;

import java.util.Arrays;

public enum ExtendedEffectType implements Effect {

    SET_FILTER(0x00) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            // filter is enabled only when argument is even (this is not clear)
            context.hardwareFilterEnabled = (channel.effectArgumentY & 1) == 0;

            if (context.hardwareFilterEnabled) {
                context.updateHardwareFilterDelta(config.samplingRate);
            } else {
                context.hardwareFilterDelta = 0.0f;
                context.hardwareFilterLeft = 0.0f;
                context.hardwareFilterRight = 0.0f;
            }
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    FINE_SLIDE_UP(0x01) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
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
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
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
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            channel.glissandoEnabled = channel.effectArgumentY != 0;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    SET_VIBRATO_WAVEFORM(0x04) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
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
        }
    },

    SET_FINE_TUNE_VALUE(0x05) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            int fineTune = channel.effectArgumentY;
            // convert (0-15) to signed range -8..7
            fineTune = (fineTune << 28) >> 28;

            channel.fineTune = fineTune;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    LOOP_PATTERN(0x06) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            int loopCounter = channel.effectArgumentY;

            if (loopCounter == 0) {
                channel.loopRowIndex = rowIndex;
            } else {
                if (channel.loopCounter == 0) {
                    channel.loopCounter = loopCounter;
                    context.loopCounter = loopCounter;
                } else {
                    channel.loopCounter--;
                    context.loopCounter = channel.loopCounter;
                }

                if (channel.loopCounter != 0) {
                    context.loopPending = true;
                    context.loopRowIndex = channel.loopRowIndex;
                }
            }
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    SET_TREMOLO_WAVEFORM(0x07) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
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
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            int panningPosition = channel.effectArgumentY;

            float rightPan = Maths.round(panningPosition / 15.0f, 2);
            float leftPan = Maths.round(1.0f - rightPan, 2);

            channel.updatePanning(leftPan, rightPan);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    RETRIGGER_SAMPLE(0x09) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            channel.retriggerTickIndex = 0;
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            channel.retriggerTickIndex++;

            int interval = channel.effectArgumentY;

            if (interval > 0 && channel.retriggerTickIndex % interval == 0) {
                channel.samplePosition = 0.0f;
            }
        }
    },

    FINE_VOLUME_SLIDE_UP(0x0A) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            channel.volume = Maths.clamp(channel.volume + channel.effectArgumentY, 0, 64);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    FINE_VOLUME_SLIDE_DOWN(0x0B) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            channel.volume = Maths.clamp(channel.volume - channel.effectArgumentY, 0, 64);
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    CUT_SAMPLE(0x0C) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            if (channel.effectArgumentY == 0) {
                // cut sample immediately
                channel.volume = 0;
            } else {
                channel.cutSampleIndex = 0;
            }
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            channel.cutSampleIndex++;

            if (channel.cutSampleIndex == channel.effectArgumentY) {
                channel.volume = 0;
            }
        }
    },

    DELAY_SAMPLE(0x0D) {
        @Override
        public void onPreEffect(Channel channel, Config config, int period, Sample sample) {
            // do not update sample, period, sample position or vibrato / tremolo state
            channel.delayedPeriod = period > 0 ? period : channel.period;
            channel.delayedSample = sample != null ? sample : channel.sample;
        }

        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            channel.delayedTickIndex = 0;

            int delay = channel.effectArgumentY;

            if (delay == 0) {
                // trigger immediately

                if (channel.delayedSample != null) {
                    // it is possible that delayed sample is null - entertainer_pizcon.mod
                    channel.updateSample(channel.delayedSample);
                }

                if (channel.delayedPeriod != 0) {
                    // it is possible that delayed period is 0 - entertainer_pizcon.mod
                    channel.updatePeriodAndIncrement(channel.delayedPeriod, config.clockHz, config.samplingRate);
                    channel.samplePosition = 0.0f;
                    channel.resetOnNewSampleWithPeriod();
                }

                // reset delayed sample
                channel.delayedSample = null;
                channel.delayedPeriod = 0;
            } else if (delay > 0 && delay < context.speed) {
                channel.delayedTriggerTickIndex = delay;
            } else {
                // otherwise the delayed sample never triggers
                channel.delayedSample = null;
                channel.delayedPeriod = 0;
            }
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            if (channel.delayedTriggerTickIndex == 0) {
                // nothing to trigger
                return;
            }

            channel.delayedTickIndex++;

            if (channel.delayedTriggerTickIndex == channel.delayedTickIndex) {
                // trigger new period with sample

                if (channel.delayedSample != null) {
                    // it is possible that delayed sample is null - entertainer_pizcon.mod
                    channel.updateSample(channel.delayedSample);
                }

                if (channel.delayedPeriod != 0) {
                    // it is possible that delayed period is 0 - entertainer_pizcon.mod
                    channel.updatePeriodAndIncrement(channel.delayedPeriod, config.clockHz, config.samplingRate);
                    channel.samplePosition = 0.0f;
                    channel.resetOnNewSampleWithPeriod();
                }

                // reset delayed sample
                channel.delayedTickIndex = 0;
                channel.delayedTriggerTickIndex = 0;
                channel.delayedSample = null;
                channel.delayedPeriod = 0;
            }
        }
    },

    DELAY_PATTERN(0x0E) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            if (channel.effectArgumentY != 0) {
                context.extraDelay = context.speed * channel.effectArgumentY;
            }
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
        }
    },

    INVERT_LOOP(0x0F) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {
            // ProTracker supposedly does not clear invert loop position or accumulator at all
        }

        @Override
        public void onMidRow(Channel channel, Context context, Config config) {
            if (channel.sample == null || !channel.sample.isLoopEnabled()) {
                return;
            }

            int funkSpeed = Mods.getFunk(channel.effectArgumentY);

            if (funkSpeed == 0) {
                return;
            }

            channel.invertLoopAccumulator += funkSpeed;

            if (channel.invertLoopAccumulator >= 128) {
                channel.invertLoopAccumulator &= 127;
                channel.invertLoopPosition++;

                int loopLength = channel.sample.getLoopLength();

                if (channel.invertLoopPosition >= loopLength) {
                    channel.invertLoopPosition = 0;
                }

                int index = channel.sample.getLoopStart() + channel.invertLoopPosition;
                channel.sample.invertData(index);
            }
        }
    },

    NONE(0xFF) {
        @Override
        public void onNewRow(Channel channel, Context context, Config config, int rowIndex) {

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
