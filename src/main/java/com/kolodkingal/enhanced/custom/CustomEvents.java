package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.modules.EnergyPowers;
import com.kolodkingal.enhanced.custom.modules.MindPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * The custom heroes' defences and the rules of their controls (3.3): what a hero's powers spare him (each element its
 * own, the falls his own powers cause, Phasing's "ghost"), the Force Field and Energy Absorption taking hits, projectiles
 * stopped or sent back by the field, and the creatures that may not pick him (or his side) as a target. At the normal
 * priority, after the bosses' guards (docs/HEROES-AND-BOSSES.md section 3).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class CustomEvents {

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !CustomHeroes.is(player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (spares(player, source)) {
            event.setCanceled(true);
            return;
        }
        if (CustomHeroes.state(player).field() && fromOutside(source) && EnergyPowers.fieldTakes(player, event.getAmount())) {
            event.setCanceled(true);
            return;
        }
        if (source.is(DamageTypeTags.IS_FALL) && CustomHeroes.pick(player, PowerModule.WIND) != null) {
            CustomHero.Pick wind = CustomHeroes.pick(player, PowerModule.WIND);
            event.setAmount(event.getAmount() * (1F - ModuleStats.FALL_GUARD.value(wind.strength())));
        }
        if (CustomHeroes.has(player, PowerModule.ABSORPTION, "energy") && EnergyPowers.isEnergy(source)) {
            float left = EnergyPowers.absorb(player, event.getAmount());
            if (left <= 0) {
                event.setCanceled(true);
            } else {
                event.setAmount(left);
            }
        }
    }

    /** What a custom hero's own powers make him immune to. */
    public static boolean spares(ServerPlayer player, DamageSource source) {
        if (CustomHeroes.isOn(player, PowerModule.PHASING) && CustomHeroes.has(player, PowerModule.PHASING, "ghost")) {
            return true;
        }
        if (source.is(DamageTypeTags.IS_FIRE) && CustomHeroes.pick(player, PowerModule.FIRE) != null) {
            return true;
        }
        if (source.is(DamageTypeTags.IS_FREEZING) && CustomHeroes.pick(player, PowerModule.ICE) != null) {
            return true;
        }
        if (source.is(DamageTypeTags.IS_LIGHTNING) && CustomHeroes.pick(player, PowerModule.LIGHTNING) != null) {
            return true;
        }
        if (source.is(DamageTypes.DROWN) && CustomHeroes.pick(player, PowerModule.WATER) != null) {
            return true;
        }
        if (source.is(DamageTypeTags.IS_FALL) && (CustomHeroes.fallGuarded(player)
                || CustomHeroes.isOn(player, PowerModule.WALL_CRAWL) && com.kolodkingal.enhanced.custom.modules.MovePowers.touchesWall(player))) {
            return true;
        }
        return false;
    }

    /** A hit from outside the hero (a blow, a shot, a blast, a beam), which a Force Field stops; not hunger, falls or poison. */
    private static boolean fromOutside(DamageSource source) {
        return source.getEntity() != null || source.getDirectEntity() != null || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_LIGHTNING);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ServerPlayer player) || projectile.getOwner() == player || !CustomHeroes.is(player)) {
            return;
        }
        if (CustomHeroes.isOn(player, PowerModule.PHASING) && CustomHeroes.has(player, PowerModule.PHASING, "ghost")) {
            event.setCanceled(true); // it passes straight through him
            return;
        }
        if (CustomHeroes.state(player).field()) {
            event.setCanceled(true);
            EnergyPowers.fieldMeets(player, projectile);
        }
    }

    /** A ghost cannot strike anyone (Phasing's "phase" is Vision's, which the shared events already stop). */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CustomHeroes.isOn(player, PowerModule.PHASING)
                && CustomHeroes.has(player, PowerModule.PHASING, "ghost")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) {
            return;
        }
        if (!MindPowers.mayTarget(mob, target)) {
            event.setCanceled(true);
            return;
        }
        if (target instanceof ServerPlayer hero && CustomHeroes.is(hero)) {
            if (CustomHeroes.isOn(hero, PowerModule.INVISIBILITY) && CustomHeroes.has(hero, PowerModule.INVISIBILITY, "full")
                    && !(mob.getLastHurtByMob() == hero && mob.tickCount - mob.getLastHurtByMobTimestamp() <= 60)) {
                event.setCanceled(true);
                return;
            }
            if (mob.getTarget() != hero) {
                MindPowers.danger(hero, mob);
            }
        }
    }

    private CustomEvents() {
    }
}
