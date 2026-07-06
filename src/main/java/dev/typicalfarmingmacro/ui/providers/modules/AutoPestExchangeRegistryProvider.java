package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.notification.NotificationManager;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.PositionSetting;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.util.TfmLang;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class AutoPestExchangeRegistryProvider extends AbstractModulesRegistryProvider {
    public AutoPestExchangeRegistryProvider() {
        super(7);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "Pest Exchange",
                        "Configure Auto Pest Exchange behavior")
                .add(new PositionSetting("Exchange Desk",
                        () -> (double) TfmConfig.PEST_EXCHANGE_DESK_X.get(),
                        v -> {
                            TfmConfig.PEST_EXCHANGE_DESK_X.set((int) Math.round(v));
                            TfmConfig.save();
                        },
                        () -> (double) TfmConfig.PEST_EXCHANGE_DESK_Y.get(),
                        v -> {
                            TfmConfig.PEST_EXCHANGE_DESK_Y.set((int) Math.round(v));
                            TfmConfig.save();
                        },
                        () -> (double) TfmConfig.PEST_EXCHANGE_DESK_Z.get(),
                        v -> {
                            TfmConfig.PEST_EXCHANGE_DESK_Z.set((int) Math.round(v));
                            TfmConfig.save();
                        },
                        () -> TfmConfig.PEST_HIGHLIGHT_DESK.get(),
                        v -> {
                            TfmConfig.PEST_HIGHLIGHT_DESK.set(v);
                            TfmConfig.save();
                        },
                        () -> {
                            var player = Minecraft.getInstance().player;
                            if (player != null) {
                                TfmConfig.PEST_EXCHANGE_DESK_X.set(player.getBlockX());
                                TfmConfig.PEST_EXCHANGE_DESK_Y.set(player.getBlockY());
                                TfmConfig.PEST_EXCHANGE_DESK_Z.set(player.getBlockZ());
                                TfmConfig.save();
                        NotificationManager.success(TfmLang.localize("Pest Exchange Desk Set"),
                                String.format("X: %d, Y: %d, Z: %d",
                                        TfmConfig.PEST_EXCHANGE_DESK_X.get(),
                                        TfmConfig.PEST_EXCHANGE_DESK_Y.get(),
                                                TfmConfig.PEST_EXCHANGE_DESK_Z.get()));
                            }
                        }))
                .add(FarmingSettingsFactory.pestExchangeFovRangeSetting()
                        .visibleWhen(() -> TfmConfig.AUTO_PEST_EXCHANGE.get()));

        group.add(new dev.typicalfarmingmacro.ui.settings.ToggleSetting("Use Abiphone",
                () -> TfmConfig.AUTO_PEST_USE_ABIPHONE.get(),
                v -> {
                    TfmConfig.AUTO_PEST_USE_ABIPHONE.set(v);
                    TfmConfig.save();
                })
                .visibleWhen(() -> TfmConfig.AUTO_PEST_EXCHANGE.get()));

        return MainGUIRegistry.toggleSubTab(
                "Auto Pest Exchange",
                "Automatically visits the pest exchange desk when ready",
                () -> TfmConfig.AUTO_PEST_EXCHANGE.get(),
                v -> {
                    TfmConfig.AUTO_PEST_EXCHANGE.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
