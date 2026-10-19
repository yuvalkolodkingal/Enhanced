package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sneak + G: Claw Climb (3.7, {@link Toggle#CLAW_CLIMB}, needs the claws out). Pressed against a wall he climbs it by
 * his claws, {@code climbSpeed} 0.12 a tick; holding Sneak he hangs still; claw marks show on the wall. Not on what a
 * claw cannot bite ({@code #enhanced:claw_slippery}: glass, ice, slime, honey). The movement is the player's own (his
 * client moves him, as for a ladder; the server does the same for a body without a client and keeps him from fall
 * damage), from {@link WolverineEvents}: the rules are here, the same on both sides.
 */
public final class ClawClimb {
    /** How far from his box a wall still counts as touched. */
    static final double REACH = 0.08;

    /** Whether the player climbs by his claws now (the switch, the claws out, not swimming, riding or flying). */
    public static boolean on(Player player, PowerData data) {
        return data.has(HeroAbility.WOLVERINE_CLAW_CLIMB) && data.toggle(Toggle.CLAW_CLIMB) && Claws.out(data)
                && !player.isInWater() && !player.isPassenger() && !player.getAbilities().flying
                && !player.isSpectator();
    }

    /** Whether a claw bites into this block: something solid that is not slippery. */
    public static boolean grips(BlockState state) {
        return !state.isAir() && !state.is(ModTags.CLAW_SLIPPERY);
    }

    /** The block of a wall beside him that his claws bite into, or null. */
    @Nullable
    public static BlockPos wall(Player player) {
        Level level = player.level();
        AABB body = player.getBoundingBox().deflate(0.0, 0.05, 0.0);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            AABB moved = body.move(dir.getStepX() * REACH, 0.0, dir.getStepZ() * REACH);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(moved.minX, moved.minY, moved.minZ),
                    BlockPos.containing(moved.maxX, moved.maxY, moved.maxZ))) {
                BlockState state = level.getBlockState(pos);
                VoxelShape shape = state.getCollisionShape(level, pos);
                if (shape.isEmpty() || !grips(state)) {
                    continue;
                }
                if (shape.bounds().move(pos).intersects(moved) && !shape.bounds().move(pos).intersects(body)) {
                    return pos.immutable();
                }
            }
        }
        return null;
    }

    /**
     * One tick of his own movement: against a wall he bites into, pushing into it (or off the ground already), he goes
     * up, or hangs still while sneaking. Returns whether he climbs. Both sides (his client for a real player).
     */
    public static boolean move(Player player) {
        PowerData data = Powers.get(player);
        if (!on(player, data)) {
            return false;
        }
        if (!player.horizontalCollision && player.zza <= 0.0F && player.onGround()) {
            return false;
        }
        if (wall(player) == null) {
            return false;
        }
        Vec3 m = player.getDeltaMovement();
        double up = player.isShiftKeyDown() ? 0.0 : WolverineAbilities.cfg(WolverineConfig.CLIMB_SPEED, 0.12);
        player.setDeltaMovement(m.x * 0.6, up, m.z * 0.6);
        player.resetFallDistance();
        return true;
    }

    /** The server's part: no fall damage while he clings, the marks and the scrape every few ticks. */
    static void serverTick(ServerPlayer player, PowerData data, WolverineAbilities.State state, long now) {
        if (!on(player, data) || player.onGround()) {
            return;
        }
        BlockPos wall = wall(player);
        if (wall == null) {
            return;
        }
        player.resetFallDistance();
        if (now - state.lastClimbFx >= 8) {
            state.lastClimbFx = now;
            ServerLevel level = player.serverLevel();
            Vec3 at = Vec3.atCenterOf(wall);
            WolverineAbilities.sound(level, at, ModSounds.WOLVERINE_CLIMB.get(), 0.6F, 0.9F
                    + level.random.nextFloat() * 0.2F);
            WolverineAbilities.send(level, player, WolverineFxPayload.CLIMB, List.of(at, player.position()), 0);
        }
    }

    private ClawClimb() {
    }
}
