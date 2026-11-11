package com.kolodkingal.enhanced.mind;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/**
 * The library's books (3.12): pages of code, the stuff of a mind made of numbers, with a piece of knowledge between
 * them ({@code book.enhanced.mind.fact.<n>}).
 */
final class MindBooks {
    static final int FACTS = 12;
    private static final String HEX = "0123456789ABCDEF";

    private MindBooks() {
    }

    static ItemStack book(RandomSource random, int n) {
        List<Filterable<Component>> pages = new ArrayList<>();
        int count = 2 + random.nextInt(3);
        for (int p = 0; p < count; p++) {
            StringBuilder code = new StringBuilder();
            for (int line = 0; line < 9; line++) {
                for (int word = 0; word < 3; word++) {
                    for (int c = 0; c < 4; c++) {
                        code.append(HEX.charAt(random.nextInt(16)));
                    }
                    code.append(word < 2 ? ' ' : '\n');
                }
            }
            pages.add(Filterable.passThrough(Component.literal(code.toString())));
            if (p == count / 2) {
                pages.add(Filterable.passThrough(Component.translatable("book.enhanced.mind.fact." + random.nextInt(FACTS))));
            }
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        String title = "SRC-" + String.format("%04X", random.nextInt(0x10000)) + " #" + (n + 1);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), "Vision", 0, pages, true));
        return book;
    }
}
