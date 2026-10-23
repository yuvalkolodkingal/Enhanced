package com.kolodkingal.enhanced.music;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.MusicConfig;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.network.MusicChunkPayload;
import com.kolodkingal.enhanced.network.MusicManifestPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadMusic;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The world's own music (3.5). A server owner puts Ogg Vorbis files in {@code <world>/serverconfig/enhanced/music/}, each
 * named after the music it replaces ({@link MusicTracks}); the server reads them when it starts, after /reload and after
 * {@code /enhanced music reload}, and tells every player who has the mod's music channel what there is (names, SHA-256,
 * sizes). A client that lacks a song asks for it and is sent it in pieces of 30 KB, a few a tick, one song at a time; it keeps
 * it by its hash, so it downloads each song once. Files that are too big ({@code music.maxFileKB}) or not Ogg Vorbis are left
 * out, and the log says why.
 */
public final class ServerMusic {
    /** Where the folder is, inside the world's folder (next to the world's own server config). */
    public static final String FOLDER = "serverconfig/" + Enhanced.MODID + "/music";

    /** One song in the folder, read and checked. */
    public record Song(String name, String file, String hash, byte[] data) {
        public int size() {
            return data.length;
        }
    }

    /** What a look at the folder found: the songs it will send, and the files it left out with the reason. */
    public record Scan(Map<String, Song> songs, Map<String, String> rejected) {
        public static final Scan EMPTY = new Scan(Map.of(), Map.of());

        public List<MusicManifestPayload.Track> manifest() {
            return songs.values().stream().map(s -> new MusicManifestPayload.Track(s.name(), s.hash(), s.size())).toList();
        }

        /** The song played for a track (the most particular name there is a file for), or null. */
        public Song resolve(String track) {
            return MusicTracks.resolve(track, songs.keySet()).map(songs::get).orElse(null);
        }
    }

    /** A song on its way to one player. */
    private static final class Transfer {
        final Song song;
        int next;

        Transfer(Song song) {
            this.song = song;
        }
    }

    private static volatile Scan current = Scan.EMPTY;
    private static final Map<UUID, Deque<Transfer>> TRANSFERS = new HashMap<>();

    /** The world's music folder. */
    public static Path folder(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(FOLDER).normalize();
    }

    /** What the server sends now. */
    public static Scan current() {
        return current;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The folder

    /** The README put in the folder: the names it takes, the Road's versions listed from its trials. */
    public static String readme() {
        StringBuilder road = new StringBuilder();
        List<String> stages = new ArrayList<>();
        stages.add(RoadMusic.ENTRANCE);
        for (RoadChallenge trial : RoadChallenges.all()) {
            stages.add(trial.id());
        }
        stages.add(RoadMusic.CHOICE);
        stages.add(com.kolodkingal.enhanced.road.EndingHall.ID);
        for (String stage : stages) {
            String file = MusicTracks.road(stage) + ".ogg";
            road.append("  ").append(file).append(" ".repeat(Math.max(1, 36 - file.length()))).append(describe(stage)).append('\n');
        }
        return """
                The world's music (Enhanced)
                ============================

                Songs put in this folder replace the mod's music for every player on this world
                or server. Name each file after the music it replaces:

                  quicksilver.ogg                     Quicksilver's Walkman song (in Quicksilver Time)
                  speed.ogg                           Speed's Walkman song (Super Speed or his slow time)
                  witches_road.ogg                    the Witches' Road, all of it
                %s  music.ogg                           any of the above that has no file of its own

                One version of the Road (witches_road_<part>.ogg) is played in that part only;
                the parts without one play witches_road.ogg, then music.ogg, then the mod's own.

                Rules:
                  * Ogg Vorbis only (.ogg): every player can play it without anything extra.
                  * At most music.maxFileKB kilobytes each (8192 by default, in the server config
                    serverconfig/enhanced-server.toml, section [music]). A file that is bigger,
                    or is not Ogg Vorbis, is skipped and the server log says why.
                  * After adding, changing or removing a file: /reload, or /enhanced music reload.
                    Players download each song once (it is kept in their
                    config/enhanced/music_cache) and hear it from then on.
                  * A player's own song in his config/enhanced/music still wins over these, and
                    a player can turn them off (ignoreServerMusic in enhanced-client.toml).
                  * Players with an older version of the mod just hear the built-in music.
                """.formatted(road);
    }

    private static String describe(String stage) {
        return switch (stage) {
            case RoadMusic.ENTRANCE -> "the Road: at the door and on the way in";
            case RoadMusic.CHOICE -> "the Road: the choice between Billy and Tommy";
            case com.kolodkingal.enhanced.road.EndingHall.ID -> "the Road: the hall at the end";
            default -> "the Road: the trial \"" + stage + "\"";
        };
    }

    /** Creates the folder and its README (kept up to date: it is the mod's own file). */
    public static void prepare(Path folder) {
        try {
            Files.createDirectories(folder);
            Path readme = folder.resolve("README.txt");
            String text = readme();
            if (!Files.exists(readme) || !Files.readString(readme, StandardCharsets.UTF_8).equals(text)) {
                Files.writeString(readme, text, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            Enhanced.LOGGER.warn("Could not create the world's music folder {}", folder, e);
        }
    }

    /**
     * Reads the folder: every file named after a track, checked (Ogg Vorbis, not empty, at most {@code maxBytes}). Two files
     * for one name (say quicksilver.ogg and Quicksilver.OGG): the first in alphabetical order.
     */
    public static Scan scan(Path folder, int maxBytes) {
        Map<String, Song> songs = new LinkedHashMap<>();
        Map<String, String> rejected = new LinkedHashMap<>();
        if (!Files.isDirectory(folder)) {
            return new Scan(Map.of(), Map.of());
        }
        List<Path> files;
        try (Stream<Path> list = Files.list(folder)) {
            files = list.filter(Files::isRegularFile).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
        } catch (IOException e) {
            Enhanced.LOGGER.warn("Could not read the world's music folder {}", folder, e);
            return new Scan(Map.of(), Map.of());
        }
        for (Path path : files) {
            String file = path.getFileName().toString();
            String name = MusicTracks.baseName(file);
            String type = MusicTracks.extension(file);
            if (file.equalsIgnoreCase("README.txt")) {
                continue;
            }
            if (!MusicTracks.isTrackName(name)) {
                rejected.put(file, "not named after any music (see README.txt)");
                continue;
            }
            if (!type.equals("ogg")) {
                rejected.put(file, "only Ogg Vorbis (.ogg) files are sent to the players");
                continue;
            }
            if (songs.containsKey(name)) {
                rejected.put(file, "another file is already " + name);
                continue;
            }
            try {
                long size = Files.size(path);
                if (size == 0) {
                    rejected.put(file, "empty");
                    continue;
                }
                if (size > maxBytes) {
                    rejected.put(file, "too big: " + (size + 1023) / 1024 + " KB, the limit is " + maxBytes / 1024 + " KB (music.maxFileKB)");
                    continue;
                }
                byte[] data = Files.readAllBytes(path);
                if (!MusicFiles.isOggVorbis(data)) {
                    rejected.put(file, "not an Ogg Vorbis file");
                    continue;
                }
                songs.put(name, new Song(name, file, MusicFiles.sha256(data), data));
            } catch (IOException e) {
                rejected.put(file, "could not be read: " + e.getMessage());
            }
        }
        return new Scan(Map.copyOf(songs), Map.copyOf(rejected));
    }

    /** Looks at the folder again (creating it if need be) and logs what it found. */
    public static Scan reload(MinecraftServer server) {
        Path folder = folder(server);
        prepare(folder);
        Scan scan = MusicConfig.enabled() ? scan(folder, MusicConfig.maxBytes()) : Scan.EMPTY;
        Scan before = current;
        current = scan;
        for (Map.Entry<String, String> bad : scan.rejected().entrySet()) {
            Enhanced.LOGGER.warn("The world's music: {} is left out: {}", bad.getKey(), bad.getValue());
        }
        if (!scan.songs().isEmpty() || !before.songs().isEmpty()) {
            Enhanced.LOGGER.info("The world's music: {} song(s) in {}: {}", scan.songs().size(), folder,
                    scan.songs().values().stream().map(s -> s.name() + " (" + s.size() + " bytes, " + s.hash().substring(0, 12) + ")").toList());
        }
        // Songs that are gone are not sent any more.
        synchronized (TRANSFERS) {
            for (Deque<Transfer> queue : TRANSFERS.values()) {
                queue.removeIf(t -> scan.songs().values().stream().noneMatch(s -> s.hash().equals(t.song.hash())));
            }
        }
        return scan;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The players

    /** Whether the player's client has the music channel (a client of an older version of the mod has not). */
    public static boolean canHear(ServerPlayer player) {
        return ModNetwork.hasMusic(player);
    }

    /** Tells the player what songs the world has (nothing for a client without the channel). */
    public static void sendManifest(ServerPlayer player) {
        if (canHear(player)) {
            Net.sendToPlayer(player, new MusicManifestPayload(current.manifest()));
        }
    }

    /** A player asks for a song: queued if the world has it (once), otherwise ignored. */
    public static void request(ServerPlayer player, String hash) {
        if (!MusicFiles.isHash(hash)) {
            return;
        }
        Song song = current.songs().values().stream().filter(s -> s.hash().equals(hash)).findFirst().orElse(null);
        if (song == null) {
            return;
        }
        synchronized (TRANSFERS) {
            Deque<Transfer> queue = TRANSFERS.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
            if (queue.stream().noneMatch(t -> t.song.hash().equals(hash)) && queue.size() < MusicManifestPayload.MAX_TRACKS) {
                queue.add(new Transfer(song));
            }
        }
    }

    /** Every tick: each player downloading a song is sent the next few pieces of it. */
    public static void tick(MinecraftServer server) {
        synchronized (TRANSFERS) {
            if (TRANSFERS.isEmpty()) {
                return;
            }
            int perTick = MusicConfig.chunksPerTick();
            TRANSFERS.entrySet().removeIf(entry -> {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                Deque<Transfer> queue = entry.getValue();
                if (player == null || !canHear(player)) {
                    return true;
                }
                for (int sent = 0; sent < perTick && !queue.isEmpty(); sent++) {
                    Transfer t = queue.peek();
                    byte[] data = t.song.data();
                    int total = MusicFiles.chunkCount(data.length);
                    Net.sendToPlayer(player, new MusicChunkPayload(t.song.hash(), t.next, total, data.length, MusicFiles.chunk(data, t.next)));
                    if (++t.next >= total) {
                        queue.poll();
                        Enhanced.LOGGER.debug("The world's music: sent {} to {}", t.song.name(), player.getGameProfile().getName());
                    }
                }
                return queue.isEmpty();
            });
        }
    }

    /** How many songs the player still waits for (for tests and the list command). */
    public static int pending(UUID player) {
        synchronized (TRANSFERS) {
            Deque<Transfer> queue = TRANSFERS.get(player);
            return queue == null ? 0 : queue.size();
        }
    }

    /** For tests: the server sends {@code scan} until the returned action puts back what it sent before. */
    public static Runnable swap(Scan scan) {
        Scan before = current;
        current = scan;
        return () -> current = before;
    }

    public static void forget(UUID player) {
        synchronized (TRANSFERS) {
            TRANSFERS.remove(player);
        }
    }

    public static void clear() {
        synchronized (TRANSFERS) {
            TRANSFERS.clear();
        }
        current = Scan.EMPTY;
    }

    private ServerMusic() {
    }
}
