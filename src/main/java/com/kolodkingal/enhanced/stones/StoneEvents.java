package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.item.InfinityStoneItem;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Where the stones' powers meet the game (3.6, lane L2): the use of a bare stone or of a filled Gauntlet's selected
 * stone (right-click, Sneak + right-click, held for the beams and the drain), the Time Stone on a crop or a baby
 * animal, every player's tick ({@link StoneUse#tick}), a projectile about to hit a Reality Stone's holder, the blows
 * that tear a Soul Drain, the Mind Command's allies who never turn on their commander, and what is forgotten when a
 * player leaves. The Gauntlet's Snap (Sneak + hold with all six) stays the Gauntlet's own (lane L1); the vessels call
 * {@link StoneUse} themselves ({@code ScepterRituals}, the Eye of Agamotto).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class StoneEvents {
    /** How long a held stone use may last as an item use (the stone ends it long before). */
    static final int HOLD_DURATION = 72000;

    static {
        StoneBeams.init();
        AreaTargets.addAllyRule(MindStonePowers::isCommandedBy);
    }

    /** A stone in a hand, and where it is. */
    private record InHand(StoneKind kind, StoneUse.Source source) {
    }

    /**
     * The stone a use of this stack would be: a bare stone, or the selected stone of a crafted Gauntlet (not the
     * Titan's, not empty, and not the Snap: Sneak with all six). Null for anything else.
     */
    @Nullable
    private static InHand stoneOf(ItemStack stack, boolean sneaking) {
        if (stack.getItem() instanceof InfinityStoneItem stone) {
            return new InHand(stone.kind, StoneUse.Source.BARE);
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

    /** Whether this use of the stone is held (a beam, a drain): the item is then used until it is let go of. */
    public static boolean held(StoneKind kind, boolean alt) {
        return !alt && (kind == StoneKind.MIND || kind == StoneKind.POWER || kind == StoneKind.SOUL);
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        InHand stone = stoneOf(event.getItemStack(), player.isShiftKeyDown());
        if (stone == null) {
            return;
        }
        boolean alt = player.isShiftKeyDown();
        boolean held = held(stone.kind(), alt);
        event.setCanceled(true);
        event.setCancellationResult(held ? InteractionResult.CONSUME : InteractionResult.SUCCESS);
        if (player instanceof ServerPlayer server) {
            useInHand(server, event.getHand(), stone.kind(), stone.source(), alt);
        }
    }

    /** A stone in {@code hand} is used (the server's half of a right-click; the tests call it too). */
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

    /** A stone or a crafted Gauntlet used as a hold lasts until it is let go of (the item has no length of its own). */
    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getDuration() > 0) {
            return;
        }
        if (stoneOf(event.getItem(), false) != null) {
            event.setDuration(HOLD_DURATION);
        }
    }

    /** The Time Stone on a crop or a sapling grows it (with Sneak, takes a crop back). */
    @SubscribeEvent
    public static void onUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        InHand stone = stoneOf(event.getItemStack(), player.isShiftKeyDown());
        if (stone == null || stone.kind() != StoneKind.TIME
                || !TimeStonePowers.isCropTarget(event.getLevel().getBlockState(event.getPos()), player.isShiftKeyDown())) {
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
    @SubscribeEvent
    public static void onUseOnEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        InHand stone = stoneOf(event.getItemStack(), player.isShiftKeyDown());
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

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive()) {
            StoneUse.tick(player);
        }
    }

    /** A projectile about to hit a holder of the Reality Stone is a bubble instead. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof ServerPlayer player
                && RealityBubbles.catches(player, event.getProjectile())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulStonePowers.hurt(player, event.getNewDamage());
        }
    }

    /** A creature under the Mind Command never takes its commander, nor his other allies, as its target. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!MindStonePowers.mayTarget(event.getEntity(), event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoneUse.forget(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoneUse.forget(player);
            StoneCooldowns.forget(player);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StoneUse.end(player, true);
            if (TimeStonePowers.slowing(player)) {
                TimeStonePowers.stopSlow(player, com.kolodkingal.enhanced.power.Powers.get(player), false);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        StoneUse.clear();
    }

    private StoneEvents() {
    }
}
