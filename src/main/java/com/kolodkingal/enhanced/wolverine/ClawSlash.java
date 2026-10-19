package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * R: the Claw Slash, and Sneak + R: the Spin Slash (3.7). The slash is a three-hit combo on the first creature in a
 * 60-degree cone before him ({@code slashReach} 3.5): {@code slashDamage1/2/3} 6, 6 and 9 ({@code wolverine_claws}),
 * the third a cross-slash that throws it back 0.8 and sweeps up to three creatures of the cone. A hit may follow the
 * last after {@code slashGap} 10 ticks (6 in the Rage); {@code comboReset} 30 ticks without one starts the combo again.
 * The claws come out for it if they are in. The spin is a full turn with both hands out: {@code spinDamage} 7 to every
 * creature within {@code spinRadius} 3 he may hurt, thrown back 0.6, the arrows round him knocked down; its cooldown is
 * {@code spinCooldown} 80.
 */
public final class ClawSlash {
    /** Half the cone's angle (degrees). */
    public static final double CONE = 30.0;
    /** The cross-slash's throw, and the spin's. */
    public static final double CROSS_PUSH = 0.8;
    public static final double SPIN_PUSH = 0.6;
    /** At most this many creatures in the cross-slash. */
    public static final int CROSS_SWEEP = 3;

    /**
     * R: the next hit of the combo. Returns the hit struck (0-2), or -1 when it was too soon for one.
     */
    public static int slash(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.WOLVERINE_SLASH)) {
            return -1;
        }
        WolverineAbilities.State state = WolverineAbilities.state(player.getUUID());
        long now = player.level().getGameTime();
        if (now < state.nextSlash) {
            return -1;
        }
        int reset = WolverineAbilities.cfg(WolverineConfig.COMBO_RESET, 30);
        int hit = state.combo < 0 || now - state.lastSlash > reset ? 0 : (state.combo + 1) % 3;
        state.combo = hit;
        state.lastSlash = now;
        int gap = Berserker.raging(player) ? Berserker.RAGE_SLASH_GAP : WolverineAbilities.cfg(WolverineConfig.SLASH_GAP,
                10);
        state.nextSlash = now + gap;
        Claws.popOut(player, data);
        ServerLevel level = player.serverLevel();
        double reach = WolverineAbilities.cfg(WolverineConfig.SLASH_REACH, 3.5);
        List<LivingEntity> cone = cone(player, reach);
        float damage = (float) switch (hit) {
            case 0 -> WolverineAbilities.cfg(WolverineConfig.SLASH_DAMAGE_1, 6.0);
            case 1 -> WolverineAbilities.cfg(WolverineConfig.SLASH_DAMAGE_2, 6.0);
            default -> WolverineAbilities.cfg(WolverineConfig.SLASH_DAMAGE_3, 9.0);
        } * WolverineAbilities.rageFactor(player);
        int count = hit == 2 ? Math.min(CROSS_SWEEP, cone.size()) : Math.min(1, cone.size());
        boolean metal = false;
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.6));
        for (int i = 0; i < count; i++) {
            LivingEntity target = cone.get(i);
            if (i == 0) {
                at = target.position().add(0, target.getBbHeight() * 0.6, 0);
            }
            if (WolverineAbilities.hurt(player, target, ModDamageTypes.WOLVERINE_CLAWS, damage) && hit == 2) {
                WolverineAbilities.push(target, player.position(), CROSS_PUSH, 0.25, player);
            }
            metal |= metal(target);
        }
        WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_SLASH.get(), 1.0F,
                0.9F + hit * 0.08F + level.random.nextFloat() * 0.06F);
        if (metal) {
            WolverineAbilities.sound(level, at, ModSounds.WOLVERINE_SLASH_HIT_METAL.get(), 0.9F, 1.0F);
        }
        WolverineAbilities.send(level, player, WolverineFxPayload.SLASH, List.of(at), hit);
        return hit;
    }

    /** Sneak + R: the Spin Slash. Returns the creatures it struck, or null when it could not (the cooldown). */
    public static List<LivingEntity> spin(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.WOLVERINE_SPIN_SLASH) || !data.isReady(HeroAbility.WOLVERINE_SPIN_SLASH, now)) {
            return null;
        }
        data.startCooldown(HeroAbility.WOLVERINE_SPIN_SLASH, now, WolverineAbilities.cfg(WolverineConfig.SPIN_COOLDOWN,
                80));
        Claws.popOut(player, data);
        ServerLevel level = player.serverLevel();
        double radius = WolverineAbilities.cfg(WolverineConfig.SPIN_RADIUS, 3.0);
        float damage = (float) WolverineAbilities.cfg(WolverineConfig.SPIN_DAMAGE, 7.0)
                * WolverineAbilities.rageFactor(player);
        List<LivingEntity> struck = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius + 1.0,
                2.0, radius + 1.0), e -> WolverineAbilities.mayHurt(player, e))) {
            double flat = Math.hypot(e.getX() - player.getX(), e.getZ() - player.getZ()) - e.getBbWidth() * 0.5;
            if (flat > radius || Math.abs(e.getY() - player.getY()) > 2.0) {
                continue;
            }
            if (WolverineAbilities.hurt(player, e, ModDamageTypes.WOLVERINE_CLAWS, damage)) {
                WolverineAbilities.push(e, player.position(), SPIN_PUSH, 0.2, player);
                struck.add(e);
            }
        }
        for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox()
                .inflate(radius + 1.0), a -> a.getOwner() != player)) {
            Vec3 m = arrow.getDeltaMovement();
            arrow.setDeltaMovement(m.x * 0.1, -0.2, m.z * 0.1);
            arrow.hurtMarked = true;
        }
        WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_SPIN.get(), 1.1F, 1.0F);
        WolverineAbilities.send(level, player, WolverineFxPayload.SPIN, List.of(player.position()),
                (int) Math.round(radius));
        return struck;
    }

    /**
     * The creatures in the cone before him within {@code reach} (beyond their half width), that he may hurt and can
     * see, the nearest first.
     */
    static List<LivingEntity> cone(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double cos = Math.cos(Math.toRadians(CONE));
        AABB box = player.getBoundingBox().inflate(reach + 1.0);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                e -> WolverineAbilities.mayHurt(player, e))) {
            Vec3 centre = e.position().add(0, e.getBbHeight() * 0.5, 0);
            Vec3 to = centre.subtract(eye);
            double distance = to.length();
            if (distance - e.getBbWidth() * 0.5 > reach) {
                continue;
            }
            // (Close enough to touch, he never misses it, whatever the angle of his look.)
            boolean touching = distance < e.getBbWidth() * 0.5 + 0.8;
            if (!touching && to.normalize().dot(look) < cos) {
                continue;
            }
            if (!player.hasLineOfSight(e)) {
                continue;
            }
            found.add(e);
        }
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return found;
    }

    /** Whether a blow on the creature rings on metal (an iron golem, armour, or a metal body). */
    static boolean metal(LivingEntity target) {
        return target instanceof IronGolem || target.getArmorValue() >= 6
                || com.kolodkingal.enhanced.ability.Metal.isMetalBody(target);
    }

    /** The hit of the combo the player struck last (0-2, -1 none): for his HUD and the tests. */
    public static int combo(ServerPlayer player) {
        WolverineAbilities.State state = WolverineAbilities.peek(player.getUUID());
        return state == null ? -1 : state.combo;
    }

    private ClawSlash() {
    }
}
