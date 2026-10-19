package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StoneKind;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.BiFunction;

/**
 * The stone sites that can no longer give their stone to anyone (3.7, lane L5; SavedData {@code
 * enhanced_stone_sites_spent} per level: kind to the set of start chunks): the Stone Locator skips them. Three hooks
 * mark a site spent: the Avengers Tower's cradle emptied ({@code TesseractCradleBlock.take}), the End tunnel's vault
 * chest rolled ({@code SiteStoneLootFunction}), a Hydra outpost's vault chest first opened (the loot function {@code
 * enhanced:mark_spent}, {@link MarkSpentFunction}). The site is the structure of the kind's tag ({@code
 * #enhanced:stone_site/<kind>}) whose piece holds {@code where}; where no such structure stands (a cradle a builder put
 * down), nothing is marked. The Sanctum, Vormir and Asgard are never spent: their pedestal and the ledger decide for
 * each player.
 */
public final class SpentSites extends SavedData {
    public static final String NAME = "enhanced_stone_sites_spent";

    /**
     * The real way to find the start chunk of the site at a position: the structure of the tag whose piece holds it.
     * GameTests, which have no real sites, put a stand-in with {@link #finder} and put this one back when they end.
     */
    public static final BiFunction<ServerLevel, Located, OptionalLong> STRUCTURES = SpentSites::structureStartAt;
    private static BiFunction<ServerLevel, Located, OptionalLong> finder = STRUCTURES;

    /** A question to the finder: the start chunk of the site of these structures that holds this position. */
    public record Located(TagKey<Structure> sites, BlockPos where) {
    }

    private final Map<StoneKind, LongSet> spent = new EnumMap<>(StoneKind.class);

    private static SavedData.Factory<SpentSites> factory() {
        return new SavedData.Factory<>(SpentSites::new, SpentSites::load, null);
    }

    /** The record of {@code level}. */
    public static SpentSites get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), NAME);
    }

    /** The structures whose sites give {@code kind} (the Asgard vault's Reality Stone has none: it is never spent). */
    @Nullable
    public static TagKey<Structure> sites(StoneKind kind) {
        return switch (kind) {
            case MIND -> ModTags.STONE_SITE_MIND;
            case TIME -> ModTags.STONE_SITE_TIME;
            case SPACE -> ModTags.STONE_SITE_SPACE;
            case POWER -> ModTags.STONE_SITE_POWER;
            case SOUL -> ModTags.STONE_SITE_SOUL;
            case REALITY -> null;
        };
    }

    /** The site of {@code kind} that holds {@code where} has given its stone away for good. */
    public static void mark(Level level, StoneKind kind, BlockPos where) {
        TagKey<Structure> tag = sites(kind);
        if (!(level instanceof ServerLevel server) || tag == null) {
            return;
        }
        OptionalLong start = finder.apply(server, new Located(tag, where));
        if (start.isPresent()) {
            mark(server, kind, start.getAsLong());
        }
    }

    /** The site of {@code kind} whose structure starts in the chunk {@code startChunk} is spent now. */
    public static void mark(ServerLevel level, StoneKind kind, long startChunk) {
        SpentSites data = get(level);
        if (data.spent.computeIfAbsent(kind, k -> new LongOpenHashSet()).add(startChunk)) {
            data.setDirty();
            SiteSearch.spent(level, startChunk);
        }
    }

    /** Whether the site of {@code kind} whose structure starts in the chunk {@code startChunk} is spent. */
    public static boolean isSpent(Level level, StoneKind kind, long startChunk) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        LongSet set = get(server).spent.get(kind);
        return set != null && set.contains(startChunk);
    }

    /** Forgets a spent site (GameTests clean up after themselves with it). */
    public static void forget(ServerLevel level, StoneKind kind, long startChunk) {
        SpentSites data = get(level);
        LongSet set = data.spent.get(kind);
        if (set != null && set.remove(startChunk)) {
            data.setDirty();
        }
    }

    /** Puts the finder of sites' start chunks (null: the real one, {@link #STRUCTURES}). */
    public static void finder(@Nullable BiFunction<ServerLevel, Located, OptionalLong> found) {
        finder = found == null ? STRUCTURES : found;
    }

    private static OptionalLong structureStartAt(ServerLevel level, Located at) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(at.where(), at.sites());
        return start.isValid() ? OptionalLong.of(start.getChunkPos().toLong()) : OptionalLong.empty();
    }

    private static SpentSites load(CompoundTag tag, HolderLookup.Provider registries) {
        SpentSites data = new SpentSites();
        for (StoneKind kind : StoneKind.values()) {
            String key = kind.name().toLowerCase(Locale.ROOT);
            if (tag.contains(key, Tag.TAG_LONG_ARRAY)) {
                data.spent.put(kind, new LongOpenHashSet(tag.getLongArray(key)));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<StoneKind, LongSet> entry : spent.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                tag.put(entry.getKey().name().toLowerCase(Locale.ROOT),
                        new LongArrayTag(entry.getValue().toLongArray()));
            }
        }
        return tag;
    }
}
