package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Every hero of the mod and every look of each, for the Hero Creator: the heroes to choose from, and the skins a custom
 * hero may wear as its base. Built from {@link Power} and {@link Variants} each time it is asked, so a hero added to the
 * mod (and its looks) appears here without a line changing. Three heroes have no list of looks, only the suit their
 * helmet, crown or goggles put on (see {@code SuitSkins}): theirs is named here.
 */
public final class HeroLooks {
    /**
     * A look a custom hero may wear.
     *
     * @param power the hero
     * @param id    the look's id ({@code Variant.id}), "suit" for the single suit of the three heroes without looks
     * @param skin  its texture
     * @param slim  the slim arms
     */
    public record Look(Power power, String id, ResourceLocation skin, boolean slim) {
        /** How a definition names it ({@code thor/ragnarok}). */
        public String ref() {
            return power.getSerializedName() + "/" + id;
        }

        /** The lang key of the look's name (the hero's name for a single suit). */
        public String nameKey() {
            return SUIT.equals(id) ? "power.enhanced." + power.getSerializedName() : "variant.enhanced." + power.getSerializedName() + "." + id;
        }
    }

    /** The id of the single suit of a hero without looks. */
    public static final String SUIT = "suit";

    /** The heroes a player can choose in the creator: every power but none and the custom hero, in the order of {@link Power}. */
    public static List<Power> heroes() {
        List<Power> list = new ArrayList<>();
        for (Power power : Power.values()) {
            if (power.hasPowers() && power != Power.CUSTOM) {
                list.add(power);
            }
        }
        return list;
    }

    /** The looks of one hero that have a skin of their own (not "your own skin"). */
    public static List<Look> of(Power power) {
        List<Look> list = new ArrayList<>();
        for (Variant v : Variants.of(power)) {
            if (v.skin() != null) {
                list.add(new Look(power, v.id(), v.skin(), v.slim()));
            }
        }
        if (list.isEmpty()) {
            Look suit = singleSuit(power);
            if (suit != null) {
                list.add(suit);
            }
        }
        return list;
    }

    /** Every look of every hero. */
    public static List<Look> all() {
        List<Look> list = new ArrayList<>();
        for (Power power : heroes()) {
            list.addAll(of(power));
        }
        return list;
    }

    /** The look a definition names ({@code <power>/<id>}), or null. */
    @Nullable
    public static Look byRef(@Nullable String ref) {
        if (ref == null) {
            return null;
        }
        int slash = ref.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        Power power = Power.byName(ref.substring(0, slash));
        if (power == null || !power.hasPowers() || power == Power.CUSTOM) {
            return null;
        }
        String id = ref.substring(slash + 1);
        for (Look look : of(power)) {
            if (look.id().equals(id)) {
                return look;
            }
        }
        return null;
    }

    /** The suit of a hero that has no list of looks (Magneto, the Scarlet Witch, Quicksilver), or null. */
    @Nullable
    private static Look singleSuit(Power power) {
        return switch (power) {
            case MAGNETO -> new Look(power, SUIT, Enhanced.id("textures/entity/suit/magneto.png"), false);
            case SCARLET_WITCH -> new Look(power, SUIT, Enhanced.id("textures/entity/suit/scarlet_witch.png"), true);
            case QUICKSILVER -> new Look(power, SUIT, Enhanced.id("textures/entity/suit/quicksilver.png"), false);
            default -> null;
        };
    }

    private HeroLooks() {
    }
}
