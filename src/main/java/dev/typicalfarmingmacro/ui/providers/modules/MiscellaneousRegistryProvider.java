package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.config.ConfigHelpers;
import dev.typicalfarmingmacro.config.UnflyMode;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MiscellaneousRegistryProvider extends AbstractModulesRegistryProvider {
    public MiscellaneousRegistryProvider() {
        super(11);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();

        groups.add(SettingGroup.alwaysOn(
                        "Miscellaneous",
                        "Miscellaneous settings")
                .add(new DropdownSetting("Unfly Mode",
                        List.of("Sneak", "2x Tap Space"),
                        () -> TfmConfig.UNFLY_MODE.get().length() > 0
                                ? Arrays.asList(UnflyMode.values()).indexOf(ConfigHelpers.getUnflyMode())
                                : 0,
                        i -> {
                            TfmConfig.UNFLY_MODE.set(UnflyMode.values()[i].name());
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Show Debug",
                        () -> TfmConfig.SHOW_DEBUG.get(),
                        v -> {
                            TfmConfig.SHOW_DEBUG.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Persist Session Timer",
                        () -> TfmConfig.PERSIST_SESSION_TIMER.get(),
                        v -> {
                            TfmConfig.PERSIST_SESSION_TIMER.set(v);
                            TfmConfig.save();
                        }))
                .add(new SliderSetting("Pathfinder Max Jump Height", 1, 6,
                        () -> (float) TfmConfig.PATHFINDER_MAX_JUMP_HEIGHT.get(),
                        v -> {
                            TfmConfig.PATHFINDER_MAX_JUMP_HEIGHT.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix(" blocks")));

        groups.add(SettingGroup.alwaysOn(
                        "Macro Settings",
                        "Macro-specific client behavior")
                .add(new ToggleSetting("Ungrab Mouse",
                        () -> TfmConfig.MACRO_UNGRAB_MOUSE.get(),
                        v -> {
                            TfmConfig.MACRO_UNGRAB_MOUSE.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Mute Game",
                        () -> TfmConfig.MUTE_GAME.get(),
                        v -> {
                            TfmConfig.MUTE_GAME.set(v);
                            TfmConfig.save();
                        }))
                .add(new SliderSetting("Game Volume", 0, 100,
                        () -> TfmConfig.MUTE_GAME_VOLUME.get() * 100.0f,
                        v -> {
                            TfmConfig.MUTE_GAME_VOLUME.set(clamp01(v / 100.0f));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix("%")
                        .visibleWhen(() -> TfmConfig.MUTE_GAME.get()))
                .add(new ToggleSetting("Keep Focus",
                        () -> TfmConfig.KEEP_FOCUS.get(),
                        v -> {
                            TfmConfig.KEEP_FOCUS.set(v);
                            TfmConfig.save();
                        })));

        SettingGroup performanceMode = SettingGroup.of(
                "Performance Mode",
                "Lowers render distance and limits FPS during macro execution",
                () -> TfmConfig.PERFORMANCE_MODE.get(),
                v -> {
                    TfmConfig.PERFORMANCE_MODE.set(v);
                    TfmConfig.save();
                });
        performanceMode.add(new ToggleSetting("Limit FPS",
                () -> TfmConfig.PERFORMANCE_LIMIT_FPS.get(),
                v -> {
                    TfmConfig.PERFORMANCE_LIMIT_FPS.set(v);
                    TfmConfig.save();
                }));
        performanceMode.add(new SliderSetting("Max FPS", 20, 60,
                () -> (float) TfmConfig.PERFORMANCE_MODE_MAX_FPS.get(),
                v -> {
                    TfmConfig.PERFORMANCE_MODE_MAX_FPS.set(Math.round(v));
                    TfmConfig.save();
                })
                .withDecimals(0).withSuffix(" fps")
                .visibleWhen(() -> TfmConfig.PERFORMANCE_LIMIT_FPS.get()));
        performanceMode.add(new ToggleSetting("Limit Chunk Distance",
                () -> TfmConfig.PERFORMANCE_LIMIT_CHUNK_DISTANCE.get(),
                v -> {
                    TfmConfig.PERFORMANCE_LIMIT_CHUNK_DISTANCE.set(v);
                    TfmConfig.save();
                }));
        performanceMode.add(new SliderSetting("Chunk Distance", 2, 8,
                () -> (float) TfmConfig.PERFORMANCE_CHUNK_DISTANCE.get(),
                v -> {
                    TfmConfig.PERFORMANCE_CHUNK_DISTANCE.set(Math.round(v));
                    TfmConfig.save();
                })
                .withDecimals(0).withSuffix(" chunks")
                .visibleWhen(() -> TfmConfig.PERFORMANCE_LIMIT_CHUNK_DISTANCE.get()));
        performanceMode.add(new ToggleSetting("Disable Block Breaking Particles",
                () -> TfmConfig.PERFORMANCE_DISABLE_PARTICLES.get(),
                v -> {
                    TfmConfig.PERFORMANCE_DISABLE_PARTICLES.set(v);
                    TfmConfig.save();
                }));
        groups.add(performanceMode);

        return MainGUIRegistry.subTab(
                "Miscellaneous",
                "Miscellaneous settings",
                groups);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
