package com.kolodkingal.enhanced.power;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One look of a hero: the TVA outfit is a Loki, White Vision is a Vision. A variant has its own skin (and cape) and its
 * own energy colours; the hero's abilities are the same in every look.
 *
 * @param id    unique within the hero, saved with the player
 * @param skin  the suit texture, or null to keep the player's own skin
 * @param cape  the cape texture, or null for no cape
 * @param slim  the suit uses the slim (3-pixel) arms
 * @param color main energy colour (particles, beams)
 * @param glow  secondary glow colour
 */
public record Variant(String id, Power power, @Nullable ResourceLocation skin, @Nullable ResourceLocation cape, boolean slim,
                      int color, int glow) {

    /** A suit texture under textures/entity/suit/. */
    static ResourceLocation suit(String name) {
        return Enhanced.id("textures/entity/suit/" + name + ".png");
    }

    public String translationKey() {
        return "variant.enhanced." + power.getSerializedName() + "." + id;
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    public boolean ownSkin() {
        return skin == null;
    }
}
