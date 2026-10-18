package com.kolodkingal.enhanced.client.custom;

import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of the custom hero (3.3), as a hero plug-in. His keys are whatever powers he put on them, so a press
 * that starts a cooldown is acted as a cast (the hand thrust forward of Billy's Hex Bolt), a held beam by the stance of
 * any beam, and his telekinesis by the Scarlet Witch's grab and throw. His HUD, skin, field and senses are drawn by the
 * other classes of this package.
 */
public final class CustomClient implements HeroClient {
    @Override
    public Power power() {
        return Power.CUSTOM;
    }

    @Nullable
    @Override
    public GestureId cooldownGesture(int ability, long left) {
        return GestureId.BILLY_BOLT;
    }

    @Nullable
    @Override
    public GestureId liveGesture(Live live) {
        return live == Live.BEAMING ? GestureId.BILLY_BOLT : null;
    }

    @Nullable
    @Override
    public GestureId grabGesture() {
        return GestureId.TK_GRAB_WANDA;
    }

    @Nullable
    @Override
    public GestureId flingGesture() {
        return GestureId.TK_FLING_WANDA;
    }

    @Override
    public void tick(Minecraft mc) {
        CustomFx.tick(mc);
    }

    @Override
    public void clear() {
        CustomFx.clear();
    }
}
