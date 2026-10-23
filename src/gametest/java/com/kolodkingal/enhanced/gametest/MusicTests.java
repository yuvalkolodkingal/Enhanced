package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.music.MusicFiles;
import com.kolodkingal.enhanced.music.MusicTracks;
import com.kolodkingal.enhanced.music.ServerMusic;
import com.kolodkingal.enhanced.music.SpeedsterTracks;
import com.kolodkingal.enhanced.network.MusicChunkPayload;
import com.kolodkingal.enhanced.network.MusicManifestPayload;
import com.kolodkingal.enhanced.network.MusicRequestPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.EndingHall;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadMusic;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * 3.5, the music lane: the world's music folder (its README, the names it takes and their fallbacks, the size limit and the
 * files it leaves out, the hashes), sending a song in pieces and putting it back together, the messages, the player's own
 * folder's names, and when a speedster's Walkman plays (which the server sees as the clients do).
 */
public final class MusicTests {
    private static final String BATCH = "music";

    /** A fresh folder of the test's own inside the world's folder (the real music folder is left alone). */
    private static Path scratch(MinecraftServer server, String name) {
        Path folder = server.getWorldPath(LevelResource.ROOT).resolve("music_test").resolve(name + "_" + System.nanoTime());
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return folder;
    }

    private static void delete(Path folder) {
        try (Stream<Path> files = Files.walk(folder)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // (only a test's leftovers)
        }
    }

    /** One of the mod's own Ogg Vorbis files. */
    private static byte[] ogg(String path) {
        try (InputStream in = MusicTests.class.getResourceAsStream("/assets/enhanced/sounds/" + path + ".ogg")) {
            check(in != null, path + " is in the mod");
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, byte[] data) {
        try {
            Files.write(file, data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** The server made its world's music folder when it started, with a README that names every file it takes. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theWorldHasAMusicFolderWithAReadme(GameTestHelper helper) throws IOException {
        MinecraftServer server = helper.getLevel().getServer();
        Path folder = ServerMusic.folder(server);
        check(folder.endsWith(Path.of("serverconfig", "enhanced", "music")), "in the world's serverconfig: " + folder);
        check(folder.startsWith(server.getWorldPath(LevelResource.ROOT).normalize()), "inside the world's folder");
        check(Files.isDirectory(folder), "made when the server started");
        Path readme = folder.resolve("README.txt");
        check(Files.isRegularFile(readme), "with its README");
        String text = Files.readString(readme, StandardCharsets.UTF_8);
        check(text.equals(ServerMusic.readme()), "the README is the current one");
        for (String name : List.of("quicksilver.ogg", "speed.ogg", "witches_road.ogg", "music.ogg", "witches_road_entrance.ogg",
                "witches_road_choice.ogg", "witches_road_ending.ogg", "maxFileKB", "/enhanced music reload", "Ogg Vorbis")) {
            check(text.contains(name), "the README names " + name);
        }
        for (RoadChallenge trial : RoadChallenges.all()) {
            check(text.contains("witches_road_" + trial.id() + ".ogg"), "the README names the version of the trial " + trial.id());
        }
        // Made again (and the README put back) where there is none.
        Path other = scratch(server, "readme").resolve("music");
        ServerMusic.prepare(other);
        check(Files.readString(other.resolve("README.txt"), StandardCharsets.UTF_8).equals(text), "a new folder gets the README");
        Files.writeString(other.resolve("README.txt"), "old words", StandardCharsets.UTF_8);
        ServerMusic.prepare(other);
        check(Files.readString(other.resolve("README.txt"), StandardCharsets.UTF_8).equals(text), "an old README is brought up to date");
        delete(other.getParent());
        helper.succeed();
    }

    /** Every piece of music has a name, and falls back to the Road's song for a version of it, then to music.ogg. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void namesFallBackToTheRoadThenToMusic(GameTestHelper helper) {
        check(MusicTracks.chain(MusicTracks.QUICKSILVER).equals(List.of("quicksilver", "music")), "Quicksilver: his, then music");
        check(MusicTracks.chain(MusicTracks.SPEED).equals(List.of("speed", "music")), "Speed: his, then music");
        check(MusicTracks.chain("witches_road_parkour").equals(List.of("witches_road_parkour", "witches_road", "music")), "a version of the Road");
        check(MusicTracks.chain(MusicTracks.ROAD).equals(List.of("witches_road", "music")), "the Road");

        check(MusicTracks.road(RoadMusic.NONE) == null, "no Road music off the Road");
        check("witches_road_entrance".equals(MusicTracks.road(RoadMusic.ENTRANCE)), "the entrance");
        check("witches_road_choice".equals(MusicTracks.road(RoadMusic.CHOICE)), "the choice");
        check("witches_road_ending".equals(MusicTracks.road(EndingHall.ID)), "the hall at the end is 'ending'");
        Set<String> tracks = new HashSet<>();
        Set<SoundEvent> sounds = new HashSet<>();
        List<String> stages = new ArrayList<>(List.of(RoadMusic.ENTRANCE, RoadMusic.CHOICE, EndingHall.ID));
        RoadChallenges.all().forEach(t -> stages.add(t.id()));
        for (String stage : stages) {
            String track = MusicTracks.road(stage);
            check(MusicTracks.isTrackName(track) && tracks.add(track), "a name of its own for " + stage + ": " + track);
            check(sounds.add(RoadMusic.soundOf(stage)), "and a version of the Road's song of its own: " + stage);
        }
        check(RoadMusic.soundOf(RoadMusic.CHOICE) == ModSounds.ROAD_MUSIC_CHOICE.get(), "the choice's version");

        Set<String> all = Set.of("music", "witches_road", "speed");
        check(MusicTracks.resolve("quicksilver", all).equals(Optional.of("music")), "music.ogg stands in for Quicksilver's");
        check(MusicTracks.resolve("speed", all).equals(Optional.of("speed")), "Speed's own wins over music.ogg");
        check(MusicTracks.resolve("witches_road_ice_boat", all).equals(Optional.of("witches_road")), "the Road's song for the race");
        check(MusicTracks.resolve("witches_road_ice_boat", Set.of("witches_road_ice_boat", "witches_road")).equals(Optional.of("witches_road_ice_boat")),
                "the race's own version wins");
        check(MusicTracks.resolve("speed", Set.of("quicksilver")).isEmpty(), "Quicksilver's song is not Speed's");
        check(MusicTracks.resolve("quicksilver", Set.of()).isEmpty(), "nothing: the built-in song");

        check(MusicTracks.isTrackName("music") && MusicTracks.isTrackName("witches_road_some_later_trial"), "names it takes");
        check(!MusicTracks.isTrackName("witches_road_") && !MusicTracks.isTrackName("my_song") && !MusicTracks.isTrackName("Speed"),
                "names it does not");
        helper.succeed();
    }

    /** A player's own folder: a file per piece of music; any other song is Quicksilver's, as before 3.5. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aPlayersOwnSongsByName(GameTestHelper helper) {
        List<String> legacy = List.of("README.txt", "my song.mp3");
        check(MusicTracks.resolvePersonal("quicksilver", legacy).equals(Optional.of("my song.mp3")), "an old single song is Quicksilver's");
        check(MusicTracks.resolvePersonal("speed", legacy).isEmpty(), "and not Speed's");
        check(MusicTracks.resolvePersonal("witches_road_parkour", legacy).isEmpty(), "nor the Road's");
        check(MusicTracks.resolvePersonal("speed", List.of("a.mp3", "Speed.WAV")).equals(Optional.of("Speed.WAV")), "Speed's own (any case)");
        check(MusicTracks.resolvePersonal("quicksilver", List.of("speed.ogg", "witches_road.mp3")).isEmpty(), "a song named for another is not his");
        check(MusicTracks.resolvePersonal("quicksilver", List.of("b.mp3", "quicksilver.ogg", "a.mp3")).equals(Optional.of("quicksilver.ogg")),
                "his own name first");
        check(MusicTracks.resolvePersonal("quicksilver", List.of("b.mp3", "a.wav")).equals(Optional.of("a.wav")), "the first in order");
        check(MusicTracks.resolvePersonal("quicksilver", List.of("music.mp3")).equals(Optional.of("music.mp3")), "music.mp3 is his, as before");
        check(MusicTracks.resolvePersonal("witches_road_ice_boat", List.of("music.mp3", "witches_road.ogg")).equals(Optional.of("witches_road.ogg")),
                "the Road's song for a part of it");
        check(MusicTracks.resolvePersonal("witches_road_ice_boat", List.of("witches_road.ogg", "witches_road_ice_boat.mp3"))
                .equals(Optional.of("witches_road_ice_boat.mp3")), "that part's own wins");
        check(MusicTracks.resolvePersonal("quicksilver", List.of("notes.txt", "quicksilver.flac")).isEmpty(), "only .mp3, .ogg and .wav");
        helper.succeed();
    }

    /** What is an Ogg Vorbis file and what is not. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onlyOggVorbisPasses(GameTestHelper helper) {
        byte[] real = ogg("sfx/road_travel");
        check(MusicFiles.isOggVorbis(real), "the mod's own Ogg Vorbis");
        check(MusicFiles.isOggVorbis(ogg("music/quicksilver_theme")), "Quicksilver's new theme");
        check(!MusicFiles.isOggVorbis(new byte[0]), "nothing");
        check(!MusicFiles.isOggVorbis("ID3 an mp3 pretending".getBytes(StandardCharsets.US_ASCII)), "an mp3");
        check(!MusicFiles.isOggVorbis(Arrays.copyOf(real, 30)), "a cut-off header");
        byte[] opus = real.clone();
        int packet = 27 + (opus[26] & 0xFF);
        System.arraycopy("OpusHead".getBytes(StandardCharsets.US_ASCII), 0, opus, packet, 8);
        check(!MusicFiles.isOggVorbis(opus), "Ogg, but Opus");
        byte[] notFirst = real.clone();
        notFirst[5] = 0;
        check(!MusicFiles.isOggVorbis(notFirst), "not the start of a stream");
        helper.succeed();
    }

    /** The folder: what is sent (with its hash and size), and what is left out and why: the size cap, bad files, names. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theFolderIsReadCheckedAndHashed(GameTestHelper helper) {
        Path folder = scratch(helper.getLevel().getServer(), "scan");
        byte[] small = ogg("sfx/road_travel");     // about 12 KB
        byte[] big = ogg("sfx/witches_road");      // about 29 KB
        write(folder.resolve("music.ogg"), small);
        write(folder.resolve("speed.ogg"), small);
        write(folder.resolve("quicksilver.ogg"), "not a song at all".getBytes(StandardCharsets.UTF_8));
        write(folder.resolve("witches_road_parkour.ogg"), big);
        write(folder.resolve("witches_road.mp3"), small);
        write(folder.resolve("my song.ogg"), small);
        write(folder.resolve("witches_road_ice_boat.ogg"), new byte[0]);
        ServerMusic.prepare(folder);
        ServerMusic.Scan scan = ServerMusic.scan(folder, 16 * 1024);
        check(scan.songs().keySet().equals(Set.of("music", "speed")), "only the good ones: " + scan.songs().keySet());
        check(scan.rejected().get("quicksilver.ogg").contains("not an Ogg Vorbis"), "a bad file: " + scan.rejected());
        check(scan.rejected().get("witches_road_parkour.ogg").contains("too big"), "over the size cap");
        check(scan.rejected().get("witches_road.mp3").contains("only Ogg Vorbis"), "an mp3 is not sent");
        check(scan.rejected().get("my song.ogg").contains("not named"), "a name that is no music");
        check(scan.rejected().get("witches_road_ice_boat.ogg").equals("empty"), "an empty file");
        check(!scan.rejected().containsKey("README.txt"), "the README is no song");
        ServerMusic.Song music = scan.songs().get("music");
        check(music.size() == small.length && music.hash().equals(MusicFiles.sha256(small)) && MusicFiles.isHash(music.hash()), "its size and SHA-256");
        check(scan.resolve("quicksilver") == music, "music.ogg plays for Quicksilver (quicksilver.ogg was bad)");
        check(scan.resolve("speed") == scan.songs().get("speed"), "speed.ogg for Speed");
        check(scan.resolve("witches_road_parkour") == music, "music.ogg for the parkour (its own was too big)");
        check(scan.manifest().size() == 2 && scan.manifest().stream().anyMatch(t -> t.name().equals("music") && t.hash().equals(music.hash())
                && t.size() == small.length), "the manifest lists them");
        // With a bigger cap, the parkour's own version is sent too; an empty folder sends nothing.
        check(ServerMusic.scan(folder, 64 * 1024).songs().containsKey("witches_road_parkour"), "under a bigger cap it passes");
        check(ServerMusic.scan(folder.resolve("missing"), 1024).songs().isEmpty(), "no folder: nothing");
        check(MusicFiles.sha256("abc".getBytes(StandardCharsets.US_ASCII)).equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
                "SHA-256 as everyone computes it");
        delete(folder);
        helper.succeed();
    }

    /** A song goes in pieces of at most 30 KB and comes back whole, in any order; a wrong piece or a wrong hash is refused. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aSongGoesInPiecesAndComesBackWhole(GameTestHelper helper) {
        byte[] song = new byte[100_000];
        new Random(35).nextBytes(song);
        String hash = MusicFiles.sha256(song);
        int total = MusicFiles.chunkCount(song.length);
        check(total == 4 && MusicFiles.chunkCount(60_000) == 2 && MusicFiles.chunkCount(1) == 1, "pieces of 30 000 bytes");
        check(MusicFiles.CHUNK_BYTES <= 30 * 1024, "at most 30 KB a piece");
        List<byte[]> chunks = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            chunks.add(MusicFiles.chunk(song, i));
        }
        check(chunks.get(3).length == 10_000, "the last piece is the rest");
        MusicFiles.Assembly assembly = new MusicFiles.Assembly(hash, song.length);
        for (int i = total - 1; i >= 0; i--) {
            check(!assembly.complete(), "not whole before the last piece");
            check(assembly.accept(i, total, chunks.get(i)), "piece " + i);
        }
        check(assembly.accept(1, total, chunks.get(1)) && assembly.received() == total, "a piece sent twice changes nothing");
        check(Arrays.equals(assembly.result(), song), "whole again, the hash checked");

        MusicFiles.Assembly wrong = new MusicFiles.Assembly(hash, song.length);
        check(!wrong.accept(4, total, chunks.get(0)), "no piece past the end");
        check(!wrong.accept(0, total + 1, chunks.get(0)), "not another count");
        check(!wrong.accept(3, total, chunks.get(0)), "not a piece of the wrong length");
        byte[] broken = chunks.get(2).clone();
        broken[7] ^= 1;
        for (int i = 0; i < total; i++) {
            wrong.accept(i, total, i == 2 ? broken : chunks.get(i));
        }
        check(wrong.complete() && wrong.result() == null, "a song whose hash does not match is thrown away");
        boolean refused = false;
        try {
            new MusicFiles.Assembly("../../evil", 10);
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        check(refused, "only a real hash names a song (it names the cached file)");
        helper.succeed();
    }

    /** The three messages go to the other side and back. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theMusicMessagesRoundTrip(GameTestHelper helper) {
        byte[] piece = new byte[MusicFiles.CHUNK_BYTES];
        new Random(5).nextBytes(piece);
        String hash = MusicFiles.sha256(piece);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        MusicChunkPayload.STREAM_CODEC.encode(buf, new MusicChunkPayload(hash, 3, 9, 250_000, piece));
        check(buf.readableBytes() < 31 * 1024, "a piece is under 31 KB on the wire: " + buf.readableBytes());
        MusicChunkPayload chunk = MusicChunkPayload.STREAM_CODEC.decode(buf);
        check(chunk.hash().equals(hash) && chunk.index() == 3 && chunk.total() == 9 && chunk.size() == 250_000 && Arrays.equals(chunk.data(), piece), "a piece");

        List<MusicManifestPayload.Track> tracks = List.of(new MusicManifestPayload.Track("music", hash, 12345),
                new MusicManifestPayload.Track("witches_road_ice_boat", hash, 99));
        buf = new FriendlyByteBuf(Unpooled.buffer());
        MusicManifestPayload.STREAM_CODEC.encode(buf, new MusicManifestPayload(tracks));
        check(MusicManifestPayload.STREAM_CODEC.decode(buf).tracks().equals(tracks), "the manifest");

        buf = new FriendlyByteBuf(Unpooled.buffer());
        MusicRequestPayload.STREAM_CODEC.encode(buf, new MusicRequestPayload(hash));
        check(MusicRequestPayload.STREAM_CODEC.decode(buf).hash().equals(hash), "a request");
        helper.succeed();
    }

    /**
     * A request is queued once, only for a song the server has, and sent a few pieces a tick (music.chunksPerTick); the pieces
     * put back together are the song. (A client without the mod's music channel, an older one, is sent nothing: {@link
     * ServerMusic#canHear}; the test's fake connection has every channel.)
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void requestsAreQueuedOnceAndSentInPieces(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(3.5, TestKit.FLOOR, 3.5);
        byte[] data = new byte[70_000];
        new Random(70).nextBytes(data);
        String hash = MusicFiles.sha256(data);
        ServerMusic.Song song = new ServerMusic.Song("music", "music.ogg", hash, data);
        Runnable restore = ServerMusic.swap(new ServerMusic.Scan(java.util.Map.of("music", song), java.util.Map.of()));
        try {
            TestKit.sentPackets(player);
            ServerMusic.sendManifest(player);
            List<Object> told = TestKit.sentPackets(player);
            if (ServerMusic.canHear(player)) {
                check(told.stream().anyMatch(p -> p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c
                        && c.payload() instanceof MusicManifestPayload m && m.tracks().equals(List.of(new MusicManifestPayload.Track("music", hash, data.length)))),
                        "he is told the world's songs: " + told);
            } else {
                check(told.isEmpty(), "a client without the channel is told nothing");
            }
            ServerMusic.request(player, MusicFiles.sha256(new byte[] {1}));
            ServerMusic.request(player, "not a hash");
            check(ServerMusic.pending(player.getUUID()) == 0, "a song the server does not have is not sent");
            ServerMusic.request(player, hash);
            ServerMusic.request(player, hash);
            check(ServerMusic.pending(player.getUUID()) == 1, "the world's song is queued, once");
            MinecraftServer server = helper.getLevel().getServer();
            if (!ServerMusic.canHear(player)) {
                ServerMusic.tick(server);
                check(ServerMusic.pending(player.getUUID()) == 0, "nothing is sent where it cannot be heard");
            } else {
                MusicFiles.Assembly back = new MusicFiles.Assembly(hash, data.length);
                int ticks = 0;
                while (ServerMusic.pending(player.getUUID()) > 0 && ticks < 10) {
                    ServerMusic.tick(server);
                    ticks++;
                    int pieces = 0;
                    for (Object p : TestKit.sentPackets(player)) {
                        if (p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c && c.payload() instanceof MusicChunkPayload chunk) {
                            check(chunk.hash().equals(hash) && chunk.size() == data.length && chunk.data().length <= MusicFiles.CHUNK_BYTES, "a piece of the song");
                            check(back.accept(chunk.index(), chunk.total(), chunk.data()), "a piece that fits");
                            pieces++;
                        }
                    }
                    check(pieces <= 2, "at most chunksPerTick (2) pieces a tick: " + pieces);
                }
                check(ticks == 2, "3 pieces in 2 ticks: " + ticks);
                check(Arrays.equals(back.result(), data), "the pieces make the song again");
            }
            ServerMusic.request(player, hash);
            ServerMusic.forget(player.getUUID());
            check(ServerMusic.pending(player.getUUID()) == 0, "forgotten when he leaves");
        } finally {
            restore.run();
        }
        helper.succeed();
    }

    /** Speed's Walkman: while he runs with his Super Speed, or in his slow time; never with the Walkman off. Quicksilver's as before. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void whenTheSpeedstersWalkmanPlays(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer tommy = f.player(3.5, TestKit.FLOOR, 3.5, Power.TOMMY);
        PowerData data = Powers.get(tommy);
        check(data.walkman(), "his Walkman starts on");
        check(SpeedsterTracks.trackOf(data) == null, "standing: nothing");
        check(data.toggle(Toggle.SUPER_SPEED), "his Super Speed (G) starts on");
        TommyAbilities.onKey(tommy, data, 2, true, false);
        tommy.setSprinting(true);
        PowerTicker.tick(tommy);
        check(!data.toggle(Toggle.SUPER_SPEED) && SpeedsterTracks.trackOf(data) == null, "G off: sprinting is only sprinting, no song");
        TommyAbilities.onKey(tommy, data, 2, true, false);
        check(data.toggle(Toggle.SUPER_SPEED), "G: Super Speed on again");
        tommy.setSprinting(false);
        PowerTicker.tick(tommy);
        check(SpeedsterTracks.trackOf(data) == null, "on, but walking: nothing yet");
        tommy.setSprinting(true);
        PowerTicker.tick(tommy);
        check(data.is(Live.SPEEDING) && MusicTracks.SPEED.equals(SpeedsterTracks.trackOf(data)), "running: his song");
        tommy.setSprinting(false);
        PowerTicker.tick(tommy);
        check(SpeedsterTracks.trackOf(data) == null, "he stops: it stops (the client lets it linger two seconds)");
        check(MusicTracks.SPEED.equals(SpeedsterTracks.trackOf(data, true)), "counted as still running, it goes on");
        // His slow time (R): set and looked at at once, so the slow motion never reaches the next arena.
        data.setToggle(Toggle.TOMMY_TIME, true);
        data.setSlowMo(true);
        check(MusicTracks.SPEED.equals(SpeedsterTracks.trackOf(data)), "in his slow time: his song");
        data.setWalkman(false);
        check(SpeedsterTracks.trackOf(data) == null && SpeedsterTracks.trackOf(data, true) == null, "Walkman off: nothing");
        data.setWalkman(true);
        data.setToggle(Toggle.TOMMY_TIME, false);
        data.setSlowMo(false);
        data.setToggle(Toggle.SUPER_SPEED, false);

        ServerPlayer pietro = f.player(6.5, TestKit.FLOOR, 3.5, Power.QUICKSILVER);
        PowerData qs = Powers.get(pietro);
        check(SpeedsterTracks.trackOf(qs, true) == null, "Quicksilver's Super Speed alone plays nothing (as before)");
        qs.setQuicksilverTime(true);
        qs.setSlowMo(true);
        check(MusicTracks.QUICKSILVER.equals(SpeedsterTracks.trackOf(qs)), "in Quicksilver Time: his song");
        qs.setWalkman(false);
        check(SpeedsterTracks.trackOf(qs) == null, "his Walkman off: nothing");
        qs.setWalkman(true);
        qs.setQuicksilverTime(false);
        qs.setSlowMo(false);
        check(SpeedsterTracks.trackOf(Powers.get(f.player(9.5, TestKit.FLOOR, 3.5, Power.MAGNETO)), true) == null, "no one else has a Walkman song");
        helper.succeed();
    }
}
