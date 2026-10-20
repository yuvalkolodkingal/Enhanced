package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * B: Assemble, and with Sneak Inspire (3.8, PLAN 3.4). He raises the shield and calls his allies within {@code
 * rallyRadius}: they (and he) get Strength I and Speed I for {@code rallyTicks}; Inspire heals them {@code inspireHeal}
 * and lifts Weakness, Slowness and Mining Fatigue. His allies are the players of his team (with no team, every player
 * while PvP is off: nobody is a foe then), tamed pets, iron golems and villagers; never a monster.
 */
public final class Assemble {
    /** Assemble: the rally. Returns how many allies (him included) were rallied, or -1 when it could not be used. */
    public static int assemble(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.CAPTAIN_ASSEMBLE) || !data.isReady(HeroAbility.CAPTAIN_ASSEMBLE, now)) {
            return -1;
        }
        data.startCooldown(HeroAbility.CAPTAIN_ASSEMBLE, now, CaptainAbilities.cfg(CaptainConfig.RALLY_COOLDOWN,
                1200));
        int ticks = CaptainAbilities.cfg(CaptainConfig.RALLY_TICKS, 400);
        List<Vec3> points = new ArrayList<>();
        int count = 0;
        for (LivingEntity ally : allies(player)) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0), player);
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0), player);
            if (points.size() < CaptainFxPayload.MAX_POINTS) {
                points.add(ally.position());
            }
            count++;
        }
        ServerLevel level = player.serverLevel();
        CaptainAbilities.sound(level, player.position(), ModSounds.CAP_ASSEMBLE.get(), 1.4F, 1.0F);
        CaptainAbilities.send(level, player, CaptainFxPayload.ASSEMBLE, points, count);
        return count;
    }

    /** Inspire: the heal. Returns how many were healed (him included), or -1 when it could not be used. */
    public static int inspire(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.CAPTAIN_INSPIRE) || !data.isReady(HeroAbility.CAPTAIN_INSPIRE, now)) {
            return -1;
        }
        data.startCooldown(HeroAbility.CAPTAIN_INSPIRE, now, CaptainAbilities.cfg(CaptainConfig.INSPIRE_COOLDOWN,
                600));
        float heal = (float) CaptainAbilities.cfg(CaptainConfig.INSPIRE_HEAL, 6.0);
        List<Vec3> points = new ArrayList<>();
        int count = 0;
        for (LivingEntity ally : allies(player)) {
            ally.heal(heal);
            ally.removeEffect(MobEffects.WEAKNESS);
            ally.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            ally.removeEffect(MobEffects.DIG_SLOWDOWN);
            if (points.size() < CaptainFxPayload.MAX_POINTS) {
                points.add(ally.position());
            }
            count++;
        }
        ServerLevel level = player.serverLevel();
        CaptainAbilities.sound(level, player.position(), ModSounds.CAP_INSPIRE.get(), 1.2F, 1.0F);
        CaptainAbilities.send(level, player, CaptainFxPayload.INSPIRE, points, count);
        return count;
    }

    /** Him and his allies within the rally's radius (him first). */
    public static List<LivingEntity> allies(ServerPlayer player) {
        double radius = CaptainAbilities.cfg(CaptainConfig.RALLY_RADIUS, 16.0);
        List<LivingEntity> allies = new ArrayList<>();
        allies.add(player);
        Vec3 at = player.position();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.distanceToSqr(player) <= radius * radius
                        && isAlly(player, e))) {
            allies.add(e);
        }
        return allies;
    }

    /** Whether the creature answers his call. */
    public static boolean isAlly(ServerPlayer player, LivingEntity e) {
        if (e instanceof Player other) {
            if (other.isSpectator()) {
                return false;
            }
            if (player.getTeam() != null || other.getTeam() != null) {
                return player.isAlliedTo(other);
            }
            return player.server == null || !player.server.isPvpAllowed();
        }
        if (e instanceof TamableAnimal pet) {
            return pet.isTame();
        }
        return e instanceof IronGolem || e instanceof AbstractVillager;
    }

    private Assemble() {
    }
}
