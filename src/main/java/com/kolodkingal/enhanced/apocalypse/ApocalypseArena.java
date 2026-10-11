package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where Apocalypse is fought: a circle of radius {@code R} (24) round the block the Capstone was planted on, walled by a
 * cylinder of whirling sand. It is the framework's arena with his numbers: nothing inside the protected cylinder (radius
 * {@code R + 8}, from the bedrock to the top of the wall) can be broken, placed, flooded, burned, pushed or blown up;
 * the <em>wall</em> at {@code R + 4} pushes back anyone who touches it, once a second, for {@code fieldDamage} that cannot
 * kill, and stops flight at {@code arenaHeight}; anyone more than 8 blocks under the ground is lifted back by a violet
 * tractor; no teleport takes a fighter out of the cylinder (a pearl, the Bifrost, Vanish, the Tesseract, a portal); the
 * drops of a fighter who dies stay until the fight is over; and players within {@code R + 24} of the middle join the
 * fight. What the wall looks like is the clients' ({@code ARENA_ON}); the ground is found by looking down from the middle,
 * so that it works under a roof too (and in a closed test arena).
 */
public class ApocalypseArena extends BossArena {
    private final int radius;

    /**
     * @param center the top face of the block the Capstone was planted on
     * @param radius {@code R}, the radius of the fight ({@code arenaRadius})
     */
    public ApocalypseArena(ServerLevel level, Vec3 center, int radius, int height) {
        super(level, center, radius + 8.0, Containment.WALL, true);
        this.radius = radius;
        containAt(radius + 4.0);
        joinWithin(radius + 24.0);
        vertical(level.getMinBuildHeight(), center.y + height);
        tickets(radius + 16, 2);
        keepDrops(true);
        pushback(ApocalypseBossConfig.FIELD_DAMAGE.get().floatValue(), 0);
    }

    /** The arena for the config's numbers. */
    public static ApocalypseArena of(ServerLevel level, Vec3 center) {
        return new ApocalypseArena(level, center, ApocalypseBossConfig.ARENA_RADIUS.get(), ApocalypseBossConfig.ARENA_HEIGHT.get());
    }

    /** {@code R}, the radius of the circle he fights in. */
    public int fightRadius() {
        return radius;
    }

    /** The ground of the column of {@code (x, z)}, found by looking down from above the middle. */
    @Nullable
    @Override
    protected Vec3 surfaceSpot(ServerPlayer player, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!level().hasChunk(bx >> 4, bz >> 4)) {
            return null;
        }
        Vec3 spot = new Vec3(x, SandControl.groundAt(level(), x, center().y + 6.0, z), z);
        return Teleports.fits(player, spot) ? spot : spot.add(0.0, 1.0, 0.0);
    }
}
