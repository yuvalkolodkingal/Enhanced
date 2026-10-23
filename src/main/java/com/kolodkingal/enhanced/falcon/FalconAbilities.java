package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.config.FalconConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.vormir.VormirTracker;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Falcon (3.8, PLAN 8): Sam Wilson with the Exo-Wings on his back. The plug-in is {@link FalconServer}; the wings'
 * rules (flight, the glide, the elytra) are {@link FalconWings}.
 *
 * <p>R: the {@link WristWeapons} (a pair of energy darts), Sneak + R: the Wrist Rocket. G: flight with the wings,
 * Sneak + G: the Glide ({@link FalconWings#toggleGlide}). V: the {@link Dive} onto a foe from the air, Sneak + V: the
 * Boost Climb. B held: the {@link WingShield}, Sneak + B: the Wing Slash. N: {@link Redwing}, his drone, Sneak + N:
 * Redwing Strike. Without the wings on his chest every wing move and Redwing say "Put on your Exo-Wings!"; only the
 * wrist weapons work.
 *
 * <p>3.4's rules from birth: every cooldown is by its row, the Sneak variant is {@code alt}, the tick's parts are
 * guarded by {@code data.has(row)}, his body is by {@link HeroAbility#FALCON_BODY} and its {@code bodyScale}. A custom
 * hero with his rows has the wings drawn by the rows themselves (no item, PLAN 11.1). The darts fire on a gap of their
 * own ({@code dartGap}), so the Wrist Rocket's wait (on R's slot) never stops a dart.
 */
public final class FalconAbilities {
    // The body's modifiers (permanent: right when a player logs in).
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("falcon_health");
    private static final ResourceLocation BODY_SPEED = Enhanced.id("falcon_speed");
    /** On the ground behind the Wing Shield he only shuffles. */
    private static final ResourceLocation SHIELD_SLOW = Enhanced.id("falcon_wing_shield_slow");
    static final double SHIELD_SLOW_AMOUNT = -0.6;

    /** The looks. */
    public static final String ENDGAME = "endgame";
    public static final String WINTER_SOLDIER = "winter_soldier";

    /** Wing steel, the red of his stripes and darts, the hot orange of his nozzles. */
    public static final int STEEL = 0x8A8F98;
    public static final int RED = 0xE0313A;
    public static final int FLAME = 0xFFB040;

    /** How far the effects are sent. */
    static final double FX_RANGE = 96.0;
    /** {@link FalconState#flags()}: his awakening's sound has been heard (once each time he becomes Falcon). */
    static final int AWAKENED = 1;

    /** What each Falcon carries from tick to tick; forgotten when he leaves or the server stops. */
    private static final Map<UUID, State> STATES = new HashMap<>();

    static final class State {
        /** The darts: when the last pair began, and the right-hand dart still to come (-1: none). */
        long lastPair = Long.MIN_VALUE / 2;
        long rightDartAt = -1L;
        /** The Dive or the Boost Climb under way, or null. */
        @Nullable
        Dive.Flight flight;
        /** No fall damage until then (the end of a dive or a climb). */
        long fallGuardUntil = Long.MIN_VALUE / 2;
        /** The Wing Shield: when it went up (-1: down). */
        long shieldStart = -1L;
        /** The glide: when he asked for it from the ground (he hops first), -1: not waiting. */
        long glideAsked = -1L;
        /** The last boost of the nozzles while gliding. */
        long lastBoost = Long.MIN_VALUE / 2;
        /** The creatures Redwing marked for him: id → the game time the mark ends. */
        final Map<Integer, Long> marks = new HashMap<>();
        /** The last time Redwing marked (every {@link Redwing#MARK_EVERY}) and zapped. */
        long lastMark = Long.MIN_VALUE / 2;
        long lastZap = Long.MIN_VALUE / 2;
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

    /**
     * A wait of {@code ticks} for an ability that keeps its own clock: unchanged for Falcon, times a custom hero's
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
     * Whether the Sneak variant {@code variant} is his: Falcon's always; a custom hero's when he has it or its parent
     * (the key router passes {@code alt} only where the binding holds the variant).
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

    /** Whether the player is Falcon himself (not a custom hero with some of his rows). */
    public static boolean isFalcon(Player player) {
        return Powers.get(player).power() == Power.FALCON;
    }

    /**
     * Whether the player has any of Falcon's rows (the hero, or a custom hero with one of them): his wings are drawn.
     */
    public static boolean anyRow(PowerData data) {
        return data.acts(Power.FALCON);
    }

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his, with its Sneak variant said explicitly ({@code alt}): Falcon's own Sneak, or the binding of a
     * custom hero with his abilities (3.4). A release lowers the Wing Shield (whatever Sneak is then).
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down) {
            if (ability == 4) {
                WingShield.lower(player, data);
            }
            return;
        }
        if (player.isSpectator() || HealingFactor.isDown(player) || !player.isAlive()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    WristWeapons.rocket(player, data);
                } else {
                    WristWeapons.fire(player, data);
                }
            }
            case 2 -> {
                // (Falcon's own G: a custom hero's flight is his Flight card's.)
                if (data.power() != Power.FALCON || !FalconWings.ready(player, data, true)) {
                    return;
                }
                if (alt) {
                    FalconWings.toggleGlide(player, data);
                } else {
                    FalconWings.endGlide(player, false);
                    PowerTicker.toggleFlight(player);
                }
            }
            case 3 -> {
                if (!FalconWings.ready(player, data, true)) {
                    return;
                }
                if (alt) {
                    Dive.climb(player, data);
                } else {
                    Dive.dive(player, data);
                }
            }
            case 4 -> {
                if (!FalconWings.ready(player, data, true)) {
                    return;
                }
                if (alt) {
                    WingShield.slash(player, data);
                } else {
                    WingShield.raise(player, data);
                }
            }
            case 5 -> {
                if (!FalconWings.ready(player, data, true)) {
                    return;
                }
                if (alt) {
                    Redwing.strike(player, data);
                } else {
                    Redwing.toggle(player, data);
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
        awaken(player, data);
        if (data.has(HeroAbility.FALCON_BLASTERS)) {
            WristWeapons.tick(player, data, state, now);
        }
        if (data.has(HeroAbility.FALCON_DIVE)) {
            Dive.tick(player, data, state, now);
        }
        if (data.has(HeroAbility.FALCON_WING_SHIELD)) {
            WingShield.tick(player, data, state, now);
        }
        if (data.has(HeroAbility.FALCON_REDWING)) {
            Redwing.ownerTick(player, data, state, now);
        }
        FalconWings.tick(player, data, state, now);
    }

    /**
     * The first tick of a new Falcon: the wings' whoosh of his awakening ({@code falcon_awaken}), once; not when he
     * logs in or comes back from death as Falcon (the flag is set quietly then).
     */
    static void awaken(ServerPlayer player, PowerData data) {
        FalconState fs = data(player);
        if (data.power() != Power.FALCON || (fs.flags() & AWAKENED) != 0) {
            return;
        }
        setData(player, fs.withFlags(fs.flags() | AWAKENED));
        if (player.tickCount > 40) {
            sound(player.serverLevel(), player.position(), ModSounds.FALCON_AWAKEN.get(), 1.2F, 1.0F);
        }
    }

    /** He came back from death (or logged in) as Falcon: no awakening sound. */
    static void quietAwaken(ServerPlayer player) {
        FalconState fs = data(player);
        if (Powers.get(player).power() == Power.FALCON && (fs.flags() & AWAKENED) == 0) {
            setData(player, fs.withFlags(fs.flags() | AWAKENED));
        }
    }

    // ---- the body ----

    /**
     * The body of Falcon while the player has it (scaled by its strength for a custom hero), nothing of it on anyone
     * else; and the shuffle behind the Wing Shield on the ground. Health keeps its share of the maximum when the
     * maximum changes.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean on = data.has(HeroAbility.FALCON_BODY);
        double k = on ? data.bodyScale(HeroAbility.FALCON_BODY) : 0.0;
        float oldMax = player.getMaxHealth();
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, cfg(FalconConfig.HEALTH_BONUS, 4.0) * k, on,
                AttributeModifier.Operation.ADD_VALUE);
        float newMax = player.getMaxHealth();
        if (newMax != oldMax && oldMax > 0.0F && player.isAlive()) {
            player.setHealth(Math.max(1.0F, Math.min(newMax, player.getHealth() * newMax / oldMax)));
        }
        setModifier(player, Attributes.MOVEMENT_SPEED, BODY_SPEED, cfg(FalconConfig.SPEED_BONUS, 0.10) * k, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        boolean shuffling = data.is(Live.FALCON_WING_SHIELD) && data.has(HeroAbility.FALCON_WING_SHIELD)
                && !player.getAbilities().flying;
        setModifier(player, Attributes.MOVEMENT_SPEED, SHIELD_SLOW, SHIELD_SLOW_AMOUNT, shuffling,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
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
        WingShield.end(player, data, false);
        data.set(Live.FALCON_DIVING, false);
        data.set(Live.FALCON_GLIDING, false);
        Redwing.recall(player, data, false);
        FalconState fs = data(player);
        if ((fs.flags() & AWAKENED) != 0) {
            setData(player, fs.withFlags(fs.flags() & ~AWAKENED));
        }
        State state = STATES.remove(player.getUUID());
        if (state != null && state.flight != null) {
            state.flight = null;
            player.fallDistance = 0.0F;
        }
    }

    /** The hold-to-use abilities are let go of (death, the power lost, going DOWN): the Wing Shield comes down. */
    static void stopHold(PowerData data) {
        data.set(Live.FALCON_WING_SHIELD, false);
    }

    /** He left, died or changed dimension: his moves end; Redwing flies home into the pack. */
    static void onGone(ServerPlayer player) {
        PowerData data = Powers.get(player);
        Redwing.recall(player, data, false);
        data.set(Live.FALCON_WING_SHIELD, false);
        data.set(Live.FALCON_DIVING, false);
        STATES.remove(player.getUUID());
    }

    static void clear() {
        STATES.clear();
    }

    // ---- shared helpers ----

    /**
     * A blow of his: {@code amount} of {@code type} from the player (through {@code direct}, the player himself for
     * the dive), every blow landing whatever the last one did.
     */
    static boolean hurt(ServerPlayer player, LivingEntity target, ResourceKey<DamageType> type, double amount,
                        @Nullable Entity direct) {
        DamageSource source = ModDamageTypes.source(player.level(), type, player, direct == null ? player : direct);
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
        if (away.horizontalDistanceSqr() < 1.0E-6) {
            away = player.getLookAngle();
        }
        target.knockback(strength, -away.x, -away.z);
        target.hurtMarked = true;
        VormirTracker.credit(target, player);
    }

    /** Whether his attacks may hurt {@code target} (the heroes' rule; never his own Redwing). */
    static boolean mayHurt(ServerPlayer player, LivingEntity target) {
        if (target instanceof RedwingEntity drone && player.getUUID().equals(drone.ownerId())) {
            return false;
        }
        return target.isAlive() && !target.isSpectator() && AreaTargets.mayHurt(player, target, false);
    }

    /** "Put on your Exo-Wings!" and the like, over his hotbar. */
    static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    /** One of his effects, to everyone near who sees it. */
    static void send(ServerLevel level, Entity who, int kind, List<Vec3> points, int arg) {
        List<Vec3> list = points.size() > FalconFxPayload.MAX_POINTS
                ? points.subList(0, FalconFxPayload.MAX_POINTS) : points;
        Net.sendToPlayersNear(level, null, who.getX(), who.getY(), who.getZ(), FX_RANGE,
                new FalconFxPayload(kind, who.getId(), List.copyOf(list), arg));
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** His Redwing's time and repairs (the attachment: saved, synced to him). */
    static FalconState data(Player player) {
        return ModAttachments.FALCON.get(player);
    }

    static void setData(Player player, FalconState state) {
        if (!state.equals(data(player))) {
            ModAttachments.FALCON.set(player, state);
        }
    }

    private FalconAbilities() {
    }
}
