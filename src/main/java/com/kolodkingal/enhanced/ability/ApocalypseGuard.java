package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFx;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.MindPower;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Apocalypse's Shield (B, hold), the hero's half of the rule that makes the boss's shield what it is: a golden sphere that
 * stops everything except the mind power of Wanda and of the Phoenix. It is a commitment like the Chaos Shield: it takes
 * {@code shieldWindUpTicks} to form and stops nothing until it is complete, holds at most {@code shieldMaxTicks}, has
 * {@code shieldStrength} points, and recharges after a drop ({@code shieldCooldown}) or after it shattered or was held to
 * its limit ({@code shieldBrokenCooldown}). It reuses the synced fields of the Chaos Shield ({@code chaosShieldActive},
 * {@code shieldFormedAt}, {@code shieldStrength}), so no codec changes.
 *
 * <p><b>What it does with a blow.</b> {@link #stops} says which damage it stops: all of it but the world's own harm (falls,
 * drowning, suffocation, starvation, cramming, the void, {@code /kill}). A stopped blow gives no knockback. Most of them cost
 * the shield {@code shieldOtherWear} of the damage they would have done (the boss's shield costs nothing; 0 here is the
 * literal rule of the film and of Eilon). <em>Mind power</em> ({@link MindPower}) wears it by its amount times its factor
 * instead, spaced by the shield's own {@code mindHitCooldownTicks} per attacker; when the strength is gone it shatters and the
 * rest of that hit goes through.
 *
 * <p>The damage hook is {@link #guard}, run by {@code ApocalypseEvents} at the highest priority so that it comes before
 * Wanda's branch of {@code CommonEvents}; the projectile hooks are {@link #neutralise} and the sweep in {@link #tick}.
 */
public final class ApocalypseGuard {
    /** The cooldown slot of B. */
    public static final int SLOT = 4;
    /** The gold of the shield. */
    public static final int GOLD = ApocalypseEntity.SHIELD_GOLD;
    private static final int GOLD_LIGHT = 0xFFF2B0;
    /** How many gold shards fly out when the shield shatters. */
    private static final int SHATTER_SHARDS = 56;
    /** Two blows' ripples are at least this many ticks apart (the shield does not ring at a beam's every tick). */
    private static final int RIPPLE_EVERY = 4;
    /** Less than this much strength left is none. */
    private static final float EPSILON = 1.0E-4F;

    /** Hero -> attacker -> game time of the last mind-power hit the shield counted. */
    private static final Map<UUID, Map<UUID, Long>> MIND_HITS = new HashMap<>();
    /** Hero -> game time of the last ripple (and its sound). */
    private static final Map<UUID, Long> LAST_RIPPLE = new HashMap<>();
    /** Hero -> when each Phoenix's touch last strained his shield ({@link MindPower#strainCounts}). */
    private static final Map<UUID, Map<String, Long>> STRAINS = new HashMap<>();

    // ---- the numbers (clients ask too: their animation predicts the press) ----

    /** The strength a fresh shield has, in points (this is also the most a client can show). */
    public static float maxStrength() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.SHIELD_STRENGTH.get().floatValue() : 60.0F;
    }

    /** How long the shield takes to form after B is pressed, in ticks. */
    public static int windUpTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.SHIELD_WIND_UP_TICKS.get() : 10;
    }

    /** How long a complete shield can be held, in ticks. */
    public static int maxTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.SHIELD_MAX_TICKS.get() : 120;
    }

    /** The drawn radius of the sphere, and how close a projectile is turned away. */
    public static double radius() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.SHIELD_RADIUS.get() : 2.4;
    }

    /** Whether the shield can be held: it costs hunger, so an empty stomach is refused. */
    public static boolean canAfford(Player player) {
        return player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0;
    }

    /** The shield is held and complete: only then does it stop anything. */
    public static boolean isUp(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.APOCALYPSE && data.chaosShieldUp(player.level().getGameTime());
    }

    // ---- the rule ----

    /**
     * Whether the shield stops this damage: all of it except what the world does to a body that no shield could help
     * (falls, drowning, suffocation, starvation, cramming, flying into a wall, the world border, the void and
     * {@code /kill}). Beams, lightning, magic, fire, lava, explosions (all of them, not three quarters), arrows, tridents and
     * blows are stopped.
     */
    public static boolean stops(DamageSource source) {
        return !(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypes.OUTSIDE_BORDER) || source.is(DamageTypes.GENERIC_KILL));
    }

    /**
     * The damage hook, for the hero whose shield is complete: cancels the blow (it costs the shield what it costs), or, if the
     * shield gives way under it, lets the rest through. Called at the highest priority of
     * {@code LivingIncomingDamageEvent}, before Wanda's branch.
     *
     * @return whether the shield took the blow (the event was cancelled or its amount cut)
     */
    public static boolean guard(ServerPlayer player, LivingIncomingDamageEvent event) {
        if (!isUp(player)) {
            return false;
        }
        DamageSource source = event.getSource();
        if (!stops(source)) {
            return false;
        }
        PowerData data = Powers.get(player);
        long now = player.level().getGameTime();
        float amount = event.getAmount();

        float mind = MindPower.shieldDamage(source, amount);
        if (mind > 0.0F) {
            // The mind: it wears the shield by its factor, once in a while per attacker; the blows in between are nothing.
            if (!countable(player, source.getEntity(), now)) {
                event.setCanceled(true);
                return true;
            }
            float left = absorb(player, data, mind);
            passTheRest(event, left, MindPower.factor(source));
        } else {
            float otherWear = ApocalypseConfig.SHIELD_OTHER_WEAR.get().floatValue();
            float wear = amount * otherWear;
            passTheRest(event, wear <= 0.0F ? 0.0F : absorb(player, data, wear), otherWear);
        }
        if (data.chaosShieldActive()) {
            ripple(player, data, source, now);
        }
        return true;
    }

    /** What was not taken by the shield (in strength points) goes through as damage; nothing left, the blow is stopped. */
    private static void passTheRest(LivingIncomingDamageEvent event, float strengthLeft, float factor) {
        if (strengthLeft > EPSILON && factor > 0.0F) {
            event.setAmount(strengthLeft / factor);
        } else {
            event.setCanceled(true);
        }
    }

    /**
     * Takes {@code cost} points from the shield's strength; if there is not so much, the shield shatters.
     *
     * @return the points the shield could not take (0 if it took them all)
     */
    private static float absorb(ServerPlayer player, PowerData data, float cost) {
        float strength = data.shieldStrength();
        if (cost < strength - EPSILON) {
            data.setShieldStrength(strength - cost);
            return 0.0F;
        }
        shatter(player, data);
        return Math.max(0.0F, cost - strength);
    }

    /**
     * A Phoenix's Mind Control or Psionic Field touches the hero: his complete shield is strained by {@code
     * phoenixMindControlStrain} or {@code phoenixPsionicStrain} (not damage; it may shatter it), once per cast ({@link
     * MindPower#strainCounts}), where an attack of hers could reach him (PvP). Her touch still does to him what it does to
     * any player. Called by the Phoenix listener of {@code ApocalypseSetup}.
     */
    public static void strain(ServerPlayer hero, ServerPlayer phoenix, PhoenixPower.Kind kind) {
        float points = MindPower.strain(kind);
        if (points <= 0.0F || !isUp(hero) || !MindPower.isBreaker(phoenix) || !AreaTargets.mayHurt(phoenix, hero, false)) {
            return;
        }
        long now = hero.level().getGameTime();
        if (!MindPower.strainCounts(STRAINS.computeIfAbsent(hero.getUUID(), id -> new HashMap<>()), phoenix.getUUID(), kind, now)) {
            return;
        }
        PowerData data = Powers.get(hero);
        absorb(hero, data, points);
        if (data.chaosShieldActive()) {
            Vec3 center = hero.position().add(0, hero.getBbHeight() * 0.5, 0);
            Vec3 away = phoenix.getEyePosition().subtract(center);
            ripple(hero, data, away.lengthSqr() < 1.0E-6 ? center.add(0, radius(), 0) : center.add(away.normalize().scale(radius())), now, true);
        }
    }

    /** Whether a mind-power hit of this attacker is to be counted now (the shield's own cooldown per attacker). */
    private static boolean countable(ServerPlayer hero, Entity attacker, long now) {
        if (attacker == null) {
            return true;
        }
        Map<UUID, Long> hits = MIND_HITS.computeIfAbsent(hero.getUUID(), id -> new HashMap<>());
        Long last = hits.get(attacker.getUUID());
        if (last != null && now - last < ApocalypseShieldConfig.MIND_HIT_COOLDOWN_TICKS.get()) {
            return false;
        }
        hits.put(attacker.getUUID(), now);
        return true;
    }

    // ---- projectiles ----

    /**
     * A projectile reaches the shield: it is turned away before it hits (gold dust; Mjolnir, Stormbreaker and the fragments
     * of a shattered Mjolnir fly back to Thor), and the shield pays {@code shieldOtherWear} of what it would have cost.
     * Called by the impact hook, and by the sweep of {@link #tick}.
     */
    public static void neutralise(ServerPlayer player, Projectile projectile) {
        ServerLevel level = player.serverLevel();
        float cost = WandaAbilities.projectilePower(projectile) * ApocalypseConfig.SHIELD_OTHER_WEAR.get().floatValue();
        WandaAbilities.neutralize(level, projectile, GOLD);
        PowerData data = Powers.get(player);
        if (cost > 0.0F) {
            absorb(player, data, cost);
        }
        if (data.chaosShieldActive()) {
            ripple(player, data, projectile.position(), level.getGameTime(), true);
        }
    }

    /** A projectile already on its way home to its owner (a weapon of Thor's, Thanos's sword): the shield lets it by. */
    public static boolean headingHome(Projectile projectile) {
        if (projectile instanceof ShieldCost cost && cost.oneOfAKind()) {
            return cost.returning();
        }
        return projectile instanceof ThrownWeaponEntity weapon ? weapon.isReturning()
                : projectile instanceof MjolnirFragmentEntity fragment && fragment.isReturning();
    }

    // ---- raising, holding, dropping ----

    /** B pressed: the shield starts to form, if it has recharged and there is food to hold it with. */
    static void raise(ServerPlayer player, PowerData data) {
        if (player.isSpectator() || data.chaosShieldActive()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(SLOT, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.shield.recharging",
                    String.format(Locale.ROOT, "%.1f", (data.cooldownUntil(SLOT) - now) / 20.0)), true);
            return;
        }
        if (!canAfford(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.shield.hungry"), true);
            return;
        }
        MIND_HITS.remove(player.getUUID());
        data.raiseChaosShield(now, ApocalypseConfig.SHIELD_WIND_UP_TICKS.get(), maxStrength());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.APOCALYPSE_SHIELD_UP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /**
     * The shield comes down (B released, hunger, or the time limit) and has to recharge: the short cooldown, or the long one
     * if it was {@code worn} out (held to its limit; a shattered shield is handled by {@link #shatter}).
     */
    static void drop(ServerPlayer player, PowerData data, boolean worn) {
        data.setChaosShieldActive(false);
        data.startCooldown(SLOT, player.level().getGameTime(),
                (worn ? ApocalypseConfig.SHIELD_BROKEN_COOLDOWN : ApocalypseConfig.SHIELD_COOLDOWN).get());
        forget(player.getUUID());
        ServerLevel level = player.serverLevel();
        if (worn) {
            level.sendParticles(new EnergyParticleOptions(GOLD, 1.2F), player.getX(), player.getY() + player.getBbHeight() * 0.5,
                    player.getZ(), 24, radius() * 0.5, 0.7, radius() * 0.5, 0.05);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.7F);
    }

    /** The shield took all it could: gold shards and a glassy sound, and a long wait before the next one. */
    static void shatter(ServerPlayer player, PowerData data) {
        data.setChaosShieldActive(false);
        data.setShieldStrength(0.0F);
        ServerLevel level = player.serverLevel();
        data.startCooldown(SLOT, level.getGameTime(), ApocalypseConfig.SHIELD_BROKEN_COOLDOWN.get());
        forget(player.getUUID());
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        RandomSource random = player.getRandom();
        for (int i = 0; i < SHATTER_SHARDS; i++) {
            // Out from a random spot on the sphere, along the way out.
            Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3 at = center.add(out.scale(radius()));
            level.sendParticles(new EnergyParticleOptions(i % 3 == 0 ? GOLD_LIGHT : GOLD, 1.5F), at.x, at.y, at.z, 0, out.x, out.y, out.z, 0.3);
        }
        level.playSound(null, center.x, center.y, center.z, ModSounds.APOCALYPSE_SHIELD_BREAK.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        ApocalypseFx.send(level, center, ApocalypseFxPayload.SHIELD_BREAK, player.getId(), center, Vec3.ZERO, 0, ApocalypseShield.State.BROKEN.ordinal());
        player.displayClientMessage(Component.translatable("message.enhanced.shield.shattered").withStyle(ChatFormatting.GOLD), true);
    }

    /** Every tick the shield is held (or forming): hunger, the time limit, the gathering motes, and the projectiles it turns away. */
    static void tick(ServerPlayer player, PowerData data) {
        if (player.isSpectator()) {
            data.setChaosShieldActive(false);
            return;
        }
        if (!canAfford(player)) {
            drop(player, data, false);
            player.displayClientMessage(Component.translatable("message.enhanced.shield.hungry"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now >= data.shieldFormedAt() + ApocalypseConfig.SHIELD_MAX_TICKS.get()) {
            drop(player, data, true);
            return;
        }
        player.causeFoodExhaustion(ApocalypseConfig.SHIELD_EXHAUSTION.get().floatValue());

        double radius = radius();
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        if (!data.chaosShieldUp(now)) {
            // Still forming: motes gather onto the growing sphere. It stops nothing yet.
            float grown = data.chaosShieldProgress(now);
            RandomSource random = player.getRandom();
            for (int i = 0; i < 2; i++) {
                Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
                Vec3 at = center.add(out.scale(radius * Math.max(0.3, grown)));
                level.sendParticles(new EnergyParticleOptions(GOLD_LIGHT, 1.0F), at.x, at.y, at.z, 0, -out.x, -out.y, -out.z, 0.1);
            }
            return;
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius + 4.0),
                p -> p.getOwner() != player && !headingHome(p))) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 toPlayer = center.subtract(projectile.position());
            double reach = radius + 0.5 + velocity.length();
            if (velocity.dot(toPlayer) > 0 && toPlayer.lengthSqr() < reach * reach) {
                neutralise(player, projectile);
                if (!data.chaosShieldActive()) {
                    return; // shattered: the rest get through
                }
            }
        }
    }

    // ---- the picture ----

    private static void ripple(ServerPlayer player, PowerData data, DamageSource source, long now) {
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Entity from = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
        Vec3 away = from != null ? from.position().add(0, from.getBbHeight() * 0.5, 0).subtract(center)
                : source.getSourcePosition() != null ? source.getSourcePosition().subtract(center) : new Vec3(0, 1, 0);
        ripple(player, data, away.lengthSqr() < 1.0E-6 ? center.add(0, radius(), 0) : center.add(away.normalize().scale(radius())), now, false);
    }

    /** A golden ripple at the point of impact and a glassy tink (the tink is rate-limited: a beam hits every tick). */
    private static void ripple(ServerPlayer player, PowerData data, Vec3 at, long now, boolean force) {
        Long last = LAST_RIPPLE.get(player.getUUID());
        if (!force && last != null && now - last < RIPPLE_EVERY) {
            return;
        }
        LAST_RIPPLE.put(player.getUUID(), now);
        ServerLevel level = player.serverLevel();
        float fraction = Math.max(0.0F, Math.min(1.0F, data.shieldStrength() / maxStrength()));
        level.playSound(null, at.x, at.y, at.z, ModSounds.APOCALYPSE_SHIELD_HIT.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        ApocalypseFx.send(level, at, ApocalypseFxPayload.SHIELD_HIT, player.getId(), at, Vec3.ZERO, Math.round(fraction * 100.0F),
                ApocalypseShield.State.UP.ordinal());
    }

    // ---- forgetting ----

    /** What the shield remembers of its blows is forgotten (it came down, or the hero is gone). */
    static void forget(UUID hero) {
        MIND_HITS.remove(hero);
        LAST_RIPPLE.remove(hero);
    }

    /** The server has stopped. */
    static void clear() {
        MIND_HITS.clear();
        LAST_RIPPLE.clear();
        STRAINS.clear();
    }

    private ApocalypseGuard() {
    }
}
