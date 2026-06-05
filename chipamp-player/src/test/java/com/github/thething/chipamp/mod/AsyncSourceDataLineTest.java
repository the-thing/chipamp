package com.github.thething.chipamp.mod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sound.sampled.SourceDataLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AsyncSourceDataLineTest {

    @Mock
    private SourceDataLine sourceDataLine;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            // return the length of written bytes
            return invocation.getArgument(2);
        }).when(sourceDataLine).write(any(byte[].class), anyInt(), anyInt());


    }

    @Test
    void shouldWriteAndCloseBuffer() throws InterruptedException {
        doReturn(true).when(sourceDataLine).isOpen();

        byte[] buffer = new byte[]{1, 2, 3, 4};

        try (AsyncSourceDataLine line = AsyncSourceDataLine.launch(sourceDataLine,1024)){
            int writeCount = line.write(buffer);

            while (!line.getBuffer().isEmpty()) {
                Thread.yield();
            }

            Thread.sleep(100L);

            assertThat(writeCount).isEqualTo(4);
        }

        verify(sourceDataLine).isOpen();
        verify(sourceDataLine).write(any(byte[].class), eq(0), eq(4));
        verifyNoMoreInteractions(sourceDataLine);
    }
}