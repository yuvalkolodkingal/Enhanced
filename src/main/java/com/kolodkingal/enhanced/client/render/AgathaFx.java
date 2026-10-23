package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AgathaAbilities;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.AgathaFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * The pictures of Agatha's magic (3.3), drawn from {@link AgathaFxPayload}s in step with the game time and the partial tick
 * (so nothing snaps or blinks from one tick to the next), the same for everyone near:
 * <ul>
 *     <li>the Rune Trap: a circle of glowing runes laid flat on the ground, two rings of violet light and a seven-pointed
 *     sigil inside them, turning slowly, with a thin curtain of light rising off its edge; it fades in and out;</li>
 *     <li>the Power Drain: three twisting violet strands flowing from the drained creature into her hands, with bright beads
 *     running along them toward her;</li>
 *     <li>the shield shattering: violet shards flung off the sphere, spinning and fading.</li>
 * </ul>
 */
@Subscriber(side = Side.CLIENT)
public final class AgathaFx {
    /** The runes: a 4 x 4 sheet of glyphs, white on clear (tinted when drawn). */
    private static final ResourceLocation RUNES = Enhanced.id("textures/entity/agatha_runes.png");
    private static final int GLYPHS = 16;
    private static final float FADE_IN = 8.0F;
    private static final float FADE_OUT = 12.0F;
    /** How far above the ground the circle lies (it never fights the block it lies on). */
    private static final double LIFT = 0.035;
    private static final int RING_SEGMENTS = 72;
    private static final int SHARDS = 28;
    private static final float SHARD_TICKS = 16.0F;

    /** A circle of runes: its owner, where, how big, when it began, when it ends (or began to fade early). */
    private static final class Circle {
        final int owner;
        final Vec3 center;
        final float radius;
        final long start;
        long end;

        Circle(int owner, Vec3 center, float radius, long start, long end) {
            this.owner = owner;
            this.center = center;
            this.radius = radius;
            this.start = start;
            this.end = end;
        }
    }

    private record Drain(int agatha, int target, Vec3 from, long start, int ticks, boolean power) {
    }

    private record Shatter(Vec3 center, float radius, long start, long seed, int owner) {
    }

    private static final List<Circle> CIRCLES = new ArrayList<>();
    /** 3.5: the colour a custom hero's pick chose for the effect being made, or -1 (Agatha's violet and lilac). */
    private static int tint = -1;

    /** One of Agatha's colours, or it recoloured to the colour chosen for the effect being made. */
    private static int c(int rgb) {
        return tint < 0 ? rgb : AbilityColors.recolor(rgb, tint);
    }

    /** The colour a hero's pick of {@code row} chose ({@code id} his entity id), or -1. */
    private static int tintOf(int id, HeroAbility row) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? -1 : AbilityColors.chosen(level.getEntity(id), row);
    }
    private static final List<Drain> DRAINS = new ArrayList<>();
    private static final List<Shatter> SHATTERS = new ArrayList<>();

    private AgathaFx() {
    }

    // ---------------------------------------------------------------------------------------------------------
    // The payloads

    /** An effect arrived from the server. */
    public static void handle(AgathaFxPayload p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        switch (p.kind()) {
            case AgathaFxPayload.RUNES -> {
                CIRCLES.removeIf(c -> c.owner == p.id());
                CIRCLES.add(new Circle(p.id(), p.a(), p.d() / 10.0F, now, now + p.c()));
            }
            case AgathaFxPayload.RUNES_END -> {
                for (Circle c : CIRCLES) {
                    if (c.owner == p.id() && c.end > now) {
                        c.end = now;
                    }
                }
            }
            case AgathaFxPayload.DRAIN -> DRAINS.add(new Drain(p.id(), p.other(), p.a(), now, p.c(), p.d() == 1));
            case AgathaFxPayload.SHIELD_BREAK -> SHATTERS.add(new Shatter(p.a(), p.c() / 10.0F, now, level.random.nextLong(), p.id()));
            default -> {
            }
        }
    }

    /** The ticks the runes of this Agatha still burn (0 if none). */
    public static long runesLeft(int owner, long now) {
        for (Circle c : CIRCLES) {
            if (c.owner == owner && c.end > now) {
                return c.end - now;
            }
        }
        return 0;
    }

    /** Once a tick: what has faded is forgotten, and the circles let a few motes rise. */
    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        CIRCLES.removeIf(c -> now > c.end + FADE_OUT);
        DRAINS.removeIf(d -> now > d.start() + d.ticks());
        SHATTERS.removeIf(s -> now > s.start() + SHARD_TICKS);
        RandomSource r = level.random;
        for (Circle c : CIRCLES) {
            tint = tintOf(c.owner, HeroAbility.AGATHA_RUNES);
            if (now < c.end && r.nextInt(2) == 0) {
                double a = r.nextDouble() * Mth.TWO_PI;
                double rr = c.radius * (0.85 + r.nextDouble() * 0.15);
                level.addParticle(new EnergyParticleOptions(r.nextBoolean() ? c(AgathaAbilities.LILAC) : c(AgathaAbilities.VIOLET), 0.6F),
                        c.center.x + Math.cos(a) * rr, c.center.y + 0.1, c.center.z + Math.sin(a) * rr, 0.0, 0.02 + r.nextDouble() * 0.02, 0.0);
            }
        }
        tint = -1;
    }

    public static void clear() {
        CIRCLES.clear();
        DRAINS.clear();
        SHATTERS.clear();
    }

    // ---------------------------------------------------------------------------------------------------------
    // Drawing

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (CIRCLES.isEmpty() && DRAINS.isEmpty() && SHATTERS.isEmpty()) {
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

        if (!CIRCLES.isEmpty()) {
            VertexConsumer light = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            for (Circle c : CIRCLES) {
                tint = tintOf(c.owner, HeroAbility.AGATHA_RUNES);
                circleLight(pose, light, c, camera, time);
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
            RenderType glyphType = EnhancedRenderTypes.energyField(RUNES);
            VertexConsumer glyphs = buffers.getBuffer(glyphType);
            for (Circle c : CIRCLES) {
                tint = tintOf(c.owner, HeroAbility.AGATHA_RUNES);
                glyphs(pose, glyphs, c, camera, time);
            }
            buffers.endBatch(glyphType);
        }
        if (!DRAINS.isEmpty() || !SHATTERS.isEmpty()) {
            VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM);
            for (Drain d : DRAINS) {
                tint = tintOf(d.agatha(), HeroAbility.AGATHA_DRAIN);
                drain(pose, vc, d, level, camera, partial, time);
            }
            for (Shatter s : SHATTERS) {
                tint = tintOf(s.owner(), HeroAbility.AGATHA_SHIELD);
                shards(pose, vc, s, camera, time);
            }
            buffers.endBatch(EnhancedRenderTypes.ENERGY_BEAM);
        }
        tint = -1;
    }

    /** How bright a circle is at this moment: it fades in after it is laid and out at its end. */
    static float circleAlpha(long start, long end, float time) {
        float in = Mth.clamp((time - start) / FADE_IN, 0.0F, 1.0F);
        float out = 1.0F - Mth.clamp((time - end) / FADE_OUT, 0.0F, 1.0F);
        float a = Math.min(in, out);
        return a * a * (3.0F - 2.0F * a);
    }

    /** The two rings, the sigil and the curtain of light off the edge (untextured, additive). */
    private static void circleLight(Matrix4f pose, VertexConsumer vc, Circle c, Vec3 camera, float time) {
        float a = circleAlpha(c.start, c.end, time);
        if (a <= 0.001F) {
            return;
        }
        float pulse = 0.85F + 0.15F * Mth.sin(time * 0.25F);
        Vec3 o = c.center.add(0, LIFT, 0).subtract(camera);
        float r = c.radius;
        float turn = time * 0.012F;
        flatRing(pose, vc, o, r, 0.16F, EnergyGeometry.argb(c(AgathaAbilities.VIOLET), 0.85F * a * pulse));
        flatRing(pose, vc, o, r - 0.08F, 0.05F, EnergyGeometry.argb(c(AgathaAbilities.LILAC), 0.6F * a));
        flatRing(pose, vc, o, r * 0.74F, 0.08F, EnergyGeometry.argb(c(AgathaAbilities.VIOLET), 0.7F * a * pulse));
        // The sigil: a seven-pointed star drawn in one line round the inner ring, turning the other way.
        float sr = r * 0.70F;
        int points = 7;
        for (int i = 0; i < points; i++) {
            double a0 = -turn * 1.5 + i * 3 * Mth.TWO_PI / points;
            double a1 = -turn * 1.5 + (i + 1) * 3 * Mth.TWO_PI / points;
            Vec3 p0 = o.add(Math.cos(a0) * sr, 0, Math.sin(a0) * sr);
            Vec3 p1 = o.add(Math.cos(a1) * sr, 0, Math.sin(a1) * sr);
            flatLine(pose, vc, p0, p1, 0.06F, EnergyGeometry.argb(c(AgathaAbilities.VIOLET), 0.55F * a * pulse));
        }
        // The curtain: a band of light rising off the edge, fading to nothing a block and a bit up.
        float height = 1.3F * a;
        int top = EnergyGeometry.argb(c(AgathaAbilities.VIOLET), 0.0F);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double t0 = i * Mth.TWO_PI / RING_SEGMENTS;
            double t1 = (i + 1) * Mth.TWO_PI / RING_SEGMENTS;
            float shimmer = 0.5F + 0.5F * Mth.sin((float) (time * 0.18F + i * 0.7F));
            int bottom = EnergyGeometry.argb(c(AgathaAbilities.VIOLET), (0.10F + 0.10F * shimmer) * a);
            Vec3 b0 = o.add(Math.cos(t0) * r, 0, Math.sin(t0) * r);
            Vec3 b1 = o.add(Math.cos(t1) * r, 0, Math.sin(t1) * r);
            vertex(pose, vc, b0, bottom);
            vertex(pose, vc, b1, bottom);
            vertex(pose, vc, b1.add(0, height, 0), top);
            vertex(pose, vc, b0.add(0, height, 0), top);
        }
    }

    /** The runes: glyphs from the sheet laid flat between the rings, turning slowly round the circle. */
    private static void glyphs(Matrix4f pose, VertexConsumer vc, Circle c, Vec3 camera, float time) {
        float a = circleAlpha(c.start, c.end, time);
        if (a <= 0.001F) {
            return;
        }
        Vec3 o = c.center.add(0, LIFT + 0.005, 0).subtract(camera);
        float r = c.radius * 0.86F;
        int count = Math.max(8, Math.round(c.radius * 3.0F));
        float size = Math.min(0.95F, c.radius * 0.21F);
        float turn = time * 0.012F;
        for (int i = 0; i < count; i++) {
            double angle = turn + i * Mth.TWO_PI / count;
            // Each rune lights up in turn as if spoken.
            float spoken = 0.55F + 0.45F * Mth.sin(time * 0.2F - i * 0.9F);
            int colour = EnergyGeometry.argb(c(AgathaAbilities.LILAC), 0.9F * a * spoken);
            Vec3 at = o.add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
            // The rune stands upright as read from the centre: its "up" points outward.
            Vec3 out = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(size * 0.5);
            Vec3 side = new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(size * 0.5);
            int glyph = Math.floorMod(i * 7 + c.owner, GLYPHS);
            float u0 = (glyph % 4) / 4.0F;
            float v0 = (glyph / 4) / 4.0F;
            float u1 = u0 + 0.25F;
            float v1 = v0 + 0.25F;
            texVertex(pose, vc, at.subtract(side).add(out), u0, v0, colour);
            texVertex(pose, vc, at.add(side).add(out), u1, v0, colour);
            texVertex(pose, vc, at.add(side).subtract(out), u1, v1, colour);
            texVertex(pose, vc, at.subtract(side).subtract(out), u0, v1, colour);
        }
    }

    /** A flat ring on the ground (a band of quads in the horizontal plane). */
    private static void flatRing(Matrix4f pose, VertexConsumer vc, Vec3 o, float radius, float width, int colour) {
        float in = radius - width * 0.5F;
        float out = radius + width * 0.5F;
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double t0 = i * Mth.TWO_PI / RING_SEGMENTS;
            double t1 = (i + 1) * Mth.TWO_PI / RING_SEGMENTS;
            vertex(pose, vc, o.add(Math.cos(t0) * in, 0, Math.sin(t0) * in), colour);
            vertex(pose, vc, o.add(Math.cos(t0) * out, 0, Math.sin(t0) * out), colour);
            vertex(pose, vc, o.add(Math.cos(t1) * out, 0, Math.sin(t1) * out), colour);
            vertex(pose, vc, o.add(Math.cos(t1) * in, 0, Math.sin(t1) * in), colour);
        }
    }

    /** A flat line on the ground. */
    private static void flatLine(Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float width, int colour) {
        Vec3 dir = b.subtract(a);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(width * 0.5);
        vertex(pose, vc, a.add(side), colour);
        vertex(pose, vc, a.subtract(side), colour);
        vertex(pose, vc, b.subtract(side), colour);
        vertex(pose, vc, b.add(side), colour);
    }

    /** The stream of the Power Drain: three strands twisting round the line from the target to her hands, beads flowing in. */
    private static void drain(Matrix4f pose, VertexConsumer vc, Drain d, ClientLevel level, Vec3 camera, float partial, float time) {
        float age = time - d.start();
        if (age < 0.0F || age > d.ticks()) {
            return;
        }
        Entity agatha = level.getEntity(d.agatha());
        if (agatha == null) {
            return;
        }
        Entity target = level.getEntity(d.target());
        Vec3 from = target != null ? target.getPosition(partial).add(0, target.getBbHeight() * 0.6, 0) : d.from();
        Vec3 to = hands(agatha, partial);
        float in = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
        float out = Mth.clamp((d.ticks() - age) / 5.0F, 0.0F, 1.0F);
        float a = Math.min(in, out);
        Vec3 axis = to.subtract(from);
        double length = axis.length();
        if (length < 0.2) {
            return;
        }
        Vec3 dir = axis.scale(1.0 / length);
        Vec3 u = dir.cross(new Vec3(0, 1, 0));
        if (u.lengthSqr() < 1.0E-6) {
            u = new Vec3(1, 0, 0);
        }
        u = u.normalize();
        Vec3 v = dir.cross(u).normalize();
        int segments = 18;
        for (int strand = 0; strand < 3; strand++) {
            double phase = strand * Mth.TWO_PI / 3.0;
            Vec3 prev = null;
            for (int i = 0; i <= segments; i++) {
                double s = i / (double) segments;
                // A swelling, twisting path: widest in the middle, closing on both ends.
                double swell = Math.sin(Math.PI * s) * 0.35;
                double twist = phase + s * 5.0 - time * 0.45;
                Vec3 p = from.add(axis.scale(s)).add(u.scale(Math.cos(twist) * swell)).add(v.scale(Math.sin(twist) * swell)).subtract(camera);
                if (prev != null) {
                    // Beads of brightness flowing toward her.
                    float bead = 0.5F + 0.5F * Mth.sin((float) (s * 14.0 - time * 0.9F));
                    int colour = EnergyGeometry.argb(bead > 0.8F ? c(AgathaAbilities.LILAC) : c(AgathaAbilities.VIOLET), (0.35F + 0.5F * bead) * a);
                    EnergyGeometry.ribbon(pose, vc, prev, p, 0.07F, colour, colour);
                }
                prev = p;
            }
        }
    }

    /** Between her two hands (where they are drawn, or a little before her chest). */
    private static Vec3 hands(Entity agatha, float partial) {
        if (agatha instanceof Player player) {
            Vec3 right = HandTracker.hand(player, HumanoidArm.RIGHT, partial);
            Vec3 left = HandTracker.hand(player, HumanoidArm.LEFT, partial);
            if (right != null && left != null) {
                return right.add(left).scale(0.5);
            }
        }
        Vec3 look = agatha.getViewVector(partial);
        return agatha.getPosition(partial).add(0, agatha.getBbHeight() * 0.62, 0).add(look.scale(0.4));
    }

    /** The shield's shards: flung off the sphere, spinning, fading. */
    private static void shards(Matrix4f pose, VertexConsumer vc, Shatter s, Vec3 camera, float time) {
        float age = time - s.start();
        if (age < 0.0F || age > SHARD_TICKS) {
            return;
        }
        float a = 1.0F - age / SHARD_TICKS;
        RandomSource r = RandomSource.create(s.seed());
        for (int i = 0; i < SHARDS; i++) {
            Vec3 out = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize();
            double speed = 0.12 + r.nextDouble() * 0.10;
            Vec3 at = s.center().add(out.scale(s.radius() + speed * age)).add(0, -0.004 * age * age, 0).subtract(camera);
            double spin = r.nextDouble() * Mth.TWO_PI + age * (0.3 + r.nextDouble() * 0.3);
            Vec3 edge = out.cross(new Vec3(Math.cos(spin), Math.sin(spin), 0.3)).normalize().scale(0.14 + r.nextDouble() * 0.1);
            int colour = EnergyGeometry.argb(i % 3 == 0 ? c(AgathaAbilities.LILAC) : c(AgathaAbilities.VIOLET),
                    0.9F * a);
            EnergyGeometry.ribbon(pose, vc, at.subtract(edge), at.add(edge), 0.05F, colour, colour);
        }
    }

    private static void vertex(Matrix4f pose, VertexConsumer vc, Vec3 p, int argb) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(argb);
    }

    private static void texVertex(Matrix4f pose, VertexConsumer vc, Vec3 p, float u, float v, int argb) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setUv(u, v).setColor(argb);
    }
}
