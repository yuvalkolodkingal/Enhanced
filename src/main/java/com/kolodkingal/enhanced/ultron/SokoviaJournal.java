package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.ArenaLoans;
import com.kolodkingal.enhanced.ultron.SokoviaShape.Spec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The note the lift leaves on disk while an island hangs in the sky, so that a crash or a kill leaves, at worst, a
 * floating island that the next start clears: a few hundred bytes in {@code <world>/data/enhanced/sokovia/<fight>.nbt},
 * gzip-compressed NBT. The file is written <em>before</em> the first block of the island is placed (the world is never
 * changed without it), whole and atomically (a temporary file, flushed to disk, then moved over the real name), and
 * let go of only after the island is gone (and the world is saved without it, below). Everything else is a function of
 * it: the island's shape comes from the seed, so recovery clears "every block inside this shape, in this range of
 * heights" and needs no copy of anything.
 *
 * <p>The ground is never edited by the lift, so there is no snapshot to restore, and a lost file's worst case is a
 * floating island of ordinary blocks, which stays like any other blocks ({@code /enhanced boss cleanup} removes only the
 * fight's creatures, never blocks).
 *
 * <p><b>A note forgets only what a save holds</b> (R48). The world reaches the disk only at a save, so an island cleared in
 * memory is still in the sky of the saved world until its dimension has been saved. A note whose island is gone (the lift
 * landed, a stop put everything back at once, a recovery cleared it) is therefore not deleted but <em>retired</em> ({@link
 * #delete}): it leaves the live notes at once (the file is moved aside as {@code <fight>.nbt.retiring}, so nothing takes it
 * for an island still up) and is deleted at the second save of its dimension after that, or when the server has stopped
 * (after the flushed save of the stop); a crash before then finds it and clears the island's column again, which is
 * harmless. So that it never gives a Program back twice: at the landing the note is first rewritten as {@link #LANDED},
 * owing nothing; a retired note that still says the Program is owed (a recovery has just paid it back, a stop gave it
 * back) says it no longer is from the first save on, when what was given back is on the disk too. The ritual's note,
 * which clears nothing, is still deleted at once.
 */
public final class SokoviaJournal {
    /** The island is, or may be, in the sky: the placing has begun. */
    public static final String PASTED = "PASTED";
    /** The island is being taken down. */
    public static final String CLEARING = "CLEARING";
    /**
     * The fight was woken with a Program and no island has risen yet: the note only says who is owed it back, so that a
     * crash on the ground does not cost the Program (it is written at the ritual and deleted when the fight is let go;
     * the lift's own note replaces it once the island is placed). It clears nothing: its heights are an empty range.
     */
    public static final String RITUAL = "RITUAL";
    /**
     * The island came down by the lift's own descent and its column in the sky is empty; nothing is owed. A note kept
     * this way until the world has saved it (R48): a recovery of it clears a column that is empty already.
     */
    public static final String LANDED = "LANDED";

    /** The folder of the notes, inside the world. */
    public static final String DIRECTORY = "data/enhanced/sokovia";
    /** The version of the file's format. */
    public static final int VERSION = 1;
    /** What is added to the name of a note that is retiring: kept until two saves hold the world without its island. */
    public static final String RETIRING = ".retiring";

    /** A note retiring, and the number of saves its dimension had when it began to. */
    private static final class Retiring {
        Header note;
        final ResourceKey<Level> dimension;
        final long save;

        Retiring(Header note, long save) {
            this.note = note;
            this.dimension = note.dimension();
            this.save = save;
        }
    }

    /** The notes retiring on this server, by fight. */
    private static final Map<UUID, Retiring> RETIRED = new HashMap<>();

    /**
     * What a note says.
     *
     * @param state   {@link #PASTED}, {@link #CLEARING}, {@link #RITUAL} or {@link #LANDED}
     * @param minY    the lowest block of the island in the sky
     * @param maxY    the highest
     * @param summoner whoever started the fight (the Program is given back to them after a crash), or null
     * @param refund  whether the fight used a Program that is owed back
     * @param members the participants who were carried (those who are offline when it ends are set down at their next
     * login)
     */
    public record Header(UUID fight, ResourceKey<Level> dimension, BlockPos centre, Spec spec, int minY, int maxY, String state,
                         @Nullable UUID summoner, boolean refund, List<UUID> members) {
        public Header {
            members = List.copyOf(members);
        }

        /** The note of a fight that has not lifted anything yet: it only says who is owed the Program back. */
        public static Header ritual(UUID fight, ResourceKey<Level> dimension, BlockPos centre, Spec spec, @Nullable UUID summoner) {
            return new Header(fight, dimension, centre, spec, 0, -1, RITUAL, summoner, true, List.of());
        }

        /** Whether this is the ritual's note, which has no island to clear. */
        public boolean isRitual() {
            return RITUAL.equals(state);
        }

        public Header withState(String next) {
            return new Header(fight, dimension, centre, spec, minY, maxY, next, summoner, refund, members);
        }

        public Header withMembers(List<UUID> next) {
            return new Header(fight, dimension, centre, spec, minY, maxY, state, summoner, refund, next);
        }

        /** The same note once the Program is settled (the fight is over: given back or not owed), so a crash gives nothing. */
        public Header withoutRefund() {
            return new Header(fight, dimension, centre, spec, minY, maxY, state, summoner, false, members);
        }

        /** The note of an island that has landed: it owes nothing, and its column in the sky is empty. */
        public Header landed() {
            return new Header(fight, dimension, centre, spec, minY, maxY, LANDED, summoner, false, members);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("v", VERSION);
            tag.putUUID("fight", fight);
            tag.putString("dim", dimension.location().toString());
            tag.putIntArray("centre", new int[]{centre.getX(), centre.getY(), centre.getZ()});
            tag.putInt("radius", spec.radius());
            tag.putInt("height", spec.height());
            tag.putInt("skin", spec.skin());
            tag.putInt("cover", spec.cover());
            tag.putInt("cone", spec.cone());
            tag.putLong("seed", spec.seed());
            tag.putInt("minY", minY);
            tag.putInt("maxY", maxY);
            tag.putString("state", state);
            if (summoner != null) {
                tag.putUUID("summoner", summoner);
            }
            tag.putBoolean("refund", refund);
            ListTag list = new ListTag();
            for (UUID member : members) {
                list.add(NbtUtils.createUUID(member));
            }
            tag.put("members", list);
            return tag;
        }

        /** The note a tag describes, or null if it is not one this version understands. */
        @Nullable
        public static Header fromTag(CompoundTag tag) {
            if (tag.getInt("v") != VERSION || !tag.hasUUID("fight") || tag.getIntArray("centre").length != 3) {
                return null;
            }
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dim"));
            String state = tag.getString("state");
            if (dimension == null || !(state.equals(PASTED) || state.equals(CLEARING) || state.equals(RITUAL) || state.equals(LANDED))) {
                return null;
            }
            int[] centre = tag.getIntArray("centre");
            Spec spec = new Spec(tag.getInt("radius"), tag.getInt("height"), tag.getInt("skin"), tag.getInt("cover"), tag.getInt("cone"), tag.getLong("seed"));
            if (spec.radius() <= 0 || spec.height() <= 0) {
                return null;
            }
            List<UUID> members = new ArrayList<>();
            for (Tag member : tag.getList("members", Tag.TAG_INT_ARRAY)) {
                members.add(NbtUtils.loadUUID(member));
            }
            return new Header(tag.getUUID("fight"), ResourceKey.create(Registries.DIMENSION, dimension), new BlockPos(centre[0], centre[1], centre[2]),
                    spec, tag.getInt("minY"), tag.getInt("maxY"), state, tag.hasUUID("summoner") ? tag.getUUID("summoner") : null,
                    tag.getBoolean("refund"), members);
        }
    }

    /** The folder of the notes of this world. */
    public static Path directory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(DIRECTORY);
    }

    /** The file of a fight's note. */
    public static Path file(MinecraftServer server, UUID fight) {
        return directory(server).resolve(fight + ".nbt");
    }

    /** The file of a fight's note once it is retiring ({@link #delete}). */
    public static Path retiringFile(MinecraftServer server, UUID fight) {
        return directory(server).resolve(fight + ".nbt" + RETIRING);
    }

    /**
     * Writes the note: to a temporary file, flushed to disk, then moved over the real name in one step, so a crash
     * leaves the old note or the new one, never half of one.
     *
     * @throws IOException if the note cannot be written (the lift does not begin without it)
     */
    public static void write(MinecraftServer server, Header header) throws IOException {
        write(file(server, header.fight()), header);
    }

    private static void write(Path target, Header header) throws IOException {
        Files.createDirectories(target.getParent());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(header.toTag(), bytes);
        Path temporary = temporary(target);
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            channel.write(ByteBuffer.wrap(bytes.toByteArray()));
            channel.force(true);
        }
        move(temporary, target);
    }

    /**
     * Deletes the fight's note if it is the ritual's (the fight is over and the Program is not owed): the note of an
     * island that may still be in the sky is never touched here.
     */
    public static void deleteRitual(MinecraftServer server, UUID fight) {
        Path path = file(server, fight);
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            if (RITUAL.equals(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()).getString("state"))) {
                delete(server, fight);
            }
        } catch (IOException | RuntimeException e) {
            Enhanced.LOGGER.error("Could not read the Sokovia journal {} to delete it", path, e);
        }
    }

    /**
     * The island of the note is gone (the lift landed, a stop put everything back, a recovery cleared it): the note is
     * retired (R48). It leaves the live notes at once, moved aside, and is kept until the second save of its dimension
     * from now, or until the server has stopped; at the first save it stops saying a Program is owed, which by then is on
     * the disk with the rest of the world. The ritual's note, which clears nothing, is deleted at once (the note's state
     * is read to know which it is), and so is one that cannot be read. A note that is not there is no error, and a note
     * already retiring stays on its count.
     */
    public static void delete(MinecraftServer server, UUID fight) {
        Header note = read(server, fight);
        if (note == null || note.isRitual()) {
            deleteNow(server, fight);
            return;
        }
        Path live = file(server, fight);
        try {
            if (Files.isRegularFile(live)) {
                move(live, retiringFile(server, fight));
            } else if (RETIRED.containsKey(fight)) {
                return;
            }
            Files.deleteIfExists(temporary(live));
        } catch (IOException e) {
            Enhanced.LOGGER.error("Could not retire the Sokovia journal of fight {}", fight, e);
            return;
        }
        ServerLevel level = server.getLevel(note.dimension());
        RETIRED.put(fight, new Retiring(note, level == null ? 0L : ArenaLoans.saves(level)));
    }

    /** Deletes the note now, live or retiring. */
    private static void deleteNow(MinecraftServer server, UUID fight) {
        RETIRED.remove(fight);
        try {
            Files.deleteIfExists(file(server, fight));
            Files.deleteIfExists(temporary(file(server, fight)));
            Files.deleteIfExists(retiringFile(server, fight));
            Files.deleteIfExists(temporary(retiringFile(server, fight)));
        } catch (IOException e) {
            Enhanced.LOGGER.error("Could not delete the Sokovia journal of fight {}", fight, e);
        }
    }

    /** The note of a fight as it is on the disk: the live one, or else the retiring one; null if there is none to read. */
    @Nullable
    public static Header read(MinecraftServer server, UUID fight) {
        for (Path path : List.of(file(server, fight), retiringFile(server, fight))) {
            if (Files.isRegularFile(path)) {
                try {
                    return Header.fromTag(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()));
                } catch (IOException | RuntimeException e) {
                    Enhanced.LOGGER.error("Could not read the Sokovia journal {}", path, e);
                    return null;
                }
            }
        }
        return null;
    }

    /** Whether this server is keeping the fight's note until the world has saved it ({@link #delete}). */
    public static boolean isRetiring(UUID fight) {
        return RETIRED.containsKey(fight);
    }

    // ---------------------------------------------------------------------------------------------------------
    // The saves of the world (R48)

    /**
     * The dimension was saved ({@code LevelEvent.Save}, its chunks queued to the disk; the save count is {@link
     * ArenaLoans#saves}): a note retiring since one save ago says it owes nothing from now on, and one retiring since two
     * saves ago is deleted. A note whose file is gone (an operator removed it) is forgotten.
     */
    public static void onSave(ServerLevel level) {
        if (RETIRED.isEmpty()) {
            return;
        }
        MinecraftServer server = level.getServer();
        long saves = ArenaLoans.saves(level);
        Iterator<Map.Entry<UUID, Retiring>> it = RETIRED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Retiring> entry = it.next();
            Retiring retiring = entry.getValue();
            if (retiring.dimension != level.dimension()) {
                continue;
            }
            Path path = retiringFile(server, entry.getKey());
            if (!Files.isRegularFile(path)) {
                it.remove();
                continue;
            }
            long since = saves - retiring.save;
            try {
                if (since >= ArenaLoans.SAVES_TO_RETIRE) {
                    Files.deleteIfExists(path);
                    it.remove();
                } else if (since >= 1 && retiring.note.refund()) {
                    // What was paid back (or given back at the stop) is on the disk now: the note owes nothing more.
                    Header settled = retiring.note.withoutRefund();
                    write(path, settled);
                    retiring.note = settled;
                }
            } catch (IOException e) {
                Enhanced.LOGGER.error("Could not retire the Sokovia journal {}", path, e);
            }
        }
    }

    /**
     * The server has stopped, after the flushed save of the stop (R48): every note retiring is deleted. (What the
     * stop's own handlers retired just before is included.)
     */
    public static void retireAll(MinecraftServer server) {
        for (UUID fight : RETIRED.keySet()) {
            try {
                Files.deleteIfExists(retiringFile(server, fight));
                Files.deleteIfExists(temporary(retiringFile(server, fight)));
            } catch (IOException e) {
                Enhanced.LOGGER.error("Could not delete the Sokovia journal of fight {}", fight, e);
            }
        }
        RETIRED.clear();
    }

    /**
     * Forgets that this server keeps the fight's note for a save, as a crash would (D-T test 68): its file stays, and the
     * next recovery reads it.
     */
    @VisibleForTesting
    public static void forgetForTests(UUID fight) {
        RETIRED.remove(fight);
    }

    private static Path temporary(Path file) {
        return file.resolveSibling(file.getFileName() + ".tmp");
    }

    /** Moves a note over another name in one step where the disk can. */
    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Every note in the folder that can be read. A file that cannot (truncated, or from a version this one does not
     * know) is set aside as {@code .corrupt} and the world is not touched on its account; a {@code .tmp} left by a
     * crash is ignored (the real note, if there was one, is still there). A retiring note ({@link #delete}) is read too,
     * after the live ones: one the server stopped short of deleting is a crash's, and its island's column is cleared
     * again; one this server is still keeping for a save is not (nor one with a live note of the same fight).
     */
    public static List<Header> readAll(MinecraftServer server) {
        List<Header> found = new ArrayList<>();
        Path directory = directory(server);
        if (!Files.isDirectory(directory)) {
            return found;
        }
        List<Path> files;
        List<Path> retiring;
        try (Stream<Path> list = Files.list(directory)) {
            List<Path> all = list.sorted().toList();
            files = all.stream().filter(path -> path.getFileName().toString().endsWith(".nbt")).toList();
            retiring = all.stream().filter(path -> path.getFileName().toString().endsWith(".nbt" + RETIRING)).toList();
        } catch (IOException e) {
            Enhanced.LOGGER.error("Could not list the Sokovia journals", e);
            return found;
        }
        Set<UUID> live = new HashSet<>();
        for (Path path : files) {
            Header header = parse(path);
            if (header != null) {
                found.add(header);
                live.add(header.fight());
            }
        }
        for (Path path : retiring) {
            Header header = parse(path);
            if (header != null && !live.contains(header.fight()) && !RETIRED.containsKey(header.fight())) {
                found.add(header);
            }
        }
        return found;
    }

    /** The note a file holds; null, and the file set aside as {@code .corrupt}, if it is not one this version can read. */
    @Nullable
    private static Header parse(Path path) {
        Header header = null;
        try {
            header = Header.fromTag(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()));
        } catch (IOException | RuntimeException e) {
            Enhanced.LOGGER.error("The Sokovia journal {} is unreadable", path, e);
        }
        if (header == null) {
            Enhanced.LOGGER.error("The Sokovia journal {} is not valid and is set aside", path);
            try {
                Files.move(path, path.resolveSibling(path.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                Enhanced.LOGGER.error("Could not set the journal {} aside", path, e);
            }
        }
        return header;
    }

    private SokoviaJournal() {
    }
}
