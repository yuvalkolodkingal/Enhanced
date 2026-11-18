package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.config.ChargedBeamConfig;
import com.kolodkingal.enhanced.config.SolarJewelConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.IntSupplier;

/**
 * (3.17) A charged beam: hold the key to charge (the glow builds, a note rises), release to fire one beam whose damage, reach
 * and width grow with the charge; a tap fires the weakest; then a cooldown. Vision's B (where his Density was) and Jim
 * Hammond's R are this one mechanism, each with its own {@link Profile} (numbers, damage type, what it does where it lands:
 * {@link Impact}).
 *
 * <p>The server owns the charge: when the key went down ({@code CHARGING}); the state {@link Live#BEAM_CHARGING} tells the
 * clients to draw the build-up (and the HUD to show it), {@link Live#BEAM_FIRING} that a beam was just fired (a few ticks).
 */
public final class ChargedBeam {
    /** How long the fired beam stays on the clients' screens. */
    public static final int FIRING_TICKS = 8;
    /** A key release that never came (a screen opened while it was held) fires the beam this long after a full charge. */
    private static final int OVER_HOLD = 100;

    /** What the beam does where it strikes, beyond hurting what it crosses. */
    @FunctionalInterface
    public interface Impact {
        /** {@code hit}: the end of the beam (a block, or a miss at its reach); {@code charge}: 0 (a tap) to 1 (full). */
        void strike(ServerPlayer player, ServerLevel level, HitResult hit, float charge);
    }

    /** What a beam costs: asked before it starts charging and again as it fires (Jim's solar charge); false refuses (it says why). */
    public interface Gate {
        boolean start(ServerPlayer player, PowerData data);

        boolean fire(ServerPlayer player, PowerData data, float charge);
    }

    /** The numbers and the extras of one hero's charged beam. */
    public record Profile(IntSupplier chargeTicks, IntSupplier tapTicks, IntSupplier cooldown, DoubleSupplier minDamage,
                          DoubleSupplier maxDamage, IntSupplier minRange, IntSupplier maxRange, DoubleSupplier maxWidth,
                          ResourceKey<DamageType> damageType, DoubleToIntFunction burnSeconds, Impact impact, Gate gate, Live charging, Live firing,
                          IntSupplier firingTicks) {
        int firingFor() {
            return firingTicks == null ? FIRING_TICKS : firingTicks.getAsInt();
        }

        float charge(long heldTicks) {
            if (heldTicks < tapTicks.getAsInt()) {
                return 0.0F;
            }
            return Mth.clamp(heldTicks / (float) Math.max(1, chargeTicks.getAsInt()), 0.0F, 1.0F);
        }

        double range(float charge) {
            return Mth.lerp(charge, minRange.getAsInt(), Math.max(minRange.getAsInt(), maxRange.getAsInt()));
        }

        float damage(float charge) {
            return Mth.lerp(charge, (float) minDamage.getAsDouble(), (float) Math.max(minDamage.getAsDouble(), maxDamage.getAsDouble()));
        }

        double width(float charge) {
            return Mth.lerp(charge, 0.15F, (float) Math.max(0.15, maxWidth.getAsDouble()));
        }
    }

    /** Vision's profile: plain, and (his Source Code look) burning and exploding where it strikes. */
    private static final Profile VISION = new Profile(ChargedBeamConfig.CHARGE_TICKS::get, ChargedBeamConfig.TAP_TICKS::get,
            ChargedBeamConfig.COOLDOWN::get, ChargedBeamConfig.MIN_DAMAGE::get, ChargedBeamConfig.MAX_DAMAGE::get,
            ChargedBeamConfig.MIN_RANGE::get, ChargedBeamConfig.MAX_RANGE::get, ChargedBeamConfig.MAX_WIDTH::get,
            ModDamageTypes.MIND_STONE_BEAM, c -> 0, null, null, Live.BEAM_CHARGING, Live.BEAM_FIRING, null);
    private static final Profile SOURCE_CODE = new Profile(ChargedBeamConfig.CHARGE_TICKS::get, ChargedBeamConfig.TAP_TICKS::get,
            ChargedBeamConfig.COOLDOWN::get, ChargedBeamConfig.MIN_DAMAGE::get, ChargedBeamConfig.MAX_DAMAGE::get,
            ChargedBeamConfig.MIN_RANGE::get, ChargedBeamConfig.MAX_RANGE::get, ChargedBeamConfig.MAX_WIDTH::get,
            ModDamageTypes.SOLAR_BEAM,
            c -> Math.round(SolarJewelConfig.SOURCE_CODE_BURN_SECONDS.get() * (1.0F + (float) c)),
            ChargedBeam::sourceCodeImpact, null, Live.BEAM_CHARGING, Live.BEAM_FIRING, null);

    public static Profile visionProfile(PowerData data) {
        return VisionAbilities.burning(data) ? SOURCE_CODE : VISION;
    }

    private static final Map<UUID, Long> CHARGING = new HashMap<>();
    private static final Map<UUID, Long> FIRED_AT = new HashMap<>();

    private ChargedBeam() {
    }

    /** A player left, died or lost the power: no charge or shot of his is remembered. */
    public static void forget(UUID player) {
        CHARGING.remove(player);
        FIRED_AT.remove(player);
    }

    public static boolean charging(ServerPlayer player) {
        return CHARGING.containsKey(player.getUUID());
    }

    /** How charged the beam is now, 0 to 1 (0 when it is not charging). */
    public static float chargeNow(ServerPlayer player, Profile profile) {
        Long start = CHARGING.get(player.getUUID());
        return start == null ? 0.0F : profile.charge(player.level().getGameTime() - start);
    }

    // ---- the key ----

    public static void onKey(ServerPlayer player, PowerData data, boolean down, Profile profile, HeroAbility ability) {
        long now = player.level().getGameTime();
        if (down) {
            if (charging(player) || player.isSpectator()) {
                return;
            }
            if (data.visionPhased()) {
                player.displayClientMessage(Component.translatable("message.enhanced.charged.phased"), true);
                return;
            }
            long until = data.cooldownUntil(data.slot(ability));
            if (until > now && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.enhanced.charged.cooldown", (until - now + 19) / 20), true);
                return;
            }
            if (profile.gate() != null && !profile.gate().start(player, data)) {
                return;
            }
            data.setBeamActive(false);
            CHARGING.put(player.getUUID(), now);
            data.set(profile.charging(), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 0.8F, 0.6F);
        } else if (charging(player)) {
            release(player, data, profile, ability);
        }
    }

    private static void release(ServerPlayer player, PowerData data, Profile profile, HeroAbility ability) {
        long now = player.level().getGameTime();
        long held = now - CHARGING.remove(player.getUUID());
        data.set(profile.charging(), false);
        fire(player, data, profile, ability, profile.charge(held));
    }

    // ---- every tick ----

    public static void tick(ServerPlayer player, PowerData data, Profile profile, HeroAbility ability) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long start = CHARGING.get(player.getUUID());
        if (start != null) {
            long held = now - start;
            if (!data.is(profile.charging()) || !player.isAlive() || player.isSpectator() || data.visionPhased()) {
                CHARGING.remove(player.getUUID());
                data.set(profile.charging(), false);
            } else if (held > profile.chargeTicks.getAsInt() + OVER_HOLD) {
                release(player, data, profile, ability);
            } else {
                chargingTick(player, level, profile, held);
            }
        } else if (data.is(profile.charging())) {
            data.set(profile.charging(), false); // (a state left by another hero's, or a stale one)
        }
        Long fired = FIRED_AT.get(player.getUUID());
        if (fired != null && now - fired >= profile.firingFor()) {
            FIRED_AT.remove(player.getUUID());
            data.set(profile.firing(), false);
        } else if (fired == null && data.is(profile.firing())) {
            data.set(profile.firing(), false);
        }
    }

    private static void chargingTick(ServerPlayer player, ServerLevel level, Profile profile, long held) {
        float charge = profile.charge(held);
        int full = profile.chargeTicks.getAsInt();
        if (held % 6 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                    0.7F, 0.7F + 1.3F * Math.min(1.0F, held / (float) full));
        }
        if (held == full) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 1.0F, 1.8F);
        }
    }

    // ---- the shot ----

    private static void fire(ServerPlayer player, PowerData data, Profile profile, HeroAbility ability, float charge) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (profile.gate() != null && !profile.gate().fire(player, data, charge)) {
            return; // (the sun's charge ran out while it gathered: it does not fire)
        }
        Vec3 eye = player.getEyePosition();
        double range = profile.range(charge);
        BlockHitResult block = Raycasts.block(player, range, 1.0F);
        Vec3 end = block.getLocation();
        // a wall in the way (a Chaos Shield, Billy's barrier from the front, a Hex that keeps him out) ends the beam there
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, eye, end, 1.0F);
        float damage = profile.damage(charge);
        if (shield != null) {
            WandaAbilities.pay(shield, player, damage);
            end = shield.point();
        } else {
            double width = profile.width(charge);
            int burn = profile.burnSeconds().applyAsInt(charge);
            AABB box = new AABB(eye, end).inflate(width + 1.0);
            for (Entity e : level.getEntities(player, box, WandaAbilities.BEAM_TARGET)) {
                double reach = width + e.getBbWidth() / 2.0;
                if (segmentDistance(e.getBoundingBox().getCenter(), eye, end) > reach
                        && e.getBoundingBox().inflate(width).clip(eye, end).isEmpty()) {
                    continue;
                }
                if (e instanceof LivingEntity living && !AreaTargets.mayHurt(player, living, true)) {
                    continue;
                }
                e.hurt(ModDamageTypes.source(level, profile.damageType(), player, player), damage);
                BeamFire.ignite(e, burn);
            }
            if (profile.impact() != null) {
                profile.impact().strike(player, level, block, charge);
            }
        }
        data.set(profile.firing(), true);
        FIRED_AT.put(player.getUUID(), now);
        data.startCooldown(ability, now, profile.cooldown().getAsInt());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                0.8F + 0.6F * charge, 1.6F - 0.8F * charge);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                0.3F + 0.5F * charge, 1.9F - 0.6F * charge);
        level.sendParticles(new EnergyParticleOptions(0xFFF3C2, 0.8F + 0.8F * charge), end.x, end.y, end.z, 3 + (int) (5 * charge),
                0.2, 0.2, 0.2, 0.15);
    }

    private static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len = ab.lengthSqr();
        double t = len < 1.0E-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    // ---- Source Code Vision: fire and a blast where it strikes ----

    private static void sourceCodeImpact(ServerPlayer player, ServerLevel level, HitResult hit, float charge) {
        if (SolarJewelConfig.BEAM_FIRE.get()) {
            BeamFire.strike(player, level, hit, 0, 3);
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return; // (it struck nothing: no blast in the air)
        }
        Vec3 at = hit.getLocation();
        float radius = Mth.lerp(charge, (float) ChargedBeamConfig.EXPLOSION_MIN_RADIUS.get().doubleValue(),
                (float) Math.max(ChargedBeamConfig.EXPLOSION_MIN_RADIUS.get(), ChargedBeamConfig.EXPLOSION_MAX_RADIUS.get()));
        explode(player, level, at, radius, charge, (float) ChargedBeamConfig.EXPLOSION_MAX_DAMAGE.get().doubleValue(),
                ChargedBeamConfig.EXPLOSION_BREAKS_BLOCKS.get());
    }

    /**
     * An explosion of the shooter's beam at {@code at}: creatures within {@code radius} he may hurt take damage that falls
     * with the distance (never the shooter), and blocks of the sphere are broken only if {@code breakBlocks}, the game rule
     * {@code mobGriefing} is on and {@link BlockRights#mayBlast} allows (a boss arena, Asgard, a Hex, spawn protection and
     * claims refuse). Nothing unbreakable or harder than 8 (obsidian is 50) is touched.
     */
    public static void explode(ServerPlayer player, ServerLevel level, Vec3 at, float radius, float charge, float maxDamage,
                               boolean breakBlocks) {
        if (radius <= 0.0F) {
            return;
        }
        // (no vanilla explosion puffs: they are grey squares that fill the view near the camera; the ember burst is the blast's look)
        Embers.burst(level, at.x, at.y, at.z, 10 + (int) (30 * charge), radius * 0.4, radius * 0.4, radius * 0.4, 0.04);
        Embers.burst(level, at.x, at.y, at.z, 12 + (int) (20 * charge), radius * 0.3, radius * 0.3, radius * 0.3, 0.08);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.6F + 0.6F * charge,
                1.1F - 0.3F * charge);
        float damage = Mth.lerp(charge, 0.2F, 1.0F) * maxDamage;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e != player && e.isAlive() && e.getBoundingBox().distanceToSqr(at) <= radius * radius)) {
            if (!AreaTargets.mayHurt(player, e, true)) {
                continue;
            }
            double falloff = 1.0 - Math.sqrt(e.getBoundingBox().distanceToSqr(at)) / radius * 0.6;
            e.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player), (float) (damage * falloff));
            Vec3 push = e.position().subtract(at).multiply(1, 0, 1);
            if (push.lengthSqr() > 1.0E-4) {
                push = push.normalize().scale(0.4 + 0.5 * charge);
                e.push(push.x, 0.25, push.z);
                e.hurtMarked = true;
            }
        }
        if (!breakBlocks || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        int r = Mth.ceil(radius);
        BlockPos centre = BlockPos.containing(at);
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
            if (Vec3.atCenterOf(pos).distanceToSqr(at) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || state.getDestroySpeed(level, pos) > 8.0F
                    || !state.getFluidState().isEmpty()) {
                continue;
            }
            BlockPos fixed = pos.immutable();
            if (BlockRights.mayBlast(player, level, fixed)) {
                level.destroyBlock(fixed, false, player);
            }
        }
    }
}
