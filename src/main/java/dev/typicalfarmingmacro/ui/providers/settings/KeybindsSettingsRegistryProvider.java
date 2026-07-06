package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.bootstrap.TfmKeybindRegistry;
import dev.typicalfarmingmacro.ui.settings.KeybindSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;

import java.util.ArrayList;
import java.util.List;

public final class KeybindsSettingsRegistryProvider extends AbstractKeybindsRegistryProvider {
    public KeybindsSettingsRegistryProvider() {
        super(0);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();
        SettingGroup keybinds = SettingGroup.alwaysOn(
                "Tfm Keybinds",
                "These bindings stay synced with Minecraft's Controls screen"
        );

        for (TfmKeybindRegistry.RegisteredKeybind registeredKeybind : TfmKeybindRegistry.getRegisteredKeybinds()) {
            keybinds.add(new KeybindSetting(registeredKeybind.name(), registeredKeybind.mapping()));
        }

        groups.add(keybinds);
        return MainGUIRegistry.subTab("Tfm", "Keyboard shortcuts for the client", groups);
    }
}
