package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.config.HeimdallConfig;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTicketTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The Guardian's Watch (3.5): how a player becomes worthy of Hofund. Heimdall (the NPC in the Observatory) offers it:
 * seven Shadows of Hela hide in the city; see them as he sees them, and strike them before the sand runs out
 * ({@code heimdall.watchSeconds}). Anyone may take it, hero or not, one Watch at a time.
 *
 * <ul>
 * <li><b>The shadows</b> ({@code enhanced:hela_shadow}) wait at seven of {@link CityPlan#hidingSpots()}, chosen by the
 * player and his try ({@link #pick}); each drifts round its spot and darts away when he comes within
 * {@link #FLEE_NEAR} blocks. They are his alone: nobody else can harm them, and one blow of his strikes one. Their chunks are
 * held loaded while the Watch runs.</li>
 * <li><b>He sees them</b>: the Watch lends him Heimdall's Sight (the shadows marked in gold through every wall, a needle to
 * the nearest, his client's {@code HeimdallSightView}); a bar counts the time down and the shadows found.</li>
 * <li><b>Win</b> (all seven struck in time): {@link AsgardProgress#WATCH_WON}, Hofund bound to him
 * ({@link HofundBond}), and the Bifrost brings him back to the Observatory. A powerless player who then holds his own Hofund
 * becomes Heimdall ({@link HeimdallServer}).</li>
 * <li><b>Lose</b> (the time, his death, leaving Asgard, logging out): the shadows slip away, and he may try again after
 * {@code heimdall.watchRetry} ticks.</li>
 * </ul>
 */
public final class GuardiansWatch {
    /** How many shadows hide. */
    public static final int SHADOWS = 7;
    /** A shadow darts away when its watcher comes this close (blocks), at most this far, and not again for a while (ticks). */
    public static final double FLEE_NEAR = 3.0;
    public static final double FLEE_DISTANCE = 4.0;
    public static final int FLEE_REST = 40;
    /** How far a shadow drifts round its spot (blocks). */
    public static final double DRIFT = 1.0;

    /** One shadow of a Watch. */
    static final class Shadow {
        final BlockPos spot;
        /** The shadow itself (held, not looked up: in a chunk whose entities are not shown yet it is still there). */
        @Nullable
        Entity entity;
        boolean struck;
        /** Where it hides now (its spot, or where it fled to). */
        Vec3 home;
        long fledAt = -FLEE_REST;

        Shadow(BlockPos spot) {
            this.spot = spot;
            this.home = Vec3.atBottomCenterOf(spot).add(0, 0.6, 0);
        }
    }

    /** One player's Watch. */
    static final class Watch {
        final UUID watcher;
        final List<Shadow> shadows;
        final long start;
        final long end;
        final ServerBossEvent bar;
        final List<ChunkPos> held = new ArrayList<>();

        Watch(UUID watcher, List<Shadow> shadows, long start, long end) {
            this.watcher = watcher;
            this.shadows = shadows;
            this.start = start;
            this.end = end;
            this.bar = new ServerBossEvent(Component.translatable("boss.enhanced.watch"), BossEvent.BossBarColor.YELLOW,
                    BossEvent.BossBarOverlay.NOTCHED_10);
        }

        int found() {
            int n = 0;
            for (Shadow s : shadows) {
                n += s.struck ? 1 : 0;
            }
            return n;
        }
    }

    private static final Map<UUID, Watch> WATCHES = new HashMap<>();
    /** The shadow's UUID -> its watcher's. */
    private static final Map<UUID, UUID> OWNERS = new HashMap<>();
    /** How many Watches each player has begun since the server started (a new try hides the shadows elsewhere). */
    private static final Map<UUID, Integer> TRIES = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // Queries

    /** Whether this player keeps a Watch now. */
    public static boolean watching(Player player) {
        return WATCHES.containsKey(player.getUUID());
    }

    /** How many of his shadows he has struck, or -1 when he keeps no Watch. */
    public static int found(Player player) {
        Watch watch = WATCHES.get(player.getUUID());
        return watch == null ? -1 : watch.found();
    }

    /** The ticks his Watch has left, or -1. */
    public static long ticksLeft(Player player, long now) {
        Watch watch = WATCHES.get(player.getUUID());
        return watch == null ? -1 : Math.max(0, watch.end - now);
    }

    /** The shadows of his Watch still hidden (alive in the world). */
    public static List<Entity> shadowsOf(ServerPlayer player) {
        Watch watch = WATCHES.get(player.getUUID());
        ServerLevel level = AsgardDimension.level(player.server);
        if (watch == null || level == null) {
            return List.of();
        }
        List<Entity> out = new ArrayList<>();
        for (Shadow s : watch.shadows) {
            if (!s.struck && s.entity != null && !s.entity.isRemoved()) {
                out.add(s.entity);
            }
        }
        return out;
    }

    /** The watcher a shadow hides from, or null (not a shadow of any Watch). */
    @Nullable
    public static UUID watcherOf(Entity shadow) {
        return OWNERS.get(shadow.getUUID());
    }

    /** The seconds of a Watch ({@code heimdall.watchSeconds}). */
    public static int seconds() {
        return EnhancedConfig.SPEC.isLoaded() ? HeimdallConfig.WATCH_SECONDS.get() : 180;
    }

    private static int retryTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? HeimdallConfig.WATCH_RETRY.get() : 1200;
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    /**
     * Why this player may not take the Watch now (the lang key of what Heimdall tells him), or null when he may: one at a
     * time, not once it is won, not before he has rested after a loss, and only in Asgard.
     */
    @Nullable
    public static String refusal(ServerPlayer player) {
        AsgardProgress progress = ModAttachments.ASGARD.get(player);
        if (watching(player)) {
            return "message.enhanced.watch.already";
        }
        if (progress.has(AsgardProgress.WATCH_WON)) {
            return "message.enhanced.watch.done";
        }
        if (now(player) < progress.watchReadyAt()) {
            return "message.enhanced.watch.rest";
        }
        if (!AsgardDimension.in(player) || CityPlan.hidingSpots().size() < SHADOWS) {
            return "message.enhanced.watch.not_here";
        }
        return null;
    }

    /**
     * The spots of a Watch: {@code count} of {@code spots}, shuffled by the player and his try. Pure, the same for the same
     * arguments.
     */
    public static List<BlockPos> pick(UUID player, int tries, List<BlockPos> spots, int count) {
        List<BlockPos> all = new ArrayList<>(spots);
        long seed = player.getMostSignificantBits() ^ Long.rotateLeft(player.getLeastSignificantBits(), 17) ^ tries * 0x9E3779B97F4A7C15L;
        Collections.shuffle(all, new Random(seed));
        return List.copyOf(all.subList(0, Math.min(count, all.size())));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Watch

    /** Begins his Watch, if he may ({@link #refusal}); he is told why not. Returns whether it began. */
    public static boolean start(ServerPlayer player) {
        String why = refusal(player);
        if (why != null) {
            long rest = Math.max(0, ModAttachments.ASGARD.get(player).watchReadyAt() - now(player));
            player.displayClientMessage(Component.translatable(why, (rest + 19) / 20), true);
            return false;
        }
        ServerLevel level = AsgardDimension.level(player.server);
        if (level == null) {
            return false;
        }
        int tries = TRIES.merge(player.getUUID(), 1, Integer::sum) - 1;
        List<Shadow> shadows = new ArrayList<>();
        for (BlockPos spot : pick(player.getUUID(), tries, CityPlan.hidingSpots(), SHADOWS)) {
            shadows.add(new Shadow(spot));
        }
        long now = now(player);
        Watch watch = new Watch(player.getUUID(), shadows, now, now + seconds() * 20L);
        WATCHES.put(player.getUUID(), watch);
        for (Shadow s : shadows) {
            ChunkPos chunk = new ChunkPos(s.spot);
            if (!watch.held.contains(chunk)) {
                watch.held.add(chunk);
                level.getChunkSource().addRegionTicket(ModTicketTypes.ENHANCED_ASGARD, chunk, 2, player.getUUID());
            }
            spawn(level, watch, s);
        }
        watch.bar.addPlayer(player);
        updateBar(watch, now);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WATCH_START.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        player.connection.send(new ClientboundSoundEntityPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.MUSIC_WATCH.get()),
                SoundSource.MUSIC, player, 1.0F, 1.0F, player.getRandom().nextLong()));
        player.sendSystemMessage(Component.translatable("message.enhanced.watch.begins", SHADOWS, seconds() / 60).withStyle(ChatFormatting.GOLD));
        report(player, watch, now);
        HeimdallSight.send(player);
        return true;
    }

    private static void spawn(ServerLevel level, Watch watch, Shadow s) {
        level.getChunk(s.spot.getX() >> 4, s.spot.getZ() >> 4);
        Mob shadow = ModEntities.HELA_SHADOW.get().create(level);
        if (shadow == null) {
            return;
        }
        shadow.moveTo(s.home.x, s.home.y, s.home.z, level.random.nextFloat() * 360.0F, 0.0F);
        shadow.setPersistenceRequired();
        shadow.setSilent(true);
        if (level.addFreshEntity(shadow)) {
            s.entity = shadow;
            OWNERS.put(shadow.getUUID(), watch.watcher);
        }
    }

    /** Every player, every tick: the Watch he keeps goes on (the shadows drift and flee, the sand runs). */
    public static void tick(ServerPlayer player) {
        Watch watch = WATCHES.get(player.getUUID());
        if (watch == null) {
            return;
        }
        long now = now(player);
        if (!AsgardDimension.in(player)) {
            lose(player, "message.enhanced.watch.left");
            return;
        }
        if (now >= watch.end) {
            lose(player, "message.enhanced.watch.lost");
            return;
        }
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < watch.shadows.size(); i++) {
            Shadow s = watch.shadows.get(i);
            if (s.struck) {
                continue;
            }
            Entity e = s.entity;
            if (e == null || e.isRemoved() || !e.isAlive()) {
                // Gone some other way (an unloaded chunk, a command): it hides again where it was.
                if (e != null) {
                    OWNERS.remove(e.getUUID());
                    e.discard();
                }
                s.entity = null;
                spawn(level, watch, s);
                continue;
            }
            drift(player, s, e, i, now);
        }
        if ((now - watch.start) % 20 == 0) {
            updateBar(watch, now);
            report(player, watch, now);
        }
        HeimdallAbilities.nightVision(player);
        if (player.tickCount % HeimdallSight.EVERY == 0) {
            HeimdallSight.send(player);
        }
    }

    /** A shadow bobs and drifts round where it hides, and darts away from its watcher when he comes close. */
    private static void drift(ServerPlayer watcher, Shadow s, Entity e, int index, long now) {
        if (watcher.distanceToSqr(e) < FLEE_NEAR * FLEE_NEAR && now - s.fledAt >= FLEE_REST) {
            Vec3 away = e.position().subtract(watcher.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            for (double d = FLEE_DISTANCE; d >= 1.0; d -= 1.0) {
                Vec3 to = e.position().add(away.scale(d));
                if (e.level().noCollision(e, e.getBoundingBox().move(to.subtract(e.position())))) {
                    s.home = to;
                    break;
                }
            }
            s.fledAt = now;
        }
        double t = now * 0.05 + index * 1.7;
        Vec3 want = s.home.add(Math.sin(t) * DRIFT, Math.sin(now * 0.12 + index) * 0.25, Math.cos(t * 0.8) * DRIFT);
        if (!e.level().noCollision(e, e.getBoundingBox().move(want.subtract(e.position())))) {
            want = s.home;
        }
        Vec3 at = e.position().lerp(want, now - s.fledAt < 8 ? 0.35 : 0.12);
        e.setPos(at.x, at.y, at.z);
        e.setDeltaMovement(Vec3.ZERO);
    }

    private static void updateBar(Watch watch, long now) {
        long left = Math.max(0, watch.end - now);
        float total = Math.max(1, watch.end - watch.start);
        watch.bar.setProgress(Mth.clamp(left / total, 0.0F, 1.0F));
        long secs = (left + 19) / 20;
        watch.bar.setName(Component.translatable("boss.enhanced.watch.count", watch.found(), SHADOWS,
                String.format(Locale.ROOT, "%d:%02d", secs / 60, secs % 60)));
    }

    /** His client is told where the shadows still hiding are (the gold marks, the needle) and the seconds left. */
    private static void report(ServerPlayer player, Watch watch, long now) {
        List<Vec3> left = new ArrayList<>();
        for (Shadow s : watch.shadows) {
            if (!s.struck) {
                left.add(s.entity != null ? s.entity.position() : s.home);
            }
        }
        HeimdallFx.to(player, HeimdallFx.WATCH, -1, left, (int) ((Math.max(0, watch.end - now) + 19) / 20));
    }

    /**
     * Someone struck at a shadow: its own watcher strikes it (it is gone in a puff of black and green, and the Watch may
     * be won); anyone else's blow does nothing. Returns whether it was its watcher's.
     */
    public static boolean strike(Entity shadow, @Nullable Entity attacker) {
        UUID watcherId = OWNERS.get(shadow.getUUID());
        if (watcherId == null || attacker == null || !attacker.getUUID().equals(watcherId) || !(attacker instanceof ServerPlayer watcher)) {
            return false;
        }
        Watch watch = WATCHES.get(watcherId);
        if (watch == null) {
            return false;
        }
        for (Shadow s : watch.shadows) {
            if (s.entity != null && shadow.getUUID().equals(s.entity.getUUID()) && !s.struck) {
                s.struck = true;
                OWNERS.remove(shadow.getUUID());
                ServerLevel level = (ServerLevel) shadow.level();
                level.sendParticles(ParticleTypes.LARGE_SMOKE, shadow.getX(), shadow.getY() + 0.3, shadow.getZ(), 16, 0.25, 0.25, 0.25, 0.02);
                level.playSound(null, shadow.getX(), shadow.getY(), shadow.getZ(), ModSounds.SHADOW_STRUCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                HeimdallFx.near(level, shadow.position(), HeimdallFx.SHADOW_STRUCK, watcher, List.of(shadow.position()), 0);
                shadow.discard();
                int found = watch.found();
                long now = now(watcher);
                updateBar(watch, now);
                if (found >= watch.shadows.size()) {
                    win(watcher);
                } else {
                    watcher.displayClientMessage(Component.translatable("message.enhanced.watch.struck", found, SHADOWS), true);
                    report(watcher, watch, now);
                }
                return true;
            }
        }
        return false;
    }

    /** Every shadow struck in time: the Watch is his, and so is Hofund. */
    static void win(ServerPlayer player) {
        Watch watch = WATCHES.get(player.getUUID());
        if (watch == null) {
            return;
        }
        end(player, watch);
        ModAttachments.ASGARD.set(player, ModAttachments.ASGARD.get(player).with(AsgardProgress.WATCH_WON).with(AsgardProgress.MET_HEIMDALL));
        HofundBond.give(player);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WATCH_WIN.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.enhanced.watch.won").withStyle(ChatFormatting.GOLD));
        // Heimdall brings him back to the Observatory by the Bifrost (unless he is there already).
        if (player.position().distanceToSqr(CityPlan.ARRIVAL) > CityPlan.OBSERVATORY_RADIUS * CityPlan.OBSERVATORY_RADIUS) {
            Bifrost.open(player, Bifrost.Destination.OBSERVATORY, Bifrost.Source.NPC);
        }
    }

    /** The Watch is lost ({@code why}: the lang key he is told): the shadows slip away, and he must rest before he tries again. */
    public static void lose(ServerPlayer player, String why) {
        Watch watch = WATCHES.get(player.getUUID());
        if (watch == null) {
            return;
        }
        end(player, watch);
        ModAttachments.ASGARD.set(player, ModAttachments.ASGARD.get(player).withWatchReadyAt(now(player) + retryTicks()));
        if (!player.hasDisconnected()) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WATCH_LOSE.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
            player.sendSystemMessage(Component.translatable(why).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Ends a Watch: its shadows, chunks, bar, music and marks are gone. */
    private static void end(ServerPlayer player, Watch watch) {
        WATCHES.remove(watch.watcher);
        ServerLevel level = AsgardDimension.level(player.server);
        for (Shadow s : watch.shadows) {
            if (s.entity != null) {
                OWNERS.remove(s.entity.getUUID());
                s.entity.discard();
            }
        }
        if (level != null) {
            for (ChunkPos chunk : watch.held) {
                level.getChunkSource().removeRegionTicket(ModTicketTypes.ENHANCED_ASGARD, chunk, 2, watch.watcher);
            }
        }
        watch.bar.removeAllPlayers();
        if (!player.hasDisconnected()) {
            player.connection.send(new ClientboundStopSoundPacket(ModSounds.MUSIC_WATCH.getId(), SoundSource.MUSIC));
            HeimdallFx.to(player, HeimdallFx.WATCH, -1, List.of(), -1);
            if (!HeimdallSight.open(player)) {
                HeimdallAbilities.clearNightVision(player);
                HeimdallSight.stop(player);
            }
        }
    }

    /** The server stopped: every Watch is forgotten (the shadows are never saved). */
    static void clear() {
        WATCHES.clear();
        OWNERS.clear();
        TRIES.clear();
    }

    private GuardiansWatch() {
    }
}
