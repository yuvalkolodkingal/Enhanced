package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.UltimateWanda;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * (3.19) The runes of Ultimate Wanda's Chaos Shield (the sphere itself, bigger than the Scarlet Witch's, is the world
 * renderer's): three rings of her glyphs orbiting the sphere on tilted paths, each glyph breathing in its own time, and her
 * great sigil turning flat under her feet. Drawn while the shield is up, and while it guards her body as her mind is in
 * another one. A few dozen quads per Wanda.
 */
@Subscriber(side = Side.CLIENT)
public final class UltimateShieldFx {
    private static final ResourceLocation GLYPHS = Enhanced.id("textures/fx/wanda_rune_glyphs.png");
    private static final ResourceLocation CIRCLE = Enhanced.id("textures/fx/wanda_rune_circle.png");
    private static final int GLYPH_COUNT = 8;
    private static final int LIGHT = 0xFF5070;
    private static final double MAX_DISTANCE = 96.0;
    private static final Matrix4f POSE = new Matrix4f();

    private UltimateShieldFx() {
    }

    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = EnhancedClientConfig.reducedMotion() ? 0.0F : level.getGameTime() + partial;
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        Vector3f right = new Vector3f(camera.getLeftVector()).negate();
        Vector3f up = new Vector3f(camera.getUpVector());
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType glyphType = EnhancedRenderTypes.oneSidedGlow(GLYPHS);
        RenderType circleType = EnhancedRenderTypes.oneSidedGlow(CIRCLE);
        VertexConsumer glyphs = null;
        boolean anyCircle = false;
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (!UltimateWanda.is(data) || player.isInvisible() || player.isSpectator()
                    || !(data.chaosShieldActive() || UltimateWanda.guarded(data))) {
                continue;
            }
            Vec3 centre = player.getPosition(partial).add(0, player.getBbHeight() * 0.5, 0);
            if (centre.distanceToSqr(eye) > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }
            ShieldStyle style = ShieldStyles.of(player, data);
            if (style == null) {
                continue;
            }
            ShieldStyle.Look look = style.look(data, time);
            float radius = look.radius();
            float shown = look.flicker();
            Vec3 c = centre.subtract(eye);
            if (glyphs == null) {
                glyphs = buffers.getBuffer(glyphType);
            }
            ring(glyphs, c, radius * 1.03F, 0.45F, 0.0F, 12, time * 0.035F, radius * 0.22F, time, shown, 0, right, up);
            ring(glyphs, c, radius * 1.03F, -0.6F, 1.2F, 10, -time * 0.045F, radius * 0.2F, time, shown, 3, right, up);
            ring(glyphs, c, radius * 1.05F, 0.1F, 2.4F, 14, time * 0.025F, radius * 0.18F, time, shown, 5, right, up);
            anyCircle = true;
        }
        if (glyphs != null) {
            buffers.endBatch(glyphType);
        }
        if (!anyCircle) {
            return;
        }
        VertexConsumer circle = buffers.getBuffer(circleType);
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (!UltimateWanda.is(data) || player.isInvisible() || player.isSpectator()
                    || !(data.chaosShieldActive() || UltimateWanda.guarded(data))) {
                continue;
            }
            ShieldStyle style = ShieldStyles.of(player, data);
            if (style == null) {
                continue;
            }
            ShieldStyle.Look look = style.look(data, time);
            Vec3 feet = player.getPosition(partial).subtract(eye).add(0, 0.05, 0);
            if (feet.lengthSqr() > MAX_DISTANCE * MAX_DISTANCE) {
                continue;
            }
            float breathe = 0.8F + 0.2F * Mth.sin(time * 0.08F);
            disc(circle, feet, look.radius() * 2.1F, time * 0.02F, argb(LIGHT, look.flicker() * breathe));
            disc(circle, feet.add(0, 0.02, 0), look.radius() * 1.2F, -time * 0.04F, argb(LIGHT, look.flicker() * 0.7F * breathe));
        }
        buffers.endBatch(circleType);
    }

    private static int argb(int rgb, float a) {
        return ((int) (Mth.clamp(a, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }

    /**
     * One ring of glyphs round the sphere: its plane tilted by {@code tilt} about the x axis and turned by {@code yaw}, the
     * glyphs carried round by {@code turn}; each faces the camera.
     */
    private static void ring(VertexConsumer vc, Vec3 c, float radius, float tilt, float yaw, int count, float turn, float size,
                             float time, float shown, int seed, Vector3f right, Vector3f up) {
        float ct = Mth.cos(tilt);
        float st = Mth.sin(tilt);
        float cy = Mth.cos(yaw);
        float sy = Mth.sin(yaw);
        for (int i = 0; i < count; i++) {
            float a = turn + i * Mth.TWO_PI / count;
            // a point on the flat ring, then tilted, then turned
            float x = Mth.cos(a) * radius;
            float z = Mth.sin(a) * radius;
            float y = z * st;
            z = z * ct;
            float px = x * cy - z * sy;
            float pz = x * sy + z * cy;
            float pulse = 0.55F + 0.45F * Mth.sin(time * 0.12F + i * 1.9F + seed);
            float half = size * (0.75F + 0.3F * pulse) * 0.5F;
            int glyph = (i * 3 + seed) % GLYPH_COUNT;
            float u0 = glyph / (float) GLYPH_COUNT;
            float u1 = (glyph + 1) / (float) GLYPH_COUNT;
            billboard(vc, (float) c.x + px, (float) c.y + y, (float) c.z + pz, half, u0, u1, right, up,
                    argb(LIGHT, shown * (0.5F + 0.5F * pulse)));
        }
    }

    private static void billboard(VertexConsumer vc, float x, float y, float z, float half, float u0, float u1, Vector3f right,
                                  Vector3f up, int argb) {
        float rx = right.x() * half;
        float ry = right.y() * half;
        float rz = right.z() * half;
        float ux = up.x() * half;
        float uy = up.y() * half;
        float uz = up.z() * half;
        float[][] p = {{x - rx - ux, y - ry - uy, z - rz - uz}, {x + rx - ux, y + ry - uy, z + rz - uz},
                {x + rx + ux, y + ry + uy, z + rz + uz}, {x - rx + ux, y - ry + uy, z - rz + uz}};
        float[][] uv = {{u0, 1}, {u1, 1}, {u1, 0}, {u0, 0}};
        for (int j = 0; j < 4; j++) {
            vc.addVertex(POSE, p[j][0], p[j][1], p[j][2]).setUv(uv[j][0], uv[j][1]).setColor(argb);
        }
        for (int j = 3; j >= 0; j--) {
            vc.addVertex(POSE, p[j][0], p[j][1], p[j][2]).setUv(uv[j][0], uv[j][1]).setColor(argb);
        }
    }

    /** Her sigil lying flat at {@code at}, {@code size} across, turned by {@code angle}; seen from above and below. */
    private static void disc(VertexConsumer vc, Vec3 at, float size, float angle, int argb) {
        float r = size * 0.5F;
        float cos = Mth.cos(angle) * r;
        float sin = Mth.sin(angle) * r;
        float x = (float) at.x;
        float y = (float) at.y;
        float z = (float) at.z;
        float[][] corners = {{-cos + sin, -sin - cos}, {cos + sin, sin - cos}, {cos - sin, sin + cos}, {-cos - sin, -sin + cos}};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int i = 0; i < 4; i++) {
            vc.addVertex(POSE, x + corners[i][0], y, z + corners[i][1]).setUv(uv[i][0], uv[i][1]).setColor(argb);
        }
        for (int i = 3; i >= 0; i--) {
            vc.addVertex(POSE, x + corners[i][0], y, z + corners[i][1]).setUv(uv[i][0], uv[i][1]).setColor(argb);
        }
    }
}
