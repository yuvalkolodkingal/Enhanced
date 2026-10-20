package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Red Hulk (3.8, lane L2), as a {@link HeroClient} plug-in: his HUD lines (Ross's keys say he
 * can't; the Anger bar, the jump's charge and the absorb's time are {@code client/redhulk/RedHulkHud}), his
 * yellow-orange eyes once he is angry, his eight gestures ({@link RedHulkGestures}, started by the payloads of his
 * moves and by his states) and four stances, plus the Hulk's hunched rest; his first-person forearms ({@link
 * RedHulkActing}). His body is the Hulk's ({@code HulkPlayerRenderer} through {@code client/redhulk/RedHulkLooks})
 * with his heat on it ({@code HeatLayer}); Ross wears his suit ({@code RossSuitLayer}). He walks heavy ({@link
 * RunCharacter#RED_HULK}); he leaps; he does not fly.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a
 * client class except in a signature of {@link HeroClient}.
 */
public final class RedHulkClient implements HeroClient {
    @Override
    public Power power() {
        return Power.RED_HULK;
    }

    // ---- the HUD and the eyes ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        if (data.is(Live.RED_HULK_TRANSFORMING)) {
            return Component.translatable("hud.enhanced.red_hulk.changing").withStyle(ChatFormatting.RED);
        }
        if (data.toggle(Toggle.ROSS_FORM) && ability != 5 && data.power() == Power.RED_HULK) {
            return Component.translatable("hud.enhanced.red_hulk.ross").withStyle(ChatFormatting.DARK_GRAY);
        }
        return PowerHud.cooldown(data, ability, now);
    }

    /** His eyes burn yellow-orange once his anger runs high (and while he absorbs). */
    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return com.kolodkingal.enhanced.client.redhulk.HeatLayer.eyes(player, data);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        RedHulkGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        RedHulkActing.stanceTargets(s, p, data, on);
    }

    @Override
    public float lean(HeroAnimState st) {
        return RedHulkActing.lean(st);
    }

    @Override
    public float drop(HeroAnimState st) {
        return RedHulkActing.drop(st);
    }

    @Override
    public void ambient(PoseContext c) {
        RedHulkActing.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        RedHulkActing.aimed(c);
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case RED_HULK_JUMP_CHARGING -> GestureId.RED_HULK_JUMP_CROUCH;
            case RED_HULK_ABSORBING -> GestureId.RED_HULK_ABSORB;
            default -> null;
        };
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        // His moves' gestures are started by their payloads (which name him), his states' by liveGesture, Ross's by
        // the toggle's route.
        return id.name().startsWith("RED_HULK_");
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        RedHulkActing.gestureStarted(player, state, id, param);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main,
            FirstPersonArms.Offset off) {
        RedHulkActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        if (mc.level != null) {
            RedHulkActing.tick(mc.level);
            com.kolodkingal.enhanced.client.redhulk.RedHulkEffects.tick(mc.level);
        }
    }

    @Override
    public void clear() {
        RedHulkActing.clear();
        com.kolodkingal.enhanced.client.redhulk.RedHulkEffects.clear();
    }
}
