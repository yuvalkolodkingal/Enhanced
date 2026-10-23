package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A player interacts with a block, an entity or the air (NeoForge's PlayerInteractEvent). */
public abstract class PlayerInteractEvent extends PlayerEvent {
    private final InteractionHand hand;
    private final BlockPos pos;
    private final @Nullable Direction face;

    protected PlayerInteractEvent(Player entity, InteractionHand hand, BlockPos pos, @Nullable Direction face) {
        super(entity);
        this.hand = hand;
        this.pos = pos;
        this.face = face;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public Direction getFace() {
        return face;
    }

    /** The item in the hand used. */
    public ItemStack getItemStack() {
        return getEntity().getItemInHand(getHand());
    }

    public Level getLevel() {
        return getEntity().level();
    }

    /** A player right-clicks an entity at a point of it (armor stands); cancelling returns the cancellation result. */
    public static class EntityInteractSpecific extends PlayerInteractEvent implements Cancellable {
        private final Entity target;
        private final Vec3 localPos;
        private InteractionResult cancellationResult;
        private boolean canceled;

        public EntityInteractSpecific(Player entity, InteractionHand hand, Entity target, Vec3 localPos) {
            super(entity, hand, target.blockPosition(), null);
            this.target = target;
            this.localPos = localPos;
            this.cancellationResult = InteractionResult.PASS;
        }

        public Entity getTarget() {
            return target;
        }

        public Vec3 getLocalPos() {
            return localPos;
        }

        public InteractionResult getCancellationResult() {
            return cancellationResult;
        }

        public void setCancellationResult(InteractionResult cancellationResult) {
            this.cancellationResult = cancellationResult;
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

    /** A player right-clicks an entity; cancelling returns the cancellation result. */
    public static class EntityInteract extends PlayerInteractEvent implements Cancellable {
        private final Entity target;
        private InteractionResult cancellationResult;
        private boolean canceled;

        public EntityInteract(Player entity, InteractionHand hand, Entity target) {
            super(entity, hand, target.blockPosition(), null);
            this.target = target;
            this.cancellationResult = InteractionResult.PASS;
        }

        public Entity getTarget() {
            return target;
        }

        public InteractionResult getCancellationResult() {
            return cancellationResult;
        }

        public void setCancellationResult(InteractionResult cancellationResult) {
            this.cancellationResult = cancellationResult;
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

    /**
     * A player right-clicks a block: setUseBlock/setUseItem decide which of the two happens; cancelling stops both and
     * returns the cancellation result.
     */
    public static class RightClickBlock extends PlayerInteractEvent implements Cancellable {
        private final BlockHitResult hitVec;
        private TriState useBlock;
        private TriState useItem;
        private InteractionResult cancellationResult;
        private boolean canceled;

        public RightClickBlock(Player entity, InteractionHand hand, BlockPos pos, BlockHitResult hitVec) {
            super(entity, hand, pos, hitVec.getDirection());
            this.hitVec = hitVec;
            this.useBlock = TriState.DEFAULT;
            this.useItem = TriState.DEFAULT;
            this.cancellationResult = InteractionResult.PASS;
        }

        public BlockHitResult getHitVec() {
            return hitVec;
        }

        public TriState getUseBlock() {
            return useBlock;
        }

        public void setUseBlock(TriState useBlock) {
            this.useBlock = useBlock;
        }

        public TriState getUseItem() {
            return useItem;
        }

        public void setUseItem(TriState useItem) {
            this.useItem = useItem;
        }

        public InteractionResult getCancellationResult() {
            return cancellationResult;
        }

        public void setCancellationResult(InteractionResult cancellationResult) {
            this.cancellationResult = cancellationResult;
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

    /** A player uses the item in a hand on nothing; cancelling returns the cancellation result. */
    public static class RightClickItem extends PlayerInteractEvent implements Cancellable {
        private InteractionResult cancellationResult;
        private boolean canceled;

        public RightClickItem(Player entity, InteractionHand hand) {
            super(entity, hand, entity.blockPosition(), null);
            this.cancellationResult = InteractionResult.PASS;
        }

        public InteractionResult getCancellationResult() {
            return cancellationResult;
        }

        public void setCancellationResult(InteractionResult cancellationResult) {
            this.cancellationResult = cancellationResult;
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

    /** A player left-clicks (starts, stops or gives up breaking) a block; cancelling stops it. */
    public static class LeftClickBlock extends PlayerInteractEvent implements Cancellable {
        private final Action action;
        private TriState useBlock;
        private TriState useItem;
        private boolean canceled;

        public LeftClickBlock(Player entity, BlockPos pos, @Nullable Direction face, Action action) {
            super(entity, InteractionHand.MAIN_HAND, pos, face);
            this.action = action;
            this.useBlock = TriState.DEFAULT;
            this.useItem = TriState.DEFAULT;
        }

        public Action getAction() {
            return action;
        }

        public TriState getUseBlock() {
            return useBlock;
        }

        public void setUseBlock(TriState useBlock) {
            this.useBlock = useBlock;
        }

        public TriState getUseItem() {
            return useItem;
        }

        public void setUseItem(TriState useItem) {
            this.useItem = useItem;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        /** What the left click does: starts or stops breaking, gives up, or (client) holds the button. */
        public enum Action {
            START,
            STOP,
            ABORT,
            CLIENT_HOLD
        }
    }
}
