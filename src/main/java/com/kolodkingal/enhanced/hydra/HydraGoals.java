package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * The goals of a Hydra soldier (3.6, the plan's 15.3), cheap and throttled: the sight check runs every 4 ticks per
 * soldier (staggered), paths are asked for again only every 10 ticks.
 *
 * <p>In order: swim; open doors on his way (the keycard, {@link HydraNavigation}); run to a radio console to raise
 * the alarm; fight; follow the one who turned him; go and look where he heard something; his duty (stand at his post,
 * or walk his patrol's loop with a pause of 2-4 s at each corner; back to it after a fight); look round in slow
 * sweeps. Targets: what attacks him (not his own side), an intruder he {@linkplain HydraSenses#sees sees}, a traitor
 * in sight; a turned soldier, the Hydra near his leader and whatever his leader fights.
 */
public final class HydraGoals {
    /** How often (ticks) a soldier looks for someone to fight (a goal is asked every other tick). */
    public static final int SIGHT_PERIOD = 4;
    /** How near a traitor must be for the others to see him for what he is. */
    public static final double TRAITOR_RANGE = 16.0;

    static void addTo(HydraSoldierEntity soldier) {
        soldier.goalSelector.addGoal(0, new FloatGoal(soldier));
        soldier.goalSelector.addGoal(1, new HydraNavigation.DoorGoal(soldier));
        soldier.goalSelector.addGoal(2, new RunToConsole(soldier));
        soldier.goalSelector.addGoal(3, new Fight(soldier));
        soldier.goalSelector.addGoal(4, new FollowLeader(soldier));
        soldier.goalSelector.addGoal(5, new Investigate(soldier));
        soldier.goalSelector.addGoal(6, new Duty(soldier));
        soldier.goalSelector.addGoal(7, new Sweep(soldier));
        soldier.targetSelector.addGoal(1, new Retaliate(soldier));
        soldier.targetSelector.addGoal(2, new Spot(soldier));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Targets

    /** He fights back whoever hurt him (never one of his side), and a turned soldier whoever hurt his leader. */
    static final class Retaliate extends Goal {
        private final HydraSoldierEntity soldier;
        private int lastStamp;

        Retaliate(HydraSoldierEntity soldier) {
            this.soldier = soldier;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity attacker = soldier.getLastHurtByMob();
            int stamp = soldier.getLastHurtByMobTimestamp();
            if (attacker == null || stamp == lastStamp || PhoenixAbilities.isHost(soldier)) {
                return false;
            }
            lastStamp = stamp;
            if (attacker == soldier.getTarget() || !attacker.isAlive() || soldier.isAlliedTo(attacker)) {
                return false;
            }
            if (soldier.isTurned() && (attacker instanceof Player || soldier.turnedBy(attacker))) {
                return false;
            }
            return !(attacker instanceof Player p && (p.isCreative() || p.isSpectator()));
        }

        @Override
        public void start() {
            soldier.setTarget(soldier.getLastHurtByMob());
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    /** Every few ticks with nobody to fight: an intruder he sees, or a traitor in sight. */
    static final class Spot extends Goal {
        private final HydraSoldierEntity soldier;
        @Nullable
        private LivingEntity found;
        /** How often canUse was asked (the selector asks every other tick): it looks every other time. */
        private int asked;

        Spot(HydraSoldierEntity soldier) {
            this.soldier = soldier;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if ((++asked + soldier.getId()) % (SIGHT_PERIOD / 2) != 0 || soldier.getTarget() != null
                    || soldier.isLeaving() || soldier.isFrozen() || PhoenixAbilities.isHost(soldier)
                    || soldier.steered()) {
                return false;
            }
            found = soldier.isTurned() ? forLeader(soldier) : intruder(soldier);
            return found != null;
        }

        @Override
        public void start() {
            soldier.setTarget(found);
            found = null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    /** An intruder in sight (the nearest), or a traitor of his side's within {@link #TRAITOR_RANGE} in sight. */
    @Nullable
    static LivingEntity intruder(HydraSoldierEntity soldier) {
        double range = HydraSenses.sightRange();
        List<LivingEntity> near = soldier.level().getEntitiesOfClass(LivingEntity.class,
                soldier.getBoundingBox().inflate(range), e -> e != soldier && e.isAlive());
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(soldier)));
        for (LivingEntity e : near) {
            if (e instanceof Player player) {
                if (HydraSenses.sees(soldier, player)) {
                    return player;
                }
            } else if (HydraLoyalty.isHydra(e) && HydraLoyalty.isTraitor(e)
                    && e.distanceToSqr(soldier) <= TRAITOR_RANGE * TRAITOR_RANGE && soldier.hasLineOfSight(e)) {
                return e;
            }
        }
        return null;
    }

    /** For a turned soldier: what his leader fights or what hurt him, else the nearest loyal Hydra near the leader. */
    @Nullable
    static LivingEntity forLeader(HydraSoldierEntity soldier) {
        LivingEntity leader = soldier.leader();
        if (leader == null) {
            return null;
        }
        LivingEntity hurter = leader.getLastHurtByMob();
        if (hurter != null && hurter.isAlive() && !(hurter instanceof Player) && !soldier.isAlliedTo(hurter)
                && hurter.distanceToSqr(leader) < 24 * 24) {
            return hurter;
        }
        LivingEntity hit = leader.getLastHurtMob();
        if (hit != null && hit.isAlive() && !(hit instanceof Player) && !soldier.isAlliedTo(hit)
                && leader.tickCount - leader.getLastHurtMobTimestamp() < 100) {
            return hit;
        }
        return soldier.level().getEntitiesOfClass(LivingEntity.class, leader.getBoundingBox().inflate(TRAITOR_RANGE),
                e -> HydraLoyalty.isHydra(e) && e != soldier && !HydraLoyalty.isTraitor(e) && e.isAlive()
                        && soldier.hasLineOfSight(e))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(soldier))).orElse(null);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Moving

    /** The alarm's runner goes to the nearest radio console he can reach; there, he raises the alarm. */
    static final class RunToConsole extends Goal {
        private final HydraSoldierEntity soldier;
        private int repath;

        RunToConsole(HydraSoldierEntity soldier) {
            this.soldier = soldier;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return HydraAlarm.isRunner(soldier);
        }

        @Override
        public boolean canContinueToUse() {
            return HydraAlarm.isRunner(soldier);
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            soldier.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            HydraOutposts.Outpost outpost = soldier.outpost();
            if (outpost == null || !(soldier.level() instanceof ServerLevel level)) {
                return;
            }
            BlockPos spot = HydraAlarm.runSpot(outpost, soldier);
            soldier.setAct(HydraSoldierEntity.Act.RUN);
            if (soldier.distanceToSqr(Vec3.atBottomCenterOf(spot)) <= HydraAlarm.CONSOLE_REACH
                    * HydraAlarm.CONSOLE_REACH) {
                soldier.getNavigation().stop();
                soldier.setAct(HydraSoldierEntity.Act.RADIO);
                HydraAlarm.runnerArrived(level, outpost, soldier);
                return;
            }
            if (--repath <= 0) {
                repath = 10;
                net.minecraft.world.level.pathfinder.Path path = soldier.getNavigation().createPath(spot, 1);
                if (path == null || !path.canReach() || !soldier.getNavigation().moveTo(path, 1.25)) {
                    HydraAlarm.runnerFailed(outpost, soldier);
                }
            }
        }
    }

    /** The fight itself, by what he carries. */
    static final class Fight extends Goal {
        private final HydraSoldierEntity soldier;
        private int repath;
        private int strafeTime;
        private boolean strafeLeft;

        Fight(HydraSoldierEntity soldier) {
            this.soldier = soldier;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = soldier.getTarget();
            return target != null && target.isAlive() && !soldier.isFrozen();
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            soldier.getNavigation().stop();
            soldier.getMoveControl().strafe(0.0F, 0.0F);
            soldier.cancelAim();
            soldier.setAggressive(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = soldier.getTarget();
            if (target == null) {
                return;
            }
            soldier.setAggressive(true);
            double distance = soldier.distanceTo(target);
            boolean sees = soldier.getSensing().hasLineOfSight(target);
            soldier.getLookControl().setLookAt(target, 30.0F, 30.0F);
            switch (soldier.arms()) {
                case BATON -> {
                    chase(target, 1.2);
                    soldier.batonTick(target, distance);
                }
                case LONG_RIFLE -> {
                    soldier.getNavigation().stop();
                    soldier.sniperTick(target, distance, sees);
                }
                case PISTOL -> {
                    keepRange(target, distance, sees);
                    soldier.pistolTick(target, distance, sees);
                }
                default -> {
                    keepRange(target, distance, sees);
                    soldier.rifleTick(target, distance, sees);
                }
            }
        }

        private void chase(LivingEntity target, double speed) {
            if (--repath <= 0) {
                repath = 10;
                soldier.getNavigation().moveTo(target, speed);
            }
        }

        /** Riflemen and the officer: 8-16 blocks, strafing like a skeleton; in cover when badly hurt. */
        private void keepRange(LivingEntity target, double distance, boolean sees) {
            if (soldier.wantsCover()) {
                BlockPos spot = soldier.coverSpot();
                if (spot == null) {
                    spot = HydraAlarm.coverFor(soldier, target);
                    soldier.setCoverSpot(spot);
                }
                if (spot != null) {
                    soldier.getMoveControl().strafe(0.0F, 0.0F);
                    if (soldier.distanceToSqr(Vec3.atBottomCenterOf(spot)) > 1.0) {
                        if (--repath <= 0) {
                            repath = 10;
                            soldier.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.2);
                        }
                    } else {
                        soldier.getNavigation().stop();
                    }
                    return;
                }
            }
            if (!sees || distance > HydraSoldierEntity.KEEP_MAX) {
                soldier.getMoveControl().strafe(0.0F, 0.0F);
                chase(target, 1.1);
                return;
            }
            soldier.getNavigation().stop();
            if (++strafeTime >= 30) {
                strafeTime = 0;
                if (soldier.getRandom().nextFloat() < 0.4F) {
                    strafeLeft = !strafeLeft;
                }
            }
            float forward = distance < HydraSoldierEntity.KEEP_MIN ? -0.5F : 0.0F;
            soldier.getMoveControl().strafe(forward, strafeLeft ? 0.4F : -0.4F);
            soldier.setYRot(Mth.rotateIfNecessary(soldier.getYRot(), soldier.getYHeadRot(), 30.0F));
        }
    }

    /** A turned soldier with nobody to fight stays near his leader. */
    static final class FollowLeader extends Goal {
        private final HydraSoldierEntity soldier;
        private int repath;

        FollowLeader(HydraSoldierEntity soldier) {
            this.soldier = soldier;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity leader = soldier.leader();
            return leader != null && soldier.getTarget() == null && soldier.distanceToSqr(leader) > 16.0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity leader = soldier.leader();
            return leader != null && soldier.getTarget() == null && soldier.distanceToSqr(leader) > 6.0;
        }

        @Override
        public void tick() {
            LivingEntity leader = soldier.leader();
            if (leader == null) {
                return;
            }
            soldier.getLookControl().setLookAt(leader, 10.0F, 10.0F);
            if (--repath <= 0) {
                repath = 10;
                if (soldier.distanceToSqr(leader) > 32 * 32) {
                    soldier.teleportTo(leader.getX(), leader.getY(), leader.getZ());
                } else {
                    soldier.getNavigation().moveTo(leader, 1.15);
                }
            }
        }

        @Override
        public void stop() {
            soldier.getNavigation().stop();
        }
    }

    /** He heard or glimpsed something: he walks there and looks round. */
    static final class Investigate extends Goal {
        private final HydraSoldierEntity soldier;
        private int repath;

        Investigate(HydraSoldierEntity soldier) {
            this.soldier = soldier;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return soldier.getTarget() == null && soldier.alertPos() != null && !soldier.isLeaving()
                    && soldier.arms() != HydraSoldierEntity.Arms.LONG_RIFLE;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            Vec3 at = soldier.alertPos();
            if (at == null) {
                return;
            }
            soldier.setAct(HydraSoldierEntity.Act.ALERT);
            soldier.getLookControl().setLookAt(at.x, at.y + 1.0, at.z);
            if (soldier.distanceToSqr(at) < 4.0) {
                soldier.getNavigation().stop();
                return;
            }
            if (--repath <= 0) {
                repath = 10;
                soldier.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
            }
        }

        @Override
        public void stop() {
            soldier.getNavigation().stop();
        }
    }

    /**
     * His duty with nobody to fight: back to his post and stand there, or walk his patrol's loop (a pause of 40-80
     * ticks at each corner); a reinforcement goes back toward the road and vanishes unseen when it is over.
     */
    static final class Duty extends Goal {
        private final HydraSoldierEntity soldier;
        private int corner = -1;
        private int pause;
        private int repath;

        Duty(HydraSoldierEntity soldier) {
            this.soldier = soldier;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return soldier.getTarget() == null && !soldier.isTurned() && soldier.outpost() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return false;
        }

        @Override
        public void tick() {
            if (soldier.isLeaving()) {
                HydraOutposts.Outpost outpost = soldier.outpost();
                if (outpost != null && --repath <= 0) {
                    repath = 20;
                    BlockPos out = HydraAlarm.roadPoint((ServerLevel) soldier.level(), outpost);
                    soldier.getNavigation().moveTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5, 1.0);
                }
                return;
            }
            List<BlockPos> route = soldier.route();
            if (!route.isEmpty()) {
                patrol(route);
                return;
            }
            BlockPos post = soldier.postPos();
            if (post == null) {
                return;
            }
            if (soldier.distanceToSqr(Vec3.atBottomCenterOf(post)) > 1.5) {
                if (--repath <= 0) {
                    repath = 20;
                    soldier.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 0.9);
                }
            } else if (!soldier.getNavigation().isDone()) {
                soldier.getNavigation().stop();
            }
        }

        private void patrol(List<BlockPos> route) {
            if (corner < 0 || corner >= route.size()) {
                corner = Math.floorMod(soldier.routeStart(), route.size());
            }
            BlockPos at = route.get(corner);
            if (soldier.distanceToSqr(Vec3.atBottomCenterOf(at)) <= 2.25) {
                if (pause <= 0) {
                    pause = 40 + soldier.getRandom().nextInt(41);
                }
                if (--pause <= 0) {
                    corner = (corner + 1) % route.size();
                    repath = 0;
                }
                return;
            }
            if (--repath <= 0 || soldier.getNavigation().isDone()) {
                repath = 20;
                soldier.getNavigation().moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.7);
            }
        }

        @Override
        public void stop() {
            soldier.getNavigation().stop();
        }
    }

    /** At his post he looks round in slow sweeps, outward from the compound. */
    static final class Sweep extends Goal {
        private final HydraSoldierEntity soldier;
        private int time;

        Sweep(HydraSoldierEntity soldier) {
            this.soldier = soldier;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return soldier.getTarget() == null && soldier.getNavigation().isDone() && soldier.alertPos() == null
                    && !soldier.steered();
        }

        @Override
        public void tick() {
            time++;
            BlockPos home = soldier.home();
            float out;
            if (home != null && soldier.blockPosition().distSqr(home) > 4) {
                out = soldier.yawTo(Vec3.atCenterOf(home)) + 180.0F;
            } else {
                out = soldier.yBodyRot;
            }
            float yaw = out + 55.0F * Mth.sin(time * 0.025F + soldier.getId());
            Vec3 eye = soldier.getEyePosition();
            Vec3 dir = Vec3.directionFromRotation(0.0F, yaw);
            soldier.getLookControl().setLookAt(eye.x + dir.x * 8.0, eye.y, eye.z + dir.z * 8.0, 6.0F, 30.0F);
        }
    }

    private HydraGoals() {
    }
}
