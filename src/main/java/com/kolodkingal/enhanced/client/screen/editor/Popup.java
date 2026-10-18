package com.kolodkingal.enhanced.client.screen.editor;

import com.kolodkingal.enhanced.client.custom.CreatorUi;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A choice the editor asks for over its page (3.4): a title and big buttons (what fits a key, a move, a stance, "yes" or
 * "no"), a page at a time. While it is open nothing else of the page can be clicked; Esc closes it.
 *
 * @param title what is being chosen
 * @param items the choices
 */
public record Popup(Component title, List<Item> items) {
    /**
     * One choice.
     *
     * @param label    its name
     * @param sub      a line under it, or null
     * @param icon     its picture (an item), or null
     * @param face     its picture (a skin's face), or null
     * @param color    its frame's colour
     * @param selected whether it is the current choice
     * @param action   what choosing it does (the popup closes first)
     */
    public record Item(Component label, @Nullable Component sub, @Nullable ItemStack icon, @Nullable ResourceLocation face, int color,
                       boolean selected, Runnable action) {
        public static Item of(Component label, @Nullable Component sub, int color, boolean selected, Runnable action) {
            return new Item(label, sub, null, null, color, selected, action);
        }

        public Item icon(String itemId) {
            return new Item(label, sub, CreatorUi.icon(itemId), face, color, selected, action);
        }
    }
}
