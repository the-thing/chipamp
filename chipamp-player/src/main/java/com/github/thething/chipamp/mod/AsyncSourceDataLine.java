package com.github.thething.chipamp.mod;

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

    public static AsyncSourceDataLine launch(SourceDataLine line, int chunkLength, ThreadFactory threadFactory) {
        if (!line.isOpen()) {
            throw new IllegalStateException("SourceDataLine is not open");
        }

        if (line.getBufferSize() <= 0) {
            throw new IllegalArgumentException("SourceDataLine buffer size must be greater than zero");
        }

        if (chunkLength <= 0) {
            throw new IllegalArgumentException("chunkLength must be greater than zero");
        }

        int readLength = Math.min(chunkLength, line.getBufferSize());

        SpScByteCircularBuffer buffer = new SpScByteCircularBuffer(1024 * 1024 * 10);
        Task task = new Task(line, buffer, 1024);
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

        public Task(SourceDataLine sourceDataLine, SpScByteCircularBuffer buffer, int readLength) {
            this.sourceDataLine = sourceDataLine;
            this.buffer = buffer;
            this.readBuffer = new byte[readLength];
        }

        @Override
        public void run() {

            int maxBufferUnderFlow = 0;

            while (!Thread.currentThread().isInterrupted()) {
                int size = buffer.size();

                // System.out.println("size: " + size);

                // int min = Math.min(size / 2, readBuffer.length);
                int readLength = buffer.peek(readBuffer, 0, 4);

                if (readLength > 0) {
                    maxBufferUnderFlow = 0;
                    int written = sourceDataLine.write(readBuffer, 0, readLength);

                    if (written != readLength) {
                        throw new RuntimeException("Unexpected end of audio");
                    }

                    buffer.skipBytes(written);
                } else {
                    maxBufferUnderFlow++;

                    System.out.println("maxBufferUnderFlow: " + maxBufferUnderFlow);

                    // System.out.println("buffer underflow: " + readLength);
//                    try {
//                        Thread.sleep(1L);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        return;
//                    }

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
