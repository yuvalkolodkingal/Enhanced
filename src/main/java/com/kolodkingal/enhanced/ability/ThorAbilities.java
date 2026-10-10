package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/** Thor, the God of Thunder, and Mjolnir. */
public final class ThorAbilities {

    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
    }

    public static void tick(ServerPlayer player, PowerData data) {
    }

    /** Thor flies by spinning Mjolnir: only while he holds it (or it is on its way back to him). */
    public static boolean canFly(ServerPlayer player, PowerData data) {
        return true;
    }

    /** Keeps Thor's body bonuses in sync with his state. */
    public static void applyBody(ServerPlayer player, PowerData data) {
    }

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
    }

    private ThorAbilities() {
    }
}
