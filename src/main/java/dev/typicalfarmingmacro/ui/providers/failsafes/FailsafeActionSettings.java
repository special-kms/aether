package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeCustomReplayManager;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class FailsafeActionSettings {
    private static final List<String> ACTION_OPTIONS = List.of("Stop", "Ignore", "Custom");

    private FailsafeActionSettings() {
    }

    static DropdownSetting createActionDropdown(String label, Supplier<String> getter, Consumer<String> setter) {
        return new DropdownSetting(label, ACTION_OPTIONS,
                () -> getActionIndex(getter.get()),
                index -> {
                    if (index < 0 || index >= ACTION_OPTIONS.size()) {
                        return;
                    }

                    setter.accept(ACTION_OPTIONS.get(index).toUpperCase(Locale.ROOT));
                    TfmConfig.save();
                });
    }

    static DropdownSetting createCustomReplayDropdown(
            FailsafeCustomReplayManager.FailsafeReplayType type,
            Supplier<String> actionGetter
    ) {
        List<String> replayOptions = new ArrayList<>(FailsafeCustomReplayManager.getAvailableReplayOptions(type));
        return new DropdownSetting("Custom Replay (/typicalfarmingmacro movement or typicalfarmingmacro.cat/editor)", replayOptions,
                () -> FailsafeCustomReplayManager.getSelectedReplayIndex(type, replayOptions),
                index -> FailsafeCustomReplayManager.setSelectedReplay(type, replayOptions, index))
                .addIconAction("/assets/typicalfarmingmacro/icons/folder.svg",
                        () -> FailsafeCustomReplayManager.openReplayFolder(type))
                .addIconAction("/assets/typicalfarmingmacro/icons/refresh.svg",
                        () -> FailsafeCustomReplayManager.refreshReplayOptions(type, replayOptions))
                .visibleWhen(() -> "CUSTOM".equalsIgnoreCase(actionGetter.get()));
    }

    private static int getActionIndex(String selected) {
        if (selected == null || selected.isBlank()) {
            return 0;
        }

        String normalized = selected.substring(0, 1).toUpperCase(Locale.ROOT)
                + selected.substring(1).toLowerCase(Locale.ROOT);
        int index = ACTION_OPTIONS.indexOf(normalized);
        return index >= 0 ? index : 0;
    }
}
