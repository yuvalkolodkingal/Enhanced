package com.kolodkingal.enhanced.road;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.LongStream;

/**
 * What the Road keeps in the world's save (the overworld's data storage, {@code data/enhanced_witches_road.dat}): every
 * player's {@link RoadProgress}, and the ledger of cells in use. A cell is entered in the ledger when it is allocated and
 * given the list of chunks it touches BEFORE its first block is placed, and it leaves the ledger only when it has been
 * cleared: so whatever happens (a crash, a stop), the next start knows every cell that may hold blocks.
 */
public final class RoadSave extends SavedData {
    private static final String NAME = "enhanced_witches_road";

    /** A cell of the grid that holds, or may hold, blocks of a trial. */
    public record Cell(int index, UUID owner, String challenge, long[] chunks, int minY, int maxY) {
        public static final Codec<Cell> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("index").forGetter(Cell::index),
                UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(Cell::owner),
                Codec.STRING.fieldOf("challenge").forGetter(Cell::challenge),
                Codec.LONG_STREAM.fieldOf("chunks").forGetter(c -> Arrays.stream(c.chunks)),
                Codec.INT.fieldOf("minY").forGetter(Cell::minY),
                Codec.INT.fieldOf("maxY").forGetter(Cell::maxY)).apply(i, (index, owner, challenge, chunks, minY, maxY) ->
                new Cell(index, owner, challenge, chunks.toArray(), minY, maxY)));
    }

    private record Data(Map<UUID, RoadProgress> players, List<Cell> cells) {
        static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, RoadProgress.CODEC).optionalFieldOf("players", Map.of()).forGetter(Data::players),
                Cell.CODEC.listOf().optionalFieldOf("cells", List.of()).forGetter(Data::cells)).apply(i, Data::new));
    }

    private final Map<UUID, RoadProgress> players = new LinkedHashMap<>();
    private final Map<Integer, Cell> cells = new HashMap<>();

    public static SavedData.Factory<RoadSave> factory() {
        return new SavedData.Factory<>(RoadSave::new, RoadSave::load, null);
    }

    /** The Road's save of this server. */
    public static RoadSave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    public static RoadSave load(CompoundTag tag, HolderLookup.Provider registries) {
        RoadSave save = new RoadSave();
        DataResult<Data> result = Data.CODEC.parse(NbtOps.INSTANCE, tag);
        result.resultOrPartial(error -> com.kolodkingal.enhanced.Enhanced.LOGGER.error("The Witches' Road save is damaged: {}", error))
                .ifPresent(data -> {
                    data.players().forEach((id, progress) -> {
                        progress.attach(save::setDirty);
                        save.players.put(id, progress);
                    });
                    data.cells().forEach(cell -> save.cells.put(cell.index(), cell));
                });
        return save;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Map<UUID, RoadProgress> kept = new LinkedHashMap<>();
        players.forEach((id, progress) -> {
            if (!progress.isBlank()) {
                kept.put(id, progress);
            }
        });
        Tag encoded = Data.CODEC.encodeStart(NbtOps.INSTANCE, new Data(kept, new ArrayList<>(cells.values()))).getOrThrow();
        if (encoded instanceof CompoundTag compound) {
            tag.merge(compound);
        }
        return tag;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Progress

    /** The player's progress, started if he has none. */
    public RoadProgress progress(UUID player) {
        return players.computeIfAbsent(player, id -> {
            RoadProgress progress = new RoadProgress();
            progress.attach(this::setDirty);
            return progress;
        });
    }

    /** The player's progress if he has any. */
    @Nullable
    public RoadProgress existing(UUID player) {
        return players.get(player);
    }

    /** Forgets everything about the player. */
    public void forget(UUID player) {
        if (players.remove(player) != null) {
            setDirty();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The cell ledger

    /** The lowest free cell below {@code limit}, entered in the ledger for the owner; -1 if there is none. */
    public int allocate(UUID owner, String challenge, int limit) {
        for (int i = 0; i < Math.min(limit, RoadCells.MAX_CELLS); i++) {
            if (!cells.containsKey(i)) {
                cells.put(i, new Cell(i, owner, challenge, new long[0], 0, 0));
                setDirty();
                return i;
            }
        }
        return -1;
    }

    /** Records the chunks and heights a cell's canvas touches (before anything is placed). */
    public void plan(int index, long[] chunks, int minY, int maxY) {
        Cell old = cells.get(index);
        if (old != null) {
            cells.put(index, new Cell(index, old.owner(), old.challenge(), chunks.clone(), minY, maxY));
            setDirty();
        }
    }

    @Nullable
    public Cell cell(int index) {
        return cells.get(index);
    }

    /** The cell has been cleared: it is free again. */
    public void release(int index) {
        if (cells.remove(index) != null) {
            setDirty();
        }
    }

    public List<Cell> cells() {
        return new ArrayList<>(cells.values());
    }

    public int cellCount() {
        return cells.size();
    }

    /** Everything, for a test that wants a clean slate (the world has nothing else of the Road). */
    public void clearAll() {
        players.clear();
        cells.clear();
        setDirty();
    }

    /** The players with a progress to keep (for the commands). */
    public Map<UUID, RoadProgress> players() {
        return Map.copyOf(players);
    }

    /** The cell's chunks as a stream (for the sweeper). */
    public static LongStream chunksOf(Cell cell) {
        return Arrays.stream(cell.chunks());
    }
}
