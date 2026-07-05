package com.github.thething.chipamp.mod;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import static java.util.Objects.checkFromToIndex;
import static java.util.Objects.requireNonNull;

// TODO implement
public class Player {

    private final Sampler sampler;

    public Player(Sampler sampler) {
        this.sampler = sampler;
    }

//    public void play() throws LineUnavailableException {
//        play(sample.getLength());
//    }
//
//    public void play(int endSequenceIndex) throws LineUnavailableException {
//        checkFromToIndex(0, endSequenceIndex, mod.getLength());
//
//        byte[] buffer = new byte[4];
//        AudioFormat format = getCompatibleAudioFormat();
//
//        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
//        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
//        line.open(format);
//        line.start();
//
//        while (sequenceIndex < endSequenceIndex) {
//            int readCount = read(buffer);
//
//            if (readCount <= 0) {
//                throw new RuntimeException("Unexpected end of audio");
//            }
//
//            line.write(buffer, 0, readCount);
//        }
//
//        line.drain();
//        line.close();
//    }
//
//    public int playPatterns(int patternCount) throws LineUnavailableException {
//        AudioFormat format = getCompatibleAudioFormat();
//        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
//        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
//        line.open(format);
//        line.start();
//
//        int playedPatternCount = playPatterns(line, patternCount);
//
//        line.drain();
//        line.close();
//
//        return playedPatternCount;
//    }
//
//    public int playPatterns(SourceDataLine line, int patternCount) {
//        requireNonNull(mod);
//
//        if (patternCount < 0) {
//            return 0;
//        }
//
//        byte[] buffer = new byte[4];
//        int lastSequenceIndex = sequenceIndex;
//        int playedPatternCount = 0;
//
//        while (sequenceIndex < mod.getLength() && playedPatternCount < patternCount) {
//            int readCount = read(buffer);
//
//            if (readCount <= 0) {
//                throw new RuntimeException("Unexpected end of audio");
//            }
//
//            line.write(buffer, 0, readCount);
//
//            if (lastSequenceIndex != sequenceIndex) {
//                playedPatternCount++;
//                lastSequenceIndex = sequenceIndex;
//            }
//        }
//
//        return playedPatternCount;
//    }
//
//    public int playRows(int rowCount) throws LineUnavailableException {
//        AudioFormat format = getCompatibleAudioFormat();
//        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
//        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
//        line.open(format);
//        line.start();
//
//        int playedRowCount = playRows(line, rowCount);
//
//        line.drain();
//        line.close();
//
//        return playedRowCount;
//    }
//
//    public int playRows(SourceDataLine line, int rowCount) {
//        requireNonNull(mod);
//
//        if (rowCount < 0) {
//            return 0;
//        }
//
//        byte[] buffer = new byte[4];
//
//        int lastRow = rowIndex;
//        int lastSequenceIndex = sequenceIndex;
//        int playedRowCount = 0;
//
//        while (sequenceIndex < mod.getLength() && playedRowCount < rowCount) {
//            int readCount = read(buffer);
//
//            if (readCount <= 0) {
//                throw new RuntimeException("Unexpected end of audio");
//            }
//
//            // TODO handle return value
//            line.write(buffer, 0, readCount);
//
//            if (rowIndex != lastRow || lastSequenceIndex != sequenceIndex) {
//                playedRowCount++;
//                lastRow = rowIndex;
//                lastSequenceIndex = sequenceIndex;
//            }
//        }
//
//        return playedRowCount;
//    }
}
