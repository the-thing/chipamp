package com.github.thething.chipamp.mod;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import static java.util.Objects.checkFromToIndex;
import static java.util.Objects.requireNonNull;

public final class Player {

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private final Sampler sampler;

    public Player(Sampler sampler) {
        this.sampler = requireNonNull(sampler);
    }

    public void play() throws LineUnavailableException {
        Mod mod = sampler.getMod();
        play(mod.getLength());
    }

    public void play(int endSequenceIndex) throws LineUnavailableException {
        play(sampler.getSequenceIndex(), endSequenceIndex);
    }

    public void play(int startSequenceIndex, int endSequenceIndex) throws LineUnavailableException {
        AudioFormat format = sampler.getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            play(line, startSequenceIndex, endSequenceIndex);

            line.drain();
        }
    }

    public void play(SourceDataLine line, int startSequenceIndex, int endSequenceIndex) {
        Mod mod = sampler.getMod();
        checkFromToIndex(startSequenceIndex, endSequenceIndex, mod.getLength());

        sampler.seekSequence(startSequenceIndex);

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int sequenceIndex = startSequenceIndex;

        while (sequenceIndex < endSequenceIndex) {
            int readCount = sampler.read(buffer);

            if (readCount <= 0) {
                throw new RuntimeException("Unexpected end of audio");
            }

            line.write(buffer, 0, readCount);

            sequenceIndex = sampler.getSequenceIndex();
        }
    }

    public int playPatterns(int patternCount) throws LineUnavailableException {
        AudioFormat format = sampler.getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        int playedPatternCount;

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            playedPatternCount = playPatterns(line, patternCount);

            line.drain();
        }

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

            int writtenCount = line.write(buffer, 0, readCount);

            if (writtenCount != readCount) {
                throw new RuntimeException("Unable to write all samples");
            }

            if (lastSequenceIndex != sampler.getSequenceIndex()) {
                playedPatternCount++;
                lastSequenceIndex = sampler.getSequenceIndex();
            }
        }

        return playedPatternCount;
    }

    public int playRows(int rowCount) throws LineUnavailableException {
        AudioFormat format = sampler.getCompatibleAudioFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        int playedRowCount;

        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();

            playedRowCount = playRows(line, rowCount);

            line.drain();
        }

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
