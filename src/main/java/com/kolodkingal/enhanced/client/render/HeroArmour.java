package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Heroes are drawn without their armour: helmet, chestplate, leggings and boots, with their trims, dye and glint, only
 * hide the characters that were drawn with such care. The armour still protects; it is just not drawn, on the player's
 * own body in third person, on everyone else's, in the inventory preview and on a Loki illusion (a copy of him). The
 * client option {@code hideHeroArmour} turns this off.
 *
 * <p>What is left drawn: the elytra (flight gear, drawn by a layer of its own), a carved pumpkin or a mob head (a
 * costume the player chose to put on, not armour: another layer draws them) and the gear of this mod itself (Magneto's
 * Helmet, Wanda's Crown, Quicksilver's Goggles: they are part of the hero's costume, and they are what puts the suit
 * on). Powerless players look as they always did.
 *
 * <p>Hooked into the armour layer by {@code HumanoidArmorLayerMixin}, one piece at a time.
 */
public final class HeroArmour {

    /** Whether the armour {@code entity} wears in {@code slot} is left undrawn. */
    public static boolean hidden(LivingEntity entity, EquipmentSlot slot) {
        if (!EnhancedClientConfig.hideHeroArmour() || !isHero(entity)) {
            return false;
        }
        return !isHeroGear(entity.getItemBySlot(slot));
    }

    /** Whether the entity is a hero, or an illusion of one (Loki's); 3.6: not the Hulk while he is Bruce Banner. */
    private static boolean isHero(LivingEntity entity) {
        return entity instanceof LokiIllusionEntity
                || (entity instanceof AbstractClientPlayer player && Powers.power(player) != Power.NONE
                && !com.kolodkingal.enhanced.hulk.HulkForms.isHuman(player));
    }

    /** What this mod makes to be worn by its heroes: the helmet, the crown and the goggles. */
    private static boolean isHeroGear(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Enhanced.MODID);
    }

    private HeroArmour() {
    }
}
