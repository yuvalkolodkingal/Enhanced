package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardRules;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.NpcState;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.config.HeimdallConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Heimdall's post (3.5.1): there is one of each character, but Heimdall is the exception. When a player is Heimdall, the
 * Heimdall of the city stays, and on their first meeting (he comes within {@link #MEET_RANGE} blocks) and again whenever
 * he talks to him, the city's Heimdall hands him his post for {@code heimdall.postSeconds} (10 minutes): "We are both
 * guardians of Asgard." He steps aside to a rest spot nearby and watches; a gold bar counts the time down.
 *
 * <p>While he keeps the post, the keeper:
 * <ul>
 *   <li>has the Hofund socket answer to him ({@link #touchSocket}): the mechanism spins up, and the Bifrost screen opens
 *   to send the nearest traveller in the Observatory (the city's Heimdall's own service);</li>
 *   <li>sends others by the Bifrost from the Observatory with no cooldown ({@link #sendsFreely}).</li>
 * </ul>
 * One keeper at a time. The post ends when its time runs out, when he gives it back (in the talk), stops being Heimdall,
 * leaves Asgard, dies or logs out; then the city's Heimdall walks back to his post and thanks him.
 */
public final class HeimdallPost {
    /** How near the city's Heimdall a player who is Heimdall must come for their first meeting. */
    public static final double MEET_RANGE = 6.0;
    /** How near the Observatory's centre the keeper sends freely. */
    public static final double OBSERVATORY_REACH = CityPlan.OBSERVATORY_RADIUS + 4.0;
    /** Where Heimdall may rest, from his post: the first spot with ground to stand on. */
    private static final int[][] REST_SPOTS = {{-5, 0, -2}, {5, 0, -2}, {-4, 0, 2}, {4, 0, 2}, {-3, 0, 0}, {3, 0, 0}};
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.35F), 1.2F);

    /** A post kept: by whom, for which Asgardian, until when, and its bar. */
    private static final class Kept {
        final UUID keeper;
        final UUID npc;
        final long start;
        final long end;
        final ServerBossEvent bar;

        Kept(UUID keeper, UUID npc, long start, long end) {
            this.keeper = keeper;
            this.npc = npc;
            this.start = start;
            this.end = end;
            this.bar = new ServerBossEvent(Component.translatable("boss.enhanced.post"), BossEvent.BossBarColor.YELLOW,
                    BossEvent.BossBarOverlay.PROGRESS);
        }
    }

    @Nullable
    private static Kept kept;
    /** The players who are Heimdall and have met the city's Heimdall since they logged in (the first meeting is once). */
    private static final Set<UUID> MET = new HashSet<>();

    private HeimdallPost() {
    }

    /** {@code heimdall.postSeconds}. */
    public static int seconds() {
        return EnhancedConfig.SPEC.isLoaded() ? HeimdallConfig.POST_SECONDS.get() : 600;
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    // ---- who keeps it ----

    /** Whether this player keeps Heimdall's post now. */
    public static boolean keeps(ServerPlayer player) {
        return kept != null && kept.keeper.equals(player.getUUID());
    }

    /** The UUID of the player who keeps the post, or null. */
    @Nullable
    public static UUID keeper() {
        return kept == null ? null : kept.keeper;
    }

    /** The keeper's name, or "" (for the city's Heimdall's line to others). */
    public static String keeperName(net.minecraft.server.MinecraftServer server) {
        ServerPlayer keeper = kept == null ? null : server.getPlayerList().getPlayer(kept.keeper);
        return keeper == null ? "" : keeper.getGameProfile().getName();
    }

    /** Whether another player than this one keeps the post. */
    public static boolean keptByAnother(ServerPlayer player) {
        return kept != null && !kept.keeper.equals(player.getUUID());
    }

    /** Whether the city's Heimdall would hand his post to this player now: he is Heimdall, in Asgard, nobody keeps it. */
    public static boolean mayHandOver(ServerPlayer player) {
        return kept == null && isHeimdall(player) && AsgardDimension.in(player) && player.isAlive() && !player.isSpectator();
    }

    private static boolean isHeimdall(ServerPlayer player) {
        return com.kolodkingal.enhanced.power.Powers.power(player) == Power.HEIMDALL;
    }

    /** The ticks of the post left (0: none). */
    public static long ticksLeft(ServerPlayer player) {
        return keeps(player) ? Math.max(0, kept.end - now(player)) : 0;
    }

    /** The bar of the post (for the tests), or null. */
    @Nullable
    public static ServerBossEvent bar() {
        return kept == null ? null : kept.bar;
    }

    // ---- handing it over, giving it back ----

    /**
     * The city's Heimdall {@code npc} hands his post to {@code player}: he steps aside to his rest spot, the bar starts, a
     * shimmer of gold round the new keeper. Returns whether it began ({@link #mayHandOver}).
     */
    public static boolean handOver(ServerPlayer player, AsgardianEntity npc) {
        if (!mayHandOver(player) || npc.role() != Role.HEIMDALL || npc.summoned()) {
            return false;
        }
        long now = now(player);
        kept = new Kept(player.getUUID(), npc.getUUID(), now, now + seconds() * 20L);
        MET.add(player.getUUID());
        BlockPos post = npc.home() != null ? npc.home() : npc.blockPosition();
        npc.setRestAt(restSpot(npc.level() instanceof ServerLevel level ? level : player.serverLevel(), post));
        kept.bar.addPlayer(player);
        updateBar(player);
        ServerLevel level = player.serverLevel();
        level.sendParticles(GOLD, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WATCH_START.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("message.enhanced.post.begins",
                String.format(Locale.ROOT, "%d:%02d", seconds() / 60, seconds() % 60)).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** Where Heimdall rests while his post is kept: the first of {@link #REST_SPOTS} round it with ground, else the post. */
    static BlockPos restSpot(ServerLevel level, BlockPos post) {
        for (int[] o : REST_SPOTS) {
            BlockPos spot = com.kolodkingal.enhanced.asgard.npc.Asgardians.standOn(level, post.offset(o[0], o[1], o[2]));
            if (spot != null) {
                return spot;
            }
        }
        return post;
    }

    /**
     * The post ends: the bar goes, the city's Heimdall walks back to his post, and (if {@code thank}) thanks the keeper.
     * Nothing if this player does not keep it.
     */
    public static void end(ServerPlayer player, boolean thank) {
        if (!keeps(player)) {
            return;
        }
        Kept ended = kept;
        kept = null;
        ended.bar.removeAllPlayers();
        AsgardianEntity npc = npcOf(player.server, ended);
        if (npc != null) {
            npc.setRestAt(null);
        }
        if (thank && !player.hasDisconnected()) {
            if (npc != null && npc.level() == player.level()) {
                AsgardTalk.speak(npc, player, "npc.enhanced.heimdall.post_return", List.of());
            } else {
                player.sendSystemMessage(Component.translatable("npc.enhanced.heimdall.says",
                        Component.translatable("npc.enhanced.heimdall.post_return")).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    @Nullable
    private static AsgardianEntity npcOf(net.minecraft.server.MinecraftServer server, Kept post) {
        ServerLevel asgard = AsgardDimension.level(server);
        if (asgard != null && asgard.getEntity(post.npc) instanceof AsgardianEntity npc) {
            return npc;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(post.npc) instanceof AsgardianEntity npc) {
                return npc;
            }
        }
        return null;
    }

    /** The Heimdall of the city whose post the keeper keeps (for the tests), or null. */
    @Nullable
    public static AsgardianEntity npc(ServerPlayer player) {
        return keeps(player) ? npcOf(player.server, kept) : null;
    }

    // ---- every tick ----

    /** Every player, every tick: the first meeting of a player who is Heimdall, and the post he keeps going on or ending. */
    public static void tick(ServerPlayer player, PowerData data) {
        if (keeps(player)) {
            if (data.power() != Power.HEIMDALL || !AsgardDimension.in(player) || !player.isAlive()) {
                end(player, true);
                return;
            }
            long now = now(player);
            if (now >= kept.end) {
                end(player, true);
                return;
            }
            if ((now - kept.start) % 20 == 0) {
                updateBar(player);
            }
            return;
        }
        if (data.power() != Power.HEIMDALL) {
            MET.remove(player.getUUID());
            return;
        }
        if (kept == null && player.tickCount % 10 == 0 && !MET.contains(player.getUUID()) && AsgardDimension.in(player)) {
            meet(player);
        }
    }

    /** Their first meeting: the city's Heimdall, near and free, hands his post to the player who is Heimdall. */
    static boolean meet(ServerPlayer player) {
        for (AsgardianEntity npc : player.serverLevel().getEntitiesOfClass(AsgardianEntity.class, player.getBoundingBox().inflate(MEET_RANGE),
                e -> e.isAlive() && e.role() == Role.HEIMDALL && !e.summoned() && !e.isDown() && e.state() != NpcState.FIGHTING)) {
            if (handOver(player, npc)) {
                AsgardTalk.speak(npc, player, "npc.enhanced.heimdall.handover", List.of());
                npc.getLookControl().setLookAt(player, 30.0F, 30.0F);
                return true;
            }
        }
        return false;
    }

    private static void updateBar(ServerPlayer player) {
        long now = now(player);
        long left = Math.max(0, kept.end - now);
        float total = Math.max(1, kept.end - kept.start);
        kept.bar.setProgress(Mth.clamp(left / total, 0.0F, 1.0F));
        long secs = (left + 19) / 20;
        kept.bar.setName(Component.translatable("boss.enhanced.post.time", String.format(Locale.ROOT, "%d:%02d", secs / 60, secs % 60)));
    }

    // ---- the keeper's rights ----

    /**
     * The keeper touches the Hofund socket: it answers to him. The mechanism spins up, and the Bifrost screen opens in Send
     * mode for the nearest other player in the Observatory (if none, he is told nobody waits). Returns false if he does not
     * keep the post (the socket does what it always did).
     */
    public static boolean touchSocket(ServerPlayer player) {
        if (!keeps(player)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        AsgardRules.spinNear(level, Vec3.atCenterOf(CityPlan.MECHANISM));
        level.sendParticles(GOLD, CityPlan.OBSERVATORY_SOCKET.getX() + 0.5, CityPlan.OBSERVATORY_SOCKET.getY() + 1.0,
                CityPlan.OBSERVATORY_SOCKET.getZ() + 0.5, 20, 0.3, 0.5, 0.3, 0.0);
        ServerPlayer traveller = traveller(player);
        if (traveller == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.post.socket_nobody").withStyle(ChatFormatting.GOLD), true);
        } else {
            HeimdallFx.to(player, HeimdallFx.OPEN_SEND, traveller.getId(), List.of(traveller.position()), 0);
        }
        return true;
    }

    /** The nearest other player in the Observatory (within {@link #OBSERVATORY_REACH} of its centre), or null. */
    @Nullable
    static ServerPlayer traveller(ServerPlayer keeper) {
        Vec3 center = Vec3.atCenterOf(CityPlan.OBSERVATORY_CENTER);
        ServerPlayer best = null;
        for (ServerPlayer other : keeper.serverLevel().players()) {
            if (other == keeper || other.isSpectator() || other.position().distanceTo(center) > OBSERVATORY_REACH) {
                continue;
            }
            if (best == null || other.distanceToSqr(keeper) < best.distanceToSqr(keeper)) {
                best = other;
            }
        }
        return best;
    }

    /** Whether this player, keeping the post, stands in the Observatory: he sends others with no cooldown there. */
    public static boolean sendsFreely(ServerPlayer player) {
        return keeps(player) && AsgardDimension.in(player)
                && player.position().distanceTo(Vec3.atCenterOf(CityPlan.OBSERVATORY_CENTER)) <= OBSERVATORY_REACH;
    }

    // ---- lifecycle ----

    /** He logged out: the post ends without a word (the city's Heimdall goes back). */
    static void onGone(ServerPlayer player) {
        end(player, false);
        MET.remove(player.getUUID());
    }

    /** The server stopped. */
    static void clear() {
        kept = null;
        MET.clear();
    }

    /** Forgets that this player has met the city's Heimdall (for the tests). */
    public static void forgetMeeting(Entity player) {
        MET.remove(player.getUUID());
    }
}
