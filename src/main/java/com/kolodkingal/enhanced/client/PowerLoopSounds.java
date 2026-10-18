package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.item.DarkholdItem;
import com.kolodkingal.enhanced.item.RubyQuartzGlassesItem;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Sounds that last as long as a power is in use: the Chaos Beam's crackle, the Mind Stone beam's hum, Magneto's
 * magnetic hum while he holds something, the roar of Tommy's whirlwind, the crackle of Thor's lightning beam, the
 * hum of the Mind Stone beam of the Vision of a Hex; and the soft hums of the Space Stone and the Mind Stone for as long
 * as someone holds the Tesseract or Loki's Scepter. Each follows its player (or Vision) and fades in and out with the
 * power (or the item in their hand).
 */
public final class PowerLoopSounds {

    private enum Kind {
        CHAOS_BEAM(ModSounds.CHAOS_BEAM, 0.9F, 1.0F, (p, d) -> d.has(HeroAbility.WANDA_CHAOS_BEAM) && d.beamActive()),
        MIND_STONE_BEAM(ModSounds.MIND_STONE_BEAM, 0.8F, 1.0F, (p, d) -> d.has(HeroAbility.VISION_MIND_BEAM) && d.beamActive()),
        MAGNET_HUM(ModSounds.MAGNET_HUM, 0.6F, 1.0F, (p, d) -> d.has(HeroAbility.MAGNETO_METAL) && d.heldEntityId() != -1),
        WHIRLWIND(ModSounds.WHIRLWIND, 0.9F, 1.0F, (p, d) -> d.has(HeroAbility.TOMMY_WHIRLWIND) && d.is(Live.WHIRLWIND)),
        /** Thor's lightning beam: the same crackle as the Chaos Beam, higher and sharper. */
        LIGHTNING_BEAM(ModSounds.CHAOS_BEAM, 0.8F, 1.45F, (p, d) -> d.has(HeroAbility.THOR_LIGHTNING) && d.is(Live.CALLING_LIGHTNING)),
        /** Billy's Hex Beam: the Chaos Beam's crackle, higher and thinner (3.2). Another source of a beam adds its own line. */
        HEX_BEAM(ModSounds.CHAOS_BEAM, 0.7F, 1.3F, (p, d) -> d.is(Live.BEAMING) && BeamRenderer.styleOf(p.getId()) == BeamStyles.BILLY),
        /** The Darkhold's purple beam, whoever fires it: the Chaos Beam's crackle, lower and darker (3.2). */
        DARKHOLD_BEAM(ModSounds.CHAOS_BEAM, 0.8F, 0.75F, (p, d) -> d.is(Live.BEAMING) && BeamRenderer.styleOf(p.getId()) == DarkholdItem.STYLE),
        /** The Space Stone in the Tesseract: far quieter than the powers' loops, only just there. */
        SPACE_STONE_HUM(ModSounds.SPACE_STONE_HUM, STONE_HUM_VOLUME, 1.0F, (p, d) -> holds(p, ModItems.TESSERACT.get())),
        /** The Mind Stone in Loki's Scepter. */
        MIND_STONE_HUM(ModSounds.MIND_STONE_HUM, STONE_HUM_VOLUME, 1.0F, (p, d) -> holds(p, ModItems.LOKIS_SCEPTER.get())),
        /** The Vision of a Hex firing his Mind Stone beam: Vision's own hum, a little lower and softer. */
        HEX_VISION_BEAM(ModSounds.MIND_STONE_BEAM, 0.6F, 0.9F, null),

        // The loops of 3.0, in one region per hero (the // ===== lines), each lane's own.
        // ===== phoenix =====
        /** The flames round the Phoenix while she charges the Burst (and for a moment after it). */
        PHOENIX_BLAZE(ModSounds.PHOENIX_BLAZE, 0.9F, 1.0F, (p, d) -> d.acts(Power.PHOENIX) && d.is(Live.PHOENIX_BLAZE)),

        // ===== spider =====
        /** The creak of the taut web while Spider-Man swings. */
        WEB_SWING(ModSounds.WEB_SWING, 0.35F, 1.0F, (p, d) -> d.has(HeroAbility.SPIDER_ZIP) && d.is(Live.SWINGING)),

        // ===== thanos =====
        /** The Soul Drain's chains: a choir shimmer with the click of chain links. */
        SOUL_DRAIN(ModSounds.SOUL_DRAIN, 0.8F, 1.0F, (p, d) -> d.has(HeroAbility.THANOS_SOUL) && d.is(Live.SOUL_DRAINING)),

        // ===== ultron =====
        /** Ultron's anti-gravity flight: the thrusters' hiss of the Sentries, low and soft (it has no jets and no flame). */
        ULTRON_FLIGHT(ModSounds.SENTRY_HOVER, 0.5F, 0.7F, (p, d) -> d.flying() && (d.power() == Power.ULTRON || fliesAs(p, d, Power.ULTRON))),

        // ===== apocalypse =====
        /** The Sandstorm: the roar of sand round him. */
        SANDSTORM(ModSounds.SAND_STORM_LOOP, 0.9F, 1.0F, (p, d) -> d.has(HeroAbility.APOCALYPSE_BESTOW) && d.is(Live.SANDSTORM)),

        // ===== cyclops (3.3) =====
        /** Cyclops's optic blast: a bright, focused hum with a pulse in it. */
        CYCLOPS_OPTIC(ModSounds.CYCLOPS_OPTIC_LOOP, 0.8F, 1.0F,
                (p, d) -> d.is(Live.BEAMING) && BeamRenderer.styleOf(p.getId()) == RubyQuartzGlassesItem.OPTIC),
        /** The glasses off: the deep roar of the giant beam, for as long as it pours out. */
        CYCLOPS_UNLEASHED(ModSounds.CYCLOPS_UNLEASHED_LOOP, 1.0F, 1.0F,
                (p, d) -> d.has(HeroAbility.CYCLOPS_GLASSES) && d.is(Live.CYCLOPS_UNLEASHED))
        ;

        final Supplier<SoundEvent> sound;
        final float volume;
        final float pitch;
        /** When a player is using the power (null for the kinds that belong to something else than a player). */
        @Nullable
        final BiPredicate<Player, PowerData> active;

        Kind(Supplier<SoundEvent> sound, float volume, float pitch, @Nullable BiPredicate<Player, PowerData> active) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.active = active;
        }
    }

    /** How loud a stone's hum is: well under the beams (which are mastered louder to begin with). */
    private static final float STONE_HUM_VOLUME = 0.5F;

    /** Whether the player has {@code item} in either hand (not a spectator, who is only watching). */
    /** Whether a custom hero flies in the style of this hero (3.4: the flight's loop goes with its style). */
    private static boolean fliesAs(Player player, PowerData data, Power hero) {
        HeroAnimState st = data.power() == Power.CUSTOM ? HeroAnimator.peek(player) : null;
        return st != null && st.flightAs == hero;
    }

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
                if (kind.active != null && kind.active.test(player, data) && !LOOPS.containsKey(key)) {
                    Loop loop = new Loop(player, kind, () -> kind.active.test(player, Powers.get(player)));
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
