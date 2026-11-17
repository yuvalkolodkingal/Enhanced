package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The portals of 3.17's magic: a sorcerer's sling portal, Billy's gates between the worlds, and the portal to the Coven's
 * Road. Each is a {@link MagicZones.Zone} of kind {@link MagicZones.Kind#PORTAL} that everyone of the squad (players and
 * tamed pets) walks into; a portal to another world takes players only (a pet stays behind).
 */
public final class Portals {
    private Portals() {
    }

    /**
     * Opens a portal at {@code at} facing {@code facing}, to {@code dest} in {@code destDim}, for {@code ticks}; with a
     * twin at the far end when both are in the same world (the way back).
     */
    public static MagicZones.Zone open(ServerPlayer owner, Vec3 at, Vec3 facing, ResourceKey<Level> destDim, Vec3 dest,
                                       int ticks, int color) {
        ServerLevel level = owner.serverLevel();
        MagicZones.Zone zone = MagicZones.add(MagicZones.Kind.PORTAL, owner, at, 1.3, ticks, 1.0F);
        zone.dest = dest;
        zone.destDim = destDim;
        MagicZones.show(zone, MagicFx.sendLasting(level, MagicFx.PORTAL, at, List.of(facing), color, 0xFFFFFF, 1.3F, ticks));
        level.playSound(null, at.x, at.y, at.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6F, 1.6F);
        if (destDim == level.dimension()) {
            MagicZones.Zone back = MagicZones.add(MagicZones.Kind.PORTAL, owner, dest, 1.3, ticks, 1.0F);
            back.dest = at.add(facing.normalize().scale(2.0));
            back.destDim = destDim;
            MagicZones.show(back, MagicFx.sendLasting(level, MagicFx.PORTAL, dest, List.of(facing.scale(-1)), color, 0xFFFFFF, 1.3F, ticks));
        }
        return zone;
    }

    /** A portal with its own action (the Coven's Road): stepping in does it. */
    public static MagicZones.Zone openAction(ServerPlayer owner, Vec3 at, Vec3 facing, int ticks, int color, Runnable onEnter) {
        ServerLevel level = owner.serverLevel();
        MagicZones.Zone zone = MagicZones.add(MagicZones.Kind.PORTAL, owner, at, 1.4, ticks, 1.0F);
        zone.onEnter = onEnter;
        MagicZones.show(zone, MagicFx.sendLasting(level, MagicFx.PORTAL, at, List.of(facing), color, 0xBFF5F0, 1.4F, ticks));
        level.playSound(null, at.x, at.y, at.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6F, 1.2F);
        return zone;
    }

    /** {@code e} steps into the portal. */
    static void through(Entity e, MagicZones.Zone z) {
        if (z.onEnter != null) {
            if (e instanceof ServerPlayer player) {
                CovenRoad.enterThrough(player, z);
            }
            return;
        }
        if (z.dest == null || z.destDim == null || !(e.level() instanceof ServerLevel here)) {
            return;
        }
        ServerLevel there = here.getServer().getLevel(z.destDim);
        if (there == null || e instanceof net.minecraft.world.entity.player.Player walker && RoadTrials.isInside(walker)) {
            return;
        }
        Vec3 to = z.dest;
        if (e instanceof ServerPlayer player) {
            Vec3 safe = EternityRealm.safeSpot(there, player, to);
            if (safe == null || !HexManager.onTeleport(there, player, safe)) {
                return;
            }
            MagicFx.burst(here, player.position().add(0, 1, 0), 0x5CC8FF, 16, 0.4, 1.0F);
            player.teleportTo(there, safe.x, safe.y, safe.z, player.getYRot(), player.getXRot());
            player.resetFallDistance();
            there.playSound(null, safe.x, safe.y, safe.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.25F, 1.6F);
            MagicFx.burst(there, safe.add(0, 1, 0), 0x5CC8FF, 16, 0.4, 1.0F);
            MagicZones.mark(player, "portal_used", 40);
        } else if (there == here && HexManager.onTeleport(here, e, to)) {
            e.teleportTo(to.x, to.y, to.z);
            e.resetFallDistance();
        }
    }

    /** The surface above (or below) {@code at} in a world, for a gate's far end. */
    public static Vec3 surface(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        level.getChunk(pos);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return new Vec3(at.x, y, at.z);
    }

    /**
     * Where a gate of Billy's leads from this world: the Overworld and the Nether to each other (an eighth or eight
     * times as far, as their own portals do), the End and everywhere else to the Overworld's spawn.
     */
    @Nullable
    public static Vec3 gateDestination(ServerLevel from, ServerLevel to, Vec3 at) {
        if (from.dimension() == Level.OVERWORLD && to.dimension() == Level.NETHER) {
            return new Vec3(at.x / 8.0, 64, at.z / 8.0);
        }
        if (from.dimension() == Level.NETHER && to.dimension() == Level.OVERWORLD) {
            return surface(to, new Vec3(at.x * 8.0, 64, at.z * 8.0));
        }
        BlockPos spawn = to.getSharedSpawnPos();
        return surface(to, Vec3.atBottomCenterOf(spawn));
    }
}
