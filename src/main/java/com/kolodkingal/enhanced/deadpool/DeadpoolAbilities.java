package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.vormir.VormirTracker;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deadpool (3.7): Wade Wilson, the mercenary whose healing factor the Workshop forced awake, who talks to whoever is
 * holding the mouse. The plug-in is {@link DeadpoolServer}; his healing factor is the shared one of {@code healing/}.
 *
 * <p>R: the {@link Katanas} (a 3-hit combo), Sneak + R held: the Parry. G: the {@link Pistols} (pairs of cartoon shots
 * from a magazine), Sneak + G: the Trick Shot. V: the {@link TeleportBelt} (a blink that sometimes glitches), Sneak +
 * V: the Drop-In. B: the {@link Taunt} (a joke over his head; monsters near turn on him), Sneak + B: the Chimichanga
 * Break (the Healing Burst). N: {@link DogpoolEntity Dogpool}, Sneak + N: the Smoke Vanish. His lines are {@link
 * DeadpoolLines}; how he is made, the Weapon X Pod's {@link PodTreatment}.
 *
 * <p>3.4's rules from birth: every cooldown is by its row, the Sneak variant is {@code alt}, the tick's parts are
 * guarded by {@code data.has(row)}, his body is by {@link HeroAbility#DEADPOOL_BODY} and its {@code bodyScale}, the
 * colours of an ability's effects are {@link Powers#color}/{@link Powers#glow} of the player. Two keys keep a variant's
 * wait apart from the key's (the decision of the lane, in its notes): the Chimichanga Break and the Smoke Vanish wait
 * on their own clocks, so a joke never keeps him from healing and Dogpool never keeps him from vanishing; the swords
 * and the pistols fire on their own gaps, so a Parry's or a Trick Shot's wait (on R's and G's slot) never stops a cut
 * or a shot.
 */
public final class DeadpoolAbilities {
    // The body's modifiers (permanent: right when a player logs in).
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("deadpool_health");
    private static final ResourceLocation BODY_SPEED = Enhanced.id("deadpool_speed");
    private static final ResourceLocation BODY_ATTACK = Enhanced.id("deadpool_attack");
    private static final ResourceLocation BODY_ATTACK_SPEED = Enhanced.id("deadpool_attack_speed");
    private static final ResourceLocation BODY_JUMP = Enhanced.id("deadpool_jump");
    private static final ResourceLocation BODY_SAFE_FALL = Enhanced.id("deadpool_safe_fall");
    /** Kidpool's size. */
    private static final ResourceLocation BODY_SCALE = Enhanced.id("deadpool_kidpool_scale");

    /** The looks. */
    public static final String CLASSIC = "classic";
    public static final String TRAINEE = "trainee";
    public static final String KIDPOOL = "kidpool";
    public static final String OWN = "own";
    /** How much smaller Kidpool is ({@code ADD_MULTIPLIED_BASE} of the scale). */
    public static final double KIDPOOL_SCALE = -0.2;

    /** Suit red, the black of his panels, the bright gold of a tracer. */
    public static final int RED = 0xC8102E;
    public static final int BLACK = 0x1E1C22;
    public static final int TRACER = 0xFFE27A;

    /** How far the effects are sent. */
    static final double FX_RANGE = 96.0;
    /** How long (ticks) after a shot his pistols stay drawn ({@code Live.DEADPOOL_AIMING}). */
    public static final int AIM_TICKS = 40;

    /** What each Deadpool carries from tick to tick; forgotten when he leaves or the server stops. */
    private static final Map<UUID, State> STATES = new HashMap<>();

    static final class State {
        /** The katanas: the step of the combo (1-3) and when the last cut fell. */
        int combo;
        long lastCut = Long.MIN_VALUE / 2;
        /** The Parry: when it began (-1: not parrying). */
        long parryStart = -1L;
        /** The pistols: when the last pair began, the right-hand shot still to come (-1: none), until when drawn. */
        long lastPair = Long.MIN_VALUE / 2;
        long rightShotAt = -1L;
        long aimUntil = Long.MIN_VALUE / 2;
        /** The Drop-In under way, or null. */
        @Nullable
        TeleportBelt.Drop drop;
        /** No fall damage until then (the Drop-In's landing). */
        long fallGuardUntil = Long.MIN_VALUE / 2;
        /** The variants that wait on clocks of their own (see the class comment). */
        long breakReadyAt = Long.MIN_VALUE / 2;
        long smokeReadyAt = Long.MIN_VALUE / 2;
        /** The game time of his last tick: a second tick in the same game tick does nothing more. */
        long lastTick = Long.MIN_VALUE;
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

    static double cfg(ModConfigSpec.DoubleValue value, double fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    static int cfg(ModConfigSpec.IntValue value, int fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    static boolean cfg(ModConfigSpec.BooleanValue value, boolean fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    /**
     * A wait of {@code ticks} for an ability that keeps its own clock: unchanged for Deadpool, times a custom hero's
     * strength's factor and {@code custom_hero.cooldownScale} (as {@link PowerData#startCooldown} does for a slot).
     */
    static long scaled(PowerData data, HeroAbility row, int ticks) {
        if (data.power() != Power.CUSTOM || data.kit() == null) {
            return ticks;
        }
        double scale = loaded() ? CustomHeroConfig.COOLDOWN_SCALE.get() : 1.0;
        return Math.max(0, Math.round(ticks * data.kit().cooldownFactor(row) * scale));
    }

    /**
     * Whether the Sneak variant {@code variant} is his: Deadpool's always; a custom hero's when he has its parent (the
     * key router passes {@code alt} only where the binding holds the variant: the parent picked with its variant on, or
     * the variant's own card).
     */
    static boolean hasVariant(PowerData data, HeroAbility variant) {
        HeroAbility parent = variant.parent();
        return data.has(variant) || parent != null && data.has(parent);
    }

    /**
     * The row whose key slot a Sneak variant waits on: its own card's key when it has one, else its parent's (the
     * parent picked with its variant on: one key, one slot).
     */
    static HeroAbility slotRow(PowerData data, HeroAbility variant) {
        HeroAbility parent = variant.parent();
        return data.slot(variant) != 0 || parent == null ? variant : parent;
    }

    // ---- who he is ----

    /** The id of the player's look ({@code classic} for the default). */
    public static String look(PowerData data) {
        return Variants.current(data).id();
    }

    /** Whether the player is Deadpool himself (not a custom hero with some of his rows). */
    public static boolean isDeadpool(Player player) {
        return Powers.get(player).power() == Power.DEADPOOL;
    }

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his, with its Sneak variant said explicitly ({@code alt}): Deadpool's own Sneak, or the binding of a
     * custom hero with his abilities (3.4). A release ends the Parry (whatever Sneak is then).
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down) {
            if (ability == 1) {
                Katanas.release(player, data);
            }
            return;
        }
        if (player.isSpectator() || HealingFactor.isDown(player) || !player.isAlive()) {
            return; // (down: no keys, PLAN 2.4)
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    Katanas.parry(player, data);
                } else {
                    Katanas.cut(player, data);
                }
            }
            case 2 -> {
                if (alt) {
                    Pistols.trickShot(player, data);
                } else {
                    Pistols.fire(player, data);
                }
            }
            case 3 -> {
                if (alt) {
                    TeleportBelt.dropIn(player, data);
                } else {
                    TeleportBelt.blink(player, data);
                }
            }
            case 4 -> {
                if (alt) {
                    Taunt.chimichangaBreak(player, data);
                } else {
                    Taunt.taunt(player, data);
                }
            }
            case 5 -> {
                if (alt) {
                    Taunt.smoke(player, data);
                } else {
                    DogpoolEntity.whistle(player, data);
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
        if (data.has(HeroAbility.DEADPOOL_KATANAS)) {
            Katanas.tick(player, data, state, now);
        }
        if (data.has(HeroAbility.DEADPOOL_PISTOLS)) {
            Pistols.tick(player, data, state, now);
        }
        if (data.has(HeroAbility.DEADPOOL_BELT)) {
            TeleportBelt.tick(player, data, state, now);
        }
        if (data.has(HeroAbility.DEADPOOL_DOGPOOL)) {
            DogpoolEntity.ownerTick(player, data, now);
        }
        if (data.power() == Power.DEADPOOL) {
            DeadpoolLines.tick(player, data, now);
        }
    }

    // ---- the body ----

    /**
     * The body of Deadpool while the player has it (scaled by its strength for a custom hero), nothing of it on anyone
     * else; Kidpool is a fifth smaller. Health keeps its share of the maximum when the maximum changes.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean on = data.has(HeroAbility.DEADPOOL_BODY);
        double k = on ? data.bodyScale(HeroAbility.DEADPOOL_BODY) : 0.0;
        float oldMax = player.getMaxHealth();
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, cfg(DeadpoolConfig.HEALTH_BONUS, 10.0) * k, on,
                AttributeModifier.Operation.ADD_VALUE);
        float newMax = player.getMaxHealth();
        if (newMax != oldMax && oldMax > 0.0F && player.isAlive()) {
            player.setHealth(Math.max(1.0F, Math.min(newMax, player.getHealth() * newMax / oldMax)));
        }
        setModifier(player, Attributes.MOVEMENT_SPEED, BODY_SPEED, cfg(DeadpoolConfig.SPEED_BONUS, 0.15) * k, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.ATTACK_DAMAGE, BODY_ATTACK, cfg(DeadpoolConfig.ATTACK_BONUS, 2.0) * k, on,
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.ATTACK_SPEED, BODY_ATTACK_SPEED, cfg(DeadpoolConfig.ATTACK_SPEED_BONUS, 0.4) * k,
                on, AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.JUMP_STRENGTH, BODY_JUMP, cfg(DeadpoolConfig.JUMP_BONUS, 0.1) * k, on,
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, BODY_SAFE_FALL, cfg(DeadpoolConfig.SAFE_FALL, 3.0) * k, on,
                AttributeModifier.Operation.ADD_VALUE);
        boolean kid = data.power() == Power.DEADPOOL && KIDPOOL.equals(look(data));
        setModifier(player, Attributes.SCALE, BODY_SCALE, KIDPOOL_SCALE, kid,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
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

    // ---- the end of it ----

    /** He is losing the power (or a custom hero with his rows redefines, dies or leaves): everything he left ends. */
    static void endAll(ServerPlayer player, PowerData data) {
        Katanas.endParry(player, data, false);
        data.set(Live.DEADPOOL_AIMING, false);
        DogpoolEntity.sendHome(player);
        State state = STATES.remove(player.getUUID());
        if (state != null && state.drop != null) {
            state.drop = null;
        }
    }

    /** The hold-to-use abilities are let go of (death, the power lost, going DOWN): the Parry ends. */
    static void stopHold(PowerData data) {
        data.set(Live.DEADPOOL_PARRYING, false);
    }

    /** He left, died or changed dimension: his moves end; Dogpool goes home. */
    static void onGone(ServerPlayer player) {
        DogpoolEntity.sendHome(player);
        STATES.remove(player.getUUID());
        Taunt.forget(player.getUUID());
    }

    static void clear() {
        STATES.clear();
        Taunt.clear();
        DeadpoolLines.clear();
    }

    // ---- shared helpers ----

    /**
     * A blow of his: {@code amount} of {@code type} from the player (through {@code direct}, the player himself for his
     * swords), every blow landing whatever the last one did; the creature he hits is the one his Dogpool goes for.
     */
    static boolean hurt(ServerPlayer player, LivingEntity target, ResourceKey<DamageType> type, double amount,
                        @Nullable net.minecraft.world.entity.Entity direct) {
        DamageSource source = com.kolodkingal.enhanced.registry.ModDamageTypes.source(player.level(), type, player,
                direct == null ? player : direct);
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(source, (float) amount);
        if (hurt) {
            player.setLastHurtMob(target);
        }
        return hurt;
    }

    /** Pushes {@code target} away from {@code from} (a push at the Vormir Cliff is credited to him). */
    static void push(ServerPlayer player, LivingEntity target, Vec3 from, double strength) {
        if (strength <= 0.0) {
            return;
        }
        Vec3 away = target.position().subtract(from);
        target.knockback(strength, -away.x, -away.z);
        target.hurtMarked = true;
        VormirTracker.credit(target, player);
    }

    /**
     * The creatures his blades reach: within {@code reach} of his eyes, inside the cone of {@code halfAngle} degrees
     * round his look, in his sight, that he may hurt (the heroes' rule: monsters, those after him, players with PvP);
     * the nearest first, at most {@code max}.
     */
    static List<LivingEntity> inCone(ServerPlayer player, double reach, double halfAngle, int max) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(halfAngle));
        AABB box = player.getBoundingBox().inflate(reach + 1.0);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player
                && e.isAlive() && !e.isSpectator())) {
            AABB body = e.getBoundingBox();
            Vec3 nearest = new Vec3(clamp(eye.x, body.minX, body.maxX), clamp(eye.y, body.minY, body.maxY),
                    clamp(eye.z, body.minZ, body.maxZ));
            double distance = nearest.distanceTo(eye);
            if (distance > reach) {
                continue;
            }
            Vec3 to = body.getCenter().subtract(eye);
            boolean inside = distance < 0.3;
            if (!inside && to.normalize().dot(look) < cos) {
                continue;
            }
            if (!AreaTargets.mayHurt(player, e, false) || !player.hasLineOfSight(e)) {
                continue;
            }
            found.add(e);
        }
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return found.size() > max ? new ArrayList<>(found.subList(0, max)) : found;
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }

    /** One of his effects, to everyone near who sees it. */
    static void send(ServerLevel level, net.minecraft.world.entity.Entity who, int kind, List<Vec3> points, int arg) {
        List<Vec3> list = points.size() > DeadpoolFxPayload.MAX_POINTS
                ? points.subList(0, DeadpoolFxPayload.MAX_POINTS) : points;
        Net.sendToPlayersNear(level, null, who.getX(), who.getY(), who.getZ(), FX_RANGE,
                new DeadpoolFxPayload(kind, who.getId(), List.copyOf(list), arg));
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** His magazine, reload and Dogpool (the attachment: saved, synced to him). */
    static DeadpoolState data(Player player) {
        return player.getData(ModAttachments.DEADPOOL);
    }

    static void setData(Player player, DeadpoolState state) {
        if (!state.equals(data(player))) {
            player.setData(ModAttachments.DEADPOOL, state);
        }
    }

    private DeadpoolAbilities() {
    }
}
