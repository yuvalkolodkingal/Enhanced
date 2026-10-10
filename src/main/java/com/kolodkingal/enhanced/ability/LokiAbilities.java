package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.server.level.ServerPlayer;

/** Loki, the God of Mischief, and his variants. */
public final class LokiAbilities {

    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
    }

    public static void tick(ServerPlayer player, PowerData data) {
    }

    /** Keeps Loki's body in step with his Frost Giant form and his shapeshifted form. */
    public static void applyForm(ServerPlayer player, PowerData data) {
    }

    /** Ends whatever the current look's signature ability left in the world (the look is changing). */
    public static void endLook(ServerPlayer player, PowerData data) {
    }

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
    }

    private LokiAbilities() {
    }
}
