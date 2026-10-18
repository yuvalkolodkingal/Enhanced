package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * The Soul Stone's sight (DESIGN section 15.11.2): for a few seconds every creature within the Sight's range glows
 * orange through the walls for the Thanos who looks, and nobody else. {@code EntityGlow} asks {@link #reveals} and
 * {@link #color}; the glow is the game's own outline, so it is drawn through walls like the Noise of Billy.
 */
public final class SoulSight {
    /** The orange of the Soul Stone. */
    public static final int COLOR = StoneKind.SOUL.color();

    /** Whether the local player is a Thanos with his Soul Sight open. */
    public static boolean active() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        PowerData data = Powers.get(player);
        return data.has(HeroAbility.THANOS_SOUL) && data.is(Live.SOUL_SIGHT);
    }

    /** Whether the local Thanos sees this entity glow right now. */
    public static boolean reveals(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || entity == player || !(entity instanceof LivingEntity) || entity instanceof ArmorStand || entity.isSpectator() || !active()) {
            return false;
        }
        double range = range();
        // (3.3: no soul is seen through the wall of a Hex that keeps him out.)
        return entity.distanceToSqr(player) < range * range && !Shelters.keepsOut(player.level(), player, player.getEyePosition(), Shelters.middle(entity), false);
    }

    /** The colour every creature he sees glows in. */
    public static int color(Entity entity) {
        return COLOR;
    }

    /** The Sight's range, as the server has it (the option reaches the client with the rest of the server's). */
    static double range() {
        try {
            return ThanosConfig.SIGHT_RANGE.get();
        } catch (IllegalStateException notLoaded) {
            return 32.0;
        }
    }

    private SoulSight() {
    }
}
