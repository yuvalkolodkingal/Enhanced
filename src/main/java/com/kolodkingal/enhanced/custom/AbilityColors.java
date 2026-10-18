package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The colour of each built-in hero's ability on a custom hero (3.5, the creator lane): every pick has its own colour
 * ({@link CustomHero.Pick#color}, -1 for "the hero's"), and every effect of a hero's ability (beams, bolts, shields,
 * domes, barriers, auras, particles, trails, projectiles, webs, portals, lightning, flames, glowing eyes, the HUD, the
 * Hex) asks here for the colour it is drawn in, naming the row it belongs to and the colour the built-in hero draws it
 * in. Built-in heroes, and a custom hero whose pick says "the hero's colour", keep exactly what they had: only a colour
 * the player chose changes anything. Both sides read the same synced definition, so what the server sends (particles,
 * beam styles) and what a client draws agree for every player who sees him.
 *
 * <p>A palette of several colours (a crimson glow, a pink core, a white flare) is recoloured as a whole by
 * {@link #shade}: each colour keeps its own lightness and takes the chosen hue and saturation, so a core stays a core
 * and white stays white.
 */
public final class AbilityColors {

    /**
     * The colour the custom hero chose for {@code row}'s effects, or -1 when there is none: a built-in hero (or no
     * hero), a pick at "the hero's colour", or a row he has not picked. A variant row he has not picked takes its
     * parent's pick (the parent's card holds both halves), and a parent he has not picked its variant row's (the
     * variant's code is the parent's).
     */
    public static int chosen(@Nullable Entity entity, HeroAbility row) {
        if (!(entity instanceof Player player)) {
            return -1;
        }
        PowerData data = Powers.get(player);
        if (data.power() != Power.CUSTOM) {
            return -1;
        }
        return chosen(CustomHeroes.hero(player), row);
    }

    /** {@link #chosen(Entity, HeroAbility)} of a definition. */
    public static int chosen(CustomHero hero, HeroAbility row) {
        CustomHero.Pick pick = hero.pick(row);
        if (pick == null && row.parent() != null) {
            pick = hero.pick(row.parent());
        }
        if (pick == null && row.variant() != null) {
            pick = hero.pick(row.variant());
        }
        return pick == null || pick.color() < 0 ? -1 : pick.color() & 0xFFFFFF;
    }

    /** The colour of {@code row}'s effect: the one chosen, else {@code builtIn} (0xRRGGBB). */
    public static int color(@Nullable Entity entity, HeroAbility row, int builtIn) {
        int c = chosen(entity, row);
        return c < 0 ? builtIn : c;
    }

    /** The glow (a bright core) of {@code row}'s effect: the chosen colour made bright, else {@code builtIn}. */
    public static int glow(@Nullable Entity entity, HeroAbility row, int builtIn) {
        int c = chosen(entity, row);
        return c < 0 ? builtIn : glowOf(c);
    }

    /**
     * One colour of a palette of {@code row}'s effect, recoloured to the chosen colour ({@link #recolor}), else as it
     * is.
     */
    public static int shade(@Nullable Entity entity, HeroAbility row, int builtIn) {
        int c = chosen(entity, row);
        return c < 0 ? builtIn : recolor(builtIn, c);
    }

    /** {@link #shade} for an ARGB colour (its alpha kept). */
    public static int shadeArgb(@Nullable Entity entity, HeroAbility row, int argb) {
        int c = chosen(entity, row);
        return c < 0 ? argb : argb & 0xFF000000 | recolor(argb & 0xFFFFFF, c);
    }

    /**
     * The vertex tint (0xRRGGBB, white when nothing was chosen) that turns a grey or white texture into the chosen
     * colour.
     */
    public static int tint(@Nullable Entity entity, HeroAbility row) {
        int c = chosen(entity, row);
        return c < 0 ? 0xFFFFFF : c;
    }

    /** The first of {@code rows} the player has ({@code PowerData.has}), or the last of them when he has none. */
    public static HeroAbility first(@Nullable Entity entity, HeroAbility... rows) {
        if (entity instanceof Player player) {
            PowerData data = Powers.get(player);
            for (HeroAbility row : rows) {
                if (data.has(row)) {
                    return row;
                }
            }
        }
        return rows[rows.length - 1];
    }

    /**
     * The shield ability of a player (one at most: {@code Resource.SHIELD}), for the colour of what his shield does.
     */
    public static HeroAbility shield(@Nullable Entity entity) {
        return first(entity, HeroAbility.AGATHA_SHIELD, HeroAbility.APOCALYPSE_SHIELD, HeroAbility.ODIN_ODINFORCE,
                HeroAbility.BILLY_HEX_BARRIER, HeroAbility.WANDA_CHAOS_SHIELD);
    }

    /** The dye whose chips (an item particle) come nearest {@code rgb}: shards and chips in the chosen colour. */
    public static Item dye(int rgb) {
        Item best = Items.WHITE_DYE;
        double bestD = Double.MAX_VALUE;
        for (DyeColor d : DyeColor.values()) {
            int c = d.getTextureDiffuseColor() & 0xFFFFFF;
            double dr = ((c >> 16) & 0xFF) - ((rgb >> 16) & 0xFF);
            double dg = ((c >> 8) & 0xFF) - ((rgb >> 8) & 0xFF);
            double db = (c & 0xFF) - (rgb & 0xFF);
            double dist = dr * dr * 0.3 + dg * dg * 0.59 + db * db * 0.11;
            if (dist < bestD) {
                bestD = dist;
                best = DyeItem.byColor(d);
            }
        }
        return best;
    }

    /** {@code builtIn} as it is, or the dye nearest the colour chosen for {@code row}. */
    public static Item dye(@Nullable Entity entity, HeroAbility row, Item builtIn) {
        int c = chosen(entity, row);
        return c < 0 ? builtIn : dye(c);
    }

    /**
     * A row whose own effect has nothing coloured (a switch with only a stance, a message, a sound) shows the colour
     * chosen for it as a short burst of sparks round the player when it is used; nothing at all when no colour was
     * chosen (a built-in hero is unchanged).
     */
    public static void mark(ServerPlayer player, HeroAbility row) {
        int c = chosen(player, row);
        if (c >= 0) {
            player.serverLevel().sendParticles(new EnergyParticleOptions(c, 1.2F), player.getX(), player.getY()
                    + player.getBbHeight() * 0.55,
                    player.getZ(), 24, 0.35, 0.6, 0.35, 0.04);
        }
    }

    /** A colour's bright glow: two thirds of the way to white. */
    public static int glowOf(int color) {
        return CustomBeams.mix(color & 0xFFFFFF, 0xFFFFFF, 0.55F);
    }

    /**
     * {@code base} in the hue and saturation of {@code target}, its lightness kept but moved as far as the target is
     * lighter or darker than a middle tone (so a near-black choice darkens the whole palette, a near-white one pales
     * it), and a third of the way to the target's (so a pale palette still reads as the chosen colour). A white or near-white core stays as it is (the hot centre of a beam or bolt), a grey target gives greys.
     */
    public static int recolor(int base, int target) {
        float[] b = hsl(base);
        float[] t = hsl(target);
        float l = b[2];
        if (t[2] <= 0.5F) {
            l = l * Math.max(0.15F, t[2] / 0.5F);
        } else {
            l = l + (1.0F - l) * (t[2] - 0.5F) * 2.0F;
        }
        float white = Math.max(0.0F, Math.min(1.0F, (b[2] - 0.85F) / 0.15F)) * (1.0F - b[1]);
        l = l + (t[2] - l) * 0.35F;
        l = l + (b[2] - l) * white;
        return rgb(t[0], t[1] * (1.0F - white), Math.max(0.0F, Math.min(1.0F, l)));
    }

    /** Hue (0-1), saturation and lightness of 0xRRGGBB. */
    public static float[] hsl(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255F;
        float g = ((rgb >> 8) & 0xFF) / 255F;
        float b = (rgb & 0xFF) / 255F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2F;
        float h = 0F;
        float s = 0F;
        if (max != min) {
            float d = max - min;
            s = l > 0.5F ? d / (2F - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6F : 0F);
            } else if (max == g) {
                h = (b - r) / d + 2F;
            } else {
                h = (r - g) / d + 4F;
            }
            h /= 6F;
        }
        return new float[]{h, s, l};
    }

    /** 0xRRGGBB of hue (0-1), saturation and lightness. */
    public static int rgb(float h, float s, float l) {
        float r;
        float g;
        float b;
        if (s <= 0F) {
            r = g = b = l;
        } else {
            float q = l < 0.5F ? l * (1F + s) : l + s - l * s;
            float p = 2F * l - q;
            r = hue(p, q, h + 1F / 3F);
            g = hue(p, q, h);
            b = hue(p, q, h - 1F / 3F);
        }
        return (Math.round(r * 255F) << 16) | (Math.round(g * 255F) << 8) | Math.round(b * 255F);
    }

    private static float hue(float p, float q, float t) {
        if (t < 0F) {
            t += 1F;
        }
        if (t > 1F) {
            t -= 1F;
        }
        if (t < 1F / 6F) {
            return p + (q - p) * 6F * t;
        }
        if (t < 0.5F) {
            return q;
        }
        if (t < 2F / 3F) {
            return p + (q - p) * (2F / 3F - t) * 6F;
        }
        return p;
    }

    private AbilityColors() {
    }
}
