package com.github.thething.chipgroove.common;

import static java.util.Objects.checkFromIndexSize;

// TODO
public final class DynamicByteArray {

    private byte[] bytes;
    private int size;

    public DynamicByteArray(int initialCapacity) {
        if (initialCapacity < 0) {
        }
    }

    public void append(byte[] array) {
        append(array, 0, array.length);
    }

    public void append(byte[] array, int offset, int length) {
        checkFromIndexSize(offset, length, array.length);
        ensureCapacity(length);
    }

    private void ensureCapacity(int additionalCapacity) {
//        if (bytes.length - size < additionalCapacity) {
//            int newLength = Maths.nextPowerOfTwo(size + additionalCapacity);
//
//
//
//            // if (newLength )
//
//        }
    }

    public int size() {
        return size;
    }
}
