package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * While freecam is active the camera entity is a detached {@code RemotePlayer}, so
 * {@link net.minecraft.client.Minecraft#pick} ray-traces from the freecam viewpoint.
 * If the <em>player</em> then interacts (attack / use / pick), the resulting break /
 * interact packet is aimed wherever the freecam points while the real player's rotation
 * packets say otherwise, tripping the server's rotation / reach checks ("rotationBreak" /
 * "farbreak") and getting the user kicked.
 *
 * <p>We only want to suppress the <em>human's</em> physical input, not the macro. The
 * macro drives interactions by holding the key programmatically (logically down but not
 * physically pressed - see {@code FreecamManager#isProgrammaticKeyDown}) or through the
 * {@code @Invoker} calls, both of which bypass these {@code handleKeybinds} call sites or
 * are recognised as programmatic below. So the farming loop keeps working in freecam while
 * manual clicks are swallowed.
 */
@Mixin(Minecraft.class)
public class MixinFreecamInteractionBlocker {

    /**
     * {@link net.minecraft.client.Minecraft#pick} computes {@code hitResult} by
     * ray-tracing from {@code getCameraEntity()} - the detached freecam camera while
     * freecam is active. That makes the macro break whatever the freecam is aimed at
     * instead of the block in front of the real player. Feed the real player as the ray
     * source so interactions stay anchored to the player, wherever the camera roams.
     */
    @Redirect(
        method = "pick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;")
    )
    private Entity typicalfarmingmacro$freecamPickFromPlayer(Minecraft self) {
        if (TfmBootstrapHooks.isFreecamEnabled() && self.player != null) {
            return self.player;
        }
        return self.getCameraEntity();
    }

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startAttack()Z")
    )
    private boolean typicalfarmingmacro$freecamStartAttack(Minecraft self) {
        if (typicalfarmingmacro$blockManualAttack(self)) {
            return false;
        }
        return ((MixinMinecraft) self).typicalfarmingmacro$startAttack();
    }

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V")
    )
    private void typicalfarmingmacro$freecamContinueAttack(Minecraft self, boolean attacking) {
        if (typicalfarmingmacro$blockManualAttack(self)) {
            ((MixinMinecraft) self).typicalfarmingmacro$continueAttack(false);
            return;
        }
        ((MixinMinecraft) self).typicalfarmingmacro$continueAttack(attacking);
    }

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V")
    )
    private void typicalfarmingmacro$freecamStartUseItem(Minecraft self) {
        if (TfmBootstrapHooks.isFreecamEnabled()
                && !TfmBootstrapHooks.isFreecamProgrammaticKeyDown(self, self.options.keyUse)) {
            return;
        }
        ((MixinMinecraft) self).typicalfarmingmacro$startUseItem();
    }

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;pickBlockOrEntity()V")
    )
    private void typicalfarmingmacro$freecamPickBlock(Minecraft self) {
        if (TfmBootstrapHooks.isFreecamEnabled()) {
            return;
        }
        ((MixinMinecraft) self).typicalfarmingmacro$pickBlockOrEntity();
    }

    /** {@code true} when freecam is on and the attack key is physically (manually) held. */
    private static boolean typicalfarmingmacro$blockManualAttack(Minecraft self) {
        return TfmBootstrapHooks.isFreecamEnabled()
                && !TfmBootstrapHooks.isFreecamProgrammaticKeyDown(self, self.options.keyAttack);
    }
}
