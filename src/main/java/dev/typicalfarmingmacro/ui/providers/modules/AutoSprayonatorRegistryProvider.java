package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.List;

public final class AutoSprayonatorRegistryProvider extends AbstractModulesRegistryProvider {
    public AutoSprayonatorRegistryProvider() {
        super(5);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "Sprayonator Settings",
                        "Configure Auto Sprayonator behavior")
                .add(new ToggleSetting("Auto Buy Material",
                        () -> TfmConfig.AUTO_SPRAYONATOR_AUTO_BUY.get(),
                        v -> {
                            TfmConfig.AUTO_SPRAYONATOR_AUTO_BUY.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.AUTO_SPRAYONATOR.get()))
                .add(new SliderSetting("Auto Buy Amount", 1, 64,
                        () -> (float) TfmConfig.AUTO_SPRAYONATOR_AUTO_BUY_AMOUNT.get(),
                        v -> {
                            TfmConfig.AUTO_SPRAYONATOR_AUTO_BUY_AMOUNT.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)
                        .visibleWhen(() -> TfmConfig.AUTO_SPRAYONATOR.get()
                                && TfmConfig.AUTO_SPRAYONATOR_AUTO_BUY.get()))
                .add(new SliderSetting("Unsprayed Plot Detect Time", 5, 30,
                        () -> (float) TfmConfig.AUTO_SPRAYONATOR_DETECT_TIME.get(),
                        v -> {
                            TfmConfig.AUTO_SPRAYONATOR_DETECT_TIME.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("s")
                        .visibleWhen(() -> TfmConfig.AUTO_SPRAYONATOR.get()));

        return MainGUIRegistry.toggleSubTab(
                "Auto Sprayonator",
                "Automatically detects and sprays unsprayed plots",
                () -> TfmConfig.AUTO_SPRAYONATOR.get(),
                v -> {
                    TfmConfig.AUTO_SPRAYONATOR.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
