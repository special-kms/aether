package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

@Mixin(ManageServerScreen.class)
public class MixinManageServerScreen {

    @Shadow private EditBox nameEdit;

    @Unique private Screen          typicalfarmingmacro$lastScreen;
    @Unique private Component       typicalfarmingmacro$title;
    @Unique private BooleanConsumer typicalfarmingmacro$callback;
    @Unique private ServerData      typicalfarmingmacro$serverData;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void typicalfarmingmacro$captureArgs(Screen lastScreen, Component title,
                                    BooleanConsumer callback, ServerData serverData,
                                    CallbackInfo ci) {
        typicalfarmingmacro$lastScreen = lastScreen;
        typicalfarmingmacro$title      = title;
        typicalfarmingmacro$callback   = callback;
        typicalfarmingmacro$serverData = serverData;
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void typicalfarmingmacro$redirectInit(CallbackInfo ci) {
        var replacement = TfmBootstrapHooks.maybeCreateManageServerScreen(
                typicalfarmingmacro$lastScreen,
                typicalfarmingmacro$title,
                typicalfarmingmacro$callback,
                typicalfarmingmacro$serverData);
        if (replacement == null) {
            return;
        }
        ci.cancel();
        Minecraft.getInstance().setScreen(replacement);
    }

    @Inject(method = "setInitialFocus", at = @At("HEAD"), cancellable = true)
    private void typicalfarmingmacro$guardSetInitialFocus(CallbackInfo ci) {
        if (nameEdit == null) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void typicalfarmingmacro$guardRender(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float pt, CallbackInfo ci) {
        if (nameEdit == null) ci.cancel();
    }
}

