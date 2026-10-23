package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SchoolLabConfig;
import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The Weapon X rack (3.7, lane L4): a steel stand holding three Adamantium Injections (drawn by its renderer, the
 * injection's own item model). Right-clicked, any player who has taken fewer than {@code [school_lab]
 * injectionsPerPlayer} (1) from this rack gets one (the block entity keeps the takers: two brothers get one each);
 * else "You already took an injection from this rack." The injection itself decides who it works on (lane L2).
 */
public class AdamantiumRackBlock extends BaseEntityBlock {
    public static final MapCodec<AdamantiumRackBlock> CODEC = simpleCodec(AdamantiumRackBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final Map<Direction, VoxelShape> SHAPE = LabShapes.turned(new double[]{1, 0, 3, 15, 2, 13},
            new double[]{1, 2, 10, 15, 14, 13});

    public AdamantiumRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        builder.add(FACING);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(FACING));
    }

    static int perPlayer() {
        return EnhancedConfig.SPEC.isLoaded() ? SchoolLabConfig.INJECTIONS_PER_PLAYER.get() : 1;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        take((ServerLevel) level, pos, player);
        return InteractionResult.CONSUME;
    }

    /** {@code player} takes an injection from the rack at {@code pos}; true if he got one. */
    public static boolean take(ServerLevel level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof AdamantiumRackBlockEntity rack)) {
            return false;
        }
        if (rack.taken(player.getUUID()) >= perPlayer()) {
            player.displayClientMessage(Component.translatable("message.enhanced.wolverine.rack_taken"), true);
            return false;
        }
        rack.take(player.getUUID());
        ItemStack injection = new ItemStack(ModItems.ADAMANTIUM_INJECTION.get());
        if (!player.getInventory().add(injection)) {
            player.drop(injection, false);
        }
        level.playSound(null, pos, ModSounds.ADAMANTIUM_RACK_TAKE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        Net.sendToPlayersNear(level, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32,
                new LabFxPayload(LabFxPayload.RACK_TAKE, pos));
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdamantiumRackBlockEntity(pos, state);
    }
}
