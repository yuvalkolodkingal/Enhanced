package com.kolodkingal.enhanced.healing;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HealingConfig;
import com.kolodkingal.enhanced.config.SoulStoneConfig;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.vormir.VormirTracker;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * The healing factor (3.7) of Wolverine, Deadpool and a custom hero with their body rows: who has it, whether poison
 * has switched it off, and the rule of what may still defeat a healer (the plan's 2.3). Written by the 3.7 skeleton,
 * pure and tested there ({@code MutantSkeletonTests}); lane L1 builds the rest on it (the DOWN state and the rise, the
 * regeneration, the shrug, the rescue, the Healing Burst: {@code Downed}, {@code Regeneration}, {@code Suppression},
 * {@code Rescue}, {@link HealingBurst}). Both sides; the config is read when asked (defaults before it is loaded).
 *
 * <pre>
 * has(player)        = healing_factor.enabled &amp;&amp; Traits.has(player, HEALING_FACTOR)
 *                      &amp;&amp; (not a custom hero || healing_factor.customHeroes)
 * suppressed(entity) = healing_factor.poisonStopsHealing &amp;&amp; it has an effect of #enhanced:stops_healing
 * active(player)     = has(player) &amp;&amp; !suppressed(player)
 * lethalForHealer(source, victim) =
 *        the void or /kill (#minecraft:bypasses_invulnerability, not the mod's own types), always
 *     || !active(victim)                                   (poisoned: anything can kill him)
 *     || one of the five groups' tags, if its option is on (stones, chaos, odin, phoenix; darkhold off by default)
 *     || a fall into the Vormir chasm with a credited push  (vormirDefeats: the Soul Stone's own magic)
 * </pre>
 */
public final class HealingFactor {

    /**
     * What defeated a healer (the group of the rule that let the blow through), for the death line {@code
     * message.enhanced.healing.defeated.<id>}: the void or {@code /kill}, poison, or one of the groups.
     */
    public enum Defeat {
        /** The void, {@code /kill}: never a trap. */
        ALWAYS,
        /** His healing was off (poisoned): anything could. */
        POISON,
        STONES,
        CHAOS,
        ODIN,
        PHOENIX,
        DARKHOLD,
        /** Pushed off the Vormir Cliff and given to the chasm. */
        VORMIR;

        /** {@code stones}, {@code chaos}...: the lang keys' last part. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The lang key of the line told to everyone near when a healer is defeated this way. */
        public String messageKey() {
            return "message.enhanced.healing.defeated." + id();
        }
    }

    /**
     * Which groups defeat a healer (the five options and the Vormir one), as a value: the config's ({@link #config()})
     * or a test's.
     */
    public record Rules(boolean stones, boolean chaos, boolean odin, boolean phoenix, boolean darkhold,
                        boolean vormir) {
        /** The plan's defaults: all but the Darkhold. */
        public static final Rules DEFAULTS = new Rules(true, true, true, true, false, true);
    }

    /** Whether the player has a healing factor (whether or not poison has switched it off). */
    public static boolean has(Player player) {
        return has(Powers.get(player));
    }

    /** Whether a player with this power data has a healing factor. */
    public static boolean has(PowerData data) {
        if (!enabled() || !Traits.has(data, Trait.HEALING_FACTOR)) {
            return false;
        }
        return data.power() != Power.CUSTOM || customHeroes();
    }

    /** Whether poison has switched the healing off: he has an effect of {@code #enhanced:stops_healing}. */
    public static boolean suppressed(LivingEntity entity) {
        if (!poisonStopsHealing()) {
            return false;
        }
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (stopsHealing(effect.getEffect())) {
                return true;
            }
        }
        return false;
    }

    /** Whether an effect is one that switches the healing off. */
    public static boolean stopsHealing(Holder<MobEffect> effect) {
        return effect.is(ModTags.STOPS_HEALING);
    }

    /** Whether his healing works now: he has it and is not poisoned. */
    public static boolean active(Player player) {
        return has(player) && !suppressed(player);
    }

    /** Whether he is DOWN (lane L1 sets and clears the state; every client sees it). */
    public static boolean isDown(Player player) {
        return Powers.get(player).is(Live.HEALING_DOWN);
    }

    /**
     * Whether {@code source} may kill {@code victim}, a healer whose health it would bring to 0 (a player without a
     * healing factor: always). Read by lane L1 in the damage and death events.
     */
    public static boolean lethalForHealer(DamageSource source, Player victim) {
        return defeat(source, victim) != null;
    }

    /** What lets {@code source} kill the healer {@code victim}, or null when it may not (he goes DOWN instead). */
    @Nullable
    public static Defeat defeat(DamageSource source, Player victim) {
        if (!has(victim)) {
            return Defeat.ALWAYS;
        }
        return defeat(source::is, isVoidOrKill(source.typeHolder()), active(victim), config(),
                isSacrificeFall(victim, source));
    }

    /**
     * The rule itself, pure: {@code is} says which of the five tags the damage type is in; {@code bypasses} whether it
     * is the void or {@code /kill}; {@code active} whether his healing is on (not poisoned); {@code sacrificeFall}
     * whether it is a credited fall into the Vormir chasm. Returns the group that lets it through, or null.
     */
    @Nullable
    public static Defeat defeat(Predicate<TagKey<DamageType>> is, boolean bypasses, boolean active, Rules rules,
                                boolean sacrificeFall) {
        if (bypasses) {
            return Defeat.ALWAYS;
        }
        if (!active) {
            return Defeat.POISON;
        }
        if (rules.stones() && is.test(ModTags.DEFEATS_HEALING_STONES)) {
            return Defeat.STONES;
        }
        if (rules.chaos() && is.test(ModTags.DEFEATS_HEALING_CHAOS)) {
            return Defeat.CHAOS;
        }
        if (rules.odin() && is.test(ModTags.DEFEATS_HEALING_ODIN)) {
            return Defeat.ODIN;
        }
        if (rules.phoenix() && is.test(ModTags.DEFEATS_HEALING_PHOENIX)) {
            return Defeat.PHOENIX;
        }
        if (rules.darkhold() && is.test(ModTags.DEFEATS_HEALING_DARKHOLD)) {
            return Defeat.DARKHOLD;
        }
        if (rules.vormir() && sacrificeFall) {
            return Defeat.VORMIR;
        }
        return null;
    }

    /**
     * Whether a damage type is "the void or {@code /kill}": in {@code #minecraft:bypasses_invulnerability} and not one
     * of the mod's own (the mod puts the Hex Wall and Vision's Mind Stone there to reach Creative players; they are a
     * group of the rule, switched by their option, not "always").
     */
    public static boolean isVoidOrKill(Holder<DamageType> type) {
        return type.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !type.unwrapKey().map(k -> k.location().getNamespace().equals(Enhanced.MODID)).orElse(false);
    }

    /**
     * Whether {@code source} is the fall of one pushed off a Vormir Cliff and given to its chasm: he left the edge with
     * a push credited to a player, within the time of a death, and the fall ends in the pit (the Soul Stone's own magic
     * takes him, healer or not). Asks {@link VormirTracker}'s public record of departures only (not the sacrifice's
     * internals), so 3.6.1's changes to the sacrifice keep it true; the chasm's own damage of 3.6.1 ({@link
     * ModDamageTypes#VORMIR_FALL}) counts as the fall. A healer who jumps or falls alone is not taken (no credit).
     */
    public static boolean isSacrificeFall(LivingEntity victim, DamageSource source) {
        boolean fall = source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(ModDamageTypes.VORMIR_FALL);
        if (!fall || victim.level().isClientSide) {
            return false;
        }
        VormirTracker.Departure gone = VormirTracker.departure(victim);
        if (gone == null || gone.credit() == null || gone.credit().pusher() == null
                || gone.credit().pusher().equals(victim.getUUID())) {
            return false;
        }
        long now = victim.level().getGameTime();
        int window = EnhancedConfig.SPEC.isLoaded() ? SoulStoneConfig.DEATH_WINDOW_TICKS.get() : 200;
        return now >= gone.tick() && now - gone.tick() <= window && gone.site().inPit(victim);
    }

    /** The groups as the config has them now (the defaults before it is loaded). */
    public static Rules config() {
        if (!EnhancedConfig.SPEC.isLoaded()) {
            return Rules.DEFAULTS;
        }
        return new Rules(HealingConfig.STONES_DEFEAT.get(), HealingConfig.CHAOS_DEFEATS.get(),
                HealingConfig.ODIN_DEFEATS.get(), HealingConfig.PHOENIX_DEFEATS.get(),
                HealingConfig.DARKHOLD_DEFEATS.get(), HealingConfig.VORMIR_DEFEATS.get());
    }

    static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || HealingConfig.ENABLED.get();
    }

    static boolean customHeroes() {
        return !EnhancedConfig.SPEC.isLoaded() || HealingConfig.CUSTOM_HEROES.get();
    }

    static boolean poisonStopsHealing() {
        return !EnhancedConfig.SPEC.isLoaded() || HealingConfig.POISON_STOPS_HEALING.get();
    }

    private HealingFactor() {
    }
}
