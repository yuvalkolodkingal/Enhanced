package com.kolodkingal.enhanced.spellbook;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block of the green magic's Earth Wall (the Book of Green Witchcraft): packed earth that crumbles by itself. It is only
 * ever put into free air ({@link EarthWall}), is not obtainable (no item, no drop) and lives as long as the one who raised
 * it scheduled its tick before it went in, or {@link #DEFAULT_TICKS} if nobody did. Scheduled ticks are saved with the
 * chunk, so a world that closes and opens again still crumbles it and nothing ever stays.
 */
public class EarthWallBlock extends Block {
    /** How long a block of the wall stands when nobody said (ticks). */
    public static final int DEFAULT_TICKS = 200;

    public EarthWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            // (A tick scheduled for this cell and block before it went in is kept: a second one for the same place is ignored.)
            level.scheduleTick(pos, this, DEFAULT_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), pos.getX() + 0.5,
                pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.6F, 0.8F);
        level.removeBlock(pos, false);
    }
}
