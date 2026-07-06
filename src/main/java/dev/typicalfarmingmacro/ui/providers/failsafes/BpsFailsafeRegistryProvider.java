package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeCustomReplayManager.FailsafeReplayType;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;

import java.util.List;

public final class BpsFailsafeRegistryProvider extends AbstractFailsafesRegistryProvider {
    public BpsFailsafeRegistryProvider() {
        super(2);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "BPS Failsafe",
                        "Triggers when block breaks per second fall below the configured threshold")
                .add(FailsafeActionSettings.createActionDropdown("Action",
                        () -> TfmConfig.FAILSAFE_BPS_ACTION.get(),
                        value -> TfmConfig.FAILSAFE_BPS_ACTION.set(value)))
                .add(FailsafeActionSettings.createCustomReplayDropdown(FailsafeReplayType.BPS,
                        () -> TfmConfig.FAILSAFE_BPS_ACTION.get()))
                .add(new SliderSetting("Threshold", 5, 15,
                        () -> (float) TfmConfig.FAILSAFE_BPS_THRESHOLD.get(),
                        v -> {
                            TfmConfig.FAILSAFE_BPS_THRESHOLD.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0))
                .add(new SliderSetting("Window", 5, 30,
                        () -> (float) TfmConfig.FAILSAFE_BPS_WINDOW_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_BPS_WINDOW_SECONDS.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("s"))
                .add(new SliderSetting("Trigger Delay", 0, 5,
                        () -> TfmConfig.FAILSAFE_BPS_TRIGGER_DELAY_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_BPS_TRIGGER_DELAY_SECONDS.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("s"));

        return MainGUIRegistry.toggleSubTab(
                "BPS",
                "Triggers when block breaks per second fall below the configured threshold",
                () -> TfmConfig.FAILSAFE_BPS.get(),
                v -> {
                    TfmConfig.FAILSAFE_BPS.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
