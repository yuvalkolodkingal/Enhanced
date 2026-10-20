package com.kolodkingal.enhanced.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A suit display of Iron Man's workshop (3.8, PLAN 9.3): two high, a steel mannequin on a plinth showing one suit (its
 * block entity: the suit and who has taken one); right-clicked, a player who has not taken this display's suit gets
 * one ({@code [avengers_tower] displaysPerPlayer}). Registered by the 3.8 skeleton with its halves, facing and
 * generated state; lane L6 owns the behaviour, lane L7 its renderer (the suit and the gantry's arms).
 */
public class SuitDisplayBlock extends BaseEntityBlock {
    public static final MapCodec<SuitDisplayBlock> CODEC = simpleCodec(SuitDisplayBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /**
     * Placed by a template (a generated tower or shrine): unbreakable in Survival (Creative breaks it). A placed one
     * (the creative tab's, for builders) breaks as any metal block.
     */
    public static final BooleanProperty GENERATED = BooleanProperty.create("generated");

    public SuitDisplayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(GENERATED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, GENERATED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (pos.getY() >= context.getLevel().getMaxBuildHeight() - 1
                || !context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A generated one does not break in Survival. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(GENERATED) && !player.isCreative() ? 0.0F : super.getDestroyProgress(state, player, level,
                pos);
    }

    /** The upper half goes on above the lower one (the skeleton's placement; lane L6 owns the block). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        if (!level.isClientSide && level.getBlockState(pos.above()).canBeReplaced()) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new SuitDisplayBlockEntity(pos, state) : null;
    }
}
