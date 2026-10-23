package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;

/** A piston moves (NeoForge's PistonEvent). */
public abstract class PistonEvent extends BlockEvent {
    private final Direction direction;
    private final PistonMoveType pistonMoveType;

    protected PistonEvent(LevelAccessor level, BlockPos pos, Direction direction, PistonMoveType pistonMoveType) {
        super(level, pos, level.getBlockState(pos));
        this.direction = direction;
        this.pistonMoveType = pistonMoveType;
    }

    public Direction getDirection() {
        return direction;
    }

    public PistonMoveType getPistonMoveType() {
        return pistonMoveType;
    }

    /** The block in front of the piston. */
    public BlockPos getFaceOffsetPos() {
        return getPos().relative(direction);
    }

    /** Whether the piston pushes out or pulls back. */
    public enum PistonMoveType {
        EXTEND,
        RETRACT;

        public boolean isExtend() {
            return this == EXTEND;
        }
    }

    /** A piston is about to move; cancelling stops it. */
    public static class Pre extends PistonEvent implements Cancellable {
        private boolean canceled;

        public Pre(LevelAccessor level, BlockPos pos, Direction direction, PistonMoveType pistonMoveType) {
            super(level, pos, direction, pistonMoveType);
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }
}
