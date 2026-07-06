package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.ActionSetting;
import dev.typicalfarmingmacro.ui.settings.ListSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class GreenhouseRegistryProvider extends AbstractModulesRegistryProvider {
    public GreenhouseRegistryProvider() {
        super(7);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "Greenhouse Settings",
                        "Configure Auto Greenhouse behavior")
                .add(new SliderSetting("Run Interval", 1, 1440,
                        () -> (float) TfmConfig.AUTO_GREENHOUSE_INTERVAL_MINUTES.get(),
                        v -> {
                            TfmConfig.AUTO_GREENHOUSE_INTERVAL_MINUTES.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix(" min"))
                .add(new ListSetting("Plots", "Add plot number",
                        () -> TfmConfig.GREENHOUSE_PLOTS.get(),
                        v -> {
                            TfmConfig.GREENHOUSE_PLOTS.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Equip Custom Item",
                        () -> TfmConfig.EQUIP_GREENHOUSE_CUSTOM_ITEM.get(),
                        v -> {
                            TfmConfig.EQUIP_GREENHOUSE_CUSTOM_ITEM.set(v);
                            TfmConfig.save();
                        }))
                .add(new TextSetting("Custom Item", "e.g. Nether Wart Hoe",
                        () -> TfmConfig.GREENHOUSE_CUSTOM_ITEM.get(),
                        v -> {
                            TfmConfig.GREENHOUSE_CUSTOM_ITEM.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.EQUIP_GREENHOUSE_CUSTOM_ITEM.get()))
                .add(new ToggleSetting("Harvest Ashwreath",
                        () -> TfmConfig.HARVEST_ASHWREATH.get(),
                        v -> {
                            TfmConfig.HARVEST_ASHWREATH.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Harvest Turtellini",
                        () -> TfmConfig.HARVEST_TURTELLINI.get(),
                        v -> {
                            TfmConfig.HARVEST_TURTELLINI.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Harvest Glasscorn",
                        () -> TfmConfig.HARVEST_GLASSCORN.get(),
                        v -> {
                            TfmConfig.HARVEST_GLASSCORN.set(v);
                            TfmConfig.save();
                        }))
                .add(new ActionSetting("Harvest Now",
                        () -> dev.typicalfarmingmacro.modules.GreenhouseManager.harvest(Minecraft.getInstance())));

        return MainGUIRegistry.toggleSubTab(
                "Auto Greenhouse",
                "Automatically harvests your greenhouse",
                () -> TfmConfig.AUTO_GREENHOUSE.get(),
                v -> {
                    TfmConfig.AUTO_GREENHOUSE.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
