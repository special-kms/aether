package dev.typicalfarmingmacro.bootstrap;

import dev.typicalfarmingmacro.Tfm;
import dev.typicalfarmingmacro.ui.MainGUI;
import dev.typicalfarmingmacro.ui.MainGUIRegistry;
import dev.typicalfarmingmacro.util.ClientUtils;
import net.minecraft.client.Minecraft;

public final class TfmUiActions {
    private TfmUiActions() {
    }

    public static void toggleMainGui(Minecraft client) {
        if (client.screen instanceof MainGUI) {
            client.setScreen(null);
        } else {
            openMainGui(client);
        }
    }

    public static void openMainGui(Minecraft client) {
        if (client == null) {
            return;
        }

        try {
            MainGUIRegistry.refresh();
            client.execute(() -> {
                try {
                    client.setScreen(new MainGUI());
                } catch (RuntimeException | LinkageError e) {
                    Tfm.LOGGER.error("Failed to open Tfm GUI from queued client task", e);
                    ClientUtils.sendMessage(client, "\u00A7cFailed to open the Tfm GUI. Check the client log.", false);
                }
            });
        } catch (RuntimeException | LinkageError e) {
            Tfm.LOGGER.error("Failed to open Tfm GUI", e);
            ClientUtils.sendMessage(client, "\u00A7cFailed to open the Tfm GUI. Check the client log.", false);
        }
    }
}
