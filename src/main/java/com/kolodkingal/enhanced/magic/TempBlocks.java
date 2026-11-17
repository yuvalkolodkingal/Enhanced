package com.kolodkingal.enhanced.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The blocks a spell conjures for a while (3.17): a bridge or a platform of solid light, a wall, thorns. They only ever
 * take the place of air (or of what air-like plants and snow fill) and go back to air when their time is up, when the
 * server stops, or when one is broken; nothing they replace is lost and nothing drops.
 */
public final class TempBlocks {
    private record Temp(BlockState placed, long until) {
    }

    private static final Map<GlobalPos, Temp> TEMPS = new LinkedHashMap<>();

    private TempBlocks() {
    }

    /** Whether a conjured block may stand at {@code pos} (only air-like places). */
    public static boolean free(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    /** Conjures {@code state} at {@code pos} until game time {@code until}; false if the place is taken. */
    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, long until) {
        if (!level.isLoaded(pos) || !free(level, pos) || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
        TEMPS.put(GlobalPos.of(level.dimension(), pos.immutable()), new Temp(state, until));
        return true;
    }

    /** Whether the block at {@code pos} is a conjured one (it drops nothing when broken). */
    public static boolean isTemp(Level level, BlockPos pos) {
        return TEMPS.containsKey(GlobalPos.of(level.dimension(), pos));
    }

    /** A conjured block was broken: forget it. */
    public static void broken(Level level, BlockPos pos) {
        TEMPS.remove(GlobalPos.of(level.dimension(), pos.immutable()));
    }

    /** Every server tick: the blocks whose time is up vanish. */
    public static void tick(MinecraftServer server) {
        if (TEMPS.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<GlobalPos, Temp>> it = TEMPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<GlobalPos, Temp> e = it.next();
            if (now < e.getValue().until()) {
                continue;
            }
            it.remove();
            restore(server, e.getKey(), e.getValue());
        }
    }

    private static void restore(MinecraftServer server, GlobalPos at, Temp temp) {
        ResourceKey<Level> dim = at.dimension();
        ServerLevel level = server.getLevel(dim);
        if (level == null || !level.isLoaded(at.pos())) {
            return;
        }
        if (level.getBlockState(at.pos()).is(temp.placed().getBlock())) {
            level.setBlock(at.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            MagicFx.burst(level, net.minecraft.world.phys.Vec3.atCenterOf(at.pos()), 0x8FE0FF, 2, 0.3, 0.7F);
        }
    }

    /** The server stops: every conjured block goes now. */
    public static void clearAll(MinecraftServer server) {
        for (Map.Entry<GlobalPos, Temp> e : TEMPS.entrySet()) {
            restore(server, e.getKey(), e.getValue());
        }
        TEMPS.clear();
    }

    public static int count() {
        return TEMPS.size();
    }
}
