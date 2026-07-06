package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.ui.settings.ColorSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

public final class HudColorsRegistryProvider extends AbstractColorsRegistryProvider {
    public HudColorsRegistryProvider() {
        super(0);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();
        SettingGroup hudColors = SettingGroup.alwaysOn(
                "HUD Colors",
                "Customize HUD element colors");
        for (Theme.ThemeEntry entry : Theme.HUD_ENTRIES) {
            hudColors.add(new ColorSetting(entry.label, entry.getter,
                    value -> {
                        entry.setter.accept(value);
                        Theme.saveTheme();
                    }));
        }
        groups.add(hudColors);
        return MainGUIRegistry.subTab("HUD Colors", "Customize HUD element colors", groups);
    }
}
