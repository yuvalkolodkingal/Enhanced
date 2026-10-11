package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Mjolnir at rest, standing on its head with the handle up, like in the crater in New Mexico. Nothing can break it,
 * blow it up or push it; only its owner can lift it (right click; the first worthy player to lift an unclaimed one
 * claims it, see {@link Worthiness}), and it remembers exactly which hammer it is, owner and all.
 */
public class MjolnirBlock extends BaseEntityBlock {
    public static final MapCodec<MjolnirBlock> CODEC = simpleCodec(MjolnirBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** The head is 12.5 pixels long and the whole hammer almost 19 pixels high: taller than the block it stands in. */
    private static final VoxelShape SHAPE_X = Shapes.or(Block.box(1.75, 0, 4.5, 14.25, 7.5, 11.5), Block.box(6.75, 7.5, 6.75, 9.25, 18.75, 9.25));
    private static final VoxelShape SHAPE_Z = Shapes.or(Block.box(4.5, 0, 1.75, 11.5, 7.5, 14.25), Block.box(6.75, 7.5, 6.75, 9.25, 18.75, 9.25));

    public MjolnirBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Puts a Mjolnir down at {@code pos} (which must be free), facing a random way. Returns false if there is no room.
     */
    public static boolean setDown(ServerLevel level, BlockPos pos, ItemStack hammer) {
        if (!level.getBlockState(pos).canBeReplaced() || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = ModBlocks.MJOLNIR.get().defaultBlockState()
                .setValue(FACING, Direction.Plane.HORIZONTAL.getRandomDirection(level.random));
        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof MjolnirBlockEntity be) {
            be.setHammer(hammer);
        }
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.6F);
        return true;
    }

    /** The hammer resting at {@code pos}, without lifting it. */
    public static ItemStack hammerAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MjolnirBlockEntity be ? be.hammer() : new ItemStack(ModItems.MJOLNIR.get());
    }

    /** Takes the hammer out of the world (its owner lifted it, or Thor called it). */
    public static ItemStack lift(Level level, BlockPos pos) {
        ItemStack hammer = hammerAt(level, pos);
        level.removeBlock(pos, false);
        return hammer;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack resting = hammerAt(level, pos);
        if (!Worthiness.canLift(player, resting)) {
            MjolnirOwner owner = Worthiness.ownerKeepingFrom(player, resting);
            player.displayClientMessage((owner != null ? Component.translatable("message.enhanced.mjolnir.not_yours", owner.name())
                    : Component.translatable("message.enhanced.mjolnir.unworthy")).withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.3F, 0.5F);
            return InteractionResult.CONSUME;
        }
        ItemStack hammer = lift(level, pos);
        Worthiness.claim(player, hammer);
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        } else if (!player.getInventory().add(hammer)) {
            player.drop(hammer, false);
        }
        level.playSound(null, pos, SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 0.6F);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.MJOLNIR.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Now and then a spark of lightning crackles over the head. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.1 + random.nextDouble() * 0.4, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MjolnirBlockEntity(pos, state);
    }
}
