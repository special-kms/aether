package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import dev.typicalfarmingmacro.proxy.TfmProxyManager;
import dev.typicalfarmingmacro.proxy.TfmProxyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MixinJoinMultiplayerScreen extends Screen {

    protected MixinJoinMultiplayerScreen(Component title) {
        super(title);
    }

    @Shadow private Screen lastScreen;
    @Shadow protected ServerSelectionList serverSelectionList;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void typicalfarmingmacro$redirectInit(CallbackInfo ci) {
        var replacement = typicalfarmingmacro$getReplacement();
        if (replacement == null) {
            return;
        }
        ci.cancel();
        Minecraft.getInstance().setScreen(replacement);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void typicalfarmingmacro$addProxyButton(CallbackInfo ci) {
        if (serverSelectionList == null) {
            return;
        }

        this.addRenderableWidget(Button.builder(
                        Component.literal(TfmProxyManager.selectedStatus()),
                        button -> Minecraft.getInstance().setScreen(new TfmProxyScreen(this)))
                .bounds(this.width - 185, 6, 180, 20)
                .build());
    }

    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void typicalfarmingmacro$guardRemoved(CallbackInfo ci) {
        if (serverSelectionList == null) ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void typicalfarmingmacro$guardTick(CallbackInfo ci) {
        var replacement = typicalfarmingmacro$getReplacement();
        if (replacement != null) {
            ci.cancel();
            Minecraft.getInstance().setScreen(replacement);
            return;
        }
        if (serverSelectionList == null) ci.cancel();
    }

    private Screen typicalfarmingmacro$getReplacement() {
        Screen replacement = TfmBootstrapHooks.maybeCreateMultiplayerScreen(lastScreen);
        return replacement == this ? null : replacement;
    }
}

