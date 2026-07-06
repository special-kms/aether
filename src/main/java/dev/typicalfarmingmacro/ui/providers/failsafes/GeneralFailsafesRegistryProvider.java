package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.config.entries.StringEntry;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeSoundManager;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.ArrayList;
import java.util.List;

public final class GeneralFailsafesRegistryProvider extends AbstractFailsafesRegistryProvider {
    /** Sentinel shown for per-action sounds that fall back to the shared default. */
    private static final String SAME_AS_DEFAULT = "Same as Default";

    public GeneralFailsafesRegistryProvider() {
        super(0);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<String> defaultOptions = getFailsafeSoundOptions();
        List<String> stopOptions = withDefaultSentinel(defaultOptions);
        List<String> ignoreOptions = withDefaultSentinel(defaultOptions);

        SettingGroup group = SettingGroup.alwaysOn(
                        "Failsafe Settings",
                        "Shared settings for failsafes")
                .add(new ToggleSetting("Play Failsafe Sound",
                        () -> TfmConfig.FAILSAFE_SOUND_ENABLED.get(),
                        v -> {
                            TfmConfig.FAILSAFE_SOUND_ENABLED.set(v);
                            TfmConfig.save();
                        }))
                .add(new SliderSetting("Failsafe Volume", 0, 100,
                        () -> TfmConfig.FAILSAFE_SOUND_VOLUME.get() * 100.0f,
                        v -> {
                            TfmConfig.FAILSAFE_SOUND_VOLUME.set(clamp01(v / 100.0f));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("%")
                        .visibleWhen(() -> TfmConfig.FAILSAFE_SOUND_ENABLED.get()))
                .add(new ToggleSetting("Desktop Notification",
                        () -> TfmConfig.FAILSAFE_DESKTOP_NOTIFICATION_ENABLED.get(),
                        v -> {
                            TfmConfig.FAILSAFE_DESKTOP_NOTIFICATION_ENABLED.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Auto Alt-Tab",
                        () -> TfmConfig.FAILSAFE_AUTO_ALT_TAB.get(),
                        v -> {
                            TfmConfig.FAILSAFE_AUTO_ALT_TAB.set(v);
                            TfmConfig.save();
                        }))
                .add(new DropdownSetting("Default Failsafe Sound", defaultOptions,
                        () -> getFailsafeSoundIndex(defaultOptions),
                        i -> {
                            if (i < 0 || i >= defaultOptions.size()) {
                                return;
                            }
                            TfmConfig.FAILSAFE_SOUND_FILE.set(defaultOptions.get(i));
                            TfmConfig.save();
                        })
                        .addIconAction("/assets/typicalfarmingmacro/icons/folder.svg", FailsafeSoundManager::openSoundFolder)
                        .addIconAction("/assets/typicalfarmingmacro/icons/refresh.svg", () -> {
                            FailsafeSoundManager.refresh();
                            refreshFailsafeSoundOptions(defaultOptions);
                            refreshOverrideOptions(stopOptions, defaultOptions);
                            refreshOverrideOptions(ignoreOptions, defaultOptions);
                        })
                        .visibleWhen(() -> TfmConfig.FAILSAFE_SOUND_ENABLED.get()))
                .add(new DropdownSetting("Stop Failsafe Sound", stopOptions,
                        () -> getOverrideIndex(stopOptions, TfmConfig.FAILSAFE_SOUND_FILE_STOP),
                        i -> setOverride(stopOptions, i, TfmConfig.FAILSAFE_SOUND_FILE_STOP))
                        .visibleWhen(() -> TfmConfig.FAILSAFE_SOUND_ENABLED.get()))
                .add(new DropdownSetting("Ignore Failsafe Sound", ignoreOptions,
                        () -> getOverrideIndex(ignoreOptions, TfmConfig.FAILSAFE_SOUND_FILE_IGNORE),
                        i -> setOverride(ignoreOptions, i, TfmConfig.FAILSAFE_SOUND_FILE_IGNORE))
                        .visibleWhen(() -> TfmConfig.FAILSAFE_SOUND_ENABLED.get()))
                .add(new SliderSetting("Failsafe Additional Random Delay", 0, 5,
                        () -> TfmConfig.FAILSAFE_ADDITIONAL_RANDOM_DELAY_SECONDS.get(),
                        v -> {
                            TfmConfig.FAILSAFE_ADDITIONAL_RANDOM_DELAY_SECONDS.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix("s"));

        return MainGUIRegistry.subTab(
                "General",
                "Shared settings for failsafes",
                List.of(group));
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static List<String> getFailsafeSoundOptions() {
        List<String> sounds = new ArrayList<>(FailsafeSoundManager.getAvailableSounds());
        if (sounds.isEmpty()) {
            sounds.add(FailsafeSoundManager.getDefaultSoundFileName());
        }
        return sounds;
    }

    private static List<String> withDefaultSentinel(List<String> base) {
        List<String> options = new ArrayList<>(base.size() + 1);
        options.add(SAME_AS_DEFAULT);
        options.addAll(base);
        return options;
    }

    private static void refreshFailsafeSoundOptions(List<String> options) {
        options.clear();
        options.addAll(getFailsafeSoundOptions());
    }

    private static void refreshOverrideOptions(List<String> overrideOptions, List<String> base) {
        overrideOptions.clear();
        overrideOptions.add(SAME_AS_DEFAULT);
        overrideOptions.addAll(base);
    }

    private static int getFailsafeSoundIndex(List<String> options) {
        String selected = TfmConfig.FAILSAFE_SOUND_FILE.get();
        int selectedIndex = options.indexOf(selected);
        if (selectedIndex >= 0) {
            return selectedIndex;
        }

        int defaultIndex = options.indexOf(FailsafeSoundManager.getDefaultSoundFileName());
        return defaultIndex >= 0 ? defaultIndex : 0;
    }

    /** Index into the sentinel-prefixed list; 0 (Same as Default) when the override is blank. */
    private static int getOverrideIndex(List<String> options, StringEntry entry) {
        String selected = entry.get();
        if (selected == null || selected.isBlank()) {
            return 0;
        }
        int index = options.indexOf(selected);
        return index >= 0 ? index : 0;
    }

    private static void setOverride(List<String> options, int index, StringEntry entry) {
        if (index <= 0 || index >= options.size()) {
            entry.set(""); // Same as Default
        } else {
            entry.set(options.get(index));
        }
        TfmConfig.save();
    }
}
