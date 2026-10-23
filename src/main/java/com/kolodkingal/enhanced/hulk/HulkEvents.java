package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.platform.event.LivingChangeTargetEvent;
import com.kolodkingal.enhanced.platform.event.LivingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.MobEffectEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Hulk's event hooks (3.6). Priorities as {@code docs/HEROES-AND-BOSSES.md} lists them:
 * <ul> <li>{@link LivingIncomingDamageEvent}, HIGH: no fall damage from his own leaps (the leap's guard); NORMAL: his
 * rage adds to the blows of his fists ({@link HulkRage#damageFactor}; his moves add it themselves) and a blow of his
 * fists feeds his rage;</li> <li>{@link LivingDamageEvent.Post}: what he took (after armour) feeds his rage, or
 * Banner's anger, and the healing factor waits;</li> <li>{@link MobEffectEvent.Applicable}: while he is enraged, Nausea
 * and the Mind Stone's confusion slide off him ("Hulk too angry.");</li> <li>{@link LivingChangeTargetEvent}: a
 * Frightened creature does not turn on the one it fears.</li> </ul>
 */
@Subscriber
public final class HulkEvents {
    /** The Hulk's own damage types: his moves' rage is in their numbers already. */
    private static final Set<net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType>> OWN =
            Set.of(
            ModDamageTypes.HULK_SMASH, ModDamageTypes.HULK_GRAB, ModDamageTypes.HULK_CLAP, ModDamageTypes.HULK_LEAP,
            ModDamageTypes.HULK_BOULDER, ModDamageTypes.HULK_CHARGE);
    /** Until when (game time) a leap of his guards him from fall damage. */
    private static final Map<UUID, Long> FALL_GUARD = new HashMap<>();
    /** The last time "Hulk too angry." was said to each (once a second at most). */
    private static final Map<UUID, Long> SAID = new HashMap<>();

    /** His own leap threw him: no fall damage for {@code ticks}. */
    static void guardFall(ServerPlayer player, int ticks) {
        FALL_GUARD.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    static boolean fallGuarded(ServerPlayer player) {
        Long until = FALL_GUARD.get(player.getUUID());
        long now = player.level().getGameTime();
        return until != null && now <= until && until - now <= 600;
    }

    @Subscribe(priority = Priority.HIGH)
    public static void onFall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypeTags.IS_FALL)
                && fallGuarded(player)) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onBlow(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer hulk) || source.getDirectEntity() != hulk
                || event.getEntity() == hulk) {
            return;
        }
        if (OWN.stream().anyMatch(source::is)) {
            return;
        }
        PowerData data = Powers.get(hulk);
        if (!HulkAbilities.hasBody(data)) {
            return;
        }
        event.setAmount((float) (event.getAmount() * HulkRage.damageFactor(hulk, data)));
        HulkRage.landed(hulk, data);
    }

    @Subscribe
    public static void onHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0.0F) {
            return;
        }
        PowerData data = Powers.get(player);
        if (!data.has(HeroAbility.HULK_BODY) && !data.has(HeroAbility.HULK_TRANSFORM)) {
            return;
        }
        HulkRage.hurt(player, data, event.getNewDamage());
        if (HulkAbilities.hasBody(data) && event.getNewDamage() >= 3.0F && player.getRandom().nextInt(3) == 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HULK_HURT.get(),
                    SoundSource.PLAYERS,
                    1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        }
    }

    @Subscribe
    public static void onEffect(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player) || !HulkRage.enraged(player)) {
            return;
        }
        if (event.getEffectInstance().is(MobEffects.CONFUSION) || event.getEffectInstance().is(ModEffects.CONFUSED)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            long now = player.level().getGameTime();
            Long said = SAID.get(player.getUUID());
            if (said == null || now - said >= 20 || said > now) {
                SAID.put(player.getUUID(), now);
                player.displayClientMessage(Component.translatable("message.enhanced.hulk.too_angry")
                        .withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    @Subscribe
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && com.kolodkingal.enhanced.effect.FrightenedEffect.fears(event.getEntity(), target)) {
            event.setCanceled(true);
        }
    }

    static void forget(ServerPlayer player) {
        FALL_GUARD.remove(player.getUUID());
        SAID.remove(player.getUUID());
    }

    static void clear() {
        FALL_GUARD.clear();
        SAID.clear();
    }

    private HulkEvents() {
    }
}
