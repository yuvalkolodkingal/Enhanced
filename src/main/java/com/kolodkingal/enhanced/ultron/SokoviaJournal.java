package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.Enhanced;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The note the lift leaves on disk while an island hangs in the sky, so that a crash or a kill leaves, at worst, a
 * floating island that the next start clears: a few hundred bytes in {@code <world>/data/enhanced/sokovia/<fight>.nbt},
 * gzip-compressed NBT. The file is written <em>before</em> the first block of the island is placed (the world is never
 * changed without it), whole and atomically (a temporary file, flushed to disk, then moved over the real name), and
 * deleted only after the island is gone. Everything else is a function of it: the island's shape comes from the seed,
 * so recovery clears "every block inside this shape, in this range of heights" and needs no copy of anything.
 *
 * <p>The ground is never edited by the lift, so there is no snapshot to restore, and a lost file's worst case is a
 * floating island of ordinary blocks ({@code /enhanced boss cleanup} removes it).
 */
public final class SokoviaJournal {
    /** The island is, or may be, in the sky: the placing has begun. */
    public static final String PASTED = "PASTED";
    /** The island is being taken down. */
    public static final String CLEARING = "CLEARING";

    /** The folder of the notes, inside the world. */
    public static final String DIRECTORY = "data/enhanced/sokovia";
    /** The version of the file's format. */
    public static final int VERSION = 1;

    /**
     * What a note says.
     *
     * @param state   {@link #PASTED} or {@link #CLEARING}
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

        public Header withState(String next) {
            return new Header(fight, dimension, centre, spec, minY, maxY, next, summoner, refund, members);
        }

        public Header withMembers(List<UUID> next) {
            return new Header(fight, dimension, centre, spec, minY, maxY, state, summoner, refund, next);
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
            if (dimension == null || !(state.equals(PASTED) || state.equals(CLEARING))) {
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

    /**
     * Writes the note: to a temporary file, flushed to disk, then moved over the real name in one step, so a crash
     * leaves the old note or the new one, never half of one.
     *
     * @throws IOException if the note cannot be written (the lift does not begin without it)
     */
    public static void write(MinecraftServer server, Header header) throws IOException {
        Path target = file(server, header.fight());
        Files.createDirectories(target.getParent());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(header.toTag(), bytes);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            channel.write(ByteBuffer.wrap(bytes.toByteArray()));
            channel.force(true);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Deletes the note (the island is gone); a note that is not there is no error. */
    public static void delete(MinecraftServer server, UUID fight) {
        try {
            Files.deleteIfExists(file(server, fight));
            Files.deleteIfExists(file(server, fight).resolveSibling(fight + ".nbt.tmp"));
        } catch (IOException e) {
            Enhanced.LOGGER.error("Could not delete the Sokovia journal of fight {}", fight, e);
        }
    }

    /**
     * Every note in the folder that can be read. A file that cannot (truncated, or from a version this one does not
     * know) is set aside as {@code .corrupt} and the world is not touched on its account; a {@code .tmp} left by a
     * crash is ignored (the real note, if there was one, is still there).
     */
    public static List<Header> readAll(MinecraftServer server) {
        List<Header> found = new ArrayList<>();
        Path directory = directory(server);
        if (!Files.isDirectory(directory)) {
            return found;
        }
        List<Path> files;
        try (Stream<Path> list = Files.list(directory)) {
            files = list.filter(path -> path.getFileName().toString().endsWith(".nbt")).sorted().toList();
        } catch (IOException e) {
            Enhanced.LOGGER.error("Could not list the Sokovia journals", e);
            return found;
        }
        for (Path path : files) {
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
            } else {
                found.add(header);
            }
        }
        return found;
    }

    private SokoviaJournal() {
    }
}
