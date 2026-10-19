package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Cerebro console's block entity (3.7): who wears the helmet and until when (lane L5), the helmet drawn on its
 * stand (lane L4's renderer). Registered by the 3.7 skeleton empty.
 */
public class CerebroConsoleBlockEntity extends BlockEntity {
    public CerebroConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CEREBRO_CONSOLE.get(), pos, state);
    }
}
