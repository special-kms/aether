package dev.typicalfarmingmacro.modules.pathfinding.provider;

import dev.typicalfarmingmacro.modules.pathfinding.pathing.context.EnvironmentContext;
import dev.typicalfarmingmacro.modules.pathfinding.wrapper.PathPosition;

public interface NavigationPointProvider {
    default NavigationPoint getNavigationPoint(PathPosition position) {
        return getNavigationPoint(position, null);
    }

    NavigationPoint getNavigationPoint(PathPosition position, EnvironmentContext environmentContext);
}
