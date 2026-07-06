package dev.typicalfarmingmacro.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import dev.typicalfarmingmacro.util.TfmLang;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TfmProxyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type PROXY_LIST_TYPE = new TypeToken<List<TfmProxy>>() {
    }.getType();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("typicalfarmingmacro_proxies.json");

    private static final List<TfmProxy> PROXIES = new ArrayList<>();
    private static boolean enabled;
    private static int selectedIndex = -1;
    private static TfmProxy lastUsedProxy;

    private TfmProxyManager() {
    }

    public static synchronized void init() {
        load();
    }

    public static synchronized void load() {
        PROXIES.clear();
        enabled = false;
        selectedIndex = -1;
        lastUsedProxy = null;

        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            enabled = root.has("enabled") && root.get("enabled").getAsBoolean();
            selectedIndex = root.has("selectedIndex") ? root.get("selectedIndex").getAsInt() : -1;
            if (root.has("proxies") && root.get("proxies").isJsonArray()) {
                List<TfmProxy> loaded = GSON.fromJson(root.get("proxies"), PROXY_LIST_TYPE);
                if (loaded != null) {
                    for (TfmProxy proxy : loaded) {
                        if (proxy != null) {
                            PROXIES.add(proxy.copy());
                        }
                    }
                }
            }
            normalizeSelection();
            refreshProxyStatusButtons();
        } catch (Exception e) {
            System.err.println("[Tfm] Proxy config load failed: " + e.getMessage());
            PROXIES.clear();
            enabled = false;
            selectedIndex = -1;
        }
    }

    public static synchronized void save() {
        normalizeSelection();
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        root.addProperty("selectedIndex", selectedIndex);
        root.add("proxies", GSON.toJsonTree(PROXIES, PROXY_LIST_TYPE));

        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(CONFIG_PATH, GSON.toJson(root));
        } catch (Exception e) {
            System.err.println("[Tfm] Proxy config save failed: " + e.getMessage());
        }
    }

    public static synchronized List<TfmProxy> proxies() {
        return Collections.unmodifiableList(new ArrayList<>(PROXIES));
    }

    public static synchronized boolean isEnabled() {
        return enabled;
    }

    public static synchronized void setEnabled(boolean value) {
        enabled = value;
        save();
        refreshProxyStatusButtons();
    }

    public static synchronized int selectedIndex() {
        normalizeSelection();
        return selectedIndex;
    }

    public static synchronized void select(int index) {
        selectedIndex = index;
        normalizeSelection();
        save();
        refreshProxyStatusButtons();
    }

    public static synchronized void add(TfmProxy proxy) {
        PROXIES.add(proxy.copy());
        selectedIndex = PROXIES.size() - 1;
        save();
        refreshProxyStatusButtons();
    }

    public static synchronized void update(int index, TfmProxy proxy) {
        if (index < 0 || index >= PROXIES.size()) {
            return;
        }
        PROXIES.set(index, proxy.copy());
        save();
        refreshProxyStatusButtons();
    }

    public static synchronized void remove(int index) {
        if (index < 0 || index >= PROXIES.size()) {
            return;
        }
        PROXIES.remove(index);
        if (selectedIndex >= PROXIES.size()) {
            selectedIndex = PROXIES.size() - 1;
        }
        save();
        refreshProxyStatusButtons();
    }

    public static synchronized TfmProxy selectedProxy() {
        normalizeSelection();
        if (!enabled || selectedIndex < 0 || selectedIndex >= PROXIES.size()) {
            return null;
        }
        TfmProxy proxy = PROXIES.get(selectedIndex);
        return proxy.isValid() ? proxy.copy() : null;
    }

    public static synchronized void markConnectionProxy(TfmProxy proxy) {
        lastUsedProxy = proxy == null ? null : proxy.copy();
    }

    public static synchronized String selectedStatus() {
        TfmProxy proxy = selectedProxy();
        if (!enabled) {
            return TfmLang.localize("Proxy: off");
        }
        if (proxy == null) {
            return TfmLang.localize("Proxy: invalid");
        }
        return TfmLang.localize("Proxy: ") + proxy.displayName();
    }

    public static synchronized String lastUsedStatus() {
        return lastUsedProxy == null ? TfmLang.localize("none") : lastUsedProxy.address();
    }

    private static void normalizeSelection() {
        if (PROXIES.isEmpty()) {
            selectedIndex = -1;
            enabled = false;
        } else if (selectedIndex < 0 || selectedIndex >= PROXIES.size()) {
            selectedIndex = 0;
        }
    }

    public static void refreshProxyStatusButtons() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        refreshProxyStatusButtons(minecraft.screen);
    }

    public static void refreshProxyStatusButtons(Screen screen) {
        if (screen == null) {
            return;
        }
        if (!(screen instanceof TitleScreen) && !(screen instanceof JoinMultiplayerScreen)) {
            return;
        }

        String status = selectedStatus();
        Button candidate = null;
        for (var listener : screen.children()) {
            if (listener instanceof Button button && isProxyStatusButton(screen, button)) {
                candidate = button;
            }
        }
        if (candidate != null) {
            candidate.setMessage(Component.literal(status));
        }
    }

    private static boolean isProxyStatusButton(Screen screen, Button button) {
        int guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return button != null
                && ((screen instanceof TitleScreen && button.getWidth() == 200
                && button.getHeight() == 20
                && button.getX() == guiWidth / 2 - 100)
                || (screen instanceof JoinMultiplayerScreen && button.getWidth() == 180
                && button.getHeight() == 20
                && button.getY() == 6
                && button.getX() == guiWidth - 185));
    }
}
