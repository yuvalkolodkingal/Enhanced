package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;

/**
 * Clear sight in lava (3.17, the Hero Creator's {@code phoenix.lava_sight} pick: {@link Trait#LAVA_SIGHT}): while the camera is
 * in lava the lava's thick fog is thinned away, so he sees as in the open. Only the fog: the lava burns as ever (pair it with
 * fire immunity). The fog event is the same on every version ({@link ViewportEvent.RenderFog#setWidens}: from 1.21.6 the
 * game's own fog may otherwise only come nearer).
 */
@Subscriber(side = Side.CLIENT)
public final class LavaSight {
    /** Where the thinned fog begins, and ends, in blocks: far enough that the lava in front of him is plain. */
    static final float NEAR = 40.0F;
    static final float FAR = 160.0F;

    /** Whether this player sees clearly in lava. */
    public static boolean sees(Player player) {
        return Traits.has(player, Trait.LAVA_SIGHT);
    }

    @Subscribe
    public static void onFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.LAVA || !(event.getCamera().getEntity() instanceof Player player) || !sees(player)) {
            return;
        }
        event.setNearPlaneDistance(Math.max(event.getNearPlaneDistance(), NEAR));
        event.setFarPlaneDistance(Math.max(event.getFarPlaneDistance(), FAR));
        event.setWidens(true);
        event.setCanceled(true);
    }

    private LavaSight() {
    }
}
