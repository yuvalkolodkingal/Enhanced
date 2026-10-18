package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The cosmic falls of Asgard (3.5): water of light falling off the edge of the world and into the chasms. Not a fluid:
 * no collision, no ticks, nothing to click; a translucent animated sheet. Its lowest block breathes mist now and then
 * (client only), and sometimes the roar of the falls.
 */
public class CosmicFallsBlock extends Block {
    public CosmicFallsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos.below()).is(this) || random.nextInt(8) != 0) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble() * 0.3,
                    pos.getZ() + random.nextDouble(), (random.nextDouble() - 0.5) * 0.05, -0.04, (random.nextDouble() - 0.5) * 0.05);
        }
        level.addParticle(ParticleTypes.SPLASH, pos.getX() + random.nextDouble(), pos.getY(), pos.getZ() + random.nextDouble(), 0, 0, 0);
        if (random.nextInt(40) == 0) {
            level.playLocalSound(pos, ModSounds.ASGARD_FALLS.get(), SoundSource.AMBIENT, 0.6F, 0.8F + random.nextFloat() * 0.3F, false);
        }
    }
}
