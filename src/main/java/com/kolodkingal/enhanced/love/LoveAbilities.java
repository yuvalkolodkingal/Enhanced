package com.kolodkingal.enhanced.love;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.config.EternityConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Love, Thor's daughter (3.8, PLAN 10.6), with Eternity's gifts: her keys, her body and her tick.
 *
 * <ul>
 * <li>R: the <b>Axe Throw</b>, Thor's throw ({@link ThrownWeaponEntity}: it smashes along its way, turns at a wall or
 * its range and flies back to her hand), only with Stormbreaker in hand ("Love needs her axe."); pressed again while it
 * flies, it turns home; one in her pack comes to her hand. Sneak + R: the <b>Thunder Strike</b> ({@link
 * ThunderStrike}).</li>
 * <li>G: flight, while she holds Stormbreaker (as Thor; {@code [love] flightNeedsAxe}).</li>
 * <li>V (held): Thor's <b>lightning</b> beam, in her gold and lilac ({@link ThorAbilities#lightningBeamFor}).</li>
 * <li>B (held): the purple <b>Eye Laser</b> ({@link EyeLaser}).</li>
 * <li>N: the <b>Bifrost</b> with the axe (the screen opens on her client, as Thor's; Sneak + N: to where she
 * looks).</li> </ul> A custom hero with her rows has each by its own row ({@code data.has}); his axe may be Mjolnir or
 * Stormbreaker.
 */
public final class LoveAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("love_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("love_armor");
    private static final ResourceLocation BODY_ATTACK = Enhanced.id("love_attack");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("love_knockback");
    private static final ResourceLocation BODY_SAFE_FALL = Enhanced.id("love_safe_fall");

    /** Her lightning's colours: a deep lilac (the tint of Thor's beam; light enough that it stays lilac), and gold. */
    public static final int LIGHTNING = 0x8A3FD8;
    public static final int LIGHTNING_GLOW = 0xFFE59A;
    /** Ticks between two throws (Thor's). */
    public static final int THROW_COOLDOWN = ThorAbilities.THROW_COOLDOWN;

    /** Whether she holds Stormbreaker in either hand. */
    public static boolean holdsStormbreaker(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(ModItems.STORMBREAKER.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The hand that holds her axe: Stormbreaker for Love; Mjolnir or Stormbreaker for a custom hero with her throw (as
     * {@code thor.hammer}). Null when she holds none.
     */
    @Nullable
    public static InteractionHand axeHand(Player player) {
        boolean love = Powers.power(player) == Power.LOVE;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.STORMBREAKER.get()) || !love && ThorAbilities.isMjolnir(stack)) {
                return hand;
            }
        }
        return null;
    }

    /**
     * Whether Love may fly now: while she holds Stormbreaker or it flies back to her (as Thor), or always when {@code
     * [love] flightNeedsAxe} is off.
     */
    public static boolean canFly(ServerPlayer player) {
        if (!cfg(EternityConfig.LOVE_FLIGHT_NEEDS_AXE, true)) {
            return true;
        }
        return holdsStormbreaker(player) || Powers.get(player).hammerEntityId() != -1;
    }

    /** Whether Love is worthy of Mjolnir ({@code [love] worthy}; {@code Traits.of} asks). */
    public static boolean worthy() {
        return cfg(EternityConfig.LOVE_WORTHY, true);
    }

    // ---- input ----

    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /** A key of hers, or (3.4) a custom hero's with her rows: {@code alt} is the Sneak variant. */
    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        // The held ones: letting go must always get through.
        if (ability == 3) {
            if (data.has(HeroAbility.LOVE_LIGHTNING)) {
                ThorAbilities.lightningBeamFor(player, data, down, LIGHTNING, LIGHTNING_GLOW);
            }
            return;
        }
        if (ability == 4) {
            if (data.has(HeroAbility.LOVE_EYE_LASER)) {
                EyeLaser.key(player, data, down);
            }
            return;
        }
        if (!down || player.isSpectator() || !player.isAlive()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    if (data.has(HeroAbility.LOVE_THUNDER_STRIKE)) {
                        ThunderStrike.strike(player, data);
                    }
                } else if (data.has(HeroAbility.LOVE_AXE)) {
                    throwOrRecall(player, data);
                }
            }
            case 2 -> {
                if (data.power() != Power.LOVE) {
                    return;
                }
                if (!player.mayFly()) {
                    player.displayClientMessage(Component.translatable("message.enhanced.love.needs_axe")
                            .withStyle(ChatFormatting.GRAY), true);
                } else {
                    PowerTicker.toggleFlight(player);
                }
            }
            case 5 -> {
                boolean look = alt;
                if (!data.has(look ? HeroAbility.LOVE_BIFROST_LOOK : HeroAbility.LOVE_BIFROST)) {
                    return;
                }
                if (axeHand(player) == null) {
                    player.displayClientMessage(Component.translatable("message.enhanced.love.needs_axe")
                            .withStyle(ChatFormatting.GRAY), true);
                } else if (look) {
                    Bifrost.toLook(player);
                } else {
                    // (Without Sneak her client opens the Bifrost screen; a bare press that reaches here goes home.)
                    Bifrost.home(player);
                }
            }
            default -> {
            }
        }
    }

    // ---- R: the axe ----

    /**
     * Thor's throw with her axe, or the recall: with the axe in hand it flies; without it, one already flying turns
     * home, else Thor's recall brings it from wherever it lies within reach; nothing within reach: "Love needs her
     * axe."
     */
    static void throwOrRecall(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.LOVE_AXE, now)) {
            return;
        }
        InteractionHand hand = axeHand(player);
        if (hand != null) {
            ItemStack axe = player.getItemInHand(hand);
            ThrownWeaponEntity thrown = new ThrownWeaponEntity(level, player, axe);
            player.setItemInHand(hand, ItemStack.EMPTY);
            level.addFreshEntity(thrown);
            data.setHammerEntityId(thrown.getId());
            data.startCooldown(HeroAbility.LOVE_AXE, now, THROW_COOLDOWN);
            player.swing(hand, true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MJOLNIR_THROW.get(),
                    SoundSource.PLAYERS, 1.2F, 0.95F);
            return;
        }
        if (level.getEntity(data.hammerEntityId()) instanceof ThrownWeaponEntity flying) {
            flying.setReturning(true);
            data.startCooldown(HeroAbility.LOVE_AXE, now, THROW_COOLDOWN);
            return;
        }
        // Her own axe from her pack to her hand; a custom hero's call is Thor's (from the ground, a creature's hands).
        int slot = player.getInventory().findSlotMatchingItem(new ItemStack(ModItems.STORMBREAKER.get()));
        if (slot >= 0 && data.power() == Power.LOVE) {
            ItemStack inHand = player.getMainHandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, player.getInventory().getItem(slot));
            player.getInventory().setItem(slot, inHand);
            data.startCooldown(HeroAbility.LOVE_AXE, now, THROW_COOLDOWN);
        } else if (data.power() != Power.LOVE && ThorAbilities.recall(player, data)) {
            data.startCooldown(HeroAbility.LOVE_AXE, now, THROW_COOLDOWN);
        } else {
            player.displayClientMessage(Component.translatable("message.enhanced.love.needs_axe")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ---- the tick ----

    /** Her tick (or a custom hero's with her rows): the axe in flight forgotten once gone, the beams. */
    public static void tick(ServerPlayer player, PowerData data) {
        if (data.has(HeroAbility.LOVE_AXE) && data.hammerEntityId() != -1
                && !(player.serverLevel().getEntity(data.hammerEntityId()) instanceof ThrownWeaponEntity)) {
            data.setHammerEntityId(-1);
        }
        if (data.has(HeroAbility.LOVE_LIGHTNING) && data.is(Live.CALLING_LIGHTNING)) {
            ThorAbilities.lightningBeamTick(player, data);
        }
        EyeLaser.tick(player, data);
    }

    // ---- the body ----

    /** Whether the player has Love's body (Love, or a custom hero with {@code love.body}). */
    public static boolean hasBody(PowerData data) {
        return data.has(HeroAbility.LOVE_BODY);
    }

    /**
     * Thor's daughter's body while the player has it (scaled by its strength for a custom hero): 32 health, armour, a
     * harder blow, hard to knock back, safe falls; nothing of it on anyone else. Health keeps its share of the maximum.
     */
    public static void applyBody(ServerPlayer player, PowerData data) {
        boolean on = hasBody(data);
        if (!on && player.getAttribute(Attributes.MAX_HEALTH).getModifier(BODY_HEALTH) == null) {
            return;
        }
        double k = on ? data.bodyScale(HeroAbility.LOVE_BODY) : 0.0;
        float oldMax = player.getMaxHealth();
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, cfg(EternityConfig.LOVE_HEALTH_BONUS, 12.0) * k, on);
        float newMax = player.getMaxHealth();
        if (newMax != oldMax && oldMax > 0.0F && player.isAlive()) {
            player.setHealth(Math.max(1.0F, Math.min(newMax, player.getHealth() * newMax / oldMax)));
        }
        setModifier(player, Attributes.ARMOR, BODY_ARMOR, cfg(EternityConfig.LOVE_ARMOR_BONUS, 4.0) * k, on);
        setModifier(player, Attributes.ATTACK_DAMAGE, BODY_ATTACK, cfg(EternityConfig.LOVE_ATTACK_BONUS, 3.0) * k, on);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK,
                Math.min(1.0, cfg(EternityConfig.LOVE_KNOCKBACK_RESISTANCE, 0.3) * k), on);
        setModifier(player, Attributes.SAFE_FALL_DISTANCE, BODY_SAFE_FALL, cfg(EternityConfig.LOVE_SAFE_FALL, 6.0) * k,
                on);
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id,
                                    double amount, boolean present) {
        AttributeInstance instance = entity.getAttribute(attribute);
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

    // ---- the end ----

    /** She loses the power: the beams stop (an axe in flight finds nobody to return to and drops by itself). */
    public static void endAll(ServerPlayer player, PowerData data) {
        if (data.is(Live.CALLING_LIGHTNING)) {
            ThorAbilities.lightningBeamFor(player, data, false, LIGHTNING, LIGHTNING_GLOW);
        }
        EyeLaser.stop(player, data, false);
        ThunderStrike.forget(player);
    }

    /** Held keys released (death, the power lost). */
    public static void stopHold(PowerData data) {
        data.set(Live.CALLING_LIGHTNING, false);
        data.set(Live.BEAMING, false);
    }

    public static void onGone(ServerPlayer player) {
        EyeLaser.forget(player);
        ThunderStrike.forget(player);
        ThorAbilities.forgetBeamPace(player);
    }

    public static void clear() {
        EyeLaser.clear();
        ThunderStrike.clear();
    }

    // ---- the numbers ----

    static double cfg(ConfigSpec.DoubleValue value, double fallback) {
        return EnhancedConfig.SPEC.isLoaded() && value != null ? value.get() : fallback;
    }

    static int cfg(ConfigSpec.IntValue value, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() && value != null ? value.get() : fallback;
    }

    static boolean cfg(ConfigSpec.BooleanValue value, boolean fallback) {
        return EnhancedConfig.SPEC.isLoaded() && value != null ? value.get() : fallback;
    }

    private LoveAbilities() {
    }
}
