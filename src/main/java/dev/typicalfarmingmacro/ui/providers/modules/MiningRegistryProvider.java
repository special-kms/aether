package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.InfoSetting;
import dev.typicalfarmingmacro.ui.settings.ListSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;

import java.util.List;

public final class MiningRegistryProvider extends AbstractMiningRegistryProvider {
    public MiningRegistryProvider() {
        super(3);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup metalDetector = SettingGroup.alwaysOn(
                        "Metal Detector",
                        "Crystal Hollows metal detector automation and backpack filling")
                .add(new InfoSetting("Trigger",
                        () -> "Run /typicalfarmingmacro metaldetector to start or stop the solver. This menu only edits backpack options.")
                        .multiline())
                .add(new ListSetting("Blacklist Backpacks", "Add backpack number (1-18)",
                        () -> TfmConfig.METAL_DETECTOR_BACKPACK_BLACKLIST.get(),
                        values -> {
                            TfmConfig.METAL_DETECTOR_BACKPACK_BLACKLIST.set(values);
                            TfmConfig.save();
                        }));

        return MainGUIRegistry.subTab(
                "Metal Detector",
                "Crystal Hollows metal detector automation settings",
                List.of(metalDetector));
    }
}
