package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import dev.typicalfarmingmacro.proxy.TfmProxyManager;
import dev.typicalfarmingmacro.proxy.TfmProxyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces {@link TitleScreen} with {@link TfmTitleScreen} with zero frame delay.
 *
 * Cancelling {@code init} means vanilla buttons/panorama are never set up.
 * Calling {@code setScreen} directly (not via {@code execute()}) means
 * {@code mc.screen} is already {@link TfmTitleScreen} before the first render tick,
 * so there is no one-frame TitleScreen flash.
 */
@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {
    private static final int TFM_BUTTON_WIDTH = 200;
    private static final int TFM_BUTTON_HEIGHT = 20;
    private static final int TFM_BUTTON_GAP = 4;

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void typicalfarmingmacro$redirectInit(CallbackInfo ci) {
        var replacement = typicalfarmingmacro$getReplacement();
        if (replacement == null) {
            return;
        }
        ci.cancel();
        Minecraft.getInstance().setScreen(replacement);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void typicalfarmingmacro$redirectRender(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var replacement = typicalfarmingmacro$getReplacement();
        if (replacement == null) {
            return;
        }
        ci.cancel();
        Minecraft.getInstance().setScreen(replacement);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void typicalfarmingmacro$addProxyButton(CallbackInfo ci) {
        int buttonX = this.width / 2 - TFM_BUTTON_WIDTH / 2;
        int buttonY = typicalfarmingmacro$nextInjectedButtonY();
        this.addRenderableWidget(Button.builder(
                        Component.literal(TfmProxyManager.selectedStatus()),
                        button -> Minecraft.getInstance().setScreen(new TfmProxyScreen(this)))
                .bounds(buttonX, buttonY, TFM_BUTTON_WIDTH, TFM_BUTTON_HEIGHT)
                .build());
    }

    private int typicalfarmingmacro$nextInjectedButtonY() {
        int minX = this.width / 2 - TFM_BUTTON_WIDTH / 2 - 4;
        int maxX = this.width / 2 + TFM_BUTTON_WIDTH / 2 + 4;
        int maxBottom = this.height / 4 + 96;
        for (var listener : this.children()) {
            if (!(listener instanceof Button button)) {
                continue;
            }
            if (button.getX() > maxX || button.getX() + button.getWidth() < minX) {
                continue;
            }
            if (button.getWidth() < 98 || button.getY() < this.height / 4 - 8) {
                continue;
            }
            maxBottom = Math.max(maxBottom, button.getY() + button.getHeight());
        }
        return maxBottom + TFM_BUTTON_GAP;
    }

    private Screen typicalfarmingmacro$getReplacement() {
        Screen replacement = TfmBootstrapHooks.maybeCreateTitleScreen();
        return replacement == this ? null : replacement;
    }
}

