package dev.typicalfarmingmacro.modules.pathfinding.pathing;

import dev.typicalfarmingmacro.modules.pathfinding.wrapper.PathPosition;
import dev.typicalfarmingmacro.modules.pathfinding.wrapper.PathVector;

@FunctionalInterface
public interface INeighborStrategy {
    Iterable<PathVector> getOffsets();

    default Iterable<PathVector> getOffsets(PathPosition currentPosition) {
        return getOffsets();
    }
}
