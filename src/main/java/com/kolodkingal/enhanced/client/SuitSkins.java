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
 * "your own skin"). Loki can also wear another player's skin, or turn his into a Frost Giant's. Hooked into
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

    /**
     * 3.17, Ultimate Mode: the Demiurge at rest (the starry suit under his red scarf) and powered (the body of stars). No
     * cape of the game's: his cloak is a cloth of its own ({@code render.DemiurgeCloakLayer}).
     */
    private static final PlayerSkin DEMIURGE = new PlayerSkin(
            Enhanced.id("textures/entity/suit/billy_demiurge.png"), null, null, null, PlayerSkin.Model.WIDE, true);
    private static final PlayerSkin DEMIURGE_POWERED = new PlayerSkin(
            Enhanced.id("textures/entity/suit/billy_demiurge_powered.png"), null, null, null, PlayerSkin.Model.WIDE, true);
    /** 3.17, Ultimate Mode: the Nexus Being's suit, sleek red and black, with its red cape. */
    private static final PlayerSkin NEXUS = new PlayerSkin(
            Enhanced.id("textures/entity/suit/scarlet_witch_nexus.png"), null,
            Enhanced.id("textures/entity/suit/scarlet_witch_nexus_cape.png"), null, PlayerSkin.Model.SLIM, true);
    /** The same suit while she does not wear her crown: her own crown is only ever the one she puts on. */
    private static final PlayerSkin NEXUS_PLAIN = new PlayerSkin(
            Enhanced.id("textures/entity/suit/scarlet_witch_nexus_plain.png"), null,
            Enhanced.id("textures/entity/suit/scarlet_witch_nexus_cape.png"), null, PlayerSkin.Model.SLIM, true);

    /** 3.17, Ultimate Mode: Source Code Vision while he burns (flying, or a power in use): a figure of molten red. */
    private static final PlayerSkin VISION_MOLTEN = new PlayerSkin(
            Enhanced.id("textures/entity/suit/vision_molten.png"), null, null, null, PlayerSkin.Model.WIDE, true);

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
        // 3.17: the Demiurge (Billy, Ultimate Mode) and the Nexus Being (Wanda's ultimate form) wear theirs
        if (power == Power.BILLY && com.kolodkingal.enhanced.magic.Magic.isDemiurge(player)) {
            return com.kolodkingal.enhanced.client.magic.DemiurgeFx.powered(player) ? DEMIURGE_POWERED : DEMIURGE;
        }
        if (power == Power.VISION && !Powers.get(player).disguise()
                && com.kolodkingal.enhanced.client.render.VisionFlameFx.burning(player, Powers.get(player))) {
            return VISION_MOLTEN;
        }
        if (power == Power.SCARLET_WITCH && com.kolodkingal.enhanced.ultimate.NexusBeing.is(player)) {
            return WandaAbilities.wearsCrown(player) ? NEXUS : NEXUS_PLAIN;
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
        if (power == Power.CUSTOM) {
            // 3.3: a custom hero wears his own PNG, a look of one of the mod's heroes, or his own skin. 3.4: in Vision's
            // disguise (his ability) he looks like himself: the player's own skin.
            if (Powers.get(player).has(HeroAbility.VISION_DISGUISE) && Powers.get(player).disguise()) {
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
