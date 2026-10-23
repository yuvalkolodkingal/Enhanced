package com.kolodkingal.enhanced.ironman;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.vormir.VormirTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
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
 * Iron Man (3.8): Tony Stark, who is nothing without a suit and everything in one. The plug-in is {@link
 * IronManServer}; the suits are {@link IronSuits}, H is {@link SuitControl}, the suit-up {@link SuitPhases}, the meter
 * {@link SuitEnergy}; each suit's keys are its {@link SuitMoves} (lane L3 the Mark 1, 3 and 5, lane L4 the nanotech
 * suits and the Hulkbuster).
 *
 * <p>Unsuited he is Tony: no body, and every key but G and H says "You need a suit." In a suit that works ({@link
 * IronSuits#powered}) R, V, B and N are the suit's, G is flight (the Mark 1's Rocket Boost); a suit that does not work
 * says why (no reactor, broken). A custom hero with Iron Man's rows has the Mark 85's moves and the suit energy meter,
 * and his {@link HeroAbility#IRON_MAN_SUIT} body row gives him the armour of a suit (no item).
 */
public final class IronManAbilities {
    // The body's modifiers: transient, set each tick from the suit he wears (nothing is saved on him).
    private static final ResourceLocation SUIT_ARMOR = Enhanced.id("iron_suit_armor");
    private static final ResourceLocation SUIT_TOUGHNESS = Enhanced.id("iron_suit_toughness");
    private static final ResourceLocation SUIT_KNOCKBACK = Enhanced.id("iron_suit_knockback");
    private static final ResourceLocation SUIT_SPEED = Enhanced.id("iron_suit_speed");
    private static final ResourceLocation PHASE_ROOT = Enhanced.id("iron_suit_phase_root");
    private static final ResourceLocation PHASE_JUMP = Enhanced.id("iron_suit_phase_jump");
    /** A custom hero's "suit" (the {@code iron_man.suit} body row): permanent, like the other heroes' bodies. */
    private static final ResourceLocation CUSTOM_ARMOR = Enhanced.id("iron_custom_suit_armor");
    private static final ResourceLocation CUSTOM_TOUGHNESS = Enhanced.id("iron_custom_suit_toughness");

    /** Suit red, the arc reactor's blue, the gold of the Mark 3. */
    public static final int RED = 0xB3121B;
    public static final int REACTOR_BLUE = 0x9FE8FF;
    public static final int GOLD = 0xE8B83A;

    /** How far his effects are sent. */
    static final double FX_RANGE = 96.0;
    /** A dive faster than this (blocks a tick, downwards) that ends on the ground is a hard landing. */
    static final double HARD_LANDING_SPEED = 0.7;
    /** A custom hero's suit: +8 armour, +4 toughness. */
    public static final double CUSTOM_SUIT_ARMOR = 8.0;
    public static final double CUSTOM_SUIT_TOUGHNESS = 4.0;

    /** What each Iron Man carries from tick to tick (not saved). */
    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();

    static final class Tracked {
        /** The suit and whether it worked last tick (a change ends the old suit's moves). */
        @Nullable
        SuitKind suit;
        boolean powered;
        /** His fall speed last tick while flying (a hard landing). */
        double fallSpeed;
        boolean flying;
        long lastTick = Long.MIN_VALUE;
        /** The next time the thrusters' loop sounds. */
        long thrusterAt;
    }

    static Tracked tracked(UUID id) {
        return TRACKED.computeIfAbsent(id, k -> new Tracked());
    }

    // ---- the numbers ----

    static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    public static double cfg(@Nullable ModConfigSpec.DoubleValue value, double fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    public static int cfg(@Nullable ModConfigSpec.IntValue value, int fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    public static boolean cfg(@Nullable ModConfigSpec.BooleanValue value, boolean fallback) {
        return loaded() && value != null ? value.get() : fallback;
    }

    // ---- who he is ----

    /** Whether the player is Iron Man himself (not a custom hero with some of his rows). */
    public static boolean isIronMan(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player p && Powers.power(p) == Power.IRON_MAN;
    }

    /** Whether a custom hero has any of Iron Man's rows (the Mark 85's moves and the meter). */
    static boolean customIron(PowerData data) {
        return data.power() == Power.CUSTOM && data.acts(Power.IRON_MAN);
    }

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his. Releases always reach the worn suit's moves (a held move ends). G is flight unless the suit owns
     * it; every other key needs a suit that works, and then it is the suit's.
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (data.power() == Power.CUSTOM) {
            SuitMoves moves = SuitMoves.of(SuitKind.MARK_85);
            if (moves != null) {
                moves.onKey(player, data, ability, down, alt);
            }
            return;
        }
        SuitKind worn = IronSuits.worn(player);
        SuitMoves moves = worn == null ? null : SuitMoves.of(worn);
        if (!down) {
            if (moves != null) {
                moves.onKey(player, data, ability, false, alt);
            }
            return;
        }
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        if (ability == 2 && (moves == null || !moves.ownsG())) {
            if (player.mayFly()) {
                PowerTicker.toggleFlight(player);
            } else {
                refuse(player, worn, true);
            }
            return;
        }
        if (worn == null) {
            say(player, "message.enhanced.iron_man.no_suit");
            return;
        }
        if (IronSuits.state(player).inPhase()) {
            return;
        }
        if (!IronSuits.powered(player)) {
            refuse(player, worn, false);
            return;
        }
        if (moves == null) {
            say(player, "message.enhanced.iron_man.not_yet");
            return;
        }
        moves.onKey(player, data, ability, true, alt);
    }

    /** Why the suit does not answer: no suit, no reactor, broken, no energy, or (G) a suit that does not fly. */
    private static void refuse(ServerPlayer player, @Nullable SuitKind worn, boolean flight) {
        if (worn == null) {
            say(player, "message.enhanced.iron_man.no_suit");
        } else if (IronSuits.state(player).inPhase()) {
            say(player, "message.enhanced.iron_man.settling");
        } else if (!IronSuits.reactorFor(player, worn)) {
            say(player, "message.enhanced.iron_man.no_reactor");
        } else if (IronSuits.broken(player)) {
            say(player, "message.enhanced.iron_man.offline");
        } else if (flight && !worn.flies()) {
            say(player, "message.enhanced.iron_man.cannot_fly");
        } else if (SuitEnergy.empty(player)) {
            SuitEnergy.refuse(player);
        }
    }

    static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    // ---- every tick ----

    /**
     * His tick: the chest watched (a suit put on by hand, a nanotech suit whose reactor went), the phase, whether the
     * suit works (the moves of a suit that stopped end), the suit's own tick, flight's cost, the meter's recharge.
     */
    static void tick(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        Tracked t = tracked(player.getUUID());
        if (t.lastTick == now) {
            return;
        }
        t.lastTick = now;
        if (data.power() == Power.CUSTOM) {
            if (customIron(data)) {
                SuitMoves moves = SuitMoves.of(SuitKind.MARK_85);
                if (moves != null) {
                    moves.tick(player, data, now);
                }
                SuitEnergy.tick(player, now);
            }
            return;
        }
        if (data.power() != Power.IRON_MAN) {
            return;
        }
        SuitControl.watch(player);
        SuitPhases.tick(player, now);
        SuitKind worn = IronSuits.worn(player);
        boolean powered = IronSuits.powered(player);
        if (t.suit != null && (t.suit != worn || t.powered && !powered)) {
            endMoves(player, t.suit);
            if (t.suit == worn && IronSuits.broken(player)) {
                send(player.serverLevel(), player, IronFxPayload.OFFLINE, List.of(player.position()), worn.ordinal());
                say(player, "message.enhanced.iron_man.offline");
            }
        }
        t.suit = worn;
        t.powered = powered;
        IronManState state = IronSuits.state(player);
        byte suitByte = worn == null ? SuitKind.NONE : worn.asByte();
        if (state.suit() != suitByte || state.has(IronManState.POWERED) != powered) {
            IronManState next = state.with(IronManState.POWERED, powered);
            next = new IronManState(suitByte, next.energy(), next.phase(), next.phaseSuit(), next.phaseStart(),
                    next.phaseTicks(), next.nanoSuit(), next.lastSuit(), next.flags(), next.dronesUntil());
            IronSuits.setState(player, next);
        }
        if (powered && worn != null) {
            SuitMoves moves = SuitMoves.of(worn);
            if (moves != null) {
                moves.tick(player, data, now);
            }
            flightTick(player, worn, t, now);
        } else {
            t.flying = false;
            t.fallSpeed = 0.0;
        }
        SuitEnergy.tick(player, now);
    }

    /** Flight's cost a tick, the thrusters' loop, and a hard landing that the suit takes for him. */
    private static void flightTick(ServerPlayer player, SuitKind worn, Tracked t, long now) {
        boolean flying = player.getAbilities().flying;
        if (flying) {
            if (!SuitEnergy.drain(player, IronSuits.flightCost(worn))) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            if (now >= t.thrusterAt) {
                t.thrusterAt = now + 30;
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        com.kolodkingal.enhanced.registry.ModSounds.IRON_THRUSTERS.get(), SoundSource.PLAYERS, 0.35F,
                        worn == SuitKind.HULKBUSTER ? 0.7F : 1.0F);
            }
        }
        double down = -player.getDeltaMovement().y;
        if (t.flying && t.fallSpeed > HARD_LANDING_SPEED && player.onGround()) {
            hardLanding(player);
        }
        t.flying = flying;
        t.fallSpeed = flying ? down : 0.0;
    }

    /** A fast dive into the ground: the suit takes it ({@code hardLandingDurability}), a puff of dust. */
    private static void hardLanding(ServerPlayer player) {
        wear(player, cfg(IronManConfig.HARD_LANDING_DURABILITY, 5));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.1, player.getZ(), 12, 0.6, 0.05, 0.6,
                0.05);
    }

    /** Wears his suit by {@code amount} (never breaking the item: at 0 it is "offline"). */
    public static void wear(ServerPlayer player, int amount) {
        if (amount <= 0 || player.isCreative()) {
            return;
        }
        net.minecraft.world.item.ItemStack stack = IronSuits.durabilityStack(player);
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return;
        }
        int max = stack.getMaxDamage() - 1;
        stack.setDamageValue(Math.min(max, stack.getDamageValue() + amount));
    }

    /** The moves of a suit end (it was taken off, stopped working, or a phase began). */
    static void endMoves(ServerPlayer player, SuitKind kind) {
        SuitMoves moves = SuitMoves.of(kind);
        PowerData data = Powers.get(player);
        if (moves != null) {
            moves.end(player, data);
        }
        com.kolodkingal.enhanced.beam.ContinuousBeam.stop(player);
        data.set(Live.IRON_DASHING, false);
        IronManState state = IronSuits.state(player);
        if (state.has(IronManState.BRACING)) {
            IronSuits.setState(player, state.with(IronManState.BRACING, false));
        }
    }

    // ---- the body ----

    /**
     * Every player, whenever the passives are applied: the worn suit's armour (a nanotech suit's, from the config, as
     * the classic items carry theirs), its walk speed, the root of a phase; a custom hero's suit row. None of it on
     * anyone else.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean iron = data.power() == Power.IRON_MAN;
        SuitKind worn = iron ? IronSuits.worn(player) : null;
        boolean nano = worn != null && worn.nanotech();
        double armor = 0.0;
        double toughness = 0.0;
        double knockback = 0.0;
        if (nano) {
            boolean m85 = worn == SuitKind.MARK_85;
            armor = m85 ? cfg(IronManConfig.MARK85_ARMOR, 20.0) : cfg(IronManConfig.MARK50_ARMOR, 20.0);
            toughness = m85 ? cfg(IronManConfig.MARK85_TOUGHNESS, 12.0) : cfg(IronManConfig.MARK50_TOUGHNESS, 8.0);
            knockback = m85 ? cfg(IronManConfig.MARK85_KNOCKBACK_RESISTANCE, 0.4)
                    : cfg(IronManConfig.MARK50_KNOCKBACK_RESISTANCE, 0.2);
        }
        setTransient(player, Attributes.ARMOR, SUIT_ARMOR, armor, nano, AttributeModifier.Operation.ADD_VALUE);
        setTransient(player, Attributes.ARMOR_TOUGHNESS, SUIT_TOUGHNESS, toughness, nano,
                AttributeModifier.Operation.ADD_VALUE);
        setTransient(player, Attributes.KNOCKBACK_RESISTANCE, SUIT_KNOCKBACK, knockback, nano,
                AttributeModifier.Operation.ADD_VALUE);
        double speed = worn == null ? 0.0 : walkSpeed(worn);
        setTransient(player, Attributes.MOVEMENT_SPEED, SUIT_SPEED, speed, worn != null && speed != 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        IronManState state = iron ? IronSuits.state(player) : IronManState.EMPTY;
        SuitKind phase = state.phaseKind();
        boolean rooted = phase != null && !(phase.nanotech() && state.phase() == IronManState.UP);
        double root = phase == null ? 0.0 : rooted ? -1.0 : -0.5;
        setTransient(player, Attributes.MOVEMENT_SPEED, PHASE_ROOT, root, phase != null,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setTransient(player, Attributes.JUMP_STRENGTH, PHASE_JUMP, -1.0, phase != null,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        // (A custom hero's suit row: armour all over and the meter; Iron Man himself has his suits.)
        boolean custom = data.power() == Power.CUSTOM && data.has(HeroAbility.IRON_MAN_SUIT);
        double k = custom ? data.bodyScale(HeroAbility.IRON_MAN_SUIT) : 0.0;
        setPermanent(player, Attributes.ARMOR, CUSTOM_ARMOR, CUSTOM_SUIT_ARMOR * k, custom);
        setPermanent(player, Attributes.ARMOR_TOUGHNESS, CUSTOM_TOUGHNESS, CUSTOM_SUIT_TOUGHNESS * k, custom);
        Mark1Moves.applyBrace(player, iron && state.has(IronManState.BRACING));
    }

    /** The walk speed a suit adds (a share of the base: the Mark 1 is a fifth slower, the Mark 85 a tenth faster). */
    static double walkSpeed(SuitKind kind) {
        return switch (kind) {
            case MARK_1 -> -cfg(IronManConfig.MARK1_SPEED_PENALTY, 0.20);
            case MARK_3 -> 0.0;
            case MARK_5 -> cfg(IronManConfig.MARK5_SPEED_BONUS, 0.05);
            case MARK_50 -> cfg(IronManConfig.MARK50_SPEED_BONUS, 0.05);
            case MARK_85 -> cfg(IronManConfig.MARK85_SPEED_BONUS, 0.10);
            case HULKBUSTER -> -cfg(IronManConfig.HULKBUSTER_SPEED_PENALTY, 0.10);
        };
    }

    static void setTransient(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount,
                             boolean present, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount || existing.operation() != operation) {
                instance.removeModifier(id);
                instance.addTransientModifier(new AttributeModifier(id, amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    private static void setPermanent(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id,
                                     double amount, boolean present) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount,
                        AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    // ---- the end of it ----

    /**
     * He is losing the power (or a custom hero redefines): the moves end, a phase completes, a nanotech suit retracts,
     * the suit stops being lit. The items stay where they are.
     */
    static void endAll(ServerPlayer player, PowerData data) {
        if (data.power() == Power.CUSTOM) {
            SuitMoves moves = SuitMoves.of(SuitKind.MARK_85);
            if (moves != null) {
                moves.end(player, data);
            }
            return;
        }
        SuitKind worn = IronSuits.worn(player);
        if (worn != null) {
            endMoves(player, worn);
        }
        SuitPhases.complete(player, true);
        SuitControl.retractNow(player);
        IronManState state = IronSuits.state(player);
        IronSuits.setState(player, state.with(IronManState.POWERED, false).with(IronManState.BRACING, false)
                .withSuit(null));
        data.set(Live.IRON_SUITING, false);
        TRACKED.remove(player.getUUID());
    }

    /** The held moves are let go of (death, the power lost). */
    static void stopHold(PowerData data) {
        data.set(Live.IRON_DASHING, false);
    }

    /**
     * He died, logged out or changed dimension: a phase completes silently, a nanotech suit retracts (it is the
     * reactor's), the moves end. (On death the state is kept: his selected suits.)
     */
    static void onGone(ServerPlayer player) {
        if (Powers.power(player) == Power.IRON_MAN || IronSuits.state(player).inPhase()
                || IronSuits.state(player).has(IronManState.NANO_OUT)) {
            SuitKind worn = IronSuits.worn(player);
            if (worn != null) {
                endMoves(player, worn);
            }
            SuitPhases.complete(player, true);
            SuitControl.retractNow(player);
            IronSuits.setState(player, IronSuits.state(player).with(IronManState.POWERED, false)
                    .with(IronManState.BRACING, false));
        }
        TRACKED.remove(player.getUUID());
    }

    static void forget(ServerPlayer player) {
        onGone(player);
        SuitEnergy.forget(player.getUUID());
        SuitPhases.forget(player.getUUID());
        Mark1Moves.forget(player.getUUID());
        Mark3Moves.forget(player.getUUID());
        Mark5Moves.forget(player.getUUID());
    }

    static void clear() {
        TRACKED.clear();
        SuitEnergy.clear();
        SuitPhases.clear();
        Mark1Moves.clear();
        Mark3Moves.clear();
        Mark5Moves.clear();
    }

    // ---- shared helpers of the moves ----

    /** A cooldown on a key's slot (the HUD shows it): 1 R, 2 G, 3 V, 4 B, 5 N. */
    static void cooldown(PowerData data, int slot, long now, int ticks) {
        data.startCooldown(slot, now, ticks);
    }

    static boolean ready(PowerData data, int slot, long now) {
        return data.isReady(slot, now);
    }

    /**
     * A blow of his: {@code amount} of {@code type} from him (through {@code direct}), every blow landing whatever the
     * last one did; players only with PvP (the heroes' rule, {@link AreaTargets#mayHurt}).
     */
    public static boolean hurt(ServerPlayer player, LivingEntity target, ResourceKey<DamageType> type, double amount,
                               @Nullable Entity direct) {
        if (!AreaTargets.mayHurt(player, target, false)) {
            return false;
        }
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
        target.knockback(strength, -away.x, -away.z);
        target.hurtMarked = true;
        VormirTracker.credit(target, player);
    }

    /**
     * A small boom at {@code at} (PLAN 5.7): {@code damage} to every creature he may hurt within {@code radius}, a
     * push, the flash and the bang; blocks broken only with {@code [iron_man] boomsBreakBlocks} and mobGriefing (both
     * off by default).
     *
     * @param power the explosion's power, for the push and (when allowed) the blocks
     * @param spare a creature the boom does not hurt again (null: none)
     */
    static void boom(ServerPlayer player, Vec3 at, double power, double radius, double damage,
                     ResourceKey<DamageType> type, @Nullable Entity direct, @Nullable LivingEntity spare) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(power >= 1.0 ? ParticleTypes.EXPLOSION : ParticleTypes.SMALL_FLAME, at.x, at.y, at.z,
                power >= 1.0 ? 1 : 10, 0.15, 0.15, 0.15, 0.05);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.03);
        sound(level, at, com.kolodkingal.enhanced.registry.ModSounds.MINI_MISSILE_BOOM.get(), 0.9F,
                0.9F + level.random.nextFloat() * 0.2F);
        if (damage > 0.0) {
            AABB box = new AABB(at, at).inflate(radius);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive()
                    && e != spare && e.getBoundingBox().distanceToSqr(at) <= radius * radius)) {
                if (AreaTargets.mayHurt(player, e, false) && hurt(player, e, type, damage, direct)) {
                    push(player, e, at, 0.3 * power);
                }
            }
        }
        if (cfg(IronManConfig.BOOMS_BREAK_BLOCKS, false)
                && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            level.explode(direct == null ? player : direct, at.x, at.y, at.z, (float) power,
                    Level.ExplosionInteraction.TNT);
        }
    }

    /**
     * The creatures in the cone before him: within {@code reach} of his eyes, within {@code halfAngle} degrees of his
     * look, in his sight, that he may hurt; the nearest first, at most {@code max}.
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

    /** Where a palm is (the right hand unless {@code left}), a little before him at chest height. */
    static Vec3 palm(ServerPlayer player, boolean left) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 side = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw)).scale(left ? -0.38 : 0.38);
        return player.getEyePosition().add(0, -0.45, 0).add(side).add(player.getLookAngle().scale(0.5));
    }

    /** Where his chest's reactor is. */
    static Vec3 chest(ServerPlayer player) {
        return player.position().add(0, player.getBbHeight() * 0.68, 0).add(player.getLookAngle().multiply(0.3, 0.0,
                0.3));
    }

    /** One of his effects, to everyone near who sees it. */
    public static void send(ServerLevel level, Entity who, int kind, List<Vec3> points, int arg) {
        List<Vec3> list = points.size() > IronFxPayload.MAX_POINTS ? points.subList(0, IronFxPayload.MAX_POINTS)
                : points;
        Net.sendToPlayersNear(level, null, who.getX(), who.getY(), who.getZ(), FX_RANGE,
                new IronFxPayload(kind, who.getId(), List.copyOf(list), arg));
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private IronManAbilities() {
    }
}
