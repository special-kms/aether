package dev.aether.modules.clips;

import dev.aether.Aether;
import dev.aether.config.AetherConfig;
import dev.aether.notification.NotificationManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FfmpegInstaller {
    private static final String VERSION = "8.1.2";
    private static final URI DOWNLOAD_URI = URI.create(
            "https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-8.1.2-essentials_build.zip");
    private static final String DOWNLOAD_SHA256 =
            "db580001caa24ac104c8cb856cd113a87b0a443f7bdf47d8c12b1d740584a2ec";
    private static final long MAX_DOWNLOAD_BYTES = 130L * 1024L * 1024L;
    private static final long MAX_EXTRACTED_BYTES = 256L * 1024L * 1024L;
    private static final int MAX_ZIP_ENTRIES = 4096;
    private static final long MAX_PROBE_OUTPUT_BYTES = 4L * 1024L * 1024L;
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private FfmpegInstaller() {
    }

    public static void findFfmpeg() {
        runSingleFlight("FFmpeg Discovery", () -> {
            Path found = discoverLocalFfmpeg();
            if (found == null) {
                notifyWarning("FFmpeg Not Found", "Install FFmpeg or configure it outside the Aether menu, then try again.");
                return;
            }
            ClipManager.setValidatedFfmpegPath(found);
            notifySuccess("FFmpeg Found", found.toString());
        });
    }

    public static void installFfmpeg() {
        runSingleFlight("FFmpeg Install", () -> {
            if (!isWindowsX64()) {
                throw new IOException("Managed installation is only available on Windows x64.");
            }
            Path managed = managedPath();
            if (isValid(managed)) {
                ClipManager.setValidatedFfmpegPath(managed);
                notifySuccess("FFmpeg Ready", managed.toString());
                return;
            }
            Path installed = downloadAndInstall(managed);
            ClipManager.setValidatedFfmpegPath(installed);
            notifySuccess("FFmpeg Installed", installed.toString());
        });
    }

    static void validate(Path executable) throws IOException {
        Path normalized = executable.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IOException("FFmpeg executable does not exist: " + normalized);
        }
        String version = probe(normalized, "-hide_banner", "-version");
        if (!version.toLowerCase(Locale.ROOT).contains("ffmpeg version")) {
            throw new IOException("The selected executable did not report an FFmpeg version.");
        }
        String encoders = probe(normalized, "-hide_banner", "-encoders").toLowerCase(Locale.ROOT);
        boolean hasLibx264 = encoders.lines()
                .map(String::trim)
                .map(line -> line.split("\\s+"))
                .anyMatch(tokens -> tokens.length >= 2 && tokens[1].equals("libx264"));
        if (!hasLibx264) {
            throw new IOException("FFmpeg does not include the libx264 encoder.");
        }
        String formats = probe(normalized, "-hide_banner", "-formats").toLowerCase(Locale.ROOT);
        requireFormat(formats, "mp4", 'e');
        requireFormat(formats, "concat", 'd');
    }

    private static Path discoverLocalFfmpeg() {
        for (Path candidate : discoveryCandidates()) {
            if (isValid(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static List<Path> discoveryCandidates() {
        Set<Path> candidates = new LinkedHashSet<>();
        String configured = AetherConfig.CLIPS_FFMPEG_PATH.get();
        if (configured != null && !configured.isBlank() && !configured.trim().equalsIgnoreCase("ffmpeg")) {
            try {
                Path path = Path.of(configured.trim());
                candidates.add(path.isAbsolute() ? path : gameDir().resolve(path));
            } catch (RuntimeException e) {
                Aether.LOGGER.debug("Configured FFmpeg path is invalid: {}", configured, e);
            }
        }

        String executable = isWindows() ? "ffmpeg.exe" : "ffmpeg";
        String pathValue = System.getenv("PATH");
        if (pathValue != null) {
            for (String directory : pathValue.split(java.util.regex.Pattern.quote(System.getProperty("path.separator")))) {
                if (!directory.isBlank()) {
                    try {
                        candidates.add(Path.of(directory).resolve(executable));
                    } catch (RuntimeException e) {
                        Aether.LOGGER.debug("Ignoring invalid PATH entry: {}", directory, e);
                    }
                }
            }
        }

        if (isWindows()) {
            addWindowsCandidates(candidates, executable);
        } else {
            candidates.add(Path.of("/usr/local/bin/ffmpeg"));
            candidates.add(Path.of("/usr/bin/ffmpeg"));
            candidates.add(Path.of("/opt/homebrew/bin/ffmpeg"));
            candidates.add(Path.of("/opt/local/bin/ffmpeg"));
        }
        candidates.add(managedPath());
        return new ArrayList<>(candidates);
    }

    private static void addWindowsCandidates(Set<Path> candidates, String executable) {
        addEnvironmentCandidate(candidates, "ProgramFiles", "ffmpeg/bin/" + executable);
        addEnvironmentCandidate(candidates, "ProgramFiles(x86)", "ffmpeg/bin/" + executable);
        addEnvironmentCandidate(candidates, "ChocolateyInstall", "bin/" + executable);
        addEnvironmentCandidate(candidates, "USERPROFILE", "scoop/apps/ffmpeg/current/bin/" + executable);
        addEnvironmentCandidate(candidates, "LOCALAPPDATA", "Microsoft/WinGet/Links/" + executable);
    }

    private static void addEnvironmentCandidate(Set<Path> candidates, String variable, String suffix) {
        String base = System.getenv(variable);
        if (base != null && !base.isBlank()) {
            try {
                candidates.add(Path.of(base).resolve(suffix));
            } catch (RuntimeException e) {
                Aether.LOGGER.debug("Ignoring invalid {} value: {}", variable, base, e);
            }
        }
    }

    private static Path downloadAndInstall(Path target) throws IOException, InterruptedException {
        Path directory = target.getParent();
        Files.createDirectories(directory);
        Path lockPath = directory.resolve("install.lock");
        String stagingId = UUID.randomUUID().toString();
        Path zipPart = directory.resolve("ffmpeg-" + VERSION + "-" + stagingId + ".zip.part");
        Path executablePart = directory.resolve("ffmpeg-" + stagingId + ".exe.part");
        try (FileChannel lockChannel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = lockChannel.lock()) {
            if (isValid(target)) {
                return target.toAbsolutePath().normalize();
            }
            try {
                download(zipPart);
                extractExecutable(zipPart, executablePart);
                validate(executablePart);
                try {
                    Files.move(executablePart, target, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    throw new IOException("The FFmpeg install directory does not support atomic replacement.", e);
                }
                return target.toAbsolutePath().normalize();
            } finally {
                Files.deleteIfExists(zipPart);
                Files.deleteIfExists(executablePart);
            }
        }
    }

    private static void download(Path destination) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(DOWNLOAD_URI)
                .timeout(Duration.ofMinutes(3))
                .header("User-Agent", "aether-mod")
                .GET()
                .build();
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IOException("FFmpeg download returned HTTP " + response.statusCode() + '.');
        }
        long declaredLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (declaredLength > MAX_DOWNLOAD_BYTES) {
            response.body().close();
            throw new IOException("FFmpeg download exceeded the 130 MiB limit.");
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IOException("SHA-256 is unavailable.", e);
        }
        long total = 0L;
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = response.body(); var output = Files.newOutputStream(destination)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (read == 0) {
                    int single = input.read();
                    if (single == -1) {
                        break;
                    }
                    buffer[0] = (byte) single;
                    read = 1;
                }
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new IOException("FFmpeg download exceeded the 130 MiB limit.");
                }
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        }
        String actualHash = HexFormat.of().formatHex(digest.digest());
        if (!DOWNLOAD_SHA256.equals(actualHash)) {
            throw new IOException("FFmpeg download failed SHA-256 verification.");
        }
    }

    private static void extractExecutable(Path archive, Path destination) throws IOException {
        int entries = 0;
        boolean extracted = false;
        byte[] buffer = new byte[64 * 1024];
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ZIP_ENTRIES) {
                    throw new IOException("FFmpeg archive contains too many entries.");
                }
                String name = entry.getName().replace('\\', '/');
                Path safeName = Path.of(name).normalize();
                if (safeName.isAbsolute() || name.startsWith("/") || name.matches("^[A-Za-z]:.*")
                        || safeName.startsWith("..")) {
                    throw new IOException("FFmpeg archive contains an unsafe path.");
                }
                if (!entry.isDirectory() && (name.equals("bin/ffmpeg.exe") || name.endsWith("/bin/ffmpeg.exe"))) {
                    if (extracted) {
                        throw new IOException("FFmpeg archive contains duplicate executables.");
                    }
                    long total = 0L;
                    try (var output = Files.newOutputStream(destination)) {
                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                            if (read == 0) {
                                int single = zip.read();
                                if (single == -1) {
                                    break;
                                }
                                buffer[0] = (byte) single;
                                read = 1;
                            }
                            total += read;
                            if (total > MAX_EXTRACTED_BYTES) {
                                throw new IOException("Extracted FFmpeg exceeded the size limit.");
                            }
                            output.write(buffer, 0, read);
                        }
                    }
                    extracted = true;
                }
                zip.closeEntry();
            }
        }
        if (!extracted) {
            throw new IOException("FFmpeg archive did not contain bin/ffmpeg.exe.");
        }
    }

    private static String probe(Path executable, String... arguments) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        java.util.concurrent.atomic.AtomicReference<IOException> readFailure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread reader = Thread.ofPlatform().name("Aether FFmpeg Probe Output").daemon(true).start(() -> {
            byte[] buffer = new byte[8192];
            long total = 0L;
            try (InputStream input = process.getInputStream()) {
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (read == 0) {
                        int single = input.read();
                        if (single == -1) {
                            break;
                        }
                        buffer[0] = (byte) single;
                        read = 1;
                    }
                    total += read;
                    if (total > MAX_PROBE_OUTPUT_BYTES) {
                        throw new IOException("FFmpeg validation produced too much output.");
                    }
                    output.write(buffer, 0, read);
                }
            } catch (IOException e) {
                readFailure.set(e);
                process.destroyForcibly();
            }
        });
        boolean exited;
        try {
            exited = process.waitFor(PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                throw new IOException("FFmpeg validation timed out.");
            }
            reader.join(2_000L);
            if (reader.isAlive()) {
                process.destroyForcibly();
                throw new IOException("FFmpeg validation output did not close.");
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            try {
                process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException suppressed) {
                e.addSuppressed(suppressed);
            }
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg validation was interrupted.", e);
        }
        IOException outputFailure = readFailure.get();
        if (outputFailure != null) {
            throw outputFailure;
        }
        if (process.exitValue() != 0) {
            throw new IOException("FFmpeg validation exited with code " + process.exitValue() + '.');
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private static void requireFormat(String formats, String format, char capability) throws IOException {
        String capabilityString = String.valueOf(capability);
        boolean present = formats.lines()
                .map(String::stripLeading)
                .filter(line -> line.length() >= 3 && line.charAt(0) != '-')
                .anyMatch(line -> line.substring(0, Math.min(2, line.length())).toLowerCase(Locale.ROOT)
                        .contains(capabilityString)
                        && line.substring(Math.min(2, line.length())).stripLeading()
                        .matches("(?:[^\\s,]+,)*" + java.util.regex.Pattern.quote(format) + "(?:,|\\s).*"));
        if (!present) {
            throw new IOException("FFmpeg does not include required " + format + " format support.");
        }
    }

    private static boolean isValid(Path candidate) {
        try {
            validate(candidate);
            return true;
        } catch (Exception e) {
            Aether.LOGGER.debug("FFmpeg candidate rejected: {}", candidate, e);
            return false;
        }
    }

    private static void runSingleFlight(String threadName, CheckedAction action) {
        if (!BUSY.compareAndSet(false, true)) {
            notifyInfo("FFmpeg Setup Busy", "Another FFmpeg operation is already in progress.");
            return;
        }
        Thread.ofPlatform().name("Aether " + threadName).daemon(true).start(() -> {
            try {
                action.run();
            } catch (Exception e) {
                Aether.LOGGER.warn("{} failed", threadName, e);
                notifyError(threadName + " Failed", messageOf(e));
            } finally {
                BUSY.set(false);
            }
        });
    }

    private static String messageOf(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static void notifyInfo(String title, String message) {
        notifyOnClient(() -> NotificationManager.info(title, message));
    }

    private static void notifySuccess(String title, String message) {
        notifyOnClient(() -> NotificationManager.success(title, message));
    }

    private static void notifyWarning(String title, String message) {
        notifyOnClient(() -> NotificationManager.warning(title, message));
    }

    private static void notifyError(String title, String message) {
        notifyOnClient(() -> NotificationManager.error(title, message));
    }

    private static void notifyOnClient(Runnable notification) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(notification);
        }
    }

    private static Path managedPath() {
        return gameDir().resolve("aether/tools/ffmpeg/").resolve(VERSION)
                .resolve("windows-x64/ffmpeg.exe").toAbsolutePath().normalize();
    }

    private static Path gameDir() {
        return FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
    }

    private static boolean isWindowsX64() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return isWindows() && (arch.equals("amd64") || arch.equals("x86_64"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
