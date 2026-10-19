package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A speedster's footwork on water: while he runs at super speed with his surface switch on he runs across water as long
 * as he keeps running (3.3: Quicksilver's Wall &amp; Water Run; 3.6.1: Tommy's Wall Run too, now Wall &amp; Water Run,
 * and a custom hero with either). Players move themselves, so this is done where he is moved: by his own client every
 * tick ({@code ClientEvents}), and by the GameTests' simulated client. Walls and ceilings are {@code SpeedsterMotion}'s.
 */
public final class WaterRun {

    /**
     * Whether the player runs on water now: he runs at super speed (Quicksilver's or Tommy's, a custom hero's of
     * either) and his switch for running on surfaces is on ({@link SpeedSurfaces#toggleOn}: Quicksilver's Wall &amp;
     * Water Run, Tommy's Wall Run), not flying.
     */
    public static boolean mayRun(Player player) {
        PowerData data = Powers.get(player);
        boolean running = (data.has(HeroAbility.QUICKSILVER_SPEED) || data.has(HeroAbility.TOMMY_SPEED))
                && data.is(Live.SPEEDING);
        return running && !player.getAbilities().flying && !player.isSpectator() && SpeedSurfaces.isSpeedster(data)
                && SpeedSurfaces.toggleOn(data);
    }

    /**
     * One tick of his movement, before he moves: just over water (not in it, not rising), with air over it, he does not
     * sink: his fall stops there, he counts as on the ground and his fall distance is gone.
     *
     * @return whether the water held him this tick
     */
    public static boolean hold(Player player) {
        if (!mayRun(player)) {
            return false;
        }
        Vec3 motion = player.getDeltaMovement();
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
        if (player.isInWater() || motion.y > 0 || !player.level().getFluidState(below).is(FluidTags.WATER)
                || !player.level().getBlockState(below.above()).isAir()) {
            return false;
        }
        player.setDeltaMovement(motion.x, 0.0, motion.z);
        player.setOnGround(true);
        player.resetFallDistance();
        return true;
    }

    private WaterRun() {
    }
}
