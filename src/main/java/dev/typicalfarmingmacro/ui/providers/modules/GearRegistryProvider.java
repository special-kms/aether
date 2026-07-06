package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.ArrayList;
import java.util.List;

public final class GearRegistryProvider extends AbstractModulesRegistryProvider {
    public GearRegistryProvider() {
        super(3);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();

        groups.add(SettingGroup.of(
                        "Auto Wardrobe",
                        "Handles wardrobe swaps for pests and visitors",
                        GearRegistryProvider::isAutoWardrobeEnabled,
                        GearRegistryProvider::setAutoWardrobeEnabled)
                .add(new ToggleSetting("Auto Wardrobe Pest",
                        () -> TfmConfig.AUTO_WARDROBE_PEST.get(),
                        v -> {
                            TfmConfig.AUTO_WARDROBE_PEST.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Auto Wardrobe Visitor",
                        () -> TfmConfig.AUTO_WARDROBE_VISITOR.get(),
                        v -> {
                            TfmConfig.AUTO_WARDROBE_VISITOR.set(v);
                            TfmConfig.save();
                        }))
                .add(new SliderSetting("Farming Wardrobe Slot", 1, 9,
                        () -> (float) TfmConfig.WARDROBE_SLOT_FARMING.get(),
                        v -> {
                            TfmConfig.WARDROBE_SLOT_FARMING.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)
                        .visibleWhen(() -> TfmConfig.AUTO_WARDROBE_PEST.get()
                                || TfmConfig.AUTO_WARDROBE_VISITOR.get()))
                .add(new SliderSetting("Pest Wardrobe Slot", 1, 9,
                        () -> (float) TfmConfig.WARDROBE_SLOT_PEST.get(),
                        v -> {
                            TfmConfig.WARDROBE_SLOT_PEST.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)
                        .visibleWhen(() -> TfmConfig.AUTO_WARDROBE_PEST.get()))
                .add(new SliderSetting("Visitor Wardrobe Slot", 1, 9,
                        () -> (float) TfmConfig.WARDROBE_SLOT_VISITOR.get(),
                        v -> {
                            TfmConfig.WARDROBE_SLOT_VISITOR.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)
                        .visibleWhen(() -> TfmConfig.AUTO_WARDROBE_VISITOR.get())));

        groups.add(SettingGroup.of(
                        "Auto Equipment",
                        "Swaps equipment automatically for pests and visitors",
                        GearRegistryProvider::isAutoEquipmentEnabled,
                        GearRegistryProvider::setAutoEquipmentEnabled)
                .add(new ToggleSetting("Auto Equipment Pest",
                        () -> TfmConfig.AUTO_EQUIPMENT_PEST.get(),
                        v -> {
                            TfmConfig.AUTO_EQUIPMENT_PEST.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Auto Equipment Visitor",
                        () -> TfmConfig.AUTO_EQUIPMENT_VISITOR.get(),
                        v -> {
                            TfmConfig.AUTO_EQUIPMENT_VISITOR.set(v);
                            TfmConfig.save();
                        })));

        groups.add(SettingGroup.of(
                        "Auto Rod",
                        "Handles rod swaps for pest cooldowns and returns",
                        GearRegistryProvider::isAutoRodEnabled,
                        GearRegistryProvider::setAutoRodEnabled)
                .add(new ToggleSetting("Auto Rod Pest CD",
                        () -> TfmConfig.AUTO_ROD_PEST_CD.get(),
                        v -> {
                            TfmConfig.AUTO_ROD_PEST_CD.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Auto Rod Pest Spawn",
                        () -> TfmConfig.AUTO_ROD_PEST_SPAWN.get(),
                        v -> {
                            TfmConfig.AUTO_ROD_PEST_SPAWN.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Auto Rod Return to Farm",
                        () -> TfmConfig.AUTO_ROD_RETURN_TO_FARM.get(),
                        v -> {
                            TfmConfig.AUTO_ROD_RETURN_TO_FARM.set(v);
                            TfmConfig.save();
                        }))
                .add(FarmingSettingsFactory.rodSwapDelaySetting()
                        .visibleWhen(() -> TfmConfig.AUTO_ROD_PEST_CD.get()
                                || TfmConfig.AUTO_ROD_PEST_SPAWN.get())));

        groups.add(SettingGroup.of(
                        "Budget Autopet",
                        "Equips pets from /pets for pest cooldowns, pest spawns, and farm returns",
                        GearRegistryProvider::isBudgetAutopetEnabled,
                        GearRegistryProvider::setBudgetAutopetEnabled)
                .add(new ToggleSetting("Autopet Pest CD",
                        () -> TfmConfig.AUTO_PET_PEST_CD.get(),
                        v -> {
                            TfmConfig.AUTO_PET_PEST_CD.set(v);
                            TfmConfig.save();
                        }))
                .add(new TextSetting("Autopet Pest CD Pet", "e.g Mosquito",
                        () -> TfmConfig.AUTO_PET_PEST_CD_PET.get(),
                        v -> {
                            TfmConfig.AUTO_PET_PEST_CD_PET.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_PET_PEST_CD.get()))
                .add(new ToggleSetting("Autopet Pest Spawn",
                        () -> TfmConfig.AUTO_PET_PEST_SPAWN.get(),
                        v -> {
                            TfmConfig.AUTO_PET_PEST_SPAWN.set(v);
                            TfmConfig.save();
                        }))
                .add(new TextSetting("Autopet Pest Spawn Pet", "e.g Hedgehog",
                        () -> TfmConfig.AUTO_PET_PEST_SPAWN_PET.get(),
                        v -> {
                            TfmConfig.AUTO_PET_PEST_SPAWN_PET.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_PET_PEST_SPAWN.get()))
                .add(new ToggleSetting("Autopet Return to Farm",
                        () -> TfmConfig.AUTO_PET_RETURN_TO_FARM.get(),
                        v -> {
                            TfmConfig.AUTO_PET_RETURN_TO_FARM.set(v);
                            TfmConfig.save();
                        }))
                .add(new TextSetting("Autopet Return to Farm Pet", "e.g Rose Dragon",
                        () -> TfmConfig.AUTO_PET_RETURN_TO_FARM_PET.get(),
                        v -> {
                            TfmConfig.AUTO_PET_RETURN_TO_FARM_PET.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_PET_RETURN_TO_FARM.get())));

        return MainGUIRegistry.toggleSubTab(
                "Gear Manager",
                "Automatically swaps equipment, wardrobe, and rod",
                GearRegistryProvider::isGearManagerEnabled,
                GearRegistryProvider::setGearManagerEnabled,
                groups);
    }

    private static boolean isAutoWardrobeEnabled() {
        return TfmConfig.AUTO_WARDROBE_PEST.get() || TfmConfig.AUTO_WARDROBE_VISITOR.get();
    }

    private static void setAutoWardrobeEnabled(boolean enabled) {
        TfmConfig.AUTO_WARDROBE_PEST.set(enabled);
        TfmConfig.AUTO_WARDROBE_VISITOR.set(enabled);
        TfmConfig.save();
    }

    private static boolean isAutoRodEnabled() {
        return TfmConfig.AUTO_ROD_PEST_CD.get()
                || TfmConfig.AUTO_ROD_PEST_SPAWN.get()
                || TfmConfig.AUTO_ROD_RETURN_TO_FARM.get();
    }

    private static void setAutoRodEnabled(boolean enabled) {
        TfmConfig.AUTO_ROD_PEST_CD.set(enabled);
        TfmConfig.AUTO_ROD_PEST_SPAWN.set(enabled);
        TfmConfig.AUTO_ROD_RETURN_TO_FARM.set(enabled);
        TfmConfig.save();
    }

    private static boolean isBudgetAutopetEnabled() {
        return TfmConfig.AUTO_PET_PEST_CD.get()
                || TfmConfig.AUTO_PET_PEST_SPAWN.get()
                || TfmConfig.AUTO_PET_RETURN_TO_FARM.get();
    }

    private static void setBudgetAutopetEnabled(boolean enabled) {
        TfmConfig.AUTO_PET_PEST_CD.set(enabled);
        TfmConfig.AUTO_PET_PEST_SPAWN.set(enabled);
        TfmConfig.AUTO_PET_RETURN_TO_FARM.set(enabled);
        TfmConfig.save();
    }

    private static boolean isAutoEquipmentEnabled() {
        return TfmConfig.AUTO_EQUIPMENT_PEST.get() || TfmConfig.AUTO_EQUIPMENT_VISITOR.get();
    }

    private static void setAutoEquipmentEnabled(boolean enabled) {
        TfmConfig.AUTO_EQUIPMENT_PEST.set(enabled);
        TfmConfig.AUTO_EQUIPMENT_VISITOR.set(enabled);
        TfmConfig.save();
    }

    private static boolean isGearManagerEnabled() {
        return TfmConfig.AUTO_EQUIPMENT_PEST.get()
                || TfmConfig.AUTO_EQUIPMENT_VISITOR.get()
                || TfmConfig.AUTO_WARDROBE_PEST.get()
                || TfmConfig.AUTO_WARDROBE_VISITOR.get()
                || TfmConfig.AUTO_ROD_PEST_CD.get()
                || TfmConfig.AUTO_ROD_PEST_SPAWN.get()
                || TfmConfig.AUTO_ROD_RETURN_TO_FARM.get()
                || TfmConfig.AUTO_PET_PEST_CD.get()
                || TfmConfig.AUTO_PET_PEST_SPAWN.get()
                || TfmConfig.AUTO_PET_RETURN_TO_FARM.get();
    }

    private static void setGearManagerEnabled(boolean enabled) {
        TfmConfig.AUTO_EQUIPMENT_PEST.set(enabled);
        TfmConfig.AUTO_EQUIPMENT_VISITOR.set(enabled);
        TfmConfig.AUTO_WARDROBE_PEST.set(enabled);
        TfmConfig.AUTO_WARDROBE_VISITOR.set(enabled);
        TfmConfig.AUTO_ROD_PEST_CD.set(enabled);
        TfmConfig.AUTO_ROD_PEST_SPAWN.set(enabled);
        TfmConfig.AUTO_ROD_RETURN_TO_FARM.set(enabled);
        TfmConfig.AUTO_PET_PEST_CD.set(enabled);
        TfmConfig.AUTO_PET_PEST_SPAWN.set(enabled);
        TfmConfig.AUTO_PET_RETURN_TO_FARM.set(enabled);
        TfmConfig.save();
    }
}
