package dev.aether.modules.clips;

import dev.aether.Aether;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class ClipEncoder implements AutoCloseable {
    static final int WIDTH = 1280;
    static final int HEIGHT = 720;
    static final int FRAME_BYTES = WIDTH * HEIGHT * 4;
    private static final long PRE_ROLL_NANOS = TimeUnit.SECONDS.toNanos(30);
    private static final long POST_ROLL_NANOS = TimeUnit.SECONDS.toNanos(10);
    private static final long MAX_SPOOL_NANOS = TimeUnit.SECONDS.toNanos(42);
    private static final int BUFFER_COUNT = 6;

    private final BlockingQueue<FrameBuffer> available = new ArrayBlockingQueue<>(BUFFER_COUNT);
    private final BlockingQueue<FrameBuffer> frames = new ArrayBlockingQueue<>(BUFFER_COUNT);
    private final Deque<Segment> spool = new ArrayDeque<>();
    private final ClipFinalizer finalizer;
    private final Thread thread;
    private final Path spoolDirectory;
    private final String ffmpegPath;
    private final int fps;
    private volatile boolean running = true;
    private volatile boolean accepting = true;
    private volatile boolean triggerRequested;
    private Process process;
    private OutputStream input;
    private long segmentStartNanos;
    private long segmentEndNanos;
    private long nextOutputNanos;
    private long triggerNanos;
    private int segmentFrames;
    private long segmentSequence;
    private byte[] lastFrame;
    private Path currentSegment;

    ClipEncoder(int fps, String ffmpegPath) {
        this.fps = fps;
        this.ffmpegPath = ffmpegPath;
        spoolDirectory = FabricLoader.getInstance().getGameDir().resolve("aether-clips").resolve("spool")
                .resolve(UUID.randomUUID().toString());
        finalizer = new ClipFinalizer(ffmpegPath, spoolDirectory);
        for (int i = 0; i < BUFFER_COUNT; i++) {
            available.add(new FrameBuffer(ByteBuffer.allocateDirect(FRAME_BYTES)));
        }
        thread = Thread.ofPlatform().name("Aether Clip Encoder").daemon(true).start(this::run);
    }

    FrameBuffer acquireBuffer() {
        return accepting ? available.poll() : null;
    }

    void submit(FrameBuffer buffer, long capturedNanos) {
        buffer.capturedNanos = capturedNanos;
        buffer.data.position(0).limit(FRAME_BYTES);
        if (!accepting || !frames.offer(buffer)) {
            release(buffer);
        }
    }

    void release(FrameBuffer buffer) {
        buffer.data.clear();
        available.offer(buffer);
    }

    void trigger() {
        if (triggerNanos == 0L) {
            triggerRequested = true;
        }
    }

    private void run() {
        try {
            while (running || !frames.isEmpty()) {
                FrameBuffer frame = frames.poll(100, TimeUnit.MILLISECONDS);
                if (frame != null) {
                    encodePaced(frame);
                }
                applyTrigger();
                completeTriggerIfReady(System.nanoTime());
            }
            completeCurrentSegment();
            completeTriggerIfReady(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        } finally {
            Path incompleteSegment = currentSegment;
            closeProcess(false);
            if (incompleteSegment != null) {
                try {
                    Files.deleteIfExists(incompleteSegment);
                } catch (IOException ignored) {
                }
            }
            FrameBuffer frame;
            while ((frame = frames.poll()) != null) {
                release(frame);
            }
        }
    }

    private void encodePaced(FrameBuffer frame) throws IOException {
        try {
            if (input == null) {
                startSegment(frame.capturedNanos);
            }
            long interval = 1_000_000_000L / fps;
            if (nextOutputNanos == 0L) {
                nextOutputNanos = frame.capturedNanos;
            }
            while (lastFrame != null && nextOutputNanos + interval <= frame.capturedNanos) {
                long outputNanos = nextOutputNanos;
                if (input == null) {
                    startSegment(outputNanos);
                }
                writeFrame(lastFrame, outputNanos);
                nextOutputNanos = outputNanos + interval;
            }
            frame.data.position(0);
            frame.data.get(lastFrame, 0, FRAME_BYTES);
            long outputNanos = Math.max(nextOutputNanos, frame.capturedNanos);
            if (input == null) {
                startSegment(outputNanos);
            }
            writeFrame(lastFrame, outputNanos);
            nextOutputNanos = outputNanos + interval;
        } finally {
            release(frame);
        }
    }

    private void writeFrame(byte[] data, long timestampNanos) throws IOException {
        input.write(data);
        segmentEndNanos = timestampNanos;
        if (++segmentFrames >= fps) {
            completeCurrentSegment();
        }
    }

    private void startSegment(long timestampNanos) throws IOException {
        Files.createDirectories(spoolDirectory);
        currentSegment = spoolDirectory.resolve(String.format("segment-%016d.mp4", segmentSequence++));
        process = new ProcessBuilder(
                ffmpegPath, "-hide_banner", "-loglevel", "error", "-y",
                "-f", "rawvideo", "-pixel_format", "rgba", "-video_size", WIDTH + "x" + HEIGHT,
                "-framerate", Integer.toString(fps), "-i", "pipe:0", "-vf", "vflip",
                "-an", "-c:v", "libx264",
                "-preset", "ultrafast", "-crf", "23", "-g", Integer.toString(fps),
                "-keyint_min", Integer.toString(fps), "-sc_threshold", "0", currentSegment.toString())
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        input = process.getOutputStream();
        segmentFrames = 0;
        segmentStartNanos = timestampNanos;
        segmentEndNanos = timestampNanos;
        nextOutputNanos = timestampNanos;
        if (lastFrame == null) {
            lastFrame = new byte[FRAME_BYTES];
        }
    }

    private void completeCurrentSegment() throws IOException {
        if (input == null) {
            return;
        }
        Path completed = currentSegment;
        int exit = closeProcess(true);
        if (exit != 0 || !Files.isRegularFile(completed) || Files.size(completed) == 0) {
            Files.deleteIfExists(completed);
            return;
        }
        spool.addLast(new Segment(completed, segmentStartNanos, segmentEndNanos));
        trimSpool(segmentEndNanos);
    }

    private int closeProcess(boolean wait) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
            }
            input = null;
        }
        int exit = -1;
        Process active = process;
        if (active != null) {
            try {
                if (wait && active.waitFor(5, TimeUnit.SECONDS)) {
                    exit = active.exitValue();
                } else if (!wait || active.isAlive()) {
                    active.destroyForcibly();
                }
            } catch (InterruptedException ignored) {
                active.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
        process = null;
        currentSegment = null;
        segmentFrames = 0;
        nextOutputNanos = 0L;
        return exit;
    }

    private void applyTrigger() {
        if (!triggerRequested || triggerNanos != 0L) {
            return;
        }
        triggerRequested = false;
        triggerNanos = System.nanoTime();
    }

    private void completeTriggerIfReady(long now) throws IOException {
        if (triggerNanos == 0L || now < triggerNanos + POST_ROLL_NANOS) {
            return;
        }
        completeCurrentSegment();
        long from = triggerNanos - PRE_ROLL_NANOS;
        long through = triggerNanos + POST_ROLL_NANOS;
        List<Segment> snapshot = spool.stream()
                .filter(segment -> segment.endNanos() >= from && segment.startNanos() <= through)
                .toList();
        snapshot.forEach(Segment::retain);
        if (!snapshot.isEmpty() && !finalizer.submit(snapshot)) {
            snapshot.forEach(Segment::release);
        }
        triggerNanos = 0L;
        triggerRequested = false;
        trimSpool(now);
    }

    private void trimSpool(long now) {
        long keepAfter = now - MAX_SPOOL_NANOS;
        while (!spool.isEmpty() && spool.peekFirst().endNanos() < keepAfter) {
            spool.removeFirst().release();
        }
    }

    private void fail(Exception error) {
        accepting = false;
        running = false;
        Path incompleteSegment = currentSegment;
        closeProcess(false);
        finalizer.close();
        spool.forEach(Segment::release);
        spool.clear();
        try {
            if (incompleteSegment != null) {
                Files.deleteIfExists(incompleteSegment);
            }
        } catch (IOException ignored) {
        }
        clearStaleSpool();
        Aether.LOGGER.error("Clip encoder failed", error);
        ClipManager.onEncoderFailure(this);
    }

    private void clearStaleSpool() {
        if (!Files.isDirectory(spoolDirectory)) {
            return;
        }
        try (var paths = Files.list(spoolDirectory)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException e) {
            Aether.LOGGER.warn("Unable to clear stale clip spool", e);
        }
    }

    @Override
    public void close() {
        Path incompleteSegment = currentSegment;
        accepting = false;
        running = false;
        try {
            thread.join(12_000L);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (thread.isAlive()) {
            closeProcess(false);
            thread.interrupt();
        }
        finalizer.close();
        spool.forEach(Segment::release);
        spool.clear();
        try {
            if (incompleteSegment != null) {
                Files.deleteIfExists(incompleteSegment);
            }
        } catch (IOException ignored) {
        }
    }

    static final class FrameBuffer {
        final ByteBuffer data;
        long capturedNanos;

        FrameBuffer(ByteBuffer data) {
            this.data = data;
        }
    }

    static final class Segment {
        private final Path path;
        private final long startNanos;
        private final long endNanos;
        private final AtomicInteger references = new AtomicInteger(1);

        Segment(Path path, long startNanos, long endNanos) {
            this.path = path;
            this.startNanos = startNanos;
            this.endNanos = endNanos;
        }

        Path path() {
            return path;
        }

        long startNanos() {
            return startNanos;
        }

        long endNanos() {
            return endNanos;
        }

        void retain() {
            references.incrementAndGet();
        }

        void release() {
            if (references.decrementAndGet() != 0) {
                return;
            }
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }
}
