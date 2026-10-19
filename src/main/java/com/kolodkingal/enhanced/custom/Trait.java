package com.kolodkingal.enhanced.custom;

/**
 * A role a player has because of his powers (3.4), asked through {@link Traits#has} wherever the mod used to ask for a
 * list of heroes: a built-in hero has the traits the old lists gave him, a custom hero those of the abilities he picked
 * ({@link HeroAbility#traits}, and a few generic modules: {@link PowerModule#traits}).
 */
public enum Trait {
    /** A sorcerer: the Hex Runes hold him, he may read the Book of the Vishanti and the Darkhold. */
    SORCERER,
    /** A telepath of Professor X's rank ({@code Telepathy.Rank.TELEPATH}). */
    TELEPATH,
    /** A stronger telepath: the Scarlet Witch's, the Phoenix's, Billy's rank ({@code Telepathy.Rank.STRONGER}). */
    TELEPATH_STRONG,
    /** Worthy of Mjolnir (Thor, Vision). */
    WORTHY,
    /** A body of metal: Magneto's powers take hold of it (Vision, Ultron). */
    METAL_BODY,
    /** The Iron Spider's mechanical legs (a built-in Spider-Man has them in that look only). */
    IRON_LEGS,
    /** Every power of his is the Darkhold's dark magic (Agatha). */
    DARK_MAGIC,
    /** Fire does not burn him. */
    FIRE_IMMUNE,
    /**
     * 3.6: he may hold a bare Infinity Stone (a god's nature: {@code StoneRules.isBearer}). The gods, the Scarlet Witch
     * and the Phoenix have it; a custom hero by a god's row ({@code stones.customBearers}).
     */
    STONE_BEARER,
    /**
     * 3.7: the healing factor ({@code healing/HealingFactor}): blows that would kill him leave him DOWN, and he rises;
     * only the Infinity Stones, chaos magic, the Allfather's power, the Phoenix or poison defeat him. Wolverine and
     * Deadpool have it; a custom hero by their body rows ({@code healing_factor.customHeroes}).
     */
    HEALING_FACTOR
}
