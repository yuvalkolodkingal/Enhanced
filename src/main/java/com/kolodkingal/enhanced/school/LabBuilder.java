package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SchoolLabConfig;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.world.SchoolLabPlan;
import com.kolodkingal.enhanced.world.SchoolRepair;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the X-Men lower lab under every Mutant School at run time (3.7, lane L4; the plan's 6.2). Never in chunk
 * generation: one code path for the schools of 3.3 to 3.6 and new ones.
 *
 * <p><b>Finding schools</b> ({@link #tick}, the {@code HydraGarrison} pattern): every {@link #PERIOD} ticks per player
 * in the Overworld, the school starts referenced by the loaded chunks within {@code [school_lab] buildRadius} are found
 * (cached per chunk {@link #CACHE_TICKS}), recorded in {@link SchoolLabs} with where the school lies ({@link LabSite})
 * and, with {@code autoBuild}, set BUILDING.
 *
 * <p><b>Building</b> ({@link #tickLevel}, every tick of the Overworld): a BUILDING lab goes on only while every chunk
 * under its box (and the study, inside it) is loaded in full and nobody stands inside the box, within {@link
 * #BUDGET_NANOS} a tick for all labs together. Its steps, each idempotent and resumed at the recorded step and cursor
 * after a crash: {@link #CHECK} (the box's bottom at least 4 above the world's: else BLOCKED {@code too_deep}); {@link
 * #SHELL} (the box's outer faces from the template, all solid, whatever was there: water, lava and caves are shut out
 * before anything inside opens); {@link #INSIDE} (from the top layer down, at most {@code layersPerTick} layers'
 * worth a tick, template blocks including air; the template's void keeps solid ground and turns the rest to stone);
 * {@link #SWEEP} (any fluid left inside but the tank's is set to its template block: water that crept in between two
 * ticks of the inside step, though nothing wakes it, is gone before the lab opens); {@link #ENTRANCE} (the study's
 * shelves, pads and the lectern's notes, {@link SecretEntrance}: a player's block in the way leaves the lab DONE with
 * reason {@code entrance}). Blocks are set with {@code UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE}: no neighbour updates (no
 * fluid wakes, nothing drops). Then DONE, and the players near hear a far-off hum ({@code school_lab_ready}).
 */
public final class LabBuilder {
    /** How often (ticks) each player's surroundings are checked for schools. */
    public static final int PERIOD = 40;
    /** The time the builder may take a tick, all labs together (the plan's budget is 3 ms of CPU: room for the last few blocks). */
    public static final long BUDGET_NANOS = 1_500_000L;
    /** How long a chunk's list of schools near it is kept (ticks). */
    static final int CACHE_TICKS = 600;
    /** The lab's lowest block must be at least this far above the world's bottom. */
    public static final int MIN_ABOVE_BOTTOM = 4;

    /** The steps of a BUILDING lab ({@link SchoolLabs.Lab#step}). */
    public static final int CHECK = 0;
    public static final int SHELL = 1;
    public static final int INSIDE = 2;
    public static final int SWEEP = 3;
    public static final int ENTRANCE = 4;

    public static final String TOO_DEEP = "too_deep";
    public static final String ENTRANCE_BLOCKED = "entrance";

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    /** The school-template point the log names a school by (the middle of its hall). */
    private static final BlockPos HALL = new BlockPos(23, 6, 22);

    private record Found(long until, LongSet starts) {
    }

    private static final Map<Long, Found> CACHE = new HashMap<>();

    // ---- the numbers ----

    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || SchoolLabConfig.ENABLED.get();
    }

    static boolean autoBuild() {
        return !EnhancedConfig.SPEC.isLoaded() || SchoolLabConfig.AUTO_BUILD.get();
    }

    static int buildRadius() {
        return EnhancedConfig.SPEC.isLoaded() ? SchoolLabConfig.BUILD_RADIUS.get() : 96;
    }

    static int layersPerTick() {
        return EnhancedConfig.SPEC.isLoaded() ? SchoolLabConfig.LAYERS_PER_TICK.get() : 2;
    }

    // ---- finding schools ----

    /** Every player tick (lane L4's {@link LabEvents}): the schools near {@code player} are recorded and begun. */
    public static void tick(ServerPlayer player) {
        if ((player.tickCount + player.getId()) % PERIOD != 0 || player.isSpectator()
                || player.level().dimension() != Level.OVERWORLD || !enabled() || !autoBuild()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        SchoolLabs store = SchoolLabs.get(level);
        int radius = buildRadius();
        for (long start : startsNear(level, player.blockPosition(), radius)) {
            SchoolLabs.Lab lab = store.byKey(start);
            if (lab == null) {
                lab = resolve(level, store, start);
                if (lab == null) {
                    continue;
                }
            }
            if (lab.state() == SchoolLabs.State.NONE && horizontalDistance(lab.site().box(), player.blockPosition())
                    <= radius) {
                store.put(lab.at(SchoolLabs.State.BUILDING, CHECK, 0, ""));
            }
        }
    }

    /** The start chunks of the schools referenced by the loaded chunks round {@code pos} (cached a while). */
    static LongSet startsNear(ServerLevel level, BlockPos pos, int radius) {
        long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        long now = level.getGameTime();
        Found found = CACHE.get(chunk);
        if (found != null && found.until() > now) {
            return found.starts();
        }
        if (CACHE.size() > 4096) {
            CACHE.entrySet().removeIf(e -> e.getValue().until() <= now);
        }
        LongSet starts = new LongOpenHashSet();
        Structure school = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SchoolRepair.SCHOOL);
        if (school != null) {
            int reach = (radius + 32) >> 4;
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    LevelChunk c = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                    if (c != null) {
                        starts.addAll(c.getReferencesForStructure(school));
                    }
                }
            }
        }
        CACHE.put(chunk, new Found(now + CACHE_TICKS, starts));
        return starts;
    }

    /** Records the school whose structure start is in chunk {@code start}, from its piece; null if not loaded. */
    @Nullable
    static SchoolLabs.Lab resolve(ServerLevel level, SchoolLabs store, long start) {
        Structure school = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(SchoolRepair.SCHOOL);
        if (school == null) {
            return null;
        }
        ChunkPos at = new ChunkPos(start);
        ChunkAccess chunk = level.getChunk(at.x, at.z, ChunkStatus.STRUCTURE_STARTS, false);
        StructureStart found = chunk == null ? null : chunk.getStartForStructure(school);
        if (found == null || !found.isValid()) {
            return null;
        }
        for (StructurePiece piece : found.getPieces()) {
            if (piece instanceof TemplateSitePiece site) {
                return record(store, start, site);
            }
        }
        return null;
    }

    /** The record of the school {@code piece} (whose start is in chunk {@code key}), made NONE if it is new. */
    public static SchoolLabs.Lab record(SchoolLabs store, long key, TemplateSitePiece piece) {
        return record(store, key, LabSite.of(piece));
    }

    /** The record of a school at {@code site} (keyed {@code key}), made NONE if it is new. */
    public static SchoolLabs.Lab record(SchoolLabs store, long key, LabSite site) {
        SchoolLabs.Lab lab = store.byKey(key);
        if (lab == null) {
            lab = new SchoolLabs.Lab(key, SchoolLabs.State.NONE, CHECK, SchoolLabPlan.VERSION, "", site.origin(),
                    site.turn(), site.pivot(), 0);
            store.put(lab);
        }
        return lab;
    }

    static double horizontalDistance(BoundingBox box, BlockPos pos) {
        int dx = Math.max(0, Math.max(box.minX() - pos.getX(), pos.getX() - box.maxX()));
        int dz = Math.max(0, Math.max(box.minZ() - pos.getZ(), pos.getZ() - box.maxZ()));
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    // ---- building ----

    /** Every tick of the Overworld: the BUILDING labs go on, within the budget. */
    public static void tickLevel(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD || !enabled()) {
            return;
        }
        SchoolLabs store = SchoolLabs.get(level);
        List<SchoolLabs.Lab> building = new ArrayList<>();
        for (SchoolLabs.Lab lab : store.all()) {
            if (lab.state() == SchoolLabs.State.BUILDING) {
                building.add(lab);
            }
        }
        if (building.isEmpty()) {
            return;
        }
        long deadline = System.nanoTime() + BUDGET_NANOS;
        boolean first = true;
        for (SchoolLabs.Lab lab : building) {
            if (!first && System.nanoTime() >= deadline) {
                break;
            }
            if (!ready(level, lab.site())) {
                continue;
            }
            store.put(advance(level, lab, deadline, layersPerTick(), false));
            first = false;
        }
    }

    /** Whether the lab at {@code site} may be built now: its chunks loaded in full, nobody inside its box. */
    public static boolean ready(ServerLevel level, LabSite site) {
        BoundingBox box = site.box();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (level.getChunkSource().getChunkNow(cx, cz) == null) {
                    return false;
                }
            }
        }
        return level.getEntitiesOfClass(ServerPlayer.class, site.bounds().inflate(1.0)).isEmpty();
    }

    /**
     * Builds the whole lab now (the command; a test): every step without a budget, loading its chunks. {@code force}
     * puts the entrance over a player's blocks; a DONE lab is only given its entrance again, and only when forced.
     */
    public static SchoolLabs.Lab buildNow(ServerLevel level, SchoolLabs.Lab lab, boolean force) {
        SchoolLabs store = SchoolLabs.get(level);
        BoundingBox box = lab.site().box();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
        if (lab.state() == SchoolLabs.State.DONE) {
            if (!force) {
                return lab;
            }
            lab = lab.at(SchoolLabs.State.BUILDING, ENTRANCE, 0, "");
        } else if (lab.state() != SchoolLabs.State.BUILDING) {
            lab = lab.at(SchoolLabs.State.BUILDING, CHECK, 0, "");
        }
        lab = advance(level, lab, Long.MAX_VALUE, Integer.MAX_VALUE, force);
        store.put(lab);
        return lab;
    }

    /**
     * Takes a BUILDING lab on as far as {@code deadline} ({@code System.nanoTime}) and {@code maxLayers} allow;
     * returns its new record (at most {@code maxLayers} layers' worth of the inside a call). Each step is idempotent:
     * run again from the same step and cursor, it ends with the same blocks.
     */
    public static SchoolLabs.Lab advance(ServerLevel level, SchoolLabs.Lab lab, long deadline, int maxLayers,
                                         boolean force) {
        LabTemplate template = LabTemplate.get(level);
        LabSite site = lab.site();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean worked = false;
        while (lab.state() == SchoolLabs.State.BUILDING) {
            switch (lab.step()) {
                case CHECK -> {
                    if (site.box().minY() < level.getMinBuildHeight() + MIN_ABOVE_BOTTOM) {
                        Enhanced.LOGGER.warn("[school_lab] the lab under the school at {} would be too deep ({}): "
                                + "not built", site.school(HALL), site.box().minY());
                        return lab.at(SchoolLabs.State.BLOCKED, CHECK, 0, TOO_DEEP);
                    }
                    lab = lab.at(SchoolLabs.State.BUILDING, SHELL, 0, "");
                }
                case SHELL -> {
                    int total = template.sx * template.sy * template.sz;
                    int i = lab.cursor();
                    int since = 0;
                    while (i < total) {
                        int x = i % template.sx;
                        int z = (i / template.sx) % template.sz;
                        int y = i / (template.sx * template.sz);
                        i++;
                        if (!template.face(x, y, z)) {
                            continue;
                        }
                        place(level, site, template, x, y, z, pos);
                        worked = true;
                        if (++since >= 32) {
                            since = 0;
                            if (System.nanoTime() >= deadline) {
                                return lab.at(SchoolLabs.State.BUILDING, SHELL, i, "");
                            }
                        }
                    }
                    lab = lab.at(SchoolLabs.State.BUILDING, INSIDE, 0, "");
                    if (System.nanoTime() >= deadline) {
                        return lab;
                    }
                }
                case INSIDE -> {
                    int across = (template.sx - 2) * (template.sz - 2);
                    int total = across * (template.sy - 2);
                    int i = lab.cursor();
                    int limit = maxLayers >= total / across ? total : Math.min(total, i + maxLayers * across);
                    int since = 0;
                    while (i < limit) {
                        int within = i % across;
                        int y = template.sy - 2 - i / across;
                        place(level, site, template, 1 + within % (template.sx - 2), y,
                                1 + within / (template.sx - 2), pos);
                        worked = true;
                        i++;
                        if (++since >= 32) {
                            since = 0;
                            if (System.nanoTime() >= deadline) {
                                break;
                            }
                        }
                    }
                    if (i < total) {
                        return lab.at(SchoolLabs.State.BUILDING, INSIDE, i, "");
                    }
                    lab = lab.at(SchoolLabs.State.BUILDING, SWEEP, 0, "");
                    if (System.nanoTime() >= deadline) {
                        return lab;
                    }
                }
                case SWEEP -> {
                    int y = lab.cursor() + 1;
                    if (y > template.sy - 2) {
                        // the entrance has a tick of its own (it touches the school above ground)
                        lab = lab.at(SchoolLabs.State.BUILDING, ENTRANCE, 0, "");
                        if (deadline != Long.MAX_VALUE) {
                            return lab;
                        }
                        continue;
                    }
                    if (worked && System.nanoTime() >= deadline) {
                        return lab;
                    }
                    sweep(level, site, template, y, pos);
                    worked = true;
                    lab = lab.at(SchoolLabs.State.BUILDING, SWEEP, lab.cursor() + 1, "");
                }
                case ENTRANCE -> {
                    boolean placed = SecretEntrance.place(level, site, force);
                    lab = lab.at(SchoolLabs.State.DONE, ENTRANCE, 0, placed ? "" : ENTRANCE_BLOCKED);
                    done(level, site, placed);
                }
                default -> lab = lab.at(SchoolLabs.State.BUILDING, CHECK, 0, "");
            }
        }
        return lab;
    }

    /** Sets one template position in the world (void: solid ground stays, anything else becomes stone). */
    private static void place(ServerLevel level, LabSite site, LabTemplate template, int x, int y, int z,
                              BlockPos.MutableBlockPos pos) {
        pos.set(site.lab(x, y, z));
        BlockState there = level.getBlockState(pos);
        BlockState want = template.at(x, y, z);
        if (want == null) {
            if (keepsGround(level, pos, there)) {
                return;
            }
            want = STONE;
        } else {
            want = site.turn(want);
        }
        if (there == want) {
            return;
        }
        level.setBlock(pos, want, FLAGS);
        CompoundTag data = template.data(x, y, z);
        if (data != null) {
            load(level, pos.immutable(), want, data);
        }
    }

    /** Whether the ground at a void position stays: whole, opaque, dry and plain (stone, dirt, ores...). */
    static boolean keepsGround(ServerLevel level, BlockPos pos, BlockState there) {
        return there.getFluidState().isEmpty() && !there.hasBlockEntity() && there.isSolidRender(level, pos);
    }

    private static void load(ServerLevel level, BlockPos pos, BlockState state, CompoundTag data) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return;
        }
        if (data.contains("LootTable") && entity instanceof RandomizableContainer container) {
            container.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.parse(data.getString("LootTable"))), level.getSeed() ^ pos.asLong());
        } else {
            entity.loadWithComponents(data, level.registryAccess());
        }
        entity.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    /** Any fluid left in a layer of the box (but the template's own water) is set to its template block. */
    private static void sweep(ServerLevel level, LabSite site, LabTemplate template, int y,
                              BlockPos.MutableBlockPos pos) {
        for (int z = 1; z < template.sz - 1; z++) {
            for (int x = 1; x < template.sx - 1; x++) {
                pos.set(site.lab(x, y, z));
                BlockState there = level.getBlockState(pos);
                if (there.getFluidState().isEmpty()) {
                    continue;
                }
                BlockState want = template.at(x, y, z);
                if (want != null && !want.getFluidState().isEmpty()) {
                    continue;
                }
                level.setBlock(pos, want == null ? STONE : site.turn(want), FLAGS);
            }
        }
    }

    private static void done(ServerLevel level, LabSite site, boolean entrance) {
        BlockPos middle = site.box().getCenter();
        Enhanced.LOGGER.info("[school_lab] the lower lab under the school at {} is built (its box {}){}",
                site.school(HALL), site.box(), entrance ? "" : "; the entrance is blocked");
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(middle) <= 96 * 96) {
                player.playNotifySound(ModSounds.SCHOOL_LAB_READY.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            }
        }
    }

    /** Drops the caches (the server stopped). */
    public static void clear() {
        CACHE.clear();
    }

    private LabBuilder() {
    }
}
