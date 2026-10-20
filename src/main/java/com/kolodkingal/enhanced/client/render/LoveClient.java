package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Love (3.8), as a {@link HeroClient} plug-in: her HUD lines ({@code client/love/LoveHud}), her
 * violet eyes while the laser burns, her gestures ({@link LoveGestures}: the throw is the cooldown of R, the lightning
 * and the eyes her states, the catch and the strike started by {@link LoveActing}) and the holds of her two beams
 * ({@link LoveActing#aimed}).
 */
public final class LoveClient implements HeroClient {
    /** Her eyes' glow (a mask the size of the skin: two violet eyes). */
    private static final EyesType EYES = new EyesType();
    /** The longest cooldown of R that is a throw (Thor's 10 ticks; the strike keeps its own clock). */
    static final long THROW_COOLDOWN = 12;

    @Override
    public Power power() {
        return Power.LOVE;
    }

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        // (In a class of the client's own: the game tests load this plug-in on a server.)
        return com.kolodkingal.enhanced.client.love.LoveHud.status(player, data, ability, now);
    }

    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return data.has(HeroAbility.LOVE_EYE_LASER) && data.is(Live.BEAMING) ? EYES.get() : null;
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        LoveGestures.register();
    }

    @Override
    @Nullable
    public GestureId cooldownGesture(int ability, long left) {
        return ability == 1 && left <= THROW_COOLDOWN ? GestureId.LOVE_THROW : null;
    }

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case CALLING_LIGHTNING -> GestureId.LOVE_LIGHTNING;
            case BEAMING -> GestureId.LOVE_EYE_LASER;
            default -> null;
        };
    }

    /**
     * The catch and the strike are started by {@link LoveActing}; her lightning's and her eyes' gestures by her states'
     * routes ({@link #liveGesture}) and, in the Hero Creator, by the rows of her beams (whose states are owned by the
     * heroes before her).
     */
    @Override
    public boolean startedElsewhere(GestureId id) {
        return id == GestureId.LOVE_CATCH || id == GestureId.LOVE_STRIKE || id == GestureId.LOVE_LIGHTNING
                || id == GestureId.LOVE_EYE_LASER;
    }

    @Override
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        LoveActing.edges(s, p, data);
    }

    @Override
    public void aimed(PoseContext c) {
        LoveActing.aimed(c);
    }

    @Override
    public void clear() {
        LoveActing.clear();
    }

    /** The eyes' render type, made on first use (render types are the client's: the tests' server never asks). */
    private static final class EyesType {
        private RenderType type;

        RenderType get() {
            if (type == null) {
                type = RenderType.eyes(Enhanced.id("textures/entity/love_eyes.png"));
            }
            return type;
        }
    }
}
