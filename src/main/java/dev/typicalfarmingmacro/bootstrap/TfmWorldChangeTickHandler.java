package dev.typicalfarmingmacro.bootstrap;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.util.TfmResources;
import dev.typicalfarmingmacro.macro.MacroState;
import dev.typicalfarmingmacro.macro.MacroStateManager;
import dev.typicalfarmingmacro.macro.ReconnectScheduler;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeAction;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeCustomReplayManager;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeManager;
import dev.typicalfarmingmacro.modules.session.RecoveryManager;
import dev.typicalfarmingmacro.modules.session.RestartManager;
import dev.typicalfarmingmacro.notification.NotificationManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

public final class TfmWorldChangeTickHandler {
    private static ClientLevel lastObservedLevel;
    private static Vec3 lastStablePlayerPosition;

    private TfmWorldChangeTickHandler() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
                        ClientLevel currentLevel = client.level;
            if (currentLevel == null) {
                lastObservedLevel = null;
                return;
            }

            if (lastObservedLevel == null) {
                lastObservedLevel = currentLevel;
                if (client.player != null) {
                    lastStablePlayerPosition = client.player.position();
                }
                return;
            }

            if (currentLevel != lastObservedLevel) {
                Vec3 savedPosition = lastStablePlayerPosition;
                lastObservedLevel = currentLevel;
                if (shouldHandleWorldChange()) {
                    handleWorldChangeFailsafe(client, savedPosition);
                }
            }

            if (client.player != null) {
                lastStablePlayerPosition = client.player.position();
            }
        });
    }

    private static boolean shouldHandleWorldChange() {
        if (!TfmConfig.FAILSAFE_WORLD_CHANGE.get()) {
            return false;
        }

        if (!MacroStateManager.isMacroRunning()) {
            return false;
        }

        if (MacroStateManager.isIntentionalDisconnect()) {
            return false;
        }

        if (MacroStateManager.getCurrentState() == MacroState.State.RECOVERING) {
            return false;
        }

        if (RestartManager.isRestartPending()) {
            return false;
        }

        return !ReconnectScheduler.isPending() && !ReconnectScheduler.shouldResume();
    }

    private static void handleWorldChangeFailsafe(Minecraft client, Vec3 savedPosition) {
        FailsafeAction action = FailsafeManager.getWorldChangeAction();
        String details = "World changed unexpectedly.";
        String debugReason = "World change detected";
        NotificationManager.error(FailsafeManager.getNotificationTitle(action), details);

        if (FailsafeManager.shouldStopMacroOnTrigger(action)) {
            FailsafeManager.handleConfiguredAction(
                    client,
                    action,
                    FailsafeCustomReplayManager.FailsafeReplayType.WORLD_CHANGE,
                    details,
                    "World changed; stopping macro");
            return;
        }

        FailsafeManager.handleConfiguredAction(
                client,
                action,
                FailsafeCustomReplayManager.FailsafeReplayType.WORLD_CHANGE,
                details,
                debugReason,
                "Macro stopped and world change recovery started.");
        if (action == FailsafeAction.CUSTOM) {
            return;
        }
        if (savedPosition == null) {
            MacroStateManager.stopMacro(client, "World change recovery skipped: no saved position", false);
            return;
        }

        MacroStateManager.stopMacro(client, "World change detected; starting recovery path", false);
        RecoveryManager.beginWorldChangeRecovery(savedPosition);
    }

}

