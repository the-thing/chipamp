package com.github.thething.chipamp.mod;

interface Effect {

    default void onPreEffect(Channel channel, Config config, int period, Sample sample) {
        if (sample != null) {
            channel.sample = sample;
            channel.volume = sample.getVolume();
        }

        Sample activeSample = sample;

        if (activeSample == null) {
            activeSample = channel.sample;
        }

        channel.periodTriggered = period > 0;

        if (period > 0) {
            if (activeSample != null && activeSample.getFineTune() != 0) {
                period = Mods.getFineTunePeriod(period, activeSample.getFineTune());
            }

            if (activeSample != null) {
                channel.updatePeriodAndIncrement(period, config.clockHz, config.samplingRate);
                channel.samplePosition = 0.0f;
                channel.resetOnNewSampleWithPeriod();
            }
        }
    }

    void onNewRow(Channel channel, Context context, Config config, int rowIndex);

    void onMidRow(Channel channel, Context context, Config config);
}
