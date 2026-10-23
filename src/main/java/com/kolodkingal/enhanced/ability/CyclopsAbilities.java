package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.CyclopsConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.item.RubyQuartzGlassesItem;
import com.kolodkingal.enhanced.network.CyclopsFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cyclops, Scott Summers (3.3): a mutant whose eyes fire a beam of pure force that only ruby quartz can hold back. A player
 * with no power becomes him by putting on his ruby-quartz glasses ({@link RubyQuartzGlassesItem}); what happens when they come
 * off is {@link CyclopsGlasses}. The plug-in is {@link CyclopsServer}; his fall-damage rule is in {@code CyclopsEvents}.
 *
 * <p>R held is the Optic Blast (a continuous beam on the shared {@link ContinuousBeam}, from his eyes along his look; each hit
 * shoves what it hits), Sneak + R the Ricochet Shot (a bolt that bounces off blocks), G the Concussion Blast (short and wide),
 * V the Optic Leap (a blast at the ground that throws him up and forward), B the Rally ("To me, X-Men!") and N the glasses
 * (Sneak + N takes them off, N puts them back on). The blasts of his eyes need the glasses on: without them he cannot aim.
 * He is a mutant, not a telepath, and he does not fly.
 */
public final class CyclopsAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("cyclops_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("cyclops_armor");

    /** The cooldown slots: R (the Ricochet; the Optic Blast has none), G, V, B, and N (the glasses' gesture, see {@link CyclopsGlasses}). */
    public static final int RICOCHET_SLOT = 1;
    public static final int CONCUSSION_SLOT = 2;
    public static final int LEAP_SLOT = 3;
    public static final int RALLY_SLOT = 4;
    public static final int GLASSES_SLOT = 5;

    /** The ruby red of his blasts (particles), and their hot core. */
    public static final int RED = 0xFF3B46;
    public static final int CORE = 0xFFD6CF;
    /** How far the effects of his blasts are sent. */
    private static final double FX_RANGE = 96.0;
    /** The Leap needs ground this close below his feet to blast against. */
    public static final double LEAP_GROUND_REACH = 4.0;

    /** What each Cyclops carries from tick to tick; forgotten when he leaves or the server stops. */
    private static final Map<UUID, State> STATES = new HashMap<>();

    static final class State {
        /** Whether he wore the glasses at the last look ({@link CyclopsGlasses}); null before the first. */
        @Nullable
        Boolean wore;
        /** In the air after an Optic Leap: no fall damage until he lands. */
        boolean leaping;
        /** When the Leap began (game time). */
        long leapStart;
        /** When the giant beam last struck everything along it (game time). */
        long lastSweep = Long.MIN_VALUE / 2;
    }

    static State state(UUID id) {
        return STATES.computeIfAbsent(id, k -> new State());
    }

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his, with its Sneak variant said explicitly ({@code alt}): the Sneak of Cyclops himself, or the binding of a
     * custom hero with his abilities (3.4).
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down) {
            if (ability == 1 && opticFiring(player)) {
                ContinuousBeam.stop(player);
            }
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    ricochet(player, data);
                } else {
                    opticStart(player, data);
                }
            }
            case 2 -> concussion(player, data);
            case 3 -> leap(player, data);
            case 4 -> rally(player, data);
            case 5 -> {
                if (alt) {
                    CyclopsGlasses.takeOff(player, data);
                } else {
                    CyclopsGlasses.putOn(player, data);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Whether the blasts of this player's eyes need the ruby-quartz glasses: Cyclops's do; a custom hero's with his abilities
     * (3.4) never do, he aims his eyes as he likes, and his "glasses" key opens and shuts them ({@link CyclopsGlasses}).
     */
    public static boolean needsGlasses(PowerData data) {
        return data.power() == Power.CYCLOPS;
    }

    /**
     * Whether his eyes can be aimed now: the glasses are on and nothing is pouring out. Otherwise he is told why, and the
     * blast that needs them does nothing.
     */
    private static boolean canAim(ServerPlayer player, PowerData data) {
        if (data.is(Live.CYCLOPS_UNLEASHED)) {
            player.displayClientMessage(Component.translatable(needsGlasses(data) ? "message.enhanced.cyclops.unleashed_busy"
                    : "message.enhanced.cyclops.eyes_open_busy"), true);
            return false;
        }
        if (needsGlasses(data) && !CyclopsGlasses.wears(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.cyclops.no_glasses"), true);
            return false;
        }
        return true;
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        // (3.4: each part for the ability it belongs to: a custom hero ticks here with any of Cyclops's abilities.)
        if (needsGlasses(data)) {
            CyclopsGlasses.tick(player, data);
        }
        if (data.is(Live.CYCLOPS_UNLEASHED) && data.has(HeroAbility.CYCLOPS_GLASSES)) {
            CyclopsGlasses.unleashedTick(player, data);
        } else if (data.has(HeroAbility.CYCLOPS_OPTIC_BLAST)
                && (opticFiring(player) || (data.is(Live.BEAMING) && ContinuousBeam.isFiring(player)))) {
            opticTick(player, data);
        }
        State state = STATES.get(player.getUUID());
        if (state != null && state.leaping) {
            player.resetFallDistance();
            if ((player.onGround() || player.isInWater() || player.getAbilities().flying)
                    && player.level().getGameTime() - state.leapStart > 3) {
                state.leaping = false;
            }
        }
    }

    /** For every player: whoever puts the glasses on with no power becomes Cyclops. */
    static void everyPlayerTick(ServerPlayer player, PowerData data) {
        if (data.power() == Power.NONE && CyclopsGlasses.wears(player) && player.isAlive() && !player.isSpectator()) {
            Powers.awaken(player, Power.CYCLOPS);
            state(player.getUUID()).wore = true;
        }
    }

    // ---- the body ----

    /**
     * The body of a hero who trains for war (30 health, the armour of his field suit) while the player is Cyclops, and nothing
     * of it on anyone else. The modifiers are permanent, so they are already right when a player logs in.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        // (3.4: and a custom hero with his body, its amounts times the strength's factor.)
        boolean hero = data.has(HeroAbility.CYCLOPS_BODY);
        double scale = data.bodyScale(HeroAbility.CYCLOPS_BODY);
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, hero ? CyclopsConfig.BODY_HEALTH_BONUS.get() * scale : 0.0, hero);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, hero ? CyclopsConfig.ARMOR.get() * scale : 0.0, hero);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ---- Ability 1: the Optic Blast (R held) ----

    /** Whether his controlled optic blast burns (not the giant beam). */
    public static boolean opticFiring(ServerPlayer player) {
        return ContinuousBeam.isFiring(player) && !Powers.get(player).is(Live.CYCLOPS_UNLEASHED);
    }

    /** With an empty hunger bar there is no strength left to hold the blast (not in Creative). */
    private static boolean starving(Player player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    /** The numbers of a tick of his optic blast, from the config. */
    public static BeamSpec opticSpec() {
        double push = CyclopsConfig.OPTIC_PUSH.get();
        return new BeamSpec(RubyQuartzGlassesItem.OPTIC, CyclopsConfig.OPTIC_RANGE.get(), CyclopsConfig.OPTIC_DAMAGE.get().floatValue(),
                CyclopsConfig.OPTIC_INTERVAL.get(), CyclopsConfig.OPTIC_EXHAUSTION_PER_SECOND.get().floatValue() / 20.0F,
                ModDamageTypes.OPTIC_BLAST, (shooter, target) -> shove(shooter, target, shooter.getLookAngle(), push, 0.05));
    }

    /** R down: a hand to the visor, and the red beam leaves his eyes (if he wears the glasses and is fed). */
    private static void opticStart(ServerPlayer player, PowerData data) {
        if (ContinuousBeam.isFiring(player) || !canAim(player, data)) {
            return;
        }
        if (starving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.cyclops.hungry"), true);
            return;
        }
        ContinuousBeam.start(player, RubyQuartzGlassesItem.OPTIC, CyclopsConfig.OPTIC_RANGE.get());
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.CYCLOPS_OPTIC_START.get(), SoundSource.PLAYERS,
                0.9F, 1.0F);
    }

    /** One tick of a held optic blast: it ends when he can no longer hold it (hunger, the glasses gone), otherwise it burns. */
    private static void opticTick(ServerPlayer player, PowerData data) {
        if (ContinuousBeam.isFiring(player) && !data.is(Live.BEAMING)) {
            ContinuousBeam.stop(player); // (something that lets go of every held ability took the flag away)
            return;
        }
        if (!ContinuousBeam.isFiring(player)) {
            data.set(Live.BEAMING, false); // (the flag outlived the beam: a server that restarted, a state cleared)
            return;
        }
        if (player.isSpectator() || starving(player) || needsGlasses(data) && !CyclopsGlasses.wears(player)) {
            if (starving(player)) {
                player.displayClientMessage(Component.translatable("message.enhanced.cyclops.hungry"), true);
            }
            ContinuousBeam.stop(player);
            return;
        }
        ContinuousBeam.tick(player, opticSpec());
    }

    /**
     * Pushes what a blast hit along {@code direction} (his blasts are force: they shove), the parent of a part of the Ender
     * Dragon in its stead. Bosses and creatures that resist knockback resist it as they resist a blow.
     */
    static void shove(ServerPlayer shooter, Entity target, Vec3 direction, double strength, double lift) {
        Entity body = target instanceof PartEntity<?> part ? part.getParent() : target;
        if (strength <= 0.0 || body == shooter || !body.isAlive()) {
            return;
        }
        double resist = body instanceof LivingEntity living ? living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0;
        double k = strength * Math.max(0.0, 1.0 - resist);
        if (k <= 0.0) {
            return;
        }
        Vec3 d = direction.lengthSqr() < 1.0E-8 ? Vec3.ZERO : direction.normalize();
        body.setDeltaMovement(body.getDeltaMovement().add(d.x * k, d.y * k * 0.5 + lift, d.z * k));
        body.hurtMarked = true;
        body.hasImpulse = true;
    }

    // ---- Ability 1, Sneak: the Ricochet Shot ----

    /**
     * Sneak + R: one bolt from his eyes along the look that bounces off the blocks it strikes, {@code ricochetBounces} times,
     * each stretch up to {@code ricochetRange} long: every creature it passes through is hit once (as his team and pets are
     * spared, and players only with PvP on), and a complete Chaos Shield stops it. Everyone near sees its path.
     *
     * @return the creatures it hit, in order
     */
    public static List<LivingEntity> ricochet(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.CYCLOPS_OPTIC_BLAST, now) || !canAim(player, data)) {
            return List.of();
        }
        List<Vec3> path = new ArrayList<>();
        List<LivingEntity> hit = new ArrayList<>();
        Vec3 from = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        path.add(from);
        Set<Entity> struck = new HashSet<>();
        float damage = CyclopsConfig.RICOCHET_DAMAGE.get().floatValue();
        double range = CyclopsConfig.RICOCHET_RANGE.get();
        int bounces = CyclopsConfig.RICOCHET_BOUNCES.get();
        for (int stretch = 0; stretch <= bounces; stretch++) {
            Vec3 to = from.add(dir.scale(range));
            BlockHitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
            WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, from, end, 1.0F);
            if (shield != null) {
                end = shield.point();
            }
            for (LivingEntity victim : along(level, player, from, end, 0.3)) {
                if (struck.add(victim) && AreaTargets.mayHurt(player, victim, true)) {
                    if (victim.hurt(ModDamageTypes.source(level, ModDamageTypes.OPTIC_BLAST, player, player), damage)) {
                        shove(player, victim, dir, 0.45, 0.1);
                    }
                    hit.add(victim);
                }
            }
            path.add(end);
            if (shield != null) {
                WandaAbilities.pay(shield, player, damage); // (3.3: a Chaos Shield or a Hex Barrier pays; a Hex or a ward stops it free)
                break;
            }
            if (block.getType() == HitResult.Type.MISS || path.size() >= CyclopsFxPayload.MAX_POINTS) {
                break;
            }
            // The bolt glances off the face it struck: the part of its way across the face's normal turns round.
            Vec3 normal = Vec3.atLowerCornerOf(block.getDirection().getNormal());
            dir = dir.subtract(normal.scale(2.0 * dir.dot(normal))).normalize();
            from = end.add(normal.scale(0.02));
            level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.CYCLOPS_RICOCHET,
                    RED), 1.0F), end.x, end.y, end.z, 6,
                    0.08, 0.08, 0.08, 0.12);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 3, 0.1, 0.1, 0.1, 0.2);
        }
        data.startCooldown(HeroAbility.CYCLOPS_OPTIC_BLAST, now, CyclopsConfig.RICOCHET_COOLDOWN.get());
        player.causeFoodExhaustion(CyclopsConfig.RICOCHET_EXHAUSTION.get().floatValue());
        send(player, CyclopsFxPayload.RICOCHET, path, hit.size());
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.CYCLOPS_RICOCHET.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return hit;
    }

    /**
     * The creatures (what a beam can hit: {@link ContinuousBeam#targets}, the bodies of the dragon's parts in their stead)
     * whose box, grown by {@code inflate}, the segment from {@code from} to {@code to} crosses, nearest first; never him.
     */
    static List<LivingEntity> along(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 to, double inflate) {
        List<LivingEntity> found = new ArrayList<>();
        List<Double> at = new ArrayList<>();
        AABB search = new AABB(from, to).inflate(inflate + 1.0);
        for (Entity e : level.getEntities(player, search, ContinuousBeam.targets(player))) {
            Entity body = e instanceof PartEntity<?> part ? part.getParent() : e;
            if (!(body instanceof LivingEntity living) || living == player || found.contains(living)) {
                continue;
            }
            AABB box = e.getBoundingBox().inflate(e.getPickRadius() + inflate);
            double dist;
            if (box.contains(from)) {
                dist = 0.0;
            } else {
                var clip = box.clip(from, to);
                if (clip.isEmpty()) {
                    continue;
                }
                dist = clip.get().distanceToSqr(from);
            }
            int i = 0;
            while (i < at.size() && at.get(i) <= dist) {
                i++;
            }
            found.add(i, living);
            at.add(i, dist);
        }
        return found;
    }

    // ---- Ability 2: the Concussion Blast ----

    /**
     * G: a short, wide blast from his eyes: every creature in a cone of {@code concussionAngle} round his look, within
     * {@code concussionRange} and in sight, takes {@code concussionDamage} and is thrown back; the projectiles in it are turned
     * away. Allies are spared (and players with PvP off, and passive creatures).
     *
     * @return how many creatures it hit
     */
    public static int concussion(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.CYCLOPS_CONCUSSION, now) || !canAim(player, data)) {
            return 0;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = CyclopsConfig.CONCUSSION_RANGE.get();
        double cos = Math.cos(Math.toRadians(CyclopsConfig.CONCUSSION_ANGLE.get()));
        float damage = CyclopsConfig.CONCUSSION_DAMAGE.get().floatValue();
        double knockback = CyclopsConfig.CONCUSSION_KNOCKBACK.get();
        int count = 0;
        AABB box = player.getBoundingBox().inflate(range);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            Vec3 to = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
            if (!inCone(eye, look, to, range + victim.getBbWidth() * 0.5, cos) || !inSight(level, player, eye, to)
                    || !AreaTargets.mayHurt(player, victim, false)) {
                continue;
            }
            if (victim.hurt(ModDamageTypes.source(level, ModDamageTypes.OPTIC_BLAST, player, player), damage)) {
                count++;
            }
            shove(player, victim, to.subtract(eye), knockback, 0.35);
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box, p -> p.getOwner() != player)) {
            if (inCone(eye, look, projectile.position(), range, cos)) {
                projectile.setDeltaMovement(projectile.position().subtract(eye).normalize().scale(1.2));
                projectile.hasImpulse = true;
                projectile.hurtMarked = true;
            }
        }
        data.startCooldown(HeroAbility.CYCLOPS_CONCUSSION, now, CyclopsConfig.CONCUSSION_COOLDOWN.get());
        player.causeFoodExhaustion(CyclopsConfig.CONCUSSION_EXHAUSTION.get().floatValue());
        send(player, CyclopsFxPayload.CONCUSSION, List.of(eye, look), (int) Math.round(range * 10.0));
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.CYCLOPS_CONCUSSION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return count;
    }

    /** Whether {@code at} lies within {@code range} of {@code eye} and inside the cone round {@code look} whose cosine is {@code cos}. */
    public static boolean inCone(Vec3 eye, Vec3 look, Vec3 at, double range, double cos) {
        Vec3 to = at.subtract(eye);
        double length = to.length();
        if (length > range) {
            return false;
        }
        return length < 0.5 || to.scale(1.0 / length).dot(look.normalize()) >= cos;
    }

    private static boolean inSight(ServerLevel level, Entity from, Vec3 eye, Vec3 to) {
        return level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)).getType() == HitResult.Type.MISS;
    }

    // ---- Ability 3: the Optic Leap ----

    /**
     * V: a blast of his eyes at the ground under him throws him up ({@code leapUp}) and forward the way he faces
     * ({@code leapForward}); the creatures round the spot it strikes are hurt and thrown out. He takes no fall damage from it
     * (until he lands). Needs ground within {@link #LEAP_GROUND_REACH} blocks below.
     *
     * @return whether he leapt
     */
    public static boolean leap(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.CYCLOPS_LEAP, now) || !canAim(player, data)) {
            return false;
        }
        if (MjolnirPin.isPinned(player)) {
            return false;
        }
        Vec3 feet = player.position();
        BlockHitResult ground = level.clip(new ClipContext(feet.add(0, 0.1, 0), feet.subtract(0, LEAP_GROUND_REACH, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        if (ground.getType() == HitResult.Type.MISS) {
            player.displayClientMessage(Component.translatable("message.enhanced.cyclops.no_ground"), true);
            return false;
        }
        Vec3 spot = ground.getLocation();
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.getYRot());
        double forward = CyclopsConfig.LEAP_FORWARD.get();
        player.setDeltaMovement(facing.x * forward, CyclopsConfig.LEAP_UP.get(), facing.z * forward);
        player.hurtMarked = true;
        player.resetFallDistance();
        State state = state(player.getUUID());
        state.leaping = true;
        state.leapStart = now;

        double radius = CyclopsConfig.LEAP_BLAST_RADIUS.get();
        float damage = CyclopsConfig.LEAP_BLAST_DAMAGE.get().floatValue();
        if (radius > 0.0) {
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(spot, spot).inflate(radius, 1.5, radius),
                    e -> e != player && e.isAlive())) {
                if (victim.position().distanceTo(spot) > radius + victim.getBbWidth() * 0.5 || !AreaTargets.mayHurt(player, victim, false)) {
                    continue;
                }
                victim.hurt(ModDamageTypes.source(level, ModDamageTypes.OPTIC_BLAST, player, player), damage);
                shove(player, victim, victim.position().subtract(spot), 0.8, 0.35);
            }
        }
        data.startCooldown(HeroAbility.CYCLOPS_LEAP, now, CyclopsConfig.LEAP_COOLDOWN.get());
        player.causeFoodExhaustion(CyclopsConfig.LEAP_EXHAUSTION.get().floatValue());
        send(player, CyclopsFxPayload.LEAP, List.of(spot, player.getEyePosition()), (int) Math.round(radius * 10.0));
        level.playSound(null, spot.x, spot.y, spot.z, ModSounds.CYCLOPS_LEAP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** Whether he is in the air after an Optic Leap (no fall damage until he lands). */
    public static boolean leaping(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.leaping;
    }

    // ---- Ability 4: the Rally ----

    /** Whom his call reaches: players, tamed pets and Iron Golems (his own side; never a hostile creature). */
    private static boolean rallies(LivingEntity target) {
        return target instanceof Player p ? !p.isSpectator() : target instanceof IronGolem || target instanceof OwnableEntity pet && pet.getOwnerUUID() != null;
    }

    /**
     * B: "To me, X-Men!" He and every ally within {@code rallyRadius} (players, tamed pets, Iron Golems) get Speed, Strength
     * and Resistance for {@code rallyTicks}. Works with the glasses on or off (it is his voice, not his eyes).
     *
     * @return how many were rallied (him included)
     */
    public static int rally(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.CYCLOPS_RALLY, now)) {
            return 0;
        }
        int radius = CyclopsConfig.RALLY_RADIUS.get();
        int ticks = CyclopsConfig.RALLY_TICKS.get();
        int count = 0;
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e.isAlive() && (e == player || rallies(e)))) {
            if (ally.distanceTo(player) > radius) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0, false, true, true), player);
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0, false, true, true), player);
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, false, true, true), player);
            count++;
        }
        data.startCooldown(HeroAbility.CYCLOPS_RALLY, now, CyclopsConfig.RALLY_COOLDOWN.get());
        player.causeFoodExhaustion(CyclopsConfig.RALLY_EXHAUSTION.get().floatValue());
        player.displayClientMessage(Component.translatable("message.enhanced.cyclops.rally", count - 1), true);
        send(player, CyclopsFxPayload.RALLY, List.of(player.position()), radius);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CYCLOPS_RALLY.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        return count;
    }

    // ---- what everyone near sees ----

    static void send(ServerPlayer player, byte kind, List<Vec3> points, int param) {
        Net.sendToPlayersNear(player.serverLevel(), null, player.getX(), player.getY(), player.getZ(), FX_RANGE,
                new CyclopsFxPayload(kind, player.getId(), List.copyOf(points), param));
    }

    // ---- the end of the power, of the life, of the session ----

    /** The power is being taken away: his beams stop, and the glasses no longer hold anything back. */
    static void endAll(ServerPlayer player, PowerData data) {
        CyclopsGlasses.end(player, data, false);
        if (ContinuousBeam.isFiring(player)) {
            ContinuousBeam.stop(player);
        }
        STATES.remove(player.getUUID());
    }

    /** He died or left: whatever pours from his eyes ends (it never outlives him), and his record goes when he leaves. */
    static void onGone(ServerPlayer player, boolean leaving) {
        PowerData data = Powers.get(player);
        if (data.acts(Power.CYCLOPS)) {
            CyclopsGlasses.end(player, data, false);
            if (ContinuousBeam.isFiring(player) && !leaving) {
                ContinuousBeam.stop(player);
            }
        }
        if (leaving) {
            STATES.remove(player.getUUID());
        } else {
            State state = STATES.get(player.getUUID());
            if (state != null) {
                state.wore = null; // (seen afresh when he is back: no glasses after a death is not a removal)
                state.leaping = false;
            }
        }
    }

    /** Back in the world (a respawn, a login): what he wears now is where the glasses stand, nothing came off. */
    static void onBack(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null) {
            state.wore = null;
        }
    }

    /** The server has stopped: nothing of the hero's is kept. */
    static void clear() {
        STATES.clear();
    }

    private CyclopsAbilities() {
    }
}
