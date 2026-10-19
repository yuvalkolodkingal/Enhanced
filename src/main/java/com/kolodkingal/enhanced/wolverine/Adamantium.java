package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * His adamantium bones are heavy (3.7, {@code [wolverine] heavy}): in water or lava he sinks ({@code sinkSpeed} 0.03 a
 * tick more downward), he cannot swim up faster than {@code maxSwimUp} 0.02 (his swimming is half as strong: the body's
 * modifier), sprint-swimming is refused; he walks along the bottom. A boat carries him; a bubble column still lifts him
 * (a way out a builder can make). Drowning brings him DOWN and the healing factor's rescue takes him to the shore (lane
 * L1). Magneto lifts him by his metal ({@code METAL_BODY}: {@code Metal.isMetalHero}). The movement is the player's own
 * (his client's, as swimming is), from {@link WolverineEvents}: the rule is here, the same on both sides.
 */
public final class Adamantium {
    /** Whether the heavy bones are on ({@code [wolverine] heavy}). */
    public static boolean heavy() {
        return WolverineAbilities.cfg(WolverineConfig.HEAVY, true);
    }

    /** Whether the player is in a bubble column (it lifts him whatever he weighs). */
    public static boolean inBubbles(Player player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        return level.getBlockState(feet).is(Blocks.BUBBLE_COLUMN) || level.getBlockState(feet.above())
                .is(Blocks.BUBBLE_COLUMN);
    }

    /** One tick of his weight in a fluid. Returns whether it pulled him down. Both sides. */
    public static boolean sink(Player player) {
        PowerData data = Powers.get(player);
        if (!heavy() || !WolverineAbilities.hasBody(data) || player.isPassenger() || player.getAbilities().flying
                || player.isSpectator()) {
            return false;
        }
        if (!player.isInWater() && !player.isInLava()) {
            return false;
        }
        if (inBubbles(player)) {
            return false;
        }
        if (player.isSprinting()) {
            player.setSprinting(false);
        }
        if (player.isSwimming()) {
            player.setSwimming(false);
        }
        Vec3 m = player.getDeltaMovement();
        double up = WolverineAbilities.cfg(WolverineConfig.MAX_SWIM_UP, 0.02);
        double sink = WolverineAbilities.cfg(WolverineConfig.SINK_SPEED, 0.03) * data.bodyScale(
                com.kolodkingal.enhanced.custom.HeroAbility.WOLVERINE_BODY);
        player.setDeltaMovement(m.x, Math.min(m.y, up) - sink, m.z);
        return true;
    }

    private Adamantium() {
    }
}
