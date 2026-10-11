package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Ultron's own game-bus events, at the priorities of {@code docs/3.0-fragments/README.md}: the damage bonuses (lightning,
 * the Mind Stone beam), the body's own pipeline of a blow, and the fighters who fall. (The rest of what the fight needs
 * is in the entities and the fight, or is the framework's.)
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class UltronEvents {

    /**
     * HIGH: electronics. Lightning does {@code lightningFactor} times to Ultron's bodies, the Sentries and the hero
     * (Thor's beam and arcs, his strike, the weather's), and the Mind Stone beam {@code visionBeamBonus} times to the
     * bodies and the Sentries (the film: Vision, who came from the same stone and the same Stark's code).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        boolean robot = target.getType().is(ModTags.ULTRON_ROBOTS);
        boolean hero = target instanceof ServerPlayer player && Powers.is(player, Power.ULTRON);
        if (!robot && !hero) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.IS_LIGHTNING)) {
            event.setAmount(event.getAmount() * UltronConfig.LIGHTNING_FACTOR.get().floatValue());
        } else if (robot && event.getSource().is(ModDamageTypes.MIND_STONE_BEAM)) {
            event.setAmount(event.getAmount() * UltronBossConfig.VISION_BEAM_BONUS.get().floatValue());
        }
    }

    /**
     * LOW, after every bonus and defence: a body's own pipeline of a blow: an explosion is halved, a staggered body takes
     * 30 percent more and no hit takes more than {@code maxHitFraction} of its health. (The void and {@code /kill} are
     * not touched.)
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBodyDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof UltronEntity body) {
            event.setAmount(body.adjust(event.getSource(), event.getAmount()));
        }
    }

    /** A fighter fell: the first one earns a line from Ultron. */
    @SubscribeEvent
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
