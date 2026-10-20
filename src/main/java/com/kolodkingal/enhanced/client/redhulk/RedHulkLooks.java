package com.kolodkingal.enhanced.client.redhulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Red Hulk through the Hulk's renderer (3.8, PLAN 4.9): {@code HulkPlayerRenderer.drawsAsHulk} asks {@link
 * #drawsAsHulk} and its texture choice asks {@link #texture} first. Red Hulk in his look {@code red_hulk} (not Ross,
 * not the look {@code own}: his powers in the player's own skin on the player's model) is drawn on the Hulk's body
 * (the Hulk's model, poses and layers, his heat added: {@code HeatLayer}) in his own 128 x 128 skin on the Hulk's
 * atlas: red, darker red in the shadows of the muscles, Ross's grey-white swept-back hair and grey moustache,
 * yellow-orange eyes, black torn trousers and a dark belt, bare feet ({@code SCRATCH/v38/art/l2/gen_red_hulk.py}).
 * Both sides may ask.
 */
public final class RedHulkLooks {
    /** His skin on the Hulk's atlas (HulkModel's boxes). */
    public static final ResourceLocation SKIN = Enhanced.id("textures/entity/red_hulk/red_hulk.png");
    /** His look drawn on the Hulk's body. */
    public static final String LOOK = "red_hulk";

    /** Whether this player is drawn by the Hulk's renderer as Red Hulk: the hero, not Ross, in his own look. */
    public static boolean drawsAsHulk(PowerData data) {
        return data.power() == Power.RED_HULK && !data.toggle(Toggle.ROSS_FORM)
                && LOOK.equals(Variants.current(data).id());
    }

    /** Red Hulk's skin for the Hulk's renderer, or null (the Hulk's own choice). */
    @Nullable
    public static ResourceLocation texture(Player player) {
        return drawsAsHulk(Powers.get(player)) ? SKIN : null;
    }

    private RedHulkLooks() {
    }
}
