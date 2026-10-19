package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.StrangeFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.registry.ModTicketTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The pairs of Sling-Ring portals (3.6). Doctor Strange's N sweeps a circle of sparks: a ring opens two blocks before
 * him and its twin where he looks (up to {@code portalRange}; at the sky, that far along his look, on the ground); the
 * Sling Ring opens a pair to a saved place ({@link SlingRingActions}), in another dimension too. A pair lives {@code
 * portalSeconds}; one pair per Strange (a new one closes the old).
 *
 * <p>Whatever crosses a ring's plane within its radius (players, creatures, items, arrows) comes out of the twin at the
 * same place in the ring, turned by the twins' difference of facing, with its speed kept, and cannot go back for {@link
 * #NO_RETURN_TICKS}. Every passer is judged by {@code HexManager.onTeleport} (a Hex, a boss's arena, Mjolnir's pin);
 * bosses, their creatures and the astral bodies never pass. A ring never takes anyone across dimensions except a pair the
 * Sling Ring opened to a saved place, and the gate of Asgard refuses that as it refuses every way but the Bifrost. The
 * far ring's chunk is held loaded ({@code ENHANCED_SLING}) while the pair lives.
 */
public final class SlingPortals {
    /** How long a passer cannot go back through a ring (ticks). */
    public static final int NO_RETURN_TICKS = 10;
    /** How far before him the near ring stands. */
    public static final double NEAR = 2.0;
    /** The radius of the far ring's chunk ticket. */
    static final int TICKET_RADIUS = 2;

    /** A pair of rings: whose, where each is, its ticket, until when. */
    public record Pair(UUID id, UUID owner, UUID near, ResourceKey<Level> nearDimension, UUID far,
                       ResourceKey<Level> farDimension, ChunkPos ticket, long until) {
    }

    private static final Map<UUID, Pair> PAIRS = new HashMap<>();
    private static final Map<UUID, UUID> BY_OWNER = new HashMap<>();
    /** Passer -> the game time it may pass a ring again. */
    private static final Map<UUID, Long> NO_RETURN = new HashMap<>();

    public static int range() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.PORTAL_RANGE.get(), 64);
    }

    public static int ticks() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.PORTAL_SECONDS.get(), 8) * 20;
    }

    public static int cooldown() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.PORTAL_COOLDOWN.get(), 100);
    }

    // ---- queries ----

    /** This Strange's pair, or null. */
    @Nullable
    public static Pair of(UUID owner) {
        UUID id = BY_OWNER.get(owner);
        return id == null ? null : PAIRS.get(id);
    }

    /** How many pairs stand (and so how many far chunks are held). */
    public static int count() {
        return PAIRS.size();
    }

    /** Whether the pair's far chunk is still held. */
    public static boolean holdsTicket(UUID pair) {
        return PAIRS.containsKey(pair);
    }

    // ---- where the rings stand ----

    /** The near ring's feet: {@link #NEAR} blocks before him along his look (flat). */
    public static Vec3 nearFeet(Player player) {
        Vec3 dir = SlingPortalEntity.normal(player.getYRot());
        return player.position().add(dir.scale(NEAR));
    }

    /**
     * The far ring's feet for a look: on top of the block he looks at, or in front of a wall he looks at, on its ground;
     * at the sky, {@code range} blocks along his look, on the ground below. Null when there is no ground.
     */
    @Nullable
    public static Vec3 farFeet(ServerPlayer player, double range) {
        ServerLevel level = player.serverLevel();
        BlockHitResult hit = Raycasts.block(player, range, 1.0F);
        Vec3 point;
        if (hit.getType() != HitResult.Type.MISS) {
            Direction face = hit.getDirection();
            point = face == Direction.UP ? hit.getLocation().add(0, 0.05, 0)
                    : hit.getLocation().add(face.getStepX() * 1.0, face.getStepY() * 1.0, face.getStepZ() * 1.0);
        } else {
            point = player.getEyePosition().add(player.getLookAngle().scale(range));
        }
        return ground(level, point, (int) Math.max(8, range));
    }

    /** The first ground at or below {@code point} (within {@code depth}): the feet of something standing there. */
    @Nullable
    static Vec3 ground(ServerLevel level, Vec3 point, int depth) {
        BlockPos at = BlockPos.containing(point);
        level.getChunkAt(at);
        for (int dy = 0; dy <= depth && at.getY() - dy > level.getMinBuildHeight(); dy++) {
            BlockPos below = at.below(dy + 1);
            BlockPos here = at.below(dy);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                    && level.getBlockState(here).getCollisionShape(level, here).isEmpty()) {
                return new Vec3(point.x, below.getY() + level.getBlockState(below).getCollisionShape(level, below)
                        .max(Direction.Axis.Y), point.z);
            }
        }
        return null;
    }

    // ---- opening ----

    /** N: a pair here and where he looks. */
    static void openByKey(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.STRANGE_PORTAL, now) || StrangeAbilities.refuseHungry(player)) {
            return;
        }
        Vec3 near = nearFeet(player);
        if (!roomFor(level, near)) {
            StrangeAbilities.say(player, "message.enhanced.strange.portal_no_room");
            return;
        }
        Vec3 far = farFeet(player, range());
        if (far == null) {
            StrangeAbilities.say(player, "message.enhanced.strange.portal_no_ground");
            return;
        }
        open(player, level, near, player.getYRot(), level, far, player.getYRot());
        data.startCooldown(HeroAbility.STRANGE_PORTAL, now, cooldown());
        StrangeAbilities.spend(player, data, 1.5F);
    }

    /** Whether a ring has room to stand at {@code feet}: its centre is not inside a block. */
    static boolean roomFor(ServerLevel level, Vec3 feet) {
        BlockPos c = BlockPos.containing(feet.add(0, SlingPortalEntity.CENTER_UP, 0));
        return level.getBlockState(c).getCollisionShape(level, c).isEmpty();
    }

    /**
     * Opens a pair for {@code owner}: the near ring at {@code nearFeet} in {@code nearLevel} facing {@code nearYaw}, the
     * far one at {@code farFeet} in {@code farLevel} facing {@code farYaw}. His old pair closes first. Returns the pair.
     */
    public static Pair open(ServerPlayer owner, ServerLevel nearLevel, Vec3 nearFeet, float nearYaw, ServerLevel farLevel,
                            Vec3 farFeet, float farYaw) {
        closeOwnedBy(owner.server, owner.getUUID());
        int life = ticks();
        UUID id = UUID.randomUUID();
        SlingPortalEntity near = ring(nearLevel, nearFeet, nearYaw, life, id);
        SlingPortalEntity far = ring(farLevel, farFeet, farYaw, life, id);
        ChunkPos chunk = new ChunkPos(BlockPos.containing(farFeet));
        farLevel.getChunkSource().addRegionTicket(ModTicketTypes.ENHANCED_SLING, chunk, TICKET_RADIUS, id);
        Pair pair = new Pair(id, owner.getUUID(), near.getUUID(), nearLevel.dimension(), far.getUUID(),
                farLevel.dimension(), chunk, nearLevel.getGameTime() + life);
        PAIRS.put(id, pair);
        BY_OWNER.put(owner.getUUID(), id);
        nearLevel.addFreshEntity(near);
        farLevel.addFreshEntity(far);
        nearLevel.playSound(null, nearFeet.x, nearFeet.y + 1, nearFeet.z, ModSounds.STRANGE_PORTAL_OPEN.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        farLevel.playSound(null, farFeet.x, farFeet.y + 1, farFeet.z, ModSounds.STRANGE_PORTAL_OPEN.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        StrangeSignals.near(nearLevel, nearFeet, StrangeFxPayload.PORTAL_OPEN, owner,
                List.of(near.center(), far.center()), life);
        if (farLevel != nearLevel || farFeet.distanceTo(nearFeet) > StrangeSignals.RANGE) {
            StrangeSignals.near(farLevel, farFeet, StrangeFxPayload.PORTAL_OPEN, null, List.of(far.center()), life);
        }
        return pair;
    }

    private static SlingPortalEntity ring(ServerLevel level, Vec3 feet, float yaw, int life, UUID pair) {
        SlingPortalEntity ring = new SlingPortalEntity(ModEntities.SLING_PORTAL.get(), level);
        ring.moveTo(feet.x, feet.y, feet.z, yaw, 0.0F);
        ring.setFacing(yaw);
        ring.setLife(life);
        ring.pair = pair;
        return ring;
    }

    // ---- closing ----

    /** Closes the pair of this Strange, if he has one. */
    public static void closeOwnedBy(MinecraftServer server, UUID owner) {
        UUID id = BY_OWNER.get(owner);
        if (id != null) {
            close(server, id);
        }
    }

    /** Closes a pair: both rings go, the far chunk is let go of, the sparks close. */
    public static void close(MinecraftServer server, UUID id) {
        Pair pair = PAIRS.remove(id);
        if (pair == null) {
            return;
        }
        BY_OWNER.remove(pair.owner(), id);
        List<Vec3> where = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ServerLevel level = server.getLevel(i == 0 ? pair.nearDimension() : pair.farDimension());
            if (level == null) {
                continue;
            }
            Entity ring = level.getEntity(i == 0 ? pair.near() : pair.far());
            if (ring instanceof SlingPortalEntity portal) {
                where.add(portal.center());
                level.playSound(null, portal.getX(), portal.getY() + 1, portal.getZ(),
                        ModSounds.STRANGE_PORTAL_CLOSE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                StrangeSignals.near(level, portal.center(), StrangeFxPayload.PORTAL_CLOSE, null,
                        List.of(portal.center()), 0);
                portal.discard();
            }
            if (i == 1) {
                level.getChunkSource().removeRegionTicket(ModTicketTypes.ENHANCED_SLING, pair.ticket(), TICKET_RADIUS,
                        pair.id());
            }
        }
    }

    /** Every server tick: pairs whose time is up close. */
    public static void serverTick(MinecraftServer server) {
        if (PAIRS.isEmpty()) {
            NO_RETURN.clear();
            return;
        }
        long now = server.overworld().getGameTime();
        List<UUID> over = new ArrayList<>();
        for (Pair pair : PAIRS.values()) {
            if (now >= pair.until()) {
                over.add(pair.id());
            }
        }
        for (UUID id : over) {
            close(server, id);
        }
        NO_RETURN.values().removeIf(until -> until < now);
    }

    // ---- passing ----

    /** A ring's tick (server): it goes when its pair is gone; whatever crossed its plane since last tick passes. */
    static void ringTick(ServerLevel level, SlingPortalEntity ring) {
        Pair pair = ring.pair == null ? null : PAIRS.get(ring.pair);
        if (pair == null) {
            ring.discard();
            return;
        }
        boolean isNear = ring.getUUID().equals(pair.near());
        ServerLevel twinLevel = level.getServer().getLevel(isNear ? pair.farDimension() : pair.nearDimension());
        Entity twin = twinLevel == null ? null : twinLevel.getEntity(isNear ? pair.far() : pair.near());
        if (!(twin instanceof SlingPortalEntity other)) {
            return; // (its twin is not loaded yet)
        }
        Vec3 center = ring.center();
        Vec3 normal = ring.normal();
        long now = level.getGameTime();
        Map<Integer, Vec3> seen = new HashMap<>();
        AABB box = new AABB(center, center).inflate(SlingPortalEntity.RADIUS + 4.0);
        for (Entity e : level.getEntities(ring, box, e -> mayPass(e))) {
            Vec3 c = e.getBoundingBox().getCenter();
            Vec3 before = ring.last.get(e.getId());
            seen.put(e.getId(), c);
            if (before == null || now < NO_RETURN.getOrDefault(e.getUUID(), Long.MIN_VALUE)) {
                continue;
            }
            Vec3 hit = crossing(center, normal, before, c);
            if (hit != null) {
                Entity arrived = pass(e, ring, other, twinLevel);
                if (arrived != null) {
                    NO_RETURN.put(arrived.getUUID(), now + NO_RETURN_TICKS);
                    other.last.remove(arrived.getId());
                    seen.remove(e.getId());
                }
            }
        }
        ring.last.clear();
        ring.last.putAll(seen);
    }

    /**
     * Where a move from {@code before} to {@code after} crosses the plane of a ring at {@code center} facing {@code
     * normal}, within its radius (either way through); null if it does not.
     */
    @Nullable
    public static Vec3 crossing(Vec3 center, Vec3 normal, Vec3 before, Vec3 after) {
        double s0 = before.subtract(center).dot(normal);
        double s1 = after.subtract(center).dot(normal);
        if (s0 == 0.0 && s1 == 0.0 || s0 * s1 > 0.0 || s0 == s1) {
            return null;
        }
        double t = s0 / (s0 - s1);
        Vec3 at = before.add(after.subtract(before).scale(t));
        Vec3 off = at.subtract(center);
        Vec3 lateral = off.subtract(normal.scale(off.dot(normal)));
        return lateral.lengthSqr() <= SlingPortalEntity.RADIUS * SlingPortalEntity.RADIUS ? at : null;
    }

    /** Who may pass a ring: no ring, no boss nor a boss's creature, no astral body, nobody riding or a spectator. */
    static boolean mayPass(Entity e) {
        return !(e instanceof SlingPortalEntity) && !(e instanceof AstralBodyEntity) && e.isAlive() && !e.isSpectator()
                && !e.isPassenger() && !e.getType().is(ModTags.BOSSES) && !e.getType().is(ModTags.BOSS_MINIONS)
                && !BossFights.isFightEntity(e);
    }

    /** Turns a vector round the vertical by {@code degrees} of yaw (the game's sense). Pure. */
    public static Vec3 turn(Vec3 v, double degrees) {
        double r = Math.toRadians(degrees);
        double cos = Math.cos(r);
        double sin = Math.sin(r);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /**
     * {@code e} passes {@code from} into {@code to} (in {@code toLevel}): the same place in the ring, turned by the
     * twins' difference of facing, its speed kept and turned. Judged by {@code HexManager.onTeleport} first. Returns
     * what arrived (a new entity across dimensions), or null if it was refused.
     */
    @Nullable
    static Entity pass(Entity e, SlingPortalEntity from, SlingPortalEntity to, ServerLevel toLevel) {
        float turnBy = to.facing() - from.facing();
        Vec3 c = e.getBoundingBox().getCenter();
        Vec3 offset = turn(c.subtract(from.center()), turnBy);
        Vec3 arrive = to.center().add(offset);
        Vec3 feet = new Vec3(arrive.x, arrive.y - e.getBbHeight() * 0.5, arrive.z);
        feet = fit(toLevel, e, feet);
        if (feet == null || !HexManager.onTeleport(toLevel, e, feet)) {
            e.setDeltaMovement(e.getDeltaMovement().scale(-0.3));
            e.hurtMarked = true;
            return null;
        }
        Vec3 velocity = turn(e.getDeltaMovement(), turnBy);
        float yaw = e.getYRot() + turnBy;
        Entity arrived = e;
        if (toLevel == e.level()) {
            if (e instanceof ServerPlayer player) {
                player.teleportTo(toLevel, feet.x, feet.y, feet.z, yaw, player.getXRot());
            } else {
                e.teleportTo(feet.x, feet.y, feet.z);
                e.setYRot(yaw);
            }
        } else if (e instanceof ServerPlayer player) {
            player.teleportTo(toLevel, feet.x, feet.y, feet.z, yaw, player.getXRot());
            if (player.level() != toLevel) {
                return null; // (a gate refused the journey)
            }
        } else {
            arrived = e.changeDimension(new DimensionTransition(toLevel, feet, velocity, yaw, e.getXRot(),
                    DimensionTransition.DO_NOTHING));
            if (arrived == null) {
                return null;
            }
        }
        arrived.setDeltaMovement(velocity);
        arrived.hurtMarked = true;
        arrived.resetFallDistance();
        toLevel.playSound(null, feet.x, feet.y, feet.z, ModSounds.STRANGE_PORTAL_PASS.get(), SoundSource.PLAYERS, 0.8F,
                1.0F);
        return arrived;
    }

    /** {@code feet}, or a little above it if the body does not fit there (the far ground may be uneven); null if none. */
    @Nullable
    static Vec3 fit(ServerLevel level, Entity e, Vec3 feet) {
        level.getChunkAt(BlockPos.containing(feet));
        for (double up = 0.0; up <= 1.5; up += 0.5) {
            Vec3 at = feet.add(0, up, 0);
            if (level.noCollision(e, e.getBoundingBox().move(at.subtract(e.position())))) {
                return at;
            }
        }
        return Teleports.fits(e, feet) ? feet : null;
    }

    /** Server stopped: every pair forgotten (the rings are never saved; the tickets go with the levels). */
    static void clear() {
        PAIRS.clear();
        BY_OWNER.clear();
        NO_RETURN.clear();
    }

    /** The pairs (a test may look). */
    public static Iterator<Pair> pairs() {
        return PAIRS.values().iterator();
    }

    private SlingPortals() {
    }
}
