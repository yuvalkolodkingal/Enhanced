package com.kolodkingal.enhanced.healing;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.HealingConfig;
import com.kolodkingal.enhanced.network.HealingFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * DOWN and the rise (3.7, the plan's 2.4): a blow that would have killed a healer leaves him at 1 health, kneeling, then
 * lying on his side for {@code downTicks}: untouchable (but to what may defeat him), still (no walking, no jumping, no
 * keys, no items, no blows), forgotten by the monsters that hunted him. Then he rises with {@code riseHealth} and a
 * little Resistance, out of a hazard first if he lies in one ({@link Rescue}). Everyone near sees it ({@link
 * HealingFxPayload} DOWN and RISE: the pose and the glints) and hears it; he is told once what can still defeat him.
 *
 * <p>The timer is the attachment's {@code downUntil} (game time); the bit everyone's client acts is {@link
 * Live#HEALING_DOWN}. A player who logs in still marked DOWN rises at once (the timer is not trusted across a logout).
 */
public final class Downed {
    /** The modifiers that hold him still (transient: never saved with the player). */
    static final ResourceLocation STILL = Enhanced.id("healing_down");
    /** How far (blocks) the monsters that hunt him are told to forget him. */
    static final double FORGET_RADIUS = 64.0;
    /** The blow cut short is shown at most this often (ticks) to those who strike a healer while he is DOWN. */
    static final int CUT_GAP = 5;
    /** The colour of the healing factor's light (a soft green-white). */
    public static final int GLINT = 0xC8FFD8;
    /** The advancement of the first rise ({@code impossible}: granted here). */
    static final ResourceLocation ADVANCEMENT = Enhanced.id("mutants/healing_factor");

    private Downed() {
    }

    // ---- the config, read when used ----

    public static int downTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.DOWN_TICKS.get() : 100;
    }

    public static float riseHealth() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.RISE_HEALTH.get().floatValue() : 8.0F;
    }

    public static int riseResistanceTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? HealingConfig.RISE_RESISTANCE_TICKS.get() : 60;
    }

    public static boolean mobsForget() {
        return !EnhancedConfig.SPEC.isLoaded() || HealingConfig.DOWN_MOBS_FORGET.get();
    }

    static boolean tipOnce() {
        return !EnhancedConfig.SPEC.isLoaded() || HealingConfig.TIP_ONCE.get();
    }

    // ---- the state ----

    public static HealingState state(ServerPlayer player) {
        return player.getData(ModAttachments.HEALING);
    }

    static void setState(ServerPlayer player, HealingState state) {
        player.setData(ModAttachments.HEALING, state);
    }

    /** The game time he rises at, or 0 when he is not DOWN. */
    public static long downUntil(ServerPlayer player) {
        return state(player).downUntil();
    }

    // ---- going down ----

    /**
     * He goes DOWN now (a blow was cut to leave him 1 health, or a death was refused): the timer, the bit, everything
     * held let go, the stillness, the monsters forget him, the effect, the one-time tip, the listeners. Nothing when he
     * is DOWN already.
     */
    public static void down(ServerPlayer player) {
        HealingState state = state(player);
        if (state.downUntil() > 0L) {
            return;
        }
        long now = player.level().getGameTime();
        int ticks = downTicks();
        boolean hazard = Rescue.inHazard(player);
        boolean tip = !state.has(HealingState.TIP_SHOWN) || !tipOnce();
        setState(player, state.withDownUntil(now + ticks).withLastHarm(now)
                .with(HealingState.WAS_DOWN_IN_HAZARD, hazard).with(HealingState.TIP_SHOWN, true));
        if (player.getHealth() < 1.0F) {
            player.setHealth(1.0F);
        }
        PowerData data = Powers.get(player);
        data.set(Live.HEALING_DOWN, true);
        PowerTicker.stopHoldAbilities(data);
        Powers.syncIfDirty(player);
        player.stopUsingItem();
        player.stopFallFlying();
        player.setSprinting(false);
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        holdStill(player, true);
        if (mobsForget()) {
            forget(player);
        }
        fx(player, HealingFxPayload.DOWN, ticks);
        sound(player, ModSounds.HEALING_DOWN.get(), 1.0F);
        if (tip) {
            player.sendSystemMessage(tip().withStyle(ChatFormatting.GREEN));
        }
        HealingHooks.down(player);
    }

    /** One tick of a DOWN healer: rise when his time is up (or his healing factor is gone), else stay still. */
    public static void tick(ServerPlayer player) {
        HealingState state = state(player);
        PowerData data = Powers.get(player);
        if (state.downUntil() <= 0L) {
            if (data.is(Live.HEALING_DOWN) || stillHeld(player)) {
                // (A bit left over: the state was cleared some other way.)
                data.set(Live.HEALING_DOWN, false);
                Powers.syncIfDirty(player);
                holdStill(player, false);
            }
            return;
        }
        long now = player.level().getGameTime();
        if (now >= state.downUntil() || !HealingFactor.has(player)) {
            rise(player);
            return;
        }
        if (player.getHealth() < 1.0F) {
            player.setHealth(1.0F);
        }
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (player.isUsingItem()) {
            player.stopUsingItem();
        }
        player.setSprinting(false);
        if (!data.is(Live.HEALING_DOWN)) {
            data.set(Live.HEALING_DOWN, true);
            Powers.syncIfDirty(player);
        }
        holdStill(player, true);
        if (mobsForget() && (now - state.downUntil()) % 10 == 0) {
            forget(player);
        }
    }

    // ---- the rise ----

    /** He rises now: out of a hazard first, his health, a little Resistance, the effect, the listeners. */
    public static void rise(ServerPlayer player) {
        HealingState state = state(player);
        setState(player, state.withDownUntil(0L).with(HealingState.WAS_DOWN_IN_HAZARD, false));
        PowerData data = Powers.get(player);
        data.set(Live.HEALING_DOWN, false);
        Powers.syncIfDirty(player);
        holdStill(player, false);
        if (!player.isAlive()) {
            return;
        }
        boolean rescued = Rescue.inHazard(player) && Rescue.rescue(player);
        float health = Math.min(player.getMaxHealth(), Math.max(player.getHealth(), riseHealth()));
        player.setHealth(health);
        int resistance = riseResistanceTicks();
        if (resistance > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resistance, 0, false, true, true));
        }
        fx(player, HealingFxPayload.RISE, rescued ? 1 : 0);
        sound(player, ModSounds.HEALING_RISE.get(), 1.0F);
        player.displayClientMessage(Component.translatable(rescued ? "message.enhanced.healing.rescued"
                : "message.enhanced.healing.back").withStyle(ChatFormatting.GREEN), true);
        award(player);
        HealingHooks.rise(player, rescued);
    }

    /** He logs in: still marked DOWN from before he left, he rises at once (with no effect: nobody saw him go). */
    public static void login(ServerPlayer player) {
        HealingState state = state(player);
        if (state.downUntil() > 0L) {
            setState(player, state.withDownUntil(0L).with(HealingState.WAS_DOWN_IN_HAZARD, false));
            player.setHealth(Math.min(player.getMaxHealth(), Math.max(player.getHealth(), riseHealth())));
        }
        PowerData data = Powers.get(player);
        if (data.is(Live.HEALING_DOWN)) {
            data.set(Live.HEALING_DOWN, false);
            Powers.syncIfDirty(player);
        }
        holdStill(player, false);
    }

    // ---- the pieces ----

    /** Puts on (or takes off) the modifiers that hold him still: no walking, no jumping. */
    static void holdStill(ServerPlayer player, boolean on) {
        hold(player, Attributes.MOVEMENT_SPEED, on);
        hold(player, Attributes.JUMP_STRENGTH, on);
    }

    private static void hold(ServerPlayer player, Holder<Attribute> attribute, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        boolean has = instance.hasModifier(STILL);
        if (on && !has) {
            instance.addTransientModifier(new AttributeModifier(STILL, -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!on && has) {
            instance.removeModifier(STILL);
        }
    }

    static boolean stillHeld(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        return instance != null && instance.hasModifier(STILL);
    }

    /**
     * Every creature that hunts him lets him be: its target, its brain's attack target, its anger. (While he is DOWN they
     * cannot take him again: {@code HealingEvents.onChangeTarget}.)
     */
    public static void forget(ServerPlayer player) {
        AABB around = player.getBoundingBox().inflate(FORGET_RADIUS);
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, around)) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
            if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                    && mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(t -> t == player).isPresent()) {
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
            if (mob instanceof NeutralMob neutral && player.getUUID().equals(neutral.getPersistentAngerTarget())) {
                neutral.stopBeingAngry();
            }
            if (mob.getLastHurtByMob() == player) {
                mob.setLastHurtByMob(null);
            }
        }
    }

    /**
     * The one-time tip of his first DOWN, from the config as it is now: "Only the Infinity Stones, chaos magic (...), the
     * Allfather's power, the Phoenix or poison can defeat you."
     */
    public static MutableComponent tip() {
        HealingFactor.Rules rules = HealingFactor.config();
        List<String> groups = new ArrayList<>();
        if (rules.stones()) {
            groups.add("stones");
        }
        if (rules.chaos()) {
            groups.add("chaos");
        }
        if (rules.odin()) {
            groups.add("odin");
        }
        if (rules.phoenix()) {
            groups.add("phoenix");
        }
        if (rules.darkhold()) {
            groups.add("darkhold");
        }
        if (HealingFactor.poisonStopsHealing()) {
            groups.add("poison");
        }
        if (groups.isEmpty()) {
            return Component.translatable("message.enhanced.healing.tip.none");
        }
        MutableComponent list = Component.translatable("message.enhanced.healing.tip." + groups.get(0));
        for (int i = 1; i < groups.size(); i++) {
            Component next = Component.translatable("message.enhanced.healing.tip." + groups.get(i));
            list = i == groups.size() - 1 ? Component.translatable("message.enhanced.healing.tip.or", list, next)
                    : Component.translatable("message.enhanced.healing.tip.comma", list, next);
        }
        return Component.translatable("message.enhanced.healing.tip", list);
    }

    /** Grants "Back on Your Feet" (the {@code mutants/healing_factor} advancement) at his first rise. */
    static void award(ServerPlayer player) {
        AdvancementHolder advancement = player.server.getAdvancements().get(ADVANCEMENT);
        if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            for (String criterion : advancement.value().criteria().keySet()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }

    /** Sends a moment of his healing factor to him and everyone who sees him. */
    public static void fx(ServerPlayer player, int kind, int arg) {
        Net.sendToPlayersTrackingEntityAndSelf(player, new HealingFxPayload(kind, player.getId(), arg));
    }

    /** A sound of the healing factor at him, for everyone near. */
    public static void sound(ServerPlayer player, SoundEvent sound, float volume) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume,
                1.0F);
    }
}
