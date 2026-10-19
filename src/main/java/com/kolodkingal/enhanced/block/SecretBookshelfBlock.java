package com.kolodkingal.enhanced.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * A secret bookshelf of the Professor's study (3.7): it looks exactly like a bookshelf; right-clicked, both columns
 * swing open ({@link #OPEN}: a thin hinged panel, no collision) onto the nook with the elevator pads, and close by
 * themselves when nobody stands there. Registered by the 3.7 skeleton with its facing and open state; lane L4 owns the
 * class (the swing, the closing, who may open it, the drop of a plain bookshelf).
 */
public class SecretBookshelfBlock extends Block {
    public static final MapCodec<SecretBookshelfBlock> CODEC = simpleCodec(SecretBookshelfBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public SecretBookshelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
