package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.ListSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.ArrayList;
import java.util.List;

public final class PestManagerRegistryProvider extends AbstractModulesRegistryProvider {
    public PestManagerRegistryProvider() {
        super(2);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<String> sprayMaterials = FarmingSettingsFactory.sprayMaterials();
        List<SettingGroup> groups = new ArrayList<>();

        groups.add(SettingGroup.of(
                        "Pest Destroyer",
                        "Cleans pests once past the threshold",
                        () -> TfmConfig.TRIGGER_PEST_ON_CHAT.get(),
                        v -> {
                            TfmConfig.TRIGGER_PEST_ON_CHAT.set(v);
                            TfmConfig.save();
                        })
                .add(new SliderSetting("Pest Threshold", 1, 8,
                        () -> (float) TfmConfig.PEST_THRESHOLD.get(),
                        v -> {
                            TfmConfig.PEST_THRESHOLD.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0))
                .add(new ToggleSetting("Skip while Crop Fever Active",
                        () -> TfmConfig.DELAY_PEST_FOR_CROP_FEVER.get(),
                        v -> {
                            TfmConfig.DELAY_PEST_FOR_CROP_FEVER.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Trigger Only After Rewarp",
                        () -> TfmConfig.PEST_TRIGGER_ONLY_AFTER_REWARP.get(),
                        v -> {
                            TfmConfig.PEST_TRIGGER_ONLY_AFTER_REWARP.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Plot TP for Current Plot",
                        () -> TfmConfig.PEST_PLOT_TP_FOR_CURRENT_PLOT.get(),
                        v -> {
                            TfmConfig.PEST_PLOT_TP_FOR_CURRENT_PLOT.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Leave One Pest Alive",
                        () -> TfmConfig.LEAVE_ONE_PEST_ALIVE.get(),
                        v -> {
                            TfmConfig.LEAVE_ONE_PEST_ALIVE.set(v);
                            TfmConfig.save();
                        }))
                .add(new ListSetting("Leave One Pest Plots", "Add plot number",
                        () -> TfmConfig.LEAVE_ONE_PEST_PLOTS.get(),
                        v -> {
                            TfmConfig.LEAVE_ONE_PEST_PLOTS.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.LEAVE_ONE_PEST_ALIVE.get()))
                .add(new ToggleSetting("AOTV Between Distant Pests",
                        () -> TfmConfig.PEST_AOTV_BETWEEN.get(),
                        v -> {
                            TfmConfig.PEST_AOTV_BETWEEN.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Confirm AOTV Between Pests",
                        () -> TfmConfig.PEST_AOTV_CONFIRM_BETWEEN.get(),
                        v -> {
                            TfmConfig.PEST_AOTV_CONFIRM_BETWEEN.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.PEST_AOTV_BETWEEN.get()))
                .add(FarmingSettingsFactory.aotvBetweenPestsDelaySetting()
                        .visibleWhen(() -> TfmConfig.PEST_AOTV_BETWEEN.get()))
                .add(FarmingSettingsFactory.pestFovRangeSetting())
                .add(FarmingSettingsFactory.pestAboveAimPitchRangeSetting()));

        groups.add(SettingGroup.of(
                        "Disco Destination",
                        "Prioritizes a selected plot and holds position for disco pests",
                        () -> TfmConfig.PEST_DISCO_DESTINATION_MODE.get(),
                        v -> {
                            TfmConfig.PEST_DISCO_DESTINATION_MODE.set(v);
                            TfmConfig.save();
                        })
                .add(new TextSetting("Disco Destination Plot", "Plot number (e.g. 5)",
                        () -> TfmConfig.PEST_DISCO_DESTINATION_PLOT.get(),
                        v -> {
                            TfmConfig.PEST_DISCO_DESTINATION_PLOT.set(v);
                            TfmConfig.save();
                        })));

        groups.add(SettingGroup.of(
                        "AOTV to Roof",
                        "Teleports to the roof before cleaning pests on selected plots",
                        () -> TfmConfig.AOTV_TO_ROOF.get(),
                        v -> {
                            TfmConfig.AOTV_TO_ROOF.set(v);
                            TfmConfig.save();
                        })
                .add(new ListSetting("AOTV Roof Plots", "Add plot number",
                        () -> TfmConfig.AOTV_ROOF_PLOTS.get(),
                        v -> {
                            TfmConfig.AOTV_ROOF_PLOTS.set(v);
                            TfmConfig.save();
                        }))
                .add(new SliderSetting("AOTV to Roof Pitch", 20, 90,
                        () -> (float) TfmConfig.AOTV_ROOF_PITCH.get(),
                        v -> {
                            TfmConfig.AOTV_ROOF_PITCH.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("\u00B0"))
                .add(FarmingSettingsFactory.aotvToRoofPitchRangeSetting())
                .add(new ToggleSetting("Break Blocks Before AOTV",
                        () -> TfmConfig.BREAK_BLOCKS_BEFORE_AOTV.get(),
                        v -> {
                            TfmConfig.BREAK_BLOCKS_BEFORE_AOTV.set(v);
                            TfmConfig.save();
                        })));

        groups.add(SettingGroup.of(
                        "Pest Traps",
                        "Clears and refills pest traps",
                        () -> TfmConfig.ENABLE_PEST_TRAPS.get(),
                        v -> {
                            TfmConfig.ENABLE_PEST_TRAPS.set(v);
                            TfmConfig.save();
                        })
                .add(new ToggleSetting("Clear Pest Traps",
                        () -> TfmConfig.AUTO_CLEAR_PEST_TRAPS.get(),
                        v -> {
                            TfmConfig.AUTO_CLEAR_PEST_TRAPS.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Pre-equip Mosquito for Pest Traps",
                        () -> TfmConfig.AUTO_MOSQUITO_FOR_PEST_TRAPS.get(),
                        v -> {
                            TfmConfig.AUTO_MOSQUITO_FOR_PEST_TRAPS.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_CLEAR_PEST_TRAPS.get()))
                .add(new ToggleSetting("Equip Pet After Trap Open",
                        () -> TfmConfig.AUTO_PET_AFTER_TRAP_OPEN.get(),
                        v -> {
                            TfmConfig.AUTO_PET_AFTER_TRAP_OPEN.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_CLEAR_PEST_TRAPS.get()))
                .add(new TextSetting("Trap Open Pet", "e.g Rose Dragon",
                        () -> TfmConfig.AUTO_PET_AFTER_TRAP_OPEN_PET.get(),
                        v -> {
                            TfmConfig.AUTO_PET_AFTER_TRAP_OPEN_PET.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_CLEAR_PEST_TRAPS.get()
                                && TfmConfig.AUTO_PET_AFTER_TRAP_OPEN.get()))
                .add(new ToggleSetting("Refill Pest Traps",
                        () -> TfmConfig.AUTO_REFILL_PEST_TRAPS.get(),
                        v -> {
                            TfmConfig.AUTO_REFILL_PEST_TRAPS.set(v);
                            TfmConfig.save();
                        }))
                .add(new DropdownSetting("Bait Material", sprayMaterials,
                        () -> {
                            String current = TfmConfig.PEST_TRAPS_BAIT_MATERIAL.get();
                            int idx = sprayMaterials.indexOf(current);
                            return idx >= 0 ? idx : 5;
                        },
                        i -> {
                            if (i >= 0 && i < sprayMaterials.size()) {
                                TfmConfig.PEST_TRAPS_BAIT_MATERIAL.set(sprayMaterials.get(i));
                                TfmConfig.save();
                            }
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_REFILL_PEST_TRAPS.get()))
                .add(new SliderSetting("Bait Amount", 1, 64,
                        () -> (float) TfmConfig.PEST_TRAPS_BAIT_AMOUNT.get(),
                        v -> {
                            TfmConfig.PEST_TRAPS_BAIT_AMOUNT.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)
                        .visibleWhen(() -> TfmConfig.AUTO_REFILL_PEST_TRAPS.get()))
                .add(new TextSetting("Pest Traps Plot", "Plot number (e.g. 5)",
                        () -> TfmConfig.PEST_TRAPS_PLOT.get(),
                        v -> {
                            TfmConfig.PEST_TRAPS_PLOT.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_CLEAR_PEST_TRAPS.get()
                                || TfmConfig.AUTO_REFILL_PEST_TRAPS.get())));

        return MainGUIRegistry.toggleSubTab(
                "Pest Manager",
                "Automatically cleans pests, and manage your pest traps",
                () -> TfmConfig.TRIGGER_PEST_ON_CHAT.get(),
                v -> {
                    TfmConfig.TRIGGER_PEST_ON_CHAT.set(v);
                    TfmConfig.save();
                },
                groups);
    }

}
