package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The small flourishes that go with Spider-Man's gestures (DESIGN section 14, "Animation"): white motes at the hand
 * that shoots a web, a ring of them round the chest as the burst is flung wide, black and red wisps round his head as
 * the Black Eyes take him, gold sparks at the roots of the Iron Spider's legs as they unfold, a red flicker at the
 * lenses of Instant Kill and two pale rings round the head when the sense tingles. Each is started with its gesture,
 * comes from the places the model really has ({@link HandTracker}), and is thinned by distance and by the client's
 * flight effects option like {@link GestureEffects}'. The landing's ring is here too.
 */
final class SpiderGestureFx {
    /** What a timed effect does each tick it lasts. */
    private enum Kind {
        /** White motes at the hand that shoots the web. */
        WEB_HAND,
        /** The expanding ring of the burst, once, at the chest. */
        BURST_RING,
        /** Black and red wisps round the head. */
        SURGE_WISPS,
        /** Gold sparks at the four roots of the legs. */
        LEGS_SPARKS,
        /** A red flicker at the lenses. */
        KILL_FLICKER,
        /** A pale-blue ring round the head, once. */
        SENSE_RING
    }

    /** An effect that runs from tick {@code from} to {@code to} of the game's clock. */
    private record Timed(int player, long from, long to, Kind kind) {
    }

    private static final int WHITE = 0xFFFFFF;
    private static final int GOLD = 0xFFC247;
    private static final int RED = 0xFF2A2A;
    private static final int WISP = 0x1A0608;
    private static final int GLINT = 0x8A1020;
    private static final int PALE_BLUE = 0xBFE3FF;
    /** Beyond this far the flourishes are not made at all. */
    private static final double RANGE = 64.0;

    private static final List<Timed> TIMED = new ArrayList<>();

    /** A gesture of his just began: start what goes with it. */
    static void started(AbstractClientPlayer player, GestureId id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible() || !EnhancedClientConfig.flightEffects()
                || player.position().distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) > RANGE * RANGE) {
            return;
        }
        long now = level.getGameTime();
        switch (id) {
            case SPIDER_SHOOT, SPIDER_ZIP_START, SPIDER_SWING_START -> add(player, now + 1, now + 4, Kind.WEB_HAND);
            // The arms are flung wide at the twelfth tick of the burst.
            case SPIDER_BURST -> add(player, now + 12, now + 12, Kind.BURST_RING);
            case SPIDER_SURGE_ON -> add(player, now + 4, now + 16, Kind.SURGE_WISPS);
            case SPIDER_LEGS_OUT -> add(player, now + 4, now + 16, Kind.LEGS_SPARKS);
            case SPIDER_KILL_ON -> add(player, now + 4, now + 14, Kind.KILL_FLICKER);
            case SPIDER_ALERT_START -> {
                add(player, now + 2, now + 2, Kind.SENSE_RING);
                add(player, now + 6, now + 6, Kind.SENSE_RING);
            }
            default -> {
            }
        }
    }

    private static void add(AbstractClientPlayer player, long from, long to, Kind kind) {
        TIMED.add(new Timed(player.getId(), from, to, kind));
    }

    /** Runs the timed effects of this client tick. */
    static void tick(ClientLevel level) {
        if (TIMED.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        TIMED.removeIf(t -> now > t.to());
        for (Timed t : new ArrayList<>(TIMED)) {
            if (now < t.from() || !(level.getEntity(t.player()) instanceof AbstractClientPlayer player) || player.isInvisible()) {
                continue;
            }
            switch (t.kind()) {
                case WEB_HAND -> webHand(level, player);
                case BURST_RING -> burstRing(level, player);
                case SURGE_WISPS -> wisps(level, player);
                case LEGS_SPARKS -> legSparks(level, player);
                case KILL_FLICKER -> flicker(level, player, now);
                case SENSE_RING -> senseRing(level, player);
            }
        }
    }

    static void clear() {
        TIMED.clear();
    }

    // ---- the places ----

    /**
     * How large the particles of an effect at his hands and head are drawn: the local hero's own hands are a hand's
     * length from the camera in first person, where a full-size mote would fill a tenth of the screen.
     */
    private static float size(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson() ? 0.4F : 1.0F;
    }

    /** The hand that shoots: the tracked one, or (the local hero in first person) where the web leaves it. */
    private static Vec3 hand(AbstractClientPlayer player) {
        Vec3 tracked = HandTracker.hand(player, player.getMainArm(), 0.0F);
        return tracked != null ? tracked : WorldEffectsRenderer.handPosition(Minecraft.getInstance(), player, 1.0F);
    }

    /** The middle of his chest. */
    private static Vec3 chest(AbstractClientPlayer player) {
        Vec3 right = HandTracker.shoulder(player, HumanoidArm.RIGHT, 0.0F);
        Vec3 left = HandTracker.shoulder(player, HumanoidArm.LEFT, 0.0F);
        return right != null && left != null ? right.add(left).scale(0.5).add(0.0, -0.2, 0.0)
                : player.position().add(0.0, player.getBbHeight() * 0.65, 0.0);
    }

    /**
     * The four roots of the Iron Spider's legs on his back (upper left, upper right, lower left, lower right: the
     * layer's own numbering), by his body's turn.
     */
    static Vec3[] roots(AbstractClientPlayer player) {
        double yaw = Math.toRadians(player.yBodyRot);
        Vec3 back = new Vec3(Math.sin(yaw), 0.0, -Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3 base = player.position().add(back.scale(0.22));
        // 3 and 8 pixels down from the top of the body, 3 pixels either side of the spine (a pixel is 0.0586 blocks).
        double top = 1.5;
        return new Vec3[]{
                base.add(left.scale(0.18)).add(0.0, top - 0.18, 0.0), base.add(left.scale(-0.18)).add(0.0, top - 0.18, 0.0),
                base.add(left.scale(0.18)).add(0.0, top - 0.47, 0.0), base.add(left.scale(-0.18)).add(0.0, top - 0.47, 0.0)
        };
    }

    // ---- the effects ----

    private static void webHand(ClientLevel level, AbstractClientPlayer player) {
        RandomSource random = level.random;
        Vec3 hand = hand(player);
        for (int i = 0; i < 2; i++) {
            level.addParticle(new EnergyParticleOptions(WHITE, 0.5F * size(player)), hand.x + (random.nextDouble() - 0.5) * 0.12,
                    hand.y + (random.nextDouble() - 0.5) * 0.12, hand.z + (random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.5) * 0.04,
                    0.01 + random.nextDouble() * 0.02, (random.nextDouble() - 0.5) * 0.04);
        }
    }

    /** The arms are flung wide: a ring of white motes leaves the chest, flat, and a few fly with it. */
    private static void burstRing(ClientLevel level, AbstractClientPlayer player) {
        Vec3 chest = chest(player);
        FlightEffects.ring(level, player.getId(), chest, new Vec3(0, 1, 0), 8, 0.3F, 2.6F, 0.07F, WHITE, 0.7F);
        RandomSource random = level.random;
        for (int i = 0; i < 16; i++) {
            double angle = i * Mth.TWO_PI / 16 + random.nextDouble() * 0.3;
            double speed = 0.18 + random.nextDouble() * 0.06;
            level.addParticle(new EnergyParticleOptions(WHITE, 0.6F * size(player)), chest.x, chest.y, chest.z, Math.cos(angle) * speed,
                    (random.nextDouble() - 0.5) * 0.04, Math.sin(angle) * speed);
        }
    }

    /** Black wisps rise round his head, with a dark red fleck now and then. */
    private static void wisps(ClientLevel level, AbstractClientPlayer player) {
        RandomSource random = level.random;
        Vec3 head = WorldEffectsRenderer.foreheadPosition(Minecraft.getInstance(), player, 0.0F);
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            level.addParticle(new EnergyParticleOptions(i == 0 ? GLINT : WISP, (i == 0 ? 0.5F : 1.0F) * size(player)), head.x + Math.cos(angle) * 0.3,
                    head.y - 0.1 + random.nextDouble() * 0.3, head.z + Math.sin(angle) * 0.3, (random.nextDouble() - 0.5) * 0.02,
                    0.04 + random.nextDouble() * 0.03, (random.nextDouble() - 0.5) * 0.02);
        }
    }

    /** Gold sparks fly from the four roots as the legs unfold. */
    private static void legSparks(ClientLevel level, AbstractClientPlayer player) {
        RandomSource random = level.random;
        for (Vec3 root : roots(player)) {
            level.addParticle(new EnergyParticleOptions(random.nextInt(3) == 0 ? RED : GOLD, 0.7F * size(player)), root.x, root.y, root.z,
                    (random.nextDouble() - 0.5) * 0.12, (random.nextDouble() - 0.3) * 0.1, (random.nextDouble() - 0.5) * 0.12);
        }
    }

    /** A red flicker at the lenses, every other tick. */
    private static void flicker(ClientLevel level, AbstractClientPlayer player, long now) {
        if ((now & 1L) != 0L) {
            return;
        }
        RandomSource random = level.random;
        Vec3 eyes = player.getEyePosition().add(player.getLookAngle().scale(0.28));
        Vec3 right = player.getLookAngle().cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        for (int side = -1; side <= 1; side += 2) {
            Vec3 at = eyes.add(right.scale(0.12 * side));
            level.addParticle(new EnergyParticleOptions(random.nextBoolean() ? RED : GLINT, 0.4F * size(player)), at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    /** A pale ring round his head, flat, spreading out. */
    private static void senseRing(ClientLevel level, AbstractClientPlayer player) {
        Vec3 head = WorldEffectsRenderer.foreheadPosition(Minecraft.getInstance(), player, 0.0F);
        FlightEffects.ring(level, player.getId(), head, new Vec3(0, 1, 0), 9, 0.25F, 1.5F, 0.05F, PALE_BLUE, 0.6F);
    }

    // ---- the landing ----

    /**
     * A hard landing: a ring of white motes on the ground and a low spray of them (not made for a hero out of view).
     */
    static void landing(AbstractClientPlayer player, float fall) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible() || !EnhancedClientConfig.flightEffects()) {
            return;
        }
        RandomSource random = level.random;
        Vec3 feet = player.position();
        FlightEffects.ring(level, player.getId(), feet.add(0, 0.05, 0), new Vec3(0, 1, 0), 8, 0.4F, 2.2F + Math.min(fall, 1.5F), 0.08F, WHITE, 0.7F);
        for (int i = 0; i < 12; i++) {
            double angle = i * Mth.TWO_PI / 12 + random.nextDouble() * 0.4;
            level.addParticle(new EnergyParticleOptions(WHITE, 0.7F), feet.x, feet.y + 0.05, feet.z, Math.cos(angle) * 0.16, 0.05 + random.nextDouble() * 0.05,
                    Math.sin(angle) * 0.16);
        }
    }

    private SpiderGestureFx() {
    }
}
