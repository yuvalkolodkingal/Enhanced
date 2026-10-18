package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The rules every blade, spike and creature of Hela's asks, whether the boss made it or the hero (3.5): who it may hurt,
 * how it hurts, and where the ground is.
 *
 * <ul>
 *     <li>The <b>boss's</b> blades and her army hurt the players of her fight (whatever PvP says: she is a villain), the
 *     creatures that belong to no fight, and never herself nor anything of her fight.</li>
 *     <li>The <b>hero's</b> blades and allies follow the rule of every hero's area attack ({@link AreaTargets#mayHurt}):
 *     never herself, her team, her pets or her own dead (the ally rule {@link HelaArmy} registers), players only with
 *     PvP, passive creatures never.</li>
 * </ul>
 */
public final class HelaRules {
    /** The blades' green: the line work on her suit, the glint on their edges. */
    public static final int GREEN = 0x3FBF6F;
    /** The pale green of her magic at its brightest. */
    public static final int PALE = 0x9CF2B5;
    /** The black-green of her smoke. */
    public static final int SMOKE = 0x14261B;

    /**
     * Whether something of {@code owner}'s (a blade, a spike, a berserker's blow) may hurt {@code victim}. A missing owner
     * (the boss already gone) hurts nothing.
     */
    public static boolean canHurt(@Nullable LivingEntity owner, LivingEntity victim) {
        if (owner == null || !victim.isAlive() || victim.isSpectator() || victim == owner) {
            return false;
        }
        if (BossFights.sameFight(owner, victim)) {
            return false;
        }
        if (HelaArmy.sameSide(owner, victim)) {
            return false;
        }
        boolean villain = !(owner instanceof Player);
        return AreaTargets.mayHurt(owner, victim, villain);
    }

    /** Hurts {@code victim} with one of her damage types, caused by {@code owner} and struck by {@code direct}. */
    public static boolean hurt(LivingEntity victim, ResourceKey<DamageType> type, @Nullable LivingEntity owner, @Nullable Entity direct,
                               float amount) {
        return victim.hurt(ModDamageTypes.source(victim.level(), type, owner, direct == null ? owner : direct), amount);
    }

    /**
     * The top of the ground in the column of {@code (x, z)}, looking down from {@code y} (climbing out first if the hint
     * is inside a block): what a spike rises from and a berserker climbs out of. {@code y - 8} if nothing is found in 24
     * blocks.
     */
    public static double groundAt(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(y), Math.floor(z));
        int top = level.getMaxBuildHeight();
        for (int i = 0; i < 8 && pos.getY() < top && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(); i++) {
            pos.move(0, 1, 0);
        }
        for (int i = 0; i < 24 && pos.getY() > level.getMinBuildHeight(); i++, pos.move(0, -1, 0)) {
            var shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return pos.getY() + shape.max(Direction.Axis.Y);
            }
        }
        return y - 8.0;
    }

    /** The point on the ground under {@code at} (see {@link #groundAt}). */
    public static Vec3 onGround(Level level, Vec3 at) {
        return new Vec3(at.x, groundAt(level, at.x, at.y + 1.0, at.z), at.z);
    }

    /** Whether {@code level} is a server level (her effects are sent only from there). */
    @Nullable
    public static ServerLevel server(Level level) {
        return level instanceof ServerLevel server ? server : null;
    }

    private HelaRules() {
    }
}
