package com.kolodkingal.enhanced.compat;
//#norenames 1.21.11

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 1.21.11 and 26.3: vanilla's {@code DimensionSpecialEffects} is gone (1.21.11 describes a dimension's sky, fog and clouds
 * with environment attributes in its dimension type). The mod's skies keep their 1.21.1 shape: this base has the
 * methods they override, and {@link #skybox()}, {@link #clouds()} and {@link #weather()} are NeoForge's custom renderers
 * that call them (registered under the dimension type's id, which the dimension type names as its
 * {@code neoforge:custom_skybox}, {@code neoforge:custom_clouds} and {@code neoforge:custom_weather_effects}).
 * The fog's colour is the dimension type's, then {@link #getBrightnessDependentFogColor} (from NeoForge's fog colour
 * event, {@link #install}).
 */
public abstract class DimensionSpecialEffects {
    public enum SkyType { NONE, NORMAL, END }

    protected DimensionSpecialEffects(float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightmap,
                                      boolean constantAmbientLight) {
    }

    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return false;
    }

    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX,
                                     double camY, double camZ) {
        return false;
    }

    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        return false;
    }

    /** 1.21.1's {@code level.getTimeOfDay(partialTick)}: the fraction of the day (0 at noon, 0.5 at midnight). */
    public static float timeOfDay(net.minecraft.world.level.Level level) {
        double d0 = net.minecraft.util.Mth.frac(level.getDayTime() / 24000.0 - 0.25);
        double d1 = 0.5 - Math.cos(d0 * Math.PI) / 2.0;
        return (float) (d0 * 2.0 + d1) / 3.0F;
    }

    private static final java.util.Map<net.minecraft.resources.Identifier, DimensionSpecialEffects> BY_TYPE = new java.util.HashMap<>();
    private static boolean listening;

    /** Gives {@code effects} the fog colour of the levels of this dimension type (1.21.1's brightness-dependent fog). */
    public static synchronized void install(net.minecraft.resources.Identifier dimensionType, DimensionSpecialEffects effects) {
        BY_TYPE.put(dimensionType, effects);
        if (!listening) {
            listening = true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                    net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor.class, DimensionSpecialEffects::fog);
        }
    }

    private static void fog(net.neoforged.neoforge.client.event.ViewportEvent.ComputeFogColor event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        DimensionSpecialEffects effects = level.dimensionTypeRegistration().unwrapKey()
                .map(k -> BY_TYPE.get(k.identifier())).orElse(null);
        if (effects == null) {
            return;
        }
        float brightness = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.cos(timeOfDay(level) * ((float) Math.PI * 2.0F)) * 2.0F + 0.5F, 0.0F, 1.0F);
        Vec3 out = effects.getBrightnessDependentFogColor(new Vec3(event.getRed(), event.getGreen(), event.getBlue()), brightness);
        event.setRed((float) out.x);
        event.setGreen((float) out.y);
        event.setBlue((float) out.z);
    }

    private static float partialTick() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public net.neoforged.neoforge.client.CustomSkyboxRenderer skybox() {
        return new net.neoforged.neoforge.client.CustomSkyboxRenderer() {
            @Override
            public boolean renderSky(net.minecraft.client.renderer.state.LevelRenderState state,
                                     net.minecraft.client.renderer.state.SkyRenderState sky, org.joml.Matrix4fc modelView,
                                     com.mojang.blaze3d.buffers.GpuBufferSlice skyFog) {
                Minecraft mc = Minecraft.getInstance();
                ClientLevel level = mc.level;
                if (level == null) {
                    return false;
                }
                // (26.3: the sky is drawn at once, compat.Gl's sky drawing; the model-view stack is set aside)
                Matrix4f turn = new Matrix4f(modelView);
                org.joml.Matrix4fStack stack = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
                stack.pushMatrix();
                stack.identity();
                Gl.sky(true);
                try {
                    return DimensionSpecialEffects.this.renderSky(level, (int) level.getGameTime(), partialTick(), turn,
                            mc.gameRenderer.getMainCamera(), new Matrix4f(), false,
                            () -> com.mojang.blaze3d.systems.RenderSystem.setShaderFog(skyFog));
                } finally {
                    Gl.sky(false);
                    stack.popMatrix();
                }
            }
        };
    }

    /** (26.3: the clouds and the weather are drawn in the game's render pass: the mod's only hide them) */
    public net.neoforged.neoforge.client.CustomCloudsRenderer clouds() {
        return new net.neoforged.neoforge.client.CustomCloudsRenderer() {
            @Override
            public boolean renderClouds(net.minecraft.client.renderer.state.LevelRenderState state,
                                        net.minecraft.client.CloudStatus status, org.joml.Matrix4fc modelView,
                                        com.mojang.blaze3d.systems.RenderPass pass) {
                ClientLevel level = Minecraft.getInstance().level;
                Vec3 cam = state.cameraRenderState.pos;
                return level != null && DimensionSpecialEffects.this.renderClouds(level, (int) level.getGameTime(), partialTick(),
                        new com.mojang.blaze3d.vertex.PoseStack(), cam.x, cam.y, cam.z, new Matrix4f(modelView), new Matrix4f());
            }
        };
    }

    public net.neoforged.neoforge.client.CustomWeatherEffectRenderer weather() {
        return new net.neoforged.neoforge.client.CustomWeatherEffectRenderer() {
            @Override
            public boolean renderSnowAndRain(net.minecraft.client.renderer.state.LevelRenderState state,
                                             net.minecraft.client.renderer.state.WeatherRenderState weather, Vec3 cam,
                                             com.mojang.blaze3d.systems.RenderPass pass) {
                ClientLevel level = Minecraft.getInstance().level;
                return level != null && DimensionSpecialEffects.this.renderSnowAndRain(level, (int) level.getGameTime(),
                        partialTick(), null, cam.x, cam.y, cam.z);
            }
        };
    }
}
