package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.bootstrap.TfmKeybindRegistry;
import dev.typicalfarmingmacro.modules.visuals.UngrabMouseManager;
import dev.typicalfarmingmacro.ui.settings.KeybindSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;

import java.util.List;

public final class UngrabMouseVisualsRegistryProvider extends AbstractVisualsRegistryProvider {
    public UngrabMouseVisualsRegistryProvider() {
        super(4);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                "Ungrab Mouse Settings",
                "Release the mouse cursor so you can move it outside the game window"
        );
        group.add(new KeybindSetting("Toggle Keybind", TfmKeybindRegistry.getUngrabMouseKey()));

        return MainGUIRegistry.toggleSubTab(
                "Ungrab Mouse",
                "Release the mouse cursor so you can move it outside the game window",
                UngrabMouseManager::isEnabled,
                UngrabMouseManager::setEnabled,
                List.of(group)
        );
    }
}
