package com.kolodkingal.enhanced.block.asgard;

import com.kolodkingal.enhanced.asgard.SeatEntity;
import com.kolodkingal.enhanced.odin.Coronation;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Throne of Asgard (3.5), on the dais of the throne room ({@code CityPlan.THRONE}), facing the hall. Right-click:
 * the player sits on it (an invisible {@link SeatEntity}); whoever sits down is told to {@link Coronation#onSit} (lane
 * L3: the coronation of Odin).
 */
public class OdinThroneBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<OdinThroneBlock> CODEC = simpleCodec(OdinThroneBlock::new);
    /** Where a sitter's seat is, above the block's bottom. */
    public static final double SEAT_HEIGHT = 0.3;
    private static final VoxelShape SEAT = Block.box(1, 0, 1, 15, 9, 15);
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        // The back is on the side away from the front: south for a throne facing north.
        SHAPES[Direction.NORTH.get2DDataValue()] = Shapes.or(SEAT, Block.box(1, 9, 13, 15, 16, 16));
        SHAPES[Direction.SOUTH.get2DDataValue()] = Shapes.or(SEAT, Block.box(1, 9, 0, 15, 16, 3));
        SHAPES[Direction.WEST.get2DDataValue()] = Shapes.or(SEAT, Block.box(13, 9, 1, 16, 16, 15));
        SHAPES[Direction.EAST.get2DDataValue()] = Shapes.or(SEAT, Block.box(0, 9, 1, 3, 16, 15));
    }

    public OdinThroneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && !player.isShiftKeyDown()) {
            sit(server, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Seats the player on the throne at {@code pos} and tells the coronation (also when he already sits there). */
    public static void sit(ServerPlayer player, BlockPos pos) {
        Vec3 seat = Vec3.atBottomCenterOf(pos).add(0, SEAT_HEIGHT, 0);
        boolean seated = player.getVehicle() instanceof SeatEntity s && pos.equals(s.seatOf());
        if (!seated && SeatEntity.sit(player, seat, pos) != null) {
            player.level().playSound(null, pos, ModSounds.THRONE_SIT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            seated = true;
        }
        if (seated) {
            Coronation.onSit(player, pos);
        }
    }
}
