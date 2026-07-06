package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;

import java.util.List;

public final class BootstrapSettingsRegistryProvider implements MainGUIRegistryProvider {
    private static final int ORDER = 1;

    @Override
    public void register(MainGUIRegistry.Registrar registrar) {
        registrar.registerSettings(ORDER, createSubTab());
    }

    private ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                "Bootstrap",
                "Settings that apply before premium modules are loaded");
        group.add(new ToggleSetting("Custom UI",
                () -> TfmConfig.CUSTOM_UI_ENABLED.get(),
                value -> {
                    TfmConfig.CUSTOM_UI_ENABLED.set(value);
                    TfmConfig.save();
                }));
        return MainGUIRegistry.subTab("Bootstrap", "Pre-login bootstrap settings", List.of(group));
    }
}
