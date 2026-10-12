package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The sand that moves on the ground, as server objects: the waves, the rings of spikes, the tornadoes and the great
 * rings of the Cairo Collapse. The boss's attacks and the hero's Sand Wave and Sandstorm are made of them. They are drawn
 * by the clients from <em>one</em> payload sent when they start ({@link ApocalypseFxPayload}), so no packet is needed per
 * tick (the tornado, which drifts, is sent again every 4 ticks), and they never place or break a block.
 *
 * <p>Each hazard stores the entities it has hurt, so nothing is hit twice by one hazard, and belongs to its owner (it
 * ends when the owner is gone) and, for the boss's, to its fight (it ends with it). Who it may hurt is {@link
 * SandControl#canHurt}. They are ticked once per level tick by {@code ApocalypseEvents}.
 */
public final class SandHazards {
    private static final List<Hazard> HAZARDS = new ArrayList<>();
    private static int nextId;

    /**
     * The numbers of a wave: the boss's (width 4, height 1.1, length 30, speed 0.8, 10 damage) and the hero's (length
     * 18, speed 0.9, 8 damage) differ only in these.
     *
     * @param speed     blocks a tick
     * @param length    blocks it travels
     * @param width     blocks wide
     * @param height    blocks tall: a creature whose feet are higher over the ground is not hit
     * @param damage    the damage
     * @param slowTicks the length of the Slowness II it leaves
     * @param knockback the shove along the wave
     */
    public record WaveSpec(double speed, double length, double width, double height, float damage, int slowTicks, double knockback) {
    }

    /** One moving thing of sand. */
    public abstract static class Hazard {
        final ServerLevel level;
        final UUID ownerId;
        @Nullable
        final UUID fightId;
        final int id = ++nextId;
        int age;

        Hazard(ServerLevel level, LivingEntity owner, @Nullable UUID fightId) {
            this.level = level;
            this.ownerId = owner.getUUID();
            this.fightId = fightId;
        }

        /** Advances it a tick; false when it is over. */
        abstract boolean step(LivingEntity owner);

        public ServerLevel level() {
            return level;
        }

        public UUID ownerId() {
            return ownerId;
        }

        public int age() {
            return age;
        }

        public int id() {
            return id;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The wave

    /** A band of sand rolling along the ground. */
    public static final class Wave extends Hazard {
        private final Vec3 origin;
        private final Vec3 dir;
        private final Vec3 perp;
        private final WaveSpec spec;
        private final Vec3 centre;
        private final double bound;
        private final IntSet hit = new IntOpenHashSet();
        private double distance;
        private double lastGround;

        Wave(ServerLevel level, LivingEntity owner, @Nullable UUID fightId, Vec3 origin, Vec3 dir, WaveSpec spec, @Nullable Vec3 centre, double bound) {
            super(level, owner, fightId);
            this.origin = origin;
            this.dir = dir;
            this.perp = new Vec3(-dir.z, 0.0, dir.x);
            this.spec = spec;
            this.centre = centre == null ? origin : centre;
            this.bound = bound;
            this.lastGround = SandControl.groundAt(level, origin.x, origin.y + 1.0, origin.z);
        }

        /** How far along its way the front is. */
        public double distance() {
            return distance;
        }

        public Vec3 front() {
            return origin.add(dir.scale(distance));
        }

        public IntSet hit() {
            return hit;
        }

        @Override
        boolean step(LivingEntity owner) {
            double from = distance;
            if (from >= spec.length) {
                return false;
            }
            distance = Math.min(spec.length, distance + spec.speed);
            Vec3 front = origin.add(dir.scale(distance));
            // It breaks up where the ground rises or falls by more than two blocks, and at the edge of the arena or the world.
            boolean leftArena = bound > 0.0 && Math.hypot(front.x - centre.x, front.z - centre.z) > bound;
            if (leftArena || !level.getWorldBorder().isWithinBounds(front.x, front.z)) {
                SandControl.puff(level, new Vec3(front.x, lastGround + 0.5, front.z), 8, 0.4);
                return false;
            }
            double ground = SandControl.groundAt(level, front.x, lastGround + 2.0, front.z);
            if (Math.abs(ground - lastGround) > 2.0) {
                SandControl.puff(level, new Vec3(front.x, Math.max(ground, lastGround) + 0.5, front.z), 8, 0.4);
                return false;
            }
            lastGround = ground;
            Vec3 start = origin.add(dir.scale(from));
            AABB box = new AABB(start, front).inflate(spec.width * 0.5 + 1.0, spec.height + 2.0, spec.width * 0.5 + 1.0);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && !hit.contains(e.getId()))) {
                if (!SandControl.canHurt(owner, victim)) {
                    continue;
                }
                Vec3 rel = victim.position().subtract(origin);
                double along = rel.dot(dir);
                double lateral = Math.abs(rel.x * perp.x + rel.z * perp.z);
                double half = victim.getBbWidth() * 0.5;
                if (along < from - half || along > distance + half || lateral > spec.width * 0.5 + half) {
                    continue;
                }
                // A creature whose feet are higher than the wave is over it: a jump at the right moment, or flight.
                double groundHere = SandControl.groundAt(level, victim.getX(), victim.getY() + 1.0, victim.getZ());
                if (victim.getY() < groundHere - 0.5 || victim.getY() > groundHere + spec.height) {
                    continue;
                }
                hit.add(victim.getId());
                if (SandControl.hurt(level, owner, victim, spec.damage)) {
                    victim.knockback(spec.knockback, -dir.x, -dir.z);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.4, 0.0));
                    victim.hurtMarked = true;
                    if (spec.slowTicks > 0) {
                        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, spec.slowTicks, 1), owner);
                    }
                }
            }
            return true;
        }
    }

    /**
     * Sends a wave rolling from {@code origin} along {@code dir} (a unit vector on the ground plane).
     *
     * @param centre where the arena is, or null; the wave breaks up when its front is more than {@code bound} blocks from
     *               it ({@code bound} 0: no limit)
     * @param fight  the fight it belongs to, or null for a hero's
     */
    public static Wave wave(ServerLevel level, LivingEntity owner, @Nullable UUID fight, Vec3 origin, Vec3 dir, WaveSpec spec,
                            @Nullable Vec3 centre, double bound) {
        Wave wave = new Wave(level, owner, fight, origin, dir, spec, centre, bound);
        HAZARDS.add(wave);
        ApocalypseFx.send(level, origin, ApocalypseFxPayload.WAVE, owner.getId(), origin, dir, (int) Math.round(spec.speed * 100.0), (int) spec.length);
        return wave;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The spikes

    /** A ring on the ground from which sand erupts after a delay. */
    public static final class SpikeRing extends Hazard {
        private final Vec3 centre;
        private final double radius;
        private final int delay;
        private final float damage;

        SpikeRing(ServerLevel level, LivingEntity owner, @Nullable UUID fightId, Vec3 centre, double radius, int delay, float damage) {
            super(level, owner, fightId);
            this.centre = centre;
            this.radius = radius;
            this.delay = delay;
            this.damage = damage;
        }

        public Vec3 centre() {
            return centre;
        }

        @Override
        boolean step(LivingEntity owner) {
            if (age < delay) {
                return true;
            }
            AABB box = new AABB(centre.x - radius, centre.y - 1.0, centre.z - radius, centre.x + radius, centre.y + 3.0, centre.z + radius);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != owner)) {
                if (Math.hypot(victim.getX() - centre.x, victim.getZ() - centre.z) > radius + victim.getBbWidth() * 0.25 || !SandControl.canHurt(owner, victim)) {
                    continue;
                }
                if (SandControl.hurt(level, owner, victim, damage)) {
                    // Launched 0.9 up by the spikes under them.
                    Vec3 motion = victim.getDeltaMovement();
                    victim.setDeltaMovement(motion.x, Math.max(motion.y, 0.9), motion.z);
                    victim.hurtMarked = true;
                }
            }
            SandControl.puff(level, centre.add(0.0, 0.6, 0.0), 24, radius * 0.5);
            return false;
        }
    }

    /** Spikes erupt from the ground within {@code radius} of {@code centre} after {@code delay} ticks. */
    public static SpikeRing spikes(ServerLevel level, LivingEntity owner, @Nullable UUID fight, Vec3 centre, double radius, int delay, float damage) {
        SpikeRing ring = new SpikeRing(level, owner, fight, centre, radius, delay, damage);
        HAZARDS.add(ring);
        ApocalypseFx.send(level, centre, ApocalypseFxPayload.SPIKES, owner.getId(), centre, Vec3.ZERO, (int) Math.round(radius * 10.0), 6);
        return ring;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The tornado

    /** A column of sand that drifts toward the nearest victim, lifting and pulling in what it touches. */
    public static final class Tornado extends Hazard {
        /** How fast it drifts, blocks a tick. */
        public static final double DRIFT = 0.12;
        /** The most a creature is lifted by one in all. */
        public static final float MAX_LIFT = 0.6F;
        private Vec3 position;
        private final double radius;
        private final float damage;
        private final Vec3 centre;
        private final double bound;
        private int ticksLeft;
        private final Int2FloatOpenHashMap lifted = new Int2FloatOpenHashMap();

        Tornado(ServerLevel level, LivingEntity owner, @Nullable UUID fightId, Vec3 position, double radius, int ticks, float damage,
                @Nullable Vec3 centre, double bound) {
            super(level, owner, fightId);
            this.position = position;
            this.radius = radius;
            this.ticksLeft = ticks;
            this.damage = damage;
            this.centre = centre == null ? position : centre;
            this.bound = bound;
        }

        public Vec3 position() {
            return position;
        }

        public int ticksLeft() {
            return ticksLeft;
        }

        public double radius() {
            return radius;
        }

        @Override
        boolean step(LivingEntity owner) {
            if (ticksLeft-- <= 0) {
                return false;
            }
            ServerPlayer nearest = null;
            double best = 64.0 * 64.0;
            for (ServerPlayer player : level.players()) {
                double d = player.position().distanceToSqr(position);
                if (d < best && SandControl.canHurt(owner, player)) {
                    best = d;
                    nearest = player;
                }
            }
            if (nearest != null) {
                Vec3 toward = SandControl.flat(position, nearest.position());
                double x = position.x + toward.x * DRIFT;
                double z = position.z + toward.z * DRIFT;
                // It follows the nearest victim but stays where the fight is: a bystander outside the wall is not its business.
                if (bound > 0.0 && Math.hypot(x - centre.x, z - centre.z) > bound) {
                    x = position.x;
                    z = position.z;
                }
                position = new Vec3(x, SandControl.groundAt(level, x, position.y + 2.0, z), z);
            }
            Vec3 aim = nearest == null ? position : nearest.position();
            if (age % 4 == 0) {
                ApocalypseFx.send(level, position, ApocalypseFxPayload.TORNADO, id, position, aim, ticksLeft, (int) Math.round(radius * 10.0));
            }
            if (age % 10 == 0) {
                AABB box = new AABB(position.x - radius, position.y - 1.0, position.z - radius, position.x + radius, position.y + 8.0, position.z + radius);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != owner)) {
                    if (Math.hypot(victim.getX() - position.x, victim.getZ() - position.z) > radius || !SandControl.canHurt(owner, victim)) {
                        continue;
                    }
                    if (SandControl.hurt(level, owner, victim, damage)) {
                        // Lifted and pulled to the axis, but never thrown into the sky: 0.6 up in all.
                        Vec3 inward = SandControl.flat(victim.position(), position).scale(0.1);
                        float already = lifted.get(victim.getId());
                        float up = Math.min(0.25F, MAX_LIFT - already);
                        lifted.put(victim.getId(), already + Math.max(0.0F, up));
                        victim.setDeltaMovement(victim.getDeltaMovement().add(inward.x, Math.max(0.0F, up), inward.z));
                        victim.hurtMarked = true;
                    }
                }
            }
            return true;
        }
    }

    /** A tornado of {@code radius} blocks starts at {@code position}, for {@code ticks}, and hurts {@code damage} every 10 ticks. */
    public static Tornado tornado(ServerLevel level, LivingEntity owner, @Nullable UUID fight, Vec3 position, double radius, int ticks, float damage) {
        return tornado(level, owner, fight, position, radius, ticks, damage, null, 0.0);
    }

    /**
     * The same, kept inside a fight's arena: it drifts only where its distance from {@code centre} stays within {@code bound}
     * ({@code bound} 0: no limit).
     */
    public static Tornado tornado(ServerLevel level, LivingEntity owner, @Nullable UUID fight, Vec3 position, double radius, int ticks, float damage,
                                  @Nullable Vec3 centre, double bound) {
        Tornado tornado = new Tornado(level, owner, fight, position, radius, ticks, damage, centre, bound);
        HAZARDS.add(tornado);
        return tornado;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The ring

    /** A ring of sand that sweeps out from a point across the arena (the Cairo Collapse). */
    public static final class Ring extends Hazard {
        private final Vec3 centre;
        private final double speed;
        private final double band;
        private final double height;
        private final double maxRadius;
        private final float damage;
        private final IntSet hit = new IntOpenHashSet();
        private double radius;

        Ring(ServerLevel level, LivingEntity owner, @Nullable UUID fightId, Vec3 centre, double speed, double band, double height, double maxRadius, float damage) {
            super(level, owner, fightId);
            this.centre = centre;
            this.speed = speed;
            this.band = band;
            this.height = height;
            this.maxRadius = maxRadius;
            this.damage = damage;
        }

        public double radius() {
            return radius;
        }

        public IntSet hit() {
            return hit;
        }

        @Override
        boolean step(LivingEntity owner) {
            double from = radius;
            if (from >= maxRadius) {
                return false;
            }
            radius = Math.min(maxRadius, radius + speed);
            double outer = radius;
            double inner = Math.max(0.0, Math.min(from, outer - band));
            AABB box = new AABB(centre.x - outer - 1.0, centre.y - 2.0, centre.z - outer - 1.0, centre.x + outer + 1.0, centre.y + height + 8.0, centre.z + outer + 1.0);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && !hit.contains(e.getId()))) {
                double d = Math.hypot(victim.getX() - centre.x, victim.getZ() - centre.z);
                double half = victim.getBbWidth() * 0.5;
                if (d + half < inner || d - half > outer || !SandControl.canHurt(owner, victim)) {
                    continue;
                }
                double ground = SandControl.groundAt(level, victim.getX(), victim.getY() + 1.0, victim.getZ());
                if (victim.getY() < ground - 0.5 || victim.getY() > ground + height) {
                    continue;
                }
                hit.add(victim.getId());
                if (SandControl.hurt(level, owner, victim, damage)) {
                    Vec3 out = SandControl.flat(centre, victim.position());
                    victim.knockback(1.2, -out.x, -out.z);
                    victim.hurtMarked = true;
                }
            }
            return true;
        }
    }

    /** A ring of sand sweeps out from {@code centre} to {@code maxRadius}, {@code band} wide and {@code height} tall. */
    public static Ring ring(ServerLevel level, LivingEntity owner, @Nullable UUID fight, Vec3 centre, double speed, double band, double height,
                            double maxRadius, float damage) {
        Ring ring = new Ring(level, owner, fight, centre, speed, band, height, maxRadius, damage);
        HAZARDS.add(ring);
        ApocalypseFx.send(level, centre, ApocalypseFxPayload.RING, owner.getId(), centre, Vec3.ZERO, (int) Math.round(speed * 100.0), (int) maxRadius);
        return ring;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Ticking and clearing

    /** Once per level tick: every hazard of the level takes a step; the ones that are over (or whose owner is gone) go. */
    public static void tickLevel(ServerLevel level) {
        if (HAZARDS.isEmpty()) {
            return;
        }
        for (Hazard hazard : new ArrayList<>(HAZARDS)) {
            if (hazard.level != level) {
                continue;
            }
            boolean keep;
            try {
                keep = hazard.level.getEntity(hazard.ownerId) instanceof LivingEntity owner && owner.isAlive() && hazard.step(owner);
            } catch (RuntimeException e) {
                Enhanced.LOGGER.error("A hazard of Apocalypse's sand failed and is removed", e);
                keep = false;
            }
            hazard.age++;
            if (!keep) {
                HAZARDS.remove(hazard);
            }
        }
    }

    /** Every hazard that runs (a copy), for tests. */
    public static List<Hazard> all() {
        return List.copyOf(HAZARDS);
    }

    public static int count() {
        return HAZARDS.size();
    }

    /** Ends the hazards of one fight (it is over). */
    public static void clearFight(UUID fight) {
        HAZARDS.removeIf(h -> fight.equals(h.fightId));
    }

    /** Ends the hazards of one owner (the hero lost the power, or died). */
    public static void clearOwnedBy(UUID owner) {
        HAZARDS.removeIf(h -> owner.equals(h.ownerId));
    }

    /** Forgets every hazard (the server has stopped). */
    public static void clear() {
        HAZARDS.clear();
    }

    /** Whether a hazard of this level is still going (iterates a snapshot). */
    public static boolean any(ServerLevel level) {
        for (Iterator<Hazard> it = HAZARDS.iterator(); it.hasNext(); ) {
            if (it.next().level == level) {
                return true;
            }
        }
        return false;
    }

    private SandHazards() {
    }
}
