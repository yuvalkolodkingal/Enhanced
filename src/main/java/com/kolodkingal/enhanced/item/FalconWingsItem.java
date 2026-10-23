package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.FalconConfig;
import com.kolodkingal.enhanced.falcon.FalconWings;
import com.kolodkingal.enhanced.platform.ext.ItemExt;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/**
 * Falcon's Exo-Wings (3.8, PLAN 8.2): a chest-slot item (an iron chestplate's armour and a bit), an elytra for Falcon
 * ({@link FalconWings#canElytraFly}: a jump in the air, or Sneak + G, spreads them into a glide; they wear as an
 * elytra's), folded wings for anyone else. Its armour texture is empty: the wings are drawn by their own layer ({@code
 * client/falcon/FalconWingsLayer}). Their durability is {@code [falcon] wingsDurability} (900); iron ingots mend them
 * on an anvil.
 */
public class FalconWingsItem extends ArmorItem implements ItemExt.ElytraFlight, ItemExt.MaxDamage {
    public FalconWingsItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
    }


    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return FalconWings.canElytraFly(stack, entity);
    }


    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        // As an elytra wears: a point every second of gliding.
        if (!entity.level().isClientSide()) {
            int next = flightTicks + 1;
            if (next % 10 == 0) {
                if (next % 20 == 0) {
                    stack.hurtAndBreak(1, entity, EquipmentSlot.CHEST);
                }
                entity.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
        }
        return true;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        if (EnhancedConfig.SPEC.isLoaded() && FalconConfig.WINGS_DURABILITY != null) {
            return FalconConfig.WINGS_DURABILITY.get();
        }
        return super.getMaxDamage(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.falcon_exo_wings.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
