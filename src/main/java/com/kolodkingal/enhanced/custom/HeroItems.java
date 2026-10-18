package com.kolodkingal.enhanced.custom;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The hero's own items that the Hero Creator's "Choose a hero" gives with the hero (3.4, R8): what becoming him the usual
 * way leaves with him, or what he carries as his. Magneto: his helmet, Quicksilver: his goggles, Cyclops: his ruby-quartz
 * glasses (each put on, as the hero put them on to awaken; the glasses also hold his eyes back); Thor: Mjolnir, claimed
 * as his (Stormbreaker in the looks of the films he wields it in: Ragnarok's eyepatch of Infinity War and Endgame, Love
 * and Thunder); Loki: his Scepter; Thanos: the Infinity Gauntlet; Ultron: his Core; Professor X: his wheelchair; Agatha:
 * the Darkhold. The others' awakening items are used up when they awaken (the gene, the core of a synthezoid, the spider,
 * the ember, the essence), so they get nothing. Nothing is given twice: an item the player already has (anywhere in the
 * inventory, worn or held) is skipped. Off with {@code custom_hero.chooseGivesItems}.
 */
public final class HeroItems {
    /** The Thor looks in which he wields Stormbreaker. */
    public static final Set<String> STORMBREAKER_LOOKS = Set.of("ragnarok", "love_and_thunder", "love_and_thunder_helmet");

    /** The items of a hero in a look (empty for none). */
    public static List<Item> of(Power power, String look) {
        return switch (power) {
            case MAGNETO -> List.of(ModItems.MAGNETO_HELMET.get());
            case QUICKSILVER -> List.of(ModItems.QUICKSILVER_GOGGLES.get());
            case CYCLOPS -> List.of(ModItems.RUBY_QUARTZ_GLASSES.get());
            case THOR -> List.of(STORMBREAKER_LOOKS.contains(look) ? ModItems.STORMBREAKER.get() : ModItems.MJOLNIR.get());
            case LOKI -> List.of(ModItems.LOKIS_SCEPTER.get());
            case THANOS -> List.of(ModItems.INFINITY_GAUNTLET.get());
            case ULTRON -> List.of(ModItems.ULTRON_CORE.get());
            case PROFESSOR_X -> List.of(ModItems.XAVIER_WHEELCHAIR.get());
            case AGATHA -> List.of(ModItems.DARKHOLD.get());
            // 3.5: their weapons are bound to their owners (OWNER and BOND): lanes L2 and L3 decide what "Choose" gives.
            case HEIMDALL, ODIN, HELA -> List.of();
            default -> List.of();
        };
    }

    /** Whether the items worn on the head go on it (when it is free). */
    private static boolean worn(Item item) {
        return item == ModItems.MAGNETO_HELMET.get() || item == ModItems.QUICKSILVER_GOGGLES.get() || item == ModItems.RUBY_QUARTZ_GLASSES.get();
    }

    /** Whether the config lets "Choose a hero" give the items. */
    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || CustomHeroConfig.CHOOSE_GIVES_ITEMS.get();
    }

    /**
     * Gives the player (who has just become {@code power} in {@code look}) the hero's items he does not have yet: worn
     * ones on the head if it is free, the rest in the inventory (dropped at his feet when it is full). Returns what was
     * given.
     */
    public static List<ItemStack> give(ServerPlayer player, Power power, String look) {
        List<ItemStack> given = new ArrayList<>();
        if (!enabled()) {
            return given;
        }
        for (Item item : of(power, look)) {
            if (has(player, item)) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            if (item == ModItems.MJOLNIR.get()) {
                // Claimed, as the worthy one who lifted it to awaken.
                stack.set(ModDataComponents.OWNER.get(), MjolnirOwner.of(player));
            }
            given.add(stack.copy());
            if (worn(item) && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                player.setItemSlot(EquipmentSlot.HEAD, stack);
            } else if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        return given;
    }

    /** The hero a worn hero item belongs to (Magneto's helmet, Quicksilver's goggles, Cyclops's glasses, Wanda's crown). */
    @Nullable
    public static Power wornItemHero(ItemStack stack) {
        if (stack.is(ModItems.MAGNETO_HELMET.get())) {
            return Power.MAGNETO;
        }
        if (stack.is(ModItems.QUICKSILVER_GOGGLES.get())) {
            return Power.QUICKSILVER;
        }
        if (stack.is(ModItems.RUBY_QUARTZ_GLASSES.get())) {
            return Power.CYCLOPS;
        }
        if (stack.is(ModItems.WANDA_CROWN.get())) {
            return Power.SCARLET_WITCH;
        }
        return null;
    }

    /**
     * 3.5: when the player becomes another hero, a worn item of a different hero comes off the head into the inventory
     * (at his feet when it is full; never deleted), so it no longer hides the new hero's look (Cyclops's glasses over a
     * custom hero's hat and visor). It stays on when the new hero is its own hero, or a custom hero with an ability of
     * that hero, and when the power is only taken away (no new look to hide; Magneto's helmet still guards anyone's
     * mind). He can put it back on whenever he likes. Returns the item taken off, or empty.
     */
    public static ItemStack takeOffForeign(ServerPlayer player, PowerData data) {
        Power now = data.power();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        Power owner = wornItemHero(head);
        if (owner == null || now == Power.NONE || now == owner) {
            return ItemStack.EMPTY;
        }
        if (now == Power.CUSTOM && data.kit() != null && data.kit().acts(owner)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = head.copy();
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        Component name = stack.getHoverName();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
            player.displayClientMessage(Component.translatable("message.enhanced.hero_item.taken_off_dropped", name), false);
        } else {
            player.displayClientMessage(Component.translatable("message.enhanced.hero_item.taken_off", name), false);
        }
        return head.copy();
    }

    /** Whether the player has an item of this kind anywhere: the inventory, the armour, the off hand. */
    public static boolean has(ServerPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private HeroItems() {
    }
}
