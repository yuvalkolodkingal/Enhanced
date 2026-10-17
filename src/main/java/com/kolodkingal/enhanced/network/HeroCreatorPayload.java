package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.power.Power;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server (3.3): a button of the Hero Creator. {@link #CHOOSE}: become one of the mod's heroes in a look ({@code
 * power} and {@code look}; {@code none} takes the powers away); {@link #APPLY}: become the custom hero {@code hero}. The
 * server checks everything again (Creative or operator, the config) and judges the hero by its own rules
 * ({@code CustomHeroRules}) before anyone becomes it. The hero travels in its binary form, the skin raw (at most 24 KiB),
 * well under the 32 KiB of a packet to the server.
 *
 * @param action {@link #CHOOSE} or {@link #APPLY}
 * @param power  the hero's name ({@code thor}), for CHOOSE
 * @param look   the look's id, for CHOOSE
 * @param hero   the custom hero, for APPLY ({@link CustomHero#EMPTY} otherwise)
 */
public record HeroCreatorPayload(int action, String power, String look, CustomHero hero) implements CustomPacketPayload {
    public static final int CHOOSE = 0;
    public static final int APPLY = 1;

    public static final Type<HeroCreatorPayload> TYPE = new Type<>(Enhanced.id("hero_creator"));

    public static final StreamCodec<ByteBuf, HeroCreatorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HeroCreatorPayload::action,
            ByteBufCodecs.stringUtf8(32), HeroCreatorPayload::power,
            ByteBufCodecs.stringUtf8(64), HeroCreatorPayload::look,
            CustomHero.STREAM_CODEC, HeroCreatorPayload::hero,
            HeroCreatorPayload::new);

    public static HeroCreatorPayload choose(Power power, String look) {
        return new HeroCreatorPayload(CHOOSE, power.getSerializedName(), look, CustomHero.EMPTY);
    }

    public static HeroCreatorPayload apply(CustomHero hero) {
        return new HeroCreatorPayload(APPLY, "", "", hero);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(HeroCreatorPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (payload.action() == APPLY) {
            CustomHeroes.apply(player, payload.hero());
        } else if (payload.action() == CHOOSE) {
            Power power = Power.byName(payload.power());
            if (power != null) {
                CustomHeroes.choose(player, power, payload.look());
            }
        }
    }
}
