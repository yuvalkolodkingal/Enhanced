package com.kolodkingal.enhanced.spellbook;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * One of the three spellbooks ({@link Spellbook}): use makes a player with no power its caster; see {@link Spellbooks}.
 */
public class SpellbookItem extends Item {
    private final Spellbook book;

    public SpellbookItem(Spellbook book, Properties properties) {
        super(properties);
        this.book = book;
    }

    public Spellbook book() {
        return book;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return Spellbooks.use(level, player, hand, book);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "item.enhanced." + book.itemId();
        tooltip.add(Component.translatable(key + ".desc").withStyle(s -> s.withColor(book.glow).withItalic(true)));
        tooltip.add(Component.translatable(key + ".powers").withStyle(s -> s.withColor(book.color)));
        tooltip.add(Component.translatable(key + ".use").withStyle(s -> s.withColor(0xAAAAAA)));
    }
}
