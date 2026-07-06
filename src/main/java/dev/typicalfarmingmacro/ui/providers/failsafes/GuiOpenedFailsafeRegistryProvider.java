package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeCustomReplayManager.FailsafeReplayType;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;

import java.util.List;

public final class GuiOpenedFailsafeRegistryProvider extends AbstractFailsafesRegistryProvider {
    public GuiOpenedFailsafeRegistryProvider() {
        super(3);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "GUI Opened",
                        "Triggers when an inventory GUI opens during farming or cleaning")
                .add(FailsafeActionSettings.createActionDropdown("Action",
                        () -> TfmConfig.FAILSAFE_UNEXPECTED_INVENTORY_GUI_ACTION.get(),
                        value -> TfmConfig.FAILSAFE_UNEXPECTED_INVENTORY_GUI_ACTION.set(value)))
                .add(FailsafeActionSettings.createCustomReplayDropdown(FailsafeReplayType.GUI_OPENED,
                        () -> TfmConfig.FAILSAFE_UNEXPECTED_INVENTORY_GUI_ACTION.get()))
                .add(new SliderSetting("Trigger Delay", 0.0f, 5.0f,
                        () -> TfmConfig.FAILSAFE_UNEXPECTED_INVENTORY_GUI_DELAY_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_UNEXPECTED_INVENTORY_GUI_DELAY_SECONDS.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("s"));

        return MainGUIRegistry.toggleSubTab(
                "GUI Opened",
                "Triggers when an inventory GUI opens during farming or cleaning",
                () -> TfmConfig.FAILSAFE_UNEXPECTED_INVENTORY_GUI.get(),
                v -> {
                    TfmConfig.FAILSAFE_UNEXPECTED_INVENTORY_GUI.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
