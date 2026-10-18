package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * The Hex Runes (3.3) on the four walls of a Hex, as on the walls of Westview: on each side of the dome (north, east, south,
 * west) a panel of glowing red runes on the wall, from just above the middle of the Hex upward, drawn just inside and just
 * outside the wall so they are seen from both sides (the dome's own veil lies between, so they never fight it). Each rune
 * breathes slowly on its own beat; they fade in when Wanda inscribes them and out when she wipes them, all from synced game
 * time and the partial tick, so nothing pops or flickers. Reduced motion: they glow steady.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class HexRunesRenderer {
    static final ResourceLocation RUNES = Enhanced.id("textures/entity/hex_runes.png");
    /** How long (ticks) the runes take to fade in or out. */
    private static final float FADE_TICKS = 30.0F;
    /** How far (blocks) inside and outside the wall the two layers lie. */
    private static final double LAYER = 0.15;
    private static final int GLOW = 0xFF3A55;
    private static final int CORE = 0xFFB0BC;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        var hexes = level.getData(ModAttachments.HEXES).hexes();
        if (hexes.isEmpty()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        float time = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double reach = mc.options.getEffectiveRenderDistance() * 16.0 + 32.0;
        boolean still = EnhancedClientConfig.reducedMotion();
        Matrix4f pose = new Matrix4f();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = EnhancedRenderTypes.energyField(RUNES);
        VertexConsumer vc = null;
        for (Hex hex : hexes) {
            float shown = shown(hex, time);
            if (shown <= 0.0F) {
                continue;
            }
            float grow = Mth.clamp((time - hex.createdAt()) / 50F, 0F, 1F);
            grow = grow * grow * (3 - 2 * grow);
            double radius = hex.radius() * grow;
            Vec3 center = hex.center().subtract(camera);
            double distance = center.length();
            if (radius < 2.0 || Math.abs(distance - radius) > reach) {
                continue;
            }
            if (vc == null) {
                vc = buffers.getBuffer(type);
            }
            for (int side = 0; side < 4; side++) {
                panel(pose, vc, center, radius, side, shown, time, still, hex.id().hashCode(), hex.color());
            }
        }
        if (vc != null) {
            buffers.endBatch(type);
        }
    }

    /** How much the runes of this Hex show now: fading in after they were inscribed, fading out after they were wiped. */
    static float shown(Hex hex, float time) {
        float since = Mth.clamp((time - hex.runesAt()) / FADE_TICKS, 0.0F, 1.0F);
        since = since * since * (3.0F - 2.0F * since);
        return hex.runes() ? since : 1.0F - since;
    }

    /** One wall's panel of runes: a grid of cells on the sphere, each a rune of the sheet, inside and outside. */
    private static void panel(Matrix4f pose, VertexConsumer vc, Vec3 center, double radius, int side, float shown, float time, boolean still,
                              int seed, int hexColor) {
        // (3.5: the runes of a Hex raised in a chosen colour glow in it.)
        int glowRgb = hexColor < 0 ? GLOW : AbilityColors.recolor(GLOW, hexColor);
        int coreRgb = hexColor < 0 ? CORE : AbilityColors.recolor(CORE, hexColor);
        double cell = Mth.clamp(radius / 12.0, 1.2, 3.0);
        double width = Math.min(radius * 0.9, 18.0);
        double height = Math.min(radius * 0.55, 9.0);
        int cols = Math.max(2, (int) Math.round(width / cell));
        int rows = Math.max(1, (int) Math.round(height / cell));
        double base = 0.5;
        double azimuth = Math.PI * 0.5 * side;
        double span = cols * cell / radius; // the angle the panel spans
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int glyph = Math.floorMod(seed * 31 + side * 977 + row * 131 + col * 17 + (col * row) * 7, 16);
                float beat = still ? 1.0F : 0.72F + 0.28F * Mth.sin(time * 0.07F + side * 1.3F + row * 0.9F + col * 1.7F);
                float alpha = shown * beat;
                double a0 = azimuth - span * 0.5 + (col + 0.08) * cell / radius;
                double a1 = azimuth - span * 0.5 + (col + 0.92) * cell / radius;
                double y0 = base + (row + 0.08) * cell;
                double y1 = base + (row + 0.92) * cell;
                if (y1 >= radius - 0.5) {
                    continue; // (a small Hex: no room up there)
                }
                float u0 = (glyph % 8) / 8.0F;
                float v0 = (glyph / 8) / 2.0F;
                float u1 = u0 + 1.0F / 8.0F;
                float v1 = v0 + 0.5F;
                for (double r : new double[]{radius - LAYER, radius + LAYER}) {
                    int glow = EnergyGeometry.argb(glowRgb, 0.85F * alpha);
                    int core = EnergyGeometry.argb(coreRgb, 0.85F * alpha);
                    vertex(pose, vc, center, r, a0, y0, u0, v1, glow);
                    vertex(pose, vc, center, r, a1, y0, u1, v1, glow);
                    vertex(pose, vc, center, r, a1, y1, u1, v0, core);
                    vertex(pose, vc, center, r, a0, y1, u0, v0, core);
                }
            }
        }
    }

    /** The point of the sphere of radius {@code r} round {@code center} at {@code azimuth}, {@code y} blocks above its middle. */
    private static void vertex(Matrix4f pose, VertexConsumer vc, Vec3 center, double r, double azimuth, double y, float u, float v, int argb) {
        double flat = Math.sqrt(Math.max(0.0, r * r - y * y));
        float x = (float) (center.x + Math.sin(azimuth) * flat);
        float z = (float) (center.z + Math.cos(azimuth) * flat);
        vc.addVertex(pose, x, (float) (center.y + y), z).setUv(u, v).setColor(argb);
    }

    private HexRunesRenderer() {
    }
}
