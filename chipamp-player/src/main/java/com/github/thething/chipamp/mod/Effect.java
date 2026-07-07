package com.github.thething.chipamp.mod;

/**
 * Defines audio effects that can be applied to channels during MOD playback. Effects are processed at specific points
 * during pattern playback to modify channel properties such as volume, period, and sample playback.
 */
interface Effect {

    /**
     * Pre-processes a channel before the main effect is applied. This default implementation handles sample assignment,
     * volume setting, fine-tune period adjustment, and sample position reset.
     *
     * @param channel the channel to apply the effect to
     * @param config  the configuration containing clock and sampling rate settings
     * @param period  the note period value; if greater than 0, triggers period update
     * @param sample  the sample to assign to the channel; may be null
     */
    default void onPreEffect(Channel channel, Config config, int period, Sample sample) {
        if (sample != null) {
            channel.updateSample(sample);
        }

        if (period > 0) {
            Sample activeSample = sample;
            int activeFineTune = sample != null ? sample.getFineTune() : 0;

            if (activeSample == null) {
                activeSample = channel.sample;
                activeFineTune = channel.fineTune;
            }

            if (activeSample != null && activeFineTune != 0) {
                period = Mods.getFineTunePeriod(period, activeFineTune);
            }

            if (activeSample != null) {
                channel.updatePeriodAndIncrement(period, config.clockHz, config.samplingRate);
                channel.samplePosition = 0.0f;
                channel.resetOnNewSampleWithPeriod();
            }
        }
    }

    /**
     * Applies the effect when a new pattern row is encountered. This method is called once per row and can modify the
     * channel state based on the effect parameters for the current row.
     *
     * @param channel  the channel to apply the effect to
     * @param context  the playback context containing pattern and module data
     * @param config   the configuration containing playback settings
     * @param rowIndex the index of the current row in the pattern
     */
    void onNewRow(Channel channel, Context context, Config config, int rowIndex);

    /**
     * Applies the effect during the middle of a row (between ticks). This method is called multiple times per row for
     * effects that require continuous or per-tick updates, such as vibrato or slides.
     *
     * @param channel the channel to apply the effect to
     * @param context the playback context containing pattern and module data
     * @param config  the configuration containing playback settings
     */
    void onMidRow(Channel channel, Context context, Config config);
}
