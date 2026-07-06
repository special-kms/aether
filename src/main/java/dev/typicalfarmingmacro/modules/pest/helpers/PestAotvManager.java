package dev.typicalfarmingmacro.modules.pest.helpers;

import dev.typicalfarmingmacro.config.TfmConfig;

import dev.typicalfarmingmacro.util.ClientUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import dev.typicalfarmingmacro.modules.gear.GearManager;
import dev.typicalfarmingmacro.modules.rotation.RotationManager;

public class PestAotvManager {
    public static volatile boolean isSneakingForAotv = false;

    public static void resetState() {
        isSneakingForAotv = false;
    }

    public static boolean shouldDoAotvOnCurrentPlot(Minecraft client, String currentInfestedPlot, boolean isSamePlot) {
        if (!TfmConfig.AOTV_TO_ROOF.get())
            return false;

        if (TfmConfig.AOTV_ROOF_PLOTS.get().isEmpty())
            return true;

        boolean inAllowedList = TfmConfig.AOTV_ROOF_PLOTS.get().contains(currentInfestedPlot);
        ClientUtils.sendDebugMessage(client,
                inAllowedList ? "plot in list, performing aotv" : "plot not in list, skipping aotv");
        return inAllowedList;
    }

    public static boolean hasRoofAbove(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return false;
        }

        BlockPos base = getRoofScanBase(client);
        for (int i = 2; i <= 20; i++) {
            if (!client.level.getBlockState(base.above(i)).isAir()) {
                return true;
            }
        }
        return false;
    }

    public static BlockPos getRoofScanBase(Minecraft client) {
        return BlockPos.containing(
                client.player.getX(),
                client.player.getY() - 0.3,
                client.player.getZ());
    }

    public static void performAotvToRoof(Minecraft client) throws InterruptedException {
        if (client.player == null || client.gameMode == null) return;

        // Pre-check: ensure there is a roof (non-air block) above within 2..20 blocks.
        // If there's no roof, abort early to avoid firing AOTV into open sky.
        if (client.level != null && !hasRoofAbove(client)) {
            ClientUtils.sendDebugMessage(client, "PestAotv: no roof detected above player (2..20). Aborting AOTV.");
            isSneakingForAotv = false;
            // Ensure sneak key is released on the client thread
            client.execute(() -> {
                if (client.options != null) ClientUtils.setKeyMappingState(client.options.keyShift, false);
                // Advance the PestDestroyer state machine so the destroyer continues
                // after this aborted AOTV attempt.
                try {
                    PestDestroyer.completeRoofAotv();
                } catch (Throwable ignored) {
                }
            });
            return;
        }

        if (TfmConfig.BREAK_BLOCKS_BEFORE_AOTV.get()) {
            Vec3 breakTarget = Vec3.atCenterOf(client.player.blockPosition().above(2));
            client.execute(() -> ClientUtils.lookAt(client.player, breakTarget));
            ClientUtils.performAttackClick(client);
            Thread.sleep(100);
        }

        isSneakingForAotv = true;
        Vec3 eyePos = client.player.getEyePosition();
        float yawRad = (float) Math.toRadians(client.player.getYRot());
        int baseUpPitch = Math.max(20, Math.min(90, TfmConfig.AOTV_ROOF_PITCH.get()));
        int humanization = Math.max(0, Math.min(15, TfmConfig.AOTV_ROOF_PITCH_HUMANIZATION.get()));
        double randomizedUpPitch = baseUpPitch + ((Math.random() * 2.0) - 1.0) * humanization;
        randomizedUpPitch = Math.max(20.0, Math.min(90.0, randomizedUpPitch));
        float targetMcPitch = (float) -randomizedUpPitch;
        double pitchRad = Math.toRadians(targetMcPitch);

        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = -Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 targetPos = eyePos.add(dirX * 100.0, dirY * 100.0, dirZ * 100.0);
        int rotTime = (int) (TfmConfig.ROTATION_TIME.get() * (0.92 + Math.random() * 0.16));

        RotationManager.initiateRotation(client, targetPos, rotTime);
        ClientUtils.waitForRotationToComplete(client, targetMcPitch, rotTime);

        int aotvSlot = GearManager.findAspectOfTheVoidSlot(client);
        if (aotvSlot != -1 && aotvSlot < 9) {
            GearManager.swapToAOTVSync(client);

            // Capture Y on the main thread (via execute) so visibility is guaranteed,
            // then fire the normal use key path immediately after.
            ClientUtils.performUseClick(client, () -> {
                PestDestroyer.setAotvStartY(client.player.getY());
                ClientUtils.sendDebugMessage(client,
                        "[PestAotv] Firing AOTV (slot=" + aotvSlot + ", startY=" + String.format("%.2f", PestDestroyer.getAotvStartY()) + ")");
            });
            // Worker thread exits immediately - no sleep needed
            } else {
                ClientUtils.sendDebugMessage(client, "[PestAotv] No AOTV found in hotbar. Aborting AOTV sequence.");
                isSneakingForAotv = false;
                client.execute(() -> {
                    if (client.options != null) ClientUtils.setKeyMappingState(client.options.keyShift, false);
                    // Continue the destroyer flow on the client thread after abort.
                    try {
                        if (PestDestroyer.getVacuumSlot() < 0) {
                            PestDestroyer.setState(PestDestroyer.State.EQUIP_VACUUM);
                        } else {
                            PestDestroyer.setState(PestDestroyer.State.CHECK_NEXT);
                        }
                    } catch (Throwable ignored) {
                    }
                });
            }
    }
}
