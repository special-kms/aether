package dev.aether.ui;

import dev.aether.bootstrap.AetherKeybindRegistry;
import dev.aether.config.AetherConfig;
import dev.aether.modules.clips.FfmpegInstaller;
import dev.aether.modules.clips.ClipManager;
import dev.aether.ui.settings.ActionSetting;
import dev.aether.ui.settings.DropdownSetting;
import dev.aether.ui.settings.KeybindSetting;
import dev.aether.ui.settings.ModulesTab;
import dev.aether.ui.settings.SettingGroup;
import dev.aether.ui.settings.ToggleSetting;

import java.util.List;

public final class ClipsVisualsRegistryProvider extends AbstractVisualsRegistryProvider {
    public ClipsVisualsRegistryProvider() {
        super(9);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                "Evidence Clips",
                "Retain short, silent gameplay clips around notable events"
        );
        group.add(new KeybindSetting("Save Clip Keybind", AetherKeybindRegistry.getClipKey()));
        group.add(new ToggleSetting("Auto-save on Failsafe",
                () -> AetherConfig.CLIPS_AUTO_FAILSAFE.get(),
                value -> {
                    AetherConfig.CLIPS_AUTO_FAILSAFE.set(value);
                    AetherConfig.save();
                }));
        group.add(new DropdownSetting("Capture FPS", List.of("15", "30", "60"),
                () -> switch (ClipManager.getConfiguredFps()) {
                    case 15 -> 0;
                    case 60 -> 2;
                    default -> 1;
                },
                index -> {
                    AetherConfig.CLIPS_FPS.set(index == 0 ? 15 : index == 2 ? 60 : 30);
                    AetherConfig.save();
                    ClipManager.syncFromConfig();
                }));
        group.add(new ActionSetting("Find FFmpeg", FfmpegInstaller::findFfmpeg));
        group.add(new ActionSetting("Install FFmpeg", FfmpegInstaller::installFfmpeg));

        return MainGUIRegistry.toggleSubTab(
                "Evidence Clips",
                "Capture 30 seconds before and 10 seconds after a request",
                () -> AetherConfig.CLIPS_ENABLED.get(),
                value -> {
                    AetherConfig.CLIPS_ENABLED.set(value);
                    AetherConfig.save();
                    ClipManager.syncFromConfig();
                },
                List.of(group)
        );
    }
}
