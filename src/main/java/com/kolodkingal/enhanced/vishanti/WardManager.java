package com.kolodkingal.enhanced.vishanti;

import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.VishantiConfig;
import com.kolodkingal.enhanced.network.WardSyncPayload;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The wards of the Book of the Vishanti at work. A ward is a sphere round a sorcerer's base: no monster (a creature of the
 * spawn category MONSTER) can be inside it. New ones are refused where it is (the spawn placement, and the finalizing of any
 * spawn), and every {@code wardCheckTicks} ticks the ward looks, with one spatial lookup of the creatures in the box round
 * it, for monsters that are inside: one at the edge is thrown back out, one deeper in (a monster that was there when the
 * ward rose, or got in by a portal) is set down just outside. Not touched: the bosses and what a boss fight makes
 * ({@link ModTags#WARD_IMMUNE}, a fight's members, everything in an arena), someone's pet, a rider. Nothing here changes a block,
 * so a ward is no matter for a claim.
 *
 * <p>The ward belongs to a base place of the owner's book ({@link BifrostData.Place#base}); {@link #refresh} keeps the wards of
 * a level the same as the bases of the owner's book and as strong as the owner is. A ward keeps working with nobody
 * online, for as long as its chunks are loaded.
 */
public final class WardManager {
    /** A monster this close to the edge (blocks, inside) is thrown back; one deeper is set down outside instead. */
    private static final double BAND = 3.0;
    /** The most monsters one ward deals with in one look. */
    private static final int MAX_PER_LOOK = 48;
    /** How far (blocks) up and down a monster set down outside looks for ground. */
    private static final int SCAN = 6;

    /** The game time a player may raise a ward again. */
    private static final Map<UUID, Long> READY = new HashMap<>();

    // ---- who is kept out ----

    /** Whether a ward keeps this kind of creature out: it is a monster, and no boss. */
    public static boolean keepsOut(EntityType<?> type) {
        return type.getCategory() == MobCategory.MONSTER && !type.is(ModTags.WARD_IMMUNE);
    }

    /** Whether this one is let through although it is a monster: a boss, a fight's, a pet, a rider. */
    private static boolean exempt(Mob mob) {
        if (mob.getType().is(ModTags.WARD_IMMUNE) || mob.isPassenger() || mob.isVehicle()) {
            return true;
        }
        if (mob instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
            return true;
        }
        return BossFights.isMember(mob) || BossFights.isFightEntity(mob);
    }

    // ---- the lookups ----

    /** The ward that protects this point of the level, or null. */
    @Nullable
    public static Ward wardAt(ServerLevel level, Vec3 point) {
        WardStore store = WardStore.peek(level);
        return store == null || store.isEmpty() ? null : store.at(point);
    }

    /** Whether a ward protects this point of the level. */
    public static boolean protects(ServerLevel level, Vec3 point) {
        return wardAt(level, point) != null;
    }

    /** Whether a monster of this type may not spawn at this point (a ward protects it, and wards stop spawns). */
    public static boolean forbidsSpawn(ServerLevel level, EntityType<?> type, Vec3 point) {
        // The cheap questions first: this is asked for every spawn attempt of the server.
        if (type.getCategory() != MobCategory.MONSTER) {
            return false;
        }
        WardStore store = WardStore.peek(level);
        return store != null && !store.isEmpty() && VishantiConfig.WARD_BLOCKS_SPAWNS.get() && keepsOut(type) && store.at(point) != null;
    }

    // ---- the look every few ticks ----

    /** Once a tick for every level: each awake ward whose turn it is looks at what is inside it. */
    public static void tickLevel(ServerLevel level) {
        WardStore store = WardStore.peek(level);
        if (store == null || store.isEmpty()) {
            return;
        }
        int every = VishantiConfig.WARD_CHECK_TICKS.get();
        long now = level.getGameTime();
        List<Ward> wards = store.wards();
        for (int i = 0; i < wards.size(); i++) {
            Ward ward = wards.get(i);
            // Staggered, so the wards of a server do not all look in the same tick.
            if (ward.awake() && (now + i) % every == 0) {
                look(level, ward);
            }
        }
    }

    /** One look: every monster inside the ward is thrown or set outside. Does nothing where nothing is loaded. */
    static int look(ServerLevel level, Ward ward) {
        if (!level.isPositionEntityTicking(BlockPos.containing(ward.center()))) {
            return 0;
        }
        AABB box = new AABB(ward.center(), ward.center()).inflate(ward.radius());
        List<Mob> inside = level.getEntitiesOfClass(Mob.class, box, mob -> intrudes(level, ward, mob));
        int done = 0;
        for (Mob mob : inside) {
            expel(level, ward, mob);
            if (++done >= MAX_PER_LOOK) {
                break;
            }
        }
        return inside.size();
    }

    private static boolean intrudes(ServerLevel level, Ward ward, Mob mob) {
        return mob.isAlive() && mob.getType().getCategory() == MobCategory.MONSTER && !exempt(mob)
                && ward.contains(mob.getBoundingBox().getCenter()) && !BossFights.inAnyArena(level, mob.blockPosition());
    }

    private static void expel(ServerLevel level, Ward ward, Mob mob) {
        Vec3 center = ward.center();
        Vec3 at = mob.getBoundingBox().getCenter();
        Vec3 out = at.subtract(center);
        double distance = out.length();
        Vec3 away = distance < 1.0E-3 ? new Vec3(1, 0, 0).yRot(level.random.nextFloat() * Mth.TWO_PI) : out.scale(1.0 / distance);
        mob.getNavigation().stop();
        if (ward.radius() - distance > BAND && setDownOutside(level, ward, mob, away)) {
            return;
        }
        double push = VishantiConfig.WARD_PUSH.get();
        mob.setDeltaMovement(away.x * push, Math.max(0.2, away.y * push), away.z * push);
    }

    /** Sets the monster down on the nearest free ground outside the ward in the direction {@code away}; false if there is none. */
    private static boolean setDownOutside(ServerLevel level, Ward ward, Mob mob, Vec3 away) {
        Vec3 center = ward.center();
        double need = ward.radius() + 1.5;
        double high = mob.getY() - center.y;
        double reach = Math.sqrt(Math.max(0.0, need * need - high * high)) + 1.0;
        Vec3 flat = new Vec3(away.x, 0, away.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0).yRot(level.random.nextFloat() * Mth.TWO_PI) : flat.normalize();
        double x = center.x + flat.x * reach;
        double z = center.z + flat.z * reach;
        if (!level.hasChunkAt(BlockPos.containing(x, mob.getY(), z))) {
            return false;
        }
        boolean grounded = mob.onGround();
        for (int i = 0; i <= 2 * SCAN; i++) {
            // 0, +1, -1, +2, -2 ...: the nearest height first.
            int step = (i + 1) / 2 * (i % 2 == 1 ? 1 : -1);
            double y = mob.getY() + step;
            AABB body = mob.getDimensions(mob.getPose()).makeBoundingBox(x, y, z);
            if (center.distanceToSqr(body.getCenter()) < need * need || !level.noCollision(mob, body)) {
                continue;
            }
            if (grounded && level.noCollision(mob, body.move(0, -0.6, 0))) {
                continue;
            }
            mob.moveTo(x, y, z, mob.getYRot(), mob.getXRot());
            mob.setDeltaMovement(Vec3.ZERO);
            mob.fallDistance = 0.0F;
            return true;
        }
        return false;
    }

    // ---- the wards of a player's book ----

    /**
     * Makes the wards of every level match the bases in this player's book, and as strong as the player is now (a ward sleeps,
     * radius 0, while its owner is no sorcerer). Sends what changed to the clients. Cheap when nothing changed.
     */
    public static void refresh(ServerPlayer owner) {
        BifrostData book = PlaceBook.Kind.VISHANTI.data(owner);
        int radius = WardStrength.radiusOf(owner);
        int color = Powers.power(owner).color;
        MinecraftServer server = owner.server;
        for (ServerLevel level : server.getAllLevels()) {
            Set<String> names = new HashSet<>();
            List<Ward> wanted = new ArrayList<>();
            for (BifrostData.Place place : book.places()) {
                if (place.base() && place.dimension() == level.dimension() && names.add(place.name().toLowerCase(Locale.ROOT))) {
                    wanted.add(new Ward(owner.getUUID(), place.name(), place.pos(), radius, color));
                }
            }
            WardStore store = wanted.isEmpty() ? WardStore.peek(level) : WardStore.get(level);
            if (store == null) {
                continue;
            }
            boolean changed = store.retain(owner.getUUID(), ward -> wanted.stream().anyMatch(w -> w.is(ward.owner(), ward.name()))) > 0;
            for (Ward ward : wanted) {
                changed |= store.put(ward);
            }
            if (changed) {
                send(level);
            }
        }
    }

    /** Takes away every ward of this owner, in every level (an operator's tool); returns how many went. */
    public static int removeAll(MinecraftServer server, UUID owner) {
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            WardStore store = WardStore.peek(level);
            if (store != null) {
                int gone = store.retain(owner, ward -> false);
                if (gone > 0) {
                    removed += gone;
                    send(level);
                }
            }
        }
        return removed;
    }

    /** How many wards (awake or asleep) the owner has in every level together. */
    public static int count(MinecraftServer server, UUID owner) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            WardStore store = WardStore.peek(level);
            if (store != null) {
                for (Ward ward : store.wards()) {
                    count += ward.owner().equals(owner) ? 1 : 0;
                }
            }
        }
        return count;
    }

    // ---- the cooldown of raising one ----

    /** Ticks until this player may raise (or move) a ward again. */
    public static long cooldown(ServerPlayer player) {
        return Math.max(0L, READY.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime());
    }

    public static void startCooldown(ServerPlayer player) {
        READY.put(player.getUUID(), player.level().getGameTime() + VishantiConfig.WARD_COOLDOWN.get());
    }

    public static void clear() {
        READY.clear();
    }

    // ---- the clients ----

    /** Tells everyone in the level which wards stand in it. */
    public static void send(ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, payload(level));
    }

    /** Tells one player which wards stand in the level they are in. */
    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload(player.serverLevel()));
    }

    public static WardSyncPayload payload(ServerLevel level) {
        WardStore store = WardStore.peek(level);
        List<WardSyncPayload.Dome> domes = new ArrayList<>();
        if (store != null) {
            for (Ward ward : store.wards()) {
                if (ward.awake() && domes.size() < WardSyncPayload.MAX_DOMES) {
                    domes.add(new WardSyncPayload.Dome(ward.center().x, ward.center().y, ward.center().z, ward.radius(), ward.color()));
                }
            }
        }
        return new WardSyncPayload(level.dimension(), domes);
    }

    private WardManager() {
    }
}
