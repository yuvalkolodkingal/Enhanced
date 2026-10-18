package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.HelaConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hela the hero (3.5, PLAN 7): a powerless player who held Hela's Headdress after her defeat. Her keys:
 *
 * <ul>
 *     <li><b>R</b> Necrosword Volley: three blades (five with her form on) fanned from behind her shoulders at what she
 *     looks at; <b>Sneak + R</b> Blade Rain: twelve blades over a five-block circle where she looks (at most 32 away).</li>
 *     <li><b>G</b> Blade Wall: a ten-long line of spikes from her feet where she looks, that stand six seconds and block;
 *     <b>Sneak + G</b> Blade Burst: a ring of spikes round her.</li>
 *     <li><b>V</b> Goddess of Death (toggle): the headdress grows: Strength, Resistance I, her blades 30 percent stronger,
 *     one hunger point every ten seconds; <b>Sneak + V</b> Death Dash: eight blocks through smoke, cutting what she
 *     passes.</li>
 *     <li><b>B</b> Raise the Dead: three berserkers (five in Asgard) for 40 seconds; <b>Sneak + B</b> Fenris for a
 *     minute (one cooldown for both).</li>
 *     <li><b>N</b> Death's Grasp (held): projectiles that come into a cone of three blocks before her hand crumble; a
 *     thrown Mjolnir is caught and crushed (once a minute), a thrown Stormbreaker stopped.</li>
 *     <li>Her body: 10 more health and 4 armour; in Asgard, Regeneration I always and her damage times
 *     {@code asgardFactor} ({@link HelaEvents}).</li>
 * </ul>
 *
 * <p>3.4: every part works for a custom hero who picked its row ({@code data.has}), with the key the binding has, its
 * Sneak variant from {@code alt}, the cooldowns started on the row ({@code startCooldown(row, ...)}), the body scaled by
 * {@code bodyScale}; her rows need no item. The plug-in is {@link HelaServer}.
 */
public final class HelaAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("hela_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("hela_armor");
    public static final double BODY_HEALTH_BONUS = 10.0;
    public static final double BODY_ARMOR_BONUS = 4.0;

    public static final double VOLLEY_RANGE = 48.0;
    public static final double RAIN_RANGE = 32.0;
    public static final double RAIN_RADIUS = 5.0;
    public static final int RAIN_BLADES = 12;
    public static final int RAIN_HANG = 16;
    public static final float RAIN_DAMAGE = 6.0F;
    public static final int WALL_LENGTH = 10;
    public static final int WALL_TELL = 8;
    public static final int WALL_STAND = 120;
    public static final double BURST_RADIUS = 4.0;
    public static final int BURST_SPIKES = 12;
    public static final float BURST_DAMAGE = 10.0F;
    public static final int BURST_STAND = 40;
    public static final double DASH_LENGTH = 8.0;
    public static final float DASH_DAMAGE = 6.0F;
    public static final int RAISE_TICKS = 800;
    public static final int FENRIS_TICKS = 1200;
    /** Her blades with the form on are this much stronger. */
    public static final float FORM_FACTOR = 1.3F;
    /** Her form costs a point of hunger this often. */
    public static final int FORM_HUNGER_EVERY = 200;
    /** Death's Grasp: the cone before her hand. */
    public static final double GRASP_REACH = 3.0;
    public static final double GRASP_CONE = 0.55;
    public static final int CATCH_COOLDOWN = 1200;

    /** Hela -> the game time her grasp must open (its most). */
    private static final Map<UUID, Long> GRASPING = new HashMap<>();
    /** Hela -> the game time she may crush a Mjolnir again. */
    private static final Map<UUID, Long> CATCH_READY = new HashMap<>();
    /** Who is holding the headdress to become Hela, and since when. */
    private static final Map<UUID, Long> HOLDING = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // Queries

    /** Whether her form is on (the headdress grown). */
    public static boolean formOn(PowerData data) {
        return data.has(HeroAbility.HELA_GODDESS) && data.toggle(Toggle.HELA_FORM);
    }

    /** Whether she holds Death's Grasp now. */
    public static boolean grasping(ServerPlayer player) {
        return GRASPING.containsKey(player.getUUID());
    }

    /** The game time her grasp opens at most, or 0. */
    public static long graspEnds(ServerPlayer player) {
        return GRASPING.getOrDefault(player.getUUID(), 0L);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input and ticks

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down) {
            if (ability == 5 && grasping(player)) {
                release(player, data);
            }
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    rain(player, data);
                } else {
                    volley(player, data);
                }
            }
            case 2 -> {
                if (alt) {
                    burst(player, data);
                } else {
                    wall(player, data);
                }
            }
            case 3 -> {
                if (alt) {
                    dash(player, data);
                } else {
                    toggleForm(player, data);
                }
            }
            case 4 -> {
                if (alt) {
                    callFenris(player, data);
                } else {
                    raise(player, data);
                }
            }
            case 5 -> grasp(player, data);
            default -> {
            }
        }
    }

    static void tick(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (formOn(data)) {
            formTick(player, data, now);
        }
        if (data.has(HeroAbility.HELA_GRASP)) {
            graspTick(player, data, now);
        }
        if (data.has(HeroAbility.HELA_ARMY)) {
            boolean any = HelaArmy.any(player);
            if (data.is(Live.HELA_SUMMONING) != any) {
                data.set(Live.HELA_SUMMONING, any);
            }
        }
        if (data.has(HeroAbility.HELA_BODY) && AsgardDimension.in(player) && now % 20 == 0) {
            // The strength of Asgard: she heals as long as she stands in it.
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        }
    }

    /**
     * Her body: 10 more health and 4 armour, for the hero (scaled by the strength of a custom hero's body), and none of it
     * on anyone else.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean body = data.has(HeroAbility.HELA_BODY);
        float scale = body ? data.bodyScale(HeroAbility.HELA_BODY) : 0.0F;
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, BODY_HEALTH_BONUS * scale, body);
        setModifier(player, Attributes.ARMOR, BODY_ARMOR, BODY_ARMOR_BONUS * scale, body);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    /** She loses her power (or a custom hero redefines): the form fades, the grasp opens, her dead crumble. */
    static void endAll(ServerPlayer player, PowerData data) {
        if (data.toggle(Toggle.HELA_FORM)) {
            data.setToggle(Toggle.HELA_FORM, false);
        }
        clearForm(player);
        GRASPING.remove(player.getUUID());
        data.set(Live.HELA_GRASPING, false);
        data.set(Live.HELA_SUMMONING, false);
        HelaArmy.dismiss(player);
    }

    /** The keys held come up (death, the power lost): the grasp opens. */
    static void stopHold(PowerData data) {
        data.set(Live.HELA_GRASPING, false);
    }

    /** She left, died or changed realm: the grasp opens and her dead crumble. */
    static void onGone(ServerPlayer player) {
        GRASPING.remove(player.getUUID());
        HOLDING.remove(player.getUUID());
        HelaArmy.dismiss(player);
        PowerData data = Powers.get(player);
        data.set(Live.HELA_GRASPING, false);
        data.set(Live.HELA_SUMMONING, false);
    }

    static void clear() {
        GRASPING.clear();
        CATCH_READY.clear();
        HOLDING.clear();
        HelaArmy.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: the volley and the rain

    /** Where her blades go: the point she looks at, at most {@code range} away (a wall stops the look). */
    static Vec3 lookPoint(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        HitResult hit = Raycasts.any(player, range, e -> e instanceof LivingEntity living && living.isAlive() && e != player, 1.0F);
        if (hit.getType() == HitResult.Type.ENTITY) {
            Entity seen = ((net.minecraft.world.phys.EntityHitResult) hit).getEntity();
            return seen.position().add(0.0, seen.getBbHeight() * 0.6, 0.0);
        }
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** R: the fan of necroswords. */
    static void volley(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.HELA_NECROSWORDS) || !data.isReady(HeroAbility.HELA_NECROSWORDS, now)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int count = formOn(data) ? 5 : 3;
        List<Vec3> from = HelaAttacks.fanPoints(player.position(), player.getYRot(), player.getBbHeight() * 0.9F, count);
        Vec3 target = lookPoint(player, VOLLEY_RANGE);
        HelaAttacks.volley(level, player, from, target, 4.0F, HelaConfig.VOLLEY_DAMAGE.get().floatValue());
        HelaFx.send(level, HelaFx.THROW, player.getId(), from, count);
        data.startCooldown(HeroAbility.HELA_NECROSWORDS, now, HelaConfig.VOLLEY_COOLDOWN.get());
    }

    /** Sneak + R: the Blade Rain where she looks. */
    static void rain(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.HELA_NECROSWORDS) || !data.isReady(HeroAbility.HELA_NECROSWORDS, now)) {
            return;
        }
        BlockHitResult hit = Raycasts.block(player, RAIN_RANGE, 1.0F);
        Vec3 centre = hit.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(RAIN_RANGE)) : hit.getLocation();
        ServerLevel level = player.serverLevel();
        HelaAttacks.rain(level, player, centre, RAIN_RADIUS, RAIN_BLADES, RAIN_HANG, RAIN_DAMAGE);
        data.startCooldown(HeroAbility.HELA_NECROSWORDS, now, HelaConfig.RAIN_COOLDOWN.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // G: the wall and the burst

    /** G: a line of spikes from her feet where she looks. */
    static void wall(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.HELA_BLADE_WALL) || !data.isReady(HeroAbility.HELA_BLADE_WALL, now)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        List<Vec3> points = HelaAttacks.wallPoints(level, player.position(), player.getLookAngle(), WALL_LENGTH);
        if (points.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enhanced.hela.no_room"), true);
            return;
        }
        HelaAttacks.wall(level, player, points, WALL_TELL, WALL_STAND, HelaConfig.WALL_DAMAGE.get().floatValue());
        data.startCooldown(HeroAbility.HELA_BLADE_WALL, now, HelaConfig.WALL_COOLDOWN.get());
    }

    /** Sneak + G: a ring of spikes round her. */
    static void burst(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.HELA_BLADE_WALL) || !data.isReady(HeroAbility.HELA_BLADE_WALL, now)) {
            return;
        }
        HelaAttacks.ring(player.serverLevel(), player, BURST_RADIUS, BURST_SPIKES, 4, BURST_STAND, BURST_DAMAGE);
        data.startCooldown(HeroAbility.HELA_BLADE_WALL, now, HelaConfig.BURST_COOLDOWN.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // V: the Goddess of Death and the dash

    /** V: the headdress grows, or fades. */
    static void toggleForm(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.HELA_GODDESS)) {
            return;
        }
        boolean on = !data.toggle(Toggle.HELA_FORM);
        if (on && !player.isCreative() && player.getFoodData().getFoodLevel() <= 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.hela.hungry"), true);
            return;
        }
        data.setToggle(Toggle.HELA_FORM, on);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), on ? ModSounds.HELA_FORM_ON.get() : ModSounds.HELA_FORM_OFF.get(),
                SoundSource.PLAYERS, 1.2F, 1.0F);
        HelaFx.send(level, HelaFx.FORM, player.getId(), player.position().add(0.0, player.getBbHeight(), 0.0), on ? 1 : 0);
        if (on) {
            formTick(player, data, level.getGameTime());
        } else {
            clearForm(player);
        }
    }

    /** Her form, each tick it is on: its strength and its hunger. */
    private static void formTick(ServerPlayer player, PowerData data, long now) {
        if (now % 10 == 0) {
            int strength = Math.max(0, HelaConfig.FORM_STRENGTH.get() - 1);
            refresh(player, MobEffects.DAMAGE_BOOST, strength);
            refresh(player, MobEffects.DAMAGE_RESISTANCE, 0);
        }
        if (now % FORM_HUNGER_EVERY == 0 && !player.isCreative()) {
            player.causeFoodExhaustion(4.0F);
            if (player.getFoodData().getFoodLevel() <= 0) {
                data.setToggle(Toggle.HELA_FORM, false);
                clearForm(player);
                player.displayClientMessage(Component.translatable("message.enhanced.hela.hungry"), true);
            }
        }
    }

    private static void refresh(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance had = player.getEffect(effect);
        if (had == null || had.getAmplifier() <= amplifier && had.getDuration() < 30) {
            player.addEffect(new MobEffectInstance(effect, 40, amplifier, true, false, true));
        }
    }

    private static void clearForm(ServerPlayer player) {
        removeShort(player, MobEffects.DAMAGE_BOOST);
        removeShort(player, MobEffects.DAMAGE_RESISTANCE);
    }

    /** Takes away an effect of her form (an ambient one of 40 ticks or less), never one from a potion. */
    private static void removeShort(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance had = player.getEffect(effect);
        if (had != null && had.isAmbient() && had.getDuration() <= 40) {
            player.removeEffect(effect);
        }
    }

    /** Sneak + V: she goes to smoke and is eight blocks on (stopped by a wall), cutting what she passes through. */
    static void dash(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.HELA_GODDESS) || !data.isReady(HeroAbility.HELA_GODDESS, now)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        Vec3 dir = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        dir = dir.normalize();
        Vec3 to = from;
        for (double d = 0.5; d <= DASH_LENGTH; d += 0.5) {
            Vec3 next = from.add(dir.scale(d));
            BlockHitResult hit = level.clip(new ClipContext(to.add(0, 0.5, 0), next.add(0, 0.5, 0), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS || !Teleports.fits(player, next)) {
                break;
            }
            to = next;
        }
        if (to.distanceToSqr(from) < 1.0 || !BossFights.allowsTeleport(level, player, to)) {
            player.displayClientMessage(Component.translatable("message.enhanced.hela.no_room"), true);
            return;
        }
        AABB path = new AABB(from, to).inflate(0.8, 1.0, 0.8).expandTowards(0.0, player.getBbHeight(), 0.0);
        Set<LivingEntity> cut = new HashSet<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, path, e -> HelaRules.canHurt(player, e))) {
            if (cut.add(e)) {
                HelaRules.hurt(e, ModDamageTypes.NECROSWORD, player, player, DASH_DAMAGE);
            }
        }
        player.teleportTo(to.x, to.y, to.z);
        player.resetFallDistance();
        level.playSound(null, to.x, to.y, to.z, ModSounds.HELA_SHADOW_STEP.get(), SoundSource.PLAYERS, 1.2F, 1.2F);
        HelaFx.send(level, HelaFx.DASH, player.getId(), List.of(from, to), 0);
        data.startCooldown(HeroAbility.HELA_GODDESS, now, HelaConfig.DASH_COOLDOWN.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // B: her dead

    /** B: three berserkers (five in Asgard) climb out of the ground round her. */
    static void raise(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.HELA_ARMY) || !data.isReady(HeroAbility.HELA_ARMY, now)) {
            return;
        }
        int count = AsgardDimension.in(player) ? HelaConfig.RAISE_COUNT_ASGARD.get() : HelaConfig.RAISE_COUNT.get();
        HelaArmy.raise(player, count, RAISE_TICKS);
        HelaFx.send(player.serverLevel(), HelaFx.RAISE, player.getId(), player.position(), count);
        data.set(Live.HELA_SUMMONING, true);
        data.startCooldown(HeroAbility.HELA_ARMY, now, HelaConfig.RAISE_COOLDOWN.get());
    }

    /** Sneak + B: Fenris comes. */
    static void callFenris(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.HELA_ARMY) || !data.isReady(HeroAbility.HELA_ARMY, now)) {
            return;
        }
        HelaArmy.fenris(player, FENRIS_TICKS);
        data.set(Live.HELA_SUMMONING, true);
        data.startCooldown(HeroAbility.HELA_ARMY, now, HelaConfig.FENRIS_COOLDOWN.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // N: Death's Grasp

    /** N down: one hand forward, the grasp closes on what comes. */
    static void grasp(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.HELA_GRASP) || grasping(player) || !data.isReady(HeroAbility.HELA_GRASP, now)) {
            return;
        }
        GRASPING.put(player.getUUID(), now + HelaConfig.GRASP_SECONDS.get() * 20L);
        data.set(Live.HELA_GRASPING, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HELA_GRASP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** N up (or its time is over): the hand opens, the cooldown starts. */
    static void release(ServerPlayer player, PowerData data) {
        if (GRASPING.remove(player.getUUID()) == null) {
            return;
        }
        data.set(Live.HELA_GRASPING, false);
        data.startCooldown(HeroAbility.HELA_GRASP, player.level().getGameTime(), HelaConfig.GRASP_COOLDOWN.get());
    }

    /** The cone before her hand: what comes into it crumbles, Mjolnir is crushed, Stormbreaker dropped. */
    private static void graspTick(ServerPlayer player, PowerData data, long now) {
        Long ends = GRASPING.get(player.getUUID());
        if (ends == null) {
            if (data.is(Live.HELA_GRASPING)) {
                data.set(Live.HELA_GRASPING, false); // (the key came up while the grasp was not hers to keep)
            }
            return;
        }
        if (now >= ends || !data.is(Live.HELA_GRASPING)) {
            release(player, data);
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 hand = eye.add(look.scale(0.8)).subtract(0.0, 0.3, 0.0);
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, new AABB(eye, eye).inflate(GRASP_REACH + 0.5),
                p -> p.isAlive() && inCone(eye, look, p.position()))) {
            if (shot.getOwner() == player || shot instanceof com.kolodkingal.enhanced.entity.ThrownWeaponEntity) {
                continue; // (her own blades; Thor's weapons are below)
            }
            if (shot instanceof NecroswordEntity blade && blade.getOwner() instanceof LivingEntity owner && HelaArmy.sameSide(player, owner)) {
                continue;
            }
            Vec3 at = shot.position();
            shot.discard();
            HelaFx.send(level, HelaFx.CRUMBLE, player.getId(), at, 0);
            level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.02);
            level.sendParticles(new EnergyParticleOptions(HelaRules.GREEN, 0.7F), at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.03);
        }
        boolean mayCrush = HelaConfig.SHATTER_MJOLNIR.get() && now >= CATCH_READY.getOrDefault(player.getUUID(), 0L);
        if (mayCrush && HelaCatch.catchNear(level, player, hand, GRASP_REACH)) {
            CATCH_READY.put(player.getUUID(), now + CATCH_COOLDOWN);
        }
        HelaCatch.stopNear(level, player, hand, GRASP_REACH);
    }

    /** Whether {@code at} is in the cone before her: within reach of her eyes, and in front of her look. */
    static boolean inCone(Vec3 eye, Vec3 look, Vec3 at) {
        Vec3 to = at.subtract(eye);
        double d = to.length();
        return d <= GRASP_REACH + 0.5 && (d < 0.6 || to.scale(1.0 / d).dot(look) >= GRASP_CONE);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Becoming Hela: the headdress

    /** One tick of a powerless player holding the headdress ({@code held} ticks so far): smoke and green gather on him. */
    public static void holding(ServerPlayer player, int held, int total) {
        ServerLevel level = player.serverLevel();
        if (held == 0 || !HOLDING.containsKey(player.getUUID())) {
            HOLDING.put(player.getUUID(), level.getGameTime());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HELA_FORM_ON.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        float progress = Math.min(1.0F, held / (float) Math.max(1, total));
        double angle = held * 0.4;
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            double r = 1.0 - 0.6 * progress;
            double y = player.getY() + player.getBbHeight() * (0.2 + 0.9 * progress);
            level.sendParticles(i == 0 ? ParticleTypes.LARGE_SMOKE : new EnergyParticleOptions(HelaRules.GREEN, 0.7F),
                    player.getX() + Math.cos(a) * r, y, player.getZ() + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** The holding was cut short. */
    public static void stopHolding(ServerPlayer player) {
        HOLDING.remove(player.getUUID());
    }

    /** She has become Hela: the headdress grows on her (the form on for its first moments is the look of it). */
    public static void awakened(ServerPlayer player) {
        HOLDING.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 0.9, 0.5, 0.05);
        level.sendParticles(new EnergyParticleOptions(HelaRules.GREEN, 1.4F), player.getX(), player.getY() + 1.6, player.getZ(), 40, 0.4, 0.4, 0.4, 0.1);
        HelaFx.send(level, HelaFx.FORM, player.getId(), player.position().add(0.0, player.getBbHeight(), 0.0), 1);
    }

    @Nullable
    static UUID holder(ServerPlayer player) {
        return HOLDING.containsKey(player.getUUID()) ? player.getUUID() : null;
    }

    private HelaAbilities() {
    }
}
