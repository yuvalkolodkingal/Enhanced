package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.hulk.HulkForms;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/**
 * Who may hold an Infinity Stone, who may use it, and who may wield a filled Infinity Gauntlet (3.6, the holding rule
 * of the plan's 1.5). The 3.6 skeleton writes the predicates; lane L1 writes what happens to one who may not (the stone
 * slips away, burns after a grace, {@code StoneTouch}). Both sides; read when asked (the config is never cached).
 *
 * <pre> isBearer(player)      = THOR | LOKI | HEIMDALL | ODIN | HELA | SCARLET_WITCH | PHOENIX | (CUSTOM &amp;&amp;
 * Traits.has(player, STONE_BEARER) &amp;&amp; stones.customBearers) mayHold(player, kind) = player.isCreative() ||
 * isBearer(player) || (kind == TIME &amp;&amp; Traits.has(player, SORCERER))    // stones.sorcerersHoldTime || (kind ==
 * MIND &amp;&amp; Powers.is(player, VISION))        // stones.visionHoldsMind mayUse(player, kind)  = mayHold(player,
 * kind) &amp;&amp; (kind != TIME || Traits.has(player, SORCERER)) mayWield(player)      = isBearer(player) || THANOS ||
 * (HULK &amp;&amp; !HulkForms.isHuman(player) &amp;&amp; hulk.wieldsGauntlet) </pre>
 *
 * So Thor may hold the Time Stone but it is quiet in his hand; Loki, the Scarlet Witch and Odin's Odinforce row (a
 * sorcerer's) may use it; the Phoenix may hold it, not use it. {@code Sorcery.isSorcerer} (books in hand) is not used:
 * holding the Darkhold does not make anyone a sorcerer of the Time Stone. The Hulk is no Bearer: a bare stone burns him
 * as anyone, but he may wield a filled Gauntlet (Endgame).
 */
public final class StoneRules {

    /** Whether the player is a Bearer: he may hold every bare stone. */
    public static boolean isBearer(Player player) {
        PowerData data = Powers.get(player);
        return isBearer(data.power(), t -> Traits.has(data, t));
    }

    /** Whether a hero of {@code power} with the roles {@code traits} is a Bearer (the pure rule: the tests ask it). */
    public static boolean isBearer(Power power, Predicate<Trait> traits) {
        return switch (power) {
            case THOR, LOKI, HEIMDALL, ODIN, HELA, SCARLET_WITCH, PHOENIX -> true;
            case CUSTOM -> traits.test(Trait.STONE_BEARER) && customBearers();
            default -> false;
        };
    }

    /** Whether the player may hold the bare stone (carry it, pick it up, take it from a pedestal). */
    public static boolean mayHold(Player player, StoneKind kind) {
        PowerData data = Powers.get(player);
        return mayHold(data.power(), t -> Traits.has(data, t), player.isCreative(), kind);
    }

    /** The pure rule of {@link #mayHold(Player, StoneKind)}. */
    public static boolean mayHold(Power power, Predicate<Trait> traits, boolean creative, StoneKind kind) {
        if (creative || isBearer(power, traits)) {
            return true;
        }
        if (kind == StoneKind.TIME && traits.test(Trait.SORCERER)) {
            return sorcerersHoldTime();
        }
        return kind == StoneKind.MIND && power == Power.VISION && visionHoldsMind();
    }

    /**
     * Whether the player may use the stone (bare, in a Gauntlet he wields, by a stone row): the Time Stone only a
     * sorcerer.
     */
    public static boolean mayUse(Player player, StoneKind kind) {
        PowerData data = Powers.get(player);
        return mayUse(data.power(), t -> Traits.has(data, t), player.isCreative(), kind);
    }

    /** The pure rule of {@link #mayUse(Player, StoneKind)}. */
    public static boolean mayUse(Power power, Predicate<Trait> traits, boolean creative, StoneKind kind) {
        return mayHold(power, traits, creative, kind) && (kind != StoneKind.TIME || traits.test(Trait.SORCERER));
    }

    /**
     * Whether the player may wield a filled Infinity Gauntlet: a Bearer, the hero Thanos, or (3.6) the Hulk as a Hulk.
     */
    public static boolean mayWield(Player player) {
        Power power = Powers.power(player);
        return isBearer(player) || power == Power.THANOS || power == Power.HULK && !HulkForms.isHuman(player)
                && hulkWields();
    }

    // ---- the config (read when asked; its defaults where it is not loaded) ----

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    private static boolean customBearers() {
        return !loaded() || StonesConfig.CUSTOM_BEARERS.get();
    }

    private static boolean sorcerersHoldTime() {
        return !loaded() || StonesConfig.SORCERERS_HOLD_TIME.get();
    }

    private static boolean visionHoldsMind() {
        return !loaded() || StonesConfig.VISION_HOLDS_MIND.get();
    }

    private static boolean hulkWields() {
        return !loaded() || HulkConfig.WIELDS_GAUNTLET.get();
    }

    private StoneRules() {
    }
}
