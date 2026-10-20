package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.asgard.FeastHall;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * The Asgardians' way of walking (3.7.2): a ground navigation as every walker's, but it never goes over a table of the
 * Hall of Feasts (nor over a dish on one, {@link FeastHall#overTable}): a table a block high is a step up from its
 * benches, and before, a diner on his way to his seat could walk across the tables.
 */
final class AsgardianNavigation extends GroundPathNavigation {
    AsgardianNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        nodeEvaluator = new Evaluator();
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, maxVisitedNodes);
    }

    /** A walker's node evaluator that takes the cells over a table (or a dish on one) as blocked. */
    static final class Evaluator extends WalkNodeEvaluator {
        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            PathType type = super.getPathType(context, x, y, z);
            return type != PathType.BLOCKED && FeastHall.overTable(context.level(), x, y, z) ? PathType.BLOCKED : type;
        }
    }
}
