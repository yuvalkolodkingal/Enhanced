package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Sounds that last as long as a power is in use: the Chaos Beam's crackle, the Mind Stone beam's hum, Magneto's
 * magnetic hum while he holds something, the roar of Tommy's whirlwind, the crackle of Thor's lightning beam, and the
 * hum of the Mind Stone beam of the Vision of a Hex. Each follows its player (or Vision) and fades in and out with the
 * power.
 */
public final class PowerLoopSounds {

    private enum Kind {
        CHAOS_BEAM(ModSounds.CHAOS_BEAM, 0.9F, 1.0F, d -> d.power() == Power.SCARLET_WITCH && d.beamActive()),
        MIND_STONE_BEAM(ModSounds.MIND_STONE_BEAM, 0.8F, 1.0F, d -> d.power() == Power.VISION && d.beamActive()),
        MAGNET_HUM(ModSounds.MAGNET_HUM, 0.6F, 1.0F, d -> d.power() == Power.MAGNETO && d.heldEntityId() != -1),
        WHIRLWIND(ModSounds.WHIRLWIND, 0.9F, 1.0F, d -> d.power() == Power.TOMMY && d.is(Live.WHIRLWIND)),
        /** Thor's lightning beam: the same crackle as the Chaos Beam, higher and sharper. */
        LIGHTNING_BEAM(ModSounds.CHAOS_BEAM, 0.8F, 1.45F, d -> d.power() == Power.THOR && d.is(Live.CALLING_LIGHTNING)),
        /** The Vision of a Hex firing his Mind Stone beam: Vision's own hum, a little lower and softer. */
        HEX_VISION_BEAM(ModSounds.MIND_STONE_BEAM, 0.6F, 0.9F, null);

        final Supplier<SoundEvent> sound;
        final float volume;
        final float pitch;
        /** When a player is using the power (null for the kinds that belong to something else than a player). */
        @Nullable
        final Predicate<PowerData> active;

        Kind(Supplier<SoundEvent> sound, float volume, float pitch, @Nullable Predicate<PowerData> active) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.active = active;
        }
    }

    private record Key(int entityId, Kind kind) {
    }

    private static final Map<Key, Loop> LOOPS = new HashMap<>();

    static void tick(Minecraft mc) {
        // The sound engine ticks every playing sound before this runs; a loop it stopped ticking was dropped (sound
        // reload, too many sounds...) and is replaced below if the power is still in use.
        boolean paused = mc.isPaused();
        LOOPS.values().removeIf(loop -> loop.isStopped() || (!paused && ++loop.idleTicks > 2));
        if (mc.level == null) {
            return;
        }
        for (Player player : mc.level.players()) {
            if (!player.isAlive()) {
                continue;
            }
            PowerData data = Powers.get(player);
            for (Kind kind : Kind.values()) {
                Key key = new Key(player.getId(), kind);
                if (kind.active != null && kind.active.test(data) && !LOOPS.containsKey(key)) {
                    Loop loop = new Loop(player, kind, () -> kind.active.test(Powers.get(player)));
                    LOOPS.put(key, loop);
                    mc.getSoundManager().play(loop);
                }
            }
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            Key key = new Key(entity.getId(), Kind.HEX_VISION_BEAM);
            if (entity instanceof HexVisionEntity vision && vision.isBeaming() && !LOOPS.containsKey(key)) {
                Loop loop = new Loop(vision, Kind.HEX_VISION_BEAM, vision::isBeaming);
                LOOPS.put(key, loop);
                mc.getSoundManager().play(loop);
            }
        }
    }

    static void stopAll() {
        LOOPS.values().forEach(Loop::end);
        LOOPS.clear();
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final Entity source;
        private final Kind kind;
        private final BooleanSupplier active;
        int idleTicks;

        Loop(Entity source, Kind kind, BooleanSupplier active) {
            super(kind.sound.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.source = source;
            this.kind = kind;
            this.active = active;
            this.looping = true;
            this.delay = 0;
            this.pitch = kind.pitch;
            this.volume = 0.05F;
            follow();
        }

        private void follow() {
            this.x = source.getX();
            this.y = source.getEyeY();
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
            idleTicks = 0;
            if (source.isRemoved() || !source.isAlive()) {
                stop();
                return;
            }
            follow();
            if (active.getAsBoolean()) {
                volume = Math.min(kind.volume, volume + 0.2F);
            } else if (volume > 0.0F) {
                volume = Math.max(0.0F, volume - 0.15F);
            } else {
                stop();
            }
        }
    }

    private PowerLoopSounds() {
    }
}
