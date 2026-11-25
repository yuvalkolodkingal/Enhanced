package com.kolodkingal.enhanced.client.nexus;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * (3.19.2) The sky of the Nexus between the universes, the dimension effects {@code enhanced:nexus}: deep space all round,
 * black overhead, dark purple at the horizon and cosmic blue below; eight far nebulae in purple, blue and magenta that turn
 * very slowly, and three fields of stars that twinkle each in its own time. No sun, moon, clouds or rain. Cost per frame:
 * three buffer draws and a dozen quads.
 */
public class NexusSkyEffects extends DimensionSpecialEffects {
    private static final ResourceLocation[] NEBULA = new ResourceLocation[6];
    /** Where the nebulae hang: yaw and pitch (degrees, some below the horizon), size; and their tints (RGB 0-1). */
    private static final float[][] NEBULA_AT = {{15, 40, 120}, {100, 12, 140}, {170, 60, 100}, {240, -15, 150}, {310, 25, 110},
            {60, -45, 130}, {200, -55, 120}, {280, 70, 90}};
    private static final float[][] TINT = {{0.78F, 0.38F, 1.0F}, {0.32F, 0.48F, 1.0F}, {1.0F, 0.32F, 0.86F}, {0.45F, 0.3F, 1.0F},
            {0.25F, 0.65F, 1.0F}, {0.9F, 0.4F, 1.0F}, {0.35F, 0.35F, 0.95F}, {1.0F, 0.45F, 0.75F}};
    public static final int TOP = 0x05030F;
    public static final int HORIZON = 0x1C0B3A;
    public static final int BELOW = 0x040A26;

    static {
        for (int i = 0; i < NEBULA.length; i++) {
            NEBULA[i] = Enhanced.id("textures/environment/asgard_nebula_" + i + ".png");
        }
    }

    private VertexBuffer starsA;
    private VertexBuffer starsB;
    private VertexBuffer bright;

    public NexusSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.07, 0.04, 0.14);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ,
                                Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY,
                                     double camZ) {
        return true;
    }

    // A NeoForge hook, not a vanilla method: no @Override (the port compiles this code against vanilla too).
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        if (starsA == null) {
            starsA = buildStars(0x4E455855L, 1400, 0.10F, 0.22F);
            starsB = buildStars(0x53544152L, 1400, 0.10F, 0.22F);
            bright = buildStars(0x42524954L, 160, 0.3F, 0.5F);
        }
        float t = ticks + partialTick;
        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);
        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        drawDome(pose.last().pose());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(t * 0.004F));
        for (int i = 0; i < NEBULA_AT.length; i++) {
            float[] at = NEBULA_AT[i];
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(at[0]));
            pose.mulPose(Axis.XP.rotationDegrees(at[1]));
            pose.mulPose(Axis.YP.rotationDegrees(t * 0.01F * (i % 2 == 0 ? 1 : -1)));
            float[] c = TINT[i];
            texturedQuad(pose.last().pose(), NEBULA[i % NEBULA.length], 100.0F, at[2], c[0], c[1], c[2], 0.9F);
            pose.popPose();
        }
        RenderSystem.setShader(GameRenderer::getPositionShader);
        // two fields of small stars, each fading in and out in its own time, and a few bright ones that flicker faster
        drawStars(starsA, pose, projectionMatrix, 0.9F, 0.88F, 1.0F, 0.55F + 0.45F * Mth.sin(t * 0.045F));
        drawStars(starsB, pose, projectionMatrix, 0.8F, 0.85F, 1.0F, 0.55F + 0.45F * Mth.sin(t * 0.045F + Mth.PI));
        drawStars(bright, pose, projectionMatrix, 1.0F, 0.92F, 1.0F, 0.7F + 0.3F * Mth.sin(t * 0.21F));
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        setupFog.run();
        return true;
    }

    private static void drawStars(VertexBuffer stars, PoseStack pose, Matrix4f projection, float r, float g, float b, float alpha) {
        RenderSystem.setShaderColor(r, g, b, alpha);
        stars.bind();
        stars.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
    }

    /** The dome: black overhead, dark purple at the horizon, cosmic blue below. */
    private static void drawDome(Matrix4f m) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float r = 100.0F;
        vertex(b, m, -r, r, -r, TOP);
        vertex(b, m, -r, r, r, TOP);
        vertex(b, m, r, r, r, TOP);
        vertex(b, m, r, r, -r, TOP);
        vertex(b, m, -r, -r, -r, BELOW);
        vertex(b, m, r, -r, -r, BELOW);
        vertex(b, m, r, -r, r, BELOW);
        vertex(b, m, -r, -r, r, BELOW);
        float[][] sides = {{-r, -r, r, -r}, {r, -r, r, r}, {r, r, -r, r}, {-r, r, -r, -r}};
        for (float[] s : sides) {
            vertex(b, m, s[0], 0, s[1], HORIZON);
            vertex(b, m, s[2], 0, s[3], HORIZON);
            vertex(b, m, s[2], r, s[3], TOP);
            vertex(b, m, s[0], r, s[1], TOP);
            vertex(b, m, s[0], -r, s[1], BELOW);
            vertex(b, m, s[2], -r, s[3], BELOW);
            vertex(b, m, s[2], 0, s[3], HORIZON);
            vertex(b, m, s[0], 0, s[1], HORIZON);
        }
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float x, float y, float z, int rgb) {
        b.addVertex(m, x, y, z).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255);
    }

    /** A square picture at {@code distance} straight up (+y) in the pose's frame, tinted. */
    private static void texturedQuad(Matrix4f m, ResourceLocation texture, float distance, float size, float r, float g, float bl,
                                     float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(r, g, bl, alpha);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float s = size / 2.0F;
        b.addVertex(m, -s, distance, -s).setUv(0.0F, 0.0F);
        b.addVertex(m, s, distance, -s).setUv(1.0F, 0.0F);
        b.addVertex(m, s, distance, s).setUv(1.0F, 1.0F);
        b.addVertex(m, -s, distance, s).setUv(0.0F, 1.0F);
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** {@code count} stars on the whole sphere: little squares facing the centre. */
    private static VertexBuffer buildStars(long seed, int count, float minSize, float extra) {
        RandomSource random = RandomSource.create(seed);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (int i = 0; i < count; i++) {
            Vector3f dir = new Vector3f((float) random.nextGaussian(), (float) random.nextGaussian(), (float) random.nextGaussian());
            if (dir.lengthSquared() < 1.0E-4F) {
                continue;
            }
            dir.normalize();
            float size = minSize + random.nextFloat() * extra;
            Vector3f centre = new Vector3f(dir).mul(100.0F);
            Vector3f up = Math.abs(dir.y) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
            Vector3f u = new Vector3f(dir).cross(up).normalize().mul(size);
            Vector3f v = new Vector3f(dir).cross(u).normalize().mul(size);
            b.addVertex(centre.x - u.x - v.x, centre.y - u.y - v.y, centre.z - u.z - v.z);
            b.addVertex(centre.x + u.x - v.x, centre.y + u.y - v.y, centre.z + u.z - v.z);
            b.addVertex(centre.x + u.x + v.x, centre.y + u.y + v.y, centre.z + u.z + v.z);
            b.addVertex(centre.x - u.x + v.x, centre.y - u.y + v.y, centre.z - u.z + v.z);
        }
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(b.buildOrThrow());
        VertexBuffer.unbind();
        return buffer;
    }
}
