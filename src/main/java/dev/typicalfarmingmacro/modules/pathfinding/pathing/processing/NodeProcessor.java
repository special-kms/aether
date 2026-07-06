package dev.typicalfarmingmacro.modules.pathfinding.pathing.processing;

import dev.typicalfarmingmacro.modules.pathfinding.pathing.processing.context.EvaluationContext;

public interface NodeProcessor extends Processor {
    default boolean isValid(EvaluationContext context) {
        return true;
    }

    default Cost calculateCostContribution(EvaluationContext context) {
        return Cost.ZERO;
    }
}
