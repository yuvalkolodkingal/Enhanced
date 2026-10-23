package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StonePowers;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Captain America (3.8): the super-soldier of the serum, with the Vibranium Shield. The plug-in is {@link
 * CaptainServer}; the shield's rules are {@link ShieldRules} and {@link CaptainEvents}, its flight {@link
 * ThrownShieldEntity} (thrown by {@link ShieldThrow}); the slam and the leap {@link ShieldSlam}, the charge and the
 * bash {@link ShieldCharge}, Assemble and Inspire {@link Assemble}, the combo and the flip kick {@link CaptainCombo}.
 *
 * <p>R is the Shield Throw (Sneak: the Ricochet Throw), G the Shield Slam (Sneak: the Super-Soldier Leap), V the Shield
 * Charge (Sneak: the Shield Bash), B Assemble (Sneak: Inspire), N the Combat Combo (Sneak: the Flip Kick). 3.4's rules
 * from birth: every cooldown is by its row, the Sneak variant is {@code alt}, every part is guarded by {@code
 * data.has(row)}, his body is {@link HeroAbility#CAPTAIN_BODY} scaled by its {@code bodyScale}.
 */
public final class CaptainAbilities {
    // The body's modifiers (permanent: right when a player logs in).
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("captain_health");
    private static final ResourceLocation BODY_SPEED = Enhanced.id("captain_speed");
    private static final ResourceLocation BODY_ATTACK = Enhanced.id("captain_attack");
    private static final ResourceLocation BODY_ATTACK_SPEED = Enhanced.id("captain_attack_speed");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("captain_attack_knockback");
    private static final ResourceLocation BODY_JUMP = Enhanced.id("captain_jump");
    private static final ResourceLocation BODY_SAFE_FALL = Enhanced.id("captain_safe_fall");

    /** The suit's blue, the star's silver-white, the stripes' red. */
    public static final int BLUE = 0x2A4BA0;
    public static final int SILVER = 0xE8EEF5;
    public static final int RED = 0xC8232C;
    /** How far his effects are sent. */
    static final double FX_RANGE = 64.0;

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** What each Captain America carries from tick to tick; forgotten when he leaves or the server stops. */
    static final class State {
        /** The game time of his last tick: a second tick in the same game tick does nothing more. */
        long lastTick = Long.MIN_VALUE;
        /** The Shield Slam under way: when it began (its shockwave comes when he lands), and whether with a shield. */
        long slamStart = -1;
        boolean slamShield;
        /** The Super-Soldier Leap under way: when it began. */
        long leapStart = -1;
        /** Until when his own leaps spare him the fall's damage. */
        long fallGuardUntil;
        /** The Shield Charge under way: when, from where, which way, and whom it has struck. */
        long chargeStart = -1;
        Vec3 chargeFrom = Vec3.ZERO;
        Vec3 chargeDir = Vec3.ZERO;
        final java.util.Set<Integer> chargeHit = new java.util.HashSet<>();
        /** The last time his shield rang for a block (the ring and the flash at most every few ticks). */
        long lastBlockFx = Long.MIN_VALUE / 2;
    }

    static State state(UUID id) {
        return STATES.computeIfAbsent(id, k -> new State());
    }

    @Nullable
    static State peek(UUID id) {
        return STATES.get(id);
    }

    // ---- the numbers ----

    static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    static double cfg(ConfigSpec.DoubleValue value, double fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    static int cfg(ConfigSpec.IntValue value, int fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    static boolean cfg(ConfigSpec.BooleanValue value, boolean fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    /** Whether Captain America is worthy of Mjolnir ({@code [captain_america] worthy}; {@code Traits.of} asks it). */
    public static boolean worthy() {
        return cfg(CaptainConfig.WORTHY, true);
    }

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his, with its Sneak variant said explicitly ({@code alt}): Captain America's own Sneak, or the binding
     * of a custom hero with his abilities (3.4). Each move asks {@code data.has} of its own row.
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down || player.isSpectator() || !player.isAlive() || HealingFactor.isDown(player)) {
            return;
        }
        switch (ability) {
            case 1 -> ShieldThrow.start(player, data, alt);
            case 2 -> {
                if (alt) {
                    ShieldSlam.leap(player, data);
                } else {
                    ShieldSlam.slam(player, data);
                }
            }
            case 3 -> {
                if (alt) {
                    ShieldCharge.bash(player, data);
                } else {
                    ShieldCharge.charge(player, data);
                }
            }
            case 4 -> {
                if (alt) {
                    Assemble.inspire(player, data);
                } else {
                    Assemble.assemble(player, data);
                }
            }
            case 5 -> {
                if (alt) {
                    CaptainCombo.flipKick(player, data);
                } else {
                    CaptainCombo.punch(player, data);
                }
            }
            default -> {
            }
        }
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        State state = state(player.getUUID());
        long now = player.level().getGameTime();
        if (state.lastTick == now) {
            return; // (one tick of his per game tick, however often the player is ticked)
        }
        state.lastTick = now;
        ShieldThrow.tick(player, data);
        ShieldSlam.tick(player, data, state, now);
        ShieldCharge.tick(player, data, state, now);
        if (data.has(HeroAbility.CAPTAIN_SHIELD_SLAM)) {
            data.set(Live.CAP_SLAMMING, state.slamStart >= 0);
        }
        if (data.has(HeroAbility.CAPTAIN_SHIELD_CHARGE)) {
            data.set(Live.CAP_CHARGING, state.chargeStart >= 0);
        }
        backFlag(player);
    }

    /**
     * Keeps the synced "on his back" flag of his state right: a Vibranium Shield anywhere in his inventory but not in
     * a hand, and none in flight. Everyone who sees him draws it on his back ({@code CapShieldLayer}).
     */
    static void backFlag(ServerPlayer player) {
        CaptainState cs = ModAttachments.CAPTAIN.get(player);
        boolean back = ShieldRules.carriesOnBack(player) && cs.shieldEntity() < 0;
        if (cs.has(CaptainState.BACK) != back) {
            ModAttachments.CAPTAIN.set(player, cs.with(CaptainState.BACK, back));
        }
    }

    // ---- the body ----

    /**
     * Whether the player has Captain America's body right now (the hero, or a custom hero with {@code captain.body}).
     */
    public static boolean hasBody(PowerData data) {
        return data.has(HeroAbility.CAPTAIN_BODY);
    }

    /**
     * The super-soldier's body while the player has it (scaled by its strength for a custom hero), nothing of it on
     * anyone else: Speed II (+40 %), Strength II (+6), more health, a better jump and fall, quicker and harder blows.
     * Resistance I is a cut of incoming damage ({@link CaptainEvents}). Attributes, never effects: no icons, milk
     * cannot take them, they are right on login. Health keeps its share of the maximum when the maximum changes.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean on = hasBody(data);
        if (!on && player.getAttribute(Attributes.MAX_HEALTH).getModifier(BODY_HEALTH) == null
                && player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BODY_SPEED) == null) {
            return;
        }
        double k = on ? data.bodyScale(HeroAbility.CAPTAIN_BODY) : 0.0;
        float oldMax = player.getMaxHealth();
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, cfg(CaptainConfig.HEALTH_BONUS, 6.0) * k, on);
        float newMax = player.getMaxHealth();
        if (newMax != oldMax && oldMax > 0.0F && player.isAlive()) {
            player.setHealth(Math.max(1.0F, Math.min(newMax, player.getHealth() * newMax / oldMax)));
        }
        setModifier(player, Attributes.MOVEMENT_SPEED, BODY_SPEED, cfg(CaptainConfig.SPEED_BONUS, 0.4) * k, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.ATTACK_DAMAGE, BODY_ATTACK, cfg(CaptainConfig.ATTACK_BONUS, 6.0) * k, on);
        setModifier(player, Attributes.ATTACK_SPEED, BODY_ATTACK_SPEED, cfg(CaptainConfig.ATTACK_SPEED_BONUS, 0.5) * k,
                on);
        setModifier(player, Attributes.ATTACK_KNOCKBACK, BODY_KNOCKBACK, cfg(CaptainConfig.ATTACK_KNOCKBACK, 0.5) * k,
                on);
        setModifier(player, Attributes.JUMP_STRENGTH, BODY_JUMP, cfg(CaptainConfig.JUMP_BONUS, 0.15) * k, on);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, BODY_SAFE_FALL, cfg(CaptainConfig.SAFE_FALL, 4.0) * k, on);
    }

    /** The share of incoming damage his body shrugs off (Resistance I), by the body's strength; 0 without it. */
    public static float resistance(PowerData data) {
        if (!hasBody(data)) {
            return 0.0F;
        }
        return (float) Math.min(0.9, cfg(CaptainConfig.RESISTANCE, 0.2) * data.bodyScale(HeroAbility.CAPTAIN_BODY));
    }

    static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount,
            boolean present) {
        setModifier(entity, attribute, id, amount, present, AttributeModifier.Operation.ADD_VALUE);
    }

    static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount,
            boolean present, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount || existing.operation() != operation) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    // ---- who his blows reach, and how ----

    /** Whether his blows may hurt the creature (PvP, teams, pets, Creative, the area rule). */
    static boolean mayHurt(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && !target.isSpectator()
                && AreaTargets.mayHurt(player, target, true);
    }

    /** Whether the creature is a boss (it takes the damage of his moves, never their stun). */
    static boolean isBoss(LivingEntity target) {
        return target.getType().is(ModTags.BOSSES);
    }

    /** Hurts the creature with a damage type of his ({@code type}), the blow always landing (the stones' rule). */
    static boolean hurt(ServerPlayer player, LivingEntity target, ResourceKey<DamageType> type, float amount,
            @Nullable Entity direct) {
        return StonePowers.hurt(player.serverLevel(), player, target, type, amount, direct != null ? direct : player);
    }

    /** Every push of his: the shared push, which credits him for the Soul Stone's sacrifice. */
    static void push(LivingEntity victim, Vec3 from, double strength, double lift, LivingEntity cap) {
        StonePowers.shove(victim, from, strength, lift, cap);
    }

    /**
     * The creature he may hurt nearest in front of him within {@code reach}, inside a cone of {@code degrees} round his
     * look (flat), or null.
     */
    @Nullable
    static LivingEntity inFront(ServerPlayer player, double reach, double degrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = flat(player.getLookAngle());
        double cos = Math.cos(Math.toRadians(degrees / 2.0));
        AABB box = player.getBoundingBox().inflate(reach);
        return player.level().getEntitiesOfClass(LivingEntity.class, box, e -> mayHurt(player, e)).stream()
                .filter(e -> {
                    Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                    double flatDist = Math.hypot(to.x, to.z);
                    double dist = Math.max(0.0, flatDist - e.getBbWidth() * 0.5);
                    if (dist > reach || Math.abs(to.y) > reach) {
                        return false;
                    }
                    return flatDist < 0.3 || flat(to).dot(look) >= cos;
                })
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }

    /** The vector flattened to the ground and made one long (straight ahead when it points up or down). */
    static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0.0, v.z);
        return f.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : f.normalize();
    }

    // ---- effects ----

    static void send(ServerLevel level, Entity who, int kind, List<Vec3> points, int arg) {
        List<Vec3> list = points.size() > CaptainFxPayload.MAX_POINTS ? points.subList(0, CaptainFxPayload.MAX_POINTS)
                : points;
        Net.sendToPlayersNear(level, null, who.getX(), who.getY(), who.getZ(), FX_RANGE,
                new CaptainFxPayload(kind, who.getId(), List.copyOf(list), arg));
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Spares him the fall's damage until {@code ticks} from now (his own leaps). */
    static void guardFall(ServerPlayer player, int ticks) {
        State state = state(player.getUUID());
        state.fallGuardUntil = Math.max(state.fallGuardUntil, player.level().getGameTime() + ticks);
        player.fallDistance = 0.0F;
    }

    /** Whether one of his leaps spares him the fall's damage now. */
    static boolean fallGuarded(Player player) {
        State state = STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() <= state.fallGuardUntil;
    }

    // ---- the end of the power, of the life, of the session ----

    /** The power is being taken away (or a custom hero is remade): every move ends; a shield in flight drops. */
    static void endAll(ServerPlayer player, PowerData data) {
        State state = STATES.get(player.getUUID());
        if (state != null) {
            state.slamStart = -1;
            state.leapStart = -1;
            state.chargeStart = -1;
            state.chargeHit.clear();
        }
        ShieldThrow.dropFlying(player);
        CaptainState cs = ModAttachments.CAPTAIN.get(player);
        if (!cs.equals(CaptainState.EMPTY)) {
            ModAttachments.CAPTAIN.set(player, CaptainState.EMPTY);
        }
        data.set(Live.CAP_SLAMMING, false);
        data.set(Live.CAP_CHARGING, false);
    }

    /** He died or left: what he was doing ends (his shield in flight drops where it is). */
    static void onGone(ServerPlayer player) {
        PowerData data = Powers.get(player);
        if (STATES.containsKey(player.getUUID()) || data.acts(Power.CAPTAIN_AMERICA)
                || ModAttachments.CAPTAIN.get(player).shieldEntity() >= 0) {
            endAll(player, data);
        }
        STATES.remove(player.getUUID());
    }

    /** A power data's hold-to-use bits: the slam's and the charge's (the moves end with the player). */
    static void clearData(PowerData data) {
        data.set(Live.CAP_SLAMMING, false);
        data.set(Live.CAP_CHARGING, false);
    }

    static void clear() {
        STATES.clear();
        ShieldCharge.clearStuns();
    }

    private CaptainAbilities() {
    }
}
