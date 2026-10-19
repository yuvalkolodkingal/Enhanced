package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.StrangeFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Shield of the Seraphim (V held) and the Seraphim Dome (Sneak + V), 3.6.
 *
 * <p>The Shield is a round mandala of orange sparks, {@link #DISC_RADIUS} across each way, held {@link #DISC_AHEAD}
 * before his eyes along his look: every projectile that would cross it turns to sparks, a beam ends at it ({@code
 * Shelters} asks {@link #across}), and a blow from the front loses most of its harm ({@link #FRONT_REDUCTION}). It is up
 * while V is held, at most {@code shieldSeconds}, and recharges after it comes down ({@code shieldCooldown}). Its state is
 * the synced {@link Live#STRANGE_SHIELD} (the pose and the drawing read it); it is not the Chaos Shield's record.
 *
 * <p>The Dome is a sphere of interlocking mandalas of radius {@code domeRadius} round him, {@code domeSeconds} long (Titan,
 * the ship's crash): projectiles from outside turn to sparks at its edge, explosions throw nobody inside it, and everyone
 * inside who is no monster takes half the harm.
 */
public final class SeraphimShield {
    /** The disc's radius and how far before his eyes it stands. */
    public static final double DISC_RADIUS = 1.3;
    public static final double DISC_AHEAD = 1.1;
    /** What a blow from the front still does through the Shield. */
    public static final float FRONT_REDUCTION = 0.4F;
    /** What a harm to someone inside the Dome still does. */
    public static final float DOME_REDUCTION = 0.5F;
    /** The Shield's hunger each tick it stands. */
    static final float HOLD_EXHAUSTION = 0.02F;

    /** Strange -> when his Shield went up. */
    private static final Map<UUID, Long> SHIELD_SINCE = new HashMap<>();
    /** Strange -> when his Dome comes down. */
    private static final Map<UUID, Long> DOMES = new HashMap<>();

    // ---- the numbers ----

    public static int shieldTicks() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.SHIELD_SECONDS.get(), 10) * 20;
    }

    public static int shieldCooldown() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.SHIELD_COOLDOWN.get(), 120);
    }

    public static int domeTicks() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.DOME_SECONDS.get(), 6) * 20;
    }

    public static double domeRadius() {
        return StrangeAbilities.cfg(() -> StrangeConfig.DOME_RADIUS.get(), 4.0);
    }

    public static int domeCooldown() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.DOME_COOLDOWN.get(), 400);
    }

    // ---- the geometry (both sides: the client draws the disc where the server holds it) ----

    /** The centre of the disc of a shield held from {@code eye} along {@code look} (a unit vector). */
    public static Vec3 discCenter(Vec3 eye, Vec3 look) {
        return eye.add(look.scale(DISC_AHEAD)).add(0, -0.15, 0);
    }

    /**
     * Where a line from {@code from} to {@code to} crosses a disc at {@code center} facing {@code normal}, coming from its
     * front, within its radius; null if it does not.
     */
    @Nullable
    public static Vec3 crossing(Vec3 center, Vec3 normal, Vec3 from, Vec3 to) {
        double s0 = from.subtract(center).dot(normal);
        double s1 = to.subtract(center).dot(normal);
        if (s0 <= 0.0 || s1 > 0.0) {
            return null;
        }
        double t = s0 / (s0 - s1);
        Vec3 hit = from.add(to.subtract(from).scale(t));
        Vec3 off = hit.subtract(center);
        Vec3 lateral = off.subtract(normal.scale(off.dot(normal)));
        return lateral.lengthSqr() <= DISC_RADIUS * DISC_RADIUS ? hit : null;
    }

    /** Whether this player's Shield of the Seraphim stands now. */
    public static boolean isUp(@Nullable Entity e) {
        if (!(e instanceof Player player)) {
            return false;
        }
        PowerData data = Powers.get(player);
        return data.is(Live.STRANGE_SHIELD) && data.has(HeroAbility.STRANGE_SHIELDS);
    }

    /** A crossing of a Seraphim disc by a beam: whose disc, where, and how far along the beam. */
    public record Crossing(Player owner, Vec3 point, double at) {
    }

    /**
     * The first Shield of the Seraphim that a line of power from {@code from} to {@code to} runs into from its front
     * (never its own caster's), or null: {@code Shelters.across} asks it, so a beam ends at the disc.
     */
    @Nullable
    public static Crossing across(Level level, @Nullable Entity firer, Vec3 from, Vec3 to, float partialTick) {
        Crossing best = null;
        AABB reach = new AABB(from, to).inflate(DISC_AHEAD + DISC_RADIUS + 2.0);
        for (Player p : level.getEntitiesOfClass(Player.class, reach, p -> p != firer && !p.isSpectator() && isUp(p))) {
            Vec3 normal = p.getViewVector(partialTick);
            Vec3 center = discCenter(p.getEyePosition(partialTick), normal);
            Vec3 hit = crossing(center, normal, from, to);
            if (hit != null) {
                double at = hit.distanceTo(from);
                if (best == null || at < best.at()) {
                    best = new Crossing(p, hit, at);
                }
            }
        }
        return best;
    }

    /**
     * Whether a harm comes at him from the front (its source's place within the half of space his Shield faces): a
     * blow, a projectile, an explosion. A harm with no place (a fall, the void, magic) is not stopped.
     */
    public static boolean fromFront(Player player, DamageSource source) {
        Vec3 at = source.getSourcePosition();
        if (at == null) {
            return false;
        }
        Vec3 to = at.subtract(player.getEyePosition());
        return to.lengthSqr() > 1.0E-4 && to.normalize().dot(player.getLookAngle()) > 0.15;
    }

    // ---- V held: the Shield ----

    static void raise(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (data.is(Live.STRANGE_SHIELD)) {
            return;
        }
        if (!data.isReady(HeroAbility.STRANGE_SHIELDS, now)) {
            long left = data.cooldownUntil(data.slot(HeroAbility.STRANGE_SHIELDS)) - now;
            StrangeAbilities.say(player, "message.enhanced.shield.recharging", String.format(Locale.ROOT, "%.1f",
                    left / 20.0));
            return;
        }
        if (StrangeAbilities.refuseHungry(player)) {
            return;
        }
        data.set(Live.STRANGE_SHIELD, true);
        SHIELD_SINCE.put(player.getUUID(), now);
        StrangeAbilities.sound(player, ModSounds.STRANGE_SHIELD_UP.get(), 1.0F, 1.0F);
        StrangeSignals.near(player.serverLevel(), player.position(), StrangeFxPayload.SHIELD, player,
                List.of(player.getEyePosition()), 1);
    }

    /** V let go of: the Shield comes down (a Dome is not held, so the release leaves it). */
    static void release(ServerPlayer player, PowerData data) {
        if (data.is(Live.STRANGE_SHIELD) || SHIELD_SINCE.containsKey(player.getUUID())) {
            drop(player, data);
        }
    }

    /** The Shield comes down and starts to recharge. */
    static void drop(ServerPlayer player, PowerData data) {
        boolean was = data.is(Live.STRANGE_SHIELD);
        data.set(Live.STRANGE_SHIELD, false);
        SHIELD_SINCE.remove(player.getUUID());
        if (was) {
            data.startCooldown(HeroAbility.STRANGE_SHIELDS, player.level().getGameTime(), shieldCooldown());
            StrangeAbilities.sound(player, ModSounds.STRANGE_SHIELD_DOWN.get(), 0.8F, 1.0F);
            StrangeSignals.near(player.serverLevel(), player.position(), StrangeFxPayload.SHIELD, player,
                    List.of(player.getEyePosition()), 0);
        }
    }

    /** The ticks his Shield has stood, or -1. */
    public static long shieldAge(UUID player, long now) {
        Long since = SHIELD_SINCE.get(player);
        return since == null ? -1 : now - since;
    }

    // ---- Sneak + V: the Dome ----

    static void dome(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.STRANGE_SHIELDS, now) || StrangeAbilities.refuseHungry(player)) {
            return;
        }
        if (data.is(Live.STRANGE_SHIELD)) {
            drop(player, data);
        }
        int ticks = domeTicks();
        DOMES.put(player.getUUID(), now + ticks);
        data.startCooldown(HeroAbility.STRANGE_SHIELDS, now, domeCooldown());
        StrangeAbilities.spend(player, data, 2.0F);
        StrangeAbilities.sound(player, ModSounds.STRANGE_DOME.get(), 1.2F, 1.0F);
        Vec3 center = domeCenter(player);
        StrangeSignals.near(level, center, StrangeFxPayload.DOME, player,
                List.of(center, new Vec3(Math.round(domeRadius() * 10.0), 0, 0)), ticks);
    }

    /** The centre of a Dome round this player: his middle. */
    public static Vec3 domeCenter(Entity owner) {
        return owner.position().add(0, owner.getBbHeight() * 0.5, 0);
    }

    /** Whether this player's Dome stands now. */
    public static boolean domeUp(UUID player, long now) {
        Long until = DOMES.get(player);
        return until != null && now < until;
    }

    /** The Dome that holds {@code e} inside it now (its caster), or null. */
    @Nullable
    public static Player domeHolding(Entity e) {
        if (DOMES.isEmpty()) {
            return null;
        }
        long now = e.level().getGameTime();
        double r = domeRadius();
        for (Player p : e.level().players()) {
            if (domeUp(p.getUUID(), now) && e.getBoundingBox().getCenter().distanceToSqr(domeCenter(p)) <= r * r) {
                return p;
            }
        }
        return null;
    }

    /** Whether the Dome shelters this one: he is inside and is no monster (the caster, his friends, their pets). */
    public static boolean shelters(LivingEntity e) {
        return !(e instanceof Enemy) && domeHolding(e) != null;
    }

    // ---- the tick ----

    static void tick(ServerPlayer player, PowerData data, long now) {
        if (data.is(Live.STRANGE_SHIELD) || SHIELD_SINCE.containsKey(player.getUUID())) {
            if (!data.has(HeroAbility.STRANGE_SHIELDS) || !data.is(Live.STRANGE_SHIELD)) {
                drop(player, data);
            } else {
                shieldTick(player, data, now);
            }
        }
        Long until = DOMES.get(player.getUUID());
        if (until != null) {
            if (now >= until || !data.has(HeroAbility.STRANGE_SHIELDS)) {
                DOMES.remove(player.getUUID());
            } else {
                domeTick(player);
            }
        }
    }

    private static void shieldTick(ServerPlayer player, PowerData data, long now) {
        long age = shieldAge(player.getUUID(), now);
        if (age < 0) {
            SHIELD_SINCE.put(player.getUUID(), now);
            age = 0;
        }
        if (age >= shieldTicks() || !StrangeAbilities.fed(player)) {
            drop(player, data);
            return;
        }
        StrangeAbilities.spend(player, data, HOLD_EXHAUSTION);
        ServerLevel level = player.serverLevel();
        Vec3 normal = player.getLookAngle();
        Vec3 center = discCenter(player.getEyePosition(), normal);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, new AABB(center, center).inflate(6.0),
                p -> p.getOwner() != player && !ApocalypseGuard.headingHome(p))) {
            Vec3 from = projectile.position();
            Vec3 to = from.add(projectile.getDeltaMovement().scale(1.5));
            if (crossing(center, normal, from, to) != null || nearDisc(center, normal, from)) {
                stop(level, projectile);
            }
        }
    }

    /** A projectile already touching the disc from the front (a slow one, or one that spawned there). */
    private static boolean nearDisc(Vec3 center, Vec3 normal, Vec3 at) {
        Vec3 off = at.subtract(center);
        double s = off.dot(normal);
        Vec3 lateral = off.subtract(normal.scale(s));
        return s >= -0.1 && s < 0.5 && lateral.lengthSqr() <= DISC_RADIUS * DISC_RADIUS;
    }

    private static void domeTick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 center = domeCenter(player);
        double r = domeRadius();
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, new AABB(center, center).inflate(r + 4.0),
                p -> !ApocalypseGuard.headingHome(p))) {
            Entity owner = projectile.getOwner();
            if (owner != null && owner.position().distanceToSqr(center) <= r * r) {
                continue; // (fired from inside: it goes out)
            }
            Vec3 from = projectile.position();
            Vec3 to = from.add(projectile.getDeltaMovement());
            double d0 = from.distanceTo(center);
            double d1 = to.distanceTo(center);
            if (d0 <= r + 0.3 || d1 <= r + 0.3) {
                stop(level, projectile);
            }
        }
    }

    /** A projectile stopped by a mandala: orange sparks (a thrown weapon of a hero turns home). */
    static void stop(ServerLevel level, Projectile projectile) {
        WandaAbilities.neutralize(level, projectile, StrangeAbilities.ORANGE);
    }

    // ---- the harm (StrangeEvents, before the shared chain) ----

    /**
     * The harm to {@code victim} as the Shield and the Dome leave it: a projectile at the Shield's front is stopped (0),
     * a blow or a blast from the front loses {@code 1 - FRONT_REDUCTION}; inside a Dome the rest is halved. Harm that
     * nothing stops ({@code bypasses_invulnerability}) is as it was.
     */
    public static float harm(LivingEntity victim, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        if (victim instanceof Player player && isUp(player) && fromFront(player, source)) {
            if (source.getDirectEntity() instanceof Projectile) {
                return 0.0F;
            }
            amount *= FRONT_REDUCTION;
        }
        if (shelters(victim)) {
            amount *= DOME_REDUCTION;
        }
        return amount;
    }

    /** The HUD of the Shield: the seconds it can still stand (gold, then red at the end). */
    public static Component shieldLine(long age) {
        float left = Math.max(0, shieldTicks() - age) / 20.0F;
        return Component.translatable("hud.enhanced.strange.shield", String.format(Locale.ROOT, "%.1f", left))
                .withStyle(left < 2.0F ? ChatFormatting.RED : ChatFormatting.GOLD);
    }

    /** The share of the Shield's time left (for the client's drawing): 1 fresh, 0 at its end. */
    public static float shieldLeft(long age) {
        return Mth.clamp(1.0F - (float) age / shieldTicks(), 0.0F, 1.0F);
    }

    // ---- the end ----

    static void endAll(ServerPlayer player, PowerData data) {
        if (data.is(Live.STRANGE_SHIELD) || SHIELD_SINCE.containsKey(player.getUUID())) {
            drop(player, data);
        }
        DOMES.remove(player.getUUID());
    }

    static void clear() {
        SHIELD_SINCE.clear();
        DOMES.clear();
    }

    private SeraphimShield() {
    }
}
