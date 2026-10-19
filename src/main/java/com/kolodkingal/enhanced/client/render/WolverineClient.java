package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.wolverine.ClawLayer;
import com.kolodkingal.enhanced.client.wolverine.ScentView;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Wolverine (3.7), as a {@link HeroClient} plug-in: his HUD lines (the combo's step, the claws out
 * or in and the climb, the Rage's seconds as a red bar, Track's way and distance; the healing factor's icon is the
 * healing lane's), his fourteen gestures ({@link WolverineGestures}: the claws' by the toggle's routes, the moves' by
 * their payloads, the Rage and the Pounce by his states) and three stances, his first-person forearms ({@link
 * WolverineActing}). His claws are {@code client/wolverine/ClawLayer}, the classic cowl's fins {@link
 * WolverineCowlLayer}, his nose {@code ScentView}, the Rage's red edge {@code WolverineHud}. He runs low and leaning
 * ({@link RunCharacter#WOLVERINE}); he does not fly.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a
 * client class except in a signature of {@link HeroClient}.
 */
public final class WolverineClient implements HeroClient {
    /** The arrows of Track's way, by eighths of a turn from straight ahead, clockwise. */
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←",
            "↖"};
    /** How long after a slash its step stays on the HUD (the combo's reset). */
    static final int COMBO_SHOWN = 30;
    /** The local player's last slash: its step (0-2) and the game time. */
    private static int comboStep = -1;
    private static long comboAt = Long.MIN_VALUE / 2;

    @Override
    public Power power() {
        return Power.WOLVERINE;
    }

    // ---- the HUD ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        // (In a class of the client's own: the game tests load this plug-in on a server.)
        return com.kolodkingal.enhanced.client.wolverine.WolverineHud.status(player, data, ability, now);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        WolverineGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        WolverineActing.stanceTargets(s, p, data, on);
    }

    @Override
    public float lean(HeroAnimState st) {
        return WolverineActing.lean(st);
    }

    @Override
    public void ambient(PoseContext c) {
        WolverineActing.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        WolverineActing.aimed(c);
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case WOLVERINE_BERSERK -> GestureId.WOLVERINE_BERSERK_ROAR;
            case WOLVERINE_POUNCING -> GestureId.WOLVERINE_POUNCE_CROUCH;
            default -> null;
        };
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        // His moves' gestures are started by their payloads (which name him), his states' by liveGesture, the claws'
        // by the toggle's routes.
        return id.name().startsWith("WOLVERINE_");
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        WolverineActing.gestureStarted(player, state, id, param);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main,
            FirstPersonArms.Offset off) {
        WolverineActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        if (mc.level != null) {
            ClawLayer.tick(mc.level);
            ScentView.tick(mc.level);
        }
    }

    @Override
    public void clear() {
        ClawLayer.clear();
        ScentView.clear();
        com.kolodkingal.enhanced.client.wolverine.WolverineHud.clear();
    }
}
