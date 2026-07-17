package dev.aether.modules.clips;

import dev.aether.Aether;

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
import java.util.concurrent.atomic.AtomicReference;

final class ClipEncoder implements AutoCloseable {
    private static final long PRE_ROLL_NANOS = TimeUnit.SECONDS.toNanos(30);
    static final long POST_ROLL_NANOS = TimeUnit.SECONDS.toNanos(10);
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
    private final int width;
    private final int height;
    private final int frameBytes;
    private final AtomicReference<TriggerRequest> pendingRequest = new AtomicReference<>();
    private volatile boolean running = true;
    private volatile boolean accepting = true;
    private Process process;
    private OutputStream input;
    private long segmentStartNanos;
    private long segmentEndNanos;
    private long nextOutputNanos;
    private TriggerRequest activeRequest;
    private int segmentFrames;
    private long segmentSequence;
    private byte[] lastFrame;
    private Path currentSegment;

    ClipEncoder(int fps, String ffmpegPath, ClipResolution resolution) {
        this.fps = fps;
        this.ffmpegPath = ffmpegPath;
        width = resolution.width();
        height = resolution.height();
        frameBytes = width * height * 4;
        spoolDirectory = ClipManager.getClipsDirectory().resolve("spool")
                .resolve(UUID.randomUUID().toString());
        finalizer = new ClipFinalizer(ffmpegPath, spoolDirectory);
        for (int i = 0; i < BUFFER_COUNT; i++) {
            available.add(new FrameBuffer(ByteBuffer.allocateDirect(frameBytes)));
        }
        thread = Thread.ofPlatform().name("Aether Clip Encoder").daemon(true).start(this::run);
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    int frameBytes() {
        return frameBytes;
    }

    FrameBuffer acquireBuffer() {
        return accepting ? available.poll() : null;
    }

    void submit(FrameBuffer buffer, long capturedNanos) {
        buffer.capturedNanos = capturedNanos;
        buffer.data.position(0).limit(frameBytes);
        if (!accepting || !frames.offer(buffer)) {
            release(buffer);
        }
    }

    void release(FrameBuffer buffer) {
        buffer.data.clear();
        available.offer(buffer);
    }

    void trigger(TriggerRequest request) {
        pendingRequest.accumulateAndGet(request, TriggerRequest::merge);
    }

    private void run() {
        try {
            while (running || !frames.isEmpty()) {
                applyPendingRequests();
                completeTriggerIfReady(System.nanoTime());
                FrameBuffer frame = frames.poll(100, TimeUnit.MILLISECONDS);
                if (frame != null) {
                    encodePaced(frame);
                }
                applyPendingRequests();
                completeTriggerIfReady(System.nanoTime());
            }
            applyPendingRequests();
            completeCurrentSegment();
            completeTriggerIfReady(true, System.nanoTime());
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
            frame.data.get(lastFrame, 0, frameBytes);
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
                "-f", "rawvideo", "-pixel_format", "rgba", "-video_size", width + "x" + height,
                "-framerate", Integer.toString(fps), "-i", "pipe:0", "-vf", "vflip,format=yuv420p",
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
            lastFrame = new byte[frameBytes];
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

    private void applyPendingRequests() {
        TriggerRequest request = pendingRequest.getAndSet(null);
        if (request != null) {
            activeRequest = TriggerRequest.merge(activeRequest, request);
        }
    }

    private void completeTriggerIfReady(long now) throws IOException {
        completeTriggerIfReady(false, now);
    }

    private void completeTriggerIfReady(boolean force, long now) throws IOException {
        TriggerRequest request = activeRequest;
        if (request == null || (!force && now < request.throughNanos())) {
            return;
        }
        completeCurrentSegment();
        long from = saturatingSubtract(request.requestedAtNanos(), PRE_ROLL_NANOS);
        List<Segment> snapshot = spool.stream()
                .filter(segment -> segment.endNanos() >= from && segment.startNanos() <= request.throughNanos())
                .toList();
        snapshot.forEach(Segment::retain);
        if (!snapshot.isEmpty() && !finalizer.submit(snapshot)) {
            snapshot.forEach(Segment::release);
        }
        activeRequest = null;
        trimSpool(now);
    }

    private static long saturatingSubtract(long value, long decrement) {
        return value < Long.MIN_VALUE + decrement ? Long.MIN_VALUE : value - decrement;
    }

    private void trimSpool(long now) {
        long keepAfter = saturatingSubtract(now, MAX_SPOOL_NANOS);
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

    record TriggerRequest(long requestedAtNanos, long throughNanos) {
        static TriggerRequest merge(TriggerRequest first, TriggerRequest second) {
            if (first == null) {
                return second;
            }
            if (second == null) {
                return first;
            }
            return new TriggerRequest(
                    Math.min(first.requestedAtNanos, second.requestedAtNanos),
                    Math.max(first.throughNanos, second.throughNanos)
            );
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
