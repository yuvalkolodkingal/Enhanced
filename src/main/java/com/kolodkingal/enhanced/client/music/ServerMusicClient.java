package com.kolodkingal.enhanced.client.music;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.music.MusicFiles;
import com.kolodkingal.enhanced.music.MusicTracks;
import com.kolodkingal.enhanced.network.MusicManifestPayload;
import com.kolodkingal.enhanced.network.MusicRequestPayload;
import net.minecraft.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The client's side of the world's music (3.5): the server says which songs it has (names, SHA-256, sizes); every song this
 * client has not kept yet is asked for and comes in pieces; once whole and its hash checked, it is kept in
 * config/enhanced/music_cache/{@code <hash>}.ogg, so each song is downloaded once, whatever server it came from. The Walkman
 * and the Road then play it ({@link MusicSources}) unless the player's own song wins, or he chose to ignore the world's music.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ServerMusicClient {
    /** The server's songs by name (empty: none, or a server without the music channel). */
    private static volatile Map<String, MusicManifestPayload.Track> manifest = Map.of();
    /** Songs coming in, by hash. */
    private static final Map<String, MusicFiles.Assembly> INCOMING = new HashMap<>();
    /** Songs asked for this session (each only once). */
    private static final Set<String> REQUESTED = ConcurrentHashMap.newKeySet();
    /** Kept songs whose file was checked this session (size and hash). */
    private static final Set<String> VERIFIED = ConcurrentHashMap.newKeySet();
    /** Goes up whenever what the world's music plays may have changed (a new manifest, a song kept). */
    private static final AtomicInteger GENERATION = new AtomicInteger();

    public static Path cacheFolder() {
        return FMLPaths.CONFIGDIR.get().resolve(Enhanced.MODID).resolve("music_cache");
    }

    public static Path cached(String hash) {
        return cacheFolder().resolve(hash + ".ogg");
    }

    public static int generation() {
        return GENERATION.get();
    }

    /** The server's list of songs: asks for every one not kept yet. */
    public static void onManifest(List<MusicManifestPayload.Track> tracks) {
        Map<String, MusicManifestPayload.Track> next = new LinkedHashMap<>();
        for (MusicManifestPayload.Track track : tracks) {
            if (MusicTracks.isTrackName(track.name()) && MusicFiles.isHash(track.hash()) && track.size() > 0 && track.size() <= MusicFiles.CLIENT_MAX_BYTES) {
                next.put(track.name(), track);
            }
        }
        manifest = Map.copyOf(next);
        GENERATION.incrementAndGet();
        if (next.isEmpty()) {
            Enhanced.LOGGER.info("The world's music: none (the built-in music plays)");
            return;
        }
        if (EnhancedClientConfig.ignoreServerMusic()) {
            Enhanced.LOGGER.info("The world's music: {} song(s), ignored (ignoreServerMusic)", next.size());
            return;
        }
        int asked = 0;
        for (MusicManifestPayload.Track track : next.values()) {
            if (isKept(track) || !REQUESTED.add(track.hash())) {
                continue;
            }
            synchronized (INCOMING) {
                INCOMING.put(track.hash(), new MusicFiles.Assembly(track.hash(), track.size()));
            }
            PacketDistributor.sendToServer(new MusicRequestPayload(track.hash()));
            asked++;
        }
        Enhanced.LOGGER.info("The world's music: {} song(s) {}, {} to download", next.size(), next.keySet(), asked);
    }

    /** A piece of a song this client asked for. */
    public static void onChunk(String hash, int index, int total, int size, byte[] data) {
        MusicFiles.Assembly song;
        synchronized (INCOMING) {
            song = INCOMING.get(hash);
            if (song == null || song.size() != size || !song.accept(index, total, data) || !song.complete()) {
                return;
            }
            INCOMING.remove(hash);
        }
        Util.ioPool().execute(() -> keep(song));
    }

    private static void keep(MusicFiles.Assembly song) {
        byte[] data = song.result();
        if (data == null || !MusicFiles.isOggVorbis(data)) {
            Enhanced.LOGGER.warn("The world's music: the song {} came broken (or is not Ogg Vorbis); it is not played", song.hash());
            return;
        }
        try {
            Files.createDirectories(cacheFolder());
            Path tmp = cacheFolder().resolve(song.hash() + ".part");
            Files.write(tmp, data);
            Files.move(tmp, cached(song.hash()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            VERIFIED.add(song.hash());
            GENERATION.incrementAndGet();
            Enhanced.LOGGER.info("The world's music: downloaded and cached {} ({} bytes) as {}", namesOf(song.hash()), data.length, cached(song.hash()));
        } catch (IOException e) {
            Enhanced.LOGGER.warn("The world's music: could not keep {}", song.hash(), e);
        }
    }

    private static List<String> namesOf(String hash) {
        return manifest.values().stream().filter(t -> t.hash().equals(hash)).map(MusicManifestPayload.Track::name).toList();
    }

    /** Whether the song is kept already, its file whole (checked once a session). */
    private static boolean isKept(MusicManifestPayload.Track track) {
        if (VERIFIED.contains(track.hash())) {
            return true;
        }
        Path file = cached(track.hash());
        try {
            if (Files.isRegularFile(file) && Files.size(file) == track.size()
                    && MusicFiles.sha256(Files.readAllBytes(file)).equals(track.hash())) {
                VERIFIED.add(track.hash());
                return true;
            }
        } catch (IOException e) {
            Enhanced.LOGGER.debug("Could not check {}", file, e);
        }
        return false;
    }

    /**
     * The kept file of the world's song for a track (the most particular name the world has a song for), if the world has
     * one, it is here already and the player does not ignore the world's music. Empty: play something else.
     */
    public static Optional<ServerSong> songFor(String track) {
        if (EnhancedClientConfig.ignoreServerMusic()) {
            return Optional.empty();
        }
        Map<String, MusicManifestPayload.Track> songs = manifest;
        return MusicTracks.resolve(track, songs.keySet()).map(songs::get)
                .filter(t -> VERIFIED.contains(t.hash()) || isKept(t))
                .map(t -> new ServerSong(t.name(), t.hash(), cached(t.hash())));
    }

    /** One of the world's songs, kept: the name of its file on the server, its hash and where it is kept. */
    public record ServerSong(String name, String hash, Path file) {
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        manifest = Map.of();
        synchronized (INCOMING) {
            INCOMING.clear();
        }
        REQUESTED.clear();
        GENERATION.incrementAndGet();
    }

    private ServerMusicClient() {
    }
}
