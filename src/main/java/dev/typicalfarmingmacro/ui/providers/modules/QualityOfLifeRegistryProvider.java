package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.ListSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.ArrayList;
import java.util.List;

public final class QualityOfLifeRegistryProvider extends AbstractModulesRegistryProvider {
    public QualityOfLifeRegistryProvider() {
        super(9);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();

        groups.add(SettingGroup.of(
                        "Book Combine",
                        "Automatically combines books when the threshold is met",
                        () -> TfmConfig.AUTO_BOOK_COMBINE.get(),
                        v -> {
                            TfmConfig.AUTO_BOOK_COMBINE.set(v);
                            TfmConfig.save();
                        })
                .add(new ToggleSetting("Always Active",
                        () -> TfmConfig.ALWAYS_ACTIVE_COMBINE.get(),
                        v -> {
                            TfmConfig.ALWAYS_ACTIVE_COMBINE.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_BOOK_COMBINE.get()))
                .add(new SliderSetting("Book Threshold", 1, 20,
                        () -> (float) TfmConfig.BOOK_THRESHOLD.get(),
                        v -> {
                            TfmConfig.BOOK_THRESHOLD.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)
                        .visibleWhen(() -> TfmConfig.AUTO_BOOK_COMBINE.get()))
                .add(new SliderSetting("Book Combine Delay", 50, 5000,
                        () -> (float) TfmConfig.BOOK_COMBINE_DELAY.get(),
                        v -> {
                            TfmConfig.BOOK_COMBINE_DELAY.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("ms")
                        .visibleWhen(() -> TfmConfig.AUTO_BOOK_COMBINE.get()))
                .add(new ListSetting("Custom Enchantment Levels", "Add Name:Level entry",
                        () -> TfmConfig.CUSTOM_ENCHANTMENT_LEVELS.get(),
                        v -> {
                            TfmConfig.CUSTOM_ENCHANTMENT_LEVELS.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_BOOK_COMBINE.get())));

        groups.add(SettingGroup.of(
                        "Auto George Sell",
                        "Automatically sells pets through George",
                        () -> TfmConfig.AUTO_GEORGE_SELL.get(),
                        v -> {
                            TfmConfig.AUTO_GEORGE_SELL.set(v);
                            TfmConfig.save();
                        })
                .add(new SliderSetting("George Sell Threshold", 1, 10,
                        () -> (float) TfmConfig.GEORGE_SELL_THRESHOLD.get(),
                        v -> {
                            TfmConfig.GEORGE_SELL_THRESHOLD.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)
                        .visibleWhen(() -> TfmConfig.AUTO_GEORGE_SELL.get()))
                .add(FarmingSettingsFactory.georgePostSellDelaySetting()
                        .visibleWhen(() -> TfmConfig.AUTO_GEORGE_SELL.get()))
                .add(FarmingSettingsFactory.farmWhileCallingGeorgeSetting()
                        .visibleWhen(() -> TfmConfig.AUTO_GEORGE_SELL.get())));

        groups.add(SettingGroup.of(
                        "Auto Sell",
                        "Automatically sells configured items",
                        QualityOfLifeRegistryProvider::isAutoSellCategoryEnabled,
                        QualityOfLifeRegistryProvider::setAutoSellCategoryEnabled)
                .add(new SliderSetting("Inventory Threshold", 1, 100,
                        () -> (float) TfmConfig.AUTO_SELL_THRESHOLD.get(),
                        v -> {
                            TfmConfig.AUTO_SELL_THRESHOLD.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("%")
                        .visibleWhen(() -> TfmConfig.AUTO_SELL.get()))
                .add(new SliderSetting("Inventory Full Time", 1, 30,
                        () -> (float) TfmConfig.AUTO_SELL_TIME.get(),
                        v -> {
                            TfmConfig.AUTO_SELL_TIME.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("s")
                        .visibleWhen(() -> TfmConfig.AUTO_SELL.get()))
                .add(new ToggleSetting("NPC Autosell",
                        () -> TfmConfig.AUTO_SELL_NPC.get(),
                        v -> {
                            TfmConfig.AUTO_SELL_NPC.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_SELL.get()))
                .add(new ToggleSetting("Bazaar Autosell",
                        () -> TfmConfig.AUTO_SELL_BAZAAR.get(),
                        v -> {
                            TfmConfig.AUTO_SELL_BAZAAR.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_SELL.get()))
                .add(new ToggleSetting("Sell Before Visitors",
                        () -> TfmConfig.AUTO_SELL_BEFORE_VISITORS.get(),
                        v -> {
                            TfmConfig.AUTO_SELL_BEFORE_VISITORS.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_SELL.get()))
                .add(new ToggleSetting("Sell Before Pest Traps",
                        () -> TfmConfig.AUTO_SELL_BEFORE_PEST_TRAPS.get(),
                        v -> {
                            TfmConfig.AUTO_SELL_BEFORE_PEST_TRAPS.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_SELL.get()))
                .add(new ToggleSetting("Auto Sell (Passive)",
                        () -> TfmConfig.AUTOSELL_PASSIVE.get(),
                        v -> {
                            TfmConfig.AUTOSELL_PASSIVE.set(v);
                            TfmConfig.save();
                        }))
                .add(new ListSetting("Auto Sell Items", "Add item name",
                        () -> TfmConfig.AUTO_SELL_ITEMS.get(),
                        v -> {
                            TfmConfig.AUTO_SELL_ITEMS.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_SELL.get() || TfmConfig.AUTOSELL_PASSIVE.get())));

        groups.add(SettingGroup.of(
                        "Stash Manager",
                        "Automatically picks items up from stash",
                        () -> TfmConfig.AUTO_STASH_MANAGER.get(),
                        v -> {
                            TfmConfig.AUTO_STASH_MANAGER.set(v);
                            TfmConfig.save();
                        })
                .add(FarmingSettingsFactory.pickUpStashDelaySetting()
                        .visibleWhen(() -> TfmConfig.AUTO_STASH_MANAGER.get())));

        groups.add(SettingGroup.of(
                        "Junk Manager",
                        "Drops configured junk items once the threshold is reached",
                        () -> TfmConfig.AUTO_DROP_JUNK.get(),
                        v -> {
                            TfmConfig.AUTO_DROP_JUNK.set(v);
                            TfmConfig.save();
                        })
                .add(new SliderSetting("Junk Threshold", 1, 10,
                        () -> (float) TfmConfig.JUNK_THRESHOLD.get(),
                        v -> {
                            TfmConfig.JUNK_THRESHOLD.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix(" items")
                        .visibleWhen(() -> TfmConfig.AUTO_DROP_JUNK.get()))
                .add(FarmingSettingsFactory.junkDropDelaySetting()
                        .visibleWhen(() -> TfmConfig.AUTO_DROP_JUNK.get()))
                .add(new TextSetting("Drop at Plot TP", "Plot number (e.g. 5)",
                        () -> TfmConfig.DROP_JUNK_PLOT_TP.get(),
                        v -> {
                            TfmConfig.DROP_JUNK_PLOT_TP.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_DROP_JUNK.get()))
                .add(new ListSetting("Junk Items", "Add item name",
                        () -> TfmConfig.JUNK_ITEMS.get(),
                        v -> {
                            TfmConfig.JUNK_ITEMS.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_DROP_JUNK.get())));

        groups.add(SettingGroup.alwaysOn(
                        "Chat Filtering",
                        "Controls chat cleanup for farming-related messages")
                .add(new ToggleSetting("Hide Pest Drops",
                        () -> TfmConfig.HIDE_FILTERED_CHAT.get(),
                        v -> {
                            TfmConfig.HIDE_FILTERED_CHAT.set(v);
                            TfmConfig.save();
                        })));

        return MainGUIRegistry.subTab(
                "Farming QOL",
                "Various quality-of-life features for farming",
                groups);
    }

    private static boolean isAutoSellCategoryEnabled() {
        return TfmConfig.AUTO_SELL.get() || TfmConfig.AUTOSELL_PASSIVE.get();
    }

    private static void setAutoSellCategoryEnabled(boolean enabled) {
        TfmConfig.AUTO_SELL.set(enabled);
        if (!enabled) {
            TfmConfig.AUTOSELL_PASSIVE.set(false);
        }
        TfmConfig.save();
    }
}
