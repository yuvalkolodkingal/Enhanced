package com.kolodkingal.enhanced.mind;

import com.kolodkingal.enhanced.power.EarnedLooks;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Source Code (3.12): the gem of Vision's brow, which the Human Torch hands over at the bottom of Vision's mind.
 * White Vision who uses it becomes Vision as his Source Code ({@code vision:source_code}, an earned look: stronger,
 * tougher, faster in flight, untouched by fire); from then on the Change Look key reaches that look too.
 */
public class SourceCodeItem extends Item {
    public SourceCodeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.success(stack);
        }
        Variant source = Variants.get(Power.VISION, "source_code");
        if (!Powers.is(server, Power.VISION)) {
            server.displayClientMessage(Component.translatable("message.enhanced.source_code.not_vision"), true);
            return InteractionResultHolder.fail(stack);
        }
        if ("source_code".equals(Powers.get(server).variant())) {
            server.displayClientMessage(Component.translatable("message.enhanced.source_code.already"), true);
            return InteractionResultHolder.pass(stack);
        }
        if (!"white_vision".equals(Powers.get(server).variant())) {
            server.displayClientMessage(Component.translatable("message.enhanced.source_code.not_white"), true);
            return InteractionResultHolder.fail(stack);
        }
        boolean first = EarnedLooks.earn(server, source);
        Powers.setVariant(server, source);
        Powers.burst(server.serverLevel(), server.getX(), server.getY() + 1.0, server.getZ(), source.color(), 80, 0.6);
        Powers.burst(server.serverLevel(), server.getX(), server.getY() + 1.0, server.getZ(), source.glow(), 40, 0.4);
        level.playSound(null, server.getX(), server.getY(), server.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        server.displayClientMessage(Component.translatable(first ? "message.enhanced.source_code.become"
                : "message.enhanced.look", source.displayName()).withStyle(s -> s.withColor(source.color())), false);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.source_code.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.enhanced.source_code.desc2").withStyle(ChatFormatting.GRAY));
    }
}
