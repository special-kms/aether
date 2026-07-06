package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.ComposterManager;
import dev.typicalfarmingmacro.notification.NotificationManager;
import dev.typicalfarmingmacro.ui.settings.ActionSetting;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.PositionSetting;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.util.TfmLang;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class ComposterRegistryProvider extends AbstractModulesRegistryProvider {
    public ComposterRegistryProvider() {
        super(8);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "Composter Settings",
                        "Configure Auto Composter behavior")
                .add(new PositionSetting("Composter Position",
                        () -> (double) TfmConfig.AUTO_COMPOSTER_X.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_X.set((int) Math.round(v));
                            TfmConfig.save();
                        },
                        () -> (double) TfmConfig.AUTO_COMPOSTER_Y.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_Y.set((int) Math.round(v));
                            TfmConfig.save();
                        },
                        () -> (double) TfmConfig.AUTO_COMPOSTER_Z.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_Z.set((int) Math.round(v));
                            TfmConfig.save();
                        },
                        () -> TfmConfig.AUTO_COMPOSTER_HIGHLIGHT.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_HIGHLIGHT.set(v);
                            TfmConfig.save();
                        },
                        () -> {
                            var player = Minecraft.getInstance().player;
                            if (player != null) {
                                TfmConfig.AUTO_COMPOSTER_X.set(player.getBlockX());
                                TfmConfig.AUTO_COMPOSTER_Y.set(player.getBlockY());
                                TfmConfig.AUTO_COMPOSTER_Z.set(player.getBlockZ());
                                TfmConfig.save();
                                NotificationManager.success(TfmLang.localize("Composter Position Set"),
                                        String.format("X: %d, Y: %d, Z: %d",
                                                TfmConfig.AUTO_COMPOSTER_X.get(),
                                                TfmConfig.AUTO_COMPOSTER_Y.get(),
                                                TfmConfig.AUTO_COMPOSTER_Z.get()));
                            }
                        }))
                .add(new SliderSetting("Run Interval", 1, 1440,
                        () -> (float) TfmConfig.AUTO_COMPOSTER_INTERVAL_MINUTES.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_INTERVAL_MINUTES.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix(" min"))
                .add(new DropdownSetting("Source Mode", List.of("Sacks", "Bazaar"),
                        ComposterManager::getSourceModeIndex,
                        ComposterSettingsBridge::setSourceModeIndex))
                .add(new SliderSetting("Minimum Purse", 0, 2000000000,
                        () -> (float) TfmConfig.AUTO_COMPOSTER_MIN_PURSE.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_MIN_PURSE.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix(" coins")
                        .visibleWhen(ComposterManager::isBazaarMode))
                .add(new TextSetting("Crop Material", "e.g. Box of Seeds",
                        () -> TfmConfig.AUTO_COMPOSTER_CROP_MATERIAL.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_CROP_MATERIAL.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(ComposterManager::isBazaarMode))
                .add(new SliderSetting("Crop Amount", 1, 2000000,
                        () -> (float) TfmConfig.AUTO_COMPOSTER_CROP_AMOUNT.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_CROP_AMOUNT.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)
                        .visibleWhen(ComposterManager::isBazaarMode))
                .add(new TextSetting("Fuel Material", "e.g. Volta",
                        () -> TfmConfig.AUTO_COMPOSTER_FUEL_MATERIAL.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_FUEL_MATERIAL.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(ComposterManager::isBazaarMode))
                .add(new SliderSetting("Fuel Amount", 1, 2000000,
                        () -> (float) TfmConfig.AUTO_COMPOSTER_FUEL_AMOUNT.get(),
                        v -> {
                            TfmConfig.AUTO_COMPOSTER_FUEL_AMOUNT.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)
                        .visibleWhen(ComposterManager::isBazaarMode))
                .add(new ActionSetting("Run Now",
                        () -> ComposterManager.manualTrigger(Minecraft.getInstance())));

        return MainGUIRegistry.toggleSubTab(
                "Auto Composter",
                "Automatically refills the Garden composter",
                () -> TfmConfig.AUTO_COMPOSTER.get(),
                v -> {
                    TfmConfig.AUTO_COMPOSTER.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
