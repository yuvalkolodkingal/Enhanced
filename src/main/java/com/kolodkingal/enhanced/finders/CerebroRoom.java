package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Telepathy;
import com.kolodkingal.enhanced.block.CerebroConsoleBlock;
import com.kolodkingal.enhanced.config.XavierConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.network.CerebroPayload;
import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.world.SchoolLabPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The lab's Cerebro (3.7, lane L5): Professor X's N stays as it is everywhere (256 blocks, his dimension, 10 s), and at
 * the Cerebro console of the X-Men lower lab it is <b>amplified</b>: {@code cerebroAmplifiedTicks} (30 s), every player
 * in every dimension not under Magneto's helmet ({@code cerebroAmplifiedAllDimensions}; each mind marked with its
 * dimension), no range limit, and the nearest Infinity Stone site of each kind in his dimension ({@code
 * cerebroShowsStoneSites}, {@link SiteSearch#nearest}). The found are told "Cerebro has found you" as with the plain
 * Cerebro. The cooldown is Cerebro's own ({@code cerebroCooldown}), shared.
 *
 * <p>Two seams: {@code XavierAbilities.cerebro} asks {@link #amplify} (N within {@code cerebroConsoleReach} of a
 * console), and the console block (lane L4) hands a right-click to {@link #useConsole}. Any telepath may use the
 * console (the Scarlet Witch, the Phoenix, Billy, a custom telepath; Jean did, X2); others are refused ("Cerebro needs
 * a telepath's mind."), and so is one who wears Magneto's helmet ("The helmet blocks Cerebro."). A second right-click
 * takes the helmet off. It ends when its time is up, when he walks away from the console, leaves, dies or the console
 * is gone.
 *
 * <p><b>The room shows them</b>: the report ({@link CerebroPayload}, amplified, with the sites) goes to him every
 * second, and to every other player standing inside the sphere (the room's stars are for everyone in it); his client
 * and theirs draw the minds as stars on the sphere ({@code client/finders/CerebroSphere}). The console's hum and light
 * are the client's ({@code LabFxPayload.CONSOLE_ON|OFF} to the players near).
 */
public final class CerebroRoom {
    /** Cerebro reports this often. */
    public static final int PING = 20;
    /** A visitor's report lasts this long (it is renewed every {@link #PING} while he stays in the sphere). */
    public static final int VISITOR_TICKS = 30;
    /** He may walk this much beyond the console's reach before the helmet comes off. */
    public static final int LEASH = 6;
    /** From the console to the sphere's centre (blocks, toward the way the console faces): the plan's two points. */
    public static final int CONSOLE_TO_CENTER = SchoolLabPlan.CEREBRO_CONSOLE.distManhattan(
            SchoolLabPlan.CEREBRO_CENTER);

    /** One telepath at a console. */
    private static final class Session {
        final BlockPos console;
        final ResourceKey<Level> dimension;
        final long until;
        final Set<UUID> visitors = new HashSet<>();

        Session(BlockPos console, ResourceKey<Level> dimension, long until) {
            this.console = console;
            this.dimension = dimension;
            this.until = until;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /**
     * When each telepath's Cerebro is ready again (game time): Cerebro's cooldown is on a key, and a telepath with no
     * Cerebro key (the Scarlet Witch, the Phoenix) has it here.
     */
    private static final Map<UUID, Long> RESTING = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // The seams

    /**
     * Whether {@code player}, pressing Cerebro's key (his cooldown ready), is at a console and the room has amplified
     * it (then the plain Cerebro does not run: the room started its own search and cooldown). A console another wears
     * is no console: the plain Cerebro runs.
     */
    public static boolean amplify(ServerPlayer player) {
        if (SESSIONS.containsKey(player.getUUID())) {
            end(player.server, player.getUUID(), true);
            return true;
        }
        BlockPos console = consoleNear(player);
        if (console == null || user(player.serverLevel(), console) != null) {
            return false;
        }
        start(player, console);
        return true;
    }

    /** A right-click on the Cerebro console at {@code pos} (lane L4's block calls it). */
    public static InteractionResult useConsole(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        if (!level.getBlockState(pos).is(ModBlocks.CEREBRO_CONSOLE.get())) {
            return InteractionResult.PASS;
        }
        Session mine = SESSIONS.get(player.getUUID());
        if (mine != null && mine.console.equals(pos)) {
            end(player.server, player.getUUID(), true); // the helmet back on its stand
            return InteractionResult.SUCCESS;
        }
        UUID other = user(level, pos);
        if (other != null) {
            player.displayClientMessage(Component.translatable("message.enhanced.cerebro.busy"), true);
            return InteractionResult.CONSUME;
        }
        String refusal = refusal(player);
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal), true);
            return InteractionResult.CONSUME;
        }
        long resting = restingTicks(player);
        if (resting > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.cerebro.resting",
                    (resting + 19) / 20), true);
            return InteractionResult.CONSUME;
        }
        if (mine != null) {
            end(player.server, player.getUUID(), false);
        }
        start(player, pos);
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Who may, and where

    /** Whether the player is a telepath (Professor X, the Scarlet Witch, the Phoenix, Billy, a custom telepath). */
    public static boolean telepath(Player player) {
        return Traits.has(player, Trait.TELEPATH) || Traits.has(player, Trait.TELEPATH_STRONG);
    }

    /**
     * Why the player may not use a console (a message key), or null: not a telepath ({@code
     * cerebroConsoleTelepathsOnly}), or Magneto's helmet on his head.
     */
    @Nullable
    public static String refusal(Player player) {
        if (bool(XavierConfig.CEREBRO_CONSOLE_TELEPATHS_ONLY, true) && !telepath(player)) {
            return "message.enhanced.school_lab.not_telepath";
        }
        return Telepathy.isShielded(player) ? "message.enhanced.school_lab.helmet" : null;
    }

    /** The nearest console within {@code cerebroConsoleReach} of the player, or null. */
    @Nullable
    public static BlockPos consoleNear(ServerPlayer player) {
        int reach = config(XavierConfig.CEREBRO_CONSOLE_REACH, 4);
        BlockPos at = player.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-reach, -reach, -reach), at.offset(reach, reach, reach))) {
            if (player.level().getBlockState(pos).is(ModBlocks.CEREBRO_CONSOLE.get())) {
                double d = pos.distToCenterSqr(player.position());
                if (d <= (reach + 0.5) * (reach + 0.5) && d < bestDist) {
                    best = pos.immutable();
                    bestDist = d;
                }
            }
        }
        return best;
    }

    /** The centre of the sphere of the console at {@code pos} (as the plan's two points stand, turned with it). */
    public static BlockPos sphereCenter(BlockState console, BlockPos pos) {
        return console.hasProperty(CerebroConsoleBlock.FACING)
                ? pos.relative(console.getValue(CerebroConsoleBlock.FACING), CONSOLE_TO_CENTER) : pos;
    }

    /** Who wears the helmet of the console at {@code pos}, or null. */
    @Nullable
    public static UUID user(ServerLevel level, BlockPos pos) {
        for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
            if (entry.getValue().console.equals(pos) && entry.getValue().dimension == level.dimension()) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** How long until the player's Cerebro is ready again (ticks; 0: ready): its key's cooldown, or the room's. */
    public static long restingTicks(ServerPlayer player) {
        long now = player.level().getGameTime();
        PowerData data = Powers.get(player);
        long left = 0;
        int slot = data.slot(HeroAbility.XAVIER_CEREBRO);
        if (slot != 0 && !data.isReady(slot, now)) {
            left = data.cooldownUntil(slot) - now;
        }
        Long until = RESTING.get(player.getUUID());
        if (until != null && until > now && until - now <= config(XavierConfig.CEREBRO_COOLDOWN, 1200)) {
            left = Math.max(left, until - now);
        }
        return left;
    }

    /** Whether the player's Cerebro is the amplified one now. */
    public static boolean amplifying(Player player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** The console the player wears the helmet of, or empty. */
    public static Optional<GlobalPos> console(Player player) {
        Session session = SESSIONS.get(player.getUUID());
        return session == null ? Optional.empty() : Optional.of(GlobalPos.of(session.dimension, session.console));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The amplified Cerebro

    private static void start(ServerPlayer player, BlockPos console) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        int ticks = config(XavierConfig.CEREBRO_AMPLIFIED_TICKS, 600);
        Session session = new Session(console, level.dimension(), now + ticks);
        SESSIONS.put(player.getUUID(), session);
        PowerData data = Powers.get(player);
        int cooldown = config(XavierConfig.CEREBRO_COOLDOWN, 1200);
        data.startCooldown(HeroAbility.XAVIER_CEREBRO, now, cooldown);
        RESTING.put(player.getUUID(), now + cooldown);
        if (data.acts(Power.PROFESSOR_X)) {
            data.set(Live.CEREBRO, true); // the helmet on his head
        }
        List<CerebroPayload.Mind> found = minds(player);
        report(player, session, now, found);
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player && found.stream().anyMatch(m -> m.name().equals(other.getGameProfile().getName()))) {
                other.displayClientMessage(Component.translatable("message.enhanced.xavier.cerebro.found_you",
                        player.getDisplayName()), true);
            }
        }
        player.displayClientMessage(Component.translatable("message.enhanced.cerebro.amplified", found.size()), true);
        level.playSound(null, console, ModSounds.CEREBRO_CONSOLE_ON.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        PacketDistributor.sendToPlayersNear(level, null, console.getX() + 0.5, console.getY() + 0.5,
                console.getZ() + 0.5, 64.0, new LabFxPayload(LabFxPayload.CONSOLE_ON, console));
    }

    /** Every server tick: the sessions report, and end when they should. */
    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Session>> it = new ArrayList<>(SESSIONS.entrySet()).iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            Session session = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !stays(player, session)) {
                end(server, entry.getKey(), true);
                continue;
            }
            long now = player.level().getGameTime();
            if (now >= session.until) {
                end(server, entry.getKey(), true);
            } else if ((session.until - now) % PING == 0) {
                report(player, session, now, minds(player));
            }
        }
    }

    /** Whether the session goes on: alive, in its dimension, near its console, the console still there. */
    private static boolean stays(ServerPlayer player, Session session) {
        if (!player.isAlive() || player.isRemoved() || player.level().dimension() != session.dimension
                || refusal(player) != null) {
            return false;
        }
        int leash = config(XavierConfig.CEREBRO_CONSOLE_REACH, 4) + LEASH;
        return session.console.distToCenterSqr(player.position()) <= (double) leash * leash
                && player.level().getBlockState(session.console).is(ModBlocks.CEREBRO_CONSOLE.get());
    }

    /** One report: to him, and to everyone else standing in the sphere. */
    private static void report(ServerPlayer player, Session session, long now, List<CerebroPayload.Mind> minds) {
        int left = (int) Math.max(0, session.until - now);
        List<CerebroPayload.Site> sites = sites(player);
        player.connection.send(new CerebroPayload(minds, left, true, sites));
        ServerLevel level = player.serverLevel();
        BlockPos center = sphereCenter(level.getBlockState(session.console), session.console);
        double r = SchoolLabPlan.CEREBRO_RADIUS + 1.0;
        Set<UUID> inside = new HashSet<>();
        for (ServerPlayer other : level.players()) {
            if (other != player && !other.isSpectator() && !Powers.get(other).is(Live.CEREBRO)
                    && other.position().distanceToSqr(Vec3.atCenterOf(center)) <= r * r) {
                inside.add(other.getUUID());
                other.connection.send(new CerebroPayload(minds, Math.min(left, VISITOR_TICKS), true, sites));
            }
        }
        session.visitors.clear();
        session.visitors.addAll(inside);
    }

    /** The helmet comes off: his Cerebro goes dark, and the room's. */
    private static void end(MinecraftServer server, UUID id, boolean sound) {
        Session session = SESSIONS.remove(id);
        if (session == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            PowerData data = Powers.get(player);
            if (data.is(Live.CEREBRO)) {
                data.set(Live.CEREBRO, false);
            }
            player.connection.send(new CerebroPayload(List.of(), 0));
        }
        for (UUID visitor : session.visitors) {
            ServerPlayer other = server.getPlayerList().getPlayer(visitor);
            if (other != null) {
                other.connection.send(new CerebroPayload(List.of(), 0));
            }
        }
        ServerLevel level = server.getLevel(session.dimension);
        if (level != null) {
            BlockPos at = session.console;
            if (sound) {
                level.playSound(null, at, ModSounds.CEREBRO_CONSOLE_OFF.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            PacketDistributor.sendToPlayersNear(level, null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 64.0,
                    new LabFxPayload(LabFxPayload.CONSOLE_OFF, at));
        }
    }

    /**
     * The minds the amplified Cerebro finds: every player of every dimension ({@code cerebroAmplifiedAllDimensions};
     * else his own), no range limit, not Spectators, not under Magneto's helmet; his dimension's first (the nearest
     * first), then the others by dimension and name; each other dimension's mind marked with its dimension.
     */
    public static List<CerebroPayload.Mind> minds(ServerPlayer player) {
        boolean all = bool(XavierConfig.CEREBRO_AMPLIFIED_ALL_DIMENSIONS, true);
        ResourceKey<Level> home = player.level().dimension();
        List<ServerPlayer> found = new ArrayList<>();
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player && other.isAlive() && !other.isSpectator() && !Telepathy.isShielded(other)
                    && (all || other.level().dimension() == home)) {
                found.add(other);
            }
        }
        found.sort(Comparator.<ServerPlayer>comparingInt(o -> o.level().dimension() == home ? 0 : 1)
                .thenComparing(o -> o.level().dimension().location().toString())
                .thenComparingDouble(o -> o.level().dimension() == home ? o.distanceToSqr(player) : 0.0)
                .thenComparing(o -> o.getGameProfile().getName()));
        List<CerebroPayload.Mind> minds = new ArrayList<>();
        for (ServerPlayer other : found) {
            if (minds.size() >= CerebroPayload.MAX) {
                break;
            }
            String dimension = other.level().dimension() == home ? "" : other.level().dimension().location().toString();
            minds.add(new CerebroPayload.Mind(other.getGameProfile().getName(), Powers.power(other).ordinal(),
                    other.position(), dimension));
        }
        return minds;
    }

    /** The nearest site of each kind in his dimension found so far ({@code cerebroShowsStoneSites}), one per kind. */
    public static List<CerebroPayload.Site> sites(ServerPlayer player) {
        List<CerebroPayload.Site> sites = new ArrayList<>();
        if (!bool(XavierConfig.CEREBRO_SHOWS_STONE_SITES, true)) {
            return sites;
        }
        for (StoneKind kind : StoneKind.values()) {
            SiteSearch.nearest(player, kind).ifPresent(at -> sites.add(new CerebroPayload.Site(kind.ordinal(),
                    at.pos())));
            if (sites.size() >= CerebroPayload.MAX_SITES) {
                break;
            }
        }
        return sites;
    }

    /** The player left, died or changed dimension: his helmet comes off. */
    public static void forget(ServerPlayer player) {
        end(player.server, player.getUUID(), true);
    }

    /** Forget everything (the server stopped). */
    public static void clear() {
        SESSIONS.clear();
        RESTING.clear();
    }

    private static int config(ModConfigSpec.IntValue value, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    private static boolean bool(ModConfigSpec.BooleanValue value, boolean fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    private CerebroRoom() {
    }
}
