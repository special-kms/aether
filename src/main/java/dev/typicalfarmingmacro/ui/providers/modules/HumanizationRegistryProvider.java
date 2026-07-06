package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.config.HumanizationPresetManager;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.ArrayList;
import java.util.List;

public final class HumanizationRegistryProvider extends AbstractModulesRegistryProvider {
    public HumanizationRegistryProvider() {
        super(10);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();

        groups.add(SettingGroup.alwaysOn(
                        "Presets",
                        "Apply editable humanization preset JSON files")
                .add(new DropdownSetting("Preset", HumanizationPresetManager.getPresetOptions(),
                        HumanizationPresetManager::getSelectedPresetIndex,
                        HumanizationPresetManager::applyPresetByIndex)
                        .addIconAction("/assets/typicalfarmingmacro/icons/folder.svg", HumanizationPresetManager::openPresetFolder)
                        .addIconAction("/assets/typicalfarmingmacro/icons/refresh.svg", HumanizationPresetManager::reapplySelectedPreset)));

        groups.add(SettingGroup.alwaysOn(
                        "Delays",
                        "Timing controls for macro and gear actions")
                .add(FarmingSettingsFactory.laneSwitchDelaySetting())
                .add(FarmingSettingsFactory.pestDestroyerTriggerDelaySetting())
                .add(FarmingSettingsFactory.pestExchangeDelaySetting())
                .add(FarmingSettingsFactory.aotvBetweenPestsDelaySetting())
                .add(FarmingSettingsFactory.rodSwapDelaySetting())
                .add(FarmingSettingsFactory.guiFirstClickDelaySetting())
                .add(FarmingSettingsFactory.guiClickDelaySetting())
                .add(FarmingSettingsFactory.pickUpStashDelaySetting())
                .add(FarmingSettingsFactory.junkDropDelaySetting())
                .add(FarmingSettingsFactory.georgePostSellDelaySetting())
                .add(FarmingSettingsFactory.bazaarGuiDelaySetting()));

        groups.add(SettingGroup.alwaysOn(
                        "Rotation Settings",
                        "Configure how your character rotates while macroing")
                .add(new SliderSetting("Rotation Time", 100, 2000,
                        () -> (float) TfmConfig.ROTATION_TIME.get(),
                        v -> {
                            TfmConfig.ROTATION_TIME.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("ms"))
                .add(new SliderSetting("Dynamic Duration", 0, 20,
                        () -> TfmConfig.ROTATION_DYNAMIC_DURATION_MS_PER_DEGREE.get(),
                        v -> {
                            TfmConfig.ROTATION_DYNAMIC_DURATION_MS_PER_DEGREE.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix(" ms/\u00B0"))
                .add(new ToggleSetting("Rotation Ease In",
                        () -> TfmConfig.ROTATION_EASE_IN.get(),
                        v -> {
                            TfmConfig.ROTATION_EASE_IN.set(v);
                            TfmConfig.save();
                        }))
                .add(new SliderSetting("Ease In Factor", 1, 5,
                        () -> TfmConfig.ROTATION_EASE_IN_FACTOR.get(),
                        v -> {
                            TfmConfig.ROTATION_EASE_IN_FACTOR.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1)
                        .visibleWhen(() -> TfmConfig.ROTATION_EASE_IN.get()))
                .add(new ToggleSetting("Rotation Ease Out",
                        () -> TfmConfig.ROTATION_EASE_OUT.get(),
                        v -> {
                            TfmConfig.ROTATION_EASE_OUT.set(v);
                            TfmConfig.save();
                        }))
                .add(new SliderSetting("Ease Out Factor", 1, 5,
                        () -> TfmConfig.ROTATION_EASE_OUT_FACTOR.get(),
                        v -> {
                            TfmConfig.ROTATION_EASE_OUT_FACTOR.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1)
                        .visibleWhen(() -> TfmConfig.ROTATION_EASE_OUT.get()))
                .add(new SliderSetting("Tracking Noise Min", 0, 10,
                        () -> TfmConfig.ROTATION_TRACKING_NOISE_MIN.get(),
                        v -> {
                            TfmConfig.ROTATION_TRACKING_NOISE_MIN.set(v);
                            if (TfmConfig.ROTATION_TRACKING_NOISE_MAX.get() < v) {
                                TfmConfig.ROTATION_TRACKING_NOISE_MAX.set(v);
                            }
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("%"))
                .add(new SliderSetting("Tracking Noise Max", 0, 10,
                        () -> TfmConfig.ROTATION_TRACKING_NOISE_MAX.get(),
                        v -> {
                            TfmConfig.ROTATION_TRACKING_NOISE_MAX.set(v);
                            if (TfmConfig.ROTATION_TRACKING_NOISE_MIN.get() > v) {
                                TfmConfig.ROTATION_TRACKING_NOISE_MIN.set(v);
                            }
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("%")));

        groups.add(SettingGroup.alwaysOn(
                        "FOV/Rotation",
                        "Angle ranges and target tolerance settings")
                .add(FarmingSettingsFactory.farmingPitchRangeSetting())
                .add(FarmingSettingsFactory.farmingYawRangeSetting())
                .add(FarmingSettingsFactory.aotvToRoofPitchRangeSetting())
                .add(FarmingSettingsFactory.pestFovRangeSetting())
                .add(FarmingSettingsFactory.pestAboveAimPitchRangeSetting())
                .add(FarmingSettingsFactory.visitorFovRangeSetting())
                .add(FarmingSettingsFactory.pestExchangeFovRangeSetting()));

        groups.add(SettingGroup.alwaysOn(
                        "Miscellaneous",
                        "Additional behavior toggles for humanization-sensitive actions")
                .add(FarmingSettingsFactory.farmWhileCallingGeorgeSetting()));

        return MainGUIRegistry.subTab(
                "Humanization",
                "Humanization controls for pitch, yaw, and easing",
                groups);
    }
}
