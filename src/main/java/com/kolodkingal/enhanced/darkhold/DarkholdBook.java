package com.kolodkingal.enhanced.darkhold;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.DarkholdConfig;
import com.kolodkingal.enhanced.item.DarkholdItem;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
import com.kolodkingal.enhanced.vishanti.WardStrength;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Darkhold in use (3.2): Agatha Harkness's book, held in either hand. It gives its holder, a sorcerer (Loki, the
 * Scarlet Witch or Billy) or anyone at all if {@code darkhold.anyoneCanUse}:
 * <ul>
 *     <li>Agatha's purple beam: hold use, a continuous beam of purple magic with a white core (the generic {@link
 *     ContinuousBeam} with the {@link DarkholdItem#STYLE} palette), aimed along the look, at a cost in hunger;</li>
 *     <li>a retaliation: when magic is used on the holder ({@code enhanced:magic_damage}) whoever used it is poisoned for
 *     ten seconds ({@code darkhold.poisonTarget} can make it the holder instead), once in a while per attacker;</li>
 *     <li>Sneak + use at a creature: a hex mark, which makes it glow, slows it and makes it take more damage from the
 *     one who marked it; Sneak + use at nothing: a mind glimpse, which makes every creature near glow and says how
 *     many there are and how the nearest fares.</li>
 * </ul>
 * None of it works in the Witches' Road. What the book does not do is change a block.
 */
public final class DarkholdBook {
    private record Mark(UUID marker, long until) {
    }

    /** Who fires the purple beam now (the generic beam does not say whose it is). */
    private static final Set<UUID> FIRING = new HashSet<>();
    /** Marked creature -> the mark. */
    private static final Map<UUID, Mark> MARKS = new HashMap<>();
    /** Attacker -> the game time it may be poisoned again. */
    private static final Map<UUID, Long> POISONED_UNTIL = new HashMap<>();
    /** Holder -> the game time of their next mark / glimpse. */
    private static final Map<UUID, Long> MARK_READY = new HashMap<>();
    private static final Map<UUID, Long> GLIMPSE_READY = new HashMap<>();

    // ---- the book and who can read it ----

    public static boolean isBook(ItemStack stack) {
        return stack.is(ModItems.DARKHOLD.get());
    }

    /** The Darkhold in either hand. */
    public static boolean holds(LivingEntity entity) {
        return isBook(entity.getMainHandItem()) || isBook(entity.getOffhandItem());
    }

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    /** Whether the book works at all ({@code darkhold.enabled}; before the server's config has reached a client it does). */
    public static boolean enabled() {
        return !loaded() || DarkholdConfig.ENABLED.get();
    }

    /** Whether anyone can use it, not only a sorcerer ({@code darkhold.anyoneCanUse}). */
    public static boolean anyoneCanUse() {
        return loaded() && DarkholdConfig.ANYONE_CAN_USE.get();
    }

    /** Whether this dimension is the Witches' Road, where the book does nothing. */
    public static boolean isTheRoad(ResourceKey<Level> dimension) {
        return dimension.location().toString().equals(VishantiBook.ROAD);
    }

    /** Whether this entity is in the Witches' Road. */
    public static boolean inTheRoad(Entity entity) {
        return isTheRoad(entity.level().dimension());
    }

    /** Whether the player can use the book now: it is on, they are a sorcerer (or anyone may), and they are not in the Road. Both sides. */
    public static boolean mayUse(Player player) {
        return enabled() && !inTheRoad(player) && (WardStrength.isSorcerer(player) || anyoneCanUse());
    }

    /** Whether the player holds the book and may use it: what the retaliation asks, and the abilities. */
    public static boolean active(LivingEntity entity) {
        return entity instanceof Player player && holds(player) && mayUse(player);
    }

    /** Says why the player cannot use the book (in the action bar); false if they can. */
    public static boolean refuse(ServerPlayer player) {
        if (!enabled()) {
            player.displayClientMessage(Component.translatable("message.enhanced.darkhold.disabled"), true);
        } else if (inTheRoad(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.darkhold.road"), true);
        } else if (!mayUse(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.darkhold.not_sorcerer"), true);
        } else {
            return false;
        }
        return true;
    }

    // ---- the purple beam ----

    /** The numbers of the purple beam for one tick. */
    public static BeamSpec beamSpec() {
        int interval = DarkholdConfig.BEAM_INTERVAL.get();
        float perHit = DarkholdConfig.BEAM_DAMAGE_PER_SECOND.get().floatValue() * interval / 20.0F;
        float exhaustion = DarkholdConfig.BEAM_EXHAUSTION_PER_SECOND.get().floatValue() / 20.0F;
        return new BeamSpec(DarkholdItem.STYLE, DarkholdConfig.BEAM_RANGE.get(), perHit, interval, exhaustion, ModDamageTypes.DARKHOLD_BEAM, null);
    }

    /** Whether this player fires the purple beam now. */
    public static boolean firing(Player player) {
        return FIRING.contains(player.getUUID());
    }

    /** With an empty hunger bar there is nothing left to feed the beam (not in Creative). */
    private static boolean starving(Player player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    /** Use went down: the beam begins, if the player may use the book, is fed and no other beam of theirs burns. */
    public static void beamStart(ServerPlayer player) {
        if (player.isSpectator() || refuse(player) || ContinuousBeam.isFiring(player)) {
            return;
        }
        if (starving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.darkhold.hungry"), true);
            return;
        }
        FIRING.add(player.getUUID());
        ContinuousBeam.start(player, DarkholdItem.STYLE, DarkholdConfig.BEAM_RANGE.get());
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** One tick of a held beam: it ends when the player can no longer feed it or use the book, otherwise it burns. */
    public static void beamTick(ServerPlayer player) {
        if (!firing(player)) {
            return;
        }
        if (player.isSpectator() || !active(player) || starving(player)) {
            if (starving(player)) {
                player.displayClientMessage(Component.translatable("message.enhanced.darkhold.hungry"), true);
            }
            beamStop(player);
            return;
        }
        if (!ContinuousBeam.isFiring(player)) {
            FIRING.remove(player.getUUID()); // (the generic beam was stopped by something else)
            return;
        }
        ContinuousBeam.tick(player, beamSpec());
    }

    /** The beam ends (use came up, the book was put away, hunger). */
    public static void beamStop(ServerPlayer player) {
        if (FIRING.remove(player.getUUID())) {
            ContinuousBeam.stop(player);
        }
    }

    /** Every player, every tick: a beam whose holder no longer uses the book is over. */
    public static void everyPlayerTick(ServerPlayer player) {
        if (FIRING.isEmpty() || !firing(player)) {
            return;
        }
        if (!player.isUsingItem() || !isBook(player.getUseItem()) || !Powers.get(player).is(Live.BEAMING)) {
            beamStop(player);
        }
    }

    // ---- the hex mark and the mind glimpse (Sneak + use) ----

    /** Sneak + use: a hex mark on the creature looked at, or, with none in sight, a mind glimpse. */
    public static void markOrGlimpse(ServerPlayer player) {
        if (player.isSpectator() || refuse(player)) {
            return;
        }
        Entity seen = null;
        HitResult hit = Raycasts.any(player, DarkholdConfig.MARK_RANGE.get(), ContinuousBeam.targets(player), 1.0F);
        if (hit instanceof EntityHitResult entityHit) {
            seen = entityHit.getEntity() instanceof PartEntity<?> part ? part.getParent() : entityHit.getEntity();
        }
        if (seen instanceof LivingEntity target && !(seen instanceof ArmorStand)) {
            mark(player, target);
        } else {
            glimpse(player);
        }
    }

    private static long wait(Map<UUID, Long> ready, ServerPlayer player) {
        return Math.max(0L, ready.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime());
    }

    private static void pay(ServerPlayer player, double exhaustion) {
        if (!player.isCreative()) {
            player.causeFoodExhaustion((float) exhaustion);
        }
    }

    /** The hex mark: the creature glows, is slowed, and takes more damage from this player until it ends. */
    public static void mark(ServerPlayer player, LivingEntity target) {
        long wait = wait(MARK_READY, player);
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.darkhold.not_ready", (wait + 19) / 20), true);
            return;
        }
        if (target instanceof ServerPlayer other && !player.canHarmPlayer(other)) {
            player.displayClientMessage(Component.translatable("message.enhanced.darkhold.mark_refused"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        int ticks = DarkholdConfig.MARK_TICKS.get();
        MARK_READY.put(player.getUUID(), level.getGameTime() + DarkholdConfig.MARK_COOLDOWN.get());
        MARKS.put(target.getUUID(), new Mark(player.getUUID(), level.getGameTime() + ticks));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false), player);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, DarkholdConfig.MARK_SLOW_AMPLIFIER.get(), false, true), player);
        pay(player, DarkholdConfig.MARK_EXHAUSTION.get());
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        level.sendParticles(new EnergyParticleOptions(DarkholdItem.STYLE.main(), 1.4F), at.x, at.y, at.z, 24, 0.4, 0.5, 0.4, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
        player.displayClientMessage(Component.translatable("message.enhanced.darkhold.marked", target.getDisplayName()), true);
    }

    /** Whether this creature is hex-marked by someone, and who by. */
    @Nullable
    public static UUID markerOf(LivingEntity target, long gameTime) {
        Mark mark = MARKS.get(target.getUUID());
        if (mark == null) {
            return null;
        }
        if (gameTime >= mark.until()) {
            MARKS.remove(target.getUUID());
            return null;
        }
        return mark.marker();
    }

    /** What the marker's damage to a marked creature is multiplied by (1.0 if it is not theirs). */
    public static float markedFactor(LivingEntity target, @Nullable Entity attacker) {
        if (attacker == null || MARKS.isEmpty() || !loaded()) {
            return 1.0F;
        }
        UUID marker = markerOf(target, target.level().getGameTime());
        return marker != null && marker.equals(attacker.getUUID()) ? 1.0F + DarkholdConfig.MARK_DAMAGE_BONUS.get().floatValue() : 1.0F;
    }

    /** The mind glimpse: every living creature near glows for a few seconds, and the action bar says how many and how the nearest fares. */
    public static void glimpse(ServerPlayer player) {
        long wait = wait(GLIMPSE_READY, player);
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.darkhold.not_ready", (wait + 19) / 20), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        GLIMPSE_READY.put(player.getUUID(), level.getGameTime() + DarkholdConfig.GLIMPSE_COOLDOWN.get());
        pay(player, DarkholdConfig.GLIMPSE_EXHAUSTION.get());
        double range = DarkholdConfig.GLIMPSE_RANGE.get();
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && e.distanceToSqr(player) <= range * range));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        if (near.size() > DarkholdConfig.GLIMPSE_MAX.get()) {
            near = near.subList(0, DarkholdConfig.GLIMPSE_MAX.get());
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 0.8F);
        if (near.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enhanced.darkhold.glimpse_none"), true);
            return;
        }
        for (LivingEntity creature : near) {
            creature.addEffect(new MobEffectInstance(MobEffects.GLOWING, DarkholdConfig.GLIMPSE_TICKS.get(), 0, false, false), player);
        }
        LivingEntity nearest = near.get(0);
        player.displayClientMessage(Component.translatable("message.enhanced.darkhold.glimpse", near.size(), nearest.getDisplayName(),
                String.format(Locale.ROOT, "%.1f", nearest.getHealth()), String.format(Locale.ROOT, "%.0f", nearest.getMaxHealth())), true);
    }

    // ---- the retaliation ----

    /**
     * Magic was used on the holder by {@code attacker}: the poison falls on the attacker (or on the holder, as {@code
     * darkhold.poisonTarget} says), unless this attacker has been poisoned a moment ago.
     *
     * @return whether the poison was dealt
     */
    public static boolean retaliate(ServerPlayer holder, LivingEntity attacker) {
        ServerLevel level = holder.serverLevel();
        long now = level.getGameTime();
        if (attacker == holder || now < POISONED_UNTIL.getOrDefault(attacker.getUUID(), 0L)) {
            return false;
        }
        if (POISONED_UNTIL.size() > 256) {
            POISONED_UNTIL.values().removeIf(t -> t <= now);
        }
        POISONED_UNTIL.put(attacker.getUUID(), now + DarkholdConfig.POISON_COOLDOWN_TICKS.get());
        LivingEntity victim = DarkholdConfig.POISON_TARGET.get() == DarkholdConfig.PoisonTarget.HOLDER ? holder : attacker;
        victim.addEffect(new MobEffectInstance(MobEffects.POISON, DarkholdConfig.POISON_TICKS.get(), DarkholdConfig.POISON_AMPLIFIER.get()), holder);
        Vec3 at = victim.position().add(0, victim.getBbHeight() * 0.6, 0);
        level.sendParticles(new EnergyParticleOptions(DarkholdItem.STYLE.main(), 1.3F), at.x, at.y, at.z, 16, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 0.4F);
        if (victim instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.enhanced.darkhold.poisoned"), true);
        }
        return true;
    }

    // ---- cleanup ----

    /** The player logged out: their beam ends without a word. */
    public static void onLogout(ServerPlayer player) {
        if (FIRING.remove(player.getUUID())) {
            ContinuousBeam.stop(player);
        }
    }

    public static void clear() {
        FIRING.clear();
        MARKS.clear();
        POISONED_UNTIL.clear();
        MARK_READY.clear();
        GLIMPSE_READY.clear();
    }

    private DarkholdBook() {
    }
}
