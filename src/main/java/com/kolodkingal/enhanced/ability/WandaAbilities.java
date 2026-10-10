package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

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
                boolean start = down && !player.isSpectator() && canAffordShield(player);
                if (down && !start && !player.isSpectator()) {
                    player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.hungry"), true);
                }
                if (start && !data.chaosShieldActive()) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.6F);
                }
                data.setChaosShieldActive(start);
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

    /** Whether the Chaos Shield can be held: it costs hunger, so a hungry Scarlet Witch is refused (the client's animation asks too). */
    public static boolean canAffordShield(Player player) {
        return player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0;
    }

    public static boolean hasShieldUp(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.SCARLET_WITCH && data.chaosShieldActive();
    }

    private static void shieldTick(ServerPlayer player, PowerData data) {
        if (player.isSpectator() || !canAffordShield(player)) {
            data.setChaosShieldActive(false);
            player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.hungry"), true);
            return;
        }
        player.causeFoodExhaustion(EnhancedConfig.CHAOS_SHIELD_EXHAUSTION.get().floatValue());

        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(SHIELD_RADIUS + 4),
                p -> p.getOwner() != player)) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 toPlayer = center.subtract(projectile.position());
            double reach = SHIELD_RADIUS + 0.5 + velocity.length();
            if (velocity.dot(toPlayer) > 0 && toPlayer.lengthSqr() < reach * reach) {
                disintegrate(player.serverLevel(), projectile);
            }
        }
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
