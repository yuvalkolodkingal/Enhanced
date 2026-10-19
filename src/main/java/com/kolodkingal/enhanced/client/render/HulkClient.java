package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.hulk.HulkForms;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of the Hulk (3.6), as a {@link HeroClient} plug-in: his HUD lines (Bruce Banner's keys say he can't;
 * the rage bar and Banner's heart rate are {@code client/hulk/HulkHud}), his gamma-green eyes while he is enraged, his
 * thirteen gestures ({@link HulkGestures}, started by the payloads of his moves) and three stances, his first-person
 * forearms ({@link HulkActing}). His body is {@code client/hulk/HulkModel} drawn by {@code HulkPlayerRenderer}, his
 * effects {@code HulkFx}. He walks with a heavy lope ({@link RunCharacter#HULK}); Banner walks as anyone. He leaps; he
 * does not fly.
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a
 * client class except in a signature of {@link HeroClient}.
 */
public final class HulkClient implements HeroClient {
    @Override
    public Power power() {
        return Power.HULK;
    }

    // ---- the HUD and the eyes ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        if (data.is(Live.HULK_TRANSFORMING)) {
            return Component.translatable("hud.enhanced.hulk.changing").withStyle(ChatFormatting.GREEN);
        }
        if (HulkForms.isHuman(data) && ability != 5) {
            return Component.translatable("hud.enhanced.hulk.bruce").withStyle(ChatFormatting.DARK_GRAY);
        }
        return PowerHud.cooldown(data, ability, now);
    }

    /** His eyes glow gamma green while he is enraged. */
    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return data.is(Live.HULK_ENRAGED) && !HulkForms.isHuman(data) ? HulkActing.eyes(player) : null;
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        HulkGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        HulkActing.stanceTargets(s, p, data, on);
    }

    @Override
    public float lean(HeroAnimState st) {
        return HulkActing.lean(st);
    }

    @Override
    public void ambient(PoseContext c) {
        HulkActing.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        HulkActing.aimed(c);
    }

    // ---- which gesture stands for what ----

    @Override
    public boolean startedElsewhere(GestureId id) {
        // Every gesture of his is started by the payload of the move it acts (it names him).
        return id.name().startsWith("HULK_");
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        HulkActing.gestureStarted(player, state, id, param);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main,
            FirstPersonArms.Offset off) {
        HulkActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        if (mc.level != null) {
            HulkActing.tick(mc.level);
            com.kolodkingal.enhanced.client.hulk.HulkEffects.tick(mc.level);
        }
        com.kolodkingal.enhanced.client.hulk.HulkHud.tick(mc);
    }

    @Override
    public void clear() {
        HulkActing.clear();
        com.kolodkingal.enhanced.client.hulk.HulkEffects.clear();
        com.kolodkingal.enhanced.client.hulk.HulkHud.clear();
    }
}
