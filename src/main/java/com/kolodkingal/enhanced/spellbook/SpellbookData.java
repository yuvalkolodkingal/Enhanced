package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.custom.CustomHero;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What a player keeps of the spellbooks (saved, kept on death, never synced: the clients read everything they draw from
 * the caster's synced definition). {@code stash} is the hero of his own he had made with the Hero Creator before a book
 * made him its caster ({@link CustomHero#EMPTY} if none): the book's definition takes its place while he is the caster,
 * and he gets it back when he gives the book's magic up. {@code hatColor} is the colour he chose for the Ethereal witch's
 * hat (it is kept while the hat is off).
 */
public record SpellbookData(CustomHero stash, int hatColor) {
    /** The hat of a witch who has never chosen: the dark violet of Agatha's own. */
    public static final int DEFAULT_HAT = 0x3A2F48;
    public static final SpellbookData EMPTY = new SpellbookData(CustomHero.EMPTY, DEFAULT_HAT);

    public static final Codec<SpellbookData> CODEC = RecordCodecBuilder.create(i -> i.group(
            CustomHero.CODEC.optionalFieldOf("stash", CustomHero.EMPTY).forGetter(SpellbookData::stash),
            Codec.INT.optionalFieldOf("hat_color", DEFAULT_HAT).forGetter(SpellbookData::hatColor)
    ).apply(i, SpellbookData::new));

    public boolean isEmpty() {
        return stash.isEmpty() && hatColor == DEFAULT_HAT;
    }

    public SpellbookData withStash(CustomHero stash) {
        return new SpellbookData(stash, hatColor);
    }

    public SpellbookData withHatColor(int color) {
        return new SpellbookData(stash, color & 0xFFFFFF);
    }
}
