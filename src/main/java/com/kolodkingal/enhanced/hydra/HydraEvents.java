package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.FMLCommonSetupEvent;
import com.kolodkingal.enhanced.platform.event.LevelTickEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.ProjectileImpactEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The game's side of the Hydra soldiers (3.6): each player's tick fills the outposts near him ({@link
 * HydraGarrison#tick}); each level's tick runs its alarms ({@link HydraAlarm#tick}); holding use on a radio console
 * switches an alarm off; a block broken or an arrow landing near a soldier is heard ({@link HydraSenses#heard}).
 */
@Subscriber
public final class HydraEvents {

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HydraGarrison.tick(player);
        }
    }

    @Subscribe
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HydraAlarm.tick(level);
        }
    }

    /** Holding use on a radio console of an outpost: the alarm goes off after 3 s. */
    @Subscribe
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.OBSERVER)) {
            return;
        }
        if (HydraAlarm.useConsole(player, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @Subscribe
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HydraSenses.heard(level, Vec3.atCenterOf(event.getPos()));
        }
    }

    @Subscribe
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level() instanceof ServerLevel level && !(projectile instanceof HydraBoltEntity)) {
            HydraSenses.heard(level, event.getRayTraceResult().getLocation());
        }
    }

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HydraAlarm.forget(event.getEntity().getUUID());
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        HydraAlarm.clear();
        HydraGarrison.clear();
    }

    /** Common setup: the ally rule every area attack asks (Hydra spare their side; heroes the soldiers they turned). */
    @Subscriber
    public static final class Setup {
        @Subscribe
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> AreaTargets.addAllyRule(HydraLoyalty::allies));
        }

        private Setup() {
        }
    }

    private HydraEvents() {
    }
}
