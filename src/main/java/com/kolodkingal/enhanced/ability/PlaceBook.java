package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The places a player has saved and the journey to one: what the Bifrost (Stormbreaker and Mjolnir, 2.x) and the Book of
 * the Vishanti (3.2) share. A {@link Kind} is a book: it names the attachment that holds its list of places and its
 * cooldown (a {@link BifrostData}), and the lang prefix of its messages. This class saves, finds and forgets places, runs
 * the cooldown, holds the journey of a traveller (a few ticks of gathering, then the arrival) and sets the traveller down;
 * what each book adds (who may use it, the light, the sound, the ward on a base) stays in {@code ThorAbilities} and
 * {@code VishantiBook}. Every teleport asks {@link HexManager#onTeleport} first, which is where a boss arena keeps its
 * fighters in, a Hex wall is judged and a pinned creature stays put.
 */
public final class PlaceBook {
    /** If a saved place has been built over, the traveller is set down on the first free spot up to this high. */
    public static final int ARRIVAL_SEARCH = 24;

    /** The books that keep places. */
    public enum Kind {
        BIFROST(ModAttachments.BIFROST, "bifrost"),
        VISHANTI(ModAttachments.VISHANTI, "vishanti");

        private final Supplier<AttachmentType<BifrostData>> attachment;
        private final String id;

        Kind(Supplier<AttachmentType<BifrostData>> attachment, String id) {
            this.attachment = attachment;
            this.id = id;
        }

        /** This player's places in this book. */
        public BifrostData data(Player player) {
            return player.getData(attachment.get());
        }

        public void set(Player player, BifrostData data) {
            player.setData(attachment.get(), data);
        }

        /** The lang key {@code message.enhanced.<book>.<key>}. */
        public String message(String key) {
            return "message.enhanced." + id + "." + key;
        }
    }

    /** Where a traveller is being taken, and the game time it takes them. */
    public record Journey(Kind kind, ResourceKey<Level> dimension, Vec3 to, float yaw, long arrivesAt) {
    }

    /** What a completed journey left behind: where the traveller was, and where they are. */
    public record Landing(ServerLevel from, Vec3 left, ServerLevel to) {
    }

    /** Traveller -> the journey that is about to take them (one at a time, whichever book began it). */
    private static final Map<UUID, Journey> JOURNEYS = new HashMap<>();

    // ---- the list ----

    /** The place at {@code index}, if it is still the one called {@code name} (the list may have changed since a screen showed it). */
    @Nullable
    public static BifrostData.Place placeAt(Player player, Kind kind, int index, String name) {
        List<BifrostData.Place> places = kind.data(player).places();
        return index >= 0 && index < places.size() && places.get(index).name().equals(name) ? places.get(index) : null;
    }

    /**
     * Marks where the player stands, facing the way they face. Saving a name again moves that place. Returns the place, or
     * null if the list is full and the name is new.
     */
    @Nullable
    public static BifrostData.Place save(ServerPlayer player, Kind kind, String typed, boolean base) {
        BifrostData book = kind.data(player);
        BlockPos at = player.blockPosition();
        String name = BifrostData.cleanName(typed, at.getX() + ", " + at.getY() + ", " + at.getZ());
        BifrostData.Place place = new BifrostData.Place(name, player.level().dimension(), player.position(), player.getYRot(), base);
        BifrostData updated = book.with(place);
        if (updated == book) {
            return null;
        }
        kind.set(player, updated);
        return place;
    }

    /** Forgets the place at {@code index} if it is still the one called {@code name}; returns it, or null. */
    @Nullable
    public static BifrostData.Place forget(ServerPlayer player, Kind kind, int index, String name) {
        BifrostData.Place place = placeAt(player, kind, index, name);
        if (place != null) {
            kind.set(player, kind.data(player).without(index));
        }
        return place;
    }

    // ---- the cooldown ----

    /** Ticks until the book can take this player again (0 if it can now). */
    public static long cooldown(Player player, Kind kind) {
        return kind.data(player).cooldown(player.level().getGameTime());
    }

    /** The book can take this player again at game time {@code readyAt}. */
    public static void readyAt(ServerPlayer player, Kind kind, long readyAt) {
        kind.set(player, kind.data(player).readyAt(readyAt));
    }

    // ---- the journey ----

    /** Whether any book is about to take this player somewhere. */
    public static boolean travelling(Player player) {
        return JOURNEYS.containsKey(player.getUUID());
    }

    /** The journey that is about to take this player, if it is the book's, or null. */
    @Nullable
    public static Journey journey(Player player, Kind kind) {
        Journey journey = JOURNEYS.get(player.getUUID());
        return journey != null && journey.kind() == kind ? journey : null;
    }

    /** The book begins to gather its light round the player: {@code charge} ticks from now it takes them to {@code to}. */
    public static void begin(ServerPlayer player, Kind kind, ResourceKey<Level> dimension, Vec3 to, float yaw, int charge) {
        JOURNEYS.put(player.getUUID(), new Journey(kind, dimension, to, yaw, player.level().getGameTime() + charge));
    }

    /** The traveller did not get there (they died, logged out or the book was put away): the light goes out. */
    public static void cancel(Player player) {
        JOURNEYS.remove(player.getUUID());
    }

    /** Whether the journey's moment has not come yet. */
    public static boolean waiting(ServerPlayer player, Journey journey) {
        return player.level().getGameTime() < journey.arrivesAt();
    }

    /**
     * The moment has come: the traveller is set down at the destination (on the first free spot above it if it has been built
     * over since), unless the world is gone, they were not there to be taken (dead, or away: the light went out) or a teleport
     * rule refuses (see the class comment). Returns where they were and are, or null if nothing happened.
     */
    @Nullable
    public static Landing complete(ServerPlayer player, Journey journey) {
        JOURNEYS.remove(player.getUUID());
        ServerLevel destination = player.server.getLevel(journey.dimension());
        long now = player.level().getGameTime();
        if (destination == null || now > journey.arrivesAt() + 5) {
            return null;
        }
        Vec3 to = safeArrival(destination, player, journey.to());
        if (!HexManager.onTeleport(destination, player, to)) {
            return null;
        }
        ServerLevel from = player.serverLevel();
        Vec3 left = player.position();
        player.teleportTo(destination, to.x, to.y, to.z, journey.yaw(), player.getXRot());
        player.resetFallDistance();
        return new Landing(from, left, destination);
    }

    /** {@code to}, or the first spot above it where the traveller fits (a saved place may have been built over since). */
    private static Vec3 safeArrival(ServerLevel level, ServerPlayer player, Vec3 to) {
        // Collision checks treat unloaded chunks as empty air: load the landing chunk first.
        level.getChunkAt(BlockPos.containing(to));
        AABB body = player.getDimensions(Pose.STANDING).makeBoundingBox(to);
        for (int up = 0; up <= ARRIVAL_SEARCH && to.y + up < level.getMaxBuildHeight(); up++) {
            if (level.noCollision(player, body.move(0, up, 0))) {
                return to.add(0, up, 0);
            }
        }
        return to;
    }

    public static void clear() {
        JOURNEYS.clear();
    }

    private PlaceBook() {
    }
}
