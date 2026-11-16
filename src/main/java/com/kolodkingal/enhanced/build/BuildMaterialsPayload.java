package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client (3.16): {@code /enhanced build materials on|off} changed whether spells take their blocks from the
 * inventory; the client's palette shows only the blocks a Survival player has while it is on.
 */
public record BuildMaterialsPayload(boolean on) implements CustomPacketPayload {
    public static final Type<BuildMaterialsPayload> TYPE = new Type<>(Enhanced.id("build_materials"));
    public static final StreamCodec<ByteBuf, BuildMaterialsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BuildMaterialsPayload::on, BuildMaterialsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuildMaterialsPayload payload, PacketContext context) {
        // (a world of this client already has it: the server and the client share the config there)
        if (BuildConfig.REQUIRE_MATERIALS.get() != payload.on()) {
            BuildConfig.REQUIRE_MATERIALS.set(payload.on());
        }
    }
}
