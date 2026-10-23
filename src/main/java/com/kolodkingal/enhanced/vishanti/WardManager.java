package com.kolodkingal.enhanced.vishanti;

import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.VishantiConfig;
import com.kolodkingal.enhanced.network.WardSyncPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
 * spawn category MONSTER), no boss and no boss's minion (3.3) can be inside it. New ones are refused where it is (the spawn
 * placement, and the finalizing of any spawn), and every {@code wardCheckTicks} ticks the ward looks, with one spatial lookup
 * of the creatures in the box round it, for monsters that are inside: one at the edge is thrown back out, one deeper in (a
 * monster that was there when the ward rose, or got in by a portal) is set down just outside. Not touched: what a player keeps
 * (someone's pet, a named or leashed creature), a rider, a Zombie Villager being cured, the tag {@link ModTags#WARD_IMMUNE}
 * (empty since 3.3: a datapack's way to let a kind in), and anything in a boss's arena (a ward cannot reach into one). What
 * monsters and bosses launch at the ward, and dark magic, stop at its edge ({@link com.kolodkingal.enhanced.protect.Shelters});
 * the powers of players pass. Nothing here changes a block, so a ward is no matter for a claim.
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

    /** Whether a ward keeps this kind of creature out: a monster, a boss or a boss's minion (3.3: every boss too). */
    public static boolean keepsOut(EntityType<?> type) {
        return (type.getCategory() == MobCategory.MONSTER || type.is(ModTags.BOSSES) || type.is(ModTags.BOSS_MINIONS))
                && !type.is(ModTags.WARD_IMMUNE);
    }

    /**
     * Whether this one is let through although the ward keeps its kind out: a pet, a rider, one the player keeps (it has a name,
     * or is on a leash) and a Zombie Villager that is being cured (it would be thrown out of the trading hall it is cured in). A
     * boss, or a minion a boss fight brought, is never let through (3.3), whatever it is called.
     */
    public static boolean exempt(Mob mob) {
        if (mob.getType().is(ModTags.WARD_IMMUNE)) {
            return true;
        }
        boolean boss = mob.getType().is(ModTags.BOSSES) || BossFights.isMember(mob) || BossFights.fightOf(mob) != null;
        if (boss) {
            return false;
        }
        if (mob.isPassenger() || mob.isVehicle() || mob.hasCustomName() || mob.isLeashed()) {
            return true;
        }
        if (mob instanceof ZombieVillager villager && villager.isConverting()) {
            return true;
        }
        return mob instanceof OwnableEntity pet && pet.getOwnerUUID() != null;
    }

    // ---- the lookups ----

    /**
     * Whether wards work at all: the book and its wards are on ({@code vishanti.enabled}, {@code wardsEnabled}). Turning them off
     * silences every ward in the world, those of owners who are offline too.
     */
    public static boolean wardsOn() {
        return VishantiConfig.ENABLED.get() && VishantiConfig.WARDS_ENABLED.get();
    }

    /** The ward that protects this point of the level, or null. */
    @Nullable
    public static Ward wardAt(ServerLevel level, Vec3 point) {
        WardStore store = WardStore.peek(level);
        return store == null || store.isEmpty() || !wardsOn() ? null : store.at(point);
    }

    /** Whether a ward protects this point of the level. */
    public static boolean protects(ServerLevel level, Vec3 point) {
        return wardAt(level, point) != null;
    }

    /** Whether a monster of this type may not spawn at this point (a ward protects it, and wards stop spawns). */
    public static boolean forbidsSpawn(ServerLevel level, EntityType<?> type, Vec3 point) {
        // The cheap questions first: this is asked for every spawn attempt of the server.
        if (type.getCategory() != MobCategory.MONSTER && !type.is(ModTags.BOSSES) && !type.is(ModTags.BOSS_MINIONS)) {
            return false;
        }
        WardStore store = WardStore.peek(level);
        return store != null && !store.isEmpty() && wardsOn() && VishantiConfig.WARD_BLOCKS_SPAWNS.get() && keepsOut(type) && store.at(point) != null;
    }

    // ---- the look every few ticks ----

    /** Once a tick for every level: each awake ward whose turn it is looks at what is inside it. */
    public static void tickLevel(ServerLevel level) {
        WardStore store = WardStore.peek(level);
        if (store == null || store.isEmpty() || !wardsOn()) {
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
        if (!playerNear(level, ward)) {
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

    /**
     * Whether a player is close enough for the ward to matter (within its radius and a margin): nobody near, nothing in the ward
     * is ticking, and a look would be work for nothing. (Each monster is also asked whether it is ticking, so a far corner of a
     * big ward is looked at when a player is there, whatever the centre's chunk does.)
     */
    private static boolean playerNear(ServerLevel level, Ward ward) {
        double reach = ward.radius() + 96.0;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(ward.center()) <= reach * reach) {
                return true;
            }
        }
        return false;
    }

    private static boolean intrudes(ServerLevel level, Ward ward, Mob mob) {
        return mob.isAlive() && level.isPositionEntityTicking(mob.blockPosition()) && keepsOut(mob.getType()) && !exempt(mob)
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
        boolean ground = needsGround(mob);
        boolean water = mob instanceof Guardian;
        for (int i = 0; i <= 2 * SCAN; i++) {
            // 0, +1, -1, +2, -2 ...: the nearest height first.
            int step = (i + 1) / 2 * (i % 2 == 1 ? 1 : -1);
            double y = mob.getY() + step;
            AABB body = mob.getDimensions(mob.getPose()).makeBoundingBox(x, y, z);
            if (center.distanceToSqr(body.getCenter()) < need * need || !level.noCollision(mob, body) || hazardous(level, body)) {
                continue;
            }
            if (ground) {
                BlockPos below = BlockPos.containing(x, y - 0.05, z);
                if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    continue;
                }
            }
            if (water && !level.getFluidState(BlockPos.containing(x, y + 0.3, z)).is(FluidTags.WATER)) {
                continue;
            }
            mob.moveTo(x, y, z, mob.getYRot(), mob.getXRot());
            mob.setDeltaMovement(Vec3.ZERO);
            mob.fallDistance = 0.0F;
            return true;
        }
        return false;
    }

    /** Whether this mob walks on the ground (a flyer or a swimmer is not set down on a floor). */
    public static boolean needsGround(Mob mob) {
        return !(mob.isNoGravity() || mob instanceof FlyingMob || mob instanceof Vex || mob instanceof Blaze || mob instanceof Guardian);
    }

    /**
     * Whether something in or just under this body would hurt whoever stands there: lava, fire, magma, a cactus, a berry bush, a
     * wither rose. A monster is never set down on one.
     */
    public static boolean hazardous(ServerLevel level, AABB body) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x0 = Mth.floor(body.minX + 0.01);
        int x1 = Mth.floor(body.maxX - 0.01);
        int y0 = Mth.floor(body.minY + 0.01) - 1;
        int y1 = Mth.floor(body.maxY - 0.01);
        int z0 = Mth.floor(body.minZ + 0.01);
        int z1 = Mth.floor(body.maxZ - 0.01);
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH)
                            || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                            || state.getFluidState().is(FluidTags.LAVA)) {
                        return true;
                    }
                }
            }
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

    /** How far (blocks) beyond a ward's edge a client is told of it: the dome is drawn from 64, and a margin for walking. */
    public static final double TELL_MARGIN = 256.0;

    /** Tells everyone in the level which wards stand near them (and their own, wherever). */
    public static void send(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            Net.sendToPlayer(player, payload(level, player));
        }
    }

    /** Tells one player which wards stand near them in the level they are in. */
    public static void send(ServerPlayer player) {
        Net.sendToPlayer(player, payload(player.serverLevel(), player));
    }

    /** Every awake ward of the level (what the operators see; no client gets this much). */
    public static WardSyncPayload payload(ServerLevel level) {
        return payload(level, null);
    }

    /**
     * The awake wards of the level that {@code viewer} is told of: those within {@link #TELL_MARGIN} blocks of their edge, and the
     * viewer's own wherever they are (so a base location far from anyone is not handed to every client).
     */
    public static WardSyncPayload payload(ServerLevel level, @Nullable ServerPlayer viewer) {
        WardStore store = WardStore.peek(level);
        List<WardSyncPayload.Dome> domes = new ArrayList<>();
        if (store != null) {
            for (Ward ward : store.wards()) {
                if (!ward.awake() || domes.size() >= WardSyncPayload.MAX_DOMES) {
                    continue;
                }
                if (viewer != null && !ward.owner().equals(viewer.getUUID())) {
                    double reach = ward.radius() + TELL_MARGIN;
                    if (viewer.distanceToSqr(ward.center()) > reach * reach) {
                        continue;
                    }
                }
                domes.add(new WardSyncPayload.Dome(ward.center().x, ward.center().y, ward.center().z, ward.radius(), ward.color()));
            }
        }
        return new WardSyncPayload(level.dimension(), domes);
    }

    private WardManager() {
    }
}
