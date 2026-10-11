package com.kolodkingal.enhanced.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Codecs for the pieces the payloads of 3.0 share. */
final class PayloadCodecs {
    /** A point in the world, three doubles. */
    static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    /** A block position, as vanilla sends it. */
    static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = BlockPos.STREAM_CODEC;

    private PayloadCodecs() {
    }
}
