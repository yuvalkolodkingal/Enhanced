package com.kolodkingal.enhanced.client.eternity;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The sky of Eternity's realm (3.8, PLAN 10.4), the dimension effects {@code enhanced:eternity}: a white dome paling to
 * a soft grey-white at the horizon, with bands of soft grey-white cloud round the horizon and a thin veil of cloud
 * overhead, all drawn twice: above, and mirrored below the horizon (what the still water reflects, seen through the
 * translucent mirror). They drift very slowly. No sun, moon, stars or vanilla clouds, no rain. The mist on the horizon
 * is {@code EternityClient}'s fog. Cost per frame: a dozen quads and a 48-sided ring, twice.
 */
public class EternitySkyEffects extends DimensionSpecialEffects {
    /** A strip of soft clouds, thickest at its bottom edge (the horizon), tiling across. */
    private static final ResourceLocation BAND = Enhanced.id("textures/environment/eternity_clouds.png");
    /** A square veil of thin cloud for overhead. */
    private static final ResourceLocation VEIL = Enhanced.id("textures/environment/eternity_veil.png");
    public static final int TOP = 0xFFFFFF;
    public static final int HORIZON = 0xE9ECF1;
    private static final float R = 100.0F;
    private static final int SIDES = 48;

    public EternitySkyEffects() {
        super(Float.NaN, false, SkyType.NONE, true, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.95, 0.96, 0.97);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX,
                                double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);
        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        dome(pose.last().pose());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float drift = (ticks + partialTick) * 0.004F;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(drift));
        // The bands round the horizon (two layers turning at different speeds) and their reflections.
        band(pose.last().pose(), 46.0F, 0.0F, 0.85F);
        band(pose.last().pose(), -40.0F, 0.0F, 0.55F);
        pose.mulPose(Axis.YP.rotationDegrees(drift * 0.6F + 70.0F));
        band(pose.last().pose(), 30.0F, 0.37F, 0.6F);
        band(pose.last().pose(), -26.0F, 0.37F, 0.4F);
        pose.popPose();
        // The veil overhead, and its reflection.
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-drift * 0.8F));
        veil(pose.last().pose(), R * 0.9F, 0.55F);
        veil(pose.last().pose(), -R * 0.9F, 0.35F);
        pose.popPose();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        setupFog.run();
        return true;
    }

    /** The dome: white overhead, the pale grey-white of the horizon, white again below (the reflection). */
    private static void dome(Matrix4f m) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        quad(b, m, R, TOP);
        quad(b, m, -R, TOP);
        float[][] sides = {{-R, -R, R, -R}, {R, -R, R, R}, {R, R, -R, R}, {-R, R, -R, -R}};
        for (float[] s : sides) {
            vertex(b, m, s[0], 0, s[1], HORIZON);
            vertex(b, m, s[2], 0, s[3], HORIZON);
            vertex(b, m, s[2], R * 0.5F, s[3], TOP);
            vertex(b, m, s[0], R * 0.5F, s[1], TOP);
            vertex(b, m, s[0], R * 0.5F, s[1], TOP);
            vertex(b, m, s[2], R * 0.5F, s[3], TOP);
            vertex(b, m, s[2], R, s[3], TOP);
            vertex(b, m, s[0], R, s[1], TOP);
            vertex(b, m, s[0], -R, s[1], TOP);
            vertex(b, m, s[2], -R, s[3], TOP);
            vertex(b, m, s[2], 0, s[3], HORIZON);
            vertex(b, m, s[0], 0, s[1], HORIZON);
        }
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
    }

    private static void quad(BufferBuilder b, Matrix4f m, float y, int rgb) {
        vertex(b, m, -R, y, -R, rgb);
        vertex(b, m, -R, y, R, rgb);
        vertex(b, m, R, y, R, rgb);
        vertex(b, m, R, y, -R, rgb);
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float x, float y, float z, int rgb) {
        b.addVertex(m, x, y, z).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255);
    }

    /**
     * A ring of cloud from the horizon up to {@code height} (down, when negative: the reflection, the strip's thick
     * edge still at the horizon), the strip shifted {@code shift} of its width.
     */
    private static void band(Matrix4f m, float height, float shift, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, BAND);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float radius = R * 0.95F;
        float repeats = 3.0F;
        for (int i = 0; i < SIDES; i++) {
            float a0 = Mth.TWO_PI * i / SIDES;
            float a1 = Mth.TWO_PI * (i + 1) / SIDES;
            float u0 = shift + repeats * i / SIDES;
            float u1 = shift + repeats * (i + 1) / SIDES;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            b.addVertex(m, x0, -2.0F * Math.signum(height), z0).setUv(u0, 1.0F);
            b.addVertex(m, x1, -2.0F * Math.signum(height), z1).setUv(u1, 1.0F);
            b.addVertex(m, x1, height, z1).setUv(u1, 0.0F);
            b.addVertex(m, x0, height, z0).setUv(u0, 0.0F);
        }
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The overhead veil at height {@code y} (negative: its reflection below). */
    private static void veil(Matrix4f m, float y, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, VEIL);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float s = R * 1.6F;
        b.addVertex(m, -s, y, -s).setUv(0.0F, 0.0F);
        b.addVertex(m, s, y, -s).setUv(1.0F, 0.0F);
        b.addVertex(m, s, y, s).setUv(1.0F, 1.0F);
        b.addVertex(m, -s, y, s).setUv(0.0F, 1.0F);
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
