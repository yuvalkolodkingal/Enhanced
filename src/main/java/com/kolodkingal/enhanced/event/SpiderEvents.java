package com.kolodkingal.enhanced.event;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Spider-Man's own game events (DESIGN section 14), so that {@code CommonEvents} needs no edit: the bite of the
 * Radioactive Spider becomes Spider-Man when it ends, a web can never wrap him or a boss, and a hard blow tears a web.
 * (Logging out, changing dimension, dying and the server stopping reach him through {@code HeroLifecycle}.) None of these
 * is ordered against another hero's handler, except that the refusal of a web runs at HIGH (the priority of a boss
 * tag's refusals, integration 5.5).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class SpiderEvents {
    /** A blow of at least this much tears the web off a webbed creature a little. */
    public static final float TEAR_DAMAGE = 6.0F;
    /** How much of the Webbed effect a hard blow takes off. */
    public static final int TEAR_TICKS = 20;

    /**
     * The sickness of the bite ran out: if the player is still there, alive and has no power, Spider-Man wakes in them (in
     * the Homemade Suit). With a power of their own the bite changes nothing.
     */
    @SubscribeEvent
    public static void onBiteExpired(MobEffectEvent.Expired event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getEffectInstance().is(ModEffects.RADIOACTIVE_BITE)) {
            return;
        }
        if (!player.isAlive() || player.hasDisconnected()) {
            return;
        }
        if (Powers.power(player).hasPowers()) {
            fades(player);
            return;
        }
        Powers.awaken(player, Power.SPIDER_MAN);
    }

    /** Milk, or anything else that takes the bite off before it ends, cures it: the spider is gone, nothing awakens. */
    @SubscribeEvent
    public static void onBiteRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffectInstance() != null
                && event.getEffectInstance().is(ModEffects.RADIOACTIVE_BITE) && player.isAlive()) {
            fades(player);
        }
    }

    private static void fades(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.enhanced.bite.fades"), true);
    }

    /**
     * A web never wraps Spider-Man, nor a boss (the {@code enhanced:web_immune} tag). HIGH, like the other refusals by a
     * boss tag (integration 5.5): it is decided before the existing handlers of Spellbound and the rest look at it.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(ModEffects.WEBBED)
                && (event.getEntity().getType().is(ModTags.WEB_IMMUNE) || SpiderServer.isSpiderMan(event.getEntity()))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** A hard blow tears the web: a creature that is being beaten breaks free sooner. */
    @SubscribeEvent
    public static void onWebTorn(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        MobEffectInstance web = victim.getEffect(ModEffects.WEBBED);
        if (web == null || event.getNewDamage() < TEAR_DAMAGE || victim.level().isClientSide) {
            return;
        }
        int left = web.getDuration() - TEAR_TICKS;
        victim.removeEffect(ModEffects.WEBBED);
        if (left > 0) {
            victim.addEffect(new MobEffectInstance(ModEffects.WEBBED, left, web.getAmplifier(), web.isAmbient(), web.isVisible(), web.showIcon()));
        }
    }

    private SpiderEvents() {
    }
}
