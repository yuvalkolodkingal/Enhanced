package com.kolodkingal.enhanced.block.asgard;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A tile of the throne room's paintings (3.5): sixteen tiles ({@link #TILE} 0-15, row by row from the top left as seen
 * from the front) make one picture, 4 x 4 blocks. {@link #CONQUEST} false shows the peaceful Asgard of Odin; true, Hela's
 * conquests (the paintings under the paintings), swapped by {@code CityBuild.setMurals} when Hela comes and when she
 * goes. The picture is on the {@link #FACING} side.
 */
public class AsgardMuralBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<AsgardMuralBlock> CODEC = simpleCodec(AsgardMuralBlock::new);
    public static final BooleanProperty CONQUEST = BooleanProperty.create("conquest");
    public static final IntegerProperty TILE = IntegerProperty.create("tile", 0, 15);

    public AsgardMuralBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONQUEST, false).setValue(TILE, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONQUEST, TILE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
