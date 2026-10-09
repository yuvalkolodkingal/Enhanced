package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.SlowMotion;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: a projectile entered Quicksilver Time (factor = how many times slower) or left it (factor 0), so the
 * client's copy flies exactly like the server's.
 */
public record SlowedProjectilePayload(int entityId, int factor) implements CustomPacketPayload {
    public static final Type<SlowedProjectilePayload> TYPE = new Type<>(Enhanced.id("slowed_projectile"));

    public static final StreamCodec<ByteBuf, SlowedProjectilePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlowedProjectilePayload::entityId,
            ByteBufCodecs.VAR_INT, SlowedProjectilePayload::factor,
            SlowedProjectilePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void handle(SlowedProjectilePayload payload, IPayloadContext context) {
        Entity entity = context.player().level().getEntity(payload.entityId());
        if (entity != null) {
            SlowMotion.onSlowedSync(entity, payload.factor());
        }
    }
}
