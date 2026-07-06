package dev.typicalfarmingmacro.ui;

import dev.typicalfarmingmacro.ui.settings.ModulesTab;

record MainGUIContentViewState(int activeMain, int activeSubtab, int activeFilter,
                               ModulesTab.SubTab activeSubTab, int activeCategoryIdx,
                               String searchQuery) {
}
