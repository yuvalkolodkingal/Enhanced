package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Suit energy (3.8, PLAN 5.4): one meter, the reactor's charge, shared by every suit (it survives a suit change), kept
 * in {@link IronManState#energy} (synced: the HUD and the eyes read it). Moves {@link #spend} it, held moves and flight
 * {@link #drain} it a tick at a time; it recharges +3 a second on the ground and +1 in the air not flying, once {@code
 * rechargeDelayTicks} have passed without a move. Empty: flight ends with Slow Falling (a soft landing, never a death
 * fall) and every move is refused ("Suit energy empty!"). Creative players spend nothing.
 */
public final class SuitEnergy {
    /** When each Iron Man last spent energy (game time): the recharge waits {@code rechargeDelayTicks} after it. */
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();
    /** Whether each one's "energy low" warning has sounded since the meter was last above a quarter. */
    private static final Map<UUID, Boolean> WARNED = new HashMap<>();

    /** The meter's top ({@code [iron_man] energyMax}). */
    public static float max() {
        return (float) IronManAbilities.cfg(IronManConfig.ENERGY_MAX, 100.0);
    }

    /** His energy now. */
    public static float get(Player player) {
        return IronSuits.state(player).energy();
    }

    /** His energy as a share of the meter (0-1). */
    public static float fraction(Player player) {
        float max = max();
        return max <= 0.0F ? 0.0F : Mth.clamp(get(player) / max, 0.0F, 1.0F);
    }

    /** Sets it (clamped to the meter). */
    public static void set(Player player, double value) {
        IronManState state = IronSuits.state(player);
        float clamped = (float) Mth.clamp(value, 0.0, max());
        if (clamped != state.energy()) {
            IronSuits.setState(player, state.withEnergy(clamped));
        }
    }

    /**
     * A move's cost: refused (false, nothing taken, "Suit energy empty!") when he has less than {@code amount}; else it
     * is taken and the recharge waits.
     */
    public static boolean spend(ServerPlayer player, double amount) {
        if (player.isCreative() || amount <= 0.0) {
            used(player);
            return true;
        }
        float now = get(player);
        if (now + 1.0E-4 < amount) {
            refuse(player);
            return false;
        }
        set(player, now - amount);
        used(player);
        afterSpend(player);
        return true;
    }

    /**
     * One tick's share of a held move's or of flight's cost ({@code perSecond} / 20); false when the meter ran dry (the
     * caller ends the move).
     */
    public static boolean drain(ServerPlayer player, double perSecond) {
        used(player);
        if (player.isCreative() || perSecond <= 0.0) {
            return true;
        }
        float now = get(player);
        if (now <= 0.0F) {
            return false;
        }
        set(player, now - perSecond / 20.0);
        afterSpend(player);
        return get(player) > 0.0F;
    }

    /** Energy given back (Veronica's arm, a charged Refocuser, a command). */
    public static void add(ServerPlayer player, double amount) {
        set(player, get(player) + amount);
    }

    /** Whether the meter is empty. */
    public static boolean empty(Player player) {
        return get(player) <= 0.0F;
    }

    /** "Suit energy empty!" in his action bar. */
    public static void refuse(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.enhanced.iron_man.energy_empty")
                .withStyle(ChatFormatting.GOLD), true);
    }

    /** Marks a use: the recharge waits from now. */
    public static void used(ServerPlayer player) {
        LAST_USE.put(player.getUUID(), player.level().getGameTime());
    }

    /** The low warning (once under a quarter), and the empty moment (flight ends softly). */
    private static void afterSpend(ServerPlayer player) {
        float frac = fraction(player);
        UUID id = player.getUUID();
        if (frac < 0.25F && !WARNED.getOrDefault(id, false) && frac > 0.0F) {
            WARNED.put(id, true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SUIT_ENERGY_LOW.get(),
                    SoundSource.PLAYERS, 0.6F, 1.0F);
        }
        if (frac <= 0.0F) {
            ranDry(player);
        }
    }

    /**
     * The meter hit zero: flight ends with Slow Falling ({@code emptySlowFallTicks}), the empty sound and effect.
     * (Moves end themselves when their {@link #drain} says false.)
     */
    static void ranDry(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SUIT_ENERGY_EMPTY.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
        if (player.getAbilities().flying || !player.onGround()) {
            int ticks = IronManAbilities.cfg(IronManConfig.EMPTY_SLOW_FALL_TICKS, 100);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0, false, false, true));
        }
        IronManAbilities.send(player.serverLevel(), player, IronFxPayload.ENERGY_EMPTY, List.of(player.position()), 0);
        refuse(player);
    }

    /**
     * Every tick of an Iron Man (suited or not): the recharge after the delay, +{@code rechargeGround}/s on the ground,
     * +{@code rechargeAir}/s in the air when not flying.
     */
    static void tick(ServerPlayer player, long now) {
        UUID id = player.getUUID();
        float energy = get(player);
        float max = max();
        if (energy > max) {
            set(player, max);
            return;
        }
        if (fraction(player) >= 0.25F) {
            WARNED.remove(id);
        }
        Long last = LAST_USE.get(id);
        int delay = IronManAbilities.cfg(IronManConfig.RECHARGE_DELAY_TICKS, 20);
        if (last != null && now - last < delay && now >= last) {
            return;
        }
        if (energy >= max) {
            return;
        }
        double perSecond;
        if (player.onGround()) {
            perSecond = IronManAbilities.cfg(IronManConfig.RECHARGE_GROUND, 3.0);
        } else if (!player.getAbilities().flying) {
            perSecond = IronManAbilities.cfg(IronManConfig.RECHARGE_AIR, 1.0);
        } else {
            return;
        }
        set(player, energy + perSecond / 20.0);
    }

    static void forget(UUID id) {
        LAST_USE.remove(id);
        WARNED.remove(id);
    }

    static void clear() {
        LAST_USE.clear();
        WARNED.clear();
    }

    private SuitEnergy() {
    }
}
