package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.healing.HealingBurst;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wolverine (3.7): Logan, the mutant whose healing factor let Weapon X bond adamantium to his bones and his claws.
 * The plug-in is {@link WolverineServer}; his claws are {@link Claws}, the R combo and the spin {@link ClawSlash}, the
 * leaps {@link Pounce}, the Rage {@link Berserker}, the nose {@link Senses}, the climb {@link ClawClimb}, the weight of
 * his bones in water {@link Adamantium}; the damage, item and fall hooks {@link WolverineEvents}. His healing factor is
 * the shared one of {@code healing/} (lane L1): this hero calls its Healing Burst; its
 * regeneration reads the Rage's bit.
 *
 * <p>R is the Claw Slash (Sneak: the Spin Slash), G the Claws out and in (Sneak: Claw Climb), V the Pounce (Sneak: the
 * Feral Leap), B the Berserker Rage (Sneak: the Healing Burst), N the Sniff (Sneak: Track). 3.4's rules from birth:
 * every cooldown is by its row, the Sneak variant is {@code alt}, every part is guarded by {@code data.has(row)}, his
 * body is {@link HeroAbility#WOLVERINE_BODY} scaled by its {@code bodyScale}.
 */
public final class WolverineAbilities {
    // The body's modifiers (permanent: right when a player logs in).
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("wolverine_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("wolverine_armor");
    private static final ResourceLocation BODY_TOUGHNESS = Enhanced.id("wolverine_toughness");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("wolverine_knockback");
    private static final ResourceLocation BODY_SPEED = Enhanced.id("wolverine_speed");
    private static final ResourceLocation BODY_ATTACK = Enhanced.id("wolverine_attack");
    private static final ResourceLocation BODY_ATTACK_SPEED = Enhanced.id("wolverine_attack_speed");
    private static final ResourceLocation BODY_JUMP = Enhanced.id("wolverine_jump");
    private static final ResourceLocation BODY_SAFE_FALL = Enhanced.id("wolverine_safe_fall");
    /** Heavy: he swims at half strength (and walks along the bottom). */
    static final ResourceLocation BODY_SWIM = Enhanced.id("wolverine_swim");
    // The Rage's (transient: they go with it).
    static final ResourceLocation RAGE_SPEED = Enhanced.id("wolverine_rage_speed");
    static final ResourceLocation RAGE_DAMAGE = Enhanced.id("wolverine_rage_damage");
    static final ResourceLocation RAGE_KNOCKBACK = Enhanced.id("wolverine_rage_knockback");

    /** The classic suit's yellow, adamantium silver, the Rage's red. */
    public static final int YELLOW = 0xF2C230;
    public static final int SILVER = 0xD9DEE3;
    public static final int RAGE_RED = 0xC8281E;
    /** How far his effects are sent. */
    static final double FX_RANGE = 64.0;

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** What each Wolverine carries from tick to tick; forgotten when he leaves or the server stops. */
    static final class State {
        /** The game time of his last tick: a second tick in the same game tick does nothing more. */
        long lastTick = Long.MIN_VALUE;
        /** After the injection the claws come out for a moment: when they go back in (0: never). */
        long sheatheAt;
        /** The Claw Slash: the hit of the combo last struck (0-2, -1 none), when, and when the next may. */
        int combo = -1;
        long lastSlash = Long.MIN_VALUE / 2;
        long nextSlash;
        /** The leap under way (the Pounce or the Feral Leap), and when it began. */
        @Nullable
        Pounce.Leap leap;
        long leapStart;
        /** Whether his last leap ended in the stab. */
        boolean lastStruck;
        /** Until when his own leap spares him the fall's damage. */
        long fallGuardUntil;
        /** Whether the Rage was on at his last tick (its end makes him tired). */
        boolean raging;
        /** Track: the creature followed (its UUID), until when, and the last time the trail was sent. */
        @Nullable
        UUID tracked;
        long trackUntil;
        /** The last trail Track sent (for the tests), and the last time a climb's marks were shown. */
        List<Vec3> lastTrail = List.of();
        long lastClimbFx;
        /** The creatures his Pounce pinned and until when (no jump, slowed). */
        final Map<UUID, Long> pinned = new HashMap<>();
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

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his, with its Sneak variant said explicitly ({@code alt}): Wolverine's own Sneak, or the binding of a
     * custom hero with his abilities (3.4). Each move asks {@code data.has} of its own row.
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down || player.isSpectator() || !player.isAlive() || HealingFactor.isDown(player)) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    ClawSlash.spin(player, data);
                } else {
                    ClawSlash.slash(player, data);
                }
            }
            case 2 -> {
                if (alt) {
                    Claws.toggleClimb(player, data);
                } else {
                    Claws.toggle(player, data);
                }
            }
            case 3 -> {
                if (alt) {
                    Pounce.feralLeap(player, data);
                } else {
                    Pounce.pounce(player, data);
                }
            }
            case 4 -> {
                if (alt) {
                    healingBurst(player, data);
                } else {
                    Berserker.start(player, data);
                }
            }
            case 5 -> {
                if (alt) {
                    Senses.toggleTrack(player, data);
                } else {
                    Senses.sniff(player, data);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Sneak + B: the Healing Burst, the shared one of the healing factor ({@link HealingBurst}, lane L1). Its cooldown
     * is spent only when it healed (refused while poisoned: nothing spent).
     */
    public static boolean healingBurst(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.WOLVERINE_HEALING_BURST) || !data.isReady(HeroAbility.WOLVERINE_HEALING_BURST, now)) {
            return false;
        }
        if (!HealingBurst.burst(player, HealingBurst.fraction())) {
            return false;
        }
        data.startCooldown(HeroAbility.WOLVERINE_HEALING_BURST, now, HealingBurst.cooldown());
        return true;
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        State state = state(player.getUUID());
        long now = player.level().getGameTime();
        if (state.lastTick == now) {
            return; // (one tick of his per game tick, however often the player is ticked)
        }
        state.lastTick = now;
        Claws.tick(player, data, state, now);
        Pounce.tick(player, data, state, now);
        Berserker.tick(player, data, state, now);
        Senses.tick(player, data, state, now);
        ClawClimb.serverTick(player, data, state, now);
        if (data.has(HeroAbility.WOLVERINE_POUNCE)) {
            data.set(Live.WOLVERINE_POUNCING, state.leap != null && state.leap.pounce());
        }
        if (data.has(HeroAbility.WOLVERINE_BERSERKER)) {
            data.set(Live.WOLVERINE_BERSERK, Berserker.raging(player));
        }
    }

    // ---- the body ----

    /**
     * Whether the player has Wolverine's body right now (the hero, or a custom hero with {@code wolverine.body}). Both
     * sides.
     */
    public static boolean hasBody(PowerData data) {
        return data.has(HeroAbility.WOLVERINE_BODY);
    }

    /**
     * The body of Wolverine while the player has it (scaled by its strength for a custom hero), nothing of it on anyone
     * else. Health keeps its share of the maximum when the maximum changes.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean on = hasBody(data);
        if (!on && player.getAttribute(Attributes.MAX_HEALTH).getModifier(BODY_HEALTH) == null
                && player.getAttribute(Attributes.ARMOR).getModifier(BODY_ARMOR) == null) {
            return;
        }
        double k = on ? data.bodyScale(HeroAbility.WOLVERINE_BODY) : 0.0;
        float oldMax = player.getMaxHealth();
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, cfg(WolverineConfig.HEALTH_BONUS, 10.0) * k, on);
        float newMax = player.getMaxHealth();
        if (newMax != oldMax && oldMax > 0.0F && player.isAlive()) {
            player.setHealth(Math.max(1.0F, Math.min(newMax, player.getHealth() * newMax / oldMax)));
        }
        setModifier(player, Attributes.ARMOR, BODY_ARMOR, cfg(WolverineConfig.ARMOR_BONUS, 4.0) * k, on);
        setModifier(player, Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS, cfg(WolverineConfig.TOUGHNESS_BONUS, 2.0) * k,
                on);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK,
                Math.min(1.0, cfg(WolverineConfig.KNOCKBACK_RESISTANCE, 0.4) * k), on);
        setModifier(player, Attributes.MOVEMENT_SPEED, BODY_SPEED, cfg(WolverineConfig.SPEED_BONUS, 0.2) * k, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.ATTACK_DAMAGE, BODY_ATTACK, cfg(WolverineConfig.ATTACK_BONUS, 2.0) * k, on);
        setModifier(player, Attributes.ATTACK_SPEED, BODY_ATTACK_SPEED, cfg(WolverineConfig.ATTACK_SPEED_BONUS, 0.6)
                * k, on);
        setModifier(player, Attributes.JUMP_STRENGTH, BODY_JUMP, cfg(WolverineConfig.JUMP_BONUS, 0.15) * k, on);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, BODY_SAFE_FALL, cfg(WolverineConfig.SAFE_FALL, 4.0) * k, on);
        setModifier(player, NeoForgeMod.SWIM_SPEED, BODY_SWIM, -0.5, on && Adamantium.heavy(),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (!on) {
            Berserker.modifiers(player, false);
        }
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

    /** Whether the creature is a boss (it takes the damage of his moves, never their pins). */
    static boolean isBoss(LivingEntity target) {
        return target.getType().is(ModTags.BOSSES);
    }

    /** The factor of the Rage on his blows: 1, or 1 + {@code berserkDamage} while it lasts. */
    public static float rageFactor(Player player) {
        return Berserker.raging(player) ? (float) (1.0 + cfg(WolverineConfig.BERSERK_DAMAGE, 0.4)) : 1.0F;
    }

    /** Hurts the creature with a damage type of his ({@code type}), the blow always landing (the stones' rule). */
    static boolean hurt(ServerPlayer player, LivingEntity target, ResourceKey<DamageType> type, float amount) {
        return StonePowers.hurt(player.serverLevel(), player, target, type, amount, player);
    }

    /**
     * Every push of his (the cross-slash, the spin, the pounce): the shared push, which credits him for the Soul
     * Stone's sacrifice ({@code VormirTracker.credit}).
     */
    static void push(LivingEntity victim, Vec3 from, double strength, double lift, LivingEntity wolverine) {
        StonePowers.shove(victim, from, strength, lift, wolverine);
    }

    // ---- effects ----

    static void send(ServerLevel level, Entity who, int kind, List<Vec3> points, int arg) {
        List<Vec3> list = points.size() > WolverineFxPayload.MAX_POINTS ? points.subList(0,
                WolverineFxPayload.MAX_POINTS) : points;
        PacketDistributor.sendToPlayersNear(level, null, who.getX(), who.getY(), who.getZ(), FX_RANGE,
                new WolverineFxPayload(kind, who.getId(), List.copyOf(list), arg));
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ---- the end of the power, of the life, of the session ----

    /** The power is being taken away (or a custom hero is remade): every move ends, the Rage and Track too. */
    static void endAll(ServerPlayer player, PowerData data) {
        State state = STATES.get(player.getUUID());
        if (state != null) {
            Pounce.end(player, state);
            Senses.endTrack(player, data, state, false);
            state.combo = -1;
            state.sheatheAt = 0;
            state.raging = false;
        }
        if (Berserker.raging(player)) {
            player.setData(ModAttachments.WOLVERINE, Berserker.get(player).withBerserkUntil(0L));
        }
        Berserker.modifiers(player, false);
        data.set(Live.WOLVERINE_POUNCING, false);
        data.set(Live.WOLVERINE_BERSERK, false);
        data.setToggle(Toggle.CLAW_CLIMB, false);
        data.setToggle(Toggle.SCENT_TRACK, false);
    }

    /** He died or left: what he was doing ends. */
    static void onGone(ServerPlayer player) {
        PowerData data = Powers.get(player);
        if (STATES.containsKey(player.getUUID()) || data.acts(com.kolodkingal.enhanced.power.Power.WOLVERINE)) {
            endAll(player, data);
        }
        STATES.remove(player.getUUID());
    }

    /** A power data's hold-to-use bits: his leap's flag (the moves end with the player). */
    static void clearData(PowerData data) {
        data.set(Live.WOLVERINE_POUNCING, false);
    }

    static void clear() {
        STATES.clear();
        Senses.clear();
    }

    private WolverineAbilities() {
    }
}
