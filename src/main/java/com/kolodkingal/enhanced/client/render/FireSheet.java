package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A sheet of real-looking fire (3.17): Jim Hammond's burning body and his fireballs.
 *
 * <p>How it is made, the way flames are usually drawn without a shader: one seamless texture of tall flame tongues (white, its alpha
 * the shape) scrolls up a quad that always faces the camera (turning only about its own upright axis, so it never shows an edge), the
 * quad split into a small grid whose corners carry the colour (white-yellow at the foot, orange, red at the tips) and a soft mask (none at
 * the sides or the top), so the sheet has no edges and its flames rise and lick continuously. Nothing ever changes frame, so nothing
 * flickers. It is drawn twice: once see-through with normal blending ({@link #DECAL}, real fire colours, readable on a bright sky) and
 * once added as light ({@link #GLOW}, the glow of the hot cores).
 */
public final class FireSheet {
    private static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/jim/flame_sheet.png");
    /** The passes: see-through colour, then added light; smoke is drawn in the colour pass, before the flames. */
    public static final int DECAL = 0;
    public static final int GLOW = 1;
    public static final int SMOKE = 2;
    private static final int COLS = 4;
    private static final int ROWS = 6;

    private FireSheet() {
    }

    /** The render type of a pass (draw every sheet of one pass before you get the other pass's buffer). */
    public static RenderType type(int pass) {
        return pass == GLOW ? EnhancedRenderTypes.energyField(TEXTURE) : EnhancedRenderTypes.webDecal(TEXTURE);
    }

    /**
     * One sheet. Every vector is in the frame of {@code m}: {@code base} the middle of its foot, {@code right} and {@code up} its
     * half-width and height (not unit: their length is the size). {@code time} in ticks; {@code seed} keeps sheets apart; {@code speed}
     * how fast its flames rise; {@code strength} 0 to 1.
     */
    public static void draw(int pass, Matrix4f m, VertexConsumer vc, Vector3f base, Vector3f right, Vector3f up, float time, float seed,
                            float speed, float strength) {
        if (strength <= 0.003F) {
            return;
        }
        // the texture holds its tongues twice, one above the other: a window of half its height slides up it and wraps unseen
        float s = ((time * speed + seed * 0.731F) % 1.0F + 1.0F) % 1.0F * 0.5F;
        float u0 = ((seed * 0.377F) % 1.0F + 1.0F) % 1.0F * 0.5F;
        for (int row = 0; row < ROWS; row++) {
            float y0 = row / (float) ROWS;
            float y1 = (row + 1) / (float) ROWS;
            for (int col = 0; col < COLS; col++) {
                float x0 = col / (float) COLS;
                float x1 = (col + 1) / (float) COLS;
                vertex(pass, m, vc, base, right, up, x0, y0, u0, s, strength);
                vertex(pass, m, vc, base, right, up, x1, y0, u0, s, strength);
                vertex(pass, m, vc, base, right, up, x1, y1, u0, s, strength);
                vertex(pass, m, vc, base, right, up, x0, y1, u0, s, strength);
            }
        }
    }

    private static void vertex(int pass, Matrix4f m, VertexConsumer vc, Vector3f base, Vector3f right, Vector3f up, float xf, float yf,
                               float u0, float s, float strength) {
        float side = xf * 2.0F - 1.0F;
        float x = base.x + right.x * side + up.x * yf;
        float y = base.y + right.y * side + up.y * yf;
        float z = base.z + right.z * side + up.z * yf;
        // the tongues narrow as they rise
        float narrow = 1.0F - 0.35F * yf;
        x -= right.x * side * (1.0F - narrow);
        y -= right.y * side * (1.0F - narrow);
        z -= right.z * side * (1.0F - narrow);
        float u = u0 + xf * 0.5F;
        float v = s + (1.0F - yf) * 0.5F;
        // the soft mask: nothing at the sides or the top, a short rise at the foot
        float across = (float) Math.pow(Mth.sin(xf * Mth.PI), 0.8);
        float along = pass == SMOKE
                // smoke: nothing at the foot, it gathers where the flames end and thins away above
                ? Mth.clamp((yf - 0.35F) / 0.3F, 0.0F, 1.0F) * (float) Math.pow(1.0F - yf, 0.8)
                : Math.min(1.0F, yf / 0.12F + 0.25F) * (float) Math.pow(1.0F - yf, 1.25);
        float k = across * along * strength;
        int r;
        int g;
        int b;
        float a;
        if (pass == DECAL) {
            // the colour order of real fire: white-yellow at the foot, saturated orange, deep red, a dark edge at the very tips
            r = 255;
            g = Math.round(Mth.lerp(Math.min(1.0F, yf * 1.6F), 236, 96));
            b = Math.round(Mth.lerp(Math.min(1.0F, yf * 2.5F), 150, 16));
            if (yf > 0.62F) {
                float tip = (yf - 0.62F) / 0.38F;
                r = Math.round(Mth.lerp(tip, 255, 120));
                g = Math.round(Mth.lerp(tip, g, 24));
                b = Math.round(Mth.lerp(tip, b, 8));
            }
            a = 0.62F * k;
        } else if (pass == SMOKE) {
            // thin dark smoke, warm-grey low down and cooler higher up
            r = Math.round(Mth.lerp(yf, 70, 46));
            g = Math.round(Mth.lerp(yf, 54, 44));
            b = Math.round(Mth.lerp(yf, 44, 44));
            a = 0.42F * k;
        } else {
            // the glow of the hot cores, strongest low down
            r = 255;
            g = Math.round(Mth.lerp(yf, 200, 90));
            b = Math.round(Mth.lerp(yf, 90, 20));
            a = 0.5F * k * (1.0F - 0.5F * yf);
        }
        vc.addVertex(m, x, y, z).setUv(u, v).setColor(r, g, b, Math.round(Mth.clamp(a, 0.0F, 1.0F) * 255.0F));
    }

    /**
     * The upright, camera-facing frame of a sheet whose foot is at {@code footWorld} (camera-relative world coordinates, the camera at
     * the origin), rising along {@code riseWorld} (a world direction; its length is the height), {@code halfWidth} blocks to each side:
     * fills {@code base}, {@code right}, {@code up} in the frame of {@code m} (whose inverse is {@code inverse}).
     */
    public static void frame(Matrix4f inverse, Vector3f footWorld, Vector3f riseWorld, float halfWidth, Vector3f base, Vector3f right,
                             Vector3f up) {
        Vector3f toCamera = new Vector3f(footWorld).negate();
        Vector3f axis = new Vector3f(riseWorld);
        if (axis.lengthSquared() < 1.0E-6F) {
            axis.set(0, 1, 0);
        }
        Vector3f side = new Vector3f(axis).cross(toCamera);
        if (side.lengthSquared() < 1.0E-6F) {
            side.set(1, 0, 0);
        }
        side.normalize(halfWidth);
        inverse.transformPosition(footWorld, base);
        inverse.transformDirection(side, right);
        inverse.transformDirection(riseWorld, up);
    }
}
