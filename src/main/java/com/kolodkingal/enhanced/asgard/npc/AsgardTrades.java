package com.kolodkingal.enhanced.asgard.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Optional;

/**
 * What the traders of Asgard sell (3.5), through the vanilla merchant screen: fixed offers, paid in gold ingots and
 * emeralds, restocked once a day; no villager levels or experience. Sif sells swords and shields; Valkyrie saddles, horse
 * armour and a horse (it waits beside her, tamed to the buyer); Volstagg food, cake and "mead" (honey). Sif's Token is
 * the prize of her training.
 */
public final class AsgardTrades {
    /** How many times an offer can be taken before the next day's restock. */
    public static final int USES = 6;

    private AsgardTrades() {
    }

    /** The offers of a role (empty for the roles that do not trade). */
    public static MerchantOffers offers(Role role) {
        MerchantOffers offers = new MerchantOffers();
        switch (role) {
            case SIF -> {
                offers.add(offer(Items.GOLD_INGOT, 3, null, 0, new ItemStack(Items.IRON_SWORD), 4));
                offers.add(offer(Items.EMERALD, 4, null, 0, new ItemStack(Items.SHIELD), 4));
                offers.add(offer(Items.GOLD_INGOT, 4, null, 0, new ItemStack(Items.BOW), 4));
                offers.add(offer(Items.GOLD_INGOT, 10, Items.EMERALD, 4, new ItemStack(Items.DIAMOND_SWORD), 2));
            }
            case VALKYRIE -> {
                offers.add(offer(Items.GOLD_INGOT, 4, null, 0, new ItemStack(Items.SADDLE), 4));
                offers.add(offer(Items.GOLD_INGOT, 6, null, 0, new ItemStack(Items.IRON_HORSE_ARMOR), 3));
                offers.add(offer(Items.GOLD_INGOT, 8, Items.EMERALD, 2, new ItemStack(Items.GOLDEN_HORSE_ARMOR), 3));
                offers.add(offer(Items.GOLD_INGOT, 16, Items.EMERALD, 8, new ItemStack(Items.DIAMOND_HORSE_ARMOR), 2));
                offers.add(offer(Items.GOLD_INGOT, 10, Items.EMERALD, 2, horseToken(), 2));
            }
            case VOLSTAGG -> {
                offers.add(offer(Items.EMERALD, 1, null, 0, new ItemStack(Items.BREAD, 6), USES * 2));
                offers.add(offer(Items.GOLD_INGOT, 1, null, 0, new ItemStack(Items.COOKED_CHICKEN, 4), USES * 2));
                offers.add(offer(Items.GOLD_INGOT, 2, null, 0, new ItemStack(Items.COOKED_BEEF, 4), USES * 2));
                offers.add(offer(Items.GOLD_INGOT, 3, null, 0, new ItemStack(Items.CAKE), USES));
                offers.add(offer(Items.EMERALD, 2, null, 0, mead(), USES));
            }
            default -> {
            }
        }
        return offers;
    }

    private static MerchantOffer offer(Item cost, int count, Item second, int secondCount, ItemStack result, int uses) {
        Optional<ItemCost> b = second == null ? Optional.empty() : Optional.of(new ItemCost(second, secondCount));
        return new MerchantOffer(new ItemCost(cost, count), b, result, uses, 0, 0.0F);
    }

    /** Volstagg's "mead": honey, with a proud name. */
    static ItemStack mead() {
        ItemStack mead = new ItemStack(Items.HONEY_BOTTLE, 2);
        mead.set(DataComponents.ITEM_NAME, Component.translatable("item.enhanced.asgard_mead"));
        return mead;
    }

    /** What the buyer of a horse takes from the screen: a lead, named for the horse that waits beside Valkyrie. */
    static ItemStack horseToken() {
        ItemStack lead = new ItemStack(Items.LEAD);
        lead.set(DataComponents.ITEM_NAME, Component.translatable("item.enhanced.valkyrie_horse"));
        return lead;
    }

    static boolean isHorseToken(ItemStack stack) {
        return stack.is(Items.LEAD) && stack.get(DataComponents.ITEM_NAME) != null;
    }

    /** A trade was made: the horse comes out of the stables (saddled, tamed to the buyer), beside Valkyrie. */
    static void traded(AsgardianEntity trader, ServerPlayer player, MerchantOffer offer) {
        if (trader.role() == Role.VALKYRIE && isHorseToken(offer.getResult()) && trader.level() instanceof ServerLevel level) {
            Horse horse = EntityType.HORSE.create(level);
            if (horse == null) {
                return;
            }
            horse.moveTo(trader.getX() + 1.5, trader.getY(), trader.getZ() + 1.5, trader.getYRot(), 0.0F);
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(horse.blockPosition()), MobSpawnType.TRIGGERED, null);
            horse.tameWithName(player);
            horse.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
            horse.equipSaddle(new ItemStack(Items.SADDLE), null);
            horse.setPersistenceRequired();
            level.addFreshEntity(horse);
            AsgardTalk.speak(trader, player, "npc.enhanced.valkyrie.horse", List.of());
        }
    }

    /** Sif's Token, the prize of her training: a gold nugget with her name. */
    public static ItemStack token() {
        ItemStack token = new ItemStack(Items.GOLD_NUGGET);
        token.set(DataComponents.ITEM_NAME, Component.translatable("item.enhanced.sif_token").withStyle(ChatFormatting.GOLD));
        token.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return token;
    }

    static void giveToken(ServerPlayer player) {
        ItemStack token = token();
        if (!player.getInventory().add(token)) {
            player.drop(token, false);
        }
    }
}
