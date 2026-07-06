package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import dev.typicalfarmingmacro.renderer.TfmRenderQueue;
import dev.typicalfarmingmacro.ui.MainGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "extract", at = @At("HEAD"))
    private void onRender(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
        TfmBootstrapHooks.onGameRenderStart(Minecraft.getInstance());
    }

    /** Fires after GUI render-state extraction. */
    @Inject(method = "extract", at = @At("TAIL"))
    private void onRenderTail(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
        TfmBootstrapHooks.onGameRenderEnd();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void afterRender(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
        TfmRenderQueue.flush();
        if (Minecraft.getInstance().screen instanceof MainGUI mainGUI) {
            mainGUI.renderAfterGameRenderer(deltaTracker.getGameTimeDeltaTicks());
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void onRenderItemInHand(CameraRenderState cameraRenderState, float partialTick, org.joml.Matrix4fc matrix4f, CallbackInfo ci) {
        if (TfmBootstrapHooks.isFreecamEnabled()) {
            ci.cancel();
        }
    }
}

