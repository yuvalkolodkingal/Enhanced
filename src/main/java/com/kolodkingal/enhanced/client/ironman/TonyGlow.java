package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The Arc Reactor in Tony's chest, glowing through his shirt (3.8.1): each of his looks has a mask the size of the
 * skin, {@code textures/entity/suit/<look's skin>_glow.png}, drawn full-bright over him by {@code GlowingEyesLayer}
 * (an eyes render type, so it shines in the dark). It shines while he wears no suit and while a suit goes on or comes
 * off (the suit's plates hide it as they close over his chest, and the suit's own reactor takes over); not in "your
 * own skin" (who knows where that chest is), and not while the Hulkbuster, drawn by its own renderer, is worn.
 */
public final class TonyGlow {
    private static final Map<Variant, RenderType> BY_LOOK = new HashMap<>();

    /** The reactor's glow for this Iron Man right now, or null. */
    @Nullable
    public static RenderType eyes(AbstractClientPlayer player, PowerData data) {
        Variant look = Variants.current(data);
        if (look.ownSkin()) {
            return null;
        }
        IronManState state = IronSuits.state(player);
        if (!state.inPhase() && state.worn() != null) {
            return null;
        }
        if (state.inPhase() && state.phaseKind() == SuitKind.HULKBUSTER) {
            return null;
        }
        return BY_LOOK.computeIfAbsent(look, v -> RenderType.eyes(Enhanced.id(
                v.skin().getPath().replace(".png", "_glow.png"))));
    }

    private TonyGlow() {
    }
}
