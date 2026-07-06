package dev.typicalfarmingmacro.config;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import dev.typicalfarmingmacro.util.ClientUtils;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ThreadLocalRandom;

public final class ConfigHelpers {
    private ConfigHelpers() {}

    public static int getRandomizedDelay(int baseDelay) {
        return Math.max(0, baseDelay);
    }

    public static int getRandomizedDelay(int minDelay, int maxDelay) {
        int lower = Math.max(0, Math.min(minDelay, maxDelay));
        int upper = Math.max(0, Math.max(minDelay, maxDelay));
        if (lower == upper) {
            return lower;
        }
        return ThreadLocalRandom.current().nextInt(lower, upper + 1);
    }

    public static void executePlotTpRewarp(Minecraft client) {
        if (TfmConfig.ENABLE_PLOT_TP_REWARP.get()) {
            executePlotTpRewarp(client, TfmConfig.PLOT_TP_NUMBER.get());
        }
    }

    public static void executePlotTpRewarp(Minecraft client, String plotNumber) {
        executeRewarpCommand(client, RewarpMode.PLOT_TP, plotNumber);
    }

    public static void executeRewarpCommand(Minecraft client, RewarpMode mode, String plotNumber) {
        RewarpMode resolvedMode = mode == null ? RewarpMode.FLY : mode;
        if (!resolvedMode.usesCommand()) {
            return;
        }

        String sanitizedPlotNumber = plotNumber == null || plotNumber.isBlank() ? "0" : plotNumber.trim();
        TfmBootstrapHooks.addRotationGracePeriod(TfmConfig.FAILSAFE_ROTATION_WARP_GRACE_MS.get());
        if (resolvedMode == RewarpMode.WARP_GARDEN) {
            ClientUtils.sendCommand(client, "/warp garden");
        } else {
            ClientUtils.sendCommand(client, "/plottp " + sanitizedPlotNumber);
        }
    }

    public static UnflyMode getUnflyMode() {
        try { return UnflyMode.valueOf(TfmConfig.UNFLY_MODE.get()); }
        catch (IllegalArgumentException e) { return UnflyMode.DOUBLE_TAP_SPACE; }
    }

    public static FreelookMode getFreelookMode() {
        try { return FreelookMode.valueOf(TfmConfig.FREELOOK_MODE.get()); }
        catch (IllegalArgumentException e) { return FreelookMode.HOLD; }
    }
}

