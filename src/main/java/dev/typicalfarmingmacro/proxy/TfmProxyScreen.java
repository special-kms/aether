package dev.typicalfarmingmacro.proxy;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.bootstrap.TfmBootstrapHooks;
import dev.typicalfarmingmacro.renderer.TfmBackground;
import dev.typicalfarmingmacro.util.TfmLang;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TfmProxyScreen extends Screen {
    private static final int PANEL_W = 520;
    private static final int LIST_W = 210;
    private static final int ROW_H = 24;
    private static final int FIELD_W = 210;
    private static final int FIELD_H = 20;

    private final Screen parent;

    private EditBox nameField;
    private EditBox addressField;
    private EditBox usernameField;
    private EditBox passwordField;
    private Checkbox enabledBox;
    private Button typeButton;
    private Button saveButton;
    private Button deleteButton;
    private TfmProxy.ProxyType type = TfmProxy.ProxyType.SOCKS5;
    private int editingIndex = -1;
    private String message = "";

    private int panelX;
    private int panelY;
    private int listX;
    private int listY;
    private int formX;
    private int formY;

    public TfmProxyScreen(Screen parent) {
        super(Component.literal(TfmLang.localize("Proxy Manager")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(28, (height - 250) / 2);
        listX = panelX;
        listY = panelY + 34;
        formX = panelX + LIST_W + 28;
        formY = panelY + 34;

        editingIndex = TfmProxyManager.selectedIndex();
        List<TfmProxy> proxies = TfmProxyManager.proxies();
        TfmProxy selected = editingIndex >= 0 && editingIndex < proxies.size() ? proxies.get(editingIndex) : null;

        enabledBox = Checkbox.builder(Component.literal(TfmLang.localize("Enable proxy")), font)
                .pos(formX, formY)
                .selected(TfmProxyManager.isEnabled())
                .build();
        addRenderableWidget(enabledBox);

        nameField = field(formX, formY + 36, "Name", selected == null ? "" : selected.name());
        addressField = field(formX, formY + 76, "IP:PORT", selected == null ? "" : selected.address());
        usernameField = field(formX, formY + 116, "Username", selected == null ? "" : selected.username());
        passwordField = field(formX, formY + 156, "Password", selected == null ? "" : selected.password());
        type = selected == null ? TfmProxy.ProxyType.SOCKS5 : selected.type();

        typeButton = addRenderableWidget(Button.builder(Component.literal(TfmLang.localize(type.displayName())), button -> {
            type = type.next();
            button.setMessage(Component.literal(TfmLang.localize(type.displayName())));
        }).bounds(formX, formY + 196, 100, 20).build());

        saveButton = addRenderableWidget(Button.builder(Component.literal(TfmLang.localize("Save")), button -> saveCurrent())
                .bounds(formX + 110, formY + 196, 100, 20)
                .build());
        deleteButton = addRenderableWidget(Button.builder(Component.literal(TfmLang.localize("Delete")), button -> deleteCurrent())
                .bounds(formX + 220, formY + 196, 70, 20)
                .build());
        deleteButton.active = editingIndex >= 0;

        addRenderableWidget(Button.builder(Component.literal(TfmLang.localize("New")), button -> newProxy())
                .bounds(listX, panelY + 4, 64, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(TfmLang.localize("Use")), button -> useCurrent())
                .bounds(listX + 72, panelY + 4, 64, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(TfmLang.localize("Back")), button -> minecraft.setScreen(parent))
                .bounds(panelX + PANEL_W - 72, panelY + 4, 72, 20)
                .build());
    }

    private EditBox field(int x, int y, String hint, String value) {
        EditBox box = new EditBox(font, x, y, FIELD_W, FIELD_H, Component.literal(TfmLang.localize(hint)));
        box.setMaxLength(512);
        box.setValue(value);
        box.setHint(Component.literal(TfmLang.localize(hint)));
        addRenderableWidget(box);
        return box;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (TfmConfig.CUSTOM_UI_ENABLED.get()) {
            TfmBackground.INSTANCE.render(width, height, mouseX, mouseY);
        } else {
            super.extractBackground(graphics, mouseX, mouseY, partialTick);
        }
        graphics.centeredText(font, title, width / 2, panelY - 18, 0xFFFFFF);
        drawProxyListBackground(graphics, mouseX, mouseY);
        drawLabels(graphics);
        try (TfmBootstrapHooks.DisplayTransformScope ignored = TfmBootstrapHooks.suspendDisplayTransforms()) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        drawProxyListText(graphics);
        if (!message.isBlank()) {
            graphics.centeredText(font, message, width / 2, panelY + 266, 0xFFFF5555);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void drawProxyListBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<TfmProxy> proxies = TfmProxyManager.proxies();
        graphics.fill(listX, listY, listX + LIST_W, listY + 206, 0x88000000);
        if (proxies.isEmpty()) {
            return;
        }

        for (int i = 0; i < proxies.size(); i++) {
            int y = listY + i * ROW_H;
            if (y + ROW_H > listY + 206) {
                break;
            }
            boolean hovered = mouseX >= listX && mouseX <= listX + LIST_W && mouseY >= y && mouseY <= y + ROW_H;
            int color = i == editingIndex ? 0xAA3A3A3A : hovered ? 0x663A3A3A : 0x33000000;
            graphics.fill(listX + 2, y + 2, listX + LIST_W - 2, y + ROW_H - 2, color);
        }
    }

    private void drawProxyListText(GuiGraphicsExtractor graphics) {
        List<TfmProxy> proxies = TfmProxyManager.proxies();
        if (proxies.isEmpty()) {
            graphics.centeredText(font, TfmLang.localize("No proxies saved"), listX + LIST_W / 2, listY + 92, 0xFFAAAAAA);
            return;
        }

        int selected = TfmProxyManager.selectedIndex();
        for (int i = 0; i < proxies.size(); i++) {
            int y = listY + i * ROW_H;
            if (y + ROW_H > listY + 206) {
                break;
            }
            String prefix = i == selected && TfmProxyManager.isEnabled() ? "* " : "";
            graphics.text(font, prefix + proxies.get(i).displayName(), listX + 8, y + 5, 0xFFFFFFFF, true);
            graphics.text(font, proxies.get(i).shortStatus(), listX + 8, y + 15, 0xFFB0B0B0, true);
        }
    }

    private void drawLabels(GuiGraphicsExtractor graphics) {
        graphics.text(font, TfmLang.localize("Name"), formX, formY + 25, 0xAAAAAA, false);
        graphics.text(font, TfmLang.localize("IP:PORT"), formX, formY + 65, 0xAAAAAA, false);
        graphics.text(font, TfmLang.localize("Username"), formX, formY + 105, 0xAAAAAA, false);
        graphics.text(font, TfmLang.localize("Password"), formX, formY + 145, 0xAAAAAA, false);
        graphics.text(font, TfmLang.localize("Type"), formX, formY + 185, 0xAAAAAA, false);
        graphics.text(font, TfmProxyManager.selectedStatus(), listX, listY + 214, 0xAAAAAA, false);
        graphics.text(font, TfmLang.localize("Last used: ") + TfmProxyManager.lastUsedStatus(), listX, listY + 226, 0x777777, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0 && TfmConfig.CUSTOM_UI_ENABLED.get()) {
            TfmBackground.INSTANCE.addRipple((float) click.x(), (float) click.y());
        }

        if (click.button() == 0 && click.x() >= listX && click.x() <= listX + LIST_W
                && click.y() >= listY && click.y() <= listY + 206) {
            int index = (int) ((click.y() - listY) / ROW_H);
            if (index >= 0 && index < TfmProxyManager.proxies().size()) {
                loadProxy(index);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void loadProxy(int index) {
        List<TfmProxy> proxies = TfmProxyManager.proxies();
        if (index < 0 || index >= proxies.size()) {
            return;
        }
        TfmProxy proxy = proxies.get(index);
        editingIndex = index;
        nameField.setValue(proxy.name());
        addressField.setValue(proxy.address());
        usernameField.setValue(proxy.username());
        passwordField.setValue(proxy.password());
        type = proxy.type();
            typeButton.setMessage(Component.literal(TfmLang.localize(type.displayName())));
        deleteButton.active = true;
        message = "";
    }

    private void newProxy() {
        editingIndex = -1;
        nameField.setValue("");
        addressField.setValue("");
        usernameField.setValue("");
        passwordField.setValue("");
        type = TfmProxy.ProxyType.SOCKS5;
        typeButton.setMessage(Component.literal(TfmLang.localize(type.displayName())));
        deleteButton.active = false;
        message = "";
    }

    private void saveCurrent() {
        TfmProxy proxy = currentFormProxy();
        if (!proxy.isValid()) {
            message = TfmLang.localize("Proxy address must be host:port.");
            addressField.setFocused(true);
            return;
        }

        if (editingIndex >= 0) {
            TfmProxyManager.update(editingIndex, proxy);
        } else {
            TfmProxyManager.add(proxy);
            editingIndex = TfmProxyManager.selectedIndex();
            deleteButton.active = true;
        }
        TfmProxyManager.setEnabled(enabledBox.selected());
        TfmProxyManager.refreshProxyStatusButtons(parent);
        message = "";
    }

    private void useCurrent() {
        saveCurrent();
        if (message.isBlank() && editingIndex >= 0) {
            TfmProxyManager.select(editingIndex);
            TfmProxyManager.setEnabled(true);
            TfmProxyManager.refreshProxyStatusButtons(parent);
            enabledBox = Checkbox.builder(Component.literal(TfmLang.localize("Enable proxy")), font)
                    .pos(formX, formY)
                    .selected(true)
                    .build();
            rebuildWidgets();
        }
    }

    private void deleteCurrent() {
        if (editingIndex < 0) {
            return;
        }
        TfmProxyManager.remove(editingIndex);
        TfmProxyManager.refreshProxyStatusButtons(parent);
        newProxy();
    }

    private TfmProxy currentFormProxy() {
        return new TfmProxy(
                nameField.getValue(),
                addressField.getValue(),
                type,
                usernameField.getValue(),
                passwordField.getValue());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

