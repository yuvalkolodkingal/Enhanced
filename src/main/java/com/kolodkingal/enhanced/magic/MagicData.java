package com.kolodkingal.enhanced.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * A player's magic (3.17): the attachment {@code ModAttachments.MAGIC} (saved, kept on death, synced to all who see him).
 * Immutable: every change makes a new record. A witch or a sorcerer has a {@link WitchPath} and a level; anyone who casts
 * spells (a witch, a sorcerer, Billy) has a mana pool, a chosen spell and the grimoires he has read; reading the
 * Darkhold's forbidden spells corrupts his magic.
 *
 * @param path       the {@link WitchPath}'s ordinal, or -1: no witchcraft of his own
 * @param level      1 to {@link WitchPath#MAX_LEVEL} (meaningless without a path)
 * @param flags      the grimoires read ({@link #DARKHOLD}, {@link #VISHANTI}, {@link #CAGLIOSTRO}) and {@link #GRANTED}
 * @param corruption 0 to 100: how far the Darkhold has darkened his magic
 * @param selected   the id of the spell the Spells key casts ("" for none yet)
 * @param mana       the mana he has now
 */
public record MagicData(int path, int level, int flags, int corruption, String selected, float mana) {
    public static final MagicData EMPTY = new MagicData(-1, 1, 0, 0, "", 0.0F);

    /** He has read the Darkhold: its forbidden spells are his. */
    public static final int DARKHOLD = 1;
    /** He has read the Book of the Vishanti: its spells of light are his. */
    public static final int VISHANTI = 2;
    /** He has read Cagliostro's grimoire: its spells of time are his. */
    public static final int CAGLIOSTRO = 4;
    /** His witchcraft is a Demiurge's gift (the Demiurge, or an operator, may take it back). */
    public static final int GRANTED = 256;
    /** The Coven's Road unbound him: no mind control, no curse and no rune of a Hex holds him (until he dies). */
    public static final int ROAD_UNBOUND = 512;
    /** Billy's Unbinding freed him a while ago: the same, for a minute (the server takes the flag away again). */
    public static final int UNBOUND = 1024;
    /**
     * His witchcraft is Billy's own gift (the Demiurge's Grant Magic, not an operator's command or the Road's prize): the
     * Witches' Mindscape, Billy's place of meditation, is open to him. (Bits 12 to 15 are the Demiurge's magic type.)
     */
    public static final int BILLYS = 2048;

    public static final Codec<MagicData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("path", "").forGetter(d -> d.witchPath() == null ? "" : d.witchPath().id()),
            Codec.INT.optionalFieldOf("level", 1).forGetter(MagicData::level),
            Codec.INT.optionalFieldOf("flags", 0).forGetter(MagicData::flags),
            Codec.INT.optionalFieldOf("corruption", 0).forGetter(MagicData::corruption),
            Codec.STRING.optionalFieldOf("selected", "").forGetter(MagicData::selected),
            Codec.FLOAT.optionalFieldOf("mana", 0.0F).forGetter(MagicData::mana)
    ).apply(i, (path, level, flags, corruption, selected, mana) -> {
        WitchPath p = WitchPath.byId(path);
        return new MagicData(p == null ? -1 : p.ordinal(), level, flags, corruption, selected, mana);
    }));

    public static final StreamCodec<ByteBuf, MagicData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, d -> d.path() + 1,
            ByteBufCodecs.VAR_INT, MagicData::level,
            ByteBufCodecs.VAR_INT, MagicData::flags,
            ByteBufCodecs.VAR_INT, MagicData::corruption,
            ByteBufCodecs.stringUtf8(64), MagicData::selected,
            ByteBufCodecs.FLOAT, MagicData::mana,
            (path, level, flags, corruption, selected, mana) -> new MagicData(path - 1, level, flags, corruption, selected, mana));

    public MagicData {
        level = Mth.clamp(level, 1, WitchPath.MAX_LEVEL);
        corruption = Mth.clamp(corruption, 0, 100);
        selected = selected == null ? "" : selected;
    }

    /** His path, or null: no witchcraft of his own. */
    @Nullable
    public WitchPath witchPath() {
        return WitchPath.byOrdinal(path);
    }

    /** Whether he is a witch or a sorcerer (of the witchcraft of 3.17). */
    public boolean hasCraft() {
        return witchPath() != null;
    }

    public boolean has(int flag) {
        return (flags & flag) == flag;
    }

    public MagicData with(int flag) {
        return new MagicData(path, level, flags | flag, corruption, selected, mana);
    }

    public MagicData without(int flag) {
        return new MagicData(path, level, flags & ~flag, corruption, selected, mana);
    }

    /** A witch (or sorcerer) of {@code path} at {@code level}; {@code granted}: a Demiurge's gift. */
    public MagicData withCraft(@Nullable WitchPath newPath, int newLevel, boolean granted) {
        int f = (granted ? flags | GRANTED : flags & ~GRANTED) & ~BILLYS; // (Billy's own gift is marked after, Grants)
        return new MagicData(newPath == null ? -1 : newPath.ordinal(), newLevel, f, corruption, "", mana);
    }

    public MagicData withCorruption(int value) {
        return new MagicData(path, level, flags, value, selected, mana);
    }

    public MagicData withSelected(String spell) {
        return new MagicData(path, level, flags, corruption, spell, mana);
    }

    public MagicData withMana(float value) {
        return new MagicData(path, level, flags, corruption, selected, value);
    }
}
