package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the island carries while it moves, and how it is carried. A carried entity is <em>locked</em>: it has no
 * gravity (and a mob no will of its own), a player cannot walk off ({@link Live#LIFTED} stops the keys on their own
 * client, as for a player Mjolnir pins), nothing hurts it, and it does not fall or fly: every tick it is moved by what
 * the island moved.
 *
 * <ul>
 *     <li><b>A player</b> is given a velocity of the island's speed (a motion packet; their client's own physics
 *     carries them, with no gravity) and, if they have drifted a long way from where the lift says they should be (a
 *     lag spike), put back there. Every tick their fall distance and vanilla's "floating too long" counter are reset: a
 *     survival player with no permission to fly who hangs in the air for 80 ticks is kicked ("Flying is not
 *     enabled on this server"), and the rise lasts 160.</li>
 *     <li><b>Anything else</b> is moved by the same amount, with its position.</li>
 * </ul>
 *
 * <p>What an entity was before (its gravity, its will) is written in its persistent data and given back when it is
 * released, so that a crash or a logout in the middle cannot leave it floating: {@link #release} is also what a login
 * does for a player who was saved carried.
 */
final class SokoviaCarry {
    /** The persistent-data mark of a carried entity: the id of the fight that carries it. */
    static final String MARK = Enhanced.MODID + ":lifted";
    private static final String GRAVITY = Enhanced.MODID + ":lifted_no_gravity";
    private static final String AI = Enhanced.MODID + ":lifted_no_ai";
    /** How far (blocks) a player may be from where the lift says they are before they are put back. */
    static final double DRIFT = 6.0;
    /** How much smaller than itself an entity is when it is asked whether it fits (rounding is not a collision). */
    private static final double FIT_MARGIN = 1.0E-3;
    private static final Set<RelativeMovement> KEEP_LOOKING = EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT);

    /** One carried entity: where it was when it was taken, and how much of the island's move it has had. */
    static final class Rider {
        final UUID id;
        final boolean player;
        /** Where it was when it was taken. */
        Vec3 base;
        /** The island's move at that moment. */
        double baseMoved;
        /** The island's move it has been given so far. */
        double applied;

        Rider(UUID id, boolean player, Vec3 base, double moved) {
            this.id = id;
            this.player = player;
            this.base = base;
            this.baseMoved = moved;
            this.applied = moved;
        }

        /** Where the lift says it is now, if the island has moved {@code moved}. */
        Vec3 expected(double moved) {
            return base.add(0.0, moved - baseMoved, 0.0);
        }
    }

    private final ServerLevel level;
    private final UUID fight;
    private final Map<UUID, Rider> riders = new LinkedHashMap<>();
    private double moved;

    SokoviaCarry(ServerLevel level, UUID fight) {
        this.level = level;
        this.fight = fight;
    }

    // ---- who ----

    boolean contains(UUID id) {
        return riders.containsKey(id);
    }

    Collection<Rider> riders() {
        return riders.values();
    }

    int size() {
        return riders.size();
    }

    @Nullable
    Rider rider(UUID id) {
        return riders.get(id);
    }

    /** The entities carried that are in the world now. */
    List<Entity> entities() {
        List<Entity> found = new ArrayList<>();
        for (Rider rider : riders.values()) {
            Entity entity = resolve(rider);
            if (entity != null) {
                found.add(entity);
            }
        }
        return found;
    }

    /** The island has moved this far since the move began. */
    double moved() {
        return moved;
    }

    /** A new move begins: nothing has moved yet. */
    void restart() {
        moved = 0.0;
        for (Rider rider : riders.values()) {
            rider.baseMoved = 0.0;
            rider.applied = 0.0;
        }
    }

    // ---- taking and letting go ----

    /**
     * Locks the entity and carries it from here on. A player is taken off any steed and out of the glide, and whatever
     * holds them (a Telekinesis grip, Mjolnir) lets go; a mob's will is switched off unless {@code keepAi} (the fight's
     * own creatures steer themselves).
     */
    void bind(Entity entity, boolean keepAi) {
        if (riders.containsKey(entity.getUUID())) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        data.putUUID(MARK, fight);
        data.putBoolean(GRAVITY, entity.isNoGravity());
        entity.setNoGravity(true);
        if (entity instanceof Mob mob && !keepAi) {
            data.putBoolean(AI, mob.isNoAi());
            mob.setNoAi(true);
        }
        letGoOf(entity);
        if (entity instanceof ServerPlayer player) {
            player.stopRiding();
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }
            Powers.get(player).set(Live.LIFTED, true);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        } else {
            entity.stopRiding();
            entity.setDeltaMovement(Vec3.ZERO);
        }
        riders.put(entity.getUUID(), new Rider(entity.getUUID(), entity instanceof ServerPlayer, entity.position(), moved));
    }

    /** Whatever holds the entity lets it go: a Telekinesis grip (theirs, or on them), Mjolnir lying on them. */
    private void letGoOf(Entity entity) {
        for (ServerPlayer holder : level.players()) {
            if (Telekinesis.isHolding(holder) && (holder == entity || level.getEntity(Powers.get(holder).heldEntityId()) == entity)) {
                Telekinesis.release(holder, true);
            }
        }
        var pin = MjolnirPin.pinOf(entity);
        if (pin != null) {
            MjolnirPin.release(level, pin);
        }
    }

    /**
     * Gives the entity its gravity and its will back and unlocks it. Safe on an entity that was never carried, or whose
     * carrying was cut short (a crash): that is what a login does.
     */
    static void release(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.hasUUID(MARK)) {
            entity.setNoGravity(data.getBoolean(GRAVITY));
            if (data.contains(AI) && entity instanceof Mob mob) {
                mob.setNoAi(data.getBoolean(AI));
            }
            data.remove(MARK);
            data.remove(GRAVITY);
            data.remove(AI);
        }
        entity.resetFallDistance();
        if (entity instanceof ServerPlayer player) {
            Powers.get(player).set(Live.LIFTED, false);
            player.connection.aboveGroundTickCount = 0;
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        } else {
            entity.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** The id of the fight that carries this entity, or null. */
    @Nullable
    static UUID markOf(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID(MARK) ? data.getUUID(MARK) : null;
    }

    /** Lets one go (it is no longer carried), wherever it is. */
    void unbind(UUID id) {
        Rider rider = riders.remove(id);
        if (rider != null) {
            Entity entity = resolve(rider);
            if (entity != null) {
                release(entity);
            }
        }
    }

    /**
     * Lets everything go. Forget them first if they must stay where they are; {@link #snapAll} puts them where the lift
     * says.
     */
    void releaseAll() {
        for (Rider rider : List.copyOf(riders.values())) {
            unbind(rider.id);
        }
        riders.clear();
    }

    /** Forgets a rider without touching the entity (it logged out and the login will see to it). */
    void forget(UUID id) {
        riders.remove(id);
    }

    // ---- every tick ----

    /**
     * The island has moved {@code total} blocks since the move began: carries everything the rest of the way. Anything
     * that is gone (dead, removed, in another dimension) is let go.
     */
    void move(double total) {
        this.moved = total;
        for (Iterator<Rider> it = riders.values().iterator(); it.hasNext(); ) {
            Rider rider = it.next();
            Entity entity = resolve(rider);
            if (entity == null || !entity.isAlive() || entity.level() != level || (entity instanceof ServerPlayer p && p.isSpectator())) {
                it.remove();
                if (entity != null) {
                    release(entity);
                }
                continue;
            }
            double delta = total - rider.applied;
            rider.applied = total;
            if (entity instanceof ServerPlayer player) {
                carryPlayer(player, rider, delta);
            } else {
                carryOther(entity, delta);
            }
        }
    }

    private void carryPlayer(ServerPlayer player, Rider rider, double delta) {
        player.connection.aboveGroundTickCount = 0;
        player.resetFallDistance();
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        Vec3 expected = rider.expected(moved);
        if (player.position().distanceTo(expected) > DRIFT + 6.0 * Math.abs(delta)) {
            teleport(player, expected);
        }
        player.setDeltaMovement(0.0, delta, 0.0);
        player.hurtMarked = true;
    }

    private static void carryOther(Entity entity, double delta) {
        if (delta != 0.0) {
            entity.setPos(entity.getX(), entity.getY() + delta, entity.getZ());
        }
        entity.resetFallDistance();
        if (!(entity instanceof LivingEntity living) || !isFightCreature(living)) {
            entity.setDeltaMovement(Vec3.ZERO);
        }
        entity.hasImpulse = true;
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    /** The boss and his Sentries steer themselves, and keep their own velocity. */
    private static boolean isFightCreature(LivingEntity living) {
        return living instanceof UltronEntity || living instanceof UltronSentryEntity;
    }

    /** Puts every carried entity where the lift says it is (at the end of a move), in free space. */
    void snapAll() {
        for (Rider rider : riders.values()) {
            Entity entity = resolve(rider);
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            Vec3 expected = rider.expected(moved);
            Vec3 spot = freeSpot(entity, expected);
            if (entity.position().distanceToSqr(spot) > 1.0E-6) {
                teleport(entity, spot);
            }
        }
    }

    // ---- places ----

    @Nullable
    private Entity resolve(Rider rider) {
        return rider.player ? level.getServer().getPlayerList().getPlayer(rider.id) : level.getEntity(rider.id);
    }

    /** Moves the entity to a point (a player's client is told; where they look is left alone). */
    static void teleport(Entity entity, Vec3 to) {
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(to.x, to.y, to.z, player.getYRot(), player.getXRot(), KEEP_LOOKING);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        } else {
            entity.setPos(to);
        }
    }

    /**
     * The nearest place to {@code spot} where the entity's body fits: the spot, else up to three blocks above it (an
     * entity must never be left inside a block), else the spot.
     */
    static Vec3 freeSpot(Entity entity, Vec3 spot) {
        for (int up = 0; up <= 3; up++) {
            Vec3 candidate = spot.add(0.0, up, 0.0);
            if (fits(entity, candidate)) {
                return candidate;
            }
        }
        return spot;
    }

    /**
     * Whether the entity's body fits at {@code feet}: it touches no block (standing on one does not count, and neither
     * does a rounding error of a billionth of a block).
     */
    static boolean fits(Entity entity, Vec3 feet) {
        AABB box = entity.getBoundingBox().move(feet.subtract(entity.position())).deflate(FIT_MARGIN);
        return entity.level().getWorldBorder().isWithinBounds(box) && entity.level().noCollision(entity, box);
    }

    /** Whether the entity's body is inside a block where it is. */
    static boolean isStuck(Entity entity) {
        return !entity.level().noCollision(entity, entity.getBoundingBox().deflate(FIT_MARGIN));
    }
}
