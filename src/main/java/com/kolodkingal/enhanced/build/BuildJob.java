package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.build.BuildRules.Left;
import com.kolodkingal.enhanced.build.SpellPlanner.Plan;
import com.kolodkingal.enhanced.build.SpellPlanner.Setup;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModTicketTypes;
import com.kolodkingal.enhanced.ultron.SokoviaJob;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * One spell, undo or redo of Build Magic, done a slice at a time inside the level tick (D-B 11.3, 11.5, 11.7): what
 * {@link BuildScheduler} runs.
 *
 * <p><b>A spell's phases</b>, each sliced under the shared budget ({@link BuildScheduler.Tick}):
 * <ol>
 *     <li><b>SELECT</b>: the magic selection's flood from the seeds, for a selected thing (reads count a quarter of a
 *     write); a refusal ends the job ({@code TOO_BIG_TO_SELECT}, {@code TOO_WIDE}, {@code TOO_FAR}, {@code NO_BOX}).
 *     Then the plan's size is known, and the ceiling and the long hold are asked (C11, C12).</li>
 *     <li><b>PREPARE</b>: a Pull's surface; a Land pulse's columns ({@link LandBrush.Pulse}), and Billy's price of
 *     them, after which it goes straight to APPLY.</li>
 *     <li><b>CAPTURE</b>: a Copy's take reads the box into its builder's pocket (and ends); a Move or a Repeat reads its
 *     source into a pocket of its own, so overlapping cells come out exact and a Move knows what changed under it.</li>
 *     <li><b>COUNT</b> (Billy, a spell paid with items, and a Scarlet Witch's spell that makes TNT, lava or fire):
 *     the cells that will really change are counted before any block does; Billy's cost is reserved here, or the
 *     spell ends with {@code TOO_BIG} or {@code REFILLING} (or {@code VOLATILE} for a copy of TNT, lava or fire), too
 *     many volatile blocks end hers with {@code VOLATILE_CAP}, and too few items in the inventory with
 *     {@code MATERIALS}.</li>
 *     <li><b>APPLY</b>: the cells in the planner's work order, each through the rules (P1 to P11) and written as D-B
 *     11.5 says ({@link #write}); a Move moves each cell in one step, compare-and-swap (C12). With
 *     {@code requireMaterials} a conjured cell takes its item from the caster's inventory and gives him the block it
 *     replaced (D-B 5.6); when the items run out, the spell stops there. A Land pulse moves a whole column at a time
 *     ({@link #moveColumn}), or leaves it whole.</li>
 *     <li><b>SETTLE</b>: fences join, torches behave, redstone and water react, once ({@link #settle}).</li>
 *     <li><b>DONE</b>: the step is sealed into the history, Billy's unused reservation refunded and his practice
 *     booked (at his next login if he is away), the verdict and the summary line said.</li>
 * </ol>
 *
 * <p><b>An undo</b> walks its step's cells from the last to the first and puts back the old state of every cell that
 * still holds exactly what the spell put there (D-B 12.3), then the side effects; <b>a redo</b> walks forwards and puts
 * the new state back where the old one still is. Both go through the same rules (P3 replaced by that check) and settle.
 * An undo gives Billy back the cost of what it restores and takes its practice back; a redo was paid for when it was
 * asked for, and gives back what it could not do. A step paid with items trades them again: an undo takes the blocks it
 * puts back from the inventory (one it no longer holds stays away, "you no longer have them") and gives the placed ones
 * back; a redo the other way round.
 *
 * <p><b>Chunks</b> (P1): only those of the plan that were loaded when it was cast ({@link #chunks}, D-B 11.7); a cell of
 * any other is left and counted, and nothing is ever generated. The job holds region tickets of
 * {@link ModTicketTypes#ENHANCED_BUILD}, distance 1, on the chunks it reaches (at most {@code maxTicketChunks} at once,
 * a moving window in work order), and lets them all go when it ends; a chunk of its set that unloaded meanwhile is loaded
 * again by its ticket while the slice waits. The "loaded" predicate is injectable for tests, as
 * {@code SokoviaJob.paste(..., loaded)}.
 *
 * <p><b>At the start of each slice</b> a spell checks that no boss fight began over its box, and that its caster, when
 * online, is not in Adventure or Spectator mode; if either, it stops, and what it did stays one undoable step. The
 * caster in the rules is the player while he is online and alive, else a fake player of his profile, so a spell
 * outlives a logout and claim mods still see who builds.
 */
public final class BuildJob {
    /** What a job does. */
    public enum Kind {
        SPELL,
        UNDO,
        REDO
    }

    /** Where a job is (see the class comment). */
    public enum Phase {
        SELECT,
        PREPARE,
        CAPTURE,
        COUNT,
        APPLY,
        SIDE_EFFECTS,
        SETTLE,
        DONE
    }

    /** What a cell written costs of the budget (a write is 8 units: a read costs a quarter, an unchanged cell an eighth). */
    public static final int WRITE = 8;
    /** What a read costs (a cell selected, captured or counted). */
    public static final int READ = 2;
    /** What a cell that changes nothing costs. */
    public static final int SKIP = 1;
    /** How far round the work (blocks) a slice looks for bodies in its way, for a plan wider than that. */
    private static final int BODY_WINDOW = 64;
    /** Every how many ticks the progress is sent to those near. */
    private static final int PROGRESS_TICKS = 10;
    /** Every how many ticks the builder's own HUD learns the progress ({@code BuildData.progress}). */
    private static final int SYNC_TICKS = 5;
    /** The most particles a spell shows a tick, over the cells it wrote (D-B 7.2). */
    private static final int SPARKS = 12;
    /** The block-entity data no cell gets. */
    private static final long NO_HASH = 0L;
    /** The write-ahead gate: the cell's record is down; its record is on its way (the job yields); the cell changed since it was read. */
    private static final int READY = 0;
    private static final int WAIT = 1;
    private static final int CHANGED = 2;

    /** A box wider than this many chunk columns whose loaded part is under 1 % is not walked (D-B 11.7). */
    private static final long MAX_COLUMNS = 1_000_000L;
    /** The most chunks of a plan's box asked one by one at the cast; a bigger box takes the level's loaded chunks. */
    private static final int ASKED_CHUNKS = 65_536;
    private static final Method GET_CHUNKS = ObfuscationReflectionHelper.findMethod(ChunkMap.class, "getChunks");
    /** {@code HangingEntity.setDirection}, which is protected: a frame a Move turns. */
    private static final Method SET_DIRECTION = ObfuscationReflectionHelper.findMethod(HangingEntity.class, "setDirection", Direction.class);
    /** The most blocks a body in new blocks is lifted (D-B 11.8). */
    private static final int LIFT_UP = 64;
    /** The farthest a body with no room above is moved aside. */
    private static final int ASIDE = 16;

    private static int lastId;
    /** Whether a block "cares about its neighbours" (D-B 11.5), per state. */
    private static final Reference2BooleanOpenHashMap<BlockState> CARES = new Reference2BooleanOpenHashMap<>();

    private final int id;
    private final Kind kind;
    private final UUID caster;
    private final GameProfile profile;
    private final ServerLevel level;
    private final Power hero;
    private final Limits limits;
    private final int token;
    private final int flags;
    private final @Nullable SpellRecipe recipe;
    private final Vec3 eyes;
    private final UndoStep step;
    private final boolean rewind;
    private final BiPredicate<Integer, Integer> loaded;
    private final LongSet chunks;
    private final BuildRules.Cells rules;
    /** The Paint stroke a dab belongs to (its step and rules are the stroke's), or null for any other job. */
    private final BuildSession.@Nullable Stroke stroke;
    private final int window;
    /** The fights of its level that ran when it was cast (a fight that begins later over its box stops it). */
    private final Set<BossFight> fightsAtCast = Collections.newSetFromMap(new IdentityHashMap<>());

    private Setup setup;
    private @Nullable Plan plan;
    private Phase phase;
    private @Nullable MagicSelect select;
    private @Nullable Pocket.Taker taker;
    private @Nullable Pocket source;
    private @Nullable SpellPlanner.Count count;
    private @Nullable SpellPlanner.Cursor cursor;
    private @Nullable UndoStep.Walk walk;
    private int sideIndex;
    /** Its step would weigh too much in memory while it works: only its cells' bits are kept, its records on disk are its step. */
    private boolean lite;
    /** Its record would not fit in what its player may keep (D-B 12.5): it runs, held for, without a step. */
    private boolean noStep;
    /** Where its undo records are written ahead (C13), or null when the history is not written to disk. */
    private UndoFiles.@Nullable Journal journal;
    /** The second cursor that reads the cells ahead for the journal. */
    private @Nullable WriteAhead ahead;
    /** A Land pulse's cells were handed to the journal. */
    private boolean pulseRecorded;
    /** The cursor sits on a cell it could not do yet (its chunk loading, the tick's sections spent). */
    private boolean holding;
    private boolean force;
    /** Whether the spell's chunks are known (at the cast, or once its selected thing or surface is). */
    private boolean chunksKnown = true;
    /**
     * Wanda's Instant level (3.2): the most cells it may write in the tick it is cast ({@code instantMaxBlocks}), or 0 when
     * it works at the normal pace (the switch was off, or it fell back).
     */
    private int instantMax;

    private final LongOpenHashSet ticketed = new LongOpenHashSet();
    private final LongArrayFIFOQueue ticketOrder = new LongArrayFIFOQueue();
    private final LongOpenHashSet everTicketed = new LongOpenHashSet();

    /** A Move's source cells that were not moved (changed since, refused): never written over. */
    private final LongOpenHashSet kept = new LongOpenHashSet();
    /** A Move's source cells that were moved but not cleared, as its target covers them (cleared if that target fails). */
    private final LongOpenHashSet movedCovered = new LongOpenHashSet();
    /** A turned or flipped Move over itself: the source cells whose move is decided (moved or kept), else null. */
    private @Nullable LongOpenHashSet movedSources;
    /** A turned or flipped Move over itself: the target cells it did out of turn (the cursor passes them by). */
    private @Nullable LongOpenHashSet doneEarly;
    /** The creatures (and armour stands) a Move takes along, held where they stand until it is done (D-B 4.9). */
    private final List<Rider> riders = new ArrayList<>();
    /** The frames and paintings a Move takes along, by the block they hang on (they go when that block goes). */
    private final Long2ObjectOpenHashMap<List<HangingEntity>> hangers = new Long2ObjectOpenHashMap<>();

    private long volume;
    private long walked;
    private long written;
    /** The cells of the plan it changed (a Move's moved cells, not its emptied sources): what the builder is told. */
    private long changed;
    private long writtenThisTick;
    private long walkedThisTick;
    private int mostInATick;
    private int ticks;
    private boolean waiting;
    private int stopCode = -1;
    private boolean finished;
    private int endCode = -1;
    private int cost;
    private boolean mom;
    private int momId = BuildFxPayload.NO_MOM;
    /** The caster as he was at the cast: his meter and game mode are read from him while he is away (D-B 8.1). */
    private final ServerPlayer castBy;
    /** Whether its blocks are paid with items from its caster's inventory (P11, D-B 5.6). */
    private boolean paying;
    /** What the spell needs, by item, as its count finds it (P11), in the order it needs them. */
    private @Nullable Object2IntLinkedOpenHashMap<Item> needs;
    /** The cells the count found paid with items, and how many of them were paid so far. */
    private long payCells;
    private long paidCells;
    /** The item that ran out while it worked (P11): it stopped there. */
    private @Nullable Item outOf;
    /** The cells an undo could not put back: their block is no longer in its builder's inventory (D-B 12.3). */
    private long notReturned;
    private @Nullable AABB bodyBox;
    private int lowX;
    private int lowY;
    private int lowZ;
    private int highX;
    private int highY;
    private int highZ;
    private boolean solid;
    /** Some cells of the spell got a falling block (only then does the settle pass clear their falls again). */
    private boolean fallables;
    /** A few cells written this tick, for the particles. */
    private final LongArrayList sparks = new LongArrayList();
    /** A pulse of a Land stroke (a dab of one): planned in PREPARE, then written a whole column at a time. */
    private @Nullable BrushPulse pulse;
    /** The pulse's next column. */
    private int pulseColumn;
    /**
     * The cells a Land pulse wrote, in order: its settle pass walks these, not its stroke's whole step (which every
     * earlier pulse settled already).
     */
    private @Nullable LongArrayList mine;
    private int settled;

    private BuildJob(Kind kind, UUID caster, GameProfile profile, ServerLevel level, Power hero, Limits limits, int token, int flags,
                     @Nullable SpellRecipe recipe, Vec3 eyes, UndoStep step, boolean rewind, BiPredicate<Integer, Integer> loaded, LongSet chunks,
                     ServerPlayer actor, BuildSession.@Nullable Stroke stroke) {
        this.id = ++lastId;
        this.kind = kind;
        this.caster = caster;
        this.profile = profile;
        this.level = level;
        this.hero = hero;
        this.limits = limits;
        this.token = token;
        this.flags = flags;
        this.recipe = recipe;
        this.eyes = eyes;
        this.step = step;
        this.rewind = rewind;
        this.loaded = loaded;
        this.chunks = chunks;
        this.stroke = stroke;
        this.castBy = actor;
        this.rules = stroke != null ? stroke.rules : new BuildRules.Cells(level, actor);
        int configured = BuildConfig.MAX_TICKET_CHUNKS.get();
        this.window = Math.max(1, configured);
        this.setup = Setup.at(BlockPos.containing(eyes));
        for (BossFight fight : BossFights.all(level.getServer())) {
            if (fight.level() == level) {
                fightsAtCast.add(fight);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Making one

    /**
     * A spell of {@code player}: {@code recipe} clamped already, {@code setup} with its material (and pocket), and its
     * {@code plan} when it is known at the cast (null for a selected thing, whose flood is the job's first phase).
     * The chunks it may touch are those of its plan's box (or the selection's hint, and its source's for a Move or a
     * Repeat) that {@code loaded} says are loaded now.
     */
    public static BuildJob spell(ServerPlayer player, Limits limits, SpellRecipe recipe, int token, int flags, Setup setup, @Nullable Plan plan,
                                 BiPredicate<Integer, Integer> loaded) {
        UndoHistory history = UndoHistory.of(player.getUUID());
        UndoStep step = new UndoStep(history.nextId(), recipe.spell(), player.level().dimension(), player.level().getGameTime());
        BoundingBox reach = plan != null && plan.prepared() && plan.bounds() != null ? plan.bounds() : null;
        BoundingBox region = plan != null && plan.region() != null ? plan.region().bounds() : null;
        LongSet chunks = new LongOpenHashSet();
        ServerLevel level = player.serverLevel();
        // A selected thing's box and a Pull's surface are known only once flooded: their chunks are taken then
        // (known), a few ticks after the cast.
        if (reach != null) {
            loadedChunks(level, reach, loaded, chunks);
        }
        if (region != null) {
            loadedChunks(level, region, loaded, chunks);
        }
        BuildJob job = new BuildJob(Kind.SPELL, player.getUUID(), player.getGameProfile(), level, limits.hero(), limits, token, flags, recipe,
                player.getEyePosition(), step, false, loaded, chunks, player, null);
        job.setup = setup;
        job.plan = plan;
        job.chunksKnown = reach != null;
        job.phase = recipe.region() == SpellRecipe.MASK && plan == null ? Phase.SELECT : Phase.PREPARE;
        if (limits.billy() && limits.limited()) {
            net.minecraft.world.entity.player.Player mom = BillyMagic.mom(player);
            job.mom = mom != null;
            job.momId = mom != null ? mom.getId() : BuildFxPayload.NO_MOM;
        }
        return job;
    }

    /**
     * One dab of the Paint stroke {@code stroke} (D-B 11.8): a spell like any other (the same rules, the same budget),
     * but it writes into the stroke's one step with the stroke's rule book, takes no time of its own to grow (a brush
     * keeps up with the cursor), says nothing, and leaves the sealing to the stroke. A dab over a selected thing uses
     * the stroke's flood once it has one. A dab of a Land stroke is one pulse of its brush ({@link LandBrush}): its
     * chunks are those of the brush's reach.
     */
    public static BuildJob dab(ServerPlayer player, Limits limits, SpellRecipe recipe, int token, Setup setup, @Nullable Plan plan,
                               BiPredicate<Integer, Integer> loaded, BuildSession.Stroke stroke) {
        LongSet chunks = new LongOpenHashSet();
        ServerLevel level = player.serverLevel();
        boolean land = isBrush(recipe.spell());
        BoundingBox reach = land ? brushReach(recipe)
                : plan != null && plan.prepared() && plan.bounds() != null ? plan.bounds() : null;
        if (reach != null) {
            loadedChunks(level, reach, loaded, chunks);
        }
        BuildJob job = new BuildJob(Kind.SPELL, player.getUUID(), player.getGameProfile(), level, limits.hero(), limits, token, 0, recipe,
                player.getEyePosition(), stroke.step, false, loaded, chunks, player, stroke);
        job.setup = setup;
        job.plan = plan;
        job.chunksKnown = reach != null;
        job.phase = recipe.region() == SpellRecipe.MASK && plan == null ? Phase.SELECT : Phase.PREPARE;
        job.mom = stroke.mom;
        job.momId = stroke.momId;
        if (land) {
            job.mine = new LongArrayList();
        }
        return job;
    }

    /**
     * An undo ({@code redo} false) or a redo of {@code step} by its player (online now: {@code player}), in the step's
     * own level; {@code rewind}: the undo of a spell that was just stopped (Ctrl + Z on a working spell), which says
     * "Spell stopped and taken back" when it is done.
     */
    public static BuildJob replay(ServerPlayer player, Limits limits, ServerLevel level, UndoStep step, boolean redo, boolean rewind,
                                  BiPredicate<Integer, Integer> loaded) {
        // An undo ticket-loads the chunks its step wrote: they exist, the spell wrote them.
        LongSet chunks = step.chunks();
        return new BuildJob(redo ? Kind.REDO : Kind.UNDO, player.getUUID(), player.getGameProfile(), level, limits.hero(), limits, 0, 0, null,
                player.getEyePosition(), step, rewind, loaded, chunks, player, null).startReplay();
    }

    /** The same for a player who is not online (an operator's undo): his profile acts. */
    public static BuildJob replay(GameProfile profile, Limits limits, ServerLevel level, UndoStep step, boolean redo) {
        ServerPlayer actor = FakePlayerFactory.get(level, profile);
        return new BuildJob(redo ? Kind.REDO : Kind.UNDO, profile.getId(), profile, level, limits.hero(), limits, 0, 0, null, actor.position(), step,
                false, level::hasChunk, step.chunks(), actor, null).startReplay();
    }

    private BuildJob startReplay() {
        phase = Phase.APPLY;
        walk = step.walk(kind == Kind.UNDO);
        volume = Math.max(1, step.changed());
        paying = step.paid() && BuildRules.paysMaterials(online());
        return this;
    }

    /**
     * Makes this job an Instant one (Wanda's Instant level, 3.2): it is written all at once, in the tick it is cast
     * ({@link BuildScheduler#runInstant}), with no pace, no budget share and no growing ripple, if it is no more than
     * {@code max} cells; a bigger one is told so and works at the normal pace. A job whose size is known only once it
     * has flooded or surfaced its thing makes the same choice then.
     */
    public void instant(int max) {
        instantMax = Math.max(0, max);
        if (instantMax == 0) {
            return;
        }
        if (kind != Kind.SPELL) {
            tooBigForInstant(step.changed());
        } else if (plan != null && plan.prepared()) {
            tooBigForInstant(plan.size());
        }
    }

    /** Whether it is still to be written at once (Instant, not finished). */
    public boolean instant() {
        return instantMax > 0 && !finished;
    }

    /** The longest it may hold the tick: {@code instantMaxMillis}, and a third of the server's watchdog at most (nanoseconds). */
    long instantNanos() {
        long millis = BuildConfig.WANDA_INSTANT_MAX_MILLIS.get();
        if (level.getServer() instanceof net.minecraft.server.dedicated.DedicatedServer dedicated && dedicated.getMaxTickLength() > 0L) {
            millis = Math.min(millis, Math.max(100L, dedicated.getMaxTickLength() / 3L));
        }
        return millis * 1_000_000L;
    }

    /** Whether the job sits on a cell it cannot write yet (its record is on its way to the disk, its chunk is loading). */
    boolean yielded() {
        return holding || waiting;
    }

    /** It is more than Instant writes in one tick: normal speed, and its caster is told. */
    private void tooBigForInstant(long size) {
        if (instantMax > 0 && size > instantMax) {
            ServerPlayer online = online();
            if (online != null) {
                online.displayClientMessage(Component.translatable("message.enhanced.build.instant_big", size, instantMax), true);
            }
            instantMax = 0;
        }
    }

    /** The tick's time ran out in the middle of an Instant spell: what is left goes at normal speed, and its caster is told. */
    void fellBehind() {
        if (instantMax > 0) {
            ServerPlayer online = online();
            if (online != null) {
                online.displayClientMessage(Component.translatable("message.enhanced.build.instant_slow"), true);
            }
            instantMax = 0;
        }
    }

    /**
     * A redo of Billy's: what he paid for it when he asked ({@code price}, the cost of the cells it would do again,
     * halved when {@code mom} helps), settled when it ends.
     */
    public void reserved(int price, @Nullable net.minecraft.world.entity.player.Player mom) {
        this.cost = price;
        this.mom = mom != null;
        this.momId = mom != null ? mom.getId() : BuildFxPayload.NO_MOM;
    }

    /**
     * Adds to {@code into} the chunks of {@code box} that {@code loaded} says are loaded (x, z of a chunk). A box of more
     * than {@link #ASKED_CHUNKS} chunks (a valley-sized spell of the Scarlet Witch, a Repeat across the world) is not
     * asked chunk by chunk: the level's loaded chunks that lie in it are taken instead.
     */
    private static void loadedChunks(ServerLevel level, BoundingBox box, BiPredicate<Integer, Integer> loaded, LongSet into) {
        BoundingBox world = SpellMath.world(level);
        int minX = Math.max(box.minX(), world.minX()) >> 4;
        int maxX = Math.min(box.maxX(), world.maxX()) >> 4;
        int minZ = Math.max(box.minZ(), world.minZ()) >> 4;
        int maxZ = Math.min(box.maxZ(), world.maxZ()) >> 4;
        if (minX > maxX || minZ > maxZ) {
            return;
        }
        if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > ASKED_CHUNKS) {
            for (ChunkPos chunk : loadedChunks(level)) {
                if (chunk.x >= minX && chunk.x <= maxX && chunk.z >= minZ && chunk.z <= maxZ && loaded.test(chunk.x, chunk.z)) {
                    into.add(chunk.toLong());
                }
            }
            return;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (loaded.test(x, z)) {
                    into.add(ChunkPos.asLong(x, z));
                }
            }
        }
    }

    /** Every chunk the level holds now ({@code ChunkMap.getChunks}, which is not public). */
    @SuppressWarnings("unchecked")
    private static List<ChunkPos> loadedChunks(ServerLevel level) {
        List<ChunkPos> all = new ArrayList<>();
        try {
            for (ChunkHolder holder : (Iterable<ChunkHolder>) GET_CHUNKS.invoke(level.getChunkSource().chunkMap)) {
                all.add(holder.getPos());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Build Magic cannot list the loaded chunks", e);
        }
        return all;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What it is

    public int id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    /** The UUID of the player whose job it is. */
    public UUID caster() {
        return caster;
    }

    /** The profile of the player whose job it is. */
    public GameProfile profile() {
        return profile;
    }

    public ServerLevel level() {
        return level;
    }

    public Power hero() {
        return hero;
    }

    public Phase phase() {
        return phase;
    }

    /** The recipe of a spell (null for an undo or a redo). */
    public @Nullable SpellRecipe recipe() {
        return recipe;
    }

    /** Its plan (null before a selection is flooded, and for an undo or a redo). */
    public @Nullable Plan plan() {
        return plan;
    }

    /** The step it writes (a spell) or replays (an undo, a redo). */
    public UndoStep step() {
        return step;
    }

    /** The Paint stroke it is a dab of, or null. */
    public BuildSession.@Nullable Stroke stroke() {
        return stroke;
    }

    /** Whether a Scarlet Witch helps this spell of Billy's (D-B 8.3: it costs him half). */
    public boolean mom() {
        return mom;
    }

    /** What it costs (or cost) Billy: reserved at its count, what it did not use given back at its end. */
    public int cost() {
        return cost;
    }

    public boolean finished() {
        return finished;
    }

    /** How it ended ({@link BuildVerdictPayload#DONE}, a refusal...), or -1 while it works. */
    public int endCode() {
        return endCode;
    }

    /** The cells it has written (or put back), each end of a Move and the other half of a door included. */
    public long written() {
        return written;
    }

    /** The cells of its plan it changed (or put back): what the builder is told, what Billy pays and learns. */
    public long changed() {
        return changed;
    }

    /** The cells its cursor has passed. */
    public long walked() {
        return walked;
    }

    /** The most cells it wrote in one tick. */
    public int mostInATick() {
        return mostInATick;
    }

    /** The ticks it has worked. */
    public int ticks() {
        return ticks;
    }

    /** Whether its last slice ended waiting for a chunk to load. */
    public boolean waiting() {
        return waiting;
    }

    /** How many cells were left for {@code why}. */
    public long left(Left why) {
        return rules.count(why);
    }

    /** The cells it may still write: what the stop's total weighs (R55). */
    public long remaining() {
        long all;
        if (volume > 0) {
            all = volume;
        } else if (plan != null) {
            all = plan.sizeBound();
        } else if (kind == Kind.SPELL) {
            BoundingBox box = SpellMath.box(recipe.a(), recipe.b());
            all = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
        } else {
            all = step.changed();
        }
        return Math.max(0, all - walked);
    }

    /** How far it is, in thousandths. */
    public int permille() {
        if (finished) {
            return 1000;
        }
        long all = Math.max(1, volume);
        return (int) Math.min(999, walked * 1000 / all);
    }

    /** The chunks it may touch: those of its plan that were loaded when it was cast ({@link ChunkPos#toLong}). */
    public LongSet chunks() {
        return chunks;
    }

    /** The chunks it holds tickets on now. */
    public LongSet ticketed() {
        return new LongOpenHashSet(ticketed);
    }

    /** Every chunk it ever held a ticket on. */
    public LongSet everTicketed() {
        return new LongOpenHashSet(everTicketed);
    }

    /**
     * How many shares of the server's work it gets ({@link BuildScheduler}): {@code build_magic_wanda.hexBoost} for a
     * spell of the Scarlet Witch inside her own Hex, else 1.
     */
    public double weight() {
        double boost = limits.hexBoost();
        BoundingBox box = bounds();
        if (boost <= 1.0 || box == null) {
            return 1.0;
        }
        Vec3 middle = box.getCenter().getCenter();
        for (Hex hex : HexManager.data(level).hexes()) {
            if (hex.owner().equals(caster) && hex.contains(middle)) {
                return boost;
            }
        }
        return 1.0;
    }

    /**
     * The box of what it changes (the plan's, a Land pulse's columns or its brush's reach, the step's, or the box the
     * recipe names), or null.
     */
    public @Nullable BoundingBox bounds() {
        if (kind != Kind.SPELL) {
            return step.bounds();
        }
        if (plan != null && plan.prepared() && plan.bounds() != null) {
            return plan.bounds();
        }
        if (isBrush(recipe.spell())) {
            BoundingBox columns = pulse != null && pulse.done() ? pulse.bounds() : null;
            return columns != null ? columns : brushReach(recipe);
        }
        return recipe.region() == SpellRecipe.NO_REGION ? new BoundingBox(recipe.anchor()) : SpellMath.box(recipe.a(), recipe.b());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Stopping

    /**
     * Stops the job after the cell it is on ({@code code}: {@link BuildVerdictPayload#STOPPED_FIGHT}, or
     * {@link BuildVerdictPayload#STOPPED}): what it wrote is settled and sealed as one undoable step at its next slice.
     */
    public void stop(int code) {
        if (stopCode < 0 && !finished) {
            stopCode = code;
        }
    }

    /**
     * Ctrl + Z on a working spell (D-B 12.2): it stops at once, after the cell it is on, its partial step goes into the
     * history, and the undo of that step is returned (the caller queues it first). Null when it changed nothing yet.
     */
    public @Nullable BuildJob stopAndRewind(ServerPlayer player) {
        stopCode = BuildVerdictPayload.STOPPED;
        releaseTickets();
        finished = true;
        endCode = BuildVerdictPayload.STOPPED;
        if (step.changed() == 0) {
            fx(BuildFxPayload.STOPPED);
            endJournal();
            return null;
        }
        bookBilly();
        step.setState(UndoStep.State.DONE);
        if (!keepsStep()) {
            // Nothing was kept to take it back with: it stops where it is.
            UndoFiles.discard(UndoHistory.of(caster), step);
            fx(BuildFxPayload.STOPPED);
            endJournal();
            return null;
        }
        UndoHistory history = UndoHistory.of(caster);
        history.push(step, limits.undoSteps());
        endJournal();
        if (!step.loaded() && !UndoFiles.load(history, step)) {
            fx(BuildFxPayload.STOPPED);
            return null; // (a lite step reads back from its records: they could not be read)
        }
        return replay(player, limits, level, step, false, true, loaded);
    }

    /**
     * The job ends where it is (the server is stopping and it is not to be finished, R55; an operator stops it): what
     * a spell did is sealed as an interrupted step (undoable), an undo stays partly undone, and {@code line} (when not
     * null, with the undo key) is said to the player at his next login.
     */
    public void interrupt(@Nullable String line) {
        if (finished) {
            return;
        }
        releaseTickets();
        finished = true;
        endCode = BuildVerdictPayload.STOPPED;
        if (stroke != null) {
            // A dab: what it painted stays in its stroke, which is sealed now.
            settleDab();
            stroke.closing = true;
            dabEnded();
            return;
        }
        // Billy pays for what it did, and no more (his reservation, a redo's too); before the step is sealed, as its file
        // holds his cost.
        if (kind == Kind.SPELL) {
            bookBilly();
        } else if (kind == Kind.REDO) {
            billRedo();
        } else {
            refundUndo();
        }
        if (kind == Kind.SPELL && step.changed() > 0 && !keepsStep()) {
            step.setState(UndoStep.State.DONE); // (nothing was kept to take it back with)
            UndoFiles.discard(UndoHistory.of(caster), step);
        } else if (kind == Kind.SPELL && step.changed() > 0) {
            step.setState(UndoStep.State.INTERRUPTED);
            step.setTell(line != null);
            UndoHistory.of(caster).push(step, limits.undoSteps());
            if (line != null) {
                BuildSession.of(caster).tellAtLogin(Component.translatable(line, Component.keybind("key.enhanced.build_undo")));
            }
        } else if (kind == Kind.UNDO && step.applied() > 0 && step.applied() < step.changed()) {
            step.setState(UndoStep.State.PARTLY);
            UndoFiles.sealed(UndoHistory.of(caster), step);
        } else if (kind != Kind.SPELL) {
            UndoFiles.sealed(UndoHistory.of(caster), step); // (what it did of the step is on disk)
        }
        endJournal();
    }

    /**
     * The job threw while it worked ({@link BuildScheduler} catches it): it ends where it is as an interrupted step, so what
     * it did can be undone, its tickets and records are let go, and its caster is told. Whatever goes wrong while it ends
     * (the same cell again) only ends the job.
     */
    public void fail(RuntimeException cause) {
        Enhanced.LOGGER.error("Build Magic: the {} of {} (job {}) failed and was stopped", kind.name().toLowerCase(java.util.Locale.ROOT), profile.getName(), id,
                cause);
        try {
            interrupt(null);
        } catch (RuntimeException again) {
            Enhanced.LOGGER.error("Build Magic: job {} could not be ended cleanly", id, again);
        }
        releaseTickets();
        finished = true;
        if (endCode < 0) {
            endCode = BuildVerdictPayload.STOPPED;
        }
        ServerPlayer online = online();
        if (online != null) {
            online.displayClientMessage(Component.translatable("message.enhanced.build.stopped_mode"), true);
        }
        try {
            BuildMagic.sync(level.getServer(), caster);
        } catch (RuntimeException ignored) {
            // (the caster's HUD catches up with the next sync)
        }
    }

    /** The job is dropped (it never started, or the server forgets it): its tickets go, nothing else happens. */
    public void drop() {
        releaseTickets();
        endJournal();
        boolean was = finished;
        finished = true;
        if (endCode < 0) {
            endCode = BuildVerdictPayload.DROPPED;
        }
        if (!was && stroke != null) {
            dabEnded();
        }
    }

    /** From now on the job loads the chunks of its set it needs instead of waiting for them (the server's stop). */
    public void force() {
        force = true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Working

    /**
     * One slice: work until {@code units} of the tick's budget are used (a write is {@link #WRITE}), the tick's time or
     * sections run out, the job's pace for this tick ({@code ceil(volume / minSpellTicks)} cells) is reached, or it
     * waits for a chunk; then the creatures in new blocks are lifted.
     *
     * @return the units it used
     */
    public long step(BuildScheduler.Tick tick, long units) {
        if (finished) {
            return 0;
        }
        ticks++;
        waiting = false;
        writtenThisTick = 0;
        walkedThisTick = 0;
        solid = false;
        Slice slice = new Slice(tick, Math.max(0, units));
        ServerPlayer actor = actor();
        rules.actAs(actor);
        if (kind == Kind.SPELL && stopCode < 0 && phase.ordinal() <= Phase.APPLY.ordinal()) {
            checkStillAllowed();
        }
        if (ahead != null && phase == Phase.APPLY && stopCode < 0) {
            slice.use(ahead.scan(walked, instantMax > 0 ? WriteAhead.WINDOW : WriteAhead.PER_SLICE) * (long) SKIP);
        }
        while (!finished && !slice.spent()) {
            boolean more = switch (phase) {
                case SELECT -> select(slice);
                case PREPARE -> prepare(slice);
                case CAPTURE -> capture(slice);
                case COUNT -> count(slice);
                case APPLY -> kind != Kind.SPELL ? replay(slice) : pulse != null ? applyPulse(slice) : apply(slice);
                case SIDE_EFFECTS -> sideEffects(slice);
                case SETTLE -> settle(slice);
                case DONE -> {
                    finish();
                    yield false;
                }
            };
            if (!more) {
                break;
            }
        }
        if (solid) {
            liftCreatures();
        }
        sparkle(tick);
        mostInATick = (int) Math.max(mostInATick, writtenThisTick);
        // (An Instant spell steps again and again inside one tick: it sends no progress and hums once, at its cast.)
        boolean paced = instantMax == 0;
        if (!finished && paced && ticks % PROGRESS_TICKS == 0) {
            fx(BuildFxPayload.PROGRESS);
        }
        if (!finished && paced && ticks % SYNC_TICKS == 0) {
            BuildMagic.sync(level.getServer(), caster);
        }
        if (!finished && paced && ticks % 20 == 0 && kind == Kind.SPELL && phase == Phase.APPLY && cursor != null) {
            humAlong(Vec3.atCenterOf(cursor.pos()));
        }
        return slice.used();
    }

    /**
     * The hum of the working spell (D-B 7.3): at its front, for anyone near it, and at the caster, who hears his own spell
     * weave wherever its front is (a fill a hundred blocks wide is not silent for the one who cast it).
     */
    private void humAlong(Vec3 front) {
        ServerPlayer online = online();
        if (online == null || online.level() != level) {
            BuildSounds.hum(level, front, hero);
            return;
        }
        Vec3 caster = online.position();
        BuildSounds.hum(level, caster, hero);
        if (front.distanceToSqr(caster) > BuildSounds.HEARD * BuildSounds.HEARD) {
            BuildSounds.hum(level, front, hero);
        }
    }

    /**
     * At the start of a spell's slice (D-B 11.7): a fight began over its box since it was cast (one that ran then is
     * P5's, cell by cell), or its online caster may no longer build.
     */
    private void checkStillAllowed() {
        BoundingBox box = bounds();
        if (box != null && fightBeganOver(box)) {
            stop(BuildVerdictPayload.STOPPED_FIGHT);
        } else {
            ServerPlayer online = online();
            if (online != null) {
                GameType mode = online.gameMode.getGameModeForPlayer();
                if (mode == GameType.ADVENTURE || mode == GameType.SPECTATOR) {
                    stop(BuildVerdictPayload.STOPPED);
                }
            }
        }
        if (stopCode >= 0) {
            phase = phase == Phase.APPLY ? Phase.SETTLE : Phase.DONE;
            walk = step.walk(false);
        }
    }

    /** Whether a fight of this level that did not run when the spell was cast has its arena over {@code box}. */
    private boolean fightBeganOver(BoundingBox box) {
        for (BossFight fight : BossFights.all(level.getServer())) {
            if (fight.level() == level && !fightsAtCast.contains(fight) && meets(fight.arena(), box)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the arena's cylinder (round its centre, between its bottom and top) reaches the box. */
    private static boolean meets(com.kolodkingal.enhanced.boss.BossArena arena, BoundingBox box) {
        if (box.maxY() + 1.0 < arena.bottom() || box.minY() > arena.top() + 1.0) {
            return false;
        }
        Vec3 center = arena.center();
        double dx = Math.max(Math.max(box.minX() - center.x, 0.0), center.x - (box.maxX() + 1.0));
        double dz = Math.max(Math.max(box.minZ() - center.z, 0.0), center.z - (box.maxZ() + 1.0));
        return dx * dx + dz * dz <= arena.radius() * arena.radius();
    }

    /** The caster while he is online and alive; else a fake player of his profile (10.3). */
    private ServerPlayer actor() {
        ServerPlayer online = online();
        return online != null && online.isAlive() ? online : FakePlayerFactory.get(level, profile);
    }

    private @Nullable ServerPlayer online() {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(caster);
        return player != null && !player.hasDisconnected() ? player : null;
    }

    // ---- SELECT, PREPARE, CAPTURE, COUNT ----

    private boolean select(Slice slice) {
        SpellMath.Volume mask = stroke != null ? stroke.mask : null;
        if (mask == null) {
            if (select == null) {
                select = limits.select(level, recipe.seeds(), eyes, true);
            }
            int reads = slice.reads();
            boolean done = select.step(reads);
            slice.use((long) reads * READ);
            if (!done) {
                return false;
            }
            if (select.refusal() != SpellPlanner.NONE) {
                refuse(select.refusal(), select.limit(), select.message(!limits.billy()));
                return false;
            }
            mask = select.mask();
            if (stroke != null) {
                stroke.mask = mask; // (the stroke's next dabs paint the same thing without flooding it again)
            }
        }
        setup = setup.withMask(mask);
        plan = SpellPlanner.plan(level, recipe, setup);
        select = null;
        if (plan.refusal() != SpellPlanner.NONE) {
            refuse(plan.refusal(), 0, BuildRules.message(plan.refusal(), 0));
            return false;
        }
        phase = Phase.PREPARE;
        return true;
    }

    private boolean prepare(Slice slice) {
        if (isBrush(recipe.spell()) && stroke != null) {
            return preparePulse(slice);
        }
        if (!plan.prepared()) {
            int reads = slice.reads();
            slice.use((long) reads * READ);
            if (!plan.prepare(reads)) {
                return false;
            }
        }
        // Now the box of a selected thing or a Pull's surface is known: its chunks loaded now are the spell's.
        if (!chunksKnown) {
            chunksKnown = true;
            if (plan.bounds() != null) {
                loadedChunks(level, plan.bounds(), loaded, chunks);
            }
            if (plan.region() != null) {
                loadedChunks(level, plan.region().bounds(), loaded, chunks);
            }
        }
        // And its size: the ceiling and the long hold (C11, C12) for a selected thing or a surface.
        long size = plan.size();
        if (limits.maxBlocks() > 0 && size > limits.maxBlocks()) {
            refuse(BuildVerdictPayload.CEILING, limits.maxBlocks(), BuildRules.message(BuildVerdictPayload.CEILING, limits.maxBlocks()));
            return false;
        }
        if (size > BuildConfig.HUGE_SPELL_BLOCKS.get() && (flags & BuildCastPayload.HELD_LONG) == 0) {
            refuse(BuildVerdictPayload.HOLD, size, BuildRules.message(BuildVerdictPayload.HOLD, size));
            return false;
        }
        if (stroke == null && plan.writes()) {
            UndoCaps.Room room = UndoCaps.room(recipe.spell(), size, plan.bounds());
            if (room == UndoCaps.Room.NONE) {
                // "Too big to remember: this one can't be undone. Hold to cast it anyway": it then runs without a step. (A Move
                // needs its cells whole to be safe, so it is refused whatever is held.)
                if (recipe.spell() == Spell.MOVE || (flags & BuildCastPayload.HELD_LONG) == 0) {
                    refuse(BuildVerdictPayload.NO_UNDO_ROOM, size, BuildRules.message(BuildVerdictPayload.NO_UNDO_ROOM, size));
                    return false;
                }
                noStep = true;
            } else if (room == UndoCaps.Room.LITE) {
                lite = true;
            }
        }
        if (needsCapture()) {
            phase = Phase.CAPTURE;
        } else {
            startCount();
        }
        return true;
    }

    /**
     * A Land pulse (D-B 4.11): its columns planned against the world as it is now, a few reads at a time, the stroke's
     * memory of each column taken from; then Wanda's ceiling and Billy's price of the cells it changes (C11, C13), and
     * straight to APPLY. Land conjures no TNT, lava or fire, and is never paid with items: it moves the ground.
     */
    private boolean preparePulse(Slice slice) {
        if (pulse == null) {
            pulse = recipe.spell() == Spell.GROW ? new GrowBrush.Pulse(level, recipe.growStyle(), recipe.anchor(), recipe.size(), recipe.seed())
                    : new LandBrush.Pulse(level, recipe.landStyle(), recipe.anchor(), recipe.size(), stroke.land());
        }
        long before = pulse.reads();
        boolean done = pulse.step(slice.reads());
        slice.use((pulse.reads() - before) * READ);
        if (!done) {
            return false;
        }
        long changes = pulse.changes();
        if (changes == 0 && recipe.spell() == Spell.GROW && GrowBrush.saysNothingGrows(recipe.growStyle()) && !stroke.saidNothingGrows) {
            stroke.saidNothingGrows = true; // ("Nothing grows here": once a stroke)
            ServerPlayer online = online();
            if (online != null) {
                online.displayClientMessage(Component.translatable("message.enhanced.build.nothing_grows"), true);
            }
        }
        if (limits.maxBlocks() > 0 && changes > limits.maxBlocks()) {
            refuse(BuildVerdictPayload.CEILING, limits.maxBlocks(), BuildRules.message(BuildVerdictPayload.CEILING, limits.maxBlocks()));
            return false;
        }
        if (!reserveBilly(changes)) {
            return false;
        }
        volume = Math.max(1, pulse.cells());
        phase = Phase.APPLY;
        return true;
    }

    /** Whether the spell is a brush that works in pulses (Land and Grow, D-B 4.11). */
    static boolean isBrush(Spell spell) {
        return spell == Spell.LAND || spell == Spell.GROW;
    }

    /** The box a pulse of the brush recipe may read or write. */
    static BoundingBox brushReach(SpellRecipe recipe) {
        return recipe.spell() == Spell.GROW ? GrowBrush.reach(recipe.anchor(), recipe.size(), recipe.growStyle())
                : LandBrush.reach(recipe.anchor(), recipe.size(), recipe.landStyle());
    }

    /** The sections of the area a Copy or a Repeat reads, by whether its caster may break there (the break event asked once). */
    private final it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap mayRead = new it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap();

    /**
     * Whether a Copy or a Repeat may keep what the container at {@code pos} holds: only where its caster may break (no
     * claim, arena, Hex or spawn protection refuses; the break event is asked once for the section). Else the pocket gets the
     * bare block: a claim's vault is not copied.
     */
    private boolean mayReadContents(BlockPos pos, BlockState state) {
        ServerPlayer actor = rules.player();
        if (BlockRights.massEditRefuses(actor, level, pos) || !BlockRights.vanillaRights(actor, level, pos)) {
            return false;
        }
        long key = SectionPos.asLong(pos);
        if (!mayRead.containsKey(key)) {
            mayRead.put(key, BlockRights.postBreak(actor, level, pos, state));
        }
        return mayRead.get(key);
    }

    /** Copy's take, a Move's put-down and a Repeat read their source first. */
    private boolean needsCapture() {
        return switch (recipe.spell()) {
            case COPY -> recipe.op() == SpellRecipe.Op.TAKE;
            case MOVE -> recipe.op() == SpellRecipe.Op.PUT_DOWN;
            case REPEAT -> true;
            default -> false;
        };
    }

    private boolean capture(Slice slice) {
        if (taker == null) {
            SpellMath.Volume region = plan.region();
            if (region == null) {
                refuse(BuildVerdictPayload.NO_BOX, 0, BuildRules.message(BuildVerdictPayload.NO_BOX, 0));
                return false;
            }
            Pocket.Contents contents = recipe.spell() == Spell.MOVE ? Pocket.Contents.ALL : Pocket.Contents.of(limits);
            taker = Pocket.take(level, region, contents, recipe.spell() == Spell.COPY ? Pocket.nextId() : 0,
                    (x, z) -> chunks.contains(ChunkPos.asLong(x, z)) && loaded.test(x, z));
            if (recipe.spell() != Spell.MOVE) {
                taker.reading(this::mayReadContents); // (a Move needs the right to break what it empties: P8, P9, P10)
            }
            if (recipe.spell() == Spell.MOVE && BuildConfig.MOVE_CARRIES_CREATURES.get()) {
                board(region);
            }
        }
        int reads = slice.reads();
        boolean done = taker.step(reads);
        slice.use((long) reads * READ);
        if (taker.bytes() > (long) BuildConfig.POCKET_MAX_MB.get() << 20) {
            // Too big to keep in her pocket (D-B 4.8): the pocket she holds stays as it was.
            taker = null;
            refuse(BuildVerdictPayload.POCKET_TOO_BIG, BuildConfig.POCKET_MAX_MB.get(), BuildRules.message(BuildVerdictPayload.POCKET_TOO_BIG, 0));
            return false;
        }
        if (!done) {
            return false;
        }
        source = taker.pocket();
        taker = null;
        if (recipe.spell() == Spell.COPY) {
            Pocket.hold(caster, source);
            ServerPlayer online = online();
            if (online != null) {
                BuildMagic.pocketTaken(online, source);
                send(online, new BuildVerdictPayload(token, BuildVerdictPayload.COPIED, 0, 0, (int) Math.min(Integer.MAX_VALUE, source.cells()), 0));
                online.displayClientMessage(Component.translatable("message.enhanced.build.copied", source.sizeX(), source.sizeY(), source.sizeZ()), true);
            }
            end(BuildVerdictPayload.COPIED);
            return false;
        }
        setup = setup.withSource(source);
        plan = SpellPlanner.plan(level, recipe, setup);
        startCount();
        return true;
    }

    /**
     * COUNT when the spell needs it (Billy's cost, the items of a spell paid with them, Wanda's volatile blocks), else
     * straight to APPLY. A spell pays with items ({@code requireMaterials}, P11) when it conjures its blocks (all but a
     * Move) and its caster is in Survival; one who has gone away pays with what he left behind: nothing.
     */
    private void startCount() {
        ServerPlayer online = online();
        paying = recipe.spell() != Spell.MOVE && BuildRules.paysMaterials(online != null ? online : castBy);
        if (paying) {
            step.setPaid(true);
        }
        if (limits.limited() || paying || (limits.volatilePerSpell() > 0 && makesVolatile())) {
            count = plan.count(level);
            if (paying) {
                needs = new Object2IntLinkedOpenHashMap<>();
                count.tally(this::tally);
            }
            phase = Phase.COUNT;
        } else {
            startApply();
        }
    }

    /** A cell the count found changing, in a spell paid with items: what its new block needs. */
    private void tally(BlockState old, BlockState now) {
        int n = BuildRules.itemsIn(now);
        if (n > 0) {
            needs.addTo(BuildRules.itemOf(now), n);
            payCells++;
        }
    }

    /** Whether the spell's material may be TNT, lava or fire (only then does the Scarlet Witch's spell count first). */
    private boolean makesVolatile() {
        if (setup.material() == null) {
            return recipe.spell() == Spell.COPY || recipe.spell() == Spell.REPEAT;
        }
        BlockPos at = recipe.anchor();
        return setup.material().at(at.getX(), at.getY(), at.getZ()).is(com.kolodkingal.enhanced.registry.ModTags.BUILD_VOLATILE) || recipe.mix();
    }

    private boolean count(Slice slice) {
        int reads = slice.reads();
        boolean done = count.step(reads);
        slice.use((long) reads * READ);
        if (!done) {
            return false;
        }
        long volatiles = count.volatiles();
        if (volatiles > 0 && !limits.volatileAllowed() && recipe.spell() != Spell.MOVE) {
            // Billy's copy or row of TNT, lava or fire: never made by his magic (C8, D-B 8.4).
            refuse(BuildVerdictPayload.VOLATILE, 0, 0, 0, BillyMagic.tooDangerous());
            return false;
        }
        if (!limits.limited() && volatiles > limits.volatilePerSpell()) {
            // "Too much TNT, lava or fire in one spell: 27 of 10" (C13): how many it makes, and her cap.
            refuse(BuildVerdictPayload.VOLATILE_CAP, volatiles, 0, limits.volatilePerSpell(), Component.translatable(
                    "message.enhanced.build.volatile_cap", volatiles, limits.volatilePerSpell()));
            return false;
        }
        ServerPlayer online = online();
        if (paying) {
            // Whole or nothing (D-B 5.6): "You need 2 more Stone", the first item short in the spell's own order.
            for (var need : needs.object2IntEntrySet()) {
                int have = online == null ? 0 : BuildRules.plainCount(online, need.getKey());
                if (have < need.getIntValue()) {
                    refuse(BuildVerdictPayload.MATERIALS, need.getIntValue(), 0, need.getIntValue() - have,
                            BuildRules.needMore(need.getIntValue() - have, need.getKey()));
                    return false;
                }
            }
            needs = null;
        }
        if (!reserveBilly(count.changed())) {
            return false;
        }
        count = null;
        startApply();
        return true;
    }

    /**
     * Billy's cost of {@code cells} changing (C13), reserved now; he gets back at the end what was left unchanged. One
     * who has gone away is asked with the meter he left with, and pays at his next login. False, the spell refused
     * ({@code TOO_BIG}, {@code REFILLING}), when he cannot pay it; true for anyone else.
     */
    private boolean reserveBilly(long cells) {
        if (!limits.limited() || cells <= 0) {
            return true;
        }
        ServerPlayer online = online();
        BuildData data = (online != null ? online : castBy).getData(com.kolodkingal.enhanced.registry.ModAttachments.BUILD_DATA);
        long time = level.getServer().overworld().getGameTime();
        int price = BillyMagic.cost(cells, mom);
        int verdict = BillyMagic.verdict(data, time, price);
        if (verdict != SpellPlanner.NONE) {
            refuse(verdict, cells, price, BuildMagic.cannotPayExtra(data, time, price, verdict), BillyMagic.cannotPay(data, time, price));
            return false;
        }
        cost = price;
        if (online != null) {
            online.setData(com.kolodkingal.enhanced.registry.ModAttachments.BUILD_DATA, BillyMagic.pay(data, time, price));
        } else {
            BuildSession.of(caster).owe(price, 0);
        }
        return true;
    }

    private void startApply() {
        if (!plan.writes()) {
            phase = Phase.DONE;
            return;
        }
        volume = Math.max(1, plan.size());
        tooBigForInstant(volume);
        cursor = plan.cursor();
        phase = Phase.APPLY;
        step.setFxOrder(plan.order());
        if (lite || noStep) {
            step.makeLite();
        }
        BoundingBox all = plan.bounds();
        if (kind == Kind.SPELL && stroke == null && chunksKnown && all != null) {
            long columns = (long) ((all.maxX() >> 4) - (all.minX() >> 4) + 1) * ((all.maxZ() >> 4) - (all.minZ() >> 4) + 1);
            if (chunks.isEmpty() || columns > MAX_COLUMNS && (long) chunks.size() * 100L < columns) {
                // Nothing of it is loaded (or so little of a box so wide that walking its columns would be the whole job): it
                // ends at once, every cell counted as too far away.
                rules.left(Left.UNLOADED, plan.size());
                phase = Phase.DONE;
                return;
            }
        }
        journal = noStep ? null : UndoFiles.journal(UndoHistory.of(caster), step);
        if (journal != null) {
            ahead = new WriteAhead(level, plan, journal, loaded, chunks, recipe.spell() == Spell.MOVE && source != null);
            ahead.scan(0, UndoFiles.FIRST_BATCH); // (the first batch is on its way before the first cell is asked for)
        }
        if (recipe.spell() == Spell.MOVE && source != null && plan.turnsOverItself()) {
            movedSources = new LongOpenHashSet();
            doneEarly = new LongOpenHashSet();
        }
    }

    // ---- APPLY (a spell) ----

    private boolean apply(Slice slice) {
        BoundingBox bounds = plan.bounds();
        findBodies(bounds, cursor.walked() > 0 ? cursor.pos() : BlockPos.containing(eyes));
        long pace = pace(slice.tick);
        boolean moving = recipe.spell() == Spell.MOVE && source != null;
        boolean conjured = recipe.spell() != Spell.MOVE;
        holdRiders();
        while (true) {
            if (stopCode >= 0) {
                riders.clear(); // (stopped: what it took along stays where it stood)
                if (!clearMovedCovered()) {
                    return false;
                }
                toSettle();
                return true;
            }
            if (slice.spent()) {
                return false;
            }
            if (!holding && !cursor.next()) {
                carryRiders();
                toSettle();
                return true;
            }
            if (walkedThisTick >= pace) {
                holding = true; // the tick's pace is reached: this cell is the next tick's
                return false;
            }
            holding = false;
            BlockPos pos = cursor.pos();
            if (bodyBox != null && !bodyBox.contains(Vec3.atCenterOf(pos))) {
                findBodies(bounds, pos);
            }
            // P1: a chunk loaded when the spell was cast, and loaded now (or the slice waits for it).
            int ready = chunkReady(pos);
            if (ready < 0) {
                holding = true;
                return false;
            }
            walked++;
            walkedThisTick++;
            if (ready == 0) {
                long skipped = cursor.skipColumn();
                if (skipped > 0) {
                    // A whole chunk column that is not loaded goes at once, counted by the cells of its box.
                    rules.left(Left.UNLOADED, skipped);
                    walked += skipped;
                    slice.use(SKIP);
                    continue;
                }
                rules.left(Left.UNLOADED);
                slice.use(SKIP);
                continue;
            }
            // P2: inside the build height and the world border.
            if (SpellMath.outside(level, pos)) {
                rules.left(Left.OUTSIDE);
                slice.use(SKIP);
                continue;
            }
            if (doneEarly != null) {
                // A turned Move over itself: a cell done out of turn is passed by; a source cell standing here moves
                // before it is written over.
                if (doneEarly.remove(pos.asLong())) {
                    slice.use(SKIP);
                    continue;
                }
                if (moveSourcesFirst(pos.immutable()) < 0) {
                    holding = true;
                    walked--;
                    walkedThisTick--;
                    return false;
                }
            }
            BlockState old = level.getBlockState(pos);
            BlockState now = cursor.resolve(level);
            CompoundTag data = cursor.data();
            // P3: nothing changes, nothing is paid. (A Move's cell whose target already holds the block still moves:
            // its source is emptied.)
            if (now == null || !moving && now.equals(old) && data == null) {
                slice.use(SKIP);
                continue;
            }
            if (journal != null) {
                // C13: a cell is written only when its record is on disk; until then the slice yields.
                int gated = gate(pos, old, now, data, moving ? cursor.from() : null);
                if (gated == WAIT) {
                    holding = true;
                    walked--;
                    walkedThisTick--;
                    return false;
                }
                if (gated == CHANGED) {
                    rules.left(Left.CHANGED); // (someone changed it since it was read ahead: it stays as they made it)
                    slice.use(SKIP);
                    continue;
                }
            }
            if (!slice.section(level, pos)) {
                // The tick's new sections are spent: this cell waits for the next tick.
                holding = true;
                walked--;
                walkedThisTick--;
                return false;
            }
            if (moving) {
                int moved = move(pos.immutable(), old, now, data, cursor.from());
                if (moved < 0) {
                    holding = true;
                    walked--;
                    walkedThisTick--;
                    return false;
                }
                slice.use(moved > 0 ? 2L * WRITE : SKIP);
                continue;
            }
            if (writeChecked(pos.immutable(), old, now, data, conjured)) {
                slice.use(WRITE);
            } else {
                slice.use(SKIP);
                if (outOf != null) {
                    stop(BuildVerdictPayload.DONE_PARTLY); // (P11: the items ran out, the spell stops here)
                }
            }
        }
    }

    // ---- Writing ahead (C13) ----

    /**
     * The write-ahead gate for the cell about to be written at {@code pos} (it holds {@code old} and gets {@code now}):
     * {@link #READY} when its record is down, {@link #WAIT} while it is on its way (it is recorded now when the read-ahead
     * never saw it), {@link #CHANGED} when it holds something else than when it was read ahead. The other half of a
     * two-cell block and a Move's source are covered the same way.
     */
    private int gate(BlockPos pos, BlockState old, BlockState now, @Nullable CompoundTag data, @Nullable BlockPos from) {
        if (journal.failed()) {
            return READY;
        }
        int result = cover(pos, old, now, data, true);
        if (result != READY) {
            return result;
        }
        BlockPos other = SpellMath.partner(old, pos);
        if (other != null && !plan.covers(other.getX(), other.getY(), other.getZ()) && SpellMath.loaded(level, other) && !SpellMath.outside(level, other)) {
            BlockState otherOld = level.getBlockState(other);
            if (otherOld.getBlock() == old.getBlock()) {
                result = cover(other, otherOld, otherHalf(now, old, otherOld), null, false);
                if (result != READY) {
                    return result;
                }
            }
        }
        if (from != null && SpellMath.loaded(level, from) && !SpellMath.outside(level, from) && !plan.covers(from.getX(), from.getY(), from.getZ())) {
            BlockState srcNow = level.getBlockState(from);
            BlockState emptied = SpellMath.vanished(srcNow);
            if (!srcNow.equals(emptied)) {
                result = cover(from, srcNow, emptied, null, false);
            }
        }
        return result;
    }

    /**
     * Whether the record of {@code pos} (holding {@code cur}, to get {@code planned}) is down: a cell the step has
     * recorded already is; one the read-ahead recorded is when its batch is on disk, and, with {@code compare}, still
     * holds what it held then; one it never saw is recorded now.
     */
    private int cover(BlockPos pos, BlockState cur, BlockState planned, @Nullable CompoundTag data, boolean compare) {
        if (step.has(pos)) {
            return READY;
        }
        long key = pos.asLong();
        UndoFiles.Entry entry = journal.entry(key);
        if (entry == null) {
            journal.add(key, cur, WriteAhead.oldData(level, pos, cur), planned, data == null ? null : data.copy());
            journal.flush();
            return journal.failed() ? READY : WAIT;
        }
        if (!journal.durable(entry)) {
            journal.flush();
            return WAIT;
        }
        if (compare && !entry.old().equals(cur)) {
            journal.release(key);
            return CHANGED;
        }
        return READY;
    }

    /** The journal of this job's step (made at the first need): where a Land pulse's records go. */
    private UndoFiles.@Nullable Journal pulseJournal() {
        if (journal == null) {
            journal = UndoFiles.journal(UndoHistory.of(caster), step);
        }
        return journal;
    }

    /** Hands every cell of a Land pulse to the journal, once: each column is then written when its records are down. */
    private void recordPulse() {
        if (pulseRecorded || pulseJournal() == null) {
            return;
        }
        pulseRecorded = true;
        for (LandBrush.Column column : pulse.columns()) {
            for (int i = 0; i < column.size(); i++) {
                if (column.changes(i)) {
                    journal.add(column.pos(i).asLong(), column.olds()[i], null, column.news()[i], null);
                }
            }
        }
        journal.flush();
    }

    // ---- APPLY (a Land pulse) ----

    /**
     * Writes a Land pulse's columns in their order (nearest the brush's middle first), each whole or not at all
     * ({@link #moveColumn}); the creatures standing on a rising column are lifted after the slice.
     */
    private boolean applyPulse(Slice slice) {
        List<LandBrush.Column> columns = pulse.columns();
        recordPulse();
        findBodies(pulse.bounds(), recipe.anchor());
        while (true) {
            if (stopCode >= 0 || pulseColumn >= columns.size()) {
                toSettle();
                return true;
            }
            if (slice.spent()) {
                return false;
            }
            LandBrush.Column column = columns.get(pulseColumn);
            if (moveColumn(column, slice) < 0) {
                holding = true; // (its chunk is loading, or the tick's sections are spent: the next tick's)
                return false;
            }
            holding = false;
            pulseColumn++;
            walked += column.size();
            walkedThisTick += column.size();
        }
    }

    /**
     * One column of a Land pulse, whole or not at all, so the ground never tears: P1 and P2 for the column, each of its
     * cells still as the pulse found it (else someone changed it since, and it is left), P4 to P8 for every cell that
     * changes, then the claims (P9, P10), then the writes. A rule that leaves one cell leaves the column, every cell it
     * would have changed counted for that rule.
     *
     * @return the cells written, or -1 when the column must wait for the next tick (its chunk loading, the tick's new
     * sections spent)
     */
    private int moveColumn(LandBrush.Column column, Slice slice) {
        int changes = column.changes();
        int ready = chunkReady(column.pos(0));
        if (ready < 0) {
            return -1;
        }
        if (ready == 0) {
            leave(Left.UNLOADED, changes);
            slice.use(SKIP);
            return 0;
        }
        BlockPos[] cells = new BlockPos[column.size()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = column.pos(i);
            if (SpellMath.outside(level, cells[i])) {
                leave(Left.OUTSIDE, changes);
                slice.use(SKIP);
                return 0;
            }
        }
        for (int i = 0; i < cells.length; i++) {
            if (!level.getBlockState(cells[i]).equals(column.olds()[i])) {
                slice.use(SKIP);
                return 0;
            }
        }
        if (journal != null && !journal.failed()) {
            boolean waiting = false;
            for (int i = 0; i < cells.length; i++) {
                if (column.changes(i) && cover(cells[i], column.olds()[i], column.news()[i], null, false) == WAIT) {
                    waiting = true;
                }
            }
            if (waiting) {
                return -1; // (its records are on their way)
            }
        }
        for (int i = 0; i < cells.length; i++) {
            if (column.changes(i) && !slice.section(level, cells[i])) {
                return -1;
            }
        }
        for (int i = 0; i < cells.length; i++) {
            if (!column.changes(i)) {
                continue;
            }
            Left why = rules.check(cells[i], column.olds()[i], column.news()[i], i == column.conjured(), false);
            if (why != null) {
                leave(why, changes);
                slice.use(SKIP);
                return 0;
            }
        }
        for (int i = 0; i < cells.length; i++) {
            if (column.changes(i) && !rules.claims(cells[i], column.olds()[i], column.news()[i])) {
                leave(Left.PROTECTED, changes);
                slice.use(SKIP);
                return 0;
            }
        }
        for (int i = 0; i < cells.length; i++) {
            if (column.changes(i)) {
                write(cells[i], column.olds()[i], column.news()[i], null, false);
                mine.add(cells[i].asLong());
                changed++;
            }
        }
        slice.use((long) changes * WRITE);
        return changes;
    }

    /** Counts {@code cells} cells left for {@code why}. */
    private void leave(Left why, int cells) {
        for (int i = 0; i < cells; i++) {
            rules.left(why);
        }
    }

    /**
     * The cells a tick of this job may walk: {@code ceil(volume / minSpellTicks)} (D-B 11.3), so a spell is seen to
     * grow; a dab of a stroke as many as the budget lets it.
     */
    private long pace(BuildScheduler.Tick tick) {
        if (stroke != null || instantMax > 0) {
            return Long.MAX_VALUE; // a brush keeps up with the cursor (its stroke is what grows); an Instant spell has no pace
        }
        long ticksAtLeast = Math.max(1, tick.minSpellTicks());
        return Math.max(1, (volume + ticksAtLeast - 1) / ticksAtLeast);
    }

    /**
     * P1 for the cell: 1 when its chunk is in the set and loaded (a ticket now holds it), 0 when it is not in the set
     * (left, counted), -1 when it is in the set but not loaded now (the slice waits; its ticket loads it).
     */
    private int chunkReady(BlockPos pos) {
        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());
        long chunk = ChunkPos.asLong(cx, cz);
        if (!chunks.contains(chunk)) {
            return 0;
        }
        ticket(chunk);
        if (!loaded.test(cx, cz)) {
            if (force) {
                level.getChunk(cx, cz);
                return 1;
            }
            waiting = true;
            return -1;
        }
        return 1;
    }

    /**
     * Finds the bodies in the way of the cells of {@code bounds} (P6), once a slice: all of them for a plan up to
     * {@code 2 x BODY_WINDOW} blocks wide, else those within {@link #BODY_WINDOW} blocks of {@code at} (the slice moves
     * on to a fresh look when its cursor leaves that window).
     */
    private void findBodies(@Nullable BoundingBox bounds, BlockPos at) {
        if (bounds == null) {
            return;
        }
        AABB all = AABB.of(bounds).inflate(1.0);
        if (bounds.getXSpan() <= 2 * BODY_WINDOW && bounds.getZSpan() <= 2 * BODY_WINDOW) {
            bodyBox = null;
            rules.findBodies(all, online());
            return;
        }
        AABB near = new AABB(at).inflate(BODY_WINDOW, 0.0, BODY_WINDOW);
        bodyBox = all.intersect(new AABB(near.minX, all.minY, near.minZ, near.maxX, all.maxY, near.maxZ));
        rules.findBodies(bodyBox, online());
    }

    /**
     * The rules (P4 to P11) and the write of one cell of a spell; false when a rule left it. A block of two cells
     * whose other half is outside the spell is never cut in half: that half is written in the same step. A spell paid
     * with items takes the cell's from its caster's inventory before the claims are asked (P11: when none is left the
     * spell stops here), and gives him the blocks it replaced.
     */
    private boolean writeChecked(BlockPos pos, BlockState old, BlockState now, @Nullable CompoundTag data, boolean conjured) {
        Left why = rules.check(pos, old, now, conjured, false);
        if (why != null) {
            rules.left(why);
            return false;
        }
        boolean spill = rules.spills();
        Partner partner = partner(pos, old, now, conjured);
        if (partner == Partner.REFUSED) {
            return false;
        }
        if (paying && !pay(now, partner != null ? partner.now : null)) {
            return false;
        }
        if (!rules.claims(pos, old, now)) {
            if (paying) {
                giveItems(pos, now); // (not placed: its items go back)
                if (partner != null) {
                    giveItems(partner.pos, partner.now);
                }
            }
            rules.left(Left.PROTECTED);
            return false;
        }
        if (partner != null) {
            partner.write(this);
        }
        write(pos, old, now, data, spill);
        if (paying && !spill) { // (a container that spilled gave its own item with its contents)
            giveItems(pos, old);
            if (partner != null) {
                giveItems(partner.pos, partner.old);
            }
        }
        changed++;
        return true;
    }

    // ---- Materials (P11, D-B 5.6) ----

    /**
     * P11: the items the cell's new block (and the other half of its block) is made of, taken from the caster's
     * inventory. False, with nothing taken, when he has too few (or has gone away): the spell stops here.
     */
    private boolean pay(BlockState now, @Nullable BlockState partnerNow) {
        if (!takeItems(now)) {
            return false;
        }
        if (partnerNow != null && !takeItems(partnerNow)) {
            giveItems(BlockPos.containing(eyes), now);
            return false;
        }
        if (BuildRules.itemsIn(now) > 0) {
            paidCells++;
        }
        return true;
    }

    /** Takes what {@code state} is made of from the caster's inventory; false, the item noted as run out, if he lacks it. */
    private boolean takeItems(BlockState state) {
        int n = BuildRules.itemsIn(state);
        if (n == 0) {
            return true;
        }
        Item item = BuildRules.itemOf(state);
        ServerPlayer online = online();
        if (online != null && BuildRules.takePlain(online, item, n)) {
            return true;
        }
        outOf = item;
        return false;
    }

    /**
     * Gives the caster what {@code state} is made of (a block his spell vanished or changed away, D-B 5.6): into his
     * inventory, or at his feet when it is full; dropped at {@code pos} while he is away.
     */
    private void giveItems(BlockPos pos, BlockState state) {
        int n = BuildRules.itemsIn(state);
        if (n == 0) {
            return;
        }
        Item item = BuildRules.itemOf(state);
        ServerPlayer online = online();
        if (online != null) {
            BuildRules.give(online, item, n);
        } else {
            Block.popResource(level, pos, new ItemStack(item, n));
        }
    }

    /** The other half of a two-cell block a cell holds, when the spell does not cover it: what happens to it. */
    private static final class Partner {
        static final Partner REFUSED = new Partner(BlockPos.ZERO, null, null);
        final BlockPos pos;
        final BlockState old;
        final BlockState now;

        Partner(BlockPos pos, BlockState old, BlockState now) {
            this.pos = pos;
            this.old = old;
            this.now = now;
        }

        void write(BuildJob job) {
            job.write(pos, old, now, null, false);
        }
    }

    /**
     * The partner of {@code old} at {@code pos} (D-B 11.5, step 3): null when it has none or the spell writes it in its
     * own turn; else the partner's write, after its rules, or {@link Partner#REFUSED} when they leave it (and then the
     * cell is left too).
     */
    private @Nullable Partner partner(BlockPos pos, BlockState old, BlockState now, boolean conjured) {
        BlockPos other = SpellMath.partner(old, pos);
        if (other == null || plan != null && plan.covers(other.getX(), other.getY(), other.getZ())) {
            return null;
        }
        if (!SpellMath.loaded(level, other) || SpellMath.outside(level, other)) {
            return null;
        }
        BlockState otherOld = level.getBlockState(other);
        if (otherOld.getBlock() != old.getBlock()) {
            return null; // (already half a block: nothing to keep whole)
        }
        BlockState otherNow = otherHalf(now, old, otherOld);
        if (otherNow.equals(otherOld)) {
            return null;
        }
        Left why = rules.check(other, otherOld, otherNow, conjured, false);
        if (why != null || !rules.claims(other, otherOld, otherNow)) {
            rules.left(why != null ? why : Left.PROTECTED);
            return Partner.REFUSED;
        }
        return new Partner(other, otherOld, otherNow);
    }

    /** What the other half becomes: the same block's other half when the cell stays that two-cell block, else it goes. */
    private static BlockState otherHalf(BlockState now, BlockState old, BlockState otherOld) {
        if (now.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && old.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = otherOld.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            return now.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, half);
        }
        if (now.hasProperty(BlockStateProperties.BED_PART) && old.hasProperty(BlockStateProperties.BED_PART)) {
            BedPart part = otherOld.getValue(BlockStateProperties.BED_PART);
            return now.setValue(BlockStateProperties.BED_PART, part);
        }
        return SpellMath.vanished(otherOld);
    }

    /**
     * One cell of a Move, in one step (C12, R49): the source cell is compared with its capture (its state, and the
     * hash of its block entity's data), or, if this Move already wrote it as a target, with what it wrote. If it
     * changed, nothing moves: the target stays as it is, the source as someone made it ({@code kept_changed}).
     * Otherwise the target gets the captured copy and, in the same step, the source is emptied unless the target
     * covers it (the Move writes it in its own turn). A target refused by a rule keeps its source block where it was.
     *
     * @return 1 moved, 0 left, -1 waiting for the source's chunk
     */
    private int move(BlockPos pos, BlockState old, BlockState now, @Nullable CompoundTag data, @Nullable BlockPos from) {
        if (from == null) {
            return writeChecked(pos, old, now, data, false) ? 1 : 0;
        }
        BlockPos src = from.immutable();
        int ready = chunkReady(src);
        if (ready < 0) {
            return -1;
        }
        long srcKey = src.asLong();
        if (movedSources != null) {
            movedSources.add(srcKey); // (its move is decided now, whatever comes of it)
        }
        if (ready == 0 || SpellMath.outside(level, src)) {
            kept.add(srcKey);
            rules.left(Left.UNLOADED);
            return failed(pos, old);
        }
        BlockPos local = src.subtract(source.origin());
        BlockState srcNow = level.getBlockState(src);
        boolean wrote = step.has(src);
        BlockState expected = wrote ? step.newState(src) : source.state(local.getX(), local.getY(), local.getZ());
        long expectedHash = wrote ? step.newHash(src) : source.hash(local.getX(), local.getY(), local.getZ());
        if (!srcNow.equals(expected) || hashOf(src, srcNow) != expectedHash) {
            // Someone took, broke, pulled or changed it since the capture: it does not move (C12).
            kept.add(srcKey);
            rules.left(Left.CHANGED);
            return failed(pos, old);
        }
        boolean writeTarget = !now.equals(old) || data != null;
        BlockState emptied = SpellMath.vanished(srcNow);
        boolean covered = plan.covers(src.getX(), src.getY(), src.getZ());
        boolean clearSource = !covered && !srcNow.equals(emptied);
        if (writeTarget && kept.contains(pos.asLong())) {
            // The target is a source cell that could not move: what it holds stays, so this cell does not move either.
            kept.add(srcKey);
            rules.left(Left.CHANGED);
            return 0;
        }
        // P7: the contents of the cells of the Move's own source were captured and are moved; a chest the Move lands on from
        // outside is protected as from a Fill (kept, or spilled).
        boolean carriedTarget = plan.region() != null && plan.region().contains(pos.getX(), pos.getY(), pos.getZ());
        Left why = writeTarget ? rules.check(pos, old, now, false, carriedTarget) : null;
        boolean spillTarget = writeTarget && why == null && rules.spills();
        if (why == null && clearSource) {
            why = rules.check(src, srcNow, emptied, false, true);
        }
        if (why == null && (writeTarget && !rules.claims(pos, old, now) || clearSource && !rules.claims(src, srcNow, emptied))) {
            why = Left.PROTECTED;
        }
        if (why != null) {
            kept.add(srcKey);
            rules.left(why);
            return failed(pos, old);
        }
        // The one step: the source emptied (its block entity cleared first, so what it holds moves and never drops),
        // the target given the captured copy.
        if (clearSource) {
            write(src, srcNow, emptied, null, false);
        } else if (covered && !srcNow.equals(emptied)) {
            movedCovered.add(srcKey);
        }
        // Whatever the target held of a source moved earlier is accounted for: it now holds this cell's copy.
        movedCovered.remove(pos.asLong());
        if (writeTarget) {
            write(pos, old, now, data, spillTarget);
            if (clearSource && data != null && !src.equals(pos)) {
                step.link(src, pos); // (the contents are in one of the two places: see the replay)
            }
        }
        // What hangs on the source's block goes with it.
        carryHangers(src);
        if (writeTarget || clearSource) {
            changed++;
            return 1;
        }
        return 0;
    }

    /**
     * A turned or flipped Move over itself: before {@code target} is written over, the source cell that stands there
     * moves to its own target first if it has not moved yet, and before that one is written over, the source standing
     * there, and so on along the chain, so that no source is written over before its own move is decided (a source
     * whose target is refused stays where it is, and so does whatever was to land on it). A chain that comes back on
     * itself (a house turned where it stands) is a cycle through {@code target} no cell of which can move first: its
     * moves are checked together before any is made (the rules that ask nobody, and that nothing changed since the
     * capture), and if one is refused, the whole cycle stays as it is; else it moves, the cursor's own move last, from
     * the Move's capture.
     *
     * @return 0, or -1 when a cell of the chain waits for its chunk (the chain goes on from there next tick)
     */
    private int moveSourcesFirst(BlockPos target) {
        SpellMath.Volume region = plan.region();
        List<BlockPos> chain = new ArrayList<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        seen.add(target.asLong());
        BlockPos at = target;
        int cycle = -1;
        while (region.contains(at.getX(), at.getY(), at.getZ()) && !movedSources.contains(at.asLong())) {
            BlockPos next = plan.targetOf(at);
            if (next == null || next.equals(at)) {
                break; // (a cell that lands on itself moves in its own turn)
            }
            chain.add(at);
            if (!seen.add(next.asLong())) {
                cycle = chain.indexOf(next); // the chain closed on itself
                break;
            }
            at = next;
        }
        if (cycle >= 0) {
            int kept = keepCycleIfRefused(chain.subList(cycle, chain.size()), target);
            if (kept < 0) {
                return -1;
            }
            if (kept > 0) {
                chain = chain.subList(0, cycle);
            }
        }
        for (int i = chain.size() - 1; i >= 0; i--) {
            BlockPos from = chain.get(i);
            BlockPos to = plan.targetOf(from);
            if (to.equals(target)) {
                continue; // (the move that closes a cycle onto the cursor's own cell is the cursor's, right after)
            }
            int ready = chunkReady(to);
            if (ready < 0) {
                return -1;
            }
            if (ready == 0 || SpellMath.outside(level, to)) {
                // Its target can never be written: it stays, and so does whatever was to land on it.
                movedSources.add(from.asLong());
                this.kept.add(from.asLong());
                rules.left(ready == 0 ? Left.UNLOADED : Left.OUTSIDE);
                doneEarly.add(to.asLong());
                continue;
            }
            SpellPlanner.Cell cell = plan.resolveCell(level, to);
            if (cell == null || cell.from() == null) {
                movedSources.add(from.asLong()); // (nothing of it was captured: it is no part of the Move)
                doneEarly.add(to.asLong());
                continue;
            }
            BlockState toNow = level.getBlockState(to);
            if (journal != null && !journal.failed() && (cover(to, toNow, cell.state(), cell.data(), false) == WAIT
                    || cell.from() != null && SpellMath.loaded(level, cell.from()) && !plan.covers(cell.from().getX(), cell.from().getY(), cell.from().getZ())
                    && cover(cell.from(), level.getBlockState(cell.from()), SpellMath.vanished(level.getBlockState(cell.from())), null, false) == WAIT)) {
                return -1; // (the records of this move are on their way)
            }
            if (move(to, toNow, cell.state(), cell.data(), cell.from()) < 0) {
                return -1;
            }
            doneEarly.add(to.asLong());
        }
        return 0;
    }

    /**
     * Checks every move of a cycle of a turned Move over itself before any is made: each source as it was captured,
     * each target within the rules that ask nobody (P4 to P8). If one fails, every cell of the cycle stays as it is
     * (counted as left for that reason once, the rest as changed under the Move), and the targets other than
     * {@code cursor}'s are passed by.
     *
     * @return 1 when the cycle stays, 0 when it may move, -1 when a cell waits for its chunk
     */
    private int keepCycleIfRefused(List<BlockPos> cycle, BlockPos cursor) {
        Left why = null;
        for (BlockPos from : cycle) {
            BlockPos to = plan.targetOf(from);
            int readyTo = chunkReady(to);
            int readyFrom = chunkReady(from);
            if (readyTo < 0 || readyFrom < 0) {
                return -1;
            }
            if (readyTo == 0 || readyFrom == 0) {
                why = Left.UNLOADED;
                break;
            }
            if (SpellMath.outside(level, to)) {
                why = Left.OUTSIDE;
                break;
            }
            SpellPlanner.Cell cell = plan.resolveCell(level, to);
            if (cell == null) {
                continue; // (nothing of it was captured: nothing lands there from it)
            }
            BlockPos local = from.subtract(source.origin());
            BlockState now = level.getBlockState(from);
            if (!now.equals(source.state(local.getX(), local.getY(), local.getZ())) || hashOf(from, now) != source.hash(local.getX(), local.getY(),
                    local.getZ())) {
                why = Left.CHANGED;
                break;
            }
            BlockState old = level.getBlockState(to);
            if (!cell.state().equals(old) || cell.data() != null) {
                why = rules.check(to, old, cell.state(), false, true);
                if (why != null) {
                    break;
                }
            }
        }
        if (why == null) {
            return 0;
        }
        rules.left(why);
        boolean counted = false;
        for (BlockPos from : cycle) {
            movedSources.add(from.asLong());
            this.kept.add(from.asLong());
            BlockPos to = plan.targetOf(from);
            if (to.equals(cursor)) {
                continue; // (the cursor's own move finds its target kept, and counts itself)
            }
            doneEarly.add(to.asLong());
            if (counted) {
                rules.left(Left.CHANGED);
            }
            counted = true;
        }
        return 1;
    }

    /**
     * A Move that stops half way leaves no block twice: each source cell it moved but did not clear (its target was to
     * cover it) is emptied now, by the rules of a refused target. False while a record for such a cell is on its way.
     */
    private boolean clearMovedCovered() {
        if (movedCovered.isEmpty()) {
            return true;
        }
        for (long key : movedCovered.toLongArray()) {
            BlockPos pos = BlockPos.of(key);
            if (!SpellMath.loaded(level, pos) || SpellMath.outside(level, pos)) {
                movedCovered.remove(key);
                continue;
            }
            BlockState here = level.getBlockState(pos);
            if (journal != null && !journal.failed() && cover(pos, here, SpellMath.vanished(here), null, false) == WAIT) {
                return false;
            }
            failed(pos, here);
        }
        return true;
    }

    /**
     * A Move's target {@code pos} was not written: if it is a source cell already moved (its block is at its new place),
     * it is emptied now, so nothing is there twice.
     */
    private int failed(BlockPos pos, BlockState old) {
        if (movedCovered.remove(pos.asLong())) {
            BlockState emptied = SpellMath.vanished(old);
            if (rules.check(pos, old, emptied, false, true) == null && rules.claims(pos, old, emptied)) {
                write(pos, old, emptied, null, false);
            }
        }
        return 0;
    }

    /** The hash of the block entity's data at {@code pos} ({@code state} is what stands there), or 0 for none. */
    private long hashOf(BlockPos pos, BlockState state) {
        if (!state.hasBlockEntity()) {
            return NO_HASH;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        return entity == null ? NO_HASH : UndoStep.hash(entity.saveWithoutMetadata(level.registryAccess()));
    }

    // ---- Writing (D-B 11.5) ----

    /** Writes a cell of the spell and records it in the step. */
    private void write(BlockPos pos, BlockState old, BlockState now, @Nullable CompoundTag data, boolean spill) {
        CompoundTag oldData = null;
        if (old.hasBlockEntity()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity != null) {
                if (spill) {
                    BuildRules.spill(level, pos, entity);
                    rules.spilled();
                }
                // Recorded after the spill: an undo never brings back items that are lying on the ground.
                oldData = entity.saveWithoutMetadata(level.registryAccess());
                Clearable.tryClear(entity);
            }
        }
        long hash = place(pos, now, data);
        step.record(pos, old, oldData, now, data == null ? null : data.copy(), hash);
        if (journal != null) {
            journal.release(pos.asLong());
        }
        written++;
        writtenThisTick++;
    }

    /**
     * Sets the block (clients told, no neighbour bothered, nothing dropped), loads its data, and holds a falling block
     * where magic put it (D-B 11.5 steps 4 to 6).
     *
     * @return the hash of the new block entity's data as it stands (0 for none)
     */
    private long place(BlockPos pos, BlockState now, @Nullable CompoundTag data) {
        level.setBlock(pos, now, SokoviaJob.FLAGS);
        long hash = NO_HASH;
        if (now.hasBlockEntity()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity != null) {
                if (data != null) {
                    entity.loadWithComponents(data.copy(), level.registryAccess());
                    entity.setChanged();
                    level.sendBlockUpdated(pos, now, now, Block.UPDATE_CLIENTS);
                }
                hash = UndoStep.hash(entity.saveWithoutMetadata(level.registryAccess()));
            }
        }
        hold(pos, now);
        if (!now.getCollisionShape(level, pos).isEmpty()) {
            grow(pos);
        }
        if (sparks.size() < SPARKS && (writtenThisTick & 7) == 0) {
            sparks.add(pos.asLong());
        }
        return hash;
    }

    /**
     * The particles of the tick (D-B 7.2): at most {@value #SPARKS} over the cells the job wrote, in the hero's colour,
     * within the tick's {@link BuildScheduler.Tick#sparks} for all spells.
     */
    private void sparkle(BuildScheduler.Tick tick) {
        if (sparks.isEmpty()) {
            return;
        }
        int colour = hero == Power.BILLY ? Power.BILLY.color : Power.SCARLET_WITCH.color;
        for (int i = 0; i < sparks.size() && tick.spark(); i++) {
            BlockPos pos = BlockPos.of(sparks.getLong(i));
            level.sendParticles(new EnergyParticleOptions(colour, 1.0F), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.02);
        }
        sparks.clear();
    }

    /** Magic holds what it places (D-B 11.5 step 6): a falling block's scheduled fall is cleared at once. */
    private void hold(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof Fallable && BuildConfig.HOLD_GRAVITY_BLOCKS.get()) {
            fallables = true;
            level.getBlockTicks().clearArea(new BoundingBox(pos));
        }
    }

    /** Notes a cell that became solid this slice (the creatures there are lifted after it). */
    private void grow(BlockPos pos) {
        if (!solid) {
            solid = true;
            lowX = highX = pos.getX();
            lowY = highY = pos.getY();
            lowZ = highZ = pos.getZ();
            return;
        }
        lowX = Math.min(lowX, pos.getX());
        lowY = Math.min(lowY, pos.getY());
        lowZ = Math.min(lowZ, pos.getZ());
        highX = Math.max(highX, pos.getX());
        highY = Math.max(highY, pos.getY());
        highZ = Math.max(highZ, pos.getZ());
    }

    /**
     * Creatures are lifted out of new blocks (D-B 11.8): every living body that may be lifted and is now inside
     * blocks goes up to the first place with room for it, at most 64 blocks up, unhurt.
     */
    private void liftCreatures() {
        if (!BuildConfig.LIFT_CREATURES.get()) {
            return;
        }
        AABB box = new AABB(lowX, lowY, lowZ, highX + 1, highY + 1, highZ + 1).inflate(0.5);
        ServerPlayer online = online();
        for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (!rules.liftable(body, online) || level.noCollision(body, body.getBoundingBox().deflate(1.0E-4))) {
                continue;
            }
            if (!lift(body)) {
                pushAside(body);
            }
        }
    }

    /**
     * Lifts a body to the first place above it with room for it, at most {@value #LIFT_UP} blocks up; false if none.
     */
    private boolean lift(LivingEntity body) {
        for (int up = 1; up <= LIFT_UP; up++) {
            AABB lifted = body.getBoundingBox().move(0.0, up, 0.0);
            if (level.noCollision(body, lifted)) {
                body.teleportTo(body.getX(), body.getY() + up, body.getZ());
                body.resetFallDistance();
                return true;
            }
        }
        return false;
    }

    /**
     * Out to the nearest free side (D-B 11.8), when there is no room above: the nearest place at its height, at most
     * {@value #ASIDE} blocks away, where it fits (the first of equal ones, in a fixed order).
     */
    private void pushAside(LivingEntity body) {
        AABB from = body.getBoundingBox();
        for (int r = 1; r <= ASIDE; r++) {
            int best = -1;
            int bestX = 0;
            int bestZ = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int distance = dx * dx + dz * dz;
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r || best >= 0 && distance >= best) {
                        continue;
                    }
                    if (level.noCollision(body, from.move(dx, 0.0, dz))) {
                        best = distance;
                        bestX = dx;
                        bestZ = dz;
                    }
                }
            }
            if (best >= 0) {
                body.teleportTo(body.getX() + bestX, body.getY(), body.getZ() + bestZ);
                body.resetFallDistance();
                return;
            }
        }
    }

    // ---- What a Move takes along (D-B 4.9, moveCarriesCreatures) ----

    /** A creature a Move takes along, and where it stood when the Move began. */
    private record Rider(LivingEntity body, Vec3 at) {
    }

    /**
     * What a Move takes along, read with its source: every living body wholly inside its source box (and over the
     * selected thing, for a thing) but a player, anything of a boss fight, anything telekinesis holds, a creature
     * Mjolnir pins (C2), and anything ridden or riding; and the frames and paintings hanging on a block of the source.
     */
    private void board(SpellMath.Volume region) {
        BoundingBox box = region.bounds();
        AABB inside = AABB.of(box);
        for (Entity entity : level.getEntities((Entity) null, inside.inflate(1.0), Entity::isAlive)) {
            if (entity instanceof HangingEntity hanging) {
                BlockPos on = hanging.getPos().relative(hanging.getDirection().getOpposite());
                if (region.contains(on.getX(), on.getY(), on.getZ())) {
                    hangers.computeIfAbsent(on.asLong(), key -> new ArrayList<>()).add(hanging);
                }
            } else if (entity instanceof LivingEntity body && rides(body) && within(inside, body.getBoundingBox())
                    && (recipe.region() != SpellRecipe.MASK || over(region, body.blockPosition()))) {
                riders.add(new Rider(body, body.position()));
            }
        }
    }

    /** Whether a Move takes {@code body} along. */
    private static boolean rides(LivingEntity body) {
        return !(body instanceof net.minecraft.world.entity.player.Player) && !BossFights.isFightEntity(body) && !Telekinesis.isHeld(body)
                && !MjolnirPin.isPinned(body) && !body.isPassenger() && !body.isVehicle();
    }

    /** Whether the box {@code inner} is wholly inside {@code outer} (a hair's breadth allowed). */
    private static boolean within(AABB outer, AABB inner) {
        return outer.inflate(1.0E-4).contains(inner.minX, inner.minY, inner.minZ) && outer.inflate(1.0E-4).contains(inner.maxX, inner.maxY, inner.maxZ);
    }

    /** Whether a body at {@code feet} stands in or on the selected thing. */
    private static boolean over(SpellMath.Volume region, BlockPos feet) {
        return region.contains(feet.getX(), feet.getY(), feet.getZ()) || region.contains(feet.getX(), feet.getY() - 1, feet.getZ());
    }

    /**
     * The creatures a Move takes along stay where they stood until it is done (the floor under them may go before
     * they do), each slice; one that telekinesis took, Mjolnir pinned, or that died or left is let go.
     */
    private void holdRiders() {
        for (Iterator<Rider> it = riders.iterator(); it.hasNext(); ) {
            Rider rider = it.next();
            LivingEntity body = rider.body();
            if (!body.isAlive() || body.level() != level || Telekinesis.isHeld(body) || MjolnirPin.isPinned(body)) {
                it.remove();
                continue;
            }
            if (body.position().distanceToSqr(rider.at()) > 1.0E-6) {
                body.teleportTo(rider.at().x, rider.at().y, rider.at().z);
            }
            body.setDeltaMovement(Vec3.ZERO);
            body.resetFallDistance();
        }
    }

    /**
     * The Move is done: each creature it takes along that is still in its source box (C12) goes where its cells
     * went, turned as they were, unhurt (a Move turned upside down keeps it on its feet).
     */
    private void carryRiders() {
        if (riders.isEmpty()) {
            return;
        }
        AABB inside = AABB.of(plan.region().bounds());
        for (Rider rider : riders) {
            LivingEntity body = rider.body();
            if (!body.isAlive() || body.level() != level || Telekinesis.isHeld(body) || !within(inside, body.getBoundingBox())) {
                continue;
            }
            // Only a creature whose cell (or the one under it) the Move really moved, where its caster may build, and where
            // there is room for it, goes along: a Move whose blocks were refused or kept leaves the animals where they stand.
            BlockPos at = body.blockPosition();
            if (kept.contains(at.asLong()) || kept.contains(at.below().asLong())) {
                continue; // (the cell it stands in, or on, stayed: a claim refused it, or someone changed it)
            }
            ServerPlayer actor = rules.player();
            if (BlockRights.massEditRefuses(actor, level, at) || !BlockRights.vanillaRights(actor, level, at)) {
                continue;
            }
            Vec3 feet = body.position();
            Vec3 to = plan.carried(feet);
            Vec3 head = plan.carried(feet.add(0.0, body.getBbHeight(), 0.0));
            if (to == null || head == null) {
                continue;
            }
            if (!level.noCollision(body, body.getBoundingBox().move(to.x - feet.x, Math.min(to.y, head.y) - feet.y, to.z - feet.z))) {
                continue;
            }
            float yaw = plan.carriedYaw(body.getYRot());
            body.moveTo(to.x, Math.min(to.y, head.y), to.z, yaw, body.getXRot());
            body.setYHeadRot(plan.carriedYaw(body.getYHeadRot()));
            body.setYBodyRot(yaw);
            body.setDeltaMovement(Vec3.ZERO);
            body.resetFallDistance();
        }
        riders.clear();
    }

    /** What hangs on the source cell {@code src} goes with its block, turned as it was, and the step remembers it. */
    private void carryHangers(BlockPos src) {
        List<HangingEntity> on = hangers.isEmpty() ? null : hangers.remove(src.asLong());
        if (on == null) {
            return;
        }
        for (HangingEntity hanging : on) {
            if (hanging.isRemoved() || hanging.level() != level) {
                continue;
            }
            BlockPos from = hanging.getPos();
            Direction facing = hanging.getDirection();
            Vec3 middle = plan.carried(Vec3.atCenterOf(from));
            if (middle == null) {
                continue;
            }
            BlockPos to = BlockPos.containing(middle);
            Direction turned = plan.carried(facing);
            hang(hanging, to, turned);
            step.carried(hanging.getUUID(), from, facing, to, turned);
        }
    }

    /**
     * An undo hangs a frame or a painting the Move carried back where it was, once the block it hung on stands there
     * again ({@code undo}); a redo carries it again once its new block is there. Only if it still hangs where it was
     * put (D-B 12.3).
     */
    private void hangBack(UndoStep.Carried thing, boolean undo) {
        if (!(level.getEntity(thing.id()) instanceof HangingEntity hanging) || hanging.isRemoved()) {
            return;
        }
        BlockPos at = undo ? thing.to() : thing.from();
        Direction facing = undo ? thing.toFacing() : thing.fromFacing();
        if (hanging.getPos().equals(at) && hanging.getDirection() == facing) {
            hang(hanging, undo ? thing.from() : thing.to(), undo ? thing.fromFacing() : thing.toFacing());
        }
    }

    /**
     * Hangs a frame or a painting in the cell {@code at}, facing {@code facing}. It sends no moves to those who see
     * it, so they are sent it again where it now hangs.
     */
    private static void hang(HangingEntity hanging, BlockPos at, Direction facing) {
        hanging.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        if (hanging.getDirection() != facing) {
            try {
                SET_DIRECTION.invoke(hanging, facing);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Build Magic cannot turn a frame", e);
            }
        }
        ServerChunkCache chunks = ((ServerLevel) hanging.level()).getChunkSource();
        chunks.removeEntity(hanging);
        chunks.addEntity(hanging);
    }

    // ---- APPLY (an undo, a redo) ----

    /** Puts a step back (undo, backwards) or does it again (redo, forwards), cell by cell (D-B 12.3). */
    private boolean replay(Slice slice) {
        boolean undo = kind == Kind.UNDO;
        findBodies(step.bounds(), walk.pos());
        long pace = pace(slice.tick);
        while (true) {
            if (slice.spent()) {
                return false;
            }
            if (!holding && !walk.next()) {
                phase = Phase.SIDE_EFFECTS;
                sideIndex = undo ? step.sideEffects().size() - 1 : 0;
                return true;
            }
            if (walkedThisTick >= pace) {
                holding = true;
                return false;
            }
            holding = false;
            byte want = undo ? UndoStep.APPLIED : UndoStep.UNDONE;
            if (walk.status() != want) {
                continue;
            }
            BlockPos pos = walk.pos();
            int ready = chunkReady(pos);
            if (ready < 0) {
                holding = true;
                return false;
            }
            walked++;
            walkedThisTick++;
            if (ready == 0 || SpellMath.outside(level, pos)) {
                rules.left(ready == 0 ? Left.UNLOADED : Left.OUTSIDE);
                slice.use(SKIP);
                continue;
            }
            BlockState here = level.getBlockState(pos);
            BlockState expected = undo ? walk.newState() : walk.oldState();
            long expectedHash = undo ? walk.newHash() : NO_HASH;
            // Only a cell that still holds exactly what the spell put there (or, for a redo, what it found); the cells
            // of a spell a crash cut short are known by their block only (the records hold what it was to put there).
            boolean holds = undo && step.recovered() ? step.recoveredHolds(pos, here, hashOf(pos, here))
                    : here.equals(expected) && (!undo || hashOf(pos, here) == expectedHash);
            if (holds && !undo && here.hasBlockEntity()) {
                // A redo does not wipe what was put into a container since the undo (D-B 12.3): the cell must hold what the
                // undo put back (its hash), or, for a block put back bare, nothing in it.
                long put = walk.restoredHash();
                holds = put != 0L ? hashOf(pos, here) == put : !BuildRules.holdsItems(level, pos, level.getBlockEntity(pos));
            }
            if (!holds) {
                walk.setStatus(UndoStep.DROPPED);
                if (!(undo && step.recovered() && here.equals(walk.oldState()))) {
                    rules.left(Left.CHANGED); // (a recorded cell the spell never reached holds what it found: nothing to say)
                }
                slice.use(SKIP);
                continue;
            }
            BlockState target = undo ? walk.oldState() : walk.newState();
            CompoundTag data = undo ? walk.oldData() : walk.newData();
            if (data != null) {
                // What a Move took from a container to another cell comes back to only one of them: an undo gives the
                // source its contents only if the target was undone, a redo writes the target's only if the source was
                // cleared again; else the block comes without them.
                BlockPos other = undo ? step.linkedTarget(pos) : step.linkedSource(pos);
                if (other != null && step.status(other) != (undo ? UndoStep.UNDONE : UndoStep.APPLIED)) {
                    data = null;
                }
            }
            if (!slice.section(level, pos)) {
                holding = true;
                walked--;
                walkedThisTick--;
                return false;
            }
            Left why = rules.check(pos, here, target, false, true);
            if (why == null && !rules.claims(pos, here, target)) {
                why = Left.PROTECTED;
            }
            if (why != null) {
                rules.left(why); // it stays in the step: the next Ctrl + Z tries it again
                slice.use(SKIP);
                continue;
            }
            BlockPos at = pos.immutable();
            if (paying && !takeItems(target)) {
                if (undo) {
                    // The block it would put back is no longer in his inventory: the cell stays as the spell left it.
                    outOf = null;
                    walk.setStatus(UndoStep.DROPPED);
                    notReturned++;
                    slice.use(SKIP);
                    continue;
                }
                // A redo whose items ran out stops here (P11).
                phase = Phase.SIDE_EFFECTS;
                sideIndex = 0;
                return true;
            }
            if (here.hasBlockEntity()) {
                Clearable.tryClear(level.getBlockEntity(at));
            }
            if (paying) {
                giveItems(at, here);
            }
            long hash = place(at, target, data);
            if (!undo) {
                walk.rehash(hash);
            } else if (target.hasBlockEntity()) {
                walk.setRestoredHash(hash);
            }
            walk.setStatus(undo ? UndoStep.UNDONE : UndoStep.APPLIED);
            for (UndoStep.Carried thing : step.carriedOn(at, undo)) {
                hangBack(thing, undo);
            }
            written++;
            changed++;
            writtenThisTick++;
            slice.use(WRITE);
        }
    }

    /** The settle pass's side effects, put back (undo, the last first) or done again (redo). */
    private boolean sideEffects(Slice slice) {
        boolean undo = kind == Kind.UNDO;
        var effects = step.sideEffects();
        while (sideIndex >= 0 && sideIndex < effects.size()) {
            if (slice.spent()) {
                return false;
            }
            UndoStep.SideEffect effect = effects.get(sideIndex);
            sideIndex += undo ? -1 : 1;
            byte want = undo ? UndoStep.APPLIED : UndoStep.UNDONE;
            if (effect.status() != want) {
                continue;
            }
            BlockPos pos = effect.pos();
            if (!SpellMath.loaded(level, pos)) {
                continue;
            }
            BlockState here = level.getBlockState(pos);
            BlockState expected = undo ? effect.newState() : effect.oldState();
            if (!here.equals(expected)) {
                effect.setStatus(UndoStep.DROPPED);
                continue;
            }
            BlockState target = undo ? effect.oldState() : effect.newState();
            if (rules.check(pos, here, target, false, true) != null || !rules.claims(pos, here, target)) {
                continue;
            }
            place(pos, target, null);
            effect.setStatus(undo ? UndoStep.UNDONE : UndoStep.APPLIED);
            slice.use(WRITE);
        }
        toSettle();
        return true;
    }

    private void toSettle() {
        phase = Phase.SETTLE;
        walk = step.walk(false);
        holding = false;
    }

    // ---- SETTLE (D-B 11.5) ----

    /**
     * Settles the cells this job wrote, a slice at a time: every written cell on the outside of the spell (a neighbour
     * the spell did not write), and every written cell whose block cares about its neighbours, is shaped by them
     * ({@code Block.updateFromNeighbourShapes}); then its outside neighbours by it (a torch on a vanished wall drops,
     * with no item, and is recorded so that undo brings it back), after P4, P5, P8 and the break event; then its
     * neighbours are told once ({@code updateNeighborsAt}), so redstone, observers and water react once. A Land pulse
     * settles only the cells it wrote itself.
     */
    private boolean settle(Slice slice) {
        byte want = kind == Kind.UNDO ? UndoStep.UNDONE : UndoStep.APPLIED;
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
        while (true) {
            if (slice.spent()) {
                return false;
            }
            BlockPos pos;
            BlockState expected;
            if (mine != null) {
                // A Land pulse settles the cells it wrote (its stroke's earlier pulses settled theirs).
                if (settled >= mine.size()) {
                    phase = Phase.DONE;
                    return true;
                }
                pos = cell.set(mine.getLong(settled++));
                expected = step.newState(pos);
                if (expected == null) {
                    continue;
                }
            } else {
                if (!walk.next()) {
                    phase = Phase.DONE;
                    return true;
                }
                if (walk.status() != want) {
                    continue;
                }
                pos = walk.pos();
                expected = kind == Kind.UNDO ? walk.oldState() : walk.newState();
            }
            if (!SpellMath.loaded(level, pos)) {
                continue;
            }
            BlockState here = level.getBlockState(pos);
            if (expected != null && !here.equals(expected)) {
                continue; // (a lite step has no states to compare)
            }
            boolean outside = false;
            for (Direction direction : Direction.values()) {
                if (!step.has(next.setWithOffset(pos, direction))) {
                    outside = true;
                    break;
                }
            }
            if (!outside && !cares(here)) {
                continue;
            }
            slice.use(SKIP);
            BlockPos at = pos.immutable();
            BlockState shaped = Block.updateFromNeighbourShapes(here, level, at);
            if (!shaped.equals(here)) {
                level.setBlock(at, shaped, SokoviaJob.FLAGS);
                if (kind != Kind.UNDO) {
                    step.settled(at, shaped);
                }
                hold(at, shaped);
                here = shaped;
                slice.use(WRITE);
            }
            if (outside) {
                for (Direction direction : Direction.values()) {
                    BlockPos side = at.relative(direction);
                    if (step.has(side) || !SpellMath.loaded(level, side)) {
                        continue;
                    }
                    BlockState before = level.getBlockState(side);
                    BlockState after = before.updateShape(direction.getOpposite(), here, level, side, at);
                    if (after.equals(before) || !mayChangeNeighbour(side, before, after)) {
                        continue;
                    }
                    if (kind == Kind.SPELL) {
                        step.sideEffect(side, before, after);
                    }
                    Block.updateOrDestroy(before, after, level, side, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                    slice.use(WRITE);
                }
            }
            level.updateNeighborsAt(at, here.getBlock());
            holdAround(at);
        }
    }

    /** P4, P5, P8 and (when the block itself changes) the break event, for a neighbour the settle pass changes. */
    private boolean mayChangeNeighbour(BlockPos pos, BlockState before, BlockState after) {
        ServerPlayer actor = rules.player();
        if (SpellMath.untouchable(before, level, pos) || BlockRights.massEditRefuses(actor, level, pos) || !BlockRights.vanillaRights(actor, level, pos)) {
            return false;
        }
        if (before.hasBlockEntity() && before.getBlock() != after.getBlock()) {
            return false; // (a sign or a banner that would pop off stays: its text is not in the step, vanilla breaks it, with its drop, in its turn)
        }
        return before.getBlock() == after.getBlock() || before.isAir() || BlockRights.postBreak(actor, level, pos, before);
    }

    /** A falling block of the spell next to a cell the settle pass changed has its fall cleared again. */
    private void holdAround(BlockPos pos) {
        if (!fallables || !BuildConfig.HOLD_GRAVITY_BLOCKS.get()) {
            return;
        }
        BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    near.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (step.has(near) && SpellMath.loaded(level, near) && level.getBlockState(near).getBlock() instanceof Fallable) {
                        level.getBlockTicks().clearArea(new BoundingBox(near.immutable()));
                    }
                }
            }
        }
    }

    /**
     * Whether a block "cares about its neighbours" (D-B 11.5), cached per state: it has a state property, holds a
     * fluid, is not a full cube, or has a block entity. A plain full block deep inside a fill needs no settling.
     */
    static boolean cares(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        return CARES.computeIfAbsent(state, (BlockState s) -> !s.getProperties().isEmpty() || !s.getFluidState().isEmpty() || s.hasBlockEntity()
                || !Block.isShapeFullBlock(s.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)));
    }

    // ---- DONE ----

    private void finish() {
        releaseTickets();
        finished = true;
        switch (kind) {
            case SPELL -> finishSpell();
            case UNDO -> finishUndo();
            case REDO -> finishRedo();
        }
        endJournal();
    }

    /**
     * The job is over: the read-ahead goes, and a spell that changed nothing leaves no journal and no file behind (a
     * stroke's journal is its step's, sealed or discarded with the stroke).
     */
    private void endJournal() {
        ahead = null;
        if (journal != null) {
            if (stroke == null && kind == Kind.SPELL && step.changed() == 0) {
                UndoFiles.discard(UndoHistory.of(caster), step);
            }
            journal = null;
        }
    }

    /**
     * Whether the step of this spell can be kept: it was not run without one, and a step that lives on its records alone has
     * them (the disk did not fail while it worked).
     */
    private boolean keepsStep() {
        return !noStep && !(step.isLite() && (step.journal == null || step.journal.failed()));
    }

    private void finishSpell() {
        if (stroke != null) {
            finishDab();
            return;
        }
        ServerPlayer online = online();
        boolean changed = step.changed() > 0;
        bookBilly(); // (before the step is sealed: its file holds Billy's cost and practice)
        if (changed && !keepsStep()) {
            step.setState(UndoStep.State.DONE); // (too big to remember: nothing is kept, nothing can be undone)
            UndoFiles.discard(UndoHistory.of(caster), step);
        } else if (changed) {
            step.setState(UndoStep.State.DONE);
            int forgotten = UndoHistory.of(caster).push(step, limits.undoSteps());
            if (forgotten > 0 && online != null) {
                online.displayClientMessage(BillyMagic.forgotten(), true);
            }
        }
        long left = rules.leftInAll() - rules.count(Left.CHANGED);
        int code;
        if (stopCode >= 0) {
            code = stopCode;
        } else if (!changed && left == 0 && rules.count(Left.CHANGED) == 0) {
            code = BuildVerdictPayload.NOTHING_CHANGES;
        } else {
            code = left > 0 || rules.count(Left.CHANGED) > 0 ? BuildVerdictPayload.DONE_PARTLY : BuildVerdictPayload.DONE;
        }
        endCode = code;
        fx(stopCode >= 0 ? BuildFxPayload.STOPPED : BuildFxPayload.DONE);
        BoundingBox box = step.bounds();
        if (box != null && stopCode < 0) {
            BuildSounds.done(level, box.getCenter().getCenter());
            burst(Vec3.atCenterOf(new BlockPos(box.getCenter().getX(), box.maxY() + 1, box.getCenter().getZ())));
        }
        tellDone(online, caster, limits, token, code, this.changed, cost, rules, outOf != null ? ranOut() : null);
        BuildMagic.sync(level.getServer(), caster);
    }

    /** "No more Stone: 3 blocks were not placed": the items ran out (P11) and the cells paid with them not yet done. */
    private Component ranOut() {
        return BuildRules.outOf(outOf, Math.max(0L, payCells - paidCells));
    }

    /**
     * What the builder is told when a spell (or a stroke) is done: the verdict with its counts, and one line, the most
     * important first ({@code said}, when not null: the items ran out; it stopped; nothing changed; what was left; else
     * the done line of her first five spells); a builder who is away hears at her next login that it finished.
     */
    private static void tellDone(@Nullable ServerPlayer online, UUID caster, Limits limits, int token, int code, long changedCells, int cost,
                                 BuildRules.Cells rules, @Nullable Component said) {
        int count = (int) Math.min(Integer.MAX_VALUE, changedCells);
        if (online == null) {
            if (changedCells > 0) {
                BuildSession.of(caster).tellAtLogin(Component.translatable("message.enhanced.build.finished_away", count));
            }
            return;
        }
        send(online, new BuildVerdictPayload(token, code, count, cost, 0, rules.flags()));
        Component line = said != null ? said : switch (code) {
            case BuildVerdictPayload.STOPPED_FIGHT -> Component.translatable("message.enhanced.build.stopped_fight", count);
            case BuildVerdictPayload.STOPPED -> Component.translatable("message.enhanced.build.stopped_mode");
            case BuildVerdictPayload.NOTHING_CHANGES -> Component.translatable("message.enhanced.build.nothing_changes");
            default -> {
                Component summary = rules.summary();
                if (summary == null && rules.count(Left.CHANGED) > 0) {
                    summary = Left.CHANGED.message(rules.count(Left.CHANGED));
                }
                if (summary != null) {
                    yield summary;
                }
                yield BuildSession.of(caster).saysDone() ? Component.translatable(limits.billy() ? "message.enhanced.build.done.billy"
                        : "message.enhanced.build.done.scarlet_witch", count, Component.keybind("key.enhanced.build_undo")) : null;
            }
        };
        if (line != null) {
            online.displayClientMessage(line, true);
        }
    }

    /**
     * A dab is done: what it changed is in its stroke's step, what Billy reserved and did not use comes back, and it
     * says nothing (the stroke speaks when it is sealed).
     */
    private void finishDab() {
        endCode = changed > 0 ? BuildVerdictPayload.DONE : BuildVerdictPayload.NOTHING_CHANGES;
        settleDab();
        if (outOf != null) {
            stroke.said = ranOut(); // (its items ran out: the stroke says so when it is sealed)
        }
        if (stopCode >= 0) {
            stroke.closing = true; // (a fight began over it, its painter may no longer build, the items ran out: it ends here)
        }
        dabEnded();
        BuildMagic.sync(level.getServer(), caster);
    }

    /** What a dab painted joins its stroke; what Billy reserved for it and it did not use comes back. */
    private void settleDab() {
        int used = BillyMagic.cost(changed, mom);
        if (limits.limited() && cost > used) {
            BuildMagic.billy(level.getServer(), caster, cost - used, 0, -1);
        }
        stroke.cost += Math.min(cost, used);
        stroke.changed += changed;
    }

    /**
     * A dab of the stroke is over (done, refused, dropped): the stroke is sealed once it is closing and none is left.
     */
    private void dabEnded() {
        stroke.pending = Math.max(0, stroke.pending - 1);
        if (stroke.closing && stroke.pending == 0) {
            sealStroke(stroke, level.getServer());
        }
    }

    /**
     * Seals a Paint or Land stroke (D-B 11.8): the one step its dabs wrote goes into the history (if they changed
     * anything), Billy's practice is booked and his pause begins, and the builder is told as for any spell; a stroke a
     * refused dab ended says why it ended (an empty meter), or nothing more when it changed nothing. Its last dab must
     * be done ({@link BuildSession.Stroke#pending} 0).
     */
    static void sealStroke(BuildSession.Stroke stroke, net.minecraft.server.MinecraftServer server) {
        if (stroke.sealed) {
            return;
        }
        stroke.sealed = true;
        stroke.closing = true;
        BuildSession.of(stroke.caster).sealed(stroke);
        ServerPlayer online = server.getPlayerList().getPlayer(stroke.caster);
        if (online != null && online.hasDisconnected()) {
            online = null;
        }
        UndoStep step = stroke.step;
        boolean changed = step.changed() > 0;
        if (changed) {
            step.setState(UndoStep.State.DONE);
            step.setBilly(stroke.cost, stroke.changed, stroke.mom);
            int forgotten = UndoHistory.of(stroke.caster).push(step, stroke.limits.undoSteps());
            if (forgotten > 0 && online != null) {
                online.displayClientMessage(BillyMagic.forgotten(), true);
            }
        }
        if (!changed) {
            UndoFiles.discard(UndoHistory.of(stroke.caster), step); // (nothing to keep: no journal, no file)
        }
        if (stroke.limits.limited()) {
            BuildMagic.billy(server, stroke.caster, 0, stroke.changed, stroke.limits.castPause());
        }
        BuildRules.Cells rules = stroke.rules;
        long left = rules.leftInAll() - rules.count(Left.CHANGED);
        int code = !changed && left == 0 && rules.count(Left.CHANGED) == 0 ? BuildVerdictPayload.NOTHING_CHANGES
                : left > 0 || rules.count(Left.CHANGED) > 0 ? BuildVerdictPayload.DONE_PARTLY : BuildVerdictPayload.DONE;
        BoundingBox box = step.bounds();
        if (box != null) {
            sendFx(stroke.level, online, BuildFxPayload.DONE, stroke.fxId, stroke.limits.hero(), box, new BlockPos(box.minX(), box.minY(), box.minZ()),
                    1000, (int) Math.min(Integer.MAX_VALUE, stroke.changed), stroke.momId, BuildFxPayload.RISE);
            BuildSounds.done(stroke.level, box.getCenter().getCenter());
        }
        if (changed || !stroke.refused) {
            // (a stroke a refusal ended before it changed anything was told why by the refusal: nothing more to say)
            tellDone(online, stroke.caster, stroke.limits, stroke.token, code, stroke.changed, stroke.cost, rules, stroke.said);
        }
        BuildMagic.sync(server, stroke.caster);
    }

    /**
     * Billy's reservation is settled at the end: what was not used comes back, the practice is booked and his pause
     * begins (D-B 8.1, 8.2), at once or at his next login.
     */
    private void bookBilly() {
        if (!limits.limited()) {
            return;
        }
        int used = BillyMagic.cost(changed, mom);
        int back = Math.max(0, cost - used);
        cost = Math.min(cost, used);
        step.setBilly(cost, changed, mom);
        BuildMagic.billy(level.getServer(), caster, back, changed, limits.castPause());
    }

    /**
     * An undo gives Billy back the cost of every cell it restored ({@code cost x restored / changed}, rounded down) and
     * takes their practice back the same way ({@code practice x restored / changed}: a stroke that changed a cell at
     * several pulses taught him more than its cells, and a Move's two ends are one block moved); his rank stays (D-B
     * 12.3).
     */
    private void refundUndo() {
        if ((step.cost() > 0 || step.practice() > 0) && step.changed() > 0 && changed > 0) {
            long back = step.cost() * changed / step.changed();
            long practice = step.practice() * changed / step.changed();
            BuildMagic.billy(level.getServer(), caster, back, -practice, -1);
        }
    }

    /**
     * A redo of Billy's: he paid for it when he asked; what it could not do again comes back, and what it did teaches
     * him again (D-B 12.2). The step now remembers this redo's cost, which its next undo gives back.
     */
    private void billRedo() {
        if (!limits.limited()) {
            return;
        }
        int used = BillyMagic.cost(changed, mom);
        int back = Math.max(0, cost - used);
        cost = Math.min(cost, used);
        step.setBilly(cost, changed, mom);
        BuildMagic.billy(level.getServer(), caster, back, changed, -1);
    }

    /** D-B 7.2: 40 sparks at the top middle of a spell that is done. */
    private void burst(Vec3 at) {
        int colour = hero == Power.BILLY ? Power.BILLY.color : Power.SCARLET_WITCH.color;
        level.sendParticles(new EnergyParticleOptions(colour, 1.0F), at.x, at.y, at.z, 40, 0.8, 0.4, 0.8, 0.05);
    }

    /** D-B 7.2: 20 sparks drift from the middle of an undone spell to its caster. */
    private void drift(BoundingBox box) {
        Vec3 from = Vec3.atCenterOf(box.getCenter());
        Vec3 to = (online() != null ? online().getEyePosition() : eyes);
        Vec3 way = to.subtract(from).normalize().scale(0.3);
        int colour = hero == Power.BILLY ? Power.BILLY.color : Power.SCARLET_WITCH.color;
        for (int i = 0; i < 20; i++) {
            level.sendParticles(new EnergyParticleOptions(colour, 1.0F), from.x, from.y, from.z, 0, way.x, way.y, way.z, 1.0);
        }
    }

    private void finishUndo() {
        step.setState(step.applied() > 0 ? UndoStep.State.PARTLY : UndoStep.State.UNDONE);
        UndoHistory history = UndoHistory.of(caster);
        history.undid(step);
        ServerPlayer online = online();
        long restored = changed;
        long kept = rules.count(Left.CHANGED);
        boolean partly = step.state() == UndoStep.State.PARTLY;
        endCode = rewind ? BuildVerdictPayload.STOPPED : partly ? BuildVerdictPayload.UNDONE_PARTLY : BuildVerdictPayload.UNDONE;
        refundUndo();
        fx(BuildFxPayload.DONE);
        BoundingBox undoneBox = step.bounds();
        if (undoneBox != null && changed > 0) {
            drift(undoneBox);
        }
        if (online != null) {
            int count = (int) Math.min(Integer.MAX_VALUE, restored);
            send(online, new BuildVerdictPayload(token, endCode, count, 0, (int) Math.min(Integer.MAX_VALUE, kept), rules.flags()));
            Component line;
            if (rewind) {
                line = Component.translatable("message.enhanced.build.stopped");
            } else if (partly) {
                line = Component.translatable("message.enhanced.build.undo_partly");
            } else if (notReturned > 0) {
                line = Component.translatable("message.enhanced.build.not_returned");
            } else if (kept > 0) {
                line = Left.CHANGED.message(kept);
            } else {
                line = Component.translatable("message.enhanced.build.undone", count);
            }
            online.displayClientMessage(line, true);
        }
        BuildMagic.sync(level.getServer(), caster);
    }

    private void finishRedo() {
        step.setState(UndoStep.State.DONE);
        UndoHistory.of(caster).redid(step);
        endCode = BuildVerdictPayload.REDONE;
        billRedo();
        fx(BuildFxPayload.DONE);
        ServerPlayer online = online();
        if (online != null) {
            int count = (int) Math.min(Integer.MAX_VALUE, changed);
            send(online, new BuildVerdictPayload(token, endCode, count, cost, 0, rules.flags()));
            online.displayClientMessage(outOf != null ? BuildRules.outOf(outOf, step.undone()) : Component.translatable("message.enhanced.build.redone",
                    count), true);
        }
        BuildMagic.sync(level.getServer(), caster);
    }

    /** The spell ends refused before any block changed (C7, C11-C13, a flood refused). */
    private void refuse(int code, long extra, @Nullable Component line) {
        refuse(code, 0, 0, extra, line);
    }

    /**
     * The same, with the counts its verdict names: {@code changed} the cells it would have changed, {@code cost} what
     * it would cost Billy, {@code extra} the number it names (the seconds to wait, his top, a cap, the items missing).
     */
    private void refuse(int code, long changed, int cost, long extra, @Nullable Component line) {
        releaseTickets();
        endJournal();
        finished = true;
        endCode = code;
        ServerPlayer online = online();
        if (online != null) {
            send(online, new BuildVerdictPayload(token, code, (int) Math.min(Integer.MAX_VALUE, changed), cost, (int) Math.min(Integer.MAX_VALUE, extra),
                    0));
            if (line != null) {
                online.displayClientMessage(line, true);
            }
            BuildSounds.fizzle(online);
        }
        fx(BuildFxPayload.FIZZLE);
        if (stroke != null) {
            // A refused dab (an empty meter, a thing too big) ends its stroke with the fizzle: what it painted stays,
            // and the stroke's sealing says why it ended, never that nothing changed.
            stroke.refused = true;
            if (stroke.said == null) {
                stroke.said = line;
            }
            stroke.closing = true;
            dabEnded();
        }
        BuildMagic.sync(level.getServer(), caster);
    }

    /** The job ended without a refusal and without a step of its own (a Copy's take). */
    private void end(int code) {
        releaseTickets();
        endJournal();
        finished = true;
        endCode = code;
        BuildMagic.sync(level.getServer(), caster);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Chunks

    /** Holds the chunk with the job's ticket (a moving window of at most {@code maxTicketChunks}). */
    private void ticket(long chunk) {
        if (!ticketed.add(chunk)) {
            return;
        }
        everTicketed.add(chunk);
        ticketOrder.enqueue(chunk);
        level.getChunkSource().addRegionTicket(ModTicketTypes.ENHANCED_BUILD, new ChunkPos(chunk), 1, (long) id);
        while (ticketed.size() > window) {
            long oldest = ticketOrder.dequeueLong();
            ticketed.remove(oldest);
            level.getChunkSource().removeRegionTicket(ModTicketTypes.ENHANCED_BUILD, new ChunkPos(oldest), 1, (long) id);
        }
    }

    /** Lets go of every ticket. */
    void releaseTickets() {
        while (!ticketOrder.isEmpty()) {
            long chunk = ticketOrder.dequeueLong();
            if (ticketed.remove(chunk)) {
                level.getChunkSource().removeRegionTicket(ModTicketTypes.ENHANCED_BUILD, new ChunkPos(chunk), 1, (long) id);
            }
        }
        ticketed.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Telling

    /** The moment of the spell, to everyone within 128 blocks of its box, and its caster. */
    void fx(int moment) {
        BoundingBox box = bounds();
        if (box == null) {
            box = new BoundingBox(BlockPos.containing(eyes));
        }
        ServerPlayer online = online();
        BlockPos origin = new BlockPos(Math.clamp((long) Math.floor(eyes.x), box.minX(), box.maxX()), Math.clamp((long) Math.floor(eyes.y), box.minY(),
                box.maxY()), Math.clamp((long) Math.floor(eyes.z), box.minZ(), box.maxZ()));
        int order = plan != null && plan.prepared() ? plan.order() : step.fxOrder();
        if (instantMax > 0) {
            order = BuildFxPayload.POP;
        } else if (kind == Kind.REDO) {
            // (the client plays an undo's ripple backwards: a redo writes the way the spell did, so its band sweeps the other way round)
            order = order == BuildFxPayload.RISE ? BuildFxPayload.MELT : order == BuildFxPayload.MELT ? BuildFxPayload.RISE : order;
        }
        int kindOfMoment = kind != Kind.SPELL && moment == BuildFxPayload.START ? BuildFxPayload.UNDO_START : moment;
        if (stroke != null && moment == BuildFxPayload.START) {
            stroke.fxId = id; // (the stroke's first dab starts its effects; its seal ends them)
        }
        sendFx(level, online, kindOfMoment, id, hero, box, origin, permille(), (int) Math.min(Integer.MAX_VALUE, finished ? changed : volume), momId,
                order);
    }

    /**
     * A moment of a spell (D-B 7.2) to everyone within 128 blocks of its box, and its caster wherever he is; a fizzle
     * to the caster alone.
     */
    static void sendFx(ServerLevel level, @Nullable ServerPlayer online, int moment, int id, Power hero, BoundingBox box, BlockPos origin, int permille,
                       int cells, int momId, int order) {
        BuildFxPayload payload = new BuildFxPayload(moment, id, online != null ? online.getId() : -1, hero == Power.BILLY ? BuildFxPayload.BILLY
                : BuildFxPayload.WANDA, new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()), origin,
                permille, cells, momId, order);
        if (moment == BuildFxPayload.FIZZLE) {
            if (online != null) {
                send(online, payload);
            }
            return;
        }
        AABB near = AABB.of(box).inflate(128.0);
        for (ServerPlayer player : level.players()) {
            if (player == online || near.contains(player.position())) {
                send(player, payload);
            }
        }
        if (online != null && online.level() != level) {
            send(online, payload);
        }
    }

    private static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }

    // ------------------------------------------------------------------------------------------------------------

    /** What one job may still use of a tick: its share of the writes, and the tick's time and sections. */
    private static final class Slice {
        final BuildScheduler.Tick tick;
        final long share;
        long used;
        int sinceClock;
        boolean timeUp;

        Slice(BuildScheduler.Tick tick, long share) {
            this.tick = tick;
            this.share = share;
        }

        void use(long units) {
            used += units;
        }

        long used() {
            return used;
        }

        /** Whether the share is used up or the tick's time ran out (the clock read every 64 cells). */
        boolean spent() {
            if (used >= share || timeUp) {
                return true;
            }
            if (++sinceClock >= BuildRules.CLOCK_EVERY) {
                sinceClock = 0;
                timeUp = tick.timeUp();
            }
            return timeUp;
        }

        /** How many reads fit in what is left of the share. */
        int reads() {
            return (int) Math.max(1, Math.min(Integer.MAX_VALUE, (share - used) / READ));
        }

        /** Whether the cell's section may be changed this tick (the tick's new sections). */
        boolean section(ServerLevel level, BlockPos pos) {
            return tick.section(level, SectionPos.asLong(pos));
        }
    }
}
