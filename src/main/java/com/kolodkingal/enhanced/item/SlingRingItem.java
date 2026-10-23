package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.client.strange.SlingRingScreen;
import com.kolodkingal.enhanced.strange.SlingRingActions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Sling Ring (3.6): a sorcerer's ring (Doctor Strange, Billy, Agatha, Loki, the Scarlet Witch, Odin, a custom
 * sorcerer). Use opens its screen of places (the PlaceBook kind {@code SLING_RING}: choosing a place opens a pair of
 * Sling-Ring portals to it, in another dimension too); Sneak + use saves where he stands. For anyone else it is just a
 * ring. The rules are {@link SlingRingActions}'.
 */
public class SlingRingItem extends Item {
    public SlingRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            if (!SlingRingActions.wears(server, true)) {
                return InteractionResultHolder.fail(stack);
            }
            if (server.isShiftKeyDown()) {
                SlingRingActions.save(server, "");
            }
        } else if (level.isClientSide() && !player.isShiftKeyDown() && SlingRingActions.isSorcerer(player)) {
            // Client only: this branch never runs on a dedicated server, so the screen class is never loaded there.
            SlingRingScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.sling_ring.desc").withStyle(ChatFormatting.GOLD,
                ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.sling_ring.sorcerers").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.enhanced.sling_ring.use").withStyle(ChatFormatting.AQUA));
    }
}
