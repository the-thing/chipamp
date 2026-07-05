package com.github.thething.chipamp.concurrent;

import com.github.thething.chipamp.common.Maths;
import com.github.thething.chipamp.common.VisibleForTesting;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.checkFromIndexSize;

public final class SpScByteCircularBuffer {

    private final byte[] buffer;
    private final int bufferMask;
    private final AtomicInteger writeIndex;
    private final AtomicInteger readIndex;

    public SpScByteCircularBuffer(int capacity) {
        this.buffer = new byte[Maths.roundUpPow2(capacity)];
        this.bufferMask = buffer.length - 1;
        this.writeIndex = new AtomicInteger();
        this.readIndex = new AtomicInteger();
    }

    public int write(byte[] data) {
        return write(data, 0, data.length);
    }

    public int write(byte[] data, int offset, int length) {
        checkFromIndexSize(offset, length, data.length);

        int writeIndex = this.writeIndex.getPlain();
        int readIndex = this.readIndex.getAcquire();

        int available = buffer.length - (writeIndex - readIndex);
        length = Math.min(length, available);

        if (length == 0) {
            return 0;
        }

        int startIndex = readIndex & bufferMask;
        int delta = buffer.length - startIndex;

        if (delta < length) {
            // buffer will be broken in half so we have to do it in two chunks
            System.arraycopy(data, offset, buffer, startIndex, delta);
            System.arraycopy(data, offset + delta, buffer, 0, length - delta);
        } else {
            System.arraycopy(data, offset, buffer, startIndex, length);
        }

        this.writeIndex.setRelease(writeIndex + length);

        return length;
    }

    public int read(byte[] output) {
        return read(output, 0, output.length);
    }

    public int read(byte[] output, int offset, int length) {
        length = peek(output, offset, length);
        skipBytes(length);
        return length;
    }

    public int peek(byte[] output) {
        return peek(output, 0, output.length);
    }

    public int peek(byte[] output, int offset, int length) {
        checkFromIndexSize(offset, length, output.length);

        int writeIndex = this.writeIndex.getAcquire();
        int readIndex = this.readIndex.getAcquire();

        int size = writeIndex - readIndex;
        length = Math.min(length, size);

        if (length == 0) {
            return 0;
        }

        int startIndex = readIndex & bufferMask;
        int delta = buffer.length - startIndex;

        if (delta < length) {
            // buffer will be broken in half so we have to do it in two chunks
            System.arraycopy(buffer, startIndex, output, offset, delta);
            System.arraycopy(buffer, 0, output, offset + delta, length - delta);
        } else {
            System.arraycopy(buffer, startIndex, output, offset, length);
        }

        return length;
    }

    public int size() {
        return writeIndex.getPlain() - readIndex.getAcquire();
    }

    public void clear() {
        this.readIndex.setRelease(this.writeIndex.getPlain());
    }

    public int skipBytes(int count) {
        int readIndex = this.readIndex.getPlain();
        int size = writeIndex.getAcquire() - readIndex;

        count = Math.min(count, size);

        if (count > 0) {
            this.readIndex.setRelease(readIndex + count);
        }

        return count;
    }

    @VisibleForTesting
    int getReadIndexPlain() {
        return readIndex.getPlain();
    }

    @VisibleForTesting
    int getReadIndexAcquire() {
        return readIndex.getAcquire();
    }

    @VisibleForTesting
    int getWriteIndexPlain() {
        return writeIndex.getPlain();
    }

    @VisibleForTesting
    int getWriteIndexAcquire() {
        return writeIndex.getAcquire();
    }
}
