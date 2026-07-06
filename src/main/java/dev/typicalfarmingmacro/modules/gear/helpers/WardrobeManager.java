package dev.typicalfarmingmacro.modules.gear.helpers;

import dev.typicalfarmingmacro.macro.MacroWorkerThread;
import dev.typicalfarmingmacro.modules.gear.GearManager;
import dev.typicalfarmingmacro.modules.pest.PestManager;
import dev.typicalfarmingmacro.modules.pest.helpers.AutoPestExchangeManager;
import dev.typicalfarmingmacro.modules.session.RestartManager;
import dev.typicalfarmingmacro.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WardrobeManager {
    public static volatile boolean isSwappingWardrobe = false;
    public static volatile long wardrobeInteractionTime = 0;
    public static volatile int wardrobeInteractionStage = 0;
    public static volatile int wardrobeCleanupTicks = 0;
    public static volatile int trackedWardrobeSlot = -1;
    public static volatile int targetWardrobeSlot = -1;
    public static volatile boolean shouldRestartFarmingAfterSwap = false;
    public static volatile long wardrobeOpenPendingTime = 0;
    public static volatile boolean wardrobeGuiDetected = false;
    public static volatile boolean wardrobeDataLoaded = false;
    public static volatile long wardrobeTimelineStartTime = 0;

    public static void resetState() {
        isSwappingWardrobe = false;
        shouldRestartFarmingAfterSwap = false;
        wardrobeCleanupTicks = 0;
        trackedWardrobeSlot = -1;
        targetWardrobeSlot = -1;
        wardrobeInteractionTime = 0;
        wardrobeInteractionStage = 0;
        wardrobeGuiDetected = false;
        wardrobeDataLoaded = false;
        wardrobeOpenPendingTime = 0;
        wardrobeTimelineStartTime = 0;
    }

    public static void triggerWardrobeSwap(Minecraft client, int slot) {
        if (trackedWardrobeSlot == slot) {
            ClientUtils.sendDebugMessage(client, "Wardrobe already on target slot, restarting farming");
            client.execute(() -> dev.typicalfarmingmacro.macro.FarmingMacroManager.disable(client));
            MacroWorkerThread.getInstance().submit("Wardrobe-AlreadyOnSlot-FastResume", () -> {
                if (MacroWorkerThread.shouldAbortTask(client, dev.typicalfarmingmacro.macro.MacroState.State.FARMING)) {
                    return;
                }
                MacroWorkerThread.sleep(400);
                if (MacroWorkerThread.shouldAbortTask(client, dev.typicalfarmingmacro.macro.MacroState.State.FARMING)) {
                    return;
                }
                if (AutoPestExchangeManager.shouldBlockFarmingResume()) {
                    ClientUtils.sendDebugMessage(client, "Wardrobe resume deferred because pest exchange has priority.");
                    return;
                }
                client.execute(() -> GearManager.swapToFarmingTool(client));
                MacroWorkerThread.sleep(250);
                if (MacroWorkerThread.shouldAbortTask(client, dev.typicalfarmingmacro.macro.MacroState.State.FARMING)) {
                    return;
                }
                if (AutoPestExchangeManager.shouldBlockFarmingResume()) {
                    ClientUtils.sendDebugMessage(client, "Wardrobe resume deferred because pest exchange has priority.");
                    return;
                }
                ClientUtils.sendDebugMessage(client, "Restarting farming macro after wardrobe swap");
                client.execute(() -> dev.typicalfarmingmacro.macro.FarmingMacroManager.enable(client,
                        dev.typicalfarmingmacro.macro.FarmingMacroManager.createMacroFromConfig()));
            });
            return;
        }

        targetWardrobeSlot = slot;
        isSwappingWardrobe = true;
        if (EquipmentManager.isSwappingEquipment) {
            EquipmentManager.isSwappingEquipment = false;
            ClientUtils.sendDebugMessage(client, "Interrupted equipment swap for wardrobe priority.");
        }
        wardrobeGuiDetected = false;
        wardrobeDataLoaded = false;
        wardrobeInteractionTime = 0;
        wardrobeInteractionStage = 0;
        wardrobeTimelineStartTime = 0;
        shouldRestartFarmingAfterSwap = true;
        dev.typicalfarmingmacro.macro.MacroStateManager.setCurrentState(dev.typicalfarmingmacro.macro.MacroState.State.WARDROBE);
        ClientUtils.sendDebugMessage(client, "Triggering wardrobe swap to slot " + slot);
        client.execute(() -> dev.typicalfarmingmacro.macro.FarmingMacroManager.disable(client));
        MacroWorkerThread.getInstance().submit("Wardrobe-OpenGui", () -> {
            if (MacroWorkerThread.shouldAbortTask(client)) {
                return;
            }
            MacroWorkerThread.sleep(375);
            if (MacroWorkerThread.shouldAbortTask(client)) {
                return;
            }
            client.execute(() -> ClientUtils.sendCommand(client, "/wardrobe"));
            ClientUtils.waitForWardrobeGui(client);
        });
    }

    public static void ensureWardrobeSlot(Minecraft client, int slot) {
        if (trackedWardrobeSlot == slot) {
            return;
        }
        targetWardrobeSlot = slot;
        isSwappingWardrobe = true;
        if (EquipmentManager.isSwappingEquipment) {
            EquipmentManager.isSwappingEquipment = false;
            ClientUtils.sendDebugMessage(client, "Interrupted equipment swap for wardrobe priority.");
        }
        wardrobeGuiDetected = false;
        wardrobeDataLoaded = false;
        wardrobeInteractionTime = 0;
        wardrobeInteractionStage = 0;
        wardrobeTimelineStartTime = 0;
        ClientUtils.sendCommand(client, "/wardrobe");
    }

    public static void abortSwapForPriorityTask(Minecraft client, String taskName) {
        if (!isSwappingWardrobe) {
            return;
        }

        isSwappingWardrobe = false;
        shouldRestartFarmingAfterSwap = false;
        wardrobeGuiDetected = false;
        wardrobeDataLoaded = false;
        wardrobeInteractionTime = 0;
        wardrobeInteractionStage = 0;
        wardrobeTimelineStartTime = 0;

        if (dev.typicalfarmingmacro.macro.MacroStateManager.getCurrentState() == dev.typicalfarmingmacro.macro.MacroState.State.WARDROBE) {
            dev.typicalfarmingmacro.macro.MacroStateManager.setCurrentState(dev.typicalfarmingmacro.macro.MacroState.State.FARMING);
        }

        ClientUtils.sendDebugMessage(client, "Aborted wardrobe swap because " + taskName + " has priority.");
        if (client.player != null) {
            client.execute(() -> client.player.closeContainer());
        }
    }

    public static void handleWardrobeMenu(Minecraft client, AbstractContainerScreen<?> screen) {
        if (!isSwappingWardrobe || targetWardrobeSlot == -1) {
            return;
        }

        String title = screen.getTitle().getString().toLowerCase();
        if (!title.contains("wardrobe")) {
            return;
        }

        long now = System.currentTimeMillis();

        if (!wardrobeGuiDetected) {
            wardrobeGuiDetected = true;
            wardrobeTimelineStartTime = now;
            sendTimedDebug(client, "Wardrobe GUI opened", now);
        }

        int slotIdx = 35 + targetWardrobeSlot;
        if (slotIdx >= screen.getMenu().slots.size()) {
            return;
        }

        Slot slot = screen.getMenu().slots.get(slotIdx);
        ItemStack stack = slot.getItem();

        if (stack.isEmpty() || stack.getItem().toString().toLowerCase().contains("air")
                || stack.getItem().toString().toLowerCase().contains("gray_dye")
                || stack.getHoverName().getString().toLowerCase().contains("gray dye")) {
            return;
        }

        if (!wardrobeDataLoaded) {
            wardrobeDataLoaded = true;
            wardrobeInteractionTime = now;
            sendTimedDebug(client, "Wardrobe slot data loaded for slot " + targetWardrobeSlot, now);
        }

        if (now - wardrobeInteractionTime < ClientUtils.getGuiClickDelayMs(wardrobeInteractionStage == 0)) {
            return;
        }

        if (wardrobeInteractionStage == 0) {
            String itemName = stack.getItem().toString().toLowerCase();
            String hoverName = stack.getHoverName().getString().toLowerCase();

            if (itemName.contains("green_dye") || hoverName.contains("green dye") || itemName.contains("lime_dye")
                    || hoverName.contains("lime dye")) {
                ClientUtils.sendMessage(client, "\u00A7aWardrobe slot " + targetWardrobeSlot + " is already active.", true);
                trackedWardrobeSlot = targetWardrobeSlot;
                isSwappingWardrobe = false;
                if (client.player != null) {
                    sendTimedDebug(client, "Wardrobe GUI close requested", now);
                    client.player.closeContainer();
                }
                sendTimedDebug(client, "Wardrobe slot " + targetWardrobeSlot + " already active. Skipping swap", now);
                handleWardrobeCompletion(client);
                return;
            }

            sendTimedDebug(client,
                    "Clicked wardrobe slot " + targetWardrobeSlot + " (" + stack.getHoverName().getString() + ")",
                    now);
            ClientUtils.performSlotClick(client, screen, slot.index, 0, ContainerInput.PICKUP);
            wardrobeInteractionTime = now;
            wardrobeInteractionStage = 1;
        } else if (wardrobeInteractionStage == 1) {
            long lastClickElapsed = now - wardrobeInteractionTime;
            if (lastClickElapsed < 150) {
                return;
            }

            int confirmSlotIdx = 35 + targetWardrobeSlot;
            if (confirmSlotIdx >= screen.getMenu().slots.size()) {
                return;
            }

            ItemStack confirmStack = screen.getMenu().slots.get(confirmSlotIdx).getItem();
            if (confirmStack.isEmpty()) {
                return;
            }

            String itemName = confirmStack.getItem().toString().toLowerCase();
            String hoverName = confirmStack.getHoverName().getString().toLowerCase();

            if (itemName.contains("green_dye") || hoverName.contains("green dye") || itemName.contains("lime_dye")
                    || hoverName.contains("lime dye")) {
                sendTimedDebug(client, "Confirmed wardrobe slot " + targetWardrobeSlot + " is active", now);
                trackedWardrobeSlot = targetWardrobeSlot;
                isSwappingWardrobe = false;
                if (client.player != null) {
                    sendTimedDebug(client, "Wardrobe GUI close requested", now);
                    client.player.closeContainer();
                }
                wardrobeInteractionTime = now;
                wardrobeInteractionStage = 2;
            }
        } else if (wardrobeInteractionStage == 2) {
            long lastClickElapsed = now - wardrobeInteractionTime;
            if (lastClickElapsed < 250) {
                return;
            }
            sendTimedDebug(client, "Wardrobe swap complete. Active slot is now " + trackedWardrobeSlot
                    + " (target was " + targetWardrobeSlot + ")", now);
            handleWardrobeCompletion(client);
            wardrobeInteractionStage = 0;
        }
    }

    private static void handleWardrobeCompletion(Minecraft client) {
        RestartManager.onWardrobeSwapCompleted(client);

        if (!shouldRestartFarmingAfterSwap) {
            return;
        }

        shouldRestartFarmingAfterSwap = false;

        if (dev.typicalfarmingmacro.macro.MacroStateManager.getCurrentState() == dev.typicalfarmingmacro.macro.MacroState.State.WARDROBE) {
            dev.typicalfarmingmacro.macro.MacroStateManager.setCurrentState(dev.typicalfarmingmacro.macro.MacroState.State.FARMING);
        }

        if (PestManager.isCleaningInProgress) {
            ClientUtils.sendMessage(client, "\u00A7aWardrobe swap finished. Cleaning in progress, skipping restart.", true);
            return;
        }

        if (AutoPestExchangeManager.shouldBlockFarmingResume()) {
            ClientUtils.sendDebugMessage(client, "Wardrobe completion deferred because pest exchange has priority.");
            return;
        }

        ClientUtils.sendMessage(client, "\u00A7aWardrobe swap finished. Restarting farming...", true);
        client.execute(() -> GearManager.swapToFarmingTool(client));
        MacroWorkerThread.getInstance().submit("WardrobeCompletion-Resume", () -> {
            if (MacroWorkerThread.shouldAbortTask(client, dev.typicalfarmingmacro.macro.MacroState.State.FARMING)) {
                return;
            }
            if (PestManager.isCleaningInProgress) {
                return;
            }
            if (AutoPestExchangeManager.shouldBlockFarmingResume()) {
                return;
            }

            GearManager.finalResume(client);
        });
    }

    public static void forceWardrobeCompletionFailsafe(Minecraft client) {
        if (isSwappingWardrobe && shouldRestartFarmingAfterSwap) {
            ClientUtils.sendDebugMessage(client, "Wardrobe swap failsafe triggered. Forcing completion.");
            trackedWardrobeSlot = targetWardrobeSlot;
            isSwappingWardrobe = false;
            wardrobeGuiDetected = false;
            wardrobeDataLoaded = false;
            handleWardrobeCompletion(client);
        }
    }

    private static void sendTimedDebug(Minecraft client, String action, long now) {
        ClientUtils.sendDebugMessage(client,
                action + " at " + ClientUtils.formatElapsedMs(wardrobeTimelineStartTime, now) + ".");
    }
}
