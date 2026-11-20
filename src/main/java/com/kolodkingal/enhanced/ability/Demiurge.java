package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.DemiurgeConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.ultimate.Ultimate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Billy the Demiurge (3.18.1): Billy's ultimate version, as Cosmic Spider-Man is Spider-Man's. A transformation of
 * {@link Power#BILLY} that only a player with Ultimate may take ({@link Ultimate#allowed}: the switch {@code [ultimate]
 * enabled}, off by default, or an operator's grant); without it Sneak + N refuses, and when Ultimate is turned off or taken
 * back while he is the Demiurge he is plain Billy again at once ({@link #tick}).
 *
 * <p><b>The body.</b> {@code maxHealth} (40: twenty hearts), a netherite set's armour, toughness and knockback resistance on
 * top of what he wears, {@code regenAmount} healed every {@code regenIntervalTicks} on top of a player's own healing, and a
 * faster flight ({@link #applyBody}, from {@code PowerTicker.applyPassives} for every player, so that it is taken away from
 * one who is no longer the Demiurge).
 *
 * <p><b>His magic.</b> Every power of his hurts {@code damageFactor} times as much ({@link #outgoing}); every effect of it is
 * drawn in the Demiurge's brighter blue with small stars ({@link #BLUE}: {@code AbilityColors.chosen} gives it to every
 * effect that asks for its colour, and the client lays stars in it); his Hex Barrier wraps his whole body
 * ({@code BillyBarrier.covers}) with {@code barrierHealthFactor} times its health.
 *
 * <p><b>Magic Types.</b> H (Sneak + H: back) gives his keys the magic of another witch or sorcerer ({@link #MAGIC_TYPES}):
 * Billy's own, the Scarlet Witch's, Doctor Strange's, Agatha's, Loki's. He keeps the Demiurge's look. The choice is kept in
 * {@link PowerData#ironCharge} (Billy has no other use for it: saved and synced); {@link #borrowed} reads it, and
 * {@link PowerData#has} answers for the borrowed hero's abilities, so the hero's own code runs for him on both sides. Sneak + N
 * with his own magic lets the Demiurge go; with another's it is that hero's Sneak + N.
 *
 * <p><b>The gift.</b> He gives magic to a player who has no hero ({@code DemiurgeGifts}).
 */
public final class Demiurge {
    /** The Demiurge's blue (brighter than Billy's own), the colour of every effect of his magic. */
    public static final int BLUE = 0x4FD2FF;
    /** The white of his stars, and the pale blue of his aura. */
    public static final int STAR = 0xF4FBFF;
    public static final int AURA = 0x9FE6FF;

    /** Whose magic H steps through, in order: his own first. */
    public static final List<Power> MAGIC_TYPES = List.of(Power.BILLY, Power.SCARLET_WITCH, Power.DOCTOR_STRANGE, Power.AGATHA,
            Power.LOKI);

    private static final ResourceLocation HEALTH = Enhanced.id("demiurge_health");
    private static final ResourceLocation ARMOR = Enhanced.id("demiurge_armor");
    private static final ResourceLocation TOUGHNESS = Enhanced.id("demiurge_toughness");
    private static final ResourceLocation KNOCKBACK = Enhanced.id("demiurge_knockback");

    private Demiurge() {
    }

    // ---- who ----

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    static double cfg(ConfigSpec.DoubleValue value, double fallback) {
        return loaded() ? value.get() : fallback;
    }

    static int cfgInt(ConfigSpec.IntValue value, int fallback) {
        return loaded() ? value.get() : fallback;
    }

    static boolean cfgBool(ConfigSpec.BooleanValue value, boolean fallback) {
        return loaded() ? value.get() : fallback;
    }

    /** Whether the server lets Billy become the Demiurge at all ({@code billy.demiurge.enabled}). */
    public static boolean enabled() {
        return cfgBool(DemiurgeConfig.ENABLED, true);
    }

    /** Whether this player may be the Demiurge now: the feature is on and he has Ultimate. */
    public static boolean allowed(Player player) {
        return enabled() && Ultimate.allowed(player);
    }

    /** The same for a holder known only as an object (a client class, never loaded by a common class). */
    public static boolean allowedHolder(Object holder) {
        return enabled() && Ultimate.allowedHolder(holder);
    }

    /** Whether he is the Demiurge right now (both sides). */
    public static boolean is(PowerData data) {
        return data.power() == Power.BILLY && data.is(Live.BILLY_DEMIURGE);
    }

    public static boolean is(@Nullable Entity entity) {
        return entity instanceof Player player && is(Powers.get(player));
    }

    /** The magic type he has chosen (an index of {@link #MAGIC_TYPES}), whether he is the Demiurge now or not. */
    public static int type(PowerData data) {
        int t = data.power() == Power.BILLY ? data.ironCharge() : 0;
        return t >= 0 && t < MAGIC_TYPES.size() ? t : 0;
    }

    /** Whose magic is on his keys: the chosen magic type's while he is the Demiurge, else his own power. */
    public static Power keysOf(PowerData data) {
        return is(data) ? MAGIC_TYPES.get(type(data)) : data.power();
    }

    /** The witch or sorcerer whose magic he has taken on, or {@link Power#NONE} (his own, or not the Demiurge). */
    public static Power borrowed(PowerData data) {
        if (!is(data)) {
            return Power.NONE;
        }
        Power p = MAGIC_TYPES.get(type(data));
        return p == Power.BILLY ? Power.NONE : p;
    }

    // ---- the keys ----

    /**
     * A key of Billy's (from {@code AbilityHandler}): Sneak + N becomes the Demiurge, or (with his own magic on his keys)
     * lets him go. Returns whether the key was the Demiurge's.
     */
    public static boolean onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (data.power() != Power.BILLY || ability != 5 || !alt || borrowed(data) != Power.NONE) {
            return false;
        }
        if (down && !player.isSpectator()) {
            toggle(player, data);
        }
        return true;
    }

    /** Sneak + N: the Demiurge, or plain Billy again. */
    public static void toggle(ServerPlayer player, PowerData data) {
        if (is(data)) {
            revert(player, data, "message.enhanced.demiurge.off");
            return;
        }
        if (!enabled()) {
            say(player, "message.enhanced.demiurge.disabled");
            return;
        }
        if (!Ultimate.allowed(player)) {
            say(player, "message.enhanced.demiurge.no_ultimate");
            return;
        }
        enter(player, data);
    }

    /** He becomes Billy the Demiurge (Ultimate is not checked: the key does). */
    public static void enter(ServerPlayer player, PowerData data) {
        if (data.power() != Power.BILLY) {
            return;
        }
        data.set(Live.BILLY_DEMIURGE, true);
        if (!cfgBool(DemiurgeConfig.MAGIC_TYPES, true) && type(data) != 0) {
            data.setIronCharge(0);
        }
        data.markDirty();
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.4F, 1.9F);
        burst(level, player, 80);
        applyBody(player, data);
        player.setHealth(player.getMaxHealth());
        player.displayClientMessage(Component.translatable("message.enhanced.demiurge.on")
                .withStyle(s -> s.withColor(AURA).withBold(true)), true);
        Powers.syncIfDirty(player);
    }

    /** He is plain Billy again (his choice, or Ultimate is gone): the borrowed magic ends, his body is his own. */
    public static void revert(ServerPlayer player, PowerData data, @Nullable String message) {
        if (!data.is(Live.BILLY_DEMIURGE)) {
            return;
        }
        Power before = borrowed(data);
        if (before != Power.NONE) {
            PowerTicker.heroEnd(before, player, data);
        }
        data.set(Live.BILLY_DEMIURGE, false);
        data.markDirty();
        applyBody(player, data);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.3F);
        burst(level, player, 24);
        if (message != null) {
            player.displayClientMessage(Component.translatable(message).withStyle(ChatFormatting.GRAY), true);
        }
        Powers.syncIfDirty(player);
    }

    /** H (+1) or Sneak + H (-1) as the Demiurge: the next (or the last) magic type. False when he is not the Demiurge. */
    public static boolean cycle(ServerPlayer player, PowerData data, int step) {
        if (!is(data)) {
            return false;
        }
        if (!Ultimate.allowed(player) || !enabled()) {
            revert(player, data, "message.enhanced.demiurge.lost");
            return true;
        }
        if (!cfgBool(DemiurgeConfig.MAGIC_TYPES, true)) {
            say(player, "message.enhanced.demiurge.types_disabled");
            return true;
        }
        int n = MAGIC_TYPES.size();
        setType(player, data, ((type(data) + (step < 0 ? -1 : 1)) % n + n) % n);
        return true;
    }

    /** Sets his magic type: whatever the one before left in the world ends, and his keys are the new one's. */
    public static void setType(ServerPlayer player, PowerData data, int type) {
        if (data.power() != Power.BILLY || type < 0 || type >= MAGIC_TYPES.size()) {
            return;
        }
        Power before = borrowed(data);
        if (before != Power.NONE) {
            PowerTicker.heroEnd(before, player, data);
        } else if (is(data)) {
            // (his own magic lets go of what it holds: the beam, the barrier)
            BillyAbilities.letGo(player, data);
        }
        PowerTicker.stopHoldAbilities(data);
        data.setIronCharge(type);
        data.markDirty();
        Powers.syncIfDirty(player);
        if (!is(data)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        burst(level, player, 40);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.8F);
        player.displayClientMessage(Component.translatable("message.enhanced.demiurge.magic_type", typeName(type))
                .withStyle(s -> s.withColor(AURA).withBold(true)), true);
    }

    /** The name of a magic type ("Billy (your own)", "Doctor Strange"). */
    public static Component typeName(int type) {
        Power p = MAGIC_TYPES.get(Math.max(0, Math.min(MAGIC_TYPES.size() - 1, type)));
        return p == Power.BILLY ? Component.translatable("hud.enhanced.demiurge.own") : p.displayName();
    }

    // ---- every tick ----

    /** Every tick of a Billy (from {@code PowerTicker.heroTick}): Ultimate is checked, and the borrowed magic's upkeep runs. */
    static void tick(ServerPlayer player, PowerData data) {
        if (!data.is(Live.BILLY_DEMIURGE)) {
            return;
        }
        if (data.power() != Power.BILLY) {
            data.set(Live.BILLY_DEMIURGE, false);
            return;
        }
        if (!allowed(player)) {
            revert(player, data, "message.enhanced.demiurge.lost");
            return;
        }
        Power borrowed = borrowed(data);
        if (borrowed != Power.NONE) {
            PowerTicker.heroTick(borrowed, player, data);
        }
        int interval = cfgInt(DemiurgeConfig.REGEN_INTERVAL_TICKS, 40);
        if (player.isAlive() && player.tickCount % interval == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) cfg(DemiurgeConfig.REGEN_AMOUNT, 2.0));
        }
    }

    /** His body's modifiers, there while he is the Demiurge and gone otherwise (every player, every tick). */
    public static void applyBody(ServerPlayer player, PowerData data) {
        boolean on = is(data);
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        double base = health == null ? 20.0 : health.getBaseValue();
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, HEALTH, Math.max(0.0, cfg(DemiurgeConfig.MAX_HEALTH, 40.0) - base), on);
        PowerTicker.setModifier(player, Attributes.ARMOR, ARMOR, cfg(DemiurgeConfig.ARMOR, 20.0), on);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS, cfg(DemiurgeConfig.TOUGHNESS, 12.0), on);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, cfg(DemiurgeConfig.KNOCKBACK_RESISTANCE, 0.4), on);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** His flight speed as the Demiurge. */
    public static float flightSpeed() {
        return (float) cfg(DemiurgeConfig.FLIGHT_SPEED, 0.12);
    }

    /** How many times as hard his powers hit. */
    public static double damageFactor() {
        return cfg(DemiurgeConfig.DAMAGE_FACTOR, 2.0);
    }

    /** How many times its health his barrier has. */
    public static double barrierFactor() {
        return cfg(DemiurgeConfig.BARRIER_HEALTH_FACTOR, 2.0);
    }

    /**
     * What a hurt done by the Demiurge comes to: his powers (anything but a bare blow of his fist) hit {@code damageFactor}
     * times as hard. Anyone else's, and his own fist, as it was.
     */
    public static float outgoing(LivingEntity target, DamageSource source, float amount) {
        Entity cause = source.getEntity();
        if (!(cause instanceof Player player) || cause == target || !is(player)) {
            return amount;
        }
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player) {
            return amount; // (his bare fist)
        }
        return (float) (amount * damageFactor());
    }

    // ---- effects and words ----

    /** A burst of his blue and white stars. */
    public static void burst(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(new EnergyParticleOptions(BLUE, 1.4F), player.getX(), player.getY() + 1.0, player.getZ(), count,
                0.6, 0.9, 0.6, 0.15);
        level.sendParticles(new EnergyParticleOptions(STAR, 1.0F), player.getX(), player.getY() + 1.0, player.getZ(), count / 2,
                0.4, 0.8, 0.4, 0.25);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), count / 3, 0.5, 0.8, 0.5, 0.06);
    }

    static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }
}
