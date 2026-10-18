package com.kolodkingal.enhanced.protect;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.HexRunesConfig;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Hex Runes (3.3): Wanda inscribes runes on the four walls of her Hex, as in Westview, from her Hex Control screen. While
 * they glow, every other sorcerer inside the Hex ({@link Sorcery#isSorcerer}: Loki, Billy, another Scarlet Witch, whoever holds
 * the Darkhold or the Book of the Vishanti) cannot use powers: no key does anything, what they held or fired ends the moment the
 * runes take them (a beam, a barrier, a grip, a flight: they float down on Slow Falling), toggled powers turn off, and the books
 * do nothing. Wanda herself keeps her powers. Inscribing costs a little hunger and has a recharge ({@code [hex_runes]}); wiping
 * them is free.
 *
 * <p>Whether a player is silenced is worked out from synced data on both sides ({@link #silences}), so a client knows it too
 * (its HUD and its keys); the moment of being silenced is the server's ({@link #tick}).
 */
public final class HexRunes {
    /** Toggled powers the runes turn off (a sorcerer's own: the Noise; and whatever a holder of a book had on). */
    private static final Toggle[] TURNED_OFF = {Toggle.THE_NOISE, Toggle.GOD_OF_THUNDER, Toggle.FROST_GIANT, Toggle.MIND_READING,
            Toggle.REALITY_VEIL, Toggle.SPIDER_LEGS};
    /** How long a sorcerer the runes take out of the air floats down. */
    private static final int FLOAT_TICKS = 100;

    /** Who the runes hold now (server), so their powers end once, the moment the runes take them. */
    private static final Set<UUID> SILENCED = new HashSet<>();
    /** When each Scarlet Witch may inscribe the runes again. */
    private static final Map<UUID, Long> READY = new HashMap<>();

    /** Whether the runes work at all ({@code hex_runes.enabled}; before the server's config has reached a client, they do). */
    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || HexRunesConfig.ENABLED.get();
    }

    /** The Hex whose runes hold this player now, or null: a sorcerer inside a Hex with its runes up that is not their own. */
    @Nullable
    public static Hex silencing(Player player) {
        if (player.isSpectator() || !enabled()) {
            return null;
        }
        Vec3 at = Shelters.middle(player);
        for (Hex hex : HexManager.data(player.level()).hexes()) {
            if (hex.runes() && hex.contains(at) && !hex.owner().equals(player.getUUID()) && Sorcery.isSorcerer(player)) {
                return hex;
            }
        }
        return null;
    }

    /** Whether the runes of a Hex hold this player: they cannot use powers. Both sides. */
    public static boolean silences(Player player) {
        return silencing(player) != null;
    }

    // ---- Wanda inscribes them ----

    /** Ticks until this Scarlet Witch may inscribe the runes again. */
    public static long cooldown(ServerPlayer wanda) {
        return Math.max(0L, READY.getOrDefault(wanda.getUUID(), 0L) - wanda.level().getGameTime());
    }

    /**
     * Inscribes the runes on the four walls of Wanda's Hex ({@code on}), or wipes them. Inscribing is refused while the runes
     * recharge, on an empty stomach, or with the runes turned off on the server; it costs {@code hex_runes.exhaustion}.
     *
     * @return whether the runes changed
     */
    public static boolean set(ServerPlayer wanda, Hex hex, boolean on) {
        if (hex.runes() == on) {
            return false;
        }
        ServerLevel level = wanda.serverLevel();
        long now = level.getGameTime();
        if (on) {
            if (!enabled()) {
                wanda.displayClientMessage(Component.translatable("message.enhanced.hex_runes.disabled"), true);
                return false;
            }
            long left = cooldown(wanda);
            if (left > 0) {
                wanda.displayClientMessage(Component.translatable("message.enhanced.hex_runes.recharging",
                        String.format(Locale.ROOT, "%.1f", left / 20.0)), true);
                return false;
            }
            if (!wanda.getAbilities().invulnerable) {
                if (wanda.getFoodData().getFoodLevel() <= 0) {
                    wanda.displayClientMessage(Component.translatable("message.enhanced.hex_runes.hungry"), true);
                    return false;
                }
                wanda.causeFoodExhaustion(HexRunesConfig.EXHAUSTION.get().floatValue());
            }
            READY.put(wanda.getUUID(), now + HexRunesConfig.COOLDOWN.get());
        }
        level.setData(ModAttachments.HEXES, HexManager.data(level).update(hex.id(), h -> h.withRunes(on, now)));
        glow(level, hex, on);
        wanda.displayClientMessage(Component.translatable(on ? "message.enhanced.hex_runes.on" : "message.enhanced.hex_runes.off")
                .withStyle(ChatFormatting.RED), true);
        return true;
    }

    /** The runes flare up (or fade) on the four walls, and everyone inside hears them. */
    private static void glow(ServerLevel level, Hex hex, boolean on) {
        Vec3 c = hex.center();
        for (int side = 0; side < 4; side++) {
            double angle = Math.PI * 0.5 * side;
            Vec3 at = c.add(Math.sin(angle) * hex.radius(), Math.min(4.0, hex.radius() * 0.25), Math.cos(angle) * hex.radius());
            level.sendParticles(new EnergyParticleOptions(hex.color() >= 0 ? hex.color() : on ? Power.SCARLET_WITCH.glow : Power.SCARLET_WITCH.color, 2.0F),
                    at.x, at.y, at.z, 30, 1.5, 1.5, 1.5, 0.05);
        }
        float volume = Math.max(1.0F, hex.radius() / 16.0F);
        level.playSound(null, c.x, c.y, c.z, on ? SoundEvents.ENCHANTMENT_TABLE_USE : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, volume,
                on ? 0.6F : 0.8F);
        if (on) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, volume, 0.7F);
        }
    }

    // ---- what they do to a sorcerer ----

    /**
     * Every player, every tick (server, before the powers run): the moment the runes take a sorcerer, whatever they held or fired
     * ends; the moment they let go, the sorcerer is told their magic is back.
     */
    public static void tick(ServerPlayer player) {
        boolean held = silences(player);
        boolean was = SILENCED.contains(player.getUUID());
        if (held && !was) {
            SILENCED.add(player.getUUID());
            silence(player);
        } else if (!held && was) {
            SILENCED.remove(player.getUUID());
            if (player.isAlive()) {
                player.displayClientMessage(Component.translatable("message.enhanced.hex_runes.free"), true);
            }
        }
        if (held && player.getAbilities().flying && !player.isCreative()) {
            land(player);
        }
    }

    /** The runes take a sorcerer: every power they have ends now. */
    private static void silence(ServerPlayer player) {
        PowerData data = Powers.get(player);
        Telekinesis.release(player, true);
        if (ContinuousBeam.isFiring(player)) {
            ContinuousBeam.stop(player);
        }
        if (DarkholdBook.firing(player)) {
            DarkholdBook.beamStop(player);
        }
        if (BillyBarrier.owns(data)) {
            BillyBarrier.lower(player, data);
        }
        PowerTicker.endPower(player, data);
        PowerTicker.stopHoldAbilities(data);
        for (Toggle toggle : TURNED_OFF) {
            data.setToggle(toggle, false);
        }
        if (player.getAbilities().flying && !player.isCreative()) {
            land(player);
        }
        Powers.syncIfDirty(player);
        Vec3 at = Shelters.middle(player);
        ServerLevel level = player.serverLevel();
        level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.2F), at.x, at.y, at.z, 24, 0.4, 0.8, 0.4, 0.02);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 0.6F);
        player.displayClientMessage(Component.translatable("message.enhanced.hex_runes.silenced").withStyle(ChatFormatting.RED), true);
    }

    /** Out of the air: flight is off (the power gives it back when the runes let go), and they float down. */
    private static void land(ServerPlayer player) {
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FLOAT_TICKS, 0, false, false, true));
    }

    /** Says (in the action bar) that the runes hold this player, if they do: what a refused key or book says. */
    public static boolean refuse(ServerPlayer player) {
        if (!silences(player)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("message.enhanced.hex_runes.silenced").withStyle(ChatFormatting.RED), true);
        return true;
    }

    public static void forget(UUID player) {
        SILENCED.remove(player);
    }

    public static void clear() {
        SILENCED.clear();
        READY.clear();
    }

    private HexRunes() {
    }
}
