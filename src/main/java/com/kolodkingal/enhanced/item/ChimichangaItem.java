package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A chimichanga (3.7): a food anyone can eat (8 food, 0.8 saturation; shapeless: bread, a cooked filling of {@code
 * #enhanced:chimichanga_fillings} and a carrot make two). It crunches as it is eaten, and Deadpool sighs happily
 * (a line of his, now and then). His Chimichanga Break eats one from his bag for a bigger heal ({@code
 * deadpool/Taunt}); "Choose a hero → Deadpool" gives three.
 */
public class ChimichangaItem extends Item {
    public ChimichangaItem(Properties properties) {
        super(properties);
    }

    @Override
    public SoundEvent getEatingSound() {
        return ModSounds.CHIMICHANGA_MUNCH.get();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.CHIMICHANGA_MUNCH.get(),
                    SoundSource.PLAYERS, 0.8F, 1.15F);
            if (entity instanceof ServerPlayer player
                    && com.kolodkingal.enhanced.power.Powers.get(player).power()
                    == com.kolodkingal.enhanced.power.Power.DEADPOOL) {
                com.kolodkingal.enhanced.deadpool.DeadpoolLines.say(player,
                        com.kolodkingal.enhanced.deadpool.DeadpoolLines.Group.CHIMICHANGA, false);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.chimichanga.desc").withStyle(ChatFormatting.GRAY,
                ChatFormatting.ITALIC));
    }
}
