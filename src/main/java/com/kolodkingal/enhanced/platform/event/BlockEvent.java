package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** An event about a block in a level (NeoForge's BlockEvent). */
public abstract class BlockEvent {
    private final LevelAccessor level;
    private final BlockPos pos;
    private final BlockState state;

    protected BlockEvent(LevelAccessor level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    public LevelAccessor getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    /** A player is about to break a block; cancelling keeps it. */
    public static class BreakEvent extends BlockEvent implements Cancellable {
        private final Player player;
        private boolean canceled;

        public BreakEvent(LevelAccessor level, BlockPos pos, BlockState state, Player player) {
            super(level, pos, state);
            this.player = player;
        }

        public Player getPlayer() {
            return player;
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

    /** An entity placed a block (it is already in the level); cancelling takes it back. */
    public static class EntityPlaceEvent extends BlockEvent implements Cancellable {
        private final BlockState placedAgainst;
        private final @Nullable Entity entity;
        private boolean canceled;

        public EntityPlaceEvent(
                LevelAccessor level, BlockPos pos, BlockState state, BlockState placedAgainst,
                @Nullable Entity entity) {
            super(level, pos, state);
            this.placedAgainst = placedAgainst;
            this.entity = entity;
        }

        public BlockState getPlacedAgainst() {
            return placedAgainst;
        }

        @Nullable
        public Entity getEntity() {
            return entity;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        /** The block placed (getState()). */
        public BlockState getPlacedBlock() {
            return getState();
        }
    }

    /** An entity is about to trample farmland; cancelling keeps it. */
    public static class FarmlandTrampleEvent extends BlockEvent implements Cancellable {
        private final float fallDistance;
        private final Entity entity;
        private boolean canceled;

        public FarmlandTrampleEvent(
                LevelAccessor level, BlockPos pos, BlockState state, float fallDistance, Entity entity) {
            super(level, pos, state);
            this.fallDistance = fallDistance;
            this.entity = entity;
        }

        public float getFallDistance() {
            return fallDistance;
        }

        public Entity getEntity() {
            return entity;
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

    /** A fluid is about to place a block (lava and water make stone); change it or cancel it. */
    public static class FluidPlaceBlockEvent extends BlockEvent implements Cancellable {
        private final BlockPos liquidPos;
        private final BlockState originalState;
        private BlockState newState;
        private boolean canceled;

        public FluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
            super(level, pos, state);
            this.liquidPos = liquidPos;
            this.originalState = level.getBlockState(pos);
            this.newState = state;
        }

        public BlockPos getLiquidPos() {
            return liquidPos;
        }

        public BlockState getOriginalState() {
            return originalState;
        }

        public BlockState getNewState() {
            return newState;
        }

        public void setNewState(BlockState newState) {
            this.newState = newState;
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

    /** A block tells its neighbours it changed; cancelling keeps them unaware. */
    public static class NeighborNotifyEvent extends BlockEvent implements Cancellable {
        private final EnumSet<Direction> notifiedSides;
        private final boolean forceRedstoneUpdate;
        private boolean canceled;

        public NeighborNotifyEvent(
                LevelAccessor level, BlockPos pos, BlockState state, EnumSet<Direction> notifiedSides,
                boolean forceRedstoneUpdate) {
            super(level, pos, state);
            this.notifiedSides = notifiedSides;
            this.forceRedstoneUpdate = forceRedstoneUpdate;
        }

        public EnumSet<Direction> getNotifiedSides() {
            return notifiedSides;
        }

        public boolean getForceRedstoneUpdate() {
            return forceRedstoneUpdate;
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

    /** A nether portal is about to light; cancelling stops it. */
    public static class PortalSpawnEvent extends BlockEvent implements Cancellable {
        private final PortalShape portalSize;
        private boolean canceled;

        public PortalSpawnEvent(LevelAccessor level, BlockPos pos, BlockState state, PortalShape portalSize) {
            super(level, pos, state);
            this.portalSize = portalSize;
        }

        public PortalShape getPortalSize() {
            return portalSize;
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

    /** A tool is about to change a block (strip a log, till dirt...); change the result or cancel it. */
    public static class BlockToolModificationEvent extends BlockEvent implements Cancellable {
        private final UseOnContext context;
        private final boolean simulated;
        private BlockState finalState;
        private boolean canceled;

        public BlockToolModificationEvent(BlockState state, UseOnContext context, boolean simulated) {
            super(context.getLevel(), context.getClickedPos(), state);
            this.context = context;
            this.simulated = simulated;
            this.finalState = state;
        }

        public UseOnContext getContext() {
            return context;
        }

        public boolean isSimulated() {
            return simulated;
        }

        public BlockState getFinalState() {
            return finalState;
        }

        public void setFinalState(BlockState finalState) {
            this.finalState = finalState;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        @Nullable
        public Player getPlayer() {
            return context.getPlayer();
        }

        public InteractionHand getHand() {
            return context.getHand();
        }

        public ItemStack getHeldItemStack() {
            return context.getItemInHand();
        }
    }
}
