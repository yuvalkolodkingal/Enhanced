package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.config.HealingConfig;
import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * B: the Berserker Rage (3.7). For {@code berserkTicks} 200 he fights in a fury: +{@code berserkDamage} 40 % damage,
 * +{@code berserkSpeed} 25 % speed, +0.5 knockback resistance, his healing {@code berserkRegenFactor} 2 times as fast,
 * the claws forced out, a slash every {@link #RAGE_SLASH_GAP} ticks; he cannot use items ("Too angry to eat!",
 * {@link WolverineEvents}). His own screen has a red edge (the client). When it ends he is tired: Weakness I for {@code
 * berserkTiredTicks} 100. Its cooldown is {@code berserkCooldown} 900. The state is {@link WolverineState#berserkUntil}
 * (synced to him, saved, not kept on death); others see {@code Live.WOLVERINE_BERSERK}.
 *
 * <p>The doubled healing: the healing factor's own regeneration is lane L1's; the Rage adds once more the factor's
 * always-on rate ({@code regenPerSecond}) for each step of {@code berserkRegenFactor} above 1, while the factor is
 * active (never while poisoned).
 */
public final class Berserker {
    /** The gap between two slashes in the Rage (ticks). */
    public static final int RAGE_SLASH_GAP = 6;
    /** The knockback resistance the Rage adds. */
    public static final double RAGE_KNOCKBACK = 0.5;

    /** The player's Wolverine state (both sides; another player's on a client is the empty one). */
    public static WolverineState get(Player player) {
        return player.getData(ModAttachments.WOLVERINE);
    }

    /** Whether the player is in the Rage now (the server, or his own client). */
    public static boolean raging(Player player) {
        return get(player).berserkUntil() > player.level().getGameTime();
    }

    /** The ticks of Rage left (0 when none). */
    public static long left(Player player) {
        return Math.max(0L, get(player).berserkUntil() - player.level().getGameTime());
    }

    /** B: the Rage begins. */
    public static boolean start(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.WOLVERINE_BERSERKER) || !data.isReady(HeroAbility.WOLVERINE_BERSERKER, now)
                || raging(player)) {
            return false;
        }
        data.startCooldown(HeroAbility.WOLVERINE_BERSERKER, now, WolverineAbilities.cfg(
                WolverineConfig.BERSERK_COOLDOWN, 900));
        int ticks = WolverineAbilities.cfg(WolverineConfig.BERSERK_TICKS, 200);
        player.setData(ModAttachments.WOLVERINE, get(player).withBerserkUntil(now + ticks));
        player.removeEffect(MobEffects.WEAKNESS);
        WolverineAbilities.state(player.getUUID()).raging = true;
        if (data.has(HeroAbility.WOLVERINE_CLAWS)) {
            Claws.popOut(player, data);
        }
        modifiers(player, true);
        ServerLevel level = player.serverLevel();
        WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_BERSERK.get(), 1.5F,
                0.95F + level.random.nextFloat() * 0.1F);
        WolverineAbilities.send(level, player, WolverineFxPayload.BERSERK_ON, List.of(player.position()), ticks);
        return true;
    }

    static void tick(ServerPlayer player, PowerData data, WolverineAbilities.State state, long now) {
        boolean raging = raging(player);
        modifiers(player, raging);
        if (raging) {
            regenerate(player, now);
            if (now % 60 == 0) {
                ServerLevel level = player.serverLevel();
                WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_GROWL.get(), 0.7F,
                        0.9F + level.random.nextFloat() * 0.2F);
            }
        } else if (state.raging) {
            end(player);
        }
        state.raging = raging;
    }

    /** The Rage is over: he is tired. */
    private static void end(ServerPlayer player) {
        int tired = WolverineAbilities.cfg(WolverineConfig.BERSERK_TIRED_TICKS, 100);
        if (tired > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, tired, 0));
        }
        WolverineAbilities.send(player.serverLevel(), player, WolverineFxPayload.BERSERK_OFF,
                List.of(player.position()), 0);
    }

    /** The Rage's extra healing: once more the factor's always-on rate per step of the factor above 1. */
    private static void regenerate(ServerPlayer player, long now) {
        if (now % 10 != 0 || !HealingFactor.active(player) || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        double factor = WolverineAbilities.cfg(WolverineConfig.BERSERK_REGEN_FACTOR, 2.0);
        double perSecond = WolverineAbilities.loaded() ? HealingConfig.REGEN_PER_SECOND.get() : 1.0;
        float amount = (float) (perSecond * (factor - 1.0) / 2.0);
        if (amount > 0.0F) {
            player.heal(amount);
        }
    }

    /** The Rage's speed, strength and weight on him, or none of them. */
    static void modifiers(ServerPlayer player, boolean on) {
        WolverineAbilities.setModifier(player, Attributes.MOVEMENT_SPEED, WolverineAbilities.RAGE_SPEED,
                WolverineAbilities.cfg(WolverineConfig.BERSERK_SPEED, 0.25), on,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        WolverineAbilities.setModifier(player, Attributes.ATTACK_DAMAGE, WolverineAbilities.RAGE_DAMAGE,
                WolverineAbilities.cfg(WolverineConfig.BERSERK_DAMAGE, 0.4), on,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        WolverineAbilities.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, WolverineAbilities.RAGE_KNOCKBACK,
                RAGE_KNOCKBACK, on);
    }

    private Berserker() {
    }
}
