package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.ArrayList;
import java.util.List;

public final class FunVisualsRegistryProvider extends AbstractVisualsRegistryProvider {
    public FunVisualsRegistryProvider() {
        super(3);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<SettingGroup> groups = new ArrayList<>();
        groups.add(SettingGroup.of(
                        "Hat",
                        "Renders a chroma pyramid above your head",
                        () -> TfmConfig.HAT_ENABLED.get(),
                        v -> {
                            TfmConfig.HAT_ENABLED.set(v);
                            TfmConfig.save();
                        })
                .add(new ToggleSetting("Filled Sides",
                        () -> TfmConfig.HAT_FILLED.get(),
                        v -> {
                            TfmConfig.HAT_FILLED.set(v);
                            TfmConfig.save();
                        }))
                .add(new ToggleSetting("Render In First Person",
                        () -> TfmConfig.HAT_RENDER_FIRST_PERSON.get(),
                        v -> {
                            TfmConfig.HAT_RENDER_FIRST_PERSON.set(v);
                            TfmConfig.save();
                        }))
                .add(new SliderSetting("Pyramid Height", 0.1f, 3.0f,
                        () -> TfmConfig.HAT_HEIGHT.get(),
                        v -> {
                            TfmConfig.HAT_HEIGHT.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1))
                .add(new SliderSetting("Radius", 0.1f, 3.0f,
                        () -> TfmConfig.HAT_RADIUS.get(),
                        v -> {
                            TfmConfig.HAT_RADIUS.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1))
                .add(new SliderSetting("Y Offset", 0.1f, 3.0f,
                        () -> TfmConfig.HAT_Y_OFFSET.get(),
                        v -> {
                            TfmConfig.HAT_Y_OFFSET.set(v);
                            TfmConfig.save();
                        })
                        .withDecimals(1))
                .add(new SliderSetting("Vertices", 3.0f, 30.0f,
                        () -> (float) TfmConfig.HAT_VERTICES.get(),
                        v -> {
                            TfmConfig.HAT_VERTICES.set(Math.round(v));
                            TfmConfig.save();
                        })
                        .withDecimals(0)));
        groups.add(SettingGroup.of(
                        "Funny Dynamic Rest",
                        "Uses a Hypixel-style ban screen during dynamic rest",
                        () -> TfmConfig.FUNNY_DYNAMIC_REST.get(),
                        v -> {
                            TfmConfig.FUNNY_DYNAMIC_REST.set(v);
                            TfmConfig.save();
                        }));
        return MainGUIRegistry.subTab("Fun", "Cosmetic world effects", groups);
    }
}
