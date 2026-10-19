package com.kolodkingal.enhanced.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * The stone pedestal (3.6): it holds one Infinity Stone at a site (the Sanctum's Time Stone, Asgard's vault's Reality
 * Stone). Unbreakable where a site put it; its stone floats above it (a renderer of lane L1). Registered by the 3.6
 * skeleton with its block entity and the property stone; lane L1 owns the class (taking the stone, the ledger, the
 * refusals).
 */
public class StonePedestalBlock extends BaseEntityBlock {
    public static final MapCodec<StonePedestalBlock> CODEC = simpleCodec(StonePedestalBlock::new);
    /** Which stone the pedestal shows ({@code none} when it is empty for everyone). */
    public static final EnumProperty<Stone> STONE = EnumProperty.create("stone", Stone.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** The stone of a pedestal ({@code none}, or one of the six by {@code StoneKind} name). */
    public enum Stone implements StringRepresentable {
        NONE, REALITY, MIND, TIME, POWER, SPACE, SOUL;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public StonePedestalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STONE, Stone.NONE).setValue(FACING, Direction.NORTH));
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
        builder.add(STONE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StonePedestalBlockEntity(pos, state);
    }
}
