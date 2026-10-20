package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexRuneLayout;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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

import java.util.Collection;

/**
 * The Hex Runes on the four walls of a Hex (3.3; 3.7.1 as in the WandaVision finale): huge scarlet runes of glowing lines
 * hanging on the wall, each in a soft red mist with faint scanlines, spread over every wall ({@link HexRuneLayout}: rows of
 * them from the ground to where the wall turns into the roof) and growing with the Hex: they are laid out each frame at the
 * radius the wall is drawn at ({@link WorldEffectsRenderer.ShownHex}), so they rise with a rising wall, swell or shrink with
 * it while it eases to a new size, and a wall that gained or lost runes fades the old ones into the new.
 *
 * <p>A rune is its strokes ({@link HexRuneGlyphs}), each a wide soft halo under a neon tube (bright edges, dimmer inside),
 * fullbright and additive, over a dark red mist that darkens a bright sky (so they stay scarlet by day) and a red glow. The runes lie on the sphere just inside the wall when the camera is inside the Hex and just
 * outside it when it is outside, facing the camera's side only (so they read the right way round from both sides and the
 * far wall's runes do not show back to front through the dome); nothing writes depth, so they never fight the wall. When
 * Wanda inscribes them they are drawn stroke by stroke; when she wipes them they fade. Each breathes on its own beat with a
 * rare flicker; under Reduced motion they glow steady.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class HexRunesRenderer {
    static final ResourceLocation GLOW_TEXTURE = Enhanced.id("textures/entity/hex_rune_glow.png");
    /** Ticks the strokes take to be drawn when Wanda inscribes the runes, and the runes to fade when she wipes them. */
    private static final float INSCRIBE_TICKS = 40.0F;
    private static final float FADE_TICKS = 30.0F;
    /** Ticks the runes of a wall that gained or lost some take to fade from the old count into the new. */
    private static final float RECOUNT_TICKS = 20.0F;
    /** How far (blocks) inside or outside the wall the runes lie. */
    private static final double LAYER = 0.12;
    /** A stroke's halo and tube widths, and the mist's half-size, in rune heights. */
    private static final double HALO_WIDTH = 0.28;
    private static final double TUBE_WIDTH = 0.085;
    private static final double MIST = 0.85;
    private static final int HALO = 0xFF1020;
    private static final int TUBE = 0xFF5A4E;
    private static final int MIST_COLOR = 0xB0102A;
    /** The dark red of the mist that darkens the sky behind a rune (so a rune is scarlet by day too). */
    private static final int SHADE = 0x3A0610;
    /** The strips of the glow texture: the halo, the tube (across a stroke) and the mist. */
    private static final float HALO_U0 = 1F / 64F;
    private static final float HALO_U1 = 15F / 64F;
    private static final float TUBE_U0 = 17F / 64F;
    private static final float TUBE_U1 = 31F / 64F;
    private static final float MIST_U0 = 32F / 64F;
    private static final float MIST_U1 = 1F;
    /** The pose the runes are drawn with (the camera's own is the level's model-view). */
    private static final Matrix4f POSE = new Matrix4f();
    /** One wall's runes, filled by {@link HexRuneLayout#place} (reused every frame). */
    private static final float[] PLACES = new float[HexRuneLayout.MAX_PER_WALL * HexRuneLayout.FIELDS];

    /** The frame's state of the rune being drawn (the render thread only), so nothing is allocated per stroke. */
    private static double cx;
    private static double cy;
    private static double cz;
    private static double r;
    private static double dx;
    private static double dy;
    private static double dz;
    private static double rightX;
    private static double rightZ;
    private static double upX;
    private static double upY;
    private static double upZ;
    private static double tiltCos;
    private static double tiltSin;

    /**
     * Draws the runes of every Hex that has them, after the walls ({@link WorldEffectsRenderer}, after the particles) and
     * after the clouds and the weather, which would otherwise be drawn over them (nothing here writes depth): first the
     * dark mist behind them, then their light.
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || Minecraft.getInstance().level == null) {
            return;
        }
        Collection<WorldEffectsRenderer.ShownHex> hexes = WorldEffectsRenderer.shownHexes();
        if (hexes.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float time = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        render(POSE, mc.renderBuffers().bufferSource(), hexes, event.getCamera().getPosition(), time,
                mc.options.getEffectiveRenderDistance() * 16.0);
    }

    static void render(Matrix4f pose, MultiBufferSource.BufferSource buffers, Collection<WorldEffectsRenderer.ShownHex> hexes,
                       Vec3 camera, float time, double renderDistance) {
        pass(pose, buffers, EnhancedRenderTypes.oneSidedShade(GLOW_TEXTURE), true, hexes, camera, time, renderDistance);
        pass(pose, buffers, EnhancedRenderTypes.oneSidedGlow(GLOW_TEXTURE), false, hexes, camera, time, renderDistance);
    }

    private static void pass(Matrix4f pose, MultiBufferSource.BufferSource buffers, RenderType type, boolean shade,
                             Collection<WorldEffectsRenderer.ShownHex> hexes, Vec3 camera, float time, double renderDistance) {
        boolean still = EnhancedClientConfig.reducedMotion();
        VertexConsumer vc = null;
        for (WorldEffectsRenderer.ShownHex shown : hexes) {
            Hex hex = shown.hex;
            float alpha = shown(hex, time) * AnimMath.ss(0.0F, 1.0F, shown.fade);
            if (alpha <= 0.0F) {
                continue;
            }
            double radius = shown.drawnRadius(time);
            cx = hex.center().x - camera.x;
            cy = hex.center().y - camera.y;
            cz = hex.center().z - camera.z;
            double distance = Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (radius < 2.0 || Math.abs(distance - radius) > renderDistance + 32.0) {
                continue;
            }
            if (vc == null) {
                vc = buffers.getBuffer(type);
            }
            boolean inside = distance < radius;
            r = inside ? radius - LAYER : radius + LAYER;
            float progress = hex.runes() ? Mth.clamp((time - hex.runesAt()) / INSCRIBE_TICKS, 0.0F, 1.0F) : 1.0F;
            // (3.5: the runes of a Hex raised in a chosen colour glow in it.)
            int halo = hex.color() < 0 ? HALO : AbilityColors.recolor(HALO, hex.color());
            int tube = hex.color() < 0 ? TUBE : AbilityColors.recolor(TUBE, hex.color());
            int mist = hex.color() < 0 ? MIST_COLOR : AbilityColors.recolor(MIST_COLOR, hex.color());
            float recount = AnimMath.ss(0.0F, 1.0F, (time - shown.runeRadiusAt) / RECOUNT_TICKS);
            boolean blend = recount < 1.0F && !HexRuneLayout.sameLayout(shown.runeRadius, shown.runeRadiusBefore);
            for (int side = 0; side < 4; side++) {
                wall(pose, vc, shade, radius, shown.runeRadius, side, blend ? alpha * recount : alpha, progress, inside,
                        still, time, halo, tube, mist);
                if (blend) {
                    wall(pose, vc, shade, radius, shown.runeRadiusBefore, side, alpha * (1.0F - recount), progress, inside,
                            still, time, halo, tube, mist);
                }
            }
        }
        if (vc != null) {
            buffers.endBatch(type);
        }
    }

    /**
     * How much the runes of this Hex show now: fully while inscribed (they are drawn in stroke by stroke), fading out after
     * they were wiped.
     */
    static float shown(Hex hex, float time) {
        if (hex.runes()) {
            return 1.0F;
        }
        return 1.0F - AnimMath.ss(0.0F, 1.0F, (time - hex.runesAt()) / FADE_TICKS);
    }

    /** One wall's runes, as many as a Hex of radius {@code countRadius} has, laid out at {@code radius}. */
    private static void wall(Matrix4f pose, VertexConsumer vc, boolean shade, double radius, int countRadius, int side,
                             float alpha, float progress, boolean inside, boolean still, float time, int halo, int tube,
                             int mist) {
        int n = HexRuneLayout.place(radius, countRadius, side, PLACES, 0);
        for (int k = 0; k < n; k++) {
            int at = k * HexRuneLayout.FIELDS;
            float phase = PLACES[at + HexRuneLayout.PHASE];
            float glow = alpha;
            if (!still) {
                glow *= 0.84F + 0.16F * Mth.sin(time * 0.08F + phase);
                // Now and then a rune flickers, as the film's do.
                if (Mth.sin(time * 0.9F + phase * 3.1F) * Mth.sin(time * 0.23F + phase) > 0.92F) {
                    glow *= 0.65F;
                }
            }
            rune(pose, vc, shade, side, PLACES[at + HexRuneLayout.AZIMUTH], PLACES[at + HexRuneLayout.ELEVATION],
                    PLACES[at + HexRuneLayout.SIZE], (int) PLACES[at + HexRuneLayout.GLYPH], PLACES[at + HexRuneLayout.TILT],
                    glow, progress, inside, halo, tube, mist);
        }
    }

    /** One rune: its dark mist (the shade pass), or its glowing mist and its strokes (as many as are drawn in yet). */
    private static void rune(Matrix4f pose, VertexConsumer vc, boolean shade, int side, float azimuth, float elevation, float size, int glyph,
                             float tilt, float alpha, float progress, boolean inside, int halo, int tube, int mist) {
        double a = side * HexRuneLayout.QUARTER + azimuth;
        double sinA = Math.sin(a);
        double cosA = Math.cos(a);
        double sinE = Math.sin(elevation);
        double cosE = Math.cos(elevation);
        dx = sinA * cosE;
        dy = sinE;
        dz = cosA * cosE;
        // Right and up on the wall as seen from the camera's side of it (from inside, right is the other way round).
        rightX = inside ? -cosA : cosA;
        rightZ = inside ? sinA : -sinA;
        upX = -sinE * sinA;
        upY = cosE;
        upZ = -sinE * cosA;
        tiltCos = 1.0;
        tiltSin = 0.0;
        // The mist first, level (its scanlines run straight across), coming up as the strokes are drawn in.
        double m = size * MIST;
        float misty = alpha * Math.min(1.0F, progress * 3.0F);
        int mistArgb = shade ? EnergyGeometry.argb(SHADE, 0.6F * misty) : EnergyGeometry.argb(mist, 0.45F * misty);
        vertex(pose, vc, -m, -m, MIST_U0, 1.0F, mistArgb);
        vertex(pose, vc, m, -m, MIST_U1, 1.0F, mistArgb);
        vertex(pose, vc, m, m, MIST_U1, 0.0F, mistArgb);
        vertex(pose, vc, -m, m, MIST_U0, 0.0F, mistArgb);
        if (shade) {
            return;
        }
        tiltCos = Math.cos(tilt);
        tiltSin = Math.sin(tilt);
        double haloHalf = size * HALO_WIDTH * 0.5;
        double tubeHalf = size * TUBE_WIDTH * 0.5;
        float drawn = progress * HexRuneGlyphs.SEGMENTS[glyph];
        int segment = 0;
        for (float[] stroke : HexRuneGlyphs.STROKES[glyph]) {
            for (int p = 0; p + 3 < stroke.length; p += 2, segment++) {
                if (segment >= drawn) {
                    return;
                }
                float part = Math.min(1.0F, drawn - segment);
                // A short segment (of a ring) is dimmer, so a small ring whose glow overlaps itself is no white blot.
                float ax = stroke[p + 2] - stroke[p];
                float ay = stroke[p + 3] - stroke[p + 1];
                double full = Math.sqrt(ax * ax + ay * ay) * size;
                int haloArgb = EnergyGeometry.argb(halo, 0.7F * alpha * (float) Math.min(1.0, full / (2.0 * haloHalf)));
                int tubeArgb = EnergyGeometry.argb(tube, alpha);
                double x0 = stroke[p] * size;
                double y0 = stroke[p + 1] * size;
                double x1 = Mth.lerp(part, stroke[p], stroke[p + 2]) * size;
                double y1 = Mth.lerp(part, stroke[p + 1], stroke[p + 3]) * size;
                ribbon(pose, vc, x0, y0, x1, y1, haloHalf, HALO_U0, HALO_U1, haloArgb);
                ribbon(pose, vc, x0, y0, x1, y1, tubeHalf, TUBE_U0, TUBE_U1, tubeArgb);
            }
        }
    }

    /** A stroke from (x0, y0) to (x1, y1) (blocks, on the rune), {@code half} wide each way, its ends a little past its points. */
    private static void ribbon(Matrix4f pose, VertexConsumer vc, double x0, double y0, double x1, double y1, double half,
                               float u0, float u1, int argb) {
        double lx = x1 - x0;
        double ly = y1 - y0;
        double length = Math.sqrt(lx * lx + ly * ly);
        if (length < 1.0E-4) {
            return;
        }
        lx /= length;
        ly /= length;
        // Its ends reach a little past its points to close the joints (less for the short segments of a ring).
        double reach = Math.min(half * 0.6, length * 0.3);
        double ex = lx * reach;
        double ey = ly * reach;
        double px = -ly * half;
        double py = lx * half;
        // Counter-clockwise as seen from the camera's side, so the culling keeps it.
        vertex(pose, vc, x0 - ex - px, y0 - ey - py, u0, 0.25F, argb);
        vertex(pose, vc, x1 + ex - px, y1 + ey - py, u0, 0.75F, argb);
        vertex(pose, vc, x1 + ex + px, y1 + ey + py, u1, 0.75F, argb);
        vertex(pose, vc, x0 - ex + px, y0 - ey + py, u1, 0.25F, argb);
    }

    /** The point (x, y) blocks from the middle of the rune (turned by its tilt), laid on the sphere the runes lie on. */
    private static void vertex(Matrix4f pose, VertexConsumer vc, double x, double y, float u, float v, int argb) {
        double tx = (x * tiltCos - y * tiltSin) / r;
        double ty = (x * tiltSin + y * tiltCos) / r;
        double px = dx + tx * rightX + ty * upX;
        double py = dy + ty * upY;
        double pz = dz + tx * rightZ + ty * upZ;
        double scale = r / Math.sqrt(px * px + py * py + pz * pz);
        vc.addVertex(pose, (float) (cx + px * scale), (float) (cy + py * scale), (float) (cz + pz * scale)).setUv(u, v)
                .setColor(argb);
    }

    private HexRunesRenderer() {
    }
}
