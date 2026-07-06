package dev.typicalfarmingmacro.modules.pest.helpers;

import dev.typicalfarmingmacro.config.TfmConfig;

import dev.typicalfarmingmacro.macro.MacroState;
import dev.typicalfarmingmacro.macro.MacroWorkerThread;
import dev.typicalfarmingmacro.util.ClientUtils;

import net.minecraft.client.Minecraft;
import dev.typicalfarmingmacro.modules.gear.GearManager;
import dev.typicalfarmingmacro.modules.gear.helpers.BudgetAutopetManager;
import dev.typicalfarmingmacro.modules.gear.helpers.EquipmentManager;
import dev.typicalfarmingmacro.modules.gear.helpers.RodManager;
import dev.typicalfarmingmacro.modules.gear.helpers.WardrobeManager;
import dev.typicalfarmingmacro.modules.pest.PestManager;

public class PestPrepSwapManager {
    public static volatile boolean prepSwappedForCurrentPestCycle = false;
    public static volatile boolean isPrepSwapping = false;

    public static void resetState() {
        prepSwappedForCurrentPestCycle = false;
        isPrepSwapping = false;
    }

    public static void updatePrepSwapFlag(int cooldownSeconds, boolean isCleaningInProgress) {
        if (TfmConfig.AUTO_EQUIPMENT_PEST.get()) {
            if (cooldownSeconds > 170 && prepSwappedForCurrentPestCycle && !isCleaningInProgress) {
                prepSwappedForCurrentPestCycle = false;
            }
        } else {
            if (cooldownSeconds > 3 && prepSwappedForCurrentPestCycle && !isCleaningInProgress) {
                prepSwappedForCurrentPestCycle = false;
            }
        }
    }

    public static boolean shouldTriggerPrepSwap(MacroState.State currentState, int cooldownSeconds,
                                                boolean isCleaningInProgress, boolean isReturnToLocationActive) {
        if (currentState != MacroState.State.FARMING)
            return false;
        if (cooldownSeconds == -1 || cooldownSeconds < 0)
            return false;
        if (prepSwappedForCurrentPestCycle || isCleaningInProgress || isReturnToLocationActive)
            return false;
        if (!hasAnyPrepSwapTasksEnabled())
            return false;

        if (TfmConfig.AUTO_EQUIPMENT_PEST.get()) {
            return cooldownSeconds <= 170;
        } else {
            return cooldownSeconds <= 3;
        }
    }

    public static void triggerPrepSwap(Minecraft client) {
        prepSwappedForCurrentPestCycle = true;
        isPrepSwapping = true;
        ClientUtils.sendDebugMessage(client, "Pest cooldown detected. Triggering prep-swap...");
        MacroWorkerThread.getInstance().submit("PrepSwap", () -> {
            try {
                if (shouldAbortPrepSwap(client))
                    return;
                ClientUtils.sendDebugMessage(client, "Disabling farming macro: Triggering prep-swap");
                client.execute(() -> dev.typicalfarmingmacro.macro.FarmingMacroManager.disable(client));
                MacroWorkerThread.sleep(400);
                if (shouldAbortPrepSwap(client))
                    return;

                if (!runPrepWardrobeSwap(client))
                    return;
                if (!runPrepEquipmentSwap(client))
                    return;

                if (TfmConfig.AUTO_ROD_PEST_CD.get()) {
                    RodManager.executeRodSequence(client);
                }
                if (TfmConfig.AUTO_PET_PEST_CD.get()) {
                    BudgetAutopetManager.equipPetByName(client,
                            TfmConfig.AUTO_PET_PEST_CD_PET.get(),
                            "pest cooldown");
                }

                if (!PestManager.isCleaningInProgress) {
                    GearManager.finalResume(client);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isPrepSwapping = false;
            }
        });
    }

    private static boolean hasAnyPrepSwapTasksEnabled() {
        return TfmConfig.AUTO_WARDROBE_PEST.get()
                || TfmConfig.AUTO_EQUIPMENT_PEST.get()
                || TfmConfig.AUTO_ROD_PEST_CD.get()
                || TfmConfig.AUTO_PET_PEST_CD.get();
    }

    private static boolean shouldAbortPrepSwap(Minecraft client) {
        if (MacroWorkerThread.shouldAbortTask(client, MacroState.State.FARMING) || PestManager.isCleaningInProgress) {
            if (dev.typicalfarmingmacro.macro.MacroStateManager.getCurrentState() != MacroState.State.FARMING || !dev.typicalfarmingmacro.macro.MacroStateManager.isMacroRunning()) {
                prepSwappedForCurrentPestCycle = false;
            }
            return true;
        }
        return false;
    }

    /**
     * Abort check for use during the equipment-swap phase, where the macro state is
     * legitimately EQUIPMENT (not FARMING). Skips the state check so we don't
     * incorrectly abort and reset prepSwappedForCurrentPestCycle.
     */
    private static boolean shouldAbortEquipmentPhase(Minecraft client) {
        if (MacroWorkerThread.shouldAbortTask(client) || PestManager.isCleaningInProgress) {
            if (dev.typicalfarmingmacro.macro.MacroStateManager.getCurrentState() != MacroState.State.FARMING || !dev.typicalfarmingmacro.macro.MacroStateManager.isMacroRunning()) {
                prepSwappedForCurrentPestCycle = false;
            }
            return true;
        }
        return false;
    }

    private static boolean runPrepWardrobeSwap(Minecraft client) throws InterruptedException {
        if (!TfmConfig.AUTO_WARDROBE_PEST.get() || TfmConfig.WARDROBE_SLOT_PEST.get() <= 0)
            return !shouldAbortPrepSwap(client);

        ClientUtils.sendDebugMessage(client,
                "Prep-swap: Initiating wardrobe swap to slot " + TfmConfig.WARDROBE_SLOT_PEST.get());
        GearManager.ensureWardrobeSlot(client, TfmConfig.WARDROBE_SLOT_PEST.get());
        if (!WardrobeManager.isSwappingWardrobe) {
            ClientUtils.sendDebugMessage(client, "Prep-swap: Wardrobe swap not needed (already on correct slot).");
            return !shouldAbortPrepSwap(client);
        }

        ClientUtils.sendDebugMessage(client, "Prep-swap: Waiting for wardrobe GUI...");
        ClientUtils.waitForWardrobeGui(client);
        if (!WardrobeManager.wardrobeGuiDetected) {
            ClientUtils.sendDebugMessage(client, "§cPrep-swap: Wardrobe GUI not detected! Retrying in 1 second...");
            MacroWorkerThread.sleep(1000);
            if (shouldAbortPrepSwap(client))
                return false;

            GearManager.ensureWardrobeSlot(client, TfmConfig.WARDROBE_SLOT_PEST.get());
            if (WardrobeManager.isSwappingWardrobe) {
                ClientUtils.sendDebugMessage(client, "Prep-swap: Retry - Waiting for wardrobe GUI...");
                ClientUtils.waitForWardrobeGui(client);
                if (!WardrobeManager.wardrobeGuiDetected) {
                    ClientUtils.sendDebugMessage(client,
                            "§cPrep-swap: Wardrobe GUI still not detected after retry! Aborting prep-swap.");
                    prepSwappedForCurrentPestCycle = false;
                    return false;
                }
            }
        }

        while (WardrobeManager.isSwappingWardrobe && !PestManager.isCleaningInProgress) {
            MacroWorkerThread.sleep(50);
        }
        MacroWorkerThread.sleep(250);
        if (shouldAbortPrepSwap(client))
            return false;

        ClientUtils.sendDebugMessage(client, "Prep-swap: Wardrobe swap completed.");
        return true;
    }

    private static boolean runPrepEquipmentSwap(Minecraft client) throws InterruptedException {
        if (!TfmConfig.AUTO_EQUIPMENT_PEST.get())
            return !shouldAbortPrepSwap(client);

        ClientUtils.sendDebugMessage(client, "Prep-swap: Initiating equipment swap to pest gear");
        GearManager.ensureEquipment(client, false);
        MacroWorkerThread.sleep(200);
        if (shouldAbortEquipmentPhase(client))
            return false;

        ClientUtils.sendDebugMessage(client, "Prep-swap: Waiting for equipment GUI...");
        ClientUtils.waitForEquipmentGui(client);
        if (!EquipmentManager.equipmentGuiDetected) {
            ClientUtils.sendDebugMessage(client,
                    "§cPrep-swap: Equipment GUI not detected! Retrying in 1 second...");
            MacroWorkerThread.sleep(1000);
            if (shouldAbortEquipmentPhase(client))
                return false;

            GearManager.ensureEquipment(client, false);
            MacroWorkerThread.sleep(200);
            ClientUtils.sendDebugMessage(client, "Prep-swap: Retry - Waiting for equipment GUI...");
            ClientUtils.waitForEquipmentGui(client);
            if (!EquipmentManager.equipmentGuiDetected) {
                ClientUtils.sendDebugMessage(client,
                        "§cPrep-swap: Equipment GUI still not detected after retry! Aborting prep-swap.");
                prepSwappedForCurrentPestCycle = false;
                return false;
            }
        }

        while (EquipmentManager.isSwappingEquipment && !PestManager.isCleaningInProgress) {
            MacroWorkerThread.sleep(50);
        }
        while (client.screen != null && !PestManager.isCleaningInProgress) {
            MacroWorkerThread.sleep(50);
        }
        MacroWorkerThread.sleep(250);
        if (shouldAbortEquipmentPhase(client))
            return false;

        ClientUtils.sendDebugMessage(client, "Prep-swap: Equipment swap completed.");
        return true;
    }
}
