package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.hud.HudEditScreen;
import dev.typicalfarmingmacro.modules.visuals.StreamerModeManager;
import dev.typicalfarmingmacro.ui.settings.ActionSetting;
import dev.typicalfarmingmacro.ui.settings.ColorSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.MultiDropdownSetting;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class HudVisualsRegistryProvider extends AbstractVisualsRegistryProvider {
    public HudVisualsRegistryProvider() {
        super(0);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();

        groups.add(SettingGroup.alwaysOn(
                        "HUD Settings",
                        "Configure overlay style and layout")
                .add(new MultiDropdownSetting("HUD Themes",
                        List.of("Main", "Watermark"),
                        () -> TfmConfig.HUD_THEME.get(),
                        i -> {
                            TfmConfig.HUD_THEME.set(i);
                            TfmConfig.save();
                        }))
                .add(new ActionSetting("Edit HUD Layout", () -> Minecraft.getInstance().setScreen(new HudEditScreen()))));

        groups.add(SettingGroup.alwaysOn(
                        "HUD Visibility",
                        "Controls when HUD overlays are shown")
                .add(new ToggleSetting("Only Show While Macro Running",
                        () -> TfmConfig.HUD_ONLY_WHILE_MACRO_RUNNING.get(),
                        v -> {
                            TfmConfig.HUD_ONLY_WHILE_MACRO_RUNNING.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Only Show HUDs In Garden",
                        () -> TfmConfig.GUI_ONLY_IN_GARDEN.get(),
                        v -> {
                            TfmConfig.GUI_ONLY_IN_GARDEN.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Show Task HUDs Outside Garden",
                        () -> TfmConfig.SHOW_HUD_OUTSIDE_GARDEN.get(),
                        v -> {
                            TfmConfig.SHOW_HUD_OUTSIDE_GARDEN.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> !TfmConfig.GUI_ONLY_IN_GARDEN.get())));

        groups.add(SettingGroup.of(
                        "Streamer Mode",
                        "Hides Tfm chat, notifications, overlays, HUDs, and world visuals",
                        StreamerModeManager::isEnabled,
                        StreamerModeManager::setEnabled));

        groups.add(SettingGroup.alwaysOn(
                        "Main Status HUD",
                        "Settings for the all-in-one main status card")
                .add(new ToggleSetting("Gradient",
                        () -> TfmConfig.MAIN_STATUS_GRADIENT.get(),
                        v -> { TfmConfig.MAIN_STATUS_GRADIENT.set(v); TfmConfig.save(); }))
                .add(new ColorSetting("Gradient Left",
                        () -> TfmConfig.MAIN_STATUS_GRADIENT_LEFT.get(),
                        v -> { TfmConfig.MAIN_STATUS_GRADIENT_LEFT.set(v); TfmConfig.save(); })
                        .visibleWhen(() -> TfmConfig.MAIN_STATUS_GRADIENT.get()))
                .add(new ColorSetting("Gradient Right",
                        () -> TfmConfig.MAIN_STATUS_GRADIENT_RIGHT.get(),
                        v -> { TfmConfig.MAIN_STATUS_GRADIENT_RIGHT.set(v); TfmConfig.save(); })
                        .visibleWhen(() -> TfmConfig.MAIN_STATUS_GRADIENT.get())));

        groups.add(SettingGroup.alwaysOn(
                        "Debug HUD",
                        "Developer overlays for macro state and task tracking")
                .add(new ToggleSetting("Macro HUD",
                        () -> TfmConfig.SHOW_HUD.get(),
                        v -> {
                            TfmConfig.SHOW_HUD.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Intermediaries HUD",
                        () -> TfmConfig.SHOW_INTERMEDIARIES_HUD.get(),
                        v -> {
                            TfmConfig.SHOW_INTERMEDIARIES_HUD.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Mid Farming HUD",
                        () -> TfmConfig.SHOW_MID_FARMING_HUD.get(),
                        v -> {
                            TfmConfig.SHOW_MID_FARMING_HUD.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Failsafes HUD",
                        () -> TfmConfig.SHOW_FAILSAFES_HUD.get(),
                        v -> {
                            TfmConfig.SHOW_FAILSAFES_HUD.set(v);
                            TfmConfig.save();
                        })));

        groups.add(SettingGroup.alwaysOn(
                        "Inventory HUD",
                        "Configure the inventory preview overlay")
                .add(new ToggleSetting("Inventory HUD",
                        () -> TfmConfig.SHOW_INVENTORY_HUD.get(),
                        v -> {
                            TfmConfig.SHOW_INVENTORY_HUD.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Show Player Model",
                        () -> TfmConfig.INVENTORY_HUD_SHOW_PLAYER_MODEL.get(),
                        v -> {
                            TfmConfig.INVENTORY_HUD_SHOW_PLAYER_MODEL.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Show Armor",
                        () -> TfmConfig.INVENTORY_HUD_SHOW_ARMOR.get(),
                        v -> {
                            TfmConfig.INVENTORY_HUD_SHOW_ARMOR.set(v);
                            TfmConfig.save();
                        })));

        SettingGroup discordStatus = SettingGroup.of(
                "Discord Status",
                "Sends macro status updates to a Discord webhook",
                () -> TfmConfig.SEND_DISCORD_STATUS.get(),
                v -> {
                    TfmConfig.SEND_DISCORD_STATUS.set(v);
                    TfmConfig.save();
                });
        discordStatus.add(new TextSetting("Webhook URL", "https://discord.com/api/webhooks/...",
                () -> TfmConfig.DISCORD_WEBHOOK_URL.get(),
                v -> {
                    TfmConfig.DISCORD_WEBHOOK_URL.set(v);
                    TfmConfig.save();
                })
                .visibleWhen(() -> TfmConfig.SEND_DISCORD_STATUS.get()));
        discordStatus.add(new SliderSetting("Update Interval", 1, 60,
                () -> (float) TfmConfig.DISCORD_STATUS_UPDATE_TIME.get(),
                v -> {
                    TfmConfig.DISCORD_STATUS_UPDATE_TIME.set(Math.round(v));
                    TfmConfig.save();
                })
                .withDecimals(0).withSuffix(" min")
                .visibleWhen(() -> TfmConfig.SEND_DISCORD_STATUS.get()));
        groups.add(discordStatus);

        SettingGroup watermark = SettingGroup.alwaysOn(
                "Watermark",
                "Displays mod name, username, FPS, ping and time");
        watermark.add(new ToggleSetting("Show Macro Status",
                () -> TfmConfig.WATERMARK_SHOW_MACRO_STATUS.get(),
                v -> { TfmConfig.WATERMARK_SHOW_MACRO_STATUS.set(v); TfmConfig.save(); }));
        watermark.add(new ToggleSetting("Show Logo",
                () -> TfmConfig.WATERMARK_SHOW_LOGO.get(),
                v -> {
                    if (!v && !TfmConfig.WATERMARK_SHOW_NAME.get()) return;
                    TfmConfig.WATERMARK_SHOW_LOGO.set(v);
                    TfmConfig.save();
                }));
        watermark.add(new ToggleSetting("Show Name",
                () -> TfmConfig.WATERMARK_SHOW_NAME.get(),
                v -> {
                    if (!v && !TfmConfig.WATERMARK_SHOW_LOGO.get()) return;
                    TfmConfig.WATERMARK_SHOW_NAME.set(v);
                    TfmConfig.save();
                }));
        watermark.add(new TextSetting("Custom Username", "leave empty to use your real name",
                () -> TfmConfig.WATERMARK_CUSTOM_USERNAME.get(),
                v -> {
                    TfmConfig.WATERMARK_CUSTOM_USERNAME.set(v);
                    TfmConfig.save();
                }));
        watermark.add(new ToggleSetting("Show Username",
                () -> TfmConfig.WATERMARK_SHOW_USERNAME.get(),
                v -> { TfmConfig.WATERMARK_SHOW_USERNAME.set(v); TfmConfig.save(); }));
        watermark.add(new ToggleSetting("Show FPS",
                () -> TfmConfig.WATERMARK_SHOW_FPS.get(),
                v -> { TfmConfig.WATERMARK_SHOW_FPS.set(v); TfmConfig.save(); }));
        watermark.add(new ToggleSetting("Show Ping",
                () -> TfmConfig.WATERMARK_SHOW_PING.get(),
                v -> { TfmConfig.WATERMARK_SHOW_PING.set(v); TfmConfig.save(); }));
        watermark.add(new ToggleSetting("Show Time",
                () -> TfmConfig.WATERMARK_SHOW_TIME.get(),
                v -> { TfmConfig.WATERMARK_SHOW_TIME.set(v); TfmConfig.save(); }));
        watermark.add(new ToggleSetting("Gradient",
                () -> TfmConfig.WATERMARK_GRADIENT.get(),
                v -> { TfmConfig.WATERMARK_GRADIENT.set(v); TfmConfig.save(); }));
        watermark.add(new ColorSetting("Gradient Left",
                () -> TfmConfig.WATERMARK_GRADIENT_LEFT.get(),
                v -> { TfmConfig.WATERMARK_GRADIENT_LEFT.set(v); TfmConfig.save(); })
                .visibleWhen(() -> TfmConfig.WATERMARK_GRADIENT.get()));
        watermark.add(new ColorSetting("Gradient Right",
                () -> TfmConfig.WATERMARK_GRADIENT_COLOR.get(),
                v -> { TfmConfig.WATERMARK_GRADIENT_COLOR.set(v); TfmConfig.save(); })
                .visibleWhen(() -> TfmConfig.WATERMARK_GRADIENT.get()));
        groups.add(watermark);

        return MainGUIRegistry.subTab("HUD", "Controls which HUD overlays are visible", groups);
    }
}
