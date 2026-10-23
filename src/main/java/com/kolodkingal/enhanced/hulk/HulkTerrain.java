package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.platform.CommonTags;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The blocks the Hulk breaks (3.6). {@code [hulk] blockBreaking} says how much: {@code none}, {@code soft} (the
 * default: {@code #enhanced:hulk_soft} only, glass, leaves, snow, ice, flowers, grass, vines, cobwebs) or {@code all}
 * (also {@code #enhanced:hulk_smashable}, hardness 3 or less: small craters in dirt, stone and wood). Every break also
 * needs the gamerule {@code mobGriefing} ({@code needsMobGriefing}) and {@link BlockRights#mayBreak} (a boss arena, the
 * city of Asgard, a Hex, a ward, the Mirror Dimension, spawn protection, claims); never a block entity, a door, a
 * trapdoor, a bed, a crop or {@code #enhanced:hulk_unbreakable}. Only on the server. What breaks drops as a
 * player-caused explosion would (one block in {@code radius}).
 */
public final class HulkTerrain {
    /** What one of his moves may break. */
    public enum Kind {
        /** Hulk Smash: soft round his feet (radius 3), a crater of 1.5 with {@code all}. */
        SMASH(3.0, 1.5, false),
        /** The Super Leap's landing: radius 2 either way. */
        LEAP(2.0, 2.0, false),
        /** The Thunder Clap's cone: glass, panes and snow only. */
        CLAP(0.0, 0.0, true),
        /** The Gamma Charge's path: two wide, three high. */
        CHARGE(1.0, 1.0, false),
        /** Where the boulder lands: a crater of 1 with {@code all} (nothing soft). */
        BOULDER(0.0, 1.0, false);

        final double soft;
        final double crater;
        final boolean glassOnly;

        Kind(double soft, double crater, boolean glassOnly) {
            this.soft = soft;
            this.crater = crater;
            this.glassOnly = glassOnly;
        }
    }

    /** The mode in the config (soft when it is not loaded). */
    public static HulkConfig.BlockBreaking mode() {
        return HulkAbilities.loaded() ? HulkConfig.BLOCK_BREAKING.get() : HulkConfig.BlockBreaking.SOFT;
    }

    /**
     * Whether the Hulk may break this block in this mode: the kind of block (soft, smashable, never the unbreakable,
     * block entities, doors, trapdoors, beds, crops), the gamerule and the protections.
     */
    public static boolean mayBreak(ServerPlayer player, BlockPos pos, Kind kind) {
        HulkConfig.BlockBreaking mode = mode();
        if (mode == HulkConfig.BlockBreaking.NONE) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !breakable(state, mode, kind)) {
            return false;
        }
        boolean griefing = HulkAbilities.cfg(HulkConfig.NEEDS_MOB_GRIEFING, true);
        return griefing ? BlockRights.mayBlast(player, level, pos) : BlockRights.mayBreak(player, level, pos, state);
    }

    /** Whether the kind of block is one the mode lets him break (no rights asked). */
    public static boolean breakable(BlockState state, HulkConfig.BlockBreaking mode, Kind kind) {
        if (mode == HulkConfig.BlockBreaking.NONE || state.hasBlockEntity() || state.is(ModTags.HULK_UNBREAKABLE)
                || state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.BEDS) || state.is(BlockTags.CROPS)
                        || state.is(Blocks.BEDROCK)
                || state.getBlock().defaultDestroyTime() < 0.0F) {
            return false;
        }
        if (kind.glassOnly) {
            return state.is(CommonTags.Blocks.GLASS_BLOCKS) || state.is(CommonTags.Blocks.GLASS_PANES)
                    || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
        }
        if (state.is(ModTags.HULK_SOFT)) {
            return true;
        }
        return mode == HulkConfig.BlockBreaking.ALL && state.is(ModTags.HULK_SMASHABLE);
    }

    /** The blocks a move of {@code kind} breaks round {@code center} (a sphere), broken. */
    static List<BlockPos> breakAround(ServerPlayer player, Vec3 center, Kind kind) {
        HulkConfig.BlockBreaking mode = mode();
        if (mode == HulkConfig.BlockBreaking.NONE) {
            return List.of();
        }
        double radius = mode == HulkConfig.BlockBreaking.ALL ? Math.max(kind.soft, kind.crater) : kind.soft;
        List<BlockPos> broken = new ArrayList<>();
        if (radius <= 0.0) {
            return broken;
        }
        int r = (int) Math.ceil(radius);
        BlockPos origin = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -r, -r), origin.offset(r, r, r))) {
            double dx = pos.getX() + 0.5 - center.x;
            double dy = pos.getY() + 0.5 - center.y;
            double dz = pos.getZ() + 0.5 - center.z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > radius) {
                continue;
            }
            BlockState state = player.level().getBlockState(pos);
            // (A crater is only as wide as its own radius; soft blocks go as far as theirs.)
            boolean soft = state.is(ModTags.HULK_SOFT);
            if (!soft && d > kind.crater) {
                continue;
            }
            if (soft && d > kind.soft && d > kind.crater) {
                continue;
            }
            if (mayBreak(player, pos, kind)) {
                breakBlock(player, pos.immutable(), radius);
                broken.add(pos.immutable());
            }
        }
        return broken;
    }

    /** Breaks one block (it drops as an explosion's would: one in {@code radius}). */
    static void breakBlock(ServerPlayer player, BlockPos pos, double radius) {
        ServerLevel level = player.serverLevel();
        boolean drop = level.random.nextDouble() < 1.0 / Math.max(1.0, radius);
        level.destroyBlock(pos, drop, player);
    }

    /** Hulk Smash: round his feet. */
    static List<BlockPos> smash(ServerPlayer player, Vec3 feet) {
        return breakAround(player, feet.add(0, 0.5, 0), Kind.SMASH);
    }

    /** The Super Leap's landing. */
    static List<BlockPos> land(ServerPlayer player, Vec3 feet) {
        return breakAround(player, feet.add(0, 0.5, 0), Kind.LEAP);
    }

    /** The boulder's impact. */
    static List<BlockPos> boulder(ServerPlayer player, Vec3 at) {
        return breakAround(player, at, Kind.BOULDER);
    }

    /** The Thunder Clap's wave over one cell: glass, panes and snow only. */
    static boolean clap(ServerPlayer player, BlockPos pos) {
        if (mayBreak(player, pos, Kind.CLAP)) {
            breakBlock(player, pos, 1.0);
            return true;
        }
        return false;
    }

    /**
     * The Gamma Charge's next cells (his feet at {@code feet}, going {@code dir}): breaks what it may of the two-wide,
     * three-high slice in front; returns whether the way is free afterwards (nothing solid left there).
     */
    static boolean chargeThrough(ServerPlayer player, BlockPos feet, net.minecraft.core.Direction dir) {
        BlockPos front = feet.relative(dir);
        net.minecraft.core.Direction side = dir.getClockWise();
        boolean free = true;
        for (int w = 0; w < 2; w++) {
            for (int h = 0; h < 3; h++) {
                BlockPos pos = front.relative(side, w).above(h);
                BlockState state = player.level().getBlockState(pos);
                if (state.getCollisionShape(player.level(), pos).isEmpty()) {
                    continue;
                }
                if (mayBreak(player, pos, Kind.CHARGE)) {
                    breakBlock(player, pos.immutable(), 6.0);
                } else if (w == 0 && h <= 1) {
                    free = false;
                }
            }
        }
        return free;
    }

    private HulkTerrain() {
    }
}
