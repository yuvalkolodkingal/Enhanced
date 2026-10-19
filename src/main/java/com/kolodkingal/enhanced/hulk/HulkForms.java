package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/**
 * The Hulk's forms (3.6): one hero, two states. The looks {@code banner} (Bruce Banner) and {@code own} (the player's
 * own skin as Banner) are human (no powers at all); {@code ragnarok} and {@code brand_new_day} are the two Hulks. A
 * custom hero with {@code hulk.transform} is human while his {@link Toggle#HUMAN_FORM} is on. The 3.6 skeleton writes
 * these questions (the stones' Gauntlet rule and the armour asks them); lane L7 owns the class and writes the change
 * itself ({@code change(player, toLook)}, the transformation).
 */
public final class HulkForms {
    /** The looks of the Hulk that are human. */
    public static final Set<String> HUMAN_LOOKS = Set.of("banner", "own");

    /** Whether the player is the Hulk (or a custom hero with his Transform) in a human form right now. Both sides. */
    public static boolean isHuman(Player player) {
        return isHuman(Powers.get(player));
    }

    /** Whether this power data is a human form of the Hulk's. */
    public static boolean isHuman(PowerData data) {
        if (data.power() == Power.HULK) {
            return HUMAN_LOOKS.contains(data.variant());
        }
        return data.power() == Power.CUSTOM && data.toggle(Toggle.HUMAN_FORM);
    }

    private HulkForms() {
    }
}
