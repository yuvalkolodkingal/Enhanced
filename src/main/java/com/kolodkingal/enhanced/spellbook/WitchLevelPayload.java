package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client (3.18.1): a witch's level ({@link WitchLevels}), for her HUD line and her Book of Spells screen: her
 * source ({@code 0} none, 1-3 a book, 4 the Demiurge's gift), her level and experience, the highest level the server
 * allows, the level at which each key R G V B N first holds a spell of her own ({@code keyLevels}, 0: never) and what
 * each of the five levels opens ({@code opens}: cells "{@code column|sneak|cell}" joined by ";"). Sent when she logs in,
 * becomes or stops being a witch, and when her experience or level changes. Kept by {@link WitchLevelView}.
 */
public record WitchLevelPayload(int source, int level, int xp, int maxLevel, int[] keyLevels, List<String> opens)
        implements CustomPacketPayload {
    public static final Type<WitchLevelPayload> TYPE = new Type<>(Enhanced.id("witch_level"));
    /** No witch. */
    public static final WitchLevelPayload NONE = new WitchLevelPayload(0, 0, 0, SpellLevels.MAX, new int[5], List.of());

    private static final StreamCodec<ByteBuf, String> TEXT = ByteBufCodecs.stringUtf8(4096);

    public static final StreamCodec<ByteBuf, WitchLevelPayload> STREAM_CODEC = StreamCodec.of(WitchLevelPayload::write,
            WitchLevelPayload::read);

    private static void write(ByteBuf buf, WitchLevelPayload p) {
        ByteBufCodecs.VAR_INT.encode(buf, p.source);
        ByteBufCodecs.VAR_INT.encode(buf, p.level);
        ByteBufCodecs.VAR_INT.encode(buf, p.xp);
        ByteBufCodecs.VAR_INT.encode(buf, p.maxLevel);
        for (int i = 0; i < 5; i++) {
            ByteBufCodecs.VAR_INT.encode(buf, i < p.keyLevels.length ? p.keyLevels[i] : 0);
        }
        int n = Math.min(SpellLevels.MAX, p.opens.size());
        ByteBufCodecs.VAR_INT.encode(buf, n);
        for (int i = 0; i < n; i++) {
            String s = p.opens.get(i);
            TEXT.encode(buf, s.length() > 4096 ? s.substring(0, 4096) : s);
        }
    }

    private static WitchLevelPayload read(ByteBuf buf) {
        int source = ByteBufCodecs.VAR_INT.decode(buf);
        int level = ByteBufCodecs.VAR_INT.decode(buf);
        int xp = ByteBufCodecs.VAR_INT.decode(buf);
        int maxLevel = ByteBufCodecs.VAR_INT.decode(buf);
        int[] keys = new int[5];
        for (int i = 0; i < 5; i++) {
            keys[i] = ByteBufCodecs.VAR_INT.decode(buf);
        }
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        if (n < 0 || n > SpellLevels.MAX) {
            throw new IllegalArgumentException("A witch with " + n + " levels");
        }
        List<String> opens = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            opens.add(TEXT.decode(buf));
        }
        return new WitchLevelPayload(source, level, xp, maxLevel, keys, opens);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WitchLevelPayload payload, PacketContext context) {
        WitchLevelView.set(payload);
    }
}
