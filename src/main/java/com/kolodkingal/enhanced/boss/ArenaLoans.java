package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.ability.BlockLifting;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

/**
 * The blocks a protected arena lends to block telekinesis (D-T 5), and the ledger that gets every one of them back. One per
 * {@link BossFight}, used only where its arena protects the ground ({@link BossArena#protects}: Ultron's and Apocalypse's,
 * and the island in the sky whatever the config says).
 *
 * <ul>
 *     <li><b>The loan.</b> A block torn out of the arena leaves a <em>home</em> behind (its cell, its state, when it is
 *     due); the lifted block is the fight's (its mark), held like any other. The home mends {@code boss.arenaMendTicks}
 *     later, lowest first; a home something stands in, or whose falling block would hang over another open home, waits for
 *     its own retry and holds up no other.</li>
 *     <li><b>What a borrowed block does.</b> Thrown, it turns to dust where it hits (no loot). Set down, it <em>stands</em>
 *     {@code boss.borrowedBlockTicks}, then crumbles back to what was there; it crumbles at once where it may not stand:
 *     no room, on another standing borrowed block, next to anyone, the owner's {@code boss.maxStandingPerPlayer} already
 *     standing, the ground the clients hide, outside its arena. A standing block lifted again is the same loan.</li>
 *     <li><b>What the arena lends.</b> Nothing with {@code boss.lendArenaBlocks} off, nothing the fight's own rule refuses
 *     ({@link BossFight#lendRefusal}), not half of a door, not a falling block over a gap ({@code arena_hanging}), not the
 *     block under an open home or a standing borrowed block ({@code arena_mending}: a hole is never deeper than one block),
 *     and no more than {@code boss.maxBorrowedBlocks} held, flying or standing at once ({@code arena_full}).</li>
 *     <li><b>Quiet writes.</b> Every block the ledger writes is set with {@link #FLAGS} (the clients told, no neighbour or
 *     shape update, nothing dropped) and made quiet ({@link #quiet}: the cell's scheduled ticks removed, which {@code onPlace}
 *     would otherwise have given a falling block), so whatever the ledger writes stays exactly where it is put.</li>
 *     <li><b>The end.</b> When the fight finishes (and before Ultron's island rises or comes down) {@link #returnAll} puts
 *     everything back in one tick.</li>
 *     <li><b>The journal.</b> A snapshot of the ledger is handed to {@link ArenaJournalWriter} before a cell changes at a
 *     borrow and at a stand (at most once a second after a mend or a crumble), so a hard stop of the server leaves a file
 *     that {@link ArenaLoansRecovery} puts the ground back from at the next start. A repair (a mend, a crumble, the return,
 *     a recovery) leaves the journal only at the second save of its dimension after it (R48): the world on disk holds it by
 *     then. The homes of the island's copies are never journaled (the island's own recovery clears the sky); a standing
 *     block on a copy cell is, with nothing but air before it.</li>
 * </ul>
 */
public final class ArenaLoans {
    /** The ledger's writes: the clients told, no neighbour or shape update, nothing dropped (the island's flags). */
    public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    /** The version of the journal's format. */
    public static final int VERSION = 1;

    public static final String PROTECTED = "message.enhanced.telekinesis.protected";
    public static final String BORROWED = "message.enhanced.telekinesis.borrowed";
    public static final String CRUMBLES = "message.enhanced.telekinesis.borrowed_crumbles";
    public static final String MENDING = "message.enhanced.telekinesis.arena_mending";
    public static final String HANGING = "message.enhanced.telekinesis.arena_hanging";
    public static final String FULL = "message.enhanced.telekinesis.arena_full";

    /** A home that cannot mend yet is tried again this many ticks later. */
    public static final int RETRY_TICKS = 20;
    /** A repaired entry leaves the journal at this many saves of its dimension after the repair (R48). */
    public static final int SAVES_TO_RETIRE = 2;
    /** A standing block lets dust fall from it in its last this many ticks ... */
    private static final int DUST_TICKS = 40;
    /** ... every this many. */
    private static final int DUST_INTERVAL = 10;
    /** A ledger with something new to journal hands over a snapshot at most this often (after a mend or a crumble). */
    private static final int SNAPSHOT_INTERVAL = 20;
    /** The borrowed blocks that are gone are forgotten this often. */
    private static final int PRUNE_INTERVAL = 20;

    /** The saves of each dimension since the server started (the clock of R48). */
    private static final Map<ResourceKey<Level>, Long> SAVES = new HashMap<>();
    /** The ledgers of finished fights whose journals still keep repairs a save does not hold yet. */
    private static final List<ArenaLoans> WAITING = new ArrayList<>();

    /** A hole a borrowed block left: it mends to {@code state} when it is due. */
    private static final class Home {
        final BlockPos pos;
        final BlockState state;
        /** A cell of the island's copy: never journaled. */
        final boolean copy;
        final long order;
        long dueAt;

        Home(BlockPos pos, BlockState state, boolean copy, long order, long dueAt) {
            this.pos = pos;
            this.state = state;
            this.copy = copy;
            this.order = order;
            this.dueAt = dueAt;
        }
    }

    /** A borrowed block set down that stands until {@code until}, then gives the cell back to {@code previous}. */
    private record Standing(BlockPos pos, BlockState placed, BlockState previous, long until, UUID owner, long order, boolean copy) {
    }

    /** A journal entry whose repair is done, kept until two saves of its dimension hold it. */
    private record Repaired(CompoundTag entry, boolean home, long save) {
    }

    /**
     * What a repaired entry is kept by: its cell, and for a standing block the block that stood there (a home has none).
     * The newest repair of a key replaces an older one: the recovery puts the same cell right with either.
     */
    private record RepairKey(BlockPos pos, @Nullable Block placed) {
    }

    private final BossFight fight;
    private final PriorityQueue<Home> due = new PriorityQueue<>(Comparator.<Home>comparingLong(h -> h.dueAt)
            .thenComparingInt(h -> h.pos.getY()).thenComparingLong(h -> h.order));
    private final Map<BlockPos, Home> homes = new HashMap<>();
    private final PriorityQueue<Standing> expiring = new PriorityQueue<>(Comparator.comparingLong(Standing::until)
            .thenComparingLong(Standing::order));
    private final Map<BlockPos, Standing> standing = new HashMap<>();
    private final Map<UUID, Integer> standingOf = new HashMap<>();
    /** The borrowed blocks held or flying. */
    private final Set<UUID> loose = new HashSet<>();
    /** The players told once that a block is borrowed. */
    private final Set<UUID> told = new HashSet<>();
    /** The repairs two saves do not hold yet, one per cell (a standing block's per cell and block): see {@link #keep}. */
    private final Map<RepairKey, Repaired> repaired = new LinkedHashMap<>();
    private long order;
    /** Whether a snapshot was ever handed to the journal (there is a file to keep up to date, or to delete). */
    private boolean written;
    /** Whether the journal is behind the ledger (a mend, a crumble). */
    private boolean dirty;
    private long lastSnapshot = Long.MIN_VALUE / 2;
    private boolean finished;

    ArenaLoans(BossFight fight) {
        this.fight = fight;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The loan

    /**
     * Why the arena does not lend the block at {@code pos} (the fight's own reasons and the switch were asked first, {@link
     * BossFights#lendRefusal}), or null: half of a door ({@code protected}: two cells cannot be one loan), a falling block
     * over a gap ({@code arena_hanging}), the block under an open home or a standing borrowed block ({@code arena_mending}),
     * the fight's cap ({@code arena_full}). A standing borrowed block is always lent again: it is the same loan.
     */
    @Nullable
    String refusal(BlockPos pos) {
        if (standing.containsKey(pos)) {
            return null;
        }
        ServerLevel level = fight.level();
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return PROTECTED;
        }
        if (state.getBlock() instanceof Fallable && FallingBlock.isFree(level.getBlockState(pos.below()))) {
            return HANGING; // sand over a cave, gravel over water: it would fall the moment the block under it moved
        }
        BlockPos above = pos.above();
        if (homes.containsKey(above) || standing.containsKey(above)) {
            return MENDING;
        }
        if (borrowed() >= BossConfig.MAX_BORROWED_BLOCKS.get()) {
            return FULL;
        }
        return null;
    }

    /**
     * Lends the block at {@code pos} ({@code state}) to {@code player}: the journal is told first (unless it is a copy of the
     * island), the cell becomes what the block leaves behind (air, or water for a waterlogged block), quietly, with the
     * sound and the dust of a broken block, and a home is recorded. A standing borrowed block lifted again gives its cell
     * back to what was there before it, and is the same loan (no new home). The first loan of a player in a fight tells
     * them the block is borrowed.
     *
     * @return the borrowed block, already in the level and the fight's, for the player to take hold of
     */
    TelekineticBlockEntity lend(ServerPlayer player, BlockPos pos, BlockState state) {
        ServerLevel level = fight.level();
        long now = level.getGameTime();
        BlockPos cell = pos.immutable();
        Standing stood = standing.get(cell);
        if (stood != null) {
            unstand(stood, now);
            set(level, cell, stood.previous());
        } else {
            Home home = new Home(cell, state, fight.isCopyCell(cell), order++, now + BossConfig.ARENA_MEND_TICKS.get());
            homes.put(cell, home);
            due.add(home);
            if (!home.copy) {
                hand(now); // written ahead: the journal knows of the hole before it is made
            }
            set(level, cell, state.getFluidState().createLegacyBlock());
        }
        level.levelEvent(2001, cell, Block.getId(state));
        TelekineticBlockEntity block = fight.adopt(new TelekineticBlockEntity(level, cell, BlockLifting.heldState(state), player));
        level.addFreshEntity(block);
        loose.add(block.getUUID());
        if (told.add(player.getUUID())) {
            player.displayClientMessage(Component.translatable(BORROWED), true);
        }
        return block;
    }

    /**
     * A borrowed block of this fight was set down (or let go) and has landed at {@code pos}: it stands there, a block again,
     * for {@code boss.borrowedBlockTicks} (the journal told first), if the stand rules let it (D-T 5.4): the cell is in the
     * fight's arena and not ground the clients hide, it takes the block and there is room, it is not on another standing
     * borrowed block, no living thing is within a block of it, and its owner (who let go of it) has fewer than {@code
     * boss.maxStandingPerPlayer} standing. Otherwise the owner, if online, is told that the arena takes it back.
     *
     * @return whether it stands (the caller discards the entity either way, and lets it crumble if not)
     */
    boolean stand(TelekineticBlockEntity block, BlockPos pos) {
        ServerLevel level = fight.level();
        loose.remove(block.getUUID());
        BlockPos cell = pos.immutable();
        BlockState placed = standingState(BlockLifting.landingState(level, cell, block.getBlockState()));
        BlockState here = level.getBlockState(cell);
        UUID owner = block.thrower();
        boolean stands = owner != null && block.level() == level && fight.arena().protects(cell) && !BossFights.hidesGround(level, cell)
                && here.canBeReplaced(new DirectionalPlaceContext(level, cell, Direction.DOWN, ItemStack.EMPTY, Direction.UP))
                && placed.canSurvive(level, cell) && level.isUnobstructed(placed, cell, CollisionContext.empty())
                && !standing.containsKey(cell.below()) && !nearAnyone(level, cell)
                && standingOf.getOrDefault(owner, 0) < BossConfig.MAX_STANDING_PER_PLAYER.get();
        if (!stands) {
            ServerPlayer thrower = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
            if (thrower != null) {
                thrower.displayClientMessage(Component.translatable(CRUMBLES), true);
            }
            return false;
        }
        long now = level.getGameTime();
        Standing stood = new Standing(cell, placed, here, now + BossConfig.BORROWED_BLOCK_TICKS.get(), owner, order++, fight.isCopyCell(cell));
        standing.put(cell, stood);
        expiring.add(stood);
        standingOf.merge(owner, 1, Integer::sum);
        hand(now); // written ahead, as at a loan
        set(level, cell, placed);
        level.playSound(null, cell, placed.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    /** A borrowed block of this fight is gone (it shattered, crumbled, or was discarded): it no longer counts. */
    void gone(Entity block) {
        loose.remove(block.getUUID());
    }

    /**
     * The state a borrowed block stands as: leaves set down never decay into saplings (a hanging lantern with nothing to
     * hang from already stands on the ground, {@link BlockLifting#landingState}).
     */
    private static BlockState standingState(BlockState state) {
        return state.hasProperty(LeavesBlock.PERSISTENT) ? state.setValue(LeavesBlock.PERSISTENT, true) : state;
    }

    /** Whether a living thing's box, grown by a block, touches the cell: no walls or roofs next to anyone. */
    private static boolean nearAnyone(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.0), e -> e.isAlive() && !e.isSpectator()).isEmpty();
    }

    /** Whether a living thing's box overlaps the cell. */
    private static boolean occupied(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), e -> e.isAlive() && !e.isSpectator()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Every tick

    /**
     * One tick of the fight (running or winding down): the homes that are due mend, the standing blocks whose time is over
     * crumble, the last two seconds of a standing block let dust fall, the borrowed blocks that are gone are forgotten, and a
     * journal that is behind gets a snapshot (at most once a second).
     */
    void tick(long now) {
        if (homes.isEmpty() && standing.isEmpty() && loose.isEmpty() && !dirty) {
            return;
        }
        ServerLevel level = fight.level();
        List<Home> later = new ArrayList<>();
        while (!due.isEmpty() && due.peek().dueAt <= now) {
            Home home = due.poll();
            if (mendable(level, home)) {
                mend(level, home, now);
            } else {
                home.dueAt = now + RETRY_TICKS; // it waits for its own retry, never holding up the homes behind it
                later.add(home);
            }
        }
        due.addAll(later);
        while (!expiring.isEmpty() && expiring.peek().until() <= now) {
            crumble(level, expiring.poll(), now);
        }
        if (now % DUST_INTERVAL == 0) {
            for (Standing stood : standing.values()) {
                if (stood.until() - now <= DUST_TICKS) {
                    BlockPos pos = stood.pos();
                    level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, stood.placed()),
                            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 4, 0.3, 0.0, 0.3, 0.0);
                }
            }
        }
        if (now % PRUNE_INTERVAL == 0) {
            loose.removeIf(id -> {
                Entity block = level.getEntity(id);
                return block == null || block.isRemoved();
            });
        }
        if (dirty && now - lastSnapshot >= SNAPSHOT_INTERVAL) {
            hand(now);
        }
    }

    /**
     * Whether the home can mend now: its cell is still a hole ({@link #isHole}: air, a fluid, or what the weather put in it)
     * or holds a standing borrowed block, which it takes back; no living thing stands in it; and a falling block is not put
     * back over another open home.
     */
    private boolean mendable(ServerLevel level, Home home) {
        BlockPos pos = home.pos;
        if (!level.isLoaded(pos)) {
            return false; // asked first: reading the cell of an unloaded chunk would load it
        }
        boolean open = isHole(level.getBlockState(pos)) || standing.containsKey(pos);
        if (!open || occupied(level, pos)) {
            return false;
        }
        return !(home.state.getBlock() instanceof Fallable) || !homes.containsKey(pos.below());
    }

    /**
     * Whether a cell a borrowed block left is still a hole for its block to come back into: air, a fluid, or what the world
     * puts into an open hole by itself, which the block replaces as a placed block would ({@link BlockState#canBeReplaced}:
     * the snow that fell into it, a vine or fire that spread into it), or the water the hole kept turned to ice. Asked by
     * the mend and by the crash recovery ({@link ArenaLoansRecovery}).
     */
    public static boolean isHole(BlockState here) {
        return here.isAir() || here.getBlock() instanceof LiquidBlock || here.canBeReplaced()
                || here.is(Blocks.ICE) || here.is(Blocks.FROSTED_ICE);
    }

    /** The home's block is back: a standing borrowed block in the hole is taken back with it. */
    private void mend(ServerLevel level, Home home, long now) {
        BlockPos pos = home.pos;
        Standing stood = standing.get(pos);
        if (stood != null) {
            unstand(stood, now);
        }
        homes.remove(pos);
        set(level, pos, home.state);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, pos, home.state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.7F, 1.2F);
        if (!home.copy) {
            keep(new RepairKey(pos, null), new Repaired(homeEntry(home), true, saves(level)));
            dirty = true;
        }
    }

    /** A standing block's time is over (or the fight gives everything back): the cell is what was there before. */
    private void crumble(ServerLevel level, Standing stood, long now) {
        unstand(stood, now);
        level.levelEvent(2001, stood.pos(), Block.getId(stood.placed()));
        set(level, stood.pos(), stood.previous());
    }

    /** Takes a standing block off the ledger (its cell is about to be given back): its journal entry is now a repair. */
    private void unstand(Standing stood, long now) {
        standing.remove(stood.pos());
        expiring.remove(stood);
        standingOf.computeIfPresent(stood.owner(), (owner, count) -> count > 1 ? count - 1 : null);
        keep(new RepairKey(stood.pos(), stood.placed().getBlock()),
                new Repaired(standingEntry(stood), false, saves(fight.level())));
        dirty = true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Giving everything back

    /**
     * Puts everything back in this tick (D-T 5.7): every standing block crumbles, every home mends, lowest first (a living
     * thing standing in a hole is lifted onto the block first), and every borrowed block still held or flying crumbles to
     * dust; each write quiet. A home mends whatever has come into its hole meanwhile (the arena lets nobody put a block
     * there; what the world put in it, a flower a bone meal grew, goes without a drop): the ground is whole again. The
     * journal gets the snapshot of it all (every entry a repair now, R48). At most {@code maxBorrowedBlocks} standing blocks
     * and the open homes.
     */
    public void returnAll() {
        ServerLevel level = fight.level();
        long now = level.getGameTime();
        for (Standing stood : List.copyOf(standing.values())) {
            crumble(level, stood, now);
        }
        List<Home> open = new ArrayList<>(homes.values());
        open.sort(Comparator.<Home>comparingInt(h -> h.pos.getY()).thenComparingLong(h -> h.order));
        for (Home home : open) {
            liftOut(level, home.pos, home.state);
            mend(level, home, now);
        }
        homes.clear();
        due.clear();
        for (UUID id : List.copyOf(loose)) {
            if (level.getEntity(id) instanceof TelekineticBlockEntity block) {
                block.crumble();
            }
        }
        loose.clear();
        if (written) {
            hand(now);
        }
    }

    /** Lifts every living thing that stands in the cell onto the top of the block about to fill it. */
    static void liftOut(ServerLevel level, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getCollisionShape(level, pos);
        double top = pos.getY() + (shape.isEmpty() ? 0.0 : shape.max(Direction.Axis.Y));
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), e -> e.isAlive() && !e.isSpectator())) {
            if (living.getY() < top) {
                living.teleportTo(living.getX(), top, living.getZ());
            }
        }
    }

    /**
     * The fight is finished: everything goes back ({@link #returnAll}), and the ledger keeps its journal only until two saves
     * of its dimension hold the repairs (R48); a journal with nothing left to keep is deleted at once.
     */
    void finish() {
        if (finished) {
            return;
        }
        returnAll();
        finished = true;
        if (repaired.isEmpty()) {
            if (written) {
                ArenaJournalWriter.delete(fight.server(), fight.id());
            }
        } else {
            WAITING.add(this);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The journal

    /**
     * Keeps a repair until two saves of the dimension hold it (R48), as the only entry of its cell (a standing block's, of
     * its cell and block): a newer repair of the same replaces the older, with its own count of saves. A cell lent, mended
     * and lent again and again (a hero throwing borrowed blocks at a shield) is one entry, so the list is never longer than
     * the cells repaired since the second-last save, and each snapshot copies no more.
     */
    private void keep(RepairKey key, Repaired repair) {
        repaired.remove(key);
        repaired.put(key, repair);
    }

    /** Hands the journal a snapshot of the ledger now. */
    private void hand(long now) {
        ArenaJournalWriter.submit(fight.server(), fight.id(), snapshot());
        written = true;
        dirty = false;
        lastSnapshot = now;
    }

    /**
     * What the journal holds for this ledger now: {@code {version, dimension, homes: [{x, y, z, state}], standing: [{x, y,
     * z, placed, previous}]}}, the open homes (not the island's copies) and the standing blocks (on a copy cell with air
     * before them), and every repair that two saves do not hold yet.
     */
    public CompoundTag snapshot() {
        ListTag homeList = new ListTag();
        ListTag standingList = new ListTag();
        for (Home home : homes.values()) {
            if (!home.copy) {
                homeList.add(homeEntry(home));
            }
        }
        for (Standing stood : standing.values()) {
            standingList.add(standingEntry(stood));
        }
        for (Repaired repair : repaired.values()) {
            (repair.home() ? homeList : standingList).add(repair.entry().copy());
        }
        CompoundTag journal = new CompoundTag();
        journal.putInt("version", VERSION);
        journal.putString("dimension", fight.level().dimension().location().toString());
        journal.put("homes", homeList);
        journal.put("standing", standingList);
        return journal;
    }

    private static CompoundTag homeEntry(Home home) {
        CompoundTag entry = cell(home.pos);
        entry.put("state", NbtUtils.writeBlockState(home.state));
        return entry;
    }

    private static CompoundTag standingEntry(Standing stood) {
        CompoundTag entry = cell(stood.pos());
        entry.put("placed", NbtUtils.writeBlockState(stood.placed()));
        // On the island's copy: nothing before it, so a crash recovery leaves only air there for the island's own to clear.
        entry.put("previous", NbtUtils.writeBlockState(stood.copy() ? Blocks.AIR.defaultBlockState() : stood.previous()));
        return entry;
    }

    private static CompoundTag cell(BlockPos pos) {
        CompoundTag entry = new CompoundTag();
        entry.putInt("x", pos.getX());
        entry.putInt("y", pos.getY());
        entry.putInt("z", pos.getZ());
        return entry;
    }

    /**
     * The repairs that two saves of the dimension now hold leave the journal (R48): true if any did. The journal of a running
     * fight gets its new snapshot at once.
     */
    private boolean retire(long saves) {
        if (!repaired.values().removeIf(repair -> saves - repair.save() >= SAVES_TO_RETIRE)) {
            return false;
        }
        if (!finished || !repaired.isEmpty()) {
            hand(fight.level().getGameTime());
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // The saves of the world (R48)

    /**
     * The dimension was saved ({@code LevelEvent.Save}, posted once its chunks are queued to the disk): its count goes up, and
     * every repair of its arenas made two saves ago or more leaves its journal; a finished fight's journal that keeps nothing
     * any more is deleted, and so is a journal a crash recovery restored two saves ago.
     */
    public static void onSave(ServerLevel level) {
        long saves = SAVES.merge(level.dimension(), 1L, Long::sum);
        for (BossFight fight : BossFights.all(level.getServer())) {
            if (fight.level() == level) {
                fight.loans().retire(saves);
            }
        }
        Iterator<ArenaLoans> it = WAITING.iterator();
        while (it.hasNext()) {
            ArenaLoans loans = it.next();
            if (loans.fight.level() == level && loans.retire(saves) && loans.repaired.isEmpty()) {
                ArenaJournalWriter.delete(level.getServer(), loans.fight.id());
                it.remove();
            }
        }
        ArenaLoansRecovery.onSave(level, saves);
    }

    /** How many times the dimension has been saved since the server started. */
    public static long saves(ServerLevel level) {
        return SAVES.getOrDefault(level.dimension(), 0L);
    }

    /** The server has stopped, after its flushed save: the journals of the finished fights keep nothing any more. */
    static void retireAll(MinecraftServer server) {
        for (ArenaLoans loans : WAITING) {
            loans.repaired.clear();
            ArenaJournalWriter.delete(server, loans.fight.id());
        }
        WAITING.clear();
    }

    /** Whether the fight of this id is known to this server: running, or finished with a journal waiting for its saves. */
    static boolean knows(MinecraftServer server, UUID fight) {
        for (BossFight running : BossFights.all(server)) {
            if (running.id().equals(fight)) {
                return true;
            }
        }
        for (ArenaLoans loans : WAITING) {
            if (loans.fight.id().equals(fight)) {
                return true;
            }
        }
        return false;
    }

    /** Forgets every save count and every waiting journal (the server has stopped). */
    static void clear() {
        SAVES.clear();
        WAITING.clear();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Helpers

    /** Writes a block as the ledger does: the clients told, no neighbour bothered, nothing dropped, and made quiet. */
    static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, FLAGS);
        quiet(level, pos);
    }

    /**
     * Removes the cell's scheduled block ticks: whatever the flags, placing a block calls its {@code onPlace}, where a
     * falling block (sand, gravel, concrete powder, an anvil) and scaffolding schedule their fall. A quiet cell keeps what
     * was written there.
     */
    public static void quiet(ServerLevel level, BlockPos pos) {
        level.getBlockTicks().clearArea(BoundingBox.fromCorners(pos, pos));
    }

    // ---------------------------------------------------------------------------------------------------------
    // What the ledger holds (for the fight's own rules and for tests)

    /** How many borrowed blocks are out: held, flying or standing (the homes do not count). */
    public int borrowed() {
        return loose.size() + standing.size();
    }

    /** Whether a home is open at {@code pos} (a borrowed block left it, and it has not mended yet). */
    public boolean isOpenHome(BlockPos pos) {
        return homes.containsKey(pos);
    }

    /** Whether a borrowed block stands at {@code pos}. */
    public boolean isStanding(BlockPos pos) {
        return standing.containsKey(pos);
    }

    /** Records a home at {@code pos} as a loan of {@code state} would, without lifting anything (D-T test 52 e). */
    @VisibleForTesting
    public void recordHomeForTests(BlockPos pos, BlockState state) {
        long now = fight.level().getGameTime();
        Home home = new Home(pos.immutable(), state, fight.isCopyCell(pos), order++, now + BossConfig.ARENA_MEND_TICKS.get());
        homes.put(home.pos, home);
        due.add(home);
    }
}
