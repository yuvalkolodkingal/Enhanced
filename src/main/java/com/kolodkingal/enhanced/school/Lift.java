package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SchoolLabConfig;
import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.world.SchoolLabPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The lift between the study and the lab (3.7, the plan's 6.4): a ride, not a shaft (nothing is dug through the ground,
 * the cellar or caves). A player standing on an elevator pad who holds Sneak {@code [school_lab] liftSneakTicks} (20)
 * rides to the linked pad: a study pad to the lab pad of the same index, a lab pad to the study's (the shelves swing
 * open for him). He arrives standing on the other pad facing out of it (east, turned as the school), with the doors'
 * fade on his screen ({@link LabFxPayload#LIFT_DOWN} / {@link LabFxPayload#LIFT_UP}), the ride's hum and a ding. A
 * missing study pad (broken in Creative) sends him out to the school's front door, never into a wall. Mobs never
 * ride; one rides once per press (let go of Sneak to ride again).
 */
public final class Lift {

    /** A player's Sneak on a pad: how long, and whether this press already rode. */
    private static final class Hold {
        int ticks;
        boolean rode;
    }

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();

    static int sneakTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? SchoolLabConfig.LIFT_SNEAK_TICKS.get() : 20;
    }

    /** Every player tick (lane L4's {@link LabEvents}). */
    public static void tick(ServerPlayer player) {
        if (!player.isShiftKeyDown() || player.isSpectator() || player.isPassenger()) {
            HOLDS.remove(player.getUUID());
            return;
        }
        BlockPos pad = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        if (!player.onGround() || !player.level().getBlockState(pad).is(ModBlocks.ELEVATOR_PAD.get())) {
            HOLDS.remove(player.getUUID());
            return;
        }
        Hold hold = HOLDS.computeIfAbsent(player.getUUID(), id -> new Hold());
        if (hold.rode) {
            return;
        }
        if (++hold.ticks >= sneakTicks()) {
            hold.rode = true;
            ride(player, pad);
        }
    }

    /** Where a ride from the pad at {@code pad} goes, or null (a pad of no lab). */
    @Nullable
    public static Ride rideFrom(ServerLevel level, BlockPos pad) {
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }
        for (SchoolLabs.Lab lab : SchoolLabs.get(level).all()) {
            if (lab.state() != SchoolLabs.State.DONE) {
                continue;
            }
            LabSite site = lab.site();
            for (int i = 0; i < SchoolLabPlan.STUDY_PADS.size(); i++) {
                BlockPos study = site.school(SchoolLabPlan.STUDY_PADS.get(i));
                BlockPos below = site.lab(SchoolLabPlan.LAB_PADS.get(i));
                if (pad.equals(study)) {
                    return new Ride(site, below, true, false);
                }
                if (pad.equals(below)) {
                    boolean there = level.getBlockState(study).is(ModBlocks.ELEVATOR_PAD.get());
                    return there ? new Ride(site, study, false, false)
                            : new Ride(site, site.school(SchoolLabPlan.FRONT_DOOR_OUTSIDE), false, true);
                }
            }
        }
        return null;
    }

    /**
     * A ride: to the block {@code to} (a pad: he stands on it; the front door's outside: he stands there).
     *
     * @param down    to the lab
     * @param outside to the school's front door (the study pad is gone)
     */
    public record Ride(LabSite site, BlockPos to, boolean down, boolean outside) {
    }

    /** Rides {@code player} from the pad at {@code pad}; false if it is a pad of no lab. */
    public static boolean ride(ServerPlayer player, BlockPos pad) {
        ServerLevel level = player.serverLevel();
        Ride ride = rideFrom(level, pad);
        if (ride == null) {
            return false;
        }
        if (!ride.down() && !ride.outside()) {
            SecretEntrance.openShelves(level, ride.site());
        }
        level.playSound(null, pad, ModSounds.LIFT_RIDE.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
        Direction out = ride.outside() ? ride.site().turn(Direction.SOUTH) : ride.site().turn(Direction.EAST);
        double y = ride.outside() ? ride.to().getY() : ride.to().getY() + 1.0;
        player.stopRiding();
        player.teleportTo(level, ride.to().getX() + 0.5, y, ride.to().getZ() + 0.5, out.toYRot(), 0.0F);
        player.resetFallDistance();
        level.playSound(null, ride.to(), ModSounds.LIFT_RIDE.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
        level.playSound(null, ride.to(), ModSounds.LIFT_DING.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        PacketDistributor.sendToPlayer(player, new LabFxPayload(ride.down() ? LabFxPayload.LIFT_DOWN
                : LabFxPayload.LIFT_UP, ride.to()));
        return true;
    }

    /** Forgets a player's hold (he left). */
    public static void forget(ServerPlayer player) {
        HOLDS.remove(player.getUUID());
    }

    public static void clear() {
        HOLDS.clear();
    }

    private Lift() {
    }
}
