package com.kolodkingal.enhanced.road.copy;

import com.kolodkingal.enhanced.platform.event.BlockDropsEvent;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.TriState;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The events of the Copy the Structure trial: the Check and Hint buttons, breaking blocks on the plot instantly (and getting the
 * block back instead of a drop), and returning the player his things if he logs in with them still put aside.
 */
@Subscriber
public final class StructureCopyEvents {

    @Subscribe
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player) || !RoadDimension.in(player)) {
            return;
        }
        RoadTrial trial = RoadTrials.trialOf(player.getUUID());
        if (trial != null && trial.challenge() instanceof StructureCopyChallenge && !StructureCopyChallenge.use(trial, player, event.getPos())
                && event.getUseItem() == TriState.FALSE && event.getItemStack().getItem() instanceof BlockItem) {
            // A block in his hand that the Road would not let him put down (it is not on the plot).
            StructureCopyChallenge.remindPlot(player);
        }
    }

    /** Hitting a block that is not his to break (the model, the platform). */
    @Subscribe(priority = Priority.LOWEST, receiveCanceled = true)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.isCanceled() && event.getEntity() instanceof ServerPlayer player && RoadDimension.in(player)) {
            RoadTrial trial = RoadTrials.trialOf(player.getUUID());
            if (trial != null && trial.challenge() instanceof StructureCopyChallenge) {
                StructureCopyChallenge.remindPlot(player);
            }
        }
    }

    /** In the Road (both sides, so that the client shows it as fast as the server does it) a block breaks in an instant: only the plot's can be broken at all. */
    @Subscribe
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (RoadDimension.in(event.getEntity())) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), 1000.0F));
        }
    }

    /**
     * A block the player breaks on the plot gives itself back to his supply (whatever the block would drop, if it would drop anything: glass
     * drops nothing, stone drops nothing without a pickaxe, a double slab is two slabs). Last of all, so that a break something else refused gives nothing.
     */
    @Subscribe(priority = Priority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level) || !RoadDimension.is(level)) {
            return;
        }
        RoadTrial trial = RoadTrials.trialOf(player.getUUID());
        if (trial == null || !(trial.challenge() instanceof StructureCopyChallenge) || trial.phase() != RoadTrial.Phase.ACTIVE) {
            return;
        }
        Item item = event.getState().getBlock().asItem();
        if (item != Items.AIR) {
            // A full backpack loses nothing: the supply chest fills itself up again.
            player.getInventory().add(StructureSupply.stack(item, StructureCompare.itemCount(event.getState())));
        }
    }

    /** What a block on the plot would drop is not dropped: it is the player's supply, which came back when he broke it. */
    @Subscribe(priority = Priority.HIGH)
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() instanceof ServerPlayer player && RoadDimension.is(event.getLevel())) {
            RoadTrial trial = RoadTrials.trialOf(player.getUUID());
            if (trial != null && trial.challenge() instanceof StructureCopyChallenge) {
                event.setCanceled(true);
            }
        }
    }

    /** What a player crafts in the Road (from the trial's blocks, in the 2x2 square) is the trial's too: marked, so that it is taken back with the rest. */
    @Subscribe
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (RoadDimension.in(event.getEntity())) {
            StructureSupply.mark(event.getCrafting());
        }
    }

    /**
     * A player who logs in with his things still put aside (the server stopped in the trial) gets them back. If the Road takes him into the trial
     * again, it puts them aside again; and if it takes him into another trial (an operator forced this one) or sends him home, they are his.
     */
    @Subscribe
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && (StructureSupply.hasStash(player, StructureCopyChallenge.ID) || hasSupply(player))) {
            StructureSupply.restore(player, StructureCopyChallenge.ID);
        }
    }

    private static boolean hasSupply(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (StructureSupply.isSupply(player.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private StructureCopyEvents() {
    }
}
