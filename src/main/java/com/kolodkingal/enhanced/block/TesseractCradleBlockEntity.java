package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Tesseract's cradle's block entity (3.6): whether its Tesseract was taken. Registered by the 3.6 skeleton empty;
 * lane L4 owns the class.
 */
public class TesseractCradleBlockEntity extends BlockEntity {
    public TesseractCradleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TESSERACT_CRADLE.get(), pos, state);
    }
}
