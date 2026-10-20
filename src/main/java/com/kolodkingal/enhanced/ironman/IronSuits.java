package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.item.IronSuitItem;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Iron Man's suits, the API every lane and shared seam calls (3.8, PLAN 5.2-5.6, 5.11): the reads of the state and the
 * inventory (which suit he wears, whether a reactor is there), whether the suit works, flies, how fast, whether it is
 * metal for Magneto, and H. Written by the 3.8 skeleton with neutral bodies; lane L3's since. Seams that call it:
 * {@code PowerTicker.applyPassives} ({@link #canFly}, {@link #flightSpeed}), {@code ChangeLookPayload} ({@link
 * #onSuitKey}), {@code Metal.isMagnetic} ({@link #isMetal}).
 */
public final class IronSuits {
    /** The state ({@code ModAttachments.IRON_MAN}). */
    public static IronManState state(Player player) {
        return player.getData(ModAttachments.IRON_MAN);
    }

    public static void setState(Player player, IronManState state) {
        player.setData(ModAttachments.IRON_MAN, state);
    }

    /** The item of a suit (a classic suit, the Mark 5's suitcase, or a nanotech housing). */
    public static Item item(SuitKind kind) {
        return switch (kind) {
            case MARK_1 -> ModItems.IRON_MAN_MARK_1.get();
            case MARK_3 -> ModItems.IRON_MAN_MARK_3.get();
            case MARK_5 -> ModItems.IRON_MAN_MARK_5.get();
            case MARK_50 -> ModItems.IRON_MAN_MARK_50.get();
            case MARK_85 -> ModItems.IRON_MAN_MARK_85.get();
            case HULKBUSTER -> ModItems.IRON_MAN_HULKBUSTER.get();
        };
    }

    /** The suit of an item, or null when it is none. */
    @Nullable
    public static SuitKind kind(ItemStack stack) {
        return stack.getItem() instanceof IronSuitItem suit ? suit.kind() : null;
    }

    /**
     * The suit he wears or has deployed: a classic suit on the chest slot, else the deployed nanotech suit, or null.
     */
    @Nullable
    public static SuitKind worn(Player player) {
        SuitKind chest = kind(player.getItemBySlot(EquipmentSlot.CHEST));
        if (chest != null && chest.classic()) {
            return chest;
        }
        IronManState state = state(player);
        return state.has(IronManState.NANO_OUT) ? SuitKind.of(state.nanoSuit()) : null;
    }

    /** Whether he carries this suit's item anywhere (the inventory, the off hand, the chest slot). */
    public static boolean carries(Player player, SuitKind kind) {
        Item item = item(kind);
        return player.getInventory().contains(stack -> stack.is(item));
    }

    /**
     * Whether he carries an Arc Reactor or a Nano Arc Reactor anywhere ({@code #enhanced:arc_reactors}): the classic
     * suits' power.
     */
    public static boolean hasReactor(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModTags.ARC_REACTORS));
    }

    /** Whether the Nano Arc Reactor is on his chest slot (the nanotech suits' home). */
    public static boolean nanoReactorOn(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.NANO_ARC_REACTOR.get());
    }

    /**
     * Whether he may put a suit on: Iron Man ({@code [iron_man] onlyIronManWears}; with it off, anyone, as plain
     * armour), or a Creative player (builders).
     */
    public static boolean mayWear(Player player) {
        return player.isCreative() || Powers.power(player) == Power.IRON_MAN
                || !IronManAbilities.cfg(IronManConfig.ONLY_IRON_MAN_WEARS, true);
    }

    /**
     * The stack that takes his suit's wear and holds its durability: the classic suit on the chest, or the deployed
     * nanotech suit's housing in his inventory; empty when he wears none.
     */
    public static ItemStack durabilityStack(Player player) {
        SuitKind kind = worn(player);
        if (kind == null) {
            return ItemStack.EMPTY;
        }
        if (kind.classic()) {
            return player.getItemBySlot(EquipmentSlot.CHEST);
        }
        Item item = item(kind);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Whether his suit is broken (no durability left: "Suit offline!"; its armour still protects). */
    public static boolean broken(Player player) {
        ItemStack stack = durabilityStack(player);
        return !stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    /** Whether the reactor his worn suit needs is there (any reactor for a classic suit; the nano one on the chest). */
    public static boolean reactorFor(Player player, SuitKind kind) {
        if (kind.nanotech()) {
            return nanoReactorOn(player) && carries(player, kind);
        }
        return hasReactor(player) || player.isCreative();
    }

    /**
     * Whether his suit works (PLAN 5.3): he is Iron Man, wears a suit (or has a nanotech one out), its reactor is
     * there, no suit-up or take-off plays, and it is not broken. Unpowered, the armour still protects.
     */
    public static boolean powered(Player player) {
        if (Powers.power(player) != Power.IRON_MAN) {
            return false;
        }
        SuitKind kind = worn(player);
        return kind != null && !state(player).inPhase() && reactorFor(player, kind) && !broken(player);
    }

    /**
     * Whether he may fly now (PLAN 5.6): powered, in a suit that flies (not the Mark 1), with energy left, and not held
     * by a move that roots him (a grab, a cannon's charge, the brace).
     */
    public static boolean canFly(ServerPlayer player) {
        SuitKind kind = worn(player);
        if (kind == null || !kind.flies() || !powered(player) || SuitEnergy.empty(player) && !player.isCreative()) {
            return false;
        }
        PowerData data = Powers.get(player);
        return !data.is(Live.IRON_GRABBING) && !data.is(Live.IRON_CHARGING)
                && !state(player).has(IronManState.BRACING);
    }

    /** His flight speed: the worn suit's ({@code [mark_*] flightSpeed}; 0.05 unsuited). */
    public static float flightSpeed(Player player) {
        SuitKind kind = worn(player);
        if (kind == null) {
            return 0.05F;
        }
        return (float) switch (kind) {
            case MARK_1 -> 0.05;
            case MARK_3 -> IronManAbilities.cfg(IronManConfig.MARK3_FLIGHT_SPEED, 0.10);
            case MARK_5 -> IronManAbilities.cfg(IronManConfig.MARK5_FLIGHT_SPEED, 0.08);
            case MARK_50 -> IronManAbilities.cfg(IronManConfig.MARK50_FLIGHT_SPEED, 0.11);
            case MARK_85 -> IronManAbilities.cfg(IronManConfig.MARK85_FLIGHT_SPEED, 0.12);
            case HULKBUSTER -> IronManAbilities.cfg(IronManConfig.HULKBUSTER_FLIGHT_SPEED, 0.06);
        };
    }

    /** The energy a second of flight costs in this suit ({@code [mark_*] flightEnergyPerSecond}). */
    public static double flightCost(SuitKind kind) {
        return switch (kind) {
            case MARK_1 -> 0.0;
            case MARK_3 -> IronManAbilities.cfg(IronManConfig.MARK3_FLIGHT_ENERGY_PER_SECOND, 0.6);
            case MARK_5 -> IronManAbilities.cfg(IronManConfig.MARK5_FLIGHT_ENERGY_PER_SECOND, 0.5);
            case MARK_50 -> IronManAbilities.cfg(IronManConfig.MARK50_FLIGHT_ENERGY_PER_SECOND, 0.4);
            case MARK_85 -> IronManAbilities.cfg(IronManConfig.MARK85_FLIGHT_ENERGY_PER_SECOND, 0.35);
            case HULKBUSTER -> IronManAbilities.cfg(IronManConfig.HULKBUSTER_FLIGHT_ENERGY_PER_SECOND, 1.0);
        };
    }

    /**
     * Whether Magneto can take him by his suit (PLAN 5.11): a powered classic suit is iron ({@code [iron_man]
     * magneticClassicSuits}); nanites are not.
     */
    public static boolean isMetal(Player player) {
        SuitKind kind = worn(player);
        return kind != null && kind.classic() && powered(player)
                && IronManAbilities.cfg(IronManConfig.MAGNETIC_CLASSIC_SUITS, true);
    }

    /**
     * Iron Man pressed the Change Look key (H; {@code step} -1 with Sneak): suit up, take off, deploy or switch (PLAN
     * 5.5, {@link SuitControl}).
     */
    public static void onSuitKey(ServerPlayer player, int step) {
        SuitControl.onKey(player, step);
        Powers.syncIfDirty(player);
    }

    private IronSuits() {
    }
}
