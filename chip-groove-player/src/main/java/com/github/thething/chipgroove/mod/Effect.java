package com.github.thething.chipgroove.mod;

public interface Effect {

    default void onPreEffect(Channel channel, Config config, int period, Sample sample) {
        if (sample != null) {
            channel.sample = sample;
            channel.volume = sample.volume();
        }

        Sample activeSample = sample;

        if (activeSample == null) {
            activeSample = channel.sample;
        }

        // TODO this is currently used only by SET SAMPLE OFFSET, maybe this should only be effect-specific field?
        channel.periodTriggered = period > 0;

        if (period > 0) {
            if (activeSample != null && activeSample.fineTune() != 0) {
                period = ModTables.getFineTunePeriod(period, activeSample.fineTune());
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
