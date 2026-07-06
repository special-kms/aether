package dev.typicalfarmingmacro.modules.gear.helpers;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.config.ConfigHelpers;

import dev.typicalfarmingmacro.mixin.AccessorInventory;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeManager;
import dev.typicalfarmingmacro.util.ClientUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class RodManager {
    private static final java.util.concurrent.atomic.AtomicBoolean isExecuting = new java.util.concurrent.atomic.AtomicBoolean(false);

    public static void resetState() {
        isExecuting.set(false);
    }

    public static void stopHoldingRodUse() {
        // No-op now as we use single clicks, but kept for compatibility
    }

    public static void executeRodSequence(Minecraft client) {
        if (client.player == null) return;

        if (!isExecuting.compareAndSet(false, true)) {
            ClientUtils.sendDebugMessage(client, "[RodManager] Sequence already in progress, skipping.");
            return;
        }

        try {
            ClientUtils.sendDebugMessage(client, "executeRodSequence called");
            if (EquipmentManager.isSwappingEquipment) {
                ClientUtils.sendDebugMessage(client, "Waiting for equipment swap before rod sequence...");
                long waitStart = System.currentTimeMillis();
                while (EquipmentManager.isSwappingEquipment && System.currentTimeMillis() - waitStart < 5000) {
                    Thread.sleep(50);
                }
                if (EquipmentManager.isSwappingEquipment) {
                    ClientUtils.sendDebugMessage(client,
                            "\u00A7cRod sequence: Equipment swap timed out, proceeding anyway.");
                } else {
                    ClientUtils.sendDebugMessage(client, "Equipment swap done! Starting rod sequence.");
                }
            }

            int rodSlot = -1;
            for (int i = 0; i < 9; i++) {
                String rodItemName = client.player.getInventory().getItem(i).getHoverName().getString().toLowerCase();
                if (rodItemName.contains("rod")) {
                    rodSlot = i;
                    break;
                }
            }

            if (rodSlot == -1) {
                ClientUtils.sendMessage(client, "Rod not found in hotbar!", true);
                return;
            }

            ClientUtils.sendMessage(client, "Executing rod swap sequence...", true);

            final int finalRodSlot = rodSlot;
            // 1. Select the slot
            client.execute(() -> FailsafeManager.selectHotbarSlot(client, finalRodSlot));

            // 2. Wait for selection confirmation
            long swapWaitStart = System.currentTimeMillis();
            boolean confirmed = false;
            while (System.currentTimeMillis() - swapWaitStart < 2000) {
                if (((AccessorInventory) client.player.getInventory()).getSelected() == finalRodSlot) {
                    ItemStack current = client.player.getInventory().getItem(finalRodSlot);
                    if (current.getHoverName().getString().toLowerCase().contains("rod")) {
                        confirmed = true;
                        break;
                    }
                }
                Thread.sleep(20);
            }

            if (!confirmed) {
                ClientUtils.sendDebugMessage(client, "§cRod sequence: Slot selection timed out!");
                return;
            }

            // 3. Delay before click (configurable)
            int rodSwapDelay = ConfigHelpers.getRandomizedDelay(
                    TfmConfig.ROD_SWAP_DELAY_MIN.get(),
                    TfmConfig.ROD_SWAP_DELAY_MAX.get());
            if (rodSwapDelay > 0) {
                Thread.sleep(rodSwapDelay);
            }

            // 4. Perform single right click via the normal use key path
            if (((AccessorInventory) client.player.getInventory()).getSelected() == finalRodSlot) {
                ClientUtils.performUseClick(client);
            }

            // 5. Small buffer after click
            Thread.sleep(100);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            isExecuting.set(false);
        }
    }
}


