package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.AgathaConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.AgathaFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Agatha's Witch's Shield (B, hold; the violet sphere she throws up round herself in the WandaVision finale). Like the
 * Chaos Shield it takes {@code shieldWindUpTicks} to form and stops nothing until it is complete, holds at most {@code
 * shieldMaxTicks}, has {@code shieldStrength} points, and recharges after a drop ({@code shieldCooldown}) or after it
 * shattered or was held to its limit ({@code shieldBrokenCooldown}). It reuses the synced fields of the Chaos Shield
 * ({@code chaosShieldActive}, {@code shieldFormedAt}, {@code shieldStrength}), so the client draws it as it draws every
 * hero's shield.
 *
 * <p>It stops what Apocalypse's shield stops ({@link ApocalypseGuard#stops}: everything but the world's own harm), and
 * every blow it stops takes the damage it would have done from its strength; when that is gone it shatters and the rest
 * of the blow goes through. Projectiles are turned to violet sparks before they reach her. The damage hook is {@link
 * #guard}, run by {@code AgathaEvents} at the highest priority (before Wanda's branch of {@code CommonEvents}).
 */
public final class AgathaShield {
    /** The cooldown slot of B. */
    public static final int SLOT = 4;
    /** How many violet shards fly out when it shatters. */
    private static final int SHATTER_SHARDS = 48;
    /** Two blows' tinks are at least this many ticks apart (a beam hits every few ticks). */
    private static final int TINK_EVERY = 4;
    private static final float EPSILON = 1.0E-4F;

    /** Agatha -> the game time of her shield's last tink. */
    private static final Map<UUID, Long> LAST_TINK = new HashMap<>();

    // ---- the numbers (clients ask too: their HUD and drawing) ----

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    public static float maxStrength() {
        return loaded() ? AgathaConfig.SHIELD_STRENGTH.get().floatValue() : 40.0F;
    }

    public static int windUpTicks() {
        return loaded() ? AgathaConfig.SHIELD_WIND_UP_TICKS.get() : 8;
    }

    public static int maxTicks() {
        return loaded() ? AgathaConfig.SHIELD_MAX_TICKS.get() : 200;
    }

    public static double radius() {
        return loaded() ? AgathaConfig.SHIELD_RADIUS.get() : 1.9;
    }

    /** Whether the shield can be held: it costs hunger, so an empty stomach is refused. */
    public static boolean canAfford(Player player) {
        return player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0;
    }

    /** The shield is held and complete: only then does it stop anything. */
    public static boolean isUp(Player player) {
        PowerData data = Powers.get(player);
        return data.has(HeroAbility.AGATHA_SHIELD) && data.chaosShieldUp(player.level().getGameTime());
    }

    // ---- the blows ----

    /**
     * The damage hook, for Agatha with her shield complete: the blow is taken off its strength and stopped, or, if it gives
     * way under it, the rest goes through.
     *
     * @return whether the shield took the blow
     */
    public static boolean guard(ServerPlayer player, LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!isUp(player) || !ApocalypseGuard.stops(source)) {
            return false;
        }
        PowerData data = Powers.get(player);
        float left = absorb(player, data, event.getAmount());
        if (left > EPSILON) {
            event.setAmount(left);
        } else {
            event.setCanceled(true);
        }
        tink(player, player.level().getGameTime());
        return true;
    }

    /**
     * Takes {@code cost} points from the shield's strength; if there is not so much, it shatters.
     *
     * @return the points it could not take (0 if it took them all)
     */
    static float absorb(ServerPlayer player, PowerData data, float cost) {
        float strength = data.shieldStrength();
        if (cost < strength - EPSILON) {
            data.setShieldStrength(strength - cost);
            return 0.0F;
        }
        shatter(player, data);
        return Math.max(0.0F, cost - strength);
    }

    /** A projectile reaches the shield: it turns to violet sparks before it hits, and costs the shield what it would have done. */
    public static void neutralise(ServerPlayer player, Projectile projectile) {
        ServerLevel level = player.serverLevel();
        float cost = WandaAbilities.projectilePower(projectile);
        WandaAbilities.neutralize(level, projectile, AgathaAbilities.VIOLET);
        PowerData data = Powers.get(player);
        if (cost > 0.0F) {
            absorb(player, data, cost);
        }
        tink(player, level.getGameTime());
    }

    private static void tink(ServerPlayer player, long now) {
        Long last = LAST_TINK.get(player.getUUID());
        if (last != null && now - last < TINK_EVERY && now >= last) {
            return;
        }
        LAST_TINK.put(player.getUUID(), now);
        player.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), ModSounds.AGATHA_SHIELD_HIT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    // ---- raising, holding, dropping ----

    /** B pressed: the shield starts to form, if it has recharged and she is fed. */
    static void raise(ServerPlayer player, PowerData data) {
        if (player.isSpectator() || data.chaosShieldActive()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.AGATHA_SHIELD, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.shield.recharging",
                    String.format(Locale.ROOT, "%.1f", (data.cooldownUntil(data.slot(HeroAbility.AGATHA_SHIELD)) - now) / 20.0)), true);
            return;
        }
        if (!canAfford(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.shield.hungry"), true);
            return;
        }
        data.raiseChaosShield(now, windUpTicks(), maxStrength());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AGATHA_SHIELD_UP.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    /** The shield comes down (B released, hunger, the time limit: {@code worn}) and has to recharge. */
    static void drop(ServerPlayer player, PowerData data, boolean worn) {
        data.setChaosShieldActive(false);
        data.startCooldown(HeroAbility.AGATHA_SHIELD, player.level().getGameTime(), (worn ? AgathaConfig.SHIELD_BROKEN_COOLDOWN : AgathaConfig.SHIELD_COOLDOWN).get());
        forget(player.getUUID());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.8F);
    }

    /** It took all it could: violet shards and a glassy crack, and the long wait before the next one. */
    static void shatter(ServerPlayer player, PowerData data) {
        data.setChaosShieldActive(false);
        data.setShieldStrength(0.0F);
        ServerLevel level = player.serverLevel();
        data.startCooldown(HeroAbility.AGATHA_SHIELD, level.getGameTime(), AgathaConfig.SHIELD_BROKEN_COOLDOWN.get());
        forget(player.getUUID());
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        RandomSource random = player.getRandom();
        for (int i = 0; i < SHATTER_SHARDS; i++) {
            Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3 at = center.add(out.scale(radius()));
            level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(player, HeroAbility.AGATHA_SHIELD,
                    i % 3 == 0 ? AgathaAbilities.LILAC : AgathaAbilities.VIOLET), 1.4F), at.x, at.y, at.z,
                    0, out.x, out.y, out.z, 0.3);
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.AGATHA_SHIELD_BREAK.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
        AgathaAbilities.send(level, new AgathaFxPayload(AgathaFxPayload.SHIELD_BREAK, player.getId(), center, 0,
                Math.round((float) radius() * 10.0F), 0));
        player.displayClientMessage(Component.translatable("message.enhanced.shield.shattered").withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    /** Every tick the shield is held (or forming): hunger, the time limit, the gathering motes, and the projectiles it turns away. */
    static void tick(ServerPlayer player, PowerData data) {
        if (player.isSpectator()) {
            data.setChaosShieldActive(false);
            return;
        }
        if (!canAfford(player)) {
            drop(player, data, false);
            player.displayClientMessage(Component.translatable("message.enhanced.shield.hungry"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now >= data.shieldFormedAt() + maxTicks()) {
            drop(player, data, true);
            return;
        }
        if (!player.isCreative()) {
            player.causeFoodExhaustion(AgathaConfig.SHIELD_EXHAUSTION.get().floatValue());
        }
        double radius = radius();
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        if (!data.chaosShieldUp(now)) {
            // Still forming: motes gather onto the growing sphere. It stops nothing yet.
            float grown = data.chaosShieldProgress(now);
            RandomSource random = player.getRandom();
            Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3 at = center.add(out.scale(radius * Math.max(0.3, grown)));
            level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(player, HeroAbility.AGATHA_SHIELD, AgathaAbilities.LILAC), 0.9F), at.x, at.y, at.z, 0, -out.x, -out.y, -out.z, 0.1);
            return;
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius + 4.0),
                p -> p.getOwner() != player && !ApocalypseGuard.headingHome(p))) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 toPlayer = center.subtract(projectile.position());
            double reach = radius + 0.5 + velocity.length();
            if (velocity.dot(toPlayer) > 0 && toPlayer.lengthSqr() < reach * reach) {
                neutralise(player, projectile);
                if (!data.chaosShieldActive()) {
                    return; // shattered: the rest get through
                }
            }
        }
    }

    static void forget(UUID agatha) {
        LAST_TINK.remove(agatha);
    }

    static void clear() {
        LAST_TINK.clear();
    }

    private AgathaShield() {
    }
}
