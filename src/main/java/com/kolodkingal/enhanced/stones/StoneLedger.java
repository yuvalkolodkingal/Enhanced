package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SoulStoneConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.config.StonesConfig.Uniqueness;
import com.kolodkingal.enhanced.item.InfinityStoneItem;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * How many stones there are (3.6, the plan's 1.4): the four site stones (Reality, Time, Power, Soul) are counted by
 * {@code [stones] uniqueness} (the Soul Stone by {@code [soul_stone] onePer}): {@code player}, each player may take
 * each site stone once (a bit of {@link StoneProgress#taken}); {@code world}, one of each in the whole world (the
 * SavedData {@code stone_ledger} of the Overworld names its holder); {@code unlimited}, every site gives its stone
 * every time. The Mind and Space Stones are as rare as their vessels and are not counted.
 *
 * <p><b>Lost stones</b> come back ({@code [stones] returnLost}): a stone item killed or fallen out of the world, or a
 * stack destroyed for good ({@link #destroyed}: the sacrifice). In {@code world} mode the kind is "at its sites" again;
 * in {@code player} mode the bit of the player who took it is cleared, so he may take it once more. Who took a stone is
 * written on the stack ({@link #stamp}, a line of its {@code custom_data}; a Gauntlet keeps the names of its stones'
 * takers the same way), so a stone given away and then lost frees its taker, not its last holder. A player who is not
 * on the server when his stone is lost gets his bit back when he next asks or logs in.
 */
public final class StoneLedger {
    /** The stones a site gives and the ledger counts. */
    public static final Set<StoneKind> SITE_STONES = Set.of(StoneKind.REALITY, StoneKind.TIME, StoneKind.POWER,
            StoneKind.SOUL);
    /** The SavedData of the Overworld: the world mode's holders, and the player mode's bits waiting to be cleared. */
    public static final String NAME = "stone_ledger";
    /** The line of a stone's (or the Orb's) {@code custom_data} that names who took it from its site. */
    static final String TAKER = "enhanced_stone_taker";
    /** The lines of a Gauntlet's {@code custom_data} that name its stones' takers ({@code <kind>: uuid}). */
    static final String TAKERS = "enhanced_stone_takers";

    /** Whether the ledger counts this stone (a site stone). */
    public static boolean isSiteStone(StoneKind kind) {
        return SITE_STONES.contains(kind);
    }

    /** How the ledger counts this kind now: its config (the Soul Stone's own); {@code unlimited} for Mind and Space. */
    public static Uniqueness mode(StoneKind kind) {
        if (!isSiteStone(kind)) {
            return Uniqueness.UNLIMITED;
        }
        if (!EnhancedConfig.SPEC.isLoaded()) {
            return Uniqueness.PLAYER;
        }
        if (kind == StoneKind.SOUL) {
            return switch (SoulStoneConfig.ONE_PER.get()) {
                case WORLD -> Uniqueness.WORLD;
                case UNLIMITED -> Uniqueness.UNLIMITED;
                default -> Uniqueness.PLAYER;
            };
        }
        return StonesConfig.UNIQUENESS.get();
    }

    /** Whether the player may take this site stone now (from a pedestal, the Orb's chest, the sacrifice). */
    public static boolean mayTake(ServerPlayer player, StoneKind kind) {
        return switch (mode(kind)) {
            case UNLIMITED -> true;
            case WORLD -> data(player.server).holder(kind) == null;
            case PLAYER -> !progress(player).hasTaken(kind);
        };
    }

    /**
     * Why the player may not take it (a message key with the stone's name as its argument), or null when he may:
     * {@code message.enhanced.stones.already_taken} (player mode), {@code message.enhanced.stones.one_per_world}.
     */
    @Nullable
    public static String refusal(ServerPlayer player, StoneKind kind) {
        if (mayTake(player, kind)) {
            return null;
        }
        return mode(kind) == Uniqueness.WORLD ? "message.enhanced.stones.one_per_world"
                : "message.enhanced.stones.already_taken";
    }

    /** The player has taken this site stone (the ledger marks it; nothing for a kind it does not count). */
    public static void taken(ServerPlayer player, StoneKind kind) {
        switch (mode(kind)) {
            case WORLD -> data(player.server).setHolder(kind, player.getUUID());
            case PLAYER -> ModAttachments.STONES.set(player, progress(player).withTaken(kind, true));
            default -> {
            }
        }
    }

    /**
     * A stack is destroyed for good (a sacrificed player's items, a lost stone): every site stone in it (in a Gauntlet,
     * the Orb, a bundle or a shulker box in it too) returns to its site. The sacrifice (lane L5) calls it; so do the
     * ledger's own watches.
     */
    public static void destroyed(ItemStack stack) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || stack.isEmpty() || !returnLost()) {
            return;
        }
        Map<StoneKind, UUID> lost = new EnumMap<>(StoneKind.class);
        Set<StoneKind> found = java.util.EnumSet.noneOf(StoneKind.class);
        collect(stack, lost, found, 0);
        for (StoneKind kind : found) {
            returnToSite(server, kind, lost.get(kind));
        }
    }

    /** The site stones in a stack (and in what it holds), with their takers where they are written. */
    static void collect(ItemStack stack, Map<StoneKind, UUID> takers, Set<StoneKind> found, int depth) {
        if (stack.isEmpty() || depth > 3) {
            return;
        }
        if (stack.getItem() instanceof InfinityStoneItem stone) {
            put(takers, found, stone.kind, taker(stack));
        } else if (stack.is(ModItems.THE_ORB.get())) {
            put(takers, found, StoneKind.POWER, taker(stack));
        } else if (stack.is(ModItems.EYE_OF_AGAMOTTO.get()) && taker(stack) != null) {
            // (3.6, integration: an Eye crafted round a site's Time Stone carries its taker; an Eye that never held a
            // site's stone, such as the one a new Doctor Strange is given, frees nothing.)
            put(takers, found, StoneKind.TIME, taker(stack));
        } else if (stack.is(ModItems.INFINITY_GAUNTLET.get())) {
            GauntletContents contents = stack.get(ModDataComponents.GAUNTLET.get());
            if (contents != null && !contents.titan()) {
                for (StoneKind kind : StoneKind.values()) {
                    if (contents.has(kind)) {
                        put(takers, found, kind, gauntletTakerOf(stack, kind));
                    }
                }
            }
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            for (ItemStack inside : bundle.items()) {
                collect(inside, takers, found, depth + 1);
            }
        }
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            for (ItemStack inside : container.nonEmptyItems()) {
                collect(inside, takers, found, depth + 1);
            }
        }
    }

    private static void put(Map<StoneKind, UUID> takers, Set<StoneKind> found, StoneKind kind, @Nullable UUID taker) {
        if (isSiteStone(kind)) {
            found.add(kind);
            if (taker != null) {
                takers.put(kind, taker);
            }
        }
    }

    /**
     * A site stone is back at its sites: in {@code world} mode it has no holder any more; in {@code player} mode its
     * taker (if known) may take it again.
     */
    static void returnToSite(MinecraftServer server, StoneKind kind, @Nullable UUID taker) {
        switch (mode(kind)) {
            case WORLD -> data(server).setHolder(kind, null);
            case PLAYER -> {
                if (taker == null) {
                    return;
                }
                ServerPlayer online = server.getPlayerList().getPlayer(taker);
                if (online != null) {
                    ModAttachments.STONES.set(online, progress(online).withTaken(kind, false));
                } else {
                    data(server).owe(taker, kind);
                }
            }
            default -> {
            }
        }
    }

    /** The player's progress, with the bits owed back to him (stones lost while he was away) cleared first. */
    static StoneProgress progress(ServerPlayer player) {
        StoneProgress progress = ModAttachments.STONES.get(player);
        int owed = data(player.server).takeOwed(player.getUUID());
        if (owed != 0) {
            progress = new StoneProgress(progress.taken() & ~owed, progress.flags(), progress.snapReadyAt());
            ModAttachments.STONES.set(player, progress);
        }
        return progress;
    }

    /** Whether lost stones return ({@code [stones] returnLost}). */
    static boolean returnLost() {
        return !EnhancedConfig.SPEC.isLoaded() || StonesConfig.RETURN_LOST.get();
    }

    /** The world mode's holder of a kind (null: at its sites). */
    @Nullable
    public static UUID holder(MinecraftServer server, StoneKind kind) {
        return data(server).holder(kind);
    }

    /** Forgets the world mode's holders and the bits owed (a test's clean slate; an operator's reset). */
    public static void reset(MinecraftServer server) {
        Data data = data(server);
        data.holders.clear();
        data.owed.clear();
        data.setDirty();
    }

    // ---- who took a stone, written on the stack ----

    /** Writes on a site stone (or the Orb) who took it from its site. */
    public static void stamp(ItemStack stack, UUID taker) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(TAKER, taker));
    }

    /**
     * A crafted stack takes the taker written on the ingredient it was made from (3.6: the Time Stone set in the Eye of
     * Agamotto, and back out of it): looks in the crafting grid of the menu the player has open for a stack {@code from}
     * accepts that carries a taker. Called from the items' {@code onCraftedBy} (both on a click and a Shift-click, while
     * the grid still holds the ingredients).
     */
    public static void carryTaker(ItemStack crafted, Player player, Predicate<ItemStack> from) {
        if (player.level().isClientSide || crafted.isEmpty()) {
            return;
        }
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container instanceof CraftingContainer && from.test(slot.getItem())) {
                UUID taker = taker(slot.getItem());
                if (taker != null) {
                    stamp(crafted, taker);
                    return;
                }
            }
        }
    }

    /** Who took this stone (or the Orb) from its site, or null. */
    @Nullable
    public static UUID taker(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(TAKER)) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        return tag.hasUUID(TAKER) ? tag.getUUID(TAKER) : null;
    }

    /** Copies a stone's taker into a Gauntlet it goes into (or clears the line when the stone has none). */
    public static void putInGauntletOf(ItemStack gauntlet, StoneKind kind, @Nullable UUID taker) {
        String key = kind.name().toLowerCase(Locale.ROOT);
        CustomData before = gauntlet.get(DataComponents.CUSTOM_DATA);
        if (taker == null && (before == null || !before.contains(TAKERS))) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, gauntlet, tag -> {
            CompoundTag takers = tag.getCompound(TAKERS);
            if (taker == null) {
                takers.remove(key);
            } else {
                takers.putUUID(key, taker);
            }
            if (takers.isEmpty()) {
                tag.remove(TAKERS);
            } else {
                tag.put(TAKERS, takers);
            }
        });
    }

    /** Who took the stone of this kind that is in a Gauntlet, or null. */
    @Nullable
    public static UUID gauntletTakerOf(ItemStack gauntlet, StoneKind kind) {
        CustomData data = gauntlet.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(TAKERS)) {
            return null;
        }
        CompoundTag takers = data.copyTag().getCompound(TAKERS);
        String key = kind.name().toLowerCase(Locale.ROOT);
        return takers.hasUUID(key) ? takers.getUUID(key) : null;
    }

    // ---- the SavedData ----

    static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data.factory(), NAME);
    }

    /** The ledger saved with the Overworld: the world mode's holders and the bits owed to absent players. */
    public static final class Data extends SavedData {
        private final Map<StoneKind, UUID> holders = new EnumMap<>(StoneKind.class);
        private final Map<UUID, Integer> owed = new HashMap<>();

        static SavedData.Factory<Data> factory() {
            return new SavedData.Factory<>(Data::new, Data::load, null);
        }

        private static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            CompoundTag holders = tag.getCompound("holders");
            for (StoneKind kind : StoneKind.values()) {
                String key = kind.name().toLowerCase(Locale.ROOT);
                if (holders.hasUUID(key)) {
                    data.holders.put(kind, holders.getUUID(key));
                }
            }
            CompoundTag owed = tag.getCompound("owed");
            for (String key : owed.getAllKeys()) {
                try {
                    data.owed.put(UUID.fromString(key), owed.getInt(key));
                } catch (IllegalArgumentException ignored) {
                    // a damaged line: nothing is owed
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag holders = new CompoundTag();
            this.holders.forEach((kind, uuid) -> holders.putUUID(kind.name().toLowerCase(Locale.ROOT), uuid));
            tag.put("holders", holders);
            CompoundTag owed = new CompoundTag();
            this.owed.forEach((uuid, bits) -> owed.putInt(uuid.toString(), bits));
            tag.put("owed", owed);
            return tag;
        }

        @Nullable
        UUID holder(StoneKind kind) {
            return holders.get(kind);
        }

        void setHolder(StoneKind kind, @Nullable UUID holder) {
            if (holder == null) {
                holders.remove(kind);
            } else {
                holders.put(kind, holder);
            }
            setDirty();
        }

        void owe(UUID player, StoneKind kind) {
            owed.merge(player, StoneProgress.bit(kind), (a, b) -> a | b);
            setDirty();
        }

        int takeOwed(UUID player) {
            Integer bits = owed.remove(player);
            if (bits == null) {
                return 0;
            }
            setDirty();
            return bits;
        }
    }

    private StoneLedger() {
    }
}
