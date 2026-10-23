package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Captain America's event hooks (3.8). Priorities as {@code docs/HEROES-AND-BOSSES.md} lists them:
 * <ul> <li>{@link LivingIncomingDamageEvent}, HIGH: no fall damage from his own leaps; the Vibranium Shield up (raised,
 * or before him in the charge) takes the whole of a blow from the front ({@link ShieldRules#blocksAll}: explosions and
 * beams too), so vanilla's blocking, and an axe's disabling, never come into it; a melee attacker in front is pushed
 * back;</li> <li>{@link LivingIncomingDamageEvent}, NORMAL: Resistance I, his body's cut of every blow that does not
 * bypass effects;</li> <li>{@link LivingShieldBlockEvent}: with {@code capBlocksEverything} off, his raised shield
 * blocks as vanilla's does but at once (no 5-tick raise);</li> <li>{@link ProjectileImpactEvent}, NORMAL: a projectile
 * that reaches his shield from the front bounces back toward its shooter ({@code reflectProjectiles});</li> <li>{@link
 * PlayerTickEvent.Post}: an axe never leaves his shield disabled ({@code axeDisables} off);</li> <li>{@link
 * ServerTickEvent.Post}: the bash's stuns run out.</li> </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class CaptainEvents {
    /** The ring and flash of a block at most this often (ticks) per player: lava does not ring every tick. */
    static final int BLOCK_FX_GAP = 4;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingGuard(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FALL) && CaptainAbilities.fallGuarded(player)) {
            event.setCanceled(true);
            return;
        }
        if (!ShieldRules.blocksAll(player, source)) {
            return;
        }
        event.setCanceled(true);
        blocked(player, source);
    }

    /** His shield took a blow: the ring, the flash, and a melee attacker in front pushed back. */
    static void blocked(ServerPlayer player, DamageSource source) {
        ServerLevel level = player.serverLevel();
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity attacker && attacker != player && !(direct instanceof Projectile)
                && !source.is(DamageTypeTags.IS_PROJECTILE)) {
            double push = CaptainAbilities.cfg(CaptainConfig.MELEE_PUSH, 0.6);
            if (push > 0.0) {
                CaptainAbilities.push(attacker, player.position(), push, 0.0, player);
            }
        }
        CaptainAbilities.State state = CaptainAbilities.state(player.getUUID());
        long now = level.getGameTime();
        if (now - state.lastBlockFx < BLOCK_FX_GAP) {
            return;
        }
        state.lastBlockFx = now;
        Vec3 from = source.getSourcePosition();
        Vec3 face = shieldFace(player, from);
        CaptainAbilities.sound(level, face, ModSounds.SHIELD_BLOCK.get(), 1.0F, 0.95F + level.random.nextFloat()
                * 0.1F);
        CaptainAbilities.send(level, player, CaptainFxPayload.BLOCK, List.of(face), 0);
    }

    /** Where the shield's face is: before his chest, toward the blow. */
    static Vec3 shieldFace(Player player, Vec3 toward) {
        Vec3 chest = player.position().add(0.0, player.getBbHeight() * 0.6, 0.0);
        Vec3 dir = toward == null ? player.getLookAngle() : toward.subtract(chest);
        return chest.add(CaptainAbilities.flat(dir).scale(0.6));
    }

    @SubscribeEvent
    public static void onIncomingResistance(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return;
        }
        float cut = CaptainAbilities.resistance(Powers.get(player));
        if (cut > 0.0F) {
            event.setAmount(event.getAmount() * (1.0F - cut));
        }
    }

    /** With {@code capBlocksEverything} off his raised Vibranium Shield is up at once (no 5-tick raise). */
    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked() || !(event.getEntity() instanceof Player player) || !ShieldRules.isCap(player)
                || !player.isUsingItem() || !ShieldRules.isVibranium(player.getUseItem())) {
            return;
        }
        DamageSource source = event.getDamageSource();
        Vec3 from = source.getSourcePosition();
        if (!source.is(DamageTypeTags.BYPASSES_SHIELD) && from != null && ShieldRules.inFront(player, from)) {
            event.setBlocked(true);
        }
    }

    /** A projectile reaching his raised shield from the front bounces back toward its shooter. */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level)
                || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ServerPlayer player) || projectile instanceof ThrownShieldEntity) {
            return;
        }
        if (projectile.getOwner() == player) {
            event.setCanceled(true); // (his own reflected shot on its way out)
            return;
        }
        if (!CaptainAbilities.cfg(CaptainConfig.REFLECT_PROJECTILES, true) || !ShieldRules.guarding(player)
                || !ShieldRules.inFront(player, projectile.position())) {
            return;
        }
        event.setCanceled(true);
        reflect(level, player, projectile);
    }

    /** Sends the projectile back toward whoever shot it (or straight back), a little slower, as his. */
    static void reflect(ServerLevel level, ServerPlayer player, Projectile projectile) {
        Vec3 v = projectile.getDeltaMovement();
        double speed = Math.max(0.3, v.length()) * CaptainAbilities.cfg(CaptainConfig.REFLECT_SPEED, 0.8);
        Entity shooter = projectile.getOwner();
        Vec3 dir = shooter != null && shooter.isAlive() && shooter.level() == level
                ? shooter.getEyePosition().subtract(projectile.position())
                : v.scale(-1.0);
        if (dir.lengthSqr() < 1.0E-6) {
            dir = player.getLookAngle();
        }
        Vec3 out = dir.normalize().scale(speed);
        projectile.setOwner(player);
        projectile.setDeltaMovement(out);
        projectile.setPos(projectile.position().add(out.normalize().scale(0.6)));
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        Vec3 face = shieldFace(player, projectile.position());
        CaptainAbilities.sound(level, face, ModSounds.SHIELD_REFLECT.get(), 1.0F, 1.0F);
        CaptainAbilities.send(level, player, CaptainFxPayload.REFLECT, List.of(face), 0);
    }

    /** An axe never leaves Captain America's Vibranium Shield disabled (unless {@code axeDisables} says so). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || CaptainAbilities.cfg(CaptainConfig.AXE_DISABLES,
                false) || !player.getCooldowns().isOnCooldown(ModItems.VIBRANIUM_SHIELD.get())
                || !ShieldRules.isCap(player)) {
            return;
        }
        player.getCooldowns().removeCooldown(ModItems.VIBRANIUM_SHIELD.get());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ShieldCharge.tickStuns(event.getServer());
    }

    private CaptainEvents() {
    }
}
