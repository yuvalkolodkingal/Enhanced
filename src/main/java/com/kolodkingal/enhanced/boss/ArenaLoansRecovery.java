package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Puts an arena's ground back after a hard stop of the server (a killed process, a power cut), from the journal its fight
 * left ({@link ArenaLoans}, D-T 5.8). A crash on an exception still stops the server, and every fight gives back what it
 * borrowed then; only a hard stop leaves a journal of a fight that never finished.
 *
 * <p>The journals are read when the server starts (HIGH, before the island's own recovery, {@code SokoviaRecovery}); an
 * unreadable one is set aside as {@code .corrupt}, a {@code .tmp} left by the crash is ignored. Each is restored at the
 * first tick of its dimension (HIGH, before the island's recovery begins to clear: a borrowed block standing on the
 * island is gone before the clearing could drop it): first every standing cell that still holds a block of the type set
 * there gets back what was there before, then every home whose cell is air or a fluid gets its block back, lowest first,
 * each write quiet ({@link ArenaLoans#quiet}). Restoring is idempotent, so a journal that also lists repairs already made
 * changes nothing there. A restored journal is kept until two saves of its dimension hold the restored ground (R48), then
 * deleted.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class ArenaLoansRecovery {
    /** A journal read from the disk: the fight it was written for, its dimension and what it says. */
    private record Journal(UUID fight, ResourceKey<Level> dimension, CompoundTag journal) {
    }

    /** A journal restored, kept until two saves of its dimension after the restoring. */
    private record Restored(ResourceKey<Level> dimension, long save) {
    }

    /** The journals read and not restored yet, by the dimension whose next tick restores them. */
    private static final Map<ResourceKey<Level>, List<Journal>> WAITING = new HashMap<>();
    /** The journals restored, by fight. */
    private static final Map<UUID, Restored> RESTORED = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onServerStarting(ServerStartingEvent event) {
        recoverAll(event.getServer());
    }

    /** The first tick of a dimension with journals waiting restores them, before the island's recovery takes its turn. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!WAITING.isEmpty() && event.getLevel() instanceof ServerLevel level) {
            restore(level);
        }
    }

    /**
     * Reads every arena journal of the world and queues each to be restored at the next tick of its dimension. A journal of
     * a fight this server still knows (running, or finished and waiting for its saves) is no crash's and is left alone.
     */
    public static void recoverAll(MinecraftServer server) {
        for (Journal journal : readAll(server)) {
            if (!ArenaLoans.knows(server, journal.fight())) {
                WAITING.computeIfAbsent(journal.dimension(), key -> new ArrayList<>()).add(journal);
            }
        }
    }

    /** Every journal in the folder that can be read; one that cannot is set aside as {@code .corrupt}. */
    private static List<Journal> readAll(MinecraftServer server) {
        List<Journal> found = new ArrayList<>();
        Path directory = ArenaJournalWriter.directory(server);
        if (!Files.isDirectory(directory)) {
            return found;
        }
        List<Path> files;
        try (Stream<Path> list = Files.list(directory)) {
            files = list.filter(path -> path.getFileName().toString().endsWith(".nbt")).sorted().toList();
        } catch (IOException e) {
            Enhanced.LOGGER.error("Could not list the arena journals", e);
            return found;
        }
        for (Path path : files) {
            Journal journal = null;
            try {
                journal = parse(path, NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()));
            } catch (IOException | RuntimeException e) {
                Enhanced.LOGGER.error("The arena journal {} is unreadable", path, e);
            }
            if (journal == null) {
                Enhanced.LOGGER.error("The arena journal {} is not valid and is set aside", path);
                try {
                    Files.move(path, path.resolveSibling(path.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    Enhanced.LOGGER.error("Could not set the arena journal {} aside", path, e);
                }
            } else {
                found.add(journal);
            }
        }
        return found;
    }

    /** The journal a file holds, or null if it is not one this version understands. */
    @Nullable
    private static Journal parse(Path path, CompoundTag tag) {
        String name = path.getFileName().toString();
        UUID fight;
        try {
            fight = UUID.fromString(name.substring(0, name.length() - ".nbt".length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
        if (tag.getInt("version") != ArenaLoans.VERSION || dimension == null) {
            return null;
        }
        return new Journal(fight, ResourceKey.create(Registries.DIMENSION, dimension), tag);
    }

    /** Restores every journal waiting for this dimension, at once. */
    static void restore(ServerLevel level) {
        List<Journal> journals = WAITING.remove(level.dimension());
        if (journals == null) {
            return;
        }
        for (Journal journal : journals) {
            try {
                restore(level, journal.journal());
            } catch (RuntimeException e) {
                Enhanced.LOGGER.error("The arena journal of fight {} could not be restored", journal.fight(), e);
            }
            RESTORED.put(journal.fight(), new Restored(level.dimension(), ArenaLoans.saves(level)));
        }
    }

    /**
     * Puts the ground of one journal back: the standing cells first (a cell that still holds a block of the type that was
     * set there gets back what was there before it), then the homes, lowest first (a cell that is air or a fluid gets its
     * block back; a creature standing in it is lifted onto it). Each write quiet; the chunks are loaded as needed.
     */
    private static void restore(ServerLevel level, CompoundTag journal) {
        HolderLookup<Block> blocks = level.holderLookup(Registries.BLOCK);
        for (Tag tag : journal.getList("standing", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            BlockPos pos = pos(entry);
            BlockState placed = NbtUtils.readBlockState(blocks, entry.getCompound("placed"));
            if (placed.isAir()) {
                continue; // a block this game no longer knows
            }
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            if (level.getBlockState(pos).is(placed.getBlock())) {
                ArenaLoans.set(level, pos, NbtUtils.readBlockState(blocks, entry.getCompound("previous")));
            }
        }
        List<CompoundTag> homes = new ArrayList<>();
        for (Tag tag : journal.getList("homes", Tag.TAG_COMPOUND)) {
            homes.add((CompoundTag) tag);
        }
        homes.sort(Comparator.comparingInt(entry -> entry.getInt("y")));
        for (CompoundTag entry : homes) {
            BlockPos pos = pos(entry);
            BlockState state = NbtUtils.readBlockState(blocks, entry.getCompound("state"));
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            BlockState here = level.getBlockState(pos);
            if (!state.isAir() && (here.isAir() || here.getBlock() instanceof LiquidBlock)) {
                ArenaLoans.liftOut(level, pos, state);
                ArenaLoans.set(level, pos, state);
            }
        }
    }

    private static BlockPos pos(CompoundTag entry) {
        return new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
    }

    /** The dimension was saved for the {@code saves}th time: a journal restored two saves ago or more is deleted. */
    static void onSave(ServerLevel level, long saves) {
        Iterator<Map.Entry<UUID, Restored>> it = RESTORED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Restored> entry = it.next();
            if (entry.getValue().dimension() == level.dimension() && saves - entry.getValue().save() >= ArenaLoans.SAVES_TO_RETIRE) {
                ArenaJournalWriter.delete(level.getServer(), entry.getKey());
                it.remove();
            }
        }
    }

    /**
     * The server has stopped, after its flushed save: every journal restored is deleted. (A journal of a dimension that never
     * ticked stays for the next start.)
     */
    static void retireAll(MinecraftServer server) {
        for (UUID fight : RESTORED.keySet()) {
            ArenaJournalWriter.delete(server, fight);
        }
        RESTORED.clear();
    }

    /** Forgets everything (the server has stopped). */
    static void clear() {
        WAITING.clear();
        RESTORED.clear();
    }

    private ArenaLoansRecovery() {
    }
}
