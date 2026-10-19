package com.kolodkingal.enhanced.deadpool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * Deadpool's state (3.7): the attachment {@code ModAttachments.DEADPOOL} (saved, not kept on death, synced to him only:
 * the HUD's ammo and Dogpool's time). Immutable: every change makes a new record. Lane L3 drives it.
 *
 * @param ammo         shots left in the magazine ({@code [deadpool] magazine} when full)
 * @param reloadAt     the game time the reload ends at (0: not reloading)
 * @param dogpool      his Dogpool, while one is out
 * @param dogpoolUntil the game time Dogpool goes home at
 */
public record DeadpoolState(int ammo, long reloadAt, Optional<UUID> dogpool, long dogpoolUntil) {
    /** A full magazine of the plan's default (16): the state of a new Deadpool. */
    public static final DeadpoolState EMPTY = new DeadpoolState(16, 0L, Optional.empty(), 0L);

    public static final Codec<DeadpoolState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("ammo", 16).forGetter(DeadpoolState::ammo),
            Codec.LONG.optionalFieldOf("reload_at", 0L).forGetter(DeadpoolState::reloadAt),
            UUIDUtil.CODEC.optionalFieldOf("dogpool").forGetter(DeadpoolState::dogpool),
            Codec.LONG.optionalFieldOf("dogpool_until", 0L).forGetter(DeadpoolState::dogpoolUntil)
    ).apply(i, DeadpoolState::new));

    public static final StreamCodec<ByteBuf, DeadpoolState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeadpoolState::ammo,
            ByteBufCodecs.VAR_LONG, DeadpoolState::reloadAt,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), DeadpoolState::dogpool,
            ByteBufCodecs.VAR_LONG, DeadpoolState::dogpoolUntil,
            DeadpoolState::new);

    public DeadpoolState withAmmo(int value) {
        return new DeadpoolState(value, reloadAt, dogpool, dogpoolUntil);
    }

    public DeadpoolState withReloadAt(long time) {
        return new DeadpoolState(ammo, time, dogpool, dogpoolUntil);
    }

    public DeadpoolState withDogpool(Optional<UUID> id, long until) {
        return new DeadpoolState(ammo, reloadAt, id, until);
    }
}
