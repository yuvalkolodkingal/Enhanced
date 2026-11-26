package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.network.UniverseTakeoverPayload;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderLevelStageEvent;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;

/**
 * The sky of a universe Ultimate Wanda has taken (3.19): it turns a deep red over two seconds for everyone in it, and back
 * when she lets it go. The sky dome takes the colour of the fog, made to cover it whole; the fog of the land turns red too.
 * From 1.21.6 the fog no longer covers the sky dome, so the red is also drawn: a see-through red vault round the camera,
 * past the last terrain (a dozen quads a frame). While her runes burn, real runes turn in it: her great sigil overhead, a
 * smaller one turning the other way inside it, and three rings of glyphs round the horizon, each rune breathing in its own
 * time. The server says when the universe the player stands in changes ({@link UniverseTakeoverPayload}).
 */
@Subscriber(side = Side.CLIENT)
public final class UniverseTakeoverClient {
    /** The red of the sky she brings, and the darker red when her runes burn in it. */
    private static final int SKY = 0xB0102A;
    private static final int RUNE_SKY = 0x7A0618;
    /** How long the sky takes to turn (ms). */
    private static final long TURN_MS = 2000L;

    private static int state = UniverseTakeoverPayload.CLEAR;
    /** The state before the last change, and when it changed (ms), for the turn of the sky. */
    private static int before = UniverseTakeoverPayload.CLEAR;
    private static long changedAt;

    private static final ResourceLocation CIRCLE = Enhanced.id("textures/fx/wanda_rune_circle.png");
    private static final ResourceLocation GLYPHS = Enhanced.id("textures/fx/wanda_rune_glyphs.png");
    private static final int GLYPH_COUNT = 8;
    private static final int RUNE_LIGHT = 0xFF5070;
    private static final Matrix4f POSE = new Matrix4f();

    private UniverseTakeoverClient() {
    }

    /** Whether the runes burn in this universe's sky now (or are still fading after they went out). */
    private static float runeness() {
        float t = Mth.clamp((System.currentTimeMillis() - changedAt) / (float) TURN_MS, 0.0F, 1.0F);
        float from = before == UniverseTakeoverPayload.RUNES ? 1.0F : 0.0F;
        float to = state == UniverseTakeoverPayload.RUNES ? 1.0F : 0.0F;
        return Mth.lerp(t, from, to);
    }

    /**
     * The red vault and the runes, drawn after the weather (as the Hex Runes are; nothing here writes depth, and the
     * terrain in front hides them).
     */
    @Subscribe
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        float k = redness();
        Minecraft mc = Minecraft.getInstance();
        if (k <= 0.001F || mc.level == null) {
            return;
        }
        float h = mc.options.getEffectiveRenderDistance() * 16.0F + 24.0F;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        int c = colour();
        VertexConsumer vc = buffers.getBuffer(EnhancedRenderTypes.ENERGY_VEIL);
        vault(vc, h, c, 0.72F * k);
        buffers.endBatch(EnhancedRenderTypes.ENERGY_VEIL);
        float runes = runeness() * k;
        if (runes <= 0.001F) {
            return;
        }
        float time = (mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false));
        boolean still = EnhancedClientConfig.reducedMotion();
        if (still) {
            time = 0.0F;
        }
        RenderType circleType = EnhancedRenderTypes.oneSidedGlow(CIRCLE);
        vc = buffers.getBuffer(circleType);
        float breathe = 0.85F + 0.15F * Mth.sin(time * 0.05F);
        disc(vc, h * 0.62F, h * 0.95F, time * 0.004F, alpha(RUNE_LIGHT, runes * breathe));
        disc(vc, h * 0.58F, h * 0.42F, -time * 0.009F, alpha(RUNE_LIGHT, runes * 0.8F * breathe));
        buffers.endBatch(circleType);
        RenderType glyphType = EnhancedRenderTypes.oneSidedGlow(GLYPHS);
        vc = buffers.getBuffer(glyphType);
        ring(vc, h * 0.9F, 0.22F, 24, time * 0.0025F, h * 0.075F, time, runes, 0);
        ring(vc, h * 0.85F, 0.50F, 18, -time * 0.0035F, h * 0.07F, time, runes, 3);
        ring(vc, h * 0.8F, 0.82F, 12, time * 0.005F, h * 0.065F, time, runes, 5);
        buffers.endBatch(glyphType);
    }

    private static int alpha(int rgb, float a) {
        return ((int) (Mth.clamp(a, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }

    /** A box of red round the camera, its faces {@code h} away: the sky behind the land turned red. */
    private static void vault(VertexConsumer vc, float h, int rgb, float a) {
        int top = alpha(rgb, a);
        int low = alpha(rgb, Math.min(1.0F, a * 1.15F));
        float y0 = -h;
        float y1 = h;
        // the four walls
        quad(vc, -h, y0, -h, h, y0, -h, h, y1, -h, -h, y1, -h, low, top);
        quad(vc, h, y0, -h, h, y0, h, h, y1, h, h, y1, -h, low, top);
        quad(vc, h, y0, h, -h, y0, h, -h, y1, h, h, y1, h, low, top);
        quad(vc, -h, y0, h, -h, y0, -h, -h, y1, -h, -h, y1, h, low, top);
        // the roof and the floor
        quad(vc, -h, y1, -h, h, y1, -h, h, y1, h, -h, y1, h, top, top);
        quad(vc, -h, y0, -h, -h, y0, h, h, y0, h, h, y0, -h, low, low);
    }

    private static void quad(VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2,
                             float z2, float x3, float y3, float z3, int bottom, int top) {
        vc.addVertex(POSE, x0, y0, z0).setColor(bottom);
        vc.addVertex(POSE, x1, y1, z1).setColor(bottom);
        vc.addVertex(POSE, x2, y2, z2).setColor(top);
        vc.addVertex(POSE, x3, y3, z3).setColor(top);
    }

    /** A sigil lying flat high overhead, {@code y} up, {@code size} across, turned by {@code angle} (seen from below). */
    private static void disc(VertexConsumer vc, float y, float size, float angle, int argb) {
        float r = size * 0.5F;
        float cos = Mth.cos(angle) * r;
        float sin = Mth.sin(angle) * r;
        float[][] corners = {{-cos + sin, -sin - cos}, {cos + sin, sin - cos}, {cos - sin, sin + cos}, {-cos - sin, -sin + cos}};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        // both windings: the sigil is seen from below, whichever way the culling faces
        for (int i = 0; i < 4; i++) {
            vc.addVertex(POSE, corners[i][0], y, corners[i][1]).setUv(uv[i][0], uv[i][1]).setColor(argb);
        }
        for (int i = 3; i >= 0; i--) {
            vc.addVertex(POSE, corners[i][0], y, corners[i][1]).setUv(uv[i][0], uv[i][1]).setColor(argb);
        }
    }

    /**
     * A ring of glyphs round the sky at {@code elevation} (radians), {@code count} of them, turning by {@code turn}: each
     * stands upright facing the camera and breathes (brightness and size) in its own time.
     */
    private static void ring(VertexConsumer vc, float distance, float elevation, int count, float turn, float size, float time,
                             float runes, int seed) {
        float ce = Mth.cos(elevation);
        float se = Mth.sin(elevation);
        for (int i = 0; i < count; i++) {
            float a = turn + i * Mth.TWO_PI / count;
            float phase = i * 1.7F + seed * 2.3F;
            float pulse = 0.55F + 0.45F * Mth.sin(time * 0.07F + phase);
            float half = size * (0.8F + 0.25F * pulse) * 0.5F;
            float sa = Mth.sin(a);
            float ca = Mth.cos(a);
            float cx = sa * ce * distance;
            float cy = se * distance;
            float cz = ca * ce * distance;
            // right and up on the sky, as the camera sees them
            float rx = ca;
            float rz = -sa;
            float ux = -sa * se;
            float uy = ce;
            float uz = -ca * se;
            int glyph = (i + seed) % GLYPH_COUNT;
            float u0 = glyph / (float) GLYPH_COUNT;
            float u1 = (glyph + 1) / (float) GLYPH_COUNT;
            int argb = alpha(RUNE_LIGHT, runes * (0.45F + 0.55F * pulse));
            float[][] p = {
                    {cx - rx * half - ux * half, cy - uy * half, cz - rz * half - uz * half},
                    {cx + rx * half - ux * half, cy - uy * half, cz + rz * half - uz * half},
                    {cx + rx * half + ux * half, cy + uy * half, cz + rz * half + uz * half},
                    {cx - rx * half + ux * half, cy + uy * half, cz - rz * half + uz * half}};
            float[][] uv = {{u0, 1}, {u1, 1}, {u1, 0}, {u0, 0}};
            for (int j = 0; j < 4; j++) {
                vc.addVertex(POSE, p[j][0], p[j][1], p[j][2]).setUv(uv[j][0], uv[j][1]).setColor(argb);
            }
            for (int j = 3; j >= 0; j--) {
                vc.addVertex(POSE, p[j][0], p[j][1], p[j][2]).setUv(uv[j][0], uv[j][1]).setColor(argb);
            }
        }
    }

    /** The server's word: what the universe the player stands in is now. */
    public static void accept(int newState) {
        if (newState == state) {
            return;
        }
        before = state;
        state = newState;
        changedAt = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && before == UniverseTakeoverPayload.CLEAR) {
            mc.player.playSound(SoundEvents.AMBIENT_CAVE.value(), 0.6F, 0.5F);
        } else if (mc.player != null && newState == UniverseTakeoverPayload.RUNES) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY() + 20.0, mc.player.getZ(), SoundEvents.EVOKER_PREPARE_WOLOLO,
                    SoundSource.AMBIENT, 0.8F, 0.5F, false);
        }
    }

    /** How red the sky is now (0 to 1). */
    private static float redness() {
        float t = Mth.clamp((System.currentTimeMillis() - changedAt) / (float) TURN_MS, 0.0F, 1.0F);
        float from = before == UniverseTakeoverPayload.CLEAR ? 0.0F : 1.0F;
        float to = state == UniverseTakeoverPayload.CLEAR ? 0.0F : 1.0F;
        return Mth.lerp(t, from, to);
    }

    private static int colour() {
        return state == UniverseTakeoverPayload.RUNES || state == UniverseTakeoverPayload.CLEAR && before == UniverseTakeoverPayload.RUNES
                ? RUNE_SKY : SKY;
    }

    @Subscribe
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        float k = redness();
        if (k <= 0.001F) {
            return;
        }
        int c = colour();
        event.setRed(Mth.lerp(k, event.getRed(), ((c >> 16) & 0xFF) / 255.0F));
        event.setGreen(Mth.lerp(k, event.getGreen(), ((c >> 8) & 0xFF) / 255.0F));
        event.setBlue(Mth.lerp(k, event.getBlue(), (c & 0xFF) / 255.0F));
    }

    /** The sky dome is lost in the red fog (the whole of it is the fog's colour); the land's fog keeps its distance. */
    @Subscribe
    public static void onFog(ViewportEvent.RenderFog event) {
        float k = redness();
        if (k <= 0.001F || event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_SKY) {
            return;
        }
        float far = event.getFarPlaneDistance();
        float wanted = Mth.lerp(k, far, 2.0F);
        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(wanted);
        event.setCanceled(true);
    }

    @Subscribe
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        state = UniverseTakeoverPayload.CLEAR;
        before = UniverseTakeoverPayload.CLEAR;
        changedAt = 0L;
    }
}
