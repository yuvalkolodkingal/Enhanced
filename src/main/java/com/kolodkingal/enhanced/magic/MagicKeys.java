package com.kolodkingal.enhanced.magic;

/**
 * The numbers of 3.17's two keys in {@code AbilityKeyPayload} (after the five ability keys and Y, {@code HeroKeys.Y}):
 * K, the spells, and O, the ultimate form.
 */
public final class MagicKeys {
    /** K: casts the chosen spell (held: chants faster). */
    public static final int SPELL = 7;
    /** O: awakens the ultimate form, or fires its signature move while it lasts. */
    public static final int ULTIMATE = 8;

    private MagicKeys() {
    }
}
