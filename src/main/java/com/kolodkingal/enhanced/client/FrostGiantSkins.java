package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loki's Frost Giant form, painted onto whatever skin he wears (any look's suit, or the player's own skin): every
 * pixel of bare skin turns Jotun blue-grey with its shading kept, raised ridges darken the face, and the eyes burn
 * red. Skin is found by its colour: the face's skin tone is measured first, then every pixel of that tone is
 * recoloured. Each source skin is recoloured once, into a texture of its own.
 */
public final class FrostGiantSkins {
    private static final int EYES = 0xD0201E;
    private static final int EYE_GLINT = 0xFF5A46;

    /** Raised ridges on the face, as darker lines (face-relative pixels of a 64x64 skin, x then y). */
    private static final int[][] FACE_RIDGES = {
            {2, 1}, {2, 2}, {5, 1}, {5, 2},     // forehead
            {0, 5}, {1, 6}, {7, 5}, {6, 6},     // cheeks
            {3, 7}, {4, 7},                     // chin
    };
    /** Ridges down the sides of the head, temple to jaw (relative to each side face). */
    private static final int[][] SIDE_RIDGES = {{3, 2}, {3, 3}, {4, 4}, {4, 5}};

    /** Source texture → its Frost Giant version, or null if it can't be recoloured (missing, not loaded yet). */
    private static final Map<ResourceLocation, ResourceLocation> FROSTED = new HashMap<>();
    private static final Map<PlayerSkin, PlayerSkin> SKINS = new HashMap<>();

    /** The same skin (model, cape) with its texture turned Frost Giant. */
    public static PlayerSkin of(PlayerSkin skin) {
        PlayerSkin cached = SKINS.get(skin);
        if (cached != null) {
            return cached;
        }
        ResourceLocation frost = frosted(skin.texture());
        if (frost == null) {
            return skin;
        }
        PlayerSkin frozen = new PlayerSkin(frost, null, skin.capeTexture(), skin.elytraTexture(), skin.model(), skin.secure());
        SKINS.put(skin, frozen);
        return frozen;
    }

    /** Forgets every recoloured skin (resource packs changed). */
    public static void clear() {
        for (ResourceLocation id : FROSTED.values()) {
            if (id != null) {
                Minecraft.getInstance().getTextureManager().release(id);
            }
        }
        FROSTED.clear();
        SKINS.clear();
    }

    @Nullable
    private static ResourceLocation frosted(ResourceLocation source) {
        if (FROSTED.containsKey(source)) {
            return FROSTED.get(source);
        }
        if (!RenderSystem.isOnRenderThread()) {
            return null;
        }
        ResourceLocation result = null;
        try (NativeImage image = read(source)) {
            if (image != null && image.getWidth() >= 64 && image.getHeight() >= 64 && image.getWidth() == image.getHeight()) {
                NativeImage frozen = new NativeImage(image.getWidth(), image.getHeight(), true);
                frozen.copyFrom(image);
                recolour(frozen);
                result = Enhanced.id("frost_giant/" + source.getNamespace() + "/" + source.getPath());
                Minecraft.getInstance().getTextureManager().register(result, new DynamicTexture(frozen));
            }
        } catch (IOException | RuntimeException e) {
            Enhanced.LOGGER.warn("Couldn't paint the Frost Giant form onto {}", source, e);
            result = null;
        }
        FROSTED.put(source, result);
        return result;
    }

    /** The pixels of a skin texture: from the resource packs, or (downloaded skins) back from the GPU. */
    @Nullable
    private static NativeImage read(ResourceLocation source) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        Optional<Resource> resource = mc.getResourceManager().getResource(source);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open()) {
                return NativeImage.read(in);
            }
        }
        AbstractTexture texture = mc.getTextureManager().getTexture(source);
        if (texture == MissingTextureAtlasSprite.getTexture()) {
            return null;
        }
        texture.bind();
        int width = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (width <= 0 || height <= 0) {
            return null;
        }
        NativeImage image = new NativeImage(width, height, false);
        image.downloadTexture(0, false);
        return image;
    }

    // ---- the recolouring ----

    /** A skin tone: hue (degrees), saturation and brightness of the face's typical skin pixel. */
    private record Tone(float hue, float sat, float value) {
        /** A generic skin tone, for faces without a clear one (masks, helmets). */
        static final Tone DEFAULT = new Tone(25.0F, 0.38F, 0.78F);

        /**
         * Whether a pixel is this skin, in light or shadow. Shadows lean red and highlights pale, so the hue window
         * reaches further towards red than towards yellow (which keeps blond hair and gold out); near-greys (the
         * whites of the eyes) and anything much darker (leather, hair) are not skin.
         */
        boolean matches(float[] hsv) {
            float dh = ((hsv[0] - hue + 540.0F) % 360.0F) - 180.0F;
            return dh >= -14.0F && dh <= 6.5F && hsv[1] >= 0.08F && Math.abs(hsv[1] - sat) <= 0.25F
                    && hsv[2] >= value * 0.55F && hsv[2] > 0.12F;
        }
    }

    /** Recolours a 64x64 (or HD, square) skin in place. */
    static void recolour(NativeImage img) {
        int s = img.getWidth() / 64;
        Tone tone = faceTone(img, s);
        int w = img.getWidth();
        int h = img.getHeight();
        boolean[][] skin = new boolean[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int abgr = img.getPixelRGBA(x, y);
                skin[x][y] = alpha(abgr) > 0 && tone.matches(hsv(abgr));
            }
        }
        // The eyes are found before the skin around them changes colour.
        List<int[]> eyes = eyes(img, skin, s);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (skin[x][y]) {
                    int abgr = img.getPixelRGBA(x, y);
                    float shade = hsv(abgr)[2] / tone.value();
                    img.setPixelRGBA(x, y, abgr(LokiAbilities.FROST_COLOR, shade, alpha(abgr)));
                }
            }
        }
        ridges(img, skin, FACE_RIDGES, 8 * s, 8 * s, s);
        ridges(img, skin, SIDE_RIDGES, 0, 8 * s, s);
        ridges(img, skin, mirror(SIDE_RIDGES), 16 * s, 8 * s, s);
        for (int[] eye : eyes) {
            int abgr = img.getPixelRGBA(eye[0], eye[1]);
            img.setPixelRGBA(eye[0], eye[1], abgr(isWhite(hsv(abgr)) ? EYE_GLINT : EYES, 1.0F, 255));
            // A hat layer drawn over the eye (glasses, a mask's eye holes) turns red too.
            int ox = eye[0] + 32 * s;
            if (ox < w && alpha(img.getPixelRGBA(ox, eye[1])) > 0 && !skin[ox][eye[1]]) {
                img.setPixelRGBA(ox, eye[1], abgr(EYES, 1.0F, alpha(img.getPixelRGBA(ox, eye[1]))));
            }
        }
    }

    /** The typical skin tone of the face's lower part (below the hairline): the medians of its skin-like pixels. */
    private static Tone faceTone(NativeImage img, int s) {
        List<float[]> found = new ArrayList<>();
        for (int y = 10 * s; y < 16 * s; y++) {
            for (int x = 8 * s; x < 16 * s; x++) {
                int abgr = img.getPixelRGBA(x, y);
                float[] c = hsv(abgr);
                boolean warm = c[0] <= 50.0F || c[0] >= 340.0F;
                if (alpha(abgr) > 0 && warm && c[1] >= 0.08F && c[1] <= 0.8F && c[2] >= 0.25F && red(abgr) >= blue(abgr) + 12) {
                    found.add(c);
                }
            }
        }
        if (found.size() < 4 * s * s) {
            return Tone.DEFAULT;
        }
        return new Tone(median(found, 0), median(found, 1), median(found, 2));
    }

    private static float median(List<float[]> values, int channel) {
        float[] sorted = new float[values.size()];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = values.get(i)[channel];
        }
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    /**
     * The eye pixels: in the face's eye rows, whatever isn't skin, away from the face's edges (hair). An eye row has
     * both the white of an eye and something coloured (the iris) in it; without one, the face's usual eye row.
     */
    private static List<int[]> eyes(NativeImage img, boolean[][] skin, int s) {
        List<List<int[]>> rows = new ArrayList<>();
        List<Boolean> eyeRow = new ArrayList<>();
        List<Integer> rowIndex = new ArrayList<>();
        for (int fy = 2 * s; fy < 6 * s; fy++) {
            int y = 8 * s + fy;
            List<int[]> found = new ArrayList<>();
            boolean white = false;
            boolean coloured = false;
            boolean anySkin = false;
            for (int fx = 0; fx < 8 * s; fx++) {
                anySkin |= skin[8 * s + fx][y];
            }
            for (int fx = s; anySkin && fx < 7 * s; fx++) {
                int x = 8 * s + fx;
                int abgr = img.getPixelRGBA(x, y);
                if (!skin[x][y] && alpha(abgr) > 0) {
                    found.add(new int[]{x, y});
                    if (isWhite(hsv(abgr))) {
                        white = true;
                    } else {
                        coloured = true;
                    }
                }
            }
            rows.add(found);
            eyeRow.add(white && coloured);
            rowIndex.add(fy);
        }
        List<int[]> eyes = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (eyeRow.get(i)) {
                eyes.addAll(rows.get(i));
            }
        }
        if (eyes.isEmpty()) {
            for (int preferred : new int[]{4, 3, 5}) {
                for (int i = 0; i < rows.size(); i++) {
                    if (rowIndex.get(i) / s == preferred && !rows.get(i).isEmpty()) {
                        eyes.addAll(rows.get(i));
                    }
                }
                if (!eyes.isEmpty()) {
                    break;
                }
            }
        }
        return eyes;
    }

    /** The white of an eye: bright and nearly grey. */
    private static boolean isWhite(float[] hsv) {
        return hsv[2] > 0.7F && hsv[1] < 0.2F;
    }

    /** Darkens the (already blue) skin pixels of a ridge pattern on one face of the head. */
    private static void ridges(NativeImage img, boolean[][] skin, int[][] pattern, int x0, int y0, int s) {
        for (int[] p : pattern) {
            for (int dy = 0; dy < s; dy++) {
                for (int dx = 0; dx < s; dx++) {
                    int x = x0 + p[0] * s + dx;
                    int y = y0 + p[1] * s + dy;
                    if (skin[x][y]) {
                        int abgr = img.getPixelRGBA(x, y);
                        img.setPixelRGBA(x, y, scale(abgr, 0.66F));
                    }
                }
            }
        }
    }

    private static int[][] mirror(int[][] pattern) {
        int[][] out = new int[pattern.length][];
        for (int i = 0; i < pattern.length; i++) {
            out[i] = new int[]{7 - pattern[i][0], pattern[i][1]};
        }
        return out;
    }

    // ---- colour helpers (NativeImage pixels are ABGR) ----

    private static int alpha(int abgr) {
        return abgr >>> 24;
    }

    private static int red(int abgr) {
        return abgr & 0xFF;
    }

    private static int green(int abgr) {
        return (abgr >> 8) & 0xFF;
    }

    private static int blue(int abgr) {
        return (abgr >> 16) & 0xFF;
    }

    /** Hue (0-360), saturation and value (0-1). */
    private static float[] hsv(int abgr) {
        float r = red(abgr) / 255.0F;
        float g = green(abgr) / 255.0F;
        float b = blue(abgr) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta < 1.0E-6F) {
            hue = 0.0F;
        } else if (max == r) {
            hue = 60.0F * (((g - b) / delta) % 6.0F);
        } else if (max == g) {
            hue = 60.0F * ((b - r) / delta + 2.0F);
        } else {
            hue = 60.0F * ((r - g) / delta + 4.0F);
        }
        if (hue < 0.0F) {
            hue += 360.0F;
        }
        return new float[]{hue, max <= 0.0F ? 0.0F : delta / max, max};
    }

    /** An RGB colour scaled by {@code shade}, as an ABGR pixel. */
    private static int abgr(int rgb, float shade, int alpha) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * shade));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * shade));
        int b = Math.min(255, Math.round((rgb & 0xFF) * shade));
        return alpha << 24 | b << 16 | g << 8 | r;
    }

    private static int scale(int abgr, float factor) {
        int r = Math.round(red(abgr) * factor);
        int g = Math.round(green(abgr) * factor);
        int b = Math.round(blue(abgr) * factor);
        return abgr & 0xFF000000 | b << 16 | g << 8 | r;
    }

    private FrostGiantSkins() {
    }
}
