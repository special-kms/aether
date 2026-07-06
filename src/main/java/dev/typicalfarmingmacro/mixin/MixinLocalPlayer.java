package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {

    @Shadow
    protected abstract boolean isControlledCamera();

    @Redirect(
        method = "applyInput",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z"
        )
    )
    private boolean typicalfarmingmacro$allowRealPlayerInputUpdate(LocalPlayer player) {
        if (TfmBootstrapHooks.isFreecamEnabled() && player == Minecraft.getInstance().player) {
            return true;
        }
        return this.isControlledCamera();
    }

    @Redirect(
        method = "sendPosition",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z"
        )
    )
    private boolean typicalfarmingmacro$allowRealPlayerPositionSync(LocalPlayer player) {
        if (TfmBootstrapHooks.isFreecamEnabled() && player == Minecraft.getInstance().player) {
            return true;
        }
        return this.isControlledCamera();
    }
}

