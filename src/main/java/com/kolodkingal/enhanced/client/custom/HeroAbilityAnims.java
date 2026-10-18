package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Toggle;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * The animation of each built-in hero's ability (3.4), for a custom hero who has it: the gesture its main action and its
 * Sneak variant play when told by the cast marker (only those the hero acts by a cooldown), every gesture and stance the
 * ability may start, and which ability of a kit owns each synced bit (a {@link Live} state, a {@link Toggle}, a
 * {@link GestureRoutes.Flag}; the rules' resources make it unique). S0 gives the signatures; the animation lane (L5) fills
 * the tables from section 2.1 of the plan.
 */
public final class HeroAbilityAnims {

    /** The gesture the ability's main action plays on a cast marker, or null (its own route plays it). */
    @Nullable
    public static GestureId castMain(HeroAbility a) {
        return null;
    }

    /** The gesture the ability's Sneak variant plays on a cast marker, or null. */
    @Nullable
    public static GestureId castAlt(HeroAbility a) {
        return null;
    }

    /** Every gesture the ability may start. */
    public static Set<GestureId> gestures(HeroAbility a) {
        return Set.of();
    }

    /** Every stance the ability may raise. */
    public static Set<Stance> stances(HeroAbility a) {
        return Set.of();
    }

    /** The ability of the kit a live state belongs to, or null. */
    @Nullable
    public static HeroAbility ownerOf(Live live, HeroKit kit) {
        return null;
    }

    /** The ability of the kit a toggle belongs to, or null. */
    @Nullable
    public static HeroAbility ownerOf(Toggle toggle, HeroKit kit) {
        return null;
    }

    /** The ability of the kit a flag belongs to, or null. */
    @Nullable
    public static HeroAbility ownerOf(GestureRoutes.Flag flag, HeroKit kit) {
        return null;
    }

    /** Whether a gesture belongs to the kit (a row's, or a generic default or override in use): the kit filter. */
    public static boolean allows(HeroKit kit, GestureId gesture) {
        return true;
    }

    /** The stances the kit's abilities may raise (the filter of their heroes' stance targets). */
    public static Set<Stance> stancesOf(HeroKit kit) {
        return Set.of();
    }

    private HeroAbilityAnims() {
    }
}
