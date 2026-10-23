package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.CyclopsAbilities;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.CyclopsFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.client.render.EnergyGeometry.argb;

/**
 * The blasts of Cyclops (3.3) that are not his two beams, drawn from the server's {@link CyclopsFxPayload}s the same for
 * everyone: the Ricochet Shot's bolt racing along its path and fading, the Concussion Blast's rings thrown out in a cone,
 * the short blast at the ground under an Optic Leap with its ring of dust, and the ring of the Rally. Each lives a few
 * ticks and is drawn every frame from the frame's time (partial ticks included), so it moves smoothly and fades out
 * rather than popping.
 */
@Subscriber(side = Side.CLIENT)
public final class CyclopsFx {
    private static final int RED = CyclopsAbilities.RED;
    private static final int CORE = CyclopsAbilities.CORE;
    private static final int DEEP = 0xA00C1E;
    private static final int MAX = 48;

    /** How long each kind is drawn, in ticks. */
    static int life(byte kind) {
        return switch (kind) {
            case CyclopsFxPayload.RICOCHET -> 8;
            case CyclopsFxPayload.CONCUSSION -> 10;
            case CyclopsFxPayload.LEAP -> 12;
            case CyclopsFxPayload.RALLY -> 18;
            default -> 0;
        };
    }

    private record Effect(CyclopsFxPayload payload, long start) {
    }

    private static final List<Effect> EFFECTS = new ArrayList<>();
    /** 3.5: the colour a custom hero's pick chose for the effect being made, or -1 (the Optic Blast's red). */
    private static int tint = -1;

    /** One of the red's colours, or it recoloured to the colour chosen for the effect being made. */
    private static int c(int rgb) {
        return tint < 0 ? rgb : AbilityColors.recolor(rgb, tint);
    }

    /** The colour the shooter's pick of the ability chose for a payload, or -1. */
    private static int tintOf(ClientLevel level, CyclopsFxPayload p) {
        HeroAbility row = switch (p.kind()) {
            case CyclopsFxPayload.RICOCHET -> HeroAbility.CYCLOPS_RICOCHET;
            case CyclopsFxPayload.CONCUSSION -> HeroAbility.CYCLOPS_CONCUSSION;
            case CyclopsFxPayload.LEAP -> HeroAbility.CYCLOPS_LEAP;
            case CyclopsFxPayload.RALLY -> HeroAbility.CYCLOPS_RALLY;
            default -> null;
        };
        return row == null ? -1 : AbilityColors.chosen(level.getEntity(p.shooter()), row);
    }

    /** A blast of his: drawn from now on for its life, with a few particles where it strikes. */
    public static void handle(CyclopsFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.points().isEmpty()) {
            return;
        }
        if (EFFECTS.size() >= MAX) {
            EFFECTS.remove(0);
        }
        EFFECTS.add(new Effect(payload, level.getGameTime()));
        RandomSource r = level.random;
        List<Vec3> points = payload.points();
        tint = tintOf(level, payload);
        switch (payload.kind()) {
            case CyclopsFxPayload.RICOCHET -> {
                for (int i = 1; i < points.size(); i++) {
                    Vec3 at = points.get(i);
                    for (int k = 0; k < 4; k++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, (r.nextDouble() - 0.5) * 0.2,
                                (r.nextDouble() - 0.5) * 0.2, (r.nextDouble() - 0.5) * 0.2);
                    }
                }
            }
            case CyclopsFxPayload.LEAP -> {
                Vec3 spot = points.get(0);
                BlockState ground = level.getBlockState(BlockPos.containing(spot.x, spot.y - 0.05, spot.z));
                if (!ground.isAir()) {
                    BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, ground);
                    for (int k = 0; k < 24; k++) {
                        double a = r.nextDouble() * Mth.TWO_PI;
                        double s = 0.15 + r.nextDouble() * 0.2;
                        level.addParticle(dust, spot.x, spot.y + 0.1, spot.z, Math.cos(a) * s, 0.15 + r.nextDouble() * 0.2, Math.sin(a) * s);
                    }
                }
                for (int k = 0; k < 10; k++) {
                    level.addParticle(new EnergyParticleOptions(k % 2 == 0 ? c(RED) : c(CORE), 1.0F), spot.x, spot.y + 0.1, spot.z,
                            (r.nextDouble() - 0.5) * 0.3, 0.05 + r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.3);
                }
            }
            case CyclopsFxPayload.CONCUSSION -> {
                if (points.size() >= 2) {
                    Vec3 dir = points.get(1).normalize();
                    Vec3 eye = points.get(0).add(dir.scale(1.2)); // (ahead of the visor: never in the eyes of the one who fires)
                    for (int k = 0; k < 12; k++) {
                        Vec3 v = dir.scale(0.4 + r.nextDouble() * 0.4).add((r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3,
                                (r.nextDouble() - 0.5) * 0.3);
                        level.addParticle(new EnergyParticleOptions(k % 3 == 0 ? c(CORE) : c(RED), 0.9F), eye.x, eye.y, eye.z, v.x, v.y, v.z);
                    }
                }
            }
            default -> {
            }
        }
        tint = -1;
    }

    /** Once per client tick: the effects whose time is over go. */
    static void tick(ClientLevel level) {
        long now = level.getGameTime();
        EFFECTS.removeIf(e -> now - e.start() > life(e.payload().kind()) || now < e.start() - 1);
    }

    static void clear() {
        EFFECTS.clear();
    }

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || EFFECTS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partial;
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
        for (Effect effect : EFFECTS) {
            float age = time - effect.start();
            float life = life(effect.payload().kind());
            if (age < 0.0F || age > life) {
                continue;
            }
            float u = age / life;
            CyclopsFxPayload p = effect.payload();
            tint = tintOf(level, p);
            switch (p.kind()) {
                case CyclopsFxPayload.RICOCHET -> ricochet(pose, vc, p.points(), camera, age, u);
                case CyclopsFxPayload.CONCUSSION -> concussion(pose, vc, p, camera, age);
                case CyclopsFxPayload.LEAP -> leap(level, pose, vc, p, camera, age, partial);
                case CyclopsFxPayload.RALLY -> rally(pose, vc, p, camera, age);
                default -> {
                }
            }
        }
        tint = -1;
        buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
    }

    /** How far along (0 to 1) the bolt of a ricochet has raced at {@code age} ticks: the whole path in two ticks. */
    static float ricochetReach(float age) {
        return Mth.clamp(age / 2.0F, 0.0F, 1.0F);
    }

    /** The bolt races along its path in two ticks and fades over the rest of its life. */
    private static void ricochet(Matrix4f pose, VertexConsumer vc, List<Vec3> points, Vec3 camera, float age, float u) {
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i).distanceTo(points.get(i - 1));
        }
        double reach = total * ricochetReach(age);
        float fade = 1.0F - Mth.clamp((age - 2.0F) / 6.0F, 0.0F, 1.0F);
        fade *= fade;
        double along = 0.0;
        for (int i = 1; i < points.size() && along < reach; i++) {
            Vec3 a = points.get(i - 1);
            Vec3 b = points.get(i);
            double length = a.distanceTo(b);
            Vec3 end = along + length <= reach ? b : a.add(b.subtract(a).scale((reach - along) / Math.max(1.0E-6, length)));
            Vec3 ca = a.subtract(camera);
            Vec3 cb = end.subtract(camera);
            EnergyGeometry.ribbon(pose, vc, ca, cb, 0.22F, argb(c(DEEP), 0.30F * fade), argb(c(DEEP), 0.30F * fade));
            EnergyGeometry.ribbon(pose, vc, ca, cb, 0.11F, argb(c(RED), 0.85F * fade), argb(c(RED), 0.85F * fade));
            EnergyGeometry.ribbon(pose, vc, ca, cb, 0.035F, argb(c(CORE), fade), argb(c(CORE), fade));
            if (end == b && i < points.size() - 1) {
                EnergyGeometry.ring(pose, vc, cb, cb.scale(-1), 0.18F, 0.08F, 10, argb(c(CORE), 0.8F * fade));
            }
            along += length;
        }
    }

    /** Three rings thrown out along the cone one after another, widening with the cone and fading as they go. */
    private static void concussion(Matrix4f pose, VertexConsumer vc, CyclopsFxPayload p, Vec3 camera, float age) {
        if (p.points().size() < 2) {
            return;
        }
        Vec3 eye = p.points().get(0);
        Vec3 dir = p.points().get(1).normalize();
        double range = p.param() / 10.0;
        double spread = Math.tan(Math.toRadians(35.0));
        for (int k = 0; k < 3; k++) {
            float t = Mth.clamp((age - k * 1.5F) / 6.0F, 0.0F, 1.0F);
            if (t <= 0.0F || t >= 1.0F) {
                continue;
            }
            double d = 0.4 + range * t;
            float radius = (float) (0.2 + d * spread);
            float alpha = (1.0F - t) * (1.0F - t);
            Vec3 at = eye.add(dir.scale(d)).subtract(camera);
            EnergyGeometry.ring(pose, vc, at, dir, radius, 0.35F * (1.0F - t) + 0.08F, 24, argb(c(RED), 0.75F * alpha));
            EnergyGeometry.ring(pose, vc, at, dir, radius * 0.92F, 0.08F, 24, argb(c(CORE), 0.7F * alpha));
        }
        // The flash at the visor as it leaves.
        float flash = 1.0F - Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        if (flash > 0.0F) {
            Vec3 at = eye.add(dir.scale(0.4)).subtract(camera);
            EnergyGeometry.ring(pose, vc, at, at.scale(-1), 0.25F * flash, 0.3F * flash, 12,
                    argb(c(CORE), 0.9F * flash));
        }
    }

    /** A short, thick blast from his eyes to the ground for the first ticks, and a ring of red over the ground. */
    private static void leap(ClientLevel level, Matrix4f pose, VertexConsumer vc, CyclopsFxPayload p, Vec3 camera, float age, float partial) {
        Vec3 spot = p.points().get(0);
        float radius = Math.max(1.0F, p.param() / 10.0F);
        float beam = 1.0F - Mth.clamp(age / 4.0F, 0.0F, 1.0F);
        if (beam > 0.0F) {
            Vec3 from = p.points().size() > 1 ? p.points().get(1) : spot.add(0, 1.6, 0);
            if (level.getEntity(p.shooter()) instanceof AbstractClientPlayer hero) {
                from = CyclopsBeamFx.eyes(hero, partial); // (he is already rising: the blast stays on his eyes)
            }
            Vec3 a = from.subtract(camera);
            Vec3 b = spot.subtract(camera);
            EnergyGeometry.ribbon(pose, vc, a, b, 0.12F, 0.5F, argb(c(DEEP), 0.35F * beam),
                    argb(c(DEEP), 0.35F * beam));
            EnergyGeometry.ribbon(pose, vc, a, b, 0.06F, 0.28F, argb(c(RED), 0.85F * beam), argb(c(RED), 0.85F * beam));
            EnergyGeometry.ribbon(pose, vc, a, b, 0.02F, 0.08F, argb(c(CORE), beam), argb(c(CORE), beam));
        }
        float t = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
        float alpha = (1.0F - t) * (1.0F - t);
        Vec3 at = spot.add(0, 0.08, 0).subtract(camera);
        EnergyGeometry.ring(pose, vc, at, new Vec3(0, 1, 0), 0.3F + radius * t, 0.4F * (1.0F - t) + 0.06F, 28, argb(c(RED), 0.8F * alpha));
    }

    /** Two rings spread over the ground from his feet to the edge of the Rally, the second a moment after the first. */
    private static void rally(Matrix4f pose, VertexConsumer vc, CyclopsFxPayload p, Vec3 camera, float age) {
        Vec3 feet = p.points().get(0).add(0, 0.1, 0).subtract(camera);
        float radius = Math.max(1, p.param());
        for (int k = 0; k < 2; k++) {
            float t = Mth.clamp((age - k * 4.0F) / 14.0F, 0.0F, 1.0F);
            if (t <= 0.0F || t >= 1.0F) {
                continue;
            }
            float ease = 1.0F - (1.0F - t) * (1.0F - t);
            float alpha = 1.0F - t;
            EnergyGeometry.ring(pose, vc, feet, new Vec3(0, 1, 0), 0.5F + radius * ease, 0.5F * alpha + 0.05F, 48, argb(c(RED), 0.6F * alpha));
            EnergyGeometry.ring(pose, vc, feet, new Vec3(0, 1, 0), 0.45F + radius * ease, 0.1F, 48, argb(c(CORE), 0.5F * alpha));
        }
    }

    private CyclopsFx() {
    }
}
