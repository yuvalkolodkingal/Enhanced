package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronDrones;
import com.kolodkingal.enhanced.ultron.UltronFx;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.UltronSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ultron, the hero: a vibranium body with the Chitauri anti-gravity technology in his palms, plasma in his fingers and
 * the Sentries as extensions of himself. R fires plasma bolts from a pool that refills (Sneak: the Overcharge, one
 * heavy blast), G flies, V yanks a creature to him (Sneak: slams everything near him down); B and N, the network and
 * the army (Upload, the Hack, Second Life, the Sentry Army, Command), are {@link UltronDrones}.
 *
 * <p>The pool is {@link PowerData#ironCharge} (Magneto's synced, saved number, which a change of power sets to 0): it
 * starts full when the body does, and Creative's is infinite. His body is permanent attribute modifiers, removed from
 * anyone who is not him; what a blow does to him (lightning, no fall damage, no Poison or Wither, Second Life) is in
 * {@code UltronEvents}.
 */
public final class UltronAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("ultron_body_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("ultron_body_armor");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("ultron_body_knockback");
    private static final ResourceLocation CHARGE_SLOW = Enhanced.id("ultron_overcharge_slow");

    /** The speed of a bolt and of the Overcharge's blast, in blocks per tick. */
    public static final double BOLT_SPEED = 2.6;
    public static final double BLAST_SPEED = 2.0;
    /** The most a bolt strays from where he looks, in degrees. */
    public static final float BOLT_SPREAD = 0.6F;
    /** The knockback of the Overcharge's blast. */
    public static final float BLAST_KNOCKBACK = 1.5F;
    /** Ticks without firing before the pool begins to refill. */
    public static final int REGEN_DELAY = 10;
    /**
     * A pulled creature is held to the spot in front of him at most this long, and it is "there" within this far of it.
     */
    public static final int PULL_TICKS = 20;
    public static final double PULL_ARRIVED = 1.0;
    /** The Pull's speed: at most this many blocks a tick, a third of the distance otherwise. */
    public static final double PULL_SPEED = 2.2;
    /** How far in front of him a creature is pulled. */
    public static final double PULL_FRONT = 2.0;
    /** The Slam's push, outward and (for what flies) down, in blocks per tick. */
    public static final double SLAM_PUSH = 1.2;

    /**
     * One hero's plasma: whether R is down, whether the pool ran dry, when he last fired, and the Overcharge's charge.
     */
    private static final class Plasma {
        boolean holding;
        boolean empty;
        long lastFired = Long.MIN_VALUE / 2;
        long nextShot;
        double fraction;
        int hand;
        /** The game time the Overcharge fires, or 0 when he is not charging it. */
        long chargeEnds;
    }

    /** A creature on its way to him. */
    private static final class Pull {
        final UUID hero;
        final UUID target;
        int ticks;

        Pull(UUID hero, UUID target) {
            this.hero = hero;
            this.target = target;
        }
    }

    private static final Map<UUID, Plasma> PLASMA = new HashMap<>();
    private static final List<Pull> PULLS = new ArrayList<>();

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his, with its Sneak variant said explicitly ({@code alt}): the Sneak of Ultron himself, or the binding of a
     * custom hero with his abilities (3.4).
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        // Letting go of R must always get through, so that a stream can't get stuck on.
        if (!down) {
            if (ability == 1) {
                release(player, data);
            }
            return;
        }
        // Held spellbound, or a spectator: the hands do nothing.
        if (player.isSpectator() || player.hasEffect(ModEffects.SPELLBOUND)) {
            return;
        }
        boolean sneak = alt;
        switch (ability) {
            case 1 -> {
                if (sneak) {
                    overcharge(player, data);
                } else {
                    press(player, data);
                }
            }
            case 2 -> PowerTicker.toggleFlight(player);
            case 3 -> {
                if (sneak) {
                    slam(player, data);
                } else {
                    pull(player, data);
                }
            }
            case 4 -> {
                if (sneak) {
                    UltronDrones.hack(player, data);
                } else {
                    UltronDrones.upload(player, data);
                }
            }
            case 5 -> {
                if (sneak) {
                    UltronDrones.command(player);
                } else {
                    UltronDrones.army(player, data);
                }
            }
            default -> {
            }
        }
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        // (3.4: each part for the ability it belongs to: a custom hero ticks here with any of Ultron's abilities.)
        if (data.has(HeroAbility.ULTRON_BODY)) {
            // A machine: never hungry, never out of breath, and no poison runs in it.
            if (player.getFoodData().getFoodLevel() < 20) {
                player.getFoodData().setFoodLevel(20);
            }
            player.setAirSupply(player.getMaxAirSupply());
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WITHER);
        }
        if (data.has(HeroAbility.ULTRON_REPULSOR)) {
            plasmaTick(player, data);
        }
        if (data.has(HeroAbility.ULTRON_GRAVITY)) {
            pulls(player, player.serverLevel());
        }
        if (data.has(HeroAbility.ULTRON_UPLOAD)) {
            UltronDrones.tickHack(player, data);
        }
    }

    /** The plasma's step: the pool refills, R held fires, the Overcharge builds and flies. */
    private static void plasmaTick(ServerPlayer player, PowerData data) {
        Plasma plasma = plasma(player, data);
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        boolean stopped = player.isSpectator() || player.hasEffect(ModEffects.SPELLBOUND);
        if (stopped) {
            plasma.holding = false;
        }
        // The charge is his only while the state says so (a death or a lost hold clears the state, not this map).
        if (plasma.chargeEnds != 0 && (stopped || !data.is(Live.ULTRON_CHARGING))) {
            plasma.chargeEnds = 0;
            data.set(Live.ULTRON_CHARGING, false);
        }
        refill(player, data, plasma, now);
        if (plasma.chargeEnds != 0) {
            // (The plasma goes into the charge: the pool does not refill while it builds.)
            plasma.lastFired = now;
            if (now >= plasma.chargeEnds) {
                fireBlast(player, data, plasma, now);
            }
        } else {
            stream(player, data, plasma, now);
        }
    }

    /**
     * The hero's body, while the power is Ultron's: more health, armour and weight, and slow while the Overcharge
     * builds.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        // (3.4: and a custom hero with his body, its amounts times the strength's factor; the Overcharge's slowness with it.)
        boolean ultron = data.has(HeroAbility.ULTRON_BODY);
        double scale = data.bodyScale(HeroAbility.ULTRON_BODY);
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, UltronConfig.BODY_HEALTH.get() * scale, ultron);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, UltronConfig.BODY_ARMOR.get() * scale, ultron);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK,
                Math.min(1.0, UltronConfig.KNOCKBACK_RESISTANCE.get() * scale), ultron);
        boolean charging = data.has(HeroAbility.ULTRON_REPULSOR) && data.is(Live.ULTRON_CHARGING);
        double slow = UltronConfig.OVERCHARGE_SLOW.get();
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, CHARGE_SLOW, -slow, charging, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        Abilities abilities = player.getAbilities();
        if (charging && abilities.flying && !player.isCreative() && !player.isSpectator()) {
            // In the air it is his flying speed that is slowed (the passives put it back as soon as the charge ends).
            float speed = data.power() == Power.CUSTOM ? CustomHeroes.flightSpeed(player) : Power.ULTRON.flightSpeed();
            float slowed = (float) (speed * (1.0 - slow));
            if (Math.abs(abilities.getFlyingSpeed() - slowed) > 1.0E-4F) {
                abilities.setFlyingSpeed(slowed);
                player.onUpdateAbilities();
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** The key of a held ability is let go of, or the body it belonged to is dead: the stream and the charge stop. */
    static void stopHold(PowerData data) {
        if (data.has(HeroAbility.ULTRON_REPULSOR)) {
            data.set(Live.ULTRON_CHARGING, false);
            data.set(Live.CASTING, false);
        }
    }

    /** The power is taken away: nothing of his is left in the world. */
    static void endAll(ServerPlayer player, PowerData data) {
        gone(player, data);
    }

    /** He died, left, or changed dimension: his army falls apart and what he had under way is forgotten. */
    static void gone(ServerPlayer player, PowerData data) {
        forget(player.getUUID());
        UltronDrones.forget(player.getUUID());
        UltronDrones.dissolveAll(player.server, player.getUUID());
        data.setBeamActive(false);
        stopHold(data);
    }

    /** The server stopped: every static is emptied. */
    static void clear() {
        PLASMA.clear();
        PULLS.clear();
        UltronDrones.clear();
    }

    private static void forget(UUID hero) {
        PLASMA.remove(hero);
        PULLS.removeIf(pull -> pull.hero.equals(hero));
    }

    // ---- ability 1: Repulsor Fire ----

    private static Plasma plasma(ServerPlayer player, PowerData data) {
        Plasma plasma = PLASMA.get(player.getUUID());
        if (plasma == null) {
            plasma = new Plasma();
            PLASMA.put(player.getUUID(), plasma);
            data.setIronCharge(UltronConfig.BOLT_POOL.get());
        }
        return plasma;
    }

    /** R went down: fire for as long as it is held, if there is plasma and he is not charging. */
    private static void press(ServerPlayer player, PowerData data) {
        Plasma plasma = plasma(player, data);
        if (plasma.chargeEnds != 0) {
            return;
        }
        if (!hasPlasma(player, data, plasma)) {
            player.displayClientMessage(Component.translatable("message.enhanced.ultron.pool_empty"), true);
            return;
        }
        plasma.holding = true;
        data.setBeamActive(true);
    }

    private static void release(ServerPlayer player, PowerData data) {
        Plasma plasma = PLASMA.get(player.getUUID());
        if (plasma != null) {
            plasma.holding = false;
        }
        data.setBeamActive(false);
    }

    /**
     * Whether there is plasma to fire: any in the pool, once it has refilled past {@code boltRestart} after running
     * dry.
     */
    private static boolean hasPlasma(ServerPlayer player, PowerData data, Plasma plasma) {
        return player.getAbilities().instabuild || (!plasma.empty && data.ironCharge() > 0);
    }

    /** The pool refills 4 a second after half a second without firing; Creative's is always full. */
    private static void refill(ServerPlayer player, PowerData data, Plasma plasma, long now) {
        int max = UltronConfig.BOLT_POOL.get();
        if (player.getAbilities().instabuild) {
            data.setIronCharge(max);
            plasma.empty = false;
            return;
        }
        if (data.ironCharge() > max) {
            data.setIronCharge(max);
        }
        if (now - plasma.lastFired >= REGEN_DELAY && data.ironCharge() < max) {
            plasma.fraction += UltronConfig.BOLT_REGEN_PER_SECOND.get() / 20.0;
            int whole = (int) plasma.fraction;
            if (whole > 0) {
                plasma.fraction -= whole;
                data.setIronCharge(Math.min(max, data.ironCharge() + whole));
            }
        }
        if (plasma.empty && data.ironCharge() >= UltronConfig.BOLT_RESTART.get()) {
            plasma.empty = false;
        }
    }

    /**
     * While R is held: one bolt every {@code boltInterval} ticks, from alternate hands, a charge each, until the pool
     * is dry.
     */
    private static void stream(ServerPlayer player, PowerData data, Plasma plasma, long now) {
        if (plasma.holding && hasPlasma(player, data, plasma) && now >= plasma.nextShot) {
            fireBolt(player, plasma);
            plasma.lastFired = now;
            plasma.nextShot = now + UltronConfig.BOLT_INTERVAL.get();
            if (!player.getAbilities().instabuild) {
                data.setIronCharge(Math.max(0, data.ironCharge() - 1));
                if (data.ironCharge() == 0) {
                    plasma.empty = true;
                    player.displayClientMessage(Component.translatable("message.enhanced.ultron.pool_empty"), true);
                }
            }
        }
        data.setBeamActive(plasma.holding && hasPlasma(player, data, plasma));
    }

    private static void fireBolt(ServerPlayer player, Plasma plasma) {
        ServerLevel level = player.serverLevel();
        Vec3 direction = Vec3.directionFromRotation(player.getXRot() + spread(player), player.getYRot() + spread(player));
        Vec3 from = fingertip(player, plasma.hand++ % 2 == 0 ? 1.0 : -1.0);
        UltronBoltEntity bolt = UltronBoltEntity.create(level, player, from, direction.scale(BOLT_SPEED),
                UltronConfig.BOLT_DAMAGE.get().floatValue(), palette(player)).tint(AbilityColors.chosen(player,
                        HeroAbility.ULTRON_REPULSOR));
        float splash = UltronConfig.BOLT_SPLASH.get().floatValue();
        if (splash > 0.0F) {
            bolt.splash(splash);
        }
        level.addFreshEntity(bolt);
        UltronSounds.BOLT.play(level, from, SoundSource.PLAYERS, 0.6F, 0.95F + player.getRandom().nextFloat() * 0.1F);
    }

    /** A random turn of up to {@link #BOLT_SPREAD} degrees either way. */
    private static float spread(ServerPlayer player) {
        return (player.getRandom().nextFloat() * 2.0F - 1.0F) * BOLT_SPREAD;
    }

    /**
     * Where a bolt leaves the hand: in front of the eye and to the side (right hand for {@code side} 1, left for -1),
     * unless a wall is in the way, then at the eye.
     */
    private static Vec3 fingertip(ServerPlayer player, double side) {
        Vec3 eye = player.getEyePosition();
        double yaw = Math.toRadians(player.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3 from = eye.add(player.getLookAngle().scale(0.7)).add(right.scale(0.35 * side)).add(0.0, -0.4, 0.0);
        HitResult wall = player.level().clip(new ClipContext(eye, from, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return wall.getType() == HitResult.Type.MISS ? from : eye;
    }

    /** The plasma of the hero's look: Prime's amber, the Sentry's blue, the human Ultron's red. */
    public static UltronBoltEntity.Palette palette(Player player) {
        return switch (Powers.variant(player).id()) {
            case "sentry" -> UltronBoltEntity.Palette.SENTRY;
            case "human" -> UltronBoltEntity.Palette.HUMAN;
            default -> UltronBoltEntity.Palette.PRIME;
        };
    }

    // ---- Sneak + R: the Overcharge ----

    /**
     * Sneak + R: with {@code overchargeCost} charges in the pool and the ability ready, a palm rises and builds for
     * {@code overchargeChargeTicks} (he is slow and cannot fire R meanwhile; a blow does not break it), and then one
     * heavy blast leaves it. The cost is taken at once, the cooldown starts when the blast flies.
     */
    private static void overcharge(ServerPlayer player, PowerData data) {
        Plasma plasma = plasma(player, data);
        long now = player.serverLevel().getGameTime();
        if (plasma.chargeEnds != 0 || !data.isReady(HeroAbility.ULTRON_REPULSOR, now)) {
            return;
        }
        boolean creative = player.getAbilities().instabuild;
        int cost = UltronConfig.OVERCHARGE_COST.get();
        if (!creative && data.ironCharge() < cost) {
            player.displayClientMessage(Component.translatable("message.enhanced.ultron.pool_empty"), true);
            return;
        }
        if (!creative) {
            data.setIronCharge(data.ironCharge() - cost);
        }
        plasma.holding = false;
        plasma.lastFired = now;
        plasma.chargeEnds = now + UltronConfig.OVERCHARGE_CHARGE_TICKS.get();
        data.setBeamActive(false);
        data.set(Live.ULTRON_CHARGING, true);
        UltronSounds.OVERCHARGE.play(player.serverLevel(), player.position(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void fireBlast(ServerPlayer player, PowerData data, Plasma plasma, long now) {
        ServerLevel level = player.serverLevel();
        plasma.chargeEnds = 0;
        plasma.lastFired = now;
        data.set(Live.ULTRON_CHARGING, false);
        data.startCooldown(HeroAbility.ULTRON_REPULSOR, now, UltronConfig.OVERCHARGE_COOLDOWN.get());
        Vec3 look = player.getLookAngle();
        Vec3 from = fingertip(player, 1.0);
        UltronBoltEntity blast = UltronBoltEntity.create(level, player, from, look.scale(BLAST_SPEED), UltronConfig.OVERCHARGE_DAMAGE.get().floatValue(),
                palette(player)).tint(AbilityColors.chosen(player, HeroAbility.ULTRON_OVERCHARGE));
        blast.heavy(UltronConfig.OVERCHARGE_RADIUS.get().floatValue(), UltronConfig.OVERCHARGE_SPLASH.get().floatValue(), BLAST_KNOCKBACK);
        level.addFreshEntity(blast);
        UltronFx.from(level, UltronFxPayload.OVERCHARGE_FLASH, player, from, look, 2.0F, 6);
    }

    // ---- ability 3: Gravity Pull ----

    /**
     * V: the first creature, dropped item or flying projectile that is not his within {@code pullRange} of where he
     * looks is yanked to two blocks in front of him (an item to his feet), takes {@code pullDamage} and Slowness II
     * when it gets there; a projectile is caught and falls. Nothing to pull, nothing happens.
     */
    private static void pull(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.ULTRON_GRAVITY, now)) {
            return;
        }
        double range = UltronConfig.PULL_RANGE.get();
        EntityHitResult hit = Raycasts.entity(player, range, 0.3, e -> canPull(player, e), 1.0F);
        if (hit == null) {
            // Small and fast things (an arrow, a dropped item) need a wider net.
            hit = Raycasts.entity(player, range, 1.0, e -> (e instanceof Projectile || e instanceof ItemEntity) && canPull(player, e), 1.0F);
        }
        if (hit == null) {
            return;
        }
        Entity target = hit.getEntity();
        if (Shelters.stops(player, player.getEyePosition(), target, 0.0F)) {
            return; // 3.3: Billy's Hex Barrier faces him (a Hex's wall was already in the way of his aim)
        }
        data.startCooldown(HeroAbility.ULTRON_GRAVITY, now, UltronConfig.PULL_COOLDOWN.get());
        if (target instanceof Projectile projectile) {
            SlowMotion.unslow(projectile);
        }
        PULLS.add(new Pull(player.getUUID(), target.getUUID()));
        Vec3 palm = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0.0, -0.3, 0.0);
        Vec3 at = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        UltronFx.from(level, UltronFxPayload.GRAVITY_PULL, player, palm, at, 0.0F, 8);
        UltronSounds.GRAVITY.play(level, player.position(), SoundSource.PLAYERS, 0.8F, 1.0F);
        level.sendParticles(new EnergyParticleOptions(0x9FD8FF, 1.0F), at.x, at.y, at.z, 10, 0.2, 0.3, 0.2, 0.05);
    }

    /**
     * What he can pull: anything alive that is not one of his drones, a creature his pull passes by ({@code
     * pull_immune}: what no telekinesis lifts, and the bosses), someone Mjolnir pins or Wanda holds, a Creative or
     * Spectator player, or (with PvP off) a player; a dropped item (but not Mjolnir, which no power lifts); a projectile
     * in flight that is not his.
     */
    private static boolean canPull(ServerPlayer hero, Entity e) {
        if (e == hero || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.getType().is(ModTags.PULL_IMMUNE)
                || Telekinesis.isHeld(e) || MjolnirPin.isPinned(e)) {
            return false;
        }
        if (e instanceof UltronSentryEntity sentry && hero.getUUID().equals(sentry.getOwnerUUID())) {
            return false;
        }
        if (e instanceof Player player) {
            return !player.isCreative() && hero.server.isPvpAllowed() && hero.canHarmPlayer(player);
        }
        if (e instanceof ItemEntity item) {
            return !item.getItem().is(ModTags.CANNOT_BE_SNATCHED);
        }
        if (e instanceof Projectile projectile) {
            return inFlight(projectile) && !isHis(hero, projectile);
        }
        return e instanceof LivingEntity;
    }

    private static boolean inFlight(Projectile projectile) {
        if (projectile instanceof AbstractArrow arrow && (arrow.inGround || arrow.isNoPhysics())) {
            return false;
        }
        return !(projectile instanceof FishingHook);
    }

    /** Whether the hero fired it, or a drone of his did. */
    private static boolean isHis(ServerPlayer hero, Projectile projectile) {
        Entity owner = projectile.getOwner();
        return owner == hero || (owner instanceof UltronSentryEntity sentry && hero.getUUID().equals(sentry.getOwnerUUID()));
    }

    /**
     * Each tick of a pull: the creature is sent toward its spot; when it gets there (or the time is up) the pull lands.
     */
    private static void pulls(ServerPlayer hero, ServerLevel level) {
        for (Iterator<Pull> it = PULLS.iterator(); it.hasNext(); ) {
            Pull pull = it.next();
            if (!pull.hero.equals(hero.getUUID())) {
                continue;
            }
            Entity target = level.getEntity(pull.target);
            if (target == null || !target.isAlive() || Shelters.shelters(hero, target)) {
                it.remove(); // (3.3: gone behind a wall: the pull lets go)
                continue;
            }
            Vec3 spot = pullSpot(hero, target);
            Vec3 diff = spot.subtract(target.position());
            boolean arrived = diff.length() <= PULL_ARRIVED || ++pull.ticks >= PULL_TICKS;
            if (arrived) {
                landed(hero, level, target);
                it.remove();
            } else {
                target.setDeltaMovement(diff.normalize().scale(Math.min(PULL_SPEED, diff.length() * 0.35)));
                target.hurtMarked = true;
                target.resetFallDistance();
            }
        }
    }

    /**
     * Where the creature's feet are pulled to: two blocks in front of him, level with his feet; an item goes to his
     * feet.
     */
    private static Vec3 pullSpot(ServerPlayer hero, Entity target) {
        if (target instanceof ItemEntity) {
            return hero.position();
        }
        Vec3 look = hero.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        double lift = target instanceof Projectile ? hero.getEyeHeight() * 0.8 : 0.0;
        return hero.position().add(flat.scale(PULL_FRONT)).add(0.0, lift, 0.0);
    }

    /**
     * It is there: a creature takes the blow and the weight, a projectile stops (and falls), an item lies at his feet.
     */
    private static void landed(ServerPlayer hero, ServerLevel level, Entity target) {
        if (target instanceof LivingEntity living) {
            living.hurt(ModDamageTypes.source(level, ModDamageTypes.ULTRON_SLAM, hero, hero), UltronConfig.PULL_DAMAGE.get().floatValue());
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), hero);
        }
        // (After the blow, which would push it back out: it stands where it was brought.)
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
    }

    // ---- Sneak + V: the Gravity Slam ----

    /**
     * Sneak + V: a ring of anti-gravity runs out from his feet to {@code slamRadius}. Everything in it that he may hurt
     * (the area rule: not his, not Creative or Spectator, not a player with PvP off, not a boss's body) takes
     * {@code slamDamage}, is thrown outward and pressed down, and is slowed for three seconds.
     */
    private static void slam(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.ULTRON_GRAVITY, now)) {
            return;
        }
        data.startCooldown(HeroAbility.ULTRON_GRAVITY, now, UltronConfig.SLAM_COOLDOWN.get());
        double radius = UltronConfig.SLAM_RADIUS.get();
        Vec3 centre = player.position();
        DamageSource source = ModDamageTypes.source(level, ModDamageTypes.ULTRON_SLAM, player, player);
        AABB reach = player.getBoundingBox().inflate(radius);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, reach, e -> canSlam(player, e) && e.position().distanceTo(centre) <= radius)) {
            if (!victim.hurt(source, UltronConfig.SLAM_DAMAGE.get().floatValue()) && Shelters.shelters(player, centre, victim)) {
                continue; // 3.3: Billy's barrier faced the slam and stopped all of it
            }
            Vec3 out = victim.position().subtract(centre).multiply(1.0, 0.0, 1.0);
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(player.getRandom().nextDouble() - 0.5, 0.0, player.getRandom().nextDouble() - 0.5).normalize() : out.normalize();
            // What stands is thrown along the ground; what flies is slammed into it.
            victim.setDeltaMovement(out.x * SLAM_PUSH, victim.onGround() ? 0.0 : -SLAM_PUSH, out.z * SLAM_PUSH);
            victim.hurtMarked = true;
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), player);
        }
        UltronFx.from(level, UltronFxPayload.GRAVITY_SLAM, player, centre, Vec3.ZERO, (float) radius, 10);
        UltronSounds.GRAVITY.play(level, centre, SoundSource.PLAYERS, 1.0F, 0.8F);
        for (int i = 0; i < 24; i++) {
            double angle = Mth.TWO_PI * i / 24.0;
            level.sendParticles(new EnergyParticleOptions(0x9FD8FF, 1.2F), centre.x + Math.cos(angle) * 1.5, centre.y + 0.2, centre.z + Math.sin(angle) * 1.5,
                    0, Math.cos(angle), 0.02, Math.sin(angle), 0.5);
        }
    }

    private static boolean canSlam(ServerPlayer hero, LivingEntity e) {
        return e != hero && e.isAlive() && !e.isSpectator() && !e.getType().is(ModTags.PULL_IMMUNE) && AreaTargets.mayHurt(hero, e, false);
    }

    private UltronAbilities() {
    }
}
