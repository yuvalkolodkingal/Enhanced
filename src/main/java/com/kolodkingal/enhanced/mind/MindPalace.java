package com.kolodkingal.enhanced.mind;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Vision's Mind Palace (3.12, Sneak + his Disguise key): five seconds of standing still, and he is in his own mind
 * ({@link MindRealm}); from inside, the same takes him back. Moving, or being hit, breaks it off, so a key pressed by
 * mistake does not fling him away from where he was.
 */
public final class MindPalace {
    public static final int CHANNEL_TICKS = 100;
    private static final double STILL = 0.25;

    private record Channel(Vec3 from, long until) {
    }

    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();

    private MindPalace() {
    }

    public static boolean channeling(ServerPlayer player) {
        return CHANNELS.containsKey(player.getUUID());
    }

    /** The key: begins (or, pressed again, cancels) the five seconds. */
    public static void press(ServerPlayer player) {
        if (CHANNELS.remove(player.getUUID()) != null) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind.cancel"), true);
            return;
        }
        CHANNELS.put(player.getUUID(), new Channel(player.position(), player.serverLevel().getGameTime() + CHANNEL_TICKS));
        player.displayClientMessage(Component.translatable(MindRealm.in(player) ? "message.enhanced.mind.leaving"
                : "message.enhanced.mind.entering"), true);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT,
                SoundSource.PLAYERS, 1.0F, 1.6F);
    }

    /** Every tick: still standing? The swirl, and at the end the way in (or out). */
    public static void tick(ServerPlayer player) {
        Channel channel = CHANNELS.get(player.getUUID());
        if (channel == null) {
            return;
        }
        if (!player.isAlive() || player.position().distanceToSqr(channel.from()) > STILL * STILL || player.hurtTime > 0) {
            CHANNELS.remove(player.getUUID());
            player.displayClientMessage(Component.translatable("message.enhanced.mind.moved"), true);
            return;
        }
        long left = channel.until() - player.serverLevel().getGameTime();
        if (left % 5 == 0) {
            double a = left * 0.4;
            for (int i = 0; i < 3; i++) {
                double r = 0.4 + 0.2 * i;
                player.serverLevel().sendParticles(new EnergyParticleOptions(Powers.glow(player), 1.2F),
                        player.getX() + Math.cos(a + i * 2.1) * r, player.getY() + 1.6 + 0.1 * i, player.getZ() + Math.sin(a + i * 2.1) * r,
                        1, 0, 0, 0, 0);
            }
        }
        if (left % 20 == 0 && left > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind.count", left / 20), true);
        }
        if (left <= 0) {
            CHANNELS.remove(player.getUUID());
            if (MindRealm.in(player)) {
                MindRealm.leave(player);
            } else {
                MindRealm.enter(player);
            }
        }
    }

    public static void forget(ServerPlayer player) {
        CHANNELS.remove(player.getUUID());
    }

    public static void clear() {
        CHANNELS.clear();
    }
}
