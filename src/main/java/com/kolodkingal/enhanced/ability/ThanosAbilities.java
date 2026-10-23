package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.stones.SoulDrain;
import com.kolodkingal.enhanced.stones.TimeRewind;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.StonePowers;
import com.kolodkingal.enhanced.thanos.ThanosEffects;
import com.kolodkingal.enhanced.thanos.ThanosSounds;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Thanos, the hero: the five stones the Mad Titan held, one on each key, made playable for one player (DESIGN section
 * 15.4). R is the Power Stone (the Power Blast; Sneak: the Titan Slam), G the Space Stone (the Portal Step; Sneak: the
 * Space Pull), V the Reality Stone (Transmute a creature to stone; Sneak: the Reality Veil), B the Soul Stone (hold:
 * the Soul Drain; Sneak: the Soul Sight) and N the Time Stone (the Rewind; Sneak: the Time Lock). He does not fly, he
 * has no sixth stone and no Snap. His body is a Titan's: more health, armour and reach, a little taller, and a healing
 * factor.
 *
 * <p>What the stones do in the world is {@link StonePowers}, the code the boss shares; this class is the player's side
 * of it: the keys, the cooldowns and the hunger, the refusals ("No target", "PvP is off"), and what lasts from tick to
 * tick (a ring that spreads, a chain, a dome, the history the Rewind goes back into). Every number is {@link
 * ThanosConfig}'s. Nothing here changes a block.
 *
 * <p>An ability that cannot do anything (no target, no room, a wall in the way) says why and starts no cooldown. While
 * he is held (Billy's spell, stone, the Soul World) no stone answers: the film's flaw, that a restrained hand cannot
 * snap. The server-side state is per Thanos and dies with his power, his life, his session and his world.
 */
public final class ThanosAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("thanos_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("thanos_armor");
    private static final ResourceLocation BODY_TOUGHNESS = Enhanced.id("thanos_toughness");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("thanos_knockback");
    private static final ResourceLocation BODY_ATTACK = Enhanced.id("thanos_attack");
    private static final ResourceLocation BODY_SCALE = Enhanced.id("thanos_scale");
    private static final ResourceLocation BODY_REACH = Enhanced.id("thanos_reach");
    private static final ResourceLocation BODY_STEP = Enhanced.id("thanos_step");
    private static final ResourceLocation BODY_FALL = Enhanced.id("thanos_fall");

    /** How long the sphere of the Transmute and the vortex of the Space Pull take to arrive. */
    private static final int FLIGHT_TICKS = 6;
    /** How far the Titan Slam's ring runs each tick (blocks). */
    private static final double SLAM_SPEED = 0.9;
    /** The Time Lock keeps its creatures locked by renewing the effect this often (ticks). */
    private static final int LOCK_RENEW = 4;
    /**
     * How long a creature stays locked after the renewal that last reached it (ticks): a little more than the interval.
     */
    private static final int LOCK_HOLD = 10;
    /** What the Soul Sight makes glow, and for how long (5 seconds). */
    private static final int REVEAL_TICKS = 100;

    /**
     * 3.6: the numbers of his Soul Drain (the drain itself is {@link SoulDrain}'s, shared with the Soul Stone), read
     * when used.
     */
    private static final SoulDrain.Numbers DRAIN = new SoulDrain.Numbers() {
        @Override
        public HeroAbility row() {
            return HeroAbility.THANOS_SOUL;
        }

        @Override
        public double range() {
            return ThanosConfig.DRAIN_RANGE.get();
        }

        @Override
        public double damagePerSecond() {
            return ThanosConfig.DRAIN_DAMAGE_PER_SECOND.get();
        }

        @Override
        public float healFraction() {
            return ThanosConfig.DRAIN_HEAL_FRACTION.get().floatValue();
        }

        @Override
        public float exhaustion() {
            return ThanosConfig.DRAIN_EXHAUSTION.get().floatValue();
        }

        @Override
        public int cooldown() {
            return ThanosConfig.DRAIN_COOLDOWN.get();
        }
    };

    // ---- what lasts from tick to tick (server only, per Thanos) ----

    /** Damage he took, for the healing factor's pause and the chain a hard blow tears. */
    private record Hurt(long time, float amount) {
    }

    /** The Transmute's sphere in flight. */
    private record Sphere(int target, long arrives) {
    }

    /** The Space Pull's vortex drawing a creature in. */
    private record Vortex(int target, long arrives) {
    }

    /** The Titan Slam's ring: where it began, how far it has run, and whom it has already thrown. */
    private static final class Slam {
        final Vec3 centre;
        double radius;
        final IntSet hit = new IntOpenHashSet();

        Slam(Vec3 centre) {
            this.centre = centre;
        }
    }

    /** The Time Lock's dome: where, until when, whom it holds and which projectiles it hangs. */
    private static final class Lock {
        final ResourceKey<Level> dimension;
        final Vec3 centre;
        final long ends;
        final Set<UUID> held = new HashSet<>();
        final IntSet hung = new IntOpenHashSet();

        Lock(ResourceKey<Level> dimension, Vec3 centre, long ends) {
            this.dimension = dimension;
            this.centre = centre;
            this.ends = ends;
        }
    }

    // (3.6: the history the Rewind goes back into is TimeRewind's, the Soul Drain SoulDrain's: the Time and Soul Stones
    // share them.)
    private static final class State {
        final Deque<Hurt> taken = new ArrayDeque<>();
        /** The creatures he turned to stone and the game time each is stone until. */
        final Map<UUID, Long> stoned = new HashMap<>();
        long lastHurt = Long.MIN_VALUE / 2;
        long sightEnds;
        @Nullable Sphere sphere;
        @Nullable Vortex vortex;
        @Nullable Slam slam;
        @Nullable Lock lock;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    @Nullable
    private static State stateOf(Player player) {
        return STATES.get(player.getUUID());
    }

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    // ---- input ----

    /** His own key: Sneak chooses the variant. */
    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of Thanos's, his own or (3.4) a custom hero's who has the stone ({@code HeroKeys.press}): {@code ability} is
     * its key for Thanos, {@code alt} whether it is the Sneak variant.
     */
    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        // Letting go of the Soul Drain always gets through, even for a Thanos who just became a spectator or was held.
        if (ability == 4 && !down) {
            endDrain(player, data);
            return;
        }
        if (!down || player.isSpectator()) {
            return;
        }
        if (held(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.held"), true);
            return;
        }
        boolean sneak = alt;
        switch (ability) {
            case 1 -> {
                if (sneak) {
                    titanSlam(player, data);
                } else {
                    powerBlast(player, data);
                }
            }
            case 2 -> {
                if (sneak) {
                    spacePull(player, data);
                } else {
                    portalStep(player, data);
                }
            }
            case 3 -> {
                if (sneak) {
                    realityVeil(player, data);
                } else {
                    transmute(player, data);
                }
            }
            case 4 -> {
                if (sneak) {
                    soulSight(player, data);
                } else {
                    soulDrain(player, data);
                }
            }
            case 5 -> {
                if (sneak) {
                    timeLock(player, data);
                } else {
                    rewind(player, data);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Whether the stones are held still: a restrained hand cannot use them (Billy's spell, the Reality Stone, the Soul
     * World).
     */
    public static boolean held(LivingEntity thanos) {
        return thanos.hasEffect(ModEffects.SPELLBOUND) || thanos.hasEffect(ModEffects.PETRIFIED) || thanos.hasEffect(ModEffects.SOULWORLD);
    }

    /** With an empty hunger bar there is nothing to fuel the stones (not in Creative). */
    private static boolean starving(Player player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    /** Whether the ability's cooldown is over and, if it costs hunger, he is not starving; says why if not. */
    private static boolean mayStart(ServerPlayer player, PowerData data, HeroAbility ability, long now) {
        if (!data.isReady(ability, now)) {
            return false;
        }
        if (starving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.too_hungry"), true);
            return false;
        }
        return true;
    }

    // ---- the body (passives; runs for every player) ----

    /**
     * A Titan's body: more health, armour and toughness, hard to knock back, strong, a little taller, with long arms
     * and a long stride, and a gentler landing. Applied while the power is his, and taken off whoever is not Thanos. (3.4:
     * a custom hero with his body has it too, its amounts by its strength.)
     */
    public static void applyBody(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.THANOS_BODY)) {
            // (Nearly every player, every tick: one lookup says whether there is anything to take off.)
            if (player.getAttribute(Attributes.MAX_HEALTH).getModifier(BODY_HEALTH) != null) {
                removeBody(player);
            }
            return;
        }
        double k = data.bodyScale(HeroAbility.THANOS_BODY);
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, ThanosConfig.HEALTH_BONUS.get() * k, true);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, ThanosConfig.ARMOR_BONUS.get() * k, true);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS, ThanosConfig.TOUGHNESS_BONUS.get() * k, true);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK,
                Math.min(1.0, ThanosConfig.KNOCKBACK_RESISTANCE.get() * k), true);
        PowerTicker.setModifier(player, Attributes.ATTACK_DAMAGE, BODY_ATTACK, ThanosConfig.ATTACK_BONUS.get() * k, true);
        PowerTicker.setModifier(player, Attributes.SCALE, BODY_SCALE, ThanosConfig.SCALE.get() * k, true,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        PowerTicker.setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, BODY_REACH, ThanosConfig.REACH_BONUS.get() * k, true);
        PowerTicker.setModifier(player, Attributes.STEP_HEIGHT, BODY_STEP, ThanosConfig.STEP_BONUS.get() * k, true);
        PowerTicker.setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, BODY_FALL, Math.max(-1.0, -0.5 * k), true);
    }

    private static void removeBody(ServerPlayer player) {
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, 0.0, false);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, 0.0, false);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS, 0.0, false);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK, 0.0, false);
        PowerTicker.setModifier(player, Attributes.ATTACK_DAMAGE, BODY_ATTACK, 0.0, false);
        PowerTicker.setModifier(player, Attributes.SCALE, BODY_SCALE, 0.0, false);
        PowerTicker.setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, BODY_REACH, 0.0, false);
        PowerTicker.setModifier(player, Attributes.STEP_HEIGHT, BODY_STEP, 0.0, false);
        PowerTicker.setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, BODY_FALL, 0.0, false);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ---- every tick (his own, alive) ----

    /**
     * His tick, or (3.4) a custom hero's with any of his stones: the history only for the Time Stone, the healing factor
     * only with his body, the Veil only with the Reality Stone (the rest only runs on what a stone started).
     */
    public static void tick(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        State state = state(player);
        if (data.has(HeroAbility.THANOS_TIME)) {
            TimeRewind.remember(player, now, ThanosConfig.REWIND_SECONDS.get() * 20L);
        }
        if (data.has(HeroAbility.THANOS_BODY)) {
            healingFactor(player, state, now, data.bodyScale(HeroAbility.THANOS_BODY));
        }
        if (data.has(HeroAbility.THANOS_REALITY)) {
            veilTick(player, data, level);
        }
        slamTick(player, state, level);
        sphereTick(player, state, level, now);
        vortexTick(player, state, level, now);
        SoulDrain.tick(player, data, held(player) || starving(player), takenLastSecond(state, now), now);
        if (data.is(Live.SOUL_SIGHT) && now >= state.sightEnds) {
            data.set(Live.SOUL_SIGHT, false);
        }
        lockTick(player, state, level, now);
        state.stoned.values().removeIf(until -> until < now);
    }

    /**
     * The Titan healing factor: when he has not been hurt for a while he heals, slowly, at the cost of a little hunger.
     */
    private static void healingFactor(ServerPlayer player, State state, long now, float scale) {
        if (player.getHealth() >= player.getMaxHealth() || now - state.lastHurt < ThanosConfig.REGEN_DELAY_TICKS.get() || starving(player)) {
            return;
        }
        float heal = ThanosConfig.REGEN_PER_SECOND.get().floatValue() / 20.0F * scale;
        if (heal > 0.0F) {
            player.heal(heal);
            player.causeFoodExhaustion(0.01F);
        }
    }

    /** He took {@code amount} of damage: the healing factor waits, and a hard enough blow tears a chain. */
    public static void hurt(ServerPlayer thanos, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        long now = thanos.level().getGameTime();
        State state = state(thanos);
        state.lastHurt = now;
        state.taken.addLast(new Hurt(now, amount));
        while (!state.taken.isEmpty() && now - state.taken.peekFirst().time() > 20) {
            state.taken.removeFirst();
        }
    }

    /** What he has taken in the last second. */
    private static float takenLastSecond(State state, long now) {
        float total = 0.0F;
        for (Hurt hurt : state.taken) {
            if (now - hurt.time() <= 20) {
                total += hurt.amount();
            }
        }
        return total;
    }

    // ---- where things are ----

    /** The Gauntlet's hand: on his off arm, a little ahead of him at the height of the chest. */
    public static Vec3 handPosition(Player player) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 left = new Vec3(forward.z, 0.0, -forward.x);
        if (player.getMainArm() == HumanoidArm.LEFT) {
            left = left.scale(-1.0);
        }
        double size = player.getScale();
        return player.position().add(left.scale(0.4 * size)).add(forward.scale(0.4 * size)).add(0.0, player.getBbHeight() * 0.7, 0.0);
    }

    /** The middle of a creature's body, where a chain or a sphere goes. */
    private static Vec3 chestOf(Entity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.6, 0.0);
    }

    /** The creature an entity is, or the creature a part of it belongs to (the Ender Dragon's body). */
    @Nullable
    private static LivingEntity creatureOf(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return Platform.get().parts().parentOf(entity) instanceof LivingEntity parent ? parent : null;
    }

    /** What the stones see along his line of sight: the creature in it, if any, and where the line ends. */
    private record Aim(@Nullable LivingEntity creature, Vec3 point, boolean block) {
    }

    /**
     * What he looks at within {@code range}: the first creature {@code wanted} accepts (its hitbox inflated by {@code
     * inflate}) unless a block is nearer, otherwise the first block, otherwise the end of the range.
     */
    private static Aim aim(ServerPlayer player, double range, double inflate, Predicate<LivingEntity> wanted) {
        EntityHitResult entity = Raycasts.entity(player, range, inflate, e -> {
            LivingEntity creature = creatureOf(e);
            return creature != null && creature != player && creature.isAlive() && !creature.isSpectator() && wanted.test(creature);
        }, 1.0F);
        if (entity != null) {
            return new Aim(creatureOf(entity.getEntity()), entity.getLocation(), false);
        }
        BlockHitResult block = Raycasts.block(player, range, 1.0F);
        if (block.getType() != HitResult.Type.MISS) {
            return new Aim(null, block.getLocation(), true);
        }
        return new Aim(null, player.getEyePosition().add(player.getViewVector(1.0F).scale(range)), false);
    }

    /**
     * A creature a single, aimed stone may strike: not an armour stand, Creative player or ally; a player only with
     * PvP.
     */
    private static boolean mayStrike(ServerPlayer thanos, LivingEntity target) {
        return !StonePowers.untouchable(target) && AreaTargets.mayHurt(thanos, target, true);
    }

    /** Whether a player is out of reach of the stones because PvP is off (or he is a friend): and says so. */
    private static boolean pvpRefuses(ServerPlayer thanos, LivingEntity target) {
        if (target instanceof Player other && !other.isCreative() && !other.isSpectator() && !thanos.canHarmPlayer(other)) {
            thanos.displayClientMessage(Component.translatable("message.enhanced.thanos.pvp_off"), true);
            return true;
        }
        return false;
    }

    /**
     * The first player in his line of sight that PvP protects, if no creature is nearer: so that a shot at a friend
     * says so.
     */
    private static boolean protectedPlayerAhead(ServerPlayer thanos, double range, @Nullable Aim aim) {
        EntityHitResult ahead = Raycasts.entity(thanos, range, 0.3, e -> e instanceof Player other && other != thanos
                && !other.isCreative() && !other.isSpectator() && !thanos.canHarmPlayer(other), 1.0F);
        if (ahead == null) {
            return false;
        }
        double limit = aim == null ? range : aim.point().distanceTo(thanos.getEyePosition());
        return ahead.getLocation().distanceTo(thanos.getEyePosition()) <= limit + 0.5;
    }

    /** Tells him if a player within {@code radius} is spared by PvP, for the stones that fall on an area. */
    private static void sayIfPvpSpares(ServerPlayer thanos, double radius) {
        if (!thanos.level().getEntitiesOfClass(Player.class, thanos.getBoundingBox().inflate(radius), other -> other != thanos
                && !other.isCreative() && !other.isSpectator() && !thanos.canHarmPlayer(other)).isEmpty()) {
            thanos.displayClientMessage(Component.translatable("message.enhanced.thanos.pvp_off"), true);
        }
    }

    // ---- R, the Power Stone ----

    /**
     * A violet beam from the Gauntlet along his line of sight, to the first creature or block: the creature takes the
     * blast and is thrown, and the energy bursts where the beam ends (everyone else near takes the splash). A complete
     * Chaos Shield stops it and wears. No block is broken.
     */
    private static void powerBlast(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!mayStart(player, data, HeroAbility.THANOS_POWER, now)) {
            return;
        }
        double range = ThanosConfig.POWER_BLAST_RANGE.get();
        Aim aim = aim(player, range, 0.3, target -> mayStrike(player, target));
        if (aim.creature() == null && protectedPlayerAhead(player, range, aim)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.pvp_off"), true);
        }
        Vec3 hand = handPosition(player);
        Vec3 end = aim.point();
        data.startCooldown(HeroAbility.THANOS_POWER, now, ThanosConfig.POWER_BLAST_COOLDOWN.get());
        player.causeFoodExhaustion(ThanosConfig.POWER_BLAST_EXHAUSTION.get().floatValue());
        ThanosSounds.CLENCH.play(level, player.position());
        ThanosSounds.POWER_BLAST.play(level, player.position());
        StoneKind.POWER.voice().use(level, player.position(), 0.6F, 1.0F);

        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, hand, end, 1.0F);
        if (shield != null) {
            // The beam spends itself on the sphere (with PvP off it would not have hurt her: it costs the shield
            // nothing).
            end = shield.point();
            if (shield.owner() instanceof ServerPlayer wanda && player.canHarmPlayer(wanda)) {
                WandaAbilities.wear(wanda, ThanosConfig.POWER_BLAST_DAMAGE.get().floatValue());
            }
            level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.2F), end.x, end.y, end.z, 6, 0.15, 0.15, 0.15, 0.12);
        } else {
            LivingEntity target = aim.creature();
            if (target != null) {
                if (StonePowers.hurt(level, player, target, ModDamageTypes.STONE_POWER, ThanosConfig.POWER_BLAST_DAMAGE.get().floatValue(), player)) {
                    StonePowers.shove(target, hand, 1.2, 0.2);
                }
            }
            if (target != null || aim.block()) {
                float splash = ThanosConfig.POWER_BLAST_SPLASH.get().floatValue();
                for (LivingEntity e : StonePowers.around(level, player, end, 2.5, e -> e != target)) {
                    if (StonePowers.hurt(level, player, e, ModDamageTypes.STONE_BLAST, splash, player)) {
                        StonePowers.shove(e, end, 0.8, 0.3);
                    }
                }
                ThanosEffects.shockRing(level, end, 2.5F, 6, AbilityColors.color(player, HeroAbility.THANOS_POWER,
                        StoneKind.POWER.color()));
                level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0, 0, 0, 0);
            }
        }
        ThanosEffects.beam(level, player, hand, end, 0.6F, 6, AbilityColors.color(player, HeroAbility.THANOS_POWER,
                StoneKind.POWER.color()));
    }

    /**
     * Sneak + R: he drives the Gauntlet into the ground and a ring runs out from his feet ({@link #slamTick}), throwing
     * everything that stands on the ground; a jump clears it.
     */
    private static void titanSlam(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        State state = state(player);
        if (!mayStart(player, data, HeroAbility.THANOS_POWER, now) || state.slam != null) {
            return;
        }
        data.startCooldown(HeroAbility.THANOS_POWER, now, ThanosConfig.SLAM_COOLDOWN.get());
        player.causeFoodExhaustion(ThanosConfig.SLAM_EXHAUSTION.get().floatValue());
        state.slam = new Slam(player.position());
        float radius = ThanosConfig.SLAM_RADIUS.get().floatValue();
        sayIfPvpSpares(player, radius);
        ThanosEffects.shockRing(level, player.position().add(0.0, 0.1, 0.0), radius, (int) Math.ceil(radius / SLAM_SPEED), AbilityColors.color(player, HeroAbility.THANOS_TITAN_SLAM, StoneKind.POWER.color()));
        ThanosSounds.CLENCH.play(level, player.position());
        ThanosSounds.HIT.play(level, player.position(), 1.2F, 0.6F);
        StoneKind.POWER.voice().use(level, player.position(), 0.6F, 0.9F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(player.blockPosition().below())),
                player.getX(), player.getY() + 0.2, player.getZ(), 24, 0.6, 0.1, 0.6, 0.2);
    }

    private static void slamTick(ServerPlayer player, State state, ServerLevel level) {
        Slam slam = state.slam;
        if (slam == null) {
            return;
        }
        double from = slam.radius;
        slam.radius = Math.min(from + SLAM_SPEED, ThanosConfig.SLAM_RADIUS.get());
        StonePowers.ring(level, player, slam.centre, from, slam.radius, ThanosConfig.SLAM_DAMAGE.get().floatValue(),
                ModDamageTypes.STONE_BLAST, 0.9, 0.5, slam.hit);
        if (slam.radius >= ThanosConfig.SLAM_RADIUS.get()) {
            state.slam = null;
        }
    }

    // ---- G, the Space Stone ----

    /**
     * A blue ring opens at his feet and another where he looks, within the range of the stone, and he steps through:
     * on top of the block he looks at, or right behind the creature. A Hex wall refuses it, Mjolnir pins him, the arena
     * of a boss keeps him in, and he needs room for his whole body.
     */
    private static void portalStep(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!mayStart(player, data, HeroAbility.THANOS_SPACE, now)) {
            return;
        }
        double range = ThanosConfig.SPACE_STEP_RANGE.get();
        HitResult hit = Raycasts.any(player, range, e -> {
            LivingEntity creature = creatureOf(e);
            return creature != null && creature != player && creature.isAlive() && !creature.isSpectator();
        }, 1.0F);
        Vec3 spot;
        if (hit instanceof EntityHitResult entityHit && creatureOf(entityHit.getEntity()) != null) {
            spot = Teleports.behind(player, creatureOf(entityHit.getEntity()));
        } else if (hit instanceof BlockHitResult blockHit && blockHit.getType() != HitResult.Type.MISS) {
            spot = Teleports.onto(player, blockHit);
        } else {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.no_block", (int) range), true);
            return;
        }
        if (spot == null || wet(level, player, spot)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.no_space"), true);
            return;
        }
        if (!HexManager.onTeleport(level, player, spot)) {
            return;
        }
        data.startCooldown(HeroAbility.THANOS_SPACE, now, ThanosConfig.SPACE_STEP_COOLDOWN.get());
        player.causeFoodExhaustion(ThanosConfig.STEP_EXHAUSTION.get().floatValue());
        Vec3 from = player.position();
        ThanosEffects.portal(level, from.add(0.0, 0.2, 0.0), 1.6F, 60,
                AbilityColors.color(player, HeroAbility.THANOS_SPACE, StoneKind.SPACE.color()));
        ThanosEffects.portal(level, spot.add(0.0, 0.2, 0.0), 1.6F, 60,
                AbilityColors.color(player, HeroAbility.THANOS_SPACE, StoneKind.SPACE.color()));
        ThanosSounds.PORTAL.play(level, from, 1.0F, 1.3F);
        StoneKind.SPACE.voice().use(level, from, 0.6F, 1.0F);
        moveTo(player, level, spot);
        ThanosSounds.PORTAL.play(level, spot, 1.0F, 1.5F);
    }

    /** Whether the body would stand in a liquid at {@code spot}. */
    private static boolean wet(ServerLevel level, Entity traveller, Vec3 spot) {
        return level.containsAnyLiquid(traveller.getBoundingBox().move(spot.subtract(traveller.position())));
    }

    /** Takes the player to {@code spot}, looking where he looked, off whatever he rode, with the fall forgotten. */
    private static void moveTo(ServerPlayer player, ServerLevel level, Vec3 spot) {
        player.stopRiding();
        player.teleportTo(level, spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        player.setYHeadRot(player.getYRot());
        player.resetFallDistance();
    }

    /**
     * Sneak + G: the creature he looks at is drawn through a portal to two blocks in front of him ({@link
     * #vortexTick}). Not a boss, not a creature held by Mjolnir, not an armour stand; a player only with PvP.
     */
    private static void spacePull(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        State state = state(player);
        if (!mayStart(player, data, HeroAbility.THANOS_SPACE, now) || state.vortex != null) {
            return;
        }
        Aim aim = aim(player, ThanosConfig.PULL_RANGE.get(), 0.3, target -> !(target instanceof ArmorStand));
        LivingEntity target = aim.creature();
        if (target == null || StonePowers.untouchable(target) || !StonePowers.graspable(target)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.no_target"), true);
            return;
        }
        if (pvpRefuses(player, target)) {
            return;
        }
        if (Shelters.stops(player, player.getEyePosition(), target, 0.0F)) {
            // 3.3: Billy's Hex Barrier faces him (a Hex's wall was already in the way of his aim)
            player.displayClientMessage(Component.translatable("message.enhanced.shelter.power_stopped"), true);
            return;
        }
        data.startCooldown(HeroAbility.THANOS_SPACE, now, ThanosConfig.PULL_COOLDOWN.get());
        player.causeFoodExhaustion(ThanosConfig.PULL_EXHAUSTION.get().floatValue());
        state.vortex = new Vortex(target.getId(), now + FLIGHT_TICKS);
        ThanosEffects.mandala(level, target.position().add(0.0, 0.05, 0.0), 1.6F, FLIGHT_TICKS, AbilityColors.color(player, HeroAbility.THANOS_SPACE_PULL, StoneKind.SPACE.color()));
        ThanosEffects.portal(level, pullSpot(player).add(0.0, 0.2, 0.0), 1.2F, FLIGHT_TICKS + 14,
                AbilityColors.color(player, HeroAbility.THANOS_SPACE_PULL, StoneKind.SPACE.color()));
        ThanosSounds.PORTAL.play(level, player.position(), 1.0F, 0.8F);
        StoneKind.SPACE.voice().use(level, player.position(), 0.6F, 0.8F);
    }

    /** Two blocks in front of him, on his floor. */
    private static Vec3 pullSpot(ServerPlayer player) {
        return player.position().add(Vec3.directionFromRotation(0.0F, player.getYRot()).scale(2.0 * player.getScale()));
    }

    private static void vortexTick(ServerPlayer player, State state, ServerLevel level, long now) {
        Vortex vortex = state.vortex;
        if (vortex == null || now < vortex.arrives()) {
            return;
        }
        state.vortex = null;
        if (!(level.getEntity(vortex.target()) instanceof LivingEntity target) || !target.isAlive() || !StonePowers.graspable(target)) {
            return;
        }
        Vec3 spot = null;
        for (double scale : new double[]{1.0, 0.75, 1.25, 0.5, 1.5}) {
            Vec3 candidate = player.position().add(Vec3.directionFromRotation(0.0F, player.getYRot()).scale(2.0 * scale * player.getScale()));
            if (Teleports.fits(target, candidate) && !wet(level, target, candidate)) {
                spot = candidate;
                break;
            }
        }
        if (spot == null || Shelters.shelters(player, target) || !HexManager.onTeleport(level, target, spot)) {
            return; // (3.3: gone behind a wall since he reached for it: the portal does not reach in)
        }
        // A creature held by telekinesis is let go first.
        for (ServerPlayer holder : level.players()) {
            if (Powers.get(holder).heldEntityId() == target.getId()) {
                Telekinesis.release(holder, false);
            }
        }
        Vec3 from = target.position();
        Vec3 toward = player.position().subtract(spot);
        float yaw = (float) (Mth.atan2(-toward.x, toward.z) * Mth.RAD_TO_DEG);
        target.teleportTo(level, spot.x, spot.y, spot.z, Set.of(), yaw, 0.0F);
        target.setYHeadRot(yaw);
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.resetFallDistance();
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, true, true), player);
        ThanosEffects.portal(level, from.add(0.0, 0.2, 0.0), 1.2F, 20,
                AbilityColors.color(player, HeroAbility.THANOS_SPACE_PULL, StoneKind.SPACE.color()));
        ThanosEffects.shockRing(level, spot, 1.8F, 5, AbilityColors.color(player, HeroAbility.THANOS_SPACE_PULL,
                StoneKind.SPACE.color()));
        ThanosSounds.PORTAL.play(level, spot, 1.0F, 1.4F);
    }

    // ---- V, the Reality Stone ----

    /**
     * A small red sphere leaves the Gauntlet and flies to the creature he looks at ({@link #sphereTick}), which is
     * turned to stone. A boss shakes it off; a player only with PvP.
     */
    private static void transmute(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        State state = state(player);
        if (!mayStart(player, data, HeroAbility.THANOS_REALITY, now) || state.sphere != null) {
            return;
        }
        Aim aim = aim(player, ThanosConfig.TRANSMUTE_RANGE.get(), 0.3, target -> !(target instanceof ArmorStand));
        LivingEntity target = aim.creature();
        if (target == null || StonePowers.untouchable(target)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.no_target"), true);
            return;
        }
        if (pvpRefuses(player, target)) {
            return;
        }
        data.startCooldown(HeroAbility.THANOS_REALITY, now, ThanosConfig.TRANSMUTE_COOLDOWN.get());
        player.causeFoodExhaustion(ThanosConfig.TRANSMUTE_EXHAUSTION.get().floatValue());
        state.sphere = new Sphere(target.getId(), now + FLIGHT_TICKS);
        ThanosEffects.beam(level, player, handPosition(player), chestOf(target), 0.35F, FLIGHT_TICKS, AbilityColors.color(player, HeroAbility.THANOS_REALITY, StoneKind.REALITY.color()));
        ThanosSounds.CLENCH.play(level, player.position(), 0.8F, 1.2F);
        StoneKind.REALITY.voice().use(level, player.position(), 0.6F, 1.0F);
    }

    private static void sphereTick(ServerPlayer player, State state, ServerLevel level, long now) {
        Sphere sphere = state.sphere;
        if (sphere == null || now < sphere.arrives()) {
            return;
        }
        state.sphere = null;
        if (!(level.getEntity(sphere.target()) instanceof LivingEntity target) || !target.isAlive() || StonePowers.untouchable(target)) {
            return;
        }
        Vec3 hand = handPosition(player);
        Vec3 chest = chestOf(target);
        // A Chaos Shield in the way stops the sphere (it is a projectile, and costs the shield a little).
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, hand, chest, 1.0F);
        if (shield != null) {
            if (shield.owner() instanceof ServerPlayer wanda && player.canHarmPlayer(wanda)) {
                WandaAbilities.wear(wanda, 2.0F);
            }
            Vec3 at = shield.point();
            level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.THANOS_REALITY, StoneKind.REALITY.core()), 1.2F), at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0.1);
            return;
        }
        if (target instanceof LokiIllusionEntity illusion) {
            illusion.dissolve(chest);
            return;
        }
        int ticks = ThanosConfig.TRANSMUTE_TICKS.get();
        if (StonePowers.petrify(level, player, target, ticks)) {
            state.stoned.put(target.getUUID(), level.getGameTime() + ticks);
            ThanosSounds.REALITY_POP.play(level, target.position());
        } else {
            // A boss shakes the stone off: it cracks and falls.
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    chest.x, chest.y, chest.z, 14, 0.3, 0.4, 0.3, 0.1);
            ThanosSounds.REALITY_POP.play(level, target.position(), 1.0F, 0.6F);
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.shaken_off"), true);
        }
    }

    /**
     * Sneak + V: the Reality Veil. While it is on, an ordinary projectile that comes at him is turned into a bubble
     * that pops ({@link #veilTick}). It burns a little hunger and goes out when he starves; it is saved with him.
     */
    private static void realityVeil(ServerPlayer player, PowerData data) {
        boolean on = !data.toggle(Toggle.REALITY_VEIL);
        if (on && starving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.too_hungry"), true);
            return;
        }
        data.setToggle(Toggle.REALITY_VEIL, on);
        ServerLevel level = player.serverLevel();
        if (on) {
            ThanosSounds.REALITY_POP.play(level, player.position(), 1.0F, 0.9F);
            StoneKind.REALITY.voice().use(level, player.position(), 0.6F, 1.0F);
            level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.THANOS_REALITY_VEIL, StoneKind.REALITY.core()), 1.2F), player.getX(), player.getY() + 1.0, player.getZ(),
                    16, 0.5, 0.8, 0.5, 0.05);
        }
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.thanos.veil.on" : "message.enhanced.thanos.veil.off"), true);
    }

    private static void veilTick(ServerPlayer player, PowerData data, ServerLevel level) {
        if (!data.toggle(Toggle.REALITY_VEIL)) {
            return;
        }
        if (player.isSpectator() || starving(player)) {
            data.setToggle(Toggle.REALITY_VEIL, false);
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.veil.off"), true);
            return;
        }
        player.causeFoodExhaustion(ThanosConfig.VEIL_EXHAUSTION.get().floatValue());
        bubble(level, player);
    }

    /**
     * Turns the projectiles within the Veil's reach that are heading for him into bubbles (not his own, not the
     * stones').
     */
    private static void bubble(ServerLevel level, ServerPlayer player) {
        Vec3 centre = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        StonePowers.bubbleProjectiles(level, centre, ThanosConfig.VEIL_RADIUS.get(),
                projectile -> !isOwnedBy(projectile.getOwner(), player) && !Shelters.keepsOut(level, player, centre, projectile.position(), false));
    }

    /** Whether the entity is the player or a pet that is his. */
    private static boolean isOwnedBy(@Nullable Entity entity, Player player) {
        return entity == player || entity instanceof TamableAnimal pet && pet.isOwnedBy(player);
    }

    /**
     * A projectile is about to hit Thanos while the Veil is drawn: it is a bubble instead. (The tick turns the ones it
     * sees coming; this catches the one that was too fast for it.)
     *
     * @return whether the Veil took it
     */
    public static boolean veilTakes(ServerPlayer thanos, Projectile projectile) {
        if (!Powers.get(thanos).has(HeroAbility.THANOS_REALITY) || !Powers.get(thanos).toggle(Toggle.REALITY_VEIL)) {
            return false;
        }
        // (3.6: the catch itself is the Reality Stone's, StonePowers.realityCatches, shared with the stone.)
        return StonePowers.realityCatches(thanos, projectile);
    }

    // ---- B, the Soul Stone ----

    /**
     * Hold B: he locks onto the creature he looks at and, while the key is held and the chain holds, takes a little of
     * its life every half second and heals with it ({@link #drainTick}). Letting go ends it, and starts the cooldown.
     */
    private static void soulDrain(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!mayStart(player, data, HeroAbility.THANOS_SOUL, now) || SoulDrain.draining(player)) {
            return;
        }
        double range = ThanosConfig.DRAIN_RANGE.get();
        Aim aim = aim(player, range, 0.3, target -> mayStrike(player, target));
        LivingEntity target = aim.creature();
        if (target == null) {
            boolean spared = protectedPlayerAhead(player, range, aim);
            player.displayClientMessage(Component.translatable(spared ? "message.enhanced.thanos.pvp_off" : "message.enhanced.thanos.no_target"), true);
            return;
        }
        SoulDrain.start(player, data, target, DRAIN);
    }

    /** Ends the Soul Drain, if there is one, and starts its cooldown. */
    private static void endDrain(ServerPlayer player, PowerData data) {
        SoulDrain.end(player, data);
    }

    /**
     * Sneak + B: for a few seconds every creature near glows through the walls for him (the client draws it while
     * {@link Live#SOUL_SIGHT} stands), and the stone sees through tricks: Loki's duplicates dissolve, a hidden Loki is
     * seen and a Loki who took another shape is himself again; whoever is invisible glows for everyone for five
     * seconds.
     */
    private static void soulSight(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!mayStart(player, data, HeroAbility.THANOS_SOUL, now)) {
            return;
        }
        State state = state(player);
        double range = ThanosConfig.SIGHT_RANGE.get();
        state.sightEnds = now + ThanosConfig.SIGHT_TICKS.get();
        data.set(Live.SOUL_SIGHT, true);
        if (!player.isCreative()) {
            data.startCooldown(HeroAbility.THANOS_SOUL, now, ThanosConfig.SIGHT_COOLDOWN.get());
        }
        player.causeFoodExhaustion(ThanosConfig.SIGHT_EXHAUSTION.get().floatValue());
        if (ThanosConfig.SIGHT_BREAKS_ILLUSIONS.get()) {
            LokiAbilities.dissolveDuplicates(level, player.position(), range, player);
            for (ServerPlayer other : level.players()) {
                if (other != player && other.isAlive() && other.distanceTo(player) <= range && !Shelters.shelters(player, other)) {
                    LokiAbilities.unmask(other);
                }
            }
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && (e.isInvisible() || e.hasEffect(MobEffects.INVISIBILITY)) && e.distanceTo(player) <= range)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, REVEAL_TICKS, 0, false, false), player);
        }
        level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.THANOS_SOUL_SIGHT, StoneKind.SOUL.color()), 1.4F), player.getX(), player.getY() + 1.0, player.getZ(),
                20, 0.6, 0.8, 0.6, 0.05);
        ThanosSounds.SOULWORLD.play(level, player.position(), 0.6F, 1.4F);
        StoneKind.SOUL.voice().use(level, player.position(), 0.6F, 1.2F);
    }

    // ---- N, the Time Stone ----

    /**
     * How much history the Rewind has to go back into, in ticks (0 if none): the age of the oldest snapshot kept. (The
     * HUD of a client cannot see this; it counts for itself.)
     */
    public static int historyTicks(ServerPlayer thanos) {
        return TimeRewind.historyTicks(thanos);
    }

    /**
     * Records the player's present state as if it were {@code ago} ticks old, so that a test can have a history without
     * waiting for one.
     */
    @VisibleForTesting
    public static void remember(ServerPlayer thanos, int ago) {
        TimeRewind.rememberAgo(thanos, ago);
    }

    /**
     * N: he goes back to where he was five seconds ago, with the health he had then if it was higher (as much as the
     * config allows), and sheds the harmful effects that began since. His food, his things and everyone else's state
     * stay as they are. With under two seconds of history there is nothing to rewind. A Hex wall or the arena of a boss
     * refuses the place (the rest still happens); Mjolnir's pin refuses all of it.
     */
    private static void rewind(ServerPlayer player, PowerData data) {
        if (!mayStart(player, data, HeroAbility.THANOS_TIME, player.serverLevel().getGameTime())) {
            return;
        }
        TimeRewind.rewind(player, data, new TimeRewind.Cost(HeroAbility.THANOS_TIME, ThanosConfig.REWIND_COOLDOWN.get(),
                ThanosConfig.REWIND_EXHAUSTION.get().floatValue(), ThanosConfig.REWIND_MAX_HEAL.get().floatValue(),
                AbilityColors.color(player, HeroAbility.THANOS_TIME, StoneKind.TIME.color())));
    }

    /**
     * Sneak + N: a green dome of the Time Stone opens at his feet and, for a few seconds, everything in it but him and
     * his own is slowed ({@link #lockTick}); projectiles in it hang in the air and crawl.
     */
    private static void timeLock(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        State state = state(player);
        if (!mayStart(player, data, HeroAbility.THANOS_TIME, now) || state.lock != null) {
            return;
        }
        int ticks = ThanosConfig.TIME_LOCK_TICKS.get();
        data.startCooldown(HeroAbility.THANOS_TIME, now, ThanosConfig.TIME_LOCK_COOLDOWN.get());
        player.causeFoodExhaustion(ThanosConfig.TIME_LOCK_EXHAUSTION.get().floatValue());
        state.lock = new Lock(level.dimension(), player.position(), now + ticks);
        float radius = ThanosConfig.TIME_LOCK_RADIUS.get().floatValue();
        sayIfPvpSpares(player, radius);
        ThanosEffects.dome(level, player.position(), radius, ticks,
                AbilityColors.color(player, HeroAbility.THANOS_TIME_LOCK, StoneKind.TIME.color()));
        ThanosEffects.mandala(level, player.position().add(0.0, 0.05, 0.0), radius, ticks,
                AbilityColors.color(player, HeroAbility.THANOS_TIME_LOCK, StoneKind.TIME.color()));
        ThanosSounds.REWIND.play(level, player.position(), 0.8F, 1.3F);
        StoneKind.TIME.voice().use(level, player.position(), 0.6F, 0.9F);
    }

    private static void lockTick(ServerPlayer player, State state, ServerLevel level, long now) {
        Lock lock = state.lock;
        if (lock == null) {
            return;
        }
        if (now >= lock.ends || level.dimension() != lock.dimension) {
            endLock(player.getServer(), state);
            return;
        }
        double radius = ThanosConfig.TIME_LOCK_RADIUS.get();
        AABB box = new AABB(lock.centre, lock.centre).inflate(radius);
        if (now % LOCK_RENEW == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> mayLock(player, e)
                    && e.position().add(0.0, e.getBbHeight() * 0.5, 0.0).distanceTo(lock.centre) <= radius + e.getBbWidth() * 0.5
                    && !Shelters.shelters(player, lock.centre, e))) {
                if (e.addEffect(new MobEffectInstance(ModEffects.TIME_LOCKED, LOCK_HOLD, 0, false, true, true), player)
                        || e.hasEffect(ModEffects.TIME_LOCKED)) {
                    lock.held.add(e.getUUID());
                }
            }
            level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.THANOS_TIME_LOCK, StoneKind.TIME.color()), 1.2F), lock.centre.x, lock.centre.y + 0.1, lock.centre.z,
                    4, radius * 0.5, 0.05, radius * 0.5, 0.0);
        }
        IntSet inside = new IntOpenHashSet();
        double drag = 1.0 - ThanosConfig.TIME_LOCK_PROJECTILE_DRAG.get();
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box, p -> !isOwnedBy(p.getOwner(), player)
                && p.position().distanceTo(lock.centre) <= radius && !Shelters.keepsOut(level, player, lock.centre, p.position(), false))) {
            if (lock.hung.contains(projectile.getId()) || !projectile.isNoGravity()) {
                // (One that has no gravity of its own is slowed too, but is not ours to let fall again.)
                inside.add(projectile.getId());
                projectile.setNoGravity(true);
            }
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(drag));
            projectile.hurtMarked = true;
        }
        // The ones that left the dome fall again.
        for (int id : lock.hung.toIntArray()) {
            if (!inside.contains(id)) {
                if (level.getEntity(id) instanceof Projectile projectile) {
                    projectile.setNoGravity(false);
                }
                lock.hung.remove(id);
            }
        }
        lock.hung.addAll(inside);
    }

    /** Whether the Time Lock holds this creature: not him, not his pets, a player only with PvP. */
    private static boolean mayLock(ServerPlayer thanos, LivingEntity e) {
        if (e == thanos || !e.isAlive() || e.isSpectator() || isOwnedBy(e, thanos) || e instanceof ArmorStand) {
            return false;
        }
        if (e instanceof Player other) {
            return !other.isCreative() && thanos.canHarmPlayer(other);
        }
        return true;
    }

    /** The dome closes: whoever it held is free and the projectiles it hung fall. */
    private static void endLock(@Nullable MinecraftServer server, State state) {
        Lock lock = state.lock;
        state.lock = null;
        if (lock == null || server == null) {
            return;
        }
        ServerLevel level = server.getLevel(lock.dimension);
        if (level == null) {
            return;
        }
        for (UUID id : lock.held) {
            if (level.getEntity(id) instanceof LivingEntity held) {
                held.removeEffect(ModEffects.TIME_LOCKED);
            }
        }
        for (int id : lock.hung.toIntArray()) {
            if (level.getEntity(id) instanceof Projectile projectile) {
                projectile.setNoGravity(false);
            }
        }
    }

    // ---- the end of things ----

    /** The power is being taken away: everything he left in the world ends. */
    public static void endAll(ServerPlayer player, PowerData data) {
        gone(player, data);
        data.setToggle(Toggle.REALITY_VEIL, false);
        removeBody(player);
    }

    /**
     * Everything of his that stands in the world ends, as it does when he dies, leaves or changes dimension: the chain,
     * the sight, the dome (the creatures it held are free and the projectiles it hung fall), the ring, the sphere and
     * the vortex, the creatures he turned to stone, the history. Nothing is left running without him.
     */
    public static void gone(ServerPlayer player, PowerData data) {
        data.set(Live.SOUL_DRAINING, false);
        data.set(Live.SOUL_SIGHT, false);
        // (3.6: the drain and the history live in the stones' classes; they end with the rest of him.)
        SoulDrain.forget(player);
        TimeRewind.forget(player);
        State state = STATES.remove(player.getUUID());
        if (state == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        endLock(server, state);
        if (server != null) {
            long now = player.level().getGameTime();
            for (Map.Entry<UUID, Long> stone : state.stoned.entrySet()) {
                if (stone.getValue() < now) {
                    continue;
                }
                for (ServerLevel level : server.getAllLevels()) {
                    if (level.getEntity(stone.getKey()) instanceof LivingEntity victim) {
                        victim.removeEffect(ModEffects.PETRIFIED);
                        break;
                    }
                }
            }
        }
    }

    /**
     * A dead player's tick, and every moment a hold is released: the chain is let go (his tick then ends it for good).
     */
    public static void stopHold(PowerData data) {
        data.set(Live.SOUL_DRAINING, false);
    }

    /** The server stopped: nobody is anything any more. */
    public static void clear() {
        STATES.clear();
        SoulDrain.clear();
        TimeRewind.clear();
    }

    private ThanosAbilities() {
    }
}
