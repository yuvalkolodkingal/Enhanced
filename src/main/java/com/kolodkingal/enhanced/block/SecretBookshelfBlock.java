package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.school.SecretEntrance;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * A secret bookshelf of the Professor's study (3.7, lane L4): it looks exactly like a bookshelf; right-clicked, it and
 * the secret shelves beside and above it (the two columns) swing open ({@link #OPEN}: a thin hinged panel at its outer
 * side, nothing to bump into) onto the nook with the elevator pads, with a creak, and close by themselves when nobody
 * stands in them any more (a look every second after {@link SecretEntrance#OPEN_TICKS}). {@link #FACING} is the side
 * that faces the room, {@link #HINGE} the side its panel swings on (left: on the left of one who stands in the nook).
 * Who may open them: {@link SecretEntrance#mayOpen}. A plain bookshelf drops from it (Creative only: in a built lab it
 * is unbreakable).
 */
public class SecretBookshelfBlock extends Block {
    public static final MapCodec<SecretBookshelfBlock> CODEC = simpleCodec(SecretBookshelfBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    /** The most shelves one swing moves together. */
    static final int GROUP = 8;

    private static final Map<Direction, VoxelShape> LEFT = LabShapes.turned(new double[]{0, 0, 0, 2, 16, 16});
    private static final Map<Direction, VoxelShape> RIGHT = LabShapes.turned(new double[]{14, 0, 0, 16, 16, 16});

    public SecretBookshelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false)
                .setValue(HINGE, DoorHingeSide.LEFT));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HINGE);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING))).cycle(HINGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
            return Shapes.block();
        }
        return (state.getValue(HINGE) == DoorHingeSide.LEFT ? LEFT : RIGHT).get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!SecretEntrance.mayOpen(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.school_lab.shelf_locked"), true);
            return InteractionResult.CONSUME;
        }
        swing((ServerLevel) level, pos, !state.getValue(OPEN));
        return InteractionResult.CONSUME;
    }

    /** Swings the shelves of the group at {@code pos} open or shut; opening, they look again after a while. */
    public static void swing(ServerLevel level, BlockPos pos, boolean open) {
        List<BlockPos> group = group(level, pos);
        boolean changed = false;
        for (BlockPos p : group) {
            BlockState state = level.getBlockState(p);
            if (state.getValue(OPEN) != open) {
                level.setBlock(p, state.setValue(OPEN, open), Block.UPDATE_ALL);
                changed = true;
            }
            if (open) {
                level.scheduleTick(p, state.getBlock(), SecretEntrance.OPEN_TICKS);
            }
        }
        if (!changed) {
            return;
        }
        level.playSound(null, pos, open ? ModSounds.SECRET_SHELF_OPEN.get() : ModSounds.SECRET_SHELF_CLOSE.get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        Net.sendToPlayersNear(level, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32,
                new LabFxPayload(open ? LabFxPayload.SHELF_OPEN : LabFxPayload.SHELF_CLOSE, pos));
    }

    /** The secret shelves that swing together with the one at {@code pos}: beside it along the wall, above, below. */
    public static List<BlockPos> group(BlockGetter level, BlockPos pos) {
        BlockState first = level.getBlockState(pos);
        List<BlockPos> out = new ArrayList<>();
        if (!(first.getBlock() instanceof SecretBookshelfBlock)) {
            return out;
        }
        Direction facing = first.getValue(FACING);
        Direction along = facing.getClockWise();
        Deque<BlockPos> todo = new ArrayDeque<>();
        todo.add(pos);
        while (!todo.isEmpty() && out.size() < GROUP) {
            BlockPos p = todo.poll();
            if (out.contains(p)) {
                continue;
            }
            BlockState state = level.getBlockState(p);
            if (!(state.getBlock() instanceof SecretBookshelfBlock) || state.getValue(FACING) != facing) {
                continue;
            }
            out.add(p);
            todo.add(p.above());
            todo.add(p.below());
            todo.add(p.relative(along));
            todo.add(p.relative(along.getOpposite()));
        }
        return out;
    }

    /** An open shelf looks whether anyone still stands in the group: if not, they all close. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OPEN)) {
            return;
        }
        if (occupied(level, group(level, pos))) {
            level.scheduleTick(pos, this, 20);
            return;
        }
        swing(level, pos, false);
    }

    /** Whether anyone (or anything alive) stands in the shelves' blocks. */
    public static boolean occupied(Level level, List<BlockPos> group) {
        AABB space = null;
        for (BlockPos p : group) {
            space = space == null ? new AABB(p) : space.minmax(new AABB(p));
        }
        return space != null && !level.getEntitiesOfClass(LivingEntity.class, space).isEmpty();
    }
}
