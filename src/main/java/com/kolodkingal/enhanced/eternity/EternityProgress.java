package com.kolodkingal.enhanced.eternity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * A player's journey to Eternity (3.8): the attachment {@code ModAttachments.ETERNITY} (saved, kept on death: one wish
 * per player per world; synced to him). Immutable: every change makes a new record. Written by the 3.8 skeleton; lane
 * L8 drives it.
 *
 * @param wishes   the wishes he has made ({@code [eternity] wishesPerPlayer})
 * @param lastWish what he wished for last ({@link #NONE}, {@link #ITEM}, {@link #LOVE})
 * @param returnTo where he came from, while he is in Eternity's realm
 * @param visits   how often he has been to the realm
 */
public record EternityProgress(int wishes, byte lastWish, Optional<ReturnPoint> returnTo, int visits) {
    public static final byte NONE = 0;
    public static final byte ITEM = 1;
    public static final byte LOVE = 2;
    public static final EternityProgress EMPTY = new EternityProgress(0, NONE, Optional.empty(), 0);

    public static final Codec<EternityProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("wishes", 0).forGetter(EternityProgress::wishes),
            Codec.BYTE.optionalFieldOf("last_wish", NONE).forGetter(EternityProgress::lastWish),
            ReturnPoint.CODEC.optionalFieldOf("return_to").forGetter(EternityProgress::returnTo),
            Codec.INT.optionalFieldOf("visits", 0).forGetter(EternityProgress::visits)
    ).apply(i, EternityProgress::new));

    public static final StreamCodec<ByteBuf, EternityProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EternityProgress::wishes,
            ByteBufCodecs.BYTE, EternityProgress::lastWish,
            ByteBufCodecs.optional(ReturnPoint.STREAM_CODEC), EternityProgress::returnTo,
            ByteBufCodecs.VAR_INT, EternityProgress::visits,
            EternityProgress::new);

    public EternityProgress withWish(byte what) {
        return new EternityProgress(wishes + 1, what, returnTo, visits);
    }

    public EternityProgress withReturnTo(Optional<ReturnPoint> point) {
        return new EternityProgress(wishes, lastWish, point, visits);
    }

    public EternityProgress withVisit() {
        return new EternityProgress(wishes, lastWish, returnTo, visits + 1);
    }

    /** {@code /enhanced eternity reset}: the wishes given back (his visits and the way home kept). */
    public EternityProgress reset() {
        return new EternityProgress(0, NONE, returnTo, visits);
    }

    /**
     * Where a traveller to Eternity came from: the dimension, the spot and the way he faced.
     *
     * @param dimension the level he left
     * @param pos       where he stood
     * @param yRot      his yaw
     * @param xRot      his pitch
     */
    public record ReturnPoint(ResourceKey<Level> dimension, Vec3 pos, float yRot, float xRot) {
        public static final Codec<ReturnPoint> CODEC = RecordCodecBuilder.create(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(ReturnPoint::dimension),
                Vec3.CODEC.fieldOf("pos").forGetter(ReturnPoint::pos),
                Codec.FLOAT.optionalFieldOf("y_rot", 0.0F).forGetter(ReturnPoint::yRot),
                Codec.FLOAT.optionalFieldOf("x_rot", 0.0F).forGetter(ReturnPoint::xRot)
        ).apply(i, ReturnPoint::new));

        private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

        public static final StreamCodec<ByteBuf, ReturnPoint> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION), ReturnPoint::dimension,
                VEC3, ReturnPoint::pos,
                ByteBufCodecs.FLOAT, ReturnPoint::yRot,
                ByteBufCodecs.FLOAT, ReturnPoint::xRot,
                ReturnPoint::new);
    }
}
