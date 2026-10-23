package com.kolodkingal.enhanced.platform.event;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;

/** The attribute modifiers of an item stack are being worked out: change them. */
public class ItemAttributeModifierEvent {
    private final ItemStack itemStack;
    private final ItemAttributeModifiers defaultModifiers;
    private final List<ItemAttributeModifiers.Entry> modifiers;

    public ItemAttributeModifierEvent(ItemStack itemStack, ItemAttributeModifiers defaultModifiers) {
        this.itemStack = itemStack;
        this.defaultModifiers = defaultModifiers;
        this.modifiers = new ArrayList<>(defaultModifiers.modifiers());
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public ItemAttributeModifiers getDefaultModifiers() {
        return defaultModifiers;
    }

    public List<ItemAttributeModifiers.Entry> getModifiers() {
        return modifiers;
    }

    /** Puts a modifier in place of the one with the same attribute and id (or adds it). */
    public void replaceModifier(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
        modifiers.removeIf(e -> e.attribute().equals(attribute) && e.modifier().id().equals(modifier.id()));
        modifiers.add(new ItemAttributeModifiers.Entry(attribute, modifier, slot));
    }

    /** The stack's modifiers after every listener. */
    public ItemAttributeModifiers build() {
        return new ItemAttributeModifiers(List.copyOf(modifiers), defaultModifiers.showInTooltip());
    }
}
