package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.config.PetInfo;
import dev.typicalfarmingmacro.config.PetRarity;
import dev.typicalfarmingmacro.modules.profit.ProfitManager;
import dev.typicalfarmingmacro.modules.profit.ProfitPriceSource;
import dev.typicalfarmingmacro.ui.settings.ActionSetting;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.ArrayList;
import java.util.List;

public final class ProfitCalculatorRegistryProvider extends AbstractVisualsRegistryProvider {
    private static final List<SettingGroup> PROFIT_GROUPS = new ArrayList<>();
    private static final List<SettingGroup> PET_TRACKER_GROUPS = new ArrayList<>();

    public ProfitCalculatorRegistryProvider() {
        super(0);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        return MainGUIRegistry.toggleSubTab(
                "Profit Tracker",
                "Tracks and displays farming earnings",
                () -> TfmConfig.PROFIT_HUD_ENABLED.get(),
                v -> {
                    TfmConfig.PROFIT_HUD_ENABLED.set(v);
                    TfmConfig.save();
                },
                buildGroups());
    }

    private static List<SettingGroup> buildGroups() {
        List<SettingGroup> profitGroups = PROFIT_GROUPS;
        profitGroups.clear();

        profitGroups.add(SettingGroup.of(
                        "Profit HUD",
                        "Displays session earnings",
                        () -> TfmConfig.PROFIT_HUD_ENABLED.get(),
                        v -> {
                            TfmConfig.PROFIT_HUD_ENABLED.set(v);
                            TfmConfig.save();
                        })
                .add(new ToggleSetting("Session Profit HUD",
                        () -> TfmConfig.SHOW_SESSION_PROFIT_HUD.get(),
                        v -> {
                            TfmConfig.SHOW_SESSION_PROFIT_HUD.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Lifetime HUD",
                        () -> TfmConfig.SHOW_LIFETIME_HUD.get(),
                        v -> {
                            TfmConfig.SHOW_LIFETIME_HUD.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Daily HUD",
                        () -> TfmConfig.SHOW_DAILY_HUD.get(),
                        v -> {
                            TfmConfig.SHOW_DAILY_HUD.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Compact Profit Calculator",
                        () -> TfmConfig.COMPACT_PROFIT_CALCULATOR.get(),
                        v -> {
                            TfmConfig.COMPACT_PROFIT_CALCULATOR.set(v);
                            TfmConfig.save();
                        }))
                .add(new DropdownSetting("Price Source",
                        java.util.Arrays.stream(ProfitPriceSource.values()).map(ProfitPriceSource::getLabel).toList(),
                        () -> ProfitPriceSource.fromConfig(TfmConfig.PROFIT_PRICE_SOURCE.get()).ordinal(),
                        i -> {
                            ProfitPriceSource[] values = ProfitPriceSource.values();
                            if (i < 0 || i >= values.length) {
                                return;
                            }
                            TfmConfig.PROFIT_PRICE_SOURCE.set(values[i].name());
                            TfmConfig.save();
                            ProfitManager.handlePriceSourceChanged();
                        }))
                .add(new ActionSetting("Reset Session",
                        dev.typicalfarmingmacro.macro.MacroStateManager::resetSession))
                .add(new ActionSetting("Reset Lifetime Profit", ProfitManager::resetLifetime)));

        profitGroups.add(SettingGroup.alwaysOn(
                        "Farming XP",
                        "Farming XP stats shown on the Session Profit HUD")
                .add(new ToggleSetting("Show Farming XP",
                        () -> TfmConfig.FARMING_XP_HUD.get(),
                        v -> {
                            TfmConfig.FARMING_XP_HUD.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Farming XP / hr",
                        () -> TfmConfig.FARMING_HUD_XP_RATE.get(),
                        v -> {
                            TfmConfig.FARMING_HUD_XP_RATE.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.FARMING_XP_HUD.get()))
                .add(new ToggleSetting("Time to Next Level",
                        () -> TfmConfig.FARMING_HUD_ETA_NEXT.get(),
                        v -> {
                            TfmConfig.FARMING_HUD_ETA_NEXT.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.FARMING_XP_HUD.get()))
                .add(new ToggleSetting("Time to Farming 60",
                        () -> TfmConfig.FARMING_HUD_ETA_MAX.get(),
                        v -> {
                            TfmConfig.FARMING_HUD_ETA_MAX.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.FARMING_XP_HUD.get())));

        rebuildPetTrackerGroups();
        return profitGroups;
    }

    private static String defaultPetTrackerEntry() {
        return "Rose Dragon:200:650000000:1250000000:LEGENDARY";
    }

    private static PetInfo getPetTrackerInfo(int index) {
        List<String> entries = TfmConfig.PET_TRACKER_LIST.get();
        if (entries.isEmpty()) return new PetInfo(defaultPetTrackerEntry());
        int safeIndex = Math.max(0, Math.min(index, entries.size() - 1));
        return new PetInfo(entries.get(safeIndex));
    }

    private static void updatePetTrackerInfo(int index, java.util.function.Consumer<PetInfo> updater) {
        List<String> entries = new ArrayList<>(TfmConfig.PET_TRACKER_LIST.get());
        while (entries.size() <= index) entries.add(defaultPetTrackerEntry());
        PetInfo info = new PetInfo(entries.get(index));
        updater.accept(info);
        entries.set(index, info.toString());
        TfmConfig.PET_TRACKER_LIST.set(entries);
        TfmConfig.save();
        ProfitManager.reloadConfiguredPetXpPrices();
    }

    private static void addPetTrackerEntry() {
        List<String> entries = new ArrayList<>(TfmConfig.PET_TRACKER_LIST.get());
        entries.add(defaultPetTrackerEntry());
        TfmConfig.PET_TRACKER_LIST.set(entries);
        TfmConfig.save();
        ProfitManager.reloadConfiguredPetXpPrices();
        rebuildPetTrackerGroups();
    }

    private static void removePetTrackerEntry(int index) {
        List<String> entries = new ArrayList<>(TfmConfig.PET_TRACKER_LIST.get());
        if (entries.size() <= 1 || index < 0 || index >= entries.size()) return;
        entries.remove(index);
        TfmConfig.PET_TRACKER_LIST.set(entries);
        TfmConfig.save();
        ProfitManager.reloadConfiguredPetXpPrices();
        rebuildPetTrackerGroups();
    }

    private static SettingGroup buildPetTrackerGroup(int petIndex, List<String> petLevelOptions,
                                                     List<String> petRarityOptions) {
        String groupName = petIndex == 0 ? "Pet XP Tracker" : "Pet XP Tracker " + (petIndex + 1);
        SettingGroup petXpTracker = SettingGroup.alwaysOn(
                groupName,
                "Configure user-defined Pet XP pricing");
        petXpTracker.add(new ActionSetting("Remove Pet", () -> removePetTrackerEntry(petIndex))
                .visibleWhen(() -> false));
        petXpTracker.add(new TextSetting("Pet Name", "e.g. Rose Dragon",
                () -> getPetTrackerInfo(petIndex).name,
                v -> updatePetTrackerInfo(petIndex, info -> info.name = v.trim())));
        petXpTracker.add(new DropdownSetting("Max Level", petLevelOptions,
                () -> getPetTrackerInfo(petIndex).maxLevel >= 200 ? 1 : 0,
                idx -> updatePetTrackerInfo(petIndex,
                        info -> info.maxLevel = idx == 1 ? 200 : 100)));
        petXpTracker.add(new TextSetting("Level 1 Price", "e.g. 650000000",
                () -> String.valueOf(getPetTrackerInfo(petIndex).level1Price),
                v -> updatePetTrackerInfo(petIndex, info -> {
                    try {
                        info.level1Price = Long.parseLong(v.replaceAll("[^\\d]", ""));
                    } catch (Exception ignored) {
                    }
                })));
        petXpTracker.add(new TextSetting("Max Level Price", "e.g. 1250000000",
                () -> String.valueOf(getPetTrackerInfo(petIndex).maxLevelPrice),
                v -> updatePetTrackerInfo(petIndex, info -> {
                    try {
                        info.maxLevelPrice = Long.parseLong(v.replaceAll("[^\\d]", ""));
                    } catch (Exception ignored) {
                    }
                })));
        petXpTracker.add(new DropdownSetting("Rarity", petRarityOptions,
                () -> getPetTrackerInfo(petIndex).rarity.ordinal(),
                idx -> updatePetTrackerInfo(petIndex,
                        info -> info.rarity = PetRarity.values()[idx])));
        petXpTracker.add(new ActionSetting("Add Pet", ProfitCalculatorRegistryProvider::addPetTrackerEntry)
                .visibleWhen(() -> false));
        return petXpTracker;
    }

    private static void rebuildPetTrackerGroups() {
        PROFIT_GROUPS.removeAll(PET_TRACKER_GROUPS);
        PET_TRACKER_GROUPS.clear();

        List<String> petLevelOptions = List.of("100", "200");
        List<String> petRarityOptions = java.util.stream.Stream.of(PetRarity.values())
                .map(Enum::name)
                .toList();
        int petTrackerCount = Math.max(1, TfmConfig.PET_TRACKER_LIST.get().size());
        for (int i = 0; i < petTrackerCount; i++) {
            SettingGroup group = buildPetTrackerGroup(i, petLevelOptions, petRarityOptions);
            PET_TRACKER_GROUPS.add(group);
        }
        PROFIT_GROUPS.addAll(PET_TRACKER_GROUPS);
    }
}
