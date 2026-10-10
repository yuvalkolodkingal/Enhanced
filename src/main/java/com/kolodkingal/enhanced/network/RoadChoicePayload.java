package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.road.WitchesRoad;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: a seeker chose whose road to walk (Billy's or Tommy's) at an open Witches' Road door. */
public record RoadChoicePayload(BlockPos door, Power choice) implements CustomPacketPayload {
    public static final Type<RoadChoicePayload> TYPE = new Type<>(Enhanced.id("road_choice"));

    private static final StreamCodec<ByteBuf, Power> POWER_CODEC =
            ByteBufCodecs.idMapper(i -> Power.values()[Math.floorMod(i, Power.values().length)], Power::ordinal);

    public static final StreamCodec<ByteBuf, RoadChoicePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RoadChoicePayload::door,
            POWER_CODEC, RoadChoicePayload::choice,
            RoadChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(RoadChoicePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            WitchesRoad.choose(player, payload.door(), payload.choice());
        }
    }
}
