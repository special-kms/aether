package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public class MixinOptions {
    @Inject(method = "getSoundSourceVolume", at = @At("HEAD"), cancellable = true)
    private void onGetSoundSourceVolume(SoundSource soundSource, CallbackInfoReturnable<Float> ci) {
        if (TfmBootstrapHooks.isMuted() && soundSource == SoundSource.MASTER) {
            ci.setReturnValue(TfmBootstrapHooks.getMuteVolume());
        }
    }
}

