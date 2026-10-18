package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.power.Power;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * The registry of {@link HeroClient}s: the heroes of 3.0 (the heroes before them are still routed by the {@code switch}
 * statements of the HUD, the eyes, the animator and the rest). The shared client code asks the registry; a hero is
 * added here with one line, and everything else about it is in its own class.
 */
public final class HeroClients {
    private static final Map<Power, HeroClient> BY_POWER = new EnumMap<>(Power.class);

    static {
        register(new PhoenixClient());
        register(new SpiderClient());
        register(new ThanosClient());
        register(new UltronClient());
        register(new ApocalypseClient());
        // 3.3
        register(new CyclopsClient());
        // 3.3: the custom hero of the Hero Creator.
        register(new com.kolodkingal.enhanced.client.custom.CustomClient());
        register(new XavierClient());
        register(new AgathaClient());
        // 3.5: the heroes of Asgard.
        register(new HeimdallClient());
        register(new OdinClient());
        register(new HelaClient());
    }

    /**
     * Registers a plug-in for the power it says it is the client side of, in place of the one there was. Done by the
     * static block above, once per hero when the client loads; a test may put in a plug-in of its own and, when it ends,
     * the one it replaced.
     *
     * @return the plug-in that was registered before, or null
     */
    @Nullable
    public static HeroClient register(HeroClient client) {
        return BY_POWER.put(client.power(), client);
    }

    /** The plug-in of a hero, or null for the heroes that are not plug-ins (and for {@link Power#NONE}). */
    @Nullable
    public static HeroClient of(Power power) {
        return BY_POWER.get(power);
    }

    /** Every registered hero. */
    public static Collection<HeroClient> all() {
        return BY_POWER.values();
    }

    /** The lean the stances of a hero of 3.0 add to the body, or 0. */
    static float lean(HeroAnimState st) {
        HeroClient hero = BY_POWER.get(st.power);
        return hero == null ? 0.0F : hero.lean(st);
    }

    /** The dip the stances of a hero of 3.0 add to the body, or 0. */
    static float drop(HeroAnimState st) {
        HeroClient hero = BY_POWER.get(st.power);
        return hero == null ? 0.0F : hero.drop(st);
    }

    /** Defines the gesture keys of every hero of 3.0 (called from the static block of {@link Gestures}). */
    static void registerGestures() {
        for (HeroClient hero : BY_POWER.values()) {
            Gestures.defining(hero);
            try {
                hero.registerGestures();
            } finally {
                Gestures.defining(null);
            }
        }
    }

    /** Once per client tick, with a world. */
    public static void tick(Minecraft mc) {
        for (HeroClient hero : BY_POWER.values()) {
            hero.tick(mc);
        }
    }

    /** The client left the world or changed dimension. */
    public static void clear() {
        for (HeroClient hero : BY_POWER.values()) {
            hero.clear();
        }
    }

    private HeroClients() {
    }
}
