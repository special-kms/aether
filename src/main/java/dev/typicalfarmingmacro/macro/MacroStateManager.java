package dev.typicalfarmingmacro.macro;

import net.minecraft.client.Minecraft;
import dev.typicalfarmingmacro.util.ClientUtils;
import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeManager;
import dev.typicalfarmingmacro.modules.metaldetector.MetalDetectorSolver;
import dev.typicalfarmingmacro.modules.misc.AutoCarnivalManager;
import dev.typicalfarmingmacro.modules.pathfinding.PathfindingManager;
import dev.typicalfarmingmacro.modules.session.DailyFarmTimeTracker;

public class MacroStateManager {
    private static volatile MacroState.State currentState = MacroState.State.OFF;
    private static volatile boolean intentionalDisconnect = false;
    private static volatile long sessionAccumulated = 0;
    private static volatile long lifetimeAccumulated = 0;
    private static volatile long lastSessionStartTime = 0;
    private static long lastPeriodicSaveTime = 0;

    public static void resetSession() {
        if (isMacroRunning()) {
            lastSessionStartTime = System.currentTimeMillis();
        } else {
            lastSessionStartTime = 0;
        }
        sessionAccumulated = 0;
        dev.typicalfarmingmacro.modules.profit.ProfitManager.reset();
        AutoCarnivalManager.resetTokenSession();
        dev.typicalfarmingmacro.modules.session.DynamicRestManager.reset();
        dev.typicalfarmingmacro.util.BpsTracker.reset();
    }

    public static void syncFromConfig() {
        lifetimeAccumulated = (long) (double) TfmConfig.LIFETIME_ACCUMULATED.get();
        DailyFarmTimeTracker.syncFromConfig();
    }

    public static void periodicUpdate() {
        if (currentState == MacroState.State.OFF || currentState == MacroState.State.RECOVERING)
            return;

        long now = System.currentTimeMillis();
        if (lastSessionStartTime <= 0) {
            lastPeriodicSaveTime = now;
            return;
        }
        if (now - lastPeriodicSaveTime > 60000) { // 1 minute
            lastPeriodicSaveTime = now;
            long diff = Math.max(0L, now - lastSessionStartTime);

            if (TfmConfig.PERSIST_SESSION_TIMER.get()) {
                // Keep session timer as is for pause/unpause if enabled
            } else {
                // Not actually hit here since we're periodic other than if someone pauses?
                // Wait, sessionAccumulated is only saved to disk if we want it to survive
                // RESTART
            }
            DailyFarmTimeTracker.periodicSave();
            TfmConfig.LIFETIME_ACCUMULATED.set((double) (lifetimeAccumulated + diff));
            TfmConfig.save();
        }
    }

    public static long getSessionRunningTime() {
        if (currentState != MacroState.State.OFF && currentState != MacroState.State.RECOVERING
                && lastSessionStartTime != 0) {
            return sessionAccumulated + (System.currentTimeMillis() - lastSessionStartTime);
        }
        return sessionAccumulated;
    }

    public static long getLifetimeRunningTime() {
        if (currentState != MacroState.State.OFF && currentState != MacroState.State.RECOVERING
                && lastSessionStartTime != 0) {
            return lifetimeAccumulated + (System.currentTimeMillis() - lastSessionStartTime);
        }
        return lifetimeAccumulated;
    }

    public static boolean isMacroRunning() {
        return currentState != MacroState.State.OFF;
    }

    public static boolean isIntentionalDisconnect() {
        return intentionalDisconnect;
    }

    public static void setIntentionalDisconnect(boolean intentional) {
        intentionalDisconnect = intentional;
    }

    public static MacroState.State getCurrentState() {
        return currentState;
    }

    public static void setCurrentState(MacroState.State state) {
        MacroState.State prevState = currentState;
        currentState = state;
        Minecraft client = Minecraft.getInstance();

        if (state == MacroState.State.FARMING && prevState != MacroState.State.FARMING) {
            MacroWorkerThread.getInstance().clearPendingTasks();
            PathfindingManager.stop();
        }

        if (prevState == MacroState.State.OFF && state != MacroState.State.OFF
                && state != MacroState.State.RECOVERING) {
            lastSessionStartTime = System.currentTimeMillis();
            DailyFarmTimeTracker.onMacroStart();
            if (!TfmConfig.PERSIST_SESSION_TIMER.get()) {
                sessionAccumulated = 0;
                dev.typicalfarmingmacro.modules.profit.ProfitManager.reset();
                AutoCarnivalManager.resetTokenSession();
            }
            lastPeriodicSaveTime = System.currentTimeMillis();
        } else if (prevState == MacroState.State.RECOVERING && state != MacroState.State.OFF
                && state != MacroState.State.RECOVERING) {
            lastSessionStartTime = System.currentTimeMillis();
            DailyFarmTimeTracker.onMacroStart();
            FailsafeManager.syncExpectedRotationFromClient(client);
            FailsafeManager.addRotationGracePeriod(TfmConfig.FAILSAFE_ROTATION_WARP_GRACE_MS.get());
        } else if (prevState != MacroState.State.OFF && prevState != MacroState.State.RECOVERING
                && (state == MacroState.State.OFF || state == MacroState.State.RECOVERING)) {
            if (lastSessionStartTime != 0) {
                long diff = System.currentTimeMillis() - lastSessionStartTime;
                sessionAccumulated += diff;
                lifetimeAccumulated += diff;
                lastSessionStartTime = 0;

                TfmConfig.LIFETIME_ACCUMULATED.set((double) lifetimeAccumulated);
                TfmConfig.save();
            }
            DailyFarmTimeTracker.onMacroStop();
        }

        // Mouse Grab/Ungrab Logic
        if (state != MacroState.State.OFF) {
            runOnClientThread(client, () -> {
                if (TfmConfig.MACRO_UNGRAB_MOUSE.get()) {
                    dev.typicalfarmingmacro.modules.farming.UngrabMouse.requestMacroUngrab();
                }
                dev.typicalfarmingmacro.modules.performance.PerformanceModeManager.start(client);
                dev.typicalfarmingmacro.modules.performance.MuteManager.start(client);
            });
        } else {
            runOnClientThread(client, () -> {
                dev.typicalfarmingmacro.modules.farming.UngrabMouse.clearMacroUngrab();
                dev.typicalfarmingmacro.modules.performance.PerformanceModeManager.stop(client);
                dev.typicalfarmingmacro.modules.performance.MuteManager.stop(client);
            });
        }
    }

    public static void stopMacro(Minecraft client) {
        stopMacro(client, "Macro stopped by user");
    }

    public static void stopMacro(Minecraft client, String debugReason) {
        stopMacro(client, debugReason, true);
    }

    public static void stopMacro(Minecraft client, String debugReason, boolean closeScreen) {
        MacroWorkerThread.getInstance().cancelCurrent();
        // Stop any active internal farming macro.
        runOnClientThread(client, () -> FarmingMacroManager.disable(client));
        MetalDetectorSolver.stopForMacro(client);
        AutoCarnivalManager.stopForMacro(client);
        FailsafeManager.reset();
        dev.typicalfarmingmacro.modules.farming.SqueakyMousematManager.clearReapplyAttempt();
        if (client != null) {
            client.execute(() -> {
                if (closeScreen && client.screen != null) {
                    client.setScreen(null);
                }
                dev.typicalfarmingmacro.modules.farming.UngrabMouse.clearMacroUngrab();
            });
        }
        setCurrentState(MacroState.State.OFF);
        ClientUtils.forceReleaseKeys(client);
        ClientUtils.sendDebugMessage(client, debugReason);
        dev.typicalfarmingmacro.modules.pest.PestManager.reset();
        dev.typicalfarmingmacro.modules.pest.helpers.PestExchangeManager.stop();
        dev.typicalfarmingmacro.modules.pest.helpers.PestDestroyer.stop(client);
        dev.typicalfarmingmacro.modules.pest.helpers.PestTrapManager.cancel(client);
        dev.typicalfarmingmacro.modules.inventorymanager.AutoSellManager.cancel(client);
        dev.typicalfarmingmacro.modules.pest.helpers.AutoSprayonatorManager.cancel();
        dev.typicalfarmingmacro.modules.pest.helpers.AutoSprayonatorManager.reset();
        dev.typicalfarmingmacro.modules.pest.helpers.AutoPestExchangeManager.reset();
        dev.typicalfarmingmacro.modules.GreenhouseManager.reset();
        dev.typicalfarmingmacro.modules.ComposterManager.reset();
        dev.typicalfarmingmacro.modules.SupercraftManager.reset();
        dev.typicalfarmingmacro.modules.gear.GearManager.reset();
        dev.typicalfarmingmacro.modules.inventorymanager.GeorgeManager.reset();
        dev.typicalfarmingmacro.modules.inventorymanager.BookCombineManager.reset();
        dev.typicalfarmingmacro.modules.inventorymanager.JunkManager.reset();
        dev.typicalfarmingmacro.modules.session.RecoveryManager.reset();
        dev.typicalfarmingmacro.modules.session.RestartManager.reset();
        if (!TfmConfig.PERSIST_SESSION_TIMER.get()) {
            dev.typicalfarmingmacro.modules.session.DynamicRestManager.reset();
            dev.typicalfarmingmacro.modules.profit.ProfitManager.reset();
            AutoCarnivalManager.resetTokenSession();
        }
        ReconnectScheduler.cancel();
        dev.typicalfarmingmacro.modules.pathfinding.PathfindingManager.stop();
        dev.typicalfarmingmacro.modules.visitor.VisitorsMacro.stop(client);
    }

    private static void runOnClientThread(Minecraft client, Runnable action) {
        if (client == null || action == null) {
            return;
        }
        if (client.isSameThread()) {
            action.run();
            return;
        }
        client.execute(action);
    }
}
