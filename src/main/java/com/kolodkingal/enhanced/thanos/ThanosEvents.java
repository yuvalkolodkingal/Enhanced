package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ThanosAbilities;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The Thanos lane's own events: it registers the fight and the summoning when the mod loads, and gives the three
 * effects of the stones their rules. A petrified creature takes only part of the damage and can neither attack nor use
 * anything; a creature in the Soul World is hurt by nothing; and the three effects do not take on the creatures that
 * are immune to being held ({@code enhanced:spellbound_immune}: the bosses). For the hero it listens to the two things
 * his tick cannot see: the damage he takes (the Titan healing factor waits, a hard blow tears the Soul Drain's chain)
 * and the projectile that is too fast for the Reality Veil's tick.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class ThanosEvents {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ThanosSummon::register);
    }

    /**
     * The damage a creature of the stones takes: nothing at all in the Soul World (unless it cannot be stopped), and in
     * stone only {@code petrifyDamageFactor} of it. Late, so that every defence has already had its say.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onStoneDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (victim.hasEffect(ModEffects.SOULWORLD)) {
            event.setCanceled(true);
        } else if (victim.hasEffect(ModEffects.PETRIFIED)) {
            event.setAmount(event.getAmount() * ThanosConfig.PETRIFY_DAMAGE_FACTOR.get().floatValue());
        }
    }

    /** Thanos the hero was hurt: his healing factor waits a while, and a hard enough blow tears a chain. */
    @SubscribeEvent
    public static void onHeroHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && Powers.get(player).acts(Power.THANOS)) {
            ThanosAbilities.hurt(player, event.getNewDamage());
        }
    }

    /** A projectile about to hit Thanos while the Reality Veil is drawn is a bubble that pops instead. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof ServerPlayer player && ThanosAbilities.veilTakes(player, event.getProjectile())) {
            event.setCanceled(true);
        }
    }

    /** Stone and time do not take on what cannot be held. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getType().is(ModTags.SPELLBOUND_IMMUNE)) {
            return;
        }
        if (event.getEffectInstance().is(ModEffects.PETRIFIED) || event.getEffectInstance().is(ModEffects.TIME_LOCKED)
                || event.getEffectInstance().is(ModEffects.SOULWORLD)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Whether the stones hold the creature still: it cannot strike or use anything. */
    private static boolean held(LivingEntity entity) {
        return entity.hasEffect(ModEffects.PETRIFIED) || entity.hasEffect(ModEffects.SOULWORLD);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (held(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (held(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (held(event.getEntity())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (held(event.getEntity())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (held(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (held(event.getEntity())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private ThanosEvents() {
    }
}
