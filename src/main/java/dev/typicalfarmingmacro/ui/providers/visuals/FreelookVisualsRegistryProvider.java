package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.bootstrap.TfmKeybindRegistry;
import dev.typicalfarmingmacro.config.ConfigHelpers;
import dev.typicalfarmingmacro.config.FreelookMode;
import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.KeybindSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;

import java.util.Arrays;
import java.util.List;

public final class FreelookVisualsRegistryProvider extends AbstractVisualsRegistryProvider {
    public FreelookVisualsRegistryProvider() {
        super(3);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                "Freelook Settings",
                "Orbit the camera around your player while your body keeps facing forward"
        );
        group.add(new KeybindSetting("Freelook Keybind", TfmKeybindRegistry.getFreelookKey()));
        group.add(new DropdownSetting("Activation Mode",
                List.of("Hold", "Toggle"),
                () -> Arrays.asList(FreelookMode.values()).indexOf(ConfigHelpers.getFreelookMode()),
                i -> {
                    TfmConfig.FREELOOK_MODE.set(FreelookMode.values()[i].name());
                    TfmConfig.save();
                }));

        return MainGUIRegistry.toggleSubTab(
                "Freelook",
                "Orbit the camera freely without turning your player",
                () -> TfmConfig.FREELOOK_ENABLED.get(),
                enabled -> {
                    TfmConfig.FREELOOK_ENABLED.set(enabled);
                    TfmConfig.save();
                },
                List.of(group)
        );
    }
}
