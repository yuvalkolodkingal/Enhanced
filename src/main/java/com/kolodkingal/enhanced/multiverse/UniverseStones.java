package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.RandomizableContainer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Infinity Stones of the other universes (3.19). Their universes have none of the stones' sites, so the stones lie in
 * chests: a chest of the game's loot opened there for the first time holds, now and then ({@link #CHANCE}), one stone of
 * that universe that nobody has found yet. A universe has one of each: copies may wait in several chests, but the first
 * one a player takes is the stone ({@link UniverseLedger}), and every other copy of it fades away as soon as anyone
 * touches it (in a player's inventory or in a chest that is opened).
 *
 * <p>Every such stone is written with its universe ({@link #ORIGIN}); a stone without it is from 616. A stone works only in
 * its own universe ({@link #refusal}): taken to another, it is just a pretty rock.
 */
public final class UniverseStones {
    /** The line of a stone's {@code custom_data} that names its universe, and the one that names this copy. */
    static final String ORIGIN = "enhanced_universe";
    static final String COPY = "enhanced_universe_copy";
    /** The chance that a chest of loot opened in another universe holds a stone. */
    public static final float CHANCE = 0.12F;

    private UniverseStones() {
    }

    /** The universe a stone (or a Gauntlet's stone) is from: 616 unless it says otherwise. */
    public static int origin(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(ORIGIN)) {
            return Multiverse.MAIN;
        }
        return data.copyTag().getInt(ORIGIN);
    }

    @Nullable
    static String copyOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(COPY)) {
            return null;
        }
        return data.copyTag().getString(COPY);
    }

    /** A new stone of a universe, a copy of its own. */
    public static ItemStack make(StoneKind kind, int universe) {
        ItemStack stack = new ItemStack(ModItems.stone(kind));
        String copy = UUID.randomUUID().toString();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(ORIGIN, universe);
            tag.putString(COPY, copy);
        });
        return stack;
    }

    /** Writes a stone's universe on a stack (a stone crafted out of a vessel keeps nothing: it is from 616). */
    public static void mark(ItemStack stack, int universe) {
        if (universe == Multiverse.MAIN) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(ORIGIN, universe));
    }

    /** Which kind of stone a stack is, or null. */
    @Nullable
    public static StoneKind kindOf(ItemStack stack) {
        for (StoneKind kind : StoneKind.values()) {
            if (stack.is(ModItems.stone(kind))) {
                return kind;
            }
        }
        return null;
    }

    // ---- where a stone works ----

    /**
     * Why a stone from {@code origin} does not work where the player is, or null if it does: a stone works in its own
     * universe only.
     */
    @Nullable
    public static Component refusal(ServerPlayer player, StoneKind kind, int origin) {
        int here = Multiverse.universeAt(player.level(), player.blockPosition());
        if (here == origin) {
            return null;
        }
        return Component.translatable("message.enhanced.multiverse.stone_elsewhere",
                Component.translatable("item.enhanced." + kind.id() + "_stone"), Multiverse.name(origin));
    }

    // ---- the chests ----

    /**
     * A player opens a container in another universe: if it still waits for its loot, the loot is rolled now and,
     * perhaps, a stone of the universe added; then any copy of a stone already found fades from it.
     */
    public static void opened(ServerPlayer player, BlockEntity entity) {
        if (!(entity instanceof Container container)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int universe = Multiverse.universeAt(level, entity.getBlockPos());
        if (entity instanceof RandomizableContainer loot && loot.getLootTable() != null) {
            loot.unpackLootTable(player);
            if (universe != Multiverse.MAIN && level.random.nextFloat() < CHANCE) {
                StoneKind kind = unfound(level.getServer(), universe, level);
                if (kind != null) {
                    List<Integer> empty = new ArrayList<>();
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        if (container.getItem(i).isEmpty()) {
                            empty.add(i);
                        }
                    }
                    if (!empty.isEmpty()) {
                        container.setItem(empty.get(level.random.nextInt(empty.size())), make(kind, universe));
                    }
                }
            }
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (stale(level.getServer(), container.getItem(i))) {
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** A random kind of stone that nobody has found in that universe yet, or null. */
    @Nullable
    static StoneKind unfound(MinecraftServer server, int universe, ServerLevel level) {
        List<StoneKind> open = new ArrayList<>();
        for (StoneKind kind : StoneKind.values()) {
            if (UniverseLedger.get(server).finder(universe, kind) == null) {
                open.add(kind);
            }
        }
        return open.isEmpty() ? null : open.get(level.random.nextInt(open.size()));
    }

    /** Whether the stack is a copy of a stone that has already been found elsewhere. */
    static boolean stale(MinecraftServer server, ItemStack stack) {
        StoneKind kind = kindOf(stack);
        String copy = copyOf(stack);
        if (kind == null || copy == null) {
            return false;
        }
        String found = UniverseLedger.get(server).finder(origin(stack), kind);
        return found != null && !found.equals(copy);
    }

    /**
     * Looks at a player's stones of other universes (every second): the first copy of a stone taken is the stone (it is
     * written as found), and every other copy fades away.
     */
    public static void check(ServerPlayer player) {
        MinecraftServer server = player.server;
        UniverseLedger ledger = UniverseLedger.get(server);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            StoneKind kind = kindOf(stack);
            String copy = copyOf(stack);
            if (kind == null || copy == null) {
                continue;
            }
            int universe = origin(stack);
            String found = ledger.finder(universe, kind);
            if (found == null) {
                ledger.found(universe, kind, copy);
                player.displayClientMessage(Component.translatable("message.enhanced.multiverse.stone_found",
                        Component.translatable("item.enhanced." + kind.id() + "_stone"), Multiverse.name(universe))
                        .withStyle(ChatFormatting.GOLD), false);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6F, 1.2F);
            } else if (!found.equals(copy)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("message.enhanced.multiverse.stone_faded",
                        Component.translatable("item.enhanced." + kind.id() + "_stone"), Multiverse.name(universe))
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    // ---- in a Gauntlet ----

    /** A stone goes into a Gauntlet: the Gauntlet keeps its universe (and its copy) for that socket. */
    public static void intoGauntlet(ItemStack gauntlet, StoneKind kind, ItemStack stone) {
        int origin = origin(stone);
        String copy = copyOf(stone);
        CustomData.update(DataComponents.CUSTOM_DATA, gauntlet, tag -> {
            tag.remove(ORIGIN + "_" + kind.id());
            tag.remove(COPY + "_" + kind.id());
            if (origin != Multiverse.MAIN) {
                tag.putInt(ORIGIN + "_" + kind.id(), origin);
            }
            if (copy != null) {
                tag.putString(COPY + "_" + kind.id(), copy);
            }
        });
    }

    /** A stone comes out of a Gauntlet: it takes its universe (and its copy) back with it. */
    public static void outOfGauntlet(ItemStack gauntlet, StoneKind kind, ItemStack stone) {
        int origin = gauntletOrigin(gauntlet, kind);
        CustomData data = gauntlet.get(DataComponents.CUSTOM_DATA);
        String copyKey = COPY + "_" + kind.id();
        String copy = data != null && data.contains(copyKey) ? data.copyTag().getString(copyKey) : null;
        if (origin != Multiverse.MAIN || copy != null) {
            CustomData.update(DataComponents.CUSTOM_DATA, stone, tag -> {
                if (origin != Multiverse.MAIN) {
                    tag.putInt(ORIGIN, origin);
                }
                if (copy != null) {
                    tag.putString(COPY, copy);
                }
            });
        }
        CustomData.update(DataComponents.CUSTOM_DATA, gauntlet, tag -> {
            tag.remove(ORIGIN + "_" + kind.id());
            tag.remove(COPY + "_" + kind.id());
        });
    }

    /** The universe of a Gauntlet's stone of that kind (616 unless it says otherwise). */
    public static int gauntletOrigin(ItemStack gauntlet, StoneKind kind) {
        CustomData data = gauntlet.get(DataComponents.CUSTOM_DATA);
        String key = ORIGIN + "_" + kind.id();
        if (data == null || !data.contains(key)) {
            return Multiverse.MAIN;
        }
        return data.copyTag().getInt(key);
    }

    /** The universe of the stone a player would use from {@code source} (its first stack that holds it; 616 for a vessel). */
    public static int originOf(net.minecraft.world.entity.player.Player player, StoneKind kind, boolean gauntletFirst) {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(player.getMainHandItem());
        stacks.add(player.getOffhandItem());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            stacks.add(player.getInventory().getItem(i));
        }
        for (ItemStack stack : stacks) {
            if (!gauntletFirst && stack.is(ModItems.stone(kind))) {
                return origin(stack);
            }
            com.kolodkingal.enhanced.stones.GauntletContents gauntlet = stack.get(com.kolodkingal.enhanced.registry.ModDataComponents.GAUNTLET.get());
            if (stack.is(ModItems.INFINITY_GAUNTLET.get()) && gauntlet != null && gauntlet.has(kind)) {
                return gauntletOrigin(stack, kind);
            }
            if (gauntletFirst && stack.is(ModItems.stone(kind))) {
                return origin(stack);
            }
        }
        return Multiverse.MAIN;
    }

    /** The ledger's line for a universe's stone (a tag key). */
    static String key(int universe, StoneKind kind) {
        return universe + "/" + kind.id();
    }
}
