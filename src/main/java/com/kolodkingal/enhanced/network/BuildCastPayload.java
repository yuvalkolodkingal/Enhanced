package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server: a Scarlet Witch or Billy casts a Build Magic spell, or asks what it would do ({@link #PREFLIGHT}). It
 * carries only the {@link SpellRecipe}, never cells or block states: the server plans the cells again and checks each
 * one. At most 129 bytes (with 8 seeds), far below the 32 KiB of a serverbound payload. The server answers with a
 * {@code BuildVerdictPayload} carrying the same {@code token}.
 *
 * @param token  the client's number for this cast, given back in the verdict
 * @param flags  {@link #PREFLIGHT}, {@link #HELD_SHORT}, {@link #HELD_LONG}
 * @param recipe what to cast
 */
public record BuildCastPayload(int token, int flags, SpellRecipe recipe) implements CustomPacketPayload {
    /** Only a look: the server answers what the spell would do, and nothing changes (no event is posted). */
    public static final int PREFLIGHT = 1;
    /** The right button was held the short hold (a spell that removes or replaces more than 64 blocks). */
    public static final int HELD_SHORT = 2;
    /** The right button was held the long hold (a spell over a million blocks, or one whose undo would not fit). */
    public static final int HELD_LONG = 4;

    public static final Type<BuildCastPayload> TYPE = new Type<>(Enhanced.id("build_cast"));

    public static final StreamCodec<ByteBuf, BuildCastPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.token);
                buf.writeByte(p.flags);
                SpellRecipe.STREAM_CODEC.encode(buf, p.recipe);
            },
            buf -> new BuildCastPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readByte(), SpellRecipe.STREAM_CODEC.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(BuildCastPayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BuildMagic.cast(player, payload.recipe(), payload.token(), payload.flags());
        }
    }
}
