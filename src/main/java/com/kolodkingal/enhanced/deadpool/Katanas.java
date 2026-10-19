package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * R: the katanas (3.7). A 3-hit combo of both swords on the first creature in a 70-degree cone ({@code katanaReach}
 * 3.5): 5, 5 and 8 ({@code deadpool_katana}, in {@code #enhanced:melee_blows}), the third an X-cut that sweeps up to
 * three; a cut every {@code katanaGap} ticks, the combo starting again after 30 ticks without one. A cut that reaches
 * nobody still swings (and steps the combo). Sneak + R, held: the Parry, both swords spinning before him ({@code
 * Live.DEADPOOL_PARRYING}): what flies at him from the front bounces back where he looks, a blow from the front loses
 * {@code parryMeleeReduction}; at most {@code parryMaxTicks}, then R's slot waits {@code parryCooldown} (the cuts do
 * not: they keep their own gap). The bounces and the cut blows are {@link DeadpoolEvents}'.
 */
public final class Katanas {
    /** Half the width of the cone his swords reach (degrees): 70 in all. */
    public static final double HALF_ANGLE = 35.0;
    /** The ticks after the last cut the combo starts again. */
    public static final int COMBO_RESET = 30;
    /** How many the X-cut sweeps. */
    public static final int SWEEP = 3;
    /** The pushes of the first two cuts and of the X-cut. */
    static final double PUSH = 0.25;
    static final double X_PUSH = 0.6;

    /** The damage of the {@code step}th cut (1-3) with the server's numbers. */
    public static double damage(int step) {
        return switch (step) {
            case 1 -> DeadpoolAbilities.cfg(DeadpoolConfig.KATANA_DAMAGE_1, 5.0);
            case 2 -> DeadpoolAbilities.cfg(DeadpoolConfig.KATANA_DAMAGE_2, 5.0);
            default -> DeadpoolAbilities.cfg(DeadpoolConfig.KATANA_DAMAGE_3, 8.0);
        };
    }

    /** The step that follows {@code last} (0: none yet) when the last cut fell {@code since} ticks ago. */
    public static int nextStep(int last, long since) {
        return last <= 0 || last >= 3 || since > COMBO_RESET ? 1 : last + 1;
    }

    /** R: the next cut of the combo (the gap kept). */
    static void cut(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.DEADPOOL_KATANAS) || data.is(Live.DEADPOOL_PARRYING)) {
            return;
        }
        DeadpoolAbilities.State state = DeadpoolAbilities.state(player.getUUID());
        long now = player.level().getGameTime();
        if (now - state.lastCut < DeadpoolAbilities.cfg(DeadpoolConfig.KATANA_GAP, 8)) {
            return;
        }
        int step = nextStep(state.combo, now - state.lastCut);
        state.combo = step;
        state.lastCut = now;
        double reach = DeadpoolAbilities.cfg(DeadpoolConfig.KATANA_REACH, 3.5);
        List<LivingEntity> targets = DeadpoolAbilities.inCone(player, reach, HALF_ANGLE, step == 3 ? SWEEP : 1);
        List<Vec3> points = new ArrayList<>();
        ServerLevel level = player.serverLevel();
        for (LivingEntity target : targets) {
            if (DeadpoolAbilities.hurt(player, target, ModDamageTypes.DEADPOOL_KATANA, damage(step), player)) {
                DeadpoolAbilities.push(player, target, player.position(), step == 3 ? X_PUSH : PUSH);
                points.add(target.getBoundingBox().getCenter());
            }
        }
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.SLASH, points, step);
        DeadpoolAbilities.sound(level, player.getEyePosition(), ModSounds.DEADPOOL_KATANA.get(), 0.9F,
                step == 3 ? 0.8F : 1.0F + 0.1F * step);
    }

    /** Sneak + R, held: the swords spin up (if R's slot is rested). */
    static void parry(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.DEADPOOL_PARRY) || data.is(Live.DEADPOOL_PARRYING)) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.DEADPOOL_PARRY, now)) {
            return;
        }
        DeadpoolAbilities.state(player.getUUID()).parryStart = now;
        data.set(Live.DEADPOOL_PARRYING, true);
        ServerLevel level = player.serverLevel();
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.PARRY_ON, List.of(player.position()), 0);
        DeadpoolAbilities.sound(level, player.getEyePosition(), ModSounds.DEADPOOL_PARRY.get(), 0.7F, 1.2F);
    }

    /** R let go of: the Parry ends. */
    static void release(ServerPlayer player, PowerData data) {
        if (data.is(Live.DEADPOOL_PARRYING)) {
            endParry(player, data, true);
        }
    }

    /** The Parry ends; with {@code rest}, R's slot waits {@code parryCooldown}. */
    static void endParry(ServerPlayer player, PowerData data, boolean rest) {
        DeadpoolAbilities.State state = DeadpoolAbilities.peek(player.getUUID());
        if (state != null) {
            state.parryStart = -1L;
        }
        if (!data.is(Live.DEADPOOL_PARRYING)) {
            return;
        }
        data.set(Live.DEADPOOL_PARRYING, false);
        if (rest) {
            data.startCooldown(HeroAbility.DEADPOOL_PARRY, player.level().getGameTime(),
                    DeadpoolAbilities.cfg(DeadpoolConfig.PARRY_COOLDOWN, 60));
        }
        DeadpoolAbilities.send(player.serverLevel(), player, DeadpoolFxPayload.PARRY_OFF, List.of(player.position()),
                0);
    }

    static void tick(ServerPlayer player, PowerData data, DeadpoolAbilities.State state, long now) {
        if (!data.is(Live.DEADPOOL_PARRYING)) {
            state.parryStart = -1L;
            return;
        }
        if (state.parryStart < 0L) {
            state.parryStart = now; // (a Parry he logged in with, or one the data kept: it counts from now)
        }
        if (now - state.parryStart >= DeadpoolAbilities.cfg(DeadpoolConfig.PARRY_MAX_TICKS, 80)) {
            endParry(player, data, true);
        }
    }

    /** Whether {@code from} is before the parrying player (within 90 degrees of where he looks). */
    public static boolean inFront(ServerPlayer player, Vec3 from) {
        Vec3 look = player.getLookAngle();
        Vec3 to = from.subtract(player.getEyePosition());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        Vec3 lookFlat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-4 || lookFlat.lengthSqr() < 1.0E-4) {
            return true;
        }
        return flat.normalize().dot(lookFlat.normalize()) > 0.0;
    }

    /** Whether the player parries right now and {@code attacker}'s blow comes from before him. */
    public static boolean parries(ServerPlayer player, Entity attacker) {
        return com.kolodkingal.enhanced.power.Powers.get(player).is(Live.DEADPOOL_PARRYING)
                && inFront(player, attacker.position().add(0.0, attacker.getBbHeight() * 0.5, 0.0));
    }

    private Katanas() {
    }
}
