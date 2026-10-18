package com.kolodkingal.enhanced.block.asgard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A golden brazier of Asgard (3.5): always lit (light 15), flames and a thread of smoke drawn by the client
 * ({@code animateTick}); no ticks, no fire spread. Decoration of the city.
 */
public class AsgardBrazierBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 2, 11), Block.box(7, 2, 7, 9, 8, 9),
            Block.box(2, 8, 2, 14, 12, 14));

    public AsgardBrazierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5, 0, 0.02, 0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.4, z, 0, 0.04, 0);
        }
    }
}
