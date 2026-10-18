package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The seconds of history the local Thanos's Rewind could go back into, as the client reckons them: the server's
 * snapshots are not synced, so the HUD counts the ticks since the last thing that clears them (a new world, a death, a
 * Rewind). It is a class of its own, apart from {@link ThanosClient}, so that the plug-in (which a dedicated server
 * also loads when it lists the heroes) names no client class in what it does.
 */
final class ThanosHistory {
    private static int ticks;
    @Nullable
    private static LocalPlayer counted;
    private static long rewindUntilSeen;

    /** The history, in seconds. */
    static double seconds() {
        return ticks / 20.0;
    }

    /** Whether there is enough of it for a Rewind to be worth the cooldown (two seconds). */
    static boolean enough() {
        return ticks >= 40;
    }

    /** Counts a tick: the history grows while he lives, and starts over with a new world, a death or a Rewind. */
    static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !Powers.get(player).has(HeroAbility.THANOS_TIME)) {
            ticks = 0;
            counted = null;
            return;
        }
        PowerData data = Powers.get(player);
        long now = mc.level.getGameTime();
        int slot = data.slot(HeroAbility.THANOS_TIME);
        long rewindUntil = slot == 0 ? 0L : data.cooldownUntil(slot);
        // A cooldown that has just begun with the Rewind's length is a Rewind (the Time Lock's is shorter). (3.4: a custom
        // hero's is as long as his Time Stone's strength makes it.)
        double factor = data.kit() == null ? 1.0 : data.kit().cooldownFactor(HeroAbility.THANOS_TIME) * CustomHeroConfig.COOLDOWN_SCALE.get();
        boolean rewound = rewindUntil > rewindUntilSeen && rewindUntil - now > (ThanosConfig.REWIND_COOLDOWN.get() - 10) * factor;
        rewindUntilSeen = rewindUntil;
        if (counted != player || player.isDeadOrDying() || rewound) {
            ticks = 0;
            counted = player;
        }
        ticks = Math.min(ticks + 1, ThanosConfig.REWIND_SECONDS.get() * 20);
    }

    static void clear() {
        ticks = 0;
        counted = null;
        rewindUntilSeen = 0L;
    }

    private ThanosHistory() {
    }
}
