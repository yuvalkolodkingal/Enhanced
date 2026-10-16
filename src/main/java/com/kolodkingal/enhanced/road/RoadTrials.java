package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Witches' Road trials, from the door to the end: who may walk them, building a trial in a private cell and taking the
 * player in, the moment each trial is won, the move on to the next, leaving, logging in and out, and the operators' tools. The
 * trial itself is a {@link RoadTrial}; this class is the one place that knows how they follow one another.
 *
 * <p>All of its state is in memory and about the live trials only; what lasts (progress, the ledger of cells) is in
 * {@link RoadSave}.
 */
public final class RoadTrials {
    /** What a door offers a player who holds a Gene and has no power. */
    public enum DoorMode {
        /** "Walk the Road": the trials. */
        ENTER,
        /** "Billy or Tommy": the Road has been walked (or has no trials). */
        CHOOSE
    }

    /** A trial being built for a player; he is taken in when it is placed. */
    private static final class Pending {
        final RoadTrial trial;
        /** The trial he is leaving behind (the move from one trial to the next), or null. */
        @Nullable
        final RoadTrial from;
        /** Where he came from, if this is how he enters the Road (null: he is in it already). */
        @Nullable
        final RoadProgress.Entry entry;
        final int startCheckpoint;
        /** Where a player who is already in the Road is held while it is built (a login). */
        @Nullable
        final Vec3 anchor;
        final long since;

        Pending(RoadTrial trial, @Nullable RoadTrial from, @Nullable RoadProgress.Entry entry, int startCheckpoint, @Nullable Vec3 anchor,
                long since) {
            this.trial = trial;
            this.from = from;
            this.entry = entry;
            this.startCheckpoint = startCheckpoint;
            this.anchor = anchor;
            this.since = since;
        }
    }

    /** A trial that is not placed after this many ticks is given up. */
    private static final int GIVE_UP_TICKS = 20 * 60;

    private static final Map<UUID, RoadTrial> ACTIVE = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** Players the Road itself is moving: its own travel is never refused. */
    private static final Set<UUID> MOVING = new HashSet<>();
    /** Player → game time he first right-clicked the exit door. */
    private static final Map<UUID, Long> LEAVE_ASKED = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // Who is where

    /** Whether the player is in a trial (or is being taken into one). */
    public static boolean isInside(Player player) {
        return ACTIVE.containsKey(player.getUUID()) || PENDING.containsKey(player.getUUID());
    }

    /** The trial the player plays now, or null. */
    @Nullable
    public static RoadTrial trialOf(UUID player) {
        return ACTIVE.get(player);
    }

    /** How many players are in a trial or being taken into one. */
    public static int playing() {
        Set<UUID> all = new HashSet<>(ACTIVE.keySet());
        all.addAll(PENDING.keySet());
        return all.size();
    }

    /** The seed of a player's course in a trial: the same every time, another for another player. */
    public static long seed(UUID player, String challenge, long salt) {
        long h = player.getMostSignificantBits() * 0x9E3779B97F4A7C15L ^ Long.rotateLeft(player.getLeastSignificantBits(), 29);
        h ^= (long) challenge.hashCode() * 0xBF58476D1CE4E5B9L ^ salt * 0x94D049BB133111EBL ^ WitchesRoadConfig.COURSE_SEED.get();
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return h;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The door

    /** What a door offers this player now. */
    public static DoorMode doorMode(ServerPlayer player) {
        if (!WitchesRoadConfig.ENABLED.get() || RoadDimension.level(player.server) == null) {
            return DoorMode.CHOOSE;
        }
        return walkedAll(player) ? DoorMode.CHOOSE : DoorMode.ENTER;
    }

    /** Whether the player has won every trial there is. */
    public static boolean walkedAll(ServerPlayer player) {
        RoadProgress progress = RoadSave.get(player.server).existing(player.getUUID());
        return progress != null && progress.completed() >= RoadChallenges.count();
    }

    /** How many trials the player has won, and how many there are. */
    public static int[] doneAndTotal(ServerPlayer player) {
        RoadProgress progress = RoadSave.get(player.server).existing(player.getUUID());
        return new int[]{progress == null ? 0 : Math.min(progress.completed(), RoadChallenges.count()), RoadChallenges.count()};
    }

    /** The lang key of why the player may not walk the Road now, or null if he may. */
    @Nullable
    public static String refusal(ServerPlayer player) {
        return refusal(player, false);
    }

    /** {@link #refusal(ServerPlayer)}; {@code forced}: an operator sends him into a given trial, so having walked the Road does not stop him. */
    @Nullable
    private static String refusal(ServerPlayer player, boolean forced) {
        MinecraftServer server = player.server;
        if (!WitchesRoadConfig.ENABLED.get() || RoadDimension.level(server) == null) {
            return "message.enhanced.road.off";
        }
        if (isInside(player)) {
            return "message.enhanced.road.already";
        }
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
            return "message.enhanced.road.mortals";
        }
        if (Powers.power(player).hasPowers()) {
            return "message.enhanced.road.own_road";
        }
        RoadProgress progress = RoadSave.get(server).existing(player.getUUID());
        if (!forced && progress != null && progress.completed() >= RoadChallenges.count()) {
            return "message.enhanced.road.walked";
        }
        if (playing() >= WitchesRoadConfig.MAX_PLAYERS.get()) {
            return "message.enhanced.road.full";
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Going in

    /**
     * The player asks to walk the Road: the trial he has reached is built for him, and when it is ready he is taken in. The
     * checks are {@link #refusal}'s.
     *
     * @return whether the trial is being built (otherwise he was told why not)
     */
    public static boolean enter(ServerPlayer player) {
        return enter(player, -1, null);
    }

    /** {@link #enter(ServerPlayer)}: the player opened this Witches' Road door (it is where he is put back if the place he stood on is gone). */
    public static boolean enter(ServerPlayer player, BlockPos door) {
        return enter(player, -1, door);
    }

    /**
     * {@link #enter(ServerPlayer)} into a given trial instead of the one he has reached (an operator's tool; -1 = as usual). The number
     * after the last trial is the end of the Road, the hall where the choice is made ({@link RoadEnding}).
     */
    public static boolean enter(ServerPlayer player, int forcedTrial) {
        return enter(player, forcedTrial, null);
    }

    private static boolean enter(ServerPlayer player, int forcedTrial, @Nullable BlockPos door) {
        String refusal = refusal(player, forcedTrial >= 0);
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        MinecraftServer server = player.server;
        RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
        int number = forcedTrial >= 0 ? forcedTrial : progress.completed();
        RoadChallenge challenge = RoadChallenges.stage(number);
        if (challenge == null) {
            return false;
        }
        int checkpoint = forcedTrial < 0 && WitchesRoadConfig.RESUME_AT_CHECKPOINT.get() && challenge.resumable() ? progress.checkpoint() : 0;
        RoadTrial trial = plan(server, player.getUUID(), challenge, number, progress.salt());
        if (trial == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.failed").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        RoadProgress.Entry entry = new RoadProgress.Entry(player.level().dimension(), player.position(), player.getYRot(), player.getXRot(),
                player.gameMode.getGameModeForPlayer().getId(), door == null ? null : door.immutable());
        PENDING.put(player.getUUID(), new Pending(trial, null, entry, Math.min(checkpoint, trial.layout().checkpoints().size() - 1), null,
                player.level().getGameTime()));
        player.displayClientMessage(Component.translatable("message.enhanced.road.opening").withStyle(s -> s.withColor(WitchesRoad.LIGHT)), true);
        return true;
    }

    /** Allocates a cell, draws the trial in a canvas and takes the cell's chunks; null if there is no room or the drawing failed. */
    @Nullable
    private static RoadTrial plan(MinecraftServer server, UUID owner, RoadChallenge challenge, int number, long salt) {
        ServerLevel road = RoadDimension.level(server);
        if (road == null) {
            return null;
        }
        RoadSave save = RoadSave.get(server);
        int cell = save.allocate(owner, challenge.id(), WitchesRoadConfig.MAX_PLAYERS.get() * 2 + 8);
        if (cell < 0) {
            return null;
        }
        ensureBorder(road);
        // A generator may fail for a seed (a parkour that boxes itself in): the next seeds of the same player are tried, the same ones every
        // time, so his course is still the same course every time.
        RuntimeException failure = null;
        for (int attempt = 0; attempt < DRAW_ATTEMPTS; attempt++) {
            try {
                long seed = seed(owner, challenge.id(), salt + attempt * 7919L);
                RoadCanvas canvas = new RoadCanvas(cell, road.getMinBuildHeight(), road.getMaxBuildHeight());
                RoadLayout layout = challenge.build(new RoadBuild(road, seed, canvas));
                canvas.seal();
                save.plan(cell, canvas.chunks(), canvas.minY(), canvas.maxY());
                RoadSweeper.hold(road, owner, canvas.chunks());
                return new RoadTrial(server, owner, challenge, number, seed, canvas, layout);
            } catch (RuntimeException e) {
                failure = e;
            }
        }
        Enhanced.LOGGER.error("The Witches' Road could not draw the trial {} for {} (even with {} seeds)", challenge.id(), owner, DRAW_ATTEMPTS, failure);
        save.release(cell);
        return null;
    }

    /** How many seeds are tried for a course before the trial is given up. */
    private static final int DRAW_ATTEMPTS = 8;
    /** How long the gate waits after a trial could not be drawn before it tries again (3 s). */
    private static final int ADVANCE_RETRY_TICKS = 60;
    /** Player → game time the next trial could not be drawn for him. */
    private static final Map<UUID, Long> ADVANCE_FAILED = new HashMap<>();

    /**
     * The Road's world border is as big as the world's and centred on the origin, whatever the overworld's is (the overworld's border is
     * copied to every dimension): the cells lie up to 130,000 blocks out, and outside a border nothing can be built, broken or used.
     */
    static void ensureBorder(ServerLevel road) {
        WorldBorder border = road.getWorldBorder();
        if (border.getSize() < MAX_BORDER || border.getCenterX() != 0.0 || border.getCenterZ() != 0.0) {
            border.setCenter(0.0, 0.0);
            border.setSize(MAX_BORDER);
        }
    }

    /** The biggest world border there is. */
    private static final double MAX_BORDER = 5.9999968E7;

    /** The player is let into the trial that is ready for him. */
    private static void arrive(ServerPlayer player, Pending pending) {
        boolean already = !MOVING.add(player.getUUID());
        try {
            arriveNow(player, pending);
        } finally {
            if (!already) {
                MOVING.remove(player.getUUID());
            }
        }
    }

    private static void arriveNow(ServerPlayer player, Pending pending) {
        MinecraftServer server = player.server;
        ServerLevel road = RoadDimension.level(server);
        RoadTrial trial = pending.trial;
        RoadLayout.Checkpoint at = trial.layout().checkpoint(pending.startCheckpoint);
        ServerLevel fromLevel = player.serverLevel();
        Vec3 fromPos = player.position();
        if (road == null || !HexManager.onTeleport(road, player, at.respawn())) {
            // Something (a boss fight, Mjolnir's pin) does not let him go: the trial is taken down, and he is told.
            PENDING.remove(player.getUUID());
            retire(trial, RoadChallenge.End.REMOVED);
            player.displayClientMessage(Component.translatable("message.enhanced.road.failed").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
        if (pending.entry != null) {
            progress.setEntry(pending.entry);
        }
        progress.setInside(true);
        progress.setCheckpoint(pending.startCheckpoint);
        progress.addStarted();
        PENDING.remove(player.getUUID());
        ACTIVE.put(player.getUUID(), trial);
        trial.setCheckpoint(pending.startCheckpoint);
        trial.setPhase(RoadTrial.Phase.ACTIVE);
        trial.markStarted();
        player.setGameMode(GameType.SURVIVAL);
        if (!moveTo(player, road, at.respawn(), at.yaw(), 0.0F)) {
            ACTIVE.remove(player.getUUID());
            if (pending.from != null) {
                // He is still in the trial he won: its gate is open for him to try again.
                ACTIVE.put(player.getUUID(), pending.from);
            }
            if (pending.entry != null) {
                player.setGameMode(GameType.byId(pending.entry.gameMode()));
                progress.setEntry(null);
            }
            progress.setInside(pending.from != null || pending.anchor != null);
            retire(trial, RoadChallenge.End.REMOVED);
            player.displayClientMessage(Component.translatable("message.enhanced.road.failed").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (pending.from == null && pending.anchor == null) {
            RoadFx.travel(fromLevel, fromPos, false);
        }
        RoadFx.travel(road, at.respawn(), true);
        restore(player);
        if (!trial.challenge().isEnding()) {
            // (The end of the Road speaks for itself.)
            RoadVoice.begin(player, trial.challenge(), trial.number() + 1);
            RoadVoice.say(player, pending.from == null && pending.anchor == null ? RoadVoice.ENTER : pending.anchor != null ? RoadVoice.BACK
                    : RoadVoice.START, progress.started());
        }
        trial.challenge().onStart(trial, player);
        if (pending.from != null) {
            retire(pending.from, RoadChallenge.End.MOVED_ON);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Playing

    /** One tick of the server: trials being built are placed, trials being played run, cells being cleared are cleared. */
    public static void serverTick(MinecraftServer server) {
        try {
            RoadSweeper.tick(server);
        } catch (RuntimeException e) {
            Enhanced.LOGGER.error("The Witches' Road could not clear a cell", e);
        }
        if (PENDING.isEmpty() && ACTIVE.isEmpty()) {
            return;
        }
        ServerLevel road = RoadDimension.level(server);
        if (road == null) {
            return;
        }
        if (road.getGameTime() % 20 == 0) {
            ensureBorder(road);
        }
        for (Map.Entry<UUID, Pending> entry : new ArrayList<>(PENDING.entrySet())) {
            try {
                tickPending(server, road, entry.getKey(), entry.getValue());
            } catch (RuntimeException e) {
                gaveUp(server, entry.getKey(), e);
            }
        }
        for (Map.Entry<UUID, RoadTrial> entry : new ArrayList<>(ACTIVE.entrySet())) {
            try {
                tickActive(server, entry.getKey(), entry.getValue());
            } catch (RuntimeException e) {
                gaveUp(server, entry.getKey(), e);
            }
        }
    }

    private static void tickPending(MinecraftServer server, ServerLevel road, UUID id, Pending pending) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null || !player.isAlive() || road.getGameTime() - pending.since > GIVE_UP_TICKS) {
            PENDING.remove(id);
            retire(pending.trial, RoadChallenge.End.REMOVED);
            if (player != null && player.isAlive()) {
                player.displayClientMessage(Component.translatable("message.enhanced.road.failed").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        if (pending.anchor != null && RoadDimension.in(player)) {
            hold(player, pending.anchor);
        }
        if (pending.trial.canvas().placeSome(road, WitchesRoadConfig.BUILD_BLOCKS_PER_TICK.get())) {
            pending.trial.setPhase(RoadTrial.Phase.READY);
            arrive(player, pending);
        }
    }

    private static void tickActive(MinecraftServer server, UUID id, RoadTrial trial) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null || !player.isAlive()) {
            return;
        }
        if (!RoadDimension.in(player) && !MOVING.contains(id)) {
            // Somebody (an operator) took him out of the Road: it lets him go.
            dropTaken(player, trial);
            return;
        }
        if (Powers.power(player).hasPowers() && !RoadEnding.isLeaving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.road.powers").withStyle(ChatFormatting.GRAY), true);
            leave(player, RoadChallenge.End.LEFT);
            return;
        }
        trial.tick(player);
    }

    /**
     * Something in a trial threw: it is given up, as if the player had left (he is sent back, with his progress and his Gene, and his save no
     * longer says he is inside), so that one bad course cannot crash the server again at every start.
     */
    private static void gaveUp(MinecraftServer server, UUID id, RuntimeException cause) {
        Enhanced.LOGGER.error("The Witches' Road failed for {} and gives the trial up", id, cause);
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        try {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.enhanced.road.failed").withStyle(ChatFormatting.GRAY), true);
                if (leave(player, RoadChallenge.End.LEFT)) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            Enhanced.LOGGER.error("... and could not send him back either", e);
        }
        // Whatever is left of it is forgotten, so the next tick does not meet it again.
        RoadTrial trial = ACTIVE.remove(id);
        Pending pending = PENDING.remove(id);
        try {
            RoadProgress progress = RoadSave.get(server).existing(id);
            if (progress != null) {
                progress.setInside(false);
            }
            if (trial != null) {
                retire(trial, RoadChallenge.End.REMOVED);
            }
            if (pending != null) {
                retire(pending.trial, RoadChallenge.End.REMOVED);
            }
        } catch (RuntimeException e) {
            Enhanced.LOGGER.error("... and could not take it down", e);
        }
    }

    /** Keeps a player who is waiting for his trial where he stands (he logged in over the void). */
    private static void hold(ServerPlayer player, Vec3 anchor) {
        if (player.getY() < anchor.y - 0.25 || player.position().distanceToSqr(anchor) > 4.0) {
            player.teleportTo(player.serverLevel(), anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    /** Heals the player (if the config says so), fills his hunger, stops him and clears his fall. */
    static void restore(ServerPlayer player) {
        if (WitchesRoadConfig.HEAL_AT_CHECKPOINT.get()) {
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(10.0F);
        }
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Winning, and on to the next

    /** The player has won the trial (called by {@link RoadTrial#succeed()}). */
    static void won(RoadTrial trial) {
        ServerLevel road = trial.level();
        RoadProgress progress = trial.progress();
        progress.setCompleted(Math.max(progress.completed(), trial.number() + 1));
        RoadLayout.Gate gate = trial.layout().gate();
        if (gate != null) {
            RoadPalette.openGate(trial.canvas(), road, gate);
        }
        ServerPlayer player = trial.player();
        if (player == null) {
            return;
        }
        RoadFx.won(road, player.position());
        RoadVoice.say(player, RoadVoice.WON, progress.started());
        player.displayClientMessage(Component.translatable("message.enhanced.road.won", trial.challenge().title()).withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)), true);
        if (WitchesRoadConfig.ANNOUNCE.get()) {
            player.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.enhanced.road.announce", player.getDisplayName(),
                    trial.challenge().title()).withStyle(s -> s.withColor(WitchesRoad.LIGHT)), false);
        }
        if (gate == null) {
            advance(player);
        }
    }

    /** The player steps through the gate: the next trial is built, and when it is ready he is taken there. */
    static void advance(ServerPlayer player) {
        RoadTrial trial = ACTIVE.get(player.getUUID());
        if (trial == null || trial.phase() != RoadTrial.Phase.WON || PENDING.containsKey(player.getUUID())) {
            return;
        }
        RoadProgress progress = trial.progress();
        // After the last trial comes the end of the Road: the hall where he chooses.
        RoadChallenge next = RoadChallenges.stage(progress.completed());
        if (next == null) {
            return;
        }
        long now = player.level().getGameTime();
        Long failed = ADVANCE_FAILED.get(player.getUUID());
        if (failed != null && now >= failed && now - failed < ADVANCE_RETRY_TICKS) {
            // (He stands in the gate: this runs every tick, and a trial that could not be drawn is not drawn again that often.)
            return;
        }
        RoadTrial built = plan(player.server, player.getUUID(), next, progress.completed(), progress.salt());
        if (built == null) {
            ADVANCE_FAILED.put(player.getUUID(), now);
            player.displayClientMessage(Component.translatable("message.enhanced.road.failed").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ADVANCE_FAILED.remove(player.getUUID());
        PENDING.put(player.getUUID(), new Pending(built, trial, null, 0, null, player.level().getGameTime()));
        player.displayClientMessage(Component.translatable("message.enhanced.road.opening").withStyle(s -> s.withColor(WitchesRoad.LIGHT)), true);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Going out

    /**
     * The player leaves the Road, whatever he is doing in it: back to where he entered, in the game mode he entered in,
     * with his Gene. His progress stays. If the way out is refused (another mod stops the change of dimension) he stays where he is,
     * in his trial, and is told; nothing of the Road is taken down.
     *
     * @return whether he is out
     */
    public static boolean leave(ServerPlayer player, RoadChallenge.End reason) {
        UUID id = player.getUUID();
        LEAVE_ASKED.remove(id);
        RoadProgress progress = RoadSave.get(player.server).progress(id);
        RoadProgress.Entry entry = progress.entry();
        if (RoadDimension.in(player)) {
            Vec3 from = player.position();
            // (The Road's own move: its own checks must not refuse it while the trial still stands.)
            boolean already = !MOVING.add(id);
            boolean moved;
            try {
                moved = sendHome(player, entry);
            } finally {
                if (!already) {
                    MOVING.remove(id);
                }
            }
            if (!moved) {
                player.displayClientMessage(Component.translatable("message.enhanced.road.cannot_leave").withStyle(ChatFormatting.GRAY), true);
                return false;
            }
            RoadFx.travel(RoadDimension.level(player.server), from, false);
            RoadFx.travel(player.serverLevel(), player.position(), true);
        }
        RoadTrial trial = ACTIVE.remove(id);
        Pending pending = PENDING.remove(id);
        if (entry != null) {
            player.setGameMode(GameType.byId(entry.gameMode()));
        }
        progress.setInside(false);
        progress.setEntry(null);
        RoadChallenge.End how = reason == RoadChallenge.End.LEFT && trial != null && trial.phase() == RoadTrial.Phase.WON ? RoadChallenge.End.WON : reason;
        if (trial != null) {
            retire(trial, how);
        }
        if (pending != null) {
            retire(pending.trial, RoadChallenge.End.REMOVED);
        }
        restore(player);
        if (Powers.power(player).hasPowers()) {
            // He has a power now (he chose at the end of the Road, or an operator gave him one): the Road is behind him and remembers nothing.
            closeRoad(player.server, id);
        } else if (reason != RoadChallenge.End.WON) {
            RoadVoice.say(player, RoadVoice.LEAVE, progress.started());
            if (progress.completed() >= RoadChallenges.count()) {
                // He walked the whole Road but did not choose: the door still offers the choice.
                player.displayClientMessage(Component.translatable("message.enhanced.road.walked_all").withStyle(s -> s.withColor(WitchesRoad.LIGHT_CORE)), false);
                player.displayClientMessage(Component.translatable("message.enhanced.road.walked_all.hint").withStyle(ChatFormatting.GRAY), false);
            }
        }
        return true;
    }

    /**
     * The Road is done with the player (he has his power): his progress is forgotten, and the courses he is dealt next time are other ones.
     * He can walk the Road again only with another Gene and no power.
     */
    public static void closeRoad(MinecraftServer server, UUID id) {
        RoadSave save = RoadSave.get(server);
        RoadProgress progress = save.existing(id);
        if (progress == null || progress.isUntouched()) {
            return;
        }
        long salt = progress.salt();
        save.forget(id);
        save.progress(id).setSalt(salt + 1);
    }

    /**
     * Takes the player to where he entered. If that place is gone or unsafe (a block there now, no floor under it, lava, or its world is
     * not loaded any more) he is put on the door he opened, and failing that on the world's spawn.
     */
    private static boolean sendHome(ServerPlayer player, @Nullable RoadProgress.Entry entry) {
        MinecraftServer server = player.server;
        ServerLevel level = entry == null ? null : server.getLevel(entry.dimension());
        List<Home> homes = new ArrayList<>();
        if (entry != null && level != null) {
            homes.add(new Home(level, entry.pos(), entry.yaw(), entry.pitch()));
            if (entry.door() != null && WitchesRoad.isDoor(level.getBlockState(entry.door()))) {
                Vec3 top = new Vec3(entry.door().getX() + 0.5, entry.door().getY() + WitchesRoad.DOOR_HEIGHT, entry.door().getZ() + 0.5);
                homes.add(new Home(level, top, entry.yaw(), entry.pitch()));
            }
        }
        for (Home home : homes) {
            Vec3 pos = safeSpot(home.level, player, home.pos);
            if (pos != null && HexManager.onTeleport(home.level, player, pos) && moveTo(player, home.level, pos, home.yaw, home.pitch)) {
                return true;
            }
        }
        // The world's spawn, on the surface; if that is refused too, nothing stops him.
        ServerLevel overworld = server.overworld();
        Vec3 spawn = worldSpawn(overworld);
        HexManager.onTeleport(overworld, player, spawn);
        return moveTo(player, overworld, spawn, 0.0F, 0.0F);
    }

    /** A place to put the player back at. */
    private record Home(ServerLevel level, Vec3 pos, float yaw, float pitch) {
    }

    /** The surface at the world's spawn point. */
    static Vec3 worldSpawn(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        level.getChunkAt(spawn);
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
        return Vec3.atBottomCenterOf(top.getY() > level.getMinBuildHeight() ? top : spawn);
    }

    /** How far below the player's feet a floor must be for the place to count as safe. */
    private static final int FLOOR_SEARCH = 24;

    /**
     * The position, or the nearest free one above it (up to six blocks) if a block is there now; null if there is none or the place is
     * not safe to stand in (fire or lava at the feet, harmful blocks or lava for a floor, no floor within {@value #FLOOR_SEARCH} blocks).
     */
    @Nullable
    static Vec3 safeSpot(ServerLevel level, ServerPlayer player, Vec3 pos) {
        level.getChunkAt(BlockPos.containing(pos));
        for (int up = 0; up <= 6; up++) {
            Vec3 candidate = pos.add(0, up, 0);
            if (candidate.y < level.getMinBuildHeight() || candidate.y > level.getMaxBuildHeight() - 2) {
                break;
            }
            // (Built from the position, not moved by a difference: far from the origin that leaves a rounding error that touches the floor.)
            AABB box = player.getDimensions(player.getPose()).makeBoundingBox(candidate).deflate(1.0E-3);
            if (level.getWorldBorder().isWithinBounds(box) && level.noCollision(player, box) && hasSafeFloor(level, BlockPos.containing(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    /** Whether a player whose feet are in this block has something to stand on that does not hurt him (or water to swim in) just below. */
    private static boolean hasSafeFloor(ServerLevel level, BlockPos feet) {
        BlockState at = level.getBlockState(feet);
        if (at.is(BlockTags.FIRE) || level.getFluidState(feet).is(FluidTags.LAVA) || at.is(Blocks.CACTUS)) {
            return false;
        }
        BlockPos.MutableBlockPos pos = feet.mutable();
        for (int i = 0; i <= FLOOR_SEARCH; i++) {
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.LAVA)) {
                return false;
            }
            if (!state.getFluidState().isEmpty()) {
                return true;
            }
            if (i > 0 && !state.getCollisionShape(level, pos).isEmpty()) {
                return !state.is(Blocks.MAGMA_BLOCK) && !state.is(Blocks.CACTUS) && !state.is(BlockTags.FIRE) && !state.is(Blocks.CAMPFIRE)
                        && !state.is(Blocks.SOUL_CAMPFIRE) && !state.is(Blocks.SWEET_BERRY_BUSH);
            }
            pos.move(0, -1, 0);
            if (pos.getY() < level.getMinBuildHeight()) {
                return false;
            }
        }
        return false;
    }

    /** An operator took the player out of the Road: nothing is moved, the trial is taken down, his game mode comes back. */
    private static void dropTaken(ServerPlayer player, RoadTrial trial) {
        UUID id = player.getUUID();
        ACTIVE.remove(id);
        RoadProgress progress = RoadSave.get(player.server).progress(id);
        RoadProgress.Entry entry = progress.entry();
        if (entry != null) {
            player.setGameMode(GameType.byId(entry.gameMode()));
        }
        progress.setInside(false);
        progress.setEntry(null);
        retire(trial, RoadChallenge.End.LEFT);
    }

    /** The trial is over: its challenge is told, what it spawned goes, and its cell is cleared. */
    private static void retire(RoadTrial trial, RoadChallenge.End reason) {
        if (trial.phase() == RoadTrial.Phase.CLEARING) {
            return;
        }
        try {
            trial.challenge().onEnd(trial, reason);
        } catch (RuntimeException e) {
            Enhanced.LOGGER.error("The trial {} failed to end", trial.challenge().id(), e);
        }
        trial.discardTracked();
        trial.setPhase(RoadTrial.Phase.CLEARING);
        RoadSweeper.queue(trial.server(), trial.cell(), true);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Moving a player

    /** Moves the player to a place in a level (another dimension too); false if something refused. */
    static boolean moveTo(ServerPlayer player, ServerLevel level, Vec3 pos, float yaw, float pitch) {
        UUID id = player.getUUID();
        boolean already = !MOVING.add(id);
        try {
            player.stopRiding();
            if (player.level() == level) {
                player.teleportTo(level, pos.x, pos.y, pos.z, yaw, pitch);
            } else if (player.changeDimension(new DimensionTransition(level, pos, Vec3.ZERO, yaw, pitch, DimensionTransition.DO_NOTHING)) == null) {
                return false;
            }
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            player.resetFallDistance();
            return true;
        } finally {
            if (!already) {
                MOVING.remove(id);
            }
        }
    }

    /** Moves the player inside the Road (a checkpoint). */
    static void moveWithin(ServerPlayer player, Vec3 pos, float yaw) {
        moveTo(player, player.serverLevel(), pos, yaw, 0.0F);
    }

    /**
     * Runs {@code action} as the Road's own move of the player (what it does is never refused: an operator's tool, and the tests that put a
     * player in the Road the way an operator's /tp would).
     */
    public static void moving(ServerPlayer player, Runnable action) {
        boolean already = !MOVING.add(player.getUUID());
        try {
            action.run();
        } finally {
            if (!already) {
                MOVING.remove(player.getUUID());
            }
        }
    }

    /** Whether the Road itself is moving the player now (its own travel is never refused). */
    public static boolean isMoving(Entity entity) {
        return MOVING.contains(entity.getUUID());
    }

    /**
     * Whether a teleport of this entity (an Ender Pearl, the Tesseract, a hero's leap) is allowed: not for a player in a trial,
     * unless it stays in his own cell.
     */
    public static boolean allowsTeleport(Entity entity, ServerLevel destination, Vec3 to) {
        if (!(entity instanceof ServerPlayer player) || isMoving(player)) {
            return true;
        }
        if (!isInside(player)) {
            // Nor can anyone outside the trials travel INTO the Road (another mod's home, a saved place, a /tpa): he would land in the void or
            // in somebody else's cell. Only the Road moves players in; an operator's /tp is free.
            if (RoadDimension.is(destination) && !player.hasPermissions(2)) {
                player.displayClientMessage(Component.translatable("message.enhanced.road.no_teleport").withStyle(ChatFormatting.GRAY), true);
                return false;
            }
            return true;
        }
        RoadTrial trial = ACTIVE.get(player.getUUID());
        if (trial != null && RoadDimension.is(destination) && RoadCells.contains(trial.cell(), BlockPos.containing(to))) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.enhanced.road.no_teleport").withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The exit door

    /** The player right-clicked a door inside the Road: the second click within a few seconds leaves it. */
    public static void useExitDoor(ServerPlayer player) {
        RoadTrial trial = ACTIVE.get(player.getUUID());
        if (trial == null) {
            return;
        }
        if (trial.challenge().isEnding()) {
            // At the end of the Road the door asks the question again.
            RoadEnding.reopen(player, true);
            return;
        }
        long now = player.level().getGameTime();
        Long asked = LEAVE_ASKED.get(player.getUUID());
        if (asked != null && now - asked <= WitchesRoadConfig.LEAVE_CONFIRM_SECONDS.get() * 20L) {
            leave(player, RoadChallenge.End.LEFT);
        } else {
            LEAVE_ASKED.put(player.getUUID(), now);
            player.displayClientMessage(Component.translatable("message.enhanced.road.leave_ask").withStyle(s -> s.withColor(WitchesRoad.LIGHT)), true);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Logging in and out, dying

    /** The player logged out: his trial is taken down (his progress says he is inside, so he comes back to it). */
    public static void onLogout(ServerPlayer player) {
        UUID id = player.getUUID();
        LEAVE_ASKED.remove(id);
        RoadTrial trial = ACTIVE.remove(id);
        Pending pending = PENDING.remove(id);
        if (trial != null) {
            retire(trial, RoadChallenge.End.REMOVED);
        }
        if (pending != null) {
            retire(pending.trial, RoadChallenge.End.REMOVED);
            if (pending.from != null) {
                retire(pending.from, RoadChallenge.End.REMOVED);
            }
        }
    }

    /**
     * A player logged in or came back from the dead: whatever the Road and the world's save disagree about is settled. In the Road
     * with a trial of his own saved he goes on at his checkpoint ({@code resumeOnLogin}), or is sent home; in the Road with no
     * trial he is sent home; outside the Road with a trial saved, it is forgotten.
     */
    public static void reconcile(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel road = RoadDimension.level(server);
        if (road == null) {
            return;
        }
        UUID id = player.getUUID();
        RoadProgress progress = RoadSave.get(server).existing(id);
        boolean inRoad = RoadDimension.in(player);
        RoadTrial live = ACTIVE.get(id);
        if (live != null) {
            if (!inRoad) {
                // Back from the dead outside the Road (nothing should let that happen): he is put back in his trial.
                RoadLayout.Checkpoint at = live.layout().checkpoint(live.checkpoint());
                moveTo(player, road, at.respawn(), at.yaw(), 0.0F);
                restore(player);
            }
            return;
        }
        if (PENDING.containsKey(id)) {
            return;
        }
        if (inRoad) {
            if (Powers.power(player).hasPowers()) {
                // He chose (the power awakened) and the server stopped in the farewell: the Road lets him go, as it would have.
                leave(player, progress != null && progress.completed() >= RoadChallenges.count() ? RoadChallenge.End.WON : RoadChallenge.End.LEFT);
                return;
            }
            if (progress != null && progress.inside() && WitchesRoadConfig.RESUME_ON_LOGIN.get() && WitchesRoadConfig.ENABLED.get()
                    && RoadChallenges.stage(progress.completed()) != null && resume(player, progress)) {
                return;
            }
            leave(player, RoadChallenge.End.LEFT);
        } else if (progress != null && progress.inside()) {
            progress.setInside(false);
            progress.setEntry(null);
        }
    }

    /** A player who logged in inside the Road: his trial is built again and he is put at his checkpoint. */
    private static boolean resume(ServerPlayer player, RoadProgress progress) {
        RoadChallenge challenge = RoadChallenges.stage(progress.completed());
        if (challenge == null) {
            return false;
        }
        RoadTrial trial = plan(player.server, player.getUUID(), challenge, progress.completed(), progress.salt());
        if (trial == null) {
            return false;
        }
        int checkpoint = challenge.resumable() ? Math.min(progress.checkpoint(), trial.layout().checkpoints().size() - 1) : 0;
        PENDING.put(player.getUUID(), new Pending(trial, null, null, checkpoint, player.position(), player.level().getGameTime()));
        player.displayClientMessage(Component.translatable("message.enhanced.road.opening").withStyle(s -> s.withColor(WitchesRoad.LIGHT)), true);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The server's life

    /** The server has started: every cell the last run left in the ledger (none belongs to a live trial yet) is cleared. */
    public static void onServerStarted(MinecraftServer server) {
        Set<Integer> inUse = new HashSet<>();
        for (RoadTrial trial : ACTIVE.values()) {
            inUse.add(trial.cell());
        }
        for (Pending pending : PENDING.values()) {
            inUse.add(pending.trial.cell());
            if (pending.from != null) {
                inUse.add(pending.from.cell());
            }
        }
        RoadSweeper.queueOrphans(server, inUse);
    }

    /** The server is stopping: the trials come down now, so the world is saved without them. */
    public static void onServerStopping(MinecraftServer server) {
        for (RoadTrial trial : new ArrayList<>(ACTIVE.values())) {
            retire(trial, RoadChallenge.End.REMOVED);
        }
        for (Pending pending : new ArrayList<>(PENDING.values())) {
            retire(pending.trial, RoadChallenge.End.REMOVED);
            if (pending.from != null) {
                retire(pending.from, RoadChallenge.End.REMOVED);
            }
        }
        ACTIVE.clear();
        PENDING.clear();
        RoadSweeper.drain(server);
    }

    /** Forget all memory (the server stopped; a test that wants a clean slate). */
    public static void clear() {
        ADVANCE_FAILED.clear();
        ACTIVE.clear();
        PENDING.clear();
        MOVING.clear();
        LEAVE_ASKED.clear();
        RoadSweeper.clear();
        RoadEnding.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The operators' tools

    /** Wins the trial the player is in; otherwise counts the next trial as won. @return whether anything happened */
    public static boolean skip(ServerPlayer player) {
        RoadTrial trial = ACTIVE.get(player.getUUID());
        if (trial != null && trial.challenge().isEnding()) {
            // (There is nothing to win at the end of the Road: he chooses.)
            return false;
        }
        if (trial != null && trial.phase() == RoadTrial.Phase.ACTIVE) {
            trial.succeed();
            return true;
        }
        RoadProgress progress = RoadSave.get(player.server).progress(player.getUUID());
        if (progress.completed() < RoadChallenges.count() && trial == null) {
            progress.setCompleted(progress.completed() + 1);
            return true;
        }
        return false;
    }

    /** Forgets everything about the player on the Road; with {@code newCourse} he is dealt other courses first. */
    public static void reset(MinecraftServer server, UUID id, boolean newCourse) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        RoadSave save = RoadSave.get(server);
        long salt = save.progress(id).salt();
        if (player != null && (isInside(player) || RoadDimension.in(player))) {
            leave(player, RoadChallenge.End.LEFT);
        }
        save.forget(id);
        if (newCourse) {
            save.progress(id).setSalt(salt + 1);
        }
    }

    /** Sets how many trials the player has won. */
    public static void setCompleted(MinecraftServer server, UUID id, int completed) {
        RoadSave.get(server).progress(id).setCompleted(Math.max(0, Math.min(completed, RoadChallenges.count())));
    }

    /** Every live trial (for the commands and the tests). */
    public static List<RoadTrial> live() {
        return new ArrayList<>(ACTIVE.values());
    }

    private RoadTrials() {
    }
}
