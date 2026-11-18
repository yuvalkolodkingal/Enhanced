package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jim Hammond's Fire Beam (R, 3.17): a continuous beam. While R is held it fires a steady beam that follows where he looks: no
 * charge-up. Every 4 ticks it strikes and sets alight what it touches along its line (never him), and every 3 ticks it
 * places fire where it meets the ground (fire only: placed on the air, never with {@code mobGriefing} off, through {@link BlockRights};
 * it never makes magma: that is the Nova Flame's). It drains his solar charge every tick ({@code beamCostPerSecond}) and stops
 * when R is let go or the charge runs out; then a short cooldown. {@link Live#BEAM_FIRING} is on while it burns (every client draws it).
 */
public final class JimBeam {
    private static final Map<UUID, Long> HELD = new HashMap<>();

    private JimBeam() {
    }

    public static boolean held(ServerPlayer player) {
        return HELD.containsKey(player.getUUID());
    }

    static void onKey(ServerPlayer player, PowerData data, boolean down) {
        if (!down) {
            if (held(player)) {
                stop(player, data);
            }
            return;
        }
        if (held(player) || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        long until = data.cooldownUntil(data.slot(HeroAbility.JIM_BEAM));
        if (until > now && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.enhanced.charged.cooldown", (until - now + 19) / 20), true);
            return;
        }
        if (!JimSolar.has(player, data, JimConfig.BEAM_MIN_CHARGE.get())) {
            return;
        }
        data.setBeamActive(false);
        HELD.put(player.getUUID(), now);
        data.set(Live.BEAM_FIRING, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    static void tick(ServerPlayer player, PowerData data) {
        Long start = HELD.get(player.getUUID());
        if (start == null) {
            if (data.is(Live.BEAM_FIRING)) {
                data.set(Live.BEAM_FIRING, false);
            }
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!player.isAlive() || player.isSpectator() || !data.is(Live.BEAM_FIRING)) {
            stop(player, data);
            return;
        }
        long age = level.getGameTime() - start;
        if (!drain(player, data, age)) {
            stop(player, data);
            return;
        }
        Vec3 eye = player.getEyePosition();
        BlockHitResult block = Raycasts.block(player, JimConfig.BEAM_RANGE.get(), 1.0F);
        Vec3 end = block.getLocation();
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, eye, end, 1.0F);
        if (shield != null) {
            if (age % 4 == 0) {
                WandaAbilities.pay(shield, player, JimConfig.BEAM_DAMAGE.get().floatValue());
            }
            return;
        }
        if (age % 4 == 0) {
            double width = JimConfig.BEAM_WIDTH.get();
            for (Entity e : level.getEntities(player, new AABB(eye, end).inflate(width + 1.0), WandaAbilities.BEAM_TARGET)) {
                double reach = width + e.getBbWidth() / 2.0;
                if (distanceToSegment(e.getBoundingBox().getCenter(), eye, end) > reach
                        && e.getBoundingBox().inflate(width).clip(eye, end).isEmpty()) {
                    continue;
                }
                if (e instanceof LivingEntity living && !AreaTargets.mayHurt(player, living, true)) {
                    continue;
                }
                e.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player), JimConfig.BEAM_DAMAGE.get().floatValue());
                BeamFire.ignite(e, JimConfig.BEAM_BURN_SECONDS.get());
            }
        }
        if (block.getType() == HitResult.Type.BLOCK && age % 3 == 0) {
            if (JimConfig.BEAM_FIRE.get()) {
                BeamFire.strike(player, level, block, 0, 1);
            }
            Embers.burst(level, end.x, end.y, end.z, 3, 0.25, 0.2, 0.25, 0.12);
        }
        if (age % 8 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1.2F, 0.8F);
        }
    }

    /** One tick's share of the cost per second (spent exactly: the whole second adds up to the setting); false when he cannot pay it. */
    private static boolean drain(ServerPlayer player, PowerData data, long age) {
        if (!JimSolar.active(player, data)) {
            return true;
        }
        int perSecond = JimConfig.BEAM_COST_PER_SECOND.get();
        int cost = (int) (perSecond * (age + 1) / 20 - perSecond * age / 20);
        return JimSolar.spend(player, data, cost);
    }

    private static void stop(ServerPlayer player, PowerData data) {
        if (HELD.remove(player.getUUID()) != null) {
            data.startCooldown(HeroAbility.JIM_BEAM, player.level().getGameTime(), JimConfig.BEAM_COOLDOWN.get());
        }
        data.set(Live.BEAM_FIRING, false);
    }

    private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        double t = len < 1.0E-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    static void forget(UUID player) {
        HELD.remove(player);
    }
}
