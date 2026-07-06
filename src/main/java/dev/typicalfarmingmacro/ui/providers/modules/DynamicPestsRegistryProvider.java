package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.DropdownListSetting;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;

import java.util.List;

public final class DynamicPestsRegistryProvider extends AbstractModulesRegistryProvider {

    static final List<String> CROP_OPTIONS = List.of(
            "Wheat", "Carrot", "Potato", "Pumpkin", "Sugar Cane",
            "Melon Slice", "Cactus", "Cocoa Beans", "Mushrooms",
            "Nether Wart", "Moonflower", "Sunflower", "Wild Rose");

    private static final List<String> MODE_OPTIONS = List.of("Feast", "Jacob", "Jacob with Feast");

    private static final List<String> SPRAY_OPTIONS = List.of(
            "Compost", "Honey Jar", "Dung", "Plant Matter", "Tasty Cheese", "Jelly");

    private static final List<String> VINYL_OPTIONS = List.of(
            "Fly", "Cricket", "Locust", "Rat", "Mosquito", "Earthworm",
            "Mite", "Moth", "Slug", "Beetle", "Firefly", "Dragonfly", "Praying Mantis");

    public DynamicPestsRegistryProvider() {
        super(6);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup modeGroup = SettingGroup.alwaysOn(
                        "Dynamic Pest Type",
                        "Which event type drives Dynamic Pests behavior")
                .add(new DropdownSetting("Dynamic Pest Type", MODE_OPTIONS,
                        () -> TfmConfig.DYNAMIC_PESTS_MODE.get(),
                        v -> {
                            TfmConfig.DYNAMIC_PESTS_MODE.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.DYNAMIC_PESTS_ENABLED.get()));

        SettingGroup fallbackGroup = SettingGroup.alwaysOn(
                        "Fallback Behavior",
                        "Used when no Feast or Jacob's Contest is active")
                .add(new DropdownSetting("Fallback Spray", SPRAY_OPTIONS,
                        () -> TfmConfig.DYNAMIC_PESTS_FALLBACK_SPRAY.get(),
                        v -> {
                            TfmConfig.DYNAMIC_PESTS_FALLBACK_SPRAY.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.DYNAMIC_PESTS_ENABLED.get()))
                .add(new DropdownSetting("Fallback Vinyl", VINYL_OPTIONS,
                        () -> TfmConfig.DYNAMIC_PESTS_FALLBACK_VINYL.get(),
                        v -> {
                            TfmConfig.DYNAMIC_PESTS_FALLBACK_VINYL.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.DYNAMIC_PESTS_ENABLED.get()));

        SettingGroup feastGroup = SettingGroup.alwaysOn(
                        "Harvest Feast Priority",
                        "Crops to prioritize during a Harvest Feast (1 = highest)")
                .add(new DropdownListSetting("Feast Crop Priority", CROP_OPTIONS,
                        () -> TfmConfig.DYNAMIC_PESTS_FEAST_PRIORITY.get(),
                        v -> {
                            TfmConfig.DYNAMIC_PESTS_FEAST_PRIORITY.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.DYNAMIC_PESTS_ENABLED.get()));

        SettingGroup contestGroup = SettingGroup.alwaysOn(
                        "Jacob's Contest Priority",
                        "Crops to prioritize during Jacob's Contest (1 = highest)")
                .add(new DropdownListSetting("Contest Crop Priority", CROP_OPTIONS,
                        () -> TfmConfig.DYNAMIC_PESTS_CONTEST_PRIORITY.get(),
                        v -> {
                            TfmConfig.DYNAMIC_PESTS_CONTEST_PRIORITY.set(v);
                            TfmConfig.save();
                        })
                        .visibleWhen(() -> TfmConfig.DYNAMIC_PESTS_ENABLED.get()));

        return MainGUIRegistry.toggleSubTab(
                "Dynamic Pests",
                "Dynamically adjusts crop priorities based on active Feast and Jacob's Contests",
                () -> TfmConfig.DYNAMIC_PESTS_ENABLED.get(),
                v -> {
                    TfmConfig.DYNAMIC_PESTS_ENABLED.set(v);
                    TfmConfig.save();
                },
                List.of(modeGroup, fallbackGroup, feastGroup, contestGroup));
    }
}
