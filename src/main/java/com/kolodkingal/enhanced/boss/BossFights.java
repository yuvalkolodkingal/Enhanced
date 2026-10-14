package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * The one door the rest of the mod uses to ask about boss fights: whoever is in one, what a fight protects, where a
 * fight lets a teleport go, how to start and end one. The heroes (Magneto, Wanda, Tommy, Loki, Spider-Man, the Phoenix)
 * call these static methods and never a boss class, so a boss can change without touching them.
 *
 * <p>This class also keeps the fights that run (at most {@code [boss].maxActiveFights} on the whole server; none
 * survives a server stop) and the {@link BossLane}s that make them: a boss lane registers its lane with {@link
 * #register}, and a kind with no lane yet refuses politely. With no fight running, every question answers as if there
 * were no boss at all ({@link #isMinion} and {@link #protects} are false, {@link #allowsTeleport} true, and so on).
 */
public final class BossFights {
    private static final Map<BossKind, BossLane> LANES = new EnumMap<>(BossKind.class);
    private static final List<BossFight> FIGHTS = new ArrayList<>();
    private static final List<Predicate<Entity>> SWEEP_EXEMPT = new CopyOnWriteArrayList<>();

    // ---------------------------------------------------------------------------------------------------------
    // The lanes

    /**
     * Registers the lane that makes the fights against {@code kind} (the boss lanes do it once, when the mod loads).
     * Registering again replaces the lane.
     *
     * @return the lane that was registered before, or null
     */
    @Nullable
    public static BossLane register(BossKind kind, BossLane lane) {
        return LANES.put(kind, lane);
    }

    /** Takes the lane away (a test that registered its own puts the old one back). */
    @Nullable
    public static BossLane unregister(BossKind kind) {
        return LANES.remove(kind);
    }

    @Nullable
    public static BossLane lane(BossKind kind) {
        return LANES.get(kind);
    }

    /**
     * An entity of a boss type that the orphan sweep must leave alone though no fight owns it. (Anything with an owner
     * already is: the Ultron hero's drones are Sentries of the same type as the boss's, and a projectile a player shot
     * is a hero's, such as the Ultron hero's bolt or the Apocalypse hero's sand lance.)
     */
    public static void exemptFromSweep(Predicate<Entity> exempt) {
        SWEEP_EXEMPT.add(exempt);
    }

    /** Takes back an exemption given with {@link #exemptFromSweep} (a test that gave its own). */
    public static void stopExempting(Predicate<Entity> exempt) {
        SWEEP_EXEMPT.remove(exempt);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Who is in a fight

    /** The fight running in this dimension (the oldest, if there are several), or null. */
    @Nullable
    public static BossFight active(ServerLevel level) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == level) {
                return fight;
            }
        }
        return null;
    }

    /** Every fight of the server, running or winding down. */
    public static List<BossFight> all(MinecraftServer server) {
        List<BossFight> fights = new ArrayList<>();
        for (BossFight fight : FIGHTS) {
            if (fight.server() == server) {
                fights.add(fight);
            }
        }
        return fights;
    }

    /** Whether a fight is running anywhere on the server. */
    public static boolean anyActive(MinecraftServer server) {
        for (BossFight fight : FIGHTS) {
            if (fight.server() == server) {
                return true;
            }
        }
        return false;
    }

    /** Whether the entity is in a fight: a fighter, or something the fight made. */
    public static boolean isMember(Entity entity) {
        if (FIGHTS.isEmpty()) {
            return false;
        }
        for (BossFight fight : FIGHTS) {
            if (fight.isMember(entity)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the player took part in any fight of the server, running or winding down (they may be far away now). */
    public static boolean isParticipant(ServerPlayer player) {
        for (BossFight fight : FIGHTS) {
            if (fight.isParticipant(player.getUUID())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the item is the drop of a fighter who died in an arena that keeps drops: it waits there for them until the
     * fight is over, and nothing else may take it (the Magnetic Pull).
     */
    public static boolean isKeptDrop(ItemEntity item) {
        for (BossFight fight : FIGHTS) {
            if (fight.keepsDrop(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the entity is one that only a boss fight makes (a boss, a minion, one of their projectiles or the key),
     * whether or not a running fight owns it: what {@code /enhanced boss cleanup} removes, and what the orphan sweep of
     * the boss framework looks for.
     */
    public static boolean isFightEntity(Entity entity) {
        EntityType<?> type = entity.getType();
        return type == ModEntities.THANOS.get() || type == ModEntities.THANOS_SWORD.get() || type == ModEntities.THANOS_METEOR.get()
                || type == ModEntities.THANOS_ORB.get() || type == ModEntities.THANOS_PHANTOM.get()
                || type == ModEntities.ULTRON.get() || type == ModEntities.ULTRON_SENTRY.get()
                || type == ModEntities.VIBRANIUM_KEY.get() || type == ModEntities.ULTRON_BOLT.get()
                || type == ModEntities.APOCALYPSE.get() || type == ModEntities.SAND_LANCE.get()
                || type == ModEntities.SAND_BOULDER.get() || type == ModEntities.SAND_COCOON.get()
                || (entity instanceof TelekineticBlockEntity && isMarked(entity)); // a block an arena lent
    }

    /** The fight a (not yet finished) entity of the fight's marks belongs to, or null. */
    @Nullable
    public static BossFight fightOf(Entity entity) {
        if (FIGHTS.isEmpty()) {
            return null;
        }
        UUID mark = BossFight.markOf(entity);
        if (mark == null) {
            return null;
        }
        for (BossFight fight : FIGHTS) {
            if (fight.id().equals(mark)) {
                return fight;
            }
        }
        return null;
    }

    /**
     * Whether the entity is a marked minion of a running fight (an Ultron Sentry, a Phantom, a Horseman's Husk): an
     * entity the fight made that is not the boss itself.
     */
    public static boolean isMinion(Entity entity) {
        return fightOf(entity) != null && !entity.getType().is(ModTags.BOSSES);
    }

    /** Whether two entities fight on the same side of the same boss fight (the boss and its minions). */
    public static boolean sameFight(Entity a, Entity b) {
        BossFight fight = fightOf(a);
        return fight != null && fight == fightOf(b);
    }

    /** Whether the Hex lets this entity through its wall while a lift carries it (called from the Hex's judgement). */
    public static boolean isHexExempt(Entity entity) {
        for (BossFight fight : FIGHTS) {
            if (fight.hexExempt() && fight.isMember(entity)) {
                return true;
            }
        }
        return false;
    }

    /** Whether an entity of a boss type has no running fight: the orphan sweep discards it when it joins a level. */
    static boolean isOrphan(Entity entity) {
        if (!isFightEntity(entity)) {
            return false;
        }
        if (fightOf(entity) != null || (entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null)) {
            return false;
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof Player) {
            return false; // a hero's own bolt, lance or boulder (a boss's are marked by its fight)
        }
        for (Predicate<Entity> exempt : SWEEP_EXEMPT) {
            if (exempt.test(entity)) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------
    // What a fight does to the rest of the mod

    /**
     * Whether the arena of a fight lets this entity teleport to {@code to} in {@code level} (called from every
     * teleport's gate): a fighter inside the arena cannot leave it, by a pearl, the Bifrost, Vanish, a portal.
     */
    public static boolean allowsTeleport(ServerLevel level, Entity entity, Vec3 to) {
        for (BossFight fight : FIGHTS) {
            if (!fight.arena().allowsTeleport(entity, level, to) && fight.isMember(entity)) {
                return false;
            }
        }
        return true;
    }

    /** Tells a player that the arena would not let them go (what a refused teleport says). */
    public static void tellHeld(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.enhanced.boss.held"), true);
        }
    }

    /**
     * Whether a fight protects the block at {@code pos}: an ability that would change it (lifting it, shaking it
     * loose, burning it, freezing the water over it, spinning a web on it) must not.
     */
    public static boolean protects(ServerLevel level, BlockPos pos) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == level && fight.arena().protects(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code pos} lies in the cylinder of any fight of this level, protected or not (Thanos's arena included): the
     * two mass edits of 3.1, Build Magic and the Magnetic Pull, change nothing there while {@code
     * build_magic.allArenasProtected} is on.
     */
    public static boolean inAnyArena(ServerLevel level, BlockPos pos) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == level && fight.arena().contains(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether any block of {@code box} lies in the cylinder of a fight of this level (as {@link #inAnyArena} asks of each
     * one): a spell at work checks it as it goes on, and stops if a fight began where it builds.
     */
    public static boolean overlapsAny(ServerLevel level, BoundingBox box) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == level && meets(fight.arena(), box)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the middle of some cell of the box is inside the arena's cylinder (see {@link BossArena#contains}). */
    private static boolean meets(BossArena arena, BoundingBox box) {
        if (box.maxY() + 0.5 < arena.bottom() || box.minY() + 0.5 > arena.top() + 1.0) {
            return false;
        }
        // The cell of the box whose middle is nearest the centre, column by column: the one the centre stands in, or an edge.
        Vec3 center = arena.center();
        double x = Mth.clamp(Math.floor(center.x), box.minX(), box.maxX()) + 0.5;
        double z = Mth.clamp(Math.floor(center.z), box.minZ(), box.maxZ()) + 0.5;
        return Math.hypot(x - center.x, z - center.z) <= arena.radius();
    }

    /**
     * Whether the clients hide the ground at {@code pos} while a fight runs (under Ultron's island in the sky): nothing
     * lands there or is borrowed from there, and neither Build Magic nor the Magnetic Pull changes it.
     */
    public static boolean hidesGround(ServerLevel level, BlockPos pos) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == level && fight.hidesGround(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the block at {@code pos} leans on a block an arena has lent and whose hole is still open (a torch on it, a
     * flower, a lantern under it): if its neighbours break it meanwhile it drops nothing, as the mend puts it back
     * ({@link ArenaLoans#isPropped}).
     */
    public static boolean leansOnALoan(ServerLevel level, BlockPos pos) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == level && fight.loans().isPropped(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code pos} is a cell of a copy a fight made of the world (Ultron's island in the sky): protected whatever the
     * config says, and never journaled as a hole (see {@link BossFight#isCopyCell}).
     */
    public static boolean isCopyCell(ServerLevel level, BlockPos pos) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == level && fight.isCopyCell(pos)) {
                return true;
            }
        }
        return false;
    }

    // ---- the arena's loans to block telekinesis (D-T 5) ----

    /** The fight whose arena protects {@code pos}: the one that lends its block. */
    @Nullable
    private static BossFight lender(ServerLevel level, BlockPos pos) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == level && !fight.isFinished() && fight.arena().protects(pos)) {
                return fight;
            }
        }
        return null;
    }

    /**
     * Why the arena that protects {@code pos} does not lend its block to {@code player}'s block telekinesis, as the key he
     * reads; null if it lends it (the player's rights are asked after this, {@link #lending}), or if no fight protects the
     * cell (it is no loan). In this order: {@code boss.lendArenaBlocks} off ({@code telekinesis.protected}, as in 3.0); the
     * fight's own rule ({@link BossFight#lendRefusal}); the ledger's ({@link ArenaLoans}: half of a door, a falling block
     * over a gap, the block under an open home or a standing borrowed block or leaning on an open home, a block that holds
     * up too much or that something hangs on, the fight's cap).
     */
    @Nullable
    public static String lendRefusal(ServerPlayer player, BlockPos pos) {
        BossFight fight = lender(player.serverLevel(), pos);
        if (fight == null) {
            return null;
        }
        if (!BossConfig.LEND_ARENA_BLOCKS.get()) {
            return ArenaLoans.PROTECTED;
        }
        String refusal = fight.lendRefusal(pos);
        return refusal != null ? refusal : fight.loans().refusal(pos);
    }

    /** The cell a loan is asking about, while it asks ({@link #lending}). */
    @Nullable
    private static BlockPos lending;

    /**
     * Runs {@code ask} (the break event of a loan, which claim mods hear) while the arena lets the one cell at {@code pos}
     * through ({@link #isLending}): the arena's own listener does not cancel it, so the claims decide.
     *
     * @return what {@code ask} answered
     */
    public static boolean lending(BlockPos pos, BooleanSupplier ask) {
        BlockPos before = lending;
        lending = pos.immutable();
        try {
            return ask.getAsBoolean();
        } finally {
            lending = before;
        }
    }

    /** Whether the break of the block at {@code pos} is a loan being asked about ({@link #lending}). */
    public static boolean isLending(BlockPos pos) {
        return pos.equals(lending);
    }

    /**
     * Lends the block at {@code pos} (it is {@code state}) to {@code player}: the cell is emptied and the hole recorded by the
     * fight whose arena protects it ({@link ArenaLoans}). Asked only after {@link #lendRefusal} and the player's rights.
     *
     * @return the borrowed block, in the level and the fight's, or null if no fight protects the cell
     */
    @Nullable
    public static TelekineticBlockEntity lend(ServerPlayer player, BlockPos pos, BlockState state) {
        BossFight fight = lender(player.serverLevel(), pos);
        return fight == null ? null : fight.loans().lend(player, pos, state);
    }

    /**
     * A borrowed block landed at {@code pos} after a set-down: whether it stands there under its arena's rules ({@link
     * ArenaLoans}); false (it crumbles) if they refuse it or its fight is gone.
     */
    public static boolean standBorrowed(TelekineticBlockEntity block, BlockPos pos) {
        BossFight fight = fightOf(block);
        return fight != null && !fight.isFinished() && fight.loans().stand(block, pos);
    }

    /** A borrowed block is gone (it shattered or crumbled): its fight no longer counts it. */
    public static void borrowedGone(TelekineticBlockEntity block) {
        BossFight fight = fightOf(block);
        if (fight != null) {
            fight.loans().gone(block);
        }
    }

    /** Whether the entity carries the mark of a fight, running or not (a borrowed block does, all its life). */
    public static boolean isMarked(Entity entity) {
        return BossFight.markOf(entity) != null;
    }

    /**
     * The entity leaves its fight: its mark is taken off, so that nothing of the boss's side spares it any more and the
     * fight no longer counts it as its own (D-T 6.1, 6.3: a bolt of Ultron's or a lance of Apocalypse's that telekinesis
     * caught and threw back is the thrower's now). Its owner must be someone else by then (a player), or the orphan sweep
     * would take it at its next join.
     */
    public static void disown(Entity entity) {
        BossFight.unmark(entity);
    }

    /**
     * Whether a fight holds the entity where it is (whatever the Sokovia lift carries): no telekinesis may take it, and a
     * hero it carries uses none.
     */
    public static boolean isLocked(Entity entity) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == entity.level() && fight.locks(entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Where telekinesis may carry {@code entity} when its holder wants it at {@code wanted} (D-T 4.5): anything a fight
     * owns, and its participants while they are in its arena, are never carried out of the arena ({@link
     * BossFight#carryLimit}); anything else goes where it is wanted.
     */
    public static Vec3 carryLimit(Entity entity, Vec3 wanted) {
        BossFight fight = carrier(entity);
        return fight == null ? wanted : fight.carryLimit(entity, wanted);
    }

    /**
     * The flight of a throw of {@code entity} is over: if it is the boss or one of his and it ended outside its carry
     * limit, its fight brings it back into the arena ({@link BossFight#bringBack}). Players are left to the arena's own
     * field and wall.
     *
     * @return whether a fight brought it back
     */
    public static boolean bringBack(Entity entity) {
        BossFight fight = fightOf(entity);
        if (fight == null || fight.isOver() || fight.level() != entity.level()) {
            return false;
        }
        Vec3 at = entity.position();
        if (fight.carryLimit(entity, at).distanceToSqr(at) < 1.0E-4) {
            return false;
        }
        fight.bringBack(entity);
        return true;
    }

    /**
     * Where a telekinetic flight of {@code entity} is stopped dead, if it would reach the edge of a gated arena (Ultron's
     * field, Apocalypse's wall) of a fight that owns it or that it takes part in at {@code next}, where it flies to (D-T
     * 4.5): half a block inside {@code containRadius}, on the line from the centre, at the height it is at. Null if it may
     * fly on.
     */
    @Nullable
    public static Vec3 edgeStop(Entity entity, Vec3 next) {
        BossFight fight = carrier(entity);
        if (fight == null || !fight.arena().gates()) {
            return null;
        }
        BossArena arena = fight.arena();
        double flat = arena.distance(next);
        if (flat < arena.containRadius() || arena.distance(entity.position()) >= arena.containRadius()) {
            return null; // it stays inside, or it was already outside (the field and the wall see to that)
        }
        Vec3 center = arena.center();
        double scale = (arena.containRadius() - 0.5) / flat;
        return new Vec3(center.x + (next.x - center.x) * scale, entity.getY(), center.z + (next.z - center.z) * scale);
    }

    /**
     * The fight of this level whose arena keeps {@code entity} in: the one that owns it, or the one it takes part in (a
     * player) while it is in that arena's cylinder (a fighter far away is not pulled back by a friend's grip); or null.
     */
    @Nullable
    private static BossFight carrier(Entity entity) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() != entity.level() || fight.isFinished()) {
                continue;
            }
            if (entity instanceof ServerPlayer player ? fight.isParticipant(player.getUUID()) && fight.arena().contains(player.position())
                    : fight.owns(entity)) {
                return fight;
            }
        }
        return null;
    }

    /**
     * Wanda pressed her telekinesis key while holding nothing: a boss's special case for it (Ultron's heart). True if
     * handled.
     */
    public static boolean telekinesisPress(ServerPlayer wanda) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == wanda.level() && fight.onTelekinesisPress(wanda)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wanda or Magneto ({@code mode}) throws what they hold: a boss's special case for it (a Sentry is crushed by either, a
     * heart torn out by her hand alone, R40). True if handled (the held thing is then not thrown).
     */
    public static boolean telekinesisLaunch(ServerPlayer holder, Telekinesis.Mode mode, Entity held) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == holder.level() && fight.onTelekinesisLaunch(holder, mode, held)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Starting and ending

    /**
     * Starts a fight against {@code kind}, called by the rituals and by {@code /enhanced boss start}. Whatever stops it
     * is told to {@code starter} in the action bar. A boss with no lane registered yet is declined, and so is any start
     * while the difficulty is Peaceful, while {@code maxActiveFights} fights run, or (unless forced) while the boss
     * rests; the lane's own checks come last, before anything is used up.
     *
     * @param force an operator's start, which skips the ritual's own checks (the site, the items, a hero nearby)
     * @return whether a fight began
     */
    public static boolean start(BossKind kind, ServerPlayer starter, boolean force) {
        BossLane lane = LANES.get(kind);
        if (lane == null) {
            refuse(starter, Component.translatable("message.enhanced.boss.unavailable"), ChatFormatting.GRAY);
            return false;
        }
        ServerLevel level = starter.serverLevel();
        MinecraftServer server = level.getServer();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            refuse(starter, Component.translatable("message.enhanced.boss.peaceful", kind.displayName()), ChatFormatting.RED);
            return false;
        }
        List<BossFight> running = all(server);
        if (running.size() >= BossConfig.MAX_ACTIVE_FIGHTS.get()) {
            BossFight fight = running.get(0);
            Vec3 at = fight.arena().center();
            refuse(starter, Component.translatable("message.enhanced.boss.busy", fight.kind().displayName(), (int) at.x, (int) at.z),
                    ChatFormatting.RED);
            return false;
        }
        long resting = restLeft(server, kind);
        if (!force && resting > 0) {
            refuse(starter, Component.translatable("message.enhanced.boss.resting", kind.displayName(), (resting + 1199) / 1200),
                    ChatFormatting.RED);
            return false;
        }
        Component refusal = lane.refusal(starter, force);
        if (refusal != null) {
            refuse(starter, refusal, ChatFormatting.RED);
            return false;
        }
        BossFight fight = lane.create(starter, force);
        if (fight == null) {
            return false;
        }
        begin(fight, starter);
        return true;
    }

    private static void refuse(ServerPlayer player, Component message, ChatFormatting color) {
        player.displayClientMessage(message.copy().withStyle(color), true);
    }

    /**
     * Registers a fight a lane made and begins it with {@code starter} as its first participant; the server is told
     * (unless {@code announceFights} is off). {@link #start} calls this; a lane that makes its own fight (an
     * operator's command) may too.
     */
    public static void begin(BossFight fight, ServerPlayer starter) {
        FIGHTS.add(fight);
        fight.begin(starter);
        if (BossConfig.ANNOUNCE_FIGHTS.get()) {
            fight.server().getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.enhanced.boss.called", starter.getDisplayName(), fight.kind().displayName())
                            .withStyle(ChatFormatting.GOLD), false);
        }
    }

    /**
     * Ticks the fights of a dimension, once per level tick. A fight that throws is ended and let go, not ticked again.
     */
    public static void tickLevel(ServerLevel level) {
        if (FIGHTS.isEmpty()) {
            return;
        }
        for (BossFight fight : List.copyOf(FIGHTS)) {
            if (fight.level() != level) {
                continue;
            }
            try {
                fight.tick();
            } catch (RuntimeException e) {
                Enhanced.LOGGER.error("The {} fight failed and is ended", fight.kind().id(), e);
                try {
                    fight.end(BossEnd.ABORT);
                } finally {
                    fight.finish(true);
                }
            }
        }
    }

    /**
     * Ends every fight (an operator's {@code stop}, or the server stopping), giving back what the rituals took. An
     * operator's stop lets a running fight wind down as usual and puts back, at once, one that was already winding
     * down; the server stopping puts everything back at once.
     */
    public static void stopAll(MinecraftServer server, StopReason why) {
        BossEnd end = why == StopReason.SERVER_STOPPING ? BossEnd.SERVER_STOPPING : BossEnd.COMMAND;
        for (BossFight fight : all(server)) {
            boolean wasOver = fight.isOver();
            fight.end(end);
            if (why == StopReason.SERVER_STOPPING || wasOver) {
                fight.finish(true);
            }
        }
    }

    /** Forgets every fight (the server has stopped): each puts back what it can, quietly, and is dropped. */
    public static void clear() {
        for (BossFight fight : List.copyOf(FIGHTS)) {
            try {
                fight.finish(true);
            } catch (RuntimeException e) {
                Enhanced.LOGGER.debug("A boss fight could not be let go", e);
            }
        }
        FIGHTS.clear();
    }

    /** A fight finished: it leaves the registry. */
    static void finished(BossFight fight) {
        FIGHTS.remove(fight);
    }

    /**
     * Drops a fight from the registry and calls nothing of it, as a hard crash would leave it: nothing is given back, its
     * journal stays as it was; only its tickets and bars, which a crash loses too, are let go (D-T test 32).
     */
    @VisibleForTesting
    public static void forgetForTests(BossFight fight) {
        FIGHTS.remove(fight);
        fight.forget();
    }

    // ---------------------------------------------------------------------------------------------------------
    // The rest between fights

    /** How many ticks {@code kind} still rests (0 if it can be called). */
    public static long restLeft(MinecraftServer server, BossKind kind) {
        return Math.max(0L, BossGifts.data(server).restUntil(kind) - server.overworld().getGameTime());
    }

    /** The boss rests until the game time {@code until}. */
    public static void rest(MinecraftServer server, BossKind kind, long until) {
        BossGifts.store(server, BossGifts.data(server).withRest(kind, until));
    }

    private BossFights() {
    }
}
