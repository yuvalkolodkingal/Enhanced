package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.item.InfinityStoneItem;
import com.kolodkingal.enhanced.platform.event.LivingChangeTargetEvent;
import com.kolodkingal.enhanced.platform.event.LivingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.ProjectileImpactEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Where the stones' powers meet the game (3.6, lane L2): the Time Stone on a crop or a baby animal (a bare stone's and
 * a Gauntlet's own uses are their items', lane L1's, which call {@link StoneUse}), every player's tick
 * ({@link StoneUse#tick}), a projectile about to hit a Reality Stone's holder, the blows that tear a Soul Drain, the
 * Mind Command's allies who never turn on their commander, and what is forgotten when a player leaves. The vessels
 * call {@link StoneUse} themselves ({@code ScepterRituals}, the Eye of Agamotto).
 */
@Subscriber
public final class StoneEvents {
    static {
        StoneBeams.init();
        AreaTargets.addAllyRule(MindStonePowers::isCommandedBy);
    }

    /** A stone in a hand, and where it is. */
    private record InHand(StoneKind kind, StoneUse.Source source) {
    }

    /**
     * The stone a use of this stack would be: a bare stone, the selected stone of a crafted Gauntlet (not the
     * Titan's, not empty, and not the Snap: Sneak with all six), or the Eye of Agamotto's Time Stone in a hero's hand.
     * Null for anything else.
     */
    @Nullable
    private static InHand stoneOf(ItemStack stack, Player player) {
        boolean sneaking = player.isShiftKeyDown();
        if (stack.getItem() instanceof InfinityStoneItem stone) {
            return new InHand(stone.kind, StoneUse.Source.BARE);
        }
        // (3.6, integration: the Eye of Agamotto is the Time Stone's vessel for a hero, a sorcerer's to use; in a
        // powerless player's hand it is held for the awakening.)
        if (stack.is(ModItems.EYE_OF_AGAMOTTO.get())) {
            return Powers.power(player).hasPowers() ? new InHand(StoneKind.TIME, StoneUse.Source.VESSEL) : null;
        }
        if (!stack.is(ModItems.INFINITY_GAUNTLET.get())) {
            return null;
        }
        GauntletContents gauntlet = stack.get(ModDataComponents.GAUNTLET.get());
        if (gauntlet == null || gauntlet.titan() || gauntlet.selectedStone() == null || sneaking && gauntlet.full()) {
            return null;
        }
        return new InHand(gauntlet.selectedStone(), StoneUse.Source.GAUNTLET);
    }

    /**
     * A stone in {@code hand} is used as its item does it (lane L1's {@code InfinityStoneItem} and
     * {@code InfinityGauntletItem} call {@link StoneUse#use} on the press and on the release): for the tests.
     */
    public static void useInHand(ServerPlayer player, InteractionHand hand, StoneKind kind, StoneUse.Source source,
                                 boolean alt) {
        if (StoneUse.holding(player)) {
            return; // (a held use already burns: a repeated click changes nothing)
        }
        StoneUse.use(player, kind, source, alt, true);
        if (StoneUse.holding(player, kind) && !player.isUsingItem()) {
            player.startUsingItem(hand);
        }
    }

    /** The Time Stone on a crop or a sapling grows it (with Sneak, takes a crop back). */
    @Subscribe
    public static void onUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        InHand stone = stoneOf(event.getItemStack(), player);
        if (stone == null || stone.kind() != StoneKind.TIME
                || !TimeStonePowers.isCropTarget(event.getLevel().getBlockState(event.getPos()),
                player.isShiftKeyDown())) {
            return;
        }
        if (player instanceof ServerPlayer server
                && !TimeStonePowers.cropMagic(server, stone.source(), event.getPos(), player.isShiftKeyDown())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** The Time Stone on a baby animal: it grows up. */
    @Subscribe
    public static void onUseOnEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        InHand stone = stoneOf(event.getItemStack(), player);
        if (stone == null || stone.kind() != StoneKind.TIME || player.isShiftKeyDown()
                || !(event.getTarget() instanceof AgeableMob mob) || !mob.isBaby()) {
            return;
        }
        if (player instanceof ServerPlayer server && !TimeStonePowers.ageUp(server, stone.source(), mob)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive()) {
            StoneUse.tick(player);
        }
    }

    /** A projectile about to hit a holder of the Reality Stone is a bubble instead. */
    @Subscribe(priority = Priority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide() && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof ServerPlayer player
                && RealityBubbles.catches(player, event.getProjectile())) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulStonePowers.hurt(player, event.getNewDamage());
        }
    }

    /** A creature under the Mind Command never takes its commander, nor his other allies, as its target. */
    @Subscribe
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!MindStonePowers.mayTarget(event.getEntity(), event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoneUse.forget(player);
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoneUse.forget(player);
            StoneCooldowns.forget(player);
        }
    }

    @Subscribe
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoneUse.end(player, true);
            if (TimeStonePowers.slowing(player)) {
                TimeStonePowers.stopSlow(player, com.kolodkingal.enhanced.power.Powers.get(player), false);
            }
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        StoneUse.clear();
    }

    private StoneEvents() {
    }
}
