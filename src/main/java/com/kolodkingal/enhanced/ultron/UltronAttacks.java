package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The six attacks of the Ultron boss, one timeline each. Every one begins with its <em>tell</em> (the state the clients
 * pose, an effect payload, a sound), has a moment at which its aim is <em>fixed</em> (so that a player who moves after it
 * is not followed), and does no damage before its tell is over:
 *
 * <ul>
 *     <li><b>Repulsor Volley</b>: 14 ticks of raised arms and a glint line, then three bursts of three bolts, six ticks
 *     apart, each aimed (led by 35 percent of the flight) at the moment it is fired. A Sentry-Ultron fires one burst.</li>
 *     <li><b>Finger Beams</b>: 30 ticks in which ten aim lines follow the target's feet, then the point locks and ten
 *     beams fan out for 20 ticks, hitting everything within 0.6 blocks of a ray every four ticks (each creature at most
 *     once a step); a Chaos Shield in the way stops a ray and pays for it.</li>
 *     <li><b>Thruster Dive</b>: he rises four blocks, a red ring appears at a fighter's feet and shrinks over 12 ticks (it
 *     does not follow), then he dives at 1.6 blocks a tick, hitting whoever is within 1.5 blocks of his path, and lands in
 *     the ring: a shockwave and a stagger.</li>
 *     <li><b>Gravity Pull</b>: 24 ticks of a contracting ring, then every fighter within 16 blocks and in sight is dragged
 *     to him for 12 ticks, and at tick 40 he punches the ground: a shockwave that spares anyone two blocks up.</li>
 *     <li><b>Call Sentries</b>: 30 ticks of cracks and pillars of light at every spawn point, then a wave emerges.</li>
 *     <li><b>Punch</b>: 8 ticks with the fist drawn back along a fixed line, then one blow.</li>
 * </ul>
 *
 * <p>Nothing here ever breaks a block, whatever the game rule says.
 */
final class UltronAttacks {
    /** The speed of a Volley bolt, in blocks a tick. */
    static final double BOLT_SPEED = 1.7;
    /** The Volley's bursts come this many ticks apart. */
    static final int BURST_GAP = 6;
    static final double BOLT_SPREAD_DEGREES = 2.5;
    /** The Beams fan this many degrees either side of the locked line. */
    static final double BEAM_FAN_DEGREES = 10.0;
    static final int BEAM_TELL = 30;
    static final int BEAM_FIRE_TICKS = 20;
    static final int BEAM_STEP = 4;
    static final double BEAM_REACH = 32.0;
    static final double BEAM_WIDTH = 0.6;
    static final double DIVE_RISE = 4.0;
    static final int DIVE_RISE_TICKS = 12;
    static final double DIVE_SPEED = 1.6;
    static final double DIVE_PATH = 1.5;
    static final double DIVE_PATH_DAMAGE = 8.0;
    static final double DIVE_RING = 4.0;
    static final double DIVE_SHOCK = 4.5;
    static final double PULL_RANGE = 16.0;
    static final int PULL_START = 24;
    static final int PULL_TICKS = 12;
    static final int PULL_PUNCH = 40;
    static final double PULL_SHOCK = 6.0;
    static final double PUNCH_KNOCKBACK = 0.8;

    /**
     * One tick of an attack.
     *
     * @param t the ticks since it began
     * @return true when it is over
     */
    static boolean tick(UltronEntity boss, UltronFight fight, UltronBrain brain, UltronAttack attack, UltronBrain.Run run, int t, @Nullable LivingEntity target) {
        ServerLevel level = (ServerLevel) boss.level();
        if (target != null) {
            run.last = target.position();
            boss.getLookControl().setLookAt(target, 40.0F, 40.0F);
        }
        return switch (attack) {
            case VOLLEY -> volley(level, boss, fight, run, t, target);
            case BEAMS -> beams(level, boss, fight, run, t, target);
            case DIVE -> dive(level, boss, fight, run, t, target);
            case PULL -> pull(level, boss, fight, run, t);
            case CALL -> call(level, boss, fight, t);
            case PUNCH -> punch(level, boss, fight, run, t, target);
        };
    }

    // ---- Repulsor Volley ----

    private static boolean volley(ServerLevel level, UltronEntity boss, UltronFight fight, UltronBrain.Run run, int t, @Nullable LivingEntity target) {
        int tell = UltronAttack.VOLLEY.tell();
        int bursts = boss.bodyIndex() >= 2 ? 1 : 3;
        if (t == 0) {
            UltronSounds.SERVO.play(level, boss.position());
        }
        if (t == tell - 6 && target != null) {
            UltronFx.from(level, UltronFxPayload.GLINT, boss, chest(boss), target.position().add(0, target.getBbHeight() * 0.6, 0), 0.0F, 6);
        }
        for (int i = 0; i < bursts; i++) {
            if (t == tell + BURST_GAP * i) {
                burst(level, boss, fight, target, run);
            }
        }
        UltronMotion.steer(boss, Vec3.ZERO, 0.2);
        return t >= tell + BURST_GAP * (bursts - 1) + 8;
    }

    /** Three bolts from the two palms at the point the target is at (and going to) now. */
    private static void burst(ServerLevel level, UltronEntity boss, UltronFight fight, @Nullable LivingEntity target, UltronBrain.Run run) {
        Vec3 chest = chest(boss);
        Vec3 point = target != null ? lead(target, chest) : run.last.add(0, 1.0, 0);
        run.aim = point;
        Vec3 forward = point.subtract(chest).normalize();
        RandomSource random = boss.getRandom();
        float damage = scaled(UltronBossConfig.VOLLEY_BOLT_DAMAGE.get());
        for (int j = 0; j < 3; j++) {
            Vec3 from = palm(boss, forward, j % 2 == 0 ? -1 : 1);
            Vec3 direction = spread(point.subtract(from).normalize(), random, BOLT_SPREAD_DEGREES);
            UltronBoltEntity bolt = UltronBoltEntity.create(level, boss, from, direction.scale(BOLT_SPEED), damage, UltronBoltEntity.Palette.PRIME);
            fight.spawn(bolt);
        }
        UltronSounds.BOLT.play(level, chest);
    }

    /** The target's chest, led by 35 percent of the bolt's flight time at the speed it is moving. */
    static Vec3 lead(LivingEntity target, Vec3 from) {
        Vec3 chest = target.position().add(0, target.getBbHeight() * 0.6, 0);
        double flight = chest.distanceTo(from) / BOLT_SPEED;
        return chest.add(target.getKnownMovement().scale(flight * 0.35));
    }

    private static Vec3 spread(Vec3 direction, RandomSource random, double degrees) {
        Vec3 helper = Math.abs(direction.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = direction.cross(helper).normalize();
        Vec3 v = direction.cross(u).normalize();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double size = Math.tan(Math.toRadians(degrees)) * Math.sqrt(random.nextDouble());
        return direction.add(u.scale(Math.cos(angle) * size)).add(v.scale(Math.sin(angle) * size)).normalize();
    }

    // ---- Finger Beams ----

    private static boolean beams(ServerLevel level, UltronEntity boss, UltronFight fight, UltronBrain.Run run, int t, @Nullable LivingEntity target) {
        if (t == 0) {
            UltronSounds.SERVO.play(level, boss.position(), 1.0F, 0.8F);
            run.lock = run.last;
        }
        UltronMotion.steer(boss, Vec3.ZERO, 0.2);
        if (t < BEAM_TELL) {
            // The ten aim lines follow the target's feet until the moment they lock.
            if (target != null) {
                run.lock = target.position();
            }
            if (t % 5 == 0) {
                UltronFx.from(level, UltronFxPayload.BEAMS_AIM, boss, run.lock, Vec3.ZERO, 0.0F, 5);
            }
            return false;
        }
        if (t == BEAM_TELL) {
            UltronSounds.BEAM.play(level, boss.position());
            UltronFx.from(level, UltronFxPayload.BEAMS_FIRE, boss, run.lock, Vec3.ZERO, 0.0F, BEAM_FIRE_TICKS);
        }
        if (t < BEAM_TELL + BEAM_FIRE_TICKS && (t - BEAM_TELL) % BEAM_STEP == 0) {
            beamStep(level, boss, run);
        }
        return t >= BEAM_TELL + BEAM_FIRE_TICKS + 6;
    }

    /** The positions of the ten fingertips: five on each hand, spread along the hand. */
    static List<Vec3> fingertips(UltronEntity boss, Vec3 toward) {
        Vec3 chest = chest(boss);
        Vec3 forward = toward.subtract(chest);
        forward = forward.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : forward.normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        double scale = scale(boss);
        List<Vec3> tips = new ArrayList<>(10);
        for (int side = -1; side <= 1; side += 2) {
            Vec3 hand = palm(boss, forward, side);
            for (int finger = 0; finger < 5; finger++) {
                tips.add(hand.add(forward.scale(0.45 * scale)).add(right.scale((finger - 2) * 0.06 * scale)));
            }
        }
        return tips;
    }

    /** Ten rays, fanned 10 degrees either side of the locked line: everything within 0.6 blocks of one is hit, once. */
    private static void beamStep(ServerLevel level, UltronEntity boss, UltronBrain.Run run) {
        Vec3 lockPoint = run.lock;
        List<Vec3> tips = fingertips(boss, lockPoint);
        Set<LivingEntity> hit = new LinkedHashSet<>();
        Map<Player, Boolean> shields = new HashMap<>();
        for (int k = 0; k < tips.size(); k++) {
            Vec3 origin = tips.get(k);
            double fan = tips.size() == 1 ? 0.0 : -BEAM_FAN_DEGREES + 2.0 * BEAM_FAN_DEGREES * k / (tips.size() - 1);
            Vec3 direction = rotateY(lockPoint.subtract(origin).normalize(), Math.toRadians(fan));
            Vec3 end = origin.add(direction.scale(BEAM_REACH));
            HitResult block = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            if (block.getType() != HitResult.Type.MISS) {
                end = block.getLocation();
            }
            WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, boss, origin, end, 1.0F);
            if (shield != null) {
                end = shield.point();
                shields.put(shield.owner(), Boolean.TRUE);
                level.sendParticles(new EnergyParticleOptions(0xFF8A5C, 1.0F), end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.1);
            }
            AABB path = new AABB(origin, end).inflate(BEAM_WIDTH + 1.0);
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, path, e -> mayHit(boss, e))) {
                if (candidate.getBoundingBox().inflate(BEAM_WIDTH).clip(origin, end).isPresent()) {
                    hit.add(candidate);
                }
            }
            if (k % 3 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 1, 0.05, 0.05, 0.05, 0.1);
            }
        }
        float damage = scaled(UltronBossConfig.FINGER_BEAM_DAMAGE.get());
        for (LivingEntity victim : hit) {
            hurt(level, boss, victim, ModDamageTypes.ULTRON_BEAM, damage, 0.0, true);
        }
        for (Player shielded : shields.keySet()) {
            if (shielded instanceof ServerPlayer wanda) {
                WandaAbilities.wear(wanda, damage);
            }
        }
    }

    private static Vec3 rotateY(Vec3 v, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    // ---- Thruster Dive ----

    private static boolean dive(ServerLevel level, UltronEntity boss, UltronFight fight, UltronBrain.Run run, int t, @Nullable LivingEntity target) {
        int diveAt = UltronAttack.DIVE.tell();
        Vec3 position = boss.position();
        if (t == 0) {
            run.from = position;
            UltronSounds.SERVO.play(level, position, 1.0F, 0.6F);
        }
        if (t < DIVE_RISE_TICKS) {
            double rise = DIVE_RISE / DIVE_RISE_TICKS;
            boolean room = level.noCollision(boss, boss.getBoundingBox().move(0, rise + 0.1, 0));
            UltronMotion.steer(boss, new Vec3(0, room ? rise : 0.0, 0), 0.2);
            return false;
        }
        if (t == DIVE_RISE_TICKS) {
            // The ring goes where the fighter stands now, on the ground, and stays there.
            Vec3 at = target != null ? target.position() : run.last;
            run.ring = new Vec3(at.x, UltronMotion.groundBelow(level, at.x, at.y + 0.1, at.z), at.z);
            UltronFx.at(level, UltronFxPayload.DIVE_RING, run.ring, (float) DIVE_RING, diveAt - DIVE_RISE_TICKS);
        }
        if (t < diveAt) {
            UltronMotion.steer(boss, Vec3.ZERO, 0.2);
            return false;
        }
        // The dive: straight at the ring, 1.6 blocks a tick.
        Vec3 toward = run.ring.subtract(position);
        double distance = toward.length();
        boolean blocked = t > diveAt + 2 && boss.getDeltaMovement().lengthSqr() < 0.01;
        if (distance <= DIVE_SPEED + 0.1 || t > diveAt + 40 || blocked) {
            land(level, boss, run);
            return true;
        }
        Vec3 velocity = toward.scale(DIVE_SPEED / distance);
        boss.setDeltaMovement(velocity);
        AABB sweep = boss.getBoundingBox().expandTowards(velocity).inflate(DIVE_PATH);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, sweep, e -> mayHit(boss, e))) {
            if (run.hit.add(victim.getUUID())) {
                hurt(level, boss, victim, ModDamageTypes.ULTRON_SLAM, scaled(DIVE_PATH_DAMAGE), 0.4, false);
            }
        }
        return false;
    }

    /** The landing: the shockwave, then the stagger the brain begins. */
    private static void land(ServerLevel level, UltronEntity boss, UltronBrain.Run run) {
        Vec3 ring = run.ring;
        boss.setPos(ring.x, ring.y, ring.z);
        boss.setDeltaMovement(Vec3.ZERO);
        UltronSounds.GRAVITY.play(level, ring, 1.4F, 0.7F);
        UltronFx.at(level, UltronFxPayload.SHOCKWAVE, ring, (float) DIVE_SHOCK, 12);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, ring.x, ring.y + 0.5, ring.z, 12, 0.6, 0.3, 0.6, 0.02);
        float damage = scaled(UltronBossConfig.DIVE_DAMAGE.get());
        for (LivingEntity victim : shockwave(level, boss, ring, DIVE_SHOCK)) {
            if (hurt(level, boss, victim, ModDamageTypes.ULTRON_SLAM, damage, 1.2, false)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), boss);
            }
        }
    }

    // ---- Gravity Pull ----

    private static boolean pull(ServerLevel level, UltronEntity boss, UltronFight fight, UltronBrain.Run run, int t) {
        Vec3 position = boss.position();
        UltronMotion.steer(boss, Vec3.ZERO, 0.2);
        if (t == 0) {
            run.from = new Vec3(position.x, UltronMotion.groundBelow(level, position.x, position.y + 0.1, position.z), position.z);
            UltronFx.at(level, UltronFxPayload.PULL_RING, run.from, (float) PULL_RANGE, PULL_START);
            UltronSounds.GRAVITY.play(level, position);
        }
        if (t >= PULL_START && t < PULL_START + PULL_TICKS) {
            Vec3 chest = chest(boss);
            for (ServerPlayer player : fight.fighters()) {
                if (player.isCreative() || player.position().distanceTo(position) > PULL_RANGE || !boss.hasLineOfSight(player)) {
                    continue;
                }
                Vec3 toward = chest.subtract(player.position().add(0, player.getBbHeight() * 0.5, 0));
                double distance = toward.length();
                if (distance > 1.0) {
                    player.setDeltaMovement(toward.scale(Math.min(1.1, distance * 0.3) / distance));
                    player.hurtMarked = true;
                    player.fallDistance = 0.0F;
                }
            }
        }
        if (t == PULL_PUNCH) {
            Vec3 ground = new Vec3(position.x, UltronMotion.groundBelow(level, position.x, position.y + 0.1, position.z), position.z);
            UltronSounds.GRAVITY.play(level, ground, 1.5F, 0.5F);
            UltronFx.at(level, UltronFxPayload.SHOCKWAVE, ground, (float) PULL_SHOCK, 12);
            float damage = scaled(UltronBossConfig.PULL_DAMAGE.get());
            for (LivingEntity victim : shockwave(level, boss, ground, PULL_SHOCK)) {
                hurt(level, boss, victim, ModDamageTypes.ULTRON_SLAM, damage, 1.0, false);
            }
        }
        return t >= PULL_PUNCH + 16;
    }

    /**
     * Whom a ground shockwave of the given radius hurts: the fighters and what is on their side that stand within the
     * radius and less than two blocks above the ground (a jump, or flight, lets a shockwave pass under).
     */
    static List<LivingEntity> shockwave(ServerLevel level, UltronEntity boss, Vec3 centre, double radius) {
        AABB box = new AABB(centre.x - radius, centre.y - 3.0, centre.z - radius, centre.x + radius, centre.y + 6.0, centre.z + radius);
        List<LivingEntity> victims = new ArrayList<>();
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box, e -> mayHit(boss, e))) {
            double dx = candidate.getX() - centre.x;
            double dz = candidate.getZ() - centre.z;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            double above = candidate.getY() - UltronMotion.groundBelow(level, candidate.getX(), candidate.getY() + 0.1, candidate.getZ());
            if (above < 2.0) {
                victims.add(candidate);
            }
        }
        return victims;
    }

    // ---- Call Sentries ----

    private static boolean call(ServerLevel level, UltronEntity boss, UltronFight fight, int t) {
        UltronMotion.steer(boss, Vec3.ZERO, 0.2);
        if (t == 0) {
            fight.callWave(true);
            UltronFx.send(level, UltronFxPayload.PULSE, boss.getId(), boss.position(), Vec3.ZERO, 0.0F, UltronAttack.CALL.tell());
            UltronSounds.SERVO.play(level, boss.position(), 1.0F, 0.5F);
        }
        return t >= UltronAttack.CALL.tell() + 10;
    }

    // ---- Punch ----

    private static boolean punch(ServerLevel level, UltronEntity boss, UltronFight fight, UltronBrain.Run run, int t, @Nullable LivingEntity target) {
        int tell = UltronAttack.PUNCH.tell();
        UltronMotion.steer(boss, Vec3.ZERO, 0.2);
        if (t == 0) {
            // The line of the fist is fixed now.
            Vec3 at = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0) : run.last;
            Vec3 direction = at.subtract(chest(boss));
            run.aim = direction.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : direction.normalize();
            UltronSounds.SERVO.play(level, boss.position(), 1.0F, 1.2F);
        }
        if (t == tell) {
            double scale = scale(boss);
            Vec3 fist = chest(boss).add(run.aim.scale(2.0 * scale));
            AABB reach = new AABB(fist, fist).inflate(1.7 * scale + 0.4);
            UltronSounds.PUNCH.play(level, fist);
            float damage = scaled(UltronBossConfig.PUNCH_DAMAGE.get());
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, reach, e -> mayHit(boss, e))) {
                if (victim.getBoundingBox().inflate(0.4).distanceToSqr(chest(boss)) <= UltronAttack.PUNCH_RANGE * UltronAttack.PUNCH_RANGE) {
                    hurt(level, boss, victim, ModDamageTypes.ULTRON_SLAM, damage, PUNCH_KNOCKBACK, false);
                }
            }
        }
        return t >= tell + 10;
    }

    // ---- shared ----

    /** The damage of a number from the config, scaled by the bosses' difficulty. */
    static float scaled(double base) {
        return (float) (base * BossConfig.DIFFICULTY_SCALE.get());
    }

    /** The size of the body, as a multiple of Prime's. */
    static double scale(UltronEntity boss) {
        return boss.body().hitboxScale();
    }

    /** The middle of the body's chest: where the palms and the beams start from. */
    static Vec3 chest(UltronEntity boss) {
        return boss.position().add(0, boss.getBbHeight() * 0.62, 0);
    }

    /** The point of a palm, to the left (-1) or right (+1) of the body as it faces {@code forward}. */
    static Vec3 palm(UltronEntity boss, Vec3 forward, int side) {
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        return chest(boss).add(right.scale(side * 0.7 * scale(boss))).add(forward.scale(0.5 * scale(boss)));
    }

    /** Whether his blow may land on this creature: anything but his own side, Creative and Spectator players. */
    static boolean mayHit(UltronEntity boss, LivingEntity victim) {
        return victim != boss && AreaTargets.mayHurt(boss, victim, false);
    }

    /**
     * One of his blows lands. The hit cooldown is cleared first when {@code fresh} (the Beams strike every four ticks and
     * every hit counts); the victim is knocked back from him by {@code knockback}.
     *
     * @return whether the creature was hurt
     */
    static boolean hurt(ServerLevel level, UltronEntity boss, LivingEntity victim, ResourceKey<DamageType> type, float amount, double knockback,
                        boolean fresh) {
        if (fresh) {
            victim.invulnerableTime = 0;
        }
        boolean hurt = victim.hurt(ModDamageTypes.source(level, type, boss, boss), amount);
        if (hurt && knockback > 0.0) {
            Vec3 out = victim.position().subtract(boss.position());
            victim.knockback(knockback, -out.x, -out.z);
            victim.hurtMarked = true;
        }
        return hurt;
    }

    private UltronAttacks() {
    }
}
