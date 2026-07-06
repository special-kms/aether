package dev.typicalfarmingmacro.modules.pathfinding.pathing.heuristic;

import dev.typicalfarmingmacro.modules.pathfinding.wrapper.PathPosition;

public interface IHeuristicStrategy {
    double calculate(HeuristicContext context);
    double calculateTransitionCost(PathPosition from, PathPosition to);
}
