package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * How a hero's energy shield is drawn, and the numbers the drawing needs from the hero: the colours of its veil and its
 * lines, the lattice texture, the radius of the sphere, how strong a fresh shield is and how long it can be held (read
 * when drawn, since they are server options). The Chaos Shield of the Scarlet Witch is the first; the heroes that raise
 * a shield of their own reuse the renderer and the HUD line by saying theirs ({@link HeroClient#shieldStyle}). The
 * shield itself is the one in {@code PowerData} ({@code chaosShieldActive}, {@code shieldFormedAt}, {@code
 * shieldStrength}).
 *
 * @param veil        the colour of the see-through body of the dome
 * @param lines       the colour of the lattice lines drawn on it
 * @param texture     the lattice texture
 * @param radius      the radius of the finished sphere, in blocks
 * @param maxStrength the strength a fresh shield has, in points (what the client shows as full)
 * @param maxTicks    how long a complete shield can be held, in ticks
 */
public record ShieldStyle(int veil, int lines, ResourceLocation texture, float radius, Supplier<Float> maxStrength, IntSupplier maxTicks) {

    /** What one frame of the shield looks like. */
    public record Look(float radius, float flicker) {
    }

    /**
     * The sphere of this player's shield at this moment: it grows from the hands to its full size while it forms (and
     * is fainter while it does), and close to shattering, or to the end of its time, it gutters.
     *
     * @param time the game time with the partial tick
     */
    public Look look(PowerData data, float time) {
        float formed = data.chaosShieldProgress(time);
        float eased = formed * formed * (3F - 2F * formed);
        float size = radius * (0.2F + 0.8F * eased);
        float flicker = (0.9F + 0.1F * Mth.sin(time * 0.6F)) * (0.4F + 0.6F * eased);
        float max = maxStrength.get();
        // (An unlimited shield, with an endless strength and time, never gutters.)
        float weak = Float.isInfinite(max) ? 0F : 1F - Mth.clamp(data.shieldStrength() / max / 0.4F, 0F, 1F);
        float ending = Mth.clamp((20F - (data.shieldFormedAt() + maxTicks.getAsInt() - time)) / 20F, 0F, 1F);
        float failing = formed < 1F ? 0F : Math.max(weak, ending);
        flicker *= 1F - failing * 0.5F * (0.5F + 0.5F * Mth.sin(time * 2.2F));
        return new Look(size, flicker);
    }
}
