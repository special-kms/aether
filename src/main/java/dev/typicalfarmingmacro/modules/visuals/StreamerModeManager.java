package dev.typicalfarmingmacro.modules.visuals;

import dev.typicalfarmingmacro.config.TfmConfig;

public final class StreamerModeManager {
    private StreamerModeManager() {
    }

    public static boolean isEnabled() {
        return TfmConfig.STREAMER_MODE.get();
    }

    public static void setEnabled(boolean enabled) {
        TfmConfig.STREAMER_MODE.set(enabled);
        if (enabled) {
            FreecamManager.setEnabled(false);
            PipManager.setEnabled(false);
            UngrabMouseManager.setEnabled(false);
        }
        TfmConfig.save();
    }
}
