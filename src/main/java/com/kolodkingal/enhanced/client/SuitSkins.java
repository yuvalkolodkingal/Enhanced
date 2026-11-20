package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.custom.HeroAbility;
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
 * "your own skin", or the player chose it for any hero: {@link com.kolodkingal.enhanced.power.PowerData#ownSkin}). Loki can also wear another player's skin, or turn his into a Frost Giant's. Hooked into
 * {@link AbstractClientPlayer#getSkin()} by a mixin, so it applies everywhere the skin is drawn: the body, first-person
 * arms and the cape (and Loki's illusions, which copy his skin).
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

    /** (3.18) Cosmic Spider-Man: a starry navy suit with blue web lines, big white eyes and a white spider. */
    private static final PlayerSkin COSMIC_SPIDER = new PlayerSkin(
            Enhanced.id("textures/entity/suit/spider_cosmic.png"), null, null, null, PlayerSkin.Model.WIDE, true);

    /** (3.18) Cosmic Spider-Man as Captain Universe: a starry night head, a cosmic-blue body, glowing pale-cyan legs and hands. */
    private static final PlayerSkin COSMIC_UNIVERSE = new PlayerSkin(
            Enhanced.id("textures/entity/suit/spider_cosmic_universe.png"), null, null, null, PlayerSkin.Model.WIDE, true);

    private static final PlayerSkin JIM_HUMAN = new PlayerSkin(
            Enhanced.id("textures/entity/suit/jim_human.png"), null, null, null, PlayerSkin.Model.WIDE, true);

    private static final PlayerSkin VISION_HUMAN = new PlayerSkin(
            Enhanced.id("textures/entity/suit/vision_human.png"), null, null, null, PlayerSkin.Model.WIDE, true);

    /**
     * The skin the player is drawn with, given their {@code own}: their suit (or their own skin), then Loki's tricks on
     * top of it: another player's skin while he wears their shape, or his look turned Frost Giant.
     */
    public static PlayerSkin skinFor(AbstractClientPlayer player, PlayerSkin own) {
        PlayerSkin suit = suitFor(player);
        PlayerSkin skin = suit != null ? suit : own;
        if (Shapeshifts.changesSkin(player)) {
            PlayerSkin copied = Shapeshifts.copiedSkin(player);
            if (copied != null) {
                return copied;
            }
            if (LokiAbilities.isFrostGiant(player)) {
                skin = FrostGiantSkins.of(skin);
            }
        }
        if (Powers.power(player) == Power.CUSTOM) {
            // 3.4: the cape of a custom hero's look, on whatever skin he wears.
            skin = com.kolodkingal.enhanced.client.custom.CustomLooks.withCape(player, skin);
        }
        return skin;
    }

    /** The suit the player is currently transformed into, or null to keep their own skin. */
    @Nullable
    public static PlayerSkin suitFor(AbstractClientPlayer player) {
        Power power = Powers.power(player);
        if (power == Power.SPIDER_MAN && com.kolodkingal.enhanced.ability.CosmicSpider.isCosmic(Powers.get(player))) {
            // (3.18) the Enigma Force turns his suit black and white, whatever his look or his own skin
            return com.kolodkingal.enhanced.ability.CosmicSpider.universe(Powers.get(player)) ? COSMIC_UNIVERSE : COSMIC_SPIDER;
        }
        if (power.hasPowers() && Powers.get(player).ownSkin()) {
            // (3.17: he chose his own skin: any hero, built-in or custom, wears it instead of the suit)
            return null;
        }
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
        if (power == Power.JIM_HAMMOND && Powers.get(player).disguise()) {
            // (3.17) in the human disguise Jim looks human
            return JIM_HUMAN;
        }
        if (power == Power.CUSTOM) {
            // 3.3: a custom hero wears his own PNG, a look of one of the mod's heroes, or his own skin. 3.4: in Vision's
            // disguise (his ability) he looks like himself: the player's own skin.
            if ((Powers.get(player).has(HeroAbility.VISION_DISGUISE) || Powers.get(player).has(HeroAbility.JIM_DISGUISE))
                    && Powers.get(player).disguise()) {
                return null;
            }
            // 3.6: in the Hulk's Human Form he is himself: the player's own skin.
            if (Powers.get(player).toggle(com.kolodkingal.enhanced.power.Toggle.HUMAN_FORM)) {
                return null;
            }
            return com.kolodkingal.enhanced.client.custom.CustomSkins.suitFor(player);
        }
        // (3.8: Red Hulk as Ross is himself, in his own skin; RossSuitLayer dresses him.)
        if (power == Power.RED_HULK && Powers.get(player).toggle(com.kolodkingal.enhanced.power.Toggle.ROSS_FORM)) {
            return null;
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
