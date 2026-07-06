package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.ListSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.ArrayList;
import java.util.List;

public final class NickHiderVisualsRegistryProvider extends AbstractVisualsRegistryProvider {
    public NickHiderVisualsRegistryProvider() {
        super(1);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();

        groups.add(SettingGroup.alwaysOn(
                        "Username",
                        "Spoofs your username in chat and overlays")
                .add(new ToggleSetting("Enable Username Spoof",
                        () -> TfmConfig.NICK_HIDER_ENABLED.get(),
                        v -> {
                            TfmConfig.NICK_HIDER_ENABLED.set(v);
                            TfmConfig.save();
                        }))
                .add(new TextSetting("Custom Username", "TfmUser",
                        () -> TfmConfig.CUSTOM_USERNAME.get(),
                        v -> {
                            TfmConfig.CUSTOM_USERNAME.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.NICK_HIDER_ENABLED.get()))
                .add(new ToggleSetting("Hide Skin",
                        () -> TfmConfig.HIDE_SKIN.get(),
                        v -> {
                            TfmConfig.HIDE_SKIN.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.NICK_HIDER_ENABLED.get())));

        groups.add(SettingGroup.of(
                        "Hide Server ID",
                        "Replaces the server identifier shown in tablist and scoreboard",
                        () -> TfmConfig.HIDE_SERVER_ID.get(),
                        v -> {
                            TfmConfig.HIDE_SERVER_ID.set(v);
                            TfmConfig.save();
                        })
                .add(new TextSetting("Custom Server ID", "typicalfarmingmacro.cat",
                        () -> TfmConfig.CUSTOM_SERVER_ID.get(),
                        v -> {
                            TfmConfig.CUSTOM_SERVER_ID.set(v);
                            TfmConfig.save();
                        })));

        groups.add(SettingGroup.of(
                        "Coop Name Hider",
                        "Obfuscates configured coop names",
                        () -> TfmConfig.COOP_HIDER_ENABLED.get(),
                        v -> {
                            TfmConfig.COOP_HIDER_ENABLED.set(v);
                            TfmConfig.save();
                        })
                .add(new ListSetting("Coop Names", "Add coop name",
                        () -> TfmConfig.COOP_NAMES.get(),
                        v -> {
                            TfmConfig.COOP_NAMES.set(v);
                            TfmConfig.save();
                        })));

        SettingGroup spoofValues = SettingGroup.of(
                "Spoof Values",
                "Customise SkyBlock level and identifiable values",
                () -> TfmConfig.SPOOF_VALUES_ENABLED.get(),
                v -> {
                    TfmConfig.SPOOF_VALUES_ENABLED.set(v);
                    TfmConfig.save();
                });
        spoofValues.add(new ToggleSetting("Custom Skyblock Level",
                () -> TfmConfig.CUSTOM_SB_LEVEL_ENABLED.get(),
                v -> {
                    TfmConfig.CUSTOM_SB_LEVEL_ENABLED.set(v);
                    TfmConfig.save();
                }).visibleWhen(() -> TfmConfig.SPOOF_VALUES_ENABLED.get()));
        spoofValues.add(new TextSetting("SkyBlock Level Override", "0",
                () -> Integer.toString(TfmConfig.CUSTOM_SB_LEVEL.get()),
                v -> saveIntValue(v, TfmConfig.CUSTOM_SB_LEVEL::set))
                .visibleWhen(() -> TfmConfig.SPOOF_VALUES_ENABLED.get() && TfmConfig.CUSTOM_SB_LEVEL_ENABLED.get()));
        spoofValues.add(new TextSetting("Purse Offset", "0",
                () -> formatDecimal(TfmConfig.PURSE_OFFSET.get()),
                v -> saveDoubleValue(v, TfmConfig.PURSE_OFFSET::set))
                .visibleWhen(() -> TfmConfig.SPOOF_VALUES_ENABLED.get()));
        spoofValues.add(new TextSetting("Bits Offset", "0",
                () -> formatDecimal(TfmConfig.BITS_OFFSET.get()),
                v -> saveDoubleValue(v, TfmConfig.BITS_OFFSET::set))
                .visibleWhen(() -> TfmConfig.SPOOF_VALUES_ENABLED.get()));
        spoofValues.add(new TextSetting("Copper Offset", "0",
                () -> formatDecimal(TfmConfig.COPPER_OFFSET.get()),
                v -> saveDoubleValue(v, TfmConfig.COPPER_OFFSET::set))
                .visibleWhen(() -> TfmConfig.SPOOF_VALUES_ENABLED.get()));
        spoofValues.add(new TextSetting("Sawdust Offset", "0",
                () -> formatDecimal(TfmConfig.SAWDUST_OFFSET.get()),
                v -> saveDoubleValue(v, TfmConfig.SAWDUST_OFFSET::set))
                .visibleWhen(() -> TfmConfig.SPOOF_VALUES_ENABLED.get()));
        spoofValues.add(new TextSetting("Farming XP Offset", "0",
                () -> formatDecimal(TfmConfig.FARMING_EXP_OFFSET.get()),
                v -> saveDoubleValue(v, TfmConfig.FARMING_EXP_OFFSET::set))
                .visibleWhen(() -> TfmConfig.SPOOF_VALUES_ENABLED.get()));
        groups.add(spoofValues);

        return MainGUIRegistry.toggleSubTab(
                "Nick Hider",
                "Spoofs names, server IDs, coop names, and identifiable values",
                () -> TfmConfig.NICK_HIDER_MASTER_ENABLED.get(),
                v -> {
                    TfmConfig.NICK_HIDER_MASTER_ENABLED.set(v);
                    TfmConfig.save();
                },
                groups);
    }

    private static void saveIntValue(String value, java.util.function.IntConsumer setter) {
        try {
            setter.accept(Integer.parseInt(value.trim()));
            TfmConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private static void saveDoubleValue(String value, java.util.function.Consumer<Double> setter) {
        try {
            setter.accept(Double.parseDouble(value.trim()));
            TfmConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private static String formatDecimal(double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }
}
