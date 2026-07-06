package dev.typicalfarmingmacro.util;

import java.io.InputStream;

public final class TfmResources {
    private TfmResources() {
    }

    public static InputStream open(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        ClassLoader classLoader = TfmResources.class.getClassLoader();
        InputStream input = classLoader.getResourceAsStream(normalized);
        if (input != null) {
            return input;
        }
        return TfmResources.class.getResourceAsStream(resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath);
    }
}
