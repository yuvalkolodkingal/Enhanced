package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The pose each Ultron body is drawn in this frame: what its synced state asks for ({@link UltronPoses}), eased from the
 * pose it was drawn in a moment ago, so that a tell rises into view and a recovery settles instead of snapping. The model,
 * the renderer and the layers all ask here; the first to ask in a frame does the work and the rest are given the same
 * pose.
 */
public final class UltronAnimation {
    /** The time constant of the joints, in ticks. */
    private static final float TAU = 2.2F;
    /** A gap longer than this (a paused game, a chunk that was not loaded) snaps to the pose instead of easing. */
    private static final float SNAP_AFTER = 12.0F;
    /** A hacked drone's eyes flicker for this many ticks when it joins. */
    private static final int HACK_FLICKER = 12;

    private static final class Smoothed {
        final UltronPose pose = new UltronPose();
        float age = Float.NaN;
    }

    private static final Map<Entity, Smoothed> SMOOTHED = new WeakHashMap<>();
    private static final UltronPose TARGET = new UltronPose();
    /** Sentries whose eyes flicker blue, white and red until a game time (the Hack is reaching for them). */
    private static final Map<Integer, Long> FLICKERS = new HashMap<>();

    /** The form of the model an entity is drawn in ({@link UltronCubes#PRIME} and the rest). */
    public static int form(LivingEntity entity) {
        return entity instanceof UltronEntity boss ? Math.min(boss.bodyIndex(), UltronCubes.ULTRON) : UltronCubes.SENTRY;
    }

    /** The pose of an Ultron body or Sentry in the frame being drawn. */
    public static UltronPose pose(LivingEntity entity, float partialTick) {
        float age = entity.tickCount + partialTick;
        Smoothed smoothed = SMOOTHED.computeIfAbsent(entity, e -> new Smoothed());
        if (smoothed.age == age) {
            return smoothed.pose;
        }
        target(entity, partialTick, age);
        float dt = age - smoothed.age;
        if (Float.isNaN(dt) || dt < 0.0F || dt > SNAP_AFTER) {
            smoothed.pose.set(TARGET);
        } else {
            smoothed.pose.approach(TARGET, dt, TAU);
        }
        smoothed.age = age;
        return smoothed.pose;
    }

    private static void target(LivingEntity entity, float partialTick, float age) {
        float forward = forwardSpeed(entity);
        if (entity instanceof UltronEntity boss) {
            UltronPoses.boss(TARGET, boss.state(), boss.stateAge(partialTick), boss.bodyIndex(), age);
            // A little lean into the way he drifts.
            TARGET.v[UltronPose.HIPS_X] += Mth.clamp(forward * 30.0F, -6.0F, 10.0F);
        } else if (entity instanceof UltronSentryEntity sentry) {
            long now = sentry.level().getGameTime();
            float t = (now - sentry.actTick()) + partialTick;
            float aim = Mth.lerp(partialTick, sentry.xRotO, sentry.getXRot());
            UltronPoses.sentry(TARGET, sentry.act(), eyesOf(sentry, now, age), t, age, aim);
            // The whole Sentry pitches toward where it flies, up to twenty degrees.
            TARGET.v[UltronPose.PITCH] += Mth.clamp(forward * 70.0F, -12.0F, 20.0F);
        } else {
            TARGET.clear();
        }
    }

    /** How fast the entity moves along the way it faces, in blocks a tick (negative: backwards). */
    private static float forwardSpeed(LivingEntity entity) {
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        float yaw = entity.yBodyRot * Mth.DEG_TO_RAD;
        return (float) (-dx * Mth.sin(yaw) + dz * Mth.cos(yaw));
    }

    /** The colour of a Sentry's eyes: what it shows, or a flicker while it is being hacked or has just been taken over. */
    private static UltronSentryEntity.Eyes eyesOf(UltronSentryEntity sentry, long now, float age) {
        Long until = FLICKERS.get(sentry.getId());
        boolean flicker = until != null && now <= until || sentry.isHacked() && sentry.tickCount < HACK_FLICKER;
        if (flicker) {
            return UltronSentryEntity.Eyes.values()[((int) (age * 0.7F) % 3 + 3) % 3];
        }
        return sentry.eyes();
    }

    /** The Hack is reaching for this Sentry: its eyes flicker until the game time {@code until}. */
    public static void flicker(int entityId, long until) {
        FLICKERS.put(entityId, until);
    }

    /** Forgets what the world held (a new world, a new dimension). */
    public static void clear() {
        SMOOTHED.clear();
        FLICKERS.clear();
    }

    private UltronAnimation() {
    }
}
