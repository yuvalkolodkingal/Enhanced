package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The nanotech shields (3.8, lane L4), held on B: the Mark 50's round <b>Nano Shield</b> on the left arm (PLAN 6.1:
 * from the front, damage −{@code shieldReduction} 80 %, 3 energy a second) and the Mark 85's <b>Giant Energy
 * Shield</b>, the big curved hexagonal one of the Endgame refs (PLAN 6.2: <b>100 % of everything from the front {@code
 * shieldAngle} 120°</b>, beams and explosions too, 4 energy a second). Both bounce projectiles back the way he looks,
 * cost 1 energy per 2 health they stop, and drop at their longest time or when the energy runs out. While one is up
 * {@link Live#IRON_SHIELD} is set (clients draw it from the bit and the worn suit). What comes from behind or from no
 * place (a fall, poison, fire) is never stopped.
 */
public final class EnergyShield {
    /** The Mark 50's round shield covers the same front as the Mark 85's (the plan names no angle for it). */
    public static final double NANO_SHIELD_ANGLE = 120.0;

    /** Whether his shield is up. */
    public static boolean up(Player player) {
        return Powers.get(player).is(Live.IRON_SHIELD);
    }

    /** B went down: the shield forms (refused without energy). */
    public static boolean raise(ServerPlayer player, PowerData data, SuitKind kind) {
        if (data.is(Live.IRON_SHIELD) || !SuitEnergy.drain(player, perSecond(kind))) {
            return false;
        }
        NanoState.of(player.getUUID()).shieldSince = player.level().getGameTime();
        data.set(Live.IRON_SHIELD, true);
        NanoTech.sound(player.serverLevel(), player.position(), ModSounds.NANO_SHIELD.get(), 1.0F,
                kind == SuitKind.MARK_85 ? 0.85F : 1.1F);
        return true;
    }

    /** B came up (or the suit stopped): the shield goes. */
    public static void lower(ServerPlayer player, PowerData data) {
        if (!data.is(Live.IRON_SHIELD)) {
            return;
        }
        data.set(Live.IRON_SHIELD, false);
        NanoState state = NanoState.peek(player.getUUID());
        if (state != null) {
            state.shieldSince = 0L;
        }
        NanoTech.sound(player.serverLevel(), player.position(), ModSounds.NANO_RETRACT.get(), 0.5F, 1.4F);
    }

    /** Every tick while it is up: its upkeep and its longest time. */
    static void tick(ServerPlayer player, PowerData data, SuitKind kind, long now) {
        if (!data.is(Live.IRON_SHIELD)) {
            return;
        }
        NanoState state = NanoState.of(player.getUUID());
        if (state.shieldSince == 0L) {
            state.shieldSince = now;
        }
        if (now - state.shieldSince >= maxTicks(kind) || !SuitEnergy.drain(player, perSecond(kind))) {
            lower(player, data);
        }
    }

    static double perSecond(SuitKind kind) {
        return kind == SuitKind.MARK_50 ? NanoTech.cfg(IronManConfig.MARK50_SHIELD_ENERGY_PER_SECOND, 3.0)
                : NanoTech.cfg(IronManConfig.MARK85_SHIELD_ENERGY_PER_SECOND, 4.0);
    }

    static int maxTicks(SuitKind kind) {
        return kind == SuitKind.MARK_50 ? NanoTech.cfg(IronManConfig.MARK50_SHIELD_MAX_TICKS, 160)
                : NanoTech.cfg(IronManConfig.MARK85_SHIELD_MAX_TICKS, 200);
    }

    /** The share of a blow from the front it stops: the Mark 85's all of it, the Mark 50's {@code shieldReduction}. */
    public static double reduction(SuitKind kind) {
        return kind == SuitKind.MARK_50 ? NanoTech.cfg(IronManConfig.MARK50_SHIELD_REDUCTION, 0.8) : 1.0;
    }

    /** How wide the front it covers is (degrees, all of it). */
    public static double angle(SuitKind kind) {
        return kind == SuitKind.MARK_50 ? NANO_SHIELD_ANGLE : NanoTech.cfg(IronManConfig.MARK85_SHIELD_ANGLE, 120.0);
    }

    /** The suit whose shield is up on him, or null (a custom hero's is the Mark 85's). */
    @Nullable
    public static SuitKind shieldOf(Player player) {
        PowerData data = Powers.get(player);
        if (!data.is(Live.IRON_SHIELD)) {
            return null;
        }
        SuitKind worn = IronSuits.worn(player);
        return worn == SuitKind.MARK_50 ? SuitKind.MARK_50 : SuitKind.MARK_85;
    }

    /** Whether a point lies in front of him, within the shield's angle (only the horizontal direction counts). */
    public static boolean inFront(Player player, Vec3 from, double angle) {
        Vec3 to = from.subtract(player.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return false;
        }
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.getYHeadRot());
        return flat.normalize().dot(facing) >= Math.cos(Math.toRadians(angle * 0.5));
    }

    /**
     * A blow is about to reach him: what his shield takes off it (0 when none is up, or it comes from behind or from no
     * place). The energy it costs is paid; when it runs out, the shield drops after this blow.
     */
    public static float absorbed(ServerPlayer player, DamageSource source, float amount) {
        SuitKind kind = shieldOf(player);
        if (kind == null || amount <= 0.0F) {
            return 0.0F;
        }
        Vec3 from = source.getSourcePosition();
        if (from == null || !inFront(player, from, angle(kind))) {
            return 0.0F;
        }
        float blocked = (float) (amount * reduction(kind));
        double cost = blocked / 2.0;
        float energy = SuitEnergy.get(player);
        if (!player.isCreative()) {
            SuitEnergy.set(player, Math.max(0.0, energy - cost));
        }
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0)
                .add(from.subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize().scale(0.9));
        NanoTech.send(level, player, IronFxPayload.SHIELD_HIT, List.of(at), kind.ordinal());
        NanoTech.sound(level, at, ModSounds.ENERGY_SHIELD_HIT.get(), 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        if (!player.isCreative() && energy - cost <= 0.0) {
            lower(player, Powers.get(player));
        }
        return blocked;
    }

    /** A projectile reaches him: from the front with a shield up it bounces back the way he looks. */
    public static boolean bounces(ServerPlayer player, Projectile projectile) {
        SuitKind kind = shieldOf(player);
        if (kind == null || projectile.getOwner() == player) {
            return false;
        }
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.getYHeadRot());
        boolean fromFront = motion.lengthSqr() < 1.0E-6 ? inFront(player, projectile.position(), angle(kind))
                : motion.normalize().dot(facing) < -Math.cos(Math.toRadians(angle(kind) * 0.5))
                && inFront(player, projectile.position(), 180.0);
        if (!fromFront) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        double speed = Math.max(1.0, motion.length() * 0.8);
        Vec3 from = player.getEyePosition().add(look.scale(1.0 + player.getBbWidth()))
                .subtract(0.0, projectile.getBbHeight() * 0.5, 0.0);
        projectile.setPos(from);
        projectile.setDeltaMovement(look.scale(speed));
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.accelerationPower = Math.max(hurting.accelerationPower, 0.1);
        }
        projectile.setOwner(player);
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        ServerLevel level = player.serverLevel();
        NanoTech.send(level, player, IronFxPayload.SHIELD_HIT, List.of(from), kind.ordinal());
        NanoTech.sound(level, from, ModSounds.ENERGY_SHIELD_HIT.get(), 1.0F, 1.3F);
        return true;
    }

    private EnergyShield() {
    }
}
