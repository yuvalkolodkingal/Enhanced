package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Tesseract's cradle's block entity (3.6): it holds nothing of its own (whether the cradle is full is its block's
 * state); it is there so the client can draw the Tesseract turning over it ({@code TesseractCradleRenderer}).
 */
public class TesseractCradleBlockEntity extends BlockEntity {
    public TesseractCradleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TESSERACT_CRADLE.get(), pos, state);
    }
}
