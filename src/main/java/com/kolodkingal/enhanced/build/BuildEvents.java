package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;

/**
 * The game-bus events of Build Magic (spec 5.6; R16: {@code CommonEvents} is not edited for it), at NORMAL priority
 * unless said:
 *
 * <ul>
 *     <li><b>The ticks</b>: {@code ServerTickEvent.Pre} sets the budget all spells share this tick; each level's
 *     {@code LevelTickEvent.Post} runs its jobs, inside the level tick (D-B 11.7); {@code ServerTickEvent.Post} seals
 *     the Paint strokes whose last dab was 40 ticks ago and sends each builder's pocket on to her client.</li>
 *     <li><b>Logout, death, respawn</b>: build mode goes off; the spells go on (with a fake player of the caster's
 *     profile while he is away, D-B 11.9); at the next login, what finished or stopped while he was away is said.</li>
 *     <li><b>A change of dimension</b>: build mode stays on, the box is forgotten; the spells go on in their own
 *     dimension (their tickets hold it); the pocket travels.</li>
 *     <li><b>Into Adventure or Spectator</b>: build mode goes off; the working spell stops at its next slice (its own
 *     check, D-B 11.7).</li>
 *     <li><b>The server's stop</b>: at LOW (R20), after the boss framework put every arena back (HIGHEST) and the Sokovia
 *     recovery cleared its islands (NORMAL), the spells left are finished while their cells add up to
 *     {@code finishOnStopBlocks}, the largest stopped first (R55); once stopped, everything is forgotten.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class BuildEvents {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        BuildScheduler.startTick(event.getServer());
    }

    /** After the levels' ticks: strokes whose last dab was long ago are sealed, and pockets go on to their clients. */
    @SubscribeEvent
    public static void onServerTickDone(ServerTickEvent.Post event) {
        BuildMagic.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BuildScheduler.tickLevel(level);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BuildSession session = BuildSession.of(player.getUUID());
            session.loggedIn();
            for (Component line : session.takeLoginLines()) {
                player.displayClientMessage(line, false);
            }
            BuildMagic.sync(player.server, player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BuildMagic.leaveMode(player, false);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BuildMagic.leaveMode(player, false);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BuildMagic.leaveMode(player, false);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BuildView view = player.getExistingData(ModAttachments.BUILD_VIEW).orElse(null);
            if (view != null && view.box().isPresent()) {
                player.setData(ModAttachments.BUILD_VIEW, view.withSpell(view.spell(), Optional.empty()));
            }
        }
    }

    @SubscribeEvent
    public static void onChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        GameType next = event.getNewGameMode();
        if (event.getEntity() instanceof ServerPlayer player && (next == GameType.ADVENTURE || next == GameType.SPECTATOR)) {
            BuildMagic.leaveMode(player, true);
        }
    }

    /** At LOW: after every arena is put back and every island cleared (R20). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onServerStopping(ServerStoppingEvent event) {
        BuildScheduler.onServerStopping(BuildConfig.FINISH_ON_STOP_BLOCKS.get());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BuildScheduler.clear();
        BuildMagic.clear();
    }

    private BuildEvents() {
    }
}
