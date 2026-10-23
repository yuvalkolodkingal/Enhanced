package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.platform.event.LivingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.MobEffectEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ultron's own game-bus events, at the priorities of {@code docs/HEROES-AND-BOSSES.md}: the damage bonuses
 * (lightning, the Mind Stone beam), the body's own pipeline of a blow, the fighters who fall, and what the hero's body
 * does about it: no fall damage, no Poison or Wither, and Second Life. (The rest of what the fight needs is in the
 * entities and the fight, or is the framework's; the hero's five abilities are {@code UltronAbilities}.)
 */
@Subscriber
public final class UltronEvents {

    /**
     * HIGH: electronics. Lightning does {@code lightningFactor} times to Ultron's bodies, the Sentries and the hero
     * (Thor's beam and arcs, his strike, the weather's), and the Mind Stone beam {@code visionBeamBonus} times to the
     * bodies and the Sentries (the film: Vision, who came from the same stone and the same Stark's code).
     */
    @Subscribe(priority = Priority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        boolean robot = target.getType().is(ModTags.ULTRON_ROBOTS);
        boolean hero = target instanceof ServerPlayer player && Powers.get(player).has(HeroAbility.ULTRON_BODY); // (3.4: his body's)
        if (!robot && !hero) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_LIGHTNING)) {
            event.setAmount(event.getAmount() * UltronConfig.LIGHTNING_FACTOR.get().floatValue());
        } else if (robot && event.getSource().is(ModDamageTypes.MIND_STONE_BEAM)) {
            event.setAmount(event.getAmount() * UltronBossConfig.VISION_BEAM_BONUS.get().floatValue());
        } else if (hero && event.getSource().is(DamageTypeTags.IS_FALL) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // The anti-gravity palms catch him.
            event.setCanceled(true);
        }
    }

    /** HIGH: a machine takes no Poison and no Wither. */
    @Subscribe(priority = Priority.HIGH)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player && Powers.get(player).has(HeroAbility.ULTRON_BODY)
                && (event.getEffectInstance().is(MobEffects.POISON) || event.getEffectInstance().is(MobEffects.WITHER))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /**
     * HIGH, after vanilla's Totem of Undying (which runs inside the blow): the hero's Second Life. If it saves him the
     * death is cancelled, and nothing that follows a death ({@code CommonEvents}, the fight's fallen fighter) hears of
     * it.
     */
    @Subscribe(priority = Priority.HIGH)
    public static void onSecondLife(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Powers.get(player).has(HeroAbility.ULTRON_UPLOAD)
                && UltronDrones.secondLife(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    /**
     * A blow that landed. What the hero struck himself (not his drones' bolts and punches, which are his in everything
     * else) is what his drones go for next; and a player who hurt him, or a drone of his, is fair game for them for a
     * while (when PvP allows it).
     */
    @Subscribe
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        Entity cause = event.getSource().getEntity();
        if (cause instanceof ServerPlayer hero && hero != victim && Powers.get(hero).acts(Power.ULTRON)
                && !UltronDrones.isDroneBlow(event.getSource())
                && !(victim instanceof UltronSentryEntity sentry && hero.getUUID().equals(sentry.getOwnerUUID()))) {
            hero.setLastHurtMob(victim);
        }
        if (cause instanceof ServerPlayer attacker) {
            ServerPlayer hero = victim instanceof ServerPlayer player && Powers.get(player).acts(Power.ULTRON) ? player
                    : victim instanceof UltronSentryEntity sentry && sentry.getOwner() instanceof ServerPlayer owner ? owner : null;
            if (hero != null && hero != attacker) {
                UltronDrones.provoke(hero, attacker);
            }
        }
    }

    /**
     * LOW, after every bonus and defence: a body's own pipeline of a blow: an explosion is halved, a staggered body takes
     * 30 percent more and no hit takes more than {@code maxHitFraction} of its health. (The void and {@code /kill} are
     * not touched.)
     */
    @Subscribe(priority = Priority.LOW)
    public static void onBodyDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof UltronEntity body) {
            event.setAmount(body.adjust(event.getSource(), event.getAmount()));
        }
    }

    /** A fighter fell: the first one earns a line from Ultron. */
    @Subscribe
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (BossFight fight : BossFights.all(player.serverLevel().getServer())) {
            if (fight instanceof UltronFight ultron && ultron.isParticipant(player.getUUID())) {
                ultron.participantFell();
            }
        }
    }

    private UltronEvents() {
    }
}
