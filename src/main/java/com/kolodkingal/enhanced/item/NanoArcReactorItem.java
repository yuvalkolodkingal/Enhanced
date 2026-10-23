package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.ext.ItemExt;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Nano Arc Reactor (3.8, PLAN 5.2): the Infinity War housing, a rounded-triangle reactor in a dark hexagonal case,
 * a chest-slot item (no armour of its own: a deployed nanotech suit's armour is its own). Hold right-click for {@code
 * reactorHoldTicks}, or put it on the chest slot ("using or wearing it"), and its holder is Iron Man by the power-set
 * path; as Iron Man, putting it on changes nothing. On the chest it is the nanotech suits' home (H deploys the selected
 * one); taken off, a deployed suit retracts at once.
 */
public class NanoArcReactorItem extends ArmorItem implements ItemExt.Equip {
    public NanoArcReactorItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (Powers.power(player) == Power.IRON_MAN) {
            // (Iron Man puts it on his chest, as any chest item.)
            return super.use(level, player, hand);
        }
        if (ArcReactorItem.refuses(player)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.enhanced.iron_man.reactor_refused")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.consume(stack);
        }
        player.startUsingItem(hand);
        if (player instanceof ServerPlayer server) {
            ArcReactorItem.hum(server, 0.45F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ArcReactorItem.holdTicks();
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer user) {
            ArcReactorItem.pull(server, user, ArcReactorItem.holdTicks(), remaining);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player) || ArcReactorItem.refuses(player)
                || Powers.power(player) == Power.IRON_MAN) {
            return stack;
        }
        ArcReactorItem.become(player);
        return stack;
    }

    /**
     * It was put on the chest slot ({@code IronEvents}): a player who is not Iron Man becomes him (unless a hero is
     * refused); Iron Man feels it settle.
     */
    public static void wornBy(ServerPlayer player) {
        if (Powers.power(player) == Power.IRON_MAN) {
            return;
        }
        if (ArcReactorItem.refuses(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.iron_man.reactor_refused")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ArcReactorItem.become(player);
        ServerLevel level = player.serverLevel();
        level.sendParticles(new EnergyParticleOptions(0x9FE8FF, 1.0F), player.getX(),
                player.getY() + player.getBbHeight() * 0.68, player.getZ(), 20, 0.2, 0.2, 0.2, 0.05);
    }


    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
        return slot == EquipmentSlot.CHEST;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.nano_arc_reactor.tooltip").withStyle(ChatFormatting.GRAY,
                ChatFormatting.ITALIC));
    }
}
