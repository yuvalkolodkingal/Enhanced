package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The small flourishes that go with the gestures of the playing Thanos (DESIGN section 15.12): a violet flare and
 * spiral at the Gauntlet as the Blast leaves it, the dust of the Slam, the blue ring the Step's hand draws, the spiral
 * of the Pull, red sparks at the fingers of the Transmute, red motes round the hand of the Veil, orange motes at the
 * eyes of the Sight, green rings on the forearm of the Rewind and a quick mandala under the feet of the Time Lock. Each
 * is started with its gesture ({@link ThanosClient#gestureStarted}, which the animation calls only when he is in view
 * and the client's flight effects are on), comes from the places the model really has ({@link HandTracker}), and runs
 * from a few ticks into the gesture to a few ticks later, timed with the keys.
 */
final class ThanosGestureFx {
    /** What a timed effect does each tick it lasts. */
    private enum Kind {
        BLAST_FLARE, SLAM_DUST, STEP_TRAIL, STEP_RING, PULL_SPIRAL, WARP_FLICK, VEIL_SHIMMER, SIGHT_EYES, REWIND_RING, LOCK_MANDALA
    }

    /** An effect that runs from tick {@code from} to {@code to} of the game's clock, for the player of this entity id. */
    private record Timed(int player, long from, long to, Kind kind) {
    }

    private static final int MAX = 24;
    private static final List<Timed> TIMED = new ArrayList<>();

    /** A gesture of Thanos just began: starts what goes with it. */
    static void started(AbstractClientPlayer player, GestureId id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        switch (id) {
            case THANOS_BLAST -> add(player, now + 1, now + 5, Kind.BLAST_FLARE);
            case THANOS_SLAM -> add(player, now + 12, now + 12, Kind.SLAM_DUST);
            case THANOS_STEP -> {
                add(player, now + 2, now + 10, Kind.STEP_TRAIL);
                add(player, now + 10, now + 10, Kind.STEP_RING);
            }
            case THANOS_PULL -> add(player, now + 2, now + 10, Kind.PULL_SPIRAL);
            case THANOS_WARP -> add(player, now + 2, now + 2, Kind.WARP_FLICK);
            case THANOS_VEIL_ON -> add(player, now + 5, now + 14, Kind.VEIL_SHIMMER);
            case THANOS_SIGHT -> add(player, now + 4, now + 9, Kind.SIGHT_EYES);
            case THANOS_REWIND -> {
                add(player, now + 4, now + 4, Kind.REWIND_RING);
                add(player, now + 9, now + 9, Kind.REWIND_RING);
            }
            case THANOS_TIMELOCK -> add(player, now + 6, now + 6, Kind.LOCK_MANDALA);
            default -> {
            }
        }
    }

    private static void add(AbstractClientPlayer player, long from, long to, Kind kind) {
        if (TIMED.size() >= MAX) {
            TIMED.remove(0);
        }
        TIMED.add(new Timed(player.getId(), from, to, kind));
    }

    /** Runs the timed effects of this client tick. */
    static void tick(ClientLevel level) {
        if (TIMED.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        TIMED.removeIf(t -> now > t.to());
        for (Timed t : TIMED) {
            if (now < t.from() || !(level.getEntity(t.player()) instanceof AbstractClientPlayer player) || player.isInvisible()) {
                continue;
            }
            emit(level, player, t.kind(), now - t.from());
        }
    }

    static void clear() {
        TIMED.clear();
    }

    // ---- the effects ----

    private static void emit(ClientLevel level, AbstractClientPlayer player, Kind kind, long tick) {
        Minecraft mc = Minecraft.getInstance();
        RandomSource random = level.random;
        float size = size(player);
        Vec3 look = player.getLookAngle();
        switch (kind) {
            case BLAST_FLARE -> {
                // A violet flare at the Gauntlet, and a spiral thrown out along the aim.
                Vec3 hand = hand(mc, player);
                int color = StoneKind.POWER.color();
                int core = StoneKind.POWER.core();
                level.addParticle(new EnergyParticleOptions(core, 1.8F * size), hand.x, hand.y, hand.z, 0.0, 0.0, 0.0);
                Vec3 right = sideways(look);
                Vec3 up = right.cross(look).normalize();
                for (int i = 0; i < 3; i++) {
                    double a = (player.tickCount + i / 3.0) * 0.9 + i * Mth.TWO_PI / 3.0;
                    Vec3 spin = right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
                    Vec3 at = hand.add(spin.scale(0.16));
                    Vec3 v = look.scale(0.16).add(spin.scale(0.03));
                    level.addParticle(new EnergyParticleOptions(i == 0 ? core : color, 0.9F * size), at.x, at.y, at.z, v.x, v.y, v.z);
                }
            }
            case SLAM_DUST -> {
                // The Gauntlet strikes the ground: dust and chips fly out from his feet, and a ring runs.
                Vec3 feet = player.position();
                BlockState below = level.getBlockState(BlockPos.containing(feet.x, feet.y - 0.2, feet.z));
                if (!below.isAir()) {
                    for (int i = 0; i < 26; i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        double r = 0.3 + random.nextDouble() * 0.9;
                        double speed = 0.12 + random.nextDouble() * 0.16;
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below), feet.x + Math.cos(angle) * r, feet.y + 0.1,
                                feet.z + Math.sin(angle) * r, Math.cos(angle) * speed, 0.12 + random.nextDouble() * 0.14, Math.sin(angle) * speed);
                    }
                }
                FlightEffects.ring(level, player.getId(), feet.add(0.0, 0.05, 0.0), new Vec3(0.0, 1.0, 0.0), 10, 0.4F, 3.2F, 0.12F, 0xB8B8B8, 0.7F);
                FlightEffects.ring(level, player.getId(), feet.add(0.0, 0.06, 0.0), new Vec3(0.0, 1.0, 0.0), 8, 0.3F, 2.0F, 0.08F, StoneKind.POWER.color(), 0.8F);
            }
            case STEP_TRAIL -> {
                // Motes left hanging where the hand has been: the circle it draws.
                Vec3 hand = hand(mc, player);
                for (int i = 0; i < 2; i++) {
                    level.addParticle(new EnergyParticleOptions(i == 0 ? StoneKind.SPACE.core() : StoneKind.SPACE.color(), 1.0F * size), hand.x, hand.y, hand.z,
                            (random.nextDouble() - 0.5) * 0.01, (random.nextDouble() - 0.5) * 0.01, (random.nextDouble() - 0.5) * 0.01);
                }
            }
            case STEP_RING -> {
                Vec3 hand = hand(mc, player);
                FlightEffects.ring(level, player.getId(), hand.add(look.scale(0.3)), look, 9, 0.2F, 1.2F, 0.06F, StoneKind.SPACE.color(), 0.9F);
            }
            case PULL_SPIRAL -> {
                // Motes drawn in to the palm in a spiral, as the arm is dragged back.
                Vec3 hand = hand(mc, player);
                Vec3 right = sideways(look);
                Vec3 up = right.cross(look).normalize();
                Vec3 palm = hand.add(look.scale(0.25));
                for (int i = 0; i < 3; i++) {
                    double a = (player.tickCount + i / 3.0) * 0.8 + i * Mth.TWO_PI / 3.0;
                    Vec3 radial = right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
                    Vec3 at = palm.add(look.scale(0.5)).add(radial.scale(0.45));
                    Vec3 v = at.subtract(palm).scale(-0.18);
                    level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? StoneKind.SPACE.core() : StoneKind.SPACE.color(), 0.9F * size), at.x, at.y, at.z,
                            v.x, v.y, v.z);
                }
            }
            case WARP_FLICK -> {
                // Red sparks thrown from the fingertips along the aim.
                Vec3 hand = hand(mc, player);
                for (int i = 0; i < 9; i++) {
                    Vec3 v = look.scale(0.12 + random.nextDouble() * 0.1).add((random.nextDouble() - 0.5) * 0.08, (random.nextDouble() - 0.5) * 0.08,
                            (random.nextDouble() - 0.5) * 0.08);
                    level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? StoneKind.REALITY.core() : StoneKind.REALITY.color(), 0.8F * size), hand.x, hand.y,
                            hand.z, v.x, v.y, v.z);
                }
            }
            case VEIL_SHIMMER -> {
                // Red motes round the hand, drifting up like heat.
                Vec3 hand = hand(mc, player);
                for (int i = 0; i < 3; i++) {
                    level.addParticle(new EnergyParticleOptions(i == 0 ? StoneKind.REALITY.core() : StoneKind.REALITY.color(), 0.8F * size),
                            hand.x + (random.nextDouble() - 0.5) * 0.5, hand.y + (random.nextDouble() - 0.5) * 0.4, hand.z + (random.nextDouble() - 0.5) * 0.5,
                            0.0, 0.015 + random.nextDouble() * 0.02, 0.0);
                }
            }
            case SIGHT_EYES -> {
                // Orange motes at the eyes, rising.
                Vec3 eyes = WorldEffectsRenderer.foreheadPosition(mc, player, 0.0F);
                for (int i = 0; i < 2; i++) {
                    level.addParticle(new EnergyParticleOptions(i == 0 ? StoneKind.SOUL.core() : StoneKind.SOUL.color(), 0.6F * size),
                            eyes.x + (random.nextDouble() - 0.5) * 0.3, eyes.y - 0.05 + (random.nextDouble() - 0.5) * 0.1, eyes.z + (random.nextDouble() - 0.5) * 0.3,
                            0.0, 0.02 + random.nextDouble() * 0.02, 0.0);
                }
            }
            case REWIND_RING -> {
                // A green ring round the forearm, opening out along it.
                Vec3 hand = hand(mc, player);
                Vec3 shoulder = HandTracker.shoulder(player, player.getMainArm().getOpposite(), 0.0F);
                Vec3 axis = shoulder == null ? look : hand.subtract(shoulder);
                axis = axis.lengthSqr() < 1.0E-4 ? look : axis.normalize();
                Vec3 at = shoulder == null ? hand : hand.subtract(axis.scale(0.25));
                FlightEffects.ring(level, player.getId(), at, axis, 10, 0.12F, 0.65F, 0.05F, StoneKind.TIME.color(), 0.9F);
            }
            case LOCK_MANDALA -> {
                // A quick green mandala on the ground under his feet.
                Vec3 feet = player.position();
                Vec3 flat = new Vec3(0.0, 1.0, 0.0);
                FlightEffects.ring(level, player.getId(), feet.add(0.0, 0.05, 0.0), flat, 12, 0.3F, 2.6F, 0.10F, StoneKind.TIME.color(), 0.9F);
                FlightEffects.ring(level, player.getId(), feet.add(0.0, 0.06, 0.0), flat, 9, 0.2F, 1.6F, 0.06F, StoneKind.TIME.core(), 0.8F);
                for (int i = 0; i < 14; i++) {
                    double angle = i * Mth.TWO_PI / 14.0;
                    level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? StoneKind.TIME.color() : StoneKind.TIME.core(), 0.8F * size),
                            feet.x + Math.cos(angle) * 1.4, feet.y + 0.1, feet.z + Math.sin(angle) * 1.4, 0.0, 0.05 + random.nextDouble() * 0.03, 0.0);
                }
            }
        }
    }

    /** Where the Gauntlet's hand is: as the body is drawn, else where the first-person hand is. */
    private static Vec3 hand(Minecraft mc, AbstractClientPlayer player) {
        Vec3 fallback = player.position().add(0.0, player.getBbHeight() * 0.65, 0.0).add(player.getLookAngle().scale(0.5));
        return ThanosFx.gauntletHand(mc, player, 0.0F, fallback);
    }

    /** A unit vector across the look, horizontal. */
    private static Vec3 sideways(Vec3 look) {
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        return right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
    }

    /** The local player's own hands are a hand's length from the camera in first person: small motes there. */
    private static float size(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson() ? 0.4F : 1.0F;
    }

    private ThanosGestureFx() {
    }
}
