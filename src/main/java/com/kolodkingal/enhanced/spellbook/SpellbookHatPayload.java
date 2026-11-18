package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server: the Ethereal witch chose on her hat's screen ({@code SpellbookHatScreen}): a colour ({@link #COLOR},
 * {@code value} the index of a swatch of {@link Spellbooks#HAT_COLORS}), the hat off or on ({@link #TOGGLE}), or to give
 * the book's magic up ({@link #RENOUNCE}). The server checks that she is the Ethereal witch.
 */
public record SpellbookHatPayload(int action, int value) implements CustomPacketPayload {
    public static final int COLOR = 0;
    public static final int TOGGLE = 1;
    public static final int RENOUNCE = 2;

    public static final Type<SpellbookHatPayload> TYPE = new Type<>(Enhanced.id("spellbook_hat"));

    public static final StreamCodec<ByteBuf, SpellbookHatPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpellbookHatPayload::action,
            ByteBufCodecs.VAR_INT, SpellbookHatPayload::value,
            SpellbookHatPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpellbookHatPayload payload, PacketContext context) {
        if (!(context.player() instanceof ServerPlayer player) || Spellbook.of(player) != Spellbook.ETHEREAL) {
            return;
        }
        switch (payload.action()) {
            case COLOR -> {
                Integer color = Spellbooks.hatColor(payload.value());
                if (color != null) {
                    Spellbooks.chooseHatColor(player, color);
                }
            }
            case TOGGLE -> Spellbooks.toggleHat(player);
            case RENOUNCE -> Spellbooks.renounce(player);
            default -> {
            }
        }
    }
}
