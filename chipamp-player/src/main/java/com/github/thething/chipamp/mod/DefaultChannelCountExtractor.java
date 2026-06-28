package com.github.thething.chipamp.mod;

import java.util.function.ToIntFunction;

public final class DefaultChannelCountExtractor implements ToIntFunction<String> {

    @Override
    public int applyAsInt(String value) {
        return 0;
    }
}
