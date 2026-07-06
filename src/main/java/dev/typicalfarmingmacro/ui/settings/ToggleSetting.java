package dev.typicalfarmingmacro.ui.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;
import dev.typicalfarmingmacro.util.TfmLang;

/**
 * Boolean on/off setting backed by a getter/setter.
 *
 * Example:
 *   new ToggleSetting("Auto Visitor", () -> TfmConfig.autoVisitor, v -> TfmConfig.autoVisitor = v)
 */
public class ToggleSetting implements Setting {

    private final String name;
    private final String rawName;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;
    private Supplier<Boolean> visibility = () -> true;

    public ToggleSetting(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.rawName = name;
        this.name = TfmLang.localize(name);
        this.getter = getter;
        this.setter = setter;
    }

    public boolean getValue() { return getter.get(); }
    public void setValue(boolean value) { setter.accept(value); }
    public void toggle() { setValue(!getValue()); }

    public ToggleSetting visibleWhen(Supplier<Boolean> condition) {
        this.visibility = condition;
        return this;
    }

    @Override public String getName() { return name; }
    @Override public String getRawName() { return rawName; }
    @Override public SettingType getType() { return SettingType.TOGGLE; }
    @Override public boolean isVisible() { return visibility.get(); }
}
