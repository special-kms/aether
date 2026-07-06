package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.macro.MacroStateManager;
import dev.typicalfarmingmacro.modules.session.DailyFarmTimeTracker;
import dev.typicalfarmingmacro.modules.session.DynamicRestManager;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.ActionSetting;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.List;

public final class DynamicRestRegistryProvider extends AbstractModulesRegistryProvider {
    public DynamicRestRegistryProvider() {
        super(8);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.alwaysOn(
                        "Dynamic Rest",
                        "Schedules automatic breaks during scripting")
                .add(new SliderSetting("Scripting Time", 1, 600,
                        () -> (float) TfmConfig.REST_SCRIPTING_TIME.get(),
                        v -> {
                            TfmConfig.REST_SCRIPTING_TIME.set(Math.round(v));
                            TfmConfig.save();
                            DynamicRestManager.refreshCurrentSession();
                        })
                        .withDecimals(0).withSuffix(" min"))
                .add(new SliderSetting("Scripting Offset", 0, 300,
                        () -> (float) TfmConfig.REST_SCRIPTING_TIME_OFFSET.get(),
                        v -> {
                            TfmConfig.REST_SCRIPTING_TIME_OFFSET.set(Math.round(v));
                            TfmConfig.save();
                            DynamicRestManager.refreshCurrentSession();
                        })
                        .withDecimals(0).withSuffix(" min"))
                .add(new SliderSetting("Break Time", 1, 600,
                        () -> (float) TfmConfig.REST_BREAK_TIME.get(),
                        v -> {
                            TfmConfig.REST_BREAK_TIME.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix(" min"))
                .add(new SliderSetting("Break Offset", 0, 300,
                        () -> (float) TfmConfig.REST_BREAK_TIME_OFFSET.get(),
                        v -> {
                            TfmConfig.REST_BREAK_TIME_OFFSET.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0).withSuffix(" min"))
                .add(new SliderSetting("Daily Threshold", 0, 24,
                        () -> (float) (double) TfmConfig.DAILY_FARM_THRESHOLD_HOURS.get(),
                        v -> {
                            TfmConfig.DAILY_FARM_THRESHOLD_HOURS.set((double) Math.max(0f, Math.min(24f, v)));
                            TfmConfig.save();
                        })
                        .withDecimals(1).withSuffix(" hr"))
                .add(new ToggleSetting("Close Game On Daily Threshold",
                        () -> TfmConfig.CLOSE_GAME_ON_DAILY_THRESHOLD.get(),
                        v -> {
                            TfmConfig.CLOSE_GAME_ON_DAILY_THRESHOLD.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.DAILY_FARM_THRESHOLD_HOURS.get() > 0.0))
                .add(new ActionSetting("Reset Daily Timer", DailyFarmTimeTracker::resetToday));

        return MainGUIRegistry.toggleSubTab(
                "Dynamic Rest",
                "Schedules automatic breaks during scripting",
                () -> TfmConfig.DYNAMIC_REST_ENABLED.get(),
                DynamicRestRegistryProvider::setDynamicRestEnabled,
                List.of(group));
    }

    private static void setDynamicRestEnabled(boolean enabled) {
        TfmConfig.DYNAMIC_REST_ENABLED.set(enabled);
        TfmConfig.save();
        if (!enabled) {
            DynamicRestManager.reset();
        } else if (MacroStateManager.isMacroRunning()) {
            DynamicRestManager.scheduleNextRest();
        }
    }
}
