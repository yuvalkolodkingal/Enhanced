package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.network.SpiderClingPayload;
import com.kolodkingal.enhanced.network.SpiderFxPayload;
import com.kolodkingal.enhanced.network.SpiderSensePayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Spider-Man, Tom Holland's Peter Parker (DESIGN section 14): the server side of the ground-and-wall hero. He cannot
 * fly; he goes everywhere by web. R shoots a web at what he looks at (Sneak: a burst of webs in every direction), G zips
 * him to the point he aims at and lets the web vanish (Sneak: swing on it for as long as G is held), V is Wall Crawl
 * (Sneak: Spider-Sense), B the Iron Spider's mechanical legs (Sneak, twice: Instant Kill) and N the Black Eyes of
 * <em>Brand New Day</em>. He has three suits whose protection differs, and he is quicker, stronger and lighter than
 * anyone, and never held by a web.
 *
 * <p>His movement is the player's own client's, like Quicksilver's wall-run: vanilla lets a client move itself and the
 * server only checks. The server decides what others see (the {@link Live} bits), validates every web (range, line of
 * sight, cooldown, Hex, Chaos Shield), places the web blocks, applies the effects and the damage, and resets the "flying
 * is not enabled" kick counter while he hangs in the air. It never moves him. A vanilla server hears nothing of the keys
 * of a player who is not riding, so it reads that he clings from where he is and how he moves (see {@link #clingTick});
 * the pure rules are in {@link SpiderClimb} and {@link SpiderMath}.
 */
public final class SpiderServer implements HeroServer {
    // ---- the modifiers of his body ----
    private static final ResourceLocation SPEED = Enhanced.id("spider_speed");
    private static final ResourceLocation JUMP = Enhanced.id("spider_jump");
    private static final ResourceLocation STRENGTH = Enhanced.id("spider_strength");
    private static final ResourceLocation FALL = Enhanced.id("spider_fall");
    private static final ResourceLocation SUIT_ARMOR = Enhanced.id("spider_suit_armor");
    private static final ResourceLocation SUIT_TOUGHNESS = Enhanced.id("spider_suit_toughness");
    private static final ResourceLocation SUIT_KNOCKBACK = Enhanced.id("spider_suit_knockback");
    private static final ResourceLocation LEGS_REACH = Enhanced.id("spider_legs_reach");
    private static final ResourceLocation LEGS_FALL = Enhanced.id("spider_legs_fall");
    private static final ResourceLocation EYES_SPEED = Enhanced.id("spider_eyes_speed");
    private static final ResourceLocation EYES_STRENGTH = Enhanced.id("spider_eyes_strength");
    private static final ResourceLocation EYES_ATTACK_SPEED = Enhanced.id("spider_eyes_attack_speed");
    private static final ResourceLocation EYES_JUMP = Enhanced.id("spider_eyes_jump");
    private static final ResourceLocation EYES_KNOCKBACK = Enhanced.id("spider_eyes_knockback");
    private static final ResourceLocation EYES_FALL = Enhanced.id("spider_eyes_fall");

    /** The look of the Iron Spider: strong protection, the legs and Instant Kill, and a metal suit for Magneto. */
    public static final String IRON_SPIDER = "iron_spider";
    /** The look of the Brand New Day suit: modest protection and organic webs. */
    public static final String BRAND_NEW_DAY = "brand_new_day";

    /** The hand pose of a Web Shot shows this many ticks. */
    public static final int CAST_TICKS = 6;
    /** The spider-sense pose shows this many ticks. */
    public static final int SENSE_TICKS = 12;
    /** A surge that came by itself cannot be calmed for this many ticks. */
    public static final int PANIC_LOCK_TICKS = 100;
    /** The food level the Black Eyes need to start (the level at which sprinting stops). */
    public static final int EYES_MIN_FOOD = 6;
    /** The legs unfold in this many ticks; a zip that arrives holds its surface this many ticks whatever his motion. */
    private static final int ARRIVAL_GRACE = 8;
    /** A swing on the ground this many ticks in a row is let go (counted after its first ticks). */
    private static final int SWING_GROUND_TICKS = 10;
    private static final int SWING_GRACE = 10;
    /**
     * A step may be this many blocks longer for each tick before it that his position was held (he is no faster than that),
     * for up to {@link #HITCH_TICKS} ticks (and twice the step before it, which a slow client makes by catching up).
     */
    private static final double HITCH_STEP = 1.6;
    private static final int HITCH_TICKS = 12;
    /** What the cling's flag outlives the contact by. */
    private static final int CLING_HYSTERESIS = 4;
    /** What his client says he clings to counts for this many ticks (it says it again every few). */
    public static final int REPORT_TICKS = 12;
    /** How near a block must be to the side of him that his client says he clings to (the server's copy of him lags). */
    public static final double CLAIM_REACH = 0.45;
    /** A jump's rise slows to nothing in about this many ticks; hanging in the air before then is no cling. */
    private static final int ARC_TICKS = 7;
    /** The Black Eyes' crash: Slowness II (amplifier 1) and Weakness I. */
    private static final int CRASH_SLOWNESS = 1;
    /** The Black Eyes make a fall softer still (another quarter off the fall damage). */
    private static final double EYES_FALL_REDUCTION = 0.25;
    /** An Instant Kill has four legs, so it strikes at most four creatures at a time. */
    private static final int LEGS = 4;

    private static final Map<UUID, SpiderState> STATES = new HashMap<>();

    @Override
    public Power power() {
        return Power.SPIDER_MAN;
    }

    // ---- who and what ----

    public static boolean isSpiderMan(Entity entity) {
        return entity instanceof Player player && Powers.is(player, Power.SPIDER_MAN);
    }

    /** Spider-Man in the Iron Spider look: a metal suit, which Magneto can lift and whose nanites his strip scatters. */
    public static boolean isIronSpider(Entity entity) {
        return entity instanceof Player player && isIronLook(Powers.get(player));
    }

    private static boolean isIronLook(PowerData data) {
        return data.power() == Power.SPIDER_MAN && IRON_SPIDER.equals(Variants.current(data).id());
    }

    /** Whether his webs are organic: the Brand New Day look, or the Black Eyes in any look. */
    public static boolean organic(Player player, PowerData data) {
        return BRAND_NEW_DAY.equals(Variants.current(data).id()) || data.is(Live.BLACK_EYES);
    }

    /** The nanites of his Iron Spider suit are scattered by Magneto (the suit's protection, the legs and Instant Kill are off). */
    public static boolean isStripped(Player player) {
        SpiderState state = STATES.get(player.getUUID());
        return state != null && player.level().getGameTime() < state.stripUntil;
    }

    private static final int SPELLBOUND = 1;
    private static final int OTHER = 2;

    /**
     * What stops him acting: Billy's spell, or the other things that hold or carry a player (a spectator, a vehicle,
     * Mjolnir's pin, telekinesis, the cocoon of sand, Sokovia's lift, flight in Creative, an elytra).
     */
    private static int blockers(ServerPlayer player, PowerData data) {
        int mask = 0;
        if (player.hasEffect(ModEffects.SPELLBOUND)) {
            mask |= SPELLBOUND;
        }
        if (player.isSpectator() || player.isPassenger() || MjolnirPin.isPinned(player) || Telekinesis.isHeld(player)
                || player.hasEffect(ModEffects.ENTOMBED) || data.is(Live.LIFTED) || player.getAbilities().flying || player.isFallFlying()) {
            mask |= OTHER;
        }
        return mask;
    }

    /** Whether the hero cannot act, and every web, zip and swing he had ends. */
    public static boolean blocked(ServerPlayer player, PowerData data) {
        return blockers(player, data) != 0;
    }

    /** With an empty stomach there is nothing left to climb or to make webs with (not in Creative). */
    private static boolean starving(Player player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    private static SpiderState state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new SpiderState());
    }

    /** Whether the server remembers anything about this player (nothing is left once the power is gone). */
    public static boolean hasState(Player player) {
        return STATES.containsKey(player.getUUID());
    }

    /** How many of his web blocks are standing (the cap counts these). */
    public static int websStanding(ServerPlayer player) {
        SpiderState state = STATES.get(player.getUUID());
        return state == null ? 0 : SpiderWebs.standing(player.serverLevel(), state.webs);
    }

    /** The web blocks he has made and not lost count of yet (the cap removes the oldest). */
    public static int websTracked(Player player) {
        SpiderState state = STATES.get(player.getUUID());
        return state == null ? 0 : state.webs.size();
    }

    /** The anchor of the zip he is on, or null. */
    @Nullable
    public static Vec3 zipAnchor(Player player) {
        SpiderState state = STATES.get(player.getUUID());
        return state == null || state.zip == null ? null : state.zip.anchor;
    }

    /** Where the zip he is on ends (his feet), or null. */
    @Nullable
    public static Vec3 zipStand(Player player) {
        SpiderState state = STATES.get(player.getUUID());
        return state == null || state.zip == null ? null : state.zip.stand;
    }

    /** The anchor of the swing he is on, or null. */
    @Nullable
    public static Vec3 swingAnchor(Player player) {
        SpiderState state = STATES.get(player.getUUID());
        return state == null || state.swing == null ? null : state.swing.anchor;
    }

    /** The rope length of the swing he is on, or 0. */
    public static double swingRope(Player player) {
        SpiderState state = STATES.get(player.getUUID());
        return state == null || state.swing == null ? 0.0 : state.swing.rope;
    }

    /** The surface the server thinks he clings to. */
    public static SpiderClimb.Surface surfaceOf(Player player) {
        SpiderState state = STATES.get(player.getUUID());
        return state == null ? SpiderClimb.Surface.NONE : state.surface;
    }

    // ---- the keys ----

    @Override
    public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        if (ability == 2 && !down) {
            // A swing lasts while G is held, and letting go always gets through.
            SpiderState state = STATES.get(player.getUUID());
            if (state != null && state.swing != null) {
                endSwing(player, data, state, 5);
            }
            return;
        }
        if (!down) {
            return;
        }
        if (blocked(player, data)) {
            if (ability == 2) {
                refuse(player, player.isShiftKeyDown());
            }
            return;
        }
        SpiderState state = state(player);
        boolean sneak = player.isShiftKeyDown();
        switch (ability) {
            case 1 -> {
                if (sneak) {
                    webBurst(player, data, state);
                } else {
                    webShot(player, data, state);
                }
            }
            case 2 -> {
                if (sneak) {
                    webSwing(player, data, state);
                } else {
                    webZip(player, data, state);
                }
            }
            case 3 -> {
                if (sneak) {
                    toggleSense(player, data);
                } else {
                    toggleCrawl(player, data);
                }
            }
            case 4 -> {
                if (sneak) {
                    instantKill(player, data, state);
                } else {
                    toggleLegs(player, data);
                }
            }
            case 5 -> blackEyes(player, data, state);
            default -> {
            }
        }
    }

    // ---- the body ----

    /**
     * His body (speed, jump, strength, softer falls) and his suit's protection by look, plus the legs' reach and soft
     * landing and the Black Eyes' surge, as permanent modifiers; idempotent, and removed from everyone who is not
     * Spider-Man.
     */
    @Override
    public void applyBody(ServerPlayer player, PowerData data) {
        boolean spider = data.power() == Power.SPIDER_MAN;
        SpiderState state = spider ? STATES.get(player.getUUID()) : null;
        String look = spider ? Variants.current(data).id() : "";
        boolean stripped = state != null && player.level().getGameTime() < state.stripUntil;
        boolean iron = spider && IRON_SPIDER.equals(look) && !stripped;
        boolean legs = iron && data.toggle(Toggle.SPIDER_LEGS);
        boolean eyes = spider && data.is(Live.BLACK_EYES);
        AttributeModifier.Operation base = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        AttributeModifier.Operation total = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, SPEED, SpiderConfig.SPEED_BONUS.get(), spider, base);
        PowerTicker.setModifier(player, Attributes.JUMP_STRENGTH, JUMP, SpiderConfig.JUMP_BONUS.get(), spider, base);
        PowerTicker.setModifier(player, Attributes.ATTACK_DAMAGE, STRENGTH, SpiderConfig.STRENGTH_BONUS.get(), spider);
        PowerTicker.setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL, -SpiderConfig.FALL_DAMAGE_REDUCTION.get(), spider);

        double armor = 0.0;
        double toughness = 0.0;
        double knockback = 0.0;
        if (BRAND_NEW_DAY.equals(look)) {
            armor = SpiderConfig.BRAND_NEW_DAY_ARMOR.get();
            toughness = SpiderConfig.BRAND_NEW_DAY_TOUGHNESS.get();
        } else if (iron) {
            armor = SpiderConfig.IRON_SPIDER_ARMOR.get();
            toughness = SpiderConfig.IRON_SPIDER_TOUGHNESS.get();
            knockback = SpiderConfig.IRON_SPIDER_KNOCKBACK_RESISTANCE.get();
        } else if (spider && !IRON_SPIDER.equals(look)) {
            armor = SpiderConfig.HOMEMADE_ARMOR.get();
        }
        PowerTicker.setModifier(player, Attributes.ARMOR, SUIT_ARMOR, armor, armor != 0.0);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, SUIT_TOUGHNESS, toughness, toughness != 0.0);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, SUIT_KNOCKBACK, knockback, knockback != 0.0);

        PowerTicker.setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, LEGS_REACH, SpiderConfig.LEGS_REACH_BONUS.get(), legs);
        // He lands on his legs: no fall damage at all (the multiplier cannot go below zero).
        PowerTicker.setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, LEGS_FALL, -1.0, legs);

        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, EYES_SPEED, SpiderConfig.BLACK_EYES_SPEED_BONUS.get(), eyes, total);
        PowerTicker.setModifier(player, Attributes.ATTACK_DAMAGE, EYES_STRENGTH, SpiderConfig.BLACK_EYES_STRENGTH_BONUS.get(), eyes);
        PowerTicker.setModifier(player, Attributes.ATTACK_SPEED, EYES_ATTACK_SPEED, SpiderConfig.BLACK_EYES_ATTACK_SPEED_BONUS.get(), eyes, base);
        PowerTicker.setModifier(player, Attributes.JUMP_STRENGTH, EYES_JUMP, SpiderConfig.BLACK_EYES_JUMP_BONUS.get(), eyes, base);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, EYES_KNOCKBACK, SpiderConfig.BLACK_EYES_KNOCKBACK_RESISTANCE.get(), eyes);
        PowerTicker.setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, EYES_FALL, -EYES_FALL_REDUCTION, eyes);
    }

    // ---- every tick ----

    @Override
    public void tick(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        SpiderState state = state(player);
        int blockers = blockers(player, data);

        // The suit decides what he has: the legs are the Iron Spider's alone, and not while the nanites are scattered.
        boolean stripped = now < state.stripUntil;
        if ((!isIronLook(data) || stripped) && data.toggle(Toggle.SPIDER_LEGS)) {
            data.setToggle(Toggle.SPIDER_LEGS, false);
        }

        if (blockers != 0) {
            if (state.zip != null) {
                endZip(player, data, state, 3);
            }
            if (state.swing != null) {
                endSwing(player, data, state, 3);
            }
        }
        if (state.zip != null) {
            zipTick(player, data, state, now);
        }
        if (state.swing != null) {
            swingTick(player, data, state, now);
        }
        clingTick(player, data, state, now, blockers);
        legsTick(player, data, state, now, blockers != 0);
        eyesTick(player, data, state, now);
        senseTick(player, data, state, now);

        data.set(Live.CASTING, now < state.castUntil);
        data.set(Live.SENSING, now < state.senseUntil);
        state.lastPosition = player.position();
    }

    // ---- messages, costs and effects ----

    private static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    /** Food exhaustion, times the organic factor if his webs are organic (nothing in Creative). */
    private static void spend(ServerPlayer player, double exhaustion, boolean organic) {
        double factor = organic ? SpiderConfig.ORGANIC_WEB_COST_FACTOR.get() : 1.0;
        player.causeFoodExhaustion((float) (exhaustion * factor));
    }

    private static void send(ServerLevel level, ServerPlayer player, SpiderFxPayload payload) {
        PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), 64.0, payload);
    }

    /**
     * The key of a zip or a swing was refused (the Hex, a shield, an empty stomach, no anchor, a cooldown, a hold on him):
     * his own client has already begun the movement on its own prediction, so it is told at once that the web never
     * came, and it stops (code 2 of {@link SpiderFxPayload}). Nobody else is told: nobody else saw anything.
     */
    private static void refuse(ServerPlayer player, boolean swing) {
        PacketDistributor.sendToPlayer(player, new SpiderFxPayload(swing ? SpiderFxPayload.Kind.SWING_END : SpiderFxPayload.Kind.ZIP_END,
                player.getId(), player.position(), player.position(), Vec3.ZERO, 2, 0F, false, 0L, List.of()));
    }

    /** About where his hand is (the client uses the real one when it can see it). */
    private static Vec3 hand(ServerPlayer player) {
        Vec3 look = player.getViewVector(1.0F);
        double yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        if (player.getMainArm() == HumanoidArm.LEFT) {
            right = right.scale(-1);
        }
        return player.getEyePosition().add(look.scale(0.4)).add(right.scale(0.3)).add(0, -0.3, 0);
    }

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ---- Ability 1: Web Shot and Web Burst ----

    private static void webShot(ServerPlayer player, PowerData data, SpiderState state) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now < state.shotReadyAt) {
            return;
        }
        boolean organic = organic(player, data);
        if (organic && starving(player)) {
            say(player, "message.enhanced.web.hungry");
            return;
        }
        HitResult hit = Raycasts.any(player, SpiderConfig.WEB_SHOT_RANGE.get(), SpiderWebs::webTarget, 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            // A hand must not thrust at nothing: no hunger, no timer, no pose.
            say(player, "message.enhanced.web.no_target");
            return;
        }
        Vec3 from = hand(player);
        Vec3 eye = player.getEyePosition();
        Vec3 to = hit.getLocation();
        Vec3 normal = Vec3.ZERO;
        int targetId = -1;
        boolean shielded = false;
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, eye, to, 1.0F);
        if (shield != null) {
            // A complete Chaos Shield is solid to a web: it ends on the sphere and costs the shield.
            to = shield.point();
            shielded = true;
            payShield(player, shield, 2.0F);
            say(player, "message.enhanced.web.shield");
        } else if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            targetId = SpiderWebs.body(target).getId();
            int ticks = (int) Math.round(SpiderConfig.WEBBED_TICKS.get() * (organic ? SpiderConfig.ORGANIC_WEB_FACTOR.get() : 1.0));
            if (SpiderWebs.web(player, target, ticks) && SpiderWebs.body(target) instanceof LivingEntity living) {
                SpiderWebs.cocoon(level, player, state.webs, living, organic);
            }
        } else if (hit instanceof BlockHitResult blockHit) {
            normal = new Vec3(blockHit.getDirection().getStepX(), blockHit.getDirection().getStepY(), blockHit.getDirection().getStepZ());
            SpiderWebs.patch(level, player, state.webs, blockHit.getBlockPos(), blockHit.getDirection(), SpiderConfig.WEB_PATCH_RADIUS.get(), organic);
        }
        spend(player, SpiderConfig.WEB_SHOT_EXHAUSTION.get(), organic);
        state.shotReadyAt = now + SpiderConfig.WEB_SHOT_COOLDOWN.get();
        state.castUntil = now + CAST_TICKS;
        data.set(Live.CASTING, true);
        sound(level, player.position(), ModSounds.WEB_SHOT.get(), 0.9F, 0.92F + player.getRandom().nextFloat() * 0.16F);
        sound(level, to, ModSounds.WEB_SPLAT.get(), shielded ? 0.5F : 0.7F, 1.0F);
        send(level, player, new SpiderFxPayload(SpiderFxPayload.Kind.SHOT, player.getId(), from, to, normal, targetId, 0F, organic, 0L, List.of()));
    }

    /** A shield (or a Hex Barrier) that a web ended on pays for it (only if PvP lets him harm its owner; a Hex's wall pays nothing). */
    private static void payShield(ServerPlayer spider, WandaAbilities.ShieldStop shield, float amount) {
        WandaAbilities.pay(shield, spider, amount);
    }

    private static void webBurst(ServerPlayer player, PowerData data, SpiderState state) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(1, now)) {
            return;
        }
        boolean organic = organic(player, data);
        if (organic && starving(player)) {
            say(player, "message.enhanced.web.hungry");
            return;
        }
        double range = SpiderConfig.WEB_BURST_RANGE.get();
        Vec3 chest = player.position().add(0, player.getBbHeight() * 0.7, 0);

        // Everyone near he can see (and may web) gets at least one strand.
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity creature : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))) {
            Vec3 centre = creature.position().add(0, creature.getBbHeight() * 0.5, 0);
            if (SpiderWebs.webTarget(creature) && creature.distanceTo(player) <= range && AreaTargets.mayHurt(player, creature, false)
                    && inSight(level, player, chest, creature) && WandaAbilities.shieldAcross(level, player, chest, centre, 1.0F) == null) {
                targets.add(creature);
            }
        }
        if (targets.isEmpty()) {
            say(player, "message.enhanced.burst.no_target");
            return;
        }
        targets.sort(Comparator.comparingDouble(creature -> creature.distanceToSqr(player)));

        int count = SpiderConfig.WEB_BURST_STRANDS.get();
        long seed = player.getRandom().nextLong();
        Vec3[] strands = SpiderMath.fibonacciSphere(count, seed);
        boolean[] aimed = new boolean[count];
        for (LivingEntity target : targets) {
            Vec3 toward = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(chest).normalize();
            int best = -1;
            for (int i = 0; i < count; i++) {
                if (!aimed[i] && (best < 0 || strands[i].dot(toward) > strands[best].dot(toward))) {
                    best = i;
                }
            }
            if (best < 0) {
                break;
            }
            strands[best] = toward;
            aimed[best] = true;
        }

        Set<Integer> webbed = new LinkedHashSet<>();
        List<LivingEntity> toWeb = new ArrayList<>(targets);
        Set<BlockPos> blockCells = new LinkedHashSet<>();
        List<Vec3> ends = new ArrayList<>(count);
        int blockedByShield = 0;
        for (Vec3 direction : strands) {
            Vec3 end = chest.add(direction.scale(range));
            BlockHitResult block = level.clip(new ClipContext(chest, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            boolean hitBlock = block.getType() != HitResult.Type.MISS;
            if (hitBlock) {
                end = block.getLocation();
            }
            EntityHitResult entity = Raycasts.entityBetween(level, player, chest, end, 0.0,
                    e -> SpiderWebs.webTarget(e) && SpiderWebs.body(e) instanceof LivingEntity living && AreaTargets.mayHurt(player, living, false));
            LivingEntity struck = null;
            if (entity != null) {
                end = entity.getLocation();
                hitBlock = false;
                struck = (LivingEntity) SpiderWebs.body(entity.getEntity());
            }
            WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, chest, end, 1.0F);
            if (shield != null) {
                // The strand ends on the sphere and pays it half a point.
                end = shield.point();
                hitBlock = false;
                struck = null;
                blockedByShield++;
                payShield(player, shield, 0.5F);
            }
            if (struck != null && webbed.add(struck.getId()) && !toWeb.contains(struck)) {
                toWeb.add(struck);
            }
            if (hitBlock) {
                blockCells.add(block.getBlockPos().relative(block.getDirection()));
            }
            ends.add(end);
        }

        int ticks = (int) Math.round(SpiderConfig.WEB_BURST_WEBBED_TICKS.get() * (organic ? SpiderConfig.ORGANIC_WEB_FACTOR.get() : 1.0));
        for (LivingEntity creature : toWeb) {
            SpiderWebs.web(player, creature, ticks);
        }
        int splats = 0;
        for (BlockPos cell : blockCells) {
            if (SpiderWebs.place(level, player, state.webs, cell, organic) && splats++ < 3) {
                sound(level, Vec3.atCenterOf(cell), ModSounds.WEB_SPLAT.get(), 0.7F, 0.9F + player.getRandom().nextFloat() * 0.2F);
            }
        }
        spend(player, SpiderConfig.WEB_BURST_EXHAUSTION.get(), organic);
        data.startCooldown(1, now, SpiderConfig.WEB_BURST_COOLDOWN.get());
        sound(level, player.position(), ModSounds.WEB_BURST.get(), 1.0F, 1.0F);
        send(level, player, new SpiderFxPayload(SpiderFxPayload.Kind.BURST, player.getId(), chest, chest, Vec3.ZERO, blockedByShield, 0F, organic,
                seed, ends));
    }

    /** Whether the line from {@code from} to the creature's middle or eyes is free of blocks. */
    private static boolean inSight(ServerLevel level, ServerPlayer player, Vec3 from, LivingEntity creature) {
        for (Vec3 to : new Vec3[]{creature.position().add(0, creature.getBbHeight() * 0.5, 0), creature.getEyePosition()}) {
            if (level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    // ---- Ability 2: Web Zip and Web Swing ----

    private static void webZip(ServerPlayer player, PowerData data, SpiderState state) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(2, now)) {
            refuse(player, false);
            return;
        }
        boolean organic = organic(player, data);
        if (organic && starving(player)) {
            say(player, "message.enhanced.web.hungry");
            refuse(player, false);
            return;
        }
        double range = SpiderConfig.ZIP_RANGE.get();
        double kick = SpiderConfig.ZIP_KICK_DAMAGE.get();
        HitResult hit = kick > 0.0 ? Raycasts.any(player, range, e -> kickable(player, e), 1.0F) : Raycasts.block(player, range, 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            // (3.3: the line ended at the wall of a Hex that keeps him out: it is the Hex that refuses him)
            Vec3 far = player.getEyePosition().add(player.getLookAngle().scale(range));
            boolean walled = Shelters.hexClip(level, player, player.getEyePosition(), far) != far;
            say(player, walled ? "message.enhanced.zip.hex" : "message.enhanced.zip.no_anchor");
            refuse(player, false);
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 anchor = hit.getLocation();
        Direction face;
        Vec3 stand;
        UUID creature = null;
        int creatureId = -1;
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = SpiderWebs.body(entityHit.getEntity());
            face = Direction.getNearest(eye.x - anchor.x, 0, eye.z - anchor.z);
            stand = standBefore(player, target);
            creature = target.getUUID();
            creatureId = target.getId();
        } else {
            BlockHitResult block = (BlockHitResult) hit;
            face = block.getDirection();
            stand = SpiderMath.standAt(spot -> Teleports.fits(player, spot), anchor, face, player.getBbHeight());
        }
        if (stand == null) {
            say(player, "message.enhanced.zip.no_anchor");
            refuse(player, false);
            return;
        }
        if (Teleports.crossesHex(level, player, stand)) {
            say(player, "message.enhanced.zip.hex");
            refuse(player, false);
            return;
        }
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, eye, anchor, 1.0F);
        if (shield != null) {
            payShield(player, shield, 2.0F);
            say(player, "message.enhanced.web.shield");
            refuse(player, false);
            return;
        }
        if (state.swing != null) {
            endSwing(player, data, state, 1);
        }
        state.zip = new SpiderState.Line(anchor, face, stand, 0.0, now, creature, player.position());
        dropCling(state, data);
        data.set(Live.ZIPPING, true);
        data.startCooldown(2, now, SpiderConfig.ZIP_COOLDOWN.get());
        spend(player, SpiderConfig.ZIP_EXHAUSTION.get(), organic);
        sound(level, player.position(), ModSounds.WEB_ZIP.get(), 0.9F, 1.0F);
        send(level, player, new SpiderFxPayload(SpiderFxPayload.Kind.ZIP_START, player.getId(), hand(player), anchor,
                new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()), creatureId, 0F, organic, 0L, List.of(stand)));
    }

    /** What a zip may be aimed at: a creature he may kick, otherwise only the blocks beyond it. */
    private static boolean kickable(ServerPlayer player, Entity entity) {
        Entity body = SpiderWebs.body(entity);
        return SpiderWebs.webTarget(entity) && body instanceof LivingEntity living && !living.getType().is(ModTags.WEB_IMMUNE)
                && AreaTargets.mayHurt(player, living, false);
    }

    /** Where he lands in front of a creature he zips to: the first spot that fits on his side of it. */
    @Nullable
    private static Vec3 standBefore(ServerPlayer player, Entity target) {
        Vec3 away = player.position().subtract(target.position());
        double flat = Math.hypot(away.x, away.z);
        Vec3 back = flat < 1.0E-3 ? new Vec3(0, 0, 1) : new Vec3(away.x / flat, 0, away.z / flat);
        for (double gap : new double[]{0.9, 1.3, 1.8}) {
            Vec3 spot = target.position().add(back.scale(target.getBbWidth() * 0.5 + gap));
            if (Teleports.fits(player, spot)) {
                return spot;
            }
        }
        return null;
    }

    private static void webSwing(ServerPlayer player, PowerData data, SpiderState state) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(2, now) || state.swing != null) {
            // (A swing that is already on is not told it was refused: it goes on.)
            if (state.swing == null) {
                refuse(player, true);
            }
            return;
        }
        boolean organic = organic(player, data);
        if (organic && starving(player)) {
            say(player, "message.enhanced.web.hungry");
            refuse(player, true);
            return;
        }
        double range = SpiderConfig.SWING_RANGE.get();
        BlockHitResult hit = Raycasts.block(player, range, 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            say(player, "message.enhanced.swing.no_anchor");
            refuse(player, true);
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 anchor = hit.getLocation();
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, eye, anchor, 1.0F);
        if (shield != null) {
            payShield(player, shield, 2.0F);
            say(player, "message.enhanced.web.shield");
            refuse(player, true);
            return;
        }
        double rope = Mth.clamp(eye.distanceTo(anchor), 4.0, range);
        if (state.zip != null) {
            endZip(player, data, state, 1);
        }
        state.swing = new SpiderState.Line(anchor, hit.getDirection(), anchor, rope, now, null, player.position());
        dropCling(state, data);
        data.set(Live.SWINGING, true);
        sound(level, player.position(), ModSounds.WEB_SHOT.get(), 0.9F, 0.85F);
        Direction face = hit.getDirection();
        send(level, player, new SpiderFxPayload(SpiderFxPayload.Kind.SWING_START, player.getId(), hand(player), anchor,
                new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()), -1, (float) rope, organic, 0L, List.of()));
    }

    /** The web is let go: the reason is one of the codes of {@link SpiderFxPayload} (0 arrived, 1 cancelled, 5 released ...). */
    private static void endSwing(ServerPlayer player, PowerData data, SpiderState state, int reason) {
        if (state.swing == null) {
            return;
        }
        Vec3 anchor = state.swing.anchor;
        state.swing = null;
        data.set(Live.SWINGING, false);
        send(player.serverLevel(), player, new SpiderFxPayload(SpiderFxPayload.Kind.SWING_END, player.getId(), hand(player), anchor, Vec3.ZERO,
                reason, 0F, false, 0L, List.of()));
    }

    /**
     * The zip is over: the web vanishes. On arrival at a wall or a ceiling with Wall Crawl on he clings to it at once (to
     * go on to the next wall), and a zip aimed at a creature kicks it.
     */
    private static void endZip(ServerPlayer player, PowerData data, SpiderState state, int reason) {
        SpiderState.Line zip = state.zip;
        if (zip == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        state.zip = null;
        data.set(Live.ZIPPING, false);
        send(level, player, new SpiderFxPayload(SpiderFxPayload.Kind.ZIP_END, player.getId(), hand(player), zip.anchor, Vec3.ZERO, reason, 0F, false,
                0L, List.of()));
        if (reason != 0) {
            return;
        }
        sound(level, player.position(), ModSounds.WEB_STICK.get(), 0.8F, 1.0F);
        if (zip.kick != null && level.getEntity(zip.kick) instanceof LivingEntity target && target.isAlive() && target.distanceTo(player) <= 3.0
                && kickable(player, target)) {
            kick(player, target);
        } else if (zip.kick == null && data.toggle(Toggle.WALL_CRAWL)
                && (zip.face == Direction.DOWN || zip.face.getAxis().isHorizontal())) {
            // (Standing on top of a block he simply lands.)
            state.surface = new SpiderClimb.Surface(zip.face == Direction.DOWN ? SpiderClimb.Kind.CEILING : SpiderClimb.Kind.WALL, zip.face);
            state.clingGraceUntil = level.getGameTime() + ARRIVAL_GRACE;
            state.clingUntil = level.getGameTime() + CLING_HYSTERESIS;
        }
    }

    /** The kick at the end of a zip aimed at a creature. */
    private static void kick(ServerPlayer player, LivingEntity target) {
        double damage = SpiderConfig.ZIP_KICK_DAMAGE.get();
        if (damage <= 0.0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        DamageSource source = ModDamageTypes.source(level, ModDamageTypes.WEB_KICK, player, player);
        if (target.hurt(source, (float) damage)) {
            Vec3 away = player.position().subtract(target.position());
            target.knockback(0.2, away.x, away.z);
            target.hurtMarked = true;
        }
    }

    private static void zipTick(ServerPlayer player, PowerData data, SpiderState state, long now) {
        SpiderState.Line zip = state.zip;
        Vec3 position = player.position();
        keepAloft(player);
        int reason = lineEnd(player, zip, position, now, SpiderConfig.ZIP_MAX_TICKS.get(), SpiderConfig.ZIP_RANGE.get(), zip.kick == null);
        if (reason < 0) {
            double distance = position.distanceTo(zip.stand);
            if (distance < 1.1) {
                reason = 0;
            } else {
                double step = position.distanceTo(zip.last);
                if (step > 0.1) {
                    zip.moved = true;
                }
                zip.stalled = zip.moved && step < 0.05 ? zip.stalled + 1 : 0;
                zip.nearest = Math.min(zip.nearest, distance);
                if (zip.stalled >= 5) {
                    // He stopped against something.
                    reason = distance <= 3.0 ? 0 : 1;
                } else if (zip.moved && distance > zip.nearest + 1.5) {
                    // He went away from the stand point: he leapt off (Space cancels a zip).
                    reason = 1;
                }
            }
        }
        zip.last = position;
        if (reason >= 0) {
            endZip(player, data, state, reason);
        }
    }

    private static void swingTick(ServerPlayer player, PowerData data, SpiderState state, long now) {
        SpiderState.Line swing = state.swing;
        Vec3 position = player.position();
        keepAloft(player);
        int reason = lineEnd(player, swing, position, now, SpiderConfig.SWING_MAX_TICKS.get(), SpiderConfig.SWING_RANGE.get(), true);
        if (reason < 0) {
            long age = now - swing.start;
            swing.groundTicks = player.onGround() && age >= SWING_GRACE ? swing.groundTicks + 1 : 0;
            if (swing.groundTicks >= SWING_GROUND_TICKS) {
                reason = 1;
            } else if (player.getEyePosition().distanceTo(swing.anchor) > swing.rope * 1.25 + 1.5) {
                // The web is slack no more than that: he let go and leapt.
                reason = 1;
            } else {
                spend(player, SpiderConfig.SWING_EXHAUSTION.get(), organic(player, data));
            }
        }
        swing.last = position;
        if (reason >= 0) {
            endSwing(player, data, state, reason);
        }
    }

    /**
     * The checks every tick of a zip or a swing: the anchor block is still solid (3 and 6 for a zip aimed at a creature
     * excepted), he is not further than the range plus 3 from it, the time is not up, he is not blocked and did not jump
     * more than 3 blocks (a teleport or the Hex). Returns the reason it ends (see {@link SpiderFxPayload}), or -1.
     */
    private static int lineEnd(ServerPlayer player, SpiderState.Line line, Vec3 position, long now, int maxTicks, double range, boolean needsBlock) {
        if (blocked(player, Powers.get(player))) {
            return 3;
        }
        // A hitch of the connection or of the server (ticks run in a burst to catch up), or a client slower than the server,
        // gives his position in steps of several ticks at once: a step is a jump only beyond what the ticks it was held
        // could hold, and twice the step before it.
        double step = position.distanceTo(line.last);
        if (step > 3.0 + HITCH_STEP * Math.min(line.still, HITCH_TICKS) + 2.0 * line.lastStep) {
            return 1;
        }
        boolean held = Math.hypot(position.x - line.last.x, position.z - line.last.z) < 0.05
                && Math.abs(position.y - line.last.y) < 0.2;
        line.still = held ? line.still + 1 : 0;
        line.lastStep = held ? 0.0 : step;
        if (needsBlock) {
            BlockPos behind = BlockPos.containing(line.anchor.subtract(new Vec3(line.face.getStepX(), line.face.getStepY(), line.face.getStepZ()).scale(0.1)));
            BlockState state = player.serverLevel().getBlockState(behind);
            if (state.getCollisionShape(player.serverLevel(), behind).isEmpty()) {
                return 6;
            }
            if (player.getEyePosition().distanceTo(line.anchor) > range + 3.0) {
                return 6;
            }
        }
        if (now - line.start >= maxTicks) {
            return 4;
        }
        return -1;
    }

    /** While he hangs on a web the vanilla "flying is not enabled" kick must not count the ticks, and a fall does not add up. */
    private static void keepAloft(ServerPlayer player) {
        player.resetFallDistance();
        if (player.connection != null) {
            player.connection.aboveGroundTickCount = 0;
        }
    }

    // ---- Ability 3: Wall Crawl and Spider-Sense ----

    private static void toggleCrawl(ServerPlayer player, PowerData data) {
        boolean on = !data.toggle(Toggle.WALL_CRAWL);
        data.setToggle(Toggle.WALL_CRAWL, on);
        say(player, on ? "message.enhanced.wall_crawl.on" : "message.enhanced.wall_crawl.off");
    }

    private static void toggleSense(ServerPlayer player, PowerData data) {
        boolean on = !data.toggle(Toggle.SPIDER_SENSE);
        data.setToggle(Toggle.SPIDER_SENSE, on);
        say(player, on ? "message.enhanced.spider_sense.on" : "message.enhanced.spider_sense.off");
    }

    /** He lets go of the wall (a zip or a swing starts, or the toggle goes off). */
    private static void dropCling(SpiderState state, PowerData data) {
        state.surface = SpiderClimb.Surface.NONE;
        state.clingUntil = 0;
        state.clingGraceUntil = 0;
        data.set(Live.CLINGING_WALL, false);
        data.set(Live.CLINGING_CEILING, false);
    }

    /**
     * The flag others see while he sticks to a wall or hangs from a ceiling. The movement is the player's own client's;
     * the server hears nothing of the keys of a player who is not riding, so it reads the flag from where he is: the same
     * {@link SpiderClimb#detect} finds what he touches, and he is held to it when he is in the air and not falling (the
     * fall check of the last three ticks: a body that is falling, or rising the way a jump does, is not clinging), or
     * has just arrived by a zip. On the ground for two ticks he lets go; the flag outlives lost contact by four ticks.
     * While it is up the fall counts for nothing, nor does the kick counter, and hunger goes down while he moves.
     * (The keys the server does have, a riding player's, only help the choice of wall at a corner.)
     */
    private static void clingTick(ServerPlayer player, PowerData data, SpiderState state, long now, int blockers) {
        Vec3 position = player.position();
        // The heights of the last three ticks.
        if (state.lastPosition != null) {
            state.y3 = state.y2;
            state.y2 = state.y1;
            state.y1 = state.lastPosition.y;
            state.samples = Math.min(state.samples + 1, 3);
        }
        state.moving = state.lastPosition != null && position.distanceToSqr(state.lastPosition) > 1.0E-4;

        boolean frozenInPlace = blockers == SPELLBOUND;
        boolean free = data.toggle(Toggle.WALL_CRAWL) && state.zip == null && state.swing == null && (blockers == 0 || frozenInPlace)
                && !player.isInWaterOrBubble() && !player.isInLava() && !starving(player);
        Vec3 wish = SpiderClimb.wish(Integer.signum(Math.round(player.zza * 2.0F)), Integer.signum(Math.round(player.xxa * 2.0F)), player.getYRot());
        SpiderClimb.Surface touched = free ? SpiderClimb.detect(player.level(), player, wish, state.surface) : SpiderClimb.Surface.NONE;

        boolean holding = false;
        if (state.reporting) {
            // His client says what he clings to; the server only checks that there is something to cling to there, and does
            // not guess from the way he moves (a crawl down a wall looks like a fall, a climb like the arc of a jump).
            SpiderClimb.Surface told = free && now - state.reportedAt <= REPORT_TICKS ? state.reported : SpiderClimb.Surface.NONE;
            state.groundTicks = 0;
            if (told.touches() && SpiderClimb.near(player.level(), player, told, CLAIM_REACH)) {
                holding = true;
                touched = told;
            } else if (now - state.reportedAt <= REPORT_TICKS && !state.reported.touches()) {
                // He has let go: the flag goes at once (the grace of a zip's arrival below does not outlive his word).
                state.clingUntil = 0;
                state.clingGraceUntil = 0;
            }
        } else if (touched.touches()) {
            if (player.onGround()) {
                state.groundTicks++;
            } else {
                // (The first tick off the ground is a jump or a climb: it cannot be told which until the next tick.)
                boolean justLeftTheGround = state.groundTicks > 0;
                state.groundTicks = 0;
                if (leaping(state, position.y)) {
                    state.arcUntil = now + ARC_TICKS;
                }
                holding = !justLeftTheGround && now > state.arcUntil && !falling(state, position.y);
            }
        } else if (player.onGround()) {
            state.groundTicks++;
        } else {
            state.groundTicks = 0;
        }
        if (free && now < state.clingGraceUntil && state.surface.touches() && !player.onGround()) {
            // He has just arrived by a zip: he is on the wall whatever he is doing this tick.
            holding = true;
            touched = touched.touches() ? touched : state.surface;
        }
        if (!free || state.groundTicks >= 2) {
            state.clingUntil = 0;
            state.clingGraceUntil = 0;
        } else if (holding) {
            state.surface = touched;
            state.clingUntil = now + CLING_HYSTERESIS;
        }
        boolean active = free && state.surface.touches() && now <= state.clingUntil;
        data.set(Live.CLINGING_WALL, active && state.surface.kind() == SpiderClimb.Kind.WALL);
        data.set(Live.CLINGING_CEILING, active && state.surface.kind() == SpiderClimb.Kind.CEILING);
        if (active) {
            keepAloft(player);
            // His own client moves him: the server's copy of him must not gather a fall of its own between two positions
            // (it would sag him off the ceiling, and the next position would look like a climb).
            Vec3 motion = player.getDeltaMovement();
            if (motion.y < 0.0) {
                player.setDeltaMovement(motion.x, 0.0, motion.z);
            }
            if (state.moving) {
                player.causeFoodExhaustion(SpiderConfig.CRAWL_EXHAUSTION.get().floatValue());
            }
        } else if (!state.surface.equals(SpiderClimb.Surface.NONE) && now > state.clingUntil) {
            state.surface = SpiderClimb.Surface.NONE;
        }
    }

    /**
     * His own client says what he clings to ({@code kind} 0 nothing, 1 a wall, 2 a ceiling, {@code normal} the 3D data value
     * of the direction the surface faces). It is believed only while a block is within {@link #CLAIM_REACH} of that side of
     * his body, and only for a Spider-Man. From his first word the server stops guessing from his motion.
     */
    public static void onClingReport(ServerPlayer player, int kind, int normal) {
        if (!isSpiderMan(player)) {
            return;
        }
        SpiderState state = state(player);
        state.reporting = true;
        state.reportedAt = player.serverLevel().getGameTime();
        Direction direction = Direction.from3DDataValue(Math.floorMod(normal, 6));
        if (kind == SpiderClingPayload.WALL && direction.getAxis().isHorizontal()) {
            state.reported = new SpiderClimb.Surface(SpiderClimb.Kind.WALL, direction);
        } else if (kind == SpiderClingPayload.CEILING) {
            state.reported = new SpiderClimb.Surface(SpiderClimb.Kind.CEILING, Direction.DOWN);
        } else {
            state.reported = SpiderClimb.Surface.NONE;
        }
    }

    /** He has fallen more than 0.04 blocks a tick on average over the last three ticks. */
    private static boolean falling(SpiderState state, double y) {
        return state.samples >= 3 && (state.y3 - y) / 3.0 > 0.04;
    }

    /** He is rising the way a jump rises (slowing by gravity each tick), not the way a climb does (steadily). */
    private static boolean leaping(SpiderState state, double y) {
        if (state.samples < 3) {
            return false;
        }
        double now = y - state.y1;
        double before = state.y1 - state.y2;
        return now > 0.02 && now - before < -0.05;
    }

    // ---- Ability 4: the Iron Spider's legs and Instant Kill ----

    private static void toggleLegs(ServerPlayer player, PowerData data) {
        if (!isIronLook(data)) {
            say(player, "message.enhanced.legs.iron_only");
            return;
        }
        if (isStripped(player)) {
            say(player, "message.enhanced.legs.stripped");
            return;
        }
        boolean out = !data.toggle(Toggle.SPIDER_LEGS);
        data.setToggle(Toggle.SPIDER_LEGS, out);
        sound(player.serverLevel(), player.position(), ModSounds.SPIDER_LEGS.get(), 0.9F, out ? 1.0F : 0.75F);
        say(player, out ? "message.enhanced.legs.out" : "message.enhanced.legs.in");
        if (!out && data.is(Live.INSTANT_KILL)) {
            endKill(player, data, player.serverLevel().getGameTime());
        }
    }

    /**
     * Sneak + B: Instant Kill, with the legs out, and only after a second press within a few seconds that confirms it
     * (the film's "Activate Instant Kill" is not done lightly). Pressed again while it lasts, it ends.
     */
    private static void instantKill(ServerPlayer player, PowerData data, SpiderState state) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (data.is(Live.INSTANT_KILL)) {
            endKill(player, data, now);
            say(player, "message.enhanced.instant_kill.off");
            return;
        }
        if (!isIronLook(data)) {
            say(player, "message.enhanced.legs.iron_only");
            return;
        }
        if (isStripped(player)) {
            say(player, "message.enhanced.legs.stripped");
            return;
        }
        if (!data.toggle(Toggle.SPIDER_LEGS)) {
            say(player, "message.enhanced.instant_kill.legs_first");
            return;
        }
        if (!data.isReady(4, now)) {
            say(player, "message.enhanced.instant_kill.recharging");
            return;
        }
        if (starving(player)) {
            say(player, "message.enhanced.instant_kill.hungry");
            return;
        }
        if (now > state.killArmedUntil) {
            state.killArmedUntil = now + SpiderConfig.INSTANT_KILL_CONFIRM_TICKS.get();
            say(player, "message.enhanced.instant_kill.confirm");
            return;
        }
        state.killArmedUntil = 0;
        state.nextKill = now;
        data.set(Live.INSTANT_KILL, true);
        data.startCooldown(4, now, SpiderConfig.INSTANT_KILL_MAX_TICKS.get());
        sound(level, player.position(), ModSounds.INSTANT_KILL_ON.get(), 1.0F, 1.0F);
        say(player, "message.enhanced.instant_kill.on");
    }

    /** Instant Kill ends, and its cooldown starts. */
    private static void endKill(ServerPlayer player, PowerData data, long now) {
        if (!data.is(Live.INSTANT_KILL)) {
            return;
        }
        data.set(Live.INSTANT_KILL, false);
        data.startCooldown(4, now, SpiderConfig.INSTANT_KILL_COOLDOWN.get());
    }

    /**
     * Who the legs go for: a hostile mob that is alive, not owned or tamed (a Loki illusion, a villager, a golem, a pet or
     * an animal never), a neutral mob only if it goes for him, and a player only if the server turned
     * {@code instantKillPlayers} on (and PvP lets him). A boss and the minions of a boss fight are hostile whoever they hunt,
     * also those that are not of the game's {@code Enemy} kind (Ultron's Sentries): the legs strike them for their own
     * damage, never a kill ({@link #killImmune}).
     */
    public static boolean isThreat(ServerPlayer spider, LivingEntity target) {
        if (target == spider || !target.isAlive() || target.isSpectator() || target instanceof ArmorStand || target instanceof LokiIllusionEntity) {
            return false;
        }
        if (target instanceof Player player) {
            return SpiderConfig.INSTANT_KILL_PLAYERS.get() && AreaTargets.mayHurt(spider, player, false);
        }
        if ((target instanceof OwnableEntity owned && owned.getOwnerUUID() != null) || spider.isAlliedTo(target) || target instanceof AbstractVillager
                || target instanceof AbstractGolem || target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature) {
            return false;
        }
        if (target instanceof NeutralMob) {
            // An Enderman or a zombified piglin is a threat only while it goes for him.
            return target instanceof Mob mob && mob.getTarget() == spider;
        }
        return target instanceof Enemy || target.getType().is(ModTags.BOSSES) || target.getType().is(ModTags.BOSS_MINIONS)
                || BossFights.isMinion(target) || (target instanceof Mob mob && mob.getTarget() == spider);
    }

    /** The boss and its minions take only the legs' own damage, never an Instant Kill. */
    public static boolean killImmune(LivingEntity target) {
        return target.getType().is(ModTags.INSTANT_KILL_IMMUNE) || BossFights.isMinion(target);
    }

    private static List<LivingEntity> threatsWithin(ServerPlayer player, double range) {
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity creature : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))) {
            if (creature.distanceTo(player) <= range && isThreat(player, creature)) {
                found.add(creature);
            }
        }
        found.sort(Comparator.comparingDouble(creature -> creature.distanceToSqr(player)));
        return found;
    }

    /** The legs guard him, or, in Instant Kill, every leg strikes. */
    private static void legsTick(ServerPlayer player, PowerData data, SpiderState state, long now, boolean blocked) {
        boolean out = data.toggle(Toggle.SPIDER_LEGS);
        if (!out) {
            if (data.is(Live.INSTANT_KILL)) {
                endKill(player, data, now);
            }
            return;
        }
        player.causeFoodExhaustion(SpiderConfig.LEGS_EXHAUSTION.get().floatValue());
        if (data.is(Live.INSTANT_KILL)) {
            if (blocked || starving(player) || now >= data.cooldownUntil(4)) {
                endKill(player, data, now);
                return;
            }
            player.causeFoodExhaustion(SpiderConfig.INSTANT_KILL_EXHAUSTION.get().floatValue());
            if (now >= state.nextKill) {
                state.nextKill = now + SpiderConfig.INSTANT_KILL_INTERVAL.get();
                List<LivingEntity> targets = threatsWithin(player, SpiderConfig.INSTANT_KILL_RANGE.get());
                for (int i = 0; i < Math.min(LEGS, targets.size()); i++) {
                    strike(player, targets.get(i), true, state);
                }
            }
            return;
        }
        if (!blocked && now >= state.nextJab) {
            List<LivingEntity> targets = threatsWithin(player, SpiderConfig.LEGS_RANGE.get());
            if (!targets.isEmpty()) {
                state.nextJab = now + SpiderConfig.LEGS_INTERVAL.get();
                strike(player, targets.get(0), false, state);
            }
        }
    }

    /**
     * One leg strikes: for the legs' damage, or, in Instant Kill, for a kill (a boss or a minion of a boss fight takes the
     * legs' damage only). A complete Chaos Shield between a leg and its victim stops the strike, and pays up to 5 for it.
     */
    private static void strike(ServerPlayer player, LivingEntity target, boolean kill, SpiderState state) {
        ServerLevel level = player.serverLevel();
        Vec3 middle = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 from = player.position().add(0, player.getBbHeight() * 0.75, 0);
        boolean deadly = kill && !killImmune(target);
        double damage = deadly ? SpiderConfig.INSTANT_KILL_DAMAGE.get() : SpiderConfig.LEGS_DAMAGE.get();
        int leg = state.legCursor++ % LEGS;
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, from, middle, 1.0F);
        if (shield != null) {
            payShield(player, shield, (float) Math.min(damage, 5.0));
            return;
        }
        if (deadly) {
            // (A boss or its minion gets the legs' damage by the vanilla rhythm of hits, not a hit every strike.)
            target.invulnerableTime = 0;
        }
        DamageSource source = ModDamageTypes.source(level, ModDamageTypes.SPIDER_LEGS, player, player);
        target.hurt(source, (float) damage);
        sound(level, middle, ModSounds.LEG_STRIKE.get(), 0.6F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        level.sendParticles(new EnergyParticleOptions(kill ? 0xFF2A2A : 0xC9A66B, 0.8F), middle.x, middle.y, middle.z, 5, 0.2, 0.2, 0.2, 0.08);
        send(level, player, new SpiderFxPayload(SpiderFxPayload.Kind.LEG_STRIKE, player.getId(), from, middle, Vec3.ZERO, leg, 0F, false, 0L, List.of()));
    }

    /** Magneto's Armor & Weapon Strip finds the Iron Spider's metal: the nanites scatter, and the suit is off for a while. */
    public static boolean onStripped(LivingEntity target) {
        int ticks = SpiderConfig.IRON_SPIDER_STRIP_TICKS.get();
        if (ticks <= 0 || !isIronSpider(target) || !(target instanceof ServerPlayer player)) {
            return false;
        }
        PowerData data = Powers.get(player);
        SpiderState state = state(player);
        long now = player.serverLevel().getGameTime();
        state.stripUntil = now + ticks;
        data.setToggle(Toggle.SPIDER_LEGS, false);
        endKill(player, data, now);
        say(player, "message.enhanced.legs.stripped");
        return true;
    }

    // ---- Ability 5: Black Eyes ----

    private static void blackEyes(ServerPlayer player, PowerData data, SpiderState state) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (data.is(Live.BLACK_EYES)) {
            if (now < state.eyesLockedUntil) {
                return; // it came by itself: it cannot be calmed yet
            }
            endEyes(player, data, state, now, true);
            return;
        }
        if (!data.isReady(5, now)) {
            say(player, "message.enhanced.black_eyes.recharging");
            return;
        }
        if (!player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() < EYES_MIN_FOOD) {
            say(player, "message.enhanced.black_eyes.hungry");
            return;
        }
        startEyes(player, data, state, now);
    }

    private static void startEyes(ServerPlayer player, PowerData data, SpiderState state, long now) {
        ServerLevel level = player.serverLevel();
        int ticks = SpiderConfig.BLACK_EYES_MAX_TICKS.get();
        state.eyesEndsAt = now + ticks;
        data.set(Live.BLACK_EYES, true);
        data.startCooldown(5, now, ticks);
        sound(level, player.position(), ModSounds.BLACK_EYES_ON.get(), 1.0F, 1.0F);
        say(player, "message.enhanced.black_eyes.on");
    }

    /**
     * The surge ends. Calmly (he pressed N): no crash, and half the cooldown. When it runs out or his stomach is empty:
     * he crashes (Slowness II and Weakness I) and the whole cooldown.
     */
    private static void endEyes(ServerPlayer player, PowerData data, SpiderState state, long now, boolean calm) {
        endEyes(player, data, state, now, calm, true);
    }

    /** As above; not {@code loud} (the power is gone, he logged out) it happens without a sound or a message. */
    private static void endEyes(ServerPlayer player, PowerData data, SpiderState state, long now, boolean calm, boolean loud) {
        data.set(Live.BLACK_EYES, false);
        state.eyesLockedUntil = 0;
        int cooldown = SpiderConfig.BLACK_EYES_COOLDOWN.get();
        data.startCooldown(5, now, calm ? cooldown / 2 : cooldown);
        if (!loud) {
            return;
        }
        sound(player.serverLevel(), player.position(), ModSounds.BLACK_EYES_OFF.get(), 1.0F, 1.0F);
        if (calm) {
            say(player, "message.enhanced.black_eyes.off");
        } else {
            int crash = SpiderConfig.BLACK_EYES_CRASH_TICKS.get();
            if (crash > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, crash, CRASH_SLOWNESS));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, crash, 0));
            }
            say(player, "message.enhanced.black_eyes.crash");
        }
    }

    private static void eyesTick(ServerPlayer player, PowerData data, SpiderState state, long now) {
        if (data.is(Live.BLACK_EYES)) {
            player.causeFoodExhaustion(SpiderConfig.BLACK_EYES_EXHAUSTION.get().floatValue());
            if (now >= state.eyesEndsAt || starving(player)) {
                endEyes(player, data, state, now, false);
            }
            return;
        }
        double panic = SpiderConfig.BLACK_EYES_PANIC_HEALTH.get();
        if (panic > 0.0 && player.getHealth() <= panic && data.isReady(5, now) && !starving(player) && !blocked(player, data)) {
            // His spider genes take over, and he cannot calm them for a while.
            startEyes(player, data, state, now);
            state.eyesLockedUntil = now + PANIC_LOCK_TICKS;
        }
    }

    // ---- passive: Spider-Sense ----

    private static void senseTick(ServerPlayer player, PowerData data, SpiderState state, long now) {
        if (!data.toggle(Toggle.SPIDER_SENSE) || now < state.nextScan) {
            return;
        }
        boolean surge = data.is(Live.BLACK_EYES);
        double factor = surge ? SpiderConfig.BLACK_EYES_SENSE_FACTOR.get() : 1.0;
        state.nextScan = now + (surge ? Math.max(1, Math.min(2, SpiderConfig.SPIDER_SENSE_INTERVAL.get())) : SpiderConfig.SPIDER_SENSE_INTERVAL.get());
        List<SpiderSense.Threat> fresh = SpiderSense.scan(player, state, SpiderConfig.SPIDER_SENSE_RANGE.get() * factor, factor, surge);
        if (!fresh.isEmpty()) {
            warn(player, data, state, fresh);
        }
    }

    /** The tingle: everyone sees his head snap round, and he is told what threatens him, and hears it. */
    private static void warn(ServerPlayer player, PowerData data, SpiderState state, List<SpiderSense.Threat> threats) {
        long now = player.serverLevel().getGameTime();
        state.senseUntil = now + SENSE_TICKS;
        data.set(Live.SENSING, true);
        List<Integer> ids = new ArrayList<>();
        boolean strong = false;
        for (SpiderSense.Threat threat : threats) {
            ids.add(threat.entity().getId());
            strong |= threat.strong();
        }
        PacketDistributor.sendToPlayer(player, new SpiderSensePayload(ids, strong));
        player.playNotifySound(ModSounds.SPIDER_SENSE.get(), SoundSource.PLAYERS, 0.8F, strong ? 1.1F : 1.0F);
    }

    /**
     * A hook for other powers: warns a Spider-Man of a danger the scan cannot see (a mind-reading threat), as if the
     * spider-sense had found it. Does nothing if he is not Spider-Man, or has switched the sense off.
     */
    public static void warnOf(ServerPlayer spider, Entity source, boolean strong) {
        PowerData data = Powers.get(spider);
        if (data.power() != Power.SPIDER_MAN || !data.toggle(Toggle.SPIDER_SENSE)) {
            return;
        }
        SpiderState state = state(spider);
        warn(spider, data, state, List.of(new SpiderSense.Threat(source, strong)));
    }

    // ---- lifecycle ----

    /** Every state he has, and the web he hangs on, ends; the Black Eyes end calmly (no crash). */
    private static void end(ServerPlayer player, PowerData data, boolean forgetAll) {
        SpiderState state = STATES.get(player.getUUID());
        long now = player.serverLevel().getGameTime();
        if (state != null) {
            endZip(player, data, state, 1);
            endSwing(player, data, state, 1);
            if (data.is(Live.BLACK_EYES)) {
                endEyes(player, data, state, now, true, false);
            }
            endKill(player, data, now);
        }
        clearLive(data);
        if (forgetAll) {
            STATES.remove(player.getUUID());
        }
    }

    private static void clearLive(PowerData data) {
        data.set(Live.CLINGING_WALL, false);
        data.set(Live.CLINGING_CEILING, false);
        data.set(Live.ZIPPING, false);
        data.set(Live.SWINGING, false);
        data.set(Live.SENSING, false);
        data.set(Live.BLACK_EYES, false);
        data.set(Live.INSTANT_KILL, false);
    }

    @Override
    public void endAll(ServerPlayer player, PowerData data) {
        // (His body is taken off by applyBody, which follows with the new power; his webs stand until their own tick.)
        end(player, data, true);
        data.set(Live.CASTING, false);
    }

    @Override
    public void stopHold(PowerData data) {
        // A body on the death screen keeps no web up.
        clearLive(data);
    }

    @Override
    public void onLogout(ServerPlayer player) {
        if (isSpiderMan(player)) {
            end(player, Powers.get(player), true);
        }
    }

    @Override
    public void onChangeDimension(ServerPlayer player) {
        if (isSpiderMan(player)) {
            end(player, Powers.get(player), true);
        }
    }

    @Override
    public void onDeath(ServerPlayer player) {
        if (isSpiderMan(player)) {
            end(player, Powers.get(player), true);
        }
    }

    @Override
    public void onServerStopped() {
        STATES.clear();
    }
}
