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
    /** (3.16) How many times a player who is neither an operator nor in Creative can become a hero with one. */
    public static final int USES = 2;

    public HeroCreatorItem(Properties properties) {
        super(properties);
    }

    /** Whether this player uses Hero Creators without wearing them out: an operator, or a player in Creative. */
    public static boolean unlimited(Player player) {
        return player.isCreative() || player.hasPermissions(2);
    }

    /** The uses this stack has left. */
    public static int usesLeft(ItemStack stack) {
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    /** The Hero Creator the player holds (a hand first, then the inventory) with a use left, or empty. */
    public static ItemStack held(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof HeroCreatorItem && usesLeft(stack) > 0) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof HeroCreatorItem && usesLeft(stack) > 0) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * (3.16) A player who is neither an operator nor in Creative became a hero with it: one use is spent, and the second
     * breaks it.
     */
    public static void spendUse(ServerPlayer player) {
        if (unlimited(player)) {
            return;
        }
        ItemStack stack = held(player);
        if (stack.isEmpty()) {
            return;
        }
        stack.setDamageValue(stack.getDamageValue() + 1);
        int left = usesLeft(stack);
        if (left <= 0) {
            stack.shrink(1);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.enhanced.hero_creator.spent").withStyle(ChatFormatting.GOLD), true);
        } else {
            player.displayClientMessage(Component.translatable("message.enhanced.hero_creator.uses_left", left)
                    .withStyle(ChatFormatting.GOLD), true);
        }
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
        } else if (unlimited(player) || (usesLeft(stack) > 0
                && !(com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded()
                && com.kolodkingal.enhanced.config.CustomHeroConfig.OPERATORS_ONLY.get()))) {
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
        // (3.16) the uses it has left for everyone else
        tooltip.add(Component.translatable("item.enhanced.hero_creator.uses", usesLeft(stack), USES).withStyle(ChatFormatting.YELLOW));
    }
}
