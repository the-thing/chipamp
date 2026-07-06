package com.github.thething.chipamp.mod;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import static java.util.Objects.checkFromToIndex;
import static java.util.Objects.requireNonNull;

// TODO play range
public final class Player {

    private final Sampler sampler;

    public Player(Sampler sampler) {
        this.sampler = requireNonNull(sampler);
    }

    public void play() throws LineUnavailableException {
        Mod mod = sampler.getMod();
        requireNonNull(mod);

        play(mod.getLength());
    }

    public void play(int endSequenceIndex) throws LineUnavailableException {
        Mod mod = sampler.getMod();
        checkFromToIndex(0, endSequenceIndex, mod.getLength());

        byte[] buffer = new byte[4];
        AudioFormat format = sampler.getCompatibleAudioFormat();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        while (sampler.getSequenceIndex() < endSequenceIndex) {
            int readCount = sampler.read(buffer);

            if (readCount <= 0) {
                throw new RuntimeException("Unexpected end of audio");
            }

            line.write(buffer, 0, readCount);
        }

        line.drain();
        line.close();
    }

    public int playPatterns(int patternCount) throws LineUnavailableException {
        Mod mod = sampler.getMod();
        requireNonNull(mod);

        AudioFormat format = sampler.getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        int playedPatternCount = playPatterns(line, patternCount);

        line.drain();
        line.close();

        return playedPatternCount;
    }

    public int playPatterns(SourceDataLine line, int patternCount) {
        Mod mod = sampler.getMod();
        requireNonNull(mod);

        byte[] buffer = new byte[4];
        int lastSequenceIndex = sampler.getSequenceIndex();
        int playedPatternCount = 0;

        while (sampler.getSequenceIndex() < mod.getLength() && playedPatternCount < patternCount) {
            int readCount = sampler.read(buffer);

            if (readCount <= 0) {
                throw new RuntimeException("Unexpected end of audio");
            }

            line.write(buffer, 0, readCount);

            if (lastSequenceIndex != sampler.getSequenceIndex()) {
                playedPatternCount++;
                lastSequenceIndex = sampler.getSequenceIndex();
            }
        }

        return playedPatternCount;
    }

    public int playRows(int rowCount) throws LineUnavailableException {
        Mod mod = sampler.getMod();
        requireNonNull(mod);

        AudioFormat format = sampler.getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        int playedRowCount = playRows(line, rowCount);

        line.drain();
        line.close();

        return playedRowCount;
    }

    public int playRows(SourceDataLine line, int rowCount) {
        Mod mod = sampler.getMod();
        requireNonNull(mod);

        if (rowCount < 0) {
            return 0;
        }

        byte[] buffer = new byte[4];

        int sequenceIndex = sampler.getSequenceIndex();
        int rowIndex = sampler.getRowIndex();
        int lastSequenceIndex = sequenceIndex;
        int lastRow = rowIndex;
        int playedRowCount = 0;

        while (sequenceIndex < mod.getLength() && playedRowCount < rowCount) {
            int readCount = sampler.read(buffer);

            if (readCount <= 0) {
                throw new RuntimeException("Unexpected end of audio");
            }

            int writtenCount = line.write(buffer, 0, readCount);

            if (writtenCount != readCount) {
                throw new RuntimeException("Unable to write all samples");
            }

            sequenceIndex = sampler.getSequenceIndex();
            rowIndex = sampler.getRowIndex();

            if (rowIndex != lastRow || lastSequenceIndex != sequenceIndex) {
                playedRowCount++;
                lastRow = rowIndex;
                lastSequenceIndex = sequenceIndex;
            }
        }

        return playedRowCount;
    }
}
