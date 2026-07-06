package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.config.TfmConfig;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinKeepFocus {
    @Redirect(
            method = "pauseIfInactive",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;isFocused()Z")
    )
    private boolean keepFocusForInactivePause(Window window) {
        return TfmConfig.KEEP_FOCUS.get() || window.isFocused();
    }

    @Inject(method = "isWindowActive", at = @At("HEAD"), cancellable = true)
    private void keepFocusForWindowActive(CallbackInfoReturnable<Boolean> cir) {
        if (TfmConfig.KEEP_FOCUS.get()) {
            cir.setReturnValue(true);
        }
    }
}
