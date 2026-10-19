package com.kolodkingal.enhanced.cottage;

import com.kolodkingal.enhanced.asgard.npc.AsgardTrades;
import com.kolodkingal.enhanced.asgard.npc.Role;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Optional;

/**
 * What Agatha and Rio sell at the cottage (3.7, the plan's 9.7), through the vanilla merchant screen as the traders of
 * Asgard ({@code AsgardTrades.offers} hands their roles here; restocked daily, {@link AsgardTrades#USES} each), paid in
 * emeralds. Agatha: potions (Healing, Night Vision, Slow Falling, Fire Resistance), purple candles, an enchanted book
 * for a book (a gentle level 1-2 enchantment, chosen when her offers are made) and "Agatha's soup" (a stew with
 * Regeneration). Rio: flowers, saplings, seeds, bone meal, glow berries and moss. Nicky does not trade (he gives
 * gifts). With {@code [agatha_cottage] trades} off, the talk offers no trade.
 */
public final class CottageTrades {
    /** The enchantments of Agatha's books (kind ones). */
    static final List<ResourceKey<Enchantment>> BOOKS = List.of(Enchantments.UNBREAKING, Enchantments.EFFICIENCY,
            Enchantments.PROTECTION, Enchantments.FEATHER_FALLING, Enchantments.RESPIRATION, Enchantments.FORTUNE,
            Enchantments.LOOTING, Enchantments.MENDING);

    /** The offers of a cottage role (empty for Nicky). */
    public static MerchantOffers offers(Role role) {
        MerchantOffers offers = new MerchantOffers();
        switch (role) {
            case AGATHA -> {
                offers.add(offer(2, PotionContents.createItemStack(Items.POTION, Potions.HEALING)));
                offers.add(offer(3, PotionContents.createItemStack(Items.POTION, Potions.NIGHT_VISION)));
                offers.add(offer(3, PotionContents.createItemStack(Items.POTION, Potions.SLOW_FALLING)));
                offers.add(offer(4, PotionContents.createItemStack(Items.POTION, Potions.FIRE_RESISTANCE)));
                offers.add(offer(1, new ItemStack(Items.PURPLE_CANDLE, 6)));
                ItemStack book = book(RandomSource.create());
                if (!book.isEmpty()) {
                    offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 10),
                            Optional.of(new ItemCost(Items.BOOK, 1)), book, 2, 0, 0.0F));
                }
                offers.add(offer(2, soup()));
            }
            case RIO -> {
                for (Item flower : List.of(Items.POPPY, Items.DANDELION, Items.CORNFLOWER, Items.ALLIUM,
                        Items.PINK_TULIP, Items.LILY_OF_THE_VALLEY)) {
                    offers.add(offer(1, new ItemStack(flower, 4)));
                }
                for (Item sapling : List.of(Items.CHERRY_SAPLING, Items.BIRCH_SAPLING, Items.DARK_OAK_SAPLING)) {
                    offers.add(offer(1, new ItemStack(sapling, 2)));
                }
                offers.add(offer(1, new ItemStack(Items.WHEAT_SEEDS, 8)));
                offers.add(offer(1, new ItemStack(Items.PUMPKIN_SEEDS, 4)));
                offers.add(offer(1, new ItemStack(Items.BONE_MEAL, 8)));
                offers.add(offer(2, new ItemStack(Items.GLOW_BERRIES, 6)));
                offers.add(offer(1, new ItemStack(Items.MOSS_BLOCK, 4)));
            }
            default -> {
            }
        }
        return offers;
    }

    private static MerchantOffer offer(int emeralds, ItemStack result) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), Optional.empty(), result, AsgardTrades.USES, 0,
                0.0F);
    }

    /** "Agatha's soup": a suspicious stew that is not suspicious at all (Regeneration for 8 s). */
    public static ItemStack soup() {
        ItemStack soup = new ItemStack(Items.SUSPICIOUS_STEW);
        soup.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, new SuspiciousStewEffects(List.of(
                new SuspiciousStewEffects.Entry(MobEffects.REGENERATION, 160))));
        soup.set(DataComponents.ITEM_NAME, Component.translatable("item.enhanced.agatha_soup"));
        return soup;
    }

    /** An enchanted book of one of {@link #BOOKS} at level 1 or 2 (empty if no server is running). */
    static ItemStack book(RandomSource random) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return ItemStack.EMPTY;
        }
        Optional<Holder.Reference<Enchantment>> enchantment = server.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT).getHolder(BOOKS.get(random.nextInt(BOOKS.size())));
        if (enchantment.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int level = Math.min(enchantment.get().value().getMaxLevel(), 1 + random.nextInt(2));
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment.get(), level));
    }

    private CottageTrades() {
    }
}
