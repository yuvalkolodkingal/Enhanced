package com.kolodkingal.enhanced.multiverse;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * The anger of the creatures of the What If universes (3.19.1): in Earth-2149 every creature hunts the living on sight;
 * under the Gamma Bomb every creature, a cow or a chicken too, turns on whoever hurts it (and its kind nearby join in).
 * A creature that has no attack of its own (no attack strength: a cow, a sheep) mauls with {@link Maul}, harder the
 * bigger it is. The goals are not saved: they are given each time the creature comes into its world.
 */
public final class WhatIfGoals {
    /** The priority of the maul: above a frightened animal's running away (its panic). */
    static final int PRIORITY = 0;

    private WhatIfGoals() {
    }

    /** Whether the creature can be given the goals (one that walks, not a fish). */
    static boolean walker(Mob mob) {
        return mob instanceof PathfinderMob && !(mob instanceof WaterAnimal);
    }

    /** Earth-2149: hunts every player on sight. */
    static void hunt(Mob mob) {
        if (!walker(mob)) {
            return;
        }
        PathfinderMob walker = (PathfinderMob) mob;
        if (!(mob instanceof Enemy)) {
            walker.goalSelector.addGoal(PRIORITY, new Maul(walker, 1.15, 1.0F, SoundEvents.ZOMBIE_AMBIENT));
        }
        walker.targetSelector.addGoal(PRIORITY, new NearestAttackableTargetGoal<>(walker, Player.class, true));
        walker.targetSelector.addGoal(PRIORITY, new HurtByTargetGoal(walker));
    }

    /** The Gamma Bomb: turns on whoever hurts it, and its kind round it join in. */
    static void temper(Mob mob) {
        if (!walker(mob) || mob instanceof Enemy) {
            return;
        }
        PathfinderMob walker = (PathfinderMob) mob;
        walker.goalSelector.addGoal(PRIORITY, new Maul(walker, 1.3, 2.0F, SoundEvents.RAVAGER_ROAR));
        walker.targetSelector.addGoal(PRIORITY, new HurtByTargetGoal(walker).setAlertOthers());
    }

    /**
     * A creature runs at its target and hits it when close. One with an attack strength of its own (a wolf, a golem)
     * hits with it ({@link Mob#doHurtTarget}); one without bites or tramples for {@code 2 + 3 × its width}, times
     * {@code strength}. It roars (or groans) when it first goes for its target.
     */
    static final class Maul extends Goal {
        private final PathfinderMob mob;
        private final double speed;
        private final float strength;
        private final SoundEvent cry;
        private int cooldown;
        private int repath;

        Maul(PathfinderMob mob, double speed, float strength, SoundEvent cry) {
            this.mob = mob;
            this.speed = speed;
            this.strength = strength;
            this.cry = cry;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private LivingEntity target() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive() && !(target instanceof Player p && (p.isCreative() || p.isSpectator())) ? target : null;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = target();
            return target != null && mob.distanceToSqr(target) < 40 * 40;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            repath = 0;
            mob.playSound(cry, 0.7F, mob.getVoicePitch() * 0.75F);
            mob.setAggressive(true);
        }

        @Override
        public void stop() {
            mob.setAggressive(false);
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = target();
            if (target == null) {
                return;
            }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--repath <= 0) {
                repath = 8 + mob.getRandom().nextInt(6);
                mob.getNavigation().moveTo(target, speed);
            }
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            double reach = mob.getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + 1.0;
            if (mob.distanceToSqr(target) <= reach * reach && mob.hasLineOfSight(target)) {
                cooldown = 20;
                mob.swing(InteractionHand.MAIN_HAND);
                AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attack != null && attack.getValue() > 0.0) {
                    mob.doHurtTarget(target);
                } else {
                    float damage = (2.0F + 3.0F * Math.min(2.0F, mob.getBbWidth())) * strength;
                    target.hurt(mob.damageSources().mobAttack(mob), damage);
                }
            }
        }
    }
}
