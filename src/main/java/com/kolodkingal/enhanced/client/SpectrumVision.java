package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.PhotonAbilities;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Client side of Photon's Spectrum Vision. */
public final class SpectrumVision {
    public static boolean active() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        PowerData data = Powers.get(player);
        return data.power() == Power.PHOTON && data.spectrumVision();
    }

    public static boolean reveals(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && entity != player && entity instanceof LivingEntity && !entity.isSpectator() && active()
                && entity.distanceToSqr(player) < PhotonAbilities.VISION_RANGE * PhotonAbilities.VISION_RANGE;
    }

    private SpectrumVision() {
    }
}
