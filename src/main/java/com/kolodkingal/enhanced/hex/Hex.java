package com.kolodkingal.enhanced.hex;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One of Wanda's Hexes: a sphere of red energy centred on {@link #center()}. Immutable; changes produce a copy.
 *
 * @param allowed        players Wanda allowed to cross the wall
 * @param allowCreatures whether mobs and animals may cross the wall
 * @param createdAt      game time of creation (used for the rising animation)
 * @param visionStays    Wanda's order to the Vision her Hex raised: he holds his post instead of following her
 * @param runes          3.3: the Hex Runes are inscribed on its four walls (every other sorcerer inside is powerless)
 * @param runesAt        game time the runes were inscribed or wiped (for their glow fading in and out)
 * @param color          3.5: the colour its walls take (0xRRGGBB: a custom hero's Hex in the colour his pick chose), or -1
 *                       for the Hex's own raspberry red
 */
public record Hex(UUID id, UUID owner, String ownerName, Vec3 center, int radius, Set<UUID> allowed,
                  boolean allowCreatures, long createdAt, boolean visionStays, boolean runes, long runesAt, int color) {

    /** A Hex in its own colours (as every Hex before 3.5). */
    public Hex(UUID id, UUID owner, String ownerName, Vec3 center, int radius, Set<UUID> allowed, boolean allowCreatures,
               long createdAt, boolean visionStays, boolean runes, long runesAt) {
        this(id, owner, ownerName, center, radius, allowed, allowCreatures, createdAt, visionStays, runes, runesAt, -1);
    }

    /** A Hex without runes (as every Hex before 3.3). */
    public Hex(UUID id, UUID owner, String ownerName, Vec3 center, int radius, Set<UUID> allowed, boolean allowCreatures,
               long createdAt, boolean visionStays) {
        this(id, owner, ownerName, center, radius, allowed, allowCreatures, createdAt, visionStays, false, 0L);
    }

    public static final Codec<Hex> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Hex::id),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Hex::owner),
            Codec.STRING.optionalFieldOf("owner_name", "").forGetter(Hex::ownerName),
            Vec3.CODEC.fieldOf("center").forGetter(Hex::center),
            Codec.INT.fieldOf("radius").forGetter(Hex::radius),
            UUIDUtil.CODEC.listOf().optionalFieldOf("allowed", List.of()).forGetter(h -> List.copyOf(h.allowed())),
            Codec.BOOL.optionalFieldOf("allow_creatures", false).forGetter(Hex::allowCreatures),
            Codec.LONG.optionalFieldOf("created_at", 0L).forGetter(Hex::createdAt),
            Codec.BOOL.optionalFieldOf("vision_stays", false).forGetter(Hex::visionStays),
            Codec.BOOL.optionalFieldOf("runes", false).forGetter(Hex::runes),
            Codec.LONG.optionalFieldOf("runes_at", 0L).forGetter(Hex::runesAt),
            Codec.INT.optionalFieldOf("color", -1).forGetter(Hex::color)
    ).apply(i, (id, owner, name, center, radius, allowed, creatures, created, stays, runes, runesAt, color) ->
            new Hex(id, owner, name, center, radius, Set.copyOf(allowed), creatures, created, stays, runes, runesAt, color)));

    public static final StreamCodec<ByteBuf, Hex> STREAM_CODEC = StreamCodec.of((buf, h) -> {
        UUIDUtil.STREAM_CODEC.encode(buf, h.id);
        UUIDUtil.STREAM_CODEC.encode(buf, h.owner);
        ByteBufCodecs.STRING_UTF8.encode(buf, h.ownerName);
        buf.writeDouble(h.center.x);
        buf.writeDouble(h.center.y);
        buf.writeDouble(h.center.z);
        ByteBufCodecs.VAR_INT.encode(buf, h.radius);
        ByteBufCodecs.VAR_INT.encode(buf, h.allowed.size());
        for (UUID u : h.allowed) {
            UUIDUtil.STREAM_CODEC.encode(buf, u);
        }
        buf.writeBoolean(h.allowCreatures);
        ByteBufCodecs.VAR_LONG.encode(buf, h.createdAt);
        buf.writeBoolean(h.visionStays);
        buf.writeBoolean(h.runes);
        ByteBufCodecs.VAR_LONG.encode(buf, h.runesAt);
        buf.writeInt(h.color);
    }, buf -> {
        UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
        UUID owner = UUIDUtil.STREAM_CODEC.decode(buf);
        String name = ByteBufCodecs.STRING_UTF8.decode(buf);
        Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int radius = ByteBufCodecs.VAR_INT.decode(buf);
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        Set<UUID> allowed = new HashSet<>();
        for (int k = 0; k < n; k++) {
            allowed.add(UUIDUtil.STREAM_CODEC.decode(buf));
        }
        boolean creatures = buf.readBoolean();
        long created = ByteBufCodecs.VAR_LONG.decode(buf);
        boolean stays = buf.readBoolean();
        boolean runes = buf.readBoolean();
        long runesAt = ByteBufCodecs.VAR_LONG.decode(buf);
        int color = buf.readInt();
        return new Hex(id, owner, name, center, radius, Set.copyOf(allowed), creatures, created, stays, runes, runesAt, color);
    });

    /** Whether a point is inside the wall. */
    public boolean contains(Vec3 point) {
        return point.distanceToSqr(center) < (double) radius * radius;
    }

    public AABB bounds(double margin) {
        return new AABB(center, center).inflate(radius + margin);
    }

    public boolean isAllowed(UUID player) {
        return player.equals(owner) || allowed.contains(player);
    }

    public Hex withAllowed(UUID player, boolean allow) {
        Set<UUID> copy = new HashSet<>(allowed);
        if (allow) {
            copy.add(player);
        } else {
            copy.remove(player);
        }
        return new Hex(id, owner, ownerName, center, radius, Set.copyOf(copy), allowCreatures, createdAt, visionStays, runes, runesAt, color);
    }

    public Hex withAllowCreatures(boolean allow) {
        return new Hex(id, owner, ownerName, center, radius, allowed, allow, createdAt, visionStays, runes, runesAt, color);
    }

    public Hex withRadius(int newRadius) {
        return new Hex(id, owner, ownerName, center, newRadius, allowed, allowCreatures, createdAt, visionStays, runes, runesAt, color);
    }

    public Hex withVisionStays(boolean stays) {
        return new Hex(id, owner, ownerName, center, radius, allowed, allowCreatures, createdAt, stays, runes, runesAt, color);
    }

    /** The same Hex with its runes inscribed (or wiped) at game time {@code at}. */
    public Hex withRunes(boolean on, long at) {
        return new Hex(id, owner, ownerName, center, radius, allowed, allowCreatures, createdAt, visionStays, on, at, color);
    }
}
