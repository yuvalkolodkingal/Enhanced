package com.kolodkingal.enhanced.tower;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.TowerConfig;
import com.kolodkingal.enhanced.world.SchoolRepair;
import com.kolodkingal.enhanced.world.SitePoints;
import com.kolodkingal.enhanced.world.TemplateSitePiece;
import com.kolodkingal.enhanced.world.TowerPlan;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The run-time refit of the Avengers Towers generated before 3.8 (PLAN 9.6, the {@code HydraGarrison} pattern):
 * chunk generation never builds an old tower again, so the armoury is set down in it here. Every {@code
 * [avengers_tower] refitIntervalTicks} (100) per player in the Overworld, the towers within {@code refitRadius} (96)
 * are found by their structure starts (cached per chunk 600 ticks); one the refit has not been to ({@link TowerRefits})
 * whose armoury's chunks and entities are loaded is refitted <b>once</b>, a step a tick within {@link #STEP_NANOS}
 * ({@link Job}, {@link #work}; {@link #refit} at once for the command): each block of {@link
 * TowerRefitTable} is set down <b>only where the 3.6.1 template's own block still stands</b> (turned as the tower was;
 * {@link #same}): a player's block is left as it is and counted; the five decorative armour stands go only if each
 * still stands on its spot with its 3.6.1 armour (else it stays, and so does the floor where its display would go),
 * Cap's item frame only if it still holds its shield. A tower that is neither 3.6.1's nor 3.8's (the 3.6.0 design:
 * most of the blocks round the armoury are others, {@link #isArmouryTower}) is left as it is and logged; one generated
 * by 3.8 is recorded with nothing to do. The takers of displays and cases are never touched. {@code refit false} stops
 * it; {@code /enhanced structure refit_tower} does it at once ({@link TowerCommand}).
 */
public final class TowerRefit {
    public static final ResourceKey<Structure> TOWER = ResourceKey.create(Registries.STRUCTURE,
            Enhanced.id("avengers_tower"));
    /** The armoury's version: a tower with it (or with {@link #OLD_DESIGN}) is not refitted again by itself. */
    public static final int VERSION = 1;
    /** The record of a 3.6.0 tower: not the 3.6.1 design, so the armoury fits nowhere in it. */
    public static final int OLD_DESIGN = -1;
    /** How long a chunk's list of towers near it is kept (ticks). */
    private static final int CACHE_TICKS = 600;
    /** Of the blocks both towers have round the armoury, at least this share must stand for a refit. */
    private static final double ANCHORS_NEEDED = 2.0 / 3.0;

    /** What a refit did: blocks set down, already there, left (a player's), entities removed, entities left. */
    public record Result(int changed, int already, int skipped, int removed, int kept, boolean oldDesign) {
        static final Result OLD = new Result(0, 0, 0, 0, 0, true);
    }

    /** A tower: the chunk of its structure start and its three pieces, base first. */
    public record Tower(long start, List<TemplateSitePiece> pieces) {
        /** The middle of its base at its lowest layer. */
        public BlockPos at() {
            BoundingBox box = pieces.get(0).getBoundingBox();
            return new BlockPos(box.getCenter().getX(), box.minY(), box.getCenter().getZ());
        }
    }

    // ---- the table ----

    /** One block of the refit: where (the model's frame), the 3.6.1 block, the 3.8.1 block and its data. */
    record Change(BlockPos model, BlockState before, BlockState after, @Nullable CompoundTag data,
                  @Nullable Sign sign) {
    }

    /** A sign's text: its colour and its four lines' lang keys ("" an empty line). */
    record Sign(DyeColor colour, List<String> keys) {
    }

    record Stand(BlockPos model, List<Item> armour) {
    }

    record Frame(BlockPos model, Direction facing, Item item) {
    }

    record Anchor(BlockPos model, BlockState state) {
    }

    private static List<Change> changes;
    private static List<Stand> stands;
    private static List<Frame> frames;
    private static List<Anchor> anchors;

    private static synchronized void parse() {
        if (changes != null) {
            return;
        }
        List<Change> c = new ArrayList<>();
        for (String row : TowerRefitTable.BLOCKS) {
            String[] f = row.split("\\|", -1);
            String data = f[3];
            Sign sign = null;
            CompoundTag tag = null;
            if (data.startsWith("sign:")) {
                String[] s = data.split(":", 3);
                sign = new Sign(DyeColor.byName(s[1], DyeColor.WHITE), List.of(s[2].split(",", -1)));
            } else if (!data.isEmpty()) {
                try {
                    tag = TagParser.parseTag(data);
                } catch (CommandSyntaxException e) {
                    throw new IllegalStateException("the tower's refit table: " + row, e);
                }
            }
            c.add(new Change(pos(f[0]), state(f[1]), state(f[2]), tag, sign));
        }
        List<Stand> s = new ArrayList<>();
        for (String row : TowerRefitTable.STANDS) {
            String[] f = row.split("\\|");
            List<Item> armour = new ArrayList<>();
            for (String id : f[1].split(",")) {
                armour.add(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
            }
            s.add(new Stand(pos(f[0]), List.copyOf(armour)));
        }
        List<Frame> fr = new ArrayList<>();
        for (String row : TowerRefitTable.FRAMES) {
            String[] f = row.split("\\|");
            fr.add(new Frame(pos(f[0]), Direction.byName(f[1]),
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse(f[2]))));
        }
        List<Anchor> a = new ArrayList<>();
        for (String row : TowerRefitTable.ANCHORS) {
            String[] f = row.split("\\|");
            a.add(new Anchor(pos(f[0]), state(f[1])));
        }
        stands = List.copyOf(s);
        frames = List.copyOf(fr);
        anchors = List.copyOf(a);
        changes = List.copyOf(c);
    }

    private static BlockPos pos(String xyz) {
        String[] p = xyz.split(" ");
        return new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
    }

    private static BlockState state(String text) {
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), text, false).blockState();
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("the tower's refit table: " + text, e);
        }
    }

    /** The table's blocks: the model point, the 3.6.1 block and the 3.8.1 block of each (for the tests). */
    public static List<BlockState[]> table(List<BlockPos> points) {
        parse();
        List<BlockState[]> out = new ArrayList<>();
        for (Change c : changes) {
            points.add(c.model());
            out.add(new BlockState[]{c.before(), c.after()});
        }
        return out;
    }

    // ---- the numbers ----

    static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || TowerConfig.REFIT.get();
    }

    static int radius() {
        return EnhancedConfig.SPEC.isLoaded() ? TowerConfig.REFIT_RADIUS.get() : 96;
    }

    static int interval() {
        return EnhancedConfig.SPEC.isLoaded() ? TowerConfig.REFIT_INTERVAL_TICKS.get() : 100;
    }

    // ---- the player's tick ----

    /** The Overworld's chunks a player stood in → the towers' start chunks near it, and until when that is known. */
    private static final Map<Long, Found> CACHE = new HashMap<>();

    private record Found(long until, LongSet starts) {
    }

    /** The refit's look round a player (every {@code refitIntervalTicks}; the Overworld only). */
    public static void tick(ServerPlayer player) {
        if ((player.tickCount + player.getId()) % interval() != 0 || player.isSpectator() || !enabled()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        refitNear(level, player.blockPosition());
    }

    /** The refits under way (the Overworld's), by start chunk: each goes on a step a tick ({@link #work}). */
    private static final Map<Long, Running> RUNNING = new HashMap<>();
    /** The time all the refits under way may take in one tick (the plan's budget is 2 ms). */
    public static final long STEP_NANOS = 1_000_000L;

    private record Running(Tower tower, Job job) {
    }

    /**
     * Starts the refit of the towers within {@code refitRadius} of {@code pos} that the refit has not been to and whose
     * armoury is loaded (it goes on a step a tick, {@link #work}); returns how many it started.
     */
    public static int refitNear(ServerLevel level, BlockPos pos) {
        LongSet starts = startsNear(level, pos);
        if (starts.isEmpty()) {
            return 0;
        }
        TowerRefits store = TowerRefits.get(level);
        int started = 0;
        for (long start : starts) {
            if (store.get(start) != null || RUNNING.containsKey(start)) {
                continue;
            }
            Tower tower = resolve(level, start, false);
            if (tower == null || horizontalDistance(tower.pieces().get(0).getBoundingBox(), pos) > radius()
                    || !loaded(level, tower.pieces())) {
                continue;
            }
            RUNNING.put(start, new Running(tower, new Job(tower.pieces(), false)));
            started++;
        }
        return started;
    }

    /**
     * One tick of the refits under way (the Overworld's tick): each takes its share of {@link #STEP_NANOS}; one whose
     * armoury is no longer loaded waits; one that is done is recorded ({@link TowerRefits}) and logged.
     */
    public static void work(ServerLevel level) {
        if (RUNNING.isEmpty() || level.dimension() != Level.OVERWORLD) {
            return;
        }
        long start = System.nanoTime();
        var it = RUNNING.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Running running = e.getValue();
            long left = STEP_NANOS - (System.nanoTime() - start);
            if (left <= 0) {
                return;
            }
            if (!loaded(level, running.tower().pieces()) || !running.job().step(level, left)) {
                continue;
            }
            it.remove();
            Result result = running.job().result();
            TowerRefits.get(level).record(e.getKey(), new TowerRefits.Entry(result.oldDesign() ? OLD_DESIGN
                    : VERSION, result, level.getGameTime()));
            BlockPos at = running.tower().at();
            if (result.oldDesign()) {
                Enhanced.LOGGER.info("[tower] The Avengers Tower at {} {} {} is of the 3.6.0 design: left as it is",
                        at.getX(), at.getY(), at.getZ());
            } else {
                Enhanced.LOGGER.info("[tower] Refitted the Avengers Tower at {} {} {}: {}", at.getX(), at.getY(),
                        at.getZ(), result);
            }
        }
    }

    /** Whether a refit of the tower whose start is in chunk {@code start} is under way. */
    public static boolean running(long start) {
        return RUNNING.containsKey(start);
    }

    /** Stops the refit under way of that tower (the command refits it at once instead). */
    public static void stop(long start) {
        RUNNING.remove(start);
    }

    /** The start chunks of the towers referenced by the loaded chunks round {@code pos} (cached a while). */
    static LongSet startsNear(ServerLevel level, BlockPos pos) {
        long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        long now = level.getGameTime();
        Found found = CACHE.get(chunk);
        if (found != null && found.until() > now && found.until() - CACHE_TICKS <= now) {
            return found.starts();
        }
        if (CACHE.size() > 4096) {
            CACHE.entrySet().removeIf(e -> e.getValue().until() <= now);
        }
        LongSet starts = new LongOpenHashSet();
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TOWER);
        if (structure != null) {
            int reach = (radius() + 32) >> 4;
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    LevelChunk c = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                    if (c != null) {
                        starts.addAll(c.getReferencesForStructure(structure));
                    }
                }
            }
        }
        CACHE.put(chunk, new Found(now + CACHE_TICKS, starts));
        return starts;
    }

    /** Forgets the towers known near each chunk and the refits under way (a stopped server; a test's tower). */
    public static void clearCache() {
        CACHE.clear();
        RUNNING.clear();
    }

    /**
     * The tower whose structure start is in chunk {@code start}, or null if there is none ({@code load}: load that
     * chunk as far as its structure starts, as a command may; else only a chunk already there).
     */
    @Nullable
    public static Tower resolve(ServerLevel level, long start, boolean load) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TOWER);
        if (structure == null) {
            return null;
        }
        ChunkPos at = new ChunkPos(start);
        ChunkAccess chunk = level.getChunk(at.x, at.z, ChunkStatus.STRUCTURE_STARTS, load);
        StructureStart found = chunk == null ? null : chunk.getStartForStructure(structure);
        return found == null || !found.isValid() ? null : of(start, found);
    }

    @Nullable
    private static Tower of(long start, StructureStart found) {
        List<TemplateSitePiece> pieces = new ArrayList<>();
        for (StructurePiece piece : found.getPieces()) {
            if (piece instanceof TemplateSitePiece site) {
                pieces.add(site);
            }
        }
        if (pieces.size() != 3) {
            return null;
        }
        pieces.sort(Comparator.comparingInt(p -> p.getBoundingBox().minY()));
        return new Tower(start, List.copyOf(pieces));
    }

    /** The tower nearest {@code pos} (horizontally, to its footprint) within {@code radius}, for the command. */
    public static Optional<Tower> nearest(ServerLevel level, BlockPos pos, int radius) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TOWER);
        if (structure == null) {
            return Optional.empty();
        }
        int reach = (radius + 32) >> 4;
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        Tower best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                ChunkAccess chunk = level.getChunk(cx + dx, cz + dz, ChunkStatus.STRUCTURE_STARTS, true);
                StructureStart start = chunk == null ? null : chunk.getStartForStructure(structure);
                if (start == null || !start.isValid()) {
                    continue;
                }
                Tower tower = of(ChunkPos.asLong(cx + dx, cz + dz), start);
                if (tower == null) {
                    continue;
                }
                double distance = horizontalDistance(tower.pieces().get(0).getBoundingBox(), pos);
                if (distance <= radius && distance < bestDistance) {
                    best = tower;
                    bestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static double horizontalDistance(BoundingBox box, BlockPos pos) {
        int dx = Math.max(0, Math.max(box.minX() - pos.getX(), pos.getX() - box.maxX()));
        int dz = Math.max(0, Math.max(box.minZ() - pos.getZ(), pos.getZ() - box.maxZ()));
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    // ---- the refit ----

    /** Where a point of the model's frame ({@link TowerPlan}) is in the world: through the piece of its part. */
    public static BlockPos world(List<TemplateSitePiece> pieces, BlockPos model) {
        TowerPlan.InTemplate in = TowerPlan.inTemplate(model);
        return SitePoints.toWorld(pieces.get(in.part()), in.local());
    }

    private static TemplateSitePiece pieceOf(List<TemplateSitePiece> pieces, BlockPos model) {
        return pieces.get(TowerPlan.inTemplate(model).part());
    }

    /** Whether every chunk of the armoury (its blocks, and the entities of the stands and the frame) is loaded. */
    public static boolean loaded(ServerLevel level, List<TemplateSitePiece> pieces) {
        parse();
        Set<Long> blocks = new HashSet<>();
        Set<Long> entities = new HashSet<>();
        for (Change c : changes) {
            blocks.add(new ChunkPos(world(pieces, c.model())).toLong());
        }
        for (Anchor a : anchors) {
            blocks.add(new ChunkPos(world(pieces, a.model())).toLong());
        }
        for (Stand s : stands) {
            entities.add(new ChunkPos(world(pieces, s.model())).toLong());
        }
        for (Frame f : frames) {
            entities.add(new ChunkPos(world(pieces, f.model())).toLong());
        }
        for (long c : blocks) {
            if (!level.hasChunk(ChunkPos.getX(c), ChunkPos.getZ(c))) {
                return false;
            }
        }
        for (long c : entities) {
            if (!level.hasChunk(ChunkPos.getX(c), ChunkPos.getZ(c)) || !level.areEntitiesLoaded(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the tower is of the 3.6.1 design (or 3.8's): most of the blocks both have round the armoury stand (a
     * player may have changed a few).
     */
    public static boolean isArmouryTower(ServerLevel level, List<TemplateSitePiece> pieces) {
        parse();
        int standing = 0;
        for (Anchor a : anchors) {
            BlockState template = SchoolRepair.turned(pieceOf(pieces, a.model()), a.state());
            if (same(level.getBlockState(world(pieces, a.model())), template)) {
                standing++;
            }
        }
        return standing >= Math.ceil(anchors.size() * ANCHORS_NEEDED);
    }

    /**
     * Whether a block in the world is still the template's: the same block, facing and half ({@link
     * SchoolRepair#same}), and the same half of a two-high block and turn of a standing sign.
     */
    public static boolean same(BlockState there, BlockState template) {
        if (!SchoolRepair.same(there, template)) {
            return false;
        }
        for (Property<?> property : List.<Property<?>>of(BlockStateProperties.DOUBLE_BLOCK_HALF,
                BlockStateProperties.ROTATION_16)) {
            if (template.hasProperty(property) && !there.getValue(property).equals(template.getValue(property))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the armoury down in a tower ({@code pieces}, base first) at once (the command's and the tests' way; the
     * player's tick spreads it over ticks, {@link Job}): each block where the 3.6.1 block still stands ({@code force}:
     * over a player's block too, which drops as if broken), the stands and the frame removed where they are still
     * 3.6.1's. A tower not of the 3.6.1 design is left as it is ({@link Result#oldDesign}).
     */
    public static Result refit(ServerLevel level, List<TemplateSitePiece> pieces, boolean force) {
        Job job = new Job(pieces, force);
        while (!job.step(level, Long.MAX_VALUE)) {
            // all of it now
        }
        return job.result();
    }

    /**
     * A refit in steps (PLAN 9.6's budget of 2 ms a tick): the first step looks at the tower and takes the stands and
     * the frame away; each later step sets blocks down until its time is up ({@link #step}); the last one returns
     * true. Its result is {@link #result} then.
     */
    public static final class Job {
        private final List<TemplateSitePiece> pieces;
        private final boolean force;
        private final Set<BlockPos> blocked = new HashSet<>();
        /** The next block of the table; -1 before the first step. */
        private int next = -1;
        private boolean old;
        private int changed;
        private int already;
        private int skipped;
        private int removed;
        private int kept;

        public Job(List<TemplateSitePiece> pieces, boolean force) {
            parse();
            this.pieces = pieces;
            this.force = force;
        }

        public boolean done() {
            return old || next >= changes.size();
        }

        public Result result() {
            return old ? Result.OLD : new Result(changed, already, skipped, removed, kept, false);
        }

        /** One tick's share, stopping once {@code budgetNanos} have gone by; true when the refit is done. */
        public boolean step(ServerLevel level, long budgetNanos) {
            long start = System.nanoTime();
            if (next < 0) {
                if (!isArmouryTower(level, pieces)) {
                    old = true;
                    return true;
                }
                entities(level);
                next = 0;
            }
            while (next < changes.size()) {
                block(level, changes.get(next++));
                if (System.nanoTime() - start >= budgetNanos) {
                    break;
                }
            }
            return done();
        }

        /** The stands and the frame: a display goes only where no player's stand stays. */
        private void entities(ServerLevel level) {
            for (Stand stand : stands) {
                BlockPos cell = world(pieces, stand.model());
                for (ArmorStand found : level.getEntitiesOfClass(ArmorStand.class, new AABB(cell),
                        e -> e.blockPosition().equals(cell))) {
                    if (wearsItsArmour(found, stand.armour())) {
                        found.discard();
                        removed++;
                    } else {
                        kept++;
                        blocked.add(cell);
                        blocked.add(cell.above());
                    }
                }
            }
            for (Frame frame : frames) {
                BlockPos cell = world(pieces, frame.model());
                StructurePlaceSettings settings = pieceOf(pieces, frame.model()).placeSettings();
                Direction facing = settings.getRotation().rotate(settings.getMirror().mirror(frame.facing()));
                for (ItemFrame found : level.getEntitiesOfClass(ItemFrame.class, new AABB(cell).inflate(0.5),
                        e -> e.getPos().equals(cell))) {
                    if (found.getDirection() == facing && found.getItem().is(frame.item())) {
                        found.discard();
                        removed++;
                    } else {
                        kept++;
                    }
                }
            }
        }

        private void block(ServerLevel level, Change change) {
            BlockPos at = world(pieces, change.model());
            TemplateSitePiece piece = pieceOf(pieces, change.model());
            BlockState after = SchoolRepair.turned(piece, change.after());
            BlockState before = SchoolRepair.turned(piece, change.before());
            BlockState there = level.getBlockState(at);
            if (blocked.contains(at)) {
                skipped++;
                return;
            }
            if (change.sign() != null && change.before().getBlock() == change.after().getBlock()) {
                // the same sign with more lines: only the template's sign (the lines it gains still empty)
                switch (signText(level, at, there, after, change.sign(), force)) {
                    case 1 -> changed++;
                    case 0 -> already++;
                    default -> skipped++;
                }
                return;
            }
            if (same(there, after)) {
                already++;
                return;
            }
            if (!same(there, before) && !force) {
                skipped++;
                return;
            }
            if (!there.isAir() && !same(there, before)) {
                level.destroyBlock(at, true);
            }
            level.setBlock(at, after, Block.UPDATE_ALL);
            BlockEntity entity = level.getBlockEntity(at);
            if (change.data() != null && entity != null) {
                entity.loadWithComponents(change.data(), level.registryAccess());
                entity.setChanged();
                level.sendBlockUpdated(at, after, after, Block.UPDATE_CLIENTS);
            } else if (change.sign() != null && entity instanceof SignBlockEntity sign) {
                write(level, sign, change.sign());
            }
            changed++;
        }
    }

    private static boolean wearsItsArmour(ArmorStand stand, List<Item> armour) {
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < slots.length; i++) {
            if (!stand.getItemBySlot(slots[i]).is(armour.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * A template's sign that gains lines: 1 if they were written, 0 if it has them already, -1 if it is not the
     * template's sign (or a line it gains already holds something else; {@code force}: written over).
     */
    private static int signText(ServerLevel level, BlockPos at, BlockState there, BlockState after, Sign sign,
                                boolean force) {
        if (!same(there, after) || !(level.getBlockEntity(at) instanceof SignBlockEntity entity)) {
            return -1;
        }
        SignText text = entity.getFrontText();
        boolean has = true;
        for (int i = 0; i < 4; i++) {
            Component line = text.getMessage(i, false);
            String key = sign.keys().get(i);
            boolean right = key.isEmpty() ? line.getString().isEmpty()
                    : line.getContents() instanceof TranslatableContents t && t.getKey().equals(key);
            if (!right && !line.getString().isEmpty() && !force) {
                return -1;
            }
            has &= right;
        }
        if (has) {
            return 0;
        }
        write(level, entity, sign);
        return 1;
    }

    private static void write(ServerLevel level, SignBlockEntity entity, Sign sign) {
        SignText text = new SignText().setColor(sign.colour()).setHasGlowingText(true);
        for (int i = 0; i < 4; i++) {
            String key = sign.keys().get(i);
            text = text.setMessage(i, key.isEmpty() ? Component.empty() : Component.translatable(key));
        }
        entity.setText(text, true);
        entity.setWaxed(true);
        entity.setChanged();
        level.sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(), entity.getBlockState(),
                Block.UPDATE_CLIENTS);
    }

    private TowerRefit() {
    }
}
