package com.kolodkingal.enhanced.healing;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.HealingConfig;
import com.kolodkingal.enhanced.network.HealingFxPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * The rescue of a healer who rises in a hazard (3.7, the plan's 2.4): DOWN under water without air, in lava or fire, in
 * powder snow, inside a block or at the very bottom of the world, he would only go DOWN again for ever; when he rises
 * his healing factor "drags him to safe ground": the nearest dry standing place within {@code rescueRadius} (an outward
 * search, ring by ring, of the columns round him: the first two blocks of air over a solid block that does not burn,
 * freeze or flow), or his respawn point (his bed, or the world's spawn) when there is none. In a boss's arena the search
 * stays inside the arena (and he stays where he is when it finds nothing there).
 */
public final class Rescue {
    private Rescue() {
    }

    public static int radius() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.RESCUE_RADIUS.get() : 32;
    }

    /** Whether he lies in a hazard that would take him DOWN again as soon as he rose. */
    public static boolean inHazard(ServerPlayer player) {
        Level level = player.level();
        if (player.getY() < level.getMinBuildHeight() + 1) {
            return true;
        }
        if (player.isInLava() || player.isInPowderSnow || player.isInWall()) {
            return true;
        }
        BlockState feet = level.getBlockState(player.blockPosition());
        if (feet.is(BlockTags.FIRE) || feet.is(Blocks.POWDER_SNOW)) {
            return true;
        }
        FluidType eyes = player.getEyeInFluidType();
        return !eyes.isAir() && player.canDrownInFluidType(eyes) && !player.canBreatheUnderwater();
    }

    /**
     * Moves him out of his hazard (the nearest safe place, else his respawn point; in an arena only inside it), puts
     * his fire out, gives him his air. Returns whether he was moved.
     */
    public static boolean rescue(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BossArena arena = arenaOf(player);
        Predicate<BlockPos> allowed = arena == null ? pos -> true : arena::contains;
        BlockPos spot = findSpot(level, player.blockPosition(), radius(), allowed);
        boolean moved = false;
        if (spot != null) {
            player.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            moved = true;
        } else if (arena == null) {
            DimensionTransition home = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
            Vec3 at = home.pos();
            player.teleportTo(home.newLevel(), at.x, at.y, at.z, home.yRot(), home.xRot());
            moved = true;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.clearFire();
        player.setTicksFrozen(0);
        player.setAirSupply(player.getMaxAirSupply());
        if (moved) {
            Downed.fx(player, HealingFxPayload.RESCUE, 0);
            Downed.sound(player, ModSounds.HEALING_RESCUE.get(), 1.0F);
        }
        return moved;
    }

    /** The arena of the boss fight he stands in, or null. */
    @Nullable
    static BossArena arenaOf(ServerPlayer player) {
        for (BossFight fight : BossFights.all(player.server)) {
            if (fight.level() == player.level() && fight.arena().contains(player.position())) {
                return fight.arena();
            }
        }
        return null;
    }

    /**
     * The nearest place to stand (the block of his feet) within {@code radius} of {@code center} that {@code allowed}
     * lets him go to, or null: the columns ring by ring outward (only loaded ones), in each column the height nearest
     * his; the nearest of all (in three dimensions: a floor beside a tank before the tank's rim above him), so the
     * search goes on only while a farther ring could still hold a nearer place.
     */
    @Nullable
    public static BlockPos findSpot(ServerLevel level, BlockPos center, int radius, Predicate<BlockPos> allowed) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        for (int ring = 0; ring <= radius && (long) ring * ring < bestDistance; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    if (!level.hasChunk(x >> 4, z >> 4)) {
                        continue;
                    }
                    for (int dy = 0; dy <= radius; dy++) {
                        int found = Integer.MIN_VALUE;
                        if (standable(level, probe.set(x, center.getY() + dy, z)) && allowed.test(probe)) {
                            found = center.getY() + dy;
                        } else if (dy > 0 && standable(level, probe.set(x, center.getY() - dy, z))
                                && allowed.test(probe)) {
                            found = center.getY() - dy;
                        }
                        if (found != Integer.MIN_VALUE) {
                            long distance = (long) dx * dx + (long) dz * dz + (long) dy * dy;
                            if (distance < bestDistance) {
                                bestDistance = distance;
                                best = new BlockPos(x, found, z);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Whether a player can stand with his feet at {@code feet}: a safe solid floor, two blocks of safe air. */
    public static boolean standable(ServerLevel level, BlockPos feet) {
        if (feet.getY() - 1 < level.getMinBuildHeight() || feet.getY() + 1 >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockPos below = feet.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.isFaceSturdy(level, below, Direction.UP) || !floor.getFluidState().isEmpty()
                || floor.is(Blocks.MAGMA_BLOCK) || floor.is(BlockTags.CAMPFIRES) || floor.is(Blocks.CACTUS)
                || floor.is(Blocks.POWDER_SNOW) || floor.is(BlockTags.FIRE)) {
            return false;
        }
        return clear(level, feet) && clear(level, feet.above());
    }

    /** Air enough to be in: nothing to collide with, no fluid, no fire, no powder snow, no web, no thorns. */
    private static boolean clear(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty()
                && !state.is(BlockTags.FIRE) && !state.is(Blocks.POWDER_SNOW) && !state.is(Blocks.COBWEB)
                && !state.is(Blocks.SWEET_BERRY_BUSH) && !state.is(Blocks.WITHER_ROSE) && !state.is(Blocks.NETHER_PORTAL)
                && !state.is(Blocks.END_PORTAL);
    }
}
