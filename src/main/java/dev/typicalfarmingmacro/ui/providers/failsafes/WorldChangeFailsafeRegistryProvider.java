package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeCustomReplayManager.FailsafeReplayType;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;

import java.util.List;

public final class WorldChangeFailsafeRegistryProvider extends AbstractFailsafesRegistryProvider {
    public WorldChangeFailsafeRegistryProvider() {
        super(7);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "World Change Failsafe",
                        "Handles unexpected world changes while farming")
                .add(FailsafeActionSettings.createActionDropdown("Action",
                        () -> TfmConfig.FAILSAFE_WORLD_CHANGE_ACTION.get(),
                        value -> TfmConfig.FAILSAFE_WORLD_CHANGE_ACTION.set(value)))
                .add(FailsafeActionSettings.createCustomReplayDropdown(FailsafeReplayType.WORLD_CHANGE,
                        () -> TfmConfig.FAILSAFE_WORLD_CHANGE_ACTION.get()))
                .add(new SliderSetting("Recovery Wait", 0, 30,
                        () -> TfmConfig.FAILSAFE_WORLD_CHANGE_RECOVERY_WAIT_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_WORLD_CHANGE_RECOVERY_WAIT_SECONDS.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("s"));

        return MainGUIRegistry.toggleSubTab(
                "World Change",
                "Handles unexpected world changes while farming",
                () -> TfmConfig.FAILSAFE_WORLD_CHANGE.get(),
                v -> {
                    TfmConfig.FAILSAFE_WORLD_CHANGE.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
