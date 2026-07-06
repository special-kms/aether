package dev.typicalfarmingmacro.modules.pest.helpers;

import dev.typicalfarmingmacro.config.TfmConfig;

import dev.typicalfarmingmacro.macro.MacroState;
import dev.typicalfarmingmacro.macro.MacroWorkerThread;
import dev.typicalfarmingmacro.modules.gear.GearManager;
import dev.typicalfarmingmacro.modules.gear.helpers.BudgetAutopetManager;
import dev.typicalfarmingmacro.modules.gear.helpers.EquipmentManager;
import dev.typicalfarmingmacro.modules.gear.helpers.WardrobeManager;
import dev.typicalfarmingmacro.modules.gear.helpers.RodManager;
import dev.typicalfarmingmacro.modules.pest.PestManager;
import dev.typicalfarmingmacro.util.ClientUtils;

import net.minecraft.client.Minecraft;

public class PestCleaningSequencer {

    public static void startCleaningSequence(Minecraft client, String plot, String currentInfestedPlot,
            int currentPestSessionId) {
        if (PestManager.isCleaningInProgress || WardrobeManager.isSwappingWardrobe
                || EquipmentManager.isSwappingEquipment) {
            PestManager.clearCleaningTriggerPending();
            return;
        }

        ClientUtils.sendDebugMessage(client,
                "Disabling farming macro: Pest threshold reached, starting cleaning sequence for plot " + plot);
        client.execute(() -> dev.typicalfarmingmacro.macro.FarmingMacroManager.disable(client));
        PestManager.isCleaningInProgress = true;
        PestManager.clearCleaningTriggerPending();
        WardrobeManager.shouldRestartFarmingAfterSwap = false;
        dev.typicalfarmingmacro.macro.MacroStateManager.setCurrentState(dev.typicalfarmingmacro.macro.MacroState.State.CLEANING);
        final int sessionId = currentPestSessionId;

        MacroWorkerThread.getInstance().submit("CleaningSequence-" + plot, () -> {
            try {
                // Set spawn with 10s timeout (increased in CommandUtils)
                if (MacroWorkerThread.shouldAbortTask(client))
                    return;
                if (!dev.typicalfarmingmacro.util.CommandUtils.setSpawn(client)) {
                    dev.typicalfarmingmacro.util.ClientUtils.sendMessage(client, 
                                    "§c[Tfm] /setspawn timed out - aborting pest cleaning to prevent roof spawn.", false);
                    PestManager.isCleaningInProgress = false;
                    dev.typicalfarmingmacro.macro.MacroStateManager.setCurrentState(MacroState.State.FARMING);
                    return;
                }
                if (MacroWorkerThread.shouldAbortTask(client))
                    return;
                if (sessionId != PestManager.currentPestSessionId)
                    return;



                boolean deferLoadoutUntilAfterDiscoTeleport = PestDiscoDestinationManager.matchesPlot(currentInfestedPlot);

                if (!deferLoadoutUntilAfterDiscoTeleport && !restoreGearForCleaning(client))
                    return;

                PestPrepSwapManager.prepSwappedForCurrentPestCycle = false;
                dev.typicalfarmingmacro.util.ClientUtils.sendMessage(client, "§6Starting Pest Cleaner script (" + currentInfestedPlot + ")...", true);
                if (MacroWorkerThread.shouldAbortTask(client))
                    return;

                if (!deferLoadoutUntilAfterDiscoTeleport) {
                    triggerPestSpawnActions(client);
                }

                if (PestDiscoDestinationManager.isUsablePlot(currentInfestedPlot)) {
                    String currentPlot = ClientUtils.getCurrentPlot(client);
                    boolean scribePlotMatch = currentPlot != null && currentPlot.equalsIgnoreCase(currentInfestedPlot);
                    String freshChatPlot = dev.typicalfarmingmacro.util.CommandUtils.getFreshKnownPlotChat();
                    boolean chatPlotMatch = freshChatPlot != null && freshChatPlot.equalsIgnoreCase(currentInfestedPlot);
                    boolean alreadyOnPlot = scribePlotMatch;
                    if (!alreadyOnPlot && (currentPlot == null || currentPlot.equalsIgnoreCase("Unknown"))) {
                        alreadyOnPlot = chatPlotMatch;
                    }

                    boolean forcePlotTpForCurrentPlot = alreadyOnPlot
                            && TfmConfig.PEST_PLOT_TP_FOR_CURRENT_PLOT.get();
                    if (alreadyOnPlot && PestDiscoDestinationManager.matchesPlot(currentInfestedPlot)) {
                        forcePlotTpForCurrentPlot = true;
                    }

                    if (alreadyOnPlot && !forcePlotTpForCurrentPlot) {
                        String source = chatPlotMatch ? "chat" : "scoreboard";
                        ClientUtils.sendDebugMessage(client, "Already on plot " + currentInfestedPlot + " (via " + source + "), skipping plottp.");
                    } else if (forcePlotTpForCurrentPlot) {
                        ClientUtils.sendDebugMessage(client,
                                "Already on plot " + currentInfestedPlot + ", but forcing plottp before starting destroyer.");
                    } else {
                        ClientUtils.sendDebugMessage(client, "Arriving at plot " + currentInfestedPlot + " before starting destroyer. (Score: " + currentPlot + ", FreshChat: " + (freshChatPlot != null ? freshChatPlot : "stale") + ")");
                    }

                    if (!alreadyOnPlot || forcePlotTpForCurrentPlot) {
                        dev.typicalfarmingmacro.util.CommandUtils.plotTp(client, currentInfestedPlot);
                        MacroWorkerThread.sleep(200); // Wait for world load / stable position
                    }
                }

                if (deferLoadoutUntilAfterDiscoTeleport) {
                    ClientUtils.sendDebugMessage(client,
                            "Disco destination active: restoring pest loadout after plot teleport.");
                    if (!restoreGearForCleaning(client))
                        return;
                    triggerPestSpawnActions(client);
                }

                if (PestBonusManager.isBonusInactive) {
                    dev.typicalfarmingmacro.util.ClientUtils.sendMessage(client, "§dBonus is INACTIVE! Triggering Phillip reactivation...", true);
                    PestBonusManager.isReactivatingBonus = true;
                    ClientUtils.sendDebugMessage(client, "Using native Pest Destroyer for reactivation.");
                    client.execute(() -> PestDestroyer.start(client, plot));
                    return;
                }

                startPestCleanerScript(client, currentInfestedPlot);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static boolean restoreGearForCleaning(Minecraft client) throws InterruptedException {
        if (TfmConfig.AUTO_WARDROBE_PEST.get()) {
            int targetSlot = TfmConfig.WARDROBE_SLOT_FARMING.get();
            if ((PestPrepSwapManager.prepSwappedForCurrentPestCycle
                    || WardrobeManager.trackedWardrobeSlot != targetSlot)
                    && targetSlot > 0) {
                dev.typicalfarmingmacro.util.ClientUtils.sendMessage(client, "§eRestoring Farming Wardrobe (Slot " + targetSlot + ") for Vacuuming...", true);
                client.execute(() -> GearManager.ensureWardrobeSlot(client, targetSlot));

                // client.execute is async; wait for swap state to actually start so later waits
                // are not skipped.
                long wardrobeStartWait = System.currentTimeMillis();
                while (!WardrobeManager.isSwappingWardrobe && System.currentTimeMillis() - wardrobeStartWait < 2000) {
                    if (MacroWorkerThread.shouldAbortTask(client))
                        return false;
                    MacroWorkerThread.sleep(25);
                }

                ClientUtils.waitForWardrobeGui(client);
                long wardrobeFinishWait = System.currentTimeMillis();
                while (WardrobeManager.isSwappingWardrobe && System.currentTimeMillis() - wardrobeFinishWait < 7000)
                    MacroWorkerThread.sleep(50);

                if (WardrobeManager.isSwappingWardrobe) {
                    ClientUtils.sendDebugMessage(client,
                            "§eWardrobe swap wait timeout in cleaning sequence. Triggering failsafe completion.");
                    WardrobeManager.forceWardrobeCompletionFailsafe(client);
                }

                while (WardrobeManager.wardrobeCleanupTicks > 0)
                    MacroWorkerThread.sleep(50);
                MacroWorkerThread.sleep(250);
                if (MacroWorkerThread.shouldAbortTask(client))
                    return false;
            }
        }

        if (TfmConfig.AUTO_EQUIPMENT_PEST.get()) {
            GearManager.ensureEquipment(client, true);

            long equipmentStartWait = System.currentTimeMillis();
            while (!EquipmentManager.isSwappingEquipment && System.currentTimeMillis() - equipmentStartWait < 2000) {
                if (MacroWorkerThread.shouldAbortTask(client))
                    return false;
                MacroWorkerThread.sleep(25);
            }

            ClientUtils.waitForEquipmentGui(client);
            long equipmentFinishWait = System.currentTimeMillis();
            while (EquipmentManager.isSwappingEquipment && System.currentTimeMillis() - equipmentFinishWait < 7000)
                MacroWorkerThread.sleep(50);

            if (EquipmentManager.isSwappingEquipment) {
                ClientUtils.sendDebugMessage(client,
                        "§eEquipment swap wait timeout in cleaning sequence. Resetting equipment state.");
                EquipmentManager.resetState();
            }

            MacroWorkerThread.sleep(250);
            if (MacroWorkerThread.shouldAbortTask(client))
                return false;
        }
        return true;
    }

    private static void triggerPestSpawnActions(Minecraft client) {
        if (TfmConfig.AUTO_ROD_PEST_SPAWN.get()) {
            ClientUtils.sendDebugMessage(client, "Auto Rod: Triggering rod cast on pest spawn.");
            RodManager.executeRodSequence(client);
        }
        if (TfmConfig.AUTO_PET_PEST_SPAWN.get()) {
            ClientUtils.sendDebugMessage(client, "BudgetAutopet: Triggering pet equip on pest spawn.");
            try {
                BudgetAutopetManager.equipPetByName(client,
                        TfmConfig.AUTO_PET_PEST_SPAWN_PET.get(),
                        "pest spawn");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void startPestCleanerScript(Minecraft client, String currentInfestedPlot) {
        ClientUtils.sendDebugMessage(client, "Ready to start pest cleaner");

        ClientUtils.sendDebugMessage(client, "Starting pest cleaner for plot " + currentInfestedPlot);

        ClientUtils.sendDebugMessage(client, "Using native Pest Destroyer.");
        client.execute(() -> PestDestroyer.start(client, currentInfestedPlot));
    }
}


