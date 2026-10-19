package com.kolodkingal.enhanced.cottage;

import com.kolodkingal.enhanced.asgard.npc.AsgardTrades;
import com.kolodkingal.enhanced.asgard.npc.Role;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Optional;

/**
 * What Agatha and Rio sell at the cottage (3.7, lane L6; the plan's 9.7), through the vanilla merchant screen as the
 * traders of Asgard ({@code AsgardTrades.offers} hands their roles here): Agatha potions and candles, Rio flowers,
 * saplings and seeds. The 3.7 skeleton gives each one offer (so they are traders from the start); lane L6 writes the
 * rest.
 */
public final class CottageTrades {

    /** The offers of a cottage role (empty for Nicky). */
    public static MerchantOffers offers(Role role) {
        MerchantOffers offers = new MerchantOffers();
        switch (role) {
            case AGATHA -> offers.add(offer(2, PotionContents.createItemStack(Items.POTION, Potions.HEALING)));
            case RIO -> offers.add(offer(1, new ItemStack(Items.POPPY, 4)));
            default -> {
            }
        }
        return offers;
    }

    private static MerchantOffer offer(int emeralds, ItemStack result) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), Optional.empty(), result, AsgardTrades.USES, 0,
                0.0F);
    }

    private CottageTrades() {
    }
}
