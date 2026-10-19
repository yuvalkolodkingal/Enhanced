package com.kolodkingal.enhanced.hydra;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.EnumSet;
import java.util.Set;

/**
 * A Hydra soldier's way about (3.6): the outpost's doors are iron and opened by its buttons, so its soldiers carry
 * the keycard. Their path finder treats a closed iron door as a door they can open, and {@link DoorGoal} opens it in
 * front of them and shuts it behind them, as a villager does a wooden one.
 */
public class HydraNavigation extends GroundPathNavigation {

    public HydraNavigation(Mob mob, Level level) {
        super(mob, level);
        setCanOpenDoors(true);
        setCanPassDoors(true);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new Keycard();
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanOpenDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    /** The walk evaluator, with closed iron doors walkable (they open them). */
    private static final class Keycard extends WalkNodeEvaluator {
        @Override
        public Set<PathType> getPathTypeWithinMobBB(PathfindingContext context, int x, int y, int z) {
            Set<PathType> types = super.getPathTypeWithinMobBB(context, x, y, z);
            if (types.contains(PathType.DOOR_IRON_CLOSED)) {
                EnumSet<PathType> out = EnumSet.copyOf(types);
                out.remove(PathType.DOOR_IRON_CLOSED);
                out.add(PathType.WALKABLE_DOOR);
                return out;
            }
            return types;
        }
    }

    /** Opens any door (wooden or iron) on the soldier's path, and closes it behind him. */
    public static final class DoorGoal extends OpenDoorGoal {
        public DoorGoal(Mob mob) {
            super(mob, true);
        }

        @Override
        public boolean canUse() {
            if (!(mob.getNavigation() instanceof GroundPathNavigation ground) || !mob.horizontalCollision) {
                return false;
            }
            Path path = ground.getPath();
            if (path == null || path.isDone()) {
                return false;
            }
            for (int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); i++) {
                Node node = path.getNode(i);
                BlockPos at = new BlockPos(node.x, node.y + 1, node.z);
                if (mob.distanceToSqr(at.getX(), mob.getY(), at.getZ()) <= 2.25 && isDoor(at)) {
                    doorPos = at;
                    hasDoor = true;
                    return true;
                }
            }
            doorPos = mob.blockPosition().above();
            hasDoor = isDoor(doorPos);
            return hasDoor;
        }

        private boolean isDoor(BlockPos pos) {
            BlockState state = mob.level().getBlockState(pos);
            return state.getBlock() instanceof DoorBlock;
        }
    }
}
