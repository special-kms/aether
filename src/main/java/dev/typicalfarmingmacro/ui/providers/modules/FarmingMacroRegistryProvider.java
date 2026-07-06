package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.config.FarmingMacroPresetManager;
import dev.typicalfarmingmacro.config.FarmType;
import dev.typicalfarmingmacro.ui.settings.ActionSetting;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;
import dev.typicalfarmingmacro.util.TfmLang;

import java.util.ArrayList;
import java.util.List;

public final class FarmingMacroRegistryProvider extends AbstractModulesRegistryProvider {
    public FarmingMacroRegistryProvider() {
        super(0);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();
        groups.add(SettingGroup.alwaysOn(
                        "Presets",
                        "Save and load Farming Macro preset JSON files")
                .add(new TextSetting("Preset Name", "e.g. Wheat",
                        () -> TfmConfig.FARMING_MACRO_PRESET_NAME.get(),
                        v -> {
                            TfmConfig.FARMING_MACRO_PRESET_NAME.set(v);
                            TfmConfig.save();
                        }))
                .add(new ActionSetting("Save Preset", FarmingMacroPresetManager::saveCurrentPreset))
                .add(new DropdownSetting("Preset", FarmingMacroPresetManager.getPresetOptions(),
                        FarmingMacroPresetManager::getSelectedPresetIndex,
                        FarmingMacroPresetManager::applyPresetByIndex)
                        .addIconAction("/assets/typicalfarmingmacro/icons/folder.svg", FarmingMacroPresetManager::openPresetFolder)
                        .addIconAction("/assets/typicalfarmingmacro/icons/refresh.svg", FarmingMacroPresetManager::refreshPresetOptions)));

        groups.add(SettingGroup.alwaysOn(
                        "Farm Macro Settings",
                        "Configure Farming Macro behavior")
                .add(new DropdownSetting("Farm Type",
                        java.util.stream.Stream.of(FarmType.values()).map(FarmType::getLabel).toList(),
                        () -> {
                            try {
                                return FarmType.valueOf(TfmConfig.FARM_TYPE.get()).ordinal();
                            } catch (Exception e) {
                                return 0;
                            }
                        },
                        i -> {
                            if (i >= 0 && i < FarmType.values().length) {
                                TfmConfig.FARM_TYPE.set(FarmType.values()[i].name());
                                TfmConfig.save();
                            }
                        }))
                .add(new ToggleSetting("Hold W While Farming",
                        () -> TfmConfig.MACRO_HOLD_W_WHILE_FARMING.get(),
                        v -> {
                            TfmConfig.MACRO_HOLD_W_WHILE_FARMING.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting(TfmLang.localize("Disable /setspawn"),
                        () -> TfmConfig.MACRO_DISABLE_SETSPAWN.get(),
                        v -> {
                            TfmConfig.MACRO_DISABLE_SETSPAWN.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Rotate on Drop",
                        () -> TfmConfig.MACRO_ROTATE_ON_DROP.get(),
                        v -> {
                            TfmConfig.MACRO_ROTATE_ON_DROP.set(v);
                            TfmConfig.save();
                        }))
                .add(new SliderSetting("Drop Rotation", -180, 180,
                        () -> (float) TfmConfig.MACRO_DROP_ROTATION_DEGREES.get(),
                        v -> {
                            TfmConfig.MACRO_DROP_ROTATION_DEGREES.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("\u00B0")
                        .visibleWhen(() -> TfmConfig.MACRO_ROTATE_ON_DROP.get()))
                .add(new ToggleSetting("Squeaky Mousemat",
                        () -> TfmConfig.SQUEAKY_MOUSEMAT.get(),
                        v -> {
                            TfmConfig.SQUEAKY_MOUSEMAT.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Custom Pitch",
                        () -> TfmConfig.MACRO_USE_CUSTOM_PITCH.get(),
                        v -> {
                            TfmConfig.MACRO_USE_CUSTOM_PITCH.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> !TfmConfig.SQUEAKY_MOUSEMAT.get()))
                .add(new SliderSetting("Pitch", -90, 90,
                        () -> TfmConfig.MACRO_CUSTOM_PITCH.get(),
                        v -> {
                            TfmConfig.MACRO_CUSTOM_PITCH.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("\u00B0")
                        .visibleWhen(() -> !TfmConfig.SQUEAKY_MOUSEMAT.get()
                                && TfmConfig.MACRO_USE_CUSTOM_PITCH.get()))
                .add(new ToggleSetting("Custom Yaw",
                        () -> TfmConfig.MACRO_USE_CUSTOM_YAW.get(),
                        v -> {
                            TfmConfig.MACRO_USE_CUSTOM_YAW.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> !TfmConfig.SQUEAKY_MOUSEMAT.get()))
                .add(new SliderSetting("Yaw", -180, 180,
                        () -> TfmConfig.MACRO_CUSTOM_YAW.get(),
                        v -> {
                            TfmConfig.MACRO_CUSTOM_YAW.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("\u00B0")
                        .visibleWhen(() -> !TfmConfig.SQUEAKY_MOUSEMAT.get()
                                && TfmConfig.MACRO_USE_CUSTOM_YAW.get()))
                .add(FarmingSettingsFactory.farmingPitchRangeSetting()
                        .visibleWhen(() -> !TfmConfig.SQUEAKY_MOUSEMAT.get()))
                .add(FarmingSettingsFactory.farmingYawRangeSetting()
                        .visibleWhen(() -> !TfmConfig.SQUEAKY_MOUSEMAT.get()))
                .add(FarmingSettingsFactory.bpsAverageWindowSetting()));

        groups.add(SettingGroup.of(
                        "Fast Lane Switch (Experimental)",
                        "Switches farming direction at configured plot lane boundaries",
                        () -> TfmConfig.MACRO_FAST_LANE_SWITCH.get(),
                        v -> {
                            TfmConfig.MACRO_FAST_LANE_SWITCH.set(v);
                            TfmConfig.save();
                        })
                .add(new DropdownSetting("Boundary Axis",
                        List.of("X", "Z"),
                        () -> "Z".equalsIgnoreCase(TfmConfig.MACRO_FAST_LANE_BOUNDARY_AXIS.get()) ? 1 : 0,
                        i -> {
                            TfmConfig.MACRO_FAST_LANE_BOUNDARY_AXIS.set(i == 1 ? "Z" : "X");
                            TfmConfig.save();
                        }))
                .add(new TextSetting("Left Boundary", "e.g. -48",
                        () -> String.valueOf(TfmConfig.MACRO_FAST_LANE_LEFT_BOUNDARY.get()),
                        v -> {
                            Integer parsed = parseBoundary(v);
                            if (parsed != null) {
                                TfmConfig.MACRO_FAST_LANE_LEFT_BOUNDARY.set(parsed);
                                TfmConfig.save();
                            }
                        }))
                .add(new TextSetting("Right Boundary", "e.g. 48 blocks",
                        () -> String.valueOf(TfmConfig.MACRO_FAST_LANE_RIGHT_BOUNDARY.get()),
                        v -> {
                            Integer parsed = parseBoundary(v);
                            if (parsed != null) {
                                TfmConfig.MACRO_FAST_LANE_RIGHT_BOUNDARY.set(parsed);
                                TfmConfig.save();
                            }
                        })));

        return MainGUIRegistry.subTab("Farming Macro", "Automatically farms crops", groups);
    }

    private static Integer parseBoundary(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
