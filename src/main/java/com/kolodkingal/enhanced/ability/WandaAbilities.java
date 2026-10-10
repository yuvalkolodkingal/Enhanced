package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.entity.LokiDaggerEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.Locale;
import java.util.function.Predicate;

/** Wanda Maximoff, the Scarlet Witch. */
public final class WandaAbilities {
    public static final double SHIELD_RADIUS = 1.9;

    private static final ResourceLocation CROWN_HEALTH = Enhanced.id("wanda_crown_health");
    private static final ResourceLocation CROWN_ARMOR = Enhanced.id("wanda_crown_armor");
    private static final ResourceLocation CROWN_TOUGHNESS = Enhanced.id("wanda_crown_toughness");

    /** What the Chaos Beam can hit: creatures, Ender Dragon parts and End Crystals. */
    public static final Predicate<Entity> BEAM_TARGET = e -> e.isPickable() && !e.isSpectator()
            && (e instanceof LivingEntity || e instanceof PartEntity<?> || e instanceof EndCrystal);

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        switch (ability) {
            case 1 -> {
                boolean start = down && !player.isSpectator();
                if (start && !data.beamActive()) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CHAOS_BEAM_START.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                data.setBeamActive(start);
            }
            case 2 -> {
                if (down) {
                    PowerTicker.toggleFlight(player);
                }
            }
            case 3 -> {
                if (down) {
                    Telekinesis.press(player, Telekinesis.Mode.WANDA);
                }
            }
            case 4 -> {
                if (down) {
                    raiseShield(player, data);
                } else if (data.chaosShieldActive()) {
                    dropShield(player, data, false);
                }
            }
            case 5 -> {
                if (down) {
                    HexManager.onHexKey(player);
                }
            }
            default -> {
            }
        }
    }

    static void tick(ServerPlayer player, PowerData data) {
        if (data.beamActive()) {
            beamTick(player);
        }
        if (data.chaosShieldActive()) {
            shieldTick(player, data);
        }
    }

    // ---- Wanda's Crown ----

    public static boolean wearsCrown(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WANDA_CROWN.get());
    }

    /** While the Scarlet Witch wears her crown: 40 max health and the protection of a full Diamond set. */
    static void applyCrown(ServerPlayer player, PowerData data) {
        boolean transformed = data.power() == Power.SCARLET_WITCH && wearsCrown(player);
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, CROWN_HEALTH, 20.0, transformed);
        PowerTicker.setModifier(player, Attributes.ARMOR, CROWN_ARMOR, 20.0, transformed);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, CROWN_TOUGHNESS, 8.0, transformed);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ---- Ability 1: Chaos Energy Beam ----

    private static void beamTick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        HitResult hit = Raycasts.any(player, EnhancedConfig.CHAOS_BEAM_RANGE.get(), BEAM_TARGET, 1.0F);
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            target.hurt(ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, player, player),
                    EnhancedConfig.CHAOS_BEAM_DAMAGE.get().floatValue());
            LivingEntity living = target instanceof LivingEntity l ? l
                    : target instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity l ? l : null;
            int nausea = EnhancedConfig.CHAOS_BEAM_NAUSEA_TICKS.get();
            if (living != null && living.isAlive() && nausea > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nausea, 0), player);
            }
        }
        Vec3 end = hit.getLocation();
        if (player.tickCount % 2 == 0) {
            level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.4F), end.x, end.y, end.z, 4, 0.12, 0.12, 0.12, 0.12);
        }
        if (hit.getType() == HitResult.Type.BLOCK && player.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    // ---- Ability 4: Chaos Shield ----

    /** What a projectile costs the shield when nothing better is known (a snowball, a potion, a shulker bullet). */
    private static final float DEFAULT_PROJECTILE_POWER = 2.0F;
    /** How many red shards fly out when the shield shatters. */
    private static final int SHATTER_SHARDS = 56;

    /** Whether the Chaos Shield can be held: it costs hunger, so a hungry Scarlet Witch is refused (the client's animation asks too). */
    public static boolean canAffordShield(Player player) {
        return player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0;
    }

    /** The shield is held and complete: only then does it stop anything (it takes time to form, and wears out). */
    public static boolean hasShieldUp(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.SCARLET_WITCH && data.chaosShieldUp(player.level().getGameTime());
    }

    /** The strength a fresh Chaos Shield has, in points (this is also the most a client can show). */
    public static float maxShieldStrength() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.CHAOS_SHIELD_STRENGTH.get().floatValue() : 30.0F;
    }

    /** How long a complete Chaos Shield can be held, in ticks. */
    public static int maxShieldTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.CHAOS_SHIELD_MAX_TICKS.get() : 160;
    }

    /** B pressed: the shield starts to form, if she is hungry enough and it has recharged. */
    private static void raiseShield(ServerPlayer player, PowerData data) {
        if (player.isSpectator() || data.chaosShieldActive()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(4, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.recharging",
                    String.format(Locale.ROOT, "%.1f", (data.cooldownUntil(4) - now) / 20.0)), true);
            return;
        }
        if (!canAffordShield(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.hungry"), true);
            return;
        }
        data.raiseChaosShield(now, EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get(), maxShieldStrength());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    /**
     * The shield comes down (B released, hunger, or the time limit) and has to recharge: the short cooldown, or the
     * long one if it was {@code worn} out (held to its limit; a shattered shield is handled by {@link #shatter}).
     */
    private static void dropShield(ServerPlayer player, PowerData data, boolean worn) {
        data.setChaosShieldActive(false);
        data.startCooldown(4, player.level().getGameTime(),
                (worn ? EnhancedConfig.CHAOS_SHIELD_BROKEN_COOLDOWN : EnhancedConfig.CHAOS_SHIELD_COOLDOWN).get());
        if (worn) {
            ServerLevel level = player.serverLevel();
            level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.2F), player.getX(), player.getY() + player.getBbHeight() * 0.5,
                    player.getZ(), 24, SHIELD_RADIUS * 0.5, 0.7, SHIELD_RADIUS * 0.5, 0.05);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.7F);
    }

    /** The shield took all it could: a burst of red shards and a glassy sound, and a long wait before the next one. */
    private static void shatter(ServerPlayer player, PowerData data) {
        data.setChaosShieldActive(false);
        ServerLevel level = player.serverLevel();
        data.startCooldown(4, level.getGameTime(), EnhancedConfig.CHAOS_SHIELD_BROKEN_COOLDOWN.get());
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        RandomSource random = player.getRandom();
        // Chips of bright red. (A red stained glass block's own particles are see-through and a dull grey-violet in the
        // air, and every block's are darkened; the red dye's chips stay a clear red and fly the way they are sent.)
        ItemParticleOption shard = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.RED_DYE));
        for (int i = 0; i < SHATTER_SHARDS; i++) {
            // Out from a random spot on the sphere, along the way out.
            Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3 at = center.add(out.scale(SHIELD_RADIUS));
            level.sendParticles(shard, at.x, at.y, at.z, 0, out.x, out.y, out.z, 0.3);
            if (i % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(i % 4 == 0 ? Power.SCARLET_WITCH.glow : Power.SCARLET_WITCH.color, 1.5F),
                        at.x, at.y, at.z, 0, out.x, out.y, out.z, 0.3);
            }
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 1.2F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.3F);
        player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.shattered").withStyle(ChatFormatting.RED), true);
    }

    private static void shieldTick(ServerPlayer player, PowerData data) {
        if (player.isSpectator()) {
            data.setChaosShieldActive(false);
            return;
        }
        if (!canAffordShield(player)) {
            dropShield(player, data, false);
            player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.hungry"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now >= data.shieldFormedAt() + EnhancedConfig.CHAOS_SHIELD_MAX_TICKS.get()) {
            dropShield(player, data, true);
            return;
        }
        player.causeFoodExhaustion(EnhancedConfig.CHAOS_SHIELD_EXHAUSTION.get().floatValue());

        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        if (!data.chaosShieldUp(now)) {
            // Still forming: motes gather onto the growing sphere. It stops nothing yet.
            float grown = data.chaosShieldProgress(now);
            RandomSource random = player.getRandom();
            for (int i = 0; i < 2; i++) {
                Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
                Vec3 at = center.add(out.scale(SHIELD_RADIUS * Math.max(0.3, grown)));
                level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.0F), at.x, at.y, at.z, 0, -out.x, -out.y, -out.z, 0.1);
            }
            return;
        }
        if (now == data.shieldFormedAt()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.5F);
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(SHIELD_RADIUS + 4),
                p -> p.getOwner() != player && !(p instanceof ThrownWeaponEntity weapon && weapon.isReturning()))) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 toPlayer = center.subtract(projectile.position());
            double reach = SHIELD_RADIUS + 0.5 + velocity.length();
            if (velocity.dot(toPlayer) > 0 && toPlayer.lengthSqr() < reach * reach) {
                block(player, projectile);
                if (!data.chaosShieldActive()) {
                    return; // shattered: the rest get through
                }
            }
        }
    }

    /**
     * What a projectile would have done to her, in the strength it takes out of the shield: an arrow's damage at its
     * speed, a trident's 8, a fireball's 6, a thrown hammer's, dagger's or shard's own damage, and 2 for the rest.
     */
    static float projectilePower(Projectile projectile) {
        if (projectile instanceof ThrownTrident) {
            return 8.0F;
        }
        if (projectile instanceof AbstractArrow arrow) {
            return (float) Math.ceil(arrow.getDeltaMovement().length() * arrow.getBaseDamage());
        }
        if (projectile instanceof AbstractHurtingProjectile) {
            return 6.0F;
        }
        if (projectile instanceof ThrownWeaponEntity weapon) {
            return (ThorAbilities.isStormbreaker(weapon.weapon()) ? EnhancedConfig.STORMBREAKER_THROW_DAMAGE
                    : EnhancedConfig.MJOLNIR_THROW_DAMAGE).get().floatValue();
        }
        if (projectile instanceof LokiDaggerEntity) {
            return EnhancedConfig.LOKI_DAGGER_DAMAGE.get().floatValue();
        }
        if (projectile instanceof IronShrapnelEntity) {
            return EnhancedConfig.SHRAPNEL_DAMAGE.get().floatValue();
        }
        return DEFAULT_PROJECTILE_POWER;
    }

    /**
     * A projectile reaches the shield: it turns to sparks, and the shield pays what it would have cost her. (Mjolnir
     * and Stormbreaker are the exception: see {@link #repel}.)
     */
    public static void block(ServerPlayer player, Projectile projectile) {
        float power = projectilePower(projectile);
        if (projectile instanceof ThrownWeaponEntity weapon) {
            repel(player.serverLevel(), weapon);
        } else {
            disintegrate(player.serverLevel(), projectile);
        }
        wear(player, power);
    }

    /**
     * Mjolnir and Stormbreaker are one of a kind (a thrown one holds the only copy of the weapon): the shield turns it
     * round and it flies home to Thor, instead of being destroyed for good.
     */
    private static void repel(ServerLevel level, ThrownWeaponEntity weapon) {
        level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.color, 1.3F), weapon.getX(), weapon.getY(), weapon.getZ(),
                10, 0.15, 0.15, 0.15, 0.08);
        level.playSound(null, weapon.getX(), weapon.getY(), weapon.getZ(), ModSounds.MJOLNIR_IMPACT.get(), SoundSource.PLAYERS, 0.9F, 0.8F);
        weapon.setReturning(true);
    }

    /**
     * Wears the shield down by what it just stopped; when nothing is left it shatters.
     *
     * @return how much of {@code amount} the shield took (all of it, unless it had less strength left)
     */
    public static float wear(ServerPlayer player, float amount) {
        PowerData data = Powers.get(player);
        float taken = Math.min(amount, data.shieldStrength());
        data.setShieldStrength(data.shieldStrength() - taken);
        if (data.shieldStrength() <= 1.0E-4F) {
            data.setShieldStrength(0F);
            shatter(player, data);
        }
        return taken;
    }

    /** A projectile touching the Chaos Shield turns to red sparks. */
    public static void disintegrate(ServerLevel level, Projectile projectile) {
        level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.color, 1.3F), projectile.getX(), projectile.getY(), projectile.getZ(),
                10, 0.15, 0.15, 0.15, 0.08);
        level.sendParticles(ParticleTypes.SMOKE, projectile.getX(), projectile.getY(), projectile.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
        level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.8F);
        projectile.discard();
    }

    private WandaAbilities() {
    }
}
