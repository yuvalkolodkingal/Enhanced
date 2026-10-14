package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
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
 *
 * <p>Since 3.1 (Eilon's item 9) a hit is also <em>critical</em> on the boss ({@link #isCritical}) when its type is in
 * the same tag and its causing player's power is one of {@link ApocalypseShieldConfig#CRIT_HEROES}: a list kept apart
 * from the breakers, so a hero a server adds to those does not get critical hits too. The factors themselves are the
 * boss's ({@link ApocalypseEntity#hurt}); the hero's shield, which reads this class as well, is never multiplied.
 */
public final class MindPower {
    /** Names in the config that are not a power: warned about once each (per option). */
    private static final Set<String> WARNED = new HashSet<>();

    /** The heroes whose mind power wears the shield, as the config lists them (a name that is no power is ignored). */
    public static List<Power> breakers() {
        return powers(ApocalypseShieldConfig.MIND_POWER_HEROES.get(), "mindPowerHeroes");
    }

    /** The heroes whose mind power hits the boss critically, as the config lists them (a name of no power: ignored). */
    public static List<Power> critHeroes() {
        return powers(ApocalypseShieldConfig.CRIT_HEROES.get(), "critHeroes");
    }

    /** The powers an option of {@code [apocalypse_shield]} names, in order; a name of no power is warned about once. */
    private static List<Power> powers(List<? extends String> names, String option) {
        List<Power> powers = new ArrayList<>();
        for (String name : names) {
            Power power = Power.byName(name);
            if (power != null && power != Power.NONE) {
                powers.add(power);
            } else if (WARNED.add(option + "/" + name)) {
                Enhanced.LOGGER.warn("apocalypse_shield.{} names \"{}\", which is not a power: ignored", option, name);
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

    /** Whether the player's mind power hits the boss critically (by default a Scarlet Witch or a Phoenix). */
    public static boolean isCritHero(Player player) {
        Power power = Powers.power(player);
        return power != Power.NONE && critHeroes().contains(power);
    }

    /**
     * Whether the damage is a critical hit on the boss: of a type in the tag, caused by a player who is one of {@code
     * critHeroes}. (On his shield it must also be mind power at all, {@link #isMindPower}, or the shield turns it
     * away.)
     */
    public static boolean isCritical(DamageSource source) {
        return source.is(ModTags.MIND_POWER) && source.getEntity() instanceof Player player && isCritHero(player);
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

    /**
     * The factor of the damage's type. A slam of a block borrowed from an arena wears the shield by {@code
     * borrowedSlamFactor} times what {@code slamFactor} gives: an arena's blocks never run out (D-T 9).
     */
    public static float factor(DamageSource source) {
        if (source.is(ModDamageTypes.CHAOS_BEAM)) {
            return ApocalypseShieldConfig.CHAOS_BEAM_FACTOR.get().floatValue();
        }
        if (source.is(ModDamageTypes.TELEKINETIC_SLAM)) {
            float slam = ApocalypseShieldConfig.SLAM_FACTOR.get().floatValue();
            return source.getDirectEntity() instanceof TelekineticBlockEntity block && block.isBorrowed()
                    ? slam * ApocalypseShieldConfig.BORROWED_SLAM_FACTOR.get().floatValue() : slam;
        }
        if (source.is(ModDamageTypes.PHOENIX_BURST)) {
            return ApocalypseShieldConfig.PHOENIX_BURST_FACTOR.get().floatValue();
        }
        return ApocalypseShieldConfig.OTHER_MIND_FACTOR.get().floatValue();
    }

    private MindPower() {
    }
}
