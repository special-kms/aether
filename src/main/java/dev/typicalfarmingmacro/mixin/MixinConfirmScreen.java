package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

@Mixin(ConfirmScreen.class)
public class MixinConfirmScreen {

    @Unique private BooleanConsumer typicalfarmingmacro$callback;
    @Unique private Component         typicalfarmingmacro$title;
    @Unique private Component         typicalfarmingmacro$message;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void typicalfarmingmacro$captureArgs(BooleanConsumer callback, Component title,
                                    Component message, CallbackInfo ci) {
        typicalfarmingmacro$callback = callback;
        typicalfarmingmacro$title    = title;
        typicalfarmingmacro$message  = message;
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void typicalfarmingmacro$redirectInit(CallbackInfo ci) {
        var replacement = TfmBootstrapHooks.maybeCreateConfirmScreen(typicalfarmingmacro$callback, typicalfarmingmacro$title, typicalfarmingmacro$message);
        if (replacement == null) {
            return;
        }
        ci.cancel();
        Minecraft.getInstance().setScreen(replacement);
    }

}

