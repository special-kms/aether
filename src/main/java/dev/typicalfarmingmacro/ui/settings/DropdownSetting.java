package dev.typicalfarmingmacro.ui.settings;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import dev.typicalfarmingmacro.util.TfmLang;

/**
 * Dropdown selection setting backed by index getter/setter.
 * Example:
 *   new DropdownSetting("Gear Swap Mode",
 *       List.of("None", "Wardrobe", "Rod"),
 *       () -> TfmConfig.gearSwapMode.ordinal(),
 *       i -> TfmConfig.gearSwapMode = TfmConfig.GearSwapMode.values()[i])
 */
public class DropdownSetting implements Setting {

    private final String name;
    private final String rawName;
    private final List<String> options;
    private final List<IconAction> iconActions = new ArrayList<>();
    private final Supplier<Integer> indexGetter;
    private final Consumer<Integer> indexSetter;
    private Supplier<Boolean> visibility = () -> true;

    public DropdownSetting(String name, List<String> options,
                           Supplier<Integer> indexGetter, Consumer<Integer> indexSetter) {
        this.rawName = name;
        this.name = TfmLang.localize(name);
        this.options = options;
        this.indexGetter = indexGetter;
        this.indexSetter = indexSetter;
    }

    public List<String> getOptions() { return options.stream().map(TfmLang::localize).toList(); }
    public List<IconAction> getIconActions() { return iconActions; }
    public int getSelectedIndex() { return indexGetter.get(); }
    public void setSelectedIndex(int index) { indexSetter.accept(index); }
    public String getSelectedOption() {
        if (options.isEmpty()) {
            return "";
        }

        int index = getSelectedIndex();
        if (index < 0 || index >= options.size()) {
            return TfmLang.localize(options.getFirst());
        }

        return TfmLang.localize(options.get(index));
    }

    public DropdownSetting addIconAction(String iconPath, Runnable action) {
        iconActions.add(new IconAction(iconPath, action));
        return this;
    }

    public DropdownSetting visibleWhen(Supplier<Boolean> condition) {
        this.visibility = condition;
        return this;
    }

    @Override public String getName() { return name; }
    @Override public String getRawName() { return rawName; }
    @Override public SettingType getType() { return SettingType.DROPDOWN; }
    @Override public boolean isVisible() { return visibility.get(); }

    public record IconAction(String iconPath, Runnable action) {
        public void execute() {
            if (action != null) action.run();
        }
    }
}
