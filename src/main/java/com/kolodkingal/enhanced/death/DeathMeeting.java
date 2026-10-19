package com.kolodkingal.enhanced.death;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.DeathMeetingConfig;
import com.kolodkingal.enhanced.network.DeathMeetingPayload;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The meeting with Death (3.7, Eilon's request: "every time you die you meet Death, and she lets you start again or
 * start again in Hell"). A player who dies is offered the meeting ({@link #offer}: a {@link DeathMeetingPayload.Offer}
 * sent in the death's last handler, before vanilla's death packet), and his client shows Rio Vidal, the Green Witch,
 * with two roads: "Start again" (vanilla's own respawn: his bed, his anchor or the world's spawn) and "Start again in
 * the Nether" (the {@link DeathMeetingPayload.Choose} answer, {@link #choose}): the server respawns him, through
 * vanilla's respawn, onto a safe spot in the Nether near his spawn point divided by 8 ({@link NetherLanding}), with a
 * few seconds of Fire Resistance.
 *
 * <p>How it lives with the mod's other deaths: every flow that saves a player cancels the death before this (the
 * healing factor's DOWN, the Witches' Road trial, Ultron's Second Life, a totem), so Death never comes; the Vormir
 * sacrifice and the boss arenas let him die and change nothing about where he wakes, so both roads stay open; the
 * respawn's own handlers (the Hex, the Road's reconcile, Asgard's kept things) run after the Nether choice as after
 * any respawn, and a handler that already changed where he respawns ({@code PlayerRespawnPositionEvent}) wins over
 * the Nether. A hardcore world and {@code doImmediateRespawn} keep vanilla's way: no offer, and the Nether is refused.
 * Server only.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class DeathMeeting {
    /** The choices of {@link DeathMeetingPayload.Choose}: start again at home (vanilla), or in the Nether. */
    public static final int AGAIN = 0;
    public static final int NETHER = 1;

    /** What {@link #choose} did. */
    public enum Result {
        /** Nothing: he is alive (or already gone). */
        REFUSED,
        /** He started again as vanilla starts him. */
        AGAIN,
        /** He started again in the Nether. */
        NETHER
    }

    /** Fire Resistance's default seconds (before the config has loaded). */
    static final int DEFAULT_FIRE_SECONDS = 10;
    /** Rio's green: the line he is told when he wakes in the Nether. */
    public static final int GREEN = 0x7FD48A;

    /** Players whose respawn under way goes to the Nether (set only inside {@link #choose}). */
    private static final Set<UUID> TO_NETHER = ConcurrentHashMap.newKeySet();
    /** Players respawned in the Nether whose welcome waits for the respawn event. */
    private static final Set<UUID> ARRIVING = ConcurrentHashMap.newKeySet();

    private DeathMeeting() {
    }

    // ---- the rules ----

    /** Whether Death meets the players at all ({@code [death_meeting] enabled}; true before the config has loaded). */
    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || DeathMeetingConfig.ENABLED.get();
    }

    static boolean allowNether() {
        return !EnhancedConfig.SPEC.isLoaded() || DeathMeetingConfig.ALLOW_NETHER.get();
    }

    /** Seconds of Fire Resistance on a Nether start. */
    public static int fireSeconds() {
        return EnhancedConfig.SPEC.isLoaded() ? DeathMeetingConfig.FIRE_RESISTANCE_SECONDS.get() : DEFAULT_FIRE_SECONDS;
    }

    /**
     * Whether vanilla's way holds on this server: a hardcore world, or {@code doImmediateRespawn} (no screen at all).
     */
    public static boolean vanillaWay(ServerPlayer player) {
        return player.server.isHardcore()
                || player.serverLevel().getGameRules().getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN);
    }

    /** Whether the road to the Nether is open: allowed by the config, the server lets players go there, it exists. */
    public static boolean netherOpen(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        return allowNether() && nether != null && server.isLevelEnabled(nether);
    }

    /** What Death offers this player now, or null (vanilla's screen). */
    @Nullable
    public static DeathMeetingPayload.Offer offer(ServerPlayer player) {
        if (!enabled() || vanillaWay(player)) {
            return null;
        }
        return new DeathMeetingPayload.Offer(netherOpen(player.server), fireSeconds());
    }

    /** Sends the offer to a dead player (nothing when there is none). */
    static void tell(ServerPlayer player) {
        DeathMeetingPayload.Offer offer = offer(player);
        if (offer != null) {
            PacketDistributor.sendToPlayer(player, offer);
        }
    }

    // ---- the choice ----

    /**
     * A player's answer: only a dead player is heard (a living one, or a body already respawned, is refused and nothing
     * happens). He starts again through vanilla's own respawn; in the Nether when he chose it and the road is open
     * (Death on, the Nether allowed and existing, not hardcore, no {@code doImmediateRespawn}), else as vanilla starts
     * him, so a stale screen never leaves him dead.
     */
    public static Result choose(ServerPlayer player, int choice) {
        if (!player.isDeadOrDying() || player.isRemoved() || player.wonGame) {
            return Result.REFUSED;
        }
        boolean nether = choice == NETHER && enabled() && !vanillaWay(player) && netherOpen(player.server);
        UUID id = player.getUUID();
        if (nether) {
            TO_NETHER.add(id);
        }
        try {
            player.connection.handleClientCommand(
                    new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        } finally {
            TO_NETHER.remove(id);
        }
        return nether ? Result.NETHER : Result.AGAIN;
    }

    // ---- the events ----

    /**
     * The last handler of a death that happens (a cancelled one never gets here): Death's offer, before vanilla's
     * packet.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tell(player);
        }
    }

    /** A player who logs in dead (he left on the death screen) meets her again. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isDeadOrDying()) {
            tell(player);
        }
    }

    /**
     * Where a player who chose the Nether stands up. Low priority: a handler that already changed his respawn wins. A
     * respawn that already goes to the Nether (his charged anchor there) stays as it is.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRespawnPosition(PlayerRespawnPositionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !TO_NETHER.contains(player.getUUID())
                || event.isFromEndFight() || event.getDimensionTransition() != event.getOriginalDimensionTransition()) {
            return;
        }
        ServerLevel nether = player.server.getLevel(Level.NETHER);
        if (nether == null) {
            return;
        }
        DimensionTransition home = event.getDimensionTransition();
        ARRIVING.add(player.getUUID());
        if (home.newLevel() == nether) {
            return;
        }
        BlockPos centre = NetherLanding.centre(nether, home.newLevel().dimensionType(), home.pos());
        BlockPos feet = NetherLanding.land(nether, centre);
        event.setDimensionTransition(new DimensionTransition(nether, Vec3.atBottomCenterOf(feet), Vec3.ZERO,
                home.yRot(), 0.0F, home.postDimensionTransition()));
    }

    /** He wakes in the Nether: Fire Resistance, Rio's line, her green magic round him. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ARRIVING.remove(player.getUUID())) {
            return;
        }
        int seconds = fireSeconds();
        if (seconds > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, seconds * 20, 0, false, true, true));
            player.sendSystemMessage(Component.translatable("message.enhanced.death_meeting.nether", seconds)
                    .withColor(GREEN));
        } else {
            player.sendSystemMessage(Component.translatable("message.enhanced.death_meeting.nether_plain")
                    .withColor(GREEN));
        }
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RIO_BLESSING.get(),
                SoundSource.PLAYERS, 0.8F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.6,
                0.8, 0.6, 0.0);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, player.getX(), player.getY() + 1.6, player.getZ(), 16, 0.8,
                0.5, 0.8, 0.0);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ARRIVING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TO_NETHER.clear();
        ARRIVING.clear();
    }
}
