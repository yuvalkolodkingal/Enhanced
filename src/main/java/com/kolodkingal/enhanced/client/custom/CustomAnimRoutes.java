package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.Stance;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.PowerData;
import org.jetbrains.annotations.Nullable;

/**
 * What a custom hero's bindings play (3.4): the gesture of a cast marker ({@code CustomState.castSeq}) and the stance of a
 * held or switched-on power. Pure (no client state), so it can be tested. S0 gives the signatures; the animation lane
 * (L5) fills them from the binding's move ({@link CustomHero#move}), the hero abilities' own gestures
 * ({@link HeroAbilityAnims}) and the generic modules' defaults (Appendix A of the plan).
 */
public final class CustomAnimRoutes {

    /**
     * The gesture binding ({@code column} 1-5, plain or with Sneak) plays when it acts, or null: none, or the hero
     * ability's own route plays it (a flag, a toggle, a live state).
     */
    @Nullable
    public static GestureId cast(CustomHero hero, HeroKit kit, int column, boolean sneak) {
        return null;
    }

    /** The default gesture of a generic module's option ({@code on}: a switch going on, else off), or null. */
    @Nullable
    public static GestureId defaultGesture(PowerModule module, String option, boolean on) {
        return null;
    }

    /** The default stance of a generic module's option while it is held or on, or null. */
    @Nullable
    public static Stance defaultStance(PowerModule module, String option) {
        return null;
    }

    /** Whether a generic pick is held or on now (its stance is up). */
    public static boolean isActive(CustomHero.Pick pick, PowerData data, CustomState state) {
        return false;
    }

    private CustomAnimRoutes() {
    }
}
