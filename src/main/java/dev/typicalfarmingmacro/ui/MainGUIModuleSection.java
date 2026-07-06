package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.ui.settings.ModulesTab;

import java.util.List;

record MainGUIModuleSection(String name, List<ModulesTab.SubTab> subtabs) {
}
