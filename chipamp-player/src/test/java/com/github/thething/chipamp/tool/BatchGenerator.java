package com.github.thething.chipamp.tool;

import com.github.thething.chipamp.io.Resources;
import com.github.thething.chipamp.mod.Mod;
import com.github.thething.chipamp.mod.ModLoader;
import com.github.thething.chipamp.mod.Mods;
import com.github.thething.chipamp.mod.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
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

@SuppressWarnings("NewClassNamingConvention")
public class BatchGenerator {

    private Player player;
    private ModLoader modLoader;

    @BeforeEach
    void setUp() {
        player = new Player();
        modLoader = new ModLoader();
    }

    @ParameterizedTest
    @MethodSource("modSource")
    void shouldGenerate(
            String resourceName, int clockHz, int samplingRate,
            int minPeriod, int maxPeriod, float volumeMultiplier,
            boolean stereoEnabled, boolean stereoFoldDownEnabled,
            boolean volumeSlideDeltaEnabled, boolean loopDetectionEnabled) throws IOException {

        player.setClockHz(clockHz);
        player.setSamplingRate(samplingRate);
        player.setMinPeriod(minPeriod);
        player.setMaxPeriod(maxPeriod);
        player.setVolumeMultiplier(volumeMultiplier);
        player.setStereoEnabled(stereoEnabled);
        player.setStereoFoldDownEnabled(stereoFoldDownEnabled);
        player.setVolumeSlideDeltaEnabled(volumeSlideDeltaEnabled);
        player.setLoopDetectionEnabled(loopDetectionEnabled);
        player.setLoggingEnabled(false);

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
                "volume" + volumeMultiplier +
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
        player.setMod(mod);

        byte[] audio = player.read();
        Resources.saveAudio(new File(newFileName), player.getCompatibleAudioFormat(), audio);
    }

    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(BatchGenerator.class))
                .build();

        Launcher launcher = LauncherFactory.create();
        launcher.execute(request);
    }

    private static Stream<Arguments> modSource() {
        return Stream.of(
                new PlayerConfig("chip/DJ Metune - Axel F.mod", Mods.PAL_CLOCK_HZ, 48_000, Mods.MIN_PERIOD, Mods.MAX_PERIOD, 0.5f, true, true, false, true)
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
