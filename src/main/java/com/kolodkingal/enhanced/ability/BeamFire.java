package com.kolodkingal.enhanced.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * (3.17) What a burning beam (Source Code Vision's, and Jim Hammond's) does where it lands: creatures are set alight, and
 * on the ground it leaves fire where it strikes and lights what can burn. Fire is only ever PLACED, on the air next to the
 * block it hits (as flint and steel does): no block is deleted. Every fire obeys the rules of the mod's other block
 * changes ({@link BlockRights#mayPlace}: boss arenas, Asgard, Hexes, spawn protection, claims) and needs the game rule
 * {@code mobGriefing}: with it off nothing is lit.
 */
public final class BeamFire {
    /** The beam lights the ground at most once in this many ticks (it hits every tick). */
    public static final int GROUND_INTERVAL = 4;

    private BeamFire() {
    }

    /** Whether fire may be placed at {@code pos} in this player's name: the gamerule and the mod's block rules. */
    public static boolean mayLight(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && BlockRights.mayPlace(player, level, pos);
    }

    /** Sets the creature a beam hit alight ({@code seconds} 0: nothing). */
    public static void ignite(Entity target, int seconds) {
        if (seconds > 0) {
            target.igniteForSeconds(seconds);
        }
    }

    /**
     * The beam's end: a creature it struck burns for {@code seconds}; a block it struck gets fire on its struck face (and,
     * if it can burn, on its other open faces: up to {@code spread} more). Rate-limited by the caller with {@link
     * #GROUND_INTERVAL}. Returns how many fires were placed.
     */
    public static int strike(ServerPlayer player, ServerLevel level, HitResult hit, int seconds, int spread) {
        if (hit instanceof EntityHitResult entityHit) {
            ignite(entityHit.getEntity(), seconds);
            return 0;
        }
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return 0;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        int placed = place(player, level, pos.relative(blockHit.getDirection())) ? 1 : 0;
        if (spread > 0) {
            for (Direction face : Direction.values()) {
                if (placed > spread || face == blockHit.getDirection() || !state.isFlammable(level, pos, face)) {
                    continue;
                }
                if (place(player, level, pos.relative(face))) {
                    placed++;
                }
            }
        }
        return placed;
    }

    /** One fire on the empty cell {@code at}, if it may be lit and the fire could stand there. */
    public static boolean place(ServerPlayer player, ServerLevel level, BlockPos at) {
        if (!level.isEmptyBlock(at) || !mayLight(player, level, at)) {
            return false;
        }
        BlockState fire = BaseFireBlock.getState(level, at);
        return fire.canSurvive(level, at) && level.setBlockAndUpdate(at, fire);
    }
}
