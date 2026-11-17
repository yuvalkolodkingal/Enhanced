package com.kolodkingal.enhanced.magic;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Items touched by 3.17's magic, written on the stack itself: a weapon or armour conjured out of light (it vanishes when
 * its time is up, {@code MagicEvents}), and an item Billy enchanted for a while (it glints, and a weapon of it hits half
 * as hard again while it lasts).
 */
public final class Conjured {
    private static final String CONJURED = "enhanced_conjured_until";
    private static final String ENCHANTED = "enhanced_enchanted_until";

    private Conjured() {
    }

    /** Marks a stack as conjured, until game time {@code until}. */
    public static void mark(ItemStack stack, long until) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(CONJURED, until));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    /** Enchants a stack until game time {@code until}. */
    public static void enchant(ItemStack stack, long until) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(ENCHANTED, until));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    private static long read(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(key)) {
            return -1;
        }
        CompoundTag tag = data.copyTag();
        return tag.getLong(key);
    }

    public static boolean conjured(ItemStack stack) {
        return read(stack, CONJURED) >= 0;
    }

    /** Whether a conjured stack's time is up. */
    public static boolean expired(ItemStack stack, long now) {
        long until = read(stack, CONJURED);
        return until >= 0 && now >= until;
    }

    /** Whether Billy's enchantment on it lasts. */
    public static boolean enchanted(ItemStack stack, long now) {
        long until = read(stack, ENCHANTED);
        return until >= 0 && now < until;
    }

    /** An enchantment whose time is up comes off (the glint too). */
    public static void fade(ItemStack stack, long now) {
        long until = read(stack, ENCHANTED);
        if (until >= 0 && now >= until) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(ENCHANTED));
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }
}
