package com.kolodkingal.enhanced.vishanti;

import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.platform.event.FinalizeSpawnEvent;
import com.kolodkingal.enhanced.platform.event.LevelTickEvent;
import com.kolodkingal.enhanced.platform.event.MobSpawnEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

/**
 * The game-bus events of the wards (3.2): they look for monsters every few ticks, refuse the spawns inside them, tell the
 * clients which wards stand in the dimension a player is in, and keep a sorcerer's wards as strong as the sorcerer is.
 */
@Subscriber
public final class WardEvents {
    @Subscribe
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WardManager.tickLevel(level);
        }
    }

    /**
     * The spawn placement of a monster is refused inside a ward (natural spawns, spawners). Only for a real server level: the
     * region of the world generator is no place for this lookup (another thread).
     */
    @Subscribe
    public static void onSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel() instanceof ServerLevel level && WardManager.forbidsSpawn(level, event.getEntityType(), Vec3.atCenterOf(event.getPos()))) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    /** And so is any spawn that gets past the placement (a mob a spawner or a structure finishes), except one a player asked for. */
    @Subscribe
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (type == MobSpawnType.COMMAND || type == MobSpawnType.SPAWN_EGG || type == MobSpawnType.DISPENSER) {
            return;
        }
        Mob mob = event.getEntity();
        if (event.getLevel() instanceof ServerLevel level && WardManager.forbidsSpawn(level, mob.getType(), new Vec3(event.getX(), event.getY(), event.getZ()))) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // A ward whose base is no longer in the book (a wiped book) is taken away too.
            if (PlaceBook.Kind.VISHANTI.data(player).bases() > 0 || WardManager.count(player.server, player.getUUID()) > 0) {
                WardManager.refresh(player);
            }
            WardManager.send(player);
        }
    }

    @Subscribe
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WardManager.send(player);
        }
    }

    @Subscribe
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WardManager.send(player);
        }
    }

    /** A book that was about to take someone does not wait for them to come back. */
    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlaceBook.journey(player, PlaceBook.Kind.VISHANTI) != null) {
            PlaceBook.cancel(player);
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        WardManager.clear();
    }

    private WardEvents() {
    }
}
