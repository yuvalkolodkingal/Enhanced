package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The one door the rest of the mod uses to ask about boss fights: whoever is in one, what a fight protects, where a
 * fight lets a teleport go, how to start and end one. The heroes (Magneto, Wanda, Tommy, Loki, Spider-Man, the Phoenix)
 * call these static methods and never a boss class, so a boss can change without touching them.
 *
 * <p>This is the stub the skeleton of 3.0 ships: no fight can run yet, so every question answers as if there were no
 * boss at all ({@link #isMinion} and {@link #protects} are false, {@link #allowsTeleport} true, and so on) and
 * {@link #start} starts nothing. The boss framework replaces the bodies, not the signatures.
 */
public final class BossFights {

    /** The fight running in this dimension, or null. */
    @Nullable
    public static BossFight active(ServerLevel level) {
        return null;
    }

    /** Whether a fight is running anywhere on the server. */
    public static boolean anyActive(MinecraftServer server) {
        return false;
    }

    /** Whether the entity is in a fight: a fighter, or something the fight made. */
    public static boolean isMember(Entity entity) {
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

    /** Whether the entity is a marked minion of a running fight (an Ultron Sentry, a Phantom, a Horseman's Husk). */
    public static boolean isMinion(Entity entity) {
        return false;
    }

    /** Whether the Hex lets this entity through its wall while a lift carries it (called from the Hex's judgement). */
    public static boolean isHexExempt(Entity entity) {
        return false;
    }

    /** Whether the arena of a fight lets this entity teleport to {@code to} (called from every teleport's gate). */
    public static boolean allowsTeleport(ServerLevel level, Entity entity, Vec3 to) {
        return true;
    }

    /**
     * Whether a fight protects the block at {@code pos}: an ability that would change it (lifting it, shaking it
     * loose, burning it, freezing the water over it, spinning a web on it) must not.
     */
    public static boolean protects(ServerLevel level, BlockPos pos) {
        return false;
    }

    /** Wanda pressed her telekinesis key: a boss's special case for it (Ultron's heart). True if handled. */
    public static boolean telekinesisPress(ServerPlayer wanda) {
        return false;
    }

    /** Wanda throws what she holds: a boss's special case for it. True if handled. */
    public static boolean telekinesisLaunch(ServerPlayer wanda, Entity held) {
        return false;
    }

    /**
     * Starts a fight against {@code kind}, called by the rituals and by {@code /enhanced boss start}.
     *
     * @param force an operator's start, which skips the ritual's own checks (the site, the items, a hero nearby)
     * @return whether a fight began
     */
    public static boolean start(BossKind kind, ServerPlayer starter, boolean force) {
        return false;
    }

    /** Ticks the fight of a dimension, once per level tick. */
    public static void tickLevel(ServerLevel level) {
    }

    /** Ends every fight (an operator's {@code stop}, or the server stopping), giving back what the rituals took. */
    public static void stopAll(MinecraftServer server, StopReason why) {
    }

    /** Forgets every fight without ending it (the server has stopped). */
    public static void clear() {
    }

    private BossFights() {
    }
}
