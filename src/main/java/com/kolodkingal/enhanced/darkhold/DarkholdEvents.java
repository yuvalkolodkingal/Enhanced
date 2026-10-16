package com.kolodkingal.enhanced.darkhold;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** The game-bus events of the Darkhold (3.2): the retaliation, the hex mark's extra damage, and what is forgotten when it ends. */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class DarkholdEvents {
    /** A hex-marked creature takes more damage from whoever marked it. */
    @SubscribeEvent
    public static void onMarkedDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        float factor = DarkholdBook.markedFactor(target, event.getSource().getEntity());
        if (factor != 1.0F) {
            event.setAmount(event.getAmount() * factor);
        }
    }

    /**
     * Magic used on the holder: whoever used it is poisoned. Heard last, and even for a hit that something else (the Chaos
     * Shield) stops: it is the use of magic on the holder that the book answers, not the harm it did.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMagicOnHolder(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer holder)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(ModTags.MAGIC_DAMAGE) && source.getEntity() instanceof LivingEntity attacker && DarkholdBook.active(holder)) {
            DarkholdBook.retaliate(holder, attacker);
        }
    }

    /**
     * Magic that does no damage answers too, where it is simple: the spell of Loki's Scepter and the mind powers (Confused and
     * Spellbound) laid on the holder by a living creature poison that creature, as a hit of magic does.
     */
    @SubscribeEvent
    public static void onMagicEffect(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer holder) || !(event.getEffectSource() instanceof LivingEntity caster)) {
            return;
        }
        var effect = event.getEffectInstance().getEffect();
        if ((effect.is(ModEffects.CONFUSED) || effect.is(ModEffects.SPELLBOUND)) && DarkholdBook.active(holder)) {
            DarkholdBook.retaliate(holder, caster);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DarkholdBook.onLogout(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DarkholdBook.clear();
    }

    private DarkholdEvents() {
    }
}
