package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeCustomReplayManager.FailsafeReplayType;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;

import java.util.List;

public final class GhostBlockFailsafeRegistryProvider extends AbstractFailsafesRegistryProvider {
    public GhostBlockFailsafeRegistryProvider() {
        super(4);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "Ghost Block Failsafe",
                        "Triggers when the farming exp text disappears while the macro is actively farming")
                .add(FailsafeActionSettings.createActionDropdown("Action",
                        () -> TfmConfig.FAILSAFE_GHOST_BLOCK_ACTION.get(),
                        value -> TfmConfig.FAILSAFE_GHOST_BLOCK_ACTION.set(value)))
                .add(FailsafeActionSettings.createCustomReplayDropdown(FailsafeReplayType.GHOST_BLOCK,
                        () -> TfmConfig.FAILSAFE_GHOST_BLOCK_ACTION.get()))
                .add(new SliderSetting("Window", 1, 30,
                        () -> (float) TfmConfig.FAILSAFE_GHOST_BLOCK_WINDOW_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_GHOST_BLOCK_WINDOW_SECONDS.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("s"))
                .add(new SliderSetting("Trigger Delay", 0, 5,
                        () -> TfmConfig.FAILSAFE_GHOST_BLOCK_TRIGGER_DELAY_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_GHOST_BLOCK_TRIGGER_DELAY_SECONDS.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("s"));

        return MainGUIRegistry.toggleSubTab(
                "Ghost Block",
                "Triggers when the farming exp text disappears while the macro is actively farming",
                () -> TfmConfig.FAILSAFE_GHOST_BLOCK.get(),
                v -> {
                    TfmConfig.FAILSAFE_GHOST_BLOCK.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
