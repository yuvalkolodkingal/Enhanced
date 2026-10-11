package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
     * already is: the Ultron hero's drones are Sentries of the same type as the boss's.)
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
                || type == ModEntities.SAND_BOULDER.get() || type == ModEntities.SAND_COCOON.get();
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
     * Wanda throws what she holds: a boss's special case for it. True if handled (the held thing is then not thrown).
     */
    public static boolean telekinesisLaunch(ServerPlayer wanda, Entity held) {
        for (BossFight fight : FIGHTS) {
            if (fight.level() == wanda.level() && fight.onTelekinesisLaunch(wanda, held)) {
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
