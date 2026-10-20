package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.tower.Armoury;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A tech case of the Avengers Tower (3.8, PLAN 9.3): a glass case on a steel plinth showing one item (the Arc Reactor,
 * the Nano Arc Reactor, the Exo-Wings, the serum, Cap's shield) under its rule (anyone, or Captain America only); its
 * block entity keeps the item, the rule and who has taken one. Right-clicked, the lid slides up and the item is given
 * ({@link Armoury#takeFromCase}); the lid closes {@link Armoury#LID_TICKS} later (a scheduled tick). Lane L6's, with
 * its renderer ({@code client/tower/TechCaseRenderer}: the item turning inside, the lid). A generated one (the tower's)
 * does not break in Survival; a Creative player sets the item of a builder's case (not a generated one) by using the
 * item on it (Cap's shield: Captain America only).
 */
public class TechCaseBlock extends BaseEntityBlock {
    public static final MapCodec<TechCaseBlock> CODEC = simpleCodec(TechCaseBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /**
     * Placed by a template (a generated tower or shrine): unbreakable in Survival (Creative breaks it). A placed one
     * (the creative tab's, for builders) breaks as any metal block.
     */
    public static final BooleanProperty GENERATED = BooleanProperty.create("generated");

    /** The plinth and the glass over it (with its lid), drawn facing north. */
    private static final Map<Direction, VoxelShape> SHAPE = LabShapes.turned(new double[]{1, 0, 1, 15, 6, 15},
            new double[]{2, 6, 2, 14, 16, 14});

    public TechCaseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
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
        builder.add(FACING, GENERATED);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A generated one does not break in Survival. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return state.getValue(GENERATED) && !player.isCreative() ? 0.0F : super.getDestroyProgress(state, player, level,
                pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Armoury.takeFromCase((ServerLevel) level, pos, player);
        return InteractionResult.CONSUME;
    }

    /** A Creative player using an item on a builder's case puts one of it on show; anything else is a plain use. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isCreative() || stack.isEmpty() || state.getValue(GENERATED)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TechCaseBlockEntity box) {
            box.setShown(stack, stack.is(ModItems.VIBRANIUM_SHIELD.get()) ? TechCaseBlockEntity.Rule.CAPTAIN_ONLY
                    : TechCaseBlockEntity.Rule.ANYONE);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The lid closes after a take. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Armoury.closeLid(level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TechCaseBlockEntity(pos, state);
    }
}
