package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * (3.19) Ultimate Wanda's giant chaos beam, Sneak + R in the Scarlet Witch's menu: hold to charge (the red light gathers at her
 * hands, a note rises), release to fire one giant beam of chaos magic straight along her look, through everything: it
 * erases every block in its path (none unbreakable; never where breaking is not allowed: spawn protection, a boss arena,
 * claims, {@code mobGriefing} off) and hurts everything it crosses with all her strength. The longer the charge, the wider
 * and farther it goes ({@link #MIN_RANGE} to {@link #MAX_RANGE} blocks, {@link #MIN_RADIUS} to {@link #MAX_RADIUS} wide).
 * The clients draw it from {@link Live#BEAM_CHARGING} and {@link Live#BEAM_FIRING} on her (Vision's states: she is never he).
 */
public final class ChaosCannon {
    public static final int CHARGE_TICKS = 40;
    public static final int TAP_TICKS = 6;
    public static final double MIN_RANGE = 32.0;
    public static final double MAX_RANGE = 96.0;
    public static final float MIN_RADIUS = 1.0F;
    public static final float MAX_RADIUS = 3.5F;
    public static final float MIN_DAMAGE = 20.0F;
    public static final float MAX_DAMAGE = 100.0F;
    /** How long the fired beam stays on the clients' screens. */
    public static final int FIRING_TICKS = 14;
    /** A release that never came (a screen opened while it was held) fires it this long after a full charge. */
    private static final int OVER_HOLD = 100;

    private static final Map<UUID, Long> CHARGING = new HashMap<>();
    private static final Map<UUID, Long> FIRED_AT = new HashMap<>();

    private ChaosCannon() {
    }

    public static boolean charging(ServerPlayer player) {
        return CHARGING.containsKey(player.getUUID());
    }

    /** 0 (a tap) to 1 (full) for a charge held this long. */
    public static float charge(long held) {
        return held < TAP_TICKS ? 0.0F : Mth.clamp(held / (float) CHARGE_TICKS, 0.0F, 1.0F);
    }

    public static double range(float charge) {
        return Mth.lerp(charge, MIN_RANGE, MAX_RANGE);
    }

    public static float radius(float charge) {
        return Mth.lerp(charge, MIN_RADIUS, MAX_RADIUS);
    }

    public static void forget(UUID player) {
        CHARGING.remove(player);
        FIRED_AT.remove(player);
    }

    /** Sneak + R down: the charge begins; up: it fires. */
    public static void onKey(ServerPlayer player, PowerData data, boolean down) {
        long now = player.level().getGameTime();
        if (down) {
            if (charging(player) || player.isSpectator() || !player.isAlive()) {
                return;
            }
            data.setBeamActive(false);
            CHARGING.put(player.getUUID(), now);
            data.set(Live.BEAM_CHARGING, true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CHAOS_BEAM_START.get(),
                    SoundSource.PLAYERS, 1.0F, 0.6F);
        } else if (charging(player)) {
            fire(player, data, charge(now - CHARGING.remove(player.getUUID())));
        }
    }

    /** Every tick of hers: the charge's sound, a release that never came, the beam fading from the screens. */
    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!CHARGING.containsKey(id) && !FIRED_AT.containsKey(id)) {
            return;
        }
        PowerData data = Powers.get(player);
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long start = CHARGING.get(id);
        if (start != null) {
            long held = now - start;
            if (!UltimateWanda.is(data) || !player.isAlive() || player.isSpectator()) {
                CHARGING.remove(id);
                data.set(Live.BEAM_CHARGING, false);
            } else if (held > CHARGE_TICKS + OVER_HOLD) {
                CHARGING.remove(id);
                fire(player, data, 1.0F);
            } else {
                if (held % 5 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                            1.0F, 0.5F + 1.2F * Math.min(1.0F, held / (float) CHARGE_TICKS));
                }
                if (held == CHARGE_TICKS) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                            SoundSource.PLAYERS, 1.0F, 0.8F);
                }
                Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.8));
                float c = charge(held);
                level.sendParticles(new EnergyParticleOptions(0xFF2040, 0.6F + c), hand.x, hand.y - 0.3, hand.z, 2 + (int) (6 * c),
                        0.2 + c * 0.4, 0.2 + c * 0.4, 0.2 + c * 0.4, 0.02);
            }
        }
        Long fired = FIRED_AT.get(id);
        if (fired != null && now - fired >= FIRING_TICKS) {
            FIRED_AT.remove(id);
            data.set(Live.BEAM_FIRING, false);
        }
    }

    /** The shot: through everything along her look. */
    static void fire(ServerPlayer player, PowerData data, float charge) {
        data.set(Live.BEAM_CHARGING, false);
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = range(charge);
        float radius = radius(charge);
        Vec3 end = eye.add(look.scale(range));
        // a wall that stops chaos magic (a Chaos Shield, Billy's barrier, a Hex) still ends it there
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, eye, end, 1.0F);
        float damage = Mth.lerp(charge, MIN_DAMAGE, MAX_DAMAGE);
        if (shield != null) {
            WandaAbilities.pay(shield, player, damage);
            end = shield.point();
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(radius + 1.0),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            if (segmentDistance(e.getBoundingBox().getCenter(), eye, end) > radius + e.getBbWidth() / 2.0 + 0.5
                    || !AreaTargets.mayHurt(player, e, true)) {
                continue;
            }
            e.hurt(ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, player, player), damage);
        }
        carve(player, level, eye.add(look.scale(1.5)), end, radius);
        data.set(Live.BEAM_FIRING, true);
        FIRED_AT.put(player.getUUID(), level.getGameTime());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                1.5F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                1.0F + charge, 0.6F);
        for (int i = 0; i <= 12; i++) {
            Vec3 p = eye.lerp(end, i / 12.0);
            level.sendParticles(new EnergyParticleOptions(0xFF2040, 1.0F + charge), p.x, p.y, p.z, 4, radius * 0.5, radius * 0.5,
                    radius * 0.5, 0.05);
        }
    }

    /** Erases every block within {@code radius} of the segment (none unbreakable, none where breaking is not allowed). */
    static int carve(ServerPlayer player, ServerLevel level, Vec3 from, Vec3 to, float radius) {
        int r = Mth.ceil(radius);
        BlockPos min = BlockPos.containing(Math.min(from.x, to.x) - r, Math.min(from.y, to.y) - r, Math.min(from.z, to.z) - r);
        BlockPos max = BlockPos.containing(Math.max(from.x, to.x) + r, Math.max(from.y, to.y) + r, Math.max(from.z, to.z) + r);
        int erased = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (segmentDistance(Vec3.atCenterOf(pos), from, to) > radius || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !state.getFluidState().isEmpty()) {
                continue;
            }
            BlockPos fixed = pos.immutable();
            if (BlockRights.mayBlast(player, level, fixed)) {
                level.setBlock(fixed, Blocks.AIR.defaultBlockState(), 3);
                erased++;
            }
        }
        return erased;
    }

    private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        double t = len < 1.0E-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len));
        return p.distanceTo(a.add(ab.scale(t)));
    }
}
