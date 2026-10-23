package com.kolodkingal.enhanced.client.asgard;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardSky;
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
 * The sky of Asgard (3.5), the dimension effects {@code enhanced:asgard}: a dome blue and gold by day, deep space at
 * night, and space below the horizon too (the void under the floating land); 1,500 stars all round (a static buffer built
 * once), six great clouds of nebula (faint by day, full at night), a ringed planet and two moons that cross the sky with
 * the time of day. No clouds, no rain, no sunrise colours. The colours come from {@link AsgardSky} (pure, tested). Cost
 * per frame: one buffer draw and a dozen quads.
 */
public class AsgardSkyEffects extends DimensionSpecialEffects {
    private static final ResourceLocation[] NEBULA = new ResourceLocation[6];
    private static final ResourceLocation PLANET = Enhanced.id("textures/environment/asgard_planet.png");
    private static final ResourceLocation RING = Enhanced.id("textures/environment/asgard_ring.png");
    private static final ResourceLocation MOON_1 = Enhanced.id("textures/environment/asgard_moon_1.png");
    private static final ResourceLocation MOON_2 = Enhanced.id("textures/environment/asgard_moon_2.png");
    /** Where the nebulae hang: yaw and pitch (degrees, some below the horizon), size. */
    private static final float[][] NEBULA_AT = {{20, 35, 90}, {130, 10, 110}, {230, 55, 80}, {300, -20, 120}, {80, -50, 100}, {190, -35, 90}};

    static {
        for (int i = 0; i < NEBULA.length; i++) {
            NEBULA[i] = Enhanced.id("textures/environment/asgard_nebula_" + i + ".png");
        }
    }

    private VertexBuffer stars;

    public AsgardSkyEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        Vec3 night = new Vec3(0.05, 0.07, 0.18);
        return night.lerp(fogColor, Mth.clamp(brightness, 0.0F, 1.0F));
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
        if (stars == null) {
            stars = buildStars();
        }
        float time = level.getTimeOfDay(partialTick);
        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);
        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        drawDome(pose.last().pose(), time);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        // the nebulae and the stars turn slowly with the day
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-30.0F));
        pose.mulPose(Axis.XP.rotationDegrees(time * 360.0F * 0.25F));
        float nebula = AsgardSky.nebulaAlpha(time);
        for (int i = 0; i < NEBULA.length; i++) {
            float[] at = NEBULA_AT[i];
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(at[0]));
            pose.mulPose(Axis.XP.rotationDegrees(at[1]));
            texturedQuad(pose.last().pose(), NEBULA[i], 100.0F, at[2], nebula * 0.8F);
            pose.popPose();
        }
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1.0F, 0.97F, 0.9F, AsgardSky.starAlpha(time));
        stars.bind();
        stars.drawWithShader(pose.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();

        // the ringed planet and the two moons cross the sky with the time of day
        RenderSystem.defaultBlendFunc();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(time * 360.0F + 40.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(25.0F));
        texturedQuad(pose.last().pose(), PLANET, 100.0F, 26.0F, 1.0F);
        pose.mulPose(Axis.XP.rotationDegrees(68.0F));
        texturedQuad(pose.last().pose(), RING, 99.0F, 60.0F, 0.9F);
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-70.0F));
        pose.mulPose(Axis.XP.rotationDegrees(time * 360.0F + 200.0F));
        texturedQuad(pose.last().pose(), MOON_1, 100.0F, 9.0F, 1.0F);
        pose.mulPose(Axis.XP.rotationDegrees(22.0F));
        pose.mulPose(Axis.YP.rotationDegrees(14.0F));
        texturedQuad(pose.last().pose(), MOON_2, 100.0F, 5.0F, 1.0F);
        pose.popPose();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        setupFog.run();
        return true;
    }

    /** The dome: overhead colour, a band down to the horizon colour, and deep space below. */
    private static void drawDome(Matrix4f m, float time) {
        int top = AsgardSky.dayColour(time);
        int horizon = AsgardSky.horizonColour(time);
        int below = AsgardSky.lerp(0x0b1030, AsgardSky.NIGHT, AsgardSky.nightness(time));
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float r = 100.0F;
        vertex(b, m, -r, r, -r, top);
        vertex(b, m, -r, r, r, top);
        vertex(b, m, r, r, r, top);
        vertex(b, m, r, r, -r, top);
        vertex(b, m, -r, -r, -r, below);
        vertex(b, m, r, -r, -r, below);
        vertex(b, m, r, -r, r, below);
        vertex(b, m, -r, -r, r, below);
        float[][] sides = {{-r, -r, r, -r}, {r, -r, r, r}, {r, r, -r, r}, {-r, r, -r, -r}};
        for (float[] s : sides) {
            vertex(b, m, s[0], 0, s[1], horizon);
            vertex(b, m, s[2], 0, s[3], horizon);
            vertex(b, m, s[2], r, s[3], top);
            vertex(b, m, s[0], r, s[1], top);
            vertex(b, m, s[0], -r, s[1], below);
            vertex(b, m, s[2], -r, s[3], below);
            vertex(b, m, s[2], 0, s[3], horizon);
            vertex(b, m, s[0], 0, s[1], horizon);
        }
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float x, float y, float z, int rgb) {
        b.addVertex(m, x, y, z).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255);
    }

    /** A square picture at {@code distance} straight up (+y) in the pose's frame. */
    private static void texturedQuad(Matrix4f m, ResourceLocation texture, float distance, float size, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
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

    /** 1,500 stars on the whole sphere (below the horizon too): little squares facing the centre. */
    private static VertexBuffer buildStars() {
        RandomSource random = RandomSource.create(0x4153474152L);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (int i = 0; i < 1500; i++) {
            Vector3f dir = new Vector3f((float) random.nextGaussian(), (float) random.nextGaussian(), (float) random.nextGaussian());
            if (dir.lengthSquared() < 1.0E-4F) {
                continue;
            }
            dir.normalize();
            float size = 0.12F + random.nextFloat() * 0.25F;
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
