package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A tech case's block entity (3.8): the item it shows, its rule ({@code ANYONE}, {@code CAPTAIN_ONLY}) and who has
 * taken one (the generator writes {@code {"shown": {...}, "rule": "anyone"}}). Registered by the 3.8 skeleton empty;
 * lane L6 owns the class.
 */
public class TechCaseBlockEntity extends BlockEntity {
    public TechCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TECH_CASE.get(), pos, state);
    }
}
