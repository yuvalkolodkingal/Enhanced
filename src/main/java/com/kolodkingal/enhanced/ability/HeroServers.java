package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * The registry of {@link HeroServer}s: the heroes of 3.0 (the heroes before them are still routed by the {@code switch}
 * statements of {@link AbilityHandler} and {@link PowerTicker}). The shared code calls the registry once; a hero is
 * added here with one line, and everything else about it is in its own class.
 */
public final class HeroServers {
    private static final Map<Power, HeroServer> BY_POWER = new EnumMap<>(Power.class);

    static {
        register(new PhoenixServer());
        register(new SpiderServer());
        register(new ThanosServer());
        register(new UltronServer());
        register(new ApocalypseServer());
        // 3.3
        register(new CyclopsServer());
        // 3.3: the custom hero of the Hero Creator.
        register(new com.kolodkingal.enhanced.custom.CustomHeroServer());
        register(new XavierServer());
    }

    /**
     * Registers a plug-in for the power it says it is the server side of, in place of the one there was. Done by the
     * static block above, once per hero when the mod loads; a test may put in a plug-in of its own and, when it ends,
     * the one it replaced.
     *
     * @return the plug-in that was registered before, or null
     */
    @Nullable
    public static HeroServer register(HeroServer server) {
        return BY_POWER.put(server.power(), server);
    }

    /** The plug-in of a hero, or null for the heroes that are not plug-ins (and for {@link Power#NONE}). */
    @Nullable
    public static HeroServer of(Power power) {
        return BY_POWER.get(power);
    }

    /** Every registered hero. */
    public static Collection<HeroServer> all() {
        return BY_POWER.values();
    }

    /** The hero's key, if the power is a plug-in (called for the player's single power only). */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        HeroServer server = of(data.power());
        if (server != null) {
            server.onKey(player, data, ability, down);
        }
    }

    static void tick(ServerPlayer player, PowerData data) {
        HeroServer server = of(data.power());
        if (server != null) {
            server.tick(player, data);
        }
    }

    static void everyPlayerTick(ServerPlayer player, PowerData data) {
        for (HeroServer server : BY_POWER.values()) {
            server.everyPlayerTick(player, data);
        }
    }

    static void applyBody(ServerPlayer player, PowerData data) {
        for (HeroServer server : BY_POWER.values()) {
            server.applyBody(player, data);
        }
    }

    static void endAll(ServerPlayer player, PowerData data) {
        HeroServer server = of(data.power());
        if (server != null) {
            server.endAll(player, data);
        }
    }

    static void stopHold(PowerData data) {
        for (HeroServer server : BY_POWER.values()) {
            server.stopHold(data);
        }
    }

    private HeroServers() {
    }
}
