package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.world.entity.player.Player;

/**
 * The layers of a custom hero's look (3.4) as the client draws them: which of the five are on now (their "when"), their
 * colours, the tinted textures. S0 gives the entry; the looks lane (L6) fills it.
 */
public final class CustomLooks {

    /** The layers of the player's look, or none when he is not a custom hero. */
    public static CustomHero.Layers of(Player player) {
        return Powers.power(player) == Power.CUSTOM ? CustomHeroes.hero(player).layers() : CustomHero.Layers.NONE_ALL;
    }

    /** Whether a layer shows now (its "when": always, while a power is on, while flying). */
    public static boolean shows(Player player, CustomHero.Layer layer) {
        return !layer.isNone();
    }

    private CustomLooks() {
    }
}
