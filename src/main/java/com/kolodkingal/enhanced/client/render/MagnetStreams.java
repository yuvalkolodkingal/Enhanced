package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.network.MagnetPullPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * What every client draws of Magneto's Magnetic Pull (D-M 8.3): the colourless pulse of his field growing round his feet to
 * the pull's radius, and each thing taken flying to his hand as its item, along a curved line of force, with a soft clink
 * when it arrives. The server sends one {@link MagnetPullPayload} per tick of a pull to him and everyone who sees him; this
 * class keeps at most {@link #MAX_STREAMS} streams (the oldest go first) and forgets them all at logout.
 *
 * <p>The streams aim at his hand as it is in every frame ({@link WorldEffectsRenderer#handPosition}), so they follow him
 * as he moves. With reduced motion the icons do not spin and the curve is half as high; they still fly, because they
 * say where the metal came from. The timing and the curve are the pure {@link MagnetStreamMath}. Only ever loaded on a
 * client: from the handler of {@link MagnetPullPayload}, {@code WorldEffectsRenderer} and {@code ClientEvents}.
 */
public final class MagnetStreams {
    /** The most streams alive on a client, and the most pulses. */
    private static final int MAX_STREAMS = 128;
    private static final int MAX_PULSES = 16;
    /** Nothing farther from the camera than this is drawn. */
    private static final double MAX_DISTANCE = 96.0;
    /** An icon closer to the camera than this is not drawn, and shrinks over the next {@link #NEAR_FADE} blocks. */
    private static final double NEAR_CAMERA = 0.4;
    private static final double NEAR_FADE = 1.6;
    /** The pulse and the lines of force: his field has no colour of its own, only the pale grey of his glow. */
    private static final float PULSE_ALPHA = 0.25F;
    private static final float LINE_ALPHA = 0.2F;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    /** The field's pulse round one Magneto, from where he stood when the pull began (it follows him while he is seen). */
    private record Pulse(int magneto, Vec3 at, float radius, long start) {
    }

    /** One thing taken: {@code count} icons that leave {@link MagnetStreamMath#ICON_GAP} ticks apart, each flying {@code ticks}. */
    private static final class Stream {
        final int magneto;
        final Vec3 from;
        final ItemStack stack;
        final int count;
        final int ticks;
        final double height;
        final long start;
        /** Where the spin starts, so icons of the same item do not turn in step. */
        final float phase;
        /** How many of its icons have arrived. */
        int arrived;

        Stream(int magneto, Vec3 from, ItemStack stack, int count, int ticks, double height, long start, float phase) {
            this.magneto = magneto;
            this.from = from;
            this.stack = stack;
            this.count = count;
            this.ticks = ticks;
            this.height = height;
            this.start = start;
            this.phase = phase;
        }

        /** When icon {@code icon} leaves. */
        long leaves(int icon) {
            return start + (long) icon * MagnetStreamMath.ICON_GAP;
        }
    }

    private static final List<Pulse> PULSES = new ArrayList<>();
    private static final List<Stream> STREAMS = new ArrayList<>();
    /** The tick of each Magneto's last clink: at most one a tick. */
    private static final Map<Integer, Long> CLINKED = new HashMap<>();

    /**
     * One tick of a pull (the handler of {@link MagnetPullPayload}): the first starts the field's pulse; each stream starts
     * its icons on their way to his hand, with a few grey motes where it leaves.
     */
    public static void accept(MagnetPullPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Entity magneto = level.getEntity(payload.magneto());
        if (magneto == null) {
            return; // nobody to draw it round, nor to fly to
        }
        long now = level.getGameTime();
        if (payload.first()) {
            if (PULSES.size() >= MAX_PULSES) {
                PULSES.remove(0);
            }
            PULSES.add(new Pulse(payload.magneto(), magneto.position(), payload.radius(), now));
        }
        if (payload.streams().isEmpty()) {
            return;
        }
        Vec3 hand = hand(mc, magneto, 1.0F);
        boolean calm = EnhancedClientConfig.reducedMotion();
        RandomSource random = level.random;
        for (MagnetPullPayload.Stream s : payload.streams()) {
            double distance = s.from().distanceTo(hand);
            while (STREAMS.size() >= MAX_STREAMS) {
                STREAMS.remove(0);
            }
            STREAMS.add(new Stream(payload.magneto(), s.from(), new ItemStack(s.item()), Mth.clamp(s.count(), 1, 3),
                    MagnetStreamMath.flightTicks(distance), MagnetStreamMath.arcHeight(distance, calm), now, random.nextFloat() * 360.0F));
            // (A block crumbles with its own break particles from the server: these are the field taking hold.)
            for (int i = 0; i < 4; i++) {
                level.addParticle(new EnergyParticleOptions(color(magneto), 0.8F), s.from().x + (random.nextDouble() - 0.5) * 0.5,
                        s.from().y + (random.nextDouble() - 0.5) * 0.5, s.from().z + (random.nextDouble() - 0.5) * 0.5,
                        (random.nextDouble() - 0.5) * 0.04, 0.02 + random.nextDouble() * 0.03, (random.nextDouble() - 0.5) * 0.04);
            }
        }
    }

    /**
     * Once per client tick: the icons that reached his hand this tick clink (softly, at most once a tick for each
     * Magneto) and leave two grey motes there; finished streams and pulses are forgotten.
     */
    public static void tick(ClientLevel level) {
        if (STREAMS.isEmpty() && PULSES.isEmpty()) {
            CLINKED.clear();
            return;
        }
        long now = level.getGameTime();
        PULSES.removeIf(p -> now - p.start() > MagnetStreamMath.PULSE_TICKS || now < p.start());
        Minecraft mc = Minecraft.getInstance();
        RandomSource random = level.random;
        for (Iterator<Stream> it = STREAMS.iterator(); it.hasNext(); ) {
            Stream s = it.next();
            Entity magneto = level.getEntity(s.magneto);
            if (magneto == null || now < s.start) {
                it.remove();
                continue;
            }
            while (s.arrived < s.count && now >= s.leaves(s.arrived) + s.ticks) {
                s.arrived++;
                arrive(mc, level, magneto, now, random);
            }
            if (s.arrived >= s.count) {
                it.remove();
            }
        }
        CLINKED.values().removeIf(at -> now - at > 1);
    }

    /** An icon reached his hand: a soft clink at him (once a tick at most) and two grey motes at the hand. */
    private static void arrive(Minecraft mc, ClientLevel level, Entity magneto, long now, RandomSource random) {
        Long last = CLINKED.get(magneto.getId());
        if (last == null || last != now) {
            CLINKED.put(magneto.getId(), now);
            level.playLocalSound(magneto.getX(), magneto.getY(), magneto.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.15F,
                    1.4F + random.nextFloat() * 0.6F, false);
        }
        Vec3 hand = hand(mc, magneto, 1.0F);
        // A hand's length from the camera in first person: a full-size mote would fill the view.
        boolean firstPerson = magneto == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        float size = firstPerson ? 0.32F : 0.8F;
        for (int i = 0; i < 2; i++) {
            level.addParticle(new EnergyParticleOptions(color(magneto), size), hand.x + (random.nextDouble() - 0.5) * 0.15,
                    hand.y + (random.nextDouble() - 0.5) * 0.15, hand.z + (random.nextDouble() - 0.5) * 0.15, 0.0, 0.01, 0.0);
        }
    }

    /** Forgets every pulse and stream (at logout, and when the client's world goes). */
    public static void clear() {
        PULSES.clear();
        STREAMS.clear();
        CLINKED.clear();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Drawing (from WorldEffectsRenderer.onRenderLevel, after its own energy)

    /**
     * Draws the pulses and the lines of force (in {@link EnhancedRenderTypes#ENERGY_BEAM}), then the flying items, each
     * lit by the world where it is. Ends its own batches.
     *
     * <p>Every age is the whole ticks since its start, counted in {@code long}, plus the frame's partial tick: a float game
     * time loses its fraction in a world played long (half ticks from about 58 hours of world time, whole ticks from 116),
     * and the streams would move in steps.
     */
    public static void render(Minecraft mc, ClientLevel level, MultiBufferSource.BufferSource buffers, Vec3 camera, float partialTick,
                              long gameTime) {
        if (PULSES.isEmpty() && STREAMS.isEmpty()) {
            return;
        }
        Matrix4f pose = new Matrix4f();
        VertexConsumer light = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Pulse p : PULSES) {
            float age = age(gameTime, p.start(), partialTick);
            if (age < 0.0F || age > MagnetStreamMath.PULSE_TICKS) {
                continue;
            }
            Entity magneto = level.getEntity(p.magneto());
            Vec3 feet = (magneto != null ? magneto.getPosition(partialTick) : p.at()).add(0.0, 0.06, 0.0);
            if (feet.distanceToSqr(camera) > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }
            float radius = MagnetStreamMath.pulseRadius(age, p.radius());
            float fade = MagnetStreamMath.pulseFade(age);
            int segments = Mth.clamp((int) (radius * 8.0F), 24, 96);
            Vec3 centre = feet.subtract(camera);
            int glow = glow(magneto);
            EnergyGeometry.ring(pose, light, centre, UP, radius, 0.14F, segments, EnergyGeometry.argb(glow, PULSE_ALPHA * fade));
            // A fainter echo just inside it, so the field reads as a wave rather than a hoop.
            EnergyGeometry.ring(pose, light, centre, UP, radius * 0.85F, 0.06F, segments,
                    EnergyGeometry.argb(glow, PULSE_ALPHA * 0.5F * fade));
        }
        // Each Magneto's hand, once a frame (most streams of a frame fly to the same one).
        Map<Integer, Vec3> hands = new HashMap<>();
        for (Stream s : STREAMS) {
            Vec3 hand = hands.computeIfAbsent(s.magneto, id -> {
                Entity magneto = level.getEntity(id);
                return magneto == null ? null : hand(mc, magneto, partialTick);
            });
            if (hand != null && s.arrived < s.count) {
                lineOfForce(s, hand, glow(level.getEntity(s.magneto)), pose, light, camera, age(gameTime, s.leaves(s.arrived), partialTick));
            }
        }
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);

        boolean calm = EnhancedClientConfig.reducedMotion();
        PoseStack stack = new PoseStack();
        for (Stream s : STREAMS) {
            Vec3 hand = hands.get(s.magneto);
            if (hand == null) {
                continue;
            }
            int handLight = LevelRenderer.getLightColor(level, BlockPos.containing(hand));
            for (int icon = s.arrived; icon < s.count; icon++) {
                float age = age(gameTime, s.leaves(icon), partialTick);
                if (age < 0.0F || age >= s.ticks) {
                    continue;
                }
                double along = MagnetStreamMath.along(age / s.ticks);
                Vec3 at = MagnetStreamMath.point(s.from, hand, s.height, along);
                Vec3 rel = at.subtract(camera);
                double distance = rel.length();
                if (distance > MAX_DISTANCE) {
                    continue;
                }
                // Near the camera it shrinks away (into his own hand in first person; past the lens of a camera in front of
                // him), so it never fills the view.
                float near = (float) Mth.clamp((distance - NEAR_CAMERA) / NEAR_FADE, 0.0, 1.0);
                if (near < 0.05F) {
                    continue;
                }
                float scale = MagnetStreamMath.scale(along) * near;
                // It spins as it flies; with reduced motion it only turns its face to the camera.
                float yaw = calm ? (float) (Mth.atan2(rel.x, rel.z) * Mth.RAD_TO_DEG) + 180.0F : s.phase + age * MagnetStreamMath.SPIN;
                // Lit by the world where it is; still inside the rock it came out of (pitch dark), as where it is going.
                int packedLight = LevelRenderer.getLightColor(level, BlockPos.containing(at));
                stack.pushPose();
                stack.translate(rel.x, rel.y, rel.z);
                stack.mulPose(Axis.YP.rotationDegrees(yaw));
                stack.scale(scale, scale, scale);
                mc.getItemRenderer().renderStatic(s.stack, ItemDisplayContext.GROUND, packedLight == 0 ? handLight : packedLight,
                        OverlayTexture.NO_OVERLAY, stack, buffers, level, s.magneto * 31 + icon);
                stack.popPose();
            }
        }
        buffers.endBatch();
    }

    /** Ticks since {@code start} in this frame: the whole ticks in {@code long}, then the partial tick. */
    private static float age(long gameTime, long start, float partialTick) {
        return (float) (gameTime - start) + partialTick;
    }

    /**
     * The thin colourless line of force behind the leading icon ({@code age}: ticks since it left): the curve from where it
     * left to where it is now, fainter toward where it left and fading out as it arrives (the look of his field).
     */
    private static void lineOfForce(Stream s, Vec3 hand, int glow, Matrix4f pose, VertexConsumer vc, Vec3 camera, float age) {
        if (age <= 0.0F) {
            return;
        }
        float u = Math.min(1.0F, age / s.ticks);
        double head = MagnetStreamMath.along(u);
        float out = 1.0F - u * u;
        int steps = 10;
        Vec3 prev = MagnetStreamMath.point(s.from, hand, s.height, 0.0).subtract(camera);
        for (int i = 1; i <= steps; i++) {
            Vec3 next = MagnetStreamMath.point(s.from, hand, s.height, head * i / steps).subtract(camera);
            float a = LINE_ALPHA * out * (0.35F + 0.65F * i / steps);
            EnergyGeometry.ribbon(pose, vc, prev, next, 0.035F, EnergyGeometry.argb(glow, a * 0.9F), EnergyGeometry.argb(glow, a));
            prev = next;
        }
    }

    /** The deep colour of a pull's field: Magneto's, or (3.4) the colour of a custom hero who pulls with his ability. */
    private static int color(@Nullable Entity magneto) {
        return magneto instanceof Player player && Powers.get(player).power() == Power.CUSTOM ? Powers.color(player) : Power.MAGNETO.color;
    }

    /** The glow of a pull's field (its rings and lines of force), as {@link #color}. */
    private static int glow(@Nullable Entity magneto) {
        return magneto instanceof Player player && Powers.get(player).power() == Power.CUSTOM ? Powers.glow(player) : Power.MAGNETO.glow;
    }

    /** Where the streams fly: his hand (where every hero's energy leaves it), or the middle of a body that is not a player. */
    private static Vec3 hand(Minecraft mc, Entity magneto, float partialTick) {
        if (magneto instanceof AbstractClientPlayer player) {
            return WorldEffectsRenderer.handPosition(mc, player, partialTick);
        }
        return magneto.getPosition(partialTick).add(0.0, magneto.getBbHeight() * 0.6, 0.0);
    }

    private MagnetStreams() {
    }
}
