package dev.typicalfarmingmacro.mixin;

import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (!TfmBootstrapHooks.isFreecamEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.options == null) {
            return;
        }

        boolean forward = TfmBootstrapHooks.isProgrammaticMovementKeyDown(client.options.keyUp)
            || TfmBootstrapHooks.isFreecamProgrammaticKeyDown(client, client.options.keyUp);
        boolean backward = TfmBootstrapHooks.isProgrammaticMovementKeyDown(client.options.keyDown)
            || TfmBootstrapHooks.isFreecamProgrammaticKeyDown(client, client.options.keyDown);
        boolean left = TfmBootstrapHooks.isProgrammaticMovementKeyDown(client.options.keyLeft)
            || TfmBootstrapHooks.isFreecamProgrammaticKeyDown(client, client.options.keyLeft);
        boolean right = TfmBootstrapHooks.isProgrammaticMovementKeyDown(client.options.keyRight)
            || TfmBootstrapHooks.isFreecamProgrammaticKeyDown(client, client.options.keyRight);
        boolean jump = TfmBootstrapHooks.isProgrammaticMovementKeyDown(client.options.keyJump)
            || TfmBootstrapHooks.isFreecamProgrammaticKeyDown(client, client.options.keyJump);
        boolean shift = TfmBootstrapHooks.isProgrammaticMovementKeyDown(client.options.keyShift)
            || TfmBootstrapHooks.isFreecamProgrammaticKeyDown(client, client.options.keyShift);
        boolean sprint = TfmBootstrapHooks.isProgrammaticMovementKeyDown(client.options.keySprint)
            || TfmBootstrapHooks.isFreecamProgrammaticKeyDown(client, client.options.keySprint);

        this.keyPresses = new Input(
            forward,
            backward,
            left,
            right,
            jump,
            shift,
            sprint
        );
        this.moveVector = new Vec2(calculateImpulse(left, right), calculateImpulse(forward, backward)).normalized();
    }

    private static float calculateImpulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }
        return positive ? 1.0f : -1.0f;
    }
}

