package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.client.sokovia.SokoviaClient;
import com.kolodkingal.enhanced.ultron.UltronAttack;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.UltronSounds;
import com.kolodkingal.enhanced.ultron.UltronState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The sounds of the Ultron fight that last: the hum of the Finger Beams that follows him from the first tick of the tell
 * to the last of the beams, the thrusters of the nearest Sentries (quiet, positional), the Dive's hiss that swells as he
 * rises, and the rumble of Sokovia, soft while the lift warns and loud while it moves. Each is a loop that follows its
 * entity and fades in and out with what it belongs to ({@link UltronSounds} names the sounds).
 */
public final class UltronLoops {
    /** How many Sentries have a hover loop at a time: the nearest ones. */
    private static final int HOVERS = 4;
    private static final double HOVER_RANGE = 24.0;

    private record Key(int entityId, int kind) {
    }

    private static final Map<Key, Loop> LOOPS = new HashMap<>();

    /** Once per client tick. */
    public static void tick(Minecraft mc) {
        LOOPS.values().removeIf(loop -> loop.isStopped());
        if (mc.level == null || mc.player == null) {
            return;
        }
        int hovering = 0;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof UltronEntity boss && boss.isAlive() && boss.distanceToSqr(mc.player) < 128.0 * 128.0) {
                UltronState state = boss.state();
                float t = boss.stateAge(0.0F);
                ensure(mc, boss, 0, UltronSounds.BEAM_LOOP, 1.0F, () -> state == UltronState.BEAMS && t < UltronAttack.BEAMS.length(0) - 4);
                ensure(mc, boss, 1, UltronSounds.HOVER_LOOP, 1.4F, () -> state == UltronState.DIVE && t < UltronAttack.DIVE.tell() + 8);
            } else if (entity instanceof UltronSentryEntity sentry && sentry.isAlive() && hovering < HOVERS && sentry.distanceToSqr(mc.player) < HOVER_RANGE * HOVER_RANGE
                    && sentry.act() != UltronSentryEntity.Act.ASSEMBLING) {
                hovering++;
                ensure(mc, sentry, 2, UltronSounds.HOVER_LOOP, 0.35F, () -> sentry.act() != UltronSentryEntity.Act.STUNNED);
            }
        }
        // Sokovia: soft under the warning, loud while it moves, positional at the middle of the island.
        SokoviaClient.View view = SokoviaClient.view(0.0F);
        if (view != null && view.moving()) {
            Vec3 centre = Vec3.atCenterOf(view.centre());
            ensureAt(mc, centre, 3, UltronSounds.RUMBLE_LOOP, 1.0F, () -> SokoviaClient.view(0.0F) != null && SokoviaClient.view(0.0F).moving());
        }
    }

    private static void ensure(Minecraft mc, Entity source, int kind, UltronSounds sound, float level, BooleanSupplier active) {
        Key key = new Key(source.getId(), kind);
        if (active.getAsBoolean() && !LOOPS.containsKey(key)) {
            Loop loop = new Loop(() -> source.isAlive() && !source.isRemoved() ? source.position().add(0, source.getBbHeight() * 0.6, 0) : null, sound, level, active);
            LOOPS.put(key, loop);
            mc.getSoundManager().play(loop);
        }
    }

    private static void ensureAt(Minecraft mc, Vec3 at, int kind, UltronSounds sound, float level, BooleanSupplier active) {
        Key key = new Key(-1 - kind, kind);
        if (active.getAsBoolean() && !LOOPS.containsKey(key)) {
            Loop loop = new Loop(() -> at, sound, level, active);
            LOOPS.put(key, loop);
            mc.getSoundManager().play(loop);
        }
    }

    /** Stops every loop (the client left the world). */
    public static void stopAll() {
        LOOPS.values().forEach(Loop::end);
        LOOPS.clear();
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final Supplier<Vec3> where;
        private final BooleanSupplier active;
        private final float top;

        Loop(Supplier<Vec3> where, UltronSounds sound, float level, BooleanSupplier active) {
            super(sound.event(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.where = where;
            this.active = active;
            this.top = sound.volume() * level;
            this.looping = true;
            this.delay = 0;
            this.pitch = sound.pitch();
            this.volume = 0.02F;
            follow();
        }

        private void follow() {
            Vec3 at = where.get();
            if (at != null) {
                this.x = at.x;
                this.y = at.y;
                this.z = at.z;
            }
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
            if (where.get() == null) {
                stop();
                return;
            }
            follow();
            if (active.getAsBoolean()) {
                volume = Math.min(top, volume + 0.15F);
            } else if (volume > 0.0F) {
                volume = Math.max(0.0F, volume - 0.12F);
            } else {
                stop();
            }
        }
    }

    private UltronLoops() {
    }
}
