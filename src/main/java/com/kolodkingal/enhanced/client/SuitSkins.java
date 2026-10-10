package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The hero suits. While Magneto wears his helmet, the Scarlet Witch her crown or Quicksilver his goggles, their whole skin (and cape) is
 * replaced by the suit. Vision and the newer heroes always wear the suit of their current look (unless the look is
 * "your own skin"). Hooked into {@link AbstractClientPlayer#getSkin()} by a mixin, so it applies everywhere the
 * skin is drawn: the body, first-person arms and the cape.
 */
public final class SuitSkins {
    private static final PlayerSkin MAGNETO = new PlayerSkin(
            Enhanced.id("textures/entity/suit/magneto.png"), null,
            Enhanced.id("textures/entity/suit/magneto_cape.png"), null,
            PlayerSkin.Model.WIDE, true);

    private static final PlayerSkin SCARLET_WITCH = new PlayerSkin(
            Enhanced.id("textures/entity/suit/scarlet_witch.png"), null,
            Enhanced.id("textures/entity/suit/scarlet_witch_cape.png"), null,
            PlayerSkin.Model.SLIM, true);

    private static final PlayerSkin QUICKSILVER = new PlayerSkin(
            Enhanced.id("textures/entity/suit/quicksilver.png"), null, null, null, PlayerSkin.Model.WIDE, true);

    private static final PlayerSkin VISION_HUMAN = new PlayerSkin(
            Enhanced.id("textures/entity/suit/vision_human.png"), null, null, null, PlayerSkin.Model.WIDE, true);

    /** The suit the player is currently transformed into, or null to keep their own skin. */
    @Nullable
    public static PlayerSkin suitFor(AbstractClientPlayer player) {
        Power power = Powers.power(player);
        if (power == Power.MAGNETO && MagnetoAbilities.wearsHelmet(player)) {
            return MAGNETO;
        }
        if (power == Power.SCARLET_WITCH && WandaAbilities.wearsCrown(player)) {
            return SCARLET_WITCH;
        }
        if (power == Power.QUICKSILVER && QuicksilverAbilities.wearsGoggles(player)) {
            return QUICKSILVER;
        }
        if (power == Power.VISION && Powers.get(player).disguise()) {
            // In disguise Vision looks human (WandaVision).
            return VISION_HUMAN;
        }
        // Vision's body is synthetic, so he is always in his look; the newer heroes always wear theirs too.
        Variant look = Powers.variant(player);
        return look.ownSkin() ? null : skinOf(look);
    }

    private static final Map<Variant, PlayerSkin> BY_LOOK = new HashMap<>();

    private static PlayerSkin skinOf(Variant look) {
        return BY_LOOK.computeIfAbsent(look, v -> new PlayerSkin(v.skin(), null, v.cape(), null,
                v.slim() ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, true));
    }

    private SuitSkins() {
    }
}
