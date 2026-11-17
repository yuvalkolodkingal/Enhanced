package com.kolodkingal.enhanced.magic;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Every spell of 3.17's magic: the witches' paths, the grimoires, Billy's spells and his powers over space and reality.
 * A spell has a group (who may learn it: a path, any witch, a grimoire's reader, Billy, the Demiurge), the level of
 * witchcraft and the rank of Billy it needs, its mana, how long its incantation is (ticks; holding K chants faster, a
 * heavy blow breaks it off; the Demiurge's star form needs none), its cooldown (ticks) and its colour. What each does is
 * {@link SpellEffects}; who knows which is {@link Magic#known}.
 *
 * <p>Saved and synced by id ({@link #id}), never by ordinal, so spells can be added anywhere.
 */
public enum Spell {
    // ===== the Green Witch (earth) =====
    VINE_SNARE(Group.GREEN, 1, 0, 20, 20, 100, 0x5FD068),
    HERBAL_MEND(Group.GREEN, 1, 0, 25, 20, 160, 0x8FE07A),
    MOONLIGHT_BLESSING(Group.GREEN, 2, 0, 35, 30, 600, 0xCFE8FF),
    THORN_WALL(Group.GREEN, 3, 0, 30, 25, 300, 0x3F9F48),
    ROAD_PORTAL(Group.GREEN, 4, 5, 60, 60, 2400, 0x45C6D6),
    // ===== the Potions Witch (water) =====
    QUICK_BREW(Group.POTIONS, 1, 0, 40, 30, 1200, 0x3FA8E8),
    HEALING_DRAUGHT(Group.POTIONS, 1, 0, 25, 15, 160, 0xFF6F9A),
    ACID_FLASK(Group.POTIONS, 2, 0, 30, 20, 120, 0x9BE03F),
    ANTIDOTE(Group.POTIONS, 2, 0, 25, 20, 300, 0x7FE8D8),
    TRUTH_SERUM(Group.POTIONS, 3, 0, 30, 25, 400, 0xE8D25F),
    // ===== the Protection Witch (fire) =====
    SHIELD_ALLY(Group.PROTECTION, 1, 0, 20, 15, 200, 0xFFB34A),
    PROTECTION_CIRCLE(Group.PROTECTION, 1, 0, 40, 40, 600, 0xFF8A2A),
    REVERSAL(Group.PROTECTION, 2, 0, 30, 20, 300, 0xFFD27A),
    FLAME_WARD(Group.PROTECTION, 3, 0, 35, 25, 400, 0xFF5A1E),
    // ===== the Divination Witch (air) =====
    WIND_STEP(Group.DIVINATION, 1, 0, 15, 5, 80, 0xD8E8FF),
    TRUE_SIGHT(Group.DIVINATION, 1, 0, 20, 15, 300, 0xB89CFF),
    FORESIGHT(Group.DIVINATION, 1, 0, 30, 20, 600, 0xC8B8FF),
    SEEING_SIGIL(Group.DIVINATION, 2, 0, 25, 30, 600, 0x9C7CFF),
    PREMONITION(Group.DIVINATION, 3, 0, 30, 20, 500, 0xE0D0FF),
    // ===== the Spirit (Chaos) Witch =====
    CHAOS_BOLT(Group.SPIRIT, 1, 0, 20, 10, 40, 0xE04090),
    SPIRIT_WALK(Group.SPIRIT, 3, 0, 40, 20, 600, 0xFFC8EC),
    // ===== the sorcerer (the mystic arts) =====
    ELDRITCH_BOLT(Group.SORCERER, 1, 0, 15, 8, 30, 0xF29A2E),
    MANDALA_SHIELD(Group.SORCERER, 1, 0, 25, 10, 300, 0xFFB347),
    CRIMSON_BANDS(Group.SORCERER, 2, 0, 35, 20, 400, 0xD8262B),
    SLING_PORTAL(Group.SORCERER, 3, 0, 50, 30, 600, 0xFF9A2E),
    // ===== every witch and sorcerer (and Billy) =====
    MINDSCAPE(Group.WITCH, 1, 1, 10, 20, 100, 0xA8C8FF),
    BROOM(Group.WITCH, 2, 1, 20, 15, 200, 0x7FD0FF),
    COVEN_CIRCLE(Group.WITCH, 3, 2, 40, 30, 900, 0x7CE0B0),
    // ===== the Darkhold (forbidden: they corrupt) =====
    DREAMWALK(Group.DARKHOLD, 1, 0, 50, 40, 1200, 0x6A2FB0),
    POWER_SIPHON(Group.DARKHOLD, 1, 0, 35, 25, 300, 0x8A2FD6),
    SOUL_BIND(Group.DARKHOLD, 2, 0, 40, 30, 600, 0x5A1A8A),
    NECROMANCY(Group.DARKHOLD, 3, 0, 60, 50, 1200, 0x3FB06A),
    // ===== the Book of the Vishanti (light) =====
    VISHANTI_LIGHT(Group.VISHANTI, 1, 0, 40, 30, 600, 0xFFF6C8),
    VISHANTI_SHIELD(Group.VISHANTI, 2, 0, 50, 40, 900, 0xFFE89A),
    BANISH(Group.VISHANTI, 1, 2, 30, 25, 400, 0xE8F4FF),
    // ===== Cagliostro's grimoire (time) =====
    CHRONO_HOLD(Group.CAGLIOSTRO, 1, 0, 35, 25, 400, 0x5FE07A),
    TIME_LOOP(Group.CAGLIOSTRO, 2, 0, 50, 30, 1200, 0x4FD068),
    DARK_DIMENSION(Group.CAGLIOSTRO, 3, 0, 60, 40, 900, 0x7A3FD6),
    // ===== Billy: his spells =====
    UNBINDING(Group.BILLY, 0, 1, 30, 25, 300, 0xCFEFFF),
    SPATIAL_FOLD(Group.BILLY, 0, 1, 15, 10, 60, 0x5CC8FF),
    CONJURE_WEAPON(Group.BILLY, 0, 1, 20, 15, 600, 0x7FD0FF),
    CONJURE_FOOD(Group.BILLY, 0, 1, 15, 15, 600, 0xFFD27A),
    LIGHT_PLATFORM(Group.BILLY, 0, 1, 15, 8, 100, 0x8FE0FF),
    SILENCING_SIGIL(Group.BILLY, 0, 2, 30, 25, 600, 0x4A6FCC),
    SPATIAL_CRUSH(Group.BILLY, 0, 2, 30, 20, 200, 0x2F8CFF),
    CONJURE_BRIDGE(Group.BILLY, 0, 2, 25, 15, 300, 0x8FE0FF),
    CONJURE_WALL(Group.BILLY, 0, 2, 25, 15, 300, 0x8FE0FF),
    CLAIRVOYANCE(Group.BILLY, 0, 2, 30, 30, 600, 0xCFEFFF),
    ENCHANT_ITEM(Group.BILLY, 0, 2, 25, 20, 600, 0xB08CFF),
    FAMILIAR(Group.BILLY, 0, 2, 35, 30, 1200, 0x9CC8FF),
    PROTECTION_RUNE(Group.BILLY, 0, 3, 45, 35, 900, 0x5CC8FF),
    SIPHON_STANCE(Group.BILLY, 0, 3, 30, 15, 600, 0x9FE8FF),
    POCKET_DIMENSION(Group.BILLY, 0, 3, 30, 30, 200, 0x6FA8FF),
    CONJURE_ARMOR(Group.BILLY, 0, 3, 35, 25, 1200, 0x7FD0FF),
    GIANT_HAND(Group.BILLY, 0, 3, 35, 20, 300, 0x8FE0FF),
    RAISE_SPIRIT(Group.BILLY, 0, 4, 45, 35, 900, 0xBFE8FF),
    INFINITE_HALLWAY(Group.BILLY, 0, 4, 40, 30, 900, 0x5C8CFF),
    SPINNING_DISKS(Group.BILLY, 0, 4, 35, 20, 400, 0x8FE0FF),
    CURSE_ITEM(Group.BILLY, 0, 4, 30, 25, 600, 0xFF6A3A),
    COMBAT_PORTALS(Group.BILLY, 0, 4, 40, 25, 600, 0x5CC8FF),
    DREAM_BIND(Group.BILLY, 0, 5, 50, 40, 900, 0xB89CFF),
    ZERO_GRAVITY(Group.BILLY, 0, 5, 40, 25, 500, 0xCFEFFF),
    GRAVITY_SLAM(Group.BILLY, 0, 5, 40, 25, 500, 0x1B4FCC),
    PROBABILITY_HEX(Group.BILLY, 0, 5, 40, 30, 600, 0x7A5CFF),
    REALITY_SHATTER(Group.BILLY, 0, 6, 50, 30, 300, 0xFFFFFF),
    TRANSMUTE(Group.BILLY, 0, 6, 45, 30, 600, 0xFFC8F0),
    DIMENSION_GATE(Group.BILLY, 0, 7, 70, 60, 2400, 0x5CC8FF),
    UNMAKING(Group.BILLY, 0, 9, 80, 50, 1800, 0xFFFFFF),
    // ===== the Demiurge =====
    GRANT_MAGIC(Group.DEMIURGE, 0, 0, 0, 0, 40, 0xFFFFFF);

    /** Who may learn a spell. */
    public enum Group {
        GREEN, POTIONS, PROTECTION, DIVINATION, SPIRIT, SORCERER,
        /** Every witch and sorcerer (and Billy, from {@link Spell#billyRank}). */
        WITCH,
        /** Whoever has read the Darkhold (a witch, a sorcerer, Billy): forbidden, they corrupt. */
        DARKHOLD,
        /** Whoever has read the Book of the Vishanti. */
        VISHANTI,
        /** Whoever has read Cagliostro's grimoire. */
        CAGLIOSTRO,
        /** Billy, from his rank. */
        BILLY,
        /** The Demiurge only. */
        DEMIURGE;

        /** The path whose spells these are, or null (not a path's). */
        @Nullable
        public WitchPath path() {
            return switch (this) {
                case GREEN -> WitchPath.GREEN;
                case POTIONS -> WitchPath.POTIONS;
                case PROTECTION -> WitchPath.PROTECTION;
                case DIVINATION -> WitchPath.DIVINATION;
                case SPIRIT -> WitchPath.SPIRIT;
                case SORCERER -> WitchPath.SORCERER;
                default -> null;
            };
        }

        /** The grimoire flag of {@link MagicData} that teaches these, or 0. */
        public int grimoire() {
            return switch (this) {
                case DARKHOLD -> MagicData.DARKHOLD;
                case VISHANTI -> MagicData.VISHANTI;
                case CAGLIOSTRO -> MagicData.CAGLIOSTRO;
                default -> 0;
            };
        }
    }

    public final Group group;
    /** The level of witchcraft a witch (or a sorcerer) needs. */
    public final int level;
    /** The rank Billy needs (0: Billy casts it only as the Demiurge, or after reading its grimoire). */
    public final int billyRank;
    public final int mana;
    /** Ticks of incantation (0: at once). */
    public final int chant;
    public final int cooldown;
    public final int color;

    Spell(Group group, int level, int billyRank, int mana, int chant, int cooldown, int color) {
        this.group = group;
        this.level = level;
        this.billyRank = billyRank;
        this.mana = mana;
        this.chant = chant;
        this.cooldown = cooldown;
        this.color = color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Whether it is one of the Darkhold's: casting it darkens the caster's magic. */
    public boolean forbidden() {
        return group == Group.DARKHOLD;
    }

    public Component displayName() {
        return Component.translatable("spell.enhanced." + id());
    }

    public Component description() {
        return Component.translatable("spell.enhanced." + id() + ".desc");
    }

    /** The words a witch chants. */
    public Component chantWords() {
        return Component.translatable("spell.enhanced." + id() + ".chant");
    }

    /** What Billy wants ("I want ..."). */
    public Component wish() {
        return Component.translatable("spell.enhanced." + id() + ".wish");
    }

    @Nullable
    public static Spell byId(String id) {
        for (Spell spell : values()) {
            if (spell.id().equals(id)) {
                return spell;
            }
        }
        return null;
    }
}
