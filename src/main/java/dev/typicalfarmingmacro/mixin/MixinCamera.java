package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.Camera;

/**
 * Applies freelook: while freelook is active the camera reads a free yaw/pitch instead of
 * the player's real view rotation, so the view orbits the player (third-person) while the
 * body keeps facing its actual direction. Only the player's own camera is affected.
 */
@Mixin(Camera.class)
public class MixinCamera {

    @Redirect(
        method = "alignWithEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F")
    )
    private float typicalfarmingmacro$freelookViewYRot(Entity entity, float partialTick) {
        if (TfmBootstrapHooks.isFreelookActive() && entity == Minecraft.getInstance().player) {
            return TfmBootstrapHooks.getFreelookYaw();
        }
        return entity.getViewYRot(partialTick);
    }

    @Redirect(
        method = "alignWithEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F")
    )
    private float typicalfarmingmacro$freelookViewXRot(Entity entity, float partialTick) {
        if (TfmBootstrapHooks.isFreelookActive() && entity == Minecraft.getInstance().player) {
            return TfmBootstrapHooks.getFreelookPitch();
        }
        return entity.getViewXRot(partialTick);
    }
}
