package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;

public final class ComposterSettingsBridge {
    private static final String SOURCE_SACKS = "SACKS";
    private static final String SOURCE_BAZAAR = "BAZAAR";

    private ComposterSettingsBridge() {
    }

    public static void setSourceModeIndex(int index) {
        TfmConfig.AUTO_COMPOSTER_SOURCE_MODE.set(index == 1 ? SOURCE_BAZAAR : SOURCE_SACKS);
        TfmConfig.save();
    }
}
