package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.network.RoadMusicPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.boat.IceBoatChallenge;
import com.kolodkingal.enhanced.road.copy.StructureCopyChallenge;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The music of the Witches' Road (3.3): an original witchy ballad, one arrangement for each stage of the Road. The server
 * knows where each player is on the Road and tells his client the stage ({@link RoadMusicPayload}) whenever it changes;
 * the client plays the stage's loop on the music slider, crossfading from one stage to the next, and fades it out when
 * he is off the Road.
 * <ul>
 *     <li>{@code entrance}: from the moment he asks a door for the Road (its ritual) while the door is open and he stays
 *     by it, and while he is being taken into the Road;</li>
 *     <li>the id of the trial he is in ({@code parkour}, {@code structure_copy}, {@code ice_boat}), and {@code road_end}
 *     in the hall at the end;</li>
 *     <li>nothing anywhere else.</li>
 * </ul>
 * The sound events are {@code enhanced:music.road.entrance}, {@code .parkour}, {@code .copy}, {@code .boat} and
 * {@code .ending}: a resource pack replaces the music by giving them other files. A trial added later that has no
 * arrangement of its own plays the parkour's.
 */
public final class RoadMusic {
    /** No music: the player is not on the Road. */
    public static final String NONE = "";
    /** At the door and on the way in. */
    public static final String ENTRANCE = "entrance";
    /** How often the stage of every player is looked at (half a second). */
    static final int INTERVAL = 10;
    /** How far from the door he asked a seeker may stand and still hear the entrance. */
    static final double DOOR_REACH = 16.0;

    /** The stage each player's client was last told. */
    private static final Map<UUID, String> SENT = new HashMap<>();
    /** The door each seeker asked for the Road, while its ritual runs or it stands open. */
    private static final Map<UUID, GlobalPos> SEEKING = new HashMap<>();

    /** The sound of a stage, or null for none. */
    @Nullable
    public static SoundEvent soundOf(String stage) {
        return switch (stage) {
            case NONE -> null;
            case ENTRANCE -> ModSounds.ROAD_MUSIC_ENTRANCE.get();
            case StructureCopyChallenge.ID -> ModSounds.ROAD_MUSIC_COPY.get();
            case IceBoatChallenge.ID -> ModSounds.ROAD_MUSIC_BOAT.get();
            case EndingHall.ID -> ModSounds.ROAD_MUSIC_ENDING.get();
            default -> ModSounds.ROAD_MUSIC_PARKOUR.get();
        };
    }

    /** A seeker asked the door at {@code door} for the Road (its ritual started, or it was open already). */
    static void seeking(ServerPlayer player, GlobalPos door) {
        SEEKING.put(player.getUUID(), door);
    }

    /** The stage of the Road the player is at now. */
    public static String stageOf(ServerPlayer player) {
        UUID id = player.getUUID();
        RoadTrial trial = RoadTrials.trialOf(id);
        if (trial != null) {
            return trial.challenge().id();
        }
        if (RoadTrials.isInside(player)) {
            return ENTRANCE;
        }
        GlobalPos door = SEEKING.get(id);
        if (door != null) {
            ServerLevel level = player.server.getLevel(door.dimension());
            boolean open = level != null && level.isLoaded(door.pos()) && (WitchesRoad.ritualRunning(door) || WitchesRoad.isOpenDoor(level, door.pos()));
            if (open && player.level().dimension().equals(door.dimension())
                    && player.position().distanceToSqr(Vec3.atBottomCenterOf(door.pos())) <= DOOR_REACH * DOOR_REACH) {
                return ENTRANCE;
            }
            SEEKING.remove(id);
        }
        return NONE;
    }

    /** Every server tick: every {@link #INTERVAL} ticks, tells each client whose stage has changed. */
    static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String stage = stageOf(player);
            if (!stage.equals(SENT.getOrDefault(player.getUUID(), NONE))) {
                SENT.put(player.getUUID(), stage);
                PacketDistributor.sendToPlayer(player, new RoadMusicPayload(stage));
            }
        }
    }

    /** The stage the player's client was last told (for tests). */
    public static String sentTo(UUID player) {
        return SENT.getOrDefault(player, NONE);
    }

    /** A player left the game: his client forgets its music by itself. */
    static void forget(UUID player) {
        SENT.remove(player);
        SEEKING.remove(player);
    }

    /** Forget everything (the server stopped). */
    static void clear() {
        SENT.clear();
        SEEKING.clear();
    }

    private RoadMusic() {
    }
}
