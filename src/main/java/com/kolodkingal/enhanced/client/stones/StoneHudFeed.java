package com.kolodkingal.enhanced.client.stones;

import com.kolodkingal.enhanced.client.StoneHud;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.stones.StoneBeams;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * What the stones' powers tell the Infinity Stones' panel (lane L1's {@link StoneHud}) about the stone in use: the
 * Reality Stone's bubbles so far ("×3 bubbles", from the server's bubble message), "Slow Time" while the Time Stone
 * slows time, "Soul Sight" while it is open, and the heat of the Mind Beam and of the Purple Beam as a bar (how long
 * the beam has burned of its longest hold).
 */
public final class StoneHudFeed {
    /** The longest holds of the beams (the {@code [mind_stone]} and {@code [power_stone]} defaults). */
    private static final float MIND_MAX = 120.0F;
    private static final float POWER_MAX = 60.0F;

    /** The local player's bubbles so far, as the server last said. */
    private static int bubbles;

    static void register() {
        StoneHud.registerLine(StoneKind.REALITY, player -> bubbles <= 0 ? null
                : Component.translatable("hud.enhanced.stones.bubbles", bubbles));
        StoneHud.registerLine(StoneKind.TIME,
                player -> flag(player, Live.TIME_SLOWING, "hud.enhanced.stones.slow_time"));
        StoneHud.registerLine(StoneKind.SOUL,
                player -> flag(player, Live.SOUL_SIGHT, "hud.enhanced.stones.soul_sight"));
        StoneHud.registerBar(StoneKind.MIND,
                player -> heat(player, StoneBeams.MIND == BeamRenderer.styleOf(player.getId()), MIND_MAX));
        StoneHud.registerBar(StoneKind.POWER,
                player -> heat(player, Powers.get(player).is(Live.POWER_VEINS), POWER_MAX));
    }

    /** The server counted another bubble of the local player's (its message carries the count). */
    static void bubbles(int count) {
        bubbles = count;
    }

    @Nullable
    private static Component flag(Player player, Live flag, String key) {
        PowerData data = Powers.get(player);
        return data.is(flag) ? Component.translatable(key) : null;
    }

    private static double heat(Player player, boolean burning, float max) {
        if (!burning || !player.isUsingItem()) {
            return -1.0;
        }
        return Math.min(1.0, player.getTicksUsingItem() / max);
    }

    private StoneHudFeed() {
    }
}
