package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The boss framework's own game-bus events: the fights tick with their level, end when the server stops, credit the
 * damage players do to them, sweep up their orphans and hand over the gifts that wait. (The events that protect an
 * arena are in {@link BossArenaEvents}.) The priorities are those of {@code docs/3.0-fragments/README.md}.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class BossEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BossFights.tickLevel(level);
        }
    }

    /** First of all: a fight puts back what it changed (an island is lowered) before anything else is saved. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerStopping(ServerStoppingEvent event) {
        BossFights.stopAll(event.getServer(), StopReason.SERVER_STOPPING);
    }

    /**
     * An entity that only a boss fight makes, and that no running fight owns, never joins a level: what a crash, a
     * copied chunk or a saved Sentry leaves behind.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && BossFights.isOrphan(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** The damage a player does to a fight's boss and minions goes in their ledger. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        BossFight fight = BossFights.fightOf(event.getEntity());
        if (fight == null) {
            return;
        }
        DamageSource source = event.getSource();
        if (fight.creditsDamageEvents() && source.getEntity() instanceof ServerPlayer player) {
            fight.credit(player, BossFight.DAMAGE, event.getNewDamage());
        }
    }

    /** Whoever kills the boss struck the last blow. (A boss that never dies reports its last blow itself.) */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        BossFight fight = BossFights.fightOf(dead);
        if (fight != null && dead.getType().is(ModTags.BOSSES)
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            fight.lastBlow(player);
        }
    }

    /** What a fighter drops when they die in an arena that keeps drops stays until the fight is over. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (BossFight fight : BossFights.all(player.serverLevel().getServer())) {
            if (fight.arena().keepsDrops() && fight.isParticipant(player.getUUID()) && fight.arena().contains(player.position())) {
                for (ItemEntity drop : event.getDrops()) {
                    fight.keepDrop(drop);
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BossGifts.deliver(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BossGifts.deliver(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BossGifts.deliver(player);
        }
    }

    private BossEvents() {
    }
}
