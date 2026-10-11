package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Sounds that last as long as a power is in use: the Chaos Beam's crackle, the Mind Stone beam's hum, Magneto's
 * magnetic hum while he holds something, the roar of Tommy's whirlwind and the crackle of Thor's lightning beam; and
 * the soft hums of the Space Stone and the Mind Stone for as long as someone holds the Tesseract or Loki's Scepter.
 * Each follows its player and fades in and out with the power (or the item in their hand).
 */
public final class PowerLoopSounds {

    private enum Kind {
        CHAOS_BEAM(ModSounds.CHAOS_BEAM, 0.9F, 1.0F, (p, d) -> d.power() == Power.SCARLET_WITCH && d.beamActive()),
        MIND_STONE_BEAM(ModSounds.MIND_STONE_BEAM, 0.8F, 1.0F, (p, d) -> d.power() == Power.VISION && d.beamActive()),
        MAGNET_HUM(ModSounds.MAGNET_HUM, 0.6F, 1.0F, (p, d) -> d.power() == Power.MAGNETO && d.heldEntityId() != -1),
        WHIRLWIND(ModSounds.WHIRLWIND, 0.9F, 1.0F, (p, d) -> d.power() == Power.TOMMY && d.is(Live.WHIRLWIND)),
        /** Thor's lightning beam: the same crackle as the Chaos Beam, higher and sharper. */
        LIGHTNING_BEAM(ModSounds.CHAOS_BEAM, 0.8F, 1.45F, (p, d) -> d.power() == Power.THOR && d.is(Live.CALLING_LIGHTNING)),
        /** The Space Stone in the Tesseract: far quieter than the powers' loops, only just there. */
        SPACE_STONE_HUM(ModSounds.SPACE_STONE_HUM, STONE_HUM_VOLUME, 1.0F, (p, d) -> holds(p, ModItems.TESSERACT.get())),
        /** The Mind Stone in Loki's Scepter. */
        MIND_STONE_HUM(ModSounds.MIND_STONE_HUM, STONE_HUM_VOLUME, 1.0F, (p, d) -> holds(p, ModItems.LOKIS_SCEPTER.get()));

        final Supplier<SoundEvent> sound;
        final float volume;
        final float pitch;
        final BiPredicate<Player, PowerData> active;

        Kind(Supplier<SoundEvent> sound, float volume, float pitch, BiPredicate<Player, PowerData> active) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.active = active;
        }
    }

    /** How loud a stone's hum is: well under the beams (which are mastered louder to begin with). */
    private static final float STONE_HUM_VOLUME = 0.5F;

    /** Whether the player has {@code item} in either hand (not a spectator, who is only watching). */
    private static boolean holds(Player player, Item item) {
        return !player.isSpectator() && player.isHolding(item);
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
                if (kind.active.test(player, data) && !LOOPS.containsKey(key)) {
                    Loop loop = new Loop(player, kind);
                    LOOPS.put(key, loop);
                    mc.getSoundManager().play(loop);
                }
            }
        }
    }

    static void stopAll() {
        LOOPS.values().forEach(Loop::end);
        LOOPS.clear();
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final Player player;
        private final Kind kind;
        int idleTicks;

        Loop(Player player, Kind kind) {
            super(kind.sound.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.kind = kind;
            this.looping = true;
            this.delay = 0;
            this.pitch = kind.pitch;
            this.volume = 0.05F;
            follow();
        }

        private void follow() {
            this.x = player.getX();
            this.y = player.getEyeY();
            this.z = player.getZ();
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
            if (player.isRemoved() || !player.isAlive()) {
                stop();
                return;
            }
            follow();
            if (kind.active.test(player, Powers.get(player))) {
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
