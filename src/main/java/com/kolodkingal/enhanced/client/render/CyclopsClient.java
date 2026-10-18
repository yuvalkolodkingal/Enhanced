package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.CyclopsAbilities;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Cyclops (3.3), as a {@link HeroClient} plug-in: his HUD lines, his blazing eyes, his two stances and
 * six gestures, and his first-person hands. The work is in small classes of its own: {@link CyclopsView} (HUD, eyes),
 * {@link CyclopsGestures} (the keys), {@link CyclopsActing} (the stances, first person, the flourishes), {@link CyclopsBeamFx}
 * (his two beams), {@link CyclopsFx} (his other blasts) and {@link CyclopsVisorLayer} (the glasses on his head).
 *
 * <p>His beams set the shared {@code Live.BEAMING}, whose generic stance holds an arm out along the look: his own stances take
 * its place (his blasts leave his eyes, not a hand).
 */
public final class CyclopsClient implements HeroClient {
    @Override
    public Power power() {
        return Power.CYCLOPS;
    }

    // ---- the HUD and the eyes ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return CyclopsView.status(player, data, ability, now);
    }

    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return CyclopsView.eyes(player, data);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        CyclopsGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        // (3.4: a custom hero's beam is his eyes' only with Cyclops's beam abilities; another hero's keeps its hand.)
        boolean hero = data.power() != Power.CUSTOM;
        boolean optic = hero || data.has(HeroAbility.CYCLOPS_OPTIC_BLAST);
        boolean open = data.is(Live.CYCLOPS_UNLEASHED) && (hero || data.has(HeroAbility.CYCLOPS_GLASSES));
        if (optic || hero || data.has(HeroAbility.CYCLOPS_GLASSES)) {
            on[Stance.BEAM_HAND.ordinal()] = false;
        }
        on[Stance.CYCLOPS_BLAST.ordinal()] = optic && data.is(Live.BEAMING) && !open;
        on[Stance.CYCLOPS_UNLEASHED.ordinal()] = open;
    }

    @Override
    public float lean(HeroAnimState st) {
        return CyclopsActing.lean(st);
    }

    @Override
    public void aimed(PoseContext c) {
        CyclopsActing.aimed(c);
    }

    @Override
    @Nullable
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        GestureId id = switch (ability) {
            case 1 -> sneaking && data.isReady(CyclopsAbilities.RICOCHET_SLOT, now) ? GestureId.CYCLOPS_RICOCHET : null;
            case 2 -> data.isReady(CyclopsAbilities.CONCUSSION_SLOT, now) ? GestureId.CYCLOPS_CONCUSSION : null;
            case 3 -> data.isReady(CyclopsAbilities.LEAP_SLOT, now) ? GestureId.CYCLOPS_LEAP : null;
            case 4 -> data.isReady(CyclopsAbilities.RALLY_SLOT, now) ? GestureId.CYCLOPS_RALLY : null;
            case 5 -> sneaking ? GestureId.CYCLOPS_GLASSES_OFF : GestureId.CYCLOPS_GLASSES_ON;
            default -> null;
        };
        // What the server will refuse is not acted: no blast of the eyes without the glasses, no Leap in the air, no glasses
        // to take off or to put on.
        return id != null && CyclopsHeroMath.works(ability, sneaking) ? id : null;
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId cooldownGesture(int ability, long left) {
        return switch (ability) {
            case 1 -> GestureId.CYCLOPS_RICOCHET;
            case 2 -> GestureId.CYCLOPS_CONCUSSION;
            case 3 -> GestureId.CYCLOPS_LEAP;
            case 4 -> GestureId.CYCLOPS_RALLY;
            case 5 -> CyclopsHeroMath.glassesOff(left) ? GestureId.CYCLOPS_GLASSES_OFF : GestureId.CYCLOPS_GLASSES_ON;
            default -> null;
        };
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        CyclopsActing.gestureStarted(player, state, id, param);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        CyclopsActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        CyclopsHeroMath.installCastCheck(CyclopsActing::canCast);
        if (mc.level != null) {
            CyclopsFx.tick(mc.level);
        }
    }

    @Override
    public void clear() {
        CyclopsFx.clear();
    }
}
