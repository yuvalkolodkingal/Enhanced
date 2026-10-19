package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Doctor Strange's events (3.6). Priorities, as {@code HEROES-AND-BOSSES.md} section 3 lists them:
 * <ul>
 * <li>{@code LivingIncomingDamageEvent}, HIGH: the Shield of the Seraphim (a projectile at its front stopped, a blow from
 * the front weakened) and the Seraphim Dome (half the harm to its sheltered); the Mirror Dimension holds whom he hurt
 * there and adds to his spells. Before the shared chain of {@code CommonEvents}.</li>
 * <li>{@code ProjectileImpactEvent}, HIGH: a projectile at the Shield's front turns to sparks before it hits.</li>
 * <li>{@code LivingFallEvent}: the Cloak's soft landing (his body), and the Whip Grapple's swing.</li>
 * <li>The Mirror Dimension's world: explosions break nothing in it ({@code ExplosionEvent.Detonate}), nobody but him
 * breaks or places a block, no creature griefs, fire does not spread; the Dome stops explosions' knockback.</li>
 * <li>The Astral Projection's spirit: it uses, opens, strikes and picks up nothing, and creatures do not see it.</li>
 * <li>{@code ServerTickEvent.Post}: the portals' pairs, the bands that hold a boss, the Mirror's fire.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class StrangeEvents {
    /** Fire that a Mirror Dimension put out this tick (cleared at the end of the server tick). */
    private static final List<Fire> FIRES = new ArrayList<>();

    private record Fire(Level level, BlockPos pos) {
    }

    // ---- the harm ----

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        float amount = SeraphimShield.harm(victim, event.getSource(), event.getAmount());
        amount = MirrorDimension.onHurt(victim, event.getSource(), amount);
        if (amount <= 1.0E-4F) {
            event.setCanceled(true);
            if (event.getSource().getDirectEntity() instanceof Projectile projectile
                    && victim.level() instanceof ServerLevel level) {
                SeraphimShield.stop(level, projectile);
            }
            return;
        }
        event.setAmount(amount);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ServerPlayer player) || projectile.getOwner() == player) {
            return;
        }
        if (SeraphimShield.isUp(player)) {
            Vec3 to = projectile.position().subtract(player.getEyePosition());
            if (to.lengthSqr() > 1.0E-4 && to.normalize().dot(player.getLookAngle()) > 0.15) {
                event.setCanceled(true);
                SeraphimShield.stop(player.serverLevel(), projectile);
            }
        }
    }

    /** The Cloak catches him: a fall shorter than {@code safeFall} does nothing, a longer one less; a swing nothing. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (EldritchWhip.fallForgiven(player.getUUID(), player.level().getGameTime())) {
            event.setCanceled(true);
            return;
        }
        PowerData data = Powers.get(player);
        if (data.has(HeroAbility.STRANGE_BODY)) {
            float safe = (float) (StrangeAbilities.safeFall() * data.bodyScale(HeroAbility.STRANGE_BODY));
            if (event.getDistance() <= safe) {
                event.setCanceled(true);
            } else {
                event.setDistance(event.getDistance() - safe);
            }
        }
    }

    // ---- the Mirror Dimension's world, the Dome ----

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (!level.isClientSide) {
            event.getAffectedBlocks().removeIf(pos -> MirrorDimension.shields(level, pos));
        }
    }

    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        if (event.getAffectedEntity() instanceof LivingEntity living && !living.level().isClientSide
                && SeraphimShield.shelters(living)) {
            event.setKnockbackVelocity(Vec3.ZERO);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getPlayer() instanceof ServerPlayer player
                && MirrorDimension.protects(player, level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && MirrorDimension.protects(event.getEntity() instanceof ServerPlayer p ? p : null, level,
                event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGrief(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && !event.getEntity().level().isClientSide
                && MirrorDimension.shields(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    /** Fire that spreads into a Mirror Dimension is put out at the end of the tick. */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getState().is(BlockTags.FIRE)
                && MirrorDimension.shields(level, event.getPos())) {
            FIRES.add(new Fire(level, event.getPos().immutable()));
        }
    }

    // ---- the Astral Projection's spirit ----

    private static boolean astral(Player player) {
        return player != null && AstralProjection.isAstral(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        if (astral(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (astral(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (astral(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (astral(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHitBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (astral(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (astral(event.getPlayer())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && astral(player)) {
            event.setCanceled(true);
        }
    }

    // ---- the server's tick ----

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SlingPortals.serverTick(event.getServer());
        CrimsonBands.serverTick();
        if (!FIRES.isEmpty()) {
            for (Fire fire : FIRES) {
                if (fire.level().getBlockState(fire.pos()).is(BlockTags.FIRE)) {
                    fire.level().setBlock(fire.pos(), Blocks.AIR.defaultBlockState(), 3);
                }
            }
            FIRES.clear();
        }
    }

    private StrangeEvents() {
    }
}
