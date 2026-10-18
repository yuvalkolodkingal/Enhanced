package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.config.HelaBossConfig;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hela's mind in her fight (3.5): what she does between the set pieces of {@link HelaFight}. In {@link HelaState#IDLE}
 * she walks toward her target and, every two to four seconds (slower in the second phase), chooses one of her attacks;
 * each attack is a state of a fixed length whose moments (the tell, the throw, the blows) fall on fixed ticks of it, so
 * that the clients' poses and the server's blades meet.
 *
 * <ul>
 *     <li><b>Necrosword Volley</b>: three blades (five with three fighters or more, and in the third phase) appear behind
 *     her and fly at the target 12 ticks later, {@code volleyDamage} each.</li>
 *     <li><b>Blade Wall</b>: a line of 12 spikes from her toward the target, rising 0.8 seconds after the sweep of her
 *     hand, {@code wallDamage} each, standing six seconds.</li>
 *     <li><b>Shadow Step</b>: she dissolves, comes out behind the farthest fighter and strikes at once.</li>
 *     <li><b>Blade Melee</b>: within four blocks, three blows of {@code meleeDamage}.</li>
 *     <li><b>Blade Rain</b> (from the second phase): twelve blades over a five-block circle at the target, falling after a
 *     second, {@code rainDamage} each.</li>
 *     <li><b>Blade Storm</b> (third phase, every 15 seconds): a ring of blades sweeps out from her to 12 blocks over two
 *     seconds; whoever is on the ground when it passes is cut ({@code stormDamage}); a jump clears it.</li>
 * </ul>
 */
public final class HelaBrain {
    /** The tick of the volley's state at which the blades fly (after the tell). */
    public static final int VOLLEY_THROW = 12;
    /** The tick of the wall's state at which the spikes are set (her hand sweeps low), and their telegraph. */
    public static final int WALL_SET = 4;
    public static final int WALL_TELL = 12;
    public static final int WALL_LENGTH = 12;
    public static final int WALL_STAND = 120;
    /** The ticks of the melee's three blows. */
    public static final int[] MELEE_BLOWS = {8, 16, 24};
    public static final double MELEE_REACH = 4.5;
    /** The tick of the rain's state at which the blades appear over the circle, and how long they hang. */
    public static final int RAIN_SET = 6;
    public static final int RAIN_HANG = 20;
    public static final double RAIN_RADIUS = 5.0;
    public static final int RAIN_BLADES = 12;
    /** The Blade Storm: how far it sweeps, and how often it comes in the third phase. */
    public static final double STORM_RADIUS = 12.0;
    public static final int STORM_EVERY = 300;
    /** How near a target must be for the melee. */
    public static final double MELEE_RANGE = 4.0;

    private final HelaEntity hela;
    private final Set<Integer> stormHit = new HashSet<>();
    private long nextAttackAt;
    private long nextStormAt;
    private int repath;
    /** For the tests: she chooses no attack by herself. */
    private boolean passive;

    HelaBrain(HelaEntity hela) {
        this.hela = hela;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    /** She lets go of what she was doing (a grip took her): back to her stance, no blade thrown. */
    public void interrupt() {
        if (hela.state().attacking()) {
            if (hela.isInvisible()) {
                hela.setInvisible(false);
            }
            hela.setState(HelaState.IDLE);
        }
    }

    /** The wait before her first attack (the arrival is over) and before the first storm of the third phase. */
    void reset(long now) {
        nextAttackAt = now + 30;
        nextStormAt = now + STORM_EVERY / 2;
    }

    /** The third phase began: the first storm comes soon. */
    void stormSoon(long now) {
        nextStormAt = now + 100;
    }

    /** One tick of her mind (the fight calls it only while she fights). */
    void tick(HelaFight fight, @Nullable LivingEntity target) {
        HelaState state = hela.state();
        int t = hela.stateTicks();
        switch (state) {
            case IDLE -> idle(fight, target);
            case VOLLEY -> volley(fight, target, t);
            case WALL -> wall(fight, t);
            case STEP_OUT -> stepOut(fight, target, t);
            case STEP_IN -> {
                face(target);
                if (t >= HelaState.STEP_IN.ticks) {
                    start(HelaState.MELEE, target);
                }
            }
            case MELEE -> melee(fight, target, t);
            case RAIN -> rain(fight, t);
            case STORM -> storm(fight, t);
            case CATCH, STUNNED -> {
                if (t >= state.ticks) {
                    toIdle(fight);
                }
            }
            default -> {
            }
        }
    }

    private void toIdle(HelaFight fight) {
        hela.setState(HelaState.IDLE);
        long now = hela.level().getGameTime();
        int phase = fight.phase();
        int min = phase == 2 ? 60 : 40;
        int spread = phase == 2 ? 40 : phase == 3 ? 30 : 40;
        nextAttackAt = now + min + hela.getRandom().nextInt(spread + 1);
    }

    /** Starts one of her attacks at {@code target} (the tests call it to choose for her). */
    public void start(HelaState attack, @Nullable LivingEntity target) {
        hela.getNavigation().stop();
        if (target != null) {
            hela.setTarget(target);
            hela.setAim(target.position());
        }
        hela.setState(attack);
        if (attack == HelaState.STORM) {
            stormHit.clear();
        }
        HelaFight fight = hela.fight();
        if (attack == HelaState.VOLLEY && fight != null) {
            int count = fight.volleyCount();
            hela.setBladeCount(count);
            List<Vec3> points = HelaAttacks.fanPoints(hela.position(), hela.getYRot(), hela.getBbHeight(), count);
            HelaFx.send((ServerLevel) hela.level(), HelaFx.VOLLEY, hela.getId(), points, VOLLEY_THROW);
        }
        if (attack == HelaState.STORM) {
            ServerLevel level = (ServerLevel) hela.level();
            HelaFx.send(level, HelaFx.STORM, hela.getId(), hela.position(), (int) Math.round(STORM_RADIUS * 10.0));
            level.playSound(null, hela.getX(), hela.getY(), hela.getZ(), ModSounds.BLADE_STORM.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        }
        if (attack == HelaState.STEP_OUT) {
            ServerLevel level = (ServerLevel) hela.level();
            level.playSound(null, hela.getX(), hela.getY(), hela.getZ(), ModSounds.HELA_SHADOW_STEP.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        }
    }

    // ---- idle: walk and choose ----

    private void idle(HelaFight fight, @Nullable LivingEntity target) {
        if (target == null) {
            hela.getNavigation().stop();
            return;
        }
        face(target);
        double distance = hela.distanceTo(target);
        if (--repath <= 0) {
            repath = 10;
            if (distance > 7.0) {
                hela.getNavigation().moveTo(target, 1.0);
            } else if (distance < 3.0) {
                hela.getNavigation().stop();
            }
        }
        long now = hela.level().getGameTime();
        if (passive || now < nextAttackAt) {
            return;
        }
        HelaState choice = choose(fight, target, distance, now);
        if (choice == HelaState.STEP_OUT) {
            LivingEntity far = fight.farthestFoe();
            start(choice, far != null ? far : target);
        } else {
            start(choice, target);
        }
    }

    /** Her choice of attack: the storm when it is due, the blade combo when close, else a throw, a wall, a step or a rain. */
    private HelaState choose(HelaFight fight, LivingEntity target, double distance, long now) {
        int phase = fight.phase();
        if (phase >= 3 && now >= nextStormAt) {
            nextStormAt = now + STORM_EVERY;
            return HelaState.STORM;
        }
        int roll = hela.getRandom().nextInt(100);
        if (distance <= MELEE_RANGE) {
            return roll < 60 ? HelaState.MELEE : roll < 80 ? HelaState.WALL : HelaState.VOLLEY;
        }
        LivingEntity far = fight.farthestFoe();
        boolean step = far != null && far.distanceTo(hela) > 8.0;
        if (phase >= 2 && roll < 25) {
            return HelaState.RAIN;
        }
        if (roll < 60) {
            return HelaState.VOLLEY;
        }
        if (roll < 85 || !step) {
            return HelaState.WALL;
        }
        return HelaState.STEP_OUT;
    }

    private void face(@Nullable LivingEntity target) {
        if (target != null) {
            hela.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double dx = target.getX() - hela.getX();
            double dz = target.getZ() - hela.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            hela.setYRot(yaw);
            hela.yBodyRot = yaw;
            hela.yHeadRot = yaw;
        }
    }

    // ---- the attacks ----

    private void volley(HelaFight fight, @Nullable LivingEntity target, int t) {
        face(target);
        if (target != null && t < VOLLEY_THROW) {
            hela.setAim(target.getEyePosition().subtract(0.0, 0.3, 0.0));
        }
        if (t == VOLLEY_THROW) {
            List<Vec3> from = HelaAttacks.fanPoints(hela.position(), hela.getYRot(), hela.getBbHeight(), hela.bladeCount());
            HelaAttacks.volley((ServerLevel) hela.level(), hela, from, hela.aim(), 5.0F, HelaBossConfig.VOLLEY_DAMAGE.get().floatValue());
        }
        if (t >= HelaState.VOLLEY.ticks) {
            toIdle(fight);
        }
    }

    private void wall(HelaFight fight, int t) {
        if (t == WALL_SET) {
            Vec3 dir = hela.aim().subtract(hela.position());
            if (dir.horizontalDistanceSqr() < 1.0E-4) {
                dir = hela.getLookAngle();
            }
            List<Vec3> points = HelaAttacks.wallPoints(hela.level(), hela.position(), dir, WALL_LENGTH);
            fight.adoptAll(HelaAttacks.wall((ServerLevel) hela.level(), hela, points, WALL_TELL, WALL_STAND,
                    HelaBossConfig.WALL_DAMAGE.get().floatValue()));
        }
        if (t >= HelaState.WALL.ticks) {
            toIdle(fight);
        }
    }

    private void stepOut(HelaFight fight, @Nullable LivingEntity target, int t) {
        ServerLevel level = (ServerLevel) hela.level();
        if (t == 4) {
            hela.setInvisible(true);
        }
        if (t < HelaState.STEP_OUT.ticks) {
            return;
        }
        Vec3 from = hela.position();
        Vec3 to = target == null ? null : behind(fight, target);
        if (to != null) {
            hela.moveTo(to.x, to.y, to.z, hela.getYRot(), 0.0F);
            hela.getNavigation().stop();
        }
        hela.setInvisible(false);
        HelaFx.send(level, HelaFx.SHADOW_STEP, hela.getId(), List.of(from, hela.position()), 0);
        level.playSound(null, hela.getX(), hela.getY(), hela.getZ(), ModSounds.HELA_SHADOW_STEP.get(), SoundSource.HOSTILE, 1.5F, 1.2F);
        hela.setState(HelaState.STEP_IN);
        face(target);
    }

    /** A spot behind {@code target} (two blocks back from where he looks), else beside him, in the arena and with room. */
    @Nullable
    private Vec3 behind(HelaFight fight, LivingEntity target) {
        Vec3 look = target.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (look.lengthSqr() < 1.0E-4) {
            look = new Vec3(0, 0, 1);
        }
        look = look.normalize();
        Vec3[] tries = {target.position().subtract(look.scale(2.0)), target.position().add(look.yRot((float) Math.PI / 2).scale(2.0)),
                target.position().add(look.yRot((float) -Math.PI / 2).scale(2.0)), target.position().add(look.scale(2.0))};
        for (Vec3 spot : tries) {
            Vec3 feet = HelaRules.onGround(hela.level(), spot.add(0.0, 1.0, 0.0));
            if (fight.arena().contains(feet) && fight.arena().distance(feet) < fight.arena().containRadius() - 1.0 && Teleports.fits(hela, feet)) {
                return feet;
            }
        }
        return null;
    }

    private void melee(HelaFight fight, @Nullable LivingEntity target, int t) {
        face(target);
        if (target != null && hela.distanceTo(target) > 2.2 && t < MELEE_BLOWS[2]) {
            hela.getNavigation().moveTo(target, 1.3);
        }
        for (int i = 0; i < MELEE_BLOWS.length; i++) {
            if (t == MELEE_BLOWS[i]) {
                blow(target, i == MELEE_BLOWS.length - 1);
                hela.swing(i % 2 == 0 ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND);
            }
        }
        if (t >= HelaState.MELEE.ticks) {
            toIdle(fight);
        }
    }

    /** One blow of the combo: everything she may hurt within reach in front of her (the last one throws them back). */
    private void blow(@Nullable LivingEntity target, boolean last) {
        ServerLevel level = (ServerLevel) hela.level();
        float damage = HelaBossConfig.MELEE_DAMAGE.get().floatValue();
        Vec3 look = Vec3.directionFromRotation(0.0F, hela.getYRot());
        level.playSound(null, hela.getX(), hela.getY(), hela.getZ(), ModSounds.NECROSWORD_THROW.get(), SoundSource.HOSTILE, 0.8F, 1.5F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, hela.getBoundingBox().inflate(MELEE_REACH),
                e -> HelaRules.canHurt(hela, e))) {
            Vec3 to = e.position().subtract(hela.position());
            double flat = to.horizontalDistance();
            if (flat > MELEE_REACH || flat > 0.5 && to.multiply(1, 0, 1).normalize().dot(look) < 0.2) {
                continue;
            }
            if (HelaRules.hurt(e, ModDamageTypes.NECROSWORD, hela, hela, damage) && last && flat > 1.0E-3) {
                e.knockback(1.2, -to.x / flat, -to.z / flat);
            }
        }
    }

    private void rain(HelaFight fight, int t) {
        if (t == RAIN_SET) {
            fight.adoptAll(HelaAttacks.rain((ServerLevel) hela.level(), hela, hela.aim(), RAIN_RADIUS, RAIN_BLADES, RAIN_HANG,
                    HelaBossConfig.RAIN_DAMAGE.get().floatValue()));
        }
        if (t >= HelaState.RAIN.ticks) {
            toIdle(fight);
        }
    }

    /** How far the storm's ring has swept at tick {@code t} of it. */
    public static double stormRadius(float t) {
        return STORM_RADIUS * Math.min(1.0F, t / HelaState.STORM.ticks);
    }

    private void storm(HelaFight fight, int t) {
        ServerLevel level = (ServerLevel) hela.level();
        double r = stormRadius(t);
        float damage = HelaBossConfig.STORM_DAMAGE.get().floatValue();
        AABB box = hela.getBoundingBox().inflate(STORM_RADIUS + 1.0, 2.0, STORM_RADIUS + 1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> HelaRules.canHurt(hela, e))) {
            double d = Math.hypot(e.getX() - hela.getX(), e.getZ() - hela.getZ());
            double floor = HelaRules.groundAt(level, e.getX(), e.getY() + 0.5, e.getZ());
            boolean grounded = e.getY() - floor < 0.5;
            if (Math.abs(d - r) <= 0.9 && grounded && stormHit.add(e.getId())) {
                HelaRules.hurt(e, ModDamageTypes.BLADE_WALL, hela, hela, damage);
            }
        }
        if (t >= HelaState.STORM.ticks) {
            toIdle(fight);
        }
    }

    /** Whether the storm already cut this creature (for the tests). */
    public boolean stormHit(LivingEntity e) {
        return stormHit.contains(e.getId());
    }

    @Nullable
    static ServerPlayer asPlayer(@Nullable LivingEntity e) {
        return e instanceof ServerPlayer player ? player : null;
    }
}
