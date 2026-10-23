package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ironman.MissileGuidance;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitMoves;
import com.kolodkingal.enhanced.ironman.SuitPhases;
import com.kolodkingal.enhanced.ironman.hulkbuster.Hulkbuster;
import com.kolodkingal.enhanced.ironman.hulkbuster.HulkbusterMoves;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The nanotech suits' event hooks (3.8, lane L4). Priorities as {@code docs/HEROES-AND-BOSSES.md} lists them:
 * <ul>
 * <li>the mod's setup: the Mark 50's, Mark 85's and Hulkbuster's {@link SuitMoves} are registered (lane L3's
 * {@code IronManAbilities} hands them his keys and ticks while the suit works);</li>
 * <li>{@link PlayerTickEvent.Post}: the housings rebuild while stowed ({@link NanoHousings});</li>
 * <li>{@link LivingIncomingDamageEvent}, HIGH: a nanotech shield takes its share of a blow from the front ({@link
 * EnergyShield}); lightning that strikes a Mark 85 charges the Refocuser; LOW: a blow of the empty hand with the
 * blades out is the blade's ({@link EnergyBlades#handDamage}) and a full one sweeps;</li>
 * <li>{@link LivingDamageEvent.Post}: the deployed suit's housing takes the armour's wear;</li>
 * <li>{@link ProjectileImpactEvent}: a shield up bounces what flies at him from the front;</li>
 * <li>{@link EntityStruckByLightningEvent}: a bolt charges the Refocuser.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class NanoEvents {
    /** The strength of each player's last blow (read before the game resets it). */
    private static final Map<UUID, Float> STRENGTH = new HashMap<>();

    /** The moves of the suits of lane L4, registered with lane L3's framework when the mod sets up. */
    @EventBusSubscriber(modid = Enhanced.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void onSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(NanoEvents::registerMoves);
        }

        private Setup() {
        }
    }

    private static boolean registered;

    /**
     * Registers the Mark 50's, the Mark 85's and the Hulkbuster's moves, and the Hulkbuster's listener on the phases
     * (once).
     */
    public static synchronized void registerMoves() {
        if (registered) {
            return;
        }
        registered = true;
        SuitMoves.register(SuitKind.MARK_50, Mark50Moves.INSTANCE);
        SuitMoves.register(SuitKind.MARK_85, Mark85Moves.INSTANCE);
        SuitMoves.register(SuitKind.HULKBUSTER, HulkbusterMoves.INSTANCE);
        SuitPhases.listen(SuitKind.HULKBUSTER, Hulkbuster.INSTANCE);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NanoHousings.rebuild(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 100 == 0) {
            MissileGuidance.sweep(level);
        }
    }

    /** A shield's share of a blow from the front; lightning charges the Refocuser. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_LIGHTNING)) {
            LightningRefocuser.struck(player);
        }
        float absorbed = EnergyShield.absorbed(player, source, event.getAmount());
        if (absorbed <= 0.0F) {
            return;
        }
        float left = event.getAmount() - absorbed;
        if (left <= 1.0E-3F) {
            event.setCanceled(true);
        } else {
            event.setAmount(left);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STRENGTH.put(player.getUUID(), player.getAttackStrengthScale(0.5F));
        }
    }

    /** A blow of his empty main hand with the blades out: the blade's damage, and a full one sweeps. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBladeBlow(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !source.is(DamageTypes.PLAYER_ATTACK) || event.getEntity() == player) {
            return;
        }
        float strength = STRENGTH.getOrDefault(player.getUUID(), 1.0F);
        float damage = EnergyBlades.handDamage(player, strength);
        if (damage < 0.0F) {
            return;
        }
        event.setAmount(damage);
        if (strength >= 0.9F) {
            EnergyBlades.sweep(player, event.getEntity(), damage);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            NanoHousings.onHurt(player, event.getSource(), event.getOriginalDamage());
        }
    }

    @SubscribeEvent
    public static void onProjectile(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof ServerPlayer player
                && EnergyShield.bounces(player, event.getProjectile())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LightningRefocuser.struck(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer player && !event.isCanceled()) {
            gone(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            gone(player);
            STRENGTH.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NanoDrones.recall(player);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        clear();
    }

    /** He died or left: his drones go, his moves are forgotten. */
    static void gone(ServerPlayer player) {
        NanoDrones.recall(player);
        NanoState.forget(player.getUUID());
        NanoHousings.forget(player.getUUID());
    }

    public static void clear() {
        NanoState.clear();
        NanoHousings.clear();
        MissileGuidance.clear();
        STRENGTH.clear();
    }

    private NanoEvents() {
    }
}
