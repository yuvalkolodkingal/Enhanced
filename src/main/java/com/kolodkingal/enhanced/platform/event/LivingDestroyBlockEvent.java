package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A mob (a wither, a dragon...) is about to break a block; cancelling keeps it. */
public class LivingDestroyBlockEvent extends LivingEvent implements Cancellable {
    private final BlockPos pos;
    private final BlockState state;
    private boolean canceled;

    public LivingDestroyBlockEvent(LivingEntity entity, BlockPos pos, BlockState state) {
        super(entity);
        this.pos = pos;
        this.state = state;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
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
