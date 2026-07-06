package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.SupercraftManager;
import dev.typicalfarmingmacro.ui.settings.ActionSetting;
import dev.typicalfarmingmacro.ui.settings.ListSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class SupercraftRegistryProvider extends AbstractModulesRegistryProvider {
    public SupercraftRegistryProvider() {
        super(9);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "Supercraft Settings",
                        "Configure Auto Supercraft behavior")
                .add(new SliderSetting("Run Interval", 1, 1440,
                        () -> (float) TfmConfig.AUTO_SUPERCRAFT_INTERVAL_MINUTES.get(),
                        v -> {
                            TfmConfig.AUTO_SUPERCRAFT_INTERVAL_MINUTES.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix(" min"))
                .add(new ListSetting("Items", "Add item name",
                        () -> TfmConfig.AUTO_SUPERCRAFT_ITEMS.get(),
                        v -> {
                            TfmConfig.AUTO_SUPERCRAFT_ITEMS.set(v);
                            TfmConfig.save();
                        }))
                .add(new ActionSetting("Run Now",
                        () -> SupercraftManager.manualTrigger(Minecraft.getInstance())));

        return MainGUIRegistry.toggleSubTab(
                "Auto Supercraft",
                "Automatically crafts configured items with Supercraft",
                () -> TfmConfig.AUTO_SUPERCRAFT.get(),
                v -> {
                    TfmConfig.AUTO_SUPERCRAFT.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
