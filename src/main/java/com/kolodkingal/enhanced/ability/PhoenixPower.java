package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What the rest of the mod may know about the Phoenix's mind powers: the one door Apocalypse's shield uses (it is broken
 * only by the mind power of the Scarlet Witch and of the Phoenix), written by the skeleton so that the Apocalypse lane
 * can build against it before the Phoenix lane exists. The Phoenix lane calls {@link #touched} wherever the Phoenix
 * reaches a creature; the Apocalypse lane registers a {@link Listener}.
 *
 * <p>The contract of the two lanes (phoenix.md 4.8):
 * <ul>
 *     <li>{@link #isPhoenixMindPower} is exactly {@code source.is(enhanced:mind_power)} with a Phoenix as the causing
 *     entity, so the tag and the definition cannot drift apart: the Burst, and the slam of what she threw.</li>
 *     <li>{@link #touched} is called <em>before</em> any refusal, once per cast, also for a creature that is
 *     {@code telepathy_immune} (Mind Control aimed at a boss is refused with "That mind is closed to you", and still
 *     reports {@link Kind#MIND_CONTROL}; a Psionic Field whose sphere holds a boss reports {@link Kind#PSIONIC_FIELD}
 *     though it is not held). The listener decides what the touch is worth.</li>
 * </ul>
 */
public final class PhoenixPower {
    /** The ways the Phoenix's mind reaches a creature. */
    public enum Kind {
        /** The Phoenix Burst (damage). */
        BURST,
        /** The slam of a creature or block she threw with telekinesis (damage, her as the causing entity). */
        SLAM,
        /** Mind Control (no damage). */
        MIND_CONTROL,
        /** The Psionic Field (no damage). */
        PSIONIC_FIELD,
        /** Mind Reading (no damage). */
        MIND_READ
    }

    /** Told, on the server, of every touch of the Phoenix's mind. */
    @FunctionalInterface
    public interface Listener {
        /**
         * @param phoenix the Phoenix
         * @param target  the creature her mind reached
         * @param kind    how
         * @param amount  the damage, for {@link Kind#BURST} and {@link Kind#SLAM}; 0 for the rest
         */
        void touched(ServerPlayer phoenix, LivingEntity target, Kind kind, float amount);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    /** Registers a listener (once, when the mod loads). */
    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    /** Takes a listener away (a test that added its own). */
    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    /** Reports a touch to every listener: the Phoenix lane calls it, before any refusal, once per cast. */
    public static void touched(ServerPlayer phoenix, LivingEntity target, Kind kind, float amount) {
        for (Listener listener : LISTENERS) {
            // A listener belongs to another hero or a boss: its mistake must not break the Phoenix's cast (which runs in
            // the server's tick).
            try {
                listener.touched(phoenix, target, kind, amount);
            } catch (RuntimeException e) {
                Enhanced.LOGGER.error("A PhoenixPower listener failed on {} ({})", target, kind, e);
            }
        }
    }

    /** Whether the entity is a player whose power is the Phoenix. */
    public static boolean isPhoenix(Entity entity) {
        return entity instanceof Player player && Powers.power(player) == Power.PHOENIX;
    }

    /**
     * Whether the damage is mind power of the Phoenix's: a damage type of the {@code enhanced:mind_power} tag (the Burst,
     * the slam) caused by a Phoenix.
     */
    public static boolean isPhoenixMindPower(DamageSource source) {
        if (!source.is(ModTags.MIND_POWER) || !(source.getEntity() instanceof Player player)) {
            return false;
        }
        if (isPhoenix(player)) {
            return true;
        }
        // 3.4: a custom hero's Burst, or the slam of what he threw with her telekinesis.
        PowerData data = Powers.get(player);
        return source.is(ModDamageTypes.PHOENIX_BURST) && data.has(HeroAbility.PHOENIX_BURST)
                || source.is(ModDamageTypes.TELEKINETIC_SLAM) && data.has(HeroAbility.PHOENIX_TELEKINESIS);
    }

    private PhoenixPower() {
    }
}
