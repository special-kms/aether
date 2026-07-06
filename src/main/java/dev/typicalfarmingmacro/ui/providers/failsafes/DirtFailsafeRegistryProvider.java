package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeCustomReplayManager.FailsafeReplayType;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;

import java.util.List;

public final class DirtFailsafeRegistryProvider extends AbstractFailsafesRegistryProvider {
    public DirtFailsafeRegistryProvider() {
        super(5);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "Dirt Check Failsafe",
                        "Triggers when a suspicious solid block stays close to the player during farming")
                .add(FailsafeActionSettings.createActionDropdown("Action",
                        () -> TfmConfig.FAILSAFE_DIRT_CHECK_ACTION.get(),
                        value -> TfmConfig.FAILSAFE_DIRT_CHECK_ACTION.set(value)))
                .add(FailsafeActionSettings.createCustomReplayDropdown(FailsafeReplayType.DIRT_CHECK,
                        () -> TfmConfig.FAILSAFE_DIRT_CHECK_ACTION.get()))
                .add(new SliderSetting("Trigger Delay", 0, 10,
                        () -> TfmConfig.FAILSAFE_DIRT_CHECK_TRIGGER_DELAY_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_DIRT_CHECK_TRIGGER_DELAY_SECONDS.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("s"));

        return MainGUIRegistry.toggleSubTab(
                "Dirt Check",
                "Triggers when a suspicious solid block stays close to the player during farming",
                () -> TfmConfig.FAILSAFE_DIRT_CHECK.get(),
                v -> {
                    TfmConfig.FAILSAFE_DIRT_CHECK.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
