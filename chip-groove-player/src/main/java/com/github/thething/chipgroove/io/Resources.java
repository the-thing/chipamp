package com.github.thething.chipgroove.io;

import java.io.IOException;
import java.io.InputStream;

public final class Resources {

    private Resources() {
    }

    public static InputStream getResourceAsStream(String name) throws IOException {
        InputStream in = Resources.class.getClassLoader().getResourceAsStream(name);

        if (in == null) {
            throw new IOException("Unable to find resource: " + name);
        }

        return in;
    }
}
