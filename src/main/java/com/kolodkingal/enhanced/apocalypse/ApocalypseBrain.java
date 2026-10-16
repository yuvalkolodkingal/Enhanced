package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The controller of the boss: it chooses his next attack among the nine, walks him between attacks, and runs the tell,
 * the aim lock and the blow of the one he has chosen ({@link ApocalypseAttacks} says what each does). The fight turns it on
 * only while he fights (not in a set piece, not while he is staggered); everything it does is told to the clients through
 * the entity's synced state, so they can draw each tell.
 *
 * <p><b>Choosing.</b> When he is idle and the global gap has passed (30 ticks in phase 1, 25 in phase 2, 20 in phase 3,
 * halved...: see {@link #gap}) it picks by weight among the attacks that are ready and in range of the nearest fighter
 * ({@link ApocalypseAttack#choose}); his target is the nearest fighter and changes only after 100 ticks unless it dies.
 * <b>Moving.</b> He walks toward his target and keeps 8 to 14 blocks from it, strafing; he closes in when nobody is
 * within 14, backs off when someone is within 5 and he is not about to Backhand; with no path to anybody for 60 ticks he
 * steps through a portal.
 */
public final class ApocalypseBrain {
    /** The target changes only after this many ticks, unless it dies. */
    public static final int TARGET_TICKS = 100;
    /** Nobody within this many blocks for {@link #FAR_TICKS} ticks wants a Portal Step. */
    public static final double NEAR = 10.0;
    public static final int FAR_TICKS = 80;
    public static final int STUCK_TICKS = 60;
    public static final int PORTAL_EVERY = 4;
    /** He keeps this far from his target when idle. */
    public static final double KEEP_MIN = 8.0;
    public static final double KEEP_MAX = 14.0;

    @Nullable
    private ApocalypseAttack attack;
    private int attackTicks;
    private boolean choosing = true;
    private final Map<ApocalypseAttack, Long> readyAt = new EnumMap<>(ApocalypseAttack.class);
    @Nullable
    private ApocalypseAttack last;
    private long gapUntil;
    private boolean afterStagger;
    private boolean inWindow;
    long lastEntomb = -100_000L;
    long lastMindCrush = -100_000L;
    private int sincePortal;
    private int nobodyNear;
    private int stuck;
    @Nullable
    private UUID targetId;
    private long targetSince;
    private int strafeSign = 1;
    private long strafeUntil;
    private RandomSource random;

    // What the attack in progress has worked out (read by ApocalypseAttacks).
    Vec3 aim = Vec3.ZERO;
    final List<Vec3> points = new ArrayList<>();
    final List<LivingEntity> targets = new ArrayList<>();
    @Nullable
    LivingEntity victim;
    @Nullable
    Vec3 exit;
    boolean interrupted;

    // ---- control (the fight, and the tests) ----

    /** Whether it chooses attacks by itself; a test turns it off and starts only what it forces. */
    public void setChoosing(boolean choosing) {
        this.choosing = choosing;
    }

    public boolean isChoosing() {
        return choosing;
    }

    /** Replaces the dice (a test seeds them). */
    public void setRandom(RandomSource random) {
        this.random = random;
    }

    @Nullable
    public ApocalypseAttack attack() {
        return attack;
    }

    public int attackTicks() {
        return attackTicks;
    }

    @Nullable
    public ApocalypseAttack last() {
        return last;
    }

    public boolean isBusy() {
        return attack != null;
    }

    /** The attack that was started last, forgotten by {@link #reset}. */
    public int sincePortal() {
        return sincePortal;
    }

    /** The first attack after a stagger is the Lances. */
    public void setAfterStagger(boolean afterStagger) {
        this.afterStagger = afterStagger;
    }

    /** In a window he attacks at half the rate: every cooldown doubles, and only Lances, Backhand and Spikes are chosen. */
    public void setInWindow(boolean inWindow) {
        this.inWindow = inWindow;
    }

    /** Lets the next choice be made at once (the gap is over), without forgetting the cooldowns. */
    public void clearGap() {
        gapUntil = 0L;
    }

    /** Makes every attack ready. */
    public void clearCooldowns() {
        readyAt.clear();
        gapUntil = 0L;
    }

    /** Whether the attack has recovered. */
    public boolean isReady(ApocalypseAttack attack, long now) {
        return now >= readyAt.getOrDefault(attack, 0L);
    }

    /** Puts an attack on cooldown until {@code until} (a test). */
    public void setReadyAt(ApocalypseAttack attack, long until) {
        readyAt.put(attack, until);
    }

    /** Abandons the attack in progress (a set piece begins, or the shield broke): no blow, no cooldown. */
    public void reset() {
        attack = null;
        attackTicks = 0;
        points.clear();
        targets.clear();
        victim = null;
        exit = null;
        interrupted = false;
    }

    /** A Phoenix's mind touched his: the Mind Crush in its tell is interrupted (the film's astral duel). */
    public boolean interrupt() {
        if (attack == ApocalypseAttack.MIND_CRUSH) {
            interrupted = true;
            return true;
        }
        return false;
    }

    /**
     * Starts an attack now, whatever its cooldown or the gap (the fight's own use, and the tests'). Does nothing if it is
     * already busy or the attack has nothing to aim at.
     *
     * @return whether the attack began
     */
    public boolean force(ApocalypseEntity boss, ApocalypseFight fight, ApocalypseAttack next) {
        if (attack != null) {
            return false;
        }
        List<ServerPlayer> foes = fight.foes();
        LivingEntity target = pickTarget(foes, boss.level().getGameTime());
        if (target == null) {
            return false;
        }
        return begin(boss, fight, next, target, boss.level().getGameTime());
    }

    // ---- one tick ----

    /** One tick of the controller. */
    public void tick(ApocalypseEntity boss, ApocalypseFight fight) {
        ServerLevel level = (ServerLevel) boss.level();
        long now = level.getGameTime();
        List<ServerPlayer> foes = fight.foes();
        LivingEntity target = pickTarget(foes, now);
        boss.setTracked(target);
        count(boss, foes);
        if (attack != null) {
            run(boss, fight, target, now);
            return;
        }
        if (boss.state().isTell() || boss.state() == ApocalypseState.BACKHAND) {
            boss.setState(ApocalypseState.IDLE);
        }
        if (target != null) {
            ApocalypseAttacks.face(boss, target.position());
        }
        move(boss, fight, target, now);
        if (!choosing || target == null || now < gapUntil) {
            return;
        }
        ApocalypseAttack next = ApocalypseAttack.choose(choice(boss, fight, foes, target, now), random(boss));
        if (next != null) {
            begin(boss, fight, next, target, now);
        }
    }

    private RandomSource random(ApocalypseEntity boss) {
        return random != null ? random : boss.getRandom();
    }

    /** What he knows when he chooses. */
    private ApocalypseAttack.Choice choice(ApocalypseEntity boss, ApocalypseFight fight, List<ServerPlayer> foes, LivingEntity target, long now) {
        double nearest = Double.MAX_VALUE;
        boolean sight = false;
        for (ServerPlayer foe : foes) {
            nearest = Math.min(nearest, flatDistance(boss, foe));
            sight |= flatDistance(boss, foe) <= 28.0 && boss.hasLineOfSight(foe);
        }
        boolean targetSeen = boss.hasLineOfSight(target);
        boolean wantsPortal = nobodyNear >= FAR_TICKS || sincePortal >= PORTAL_EVERY || stuck >= STUCK_TICKS;
        exit = wantsPortal ? ApocalypseAttacks.findExit(boss, fight, foes) : null;
        return new ApocalypseAttack.Choice(fight.attackPhase(), nearest, inWindow, afterStagger, last, now, lastEntomb, lastMindCrush, sight,
                exit != null, a -> isReady(a, now) && (a != ApocalypseAttack.LANCES || targetSeen));
    }

    private void count(ApocalypseEntity boss, List<ServerPlayer> foes) {
        boolean near = false;
        for (ServerPlayer foe : foes) {
            near |= flatDistance(boss, foe) <= NEAR;
        }
        nobodyNear = near || foes.isEmpty() ? 0 : nobodyNear + 1;
    }

    static double flatDistance(LivingEntity a, LivingEntity b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    /** The nearest fighter; it changes only after {@link #TARGET_TICKS} unless it is gone. */
    @Nullable
    private LivingEntity pickTarget(List<ServerPlayer> foes, long now) {
        if (foes.isEmpty()) {
            targetId = null;
            return null;
        }
        if (targetId != null && now - targetSince < TARGET_TICKS) {
            for (ServerPlayer foe : foes) {
                if (foe.getUUID().equals(targetId)) {
                    return foe;
                }
            }
        }
        ServerPlayer best = foes.get(0);
        if (foes.size() > 1) {
            // (nearest to the boss: the list is sorted by the fight)
            best = foes.get(0);
        }
        if (!best.getUUID().equals(targetId)) {
            targetId = best.getUUID();
            targetSince = now;
        } else if (now - targetSince >= TARGET_TICKS) {
            targetSince = now;
        }
        return best;
    }

    // ---- walking ----

    private void move(ApocalypseEntity boss, ApocalypseFight fight, @Nullable LivingEntity target, long now) {
        Vec3 centre = fight.arena().center();
        double leash = Math.max(2.0, fight.arena().containRadius() - 2.0 - 2.0);
        if (Math.hypot(boss.getX() - centre.x, boss.getZ() - centre.z) > leash) {
            boss.getNavigation().moveTo(centre.x, boss.getY(), centre.z, 1.0);
            return;
        }
        if (target == null) {
            boss.getNavigation().stop();
            return;
        }
        double distance = flatDistance(boss, target);
        boolean backhandSoon = distance <= ApocalypseAttack.BACKHAND.maxRange() + 1.0 && isReady(ApocalypseAttack.BACKHAND, now);
        Vec3 toTarget = SandControl.flat(boss.position(), target.position());
        Vec3 goal;
        if (distance > KEEP_MAX) {
            goal = target.position();
            stuck = boss.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.0) ? 0 : stuck + 1;
            return;
        }
        if (distance < 5.0 && !backhandSoon) {
            goal = boss.position().add(toTarget.scale(-6.0));
        } else if (distance < KEEP_MIN) {
            goal = boss.position().add(toTarget.scale(-3.0)).add(-toTarget.z * 3.0 * strafeSign, 0.0, toTarget.x * 3.0 * strafeSign);
        } else {
            if (now >= strafeUntil) {
                strafeSign = random(boss).nextBoolean() ? 1 : -1;
                strafeUntil = now + 40;
            }
            goal = boss.position().add(-toTarget.z * 4.0 * strafeSign, 0.0, toTarget.x * 4.0 * strafeSign);
        }
        // Not out of the circle he fights in.
        double dx = goal.x - centre.x;
        double dz = goal.z - centre.z;
        double out = Math.hypot(dx, dz);
        if (out > leash) {
            goal = new Vec3(centre.x + dx / out * leash, goal.y, centre.z + dz / out * leash);
        }
        boss.getNavigation().moveTo(goal.x, goal.y, goal.z, 0.9);
        stuck = 0;
    }

    // ---- attacking ----

    private boolean begin(ApocalypseEntity boss, ApocalypseFight fight, ApocalypseAttack next, LivingEntity target, long now) {
        points.clear();
        targets.clear();
        victim = null;
        interrupted = false;
        if (next == ApocalypseAttack.PORTAL_STEP) {
            if (exit == null) {
                exit = ApocalypseAttacks.findExit(boss, fight, fight.foes());
            }
        } else {
            exit = null;
        }
        if (!ApocalypseAttacks.begin(this, boss, fight, next, target)) {
            reset();
            return false;
        }
        attack = next;
        attackTicks = 0;
        boss.getNavigation().stop();
        boss.setState(next.state());
        return true;
    }

    private void run(ApocalypseEntity boss, ApocalypseFight fight, @Nullable LivingEntity target, long now) {
        ApocalypseAttack current = attack;
        attackTicks++;
        boss.getNavigation().stop();
        if (interrupted) {
            ApocalypseAttacks.interrupted(boss, fight);
            finish(boss, fight, current, now);
            return;
        }
        int lock = current.lockTick();
        if (lock >= 0 && attackTicks <= lock) {
            ApocalypseAttacks.track(this, boss, fight, current, target);
        }
        if (attackTicks == lock) {
            ApocalypseAttacks.lock(this, boss, fight, current);
        }
        if (attackTicks >= current.tellTicks()) {
            ApocalypseAttacks.fire(this, boss, fight, current);
            finish(boss, fight, current, now);
        }
    }

    private void finish(ApocalypseEntity boss, ApocalypseFight fight, ApocalypseAttack done, long now) {
        readyAt.put(done, now + ApocalypseBossConfig.slower(done.cooldown()) * (inWindow ? 2L : 1L));
        last = done;
        gapUntil = now + ApocalypseBossConfig.slower(gap(fight.attackPhase()));
        afterStagger = false;
        if (done == ApocalypseAttack.ENTOMB) {
            lastEntomb = now;
        } else if (done == ApocalypseAttack.MIND_CRUSH) {
            lastMindCrush = now;
        }
        sincePortal = done == ApocalypseAttack.PORTAL_STEP ? 0 : sincePortal + 1;
        if (done == ApocalypseAttack.PORTAL_STEP) {
            stuck = 0;
            nobodyNear = 0;
        }
        reset();
        boss.setState(ApocalypseState.IDLE);
    }

    /** The global gap between two attacks: 30 ticks in phase 1, 25 in phase 2, 20 in phase 3. */
    public static int gap(int phase) {
        return Mth.clamp(35 - 5 * phase, 20, 30);
    }
}
