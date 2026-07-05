package com.github.thething.chipamp.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SpScByteCircularBufferTest {

    private SpScByteCircularBuffer underTest;

    @BeforeEach
    void setUp() {
        underTest = new SpScByteCircularBuffer(8);
    }

    @Test
    void shouldWriteAndRead() {
        byte[] buffer = new byte[8];
        int readCount, writeCount;

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(0);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(0);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(0);

        writeCount = underTest.write(new byte[]{1, 2, 3, 4});
        assertThat(writeCount).isEqualTo(4);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(4);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(4);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(4);

        readCount = underTest.read(buffer);
        assertThat(readCount).isEqualTo(4);
        assertThat(Arrays.copyOf(buffer, 4)).containsExactly(1, 2, 3, 4);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(4);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(4);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(4);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(4);
        assertThat(underTest.size()).isEqualTo(0);

        writeCount = underTest.write(new byte[]{5, 6, 7, 8});
        assertThat(writeCount).isEqualTo(4);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(8);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(8);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(4);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(4);
        assertThat(underTest.size()).isEqualTo(4);

        readCount = underTest.read(buffer);
        assertThat(readCount).isEqualTo(4);
        assertThat(Arrays.copyOf(buffer, 4)).containsExactly(5, 6, 7, 8);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(8);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(8);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(8);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(8);
        assertThat(underTest.size()).isEqualTo(0);

        writeCount = underTest.write(new byte[]{9, 10, 11, 12, 13, 14, 15, 16});
        assertThat(writeCount).isEqualTo(8);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(16);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(16);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(8);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(8);
        assertThat(underTest.size()).isEqualTo(8);

        readCount = underTest.read(buffer);
        assertThat(readCount).isEqualTo(8);
        assertThat(buffer).containsExactly(9, 10, 11, 12, 13, 14, 15, 16);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(16);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(16);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(16);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(16);
        assertThat(underTest.size()).isEqualTo(0);
    }

    @Test
    void shouldPeek() {
        byte[] buffer = new byte[8];
        int readCount, writeCount;

        readCount = underTest.peek(buffer);
        assertThat(readCount).isEqualTo(0);
        assertThat(Arrays.copyOf(buffer, 0)).containsExactly();

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(0);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(0);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(0);

        writeCount = underTest.write(new byte[]{1, 2, 3, 4});
        assertThat(writeCount).isEqualTo(4);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(4);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(4);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(4);

        readCount = underTest.peek(buffer);
        assertThat(readCount).isEqualTo(4);
        assertThat(Arrays.copyOf(buffer, 4)).containsExactly(1, 2, 3, 4);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(4);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(4);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(4);

        readCount = underTest.read(buffer);
        assertThat(readCount).isEqualTo(4);
        assertThat(Arrays.copyOf(buffer, 4)).containsExactly(1, 2, 3, 4);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(4);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(4);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(4);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(4);
        assertThat(underTest.size()).isEqualTo(0);
    }

    @Test
    void shouldSkipAvailableBytes() {
        int skipCount, writeCount;

        writeCount = underTest.write(new byte[]{1, 2, 3, 4});
        assertThat(writeCount).isEqualTo(4);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(4);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(4);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(4);

        skipCount = underTest.skipBytes(1);
        assertThat(skipCount).isEqualTo(1);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(4);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(4);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(1);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(1);
        assertThat(underTest.size()).isEqualTo(3);

        skipCount = underTest.skipBytes(10);
        assertThat(skipCount).isEqualTo(3);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(4);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(4);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(4);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(4);
        assertThat(underTest.size()).isEqualTo(0);

        skipCount = underTest.skipBytes(1);
        assertThat(skipCount).isEqualTo(0);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(4);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(4);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(4);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(4);
        assertThat(underTest.size()).isEqualTo(0);
    }

    @Test
    void shouldWriteAndReadAtBoundary() {
        byte[] buffer = new byte[8];
        int readCount, writeCount;

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(0);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(0);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(0);

        writeCount = underTest.write(new byte[]{1, 2});
        assertThat(writeCount).isEqualTo(2);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(2);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(2);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(2);

        readCount = underTest.read(buffer);
        assertThat(readCount).isEqualTo(2);
        assertThat(Arrays.copyOf(buffer, 2)).containsExactly(1, 2);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(2);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(2);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(2);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(2);
        assertThat(underTest.size()).isEqualTo(0);

        writeCount = underTest.write(new byte[]{3, 4, 5, 6, 7, 8, 9, 10});
        assertThat(writeCount).isEqualTo(8);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(10);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(10);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(2);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(2);
        assertThat(underTest.size()).isEqualTo(8);

        readCount = underTest.read(buffer);
        assertThat(readCount).isEqualTo(8);
        assertThat(buffer).containsExactly(3, 4, 5, 6, 7, 8, 9, 10);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(10);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(10);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(10);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(10);
        assertThat(underTest.size()).isEqualTo(0);

        writeCount = underTest.write(new byte[]{11, 12, 13, 14, 15, 16});
        assertThat(writeCount).isEqualTo(6);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(16);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(16);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(10);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(10);
        assertThat(underTest.size()).isEqualTo(6);

        readCount = underTest.read(buffer, 0, 2);
        assertThat(readCount).isEqualTo(2);
        assertThat(Arrays.copyOf(buffer, 2)).containsExactly(11, 12);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(16);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(16);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(12);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(12);
        assertThat(underTest.size()).isEqualTo(4);
    }

    @Test
    void shouldWriteNoBytesWhenFull() {
        int writeCount;

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(0);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(0);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(0);

        writeCount = underTest.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        assertThat(writeCount).isEqualTo(8);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(8);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(8);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(8);

        writeCount = underTest.write(new byte[]{1});
        assertThat(writeCount).isEqualTo(0);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(8);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(8);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(8);

        writeCount = underTest.write(new byte[]{1});
        assertThat(writeCount).isEqualTo(0);

        assertThat(underTest.getWriteIndexPlain()).isEqualTo(8);
        assertThat(underTest.getWriteIndexAcquire()).isEqualTo(8);
        assertThat(underTest.getReadIndexPlain()).isEqualTo(0);
        assertThat(underTest.getReadIndexAcquire()).isEqualTo(0);
        assertThat(underTest.size()).isEqualTo(8);
    }
}
