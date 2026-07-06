package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.ui.settings.ActionSetting;
import dev.typicalfarmingmacro.ui.settings.DropdownSetting;
import dev.typicalfarmingmacro.ui.settings.ModulesTab;
import dev.typicalfarmingmacro.ui.settings.SettingGroup;
import dev.typicalfarmingmacro.util.TfmLanguageManager;

import java.util.List;

public final class LanguageSettingsRegistryProvider extends AbstractSettingsRegistryProvider {
    public LanguageSettingsRegistryProvider() {
        super(1);
    }

    @Override
    protected ModulesTab.SubTab createSubTab() {
        List<String> languageOptions = TfmLanguageManager.getAvailableLanguageCodes();

        SettingGroup group = SettingGroup.alwaysOn(
                "Language",
                "Switch the Tfm UI language and refetch language packs");
        group.add(new DropdownSetting("Language",
                languageOptions,
                () -> TfmLanguageManager.getSelectedLanguageIndex(languageOptions),
                index -> {
                    if (index < 0 || index >= languageOptions.size()) {
                        return;
                    }
                    TfmLanguageManager.selectLanguage(languageOptions.get(index));
                }));
        group.add(new ActionSetting("Refresh Language Packs",
                () -> TfmLanguageManager.refreshFromRemoteAsync(true)));

        return MainGUIRegistry.subTab("Language", "Switch the Tfm UI language", List.of(group));
    }
}
