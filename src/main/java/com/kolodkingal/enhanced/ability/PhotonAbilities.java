package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.entity.PhotonBlastEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Photon: Monica Rambeau's energy powers, earned by surviving the Hex wall. */
public final class PhotonAbilities {
    public static final int BLAST_COOLDOWN = 12;
    public static final float SUPERCHARGE_COST = 10F;
    public static final float RELEASE_MIN_ENERGY = 10F;
    public static final int RELEASE_COOLDOWN = 100;
    public static final double RELEASE_RADIUS = 6.0;
    /** Energy stored per point of damage absorbed. */
    public static final float ENERGY_PER_DAMAGE = 2F;
    public static final int VISION_RANGE = 48;

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        if (!down || player.isSpectator()) {
            return;
        }
        if (data.isPhased() && (ability == 1 || ability == 3)) {
            // Intangible: no attacks of any kind until the phase ends.
            return;
        }
        switch (ability) {
            case 1 -> blast(player, data);
            case 2 -> PowerTicker.toggleFlight(player);
            case 3 -> release(player, data);
            case 4 -> phase(player, data);
            case 5 -> toggleVision(player, data);
            default -> {
            }
        }
    }

    static void tick(ServerPlayer player, PowerData data) {
        if (data.isPhased()) {
            data.setPhaseTicks(data.phaseTicks() - 1);
            if (player.tickCount % 3 == 0) {
                player.serverLevel().sendParticles(new EnergyParticleOptions(0xE8FBFF, 1.0F), player.getX(), player.getY() + 1.0, player.getZ(),
                        2, 0.3, 0.6, 0.3, 0.01);
            }
        }
        MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
        if (data.spectrumVision()) {
            if (nightVision == null || (nightVision.isAmbient() && nightVision.getDuration() < 220)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
            }
        } else if (nightVision != null && isVisionEffect(nightVision)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static boolean isVisionEffect(MobEffectInstance effect) {
        return effect.isAmbient() && !effect.isVisible() && !effect.showIcon() && effect.getDuration() <= 400;
    }

    // ---- Ability 1: Photon Blast ----

    private static void blast(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(1, now)) {
            return;
        }
        data.startCooldown(1, now, BLAST_COOLDOWN);
        float damage = EnhancedConfig.PHOTON_BLAST_DAMAGE.get().floatValue();
        boolean supercharged = data.energy() >= SUPERCHARGE_COST;
        if (supercharged) {
            data.setEnergy(data.energy() - SUPERCHARGE_COST);
            damage *= 2;
        }
        PhotonBlastEntity bolt = new PhotonBlastEntity(level, player, damage, supercharged);
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.getEyePosition().add(look.scale(0.8)).add(0, -0.2, 0);
        bolt.setPos(origin.x, origin.y, origin.z);
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 0.0F);
        level.addFreshEntity(bolt);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PHOTON_BLAST.get(), SoundSource.PLAYERS, 0.8F,
                supercharged ? 0.75F : 1.0F);
    }

    // ---- Ability 3: Energy Absorption ----

    /** @return the damage left after absorbing, and stores the absorbed part as energy */
    public static float absorb(Player player, float amount) {
        PowerData data = Powers.get(player);
        float absorbed = amount * EnhancedConfig.PHOTON_ABSORB_FRACTION.get().floatValue();
        data.setEnergy(data.energy() + absorbed * ENERGY_PER_DAMAGE);
        return amount - absorbed;
    }

    private static void release(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(3, now)) {
            return;
        }
        float energy = data.energy();
        if (energy < RELEASE_MIN_ENERGY) {
            player.displayClientMessage(Component.translatable("message.enhanced.energy.low"), true);
            return;
        }
        data.setEnergy(0);
        data.startCooldown(3, now, RELEASE_COOLDOWN);
        float damage = Math.min(25F, energy * 0.25F);
        Vec3 center = player.position().add(0, 1.0, 0);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RELEASE_RADIUS),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.distanceToSqr(center) < RELEASE_RADIUS * RELEASE_RADIUS)) {
            victim.hurt(ModDamageTypes.source(level, ModDamageTypes.ENERGY_BURST, player, player), damage);
            Vec3 away = victim.position().subtract(player.position());
            victim.knockback(1.5, -away.x, -away.z);
        }
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2 / 48;
            level.sendParticles(new EnergyParticleOptions(Power.PHOTON.color, 1.8F), center.x, center.y, center.z, 0,
                    Math.cos(angle), 0.05, Math.sin(angle), 0.6);
        }
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ENERGY_RELEASE.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
    }

    // ---- Ability 4: Phase Shift ----

    private static void phase(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(4, now) || data.isPhased()) {
            return;
        }
        data.setPhaseTicks(EnhancedConfig.PHASE_DURATION.get());
        data.startCooldown(4, now, EnhancedConfig.PHASE_COOLDOWN.get() + EnhancedConfig.PHASE_DURATION.get());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    public static boolean isPhased(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.PHOTON && data.isPhased();
    }

    // ---- Ability 5: Spectrum Vision ----

    private static void toggleVision(ServerPlayer player, PowerData data) {
        boolean on = !data.spectrumVision();
        data.setSpectrumVision(on);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.spectrum_vision.on" : "message.enhanced.spectrum_vision.off"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, on ? 1.6F : 0.8F);
    }

    private PhotonAbilities() {
    }
}
