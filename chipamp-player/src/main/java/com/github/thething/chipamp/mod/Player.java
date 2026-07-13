package com.github.thething.chipamp.mod;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.function.Function;

import static java.util.Objects.checkFromToIndex;
import static java.util.Objects.requireNonNull;

/**
 * Plays audio from MOD format tracker files using a {@link Sampler}.
 * <p>
 * This class provides methods to play audio through Java's sound system, supporting playback by sequence index,
 * patterns, or rows. It manages the audio line and buffer for audio output.
 * </p>
 */
public final class Player {

    private static final Function<AudioFormat, SourceDataLine> DEFAULT_SOURCE_DATA_LINE_FACTORY = (format) -> {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try {
            return (SourceDataLine) AudioSystem.getLine(info);
        } catch (LineUnavailableException e) {
            throw new RuntimeException("  to obtain source data line: " + e.getMessage(), e);
        }
    };

    private final Sampler sampler;
    private final Function<AudioFormat, SourceDataLine> sourceDataLineFactory;

    public Player(Sampler sampler) {
        this(sampler, DEFAULT_SOURCE_DATA_LINE_FACTORY);
    }

    public Player(Sampler sampler, Function<AudioFormat, SourceDataLine> sourceDataLineFactory) {
        this.sampler = requireNonNull(sampler);
        this.sourceDataLineFactory = requireNonNull(sourceDataLineFactory);
    }

    /**
     * Plays the entire MOD file from the current position to the end.
     *
     * @throws LineUnavailableException if the audio line cannot be opened
     */
    public void play() throws LineUnavailableException {
        Mod mod = sampler.getMod();
        play(mod.getLength());
    }

    /**
     * Plays the MOD file from the current sequence position to the specified end sequence index.
     * <p>
     * This method opens an audio line, plays the audio, and drains the line before closing it.
     * </p>
     *
     * @param endSequenceIndex the sequence index to stop at (exclusive)
     * @throws LineUnavailableException if the audio line cannot be opened
     */
    public void play(int endSequenceIndex) throws LineUnavailableException {
        play(sampler.getSequenceIndex(), endSequenceIndex);
    }

    /**
     * Plays the MOD file from the specified start sequence index to the end sequence index.
     *
     * @param startSequenceIndex the sequence index to start at (inclusive)
     * @param endSequenceIndex   the sequence index to stop at (exclusive)
     * @throws LineUnavailableException  if the audio line cannot be opened
     * @throws IndexOutOfBoundsException if the sequence indices are out of bounds
     */
    public void play(int startSequenceIndex, int endSequenceIndex) throws LineUnavailableException {
        AudioFormat format = sampler.getCompatibleAudioFormat();

        try (SourceDataLine line = sourceDataLineFactory.apply(format)) {
            line.open(format);
            line.start();

            play(line, startSequenceIndex, endSequenceIndex);

            line.drain();
        }
    }

    /**
     * Plays the MOD file from the specified start sequence index to the end sequence index using the provided audio
     * line.
     *
     * @param line               the audio line to write audio data to
     * @param startSequenceIndex the sequence index to start at (inclusive)
     * @param endSequenceIndex   the sequence index to stop at (exclusive)
     * @throws IndexOutOfBoundsException if the sequence indices are out of bounds
     * @throws RuntimeException          if the sampler unexpectedly stops producing audio
     */
    public void play(SourceDataLine line, int startSequenceIndex, int endSequenceIndex) {
        Mod mod = sampler.getMod();
        checkFromToIndex(startSequenceIndex, endSequenceIndex, mod.getLength());

        if (startSequenceIndex != sampler.getSequenceIndex()) {
            // only change sequence when the current one is different
            // this means we could start playing from any row, tick or sample from the current pattern
            sampler.seekSequence(startSequenceIndex);
        }

        // intentionally matches sample size not to read more than end sequence
        byte[] buffer = new byte[sampler.getBytesPerSample()];
        int sequenceIndex = startSequenceIndex;

        while (sequenceIndex < endSequenceIndex) {
            int readCount = sampler.read(buffer);

            if (readCount <= 0) {
                throw new RuntimeException("Unexpected end of audio");
            }

            int writtenCount = line.write(buffer, 0, readCount);

            if (writtenCount != readCount) {
                throw new RuntimeException("Unable to write all samples");
            }

            sequenceIndex = sampler.getSequenceIndex();
        }
    }

    /**
     * Plays the specified number of patterns from the current position.
     *
     * @param patternCount the number of patterns to play
     * @return the actual number of patterns played
     * @throws LineUnavailableException if the audio line cannot be opened
     */
    public int playPatterns(int patternCount) throws LineUnavailableException {
        AudioFormat format = sampler.getCompatibleAudioFormat();
        int playedPatternCount;

        try (SourceDataLine line = sourceDataLineFactory.apply(format)) {
            line.open(format);
            line.start();

            playedPatternCount = playPatterns(line, patternCount);

            line.drain();
        }

        return playedPatternCount;
    }

    /**
     * Plays the specified number of patterns from the current position using the provided audio line.
     * <p>
     * The audio line should be opened and started before calling this method. This method does not drain or close the
     * line. Playback stops when the requested number of patterns have been played or the end of the MOD is reached.
     * </p>
     *
     * @param line         the audio line to write audio data to
     * @param patternCount the number of patterns to play
     * @return the actual number of patterns played
     * @throws RuntimeException if the sampler unexpectedly stops producing audio or if not all samples can be written
     *                          to the line
     */
    public int playPatterns(SourceDataLine line, int patternCount) {
        Mod mod = sampler.getMod();
        requireNonNull(mod);

        // intentionally matches sample size not to read more than end sequence
        byte[] buffer = new byte[sampler.getBytesPerSample()];
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

    /**
     * Plays the specified number of rows from the current position.
     * <p>
     * This method opens a new audio line, plays the rows, and drains the line before closing it.
     * </p>
     *
     * @param rowCount the number of rows to play
     * @return the actual number of rows played
     * @throws LineUnavailableException if the audio line cannot be opened
     */
    public int playRows(int rowCount) throws LineUnavailableException {
        AudioFormat format = sampler.getCompatibleAudioFormat();
        int playedRowCount;

        try (SourceDataLine line = sourceDataLineFactory.apply(format)) {
            line.open(format);
            line.start();

            playedRowCount = playRows(line, rowCount);

            line.drain();
        }

        return playedRowCount;
    }

    /**
     * Plays the specified number of rows from the current position using the provided audio line.
     *
     * @param line     the audio line to write audio data to
     * @param rowCount the number of rows to play
     * @return the actual number of rows played
     * @throws RuntimeException if the sampler unexpectedly stops producing audio, or if not, all samples can be written
     *                          to the line
     */
    public int playRows(SourceDataLine line, int rowCount) {
        Mod mod = sampler.getMod();
        requireNonNull(mod);

        // intentionally matches sample size not to read more than end sequence
        byte[] buffer = new byte[sampler.getBytesPerSample()];

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
