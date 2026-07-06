package dev.typicalfarmingmacro.modules.failsafe;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.macro.MacroState;
import dev.typicalfarmingmacro.macro.MacroStateManager;
import dev.typicalfarmingmacro.modules.pest.helpers.PestDestroyer;
import dev.typicalfarmingmacro.modules.rotation.RotationManager;
import dev.typicalfarmingmacro.notification.NotificationManager;
import dev.typicalfarmingmacro.modules.session.RestartManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

final class RotationFailsafe {
    enum State {
        IDLE,
        WAIT,
        TRIGGERED
    }

    private static volatile boolean expectedRotationSet = false;
    private static volatile float expectedYaw = 0.0f;
    private static volatile float expectedPitch = 0.0f;
    private static volatile long graceUntilMs = 0L;
    private static volatile long mismatchSince = 0L;
    private static volatile long mismatchRandomDelayMs = 0L;
    private static volatile boolean triggered = false;

    private RotationFailsafe() {}

    static void reset() {
        expectedRotationSet = false;
        expectedYaw = 0.0f;
        expectedPitch = 0.0f;
        graceUntilMs = 0L;
        mismatchSince = 0L;
        mismatchRandomDelayMs = 0L;
        triggered = false;
    }

    static void expectRotation(float yaw, float pitch) {
        expectedRotationSet = true;
        expectedYaw = Mth.wrapDegrees(yaw);
        expectedPitch = Mth.clamp(pitch, -90.0f, 90.0f);
        mismatchSince = 0L;
        mismatchRandomDelayMs = 0L;
    }

    static void syncExpectedRotationFromClient(Minecraft client) {
        if (client == null || client.player == null) return;
        expectRotation(client.player.getYRot(), client.player.getXRot());
        triggered = false;
    }

    static void addGracePeriod(long durationMs) {
        if (durationMs <= 0L) return;
        long until = System.currentTimeMillis() + durationMs;
        graceUntilMs = Math.max(graceUntilMs, until);
        mismatchSince = 0L;
        mismatchRandomDelayMs = 0L;
    }

    static void tick(Minecraft client) {
        if (client == null || client.player == null) {
            reset();
            return;
        }

        MacroState.State state = MacroStateManager.getCurrentState();
        if (state == MacroState.State.OFF || state == MacroState.State.RECOVERING) {
            syncExpectedRotationFromClient(client);
            mismatchSince = 0L;
            mismatchRandomDelayMs = 0L;
            return;
        }

        if (RestartManager.isRestartSequenceActive()) {
            syncExpectedRotationFromClient(client);
            mismatchSince = 0L;
            mismatchRandomDelayMs = 0L;
            return;
        }

        if (!TfmConfig.FAILSAFE_ROTATION.get()) {
            mismatchSince = 0L;
            mismatchRandomDelayMs = 0L;
            if (!expectedRotationSet) {
                syncExpectedRotationFromClient(client);
            }
            return;
        }

        if (System.currentTimeMillis() < graceUntilMs) {
            syncExpectedRotationFromClient(client);
            mismatchSince = 0L;
            mismatchRandomDelayMs = 0L;
            return;
        }

        if (isPestCleanerActive() && !TfmConfig.FAILSAFE_ROTATION_TRIGGER_DURING_PEST_CLEANER.get()) {
            syncExpectedRotationFromClient(client);
            mismatchSince = 0L;
            mismatchRandomDelayMs = 0L;
            return;
        }

        if (!expectedRotationSet) {
            syncExpectedRotationFromClient(client);
            return;
        }

        float currentYaw = Mth.wrapDegrees(client.player.getYRot());
        float currentPitch = Mth.clamp(client.player.getXRot(), -90.0f, 90.0f);
        float yawDiff = Math.abs(Mth.wrapDegrees(currentYaw - expectedYaw));
        float pitchDiff = Math.abs(currentPitch - expectedPitch);

        if (yawDiff <= TfmConfig.FAILSAFE_ROTATION_YAW_THRESHOLD.get()
                && pitchDiff <= TfmConfig.FAILSAFE_ROTATION_PITCH_THRESHOLD.get()) {
            mismatchSince = 0L;
            mismatchRandomDelayMs = 0L;
            return;
        }

        if (mismatchSince == 0L) {
            mismatchSince = System.currentTimeMillis();
            mismatchRandomDelayMs = FailsafeManager.sampleAdditionalTriggerDelayMs();
            if (shouldSuppressPestCleanerRotation(client)) {
                RotationManager.cancelRotation();
            }
            return;
        }

        if (shouldSuppressPestCleanerRotation(client)) {
            RotationManager.cancelRotation();
        }

        long triggerDelayMs = getConfiguredTriggerDelayMs() + mismatchRandomDelayMs;
        if (System.currentTimeMillis() - mismatchSince < triggerDelayMs) {
            return;
        }

        trigger(client, currentYaw, currentPitch, yawDiff, pitchDiff);
    }

    static State getState(Minecraft client) {
        if (triggered) return State.TRIGGERED;
        MacroState.State state = MacroStateManager.getCurrentState();
        if (client == null || client.player == null
                || state == MacroState.State.OFF
                || state == MacroState.State.RECOVERING
                || RestartManager.isRestartSequenceActive()
                || !TfmConfig.FAILSAFE_ROTATION.get() || !expectedRotationSet) {
            return State.IDLE;
        }
        if (System.currentTimeMillis() < graceUntilMs) {
            return State.IDLE;
        }

        if (isPestCleanerActive() && !TfmConfig.FAILSAFE_ROTATION_TRIGGER_DURING_PEST_CLEANER.get()) {
            return State.IDLE;
        }

        float currentYaw = Mth.wrapDegrees(client.player.getYRot());
        float currentPitch = Mth.clamp(client.player.getXRot(), -90.0f, 90.0f);
        float yawDiff = Math.abs(Mth.wrapDegrees(currentYaw - expectedYaw));
        float pitchDiff = Math.abs(currentPitch - expectedPitch);
        if (yawDiff > TfmConfig.FAILSAFE_ROTATION_YAW_THRESHOLD.get()
                || pitchDiff > TfmConfig.FAILSAFE_ROTATION_PITCH_THRESHOLD.get()) {
            return State.WAIT;
        }
        return State.IDLE;
    }

    static float getExpectedYaw() {
        return expectedYaw;
    }

    static float getExpectedPitch() {
        return expectedPitch;
    }

    static long getTriggerRemainingMs() {
        if (mismatchSince == 0L) return 0L;
        long elapsedMs = System.currentTimeMillis() - mismatchSince;
        long triggerDelayMs = getConfiguredTriggerDelayMs() + mismatchRandomDelayMs;
        return Math.max(0L, triggerDelayMs - elapsedMs);
    }

    static boolean shouldSuppressPestCleanerRotation(Minecraft client) {
        return client != null
                && mismatchSince != 0L
                && !RotationManager.isRotating()
                && getState(client) == State.WAIT
                && isPestCleanerActive()
                && TfmConfig.FAILSAFE_ROTATION_TRIGGER_DURING_PEST_CLEANER.get();
    }

    private static long getConfiguredTriggerDelayMs() {
        if (isPestCleanerActive() && TfmConfig.FAILSAFE_ROTATION_TRIGGER_DURING_PEST_CLEANER.get()) {
            return TfmConfig.FAILSAFE_ROTATION_PEST_CLEANER_DELAY_MS.get();
        }
        return Math.round(TfmConfig.FAILSAFE_ROTATION_TRIGGER_DELAY_SECONDS.get() * 1000.0f);
    }

    private static boolean isPestCleanerActive() {
        return PestDestroyer.isActive();
    }

    private static void trigger(Minecraft client, float currentYaw, float currentPitch, float yawDiff, float pitchDiff) {
        if (triggered) {
            return;
        }

        boolean pestCleanerRotation = isPestCleanerActive();
        FailsafeAction action = FailsafeManager.getRotationAction(pestCleanerRotation);
        triggered = true;
        NotificationManager.error(
                FailsafeManager.getNotificationTitle(action),
                "Player rotation changed unexpectedly.");
        FailsafeManager.handleConfiguredAction(
                client,
                action,
                pestCleanerRotation
                        ? FailsafeCustomReplayManager.FailsafeReplayType.PEST_ROTATION
                        : FailsafeCustomReplayManager.FailsafeReplayType.ROTATION,
                String.format(java.util.Locale.US,
                        "unexpected rotation. Current yaw/pitch %.1f / %.1f, expected %.1f / %.1f, diff %.1f / %.1f.",
                        currentYaw, currentPitch, expectedYaw, expectedPitch, yawDiff, pitchDiff),
                String.format(java.util.Locale.US,
                        "RotationFailsafe: unexpected rotation (current %.1f / %.1f, expected %.1f / %.1f, diff %.1f / %.1f)",
                        currentYaw, currentPitch, expectedYaw, expectedPitch, yawDiff, pitchDiff));
        reset();
    }
}
