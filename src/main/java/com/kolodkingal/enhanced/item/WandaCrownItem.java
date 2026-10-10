package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wanda's crown. On the Scarlet Witch it transforms her (suit, 40 max health, Diamond-level protection); on anyone
 * else it is just a crown. The bonuses are applied in {@link com.kolodkingal.enhanced.ability.WandaAbilities}.
 */
public class WandaCrownItem extends ArmorItem {
    public WandaCrownItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    /** An all-transparent armor texture: the transformed Scarlet Witch's skin already draws her crown. */
    private static final ResourceLocation HIDDEN = Enhanced.id("textures/models/armor/hidden.png");

    /** On the Scarlet Witch the suit's own crown shows; on anyone else the crown model does. */
    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return entity instanceof Player player && Powers.is(player, Power.SCARLET_WITCH) ? HIDDEN : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.wanda_crown.desc").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.enhanced.wanda_crown.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
