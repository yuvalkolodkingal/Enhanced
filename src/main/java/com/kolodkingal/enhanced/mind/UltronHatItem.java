package com.kolodkingal.enhanced.mind;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ultron's gift to Vision (3.12), on the little table in Ultron's room: a hat, worn on the head. It is drawn on any
 * head, a hero's too (the heroes' own helmets and crowns give way to anything worn on the head).
 */
public class UltronHatItem extends Item
        //#if <1.21.4
        implements net.minecraft.world.item.Equipable
        //#endif
{
    public UltronHatItem(Properties properties) {
        super(properties);
    }

    //#if <1.21.4
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }
    //#endif

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.ultron_hat.desc").withStyle(ChatFormatting.GRAY));
    }
}
