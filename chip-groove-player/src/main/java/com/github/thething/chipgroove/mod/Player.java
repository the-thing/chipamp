package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.common.Maths;
import com.github.thething.chipgroove.io.Resources;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Player {

    // private static final float SAMPLE_RATE = 8_287.0f;
    // private static final float SAMPLE_RATE = 44_100.0f;
    private static final float SAMPLE_RATE = 28_604.0f;

    private final Channel[] channels;

    public Player() {
        channels = new Channel[4];
        channels[0] = new Channel();
        channels[1] = new Channel();
        channels[2] = new Channel();
        channels[3] = new Channel();
    }

    public void reset() {
        for (int i = 0; i < channels.length; i++) {
            channels[i].reset();
        }
    }

    public void play(Mod mod) throws LineUnavailableException {
        byte[] buffer = new byte[2048];

        AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

        line.open();
        line.start();

        int currentTick = 0;
        int patternIndex = 0;
        int rowIndex = 0;
        int ticksPerRow = 5;
        int rowCount = Mod.ROW_COUNT;

        System.out.println("Song length: " + mod.getLength());

        while (patternIndex < mod.getLength()) {
            if (currentTick == 0) {
                System.out.println("newRow: " + rowIndex);

                newRow(mod, patternIndex, rowIndex);

                // TODO print row
                // TODO some effects have to be implemented here
            }

            Arrays.fill(buffer, (byte) 0);
            tick(mod, buffer);
            currentTick++;

            // TODO write to buffer
            line.write(buffer, 0, buffer.length);

            if (currentTick == ticksPerRow) {
                currentTick = 0;
                rowIndex++;

                if (rowIndex == rowCount) {
                    rowIndex = 0;
                    patternIndex++;
                }
            }
        }

        line.drain();
        line.close();
    }

    private static float convertPeriodToFrequency(int period) {
        return 7159090.5f / (period * 2);
    }

    private static int calculateTickSamples(int tempo) {
        // Calculate samples per tick based on tempo
        // 2500 / tempo = milliseconds per tick
        // (ms * sample_rate) / 1000 = samples per tick
        return (2500 * (int) SAMPLE_RATE) / (tempo * 1000);
    }

    private void newRow(Mod mod, int position, int rowIndex) {
        int patternIndex = mod.getPatternSequence(position);

        for (int channelIndex = 0; channelIndex < channels.length; channelIndex++) {
            Instrument instrument = mod.getInstrument(patternIndex, rowIndex, channelIndex);

            // replace sample number and volume if not empty
            if (instrument.sampleNumber() > 0) {
                channels[channelIndex].sampleNumber = instrument.sampleNumber();
                channels[channelIndex].volume = mod.getSample(instrument.sampleNumber() - 1).getVolume();
            }

            if (instrument.period() > 0) {
                if (instrument.effect() != Effect.TONE_PORTAMENTO) {
                    channels[channelIndex].period = instrument.period();
                    channels[channelIndex].samplePosition = 0;

                    float frequency = convertPeriodToFrequency(instrument.period());
                    channels[channelIndex].sampleIncrement = (int) ((frequency * 65536.0f) / SAMPLE_RATE);
                }
            }

            channels[channelIndex].effect = instrument.effect();
            channels[channelIndex].extendedEffect = instrument.extendedEffect();
            channels[channelIndex].effectArgumentX = instrument.effectArgumentX();
            channels[channelIndex].effectArgumentY = instrument.effectArgumentY();
        }
    }

    private void tick(Mod mod, byte[] buffer) {
        for (int i = 0; i < buffer.length; i += 2) {
            int mixed = 0;
            int rightMix = 0;

            for (int channelIndex = 0; channelIndex < channels.length; channelIndex++) {
                Channel channel = channels[channelIndex];

                if (!channel.muted && channel.period > 0 && channel.sampleNumber > 0) {
                    int sampleIndex = channel.sampleNumber - 1;

                    if (sampleIndex >= 0 && sampleIndex < 31) {
                        Sample sample = mod.getSample(sampleIndex);

                        // TODO validate that data length matches sample length
                        if (sample.getDataLength() != 0 && sample.getDataLength() > 0) {
                            int samplePosition = Math.toIntExact(channel.samplePosition >> 16);

                            if (samplePosition < sample.getDataLength()) {
                                int sampleValue = sample.getData(samplePosition);
                                int scaled = (sampleValue * channel.volume) / 64;
                                mixed += scaled;
                                channel.samplePosition += channel.sampleIncrement;
                            }

                            samplePosition = Math.toIntExact(channel.samplePosition >> 16);

                            if (sample.getLoopLength() > 2) {
                                // handle loop
                                int loopEnd = sample.getLoopStart() + sample.getLoopLength();

                                if (samplePosition >= loopEnd) {
                                    channel.samplePosition = sample.getLoopStart() << 16;
                                }
                            }

                            if (samplePosition >= sample.getDataLength()) {
                                channel.samplePosition = 0;
                                channel.period = 0;
                            }
                        }
                    }
                }
            }

            mixed = Maths.clamp(mixed, Short.MIN_VALUE, Short.MAX_VALUE);

            buffer[i] = (byte) (mixed >> 8);
            buffer[i + 1] = (byte) (mixed & 0xFF);
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException, LineUnavailableException {
        ModLoader modLoader = new ModLoader();
        Mod mod = modLoader.load("DJ Metune - Axel F.mod");

        Sample sample = mod.getSample(1);

        byte[] sampleData8bit = sample.getData();
        byte[] sampleData16bit = new byte[sampleData8bit.length * 2];

        for (int i = 0; i < sampleData8bit.length; i++) {
            short sampleValue = (short) (sampleData8bit[i] << 8);
            sampleData16bit[i * 2] = (byte) (sampleValue & 0xFF);
            sampleData16bit[i * 2 + 1] = (byte) (sampleValue >> 8);
        }

        AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(dataLineInfo);

        line.open();
        line.start();
        line.write(sampleData8bit, 0, sampleData8bit.length);
        line.drain();
        line.close();

        System.out.println("dupa: " + sampleData8bit.length * 214);

        Resources.saveAudio(new File("dupa.wav"), audioFormat, sampleData8bit);
        byte[] resample = resample(sampleData8bit, sampleData8bit.length * 2);
        Resources.saveAudio(new File("dupa2.wav"), audioFormat, resample);
    }

    public static byte[] resample(byte[] audio, int newLength) {
        byte[] newAudio = new byte[newLength];

        for (int i = 0; i < newLength; i++) {
            float ratio = (float) i / (float) newLength;
            newAudio[i] = audio[(int) (audio.length * ratio)];
        }

        return newAudio;
    }
}
