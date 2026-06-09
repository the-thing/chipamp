package com.github.thething.chipgroove.mod;

import com.github.thething.chipgroove.common.Maths;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.util.Arrays;

class Channel {

    int pitch;
    int sampleNumber;
    int volume;
    long samplePosition;
    long sampleIncrement;
    int effect;
    int effectArgument;
    boolean muted;

    void reset() {
        pitch = 0;
        sampleNumber = 0;
        volume = 0;
        samplePosition = 0L;
        sampleIncrement = 0L;
        effect = 0;
        effectArgument = 0;
        muted = false;
    }
}

public class Player {

    private static final float SAMPLE_RATE = 44_100.0f;

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
        int rowCount = mod.getRowCount();

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

            // stream.write(buffer, 0, buffer.length);

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

    private static float convertPitchToFrequency(int pitch) {
        return 7159090.5f / (pitch * 2);
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

            if (instrument.pitch() > 0) {
                if (instrument.effect() != Effects.TONE_PORTAMENTO) {
                    channels[channelIndex].pitch = instrument.pitch();
                    channels[channelIndex].samplePosition = 0;

                    float frequency = convertPitchToFrequency(instrument.pitch());
                    channels[channelIndex].sampleIncrement = (int) ((frequency * 65536.0f) / SAMPLE_RATE);
                }
            }

            channels[channelIndex].effect = instrument.effect();
            channels[channelIndex].effectArgument = instrument.effectArgument();
        }
    }

    private void tick(Mod mod, byte[] buffer) {
        for (int i = 0; i < buffer.length; i += 2) {
            int mixed = 0;
            int rightMix = 0;

            for (int channelIndex = 0; channelIndex < channels.length; channelIndex++) {
                Channel channel = channels[channelIndex];

                if (!channel.muted && channel.pitch > 0 && channel.sampleNumber > 0) {
                    int sampleIndex = channel.sampleNumber - 1;

                    if (sampleIndex >= 0 && sampleIndex < 31) {
                        Sample sample = mod.getSample(sampleIndex);

                        // TODO validate that data length matches sample length
                        if (sample.getData().length != 0 && sample.getLength() > 0) {
                            int samplePosition = Math.toIntExact(channel.samplePosition >> 16);

                            if (samplePosition < sample.getLength()) {
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

                            if (samplePosition >= sample.getLength()) {
                                channel.samplePosition = 0;
                                channel.pitch = 0;
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
        Mod mod = ModLoader.load("DJ Metune - Axel F.mod");
//        Mod mod = ModLoader.load("Hoffman - Eon.mod");

        System.out.println("patternCount = " + mod.getPatternCount());

        Player player = new Player();
        player.play(mod);

//        System.out.println(Integer.MAX_VALUE & 0xFFFF_FFFF);

        /**
         *   Mod mod = ModLoader.load("DJ Metune - Axel F.mod");
         *
         *          Sample sample = mod.getSample(0xa - 1);
         *         byte[] audio = sample.getData();
         *
         *         int tickSamples = calculateTickSamples(125);
         *         System.out.println("Tick samples: " + tickSamples);
         *
         *
         *         // float sampleRate = 44_100f;
         *          // float sampleRate = 8_363f;
         *         float sampleRate =  16_574f; // PAL
         *         AudioFormat audioFormat = new AudioFormat(sampleRate, 8, 1, true, true);
         *         DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
         *         SourceDataLine line = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
         *
         *         line.open();
         *         line.start();
         *         line.write(audio, 0, audio.length);
         *         line.drain();
         *         line.close();
         */
    }
}
