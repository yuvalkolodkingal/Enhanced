package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hulk.GrabSmash;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.hulk.SuperLeap;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StonePowers;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Red Hulk (3.8, PLAN 4; Captain America: Brave New World): Thaddeus Ross, whose red skin grows hotter with his anger.
 * The plug-in is {@link RedHulkServer}; the meter {@link Anger} and its {@link RedHulkBurst}; the moves {@link HeatWave},
 * {@link SuperJump}, {@link EnergyAbsorb}; the two forms {@link RossForm}; the damage hooks {@link RedHulkEvents}. What
 * he shares with the Hulk he takes from the Hulk's code through the 3.8 seams: the room check ({@code
 * HulkForms.roomFor}), the Bound ({@code SuperLeap.bound} with his numbers), the grab ({@code GrabSmash.start} with his
 * numbers), the push the Vormir Cliff credits ({@code HulkAbilities.push}), and on the client the Hulk's body and
 * renderer.
 *
 * <p>R is the Ground Smash (a ring of {@code smashRadius} from his feet, the creatures on the ground in it hurt and
 * thrown up), Sneak + R Grab &amp; Slam; G the Heat Wave (a cone of heat), Sneak + G the Roar; V the Super Jump (held:
 * the charge, released: the launch), Sneak + V the Bound; B the Anger Burst now (from {@code earlyBurstMin}), Sneak + B
 * (held) the Energy Absorb; N the Transform (Ross and Red Hulk). As Ross every key but N is refused.
 *
 * <p>3.4's rules from birth: every cooldown by its row, the Sneak variant is {@code alt}, the tick's parts guarded by
 * {@code data.has(row)}, his body by {@link HeroAbility#RED_HULK_BODY} and its {@code bodyScale}, the colours by
 * {@link Powers#color}/{@link Powers#glow}. A custom hero's N (with {@code red_hulk.transform}) is the Human Form: every
 * power off until N again, as the Hulk's.
 */
public final class RedHulkAbilities {
    // The body's modifiers (permanent: right when a player logs in).
    static final ResourceLocation BODY_SCALE = Enhanced.id("red_hulk_scale");
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("red_hulk_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("red_hulk_armor");
    private static final ResourceLocation BODY_TOUGHNESS = Enhanced.id("red_hulk_toughness");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("red_hulk_knockback");
    private static final ResourceLocation BODY_ATTACK = Enhanced.id("red_hulk_attack");
    private static final ResourceLocation BODY_SPEED = Enhanced.id("red_hulk_speed");
    private static final ResourceLocation BODY_REACH = Enhanced.id("red_hulk_reach");
    private static final ResourceLocation BODY_BLOCK_REACH = Enhanced.id("red_hulk_block_reach");
    private static final ResourceLocation BODY_STEP = Enhanced.id("red_hulk_step");
    private static final ResourceLocation BODY_JUMP = Enhanced.id("red_hulk_jump");
    private static final ResourceLocation BODY_SAFE_FALL = Enhanced.id("red_hulk_safe_fall");
    // While he charges a jump, absorbs, grabs or changes: he cannot walk or jump (transient, never saved for long).
    private static final ResourceLocation HOLD_SPEED = Enhanced.id("red_hulk_hold_speed");
    private static final ResourceLocation HOLD_JUMP = Enhanced.id("red_hulk_hold_jump");

    /** His red, his heat, and the white-hot core of his anger. */
    public static final int RED = 0xC4231B;
    public static final int HEAT = 0xFF7A3A;
    public static final int WHITE_HOT = 0xFFE6B0;

    /** The Ground Smash: the ticks the fists are up before they come down; the ring's push out. */
    public static final int SMASH_WINDUP = 6;
    public static final double SMASH_PUSH = 0.8;
    /** How far the effects are sent; how far the camera shakes. */
    static final double FX_RANGE = 96.0;
    public static final double SHAKE_RANGE = 12.0;
    /** The Transform's cooldown (ticks; the plan names none: two changes cannot overlap, decision of lane L2). */
    public static final int TRANSFORM_COOLDOWN = 40;
    /** The anger at which he steams ({@code Live.RED_HULK_HEATED}). */
    public static final float HEATED = 75.0F;

    /** What each Red Hulk carries from tick to tick; forgotten when he leaves or the server stops. */
    private static final Map<UUID, State> STATES = new HashMap<>();

    /** The Hulk's move he is making through the Hulk's code (a grab, a Bound). */
    enum HulkMove {
        NONE, GRAB, BOUND
    }

    static final class State {
        /** The change of form under way, or null ({@link RossForm}). */
        @Nullable
        RossForm.Change change;
        /** The Ground Smash's ring, or null. */
        @Nullable
        Smash smash;
        /** The Anger Burst's ring, or null. */
        @Nullable
        RedHulkBurst burst;
        /** The Super Jump (charging or in the air), or null. */
        @Nullable
        SuperJump jump;
        /** The game time the Energy Absorb began at (it lasts while the key is held), or -1. */
        long absorbStart = -1L;
        /** The Hulk's move the Hulk's code flies or swings for him. */
        HulkMove hulkMove = HulkMove.NONE;
        /** The game time the Super Jump's fall guard lapses at, if no fall came to cancel. */
        long fallSafeUntil = Long.MIN_VALUE;
        /** He has grown into his size since he became Red Hulk (a growth that ends at once leaves no scale behind). */
        boolean sized;
        /** The game time of his last tick: a second tick in the same game tick does nothing more. */
        long lastTick = Long.MIN_VALUE;

        boolean busy() {
            return smash != null || burst != null || jump != null || absorbStart >= 0L || hulkMove != HulkMove.NONE
                    || change != null;
        }
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
        return loaded() ? value.get() : fallback;
    }

    static int cfg(ModConfigSpec.IntValue value, int fallback) {
        return loaded() ? value.get() : fallback;
    }

    static boolean cfg(ModConfigSpec.BooleanValue value, boolean fallback) {
        return loaded() ? value.get() : fallback;
    }

    /** His scale bonus ({@code ADD_MULTIPLIED_BASE}) at full size, by his body's strength. */
    public static double scaleBonus(PowerData data) {
        return cfg(RedHulkConfig.SCALE_BONUS, 0.5) * data.bodyScale(HeroAbility.RED_HULK_BODY);
    }

    // ---- who he is ----

    /** Whether he is in Ross's form (the hero himself; a custom hero's Ross is his Human Form). Both sides. */
    public static boolean isRoss(PowerData data) {
        return data.toggle(Toggle.ROSS_FORM);
    }

    /** Whether the player has Red Hulk's body now: the hero (not Ross), or a custom hero with {@code red_hulk.body}. */
    public static boolean hasBody(PowerData data) {
        return data.has(HeroAbility.RED_HULK_BODY) && !isRoss(data);
    }

    /** Whether the player is the hero Red Hulk himself in his big form right now. Both sides. */
    public static boolean isRedHulk(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.RED_HULK && !isRoss(data);
    }

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (player.isSpectator()) {
            return;
        }
        State st = state(player.getUUID());
        // (A hold ends whatever Sneak says by the time the key comes up.)
        if (!down) {
            if (ability == 3 && st.jump != null && st.jump.charging()) {
                st.jump.release(player, data);
            } else if (ability == 4 && st.absorbStart >= 0L) {
                EnergyAbsorb.stop(player, data, st);
            }
            return;
        }
        if (ability == 5) {
            if (alt) {
                return; // (N has no variant)
            }
            if (data.power() == Power.CUSTOM) {
                RossForm.humanForm(player, data);
            } else {
                RossForm.transform(player, data);
            }
            return;
        }
        if (data.power() == Power.RED_HULK && isRoss(data)) {
            player.displayClientMessage(Component.translatable("message.enhanced.red_hulk.ross")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (st.busy()) {
            return; // (one move at a time; while he changes his keys are ignored)
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    grabSlam(player, data);
                } else {
                    smash(player, data);
                }
            }
            case 2 -> {
                if (alt) {
                    roar(player, data);
                } else {
                    HeatWave.wave(player, data);
                }
            }
            case 3 -> {
                if (alt) {
                    bound(player, data);
                } else {
                    SuperJump.charge(player, data);
                }
            }
            case 4 -> {
                if (alt) {
                    EnergyAbsorb.start(player, data);
                } else {
                    RedHulkBurst.early(player, data);
                }
            }
            default -> {
            }
        }
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        State st = state(player.getUUID());
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (st.lastTick == now) {
            return; // (one tick of his per game tick, however often the player is ticked)
        }
        st.lastTick = now;
        RossForm.tick(player, data, st, now);
        boolean body = hasBody(data);
        if (body && cfg(RedHulkConfig.FIRE_IMMUNE, true) && player.getRemainingFireTicks() > 0) {
            player.clearFire();
        }
        if (data.has(HeroAbility.RED_HULK_BODY)) {
            Anger.tick(player, data, st, now);
        }
        if (st.smash != null && st.smash.tick(player, data, (int) (now - st.smash.start))) {
            st.smash = null;
        }
        if (st.burst != null && st.burst.tick(player, data, now)) {
            st.burst = null;
        }
        if (st.jump != null && st.jump.tick(player, data, st, now)) {
            st.jump = null;
        }
        if (st.absorbStart >= 0L) {
            EnergyAbsorb.tick(player, data, st, now);
        }
        if (st.hulkMove != HulkMove.NONE) {
            HulkAbilities.driveMove(player, data);
            if (!HulkAbilities.moving(player)) {
                st.hulkMove = HulkMove.NONE;
            }
        }
        if (st.fallSafeUntil != Long.MIN_VALUE && now > st.fallSafeUntil) {
            st.fallSafeUntil = Long.MIN_VALUE;
            Anger.set(player, Anger.get(player).with(RedHulkState.FALL_SAFE, false));
        }
        boolean jumpCharging = st.jump != null && st.jump.charging();
        boolean leaping = st.jump != null && !st.jump.charging() || st.hulkMove == HulkMove.BOUND;
        data.set(Live.RED_HULK_TRANSFORMING, st.change != null);
        data.set(Live.RED_HULK_HEATED, body && Anger.get(player).anger() >= HEATED);
        if (data.has(HeroAbility.RED_HULK_SUPER_JUMP)) {
            data.set(Live.RED_HULK_JUMP_CHARGING, jumpCharging);
            data.set(Live.RED_HULK_LEAPING, leaping);
        }
        if (data.has(HeroAbility.RED_HULK_SMASH)) {
            data.set(Live.RED_HULK_GRABBING, st.hulkMove == HulkMove.GRAB);
        }
        if (data.has(HeroAbility.RED_HULK_BURST)) {
            data.set(Live.RED_HULK_ABSORBING, st.absorbStart >= 0L);
        }
        hold(player, jumpCharging || st.absorbStart >= 0L || st.hulkMove == HulkMove.GRAB || st.change != null);
    }

    /** He cannot walk or jump while he charges, absorbs, grabs or changes (a transient modifier). */
    static void hold(ServerPlayer player, boolean still) {
        setModifier(player, Attributes.MOVEMENT_SPEED, HOLD_SPEED, -1.0, still,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.JUMP_STRENGTH, HOLD_JUMP, -1.0, still,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    // ---- the body ----

    /**
     * Red Hulk's body while the player has it (scaled by its strength for a custom hero), none of it on anyone else nor
     * on Ross. The scale follows the change under way ({@link RossForm}: it grows out of Ross and shrinks back); the rest
     * comes when the growth is over and goes when he has shrunk. A Red Hulk who has no scale yet (he has just become
     * him) grows into it, if there is room; if there is none he stays Ross. Health keeps its share of the maximum.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.RED_HULK_BODY)) {
            if (player.getAttribute(Attributes.SCALE).getModifier(BODY_SCALE) != null
                    || player.getAttribute(Attributes.MAX_HEALTH).getModifier(BODY_HEALTH) != null) {
                removeBody(player);
            }
            State gone = STATES.get(player.getUUID());
            if (gone != null) {
                gone.sized = false;
            }
            return;
        }
        State st = state(player.getUUID());
        if (player.getAttribute(Attributes.SCALE).getModifier(BODY_SCALE) != null) {
            st.sized = true;
        }
        if (!isRoss(data) && st.change == null && !st.sized && player.isAlive() && scaleBonus(data) > 1.0E-4) {
            RossForm.awaken(player, data, st);
        }
        double k = data.bodyScale(HeroAbility.RED_HULK_BODY);
        double full = scaleBonus(data);
        double scale;
        boolean on;
        RossForm.Change change = st.change;
        if (change != null) {
            scale = change.scale(player.level().getGameTime(), full);
            on = !change.toRed(); // (his strength comes when he has grown, and goes when he has shrunk)
        } else {
            on = hasBody(data);
            scale = on ? full : 0.0;
        }
        setModifier(player, Attributes.SCALE, BODY_SCALE, scale, scale > 1.0E-4,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (isRoss(data) && st.change == null) {
            st.sized = false; // (Ross: the next Red Hulk grows again)
        }
        float oldMax = player.getMaxHealth();
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, cfg(RedHulkConfig.HEALTH_BONUS, 24.0) * k, on);
        float newMax = player.getMaxHealth();
        if (newMax != oldMax && oldMax > 0.0F && player.isAlive()) {
            player.setHealth(Math.max(1.0F, Math.min(newMax, player.getHealth() * newMax / oldMax)));
        }
        setModifier(player, Attributes.ATTACK_DAMAGE, BODY_ATTACK, cfg(RedHulkConfig.ATTACK_BONUS, 12.0) * k, on);
        setModifier(player, Attributes.MOVEMENT_SPEED, BODY_SPEED, -cfg(RedHulkConfig.SPEED_PENALTY, 0.15) * k, on,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.ARMOR, BODY_ARMOR, cfg(RedHulkConfig.ARMOR_BONUS, 6.0) * k, on);
        setModifier(player, Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS, cfg(RedHulkConfig.TOUGHNESS_BONUS, 3.0) * k,
                on);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK, Math.min(1.0,
                cfg(RedHulkConfig.KNOCKBACK_RESISTANCE, 0.8) * k), on);
        double reach = cfg(RedHulkConfig.REACH_BONUS, 1.5) * k;
        setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, BODY_REACH, reach, on);
        setModifier(player, Attributes.BLOCK_INTERACTION_RANGE, BODY_BLOCK_REACH, reach, on);
        setModifier(player, Attributes.STEP_HEIGHT, BODY_STEP, cfg(RedHulkConfig.STEP_BONUS, 0.6) * k, on);
        setModifier(player, Attributes.JUMP_STRENGTH, BODY_JUMP, cfg(RedHulkConfig.JUMP_BONUS, 0.4) * k, on);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, BODY_SAFE_FALL, cfg(RedHulkConfig.SAFE_FALL, 20.0) * k, on);
    }

    private static void removeBody(ServerPlayer player) {
        float oldMax = player.getMaxHealth();
        for (Object[] m : new Object[][]{{Attributes.SCALE, BODY_SCALE}, {Attributes.MAX_HEALTH, BODY_HEALTH},
                {Attributes.ATTACK_DAMAGE, BODY_ATTACK}, {Attributes.MOVEMENT_SPEED, BODY_SPEED},
                {Attributes.ARMOR, BODY_ARMOR}, {Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS},
                {Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK}, {Attributes.ENTITY_INTERACTION_RANGE, BODY_REACH},
                {Attributes.BLOCK_INTERACTION_RANGE, BODY_BLOCK_REACH}, {Attributes.STEP_HEIGHT, BODY_STEP},
                {Attributes.JUMP_STRENGTH, BODY_JUMP}, {Attributes.SAFE_FALL_DISTANCE, BODY_SAFE_FALL}}) {
            @SuppressWarnings("unchecked")
            Holder<Attribute> attribute = (Holder<Attribute>) m[0];
            setModifier(player, attribute, (ResourceLocation) m[1], 0.0, false);
        }
        hold(player, false);
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

    // ---- who his blows reach ----

    /** Whether his area blows may hurt the creature (PvP, teams, pets, Creative, the area rule). */
    static boolean mayHurt(ServerPlayer player, LivingEntity target) {
        return target.isAlive() && !target.isSpectator() && AreaTargets.mayHurt(player, target, false);
    }

    /** Hurts the creature with a damage of his ({@code type}), the blow always landing (the stones' rule). */
    static boolean hurt(ServerPlayer player, LivingEntity target, ResourceKey<DamageType> type, float amount,
            @Nullable Entity direct) {
        return StonePowers.hurt(player.serverLevel(), player, target, type, amount, direct);
    }

    // ---- R: the Ground Smash ----

    /**
     * R: both fists high, then down into the ground: a ring runs out from his feet ({@code smashRadius}, one block a
     * tick) and every creature on the ground in it takes {@code smashDamage}, thrown up {@code smashLaunch} and out.
     * The ground under the ring bursts into its own crumbs (particles: no block is broken, unless {@code
     * smashBreaksBlocks} and the Hulk's rules let him break the soft ones round his feet).
     */
    public static boolean smash(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.RED_HULK_SMASH, now)) {
            return false;
        }
        data.startCooldown(HeroAbility.RED_HULK_SMASH, now, cfg(RedHulkConfig.SMASH_COOLDOWN, 80));
        State st = state(player.getUUID());
        st.smash = new Smash(now);
        send(player.serverLevel(), player, RedHulkFxPayload.SMASH, List.of(player.position()), 0);
        return true;
    }

    /** The Ground Smash: the fists up, down, then the ring. */
    static final class Smash {
        final long start;
        private final IntSet hit = new IntOpenHashSet();
        private Vec3 center = Vec3.ZERO;
        private float amount;

        Smash(long start) {
            this.start = start;
        }

        /** One tick, {@code t} after it began; whether it is over. */
        boolean tick(ServerPlayer player, PowerData data, int t) {
            ServerLevel level = player.serverLevel();
            double radius = cfg(RedHulkConfig.SMASH_RADIUS, 6.0);
            if (t < SMASH_WINDUP) {
                return false;
            }
            if (t == SMASH_WINDUP) {
                center = player.position();
                amount = (float) cfg(RedHulkConfig.SMASH_DAMAGE, 8.0);
                sound(level, center, ModSounds.RED_HULK_SMASH.get(), 1.8F, 0.9F + level.random.nextFloat() * 0.1F);
                send(level, player, RedHulkFxPayload.SMASH, List.of(center), (int) Math.ceil(radius));
                if (cfg(RedHulkConfig.SMASH_BREAKS_BLOCKS, false)) {
                    breakSoft(player, center);
                }
            }
            int k = t - SMASH_WINDUP;
            ring(player, k, k + 1.0);
            return k + 1 >= radius;
        }

        private void ring(ServerPlayer player, double from, double to) {
            ServerLevel level = player.serverLevel();
            double launch = cfg(RedHulkConfig.SMASH_LAUNCH, 0.9);
            AABB box = new AABB(center, center).inflate(to + 1.5, 3.0, to + 1.5);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player
                    && !hit.contains(e.getId()) && mayHurt(player, e))) {
                double distance = Math.hypot(e.getX() - center.x, e.getZ() - center.z);
                double half = e.getBbWidth() * 0.5;
                if (distance + half < from || distance - half > to || Math.abs(e.getY() - center.y) > 2.5
                        || !HulkAbilities.grounded(e)) {
                    continue;
                }
                hit.add(e.getId());
                if (hurt(player, e, ModDamageTypes.RED_HULK_SMASH, amount, player)) {
                    HulkAbilities.push(e, center, SMASH_PUSH, launch, player);
                }
            }
        }
    }

    /** With {@code smashBreaksBlocks}: the soft blocks round his feet, by the Hulk's rules (mode, gamerule, rights). */
    static List<BlockPos> breakSoft(ServerPlayer player, Vec3 feet) {
        List<BlockPos> broken = new ArrayList<>();
        Vec3 c = feet.add(0, 0.5, 0);
        BlockPos origin = BlockPos.containing(c);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
            if (pos.getCenter().distanceTo(c) > 3.0 || !player.level().getBlockState(pos).is(ModTags.HULK_SOFT)
                    || !com.kolodkingal.enhanced.hulk.HulkTerrain.mayBreak(player, pos,
                    com.kolodkingal.enhanced.hulk.HulkTerrain.Kind.SMASH)) {
                continue;
            }
            player.serverLevel().destroyBlock(pos, player.getRandom().nextInt(3) == 0, player);
            broken.add(pos.immutable());
        }
        return broken;
    }

    // ---- Sneak + R: Grab & Slam ----

    /** Sneak + R: the Hulk's Grab Smash with his numbers (5, 5, 7; a boss gets one punch of 7). */
    public static boolean grabSlam(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.RED_HULK_GRAB_SLAM, now)) {
            return false;
        }
        double[] damages = {cfg(RedHulkConfig.GRAB_DAMAGE_1, 5.0), cfg(RedHulkConfig.GRAB_DAMAGE_2, 5.0),
                cfg(RedHulkConfig.GRAB_DAMAGE_3, 7.0)};
        LivingEntity target = GrabSmash.start(player, data, damages, damages[2]);
        if (target == null) {
            return false;
        }
        data.startCooldown(HeroAbility.RED_HULK_GRAB_SLAM, now, cfg(RedHulkConfig.GRAB_COOLDOWN, 160));
        if (HulkAbilities.moving(player)) {
            state(player.getUUID()).hulkMove = HulkMove.GRAB;
        }
        send(player.serverLevel(), player, RedHulkFxPayload.GRAB, List.of(target.position()), target.getId());
        return true;
    }

    // ---- Sneak + G: the Roar ----

    /**
     * Sneak + G: every hostile creature within {@code roarRadius} (not the bosses) is Frightened (it runs from him) and
     * weakened; players he may fight are weakened; his anger rises by {@code angerOnRoar}.
     */
    public static List<LivingEntity> roar(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.RED_HULK_ROAR, now)) {
            return List.of();
        }
        data.startCooldown(HeroAbility.RED_HULK_ROAR, now, cfg(RedHulkConfig.ROAR_COOLDOWN, 300));
        double radius = cfg(RedHulkConfig.ROAR_RADIUS, 12.0);
        int fright = cfg(RedHulkConfig.ROAR_FRIGHT_TICKS, 80);
        int weakness = cfg(RedHulkConfig.ROAR_WEAKNESS_TICKS, 160);
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
        Anger.add(player, data, (float) cfg(RedHulkConfig.ANGER_ON_ROAR, 10.0), true);
        sound(level, player.position(), ModSounds.RED_HULK_ROAR.get(), 2.0F, 0.9F + level.random.nextFloat() * 0.1F);
        send(level, player, RedHulkFxPayload.ROAR, List.of(player.getEyePosition()), (int) radius);
        return touched;
    }

    // ---- Sneak + V: the Bound ----

    /** Sneak + V: the Hulk's Bound, {@code boundDistance} along his look, a landing blow of the jump's; no fall harm. */
    public static boolean bound(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.RED_HULK_BOUND, now)) {
            return false;
        }
        data.startCooldown(HeroAbility.RED_HULK_BOUND, now, cfg(RedHulkConfig.BOUND_COOLDOWN, 40));
        SuperLeap.bound(player, data, cfg(RedHulkConfig.BOUND_DISTANCE, 20.0),
                (float) cfg(RedHulkConfig.JUMP_LAND_DAMAGE, 4.0));
        state(player.getUUID()).hulkMove = HulkMove.BOUND;
        sound(player.serverLevel(), player.position(), ModSounds.RED_HULK_JUMP.get(), 1.0F, 1.15F);
        return true;
    }

    // ---- effects ----

    static void send(ServerLevel level, Entity who, int kind, List<Vec3> points, int arg) {
        List<Vec3> list = points.size() > RedHulkFxPayload.MAX_POINTS ? points.subList(0, RedHulkFxPayload.MAX_POINTS)
                : points;
        Net.sendToPlayersNear(level, null, who.getX(), who.getY(), who.getZ(), FX_RANGE,
                new RedHulkFxPayload(kind, who.getId(), List.copyOf(list), arg));
    }

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ---- the end of the power, of the life, of the session ----

    /** The power is being taken away (or a custom hero is remade): every move ends, the change is settled. */
    static void endAll(ServerPlayer player, PowerData data) {
        State st = STATES.get(player.getUUID());
        if (st != null) {
            st.smash = null;
            st.burst = null;
            if (st.jump != null) {
                st.jump = null;
            }
            if (st.absorbStart >= 0L) {
                EnergyAbsorb.stop(player, data, st);
            }
            st.hulkMove = HulkMove.NONE;
            RossForm.finish(player, data, st);
        }
        clearData(data);
        hold(player, false);
    }

    /** He died or left: what he was doing ends; his anger goes with a death (the attachment is not kept on death). */
    static void onGone(ServerPlayer player) {
        endAll(player, Powers.get(player));
        STATES.remove(player.getUUID());
    }

    static void clearData(PowerData data) {
        data.set(Live.RED_HULK_TRANSFORMING, false);
        data.set(Live.RED_HULK_JUMP_CHARGING, false);
        data.set(Live.RED_HULK_LEAPING, false);
        data.set(Live.RED_HULK_HEATED, false);
        data.set(Live.RED_HULK_ABSORBING, false);
        data.set(Live.RED_HULK_GRABBING, false);
    }

    static void clear() {
        STATES.clear();
    }

    private RedHulkAbilities() {
    }
}
