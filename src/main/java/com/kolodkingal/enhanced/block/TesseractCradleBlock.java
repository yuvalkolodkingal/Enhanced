package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Tesseract's cradle on the roof of Avengers Tower (3.6, Selvig's portal device): a steel plinth that holds a
 * Tesseract ({@code full}), drawn floating and turning over it by the client while it is there. Right-click takes it
 * (into the hand if it is empty, else the inventory, else at the feet), once: the tower never makes another. A
 * Tesseract held to an empty cradle is set back in it. Generated full (the template's state); a placed one is empty.
 * Unbreakable (the registry's properties); it glows while full.
 */
public class TesseractCradleBlock extends BaseEntityBlock {
    public static final MapCodec<TesseractCradleBlock> CODEC = simpleCodec(TesseractCradleBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** Whether a Tesseract sits in it. */
    public static final BooleanProperty FULL = BooleanProperty.create("full");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(4, 3, 4, 12, 9, 12),
            Block.box(2, 9, 2, 14, 11, 14));

    public TesseractCradleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FULL, false));
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FULL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(FULL) || !stack.is(ModItems.TESSERACT.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            stack.consume(1, player);
            set(level, pos, state, true, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!state.getValue(FULL)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            take(level, pos, state, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Gives the cradle's Tesseract to {@code player} and empties it (false if it was empty). */
    public static boolean take(Level level, BlockPos pos, BlockState state, Player player) {
        if (!(state.getBlock() instanceof TesseractCradleBlock) || !state.getValue(FULL)) {
            return false;
        }
        ItemStack tesseract = new ItemStack(ModItems.TESSERACT.get());
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, tesseract);
        } else if (!player.getInventory().add(tesseract)) {
            player.drop(tesseract, false);
        }
        set(level, pos, state, false, player);
        return true;
    }

    private static void set(Level level, BlockPos pos, BlockState state, boolean full, Player player) {
        level.setBlock(pos, state.setValue(FULL, full), Block.UPDATE_ALL);
        level.playSound(null, pos, ModSounds.PEDESTAL_TAKE.get(), SoundSource.BLOCKS, 1.0F, full ? 0.8F : 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TesseractCradleBlockEntity(pos, state);
    }
}
