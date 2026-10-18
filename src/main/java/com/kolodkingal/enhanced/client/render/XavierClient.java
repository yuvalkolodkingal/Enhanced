package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.XavierAbilities;
import com.kolodkingal.enhanced.client.CerebroView;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.client.XavierControl;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The client side of Professor X (3.3), as a {@link HeroClient} plug-in: his HUD lines, his stances (the old professor
 * seated in his chair, the trance while his mind is away, Cerebro's helmet held, Billy's listening tilt while he hears
 * thoughts) and his gestures ({@link XavierGestures}), his first-person hands, and the wheels of his chair
 * ({@link XavierChair}). The rest of his client is in {@code client/}: {@code XavierControl} (the body he steers),
 * {@code ThoughtBubbles}, {@code CerebroView}, {@code XavierOverlay} and the screen of his two versions.
 */
public final class XavierClient implements HeroClient {
    @Override
    public Power power() {
        return Power.PROFESSOR_X;
    }

    // ---- the HUD ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return switch (ability) {
            case 2 -> onOff(data.toggle(Toggle.HEAR_THOUGHTS));
            case 3 -> XavierControl.linked()
                    ? Component.translatable("hud.enhanced.xavier.linked", seconds(XavierControl.ticksLeft())).withStyle(ChatFormatting.AQUA)
                    : PowerHud.cooldown(data, ability, now);
            case 5 -> CerebroView.active()
                    ? Component.translatable("hud.enhanced.xavier.searching", seconds(CerebroView.ticksLeft())).withStyle(ChatFormatting.AQUA)
                    : PowerHud.cooldown(data, ability, now);
            default -> PowerHud.cooldown(data, ability, now);
        };
    }

    private static String seconds(long ticks) {
        return String.format(Locale.ROOT, "%.0f", ticks / 20.0);
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "hud.enhanced.on" : "hud.enhanced.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        XavierGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        on[Stance.XAVIER_SEATED.ordinal()] = seated(p, s);
        on[Stance.XAVIER_TRANCE.ordinal()] = data.is(Live.MIND_LINKED);
        on[Stance.XAVIER_CEREBRO.ordinal()] = data.is(Live.CEREBRO) && !data.is(Live.MIND_LINKED);
        on[Stance.NOISE.ordinal()] = data.toggle(Toggle.HEAR_THOUGHTS) && !data.is(Live.MIND_LINKED);
    }

    /**
     * Whether he sits in his chair this tick: the old professor, unless he swims, sleeps, glides or rides something else.
     * (The player goes through {@code Object}: a dedicated server, which loads this class with the registry of heroes,
     * must never be asked whether a client player is a player.)
     */
    static boolean seated(AbstractClientPlayer p, HeroAnimState s) {
        return XavierAbilities.OLD.equals(s.look) && XavierAbilities.chairShown((Player) (Object) p);
    }

    @Override
    public void ambient(PoseContext c) {
        float w = c.st.stance(Stance.XAVIER_SEATED);
        if (w > 0.001F) {
            c.leg(true, XavierGestures.SEATED_LEG_X, XavierGestures.SEATED_LEG_Z, w);
            c.leg(false, XavierGestures.SEATED_LEG_X, -XavierGestures.SEATED_LEG_Z, w);
            c.arm(true, XavierGestures.ARMREST_M[0], XavierGestures.ARMREST_M[1], XavierGestures.ARMREST_M[2], w);
            c.arm(false, XavierGestures.ARMREST_O[0], XavierGestures.ARMREST_O[1], XavierGestures.ARMREST_O[2], w);
        }
    }

    @Override
    public void aimed(PoseContext c) {
        float w = c.st.stance(Stance.XAVIER_TRANCE);
        if (w > 0.001F) {
            float[] m = XavierGestures.TEMPLE_M;
            c.arm(true, m[0], m[1], m[2], w);
            c.occupy(true, w);
            c.head(XavierGestures.TRANCE_HEAD, 0, 0, w);
        }
        w = c.st.stance(Stance.XAVIER_CEREBRO);
        if (w > 0.001F) {
            float[] m = XavierGestures.TEMPLE_M;
            float[] o = XavierGestures.TEMPLE_O;
            c.arm(true, m[0], m[1], m[2], w);
            c.arm(false, o[0], o[1], o[2], w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    @Override
    @Nullable
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        // Only what never waits on a target: Freeze Minds and the Mental Blast need one the server finds.
        if (ability == 5 && data.isReady(5, now) && !data.is(Live.MIND_LINKED)) {
            return GestureId.XAVIER_CEREBRO;
        }
        return null;
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId cooldownGesture(int ability, long left) {
        return switch (ability) {
            case 1 -> GestureId.XAVIER_BLAST;
            case 4 -> GestureId.XAVIER_FREEZE;
            default -> null;
        };
    }

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case MIND_LINKED -> GestureId.XAVIER_LINK;
            case CEREBRO -> GestureId.XAVIER_CEREBRO;
            default -> null;
        };
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        float w = s.stance(Stance.XAVIER_CEREBRO);
        if (w > 0.001F) {
            main.add(w, -0.02F, 0.42F, 0.10F, -70, 10, 30);
            off.add(w, 0.02F, 0.42F, 0.10F, -70, -10, -30);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            switch (slot.id) {
                // The fingers rise toward the temple: the hand goes up and in, at the edge of the screen.
                case XAVIER_BLAST, XAVIER_LINK, XAVIER_THOUGHTS_ON -> main.add(slot.weight, -0.10F, 0.40F, 0.08F, -60, 14, 30);
                case XAVIER_FREEZE -> {
                    main.add(slot.weight, -0.10F, 0.40F, 0.08F, -60, 14, 30);
                    off.add(slot.weight, 0.06F, 0.16F, 0.22F, -40, -6, 0);
                }
                default -> {
                }
            }
        }
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        if (mc.level != null) {
            XavierChair.tick(mc.level);
        }
    }

    @Override
    public void clear() {
        XavierChair.clear();
    }
}
