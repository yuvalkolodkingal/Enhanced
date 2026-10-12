package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What counts as mind power: the one rule of Apocalypse's shield, for the boss's and the hero's alike. Eilon's words are
 * that the shield stops everything except the mind power of Wanda and of the Phoenix, and this is that sentence as a
 * pure function.
 *
 * <p>A hit counts if and only if all three hold: its damage type is in the data-driven tag {@code enhanced:mind_power}
 * (the Chaos Beam, a telekinetic slam, the Phoenix Burst), its causing entity is a player whose power is one of {@link
 * ApocalypseShieldConfig#MIND_POWER_HEROES} (Scarlet Witch and Phoenix by default, so a slam thrown by Magneto or Billy
 * is not mind power), and it arrives as damage. It then wears the shield by its amount times the factor of its type.
 */
public final class MindPower {
    /** Names in the config that are not a power: warned about once each. */
    private static final Set<String> WARNED = new HashSet<>();

    /** The heroes whose mind power wears the shield, as the config lists them (a name that is no power is ignored). */
    public static List<Power> breakers() {
        List<Power> powers = new ArrayList<>();
        for (String name : ApocalypseShieldConfig.MIND_POWER_HEROES.get()) {
            Power power = Power.byName(name);
            if (power != null && power != Power.NONE) {
                powers.add(power);
            } else if (WARNED.add(name)) {
                Enhanced.LOGGER.warn("apocalypse_shield.mindPowerHeroes names \"{}\", which is not a power: ignored", name);
            }
        }
        return powers;
    }

    /** Whether this hero's mind power can wear the shield down. */
    public static boolean isBreaker(Power power) {
        return power != Power.NONE && breakers().contains(power);
    }

    /** Whether the player is one who can break it (a Scarlet Witch or a Phoenix, unless the server says more). */
    public static boolean isBreaker(Player player) {
        return isBreaker(Powers.power(player));
    }

    /** Whether the damage is mind power: of a type in the tag, caused by a player who is a breaker. */
    public static boolean isMindPower(DamageSource source) {
        return source.is(ModTags.MIND_POWER) && source.getEntity() instanceof Player player && isBreaker(player);
    }

    /**
     * How much of the shield's strength the damage removes: 0 unless it is mind power, otherwise {@code amount} times the
     * factor of its type ({@code chaosBeamFactor}, {@code slamFactor}, {@code phoenixBurstFactor}, or {@code
     * otherMindFactor} for any other type of the tag).
     */
    public static float shieldDamage(DamageSource source, float amount) {
        if (amount <= 0.0F || !isMindPower(source)) {
            return 0.0F;
        }
        return amount * factor(source);
    }

    /**
     * The strain of a Phoenix's touch of this kind: {@code phoenixMindControlStrain} or {@code phoenixPsionicStrain}, 0 for
     * any other.
     */
    public static float strain(PhoenixPower.Kind kind) {
        return switch (kind) {
            case MIND_CONTROL -> ApocalypseShieldConfig.PHOENIX_MIND_CONTROL_STRAIN.get().floatValue();
            case PSIONIC_FIELD -> ApocalypseShieldConfig.PHOENIX_PSIONIC_STRAIN.get().floatValue();
            default -> 0.0F;
        };
    }

    /**
     * Whether a strain of {@code phoenix}'s touch of {@code kind} counts now on a shield whose strains so far are in
     * {@code last} (updated when it counts): once per cast. A cast that the mind refuses (his always do) starts no recharge
     * of hers, so pressing the key again at once is not another cast; the shield keeps her recharge of it itself
     * ({@code mindControlCooldown}, {@code psionicCooldown}).
     */
    public static boolean strainCounts(Map<String, Long> last, UUID phoenix, PhoenixPower.Kind kind, long now) {
        String key = phoenix + "/" + kind;
        Long before = last.get(key);
        long recharge = kind == PhoenixPower.Kind.MIND_CONTROL ? PhoenixConfig.MIND_CONTROL_COOLDOWN.get() : PhoenixConfig.PSIONIC_COOLDOWN.get();
        if (before != null && now - before >= 0 && now - before < recharge) {
            return false;
        }
        last.put(key, now);
        return true;
    }

    /** The factor of the damage's type. */
    public static float factor(DamageSource source) {
        if (source.is(ModDamageTypes.CHAOS_BEAM)) {
            return ApocalypseShieldConfig.CHAOS_BEAM_FACTOR.get().floatValue();
        }
        if (source.is(ModDamageTypes.TELEKINETIC_SLAM)) {
            return ApocalypseShieldConfig.SLAM_FACTOR.get().floatValue();
        }
        if (source.is(ModDamageTypes.PHOENIX_BURST)) {
            return ApocalypseShieldConfig.PHOENIX_BURST_FACTOR.get().floatValue();
        }
        return ApocalypseShieldConfig.OTHER_MIND_FACTOR.get().floatValue();
    }

    private MindPower() {
    }
}
