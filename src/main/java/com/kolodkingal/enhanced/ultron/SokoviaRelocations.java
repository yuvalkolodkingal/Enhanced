package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The players who were on the island when it went away while they were offline, and where to find them: a player who
 * logs out in the sky is saved in the sky, and the next time they log in (after the fight has ended, or a crash has
 * been recovered) the island is not there. Each record is a box in the sky round the island and how far down the ground
 * is; a player who logs in inside the box is set down on the ground below, and the record is dropped either way. Saved
 * with the Overworld, so it is the same from every dimension and survives a restart (see {@code
 * ModAttachments.SOKOVIA_RELOCATIONS}).
 *
 * <p>The record never changes: every {@code with...} method returns a new one, which {@link #store} puts back.
 *
 * @param entries the box of each player, by their UUID
 */
public record SokoviaRelocations(Map<UUID, Entry> entries) {
    public static final SokoviaRelocations EMPTY = new SokoviaRelocations(Map.of());

    /**
     * Where the island was: a cylinder of {@code radius} round {@code (x, z)} between two heights, and how far down the
     * ground is below it.
     */
    public record Entry(double x, double z, double radius, double minY, double maxY, double down) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Entry::x),
                Codec.DOUBLE.fieldOf("z").forGetter(Entry::z),
                Codec.DOUBLE.fieldOf("radius").forGetter(Entry::radius),
                Codec.DOUBLE.fieldOf("min_y").forGetter(Entry::minY),
                Codec.DOUBLE.fieldOf("max_y").forGetter(Entry::maxY),
                Codec.DOUBLE.fieldOf("down").forGetter(Entry::down)
        ).apply(i, Entry::new));

        /** Whether a player saved at this point was in the sky round the island. */
        public boolean contains(Vec3 position) {
            return Math.hypot(position.x - x, position.z - z) <= radius && position.y >= minY && position.y <= maxY;
        }
    }

    public static final Codec<SokoviaRelocations> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC)
            .xmap(SokoviaRelocations::new, SokoviaRelocations::entries);

    @Nullable
    public Entry of(UUID who) {
        return entries.get(who);
    }

    public SokoviaRelocations with(UUID who, Entry entry) {
        Map<UUID, Entry> next = new HashMap<>(entries);
        next.put(who, entry);
        return new SokoviaRelocations(Map.copyOf(next));
    }

    public SokoviaRelocations without(UUID who) {
        if (!entries.containsKey(who)) {
            return this;
        }
        Map<UUID, Entry> next = new HashMap<>(entries);
        next.remove(who);
        return new SokoviaRelocations(Map.copyOf(next));
    }

    // ---- the world's copy ----

    /** The records of this world (kept on the Overworld). */
    public static SokoviaRelocations of(MinecraftServer server) {
        return server.overworld().getData(ModAttachments.SOKOVIA_RELOCATIONS);
    }

    /** Puts the records back (which also marks the Overworld for saving). */
    public static void store(MinecraftServer server, SokoviaRelocations data) {
        server.overworld().setData(ModAttachments.SOKOVIA_RELOCATIONS, data);
    }
}
