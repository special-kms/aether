package dev.typicalfarmingmacro.modules.pathfinding.pathing.result;

import dev.typicalfarmingmacro.modules.pathfinding.wrapper.PathPosition;
import java.util.Collection;

public interface Path extends Iterable<PathPosition> {
    int length();
    PathPosition getStart();
    PathPosition getEnd();
    Collection<PathPosition> collect();
}
