package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.ability.BlockRights;
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
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.LivingEntity;
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
 *     <li><b>PREPARE</b>: a Pull's surface.</li>
 *     <li><b>CAPTURE</b>: a Copy's take reads the box into its builder's pocket (and ends); a Move or a Repeat reads its
 *     source into a pocket of its own, so overlapping cells come out exact and a Move knows what changed under it.</li>
 *     <li><b>COUNT</b> (Billy, and a Scarlet Witch's spell that makes TNT, lava or fire): the cells that will really
 *     change are counted before any block does; Billy's cost is reserved here, or the spell ends with {@code TOO_BIG} or
 *     {@code REFILLING}, and too many volatile blocks end hers with {@code VOLATILE_CAP}.</li>
 *     <li><b>APPLY</b>: the cells in the planner's work order, each through the rules (P1 to P10) and written as D-B 11.5
 *     says ({@link #write}); a Move moves each cell in one step, compare-and-swap (C12).</li>
 *     <li><b>SETTLE</b>: fences join, torches behave, redstone and water react, once ({@link #settle}).</li>
 *     <li><b>DONE</b>: the step is sealed into the history, Billy's unused reservation refunded and his practice
 *     booked, the verdict and the summary line said.</li>
 * </ol>
 *
 * <p><b>An undo</b> walks its step's cells from the last to the first and puts back the old state of every cell that
 * still holds exactly what the spell put there (D-B 12.3), then the side effects; <b>a redo</b> walks forwards and puts
 * the new state back where the old one still is. Both go through the same rules (P3 replaced by that check) and settle.
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

    /** The most chunks of a plan's box asked one by one at the cast; a bigger box takes the level's loaded chunks. */
    private static final int ASKED_CHUNKS = 65_536;
    private static final Method GET_CHUNKS = ObfuscationReflectionHelper.findMethod(ChunkMap.class, "getChunks");

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
    /** The cursor sits on a cell it could not do yet (its chunk loading, the tick's sections spent). */
    private boolean holding;
    private boolean force;
    /** Whether the spell's chunks are known (at the cast, or once its selected thing or surface is). */
    private boolean chunksKnown = true;

    private final LongOpenHashSet ticketed = new LongOpenHashSet();
    private final LongArrayFIFOQueue ticketOrder = new LongArrayFIFOQueue();
    private final LongOpenHashSet everTicketed = new LongOpenHashSet();

    /** A Move's source cells that were not moved (changed since, refused): never written over. */
    private final LongOpenHashSet kept = new LongOpenHashSet();
    /** A Move's source cells that were moved but not cleared, as its target covers them (cleared if that target fails). */
    private final LongOpenHashSet movedCovered = new LongOpenHashSet();

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

    private BuildJob(Kind kind, UUID caster, GameProfile profile, ServerLevel level, Power hero, Limits limits, int token, int flags,
                     @Nullable SpellRecipe recipe, Vec3 eyes, UndoStep step, boolean rewind, BiPredicate<Integer, Integer> loaded, LongSet chunks,
                     ServerPlayer actor) {
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
        this.rules = new BuildRules.Cells(level, actor);
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
                player.getEyePosition(), step, false, loaded, chunks, player);
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
     * An undo ({@code redo} false) or a redo of {@code step} by its player (online now: {@code player}), in the step's
     * own level; {@code rewind}: the undo of a spell that was just stopped (Ctrl + Z on a working spell), which says
     * "Spell stopped and taken back" when it is done.
     */
    public static BuildJob replay(ServerPlayer player, Limits limits, ServerLevel level, UndoStep step, boolean redo, boolean rewind,
                                  BiPredicate<Integer, Integer> loaded) {
        // An undo ticket-loads the chunks its step wrote: they exist, the spell wrote them.
        LongSet chunks = step.chunks();
        return new BuildJob(redo ? Kind.REDO : Kind.UNDO, player.getUUID(), player.getGameProfile(), level, limits.hero(), limits, 0, 0, null,
                player.getEyePosition(), step, rewind, loaded, chunks, player).startReplay();
    }

    /** The same for a player who is not online (an operator's undo): his profile acts. */
    public static BuildJob replay(GameProfile profile, Limits limits, ServerLevel level, UndoStep step, boolean redo) {
        ServerPlayer actor = FakePlayerFactory.get(level, profile);
        return new BuildJob(redo ? Kind.REDO : Kind.UNDO, profile.getId(), profile, level, limits.hero(), limits, 0, 0, null, actor.position(), step,
                false, level::hasChunk, step.chunks(), actor).startReplay();
    }

    private BuildJob startReplay() {
        phase = Phase.APPLY;
        walk = step.walk(kind == Kind.UNDO);
        volume = Math.max(1, step.changed());
        return this;
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

    /** The box of what it changes (the plan's, the step's, or the box the recipe names), or null. */
    public @Nullable BoundingBox bounds() {
        if (kind != Kind.SPELL) {
            return step.bounds();
        }
        if (plan != null && plan.prepared() && plan.bounds() != null) {
            return plan.bounds();
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
            return null;
        }
        step.setState(UndoStep.State.DONE);
        UndoHistory.of(caster).push(step, limits.undoSteps());
        bookBilly(player);
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
        if (kind == Kind.SPELL && step.changed() > 0) {
            step.setState(UndoStep.State.INTERRUPTED);
            UndoHistory.of(caster).push(step, limits.undoSteps());
            if (line != null) {
                BuildSession.of(caster).tellAtLogin(Component.translatable(line, Component.keybind("key.enhanced.build_undo")));
            }
        } else if (kind == Kind.UNDO && step.applied() > 0 && step.applied() < step.changed()) {
            step.setState(UndoStep.State.PARTLY);
        }
    }

    /** The job is dropped (it never started, or the server forgets it): its tickets go, nothing else happens. */
    public void drop() {
        releaseTickets();
        finished = true;
        if (endCode < 0) {
            endCode = BuildVerdictPayload.DROPPED;
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
        while (!finished && !slice.spent()) {
            boolean more = switch (phase) {
                case SELECT -> select(slice);
                case PREPARE -> prepare(slice);
                case CAPTURE -> capture(slice);
                case COUNT -> count(slice);
                case APPLY -> kind == Kind.SPELL ? apply(slice) : replay(slice);
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
        if (!finished && ticks % PROGRESS_TICKS == 0) {
            fx(BuildFxPayload.PROGRESS);
        }
        if (!finished && ticks % SYNC_TICKS == 0) {
            BuildMagic.sync(level.getServer(), caster);
        }
        if (!finished && ticks % 20 == 0 && kind == Kind.SPELL && phase == Phase.APPLY && cursor != null) {
            BuildSounds.hum(level, Vec3.atCenterOf(cursor.pos()), hero);
        }
        return slice.used();
    }

    /**
     * At the start of a spell's slice (D-B 11.7): a fight began over its box since it was cast (one that ran then is
     * P5's, cell by cell), or its online caster may no longer build.
     */
    private void checkStillAllowed() {
        BoundingBox box = bounds();
        if (box != null && fightBegan() && BossFights.overlapsAny(level, box)) {
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

    /** Whether a fight of this level runs now that did not when the spell was cast. */
    private boolean fightBegan() {
        for (BossFight fight : BossFights.all(level.getServer())) {
            if (fight.level() == level && !fightsAtCast.contains(fight)) {
                return true;
            }
        }
        return false;
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
        setup = setup.withMask(select.mask());
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
        if (needsCapture()) {
            phase = Phase.CAPTURE;
        } else {
            startCount();
        }
        return true;
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
        }
        int reads = slice.reads();
        boolean done = taker.step(reads);
        slice.use((long) reads * READ);
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

    /** COUNT when the spell needs it (Billy's cost, Wanda's volatile blocks), else straight to APPLY. */
    private void startCount() {
        if (limits.limited() || (limits.volatilePerSpell() > 0 && makesVolatile())) {
            count = plan.count(level);
            phase = Phase.COUNT;
        } else {
            startApply();
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
        if (!limits.limited() && volatiles > limits.volatilePerSpell()) {
            refuse(BuildVerdictPayload.VOLATILE_CAP, volatiles, Component.translatable("message.enhanced.build.volatile_cap", volatiles,
                    limits.volatilePerSpell()));
            return false;
        }
        if (limits.limited() && count.changed() > 0) {
            ServerPlayer online = online();
            BuildData data = online != null ? online.getData(com.kolodkingal.enhanced.registry.ModAttachments.BUILD_DATA) : null;
            if (data != null) {
                long time = level.getServer().overworld().getGameTime();
                int price = BillyMagic.cost(count.changed(), mom);
                int verdict = BillyMagic.verdict(data, time, price);
                if (verdict != SpellPlanner.NONE) {
                    refuse(verdict, verdict == BuildVerdictPayload.REFILLING ? BillyMagic.secondsToPay(data, time, price) : price,
                            BillyMagic.cannotPay(data, time, price));
                    return false;
                }
                cost = price;
                online.setData(com.kolodkingal.enhanced.registry.ModAttachments.BUILD_DATA, BillyMagic.pay(data, time, price));
            }
        }
        count = null;
        startApply();
        return true;
    }

    private void startApply() {
        if (!plan.writes()) {
            phase = Phase.DONE;
            return;
        }
        volume = Math.max(1, plan.size());
        cursor = plan.cursor();
        phase = Phase.APPLY;
    }

    // ---- APPLY (a spell) ----

    private boolean apply(Slice slice) {
        BoundingBox bounds = plan.bounds();
        findBodies(bounds, cursor.walked() > 0 ? cursor.pos() : BlockPos.containing(eyes));
        long pace = pace(slice.tick);
        boolean moving = recipe.spell() == Spell.MOVE && source != null;
        boolean conjured = recipe.spell() != Spell.MOVE;
        while (true) {
            if (stopCode >= 0) {
                toSettle();
                return true;
            }
            if (slice.spent()) {
                return false;
            }
            if (!holding && !cursor.next()) {
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
            BlockState old = level.getBlockState(pos);
            BlockState now = cursor.resolve(level);
            CompoundTag data = cursor.data();
            // P3: nothing changes, nothing is paid. (A Move's cell whose target already holds the block still moves:
            // its source is emptied.)
            if (now == null || !moving && now.equals(old) && data == null) {
                slice.use(SKIP);
                continue;
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
            }
        }
    }

    /** The cells a tick of this job may walk: {@code ceil(volume / minSpellTicks)} (D-B 11.3), so a spell is seen to grow. */
    private long pace(BuildScheduler.Tick tick) {
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
     * The rules (P4 to P10) and the write of one cell of a spell; false when a rule left it. A block of two cells
     * whose other half is outside the spell is never cut in half: that half is written in the same step.
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
        if (!rules.claims(pos, old, now)) {
            rules.left(Left.PROTECTED);
            return false;
        }
        if (partner != null) {
            partner.write(this);
        }
        write(pos, old, now, data, spill);
        changed++;
        return true;
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
        Left why = writeTarget ? rules.check(pos, old, now, false, true) : null;
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
            write(pos, old, now, data, false);
        }
        if (writeTarget || clearSource) {
            changed++;
            return 1;
        }
        return 0;
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
            for (int up = 1; up <= 64; up++) {
                AABB lifted = body.getBoundingBox().move(0.0, up, 0.0);
                if (level.noCollision(body, lifted)) {
                    body.teleportTo(body.getX(), body.getY() + up, body.getZ());
                    body.resetFallDistance();
                    break;
                }
            }
        }
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
            // Only a cell that still holds exactly what the spell put there (or, for a redo, what it found).
            if (!here.equals(expected) || undo && hashOf(pos, here) != expectedHash) {
                walk.setStatus(UndoStep.DROPPED);
                rules.left(Left.CHANGED);
                slice.use(SKIP);
                continue;
            }
            BlockState target = undo ? walk.oldState() : walk.newState();
            CompoundTag data = undo ? walk.oldData() : walk.newData();
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
            if (here.hasBlockEntity()) {
                Clearable.tryClear(level.getBlockEntity(at));
            }
            long hash = place(at, target, data);
            if (!undo) {
                walk.rehash(hash);
            }
            walk.setStatus(undo ? UndoStep.UNDONE : UndoStep.APPLIED);
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
     * neighbours are told once ({@code updateNeighborsAt}), so redstone, observers and water react once.
     */
    private boolean settle(Slice slice) {
        byte want = kind == Kind.UNDO ? UndoStep.UNDONE : UndoStep.APPLIED;
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        while (true) {
            if (slice.spent()) {
                return false;
            }
            if (!walk.next()) {
                phase = Phase.DONE;
                return true;
            }
            if (walk.status() != want) {
                continue;
            }
            BlockPos pos = walk.pos();
            if (!SpellMath.loaded(level, pos)) {
                continue;
            }
            BlockState expected = kind == Kind.UNDO ? walk.oldState() : walk.newState();
            BlockState here = level.getBlockState(pos);
            if (!here.equals(expected)) {
                continue;
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
    }

    private void finishSpell() {
        ServerPlayer online = online();
        boolean changed = step.changed() > 0;
        if (changed) {
            step.setState(UndoStep.State.DONE);
            int forgotten = UndoHistory.of(caster).push(step, limits.undoSteps());
            if (forgotten > 0 && online != null) {
                online.displayClientMessage(BillyMagic.forgotten(), true);
            }
        }
        bookBilly(online);
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
        }
        int count = (int) Math.min(Integer.MAX_VALUE, this.changed);
        if (online == null) {
            if (changed) {
                BuildSession.of(caster).tellAtLogin(Component.translatable("message.enhanced.build.finished_away", count));
            }
        } else {
            send(online, new BuildVerdictPayload(token, code, count, cost, 0, rules.flags()));
            Component line = switch (code) {
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
        BuildMagic.sync(level.getServer(), caster);
    }

    /** Billy's reservation is settled at the end: what was not used comes back, the practice is booked (D-B 8.1, 8.2). */
    private void bookBilly(@Nullable ServerPlayer online) {
        if (!limits.limited() || online == null) {
            return;
        }
        long time = level.getServer().overworld().getGameTime();
        BuildData data = online.getData(com.kolodkingal.enhanced.registry.ModAttachments.BUILD_DATA);
        int used = BillyMagic.cost(changed, mom);
        if (cost > used) {
            data = BillyMagic.refund(data, time, cost - used);
        }
        cost = Math.min(cost, used);
        step.setBilly(cost, changed, mom);
        BillyMagic.Practiced practiced = BillyMagic.practise(data, time, changed);
        data = BillyMagic.pauseAfter(practiced.data(), time, limits.castPause());
        online.setData(com.kolodkingal.enhanced.registry.ModAttachments.BUILD_DATA, data);
        if (practiced.rankedUp()) {
            online.displayClientMessage(BillyMagic.rankUp(data.rank()), false);
            BuildSounds.rankUp(online);
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
        if (online != null && step.cost() > 0 && step.changed() > 0) {
            long time = level.getServer().overworld().getGameTime();
            int back = (int) (step.cost() * restored / step.changed());
            BuildData data = online.getData(com.kolodkingal.enhanced.registry.ModAttachments.BUILD_DATA);
            data = BillyMagic.unpractise(BillyMagic.refund(data, time, back), restored);
            online.setData(com.kolodkingal.enhanced.registry.ModAttachments.BUILD_DATA, data);
        }
        fx(BuildFxPayload.DONE);
        if (online != null) {
            int count = (int) Math.min(Integer.MAX_VALUE, restored);
            send(online, new BuildVerdictPayload(token, endCode, count, 0, (int) Math.min(Integer.MAX_VALUE, kept), rules.flags()));
            Component line;
            if (rewind) {
                line = Component.translatable("message.enhanced.build.stopped");
            } else if (partly) {
                line = Component.translatable("message.enhanced.build.undo_partly");
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
        fx(BuildFxPayload.DONE);
        ServerPlayer online = online();
        if (online != null) {
            int count = (int) Math.min(Integer.MAX_VALUE, changed);
            send(online, new BuildVerdictPayload(token, endCode, count, 0, 0, rules.flags()));
            online.displayClientMessage(Component.translatable("message.enhanced.build.redone", count), true);
        }
        BuildMagic.sync(level.getServer(), caster);
    }

    /** The spell ends refused before any block changed (C7, C11-C13, a flood refused). */
    private void refuse(int code, long extra, @Nullable Component line) {
        releaseTickets();
        finished = true;
        endCode = code;
        ServerPlayer online = online();
        if (online != null) {
            send(online, new BuildVerdictPayload(token, code, 0, 0, (int) Math.min(Integer.MAX_VALUE, extra), 0));
            if (line != null) {
                online.displayClientMessage(line, true);
            }
            BuildSounds.fizzle(online);
        }
        fx(BuildFxPayload.FIZZLE);
        BuildMagic.sync(level.getServer(), caster);
    }

    /** The job ended without a refusal and without a step of its own (a Copy's take). */
    private void end(int code) {
        releaseTickets();
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
        int order = plan != null && plan.prepared() ? plan.order() : kind == Kind.UNDO ? BuildFxPayload.MELT : BuildFxPayload.RISE;
        int kindOfMoment = kind != Kind.SPELL && moment == BuildFxPayload.START ? BuildFxPayload.UNDO_START : moment;
        BuildFxPayload payload = new BuildFxPayload(kindOfMoment, id, online != null ? online.getId() : -1, hero == Power.BILLY ? BuildFxPayload.BILLY
                : BuildFxPayload.WANDA, new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()), origin,
                permille(), (int) Math.min(Integer.MAX_VALUE, finished ? changed : volume), momId, order);
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
