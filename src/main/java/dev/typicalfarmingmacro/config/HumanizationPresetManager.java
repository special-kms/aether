package dev.typicalfarmingmacro.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.typicalfarmingmacro.config.entries.ConfigEntry;
import dev.typicalfarmingmacro.util.TfmResources;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HumanizationPresetManager {
    private static final Gson PRESET_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> PRESET_OPTIONS = List.of("Safe", "Normal", "Efficient");
    private static final List<String> BUNDLED_PRESET_IDS = List.of("safe", "normal", "efficient");
    private static final String RESOURCE_BASE = "assets/typicalfarmingmacro/humanization-presets/";
    private static final Path PRESET_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("typicalfarmingmacro")
            .resolve("humanization-presets");
    private static final Map<String, ConfigEntry<?>> PRESET_ENTRIES = createPresetEntries();

    private HumanizationPresetManager() {
    }

    public static void init() {
        ensurePresetDirectory();
        for (String presetId : BUNDLED_PRESET_IDS) {
            ensureBundledPreset(presetId);
            syncBundledPresetDefaults(presetId);
        }
    }

    public static List<String> getPresetOptions() {
        return PRESET_OPTIONS;
    }

    public static int getSelectedPresetIndex() {
        String selectedPreset = TfmConfig.HUMANIZATION_PRESET.get();
        if (selectedPreset == null || selectedPreset.isBlank()) {
            return 1;
        }

        for (int i = 0; i < PRESET_OPTIONS.size(); i++) {
            if (PRESET_OPTIONS.get(i).equalsIgnoreCase(selectedPreset)) {
                return i;
            }
        }

        return 1;
    }

    public static void applyPresetByIndex(int index) {
        if (index < 0 || index >= PRESET_OPTIONS.size()) {
            return;
        }

        applyPreset(PRESET_OPTIONS.get(index));
    }

    public static void reapplySelectedPreset() {
        applyPreset(PRESET_OPTIONS.get(getSelectedPresetIndex()));
    }

    public static void openPresetFolder() {
        init();
        try {
            if (isWindows()) {
                new ProcessBuilder("explorer.exe", PRESET_DIR.toAbsolutePath().toString()).start();
                return;
            }

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(PRESET_DIR.toFile());
            }
        } catch (IOException e) {
            System.err.println("[Tfm] Failed to open humanization preset folder: " + e.getMessage());
        }
    }

    private static void applyPreset(String presetName) {
        init();

        String presetId = normalizePresetId(presetName);
        Path presetPath = PRESET_DIR.resolve(presetId + ".json");
        if (!Files.exists(presetPath)) {
            System.err.println("[Tfm] Humanization preset file missing: " + presetPath.toAbsolutePath());
            return;
        }

        try (Reader reader = Files.newBufferedReader(presetPath)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                throw new IllegalStateException("root value is not a JSON object");
            }

            JsonObject preset = root.getAsJsonObject();
            Config.suspendAutosave(() -> {
                for (Map.Entry<String, ConfigEntry<?>> entry : PRESET_ENTRIES.entrySet()) {
                    JsonElement value = preset.get(entry.getKey());
                    if (value != null && !value.isJsonNull()) {
                        entry.getValue().fromJson(value);
                    }
                }
                TfmConfig.HUMANIZATION_PRESET.set(presetId.toUpperCase(Locale.ROOT));
            });
            TfmConfig.save();
        } catch (Exception e) {
            System.err.println("[Tfm] Failed to apply humanization preset '" + presetId + "': " + e.getMessage());
        }
    }

    private static Map<String, ConfigEntry<?>> createPresetEntries() {
        Map<String, ConfigEntry<?>> entries = new LinkedHashMap<>();
        addEntry(entries, TfmConfig.MACRO_LANE_SWITCH_DELAY_MIN);
        addEntry(entries, TfmConfig.MACRO_LANE_SWITCH_DELAY_MAX);
        addEntry(entries, TfmConfig.REWARP_DELAY_MIN);
        addEntry(entries, TfmConfig.REWARP_DELAY_MAX);
        addEntry(entries, TfmConfig.PEST_CHAT_TRIGGER_DELAY_MIN);
        addEntry(entries, TfmConfig.PEST_CHAT_TRIGGER_DELAY_MAX);
        addEntry(entries, TfmConfig.PEST_EXCHANGE_DELAY_MIN);
        addEntry(entries, TfmConfig.PEST_EXCHANGE_DELAY_MAX);
        addEntry(entries, TfmConfig.PEST_AOTV_DELAY_MIN);
        addEntry(entries, TfmConfig.PEST_AOTV_DELAY_MAX);
        addEntry(entries, TfmConfig.ROD_SWAP_DELAY_MIN);
        addEntry(entries, TfmConfig.ROD_SWAP_DELAY_MAX);
        addEntry(entries, TfmConfig.GUI_FIRST_CLICK_DELAY_MIN);
        addEntry(entries, TfmConfig.GUI_FIRST_CLICK_DELAY_MAX);
        addEntry(entries, TfmConfig.GUI_CLICK_DELAY_MIN);
        addEntry(entries, TfmConfig.GUI_CLICK_DELAY_MAX);
        addEntry(entries, TfmConfig.PICK_UP_STASH_DELAY_MIN);
        addEntry(entries, TfmConfig.PICK_UP_STASH_DELAY_MAX);
        addEntry(entries, TfmConfig.JUNK_ITEM_DROP_DELAY_MIN);
        addEntry(entries, TfmConfig.JUNK_ITEM_DROP_DELAY_MAX);
        addEntry(entries, TfmConfig.GEORGE_POST_SELL_DELAY_MIN_MS);
        addEntry(entries, TfmConfig.GEORGE_POST_SELL_DELAY_MAX_MS);
        addEntry(entries, TfmConfig.FARM_WHILE_CALLING_GEORGE);
        addEntry(entries, TfmConfig.BAZAAR_DELAY_MIN);
        addEntry(entries, TfmConfig.BAZAAR_DELAY_MAX);
        addEntry(entries, TfmConfig.ROTATION_TIME);
        addEntry(entries, TfmConfig.ROTATION_DYNAMIC_DURATION_MS_PER_DEGREE);
        addEntry(entries, TfmConfig.ROTATION_EASE_IN);
        addEntry(entries, TfmConfig.ROTATION_EASE_IN_FACTOR);
        addEntry(entries, TfmConfig.ROTATION_EASE_OUT);
        addEntry(entries, TfmConfig.ROTATION_EASE_OUT_FACTOR);
        addEntry(entries, TfmConfig.ROTATION_TRACKING_NOISE_MIN);
        addEntry(entries, TfmConfig.ROTATION_TRACKING_NOISE_MAX);
        addEntry(entries, TfmConfig.MACRO_CUSTOM_PITCH_HUMANIZATION);
        addEntry(entries, TfmConfig.MACRO_CUSTOM_YAW_HUMANIZATION);
        addEntry(entries, TfmConfig.AOTV_ROOF_PITCH_HUMANIZATION);
        addEntry(entries, TfmConfig.PEST_FOV_RANGE);
        addEntry(entries, TfmConfig.PEST_ABOVE_TARGET_PITCH_MIN);
        addEntry(entries, TfmConfig.PEST_ABOVE_TARGET_PITCH_MAX);
        addEntry(entries, TfmConfig.VISITOR_FOV_RANGE);
        addEntry(entries, TfmConfig.PEST_EXCHANGE_FOV_RANGE);
        return entries;
    }

    private static void addEntry(Map<String, ConfigEntry<?>> entries, ConfigEntry<?> entry) {
        entries.put(entry.getKey(), entry);
    }

    private static void ensurePresetDirectory() {
        try {
            Files.createDirectories(PRESET_DIR);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create humanization preset directory: " + PRESET_DIR, e);
        }
    }

    private static void ensureBundledPreset(String presetId) {
        Path presetPath = PRESET_DIR.resolve(presetId + ".json");
        if (Files.exists(presetPath)) {
            return;
        }

        String resourcePath = RESOURCE_BASE + presetId + ".json";
        try (InputStream inputStream = TfmResources.open(resourcePath)) {
            if (inputStream == null) {
                System.err.println("[Tfm] Bundled humanization preset missing: " + resourcePath);
                return;
            }

            Files.copy(inputStream, presetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[Tfm] Failed to copy bundled humanization preset '" + presetId + "': " + e.getMessage());
        }
    }

    private static void syncBundledPresetDefaults(String presetId) {
        Path presetPath = PRESET_DIR.resolve(presetId + ".json");
        if (!Files.exists(presetPath)) {
            return;
        }

        String resourcePath = RESOURCE_BASE + presetId + ".json";
        try (InputStream inputStream = TfmResources.open(resourcePath)) {
            if (inputStream == null) {
                System.err.println("[Tfm] Bundled humanization preset missing: " + resourcePath);
                return;
            }

            JsonObject bundledPreset = parsePresetJson(inputStream);
            if (bundledPreset == null) {
                System.err.println("[Tfm] Failed to parse bundled humanization preset: " + resourcePath);
                return;
            }

            JsonObject diskPreset;
            try (Reader reader = Files.newBufferedReader(presetPath)) {
                JsonElement diskRoot = JsonParser.parseReader(reader);
                if (diskRoot == null || !diskRoot.isJsonObject()) {
                    return;
                }
                diskPreset = diskRoot.getAsJsonObject();
            }

            boolean updated = false;
            for (Map.Entry<String, JsonElement> entry : bundledPreset.entrySet()) {
                if (!diskPreset.has(entry.getKey())) {
                    diskPreset.add(entry.getKey(), entry.getValue());
                    updated = true;
                }
            }

            if (updated) {
                Files.writeString(presetPath, PRESET_GSON.toJson(diskPreset));
            }
        } catch (Exception e) {
            System.err.println("[Tfm] Failed to sync bundled humanization preset '" + presetId + "': " + e.getMessage());
        }
    }

    private static JsonObject parsePresetJson(InputStream inputStream) throws IOException {
        try (Reader reader = new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonObject()) {
                return null;
            }
            return root.getAsJsonObject();
        }
    }

    private static String normalizePresetId(String presetName) {
        return presetName == null ? "normal" : presetName.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}

