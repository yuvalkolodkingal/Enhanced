package com.kolodkingal.enhanced.ultron;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What Ultron's body decides, tick by tick: whom to hunt, where to hover, which of the six attacks ({@link
 * UltronAttack}) to do next, and how long it takes. One brain belongs to one body.
 *
 * <p>He hunts the nearest fighter who is not Creative or Spectator, and keeps his choice for 100 ticks unless it dies.
 * While he is idle he hovers 10 to 14 blocks from the target, three blocks above the ground under him (a bob of one),
 * strafing at five blocks a second and backing away from anyone within five blocks unless he is about to punch; when the
 * global gap has passed he picks an attack by weight ({@link UltronAttack#choose}). The attack then runs on its own
 * timeline ({@link UltronAttacks}): a tell, a fixed aim moment, the blow, and a recovery. The Dive ends in a stagger of
 * 50 ticks, after which the next attack is a Volley.
 *
 * <p>The fight decides when the brain acts ({@link #setEnabled}): not while he is being assembled, channelling the lift,
 * uploading or dying.
 */
public final class UltronBrain {
    /** How long he keeps a target, in ticks, unless it dies. */
    public static final int TARGET_KEEP = 100;
    /** The stagger after a Dive. */
    public static final int STAGGER_TICKS = 50;

    /** Scratch space of the attack that is running: what it locked, what it already hit. */
    static final class Run {
        Vec3 lock = Vec3.ZERO;
        Vec3 aim = Vec3.ZERO;
        Vec3 ring = Vec3.ZERO;
        Vec3 from = Vec3.ZERO;
        Vec3 last = Vec3.ZERO;
        final Set<UUID> hit = new HashSet<>();
        int landed = -1;
    }

    private boolean enabled;
    /** Whether the brain chooses its next attack by itself; off, only {@link #force} starts one (the tests, an operator). */
    private boolean choosing = true;
    @Nullable
    private UUID targetId;
    private long targetSince;
    @Nullable
    private UltronAttack current;
    private int ticks;
    @Nullable
    private UltronAttack last;
    private final long[] readyAt = new long[UltronAttack.values().length];
    private long lastBeams = -1000;
    private long lastDive = -1000;
    private long nextActionAt;
    private boolean afterStagger;
    private int stagger;
    private int strafeSign = 1;
    private int strafeUntil;
    private Run run = new Run();
    /** Every call of {@link #tick} (see {@link #ticked}). */
    private long ticked;

    /** Whether the brain acts at all. The fight turns it on when the fight is on and off for the cutscenes. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether the brain picks the next attack itself (the default). Off, it still hovers, hunts and runs the attack it
     * was given, but starts none of its own: only {@link #force} does.
     */
    public void setChoosing(boolean choosing) {
        this.choosing = choosing;
    }

    public boolean isChoosing() {
        return choosing;
    }

    /** The attack that is running, or null while he is idle, recovering or staggered. */
    @Nullable
    public UltronAttack current() {
        return current;
    }

    /** The ticks the running attack has been going. */
    public int ticks() {
        return ticks;
    }

    /** The attack he did last (null before the first). */
    @Nullable
    public UltronAttack last() {
        return last;
    }

    /** Whether he is between attacks: nothing running and not staggered (the lift starts only then). */
    public boolean isIdle() {
        return current == null && stagger == 0;
    }

    /** The ticks of the stagger that are left. */
    public int staggerLeft() {
        return stagger;
    }

    /** Whether the next attack is the Volley that follows a stagger. */
    public boolean afterStagger() {
        return afterStagger;
    }

    /** The creature he is hunting, or null. */
    @Nullable
    public LivingEntity target(ServerLevel level) {
        return targetId != null && level.getEntity(targetId) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    /** Where the running (or last) Dive's ring is: fixed when it appears, it does not follow anyone. */
    public Vec3 diveRing() {
        return run.ring;
    }

    /** Where the Finger Beams are aimed: the target's feet until the tell ends, then the point they locked. */
    public Vec3 beamsLock() {
        return run.lock;
    }

    /** The game time {@code attack} can be chosen again. */
    public long readyAt(UltronAttack attack) {
        return readyAt[attack.ordinal()];
    }

    /** Makes an attack ready at once (the tests, an operator). */
    public void resetCooldowns() {
        Arrays.fill(readyAt, 0L);
        lastBeams = -1000;
        lastDive = -1000;
        nextActionAt = 0;
        last = null;
    }

    /** Stops whatever he is doing, and any stagger (the lift begins, a transfer begins). */
    public void abort(UltronEntity boss) {
        current = null;
        ticks = 0;
        stagger = 0;
        afterStagger = false;
        if (boss.state().isAttack() || boss.state() == UltronState.STAGGER) {
            boss.setState(UltronState.IDLE);
        }
    }

    /**
     * Drops the attack in progress (telekinesis took hold of the body, D-T 4.4), but unlike {@link #abort} leaves a
     * stagger as it is: a grab never ends the team's window after a Dive, nor lengthens it.
     */
    public void dropAttack(UltronEntity boss) {
        current = null;
        ticks = 0;
        if (boss.state().isAttack()) {
            boss.setState(UltronState.IDLE);
        }
    }

    /** How many times the brain was ticked (the tests count the ticks on which he could act). */
    @VisibleForTesting
    public long ticked() {
        return ticked;
    }

    // ---------------------------------------------------------------------------------------------------------

    /** One tick of the brain. */
    void tick(UltronEntity boss, UltronFight fight) {
        ticked++;
        if (!enabled || !(boss.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        trackTarget(boss, fight, level, now);
        LivingEntity target = target(level);
        if (current != null) {
            if (UltronAttacks.tick(boss, fight, this, current, run, ticks, target)) {
                finish(boss, now);
            } else {
                ticks++;
            }
            return;
        }
        if (stagger > 0) {
            UltronMotion.steer(boss, Vec3.ZERO, 0.2);
            countStagger(boss, now);
            return;
        }
        if (boss.state() != UltronState.IDLE) {
            boss.setState(UltronState.IDLE);
        }
        hover(boss, fight, level, target, now);
        if (target == null || now < nextActionAt || !choosing) {
            return;
        }
        UltronAttack next = UltronAttack.choose(situation(boss, fight, target, now), boss.getRandom());
        if (next != null) {
            begin(boss, fight, next, now);
        }
    }

    /**
     * One tick of a body that telekinesis holds, or that a throw keeps still (D-T 4.4): he does nothing, and only a stagger
     * counts down, so that it ends on the tick it would have ended without the grip.
     */
    void tickStill(UltronEntity boss) {
        if (enabled && stagger > 0 && boss.level() instanceof ServerLevel level) {
            countStagger(boss, level.getGameTime());
        }
    }

    /** One tick of the stagger after a Dive; when it is over, the next attack is the Volley, after a short gap. */
    private void countStagger(UltronEntity boss, long now) {
        boss.setState(UltronState.STAGGER);
        if (--stagger == 0) {
            afterStagger = true;
            boss.setState(UltronState.IDLE);
            nextActionAt = now + 10;
        }
    }

    /** Begins an attack now, whatever rests: for the tests and for an operator. Returns false if there is nothing to aim at. */
    public boolean force(UltronEntity boss, UltronFight fight, UltronAttack attack) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return false;
        }
        trackTarget(boss, fight, level, level.getGameTime());
        if (target(level) == null && attack != UltronAttack.CALL) {
            return false;
        }
        current = null;
        stagger = 0;
        begin(boss, fight, attack, level.getGameTime());
        return true;
    }

    private UltronAttack.Situation situation(UltronEntity boss, UltronFight fight, LivingEntity target, long now) {
        boolean allowed = fight.mayCall();
        return new UltronAttack.Situation(boss.bodyIndex(), boss.distanceTo(target), now, last, lastBeams, lastDive, afterStagger,
                allowed, allowed && fight.callUrgent(), readyAt);
    }

    private void begin(UltronEntity boss, UltronFight fight, UltronAttack attack, long now) {
        current = attack;
        ticks = 0;
        run = new Run();
        afterStagger = false;
        readyAt[attack.ordinal()] = now + attack.cooldown();
        if (attack == UltronAttack.BEAMS) {
            lastBeams = now;
        } else if (attack == UltronAttack.DIVE) {
            lastDive = now;
        }
        boss.setState(attack.state());
    }

    private void finish(UltronEntity boss, long now) {
        UltronAttack done = current;
        last = done;
        current = null;
        ticks = 0;
        nextActionAt = now + UltronAttack.GLOBAL_GAP + (boss.bodyIndex() >= 2 ? 10 : 0);
        if (done == UltronAttack.DIVE) {
            stagger = STAGGER_TICKS;
            boss.setState(UltronState.STAGGER);
        } else {
            boss.setState(UltronState.IDLE);
        }
    }

    // ---- hunting ----

    private void trackTarget(UltronEntity boss, UltronFight fight, ServerLevel level, long now) {
        LivingEntity target = target(level);
        boolean valid = target != null && fight.isTargetable(target);
        if (valid && now - targetSince < TARGET_KEEP) {
            return;
        }
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : fight.targets()) {
            double distance = candidate.distanceToSqr(boss);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        if (nearest == null) {
            if (!valid) {
                targetId = null;
                boss.setAimedAt(null);
            }
            return;
        }
        if (!nearest.getUUID().equals(targetId)) {
            targetId = nearest.getUUID();
            boss.setTarget(nearest);
            boss.setAimedAt(nearest);
        }
        targetSince = now;
    }

    // ---- hovering ----

    /**
     * Where he drifts while he is idle: 10 to 14 blocks from the target, three blocks over the ground under him, strafing
     * and bobbing; he backs away from anyone within five blocks unless his fist is ready and the target is within reach of
     * it, and then he closes in.
     */
    private void hover(UltronEntity boss, UltronFight fight, ServerLevel level, @Nullable LivingEntity target, long now) {
        Vec3 position = boss.position();
        double ground = UltronMotion.groundBelow(level, position.x, position.y + 0.1, position.z);
        double wantY = ground + 3.0 + Math.sin(boss.tickCount * 0.08);
        double vertical = Math.max(-0.25, Math.min(0.25, (wantY - position.y) * 0.12));
        Vec3 horizontal = Vec3.ZERO;
        if (target != null) {
            boss.getLookControl().setLookAt(target, 20.0F, 20.0F);
            Vec3 dir = UltronMotion.flatDirection(position, target.position());
            double flat = Math.hypot(target.getX() - position.x, target.getZ() - position.z);
            boolean fist = readyAt[UltronAttack.PUNCH.ordinal()] <= now;
            double radial = 0.0;
            if (flat < 10.0) {
                radial = -(10.0 - flat) * 0.06;
            } else if (flat > 14.0) {
                radial = (flat - 14.0) * 0.06;
            }
            if (fist && flat < 6.0 && flat > UltronAttack.PUNCH_RANGE * 0.6) {
                radial = 0.2; // closing in to punch
            } else if (flat < 5.0) {
                radial -= 0.12;
            }
            if (--strafeUntil <= 0) {
                strafeUntil = 60 + boss.getRandom().nextInt(40);
                strafeSign = boss.getRandom().nextBoolean() ? 1 : -1;
            }
            Vec3 tangent = new Vec3(-dir.z, 0.0, dir.x).scale(strafeSign * 0.25);
            horizontal = dir.scale(radial).add(tangent);
            double speed = horizontal.length();
            double top = Math.max(0.05, boss.getAttributeValue(Attributes.MOVEMENT_SPEED));
            if (speed > top) {
                horizontal = horizontal.scale(top / speed);
            }
        }
        Vec3 wanted = new Vec3(horizontal.x, vertical, horizontal.z);
        UltronMotion.steer(boss, UltronMotion.keepInDisc(position, wanted, fight.arena().center(), fight.armyReach(fight.islandRadius() - 2.0)), 0.05);
    }

    /** Flies toward a point at up to {@code speed} blocks a tick (the fight uses it to bring him to the middle). */
    public void flyTo(UltronEntity boss, Vec3 point, double speed) {
        Vec3 toward = point.subtract(boss.position());
        double distance = toward.length();
        Vec3 wanted = distance < 0.2 ? Vec3.ZERO : toward.scale(Math.min(speed, distance * 0.15) / distance);
        UltronMotion.steer(boss, wanted, 0.08);
    }

    /** Every fighter within reach of a point (a convenience for the attacks): the fight's targets nearer than {@code range}. */
    static List<LivingEntity> within(UltronFight fight, Vec3 point, double range) {
        return fight.targets().stream().filter(e -> e.position().distanceToSqr(point) <= range * range).toList();
    }
}
