package com.kolodkingal.enhanced.client.falcon;

import com.kolodkingal.enhanced.client.render.FalconActing;
import com.kolodkingal.enhanced.falcon.RedwingEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Falcon's sounds that last (3.8, PLAN 8.6): the nozzles' thrust while he flies, glides, dives or climbs with the wings
 * ({@code falcon_thrust}), and Redwing's buzz while it is out ({@code redwing_buzz}); each a loop that follows its
 * entity and fades in and out with what it belongs to.
 */
public final class FalconLoops {
    /** How far away (blocks) a thrust or a buzz is still played. */
    static final double RANGE = 48.0;

    private record Key(int entityId, int kind) {
    }

    private static final Map<Key, Loop> LOOPS = new HashMap<>();

    /** Once per client tick. */
    static void tick(Minecraft mc) {
        LOOPS.values().removeIf(AbstractTickableSoundInstance::isStopped);
        if (mc.level == null || mc.player == null) {
            return;
        }
        for (Player player : mc.level.players()) {
            if (player.distanceToSqr(mc.player) < RANGE * RANGE && FalconWingsState.drawsWings(player)) {
                ensure(mc, player, 0, ModSounds.FALCON_THRUST.get(), 0.45F, () -> thrusting(player));
            }
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof RedwingEntity drone && drone.distanceToSqr(mc.player) < RANGE * RANGE) {
                ensure(mc, drone, 1, ModSounds.REDWING_BUZZ.get(), 0.35F, () -> drone.isAlive() && !drone.isRemoved());
            }
        }
    }

    /** Whether the player's nozzles roar now. */
    static boolean thrusting(Player player) {
        if (player.isRemoved() || !player.isAlive() || player.onGround() || !FalconWingsState.drawsWings(player)) {
            return false;
        }
        PowerData data = Powers.get(player);
        return data.flying() || player.isFallFlying() || data.is(Live.FALCON_DIVING)
                || FalconActing.climb(player, 0.0F) > 0.0F;
    }

    private static void ensure(Minecraft mc, Entity source, int kind, SoundEvent sound, float level,
                               BooleanSupplier active) {
        Key key = new Key(source.getId(), kind);
        if (active.getAsBoolean() && !LOOPS.containsKey(key)) {
            Loop loop = new Loop(source, sound, level, active);
            LOOPS.put(key, loop);
            mc.getSoundManager().play(loop);
        }
    }

    /** Stops every loop (the client left the world). */
    static void stopAll() {
        LOOPS.values().forEach(Loop::end);
        LOOPS.clear();
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final Entity source;
        private final BooleanSupplier active;
        private final float top;

        Loop(Entity source, SoundEvent sound, float level, BooleanSupplier active) {
            super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.source = source;
            this.active = active;
            this.top = level;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.02F;
            follow();
        }

        private void follow() {
            this.x = source.getX();
            this.y = source.getY() + source.getBbHeight() * 0.6;
            this.z = source.getZ();
        }

        void end() {
            stop();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (source.isRemoved()) {
                stop();
                return;
            }
            follow();
            if (active.getAsBoolean()) {
                volume = Math.min(top, volume + top * 0.2F);
            } else {
                volume -= top * 0.15F;
                if (volume <= 0.01F) {
                    stop();
                }
            }
        }
    }

    private FalconLoops() {
    }
}
