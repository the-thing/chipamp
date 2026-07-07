package com.github.thething.chipamp.mod;

import com.github.thething.chipamp.common.Maths;
import com.github.thething.chipamp.concurrent.SpScByteCircularBuffer;

import javax.sound.sampled.SourceDataLine;
import java.io.Closeable;
import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

public final class AsyncSourceDataLine implements Closeable {

    private final SpScByteCircularBuffer buffer;
    private final Thread thread;

    private AsyncSourceDataLine(SpScByteCircularBuffer buffer, Thread thread) {
        this.buffer = requireNonNull(buffer);
        this.thread = requireNonNull(thread);
    }

    public static AsyncSourceDataLine launch(SourceDataLine line, int bufferCapacity, int readLength, ThreadFactory threadFactory) {
        if (!line.isOpen()) {
            throw new IllegalStateException("SourceDataLine is not open");
        }

        if (line.getBufferSize() <= 0) {
            throw new IllegalArgumentException("SourceDataLine buffer size must be greater than zero");
        }

        if (bufferCapacity <= 0) {
            throw new IllegalArgumentException("bufferCapacity must be greater than zero");
        }

        int length = Math.min(bufferCapacity, line.getBufferSize());
        length = Maths.roundDownPow2(length);

        if (length < 4) {
            throw new IllegalArgumentException("bufferCapacity must be greater than or equal to 4");
        }

        SpScByteCircularBuffer buffer = new SpScByteCircularBuffer(length);
        Task task = new Task(line, buffer, readLength);
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
        private final byte[] readBuffer;

        private Task(SourceDataLine sourceDataLine, SpScByteCircularBuffer buffer, int readLength) {
            this.sourceDataLine = sourceDataLine;
            this.buffer = buffer;
            this.readBuffer = new byte[readLength];
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                int readCount = buffer.peek(readBuffer);

                if (readCount > 0) {
                    int writeCount = sourceDataLine.write(readBuffer, 0, readCount);

                    if (writeCount != readCount) {
                        throw new RuntimeException("Unable to write all samples");
                    }

                    buffer.skipBytes(writeCount);
                } else {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
