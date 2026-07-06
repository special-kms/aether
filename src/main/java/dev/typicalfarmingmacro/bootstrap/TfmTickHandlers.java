package dev.typicalfarmingmacro.bootstrap;

import dev.typicalfarmingmacro.modules.visuals.FreecamManager;
import dev.typicalfarmingmacro.modules.visuals.FreelookManager;
import dev.typicalfarmingmacro.modules.movement.MovementPlaybackManager;

public final class TfmTickHandlers {
    private TfmTickHandlers() {
    }

    public static void register() {
        TfmKeybindHandler.register();
        TfmReconnectTickHandler.register();
        TfmWorldChangeTickHandler.register();
        TfmAutomationTickHandler.register();
        MovementPlaybackManager.register();
        FreecamManager.register();
        FreelookManager.register();
    }

    public static void setPickingUpStash(boolean pickingUpStash) {
        TfmAutomationTickHandler.setPickingUpStash(pickingUpStash);
    }
}
