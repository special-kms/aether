package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeCustomReplayManager.FailsafeReplayType;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;

import java.util.List;

public final class InventoryFailsafeRegistryProvider extends AbstractFailsafesRegistryProvider {
    public InventoryFailsafeRegistryProvider() {
        super(2);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup slotGroup = SettingGroup.alwaysOn(
                        "Inventory Slot Changed",
                        "Triggers when the selected hotbar slot changes unexpectedly")
                .add(FailsafeActionSettings.createActionDropdown("Action",
                        () -> TfmConfig.FAILSAFE_INVENTORY_SLOT_CHANGED_ACTION.get(),
                        value -> TfmConfig.FAILSAFE_INVENTORY_SLOT_CHANGED_ACTION.set(value)))
                .add(FailsafeActionSettings.createCustomReplayDropdown(FailsafeReplayType.INVENTORY_SLOT,
                        () -> TfmConfig.FAILSAFE_INVENTORY_SLOT_CHANGED_ACTION.get()))
                .add(new SliderSetting("Trigger Delay", 0, 5,
                        () -> TfmConfig.FAILSAFE_INVENTORY_SLOT_CHANGED_DELAY_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_INVENTORY_SLOT_CHANGED_DELAY_SECONDS.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("s"));

        return MainGUIRegistry.toggleSubTab(
                "Inventory Slot Changed",
                "Triggers when the selected hotbar slot changes unexpectedly",
                () -> TfmConfig.FAILSAFE_INVENTORY_SLOT_CHANGED.get(),
                v -> {
                    TfmConfig.FAILSAFE_INVENTORY_SLOT_CHANGED.set(v);
                    TfmConfig.save();
                },
                List.of(slotGroup));
    }
}
