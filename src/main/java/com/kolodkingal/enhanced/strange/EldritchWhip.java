package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.StrangeFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Eldritch Whip (R) and the Whip Grapple (Sneak + R), 3.6. A whip of orange sparks lashes the first creature in a
 * narrow cone before him and pulls it toward him; three lashes make a combo (the third wraps it and lifts it). The
 * Grapple catches the block he looks at and pulls him there in an arc, the fall forgotten for the swing.
 */
public final class EldritchWhip {
    /** Half the width of the cone a lash finds its creature in (degrees). */
    public static final double CONE = 25.0;
    /** Ticks between two lashes, and how long after one the next still continues the combo. */
    public static final int GAP = 10;
    public static final int COMBO_RESET = 30;
    /** How hard a lash pulls (a speed toward him: about two blocks), and how high the third lifts (about one). */
    static final double PULL = 0.55;
    static final double LIFT = 0.42;
    /** The Grapple: the longest swing (ticks), the speed it pulls at, and how near the anchor it lets go. */
    public static final int GRAPPLE_TICKS = 40;
    static final double GRAPPLE_SPEED = 1.3;
    static final double GRAPPLE_ARRIVE = 1.6;
    /** After a swing, his fall is forgotten this long (ticks). */
    static final int SAFE_AFTER = 30;

    record Combo(int step, long at) {
    }

    /** A Grapple in progress: the point it pulls to, when it began and when the fall stops being forgiven. */
    record Swing(Vec3 anchor, long since) {
    }

    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    private static final Map<UUID, Swing> SWINGS = new HashMap<>();
    private static final Map<UUID, Long> SAFE_UNTIL = new HashMap<>();

    // ---- the numbers ----

    public static double reach() {
        return StrangeAbilities.cfg(() -> StrangeConfig.WHIP_REACH.get(), 8.0);
    }

    /** The damage of lash {@code step} (1-3): 7, 7 and 10 by default. */
    public static float damage(int step) {
        return (float) switch (step) {
            case 1 -> StrangeAbilities.cfg(() -> StrangeConfig.WHIP_DAMAGE1.get(), 7.0);
            case 2 -> StrangeAbilities.cfg(() -> StrangeConfig.WHIP_DAMAGE2.get(), 7.0);
            default -> StrangeAbilities.cfg(() -> StrangeConfig.WHIP_DAMAGE3.get(), 10.0);
        };
    }

    public static int grappleRange() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.GRAPPLE_RANGE.get(), 24);
    }

    public static int grappleCooldown() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.GRAPPLE_COOLDOWN.get(), 60);
    }

    /** The lash that follows {@code last} (struck at {@code lastAt}) at {@code now}: the next of three, or the first. */
    public static int nextStep(int last, long lastAt, long now) {
        return last <= 0 || last >= 3 || now - lastAt > COMBO_RESET ? 1 : last + 1;
    }

    /** The lash his combo is on (the last one struck, 1-3), or 0 when it has lapsed. */
    public static int comboStep(UUID player, long now) {
        Combo combo = COMBOS.get(player);
        return combo == null || now - combo.at() > COMBO_RESET ? 0 : combo.step();
    }

    /** Whether he swings on the Grapple now. */
    public static boolean swinging(UUID player) {
        return SWINGS.containsKey(player);
    }

    /** Whether his fall is forgiven (a swing, or just after one). */
    public static boolean fallForgiven(UUID player, long now) {
        return SWINGS.containsKey(player) || now < SAFE_UNTIL.getOrDefault(player, Long.MIN_VALUE);
    }

    // ---- R: the lash ----

    /**
     * The creature a lash finds: the nearest living one within {@code reach} of his eyes whose centre lies within {@link
     * #CONE} degrees of his look, in his sight (no wall between), that he may hurt. Null if none.
     */
    @Nullable
    public static LivingEntity target(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(CONE));
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(reach + 1.0), e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double distance = to.length();
            if (distance > reach + e.getBbWidth() * 0.5 || distance < 1.0E-4) {
                continue;
            }
            if (to.dot(look) / distance < cos && distance > 1.5) {
                continue;
            }
            if (distance >= bestDistance || !AreaTargets.mayHurt(player, e, false) || !inSight(player, eye, e)) {
                continue;
            }
            best = e;
            bestDistance = distance;
        }
        return best;
    }

    private static boolean inSight(ServerPlayer player, Vec3 eye, LivingEntity e) {
        BlockHitResult hit = player.level().clip(new ClipContext(eye, e.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** R: the next lash of the combo. */
    static void lash(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.STRANGE_WHIP, now) || StrangeAbilities.refuseHungry(player)) {
            return;
        }
        Combo last = COMBOS.get(player.getUUID());
        int step = last == null ? 1 : nextStep(last.step(), last.at(), now);
        COMBOS.put(player.getUUID(), new Combo(step, now));
        double reach = reach();
        LivingEntity target = target(player, reach);
        Vec3 end;
        if (target != null) {
            end = target.getBoundingBox().getCenter();
            if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.ELDRITCH_WHIP, player, player), damage(step))) {
                pull(player, target, step);
            }
        } else {
            end = player.getEyePosition().add(player.getLookAngle().scale(reach));
        }
        data.startCooldown(HeroAbility.STRANGE_WHIP, now, step == 3 ? GAP + 6 : GAP);
        StrangeAbilities.spend(player, data, 0.3F);
        StrangeAbilities.sound(player, ModSounds.STRANGE_WHIP.get(), 1.0F, 0.9F + 0.1F * step);
        StrangeSignals.near(level, player.position(), StrangeFxPayload.WHIP, player, List.of(end), step);
    }

    /** The lash pulls the creature toward him; the third wraps it and lifts it. */
    static void pull(ServerPlayer player, LivingEntity target, int step) {
        Vec3 toward = player.position().subtract(target.position()).multiply(1, 0, 1);
        toward = toward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : toward.normalize();
        double strength = 1.0 - target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes
                .KNOCKBACK_RESISTANCE);
        if (strength <= 0.0) {
            return;
        }
        Vec3 motion = toward.scale(PULL * strength).add(0, (step == 3 ? LIFT : 0.18) * strength, 0);
        target.setDeltaMovement(motion);
        target.hurtMarked = true;
    }

    // ---- Sneak + R: the Grapple ----

    /** Sneak + R: the whip catches the block he looks at and pulls him there. */
    static void grapple(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.STRANGE_WHIP, now) || StrangeAbilities.refuseHungry(player)) {
            return;
        }
        BlockHitResult hit = Raycasts.block(player, grappleRange(), 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            StrangeAbilities.say(player, "message.enhanced.strange.grapple_nothing");
            return;
        }
        Vec3 anchor = hit.getLocation();
        SWINGS.put(player.getUUID(), new Swing(anchor, now));
        player.resetFallDistance();
        swingTick(player, anchor, 0);
        data.startCooldown(HeroAbility.STRANGE_WHIP, now, grappleCooldown());
        StrangeAbilities.spend(player, data, 0.6F);
        StrangeAbilities.sound(player, ModSounds.STRANGE_GRAPPLE.get(), 1.0F, 1.0F);
        StrangeSignals.near(level, player.position(), StrangeFxPayload.WHIP, player, List.of(anchor),
                StrangeSignals.GRAPPLE);
    }

    /**
     * The pull of one tick of a swing toward {@code anchor}: straight for it at the swing's speed, with a lift that
     * makes the path an arc (more while far, none at the end), gentler as he arrives.
     */
    public static Vec3 swingVelocity(Vec3 from, Vec3 anchor, int age) {
        Vec3 to = anchor.subtract(from);
        double distance = to.length();
        if (distance < 1.0E-4) {
            return Vec3.ZERO;
        }
        double speed = Math.min(GRAPPLE_SPEED, 0.35 + distance * 0.25);
        double lift = age < 6 ? Math.min(0.35, distance * 0.03) : 0.0;
        return to.scale(speed / distance).add(0, lift, 0);
    }

    private static void swingTick(ServerPlayer player, Vec3 anchor, int age) {
        player.setDeltaMovement(swingVelocity(player.position(), anchor, age));
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    // ---- the tick ----

    static void tick(ServerPlayer player, PowerData data, long now) {
        Swing swing = SWINGS.get(player.getUUID());
        if (swing == null) {
            return;
        }
        int age = (int) (now - swing.since());
        double distance = player.position().distanceTo(swing.anchor());
        if (!data.has(HeroAbility.STRANGE_WHIP) || age > GRAPPLE_TICKS || distance < GRAPPLE_ARRIVE
                || player.isSpectator()) {
            SWINGS.remove(player.getUUID());
            SAFE_UNTIL.put(player.getUUID(), now + SAFE_AFTER);
            if (distance < GRAPPLE_ARRIVE) {
                // He arrives: the pull lets go gently.
                player.setDeltaMovement(player.getDeltaMovement().scale(0.3));
                player.hurtMarked = true;
            }
            return;
        }
        if (age > 0) {
            swingTick(player, swing.anchor(), age);
        }
    }

    static void forget(UUID id) {
        COMBOS.remove(id);
        SWINGS.remove(id);
        SAFE_UNTIL.remove(id);
    }

    static void clear() {
        COMBOS.clear();
        SWINGS.clear();
        SAFE_UNTIL.clear();
    }

    private EldritchWhip() {
    }
}
