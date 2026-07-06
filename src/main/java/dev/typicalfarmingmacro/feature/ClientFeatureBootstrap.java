package dev.typicalfarmingmacro.feature;

import dev.typicalfarmingmacro.bootstrap.TfmChatEvents;
import dev.typicalfarmingmacro.bootstrap.TfmCommandRegistrar;
import dev.typicalfarmingmacro.bootstrap.TfmScreenHooks;
import dev.typicalfarmingmacro.bootstrap.TfmTickHandlers;
import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.config.ConfigManager;
import dev.typicalfarmingmacro.hud.HudRegistry;
import dev.typicalfarmingmacro.macro.MacroStateManager;
import dev.typicalfarmingmacro.macro.MacroWorkerThread;
import dev.typicalfarmingmacro.macro.ReconnectScheduler;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeSoundManager;
import dev.typicalfarmingmacro.modules.misc.AutoCarnivalManager;
import dev.typicalfarmingmacro.modules.pathfinding.debug.PathVisualizer;
import dev.typicalfarmingmacro.modules.performance.MuteManager;
import dev.typicalfarmingmacro.modules.performance.PerformanceModeManager;
import dev.typicalfarmingmacro.modules.profit.ProfitManager;
import dev.typicalfarmingmacro.modules.visuals.StreamerModeManager;
import dev.typicalfarmingmacro.notification.NotificationManager;
import dev.typicalfarmingmacro.renderer.FunRenderer;
import dev.typicalfarmingmacro.renderer.PositionHighlighter;
import dev.typicalfarmingmacro.ui.theme.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

import java.io.File;

public final class ClientFeatureBootstrap {
    private static boolean initialized;

    private ClientFeatureBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        ConfigManager.init();
        TfmConfig.init();
        FailsafeSoundManager.init();
        Theme.loadTheme();
        ProfitManager.loadLifetime();
        ProfitManager.loadDaily();
        MacroStateManager.syncFromConfig();
        AutoCarnivalManager.syncFromConfig(Minecraft.getInstance());
        ReconnectScheduler.clearState();
        HudRegistry.register();
        MacroWorkerThread.getInstance().start();
        PathVisualizer.register();

        LevelRenderEvents.END_MAIN.register(ctx -> {
            if (StreamerModeManager.isEnabled()) {
                return;
            }

            boolean drawPathVisualizer = PathVisualizer.shouldRender();
            boolean drawPositionHighlights = PositionHighlighter.hasVisibleHighlights();
            boolean drawFunEffects = FunRenderer.hasVisibleEffects();
            if (!drawPathVisualizer && !drawPositionHighlights && !drawFunEffects) {
                return;
            }
            if (drawPathVisualizer) {
                PathVisualizer.renderWorld();
            }
            if (drawPositionHighlights) {
                PositionHighlighter.renderWorld(ctx);
            }
            if (drawFunEffects) {
                FunRenderer.renderWorld(ctx);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(PerformanceModeManager::stop);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> TfmConfig.flush());

        TfmScreenHooks.register();
        TfmChatEvents.register();
        TfmCommandRegistrar.register();
        TfmTickHandlers.register();

        initialized = true;
    }

    public static synchronized void shutdown() {
        // Commit any in-progress daily farming time before teardown: a macro left running
        // until the game closes never fires onMacroStop(), so this prevents the day's
        // un-committed time from being lost on exit.
        dev.typicalfarmingmacro.modules.session.DailyFarmTimeTracker.persistNow();
        PerformanceModeManager.stop(Minecraft.getInstance());
        NotificationManager.clearAll();
        HudRegistry.reset();
        PathVisualizer.clear();
        ReconnectScheduler.clearState();
        MacroWorkerThread.getInstance().cancelCurrent();
        MacroWorkerThread.getInstance().clearPendingTasks();
        initialized = false;
    }

    public static synchronized void onConfigProfileLoaded(File profileFile) {
        FailsafeSoundManager.refresh();
        MacroStateManager.syncFromConfig();
        AutoCarnivalManager.syncFromConfig(Minecraft.getInstance());

        Minecraft client = Minecraft.getInstance();
        PerformanceModeManager.stop(client);
        MuteManager.stop(client);
        if (MacroStateManager.isMacroRunning()) {
            PerformanceModeManager.start(client);
            MuteManager.start(client);
        }
    }

}

