package com.github.thething.chipamp.tool;

import com.github.thething.chipamp.io.Resources;
import com.github.thething.chipamp.mod.Mod;
import com.github.thething.chipamp.mod.ModLoader;
import com.github.thething.chipamp.mod.Mods;
import com.github.thething.chipamp.mod.Sampler;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

class BatchGeneratorTest {

    private Sampler sampler;
    private ModLoader modLoader;

    @BeforeEach
    void setUp() {
        sampler = new Sampler();
        modLoader = new ModLoader();
    }

    @ParameterizedTest
    @MethodSource("modSource")
    @Disabled
    void shouldGenerate(
            String resourceName, int clockHz, int samplingRate,
            int minPeriod, int maxPeriod, float volumeMultiplier,
            boolean stereoEnabled, boolean stereoFoldDownEnabled,
            boolean volumeSlideDeltaEnabled, boolean loopDetectionEnabled) throws IOException {

        sampler.setClockHz(clockHz);
        sampler.setSamplingRate(samplingRate);
        sampler.setMinPeriod(minPeriod);
        sampler.setMaxPeriod(maxPeriod);
        sampler.setVolumeMultiplier(volumeMultiplier);
        sampler.setStereoEnabled(stereoEnabled);
        sampler.setStereoFoldDownEnabled(stereoFoldDownEnabled);
        sampler.setVolumeSlideDeltaEnabled(volumeSlideDeltaEnabled);
        sampler.setLoopDetectionEnabled(loopDetectionEnabled);
        sampler.setLoggingEnabled(false);

        String fileName = Resources.getFileName(resourceName);
        fileName = fileName.substring(0, fileName.length() - 4);
        fileName = fileName.replaceAll("\\W+", "-");
        fileName = fileName.toLowerCase();

        String newFileName = fileName +
                '-' +
                "clock" + clockHz + "Hz" +
                '-' +
                "sampling" + samplingRate + "Hz" +
                '-' +
                "volumeMultiplier" + volumeMultiplier +
                '-' +
                (stereoEnabled ? "stereo" : "mono") +
                '-' +
                (stereoFoldDownEnabled ? "foldDown" : "noFoldDown") +
                '-' +
                (volumeSlideDeltaEnabled ? "volumeSlideDelta" : "noVolumeSlideDelta") +
                '-' +
                (loopDetectionEnabled ? "loopDetection" : "noLoopDetection") +
                ".wav";

        Mod mod = modLoader.load(resourceName);
        sampler.updateMod(mod);

        byte[] audio = sampler.read();
        Resources.saveAudio(new File(newFileName), sampler.getCompatibleAudioFormat(), audio);
    }

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(BatchGeneratorTest.class))
                .build();

        Launcher launcher = LauncherFactory.create();
        launcher.execute(request);
    }

    private static Stream<Arguments> modSource() {
        return Stream.of(
                new PlayerConfig("chip/DJ Metune - Axel F.mod", Mods.PAL_CLOCK_HZ, 48_000, Mods.MIN_PERIOD, Mods.MAX_PERIOD, 0.5f, true, true, false, true),
                new PlayerConfig("chip/Allister Brimble - Superfrog Intro.mod", Mods.PAL_CLOCK_HZ, 48_000, Mods.MIN_PERIOD, Mods.MAX_PERIOD, 0.5f, true, true, false, true)
        );
    }

    private record PlayerConfig(String resourceName, int clockHz, int samplingRate, int minPeriod, int maxPeriod,
                                float volumeMultiplier, boolean stereoEnabled, boolean stereoFoldDownEnabled,
                                boolean volumeSlideDeltaEnabled, boolean loopDetectionEnabled) implements Arguments {

        @Override
        @NullMarked
        public Object[] get() {
            return new Object[]{resourceName, clockHz, samplingRate, minPeriod, maxPeriod, volumeMultiplier,
                    stereoEnabled, stereoFoldDownEnabled, volumeSlideDeltaEnabled, loopDetectionEnabled};
        }
    }
}
