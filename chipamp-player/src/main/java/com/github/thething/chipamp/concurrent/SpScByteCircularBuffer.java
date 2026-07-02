package com.github.thething.chipamp.concurrent;

import com.github.thething.chipamp.common.Maths;

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

        System.arraycopy(data, offset, buffer, writeIndex & bufferMask, length);
        this.writeIndex.setRelease(writeIndex + length);

        return length;
    }

    public int read(byte[] output) {
        return read(output, 0, output.length);
    }

    public int read(byte[] output, int offset, int length) {
        length = readNoAdvance(output, offset, length);
        this.readIndex.addAndGet(length);
        return length;
    }

    public int readNoAdvance(byte[] output) {
        return readNoAdvance(output, 0, output.length);
    }

    public int readNoAdvance(byte[] output, int offset, int length) {
        checkFromIndexSize(offset, length, output.length);

        int writeIndex = this.writeIndex.getAcquire();
        int readIndex = this.readIndex.getAcquire();

        int size = writeIndex - readIndex;
        length = Math.min(length, size);

        if (length == 0) {
            return 0;
        }

        System.arraycopy(buffer, readIndex & bufferMask, output, offset, length);

        return length;
    }

    public int size() {
        return writeIndex.getPlain() - readIndex.getAcquire();
    }

    public void clear() {
        this.readIndex.setRelease(this.writeIndex.getPlain());
    }

    public int getReadIndexPlain() {
        return readIndex.getPlain();
    }

    public int getReadIndexAcquire() {
        return readIndex.getAcquire();
    }

    public int getWriteIndexPlain() {
        return writeIndex.getPlain();
    }

    public int getWriteIndexAcquire() {
        return writeIndex.getAcquire();
    }

    public void setReadIndexPlain(int index) {
        readIndex.setPlain(index);
    }

    public void setReadIndexRelease(int index) {
        readIndex.setRelease(index);
    }

    public void addReadIndex(int delta) {
        readIndex.setRelease(readIndex.getPlain() + delta);
    }

    public void setWriteIndexPlain(int index) {
        writeIndex.setPlain(index);
    }

    public void setWriteIndexRelease(int index) {
        writeIndex.setRelease(index);
    }

    public int getCapacity() {
        return buffer.length;
    }
}
