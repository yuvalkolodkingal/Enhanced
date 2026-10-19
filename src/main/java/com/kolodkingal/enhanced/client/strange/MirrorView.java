package com.kolodkingal.enhanced.client.strange;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.EnergyGeometry;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.strange.StrangeAbilities;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * The Mirror Dimension as the clients see it (3.6): told by its payloads (the caster, the centre, the radius), every
 * client draws its sphere as a shimmering kaleidoscope shell and hears its shimmer (a looping {@code strange_mirror_loop}
 * at the centre while it stands); a client whose camera is inside sees the world bend: the fog's colour turns through
 * the mirror's hues, the camera sways in a slow roll (two degrees at most) and faint fractal mandalas turn at the edges of
 * the screen. No shader and no copy of the world: the camera and an overlay. The client option {@code reducedMotion}
 * stills the sway and the turning.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class MirrorView {
    private static final ResourceLocation FRACTAL = Enhanced.id("textures/fx/mandala_ring.png");
    /** The longest a sphere is believed without its closing payload (ticks): its time and a margin. */
    private static final int LONGEST = 20 * 60;
    /** The hues the fog turns through inside: orange, violet, teal. */
    private static final float[][] HUES = {{0.95F, 0.6F, 0.2F}, {0.55F, 0.35F, 0.9F}, {0.3F, 0.8F, 0.85F}};

    /** A sphere: where, how far, since when (this client's clock), and the loop that sounds in it. */
    private record Sphere(Vec3 center, float radius, long since, Loop loop) {
    }

    private static final Map<Integer, Sphere> SPHERES = new HashMap<>();
    /** How deep the local camera is in a sphere (0 outside, 1 well inside), eased each tick. */
    private static float depth;
    private static float depthO;

    /** A sphere opened (its caster, centre and radius). */
    public static void open(int caster, Vec3 center, float radius, long now) {
        close(caster);
        Loop loop = new Loop(center);
        SPHERES.put(caster, new Sphere(center, radius, now, loop));
        Minecraft.getInstance().getSoundManager().play(loop);
    }

    /** A sphere closed. */
    public static void close(int caster) {
        Sphere sphere = SPHERES.remove(caster);
        if (sphere != null) {
            sphere.loop().finish();
        }
    }

    /** How much the local camera is inside a sphere now (0-1), with the partial tick. */
    public static float depth(float partial) {
        return Mth.lerp(partial, depthO, depth);
    }

    /** Whether a sphere stands at all (a test of the client may ask). */
    public static boolean any() {
        return !SPHERES.isEmpty();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        depthO = depth;
        if (level == null || mc.player == null) {
            for (Sphere s : SPHERES.values()) {
                s.loop().finish();
            }
            SPHERES.clear();
            depth = 0.0F;
            return;
        }
        long now = level.getGameTime();
        SPHERES.values().removeIf(s -> {
            boolean old = now - s.since() > LONGEST || now < s.since() - 20;
            if (old) {
                s.loop().finish();
            }
            return old;
        });
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        float target = 0.0F;
        for (Sphere s : SPHERES.values()) {
            double d = eye.distanceTo(s.center());
            target = Math.max(target, (float) Mth.clamp((s.radius() - d) / 2.0, 0.0, 1.0));
        }
        depth += (target - depth) * 0.15F;
    }

    /** The slow sway of the camera inside. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float d = depth((float) event.getPartialTick());
        if (d <= 0.001F || EnhancedClientConfig.reducedMotion()) {
            return;
        }
        float time = time((float) event.getPartialTick());
        event.setRoll(event.getRoll() + 2.0F * d * Mth.sin(time * 0.045F));
    }

    /** The fog turns through the mirror's hues inside. */
    @SubscribeEvent
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        float d = depth((float) event.getPartialTick());
        if (d <= 0.001F) {
            return;
        }
        float time = time((float) event.getPartialTick());
        float phase = (time * 0.01F) % HUES.length;
        int a = (int) phase;
        float f = phase - a;
        float[] h0 = HUES[a % HUES.length];
        float[] h1 = HUES[(a + 1) % HUES.length];
        float k = 0.45F * d;
        event.setRed(Mth.lerp(k, event.getRed(), Mth.lerp(f, h0[0], h1[0])));
        event.setGreen(Mth.lerp(k, event.getGreen(), Mth.lerp(f, h0[1], h1[1])));
        event.setBlue(Mth.lerp(k, event.getBlue(), Mth.lerp(f, h0[2], h1[2])));
    }

    private static float time(float partial) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0.0F : mc.level.getGameTime() + partial;
    }

    /** The sphere's shell: a faint kaleidoscope of the mirror's hues, brighter at its rim. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || SPHERES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = time(partial);
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = EnhancedRenderTypes.energyField(FRACTAL);
        VertexConsumer vc = buffers.getBuffer(type);
        for (Sphere s : SPHERES.values()) {
            float grow = Mth.clamp((time - s.since()) / 10.0F, 0.0F, 1.0F);
            float scroll = EnhancedClientConfig.reducedMotion() ? 0.0F : time * 0.004F;
            int hue = hue(time);
            EnergyGeometry.sphere(pose, vc, s.center().subtract(camera), s.radius() * grow, 32, 16, 8.0F, 4.0F, scroll,
                    hue, 0.02F, 0.35F);
        }
        buffers.endBatch(type);
    }

    /** The hue the shell takes at {@code time}: the mirror's colours, turning. */
    private static int hue(float time) {
        float phase = (time * 0.01F) % HUES.length;
        int a = (int) phase;
        float f = phase - a;
        float[] h0 = HUES[a % HUES.length];
        float[] h1 = HUES[(a + 1) % HUES.length];
        int r = (int) (255 * Mth.lerp(f, h0[0], h1[0]));
        int g = (int) (255 * Mth.lerp(f, h0[1], h1[1]));
        int b = (int) (255 * Mth.lerp(f, h0[2], h1[2]));
        return (r << 16) | (g << 8) | b;
    }

    /** The fractal mandalas at the edges of the screen, inside. */
    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        float d = depth(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (d <= 0.01F || mc.options.hideGui) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        float time = time(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        boolean calm = EnhancedClientConfig.reducedMotion();
        int size = Math.max(w, h) / 3;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        int hue = hue(time);
        RenderSystem.setShaderColor(((hue >> 16) & 0xFF) / 255F, ((hue >> 8) & 0xFF) / 255F, (hue & 0xFF) / 255F,
                0.28F * d);
        int[][] corners = {{0, 0}, {w, 0}, {0, h}, {w, h}};
        for (int i = 0; i < corners.length; i++) {
            for (int layer = 0; layer < 2; layer++) {
                int s = layer == 0 ? size : size * 2 / 3;
                g.pose().pushPose();
                g.pose().translate(corners[i][0], corners[i][1], 0);
                if (!calm) {
                    g.pose().mulPose(Axis.ZP.rotationDegrees((layer == 0 ? 1 : -1.6F) * time * 0.6F + i * 45));
                }
                g.blit(FRACTAL, -s / 2, -s / 2, s, s, 0, 0, 128, 128, 128, 128);
                g.pose().popPose();
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** The sphere's shimmer: looping at its centre while it stands. */
    static final class Loop extends AbstractTickableSoundInstance {
        Loop(Vec3 at) {
            super(ModSounds.STRANGE_MIRROR_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.7F;
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
            this.attenuation = Attenuation.LINEAR;
        }

        void finish() {
            stop();
        }

        @Override
        public void tick() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                stop();
            }
        }
    }

    private MirrorView() {
    }
}
