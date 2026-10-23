package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadEnding;
import com.kolodkingal.enhanced.road.WitchesRoad;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client → server: a seeker answered at an open Witches' Road door: he chose whose road to walk (Billy's or Tommy's), or, with
 * {@link Power#NONE}, "Walk the Road" (the trials). At the end of the Road, in the Road itself, the same message is the answer there
 * ({@link RoadEnding}: {@link Power#NONE} means "Leave the Road"); the server goes by where the player is, never by what the message
 * says, and checks everything again.
 */
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

    static void handle(RoadChoicePayload payload, PacketContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (RoadEnding.isAtEnding(player)) {
                if (payload.choice() == Power.NONE) {
                    RoadEnding.leaveUnchosen(player);
                } else {
                    RoadEnding.choose(player, payload.choice());
                }
            } else if (RoadDimension.in(player)) {
                // Anywhere else in the Road there is nothing to answer.
                return;
            } else if (payload.choice() == Power.NONE) {
                WitchesRoad.enter(player, payload.door());
            } else {
                WitchesRoad.choose(player, payload.door(), payload.choice());
            }
        }
    }
}
