package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.item.IronSuitItem;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Iron Man's suits, the API every lane and shared seam calls (3.8, PLAN 5.2-5.6, 5.11). The 3.8 skeleton writes the
 * signatures with neutral bodies (no suit ever powers, flies or is metal; H says the suits are not built yet) and the
 * plain reads of the state and the inventory; lane L3 fills the bodies (L4 the nanotech parts) and keeps the
 * signatures. Seams that call it: {@code PowerTicker.applyPassives} ({@link #canFly}, {@link #flightSpeed}), {@code
 * ChangeLookPayload} ({@link #onSuitKey}), {@code Metal.isMagnetic} ({@link #isMetal}).
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
     * Whether he may put a suit on: Iron Man ({@code [iron_man] onlyIronManWears}), or a Creative player (builders).
     * (The skeleton's rule as the plan says it; lane L3 owns it.)
     */
    public static boolean mayWear(Player player) {
        return player.isCreative() || Powers.power(player) == Power.IRON_MAN;
    }

    /** Whether his suit works (PLAN 5.3: Iron Man, worn, a reactor, no phase, not broken). Skeleton: never. */
    public static boolean powered(Player player) {
        return false;
    }

    /** Whether he may fly now (PLAN 5.6: powered, the suit flies, energy left). Skeleton: never. */
    public static boolean canFly(ServerPlayer player) {
        return false;
    }

    /** His flight speed (the worn suit's; 0.05 unsuited). Skeleton: 0.05. */
    public static float flightSpeed(Player player) {
        return 0.05F;
    }

    /** Whether Magneto can take him by his suit (a powered classic suit, PLAN 5.11). Skeleton: never. */
    public static boolean isMetal(Player player) {
        return false;
    }

    /**
     * Iron Man pressed the Change Look key (H; {@code step} -1 with Sneak): suit up, take off, deploy or switch (PLAN
     * 5.5). Skeleton: he is told the suits are not built yet.
     */
    public static void onSuitKey(ServerPlayer player, int step) {
        player.displayClientMessage(Component.translatable("message.enhanced.iron_man.not_yet"), true);
    }

    private IronSuits() {
    }
}
