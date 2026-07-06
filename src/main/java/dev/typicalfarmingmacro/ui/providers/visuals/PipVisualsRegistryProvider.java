package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.bootstrap.TfmKeybindRegistry;
import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.visuals.PipManager;
import dev.typicalfarmingmacro.ui.settings.KeybindSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.List;

public final class PipVisualsRegistryProvider extends AbstractVisualsRegistryProvider {
    public PipVisualsRegistryProvider() {
        super(3);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                "PiP Settings",
                "Open a picture-in-picture view of the game"
        );
        group.add(new KeybindSetting("Toggle Keybind", TfmKeybindRegistry.getPipKey()));
        group.add(new ToggleSetting("Start Floating",
                () -> TfmConfig.PIP_START_FLOATING.get(),
                value -> {
                    TfmConfig.PIP_START_FLOATING.set(value);
                    TfmConfig.save();
                }));
        group.add(new ToggleSetting("Window Decorations",
                () -> TfmConfig.PIP_START_DECORATED.get(),
                value -> {
                    TfmConfig.PIP_START_DECORATED.set(value);
                    TfmConfig.save();
                }));
        group.add(new SliderSetting("Window Width", 240.0f, 1920.0f,
                () -> (float) TfmConfig.PIP_WINDOW_WIDTH.get(),
                value -> {
                    TfmConfig.PIP_WINDOW_WIDTH.set(Math.round(value));
                    TfmConfig.save();
                })
                .withDecimals(0)
                .withSuffix(" px"));
        group.add(new SliderSetting("Window Height", 135.0f, 1080.0f,
                () -> (float) TfmConfig.PIP_WINDOW_HEIGHT.get(),
                value -> {
                    TfmConfig.PIP_WINDOW_HEIGHT.set(Math.round(value));
                    TfmConfig.save();
                })
                .withDecimals(0)
                .withSuffix(" px"));

        return MainGUIRegistry.toggleSubTab(
                "PiP",
                "Open a picture-in-picture view of the game",
                PipManager::isEnabled,
                PipManager::setEnabled,
                List.of(group)
        );
    }
}
