package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The one rule book of telepathy (3.3): who is a telepath, who is stronger, and whose mind a telepath may touch. Every
 * power that reads, hears, freezes or takes a mind asks it (Professor X's five, the thoughts The Noise hears), never a
 * list of its own.
 *
 * <p>The ranks (Eilon's list): <b>Professor X</b> is a {@link Rank#TELEPATH}; the <b>Scarlet Witch</b>, the
 * <b>Phoenix</b> and <b>Billy</b> are {@link Rank#STRONGER} telepaths; everyone else, Vision too, has no telepathy
 * ({@link Rank#NONE}). A mind is open to a telepath only if its rank is lower than the telepath's own: a telepath of the
 * same rank keeps him out, and a stronger one throws him out (the caller decides what that costs him). Magneto's helmet,
 * and the creatures of the {@code enhanced:telepathy_immune} tag (the bosses), shut a mind to every telepath.
 */
public final class Telepathy {
    /** How strong a mind's telepathy is, weakest first. */
    public enum Rank {
        /** No telepathy of its own: open to every telepath. */
        NONE,
        /** Professor X. */
        TELEPATH,
        /** The Scarlet Witch, the Phoenix, Billy: stronger than Professor X. */
        STRONGER
    }

    /** What happens when a telepath reaches for a mind. */
    public enum Verdict {
        /** The mind is open to him. */
        OPEN,
        /** Magneto's helmet (or a mind too vast, a boss's) shuts it: nothing happens. */
        SHIELDED,
        /** A telepath of the same rank keeps him out. */
        TELEPATH,
        /** A stronger telepath throws him out. */
        THROWS_OUT
    }

    /** The rank of a hero's telepathy. */
    public static Rank rank(Power power) {
        return switch (power) {
            case PROFESSOR_X -> Rank.TELEPATH;
            case SCARLET_WITCH, PHOENIX, BILLY -> Rank.STRONGER;
            default -> Rank.NONE;
        };
    }

    /** The rank of a creature's telepathy: a player's is their hero's; no other creature has any. */
    public static Rank rank(Entity entity) {
        if (!(entity instanceof Player player)) {
            return Rank.NONE;
        }
        // 3.4: a custom hero's rank is that of the telepathy he picked (Traits); a built-in hero's is his own.
        PowerData data = Powers.get(player);
        if (data.power() != Power.CUSTOM) {
            return rank(data.power());
        }
        return Traits.has(data, Trait.TELEPATH_STRONG) ? Rank.STRONGER : Traits.has(data, Trait.TELEPATH) ? Rank.TELEPATH : Rank.NONE;
    }

    /** Whether this creature is a telepath at all (any rank above {@link Rank#NONE}). */
    public static boolean isTelepath(Entity entity) {
        return rank(entity) != Rank.NONE;
    }

    /** Whether {@code a}'s telepathy is stronger than {@code b}'s. */
    public static boolean isStronger(Entity a, Entity b) {
        return rank(a).ordinal() > rank(b).ordinal();
    }

    /** Whether this mind is shut to every telepath: Magneto's helmet, or a creature of the telepathy-immune tag. */
    public static boolean isShielded(Entity entity) {
        return entity instanceof LivingEntity living && (MagnetoAbilities.wearsHelmet(living) || living.getType().is(ModTags.TELEPATHY_IMMUNE));
    }

    /** What happens when {@code telepath} reaches for the mind of {@code target}. */
    public static Verdict reach(Entity telepath, Entity target) {
        if (isShielded(target)) {
            return Verdict.SHIELDED;
        }
        int theirs = rank(target).ordinal();
        int mine = rank(telepath).ordinal();
        if (theirs > mine) {
            return Verdict.THROWS_OUT;
        }
        return theirs == Rank.NONE.ordinal() || theirs < mine ? Verdict.OPEN : Verdict.TELEPATH;
    }

    /** Whether the mind of {@code target} is open to {@code telepath} (see {@link #reach}). */
    public static boolean isOpen(Entity telepath, Entity target) {
        return reach(telepath, target) == Verdict.OPEN;
    }

    private Telepathy() {
    }
}
