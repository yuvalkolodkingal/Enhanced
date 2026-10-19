package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Set;

/**
 * Who has which role (3.4): the one question every list of heroes ("is he a sorcerer?", "a telepath?", "worthy?") became.
 * A built-in hero has the roles the old lists gave him ({@link #of(Power)}, unchanged); a custom hero those of the
 * abilities and modules he picked ({@link HeroKit#hasTrait}). Both sides.
 */
public final class Traits {

    /** Whether the player has the role now. */
    public static boolean has(Player player, Trait trait) {
        return has(Powers.get(player), trait);
    }

    /** Whether a player with this power data has the role. */
    public static boolean has(PowerData data, Trait trait) {
        if (data.power() == Power.CUSTOM) {
            HeroKit kit = data.kit();
            return kit != null && kit.hasTrait(trait);
        }
        return of(data.power()).contains(trait);
    }

    /**
     * The roles of a built-in hero, as the lists of 3.3 said: Loki, Billy, the Scarlet Witch and Agatha are sorcerers;
     * Professor X a telepath, the Scarlet Witch, the Phoenix and Billy stronger ones; Thor and Vision worthy; Vision
     * and Ultron metal; Agatha's magic dark; the Phoenix fire-proof. (The Iron Spider's legs go with his look, not his
     * power: {@code SpiderServer.isIronSpider}.) 3.6: Doctor Strange is a sorcerer; the gods of Asgard, Loki, the
     * Scarlet Witch and the Phoenix may hold a bare Infinity Stone ({@link Trait#STONE_BEARER}); the Hulk has no role.
     */
    public static Set<Trait> of(Power power) {
        EnumSet<Trait> traits = EnumSet.noneOf(Trait.class);
        switch (power) {
            case LOKI -> traits.addAll(Set.of(Trait.SORCERER, Trait.STONE_BEARER));
            case BILLY -> traits.addAll(Set.of(Trait.SORCERER, Trait.TELEPATH_STRONG));
            case SCARLET_WITCH -> traits.addAll(Set.of(Trait.SORCERER, Trait.TELEPATH_STRONG, Trait.STONE_BEARER));
            case AGATHA -> traits.addAll(Set.of(Trait.SORCERER, Trait.DARK_MAGIC));
            case PHOENIX -> traits.addAll(Set.of(Trait.TELEPATH_STRONG, Trait.FIRE_IMMUNE, Trait.STONE_BEARER));
            case PROFESSOR_X -> traits.add(Trait.TELEPATH);
            case THOR -> traits.addAll(Set.of(Trait.WORTHY, Trait.STONE_BEARER));
            case VISION -> traits.addAll(Set.of(Trait.WORTHY, Trait.METAL_BODY));
            case ULTRON -> traits.add(Trait.METAL_BODY);
            // 3.5: no other role (their lanes say: Hela's rows are dark magic, Odin's Odinforce a sorcerer's, see
            // HeroAbility); 3.6: the gods may hold a bare stone.
            case HEIMDALL, ODIN, HELA -> traits.add(Trait.STONE_BEARER);
            // 3.6: the Sorcerer Supreme (the Hex Runes hold him, he reads the Vishanti and the Darkhold as a sorcerer).
            case DOCTOR_STRANGE -> traits.add(Trait.SORCERER);
            // 3.6: the Hulk has no role: not a sorcerer, not a Bearer, his mind open to Xavier and Wanda.
            case HULK -> {
            }
            default -> {
            }
        }
        return traits;
    }

    private Traits() {
    }
}
