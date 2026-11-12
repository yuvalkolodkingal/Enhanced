package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** 1.21.11 only: 1.21.1's {@code stack.save(registries)} and {@code ItemStack.parse(registries, tag)} (the item codecs). */
public final class Stacks {
    private Stacks() {
    }

    public static Tag save(ItemStack stack, HolderLookup.Provider registries) {
        return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    public static Tag save(ItemStack stack, HolderLookup.Provider registries, Tag into) {
        return save(stack, registries);
    }

    public static Optional<ItemStack> parse(HolderLookup.Provider registries, Tag tag) {
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result();
    }

    public static ItemStack parseOptional(HolderLookup.Provider registries, CompoundTag tag) {
        return tag.isEmpty() ? ItemStack.EMPTY : parse(registries, tag).orElse(ItemStack.EMPTY);
    }

    /** 1.21.1's {@code inventory.save(new ListTag())}: the inventory's slots as a list of tags. */
    public static net.minecraft.nbt.ListTag saveInventory(net.minecraft.world.entity.player.Inventory inventory,
                                                          HolderLookup.Provider registries) {
        net.minecraft.world.level.storage.TagValueOutput out = ValueBridge.output(registries);
        inventory.save(out.list("Inventory", net.minecraft.world.ItemStackWithSlot.CODEC));
        return out.buildResult().getListOrEmpty("Inventory");
    }

    /** 1.21.1's {@code inventory.load(list)}. */
    public static void loadInventory(net.minecraft.world.entity.player.Inventory inventory, net.minecraft.nbt.ListTag list,
                                     HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Inventory", list);
        inventory.load(ValueBridge.input(registries, tag).listOrEmpty("Inventory", net.minecraft.world.ItemStackWithSlot.CODEC));
    }
}
