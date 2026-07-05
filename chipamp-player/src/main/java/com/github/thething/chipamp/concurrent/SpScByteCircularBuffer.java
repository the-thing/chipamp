package com.github.thething.chipamp.concurrent;

import com.github.thething.chipamp.common.Maths;
import com.github.thething.chipamp.common.VisibleForTesting;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.checkFromIndexSize;

/**
 * A lock-free Single Producer Single Consumer (SPSC) circular buffer for byte data.
 * <p>
 * This buffer is designed for concurrent access by exactly one producer thread and one consumer thread. The
 * implementation uses atomic operations with acquire/release semantics to ensure thread-safe communication between the
 * producer and consumer without locks.
 * <p>
 */
public final class SpScByteCircularBuffer {

    private final byte[] buffer;
    private final int bufferMask;
    private final AtomicInteger writeIndex;
    private final AtomicInteger readIndex;

    /**
     * Creates a new circular buffer with the specified capacity.
     * <p>
     * The actual capacity will be rounded up to the nearest power of 2 for performance optimization.
     *
     * @param capacity the desired minimum capacity in bytes
     */
    public SpScByteCircularBuffer(int capacity) {
        this.buffer = new byte[Maths.roundUpPow2(capacity)];
        this.bufferMask = buffer.length - 1;
        this.writeIndex = new AtomicInteger();
        this.readIndex = new AtomicInteger();
    }

    /**
     * Writes all bytes from the specified array to the buffer.
     * <p>
     * <b>Thread Safety:</b> This method must be called only by the producer thread.
     *
     * @param data the byte array to write
     * @return the number of bytes actually written (may be less than {@code data.length} if buffer is full)
     */
    public int write(byte[] data) {
        return write(data, 0, data.length);
    }

    /**
     * Writes bytes from a portion of the specified array to the buffer.
     * <p>
     * <b>Thread Safety:</b> This method must be called only by the producer thread.
     *
     * @param data   the byte array containing the data to write
     * @param offset the starting position in the array
     * @param length the number of bytes to write
     * @return the number of bytes actually written (may be less than {@code length} if buffer is full)
     * @throws IndexOutOfBoundsException if {@code offset} or {@code length} are invalid
     */
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

    /**
     * Reads and removes bytes from the buffer into the specified array.
     * <p>
     * This is equivalent to {@link #peek(byte[])} followed by {@link #skipBytes(int)}.
     * <p>
     * <b>Thread Safety:</b> This method must be called only by the consumer thread.
     *
     * @param output the array to fill with data from the buffer
     * @return the number of bytes read (may be less than {@code output.length} if buffer has fewer bytes available)
     */
    public int read(byte[] output) {
        return read(output, 0, output.length);
    }

    /**
     * Reads and removes bytes from the buffer into a portion of the specified array.
     * <p>
     * This is equivalent to {@link #peek(byte[], int, int)} followed by {@link #skipBytes(int)}.
     * <p>
     * <b>Thread Safety:</b> This method must be called only by the consumer thread.
     *
     * @param output the array to fill with data from the buffer
     * @param offset the starting position in the output array
     * @param length the maximum number of bytes to read
     * @return the number of bytes read (may be less than {@code length} if buffer has fewer bytes available)
     * @throws IndexOutOfBoundsException if {@code offset} or {@code length} are invalid
     */
    public int read(byte[] output, int offset, int length) {
        length = peek(output, offset, length);
        skipBytes(length);
        return length;
    }

    /**
     * Peeks at bytes in the buffer without removing them, copying into the specified array.
     * <p>
     * Unlike {@link #read(byte[])}, this method does not advance the read position.
     * <p>
     * <b>Thread Safety:</b> This method must be called only by the consumer thread.
     *
     * @param output the array to fill with data from the buffer
     * @return the number of bytes peeked (may be less than {@code output.length} if buffer has fewer bytes available)
     */
    public int peek(byte[] output) {
        return peek(output, 0, output.length);
    }

    /**
     * Peeks at bytes in the buffer without removing them, copying into a portion of the specified array.
     * <p>
     * Unlike {@link #read(byte[], int, int)}, this method does not advance the read position.
     * <p>
     * <b>Thread Safety:</b> This method must be called only by the consumer thread.
     *
     * @param output the array to fill with data from the buffer
     * @param offset the starting position in the output array
     * @param length the maximum number of bytes to peek
     * @return the number of bytes peeked (may be less than {@code length} if buffer has fewer bytes available)
     * @throws IndexOutOfBoundsException if {@code offset} or {@code length} are invalid
     */
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

    /**
     * Returns the approximate number of bytes currently in the buffer.
     * <p>
     * <b>Thread Safety:</b> This method can be called by either producer or consumer thread,
     * but the returned value is approximate and may not reflect concurrent modifications.
     *
     * @return the approximate number of bytes available for reading
     */
    public int size() {
        return writeIndex.getAcquire() - readIndex.getAcquire();
    }

    /**
     * Advances the read position by skipping the specified number of bytes.
     * <p>
     * <b>Thread Safety:</b> This method must be called only by the consumer thread.
     *
     * @param count the number of bytes to skip
     * @return the actual number of bytes skipped (may be less than {@code count} if fewer bytes are available)
     */
    public int skipBytes(int count) {
        int readIndex = this.readIndex.getPlain();
        int size = writeIndex.getAcquire() - readIndex;

        count = Math.min(count, size);

        if (count > 0) {
            this.readIndex.setRelease(readIndex + count);
        }

        return count;
    }

    /**
     * Returns the current read index using plain memory semantics.
     *
     * @return the current read index
     */
    @VisibleForTesting
    int getReadIndexPlain() {
        return readIndex.getPlain();
    }

    /**
     * Returns the current read index using acquire memory semantics.
     *
     * @return the current read index
     */
    @VisibleForTesting
    int getReadIndexAcquire() {
        return readIndex.getAcquire();
    }

    /**
     * Returns the current write index using plain memory semantics.
     *
     * @return the current write index
     */
    @VisibleForTesting
    int getWriteIndexPlain() {
        return writeIndex.getPlain();
    }

    /**
     * Returns the current write index using acquire memory semantics.
     *
     * @return the current write index
     */
    @VisibleForTesting
    int getWriteIndexAcquire() {
        return writeIndex.getAcquire();
    }
}
