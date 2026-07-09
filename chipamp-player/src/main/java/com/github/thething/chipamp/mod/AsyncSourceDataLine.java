package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.concurrent.IdleStrategy;
import com.github.thething.chipamp.concurrent.SleepingIdleStrategy;
import com.github.thething.chipamp.concurrent.SpScByteCircularBuffer;

import javax.sound.sampled.SourceDataLine;
import java.io.Closeable;
import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

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

    public static AsyncSourceDataLine launch(SourceDataLine line, int readLength) {
        return launch(line, DEFAULT_IDLE_STRATEGY, DEFAULT_BUFFER_CAPACITY, readLength, DEFAULT_THREAD_FACTORY);
    }

    public static AsyncSourceDataLine launch(SourceDataLine line, IdleStrategy idleStrategy, int bufferCapacity, int readLength, ThreadFactory threadFactory) {
        if (!line.isOpen()) {
            throw new IllegalStateException("SourceDataLine is not open");
        }

        requireNonNull(idleStrategy);

        if (bufferCapacity < 0) {
            throw new IllegalArgumentException("bufferCapacity must be greater than or equal to zero");
        }

        SpScByteCircularBuffer buffer = new SpScByteCircularBuffer(bufferCapacity);
        Task task = new Task(line, buffer, idleStrategy, readLength);
        Thread thread = threadFactory.newThread(task);
        thread.start();

        return new AsyncSourceDataLine(buffer, thread);
    }

    public int write(byte[] data) {
        return write(data, 0, data.length);
    }

    public int write(byte[] data, int offset, int length) {
        return buffer.write(data, offset, length);
    }

    public int size() {
        return buffer.size();
    }

    @Override
    public void close() {
        thread.interrupt();
    }

    private static final class Task implements Runnable {

        private final SourceDataLine sourceDataLine;
        private final SpScByteCircularBuffer buffer;
        private final IdleStrategy idleStrategy;
        private final byte[] readBuffer;

        private Task(SourceDataLine sourceDataLine, SpScByteCircularBuffer buffer, IdleStrategy idleStrategy, int readLength) {
            this.sourceDataLine = sourceDataLine;
            this.buffer = buffer;
            this.idleStrategy = idleStrategy;
            this.readBuffer = new byte[readLength];
        }

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
