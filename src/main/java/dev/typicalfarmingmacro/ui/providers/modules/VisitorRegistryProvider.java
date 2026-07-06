package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.config.TfmConfig;
import dev.typicalfarmingmacro.ui.settings.ListSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.ui.settings.SliderSetting;
import dev.typicalfarmingmacro.ui.settings.TextSetting;
import dev.typicalfarmingmacro.ui.settings.ToggleSetting;

import java.util.List;

public final class VisitorRegistryProvider extends AbstractModulesRegistryProvider {
    public VisitorRegistryProvider() {
        super(4);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        SettingGroup group = SettingGroup.of(
                "Auto Visitor Settings",
                "Automatically fulfills visitors requests",
                () -> TfmConfig.AUTO_VISITOR.get(),
                v -> {
                    TfmConfig.AUTO_VISITOR.set(v);
                    TfmConfig.save();
                });
        group.add(new SliderSetting("Visitor Threshold", 1, 5,
                () -> (float) TfmConfig.VISITOR_THRESHOLD.get(),
                v -> {
                    TfmConfig.VISITOR_THRESHOLD.set(Math.round(v));
                    TfmConfig.save();
                })
                .withDecimals(0));
        group.add(new SliderSetting("Max Visitor Purchase", 0.0f, 20.0f,
                () -> TfmConfig.VISITOR_MAX_PURCHASE_LIMIT.get() / 1_000_000.0f,
                v -> {
                    TfmConfig.VISITOR_MAX_PURCHASE_LIMIT.set(Math.round(v * 1_000_000.0f));
                    TfmConfig.save();
                })
                .withDecimals(1).withSuffix("m"));
        group.add(new ListSetting("Visitor Ignored", "Add visitor name",
                () -> TfmConfig.VISITOR_ignore.get(),
                v -> {
                    TfmConfig.VISITOR_ignore.set(v);
                    TfmConfig.save();
                }));
        group.add(new ListSetting("Visitor Reject", "Add visitor name",
                () -> TfmConfig.VISITOR_REJECT.get(),
                v -> {
                    TfmConfig.VISITOR_REJECT.set(v);
                    TfmConfig.save();
                }));
        group.add(new ToggleSetting("Equip Custom Item",
                () -> TfmConfig.EQUIP_VISITOR_CUSTOM_ITEM.get(),
                v -> {
                    TfmConfig.EQUIP_VISITOR_CUSTOM_ITEM.set(v);
                    TfmConfig.save();
                }));
        group.add(new TextSetting("Custom Item", "e.g. Blessed Melon Dicer",
                () -> TfmConfig.VISITOR_CUSTOM_ITEM.get(),
                v -> {
                    TfmConfig.VISITOR_CUSTOM_ITEM.set(v);
                    TfmConfig.save();
                })
                .visibleWhen(() -> TfmConfig.EQUIP_VISITOR_CUSTOM_ITEM.get()));
        group.add(new ToggleSetting("Disable Compactors during Visitors",
                () -> TfmConfig.DISABLE_COMPACTORS_DURING_VISITORS.get(),
                v -> {
                    TfmConfig.DISABLE_COMPACTORS_DURING_VISITORS.set(v);
                    TfmConfig.save();
                }));
        group.add(new ToggleSetting("Disable during Jacob's Contests",
                () -> TfmConfig.DISABLE_VISITORS_DURING_JACOBS_CONTEST.get(),
                v -> {
                    TfmConfig.DISABLE_VISITORS_DURING_JACOBS_CONTEST.set(v);
                    TfmConfig.save();
                }));
        group.add(FarmingSettingsFactory.visitorFovRangeSetting());
        return MainGUIRegistry.toggleSubTab(
                "Auto Visitor",
                "Automatically interacts with visitors and fulfills their requests",
                () -> TfmConfig.AUTO_VISITOR.get(),
                v -> {
                    TfmConfig.AUTO_VISITOR.set(v);
                    TfmConfig.save();
                },
                List.of(group));
    }
}
