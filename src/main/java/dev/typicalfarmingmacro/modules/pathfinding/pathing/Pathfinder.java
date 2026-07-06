package dev.typicalfarmingmacro.modules.pathfinding.pathing;

import dev.typicalfarmingmacro.modules.pathfinding.pathing.context.EnvironmentContext;
import dev.typicalfarmingmacro.modules.pathfinding.pathing.result.PathfinderResult;
import dev.typicalfarmingmacro.modules.pathfinding.wrapper.PathPosition;
import java.util.concurrent.CompletionStage;

public interface Pathfinder {
    default CompletionStage<PathfinderResult> findPath(PathPosition start, PathPosition target) {
        return findPath(start, target, null);
    }

    CompletionStage<PathfinderResult> findPath(PathPosition start, PathPosition target,
                                               EnvironmentContext context);

    void abort();
}
