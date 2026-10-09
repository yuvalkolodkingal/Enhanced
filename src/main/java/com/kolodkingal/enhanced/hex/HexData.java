package com.kolodkingal.enhanced.hex;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Every Hex in one dimension. Stored as a level attachment, so it is saved with the world and synced to every
 * player in that dimension.
 */
public record HexData(List<Hex> hexes) {
    public static final HexData EMPTY = new HexData(List.of());

    public static final Codec<HexData> CODEC = Hex.CODEC.listOf().xmap(HexData::new, HexData::hexes);

    public static final StreamCodec<ByteBuf, HexData> STREAM_CODEC =
            Hex.STREAM_CODEC.apply(ByteBufCodecs.list()).map(HexData::new, HexData::hexes);

    public HexData {
        hexes = List.copyOf(hexes);
    }

    public Optional<Hex> byOwner(UUID owner) {
        return hexes.stream().filter(h -> h.owner().equals(owner)).findFirst();
    }

    public HexData with(Hex hex) {
        List<Hex> list = new ArrayList<>(hexes);
        list.removeIf(h -> h.id().equals(hex.id()));
        list.add(hex);
        return new HexData(list);
    }

    public HexData without(UUID hexId) {
        List<Hex> list = new ArrayList<>(hexes);
        list.removeIf(h -> h.id().equals(hexId));
        return new HexData(list);
    }

    public HexData update(UUID hexId, UnaryOperator<Hex> change) {
        List<Hex> list = new ArrayList<>(hexes.size());
        for (Hex h : hexes) {
            list.add(h.id().equals(hexId) ? change.apply(h) : h);
        }
        return new HexData(list);
    }
}
