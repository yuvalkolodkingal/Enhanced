package com.kolodkingal.enhanced.event;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AgathaAbilities;
import com.kolodkingal.enhanced.ability.AgathaShield;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.config.AgathaConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Agatha Harkness's own game-bus events (3.3), at the priorities of {@code docs/HEROES-AND-BOSSES.md}: her shield takes the
 * blows meant for her before Wanda's branch does (highest), a projectile that reaches it is turned away before it hits,
 * and magic used on her poisons whoever used it, as the Darkhold does for its holder (lowest, and even for a blow her
 * shield stopped: it is the use of magic on her that is answered). The poison is the book's ({@link
 * DarkholdBook#retaliate}: its numbers, its target and its wait per attacker), so a blow on Agatha holding the book is
 * answered once.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class AgathaEvents {
    /** Her shield, complete, takes the blow (or what it can of it). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShieldDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AgathaAbilities.has(player, HeroAbility.AGATHA_SHIELD)) {
            AgathaShield.guard(player, event);
        }
    }

    /**
     * Whether magic used on this player is answered with the poison because she is Agatha (the book in hand answers by itself);
     * 3.4: a custom hero with her body (the dark magic in her) too.
     */
    private static boolean retaliates(ServerPlayer player) {
        return AgathaAbilities.has(player, HeroAbility.AGATHA_BODY) && AgathaConfig.RETALIATES.get() && !DarkholdBook.inTheRoad(player);
    }

    /** Magic used on her: whoever used it is poisoned. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMagicOnHer(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer agatha) || !retaliates(agatha)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(ModTags.MAGIC_DAMAGE) && source.getEntity() instanceof LivingEntity attacker) {
            DarkholdBook.retaliate(agatha, attacker);
        }
    }

    /**
     * Magic that does no damage answers too: the spell of Loki's Scepter and the mind powers (Confused, Spellbound), and her
     * own Power Drain, laid on Agatha (or on the holder of the Darkhold, who is answered by the book for the others).
     */
    @SubscribeEvent
    public static void onMagicEffect(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer target) || !(event.getEffectSource() instanceof LivingEntity caster)) {
            return;
        }
        var effect = event.getEffectInstance().getEffect();
        boolean drain = effect.is(ModEffects.DRAINED);
        boolean mind = effect.is(ModEffects.CONFUSED) || effect.is(ModEffects.SPELLBOUND);
        if ((drain || mind) && retaliates(target) || drain && DarkholdBook.active(target)) {
            DarkholdBook.retaliate(target, caster);
        }
    }

    /** A projectile that reaches her complete shield is turned to violet sparks before it hits. */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide() || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        if (hit.getEntity() instanceof ServerPlayer agatha && projectile.getOwner() != agatha && AgathaShield.isUp(agatha)
                && !ApocalypseGuard.headingHome(projectile)) {
            event.setCanceled(true);
            AgathaShield.neutralise(agatha, projectile);
        }
    }

    private AgathaEvents() {
    }
}
