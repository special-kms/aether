package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeCustomReplayManager.FailsafeReplayType;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.List;

public final class RotationFailsafeRegistryProvider extends AbstractFailsafesRegistryProvider {
    public RotationFailsafeRegistryProvider() {
        super(6);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup rotationGroup = SettingGroup.alwaysOn(
                        "Rotation Failsafe",
                        "Triggers when player rotation deviates beyond the configured thresholds")
                .add(FailsafeActionSettings.createActionDropdown("Rotation Action",
                        () -> TfmConfig.FAILSAFE_ROTATION_ACTION.get(),
                        value -> TfmConfig.FAILSAFE_ROTATION_ACTION.set(value)))
                .add(FailsafeActionSettings.createCustomReplayDropdown(FailsafeReplayType.ROTATION,
                        () -> TfmConfig.FAILSAFE_ROTATION_ACTION.get()))
                .add(new SliderSetting("Pitch Threshold", 5, 30,
                        () -> (float) TfmConfig.FAILSAFE_ROTATION_PITCH_THRESHOLD.get(),
                        v -> {
                            TfmConfig.FAILSAFE_ROTATION_PITCH_THRESHOLD.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("\u00B0"))
                .add(new SliderSetting("Yaw Threshold", 5, 30,
                        () -> (float) TfmConfig.FAILSAFE_ROTATION_YAW_THRESHOLD.get(),
                        v -> {
                            TfmConfig.FAILSAFE_ROTATION_YAW_THRESHOLD.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("\u00B0"))
                .add(new SliderSetting("Trigger Delay", 0, 5,
                        () -> TfmConfig.FAILSAFE_ROTATION_TRIGGER_DELAY_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_ROTATION_TRIGGER_DELAY_SECONDS.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("s"))
                .add(new SliderSetting("Warp Grace Period", 1000, 5000,
                        () -> (float) TfmConfig.FAILSAFE_ROTATION_WARP_GRACE_MS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_ROTATION_WARP_GRACE_MS.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("ms"));

        SettingGroup pestRotationGroup = SettingGroup.alwaysOn(
                        "Pest Cleaner Rotation",
                        "Controls how rotation failsafes behave while the pest cleaner is rotating")
                .add(new ToggleSetting("Trigger During Pest Cleaner",
                        () -> TfmConfig.FAILSAFE_ROTATION_TRIGGER_DURING_PEST_CLEANER.get(),
                        v -> {
                            TfmConfig.FAILSAFE_ROTATION_TRIGGER_DURING_PEST_CLEANER.set(v);
                            TfmConfig.save();
                        }))
                .add(FailsafeActionSettings.createActionDropdown("Pest Rotation Action",
                        () -> TfmConfig.FAILSAFE_PEST_ROTATION_ACTION.get(),
                        value -> TfmConfig.FAILSAFE_PEST_ROTATION_ACTION.set(value)))
                .add(FailsafeActionSettings.createCustomReplayDropdown(FailsafeReplayType.PEST_ROTATION,
                        () -> TfmConfig.FAILSAFE_PEST_ROTATION_ACTION.get()))
                .add(new SliderSetting("Pest Cleaner Rotation Failsafe Delay", 0, 5000,
                        () -> (float) TfmConfig.FAILSAFE_ROTATION_PEST_CLEANER_DELAY_MS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_ROTATION_PEST_CLEANER_DELAY_MS.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("ms")
                        .visibleWhen(() -> TfmConfig.FAILSAFE_ROTATION_TRIGGER_DURING_PEST_CLEANER.get()));

        return MainGUIRegistry.toggleSubTab(
                "Rotation",
                "Triggers when player rotation deviates beyond the configured thresholds",
                () -> TfmConfig.FAILSAFE_ROTATION.get(),
                v -> {
                    TfmConfig.FAILSAFE_ROTATION.set(v);
                    TfmConfig.save();
                },
                List.of(rotationGroup, pestRotationGroup));
    }
}
