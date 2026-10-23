package com.kolodkingal.enhanced.healing;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HealingConfig;
import com.kolodkingal.enhanced.mixin.MobEffectInstanceAccessor;
import com.kolodkingal.enhanced.network.HealingFxPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Poison and the healing factor (3.7, the plan's 2.2 and 2.6). Poison (the effects of {@code #enhanced:stops_healing})
 * switches the healing <b>off</b> while it lasts: no regeneration, no shrug, no Burst, no protection (anything can
 * defeat him: {@link HealingFactor#defeat}); milk or the end of the Poison turns it back <b>on</b> at once. The moment
 * it goes off or comes back is told plainly (a sour hiss or a bright chime, the HUD's icon, a line).
 *
 * <p>And the <b>shrug</b>: a working healing factor makes the game's own harmful effects last {@code shrugFactor} (half)
 * as long, except those that stop the healing and the mod's own spells (Spellbound, Drained, Frightened, Confused,
 * Mjolnir's pin...: other mods' and this mod's effects are never shortened).
 */
public final class Suppression {
    /** Those whose healing poison has switched off, as the server last told them (the edges of {@link #tick}). */
    private static final Set<UUID> OFF = ConcurrentHashMap.newKeySet();

    private Suppression() {
    }

    public static double shrugFactor() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.SHRUG_FACTOR.get() : 0.5;
    }

    /** Whether a healer shrugs this effect off faster: one of the game's own harmful effects that does not stop healing. */
    public static boolean shrugs(Holder<MobEffect> effect) {
        return effect.value().getCategory() == MobEffectCategory.HARMFUL && !HealingFactor.stopsHealing(effect)
                && effect.unwrapKey().map(k -> k.location().getNamespace().equals("minecraft")).orElse(false);
    }

    /** The time a healer keeps an effect of {@code duration} ticks (infinite stays infinite; at least a tick). */
    public static int shrunk(int duration, double factor) {
        if (duration < 0) {
            return duration;
        }
        return Math.max(1, (int) Math.round(duration * factor));
    }

    /** An effect is about to be put on {@code player}: a healer whose healing works keeps it shorter. */
    static void onAdded(ServerPlayer player, MobEffectInstance added) {
        if (!HealingFactor.active(player) || !shrugs(added.getEffect()) || added.isInfiniteDuration()) {
            return;
        }
        ((MobEffectInstanceAccessor) added).enhanced$setDuration(shrunk(added.getDuration(), shrugFactor()));
    }

    /** Whether the server has told {@code player} that his healing is off. */
    public static boolean toldOff(ServerPlayer player) {
        return OFF.contains(player.getUUID());
    }

    /** One tick: the healing has just gone off (poisoned) or come back. */
    static void tick(ServerPlayer player) {
        boolean off = player.isAlive() && HealingFactor.has(player) && HealingFactor.suppressed(player);
        boolean was = OFF.contains(player.getUUID());
        if (off == was) {
            return;
        }
        if (off) {
            OFF.add(player.getUUID());
            Downed.fx(player, HealingFxPayload.OFF, 0);
            Downed.sound(player, ModSounds.HEALING_OFF.get(), 0.9F);
            player.displayClientMessage(Component.translatable("message.enhanced.healing.poisoned")
                    .withStyle(ChatFormatting.RED), true);
        } else {
            OFF.remove(player.getUUID());
            if (player.isAlive() && HealingFactor.has(player)) {
                Downed.fx(player, HealingFxPayload.ON, 0);
                Downed.sound(player, ModSounds.HEALING_ON.get(), 0.9F);
                player.displayClientMessage(Component.translatable("message.enhanced.healing.on_again")
                        .withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    /** He left (or the server stops): forget what he was told. */
    static void forget(UUID player) {
        OFF.remove(player);
    }

    static void clear() {
        OFF.clear();
    }
}
