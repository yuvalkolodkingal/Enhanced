package com.kolodkingal.enhanced.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Whose Mjolnir this is: the player who claimed it, and their name as it was then, for the tooltip and the "only X can
 * lift it" message. Kept on the item as the {@code enhanced:owner} data component (see
 * {@link com.kolodkingal.enhanced.registry.ModDataComponents}), so it survives the hammer being thrown, dropped or set
 * down as a block.
 */
public record MjolnirOwner(UUID id, String name) {
    public static final int MAX_NAME_LENGTH = 64;

    public static final Codec<MjolnirOwner> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(MjolnirOwner::id),
            Codec.string(0, MAX_NAME_LENGTH).fieldOf("name").forGetter(MjolnirOwner::name)
    ).apply(i, MjolnirOwner::new));

    public static final StreamCodec<ByteBuf, MjolnirOwner> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, MjolnirOwner::id,
            ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH), MjolnirOwner::name,
            MjolnirOwner::new);

    public static MjolnirOwner of(Player player) {
        return new MjolnirOwner(player.getUUID(), player.getGameProfile().getName());
    }

    public boolean is(Player player) {
        return id.equals(player.getUUID());
    }
}
