package dev.aether.modules.clips;

public enum ClipResolution {
    P480("480p", "480p (854x480)", 854, 480),
    P720("720p", "720p (1280x720)", 1280, 720),
    P1080("1080p", "1080p (1920x1080)", 1920, 1080);

    private final String id;
    private final String label;
    private final int width;
    private final int height;

    ClipResolution(String id, String label, int width, int height) {
        this.id = id;
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public static ClipResolution fromId(String id) {
        for (ClipResolution resolution : values()) {
            if (resolution.id.equals(id)) {
                return resolution;
            }
        }
        return P720;
    }
}
