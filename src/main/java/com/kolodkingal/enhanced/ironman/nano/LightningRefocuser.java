package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sneak + B on the Mark 85: the <b>Lightning Refocuser</b> (3.8, lane L4, PLAN 6.2; Endgame, where Tony takes Thor's
 * lightning into the suit and throws it on). Chain lightning from his raised gauntlet: the creature at his crosshair
 * within 24 (or else the nearest he can see in front of him) takes {@code refocuserDamage} 10, then it jumps to up to
 * {@code refocuserJumps} 4 more, each the nearest not yet struck within {@code refocuserJumpRadius} 6 of the last: 8,
 * 6, 5, 4 (the plan's falloff). 25 energy, cooldown 240. <b>Charged</b>: lightning that struck him in the last {@code
 * refocuserChargedTicks} 200 (a vanilla bolt, Thor's, Love's: any {@code #minecraft:is_lightning} damage) makes it free
 * and {@code refocuserChargedFactor} × 1.5, and is used up by it ({@link IronManState#CHARGED} shows it on the HUD).
 */
public final class LightningRefocuser {
    /** The damages of the chain after the first, as shares of {@code refocuserDamage} (10 → 8, 6, 5, 4). */
    static final double[] FALLOFF = {1.0, 0.8, 0.6, 0.5, 0.4};
    /** How far it reaches for its first creature. */
    public static final double RANGE = 24.0;
    /** The bolt's pale blue (the flash where it meets a shield). */
    static final int BOLT_BLUE = 0xBFF4FF;

    /**
     * Fires the chain.
     *
     * @return the creatures struck, in the order of the chain (empty: nothing in reach, and then nothing is spent;
     *         or, 3.8.2, the bolt struck a Chaos Shield)
     */
    public static List<LivingEntity> fire(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.IRON_MAN_LIGHTNING_REFOCUSER)
                || !data.isReady(HeroAbility.IRON_MAN_LIGHTNING_REFOCUSER, now)) {
            return List.of();
        }
        // (3.8.2) Aimed at a Chaos Shield (or another wall), the bolt strikes it: the chain ends there.
        NanoTech.Ray aim = NanoTech.ray(player, RANGE);
        LivingEntity first = aim.blocked() ? null : firstTarget(player, aim);
        if (first == null && !aim.blocked()) {
            return List.of();
        }
        NanoState state = NanoState.of(player.getUUID());
        boolean charged = state.charged(now);
        if (!charged && !SuitEnergy.spend(player, NanoTech.cfg(IronManConfig.MARK85_REFOCUSER_ENERGY, 25.0))) {
            return List.of();
        }
        data.startCooldown(HeroAbility.IRON_MAN_LIGHTNING_REFOCUSER, now,
                NanoTech.cfg(IronManConfig.MARK85_REFOCUSER_COOLDOWN, 240));
        double factor = charged ? NanoTech.cfg(IronManConfig.MARK85_REFOCUSER_CHARGED_FACTOR, 1.5) : 1.0;
        if (charged) {
            discharge(player);
        }
        double base = NanoTech.cfg(IronManConfig.MARK85_REFOCUSER_DAMAGE, 10.0);
        int jumps = NanoTech.cfg(IronManConfig.MARK85_REFOCUSER_JUMPS, 4);
        double radius = NanoTech.cfg(IronManConfig.MARK85_REFOCUSER_JUMP_RADIUS, 6.0);
        List<LivingEntity> chain = new ArrayList<>();
        if (first != null) {
            chain.add(first);
            LivingEntity last = first;
            for (int i = 0; i < jumps; i++) {
                LivingEntity next = nextTarget(player, last, chain, radius);
                if (next == null) {
                    break;
                }
                chain.add(next);
                last = next;
            }
        }
        ServerLevel level = player.serverLevel();
        List<Vec3> points = new ArrayList<>();
        points.add(NanoTech.palm(player, false).add(0.0, 0.3 * player.getScale(), 0.0));
        if (aim.blocked()) {
            NanoTech.stopped(player, aim.wall(), (float) (base * factor), BOLT_BLUE);
            points.add(aim.wall().point());
        }
        List<LivingEntity> struck = new ArrayList<>();
        for (int i = 0; i < chain.size(); i++) {
            LivingEntity e = chain.get(i);
            float amount = (float) (base * FALLOFF[Math.min(i, FALLOFF.length - 1)] * factor);
            Vec3 at = e.position().add(0.0, e.getBbHeight() * 0.6, 0.0);
            // (3.8.2) A link that runs into a Chaos Shield (or another wall) ends at it, and the chain with it.
            WandaAbilities.ShieldStop wall = NanoTech.wallAcross(player, points.get(points.size() - 1), at);
            if (wall != null) {
                NanoTech.stopped(player, wall, amount, BOLT_BLUE);
                points.add(wall.point());
                break;
            }
            NanoTech.hurt(player, e, ModDamageTypes.REFOCUSER_LIGHTNING, amount);
            struck.add(e);
            points.add(at);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 8, 0.25, 0.3, 0.25, 0.3);
        }
        NanoTech.send(level, player, IronFxPayload.REFOCUSER, points, charged ? 1 : 0);
        NanoTech.sound(level, player.position(), ModSounds.LIGHTNING_REFOCUSER.get(), 1.2F, charged ? 0.8F : 1.0F);
        return struck;
    }

    /** The creature at his crosshair within reach, or else the nearest he can see in front of him (60° either side). */
    @Nullable
    static LivingEntity firstTarget(ServerPlayer player, NanoTech.Ray ray) {
        if (ray.target() instanceof LivingEntity living && NanoTech.mayHurt(player, living)) {
            return living;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), e -> NanoTech.mayHurt(player, e))) {
            Vec3 to = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0).subtract(eye);
            double dist = to.length();
            if (dist > RANGE || dist >= bestDist || to.normalize().dot(look) < 0.5
                    || !Raycasts.inSight(player, e.position().add(0.0, e.getBbHeight() * 0.5, 0.0))) {
                continue;
            }
            best = e;
            bestDist = dist;
        }
        return best;
    }

    /** The nearest creature he may hurt within {@code radius} of the last one, not yet in the chain. */
    @Nullable
    static LivingEntity nextTarget(ServerPlayer player, LivingEntity last, List<LivingEntity> chain, double radius) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = last.getBoundingBox().inflate(radius);
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                e -> !chain.contains(e) && NanoTech.mayHurt(player, e))) {
            double dist = e.distanceTo(last);
            if (dist <= radius && dist < bestDist) {
                best = e;
                bestDist = dist;
            }
        }
        return best;
    }

    // ---- the charge ----

    /**
     * Lightning struck him: a Mark 85 (or a custom hero with the Refocuser) is charged for {@code
     * refocuserChargedTicks}.
     */
    public static void struck(ServerPlayer player) {
        PowerData data = Powers.get(player);
        if (!data.has(HeroAbility.IRON_MAN_LIGHTNING_REFOCUSER)
                || data.power() != com.kolodkingal.enhanced.power.Power.CUSTOM
                && IronSuits.worn(player) != SuitKind.MARK_85) {
            return;
        }
        long now = player.level().getGameTime();
        NanoState.of(player.getUUID()).chargedUntil = now
                + NanoTech.cfg(IronManConfig.MARK85_REFOCUSER_CHARGED_TICKS, 200);
        IronManState state = IronSuits.state(player);
        if (!state.has(IronManState.CHARGED)) {
            IronSuits.setState(player, state.with(IronManState.CHARGED, true));
        }
    }

    /** The charge is used up, or ran out. */
    static void discharge(Player player) {
        NanoState state = NanoState.peek(player.getUUID());
        if (state != null) {
            state.chargedUntil = 0L;
        }
        IronManState iron = IronSuits.state(player);
        if (iron.has(IronManState.CHARGED)) {
            IronSuits.setState(player, iron.with(IronManState.CHARGED, false));
        }
    }

    /** Every tick: the charge runs out after its time. */
    static void tick(ServerPlayer player, long now) {
        NanoState state = NanoState.peek(player.getUUID());
        if (IronSuits.state(player).has(IronManState.CHARGED) && (state == null || !state.charged(now))) {
            discharge(player);
        }
    }

    private LightningRefocuser() {
    }
}
