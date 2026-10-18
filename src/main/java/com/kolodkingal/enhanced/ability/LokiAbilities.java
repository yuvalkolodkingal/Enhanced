package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.LokiDaggerEntity;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.LokiForm;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loki, the God of Mischief. Every look (his Asgardian clothes, the TVA's, the God of Stories', his own skin) shares the
 * same tricks: conjured daggers, illusory duplicates, shapeshifting and vanishing. Ability 5 is his true Frost Giant
 * form.
 */
public final class LokiAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("loki_body_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("loki_body_armor");
    private static final ResourceLocation BODY_TOUGHNESS = Enhanced.id("loki_body_toughness");
    private static final ResourceLocation FROST_ARMOR = Enhanced.id("loki_frost_armor");

    public static final double FROST_ARMOR_BONUS = 4.0;

    public static final float DAGGER_SPEED = 2.5F;
    /** Angle between the daggers of a fan (Sneak + ability 1). */
    public static final float FAN_ANGLE = 10.0F;
    public static final int DAGGER_COOLDOWN = 8;
    public static final int FAN_COOLDOWN = 30;

    public static final int DUPLICATES = 3;
    public static final int HIDE_TICKS = 100;
    public static final int DUPLICATE_TICKS = 160;
    /** Hostile creatures this close that were after Loki go after a duplicate instead. */
    public static final double DECOY_RADIUS = 24.0;
    /** How far the duplicates walk off. */
    public static final double DECOY_WALK = 12.0;
    public static final int DUPLICATES_COOLDOWN = 300;

    public static final double SHAPESHIFT_RANGE = 16.0;

    public static final double VANISH_RANGE = 20.0;
    public static final int VANISH_COOLDOWN = 60;

    /** Frost Giant skin, and the cold light of the change. */
    public static final int FROST_COLOR = 0x5E7A9E;
    public static final int FROST_GLOW = 0xBFE3FF;
    /** Slowness II from a Frost Giant's blow, in ticks (3 seconds). */
    public static final int FREEZE_TICKS = 60;
    public static final int FROST_WALK_RADIUS = 3;

    /** How long a throwing arm or a raised casting hand is shown. */
    private static final int GESTURE_TICKS = 8;
    /** Momentary states that end on their own, and when each Loki's ends (game time). */
    private static final Map<UUID, Map<Live, Long>> UNTIL = new HashMap<>();
    private static final Live[] TIMED = {Live.HIDDEN, Live.THROWING, Live.CASTING};

    // ---- input ----

    /** Loki's own key: Sneak chooses the variant (a fan of daggers). */
    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of Loki's, his own or (3.4) a custom hero's who has the ability ({@code HeroKeys.press}): {@code ability} is
     * its key for Loki, {@code alt} whether it is the Sneak variant.
     */
    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down || player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> daggers(player, data, alt);
            case 2 -> duplicates(player, data);
            case 3 -> shapeshift(player, data);
            case 4 -> vanish(player, data);
            case 5 -> toggleFrostGiant(player, data);
            default -> {
            }
        }
    }

    /** Loki's tick, or (3.4) a custom hero's with any of his abilities: each part only for the ability it belongs to. */
    public static void tick(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        Map<Live, Long> until = UNTIL.get(player.getUUID());
        for (Live state : TIMED) {
            if (until == null || !until.containsKey(state)) {
                // (Not his to end: a custom hero's other heroes show CASTING too.)
                continue;
            }
            boolean on = until.get(state) > now;
            if (!on) {
                until.remove(state);
            }
            // Drinking milk (or anything else that ends the invisibility) ends the trick.
            data.set(state, on && (state != Live.HIDDEN || player.hasEffect(MobEffects.INVISIBILITY)));
        }
        if (data.has(HeroAbility.LOKI_SHAPESHIFT) && !data.form().isEmpty() && LokiForm.of(data).isEmpty()) {
            // The creature he copied no longer exists (a removed mod): he is himself again.
            data.setForm("");
        }
        if (data.has(HeroAbility.LOKI_FROST_GIANT) && data.toggle(Toggle.FROST_GIANT)) {
            frostTick(player);
        }
    }

    /**
     * Keeps Loki's body in step with his Frost Giant form and his shapeshifted form; also called when the power changes.
     * His Asgardian body (3.5: {@code [loki]} bodyHealthBonus 10, bodyArmor 6, bodyToughness 2, so 30 health as Thor's)
     * is his own (no Hero Creator row); a custom hero's Frost Giant form armours him too (3.4).
     */
    public static void applyForm(ServerPlayer player, PowerData data) {
        boolean loki = data.power() == Power.LOKI;
        boolean frost = data.has(HeroAbility.LOKI_FROST_GIANT) && data.toggle(Toggle.FROST_GIANT);
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, EnhancedConfig.LOKI_BODY_HEALTH.get(), loki);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, EnhancedConfig.LOKI_BODY_ARMOR.get(), loki);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, BODY_TOUGHNESS, EnhancedConfig.LOKI_BODY_TOUGHNESS.get(), loki);
        PowerTicker.setModifier(player, Attributes.ARMOR, FROST_ARMOR, FROST_ARMOR_BONUS, frost);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Ends everything this hero left in the world (the power is being taken away). */
    public static void endAll(ServerPlayer player, PowerData data) {
        for (ServerLevel level : player.server.getAllLevels()) {
            for (LokiIllusionEntity illusion : LokiIllusionEntity.ownedBy(level, player.getUUID())) {
                illusion.dissolve(null);
            }
        }
        if (data.is(Live.HIDDEN)) {
            player.removeEffect(MobEffects.INVISIBILITY);
        }
        UNTIL.remove(player.getUUID());
        for (Live state : TIMED) {
            data.set(state, false);
        }
        if (data.has(HeroAbility.LOKI_SHAPESHIFT)) {
            // (3.4: a custom hero's creature shape of his generic Shapeshifting is not Loki's to end.)
            data.setForm("");
        }
        data.setToggle(Toggle.FROST_GIANT, false);
    }

    public static void clear() {
        UNTIL.clear();
    }

    // ---- queries (both sides) ----

    public static boolean isFrostGiant(Player player) {
        PowerData data = Powers.get(player);
        return data.has(HeroAbility.LOKI_FROST_GIANT) && data.toggle(Toggle.FROST_GIANT);
    }

    /** Loki is invisible behind his duplicates: nothing of him is drawn, not even what he holds. */
    public static boolean isHidden(Player player) {
        PowerData data = Powers.get(player);
        return data.has(HeroAbility.LOKI_DUPLICATES) && data.is(Live.HIDDEN);
    }

    /** Whether {@code player} is a Loki (3.4: or has his Shapeshift) who has taken the shape of this kind of creature. */
    public static boolean passesAs(Player player, EntityType<?> type) {
        PowerData data = Powers.get(player);
        return data.has(HeroAbility.LOKI_SHAPESHIFT) && !data.form().isEmpty()
                && LokiForm.of(data).filter(form -> !form.isPlayer() && form.type() == type).isPresent();
    }

    // ---- seen through (the Soul Stone of Thanos, the boss and the hero) ----

    /**
     * The Soul Stone sees which Loki is the true one: every duplicate within {@code radius} of {@code center} dissolves.
     *
     * @return how many copies dissolved
     */
    public static int dissolveDuplicates(ServerLevel level, Vec3 center, double radius) {
        return dissolveDuplicates(level, center, radius, null);
    }

    /**
     * {@link #dissolveDuplicates(ServerLevel, Vec3, double)} for what {@code seer} (the one whose Soul Stone it is) can reach:
     * a copy behind the wall of a Hex that keeps the seer out, or in a ward against a boss's sight, is not seen through (3.3).
     */
    public static int dissolveDuplicates(ServerLevel level, Vec3 center, double radius, @Nullable LivingEntity seer) {
        int dissolved = 0;
        for (LokiIllusionEntity illusion : level.getEntitiesOfClass(LokiIllusionEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(radius),
                copy -> seer == null || !Shelters.shelters(seer, copy))) {
            illusion.dissolve(null);
            dissolved++;
        }
        return dissolved;
    }

    /**
     * The Soul Stone shows Loki as he is: an invisible Loki (hiding behind his duplicates) is seen again, and a Loki who
     * took another shape is himself.
     *
     * @return whether anything about him changed
     */
    public static boolean unmask(ServerPlayer loki) {
        PowerData data = Powers.get(loki);
        if (!data.acts(Power.LOKI)) {
            return false;
        }
        boolean changed = false;
        if (data.has(HeroAbility.LOKI_DUPLICATES) && (data.is(Live.HIDDEN) || loki.hasEffect(MobEffects.INVISIBILITY))) {
            loki.removeEffect(MobEffects.INVISIBILITY);
            Map<Live, Long> until = UNTIL.get(loki.getUUID());
            if (until != null) {
                until.remove(Live.HIDDEN);
            }
            data.set(Live.HIDDEN, false);
            changed = true;
        }
        if (data.has(HeroAbility.LOKI_SHAPESHIFT) && !data.form().isEmpty()) {
            data.setForm("");
            shimmer(loki, data);
            changed = true;
        }
        return changed;
    }

    // ---- Ability 1: Daggers ----

    /** A conjured dagger thrown from the hand, or (sneaking) a fan of three. */
    private static void daggers(ServerPlayer player, PowerData data, boolean fan) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.LOKI_DAGGERS, now)) {
            return;
        }
        data.startCooldown(HeroAbility.LOKI_DAGGERS, now, fan ? FAN_COOLDOWN : DAGGER_COOLDOWN);
        float damage = EnhancedConfig.LOKI_DAGGER_DAMAGE.get().floatValue();
        for (float spread : fan ? new float[]{-FAN_ANGLE, 0.0F, FAN_ANGLE} : new float[]{0.0F}) {
            LokiDaggerEntity dagger = new LokiDaggerEntity(level, player, damage);
            dagger.tint(AbilityColors.chosen(player, fan ? HeroAbility.LOKI_DAGGER_FAN : HeroAbility.LOKI_DAGGERS));
            dagger.shootFromRotation(player, player.getXRot(), player.getYRot() + spread, 0.0F, DAGGER_SPEED, 0.5F);
            level.addFreshEntity(dagger);
        }
        gesture(player, data, Live.THROWING, GESTURE_TICKS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOKI_DAGGER.get(), SoundSource.PLAYERS,
                0.8F, fan ? 0.9F : 1.0F);
    }

    // ---- Ability 2: Duplicates ----

    /**
     * "Surprise!": Loki turns invisible and three copies of him walk off in different directions. Everything that
     * was hunting him goes after a copy instead.
     */
    private static void duplicates(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.LOKI_DUPLICATES, now)) {
            return;
        }
        data.startCooldown(HeroAbility.LOKI_DUPLICATES, now, DUPLICATES_COOLDOWN);
        Variant look = Powers.variant(player);

        // The copies are conjured first, so they look like him (and hold what he holds) before he disappears.
        List<LokiIllusionEntity> copies = new ArrayList<>();
        float start = player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * 60.0F;
        for (int i = 0; i < DUPLICATES; i++) {
            Vec3 dir = Vec3.directionFromRotation(0.0F, start + i * 360.0F / DUPLICATES);
            Vec3 at = player.position().add(dir.scale(1.2));
            if (!Teleports.fits(player, at)) {
                at = player.position();
            }
            copies.add(LokiIllusionEntity.conjure(player, at, player.position().add(dir.scale(DECOY_WALK)),
                    LokiIllusionEntity.Mode.DECOY, DUPLICATE_TICKS));
            level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(player, HeroAbility.LOKI_DUPLICATES, LokiIllusionEntity.LIGHT_COLOR), 1.2F),
                    at.x, at.y + 1.0, at.z, 12, 0.25, 0.6, 0.25, 0.02);
        }

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, HIDE_TICKS, 0, false, false, true), player);
        player.setInvisible(true);
        gesture(player, data, Live.HIDDEN, HIDE_TICKS);

        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(DECOY_RADIUS),
                mob -> mob.isAlive() && hunts(mob, player) && mob.distanceToSqr(player) <= DECOY_RADIUS * DECOY_RADIUS
                        && !Shelters.shelters(player, mob))) {
            LokiIllusionEntity decoy = copies.stream().min(Comparator.comparingDouble(c -> c.distanceToSqr(mob))).orElseThrow();
            mob.setTarget(decoy);
            if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
                mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, decoy);
            }
        }

        Powers.burst(level, player.getX(), player.getY() + 1.0, player.getZ(), look.color(), 30, 0.4);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOKI_ILLUSION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.enhanced.duplicates.surprise").withStyle(s -> s.withColor(look.color())), true);
    }

    /** Whether the mob is after this player (an ordinary target, or a brain's attack memory). */
    private static boolean hunts(Mob mob, Player player) {
        return mob.getTarget() == player || (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == player);
    }

    // ---- Ability 3: Shapeshift ----

    /**
     * Loki takes the shape of the creature (or player) he looks at, in a flash of green light; looking at nothing,
     * he is himself again.
     */
    private static void shapeshift(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        EntityHitResult hit = Raycasts.entity(player, SHAPESHIFT_RANGE, 0.3,
                e -> e != player && e.isAlive() && !e.isSpectator() && (e instanceof Mob || e instanceof Player), 1.0F);
        if (hit == null) {
            if (data.form().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.enhanced.shapeshift.nothing"), true);
                return;
            }
            data.setForm("");
            shimmer(player, data);
            player.displayClientMessage(Component.translatable("message.enhanced.shapeshift.self"), true);
            return;
        }
        Entity target = hit.getEntity();
        if (target.getType().is(ModTags.SHAPESHIFT_IMMUNE)) {
            player.displayClientMessage(Component.translatable("message.enhanced.shapeshift.cannot", target.getName()), true);
            return;
        }
        LokiForm form = target instanceof Player other
                ? LokiForm.player(other.getUUID(), other.getGameProfile().getName())
                : LokiForm.creature(target.getType());
        if (form.serialize().equals(data.form())) {
            return;
        }
        data.setForm(form.serialize());
        if (!form.isPlayer()) {
            // Creatures of his new kind stop hunting him at once.
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0),
                    mob -> mob.getType() == form.type() && hunts(mob, player) && !Shelters.shelters(player, mob))) {
                mob.setTarget(null);
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        }
        shimmer(player, data);
        player.displayClientMessage(Component.translatable("message.enhanced.shapeshift.into", form.displayName()), true);
    }

    /** The flash of green light of a change of shape. */
    private static void shimmer(ServerPlayer player, PowerData data) {
        Variant look = Powers.variant(player);
        ServerLevel level = player.serverLevel();
        level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.LOKI_SHAPESHIFT, look.color()), 1.6F), player.getX(),
                player.getY() + 1.0, player.getZ(), 60, 0.35, 0.8, 0.35, 0.03);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.glow(player, HeroAbility.LOKI_SHAPESHIFT, look.glow()), 1.2F), player.getX(),
                player.getY() + 1.0, player.getZ(), 24, 0.3, 0.7, 0.3, 0.06);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LOKI_SHAPESHIFT.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        gesture(player, data, Live.CASTING, GESTURE_TICKS);
    }

    // ---- Ability 4: Vanish ----

    /**
     * Gone in a green flash, and back where he looks (up to 20 blocks): on top of the block, or right behind the
     * creature, facing its back.
     */
    private static void vanish(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.LOKI_VANISH, now)) {
            return;
        }
        HitResult hit = Raycasts.any(player, VANISH_RANGE, e -> e instanceof LivingEntity living && living.isAlive() && e != player
                && !e.isSpectator() && !(e instanceof LokiIllusionEntity illusion && player.getUUID().equals(illusion.ownerId())), 1.0F);
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        Vec3 to;
        if (hit instanceof EntityHitResult entityHit) {
            LivingEntity target = (LivingEntity) entityHit.getEntity();
            to = Teleports.behind(player, target);
            if (to != null) {
                yaw = target.yBodyRot;
                Vec3 eye = to.add(0, player.getEyeHeight(), 0);
                Vec3 look = target.position().add(0, target.getBbHeight() * 0.6, 0).subtract(eye);
                pitch = (float) -(Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
            }
        } else if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            to = Teleports.onto(player, blockHit);
        } else {
            // Nothing in reach: he reappears in mid-air where he looks.
            to = hit.getLocation().subtract(0, player.getBbHeight() * 0.5, 0);
            if (!Teleports.fits(player, to)) {
                to = null;
            }
        }
        if (to == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.teleport.no_room"), true);
            return;
        }
        if (!HexManager.onTeleport(level, player, to)) {
            return;
        }
        data.startCooldown(HeroAbility.LOKI_VANISH, now, VANISH_COOLDOWN);
        Variant look = Powers.variant(player);
        // (3.5: the flash in the colour its pick chose.)
        int color = AbilityColors.color(player, HeroAbility.LOKI_VANISH, look.color());
        int glow = AbilityColors.glow(player, HeroAbility.LOKI_VANISH, look.glow());
        Vec3 from = player.position();
        vanishFlash(level, from, color, glow);
        level.playSound(null, from.x, from.y, from.z, ModSounds.LOKI_VANISH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.teleportTo(level, to.x, to.y, to.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.resetFallDistance();
        vanishFlash(level, to, color, glow);
        level.playSound(null, to.x, to.y, to.z, ModSounds.LOKI_VANISH.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        gesture(player, data, Live.CASTING, GESTURE_TICKS);
    }

    private static void vanishFlash(ServerLevel level, Vec3 at, int color, int glow) {
        level.sendParticles(new EnergyParticleOptions(color, 1.8F), at.x, at.y + 1.0, at.z, 36, 0.3, 0.7, 0.3, 0.08);
        level.sendParticles(new EnergyParticleOptions(glow, 2.4F), at.x, at.y + 1.0, at.z, 10, 0.1, 0.3, 0.1, 0.02);
    }

    // ---- Ability 5: Frost Giant ----

    /** Loki's true Jotun form, or Asgardian again. */
    private static void toggleFrostGiant(ServerPlayer player, PowerData data) {
        boolean frost = !data.toggle(Toggle.FROST_GIANT);
        data.setToggle(Toggle.FROST_GIANT, frost);
        applyForm(player, data);
        ServerLevel level = player.serverLevel();
        Variant look = Powers.variant(player);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(player, HeroAbility.LOKI_FROST_GIANT, frost ? FROST_COLOR : look.color()), 1.5F),
                player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.35, 0.8, 0.35, 0.03);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(player, HeroAbility.LOKI_FROST_GIANT, frost ? FROST_GLOW : look.glow()), 1.2F),
                player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.7, 0.3, 0.05);
        if (frost) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FROST_GIANT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
        player.displayClientMessage(Component.translatable(frost ? "message.enhanced.frost_giant.on" : "message.enhanced.frost_giant.off")
                .withStyle(s -> s.withColor(frost ? FROST_GLOW : look.color())), true);
    }

    /** A Frost Giant never freezes, and still water freezes over where he walks (like Frost Walker). */
    private static void frostTick(ServerPlayer player) {
        player.setTicksFrozen(0);
        if (!player.onGround() || player.isPassenger() || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ());
        double r2 = FROST_WALK_RADIUS * FROST_WALK_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(below.offset(-FROST_WALK_RADIUS, 0, -FROST_WALK_RADIUS),
                below.offset(FROST_WALK_RADIUS, 0, FROST_WALK_RADIUS))) {
            if (pos.distToCenterSqr(player.getX(), pos.getY() + 0.5, player.getZ()) >= r2
                    || !level.getBlockState(pos.above()).isAir() || BossFights.protects(level, pos) || BlockRights.behindTheWall(player, level, pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) && state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource()
                    && level.isUnobstructed(ice, pos, CollisionContext.empty())
                    && !EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP)) {
                level.setBlockAndUpdate(pos, ice);
            }
        }
    }

    /** A Frost Giant's blow: the creature is slowed and freezes over (it shivers and frosts up, as in powder snow). */
    public static void onMeleeHit(ServerPlayer player, LivingEntity target) {
        if (!isFrostGiant(player) || !target.isAlive() || !target.canFreeze()) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS, 1), player);
        // Thawing takes 2 a tick out of powder snow: this keeps it fully frozen for the length of the slowness.
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + FREEZE_TICKS * 2));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                20, target.getBbWidth() * 0.4, target.getBbHeight() * 0.3, target.getBbWidth() * 0.4, 0.03);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.shade(player, HeroAbility.LOKI_FROST_GIANT, FROST_GLOW), 1.2F), target.getX(),
                target.getY() + target.getBbHeight() * 0.6,
                target.getZ(), 10, target.getBbWidth() * 0.3, target.getBbHeight() * 0.3, target.getBbWidth() * 0.3, 0.02);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    // ---- gestures ----

    /** Shows a momentary state (a pose, the duplicates' invisibility) for {@code ticks}. */
    private static void gesture(ServerPlayer player, PowerData data, Live state, int ticks) {
        UNTIL.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(Live.class)).put(state, player.level().getGameTime() + ticks);
        data.set(state, true);
    }

    private LokiAbilities() {
    }
}
