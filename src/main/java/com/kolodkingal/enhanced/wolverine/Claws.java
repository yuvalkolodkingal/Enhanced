package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroDamage;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * G: his claws (3.7). Three adamantium blades from between the knuckles of each fist come out ("snikt") or go back in
 * ("shink"): {@link Toggle#CLAWS_OUT}. While they are out his empty main hand hits for 1 + 2 (his body) + {@code
 * clawBonus} 4 = 7 and sweeps ({@link WolverineEvents}); an item in his main hand hides that hand's claws (he uses it
 * as ever). The claws are his bones, not gear: nothing can take them. Sneak + G is Claw Climb ({@link ClawClimb}),
 * which needs them out.
 *
 * <p>They come out by themselves for the Claw Slash and the Spin Slash, stay out through the Berserker Rage (G cannot
 * put them away then), and come out for two seconds when the Adamantium Injection makes him.
 */
public final class Claws {
    /** How long the claws take to come out and go in (ticks; the client draws them over these). */
    public static final int OUT_TICKS = 3;
    public static final int IN_TICKS = 2;
    /** How long they stay out after the injection. */
    public static final int AWAKENING_TICKS = 40;

    /** Whether the player's claws are out (both sides): the toggle, of a player who has the Claws. */
    public static boolean out(PowerData data) {
        return data.has(HeroAbility.WOLVERINE_CLAWS) && data.toggle(Toggle.CLAWS_OUT);
    }

    public static boolean out(Player player) {
        return out(Powers.get(player));
    }

    /** G: the claws out, or back in (not in the Rage: they stay out). */
    public static boolean toggle(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.WOLVERINE_CLAWS)) {
            return false;
        }
        if (data.toggle(Toggle.CLAWS_OUT)) {
            if (Berserker.raging(player)) {
                return false;
            }
            sheathe(player, data);
        } else {
            popOut(player, data);
        }
        WolverineAbilities.state(player.getUUID()).sheatheAt = 0;
        return true;
    }

    /** The claws come out (if they are not): the toggle on, the snikt. */
    static void popOut(ServerPlayer player, PowerData data) {
        if (data.toggle(Toggle.CLAWS_OUT)) {
            return;
        }
        data.setToggle(Toggle.CLAWS_OUT, true);
        ServerLevel level = player.serverLevel();
        WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_SNIKT.get(), 1.0F,
                0.95F + level.random.nextFloat() * 0.1F);
        WolverineAbilities.send(level, player, WolverineFxPayload.SNIKT, List.of(player.position()), 0);
    }

    /** The claws go back in (and the climb stops: it needs them). */
    static void sheathe(ServerPlayer player, PowerData data) {
        if (!data.toggle(Toggle.CLAWS_OUT)) {
            return;
        }
        data.setToggle(Toggle.CLAWS_OUT, false);
        data.setToggle(Toggle.CLAW_CLIMB, false);
        ServerLevel level = player.serverLevel();
        WolverineAbilities.sound(level, player.position(), ModSounds.WOLVERINE_SHINK.get(), 0.8F, 1.0F);
        WolverineAbilities.send(level, player, WolverineFxPayload.SHINK, List.of(player.position()), 0);
    }

    /** Sneak + G: Claw Climb on or off; it needs the claws out. */
    public static boolean toggleClimb(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.WOLVERINE_CLAW_CLIMB)) {
            return false;
        }
        boolean on = !data.toggle(Toggle.CLAW_CLIMB);
        if (on && !out(data)) {
            if (data.has(HeroAbility.WOLVERINE_CLAWS) && data.power() == Power.WOLVERINE) {
                popOut(player, data); // (the hero's own claws come out for it)
            } else {
                player.displayClientMessage(Component.translatable("message.enhanced.wolverine.climb_needs_claws")
                        .withStyle(ChatFormatting.GRAY), true);
                return false;
            }
        }
        data.setToggle(Toggle.CLAW_CLIMB, on);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.wolverine.climb_on"
                : "message.enhanced.wolverine.climb_off").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    /** After the injection: the claws out for {@link #AWAKENING_TICKS}, then in. */
    public static void awakened(ServerPlayer player) {
        PowerData data = Powers.get(player);
        popOut(player, data);
        WolverineAbilities.state(player.getUUID()).sheatheAt = player.level().getGameTime() + AWAKENING_TICKS;
    }

    static void tick(ServerPlayer player, PowerData data, WolverineAbilities.State state, long now) {
        if (!data.has(HeroAbility.WOLVERINE_CLAWS)) {
            if (data.toggle(Toggle.CLAWS_OUT) && data.power() != Power.CUSTOM) {
                data.setToggle(Toggle.CLAWS_OUT, false);
            }
            return;
        }
        if (Berserker.raging(player)) {
            popOut(player, data);
            state.sheatheAt = 0;
        } else if (state.sheatheAt != 0 && now >= state.sheatheAt) {
            state.sheatheAt = 0;
            sheathe(player, data);
        }
    }

    /**
     * What the claws add to a blow of his empty main hand: {@code clawBonus} (times a custom hero's strength of the
     * claws' damage), times the Rage's factor.
     */
    public static float bonus(Player player, PowerData data) {
        float bonus = (float) WolverineAbilities.cfg(WolverineConfig.CLAW_BONUS, 4.0);
        if (data.power() == Power.CUSTOM && data.kit() != null) {
            bonus *= HeroDamage.factor(ModDamageTypes.WOLVERINE_CLAWS, data.kit());
        }
        return bonus * WolverineAbilities.rageFactor(player);
    }

    private Claws() {
    }
}
