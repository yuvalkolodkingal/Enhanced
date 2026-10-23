package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** What a broken block drops (a live list), and its experience; cancelling drops nothing. */
public class BlockDropsEvent extends BlockEvent implements Cancellable {
    private final @Nullable BlockEntity blockEntity;
    private final List<ItemEntity> drops;
    private final @Nullable Entity breaker;
    private final ItemStack tool;
    private int droppedExperience;
    private boolean canceled;

    public BlockDropsEvent(
            LevelAccessor level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            List<ItemEntity> drops, @Nullable Entity breaker, ItemStack tool) {
        super(level, pos, state);
        this.blockEntity = blockEntity;
        this.drops = drops;
        this.breaker = breaker;
        this.tool = tool;
        this.droppedExperience = 0;
    }

    @Nullable
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    public List<ItemEntity> getDrops() {
        return drops;
    }

    @Nullable
    public Entity getBreaker() {
        return breaker;
    }

    public ItemStack getTool() {
        return tool;
    }

    public int getDroppedExperience() {
        return droppedExperience;
    }

    public void setDroppedExperience(int droppedExperience) {
        this.droppedExperience = droppedExperience;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public ServerLevel getLevel() {
        return (ServerLevel) super.getLevel();
    }
}
