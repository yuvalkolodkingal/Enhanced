package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AgathaShield;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Agatha Harkness (3.3), as a {@link HeroClient} plug-in. It only routes: her HUD lines, the violet
 * eyes of her magic and the look of her shield are {@link AgathaView}; her stance, her hands in first person and the
 * flourishes of her gestures are {@link AgathaActing}; the keys of the gestures {@link AgathaGestures}; the motes of her
 * flight {@link AgathaFlight}; the circle of runes, the stream of the Power Drain and the shield's shards {@link AgathaFx};
 * the hat of the Witches' Road {@link AgathaHatLayer}. Her beam is the Darkhold's, drawn by {@code BeamRenderer} and held
 * by the stance every beam has ({@link Stance#BEAM_HAND}).
 *
 * <p>The plug-in is built when {@link HeroClients} loads, which the game tests do on a server: nothing here names a client
 * class except in a signature of {@link HeroClient}.
 */
public final class AgathaClient implements HeroClient {
    /**
     * Her shield: a violet veil with a lattice of runes, the radius of the config (the style is remade when it changes).
     * Kept here, with no client class in it, because the shield's style is asked on a server too (the tests).
     */
    private static final ResourceLocation RUNE_LATTICE = Enhanced.id("textures/entity/agatha_shield.png");
    private static final int SHIELD_VEIL = 0x7A2FD6;
    private static final int SHIELD_LINES = 0xC48CFF;
    @Nullable
    private static ShieldStyle shield;

    @Override
    public Power power() {
        return Power.AGATHA;
    }

    // ---- the HUD, the eyes, the shield ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return AgathaView.status(player, data, ability, now);
    }

    @Override
    @Nullable
    public ShieldStyle shieldStyle() {
        float radius = (float) AgathaShield.radius();
        if (shield == null || shield.radius() != radius) {
            shield = new ShieldStyle(SHIELD_VEIL, SHIELD_LINES, RUNE_LATTICE, radius, AgathaShield::maxStrength, AgathaShield::maxTicks);
        }
        return shield;
    }

    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return AgathaView.eyes(player, data);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        AgathaGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        // (3.4: for a custom hero, only when the shield fields are her shield's: they are shared by every hero's shield.)
        on[Stance.AGATHA_SHIELD.ordinal()] = data.chaosShieldActive() && data.has(HeroAbility.AGATHA_SHIELD);
    }

    @Override
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        AgathaActing.edges(s, p);
    }

    @Override
    public GestureId takeoffGesture() {
        return GestureId.TAKEOFF_AGATHA;
    }

    @Override
    public float lean(HeroAnimState st) {
        return -1.5F * st.stance(Stance.AGATHA_SHIELD);
    }

    @Override
    public void aimed(PoseContext c) {
        AgathaActing.aimed(c);
    }

    @Override
    @Nullable
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        return AgathaGestures.predicted(data, ability, now, AgathaActing.fed());
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId cooldownGesture(int ability, long left) {
        return switch (ability) {
            case 3 -> GestureId.AGATHA_DRAIN;
            case 5 -> GestureId.AGATHA_RUNES;
            default -> null;
        };
    }

    @Override
    @Nullable
    public GestureId flagGesture(GestureRoutes.Flag flag, boolean on) {
        return flag == GestureRoutes.Flag.CHAOS_SHIELD ? (on ? GestureId.AGATHA_SHIELD_RAISE : GestureId.AGATHA_SHIELD_DROP) : null;
    }

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case BEAMING -> GestureId.AGATHA_BEAM;
            case CASTING -> GestureId.AGATHA_HEX;
            default -> null;
        };
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        return id == GestureId.TAKEOFF_AGATHA; // (the animator's own edge)
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        AgathaActing.started(player, id);
    }

    // ---- first person and flight ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        AgathaActing.firstPerson(s, main, off);
    }

    @Override
    public void flightParticles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density,
                                boolean local) {
        AgathaFlight.particles(level, player, st, fx, density, local);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        AgathaFx.tick(mc);
    }

    @Override
    public void clear() {
        AgathaFx.clear();
        AgathaActing.clear();
    }
}
