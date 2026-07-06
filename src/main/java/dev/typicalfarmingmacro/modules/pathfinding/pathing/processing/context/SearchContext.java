package dev.typicalfarmingmacro.modules.pathfinding.pathing.processing.context;

import dev.typicalfarmingmacro.modules.pathfinding.pathing.configuration.PathfinderConfiguration;
import dev.typicalfarmingmacro.modules.pathfinding.pathing.context.EnvironmentContext;
import dev.typicalfarmingmacro.modules.pathfinding.provider.NavigationPointProvider;
import dev.typicalfarmingmacro.modules.pathfinding.wrapper.PathPosition;
import java.util.Map;

public interface SearchContext {
    PathPosition getStartPathPosition();
    PathPosition getTargetPathPosition();
    PathfinderConfiguration getPathfinderConfiguration();
    NavigationPointProvider getNavigationPointProvider();
    Map<String, Object> getSharedData();
    EnvironmentContext getEnvironmentContext();
}
