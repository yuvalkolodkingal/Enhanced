package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.hulk.HulkHooks;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Snap (3.6, the plan's 2.1): all six stones in a crafted Infinity Gauntlet, held by one who may wield it (a
 * Bearer, the hero Thanos, the Hulk as a Hulk), Sneak and hold use for {@code [gauntlet] snapHoldTicks} (3 s; the six
 * stones light in turn). Of the hostile creatures within {@code snapRadius} ({@link MobCategory#MONSTER}, not bosses,
 * not the bosses' minions, not named, not tamed) {@code snapFraction} (every second one, by entity id, the start turned
 * by the game time) turn to dust: ash drifting up, gone, no drops, no experience. Never players, pets, villagers or
 * animals.
 *
 * <p>The cost: a wielder other than Thanos is left at {@code snapCostHealth} with Weakness II for 30 s (the Hulk pays
 * his own way, {@link HulkHooks#onSnap}); the Gauntlet's wielder may not snap again for {@code snapCooldownTicks} (a
 * day; {@link StoneProgress#snapReadyAt}). Refused in a boss arena, in the Witches' Road and in Asgard ({@code
 * snapInAsgard}). The flag {@link StoneProgress#SNAPPED} and the advancement "Half of Everything" mark the first.
 */
public final class Snap {
    /** A Sneak + use let go sooner than this is the selected stone's variant, not the start of a Snap. */
    public static final int TAP_TICKS = 10;
    /** Weakness II, 30 s: the cost of a wielder other than Thanos. */
    private static final int WEAKNESS_TICKS = 600;
    /** How many of a Snap's dusted creatures play the dust's sound (1.7 s long: a crowd would drown everything). */
    private static final int DUST_VOICES = 8;
    /** The advancement of the first Snap. */
    public static final ResourceLocation ADVANCEMENT = Enhanced.id("stones/snap");

    /** Who is charging a Snap (server), and since when (game time). */
    private static final Map<UUID, Long> CHARGING = new HashMap<>();

    // ---- the config ----

    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || StonesConfig.SNAP_ENABLED.get();
    }

    public static int holdTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? StonesConfig.SNAP_HOLD_TICKS.get() : 60;
    }

    static int radius() {
        return EnhancedConfig.SPEC.isLoaded() ? StonesConfig.SNAP_RADIUS.get() : 64;
    }

    static double fraction() {
        return EnhancedConfig.SPEC.isLoaded() ? StonesConfig.SNAP_FRACTION.get() : 0.5;
    }

    static int cooldownTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? StonesConfig.SNAP_COOLDOWN_TICKS.get() : 24000;
    }

    static float costHealth() {
        return EnhancedConfig.SPEC.isLoaded() ? StonesConfig.SNAP_COST_HEALTH.get().floatValue() : 2.0F;
    }

    static boolean inAsgard() {
        return EnhancedConfig.SPEC.isLoaded() && StonesConfig.SNAP_IN_ASGARD.get();
    }

    // ---- the charge ----

    /** Whether the player's Gauntlet may begin a Snap: on, all six in, and its day of rest over. */
    public static boolean mayCharge(ServerPlayer player, GauntletContents contents) {
        return enabled() && !contents.titan() && contents.full() && ready(player);
    }

    /** Whether the player's wait after his last Snap is over. */
    public static boolean ready(ServerPlayer player) {
        return player.level().getGameTime() >= ModAttachments.STONES.get(player).snapReadyAt();
    }

    /** Game ticks before the player may snap again (0: ready). */
    public static long waitLeft(net.minecraft.world.entity.player.Player player) {
        return Math.max(0L, ModAttachments.STONES.get(player).snapReadyAt() - player.level().getGameTime());
    }

    public static void begin(ServerPlayer player) {
        CHARGING.put(player.getUUID(), player.level().getGameTime());
        Vec3 hand = hand(player);
        player.level().playSound(null, hand.x, hand.y, hand.z, ModSounds.SNAP_CHARGE.get(), SoundSource.PLAYERS, 1.0F,
                1.0F);
    }

    public static boolean charging(ServerPlayer player) {
        return CHARGING.containsKey(player.getUUID());
    }

    /** Ends a charge (let go, or done). */
    public static void end(ServerPlayer player) {
        CHARGING.remove(player.getUUID());
    }

    /**
     * A tick of the charge, {@code used} ticks after it began: the six stones light in turn (their voices, a spark of
     * their colour), and at {@link #holdTicks} the Snap.
     *
     * @return whether the Snap happened (or was refused) this tick: the use ends
     */
    public static boolean chargeTick(ServerPlayer player, int used) {
        int hold = Math.max(StoneKind.values().length, holdTicks());
        int step = hold / StoneKind.values().length;
        if (used < hold && used % step == 0 && used / step < StoneKind.values().length) {
            StoneKind kind = StoneKind.values()[used / step];
            Vec3 hand = hand(player);
            kind.voice().use(player.level(), hand, 0.5F, 1.0F);
            player.serverLevel().sendParticles(new EnergyParticleOptions(kind.color(), 0.5F), hand.x, hand.y, hand.z,
                    8, 0.15, 0.15, 0.15, 0.04);
        }
        if (used < hold) {
            return false;
        }
        end(player);
        perform(player);
        return true;
    }

    // ---- the Snap ----

    /** Why the Snap is refused here (a message key), or null. */
    @Nullable
    public static String refusal(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (BossFights.inAnyArena(level, player.blockPosition()) || RoadDimension.in(player)
                || AsgardDimension.in(player) && !inAsgard()) {
            return "message.enhanced.gauntlet.not_here";
        }
        return null;
    }

    /**
     * The Snap itself (the checks of the charge are done): half the hostile creatures near turn to dust, then the cost.
     *
     * @return how many turned to dust, or -1 when it was refused here
     */
    public static int perform(ServerPlayer player) {
        String refused = refusal(player);
        if (refused != null) {
            player.displayClientMessage(Component.translatable(refused).withStyle(ChatFormatting.GRAY), true);
            return -1;
        }
        ServerLevel level = player.serverLevel();
        int radius = radius();
        AABB box = player.getBoundingBox().inflate(radius);
        double r2 = (double) radius * radius;
        List<Mob> near = level.getEntitiesOfClass(Mob.class, box, mob -> eligible(mob)
                && mob.distanceToSqr(player) <= r2);
        List<Mob> dust = chosen(near, fraction(), level.getGameTime());
        for (int i = 0; i < dust.size(); i++) {
            toDust(level, dust.get(i), i < DUST_VOICES);
        }
        Vec3 hand = hand(player);
        level.playSound(null, hand.x, hand.y, hand.z, ModSounds.SNAP.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.FLASH, hand.x, hand.y, hand.z, 1, 0.0, 0.0, 0.0, 0.0);
        Net.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(), radius + 32.0,
                new StoneFxPayload(StoneFxPayload.SNAP, player.getId(), List.of(hand), dust.size()));
        cost(player);
        ModAttachments.STONES.set(player, ModAttachments.STONES.get(player).with(StoneProgress.SNAPPED)
                .withSnapReadyAt(level.getGameTime() + cooldownTicks()));
        award(player);
        player.displayClientMessage(Component.translatable("message.enhanced.gauntlet.snapped", dust.size())
                .withStyle(ChatFormatting.WHITE), true);
        return dust.size();
    }

    /** Whether the Snap may take this creature: hostile, not a boss or a boss's minion, not named, not tamed. */
    public static boolean eligible(Entity entity) {
        if (!(entity instanceof Mob mob) || !mob.isAlive() || mob.getType().getCategory() != MobCategory.MONSTER) {
            return false;
        }
        if (mob.getType().is(ModTags.BOSSES) || mob.getType().is(ModTags.BOSS_MINIONS) || mob.hasCustomName()) {
            return false;
        }
        return !(mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null);
    }

    /**
     * Which of {@code near} turn to dust: sorted by entity id, the start turned by the game time's parity, the ones
     * where a running count of {@code fraction} steps up (for one half: every second one), so exactly {@code floor(n *
     * fraction)} go. Deterministic.
     */
    public static <T extends Entity> List<T> chosen(List<T> near, double fraction, long gameTime) {
        List<T> sorted = new ArrayList<>(near);
        sorted.sort(Comparator.comparingInt(Entity::getId));
        int n = sorted.size();
        List<T> out = new ArrayList<>();
        if (n == 0 || fraction <= 0.0) {
            return out;
        }
        int start = (int) (gameTime & 1L) % n;
        for (int i = 0; i < n; i++) {
            if (Math.floor((i + 1) * fraction + 1.0E-9) > Math.floor(i * fraction + 1.0E-9)) {
                out.add(sorted.get((i + start) % n));
            }
        }
        return out;
    }

    /**
     * A creature turns to dust: ash drifting up, then it is gone (no drops, no experience); {@code voice}: with the
     * dust's sound (a long one, so only the first {@link #DUST_VOICES} of a Snap have it).
     */
    static void toDust(ServerLevel level, LivingEntity mob, boolean voice) {
        double h = mob.getBbHeight();
        level.sendParticles(ParticleTypes.ASH, mob.getX(), mob.getY() + h * 0.5, mob.getZ(), 40, mob.getBbWidth() * 0.4,
                h * 0.4, mob.getBbWidth() * 0.4, 0.02);
        level.sendParticles(ParticleTypes.WHITE_ASH, mob.getX(), mob.getY() + h * 0.6, mob.getZ(), 30,
                mob.getBbWidth() * 0.4, h * 0.4, mob.getBbWidth() * 0.4, 0.05);
        if (voice) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), ModSounds.SNAP_DUST.get(), SoundSource.HOSTILE,
                    0.8F, 0.9F + level.random.nextFloat() * 0.2F);
        }
        mob.discard();
    }

    /** What the Snap costs its wielder: nothing for Thanos, the Hulk's arm, everyone else's health and strength. */
    static void cost(ServerPlayer player) {
        Power power = Powers.power(player);
        if (power == Power.THANOS || player.isCreative()) {
            return;
        }
        if (power == Power.HULK) {
            HulkHooks.onSnap(player);
            return;
        }
        float left = costHealth();
        if (player.getHealth() > left) {
            player.setHealth(left);
        }
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 1));
    }

    private static void award(ServerPlayer player) {
        AdvancementHolder advancement = player.server.getAdvancements().get(ADVANCEMENT);
        if (advancement != null) {
            player.getAdvancements().award(advancement, "snap");
        }
    }

    private static Vec3 hand(ServerPlayer player) {
        return player.position().add(0.0, player.getBbHeight() * 0.7, 0.0).add(player.getLookAngle().scale(0.4));
    }

    private Snap() {
    }
}
