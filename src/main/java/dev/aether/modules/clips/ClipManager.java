package dev.aether.modules.clips;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aether.Aether;
import dev.aether.config.AetherConfig;
import dev.aether.macro.MacroStateManager;
import dev.aether.mixin.AccessorGlDevice;
import dev.aether.mixin.AccessorGpuDevice;
import dev.aether.notification.NotificationManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class ClipManager {
    private static final int PBO_COUNT = 3;
    private static final long TRIGGER_DEBOUNCE_NANOS = 2_000_000_000L;
    private static final int[] PBOS = new int[PBO_COUNT];
    private static final long[] FENCES = new long[PBO_COUNT];
    private static final Path CLIPS_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve("aether-clips");
    private static volatile ClipEncoder encoder;
    private static volatile String encoderPath;
    private static volatile int encoderFps;
    private static volatile ClipResolution encoderResolution;
    private static boolean pbosReady;
    private static ClipEncoder pboEncoder;
    private static boolean failureNotified;
    private static int captureFbo;
    private static int captureTexture;
    private static int pboIndex;
    private static long nextFrameNanos;
    private static long lastManualTriggerNanos;
    private static long lastFailsafeTriggerNanos;
    private static boolean manualTriggerSeen;
    private static boolean failsafeTriggerSeen;

    private ClipManager() {
    }

    public static synchronized void syncFromConfig() {
        int fps = getConfiguredFps();
        String path = getConfiguredFfmpegPath();
        ClipResolution resolution = getConfiguredResolution();
        if (!AetherConfig.CLIPS_ENABLED.get()) {
            stop();
            return;
        }
        if (encoder == null || encoderFps != fps || encoderResolution != resolution || !path.equals(encoderPath)) {
            stop();
            start();
        }
    }

    private static synchronized void start() {
        if (encoder != null) {
            return;
        }
        encoderFps = getConfiguredFps();
        encoderPath = getConfiguredFfmpegPath();
        encoderResolution = getConfiguredResolution();
        encoder = new ClipEncoder(encoderFps, encoderPath, encoderResolution);
        failureNotified = false;
        nextFrameNanos = 0L;
        lastManualTriggerNanos = 0L;
        lastFailsafeTriggerNanos = 0L;
        manualTriggerSeen = false;
        failsafeTriggerSeen = false;
    }

    public static void stop() {
        ClipEncoder oldEncoder;
        synchronized (ClipManager.class) {
            oldEncoder = encoder;
            encoder = null;
            encoderPath = null;
            encoderFps = 0;
            encoderResolution = null;
        }
        if (RenderSystem.isOnRenderThread()) {
            destroyPbos();
        }
        if (oldEncoder != null) {
            Thread.ofPlatform().name("Aether Clip Shutdown").daemon(true).start(oldEncoder::close);
        }
    }

    static void onEncoderFailure(ClipEncoder failedEncoder) {
        synchronized (ClipManager.class) {
            if (encoder != failedEncoder) {
                return;
            }
            encoder = null;
            encoderPath = null;
            encoderFps = 0;
            encoderResolution = null;
            if (!failureNotified) {
                failureNotified = true;
                Minecraft client = Minecraft.getInstance();
                if (client != null) {
                    client.execute(() -> NotificationManager.error("Gameplay clips unavailable"));
                }
            }
        }
    }

    public static void captureFrame() {
        ClipEncoder currentEncoder = encoder;
        if (currentEncoder == null) {
            if (pbosReady) {
                destroyPbos();
            }
            return;
        }
        int fps = encoderFps;
        long now = System.nanoTime();
        if (now < nextFrameNanos) {
            pollCompletedPbo(currentEncoder, now);
            return;
        }
        nextFrameNanos = now + 1_000_000_000L / fps;

        Minecraft client = Minecraft.getInstance();
        var target = client.getMainRenderTarget();
        if (target == null || target.width <= 0 || target.height <= 0) {
            return;
        }
        try {
            if (encoder != currentEncoder) {
                return;
            }
            ensurePbos(currentEncoder);
            pollCompletedPbo(currentEncoder, now);
            int slot = pboIndex;
            if (FENCES[slot] != 0L) {
                return;
            }
            int previousReadFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int previousDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int previousPbo = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
            try {
                int targetFbo = ((GlTexture) target.getColorTexture()).getFbo(resolveDirectStateAccess(), null);
                blitLetterboxed(targetFbo, target.width, target.height, currentEncoder);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, captureFbo);
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, PBOS[slot]);
                GL11.glReadPixels(0, 0, currentEncoder.width(), currentEncoder.height(),
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
                FENCES[slot] = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
            } finally {
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, previousPbo);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFbo);
            }
            pboIndex = (pboIndex + 1) % PBO_COUNT;
        } catch (RuntimeException | LinkageError e) {
            Aether.LOGGER.error("Clip frame capture failed", e);
            onEncoderFailure(currentEncoder);
        }
    }

    private static void pollCompletedPbo(ClipEncoder currentEncoder, long capturedNanos) {
        if (!pbosReady || pboEncoder != currentEncoder) {
            return;
        }
        int previousPbo = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        try {
            for (int slot = 0; slot < PBO_COUNT; slot++) {
            long fence = FENCES[slot];
            if (fence == 0L) {
                continue;
            }
            int result = GL32.glClientWaitSync(fence, 0, 0L);
            if (result != GL32.GL_ALREADY_SIGNALED && result != GL32.GL_CONDITION_SATISFIED) {
                continue;
            }
            ClipEncoder.FrameBuffer buffer = currentEncoder.acquireBuffer();
            if (buffer == null) {
                return;
            }
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, PBOS[slot]);
            ByteBuffer mapped = GL30.glMapBufferRange(GL21.GL_PIXEL_PACK_BUFFER, 0L,
                    currentEncoder.frameBytes(), GL30.GL_MAP_READ_BIT);
            if (mapped != null) {
                buffer.data.clear();
                buffer.data.put(mapped);
                GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
                currentEncoder.submit(buffer, capturedNanos);
            } else {
                currentEncoder.release(buffer);
            }
            GL32.glDeleteSync(fence);
            FENCES[slot] = 0L;
        }
        } finally {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, previousPbo);
        }
    }

    private static void blitLetterboxed(int sourceFbo, int sourceWidth, int sourceHeight,
                                         ClipEncoder currentEncoder) {
        int targetWidth = currentEncoder.width();
        int targetHeight = currentEncoder.height();
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, captureFbo);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        double sourceAspect = (double) sourceWidth / sourceHeight;
        double targetAspect = (double) targetWidth / targetHeight;
        int width = targetWidth;
        int height = targetHeight;
        if (sourceAspect > targetAspect) {
            height = Math.max(1, (int) Math.round(width / sourceAspect));
        } else {
            width = Math.max(1, (int) Math.round(height * sourceAspect));
        }
        int x = (targetWidth - width) / 2;
        int y = (targetHeight - height) / 2;
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFbo);
        GL30.glBlitFramebuffer(0, 0, sourceWidth, sourceHeight,
                x, y, x + width, y + height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
    }

    public static void triggerManual() {
        trigger(false, AetherConfig.CLIPS_INSTANT_MANUAL_SAVE.get() ? 0L : ClipEncoder.POST_ROLL_NANOS);
    }

    public static void onFailsafeTriggered() {
        if (AetherConfig.CLIPS_AUTO_FAILSAFE.get() && MacroStateManager.isMacroRunning()) {
            trigger(true, ClipEncoder.POST_ROLL_NANOS);
        }
    }

    private static synchronized void trigger(boolean failsafe, long postRollNanos) {
        ClipEncoder currentEncoder = encoder;
        long requestedAtNanos = System.nanoTime();
        long lastTriggerNanos = failsafe ? lastFailsafeTriggerNanos : lastManualTriggerNanos;
        boolean triggerSeen = failsafe ? failsafeTriggerSeen : manualTriggerSeen;
        if (currentEncoder == null || (triggerSeen && requestedAtNanos - lastTriggerNanos < TRIGGER_DEBOUNCE_NANOS)) {
            return;
        }
        if (failsafe) {
            lastFailsafeTriggerNanos = requestedAtNanos;
            failsafeTriggerSeen = true;
        } else {
            lastManualTriggerNanos = requestedAtNanos;
            manualTriggerSeen = true;
        }
        long throughNanos = saturatingAdd(requestedAtNanos, postRollNanos);
        currentEncoder.trigger(new ClipEncoder.TriggerRequest(requestedAtNanos, throughNanos));
        Aether.LOGGER.info("Gameplay clip requested ({})", failsafe ? "failsafe" : "manual");
    }

    private static long saturatingAdd(long value, long increment) {
        return value > Long.MAX_VALUE - increment ? Long.MAX_VALUE : value + increment;
    }

    public static void setValidatedFfmpegPath(Path path) throws java.io.IOException {
        FfmpegInstaller.validate(path);
        String validatedPath = path.toAbsolutePath().normalize().toString();
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.isSameThread()) {
            persistValidatedFfmpegPath(validatedPath);
            return;
        }
        CompletableFuture<Void> applied = new CompletableFuture<>();
        client.execute(() -> {
            try {
                persistValidatedFfmpegPath(validatedPath);
                applied.complete(null);
            } catch (Throwable e) {
                applied.completeExceptionally(e);
            }
        });
        try {
            applied.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new java.io.IOException("FFmpeg configuration was interrupted.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new java.io.IOException("FFmpeg configuration failed.", cause);
        }
    }

    private static synchronized void persistValidatedFfmpegPath(String validatedPath) {
        String previousPath = AetherConfig.CLIPS_FFMPEG_PATH.get();
        try {
            AetherConfig.CLIPS_FFMPEG_PATH.set(validatedPath);
            AetherConfig.save();
            syncFromConfig();
        } catch (RuntimeException e) {
            AetherConfig.CLIPS_FFMPEG_PATH.set(previousPath);
            try {
                AetherConfig.save();
                syncFromConfig();
            } catch (RuntimeException rollbackFailure) {
                e.addSuppressed(rollbackFailure);
            }
            throw e;
        }
    }

    public static int getConfiguredFps() {
        int fps = AetherConfig.CLIPS_FPS.get();
        return fps == 15 || fps == 60 ? fps : 30;
    }

    public static ClipResolution getConfiguredResolution() {
        return ClipResolution.fromId(AetherConfig.CLIPS_RESOLUTION.get());
    }

    public static Path getClipsDirectory() {
        return CLIPS_DIRECTORY;
    }

    public static void openClipsFolder() {
        try {
            Files.createDirectories(CLIPS_DIRECTORY);
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("explorer.exe", CLIPS_DIRECTORY.toAbsolutePath().toString()).start();
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(CLIPS_DIRECTORY.toFile());
            } else {
                throw new IOException("desktop open is not supported on this platform");
            }
            NotificationManager.success("Clips folder opened", CLIPS_DIRECTORY.toAbsolutePath().toString());
        } catch (IOException | RuntimeException e) {
            Aether.LOGGER.error("Unable to open clips folder", e);
            NotificationManager.error("Could not open clips folder", e.getMessage());
        }
    }

    private static String getConfiguredFfmpegPath() {
        String path = AetherConfig.CLIPS_FFMPEG_PATH.get();
        return path == null || path.isBlank() ? "ffmpeg" : path.trim();
    }

    private static void ensurePbos(ClipEncoder currentEncoder) {
        if (pbosReady && pboEncoder == currentEncoder) {
            return;
        }
        if (pbosReady) {
            destroyPbos();
        }
        int previousFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int previousPbo = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        captureTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, captureTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                currentEncoder.width(), currentEncoder.height(),
                0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        captureFbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, captureTexture, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        GL15.glGenBuffers(PBOS);
        for (int pbo : PBOS) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo);
            GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, currentEncoder.frameBytes(), GL15.GL_STREAM_READ);
        }
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, previousPbo);
        pbosReady = true;
        pboEncoder = currentEncoder;
        pboIndex = 0;
    }

    private static void destroyPbos() {
        if (!pbosReady) {
            return;
        }
        for (int i = 0; i < PBO_COUNT; i++) {
            if (FENCES[i] != 0L) {
                GL32.glDeleteSync(FENCES[i]);
                FENCES[i] = 0L;
            }
        }
        GL15.glDeleteBuffers(PBOS);
        GL30.glDeleteFramebuffers(captureFbo);
        GL11.glDeleteTextures(captureTexture);
        captureFbo = 0;
        captureTexture = 0;
        pbosReady = false;
        pboEncoder = null;
        pboIndex = 0;
    }

    private static DirectStateAccess resolveDirectStateAccess() {
        return ((AccessorGlDevice) ((AccessorGpuDevice) RenderSystem.getDevice()).aether$getBackend())
                .aether$directStateAccess();
    }
}
