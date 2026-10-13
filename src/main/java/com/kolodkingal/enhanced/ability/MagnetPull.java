package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Magneto's Magnetic Pull: Sneak + R with nothing held draws the metal all round him into his inventory (D-M 4). Ores give
 * their metal through rock, loose metal, stuck arrows and empty carts come, hostile creatures lose their metal gear, and
 * built metal comes whole when he can see it in the cone he faces. One job per Magneto, sliced over ticks.
 *
 * <p>A stub of the skeleton (3.1, step S): {@link #press} declines every press, so {@code Telekinesis.press} goes on as in
 * 3.0 and Sneak + R still grabs; {@link #wouldTakeBuilt} already answers R's message about a fixed metal block. Lane MAG
 * fills it (D-M 4.1 to 4.8).
 */
public final class MagnetPull {

    /**
     * Sneak + R with nothing held (called by {@code Telekinesis.press}, after a grip's set-down or throw and the lift's
     * refusal, before anything else). Lane MAG: the refusals of D-M 4.1 (a Spectator, a pull already running, riding, the
     * grace after a grip, the retry, the cooldown, hunger), then the job starts.
     *
     * @return true if the pull took the press (nothing else of R runs); false to let R go on as without the pull (always,
     * in the skeleton)
     */
    public static boolean press(ServerPlayer player) {
        return false;
    }

    /**
     * Whether the pull would take this built metal block whole, which R's message at a block it cannot lift names
     * ({@code telekinesis.fixed_metal} or {@code fixed_metal_off}, R34): {@code magneto.pullTakesBuiltMetal} is on, the
     * block holds no items ({@link #holdsItems}) and it is no piston that is extended or powered (R36). Lane MAG adds the
     * block a villager has claimed (D-M 4.2).
     */
    public static boolean wouldTakeBuilt(ServerLevel level, BlockPos pos, BlockState state) {
        if (!EnhancedConfig.PULL_TAKES_BUILT_METAL.get() || holdsItems(level, pos)) {
            return false;
        }
        return !(state.getBlock() instanceof PistonBaseBlock)
                || (!state.getValue(PistonBaseBlock.EXTENDED) && !level.hasNeighborSignal(pos));
    }

    /**
     * Whether the block at {@code pos} holds items (a hopper, a blast furnace or a crafter with anything inside, a container
     * whose loot is still to be rolled, or another mod's storage): such a block never comes whole, and R says {@code
     * telekinesis.fixed_metal_full} at it (R36). A pure query: it rolls no loot.
     */
    public static boolean holdsItems(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return false;
        }
        if (entity instanceof RandomizableContainer loot && loot.getLootTable() != null) {
            return true; // asking it whether it is empty would roll its loot
        }
        if (entity instanceof Container container) {
            return !container.isEmpty();
        }
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (items != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                if (!items.getStackInSlot(slot).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** One tick of this Magneto's pull, if one runs (from {@code MagnetoAbilities.tick}). Lane MAG: D-M 4.5. */
    public static void tick(ServerPlayer player) {
    }

    /**
     * Ends this Magneto's pull at once (he died, logged out, changed dimension, lost his power); what was taken stays his.
     * Lane MAG: D-M 4.8.
     */
    public static void cancel(ServerPlayer player) {
    }

    /** Forgets every pull and every grace: for the server's stop and for tests. */
    public static void clear() {
    }

    private MagnetPull() {
    }
}
