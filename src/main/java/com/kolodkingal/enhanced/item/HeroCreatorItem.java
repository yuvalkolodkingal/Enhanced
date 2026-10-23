package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.client.screen.HeroCreatorScreen;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * The Hero Creator (3.3): a Creative-only item (it is in the mod's Creative tab and nowhere else: no recipe, no loot) that
 * opens the creator's screen, where its user becomes any hero of the mod, or makes and becomes a hero of his own. It works
 * for a player in Creative mode or an operator ({@code custom_hero.operatorsOnly}: operators only); the server checks
 * that again for every button of the screen.
 */
public class HeroCreatorItem extends Item {
    public HeroCreatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            String refused = CustomHeroes.refusal(server);
            if (refused != null) {
                server.displayClientMessage(Component.translatable(refused).withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HERO_CREATOR_OPEN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (player.isCreative() || player.hasPermissions(2)) {
            // Client only: this branch never runs on a dedicated server, so the screen class is never loaded there.
            HeroCreatorScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.hero_creator.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.hero_creator.choose").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.hero_creator.create").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.hero_creator.creative").withStyle(ChatFormatting.AQUA));
    }
}
