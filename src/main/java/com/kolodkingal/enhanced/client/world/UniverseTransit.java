package com.kolodkingal.enhanced.client.world;

import com.kolodkingal.enhanced.multiverse.Multiverse;
import com.kolodkingal.enhanced.multiverse.UniverseRecipe;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * The passage between universes (3.19.1), drawn over the screen whenever the player comes into another universe (by the
 * Multiverse Bridge, the screen, a portal or a stone): for {@link #DURATION} ticks he rushes down a tunnel of light in
 * the colour of the universe he comes into, streaks of it flying past him, rings of it turning round him, then the
 * tunnel opens and the new world shows through. Worked out on the client alone, from where he is ({@link
 * Multiverse#universeAt}): the Nether and the End are the main universe's, so going there plays nothing.
 */
public final class UniverseTransit implements LayeredDraw.Layer {
    /** How long the passage lasts (ticks). */
    static final float DURATION = 46.0F;
    static final int STREAKS = 150;
    static final int RINGS = 4;
    static final int RING_DOTS = 44;
    private static final float GOLDEN = 2.3999631F;

    private static Integer last;
    private static Object lastLevel;
    private static float age = DURATION;
    private static int colour = 0x4FB8FF;

    /** Plays the passage into a universe now (the mind tour's pictures of it). */
    public static void play(int universe) {
        age = 0.0F;
        colour = colourOf(universe);
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            last = null;
            return;
        }
        int universe = Multiverse.universeAt(mc.level, mc.player.blockPosition());
        if (last != null && last != universe && lastLevel != null) {
            age = 0.0F;
            colour = colourOf(universe);
        }
        last = universe;
        lastLevel = mc.level;
        if (age >= DURATION) {
            return;
        }
        age += deltaTracker.getRealtimeDeltaTicks();
        if (mc.options.hideGui) {
            return;
        }
        draw(graphics, Math.min(1.0F, age / DURATION));
    }

    /** The colour of a universe's passage. */
    static int colourOf(int universe) {
        if (universe == Multiverse.GAMMA) {
            return 0x6FE04A;
        }
        if (universe == Multiverse.HYDRA) {
            return 0xD8312A;
        }
        if (universe == Multiverse.ULTRON) {
            return 0xB83A3A;
        }
        if (universe == Multiverse.ZOMBIES) {
            return 0x9CB06A;
        }
        if (universe == Multiverse.XMEN) {
            return 0xF2C230;
        }
        if (universe == Multiverse.MAIN) {
            return 0x7FD0FF;
        }
        int fog = UniverseRecipe.of(universe).fog();
        return fog < 0 ? 0x4FB8FF : fog;
    }

    private void draw(GuiGraphics graphics, float p) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        float cx = w / 2.0F;
        float cy = h / 2.0F;
        float reach = (float) Math.sqrt(cx * cx + cy * cy);
        // The tunnel: dark at first, opening onto the new world.
        float open = smooth(Mth.clamp((p - 0.35F) / 0.65F, 0.0F, 1.0F));
        float dark = 0.92F * (1.0F - open);
        graphics.fill(0, 0, w, h, argb(dark, 0x05060C));
        // Its walls, in the universe's colour, closing in at the edges.
        int edge = argb(0.55F * (1.0F - open), colour);
        graphics.fillGradient(0, 0, w, h / 4, edge, argb(0.0F, colour));
        graphics.fillGradient(0, h - h / 4, w, h, argb(0.0F, colour), edge);
        float fade = 1.0F - open;
        float speed = 2.6F - 1.6F * p;
        // The streaks: light flying past along every way out of the middle.
        for (int i = 0; i < STREAKS; i++) {
            float angle = i * GOLDEN;
            float phase = (i * 0.618034F) % 1.0F;
            float t = (p * speed * 3.0F + phase) % 1.0F;
            float r = t * t * reach;
            float size = 1.0F + t * 4.0F;
            float alpha = fade * Math.min(1.0F, t * 3.0F);
            int c = FastColor.ARGB32.lerp(t, 0xFFFFFF, colour);
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            for (int k = 0; k < 3; k++) {
                float rr = r * (1.0F - k * 0.07F);
                dot(graphics, cx + cos * rr, cy + sin * rr, size * (1.0F - k * 0.25F), argb(alpha * (1.0F - k * 0.3F), c));
            }
        }
        // The rings: the passage's walls turning round him, swept outwards.
        for (int ring = 0; ring < RINGS; ring++) {
            float t = (p * 1.8F + ring / (float) RINGS) % 1.0F;
            float r = (0.08F + t * t) * reach;
            float spin = (ring % 2 == 0 ? 1 : -1) * p * 9.0F + ring;
            float alpha = fade * (1.0F - t) * 0.9F;
            int c = FastColor.ARGB32.lerp(0.35F, colour, 0xFFFFFF);
            for (int d = 0; d < RING_DOTS; d++) {
                float a = spin + d * Mth.TWO_PI / RING_DOTS;
                dot(graphics, cx + Mth.cos(a) * r, cy + Mth.sin(a) * r * 0.82F, 2.0F + t * 3.0F, argb(alpha, c));
            }
        }
        // The far end of the tunnel: a light that grows, and opens.
        float core = (0.04F + 0.5F * smooth(p)) * Math.min(w, h);
        int glow = argb(0.9F * (1.0F - p), FastColor.ARGB32.lerp(0.5F, 0xFFFFFF, colour));
        graphics.fill((int) (cx - core * 0.5F), (int) (cy - core * 0.5F), (int) (cx + core * 0.5F), (int) (cy + core * 0.5F), argb(0.3F * (1.0F - p), colour));
        float inner = core * 0.35F;
        graphics.fill((int) (cx - inner), (int) (cy - inner), (int) (cx + inner), (int) (cy + inner), glow);
        // A flash as he comes out.
        if (p < 0.12F) {
            graphics.fill(0, 0, w, h, argb((0.12F - p) / 0.12F * 0.8F, 0xFFFFFF));
        }
    }

    private static void dot(GuiGraphics graphics, float x, float y, float size, int color) {
        int half = Math.max(1, (int) (size * 0.5F));
        graphics.fill((int) x - half, (int) y - half, (int) x + half, (int) y + half, color);
    }

    private static int argb(float alpha, int rgb) {
        return (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | rgb & 0xFFFFFF;
    }

    private static float smooth(float x) {
        return x * x * (3.0F - 2.0F * x);
    }
}
