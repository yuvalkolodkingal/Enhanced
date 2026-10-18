package com.kolodkingal.enhanced.protect;

import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
import net.minecraft.world.entity.player.Player;

/**
 * Who is a sorcerer for the Hex Runes (3.3): whoever the runes on the walls of Wanda's Hex silence. Loki, Billy, the
 * Scarlet Witch (another Scarlet Witch: the owner of the Hex keeps her powers, {@link HexRunes} asks that apart), and anyone
 * who holds the Darkhold or the Book of the Vishanti in a hand. A new sorcerer joins in {@link #isSorcerer} alone (one line:
 * {@code || power == Power.AGATHA}).
 *
 * <p>The Book of the Vishanti has its own, older list of who may read it ({@code WardStrength.isSorcerer}).
 */
public final class Sorcery {

    /** Whether the player is a sorcerer now: by power, or by a book of magic held in either hand. Both sides. */
    public static boolean isSorcerer(Player player) {
        Power power = Powers.power(player);
        return power == Power.LOKI || power == Power.BILLY || power == Power.SCARLET_WITCH || power == Power.AGATHA
                || DarkholdBook.holds(player) || VishantiBook.holds(player);
    }

    private Sorcery() {
    }
}
