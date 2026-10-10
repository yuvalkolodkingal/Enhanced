package com.kolodkingal.enhanced.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A player's Bifrost: the places they have marked for it (where they stood, in which dimension, facing which way), and
 * the game time when it can open again. Saved with the player, kept on death, and synced to that player alone (nobody
 * else sees where their Bifrost can take them).
 */
public record BifrostData(List<Place> places, long readyAt) {
    public static final int MAX_PLACES = 24;
    public static final int MAX_NAME_LENGTH = 32;
    public static final BifrostData EMPTY = new BifrostData(List.of(), 0L);

    public record Place(String name, ResourceKey<Level> dimension, Vec3 pos, float yaw) {
        public static final Codec<Place> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Place::name),
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Place::dimension),
                Vec3.CODEC.fieldOf("pos").forGetter(Place::pos),
                Codec.FLOAT.optionalFieldOf("yaw", 0F).forGetter(Place::yaw)
        ).apply(i, Place::new));

        private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

        public static final StreamCodec<ByteBuf, Place> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(MAX_NAME_LENGTH * 4), Place::name,
                ResourceKey.streamCodec(Registries.DIMENSION), Place::dimension,
                VEC3, Place::pos,
                ByteBufCodecs.FLOAT, Place::yaw,
                Place::new);
    }

    public static final Codec<BifrostData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Place.CODEC.listOf().optionalFieldOf("places", List.of()).forGetter(BifrostData::places),
            Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(BifrostData::readyAt)
    ).apply(i, BifrostData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BifrostData> STREAM_CODEC = StreamCodec.<ByteBuf, BifrostData, List<Place>, Long>composite(
            Place.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PLACES)), BifrostData::places,
            ByteBufCodecs.VAR_LONG, BifrostData::readyAt,
            BifrostData::new).cast();

    public BifrostData {
        places = List.copyOf(places);
    }

    public boolean isFull() {
        return places.size() >= MAX_PLACES;
    }

    /**
     * This list with {@code place} added; a place with the same name (ignoring case) is replaced instead, so saving
     * "Home" again moves it. Returns this list unchanged if it is full and the name is new.
     */
    public BifrostData with(Place place) {
        List<Place> updated = new ArrayList<>(places);
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).name().equalsIgnoreCase(place.name())) {
                updated.set(i, place);
                return new BifrostData(updated, readyAt);
            }
        }
        if (isFull()) {
            return this;
        }
        updated.add(place);
        return new BifrostData(updated, readyAt);
    }

    /** This list without the place at {@code index} (unchanged if there is none). */
    public BifrostData without(int index) {
        if (index < 0 || index >= places.size()) {
            return this;
        }
        List<Place> updated = new ArrayList<>(places);
        updated.remove(index);
        return new BifrostData(updated, readyAt);
    }

    /** The same places; the Bifrost opens again at game time {@code readyAt}. */
    public BifrostData readyAt(long readyAt) {
        return new BifrostData(places, readyAt);
    }

    /** Ticks until the Bifrost can open again (0 if it can now). */
    public long cooldown(long gameTime) {
        return Math.max(0L, readyAt - gameTime);
    }

    /**
     * A safe display name from what the player typed: no formatting codes or control characters, trimmed, at most
     * {@link #MAX_NAME_LENGTH} characters; {@code fallback} if nothing is left.
     */
    public static String cleanName(String typed, String fallback) {
        String stripped = ChatFormatting.stripFormatting(typed == null ? "" : typed);
        StringBuilder out = new StringBuilder();
        stripped.codePoints().filter(c -> !Character.isISOControl(c) && c != '§').forEach(out::appendCodePoint);
        String name = out.toString().strip();
        if (name.codePointCount(0, name.length()) > MAX_NAME_LENGTH) {
            name = name.substring(0, name.offsetByCodePoints(0, MAX_NAME_LENGTH)).strip();
        }
        return name.isEmpty() ? fallback : name;
    }

    /** "overworld", "the_nether", "the_end", or the namespaced id of a modded dimension. */
    public static String dimensionId(ResourceKey<Level> dimension) {
        return dimension.location().getNamespace().equals("minecraft")
                ? dimension.location().getPath().toLowerCase(Locale.ROOT) : dimension.location().toString();
    }
}
