package com.kolodkingal.enhanced.event;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.MindLink;
import com.kolodkingal.enhanced.config.XavierConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Professor X's game-bus events (3.3): a creature he steers is steered just before its own tick, and has no will of
 * its own to choose a target; a player he steers, and his own body while he is away in another, can neither strike,
 * use an item nor touch a block (their own clients stop those clicks too: this is the server's half); a blow to his own
 * body brings him back ({@code linkBreaksOnHurt}); a body that dies lets him go; a stopping server gives every body back
 * its will before the world is saved.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class XavierEvents {

    /** Just before a creature's tick: if he steers it, his wish is its will. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Mob mob) {
            MindLink.steerMob(mob);
        }
    }

    /** A creature he steers takes no target of its own. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && MindLink.isHost(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Neither a body he steers nor his own body, while he is away, acts by itself. */
    private static boolean entranced(Entity entity) {
        return entity instanceof ServerPlayer player && (MindLink.isHost(player) || MindLink.isLinked(player));
    }

    private static void refuse(Entity entity, ICancellableEvent event) {
        if (entranced(entity)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        refuse(event.getEntity(), event);
    }

    /** A blow to his own body, left behind, brings him back to it. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0.0F && event.getEntity() instanceof ServerPlayer player && MindLink.isLinked(player)
                && XavierConfig.LINK_BREAKS_ON_HURT.get()) {
            MindLink.end(player.server, player.getUUID(), MindLink.End.HURT);
        }
    }

    /** A creature he steers died: he comes back. (A player's death is the lifecycle's, {@code XavierServer.onDeath}.) */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.isCanceled() && !(event.getEntity() instanceof ServerPlayer)) {
            MindLink.onDeath(event.getEntity());
        }
    }

    /** The server stops: every link ends while the worlds are still there, so every creature is saved with its will. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MindLink.endAll(event.getServer());
    }

    private XavierEvents() {
    }
}
