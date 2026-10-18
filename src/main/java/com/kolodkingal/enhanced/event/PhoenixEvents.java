package com.kolodkingal.enhanced.event;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.ability.PhoenixBurst;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Phoenix's game-bus events (Jean Grey, DESIGN section 13), with the priorities of the lane guide so that they
 * compose with the older heroes' chain in {@link CommonEvents} and with the other 3.0 heroes: her fire immunity is
 * decided {@code HIGH} (before Wanda's shield and the rest of the chain), who her hosts may go for {@code HIGH}, and the
 * rest at the default.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class PhoenixEvents {

    // ---- fire never hurts her ----

    /** Fire, lava and burning do nothing to a Phoenix (unless the server turned that off); unstoppable damage still does. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFire(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && PhoenixAbilities.isFireImmune(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ---- the Burst against Wanda's Chaos Shield ----

    /**
     * The Burst does not pass the Chaos Shield for free: it wears the shield down by up to what is left of it, and
     * only the rest gets through (a shield it exhausts shatters, as it does for an explosion).
     */
    @SubscribeEvent
    public static void onBurstOnShield(LivingIncomingDamageEvent event) {
        if (event.getSource().is(ModDamageTypes.PHOENIX_BURST) && event.getEntity() instanceof ServerPlayer wanda
                && WandaAbilities.hasShieldUp(wanda)) {
            float rest = event.getAmount() - WandaAbilities.wear(wanda, event.getAmount());
            if (rest <= 0.0F) {
                event.setCanceled(true);
            } else {
                event.setAmount(rest);
            }
        }
    }

    // ---- what she throws touches her mind-power listeners ----

    /**
     * The slam of something the Phoenix threw with telekinesis is her mind power; whoever listens (Apocalypse's shield)
     * is told of it, with what it is worth. (The Burst reports itself, per creature, where it burns it.)
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSlam(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (source.is(ModDamageTypes.TELEKINETIC_SLAM) && source.getEntity() instanceof ServerPlayer phoenix
                && Powers.get(phoenix).has(HeroAbility.PHOENIX_TELEKINESIS)) {
            PhoenixPower.touched(phoenix, event.getEntity(), PhoenixPower.Kind.SLAM, event.getAmount());
        }
    }

    // ---- possession ----

    /**
     * A creature she has taken over takes no target of its own: it goes for what she sends it after and for nothing
     * else (so it never turns on her, her pets, her team or anyone else near her). Letting go of a target is always
     * allowed.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && event.getEntity() instanceof Mob mob && mob.hasEffect(ModEffects.POSSESSED)
                && !PhoenixAbilities.isSteering()) {
            event.setCanceled(true);
        }
    }

    /** The bosses and the Warden cannot be possessed (the effect is refused; Mind Control says why). */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(ModEffects.POSSESSED) && event.getEntity().getType().is(ModTags.TELEPATHY_IMMUNE)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** A possessed creature was cured (milk, a command): it is nobody's any more. */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.POSSESSED.getId())) {
            PhoenixAbilities.hostEnded(event.getEntity());
        }
    }

    /** A possessed creature's time ran out. */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance().is(ModEffects.POSSESSED)) {
            PhoenixAbilities.hostEnded(event.getEntity());
        }
    }

    // ---- death ----

    /** A host that dies leaves her list; a creature her Burst kills leaves a puff of golden ash. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) {
            return;
        }
        if (dead instanceof Mob) {
            PhoenixAbilities.hostEnded(dead);
        }
        if (event.getSource().is(ModDamageTypes.PHOENIX_BURST) && dead.level() instanceof ServerLevel level) {
            double y = dead.getY() + dead.getBbHeight() * 0.5;
            level.sendParticles(ParticleTypes.ASH, dead.getX(), y, dead.getZ(), 30, dead.getBbWidth() * 0.5, dead.getBbHeight() * 0.4, dead.getBbWidth() * 0.5, 0.05);
            level.sendParticles(ParticleTypes.SMALL_FLAME, dead.getX(), y, dead.getZ(), 12, dead.getBbWidth() * 0.4, dead.getBbHeight() * 0.3, dead.getBbWidth() * 0.4, 0.04);
        }
    }

    // ---- the waves ----

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PhoenixBurst.tickLevel(level);
        }
    }

    private PhoenixEvents() {
    }
}
