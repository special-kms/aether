package dev.typicalfarmingmacro.feature;

import com.mojang.authlib.GameProfile;
import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.hud.HudEditScreen;
import dev.typicalfarmingmacro.hud.HudRegistry;
import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import dev.typicalfarmingmacro.macro.MacroStateManager;
import dev.typicalfarmingmacro.macro.ReconnectScheduler;
import dev.typicalfarmingmacro.modules.failsafe.FailsafeManager;
import dev.typicalfarmingmacro.modules.farming.UngrabMouse;
import dev.typicalfarmingmacro.modules.pathfinding.rotation.RotationExecutor;
import dev.typicalfarmingmacro.modules.performance.MuteManager;
import dev.typicalfarmingmacro.modules.performance.PerformanceModeManager;
import dev.typicalfarmingmacro.modules.pest.helpers.PestDestroyer;
import dev.typicalfarmingmacro.modules.pest.helpers.VacuumParticleDebug;
import dev.typicalfarmingmacro.modules.rotation.RotationManager;
import dev.typicalfarmingmacro.modules.visuals.FreecamManager;
import dev.typicalfarmingmacro.modules.visuals.FreelookManager;
import dev.typicalfarmingmacro.modules.visuals.StreamerModeManager;
import dev.typicalfarmingmacro.renderer.TfmBackground;
import dev.typicalfarmingmacro.renderer.TfmBackgroundScreens;
import dev.typicalfarmingmacro.renderer.NVGRenderer;
import dev.typicalfarmingmacro.ui.TfmConfirmScreen;
import dev.typicalfarmingmacro.ui.TfmDirectJoinScreen;
import dev.typicalfarmingmacro.ui.TfmManageServerScreen;
import dev.typicalfarmingmacro.ui.TfmMultiplayerScreen;
import dev.typicalfarmingmacro.ui.TfmTitleScreen;
import dev.typicalfarmingmacro.ui.MainGUI;
import dev.typicalfarmingmacro.util.BpsTracker;
import dev.typicalfarmingmacro.util.DelayedBlockBreakTracker;
import dev.typicalfarmingmacro.util.NickHiderUtils;
import dev.typicalfarmingmacro.util.ProgrammaticMovementTracker;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;

import java.io.File;

public final class LiveTfmBootstrapHooks implements TfmBootstrapHooks.FeatureHooks {
    @Override
    public void onConfigProfileLoaded(File profileFile) {
        ClientFeatureBootstrap.onConfigProfileLoaded(profileFile);
    }

    @Override
    public void onUnexpectedDisconnect() {
        if (MacroStateManager.isMacroRunning() && !MacroStateManager.isIntentionalDisconnect()) {
            long delay = 30 + (long) (Math.random() * 30);
            ReconnectScheduler.scheduleReconnect(delay, true);
        }
    }

    @Override
    public Screen maybeCreateConfirmScreen(BooleanConsumer callback, Component title, Component message) {
        if (StreamerModeManager.isEnabled() || !TfmConfig.CUSTOM_UI_ENABLED.get()) {
            return null;
        }
        return new TfmConfirmScreen(callback, title, message);
    }

    @Override
    public Screen maybeCreateDirectJoinScreen(Screen lastScreen, BooleanConsumer callback, ServerData serverData) {
        if (StreamerModeManager.isEnabled() || !TfmConfig.CUSTOM_UI_ENABLED.get()) {
            return null;
        }
        return new TfmDirectJoinScreen(lastScreen, callback, serverData);
    }

    @Override
    public Screen maybeCreateMultiplayerScreen(Screen lastScreen) {
        if (StreamerModeManager.isEnabled() || !TfmConfig.CUSTOM_UI_ENABLED.get()) {
            return null;
        }
        return new TfmMultiplayerScreen(lastScreen);
    }

    @Override
    public Screen maybeCreateManageServerScreen(Screen lastScreen, Component title, BooleanConsumer callback, ServerData serverData) {
        if (StreamerModeManager.isEnabled() || !TfmConfig.CUSTOM_UI_ENABLED.get()) {
            return null;
        }
        return new TfmManageServerScreen(lastScreen, title, callback, serverData);
    }

    @Override
    public Screen maybeCreateTitleScreen() {
        if (StreamerModeManager.isEnabled() || !TfmConfig.CUSTOM_UI_ENABLED.get()) {
            return null;
        }
        return new TfmTitleScreen();
    }

    @Override
    public Screen maybeCreateHudEditScreen() {
        return new HudEditScreen();
    }

    @Override
    public void onGameRenderStart(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        RotationManager.update(minecraft);
        RotationExecutor.update(minecraft);
    }

    @Override
    public void onGameRenderEnd() {
        HudRegistry.onGuiGraphicsClosed();
    }

    @Override
    public boolean shouldSuppressVanillaHud(Screen screen) {
        return TfmBootstrapHooks.isBootstrapConfigScreen(screen) || screen instanceof MainGUI || screen instanceof HudEditScreen;
    }

    @Override
    public void renderConfigScreenOverlay(NVGRenderer renderer, float width, float height, float deltaTime) {
        HudRegistry.renderConfigTransition(renderer);
    }

    @Override
    public Component transformOverlayMessage(Component component) {
        FailsafeManager.observeGhostBlockOverlayMessage(component);
        dev.typicalfarmingmacro.modules.profit.helpers.FarmingXpTracker.onActionBar(component);
        return transformDisplayComponent(component);
    }

    @Override
    public Component transformDisplayComponent(Component component) {
        if (component == null) {
            return null;
        }
        if (!TfmConfig.NICK_HIDER_ENABLED.get() && !TfmConfig.COOP_HIDER_ENABLED.get() && !TfmConfig.HIDE_SERVER_ID.get()) {
            return component;
        }
        Component transformed = NickHiderUtils.transformComponent(component);
        return transformed != null ? transformed : component;
    }

    @Override
    public String transformDisplayString(String text) {
        if (!TfmConfig.NICK_HIDER_ENABLED.get() && !TfmConfig.COOP_HIDER_ENABLED.get() && !TfmConfig.HIDE_SERVER_ID.get()) {
            return text;
        }
        return NickHiderUtils.transformString(text);
    }

    @Override
    public boolean shouldHidePlayerSkin(GameProfile profile) {
        return profile != null
                && TfmConfig.NICK_HIDER_ENABLED.get()
                && TfmConfig.HIDE_SKIN.get()
                && profile.name().equals(Minecraft.getInstance().getUser().getName());
    }

    @Override
    public boolean shouldHideFilteredChatMessage(Component message) {
        if (!TfmConfig.HIDE_FILTERED_CHAT.get() || message == null) {
            return false;
        }
        return message.getString().contains("for killing a");
    }

    @Override
    public boolean isFreecamEnabled() {
        return FreecamManager.isEnabled();
    }

    @Override
    public boolean isFreecamProgrammaticKeyDown(Minecraft client, KeyMapping keyMapping) {
        return FreecamManager.isProgrammaticKeyDown(client, keyMapping);
    }

    @Override
    public boolean isProgrammaticMovementKeyDown(KeyMapping keyMapping) {
        return ProgrammaticMovementTracker.isDown(keyMapping);
    }

    @Override
    public boolean turnFreecamCamera(double yRot, double xRot) {
        return FreecamManager.turnCamera(yRot, xRot);
    }

    @Override
    public boolean isFreelookActive() {
        return FreelookManager.isActive();
    }

    @Override
    public boolean turnFreelookCamera(double yRot, double xRot) {
        return FreelookManager.turn(yRot, xRot);
    }

    @Override
    public float getFreelookYaw() {
        return FreelookManager.getYaw();
    }

    @Override
    public float getFreelookPitch() {
        return FreelookManager.getPitch();
    }

    @Override
    public boolean shouldCancelMouseTurn() {
        return RotationManager.isRotating() && !FreecamManager.isEnabled() && !FreelookManager.isActive();
    }

    @Override
    public boolean isMouseUngrabbed() {
        return UngrabMouse.isMouseUngrabbed();
    }

    @Override
    public boolean hasCustomScreenBackground(Screen screen) {
        return !StreamerModeManager.isEnabled()
                && TfmConfig.CUSTOM_UI_ENABLED.get()
                && screen != null
                && TfmBackgroundScreens.matches(screen);
    }

    @Override
    public void renderCustomScreenBackground(int width, int height, int mouseX, int mouseY) {
        TfmBackground.INSTANCE.render(width, height, mouseX, mouseY);
    }

    @Override
    public void onBackgroundLeftClick(Minecraft minecraft, Screen screen, double mouseX, double mouseY) {
        if (hasCustomScreenBackground(screen)) {
            TfmBackground.INSTANCE.addRipple((float) mouseX, (float) mouseY);
        }
    }

    @Override
    public void onBlockBreak() {
        BpsTracker.onBlockBreak();
        FailsafeManager.onBlockBreak();
    }

    @Override
    public void onBlockBreak(net.minecraft.core.BlockPos pos) {
        DelayedBlockBreakTracker.onImmediateBlockBreak(pos);
        BpsTracker.onBlockBreak();
        FailsafeManager.onBlockBreak(pos);
    }

    @Override
    public void onBlockBreakClick(Minecraft minecraft, net.minecraft.core.BlockPos pos) {
        DelayedBlockBreakTracker.onBlockBreakClick(minecraft, pos);
    }

    @Override
    public void onBlockChanged(Minecraft minecraft, net.minecraft.core.BlockPos pos,
                               net.minecraft.world.level.block.state.BlockState oldState,
                               net.minecraft.world.level.block.state.BlockState newState) {
        DelayedBlockBreakTracker.onBlockChanged(minecraft, pos, newState);
        FailsafeManager.onBlockChanged(minecraft, pos, oldState, newState);
    }

    @Override
    public void tickFailsafes(Minecraft minecraft) {
        DelayedBlockBreakTracker.tick(minecraft);
        FailsafeManager.tick(minecraft);
    }

    @Override
    public void resetFailsafes() {
        DelayedBlockBreakTracker.reset();
        FailsafeManager.reset();
    }

    @Override
    public void resetFailsafeRuntimeState() {
        DelayedBlockBreakTracker.reset();
        FailsafeManager.resetRuntimeState();
    }

    @Override
    public void addRotationGracePeriod(long durationMs) {
        FailsafeManager.addRotationGracePeriod(durationMs);
    }

    @Override
    public void selectHotbarSlot(Minecraft minecraft, int slot) {
        FailsafeManager.selectHotbarSlot(minecraft, slot);
    }

    @Override
    public boolean isMuted() {
        return MuteManager.isMuted();
    }

    @Override
    public float getMuteVolume() {
        return MuteManager.getVolume();
    }

    @Override
    public boolean areParticlesDisabled() {
        return PerformanceModeManager.isParticlesDisabled();
    }

    @Override
    public void onParticlePacket(Minecraft minecraft, ClientboundLevelParticlesPacket packet) {
        VacuumParticleDebug.onParticlePacket(minecraft, packet);
        if (packet.getParticle().getType() == ParticleTypes.ANGRY_VILLAGER) {
            PestDestroyer.onFireworkParticle(packet.getX(), packet.getY(), packet.getZ());
        }
    }
}

