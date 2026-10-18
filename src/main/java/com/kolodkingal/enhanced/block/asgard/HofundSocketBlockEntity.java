package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Hofund socket's block entity (3.5): it keeps nothing; it is there so that the client draws the sword in it. */
public class HofundSocketBlockEntity extends BlockEntity {
    public HofundSocketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOFUND_SOCKET.get(), pos, state);
    }
}
