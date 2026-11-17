package com.kolodkingal.enhanced.magic;

/**
 * The numbers of 3.17's three keys in {@code AbilityKeyPayload} (after the five ability keys and Y, {@code HeroKeys.Y}):
 * the spells, the ultimate form, and the Demiurge's Magic types (their keys: {@code client.KeyBindings}).
 */
public final class MagicKeys {
    /** Casts the chosen spell (held: chants faster). */
    public static final int SPELL = 7;
    /** Awakens the ultimate form, or fires its signature move while it lasts. */
    public static final int ULTIMATE = 8;
    /** The Demiurge's Magic types: on to the next magic user's abilities (after the last, his own). */
    public static final int MAGIC_TYPE = 9;

    private MagicKeys() {
    }
}
