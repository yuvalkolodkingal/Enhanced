package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ApocalypseAbilities;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Apocalypse's own game-bus events, at the priorities of {@code docs/HEROES-AND-BOSSES.md}: the sand hazards tick with
 * their level, a projectile that reaches the boss's shield (or the hero's) is turned away before it hits, the hero's shield
 * takes the blows meant for him before Wanda's branch does, the Horsemen drop nothing and are swept up if no fight owns
 * them, a player who comes into view of the boss is told where the arena is, a fighter who falls earns a word from him, and a
 * creature in a cocoon of sand can neither strike nor use anything. (The events that protect the arena and hand over the
 * rewards are the boss framework's.)
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class ApocalypseEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SandHazards.tickLevel(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SandHazards.clear();
    }

    /**
     * HIGHEST: the hero's shield takes the blows meant for him, before Wanda's branch of the damage chain (see {@link
     * ApocalypseGuard#guard}): everything but mind power is stopped, mind power wears it, and what it cannot take goes on.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide) {
            ApocalypseGuard.guard(player, event);
        }
    }

    /**
     * HIGHEST: he and his Horsemen are one side. War's iron shards fly at the fighters past Pestilence, Famine's gust and
     * Death's slam spare War, and none of them strikes another, so none of them turns on another (in a real fight War's
     * volley cut Pestilence down and he and Famine fought it out within half a minute of their coming). A Horseman a
     * Phoenix has taken over is hers: it fights them, and they may fight it back.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFriendlyFire(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.level().isClientSide && sameSide(victim, event.getSource().getEntity())) {
            event.setCanceled(true);
        }
    }

    /** No Horseman takes another of the fight, or him, for a target (unless a Phoenix holds one of the two). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && sameSide(entity, event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    /** Whether two entities are his and on his side: the boss or a Horseman each, of the same fight, neither held by a Phoenix. */
    static boolean sameSide(Entity a, @Nullable Entity b) {
        return b != null && a != b && ofHisSide(a) && ofHisSide(b) && BossFights.sameFight(a, b);
    }

    private static boolean ofHisSide(Entity entity) {
        return (entity instanceof ApocalypseEntity || Horsemen.kindOf(entity) != null) && !PhoenixAbilities.isHost(entity);
    }

    /** The healing factor of the hero waits for quiet: any damage that reached him starts the wait over. */
    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && Powers.is(player, Power.APOCALYPSE)) {
            ApocalypseAbilities.hurt(player);
        }
    }

    /**
     * HIGH: a projectile about to hit the boss while his shield is up, forming or re-forming is turned away first (so an
     * arrow does not stick in it and bounce): Mjolnir, Stormbreaker and the pieces of a shattered Mjolnir go back to Thor,
     * everything else turns to gold dust. What his own side shoots (a lance, a Horseman's shard) is left alone. The same for
     * the hero, whose complete shield turns away what is shot at him (and charges it a part of the damage it would have done).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide || !(event.getRayTraceResult() instanceof EntityHitResult hit)) {
            return;
        }
        if (hit.getEntity() instanceof ServerPlayer hero) {
            if (projectile.getOwner() != hero && ApocalypseGuard.isUp(hero) && !ApocalypseGuard.headingHome(projectile)) {
                event.setCanceled(true);
                ApocalypseGuard.neutralise(hero, projectile);
            }
            return;
        }
        if (!(hit.getEntity() instanceof ApocalypseEntity boss)) {
            return;
        }
        if (projectile.getOwner() == boss || BossFights.sameFight(boss, projectile) || boss.shield().isVulnerable()) {
            return;
        }
        event.setCanceled(true);
        boss.neutralise(projectile);
    }

    /** A Horseman that no fight owns (a crash left it in a chunk) never joins a level. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide && entity instanceof Husk && Horsemen.kindOf(entity) != null && BossFights.fightOf(entity) == null) {
            event.setCanceled(true);
        }
    }

    /** The Horsemen drop nothing and give no experience: they are never farmed. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDrops(LivingDropsEvent event) {
        if (Horsemen.kindOf(event.getEntity()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onExperience(LivingExperienceDropEvent event) {
        if (Horsemen.kindOf(event.getEntity()) != null || event.getEntity() instanceof ApocalypseEntity) {
            event.setDroppedExperience(0);
        }
    }

    /** A player comes into view of the boss: the clients that were not there at the ritual are told where the arena is. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ApocalypseEntity boss && event.getEntity() instanceof ServerPlayer player && boss.fight() != null) {
            boss.fight().tellArena(player);
        }
    }

    /** The first fighter to fall earns a word. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (BossFight fight : BossFights.all(player.serverLevel().getServer())) {
            if (fight instanceof ApocalypseFight apocalypse && apocalypse.isParticipant(player.getUUID())) {
                apocalypse.participantFell();
            }
        }
    }

    // ---- a creature in a cocoon of sand: the hands are covered ----

    private static boolean entombed(LivingEntity entity) {
        return entity.hasEffect(ModEffects.ENTOMBED);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        if (entombed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (entombed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (entombed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (entombed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (entombed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (entombed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private ApocalypseEvents() {
    }
}
