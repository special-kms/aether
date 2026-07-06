package dev.typicalfarmingmacro.modules.gear.helpers;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.macro.MacroWorkerThread;
import dev.typicalfarmingmacro.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EquipmentManager {
    private static final int[] GUI_SLOTS = { 10, 19, 28, 37 };
    private static final String[] EQUIPMENT_KEYWORDS = { "necklace", "cloak|vest|cape", "belt",
            "gloves|bracelet|gauntlet" };
    private static final long EQUIPMENT_CONFIRMATION_TIMEOUT_MS = 500L;
    private static final boolean[] EQUIPMENT_CONFIRMATION_PENDING = new boolean[GUI_SLOTS.length];
    private static final int[] EQUIPMENT_CONFIRMATION_PRIORITY = new int[GUI_SLOTS.length];
    private static final long[] EQUIPMENT_CONFIRMATION_START = new long[GUI_SLOTS.length];

    public enum EquipmentSwapTarget {
        FARMING,
        PEST,
        VISITOR_LUNAR,
        VISITOR_PREVIOUS,
        FARMING_WITH_PEST_FALLBACK
    }

    public static volatile boolean isSwappingEquipment = false;
    public static volatile int equipmentInteractionStage = 0;
    public static volatile long equipmentInteractionTime = 0;
    public static volatile EquipmentSwapTarget swapTarget = EquipmentSwapTarget.PEST;
    public static volatile int equipmentTargetIndex = 0;
    public static volatile boolean equipmentGuiDetected = false;
    public static volatile boolean equipmentFirstClickPending = false;
    public static volatile long equipmentTimelineStartTime = 0;
    public static volatile boolean shouldRestartFarmingAfterSwap = false;
    private static final String[] VISITOR_RETURN_EQUIPMENT_SNAPSHOT = new String[GUI_SLOTS.length];
    private static volatile boolean shouldCaptureVisitorReturnSnapshot = false;
    private static volatile boolean visitorReturnSnapshotCaptured = false;

    public static void resetState() {
        isSwappingEquipment = false;
        equipmentInteractionStage = 0;
        equipmentInteractionTime = 0;
        swapTarget = EquipmentSwapTarget.PEST;
        equipmentTargetIndex = 0;
        equipmentGuiDetected = false;
        equipmentFirstClickPending = false;
        equipmentTimelineStartTime = 0;
        shouldRestartFarmingAfterSwap = false;
        clearVisitorReturnSnapshot();
        resetConfirmationState();
    }

    public static void ensureEquipment(Minecraft client, boolean toFarming) {
        ensureEquipment(client, toFarming ? EquipmentSwapTarget.FARMING : EquipmentSwapTarget.PEST);
    }

    public static void ensureEquipment(Minecraft client, EquipmentSwapTarget target) {
        if (!isTargetEnabled(target)) {
            return;
        }

        if (Minecraft.getInstance().isSameThread()) {
            if (WardrobeManager.isSwappingWardrobe) {
                ClientUtils.sendDebugMessage(client, "Equipment swap deferred because wardrobe is busy.");
                return;
            }
        } else {
            try {
                int timeout = 0;
                boolean waited = false;
                while (WardrobeManager.isSwappingWardrobe && timeout < 100) {
                    Thread.sleep(50);
                    timeout++;
                    waited = true;
                }
                if (WardrobeManager.isSwappingWardrobe) {
                    ClientUtils.sendDebugMessage(client, "Auto-equipment aborted because wardrobe swap timed out.");
                    return;
                }
                if (waited) {
                    ClientUtils.sendDebugMessage(client, "Wardrobe swap completed. Triggering equipment swap.");
                }
            } catch (InterruptedException ignored) {
            }
        }

        if (target == EquipmentSwapTarget.VISITOR_LUNAR) {
            clearVisitorReturnSnapshot();
            shouldCaptureVisitorReturnSnapshot = true;
        }

        swapTarget = target;
        equipmentGuiDetected = false;
        equipmentFirstClickPending = true;
        equipmentInteractionTime = 0;
        equipmentTimelineStartTime = 0;
        resetConfirmationState();
        isSwappingEquipment = true;
        if (dev.typicalfarmingmacro.macro.MacroStateManager.getCurrentState() == dev.typicalfarmingmacro.macro.MacroState.State.FARMING) {
            shouldRestartFarmingAfterSwap = true;
            dev.typicalfarmingmacro.macro.MacroStateManager.setCurrentState(dev.typicalfarmingmacro.macro.MacroState.State.EQUIPMENT);
            client.execute(() -> dev.typicalfarmingmacro.macro.FarmingMacroManager.disable(client));
        } else {
            shouldRestartFarmingAfterSwap = false;
        }
        equipmentInteractionStage = 0;
        equipmentTargetIndex = 0;
        ClientUtils.sendCommand(client, "/equipment");
    }

    public static void abortSwapForPriorityTask(Minecraft client, String taskName) {
        if (!isSwappingEquipment) {
            return;
        }

        isSwappingEquipment = false;
        equipmentGuiDetected = false;
        equipmentInteractionStage = 0;
        equipmentInteractionTime = 0;
        equipmentTargetIndex = 0;
        equipmentFirstClickPending = false;
        equipmentTimelineStartTime = 0;
        shouldRestartFarmingAfterSwap = false;
        resetConfirmationState();

        if (dev.typicalfarmingmacro.macro.MacroStateManager.getCurrentState() == dev.typicalfarmingmacro.macro.MacroState.State.EQUIPMENT) {
            dev.typicalfarmingmacro.macro.MacroStateManager.setCurrentState(dev.typicalfarmingmacro.macro.MacroState.State.FARMING);
        }

        ClientUtils.sendDebugMessage(client, "Aborted equipment swap because " + taskName + " has priority.");
        client.execute(() -> {
            if (client.screen != null) {
                client.screen.onClose();
            } else {
                client.setScreen(null);
            }
        });
    }

    public static void handleEquipmentMenu(Minecraft client, AbstractContainerScreen<?> screen) {
        if (!isSwappingEquipment) {
            return;
        }

        if (WardrobeManager.isSwappingWardrobe) {
            isSwappingEquipment = false;
            ClientUtils.sendDebugMessage(client, "Aborting equipment menu because wardrobe has priority.");
            return;
        }

        long now = System.currentTimeMillis();

        String title = screen.getTitle().getString().toLowerCase();
        if (!title.contains("equipment")) {
            return;
        }

        if (!areAllEquipmentSlotsLoaded(screen)) {
            ClientUtils.sendDebugMessage(client, "Equipment GUI is open but not all slot data is loaded yet.");
            return;
        }

        if (!equipmentGuiDetected) {
            equipmentGuiDetected = true;
            equipmentTimelineStartTime = now;
            sendTimedDebug(client, "Equipment GUI opened", now);
            if (shouldCaptureVisitorReturnSnapshot) {
                captureVisitorReturnSnapshot(client, screen);
            }
            equipmentInteractionTime = now;
            return;
        }

        if (now - equipmentInteractionTime < ClientUtils.getGuiClickDelayMs(equipmentFirstClickPending)) {
            return;
        }

        int totalSlots = screen.getMenu().slots.size();
        int playerInvStart = Math.max(0, totalSlots - 36);
        ItemStack carried = client.player.containerMenu.getCarried();
        processPendingConfirmations(client, screen, now);

        if (equipmentTargetIndex >= GUI_SLOTS.length) {
            if (hasPendingConfirmations()) {
                return;
            }
            if (hasUpgradeableEquipment(screen, playerInvStart, totalSlots)) {
                sendTimedDebug(client, "Equipment swap retrying because target items remain in inventory", now);
                equipmentTargetIndex = 0;
                equipmentInteractionStage = 0;
                equipmentInteractionTime = now;
                return;
            }

            sendTimedDebug(client, "Equipment swap complete", now);
            boolean completedVisitorRestore = swapTarget == EquipmentSwapTarget.VISITOR_PREVIOUS;
            isSwappingEquipment = false;
            equipmentFirstClickPending = false;
            boolean restartFarming = shouldRestartFarmingAfterSwap;
            shouldRestartFarmingAfterSwap = false;
            int containerId = screen.getMenu().containerId;
            client.setScreen(null);
            WardrobeManager.wardrobeCleanupTicks = 10;
            equipmentInteractionStage = 0;
            if (completedVisitorRestore) {
                clearVisitorReturnSnapshot();
            }
            sendTimedDebug(client, "Equipment GUI close requested", now);
            MacroWorkerThread.getInstance().submit("Equipment-CloseContainer", () -> {
                MacroWorkerThread.sleep(100);
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.closeContainer();
                    }
                });
                if (!restartFarming) {
                    if (dev.typicalfarmingmacro.macro.MacroStateManager.getCurrentState() == dev.typicalfarmingmacro.macro.MacroState.State.EQUIPMENT) {
                        dev.typicalfarmingmacro.macro.MacroStateManager.setCurrentState(dev.typicalfarmingmacro.macro.MacroState.State.FARMING);
                    }
                    return;
                }

                long closeWaitStart = System.currentTimeMillis();
                while (client.screen != null && System.currentTimeMillis() - closeWaitStart < 3000) {
                    MacroWorkerThread.sleep(50);
                }

                if (dev.typicalfarmingmacro.macro.MacroStateManager.getCurrentState() == dev.typicalfarmingmacro.macro.MacroState.State.EQUIPMENT) {
                    dev.typicalfarmingmacro.macro.MacroStateManager.setCurrentState(dev.typicalfarmingmacro.macro.MacroState.State.FARMING);
                }
                ClientUtils.sendDebugMessage(client, "Restarting farming macro after equipment swap");
                client.execute(() -> dev.typicalfarmingmacro.macro.FarmingMacroManager.enable(client,
                        dev.typicalfarmingmacro.macro.FarmingMacroManager.createMacroFromConfig()));
            });
            return;
        }

        if (equipmentInteractionStage == 0) {
            if (!carried.isEmpty()) {
                String carriedName = carried.getHoverName().getString().toLowerCase();
                int carriedSlotGroup = getSlotGroup(carriedName);
                if (carriedSlotGroup != -1) {
                    int carriedPriority = getTargetPriority(carriedName, carriedSlotGroup, swapTarget);
                    int currentPriority = getEquippedPriority(screen, carriedSlotGroup);
                    if (carriedPriority > currentPriority) {
                        ClientUtils.sendDebugMessage(client, "Equipment item " + carriedName
                                + " is stuck in cursor. Trying slot " + (carriedSlotGroup + 1) + ".");
                        equipmentTargetIndex = carriedSlotGroup;
                        equipmentInteractionStage = 1;
                        return;
                    }
                }

                ClientUtils.sendDebugMessage(client, "Equipment swap waiting for cursor to clear (" + carriedName + ").");
                return;
            }

            int equippedPriority = getEquippedPriority(screen, equipmentTargetIndex);
            int inventorySlotIndex = findBestInventorySlot(screen, playerInvStart, totalSlots, equipmentTargetIndex);
            int candidatePriority = getInventoryPriority(screen, inventorySlotIndex);

            if (equippedPriority >= candidatePriority && equippedPriority > 0) {
                sendTimedDebug(client,
                        "Equipment slot " + (equipmentTargetIndex + 1) + " confirmed with correct gear", now);
                equipmentTargetIndex++;
                equipmentInteractionTime = now;
                return;
            }

            if (inventorySlotIndex != -1) {
                Slot inventorySlot = screen.getMenu().slots.get(inventorySlotIndex);
                String invItemName = inventorySlot.getItem().getHoverName().getString().toLowerCase();
                sendTimedDebug(client, "Clicked equipment item for slot " + (equipmentTargetIndex + 1)
                        + ": " + invItemName, now);
                equipmentFirstClickPending = false;
                ClientUtils.performSlotClick(client, screen, inventorySlot.index, 0, ContainerInput.PICKUP);
                equipmentInteractionTime = now;
                equipmentInteractionStage = 1;
                return;
            }

            equipmentTargetIndex++;
            equipmentInteractionTime = now;
            return;
        }

        if (equipmentInteractionStage == 1) {
            if (carried.isEmpty()) {
                ClientUtils.sendDebugMessage(client, "Equipment swap cursor was empty in stage 1. Resetting search.");
                equipmentInteractionStage = 0;
                return;
            }

            int carriedPriority = getTargetPriority(
                    carried.getHoverName().getString().toLowerCase(),
                    equipmentTargetIndex,
                    swapTarget);
            int equippedPriority = getEquippedPriority(screen, equipmentTargetIndex);
            if (carriedPriority <= equippedPriority) {
                ClientUtils.sendDebugMessage(client,
                        "Equipment swap found better gear already equipped for slot " + (equipmentTargetIndex + 1)
                                + ". Resetting search.");
                equipmentInteractionStage = 0;
                equipmentInteractionTime = now;
                return;
            }

            int gearSlotIdx = GUI_SLOTS[equipmentTargetIndex];
            sendTimedDebug(client,
                    "Clicked equipment slot " + (equipmentTargetIndex + 1) + " with "
                            + carried.getHoverName().getString(),
                    now);
            equipmentFirstClickPending = false;
            EQUIPMENT_CONFIRMATION_PENDING[equipmentTargetIndex] = true;
            EQUIPMENT_CONFIRMATION_PRIORITY[equipmentTargetIndex] = carriedPriority;
            EQUIPMENT_CONFIRMATION_START[equipmentTargetIndex] = now;
            ClientUtils.performSlotClick(client, screen, gearSlotIdx, 0, ContainerInput.PICKUP);
            equipmentInteractionTime = now;
            equipmentInteractionStage = 0;
            equipmentTargetIndex++;
        }
    }

    private static void sendTimedDebug(Minecraft client, String action, long now) {
        ClientUtils.sendDebugMessage(client,
                action + " at " + ClientUtils.formatElapsedMs(equipmentTimelineStartTime, now) + ".");
    }

    private static void processPendingConfirmations(Minecraft client, AbstractContainerScreen<?> screen, long now) {
        for (int slotGroup = 0; slotGroup < EQUIPMENT_CONFIRMATION_PENDING.length; slotGroup++) {
            if (!EQUIPMENT_CONFIRMATION_PENDING[slotGroup]) {
                continue;
            }

            int expectedPriority = EQUIPMENT_CONFIRMATION_PRIORITY[slotGroup];
            if (expectedPriority > 0 && getEquippedPriority(screen, slotGroup) >= expectedPriority) {
                sendTimedDebug(client, "Confirmed equipment slot " + (slotGroup + 1) + " equipped", now);
                clearPendingConfirmation(slotGroup);
                continue;
            }

            if (now - EQUIPMENT_CONFIRMATION_START[slotGroup] >= EQUIPMENT_CONFIRMATION_TIMEOUT_MS) {
                sendTimedDebug(client, "Equipment slot " + (slotGroup + 1) + " confirmation timed out", now);
                clearPendingConfirmation(slotGroup);
            }
        }
    }

    private static boolean hasPendingConfirmations() {
        for (boolean pending : EQUIPMENT_CONFIRMATION_PENDING) {
            if (pending) {
                return true;
            }
        }
        return false;
    }

    private static void resetConfirmationState() {
        for (int i = 0; i < EQUIPMENT_CONFIRMATION_PENDING.length; i++) {
            clearPendingConfirmation(i);
        }
    }

    private static void clearPendingConfirmation(int slotGroup) {
        EQUIPMENT_CONFIRMATION_PENDING[slotGroup] = false;
        EQUIPMENT_CONFIRMATION_PRIORITY[slotGroup] = 0;
        EQUIPMENT_CONFIRMATION_START[slotGroup] = 0L;
    }

    private static boolean isTargetEnabled(EquipmentSwapTarget target) {
        return switch (target) {
            case FARMING, PEST -> TfmConfig.AUTO_EQUIPMENT_PEST.get();
            case VISITOR_LUNAR, VISITOR_PREVIOUS, FARMING_WITH_PEST_FALLBACK -> TfmConfig.AUTO_EQUIPMENT_VISITOR.get();
        };
    }

    private static boolean hasUpgradeableEquipment(AbstractContainerScreen<?> screen, int playerInvStart, int totalSlots) {
        for (int slotGroup = 0; slotGroup < GUI_SLOTS.length; slotGroup++) {
            int equippedPriority = getEquippedPriority(screen, slotGroup);
            int inventorySlotIndex = findBestInventorySlot(screen, playerInvStart, totalSlots, slotGroup);
            int candidatePriority = getInventoryPriority(screen, inventorySlotIndex);
            if (candidatePriority > equippedPriority) {
                return true;
            }
        }
        return false;
    }

    private static int findBestInventorySlot(AbstractContainerScreen<?> screen, int playerInvStart, int totalSlots,
            int slotGroup) {
        int bestSlot = -1;
        int bestPriority = 0;
        for (int i = playerInvStart; i < totalSlots && i < screen.getMenu().slots.size(); i++) {
            Slot invSlot = screen.getMenu().slots.get(i);
            if (invSlot == null || !invSlot.hasItem()) {
                continue;
            }

            String itemName = invSlot.getItem().getHoverName().getString().toLowerCase();
            if (getSlotGroup(itemName) != slotGroup) {
                continue;
            }

            int priority = getTargetPriority(itemName, slotGroup, swapTarget);
            if (priority > bestPriority) {
                bestPriority = priority;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static int getInventoryPriority(AbstractContainerScreen<?> screen, int inventorySlotIndex) {
        if (inventorySlotIndex == -1 || inventorySlotIndex >= screen.getMenu().slots.size()) {
            return 0;
        }
        Slot slot = screen.getMenu().slots.get(inventorySlotIndex);
        if (slot == null || !slot.hasItem()) {
            return 0;
        }
        return getTargetPriority(slot.getItem().getHoverName().getString().toLowerCase(), swapTarget);
    }

    private static int getEquippedPriority(AbstractContainerScreen<?> screen, int slotGroup) {
        if (slotGroup < 0 || slotGroup >= GUI_SLOTS.length) {
            return 0;
        }

        int slotIndex = GUI_SLOTS[slotGroup];
        if (slotIndex >= screen.getMenu().slots.size()) {
            return 0;
        }

        Slot slot = screen.getMenu().getSlot(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return 0;
        }

        return getTargetPriority(slot.getItem().getHoverName().getString().toLowerCase(), slotGroup, swapTarget);
    }

    private static int getTargetPriority(String itemName, EquipmentSwapTarget target) {
        return getTargetPriority(itemName, -1, target);
    }

    private static int getTargetPriority(String itemName, int slotGroup, EquipmentSwapTarget target) {
        return switch (target) {
            case FARMING -> isFarmingEquipment(itemName) ? 1 : 0;
            case PEST -> isPestEquipment(itemName) ? 1 : 0;
            case VISITOR_LUNAR -> itemName.contains("lunar") ? 1 : 0;
            case VISITOR_PREVIOUS -> getVisitorPreviousEquipmentPriority(itemName, slotGroup);
            case FARMING_WITH_PEST_FALLBACK -> {
                if (isFarmingEquipment(itemName)) {
                    yield 2;
                }
                yield isPestEquipment(itemName) ? 1 : 0;
            }
        };
    }

    private static int getVisitorPreviousEquipmentPriority(String itemName, int slotGroup) {
        if (!visitorReturnSnapshotCaptured) {
            return getTargetPriority(itemName, EquipmentSwapTarget.FARMING_WITH_PEST_FALLBACK);
        }

        if (slotGroup < 0 || slotGroup >= VISITOR_RETURN_EQUIPMENT_SNAPSHOT.length) {
            return 0;
        }

        String previousItemName = VISITOR_RETURN_EQUIPMENT_SNAPSHOT[slotGroup];
        return previousItemName != null && previousItemName.equals(itemName) ? 1 : 0;
    }

    private static int getSlotGroup(String itemName) {
        for (int i = 0; i < EQUIPMENT_KEYWORDS.length; i++) {
            for (String keyword : EQUIPMENT_KEYWORDS[i].split("\\|")) {
                if (itemName.contains(keyword)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static boolean isFarmingEquipment(String itemName) {
        return itemName.contains("peony") || itemName.contains("blossom") || itemName.contains("zorro");
    }

    private static boolean isPestEquipment(String itemName) {
        return itemName.contains("pest");
    }

    private static void captureVisitorReturnSnapshot(Minecraft client, AbstractContainerScreen<?> screen) {
        for (int slotGroup = 0; slotGroup < GUI_SLOTS.length; slotGroup++) {
            int slotIndex = GUI_SLOTS[slotGroup];
            VISITOR_RETURN_EQUIPMENT_SNAPSHOT[slotGroup] = null;
            if (slotIndex >= screen.getMenu().slots.size()) {
                continue;
            }

            Slot slot = screen.getMenu().getSlot(slotIndex);
            if (slot == null || !slot.hasItem()) {
                continue;
            }

            VISITOR_RETURN_EQUIPMENT_SNAPSHOT[slotGroup] = slot.getItem().getHoverName().getString().toLowerCase();
        }

        visitorReturnSnapshotCaptured = true;
        shouldCaptureVisitorReturnSnapshot = false;
        ClientUtils.sendDebugMessage(client, "Visitor equipment snapshot captured for return restore.");
    }

    private static void clearVisitorReturnSnapshot() {
        for (int i = 0; i < VISITOR_RETURN_EQUIPMENT_SNAPSHOT.length; i++) {
            VISITOR_RETURN_EQUIPMENT_SNAPSHOT[i] = null;
        }
        visitorReturnSnapshotCaptured = false;
        shouldCaptureVisitorReturnSnapshot = false;
    }

    private static boolean areAllEquipmentSlotsLoaded(AbstractContainerScreen<?> screen) {
        for (int slotIdx : GUI_SLOTS) {
            if (slotIdx >= screen.getMenu().slots.size()) {
                return false;
            }

            Slot slot = screen.getMenu().getSlot(slotIdx);
            if (slot == null || !slot.hasItem()) {
                return false;
            }

            ItemStack stack = slot.getItem();
            String itemName = stack.getHoverName().getString().toLowerCase();
            if (stack.isEmpty() || stack.getItem().toString().toLowerCase().contains("gray_dye")
                    || itemName.contains("gray dye")) {
                return false;
            }
        }

        return true;
    }
}
