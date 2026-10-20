package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The nanotech housings' durability (3.8, lane L4, PLAN 5.3, 6.3): while a Mark 50 or Mark 85 is deployed, the damage
 * its armour takes goes to the housing he carries (vanilla's armour wear: a quarter of the damage, at least one; never
 * the Nano Arc Reactor, which has none), down to its last point where the suit goes offline ("Suit offline! Repair
 * it.") but the housing is never lost; while a housing is stowed it <b>rebuilds</b> one point every {@code
 * nanoRebuildTicks} 40 (and diamonds or netherite scrap repair it at an anvil, lane L3's repair items).
 */
public final class NanoHousings {
    /** When each player's housings last rebuilt (game time). */
    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** The housing of a nanotech suit he carries (the first in his inventory), or an empty stack. */
    public static ItemStack housing(Player player, SuitKind kind) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(IronSuits.item(kind))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The nanotech suit he has deployed, or null. */
    @Nullable
    public static SuitKind deployed(Player player) {
        SuitKind worn = IronSuits.worn(player);
        return worn != null && worn.nanotech() ? worn : null;
    }

    /** How much a blow of {@code amount} wears the armour (vanilla's: a quarter, at least one; 0 if it ignores it). */
    public static int wear(DamageSource source, float amount) {
        if (amount <= 0.0F || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return 0;
        }
        return Math.max(1, (int) (amount / 4.0F));
    }

    /**
     * A blow of {@code amount} reached him: his deployed suit's housing takes the wear.
     *
     * @return the points it lost
     */
    public static int onHurt(Player player, DamageSource source, float amount) {
        SuitKind kind = deployed(player);
        if (kind == null || player.isCreative()) {
            return 0;
        }
        ItemStack housing = housing(player, kind);
        int points = wear(source, amount);
        if (housing.isEmpty() || !housing.isDamageableItem() || points <= 0) {
            return 0;
        }
        int before = housing.getDamageValue();
        housing.setDamageValue(Math.min(housing.getMaxDamage() - 1, before + points));
        return housing.getDamageValue() - before;
    }

    /**
     * Every tick of every player: each nanotech housing he carries that is not deployed rebuilds one point every
     * {@code nanoRebuildTicks}.
     */
    public static void rebuild(Player player) {
        int every = Math.max(1, NanoTech.cfg(IronManConfig.NANO_REBUILD_TICKS, 40));
        long now = player.level().getGameTime();
        Long last = LAST.putIfAbsent(player.getUUID(), now);
        if (last == null || now - last < every && now >= last) {
            return;
        }
        LAST.put(player.getUUID(), now);
        SuitKind out = deployed(player);
        ItemStack inUse = out == null ? ItemStack.EMPTY : housing(player, out);
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            SuitKind kind = IronSuits.kind(stack);
            if (kind != null && kind.nanotech() && stack.isDamaged() && stack != inUse) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
    }

    static void forget(UUID id) {
        LAST.remove(id);
    }

    static void clear() {
        LAST.clear();
    }

    private NanoHousings() {
    }
}
