package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * H, the suit key (3.8, PLAN 5.5): Iron Man's Change Look key ({@code ChangeLookPayload} → {@link
 * IronSuits#onSuitKey}). In a classic suit, H takes it off (the Mark 5 folds back into its suitcase in the hand); with
 * the Nano Arc Reactor on the chest, H deploys or retracts the selected nanotech suit and Sneak + H switches it among
 * the housings he carries; unsuited, H suits him up from the inventory (the last suit he wore if he carries it, else
 * the Mark 5's suitcase, else the best classic suit) and Sneak + H picks which; during a phase, H skips it. Also where
 * the items move: onto the chest at the start of a suit-up, off it at the end of a take-off ({@link SuitPhases}), and
 * the suits put on by hand.
 */
public final class SuitControl {
    /** The order H prefers when it suits him up from the inventory (the Hulkbuster only by Veronica). */
    private static final SuitKind[] CLASSIC_ORDER = {SuitKind.MARK_5, SuitKind.MARK_3, SuitKind.MARK_1};

    /** H (step -1: with Sneak). */
    public static void onKey(ServerPlayer player, int step) {
        if (Powers.power(player) != Power.IRON_MAN || !player.isAlive() || player.isSpectator()) {
            return;
        }
        IronManState state = IronSuits.state(player);
        if (state.inPhase()) {
            SuitPhases.skip(player);
            return;
        }
        boolean sneak = step < 0;
        SuitKind chest = IronSuits.kind(player.getItemBySlot(EquipmentSlot.CHEST));
        if (chest != null && chest.classic()) {
            if (!sneak) {
                SuitPhases.begin(player, chest, false);
            }
            return;
        }
        if (IronSuits.nanoReactorOn(player)) {
            if (sneak) {
                switchNano(player);
            } else if (state.has(IronManState.NANO_OUT)) {
                SuitKind out = SuitKind.of(state.nanoSuit());
                if (out != null) {
                    SuitPhases.begin(player, out, false);
                }
            } else {
                deploy(player);
            }
            return;
        }
        if (sneak) {
            cycleNext(player);
        } else {
            SuitKind next = nextClassic(player);
            if (next == null) {
                say(player, "message.enhanced.iron_man.no_suit");
            } else {
                putOn(player, next, findStack(player, next), false);
            }
        }
    }

    // ---- the classic suits ----

    /**
     * Suits him up in a classic suit from {@code stack} (in his inventory or hand): the UP phase begins and the item
     * goes onto the chest slot at once (what the chest held goes to the inventory, or is dropped). The Mark 5 unfolds.
     *
     * @return whether it began
     */
    public static boolean putOn(ServerPlayer player, SuitKind kind, ItemStack stack, boolean byHand) {
        if (stack.isEmpty() || !kind.classic()) {
            return false;
        }
        if (!IronSuits.mayWear(player)) {
            say(player, "message.enhanced.iron_man.only_iron_man");
            return false;
        }
        if (!SuitPhases.begin(player, kind, true, SuitPhases.length(kind, true), byHand)) {
            return false;
        }
        ItemStack suit = stack.copy();
        stack.setCount(0);
        if (kind == SuitKind.MARK_5) {
            suit.set(ModDataComponents.SUIT.get(), SuitItemState.UNFOLDED);
        }
        ItemStack old = player.getItemBySlot(EquipmentSlot.CHEST);
        player.setItemSlot(EquipmentSlot.CHEST, suit);
        if (!old.isEmpty()) {
            give(player, old);
        }
        IronSuits.setState(player, IronSuits.state(player).withSuit(kind));
        return true;
    }

    /** The end of a take-off (or an aborted suit-up): the suit comes off. */
    static void takeOff(ServerPlayer player, SuitKind kind) {
        IronManState state = IronSuits.state(player);
        if (kind.nanotech()) {
            IronSuits.setState(player, state.with(IronManState.NANO_OUT, false).with(IronManState.POWERED, false)
                    .withSuit(null));
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (IronSuits.kind(chest) == kind) {
            ItemStack suit = chest.copy();
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            if (kind == SuitKind.MARK_5) {
                suit.set(ModDataComponents.SUIT.get(), SuitItemState.FOLDED);
                if (player.getMainHandItem().isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, suit);
                } else {
                    give(player, suit);
                }
            } else {
                give(player, suit);
            }
        }
        IronSuits.setState(player, IronSuits.state(player).with(IronManState.POWERED, false).withSuit(null)
                .withLast(kind));
    }

    /**
     * A classic suit appeared on his chest without H (put on by hand, a dispenser, a command): its suit-up plays
     * (a shorter one is the listener's business); when it may not (the cooldown), it is simply on.
     */
    static void putOnByHand(ServerPlayer player, SuitKind kind) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (kind == SuitKind.MARK_5) {
            chest.set(ModDataComponents.SUIT.get(), SuitItemState.UNFOLDED);
        }
        IronSuits.setState(player, IronSuits.state(player).withSuit(kind));
        if (!IronSuits.state(player).inPhase() && Powers.power(player) == Power.IRON_MAN) {
            // (3.8.1: the Hulkbuster's by-hand version is its short one from the start, as its listener wants it.)
            int ticks = kind == SuitKind.HULKBUSTER
                    ? com.kolodkingal.enhanced.ironman.hulkbuster.Hulkbuster.BY_HAND_TICKS
                    : SuitPhases.length(kind, true);
            SuitPhases.begin(player, kind, true, ticks, true);
        }
    }

    /**
     * Which carried classic suit H puts on next: the chosen or last one, else the suitcase, else Mark 3, else Mark 1.
     */
    @Nullable
    public static SuitKind nextClassic(ServerPlayer player) {
        SuitKind last = SuitKind.of(IronSuits.state(player).lastSuit());
        if (last != null && last.classic() && last != SuitKind.HULKBUSTER && IronSuits.carries(player, last)) {
            return last;
        }
        for (SuitKind kind : CLASSIC_ORDER) {
            if (IronSuits.carries(player, kind)) {
                return kind;
            }
        }
        return null;
    }

    /** Sneak + H unsuited: the next carried classic suit becomes the one H puts on (the HUD names it). */
    private static void cycleNext(ServerPlayer player) {
        SuitKind current = nextClassic(player);
        SuitKind[] order = {SuitKind.MARK_1, SuitKind.MARK_3, SuitKind.MARK_5};
        int start = current == null ? -1 : java.util.Arrays.asList(order).indexOf(current);
        for (int i = 1; i <= order.length; i++) {
            SuitKind kind = order[Math.floorMod(start + i, order.length)];
            if (IronSuits.carries(player, kind)) {
                IronSuits.setState(player, IronSuits.state(player).withLast(kind));
                player.displayClientMessage(Component.translatable("message.enhanced.iron_man.next_suit",
                        Component.translatable(IronSuits.item(kind).getDescriptionId())).withStyle(ChatFormatting.GOLD),
                        true);
                return;
            }
        }
        say(player, "message.enhanced.iron_man.no_suit");
    }

    // ---- the nanotech suits ----

    /** H with the Nano Arc Reactor on: the selected housing (or the other one he carries) flows out. */
    private static void deploy(ServerPlayer player) {
        IronManState state = IronSuits.state(player);
        SuitKind selected = SuitKind.of(state.nanoSuit());
        SuitKind kind = selected != null && selected.nanotech() && IronSuits.carries(player, selected) ? selected
                : otherNano(player, selected);
        if (kind == null) {
            say(player, "message.enhanced.iron_man.no_housing");
            return;
        }
        if (SuitPhases.begin(player, kind, true)) {
            IronSuits.setState(player, IronSuits.state(player).withNanoSuit(kind).with(IronManState.NANO_OUT, true)
                    .withSuit(kind));
        }
    }

    /** Sneak + H: the other housing he carries is selected (and, if a suit is out, it changes at once). */
    private static void switchNano(ServerPlayer player) {
        IronManState state = IronSuits.state(player);
        SuitKind selected = SuitKind.of(state.nanoSuit());
        SuitKind other = otherNano(player, selected);
        if (other == null) {
            if (selected == null || !IronSuits.carries(player, selected)) {
                say(player, "message.enhanced.iron_man.no_housing");
            }
            return;
        }
        boolean out = state.has(IronManState.NANO_OUT);
        if (out && selected != null) {
            IronManAbilities.endMoves(player, selected);
            IronSuits.setState(player, state.with(IronManState.NANO_OUT, false).withSuit(null).withNanoSuit(other));
            if (SuitPhases.begin(player, other, true)) {
                IronSuits.setState(player, IronSuits.state(player).with(IronManState.NANO_OUT, true).withSuit(other));
            }
        } else {
            IronSuits.setState(player, state.withNanoSuit(other));
        }
        player.displayClientMessage(Component.translatable("message.enhanced.iron_man.next_suit",
                Component.translatable(IronSuits.item(other).getDescriptionId())).withStyle(ChatFormatting.GOLD), true);
    }

    /** The carried nanotech suit that is not {@code not}, or null. */
    @Nullable
    private static SuitKind otherNano(ServerPlayer player, @Nullable SuitKind not) {
        for (SuitKind kind : new SuitKind[]{SuitKind.MARK_85, SuitKind.MARK_50}) {
            if (kind != not && IronSuits.carries(player, kind)) {
                return kind;
            }
        }
        return null;
    }

    /**
     * A deployed nanotech suit goes back at once, with no phase (the reactor came off the chest, the housing left his
     * inventory, the power was lost, he died).
     */
    public static void retractNow(ServerPlayer player) {
        IronManState state = IronSuits.state(player);
        if (!state.has(IronManState.NANO_OUT)) {
            return;
        }
        SuitKind out = SuitKind.of(state.nanoSuit());
        if (out != null) {
            IronManAbilities.endMoves(player, out);
        }
        if (state.inPhase()) {
            SuitPhases.complete(player, true);
            state = IronSuits.state(player);
        }
        IronSuits.setState(player, state.with(IronManState.NANO_OUT, false).with(IronManState.POWERED, false)
                .withSuit(null));
    }

    // ---- every tick: what changed on the chest ----

    /**
     * Every tick of Iron Man: a classic suit put on by hand plays its suit-up; a classic suit gone from the chest is
     * forgotten; a deployed nanotech suit whose reactor or housing is gone retracts at once.
     */
    static void watch(ServerPlayer player) {
        IronManState state = IronSuits.state(player);
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        SuitKind chest = IronSuits.kind(chestStack);
        if (state.has(IronManState.NANO_OUT)) {
            SuitKind out = SuitKind.of(state.nanoSuit());
            if (!IronSuits.nanoReactorOn(player) || out == null || !IronSuits.carries(player, out)) {
                retractNow(player);
            }
            return;
        }
        SuitKind known = SuitKind.of(state.suit());
        if (chest != null && chest.classic()) {
            if (known != chest) {
                putOnByHand(player, chest);
            }
        } else if (known != null && known.classic()) {
            // (Taken off by hand or by a command: nothing to give back; its moves end with the power check.)
            if (state.inPhase()) {
                SuitPhases.complete(player, true);
            }
            IronSuits.setState(player, IronSuits.state(player).withSuit(null).with(IronManState.POWERED, false)
                    .withLast(known));
        }
    }

    // ---- helpers ----

    /** The first stack of this suit's item he carries (the hands first), or empty. */
    static ItemStack findStack(ServerPlayer player, SuitKind kind) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(IronSuits.item(kind))) {
                return held;
            }
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(IronSuits.item(kind))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Into the inventory, or dropped at his feet when it is full. */
    static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    /** Whether he is suited in anything (a classic suit on the chest or a nanotech suit out). */
    public static boolean suited(ServerPlayer player) {
        return IronSuits.worn(player) != null;
    }

    /** The Nano Arc Reactor's item (for the tests and the HUD). */
    static boolean isNanoReactor(ItemStack stack) {
        return stack.is(ModItems.NANO_ARC_REACTOR.get());
    }

    private SuitControl() {
    }
}
