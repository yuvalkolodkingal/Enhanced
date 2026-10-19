package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.custom.CustomHeroServer;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StonePowers;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Hulk (3.6): Bruce Banner, whose gamma accident made him the green giant. One hero with two states: the human
 * Banner (no powers at all) and the Hulk, in one of two looks, the gladiator of Thor: Ragnarok or the bigger savage
 * Hulk of Spider-Man: Brand New Day (every damage x {@code bndDamageFactor}). The plug-in is {@link HulkServer}; the
 * looks and the transformation are {@link HulkForms}, the meters {@link HulkRage}, the blocks he breaks {@link
 * HulkTerrain}, the damage hooks {@link HulkEvents}.
 *
 * <p>R is Hulk Smash (a ring that runs out from his feet and hurts everyone on the ground), Sneak + R the Grab Smash
 * ({@link GrabSmash}); G the Thunder Clap ({@link ThunderClap}), Sneak + G the Roar; V the Super Leap, Sneak + V the
 * Bound ({@link SuperLeap}); B the Boulder Throw ({@link HulkBoulderEntity}), Sneak + B the Gamma Charge ({@link
 * GammaCharge}); N the Transform (Banner and the Hulk), Sneak + N Switch Hulk. As Banner every key but N is refused.
 *
 * <p>3.4's rules from birth: every cooldown is by its row, the Sneak variant is {@code alt}, the tick's parts are
 * guarded by {@code data.has(row)}, his body is by {@link HeroAbility#HULK_BODY} and its {@code bodyScale}, the colours
 * of an ability's effects are {@link Powers#color}/{@link Powers#glow} of the player. A custom hero's N is the Human
 * Form ({@link Toggle#HUMAN_FORM}): every power off until N again.
 */
public final class HulkAbilities {
    // The body's modifiers (permanent: right when a player logs in).
    private static final ResourceLocation BODY_SCALE = Enhanced.id("hulk_scale");
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("hulk_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("hulk_armor");
    private static final ResourceLocation BODY_TOUGHNESS = Enhanced.id("hulk_toughness");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("hulk_knockback");
    private static final ResourceLocation BODY_ATTACK = Enhanced.id("hulk_attack");
    private static final ResourceLocation BODY_ATTACK_KNOCKBACK = Enhanced.id("hulk_attack_knockback");
    private static final ResourceLocation BODY_REACH = Enhanced.id("hulk_reach");
    private static final ResourceLocation BODY_BLOCK_REACH = Enhanced.id("hulk_block_reach");
    private static final ResourceLocation BODY_JUMP = Enhanced.id("hulk_jump");
    private static final ResourceLocation BODY_SAFE_FALL = Enhanced.id("hulk_safe_fall");
    private static final ResourceLocation BODY_FALL = Enhanced.id("hulk_fall");
    private static final ResourceLocation BODY_STEP = Enhanced.id("hulk_step");
    private static final ResourceLocation BODY_SPEED = Enhanced.id("hulk_speed");
    private static final ResourceLocation BODY_BREAK = Enhanced.id("hulk_break");
    // While he grabs, transforms or charges: he cannot walk, jump or be pushed (transient, never saved for long).
    private static final ResourceLocation HOLD_SPEED = Enhanced.id("hulk_hold_speed");
    private static final ResourceLocation HOLD_JUMP = Enhanced.id("hulk_hold_jump");
    private static final ResourceLocation HOLD_KNOCKBACK = Enhanced.id("hulk_hold_knockback");
    /** The Super Leap's fast fall. */
    static final ResourceLocation LEAP_GRAVITY = Enhanced.id("hulk_leap_gravity");

    /** Hulk green, the gamma glow, and the dust and stone of what he smashes. */
    public static final int GREEN = 0x5BA33A;
    public static final int GAMMA = 0xB6F27A;

    /** The looks. */
    public static final String RAGNAROK = "ragnarok";
    public static final String BRAND_NEW_DAY = "brand_new_day";
    public static final String BANNER = "banner";
    public static final String OWN = "own";

    /** Smash: the ticks the fists are up before they come down. */
    public static final int SMASH_WINDUP = 6;
    /** Smash: how far up and out it throws. */
    public static final double SMASH_LIFT = 0.6;
    public static final double SMASH_PUSH = 0.8;
    /** Who stands on the ground: feet at most this far above it. */
    public static final double GROUNDED = 0.5;
    /** The Boulder Throw: the ticks the boulder is torn up before it flies, and its speed. */
    public static final int RIP_TICKS = 8;
    public static final double BOULDER_SPEED = 1.6;
    /** How far the effects are sent; how far the camera shakes. */
    static final double FX_RANGE = 96.0;
    public static final double SHAKE_RANGE = 12.0;

    /** What each Hulk carries from tick to tick; forgotten when he leaves or the server stops. */
    private static final Map<UUID, State> STATES = new HashMap<>();

    /**
     * A move that lasts some ticks (the Smash's ring, the Clap's wave, a leap, a grab, a charge, a boulder torn up).
     */
    interface Move {
        /**
         * One tick of it, {@code t} ticks after it began (0 the first).
         *
         * @return whether it is over
         */
        boolean tick(ServerPlayer player, PowerData data, int t);

        /** It ended (over, or cut short: death, a dimension, the power lost). */
        default void end(ServerPlayer player, PowerData data) {
        }

        /** The {@code Live} state that stands for it, or null. */
        @Nullable
        default Live live() {
            return null;
        }
    }

    static final class State {
        /** The move he is making, and the tick it began. */
        @Nullable
        Move move;
        long moveStart;
        /** The look his body has settled in (null until first seen), and the change under way. */
        @Nullable
        String settled;
        @Nullable
        HulkForms.Change change;
        /** The last time he was hurt, the last time he fought (hurt or hit), his exhaustion at the last tick. */
        long lastHurt = Long.MIN_VALUE / 2;
        long lastFight = Long.MIN_VALUE / 2;
        float lastExhaustion = -1.0F;
        /** Banner's health was at least 4 at the last hurt (the "Hulk protects Banner" jump is once per fall). */
        boolean wellBefore = true;
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

    static double cfg(net.neoforged.neoforge.common.ModConfigSpec.DoubleValue value, double fallback) {
        return loaded() ? value.get() : fallback;
    }

    static int cfg(net.neoforged.neoforge.common.ModConfigSpec.IntValue value, int fallback) {
        return loaded() ? value.get() : fallback;
    }

    static boolean cfg(net.neoforged.neoforge.common.ModConfigSpec.BooleanValue value, boolean fallback) {
        return loaded() ? value.get() : fallback;
    }

    // ---- who he is ----

    /** Whether the look is one of the two Hulks'. */
    public static boolean isHulkLook(String look) {
        return RAGNAROK.equals(look) || BRAND_NEW_DAY.equals(look);
    }

    /** The id of the player's look ({@code ragnarok} for the default). */
    public static String look(PowerData data) {
        return com.kolodkingal.enhanced.power.Variants.current(data).id();
    }

    /** Whether the player is the Hulk himself (not Banner) right now. */
    public static boolean isHulk(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.HULK && isHulkLook(look(data));
    }

    /**
     * Whether the player has the Hulk's body right now: the Hulk (not Banner), or a custom hero with {@code hulk.body}
     * out of his human form. Both sides.
     */
    public static boolean hasBody(PowerData data) {
        return data.has(HeroAbility.HULK_BODY) && !HulkForms.isHuman(data);
    }

    /** How much bigger a look is ({@code ADD_MULTIPLIED_BASE} of the scale; 0 for a human). */
    public static double scaleOf(String look) {
        return switch (look) {
            case RAGNAROK -> cfg(HulkConfig.SCALE_RAGNAROK, 0.5);
            case BRAND_NEW_DAY -> cfg(HulkConfig.SCALE_BRAND_NEW_DAY, 0.75);
            default -> 0.0;
        };
    }

    /** The factor every damage of the Hulk in this look is multiplied by: the Brand New Day Hulk hits harder. */
    public static double lookFactor(PowerData data) {
        return data.power() == Power.HULK && BRAND_NEW_DAY.equals(look(data)) ? cfg(HulkConfig.BND_DAMAGE_FACTOR, 1.15)
                : 1.0;
    }

    /**
     * The damage of one of his blows: {@code base} (the Ragnarok Hulk's) times his look's factor and his rage's ({@link
     * HulkRage#damageFactor}).
     */
    public static float damage(ServerPlayer player, PowerData data, double base) {
        return (float) (base * lookFactor(data) * HulkRage.damageFactor(player, data));
    }

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his, with its Sneak variant said explicitly ({@code alt}): the Hulk's own Sneak, or the binding of a
     * custom hero with his abilities (3.4).
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down || player.isSpectator()) {
            return;
        }
        if (data.power() == Power.CUSTOM) {
            customKey(player, data, ability, alt);
            return;
        }
        State state = state(player.getUUID());
        if (state.change != null) {
            return; // (while he changes, his keys are ignored)
        }
        if (HulkForms.isHuman(data)) {
            if (ability == 5 && !alt) {
                transform(player, data);
            } else {
                player.displayClientMessage(Component.translatable("message.enhanced.hulk.human_form")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        if (state.move != null) {
            return; // (one move at a time)
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    GrabSmash.start(player, data);
                } else {
                    smash(player, data);
                }
            }
            case 2 -> {
                if (alt) {
                    roar(player, data);
                } else {
                    ThunderClap.start(player, data);
                }
            }
            case 3 -> {
                if (alt) {
                    SuperLeap.bound(player, data);
                } else {
                    SuperLeap.leap(player, data);
                }
            }
            case 4 -> {
                if (alt) {
                    GammaCharge.start(player, data);
                } else {
                    rip(player, data);
                }
            }
            case 5 -> {
                if (alt) {
                    switchHulk(player, data);
                } else {
                    transform(player, data);
                }
            }
            default -> {
            }
        }
    }

    /** A custom hero's key of the Hulk's: the four moves as the Hulk's, N his Human Form. */
    private static void customKey(ServerPlayer player, PowerData data, int ability, boolean alt) {
        State state = state(player.getUUID());
        if (ability == 5) {
            if (alt) {
                player.displayClientMessage(Component.translatable("message.enhanced.hulk.only_hulk")
                        .withStyle(ChatFormatting.GRAY), true);
            } else {
                humanForm(player, data);
            }
            return;
        }
        if (state.move != null || data.toggle(Toggle.HUMAN_FORM)) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    GrabSmash.start(player, data);
                } else {
                    smash(player, data);
                }
            }
            case 2 -> {
                if (alt) {
                    roar(player, data);
                } else {
                    ThunderClap.start(player, data);
                }
            }
            case 3 -> {
                if (alt) {
                    SuperLeap.bound(player, data);
                } else {
                    SuperLeap.leap(player, data);
                }
            }
            case 4 -> {
                if (alt) {
                    GammaCharge.start(player, data);
                } else {
                    rip(player, data);
                }
            }
            default -> {
            }
        }
    }

    /** N: Banner Hulks out into his last Hulk, the Hulk calms down into Banner. */
    static void transform(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_TRANSFORM, now)) {
            return;
        }
        String to = HulkForms.isHuman(data) ? HulkRage.get(player).lastHulk() : BANNER;
        if (!isHulkLook(to) && HulkForms.isHuman(data)) {
            to = RAGNAROK;
        }
        if (HulkForms.change(player, to)) {
            data.startCooldown(HeroAbility.HULK_TRANSFORM, now, cfg(HulkConfig.TRANSFORM_COOLDOWN, 100));
        }
    }

    /** Sneak + N: Ragnarok and Brand New Day trade places. */
    static void switchHulk(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_TRANSFORM, now)) {
            return;
        }
        String to = BRAND_NEW_DAY.equals(look(data)) ? RAGNAROK : BRAND_NEW_DAY;
        if (HulkForms.change(player, to)) {
            data.startCooldown(HeroAbility.HULK_TRANSFORM, now, cfg(HulkConfig.SWITCH_COOLDOWN, 40));
        }
    }

    /**
     * A custom hero's N: his Human Form. Every power of his ends and stays off (his rows answer no, his modules give
     * nothing, his body is gone, his own skin shows) until N again.
     */
    public static void humanForm(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_TRANSFORM, now)) {
            return;
        }
        boolean human = !data.toggle(Toggle.HUMAN_FORM);
        ServerLevel level = player.serverLevel();
        if (human) {
            CustomHeroServer.endPowers(player, data);
            data.setToggle(Toggle.HUMAN_FORM, true);
            send(level, player, HulkFxPayload.TRANSFORM_IN, List.of(player.position()), 0);
            sound(level, player.position(), ModSounds.HULK_TRANSFORM_IN.get(), 1.0F, 1.0F);
        } else {
            data.setToggle(Toggle.HUMAN_FORM, false);
            send(level, player, HulkFxPayload.TRANSFORM_OUT, List.of(player.position()), 0);
            sound(level, player.position(), ModSounds.HULK_TRANSFORM_OUT.get(), 1.0F, 1.0F);
        }
        data.startCooldown(HeroAbility.HULK_TRANSFORM, now, cfg(HulkConfig.TRANSFORM_COOLDOWN, 100));
        CustomHeroes.syncState(player);
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        State state = state(player.getUUID());
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (state.lastTick == now) {
            return; // (one tick of his per game tick, however often the player is ticked)
        }
        state.lastTick = now;
        if (data.power() == Power.HULK) {
            HulkForms.tick(player, data, state, now);
        }
        if (state.move != null) {
            Move move = state.move;
            boolean over;
            try {
                over = move.tick(player, data, (int) (now - state.moveStart));
            } catch (RuntimeException e) {
                over = true;
                Enhanced.LOGGER.warn("A move of the Hulk failed", e);
            }
            if (over && state.move == move) {
                finishMove(player, data, state);
            }
        }
        if (data.has(HeroAbility.HULK_BODY)) {
            HulkRage.tick(player, data, state, now);
            if (hasBody(data)) {
                bodyTick(player, data, state, now);
            }
        }
        boolean enraged = hasBody(data) && HulkRage.enraged(player);
        data.set(Live.HULK_ENRAGED, enraged);
        boolean leaping = state.move != null && state.move.live() == Live.HULK_LEAPING;
        boolean grabbing = state.move != null && state.move.live() == Live.HULK_GRABBING;
        boolean charging = state.move != null && state.move.live() == Live.HULK_CHARGING;
        if (data.has(HeroAbility.HULK_LEAP)) {
            data.set(Live.HULK_LEAPING, leaping);
        }
        if (data.has(HeroAbility.HULK_SMASH)) {
            data.set(Live.HULK_GRABBING, grabbing);
        }
        if (data.has(HeroAbility.HULK_BOULDER)) {
            data.set(Live.HULK_CHARGING, charging);
        }
        hold(player, grabbing || charging || state.change != null, state.change != null);
    }

    /** The healing factor and the hunger of his body. */
    private static void bodyTick(ServerPlayer player, PowerData data, State state, long now) {
        float k = data.bodyScale(HeroAbility.HULK_BODY);
        if (now % 20 == 0 && now - state.lastHurt >= 100 && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) (cfg(HulkConfig.REGEN_PER_SECOND, 0.35) * k));
        }
        float exhaustion = player.getFoodData().getExhaustionLevel();
        if (state.lastExhaustion >= 0.0F && exhaustion > state.lastExhaustion) {
            float extra = (float) ((cfg(HulkConfig.EXHAUSTION_FACTOR,
                    1.5) - 1.0) * (exhaustion - state.lastExhaustion));
            player.getFoodData().addExhaustion(extra);
            exhaustion = player.getFoodData().getExhaustionLevel();
        }
        state.lastExhaustion = exhaustion;
    }

    /** He cannot walk, jump or be pushed while he grabs, charges or changes (a transient modifier). */
    private static void hold(ServerPlayer player, boolean still, boolean unpushable) {
        setModifier(player, Attributes.MOVEMENT_SPEED, HOLD_SPEED, -1.0, still,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.JUMP_STRENGTH, HOLD_JUMP, -1.0, still,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, HOLD_KNOCKBACK, 1.0, unpushable,
                AttributeModifier.Operation.ADD_VALUE);
    }

    static void startMove(ServerPlayer player, State state, Move move) {
        state.move = move;
        state.moveStart = player.level().getGameTime();
    }

    private static void finishMove(ServerPlayer player, PowerData data, State state) {
        Move move = state.move;
        state.move = null;
        if (move != null) {
            move.end(player, data);
        }
    }

    /** The move he is making, or null. */
    @Nullable
    static Move move(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null ? null : state.move;
    }

    // ---- the body ----

    /**
     * The body of the Hulk while the player has it (scaled by its strength for a custom hero), nothing of it on anyone
     * else. The scale follows the change under way (it grows and shrinks over the transformation); the rest comes when
     * the growth is over and goes when he has shrunk. Health keeps its share of the maximum when the maximum changes.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean has = data.has(HeroAbility.HULK_BODY);
        if (!has) {
            if (player.getAttribute(Attributes.SCALE).getModifier(BODY_SCALE) != null
                    || player.getAttribute(Attributes.MAX_HEALTH).getModifier(BODY_HEALTH) != null) {
                removeBody(player);
            }
            return;
        }
        State state = STATES.get(player.getUUID());
        double k = data.bodyScale(HeroAbility.HULK_BODY);
        boolean on;
        double scale;
        String look;
        if (data.power() == Power.HULK) {
            look = look(data);
            HulkForms.Change change = state == null ? null : state.change;
            if (change != null) {
                scale = change.scale(player.level().getGameTime());
                // (Growing out of Banner, the Hulk's strength comes when he has grown; shrinking, it goes when he has.)
                on = isHulkLook(change.from()) && isHulkLook(look);
            } else {
                on = isHulkLook(look);
                scale = scaleOf(look);
            }
        } else {
            look = RAGNAROK;
            on = !data.toggle(Toggle.HUMAN_FORM);
            scale = on ? scaleOf(RAGNAROK) * k : 0.0;
        }
        setModifier(player, Attributes.SCALE, BODY_SCALE, scale, scale > 1.0E-4,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        boolean bnd = BRAND_NEW_DAY.equals(look);
        double factor = bnd ? cfg(HulkConfig.BND_DAMAGE_FACTOR, 1.15) : 1.0;
        float oldMax = player.getMaxHealth();
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, cfg(HulkConfig.HEALTH_BONUS, 20.0) * k, on);
        float newMax = player.getMaxHealth();
        if (newMax != oldMax && oldMax > 0.0F && player.isAlive()) {
            player.setHealth(Math.max(1.0F, Math.min(newMax, player.getHealth() * newMax / oldMax)));
        }
        setModifier(player, Attributes.ARMOR, BODY_ARMOR, cfg(HulkConfig.ARMOR_BONUS, 8.0) * k, on);
        setModifier(player, Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS, cfg(HulkConfig.TOUGHNESS_BONUS, 4.0) * k, on);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK, Math.min(1.0,
                cfg(HulkConfig.KNOCKBACK_RESISTANCE, 0.9) * k), on);
        setModifier(player, Attributes.ATTACK_DAMAGE, BODY_ATTACK, cfg(HulkConfig.ATTACK_BONUS, 7.0) * factor * k, on);
        setModifier(player, Attributes.ATTACK_KNOCKBACK, BODY_ATTACK_KNOCKBACK, cfg(HulkConfig.ATTACK_KNOCKBACK,
                1.5) * k, on);
        double reach = (bnd ? cfg(HulkConfig.REACH_BRAND_NEW_DAY, 2.0) : cfg(HulkConfig.REACH_RAGNAROK, 1.5)) * k;
        setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, BODY_REACH, reach, on);
        setModifier(player, Attributes.BLOCK_INTERACTION_RANGE, BODY_BLOCK_REACH, reach, on);
        setModifier(player, Attributes.JUMP_STRENGTH, BODY_JUMP, cfg(HulkConfig.JUMP_BONUS, 0.5) * k, on);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, BODY_SAFE_FALL, cfg(HulkConfig.SAFE_FALL, 24.0) * k, on);
        setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, BODY_FALL, Math.max(-1.0, -1.0 * k), on);
        setModifier(player, Attributes.STEP_HEIGHT, BODY_STEP, cfg(HulkConfig.STEP_BONUS, 0.6) * k, on);
        setModifier(player, Attributes.MOVEMENT_SPEED, BODY_SPEED, cfg(HulkConfig.SPEED_BONUS, 0.15) * k, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.BLOCK_BREAK_SPEED, BODY_BREAK, (cfg(HulkConfig.BREAK_SPEED_FACTOR,
                4.0) - 1.0) * k, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void removeBody(ServerPlayer player) {
        float oldMax = player.getMaxHealth();
        for (Object[] m : new Object[][]{{Attributes.SCALE, BODY_SCALE}, {Attributes.MAX_HEALTH, BODY_HEALTH},
                {Attributes.ARMOR, BODY_ARMOR},
                {Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS}, {Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK},
                {Attributes.ATTACK_DAMAGE, BODY_ATTACK}, {Attributes.ATTACK_KNOCKBACK, BODY_ATTACK_KNOCKBACK},
                {Attributes.ENTITY_INTERACTION_RANGE, BODY_REACH}, {Attributes.BLOCK_INTERACTION_RANGE,
                        BODY_BLOCK_REACH},
                {Attributes.JUMP_STRENGTH, BODY_JUMP}, {Attributes.SAFE_FALL_DISTANCE, BODY_SAFE_FALL},
                {Attributes.FALL_DAMAGE_MULTIPLIER, BODY_FALL}, {Attributes.STEP_HEIGHT, BODY_STEP},
                        {Attributes.MOVEMENT_SPEED, BODY_SPEED},
                {Attributes.BLOCK_BREAK_SPEED, BODY_BREAK}}) {
            @SuppressWarnings("unchecked")
            Holder<Attribute> attribute = (Holder<Attribute>) m[0];
            setModifier(player, attribute, (ResourceLocation) m[1], 0.0, false);
        }
        hold(player, false, false);
        setModifier(player, Attributes.GRAVITY, LEAP_GRAVITY, 0.0, false);
        float newMax = player.getMaxHealth();
        if (player.isAlive() && newMax != oldMax && oldMax > 0.0F) {
            player.setHealth(Math.max(1.0F, Math.min(newMax, player.getHealth() * newMax / oldMax)));
        }
    }

    static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount,
            boolean present) {
        setModifier(player, attribute, id, amount, present, AttributeModifier.Operation.ADD_VALUE);
    }

    static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount,
            boolean present,
                            AttributeModifier.Operation operation) {
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

    // ---- who his blows reach ----

    /**
     * Whether the creature stands on the ground: on it, or its feet at most {@link #GROUNDED} above it; never one that
     * flies, swims or rides.
     */
    public static boolean grounded(LivingEntity e) {
        if (e.isInWater() || e.isPassenger() || e.isFallFlying() || e instanceof Player p && p.getAbilities().flying) {
            return false;
        }
        if (e instanceof FlyingAnimal flier && flier.isFlying()) {
            return false;
        }
        if (e.onGround()) {
            return true;
        }
        return !e.level().noCollision(e, e.getBoundingBox().expandTowards(0.0, -GROUNDED - 0.01, 0.0));
    }

    /** Whether his area blows may hurt the creature (PvP, teams, pets, Creative, the area rule). */
    static boolean mayHurt(ServerPlayer player, LivingEntity target) {
        return target.isAlive() && !target.isSpectator() && AreaTargets.mayHurt(player, target, false);
    }

    /** Hurts the creature with a damage of his ({@code type}), the blow always landing (the stones' rule). */
    static boolean hurt(ServerPlayer player, LivingEntity target,
            net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type,
                        float amount, @Nullable Entity direct) {
        boolean hit = StonePowers.hurt(player.serverLevel(), player, target, type, amount, direct);
        if (hit) {
            HulkRage.landed(player, Powers.get(player));
        }
        return hit;
    }

    /** The last push of a Hulk's on each creature (by id): for the Vormir credit's tests. Cleared with the statics. */
    private static final Map<Integer, UUID> PUSHED = new HashMap<>();

    /**
     * Every push of his (the Smash, the Clap, the landing, the Grab Smash's last slam, the charge, the boulder): the
     * shared push, which credits him for the Soul Stone's sacrifice ({@code VormirTracker.credit}: he is no Bearer, so
     * a friend he pushes off the Vormir Cliff dies an ordinary death).
     */
    public static void push(LivingEntity victim, Vec3 from, double strength, double lift, LivingEntity hulk) {
        StonePowers.shove(victim, from, strength, lift, hulk);
        if (PUSHED.size() > 4096) {
            PUSHED.clear();
        }
        PUSHED.put(victim.getId(), hulk.getUUID());
    }

    /** Who of the Hulks last pushed the creature, or null. */
    @Nullable
    public static UUID lastPushedBy(LivingEntity victim) {
        return PUSHED.get(victim.getId());
    }

    // ---- R: Hulk Smash ----

    /**
     * R: both fists high, then down into the ground: a ring runs out from his feet ({@code smashRadius}, one block a
     * tick) and every creature standing on the ground in it takes {@code smashDamage}, thrown up and out. A flier, a
     * swimmer, a jumper is untouched.
     */
    public static boolean smash(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_SMASH, now)) {
            return false;
        }
        State state = state(player.getUUID());
        data.startCooldown(HeroAbility.HULK_SMASH, now, cfg(HulkConfig.SMASH_COOLDOWN, 60));
        startMove(player, state, new Smash());
        send(player.serverLevel(), player, HulkFxPayload.SMASH, List.of(player.position()), 0);
        return true;
    }

    /** The Smash: the fists up, down, then the ring. */
    static final class Smash implements Move {
        private final IntSet hit = new IntOpenHashSet();
        private Vec3 center = Vec3.ZERO;
        /** The blow's damage, fixed when the fists come down (the rage its first hits add is for the next blow). */
        private float amount;

        @Override
        public boolean tick(ServerPlayer player, PowerData data, int t) {
            ServerLevel level = player.serverLevel();
            int radius = cfg(HulkConfig.SMASH_RADIUS, 6);
            if (t < SMASH_WINDUP) {
                return false;
            }
            if (t == SMASH_WINDUP) {
                center = player.position();
                sound(level, center, ModSounds.HULK_SMASH.get(), 1.6F, 0.9F + level.random.nextFloat() * 0.1F);
                send(level, player, HulkFxPayload.SMASH, List.of(center), radius);
                HulkTerrain.smash(player, center);
                amount = damage(player, data, cfg(HulkConfig.SMASH_DAMAGE, 7.0));
            }
            int k = t - SMASH_WINDUP;
            ring(player, center, k, k + 1.0, amount);
            return k + 1 >= radius;
        }

        private void ring(ServerPlayer player, Vec3 center, double from, double to, float amount) {
            ServerLevel level = player.serverLevel();
            AABB box = new AABB(center, center).inflate(to + 1.5, 3.0, to + 1.5);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player
                    && !hit.contains(e.getId()) && mayHurt(player, e))) {
                double distance = Math.hypot(e.getX() - center.x, e.getZ() - center.z);
                double half = e.getBbWidth() * 0.5;
                if (distance + half < from || distance - half > to || Math.abs(e.getY() - center.y) > 2.5
                        || !grounded(e)) {
                    continue;
                }
                hit.add(e.getId());
                if (hurt(player, e, ModDamageTypes.HULK_SMASH, amount, player)) {
                    push(e, center, SMASH_PUSH, SMASH_LIFT, player);
                }
            }
        }
    }

    // ---- Sneak + G: the Roar ----

    /**
     * Sneak + G: the arena's roar. Hostile creatures round him (not the bosses) are Frightened (they run from him) and
     * weakened; players he may fight are weakened; his rage rises.
     */
    public static List<LivingEntity> roar(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.HULK_ROAR, now)) {
            return List.of();
        }
        data.startCooldown(HeroAbility.HULK_ROAR, now, cfg(HulkConfig.ROAR_COOLDOWN, 300));
        int radius = cfg(HulkConfig.ROAR_RADIUS, 12);
        int fright = cfg(HulkConfig.ROAR_FRIGHT_TICKS, 80);
        int weakness = cfg(HulkConfig.ROAR_WEAKNESS_TICKS, 160);
        List<LivingEntity> touched = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && e.distanceTo(player) <= radius)) {
            if (e instanceof Player other) {
                if (mayHurt(player, other)) {
                    other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), player);
                    touched.add(other);
                }
            } else if (e instanceof Enemy && !e.getType().is(ModTags.BOSSES) && !e.getType().is(ModTags.BOSS_MINIONS)
                    && mayHurt(player, e)) {
                e.addEffect(new MobEffectInstance(ModEffects.FRIGHTENED, fright, 0), player);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weakness, 0), player);
                com.kolodkingal.enhanced.effect.FrightenedEffect.scare(e, player);
                touched.add(e);
            }
        }
        HulkRage.add(player, data, 20);
        sound(level, player.position(), ModSounds.HULK_ROAR.get(), 2.0F, 0.9F + level.random.nextFloat() * 0.15F);
        send(level, player, HulkFxPayload.ROAR, List.of(player.getEyePosition()), radius);
        return touched;
    }

    // ---- B: the Boulder Throw ----

    /**
     * B: he tears a boulder out of the ground (it looks like the block he stands on; no block is taken) and, a moment
     * later, hurls it where he looks.
     */
    public static boolean rip(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HULK_BOULDER, now)) {
            return false;
        }
        data.startCooldown(HeroAbility.HULK_BOULDER, now, cfg(HulkConfig.BOULDER_COOLDOWN, 80));
        BlockState under = groundUnder(player);
        startMove(player, state(player.getUUID()), new Rip(under));
        ServerLevel level = player.serverLevel();
        sound(level, player.position(), ModSounds.HULK_RIP.get(), 1.2F, 1.0F);
        send(level, player, HulkFxPayload.RIP, List.of(player.position()),
                net.minecraft.world.level.block.Block.getId(under));
        return true;
    }

    /**
     * The block a boulder of his looks like: the one he stands on, or stone where that is nothing a boulder could be.
     */
    static BlockState groundUnder(ServerPlayer player) {
        BlockPos pos = player.getOnPos();
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir() || !state.isSolidRender(player.level(), pos) || state.hasBlockEntity()) {
            state = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        }
        return state;
    }

    /** The boulder torn up and held overhead, then thrown. */
    static final class Rip implements Move {
        private final BlockState block;

        Rip(BlockState block) {
            this.block = block;
        }

        @Override
        public boolean tick(ServerPlayer player, PowerData data, int t) {
            if (t < RIP_TICKS) {
                return false;
            }
            throwBoulder(player, data, block);
            return true;
        }
    }

    /** The boulder leaves his hands along his look. */
    public static HulkBoulderEntity throwBoulder(ServerPlayer player, PowerData data, BlockState block) {
        ServerLevel level = player.serverLevel();
        HulkBoulderEntity boulder = new HulkBoulderEntity(ModEntities.HULK_BOULDER.get(), level);
        boulder.setOwner(player);
        boulder.setBlock(block);
        boulder.setDamage(damage(player, data, cfg(HulkConfig.BOULDER_DAMAGE, 8.0)), damage(player, data,
                cfg(HulkConfig.BOULDER_SPLASH, 4.0)));
        Vec3 look = player.getLookAngle();
        Vec3 at = player.getEyePosition().add(0, player.getBbHeight() * 0.15, 0).add(look.scale(1.0
                + player.getBbWidth()));
        boulder.moveTo(at.x, at.y - 0.5, at.z, player.getYRot(), 0.0F);
        boulder.setDeltaMovement(look.scale(BOULDER_SPEED));
        level.addFreshEntity(boulder);
        sound(level, player.position(), ModSounds.HULK_THROW.get(), 1.2F, 1.0F);
        return boulder;
    }

    // ---- the creature in front ----

    /** The creature he looks at within his reach and {@code extra} more, or null. */
    @Nullable
    static LivingEntity inFront(ServerPlayer player, double extra) {
        double reach = player.entityInteractionRange() + extra;
        EntityHitResult hit = Raycasts.entity(player, reach, 0.4, e -> e instanceof LivingEntity living
                && living.isAlive() && !e.isSpectator(), 1.0F);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** Whether the creature cannot be grabbed (a boss, the Stonekeeper, an Asgardian, a pinned one). */
    static boolean grabImmune(LivingEntity target) {
        return target.getType().is(ModTags.GRAB_IMMUNE) || target.getType().is(ModTags.BOSSES)
                || MjolnirPin.isPinned(target);
    }

    // ---- effects ----

    static void send(ServerLevel level, Entity who, int kind, List<Vec3> points, int arg) {
        List<Vec3> list = points.size() > HulkFxPayload.MAX_POINTS ? points.subList(0, HulkFxPayload.MAX_POINTS)
                : points;
        PacketDistributor.sendToPlayersNear(level, null, who.getX(), who.getY(), who.getZ(), FX_RANGE,
                new HulkFxPayload(kind, who.getId(), List.copyOf(list), arg));
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ---- the end of the power, of the life, of the session ----

    /** The power is being taken away (or a custom hero is remade): every move ends, the change is settled. */
    static void endAll(ServerPlayer player, PowerData data) {
        State state = STATES.get(player.getUUID());
        if (state != null) {
            if (state.move != null) {
                finishMove(player, data, state);
            }
            HulkForms.finish(player, data, state);
        }
        data.set(Live.HULK_LEAPING, false);
        data.set(Live.HULK_GRABBING, false);
        data.set(Live.HULK_CHARGING, false);
        data.set(Live.HULK_ENRAGED, false);
        data.set(Live.HULK_TRANSFORMING, false);
        hold(player, false, false);
        setModifier(player, Attributes.GRAVITY, LEAP_GRAVITY, 0.0, false);
    }

    /** He died or left: what he was doing ends; on a death his rage and his anger go. */
    static void onGone(ServerPlayer player, boolean logout) {
        PowerData data = Powers.get(player);
        endAll(player, data);
        if (!logout) {
            HulkRage.reset(player);
        }
        STATES.remove(player.getUUID());
    }

    static void clearData(PowerData data) {
        data.set(Live.HULK_LEAPING, false);
        data.set(Live.HULK_GRABBING, false);
        data.set(Live.HULK_CHARGING, false);
    }

    /** Hurt now: the healing factor waits, and he is in a fight. */
    static void hurtNow(ServerPlayer player) {
        State state = state(player.getUUID());
        long now = player.level().getGameTime();
        state.lastHurt = now;
        state.lastFight = now;
    }

    static void clear() {
        STATES.clear();
        PUSHED.clear();
    }

    /** A number of the fall of a ring, for the client: the ring is {@code radius} wide after {@code t} ticks. */
    public static float ringRadius(float t, float radius) {
        return Mth.clamp(t, 0.0F, radius);
    }

    private HulkAbilities() {
    }
}
