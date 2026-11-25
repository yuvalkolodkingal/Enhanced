package com.kolodkingal.enhanced.hydra;

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
 * What the rulers of What If... Hydra Won? drop when they fall (3.19.1), and what makes whoever uses it one of them:
 * <ul>
 *     <li>Schmidt's serum ({@code red_skull}): Erskine's first formula, the one that gave Johann Schmidt his strength
 *     and his red skull;</li>
 *     <li>a shard of Kobik ({@code captain_hydra}): the living Cosmic Cube that rewrote Steve Rogers' past, so that he
 *     had always served Hydra.</li>
 * </ul>
 * The user becomes Captain America's powers in that look, a character of its own ({@code Characters}): Change Look keeps
 * him in it, and only another hero takes him out of it. The item is used up (not in Creative).
 */
public class HydraRulerItem extends Item {
    private final String look;

    public HydraRulerItem(String look, Properties properties) {
        super(properties);
        this.look = look;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.success(stack);
        }
        Variant variant = Variants.get(Power.CAPTAIN_AMERICA, look);
        if (Powers.is(server, Power.CAPTAIN_AMERICA) && look.equals(Powers.get(server).variant())) {
            server.displayClientMessage(Component.translatable("message.enhanced.hydra_ruler.already", variant.displayName()), true);
            return InteractionResultHolder.pass(stack);
        }
        EarnedLooks.earn(server, variant);
        if (Powers.is(server, Power.CAPTAIN_AMERICA)) {
            Powers.setVariant(server, variant);
        } else {
            Powers.awaken(server, Power.CAPTAIN_AMERICA, look);
        }
        Powers.burst(server.serverLevel(), server.getX(), server.getY() + 1.0, server.getZ(), variant.color(), 80, 0.6);
        Powers.burst(server.serverLevel(), server.getX(), server.getY() + 1.0, server.getZ(), variant.glow(), 40, 0.4);
        level.playSound(null, server.getX(), server.getY(), server.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.4F);
        server.displayClientMessage(Component.translatable("message.enhanced.hydra_ruler.become", variant.displayName())
                .withStyle(ChatFormatting.DARK_RED), false);
        if (!server.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.enhanced.hydra_ruler.desc2").withStyle(ChatFormatting.GRAY));
    }
}
