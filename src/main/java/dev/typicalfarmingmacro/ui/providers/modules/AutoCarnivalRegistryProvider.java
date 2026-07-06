package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.misc.AutoCarnivalManager;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class AutoCarnivalRegistryProvider extends AbstractMiningRegistryProvider {
    public AutoCarnivalRegistryProvider() {
        super(12);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "Miscellaneous",
                        "Shootout settings")
                .add(new SliderSetting("Offset", 0, 1000,
                        () -> (float) TfmConfig.AUTO_CARNIVAL_PING.get(),
                        value -> {
                            TfmConfig.AUTO_CARNIVAL_PING.set(Math.round(value));
                            TfmConfig.save();
                        })
                        .withDecimals(0));

        return MainGUIRegistry.toggleSubTab(
                "Auto Carnival (Shootout)",
                "Automatically clears shootout rounds and requeues them until tickets run out",
                () -> TfmConfig.AUTO_CARNIVAL_SHOOTOUT.get(),
                value -> AutoCarnivalManager.setEnabled(Minecraft.getInstance(), value),
                List.of(group));
    }
}
