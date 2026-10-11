package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.apocalypse.ApocalypseLane;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Capstone, the golden tip of the pyramid, which the sun feeds. It wakes Apocalypse: <b>Sneak and right-click on a
 * block</b> of sand or sandstone, by day, under the open sky, with a hero who can break his shield (a Scarlet Witch or a
 * Phoenix) near. A plain right-click does nothing but tell the player where to plant it. The ritual's checks, the fight
 * and the refund are {@link ApocalypseLane}'s; the Capstone is used up only if the ritual starts. (DESIGN section 17.)
 */
public class CapstoneItem extends Item {
    public CapstoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.enhanced.apocalypse.capstone_use"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player instanceof ServerPlayer starter && !level.isClientSide) {
            ApocalypseLane.startedBy(context.getClickedPos());
            try {
                BossFights.start(BossKind.APOCALYPSE, starter, false);
            } finally {
                ApocalypseLane.startedBy(null);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.enhanced.apocalypse.capstone_use"), true);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.capstone.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.enhanced.capstone.desc2").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
