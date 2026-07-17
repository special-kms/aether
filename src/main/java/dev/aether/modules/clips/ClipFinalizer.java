package dev.aether.modules.clips;

import dev.aether.Aether;
import dev.aether.notification.NotificationManager;
import net.minecraft.client.Minecraft;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

final class ClipFinalizer implements AutoCloseable {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final BlockingQueue<List<ClipEncoder.Segment>> jobs = new ArrayBlockingQueue<>(2);
    private final Thread thread;
    private final String ffmpegPath;
    private final Path spoolDirectory;
    private volatile boolean running = true;
    private volatile Process process;

    ClipFinalizer(String ffmpegPath, Path spoolDirectory) {
        this.ffmpegPath = ffmpegPath;
        this.spoolDirectory = spoolDirectory;
        thread = Thread.ofPlatform().name("Aether Clip Finalizer").daemon(true).start(this::run);
    }

    boolean submit(List<ClipEncoder.Segment> segments) {
        return jobs.offer(List.copyOf(segments));
    }

    private void run() {
        while (running || !jobs.isEmpty()) {
            List<ClipEncoder.Segment> segments = null;
            try {
                segments = jobs.poll(250, TimeUnit.MILLISECONDS);
                if (segments != null) {
                    finalizeClip(segments);
                }
            } catch (InterruptedException ignored) {
                if (!running) {
                    break;
                }
            } catch (Exception e) {
                Aether.LOGGER.error("Gameplay clip finalization failed", e);
                notifyClient(false, "Gameplay clip failed");
            } finally {
                if (segments != null) {
                    segments.forEach(ClipEncoder.Segment::release);
                }
            }
        }
        List<ClipEncoder.Segment> abandoned;
        while ((abandoned = jobs.poll()) != null) {
            abandoned.forEach(ClipEncoder.Segment::release);
        }
    }

    private void finalizeClip(List<ClipEncoder.Segment> segments) throws IOException, InterruptedException {
        Path outputDirectory = ClipManager.getClipsDirectory();
        Files.createDirectories(outputDirectory);
        Path manifest = Files.createTempFile(spoolDirectory, "concat-", ".txt");
        try {
            writeManifest(manifest, segments);
            Path output = outputDirectory.resolve("clip-" + FILE_TIME.format(LocalDateTime.now())
                    + "-" + UUID.randomUUID().toString().substring(0, 8) + ".mp4");
            process = new ProcessBuilder(ffmpegPath, "-hide_banner", "-loglevel", "error",
                    "-y", "-f", "concat", "-safe", "0", "-i", manifest.toString(), "-c", "copy", output.toString())
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            int exit = finished ? process.exitValue() : -1;
            if (!finished) {
                process.destroyForcibly();
            }
            if (exit == 0 && Files.isRegularFile(output) && Files.size(output) > 0) {
                notifyClient(true, "Gameplay clip saved");
            } else {
                Files.deleteIfExists(output);
                notifyClient(false, "Gameplay clip failed");
            }
        } finally {
            Process active = process;
            if (active != null && active.isAlive()) {
                active.destroyForcibly();
            }
            process = null;
            Files.deleteIfExists(manifest);
        }
    }

    private static void writeManifest(Path manifest, List<ClipEncoder.Segment> segments) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(manifest, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (ClipEncoder.Segment segment : segments) {
                String path = segment.path().toAbsolutePath().toString().replace('\\', '/').replace("'", "'\\''");
                writer.write("file '" + path + "'");
                writer.newLine();
            }
        }
    }

    private static void notifyClient(boolean success, String title) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> {
                if (success) {
                    NotificationManager.success(title);
                } else {
                    NotificationManager.error(title);
                }
            });
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            thread.join(15_000L);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            Process active = process;
            if (active != null) {
                active.destroyForcibly();
            }
            thread.interrupt();
        }
    }
}
