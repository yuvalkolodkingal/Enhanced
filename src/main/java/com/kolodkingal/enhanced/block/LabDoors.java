package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The lab's automatic doors (3.7, lane L4): an {@link XDoorBlock} (two leaves, each two high) and Cerebro's {@link
 * XDoorRoundBlock} (3 x 3) open with a hiss when a player comes within {@link #NEAR} of them (each player's tick looks
 * round him every {@link #LOOK_EVERY} ticks), and while open look every {@link #RECHECK} ticks whether anyone is still
 * near: if not, they shut with a thud. Mobs never open them. A door's blocks are set with {@code UPDATE_CLIENTS} only.
 */
public final class LabDoors {
    /** How near (blocks, to the door's blocks) a player opens a door. */
    public static final double NEAR = 2.0;
    public static final int LOOK_EVERY = 4;
    public static final int RECHECK = 20;

    /** A player's tick: the closed lab doors within reach of him open. */
    public static void nearby(ServerPlayer player) {
        if ((player.tickCount + player.getId()) % LOOK_EVERY == 0 && !player.isSpectator()) {
            openNear(player);
        }
    }

    /** Opens the closed lab doors within reach of {@code player}. */
    public static void openNear(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos at = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 2, 2))) {
            BlockState state = level.getBlockState(p);
            if ((state.getBlock() instanceof XDoorBlock || state.getBlock() instanceof XDoorRoundBlock)
                    && !state.getValue(BlockStateProperties.OPEN)) {
                List<BlockPos> cells = cells(level, p.immutable());
                if (near(player, cells)) {
                    set(level, cells, true);
                }
            }
        }
    }

    /** The blocks of the door that has a block at {@code pos}. */
    public static List<BlockPos> cells(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof XDoorBlock) {
            return XDoorBlock.cells(level, pos);
        }
        if (state.getBlock() instanceof XDoorRoundBlock) {
            return XDoorRoundBlock.cells(level, pos);
        }
        return List.of();
    }

    /** The door's space, every block of it. */
    static AABB space(List<BlockPos> cells) {
        AABB space = null;
        for (BlockPos p : cells) {
            space = space == null ? new AABB(p) : space.minmax(new AABB(p));
        }
        return space;
    }

    static boolean near(ServerPlayer player, List<BlockPos> cells) {
        AABB space = space(cells);
        return space != null && space.inflate(NEAR, 1.0, NEAR).intersects(player.getBoundingBox());
    }

    /** Whether any player (not a spectator) is near the door. */
    static boolean anyoneNear(ServerLevel level, List<BlockPos> cells) {
        AABB space = space(cells);
        return space != null && !level.getEntitiesOfClass(ServerPlayer.class, space.inflate(NEAR, 1.0, NEAR),
                p -> !p.isSpectator()).isEmpty();
    }

    /** Opens or shuts the door of {@code cells}; opening, it looks again in {@link #RECHECK} ticks. */
    public static void set(ServerLevel level, List<BlockPos> cells, boolean open) {
        boolean changed = false;
        for (BlockPos p : cells) {
            BlockState state = level.getBlockState(p);
            if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN) != open) {
                level.setBlock(p, state.setValue(BlockStateProperties.OPEN, open), Block.UPDATE_CLIENTS);
                changed = true;
            }
        }
        if (cells.isEmpty()) {
            return;
        }
        BlockPos first = cells.get(0);
        if (open) {
            level.scheduleTick(first, level.getBlockState(first).getBlock(), RECHECK);
        }
        if (changed) {
            AABB space = space(cells);
            level.playSound(null, space.getCenter().x, space.getCenter().y, space.getCenter().z,
                    open ? ModSounds.X_DOOR_OPEN.get() : ModSounds.X_DOOR_CLOSE.get(), SoundSource.BLOCKS, 0.8F,
                    1.0F);
        }
    }

    /**
     * Shuts every open lab door within {@code radius} of {@code pos} now (Cerebro's round door when the console turns
     * on: lane L5's {@code CerebroRoom} may call it; the door also shuts by itself once nobody stands near it).
     */
    public static void shutNear(ServerLevel level, BlockPos pos, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius,
                radius))) {
            BlockState state = level.getBlockState(p);
            if ((state.getBlock() instanceof XDoorBlock || state.getBlock() instanceof XDoorRoundBlock)
                    && state.getValue(BlockStateProperties.OPEN)) {
                set(level, cells(level, p.immutable()), false);
            }
        }
    }

    /** An open door's look: someone near keeps it open, else it shuts. */
    static void recheck(ServerLevel level, BlockPos pos) {
        List<BlockPos> cells = cells(level, pos);
        if (cells.isEmpty()) {
            return;
        }
        if (anyoneNear(level, cells)) {
            level.scheduleTick(pos, level.getBlockState(pos).getBlock(), RECHECK);
        } else {
            set(level, cells, false);
        }
    }

    private LabDoors() {
    }
}
