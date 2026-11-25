package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * (3.19) Ultimate Wanda's own Eye of Agamotto, the open eye on her chest (made of her magic, not Doctor Strange's item), N in
 * her Ultimate menu. What the Eye is in the comics (not the films' Time Stone): an amulet of the Vishanti whose light reveals
 * every truth (it sees through illusion and invisibility), weakens and drives back demons and the dead, binds whom it shines
 * on, and through which its bearer sees far and into others.
 * <ul>
 *   <li>N, the Light of Agamotto: within {@link #LIGHT_RANGE} blocks the invisible are seen (and glow); before her, in its
 *   light, her foes are held fast and the undead and demons (the Nether's creatures, endermen, vexes) burn;</li>
 *   <li>Sneak + N, the All-Seeing Eye: every creature within {@link #SIGHT_RANGE} blocks glows for a while, and she is told
 *   who the players near are, how far, and how they fare.</li>
 * </ul>
 */
public final class UltimateEye {
    public static final double LIGHT_RANGE = 24.0;
    public static final double SIGHT_RANGE = 128.0;
    public static final int HOLD_TICKS = 100;
    public static final float BANISH_DAMAGE = 20.0F;
    public static final int LIGHT = 0xFFF2B0;

    private UltimateEye() {
    }

    /** Whether the creature is undead or demonic, what the Eye's light burns. */
    public static boolean evil(LivingEntity e) {
        return e.getType().is(EntityTypeTags.UNDEAD) || e instanceof Blaze || e instanceof Ghast || e instanceof MagmaCube
                || e instanceof AbstractPiglin || e instanceof EnderMan || e instanceof Vex;
    }

    /** N: the Light of Agamotto. Returns how many it bound or burned. */
    public static int light(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 chest = player.position().add(0, player.getBbHeight() * 0.7, 0);
        int touched = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(LIGHT_RANGE),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.distanceToSqr(player) <= LIGHT_RANGE * LIGHT_RANGE)) {
            // it reveals every truth: nothing is hidden from it, all round her
            if (e.hasEffect(MobEffects.INVISIBILITY)) {
                e.removeEffect(MobEffects.INVISIBILITY);
            }
            e.setInvisible(false);
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false), player);
            Vec3 to = e.getEyePosition().subtract(eye);
            if (to.normalize().dot(look) < 0.5 || !AreaTargets.mayHurt(player, e, false)) {
                continue;
            }
            // in its light, before her: her foes are held, the dead and the demons burn
            e.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, HOLD_TICKS, 0, false, false, true), player);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, HOLD_TICKS * 2, 1), player);
            if (evil(e)) {
                e.hurt(ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, player, player), BANISH_DAMAGE);
                e.igniteForSeconds(5);
            }
            touched++;
        }
        for (int i = 1; i <= 8; i++) {
            Vec3 p = eye.add(look.scale(i * 2.5));
            level.sendParticles(new EnergyParticleOptions(LIGHT, 0.8F + i * 0.15F), p.x, p.y, p.z, 6 + i * 2, i * 0.35, i * 0.35,
                    i * 0.35, 0.01);
        }
        level.sendParticles(new EnergyParticleOptions(LIGHT, 1.6F), chest.x, chest.y, chest.z, 30, 0.3, 0.3, 0.3, 0.12);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.6F);
        return touched;
    }

    /** Sneak + N: the All-Seeing Eye. Returns how many it saw. */
    public static int sight(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<LivingEntity> seen = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SIGHT_RANGE),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.distanceToSqr(player) <= SIGHT_RANGE * SIGHT_RANGE);
        List<Player> players = new ArrayList<>();
        for (LivingEntity e : seen) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false), player);
            if (e instanceof Player other) {
                players.add(other);
            }
        }
        players.sort(Comparator.comparingDouble(p -> p.distanceToSqr(player)));
        player.sendSystemMessage(Component.translatable("message.enhanced.ultimate_wanda.eye.sees", seen.size(), players.size())
                .withStyle(ChatFormatting.GOLD));
        for (Player other : players) {
            player.sendSystemMessage(Component.translatable("message.enhanced.ultimate_wanda.eye.player", other.getDisplayName(),
                    (int) Math.sqrt(other.distanceToSqr(player)), Math.round(other.getHealth()), Math.round(other.getMaxHealth()))
                    .withStyle(ChatFormatting.YELLOW));
        }
        Vec3 chest = player.position().add(0, player.getBbHeight() * 0.7, 0);
        level.sendParticles(new EnergyParticleOptions(LIGHT, 1.4F), chest.x, chest.y, chest.z, 40, 1.5, 0.8, 1.5, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1.0F, 0.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 0.7F);
        return seen.size();
    }
}
