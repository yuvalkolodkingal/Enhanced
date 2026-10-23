package com.kolodkingal.enhanced.road.copy;

import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The blocks of the Copy the Structure trial: what the player is given to build with, and how he gets his own things back.
 * Every block the trial hands out is marked ({@link #stack}) so that none of it can leave the trial. The player's own backpack is
 * put aside while he builds (saved with his progress, so a crash or a power cut loses nothing) and returned to him when the
 * trial ends, whatever way it ends; the Scarlet Witch Gene stays with him all the time.
 */
public final class StructureSupply {
    /** The mark on the blocks the trial gave out. */
    static final String MARK = "enhanced_road_supply";
    /** The most stacks of an item the supply chest holds. */
    public static final int CHEST_STACKS = 3;
    /** The least of an item the player is given, however little the model needs. */
    public static final int MINIMUM = 16;
    private static final String STASH = "stash";
    /** The slots of the main inventory (hotbar first). */
    private static final int SLOTS = 36;

    /** A stack of the trial's blocks. */
    public static ItemStack stack(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(MARK, true));
        return stack;
    }

    /** Marks a stack as the trial's (what he crafts from its blocks is too: it cannot leave either). */
    public static void mark(ItemStack stack) {
        if (!stack.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(MARK, true));
        }
    }

    /** Whether the stack is one of the trial's blocks. */
    public static boolean isSupply(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(MARK);
    }

    /** How many of each item the player is given at the start: what the model needs and a good deal more, in whole stacks of the item. */
    public static Map<Item, Integer> amounts(StructureModel model, int percent) {
        Map<Item, Integer> amounts = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> need : model.itemsNeeded().entrySet()) {
            amounts.put(need.getKey(), Math.max(MINIMUM, (int) Math.ceil(need.getValue() * percent / 100.0)));
        }
        return amounts;
    }

    /** How many stacks of the main inventory the amounts take. */
    public static int stacksFor(Map<Item, Integer> amounts) {
        int stacks = 0;
        for (Map.Entry<Item, Integer> entry : amounts.entrySet()) {
            int size = entry.getKey().getDefaultMaxStackSize();
            stacks += (entry.getValue() + size - 1) / size;
        }
        return stacks;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The player's backpack

    /**
     * Puts the player's things aside (the Gene stays) and gives him the blocks to build with: the first stack of each kind on the
     * hotbar, in the order of how much the model needs, the rest of the stacks in the backpack. Whatever was put aside before and
     * never returned is returned first.
     */
    public static void give(ServerPlayer player, StructureModel model, String challenge) {
        restore(player, challenge);
        Inventory inventory = player.getInventory();
        ListTag stash = new ListTag();
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack.is(ModItems.SCARLET_WITCH_GENE.get())) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.put("Item", stack.save(player.registryAccess()));
            stash.add(entry);
            inventory.setItem(slot, ItemStack.EMPTY);
        }
        CompoundTag data = new CompoundTag();
        data.put(STASH, stash);
        CompoundTag all = ModAttachments.ROAD_STASH.get(player).copy();
        all.put(challenge, data);
        ModAttachments.ROAD_STASH.set(player, all);
        Map<Item, Integer> amounts = amounts(model, WitchesRoadConfig.STRUCTURE_COPY_SUPPLY_PERCENT.get());
        for (Map.Entry<Item, Integer> entry : amounts.entrySet()) {
            int size = entry.getKey().getDefaultMaxStackSize();
            int left = entry.getValue();
            boolean first = true;
            while (left > 0) {
                int slot = free(inventory, first);
                if (slot < 0) {
                    break;
                }
                int n = Math.min(size, left);
                inventory.setItem(slot, stack(entry.getKey(), n));
                left -= n;
                first = false;
            }
        }
        inventory.selected = 0;
        player.containerMenu.broadcastChanges();
    }

    /** The first empty slot: the hotbar first for the first stack of a kind, the backpack first for the others; -1 if there is none. */
    private static int free(Inventory inventory, boolean hotbarFirst) {
        for (int i = 0; i < SLOTS; i++) {
            int slot = hotbarFirst ? i : (i + 9) % SLOTS;
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** Takes the trial's blocks away from the player and gives him back what was put aside. Does nothing if there is nothing to return. */
    public static void restore(ServerPlayer player, String challenge) {
        strip(player);
        CompoundTag data = ModAttachments.ROAD_STASH.get(player).getCompound(challenge);
        if (!data.contains(STASH, Tag.TAG_LIST)) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (Tag tag : data.getList(STASH, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), entry.getCompound("Item"));
            int slot = entry.getInt("Slot");
            if (stack.isEmpty()) {
                continue;
            }
            if (slot >= 0 && slot < SLOTS && inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, stack);
            } else if (!inventory.add(stack)) {
                player.drop(stack, false);
            }
        }
        // (The inventory and the stash are saved with the player: the stash goes in the same breath as the things come back.)
        CompoundTag all = ModAttachments.ROAD_STASH.get(player).copy();
        all.remove(challenge);
        ModAttachments.ROAD_STASH.set(player, all);
        player.containerMenu.broadcastChanges();
    }

    /** Whether the player has things put aside that he has not got back. */
    public static boolean hasStash(ServerPlayer player, String challenge) {
        return ModAttachments.ROAD_STASH.get(player).getCompound(challenge).contains(STASH, Tag.TAG_LIST);
    }

    /** Takes every block the trial gave out away from the player (his backpack, armour, off hand, the crafting square, the cursor). */
    public static void strip(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isSupply(inventory.getItem(i))) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        for (Slot slot : player.inventoryMenu.slots) {
            if (isSupply(slot.getItem())) {
                slot.set(ItemStack.EMPTY);
            }
        }
        // The 2x2 crafting square: what was crafted from the trial's blocks carries no mark, so nothing of the square stays (the blocks in it are
        // the trial's; whatever else he had there was put aside with the rest of his things).
        for (int i = 0; i < player.inventoryMenu.getCraftSlots().getContainerSize(); i++) {
            player.inventoryMenu.getCraftSlots().setItem(i, ItemStack.EMPTY);
        }
        if (isSupply(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        player.containerMenu.broadcastChanges();
    }

    private StructureSupply() {
    }
}
