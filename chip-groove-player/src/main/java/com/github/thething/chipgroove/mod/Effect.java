package com.github.thething.chipgroove.mod;

public interface Effect {

    void onNewRow(Channel channel, Context context, Config config);

    void onMidRow(Channel channel, Context context, Config config);
}
