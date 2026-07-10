package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.concurrent.IdleStrategy;
import com.github.thething.chipamp.concurrent.SleepingIdleStrategy;
import com.github.thething.chipamp.concurrent.SpScByteCircularBuffer;

import javax.sound.sampled.SourceDataLine;
import java.io.Closeable;
import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

/**
 * An asynchronous wrapper for {@link SourceDataLine} that writes audio data to a line on a separate thread.
 */
public final class AsyncSourceDataLine implements Closeable {

    private static final IdleStrategy DEFAULT_IDLE_STRATEGY = new SleepingIdleStrategy(100L);
    private static final ThreadFactory DEFAULT_THREAD_FACTORY = (runnable) -> new Thread(runnable, "AsyncSourceDataLine");
    private static final int DEFAULT_BUFFER_CAPACITY = 1024 * 64;

    private final SpScByteCircularBuffer buffer;
    private final Thread thread;

    private AsyncSourceDataLine(SpScByteCircularBuffer buffer, Thread thread) {
        this.buffer = requireNonNull(buffer);
        this.thread = requireNonNull(thread);
    }

    /**
     * Launches an asynchronous wrapper for the specified {@link SourceDataLine} with default settings.
     *
     * @param line       the SourceDataLine to wrap (must be already open)
     * @param readLength the number of bytes to read from the buffer per iteration
     * @return a new AsyncSourceDataLine instance
     * @throws IllegalStateException if the line is not open
     */
    public static AsyncSourceDataLine launch(SourceDataLine line, int readLength) {
        return launch(line, DEFAULT_IDLE_STRATEGY, DEFAULT_BUFFER_CAPACITY, readLength, DEFAULT_THREAD_FACTORY);
    }

    /**
     * Launches an asynchronous wrapper for the specified {@link SourceDataLine} with custom settings.
     *
     * @param line           the SourceDataLine to wrap (must be already open)
     * @param idleStrategy   the strategy to use when the buffer is empty
     * @param bufferCapacity the capacity of the internal circular buffer in bytes
     * @param readLength     the number of bytes to read from the buffer per iteration
     * @param threadFactory  the factory to create the background thread
     * @return a new AsyncSourceDataLine instance
     * @throws IllegalStateException    if the line is not open
     * @throws IllegalArgumentException if bufferCapacity is negative
     * @throws NullPointerException     if idleStrategy or threadFactory is null
     */
    public static AsyncSourceDataLine launch(SourceDataLine line, IdleStrategy idleStrategy, int bufferCapacity, int readLength, ThreadFactory threadFactory) {
        if (!line.isOpen()) {
            throw new IllegalStateException("Source data line is not open");
        }

        requireNonNull(idleStrategy);

        if (bufferCapacity < 0) {
            throw new IllegalArgumentException("Buffer capacity must be greater than or equal to zero");
        }

        SpScByteCircularBuffer buffer = new SpScByteCircularBuffer(bufferCapacity);
        Task task = Task.newInstance(line, buffer, idleStrategy, readLength);

        Thread thread = threadFactory.newThread(task);
        thread.start();

        return new AsyncSourceDataLine(buffer, thread);
    }

    /**
     * Writes audio data to the internal buffer.
     * <p>
     * This method is non-blocking and will write as much data as possible to the buffer. If the buffer is full, it will
     * return the number of bytes actually written, which may be less than the array length.
     *
     * @param data the audio data to write
     * @return the number of bytes actually written to the buffer
     */
    public int write(byte[] data) {
        return write(data, 0, data.length);
    }

    /**
     * Writes audio data to the internal buffer from the specified offset.
     * <p>
     * This method is non-blocking and will write as much data as possible to the buffer. If the buffer is full, it will
     * return the number of bytes actually written, which may be less than the requested length.
     *
     * @param data   the audio data to write
     * @param offset the starting offset in the data array
     * @param length the number of bytes to write
     * @return the number of bytes actually written to the buffer
     */
    public int write(byte[] data, int offset, int length) {
        return buffer.write(data, offset, length);
    }

    /**
     * Returns the number of bytes currently in the internal buffer.
     *
     * @return the number of bytes available in the buffer
     */
    public int size() {
        return buffer.size();
    }

    /**
     * Closes this AsyncSourceDataLine by interrupting the background thread.
     * <p>
     * This will cause the background thread to stop writing to the SourceDataLine and terminate.
     */
    @Override
    public void close() {
        thread.interrupt();
    }

    /**
     * A task that runs on a background thread to transfer data from the circular buffer to the SourceDataLine.
     *
     * @param sourceDataLine the target SourceDataLine to write to
     * @param buffer         the circular buffer to read from
     * @param idleStrategy   the strategy to use when the buffer is empty
     * @param readBuffer     the reusable buffer for reading data
     */
    private record Task(SourceDataLine sourceDataLine, SpScByteCircularBuffer buffer, IdleStrategy idleStrategy,
                        byte[] readBuffer) implements Runnable {

        /**
         * Creates a new Task instance with a read buffer of the specified length.
         *
         * @param sourceDataLine the target SourceDataLine to write to
         * @param buffer         the circular buffer to read from
         * @param idleStrategy   the strategy to use when the buffer is empty
         * @param readLength     the length of the read buffer to allocate
         * @return a new Task instance
         */
        public static Task newInstance(SourceDataLine sourceDataLine, SpScByteCircularBuffer buffer, IdleStrategy idleStrategy, int readLength) {
            return new Task(sourceDataLine, buffer, idleStrategy, new byte[readLength]);
        }

        /**
         * Continuously reads data from the circular buffer and writes it to the SourceDataLine until the thread is
         * interrupted.
         *
         * @throws RuntimeException if the SourceDataLine cannot accept all the data
         */
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                int readCount = buffer.read(readBuffer);

                if (readCount > 0) {
                    int writeCount = sourceDataLine.write(readBuffer, 0, readCount);

                    if (writeCount != readCount) {
                        throw new RuntimeException("Unable to write all samples: " + readCount + " != " + writeCount);
                    }
                } else {
                    idleStrategy.idle();
                }
            }
        }
    }
}
