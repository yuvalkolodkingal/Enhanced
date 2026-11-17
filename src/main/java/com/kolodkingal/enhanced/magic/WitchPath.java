package com.kolodkingal.enhanced.magic;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The paths of the witchcraft of 3.17 (Agatha All Along's coven): each witch has one affinity, an element and the
 * spells that go with it; the Spirit (or Chaos) witch is the rare all-rounder who learns from every path, as Agatha,
 * Wanda and Billy do. {@link #SORCERER} is the other kind of magic a Demiurge can grant: the mystic arts of Kamar-Taj.
 */
public enum WitchPath {
    /** Earth: vines and herbs to trap, healing by nature and the moon, the Witches' Road opened (Rio's). */
    GREEN(0x5FD068, 0xD8FFC8, false),
    /** Water: strong combat elixirs brewed in seconds, truth serum, antidotes, healing (Jen's). */
    POTIONS(0x3FA8E8, 0xC8F0FF, false),
    /** Fire: protective circles that keep dark magic out, reversal, allies shielded (Alice's). */
    PROTECTION(0xFF8A2A, 0xFFE2A8, false),
    /** Air: dodging, enemies seen through walls, attacks foreseen, sigils (Lilia's). */
    DIVINATION(0xB89CFF, 0xF0E8FF, false),
    /** All of them: the rare witch who can learn every path's spells (Agatha's, Wanda's and Billy's kind). */
    SPIRIT(0xE04090, 0xFFC8EC, true),
    /** The mystic arts: eldritch light, mandalas, the Crimson Bands, portals (a sorcerer, not a witch). */
    SORCERER(0xF29A2E, 0xFFE0A8, false);

    public final int color;
    public final int glow;
    /** Rare: never offered at random, only chosen where every path is (a Demiurge's gift, Creative, an operator). */
    public final boolean rare;

    WitchPath(int color, int glow, boolean rare) {
        this.color = color;
        this.glow = glow;
        this.rare = rare;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean witch() {
        return this != SORCERER;
    }

    public Component displayName() {
        return Component.translatable("witch_path.enhanced." + id());
    }

    public Component description() {
        return Component.translatable("witch_path.enhanced." + id() + ".desc");
    }

    /** The highest level of witchcraft (and of a sorcerer's art). */
    public static final int MAX_LEVEL = 5;

    @Nullable
    public static WitchPath byId(String id) {
        for (WitchPath path : values()) {
            if (path.id().equals(id)) {
                return path;
            }
        }
        return null;
    }

    @Nullable
    public static WitchPath byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
    }
}
