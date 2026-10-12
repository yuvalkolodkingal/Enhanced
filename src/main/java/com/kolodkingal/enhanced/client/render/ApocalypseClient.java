package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.ApocalypseMusic;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Apocalypse, the hero, as a {@link HeroClient} plug-in: his HUD lines, the eyes that glow with his
 * shield or his storm, the look of his shield, his two stances and eight gestures, his first-person hands and the sand
 * that swirls round his feet in the air. The boss, his hazards and the arena are drawn elsewhere ({@link ApocalypseRenderer},
 * {@link SandFx}, {@link ArenaFx}); this class is only the player.
 *
 * <p>The work is in small classes of its own: {@link ApocalypseView} (HUD, eyes, shield), {@link ApocalypseGestures} (the
 * keys), {@link ApocalypseActing} (stances, first-person rows, flourishes), {@link ApocalypseFlight}, {@link ApocalypseEmpower}
 * and {@link ApocalypseAuraLayer}.
 */
public final class ApocalypseClient implements HeroClient {
    @Override
    public Power power() {
        return Power.APOCALYPSE;
    }

    // ---- the HUD, the eyes, the shield ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return ApocalypseView.status(data, ability, now);
    }

    @Override
    @Nullable
    public ShieldStyle shieldStyle() {
        return ApocalypseView.shieldStyle();
    }

    @Override
    @Nullable
    public RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return ApocalypseView.eyes(player, data);
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        ApocalypseGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        on[Stance.APOC_SHIELD.ordinal()] = data.chaosShieldActive();
        on[Stance.APOC_STORM.ordinal()] = data.is(Live.SANDSTORM);
    }

    @Override
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        ApocalypseActing.edges(s, p, data, now);
    }

    @Override
    public GestureId takeoffGesture() {
        return GestureId.TAKEOFF_APOC;
    }

    @Override
    public float lean(HeroAnimState st) {
        return ApocalypseHeroMath.lean(st.stance(Stance.APOC_SHIELD), st.stance(Stance.APOC_STORM));
    }

    @Override
    public void aimed(PoseContext c) {
        ApocalypseActing.aimed(c);
    }

    @Override
    @Nullable
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        GestureId id = null;
        if (ability == 1 && data.isReady(1, now)) {
            id = sneaking ? GestureId.APOC_ENTOMB : GestureId.APOC_SAND_WAVE;
        } else if (ability == 3 && data.isReady(3, now)) {
            id = GestureId.APOC_PORTAL;
        } else if (ability == 4 && data.isReady(4, now) && !data.chaosShieldActive()) {
            id = GestureId.APOC_SHIELD_RAISE;
        } else if (ability == 5 && data.isReady(5, now)) {
            id = sneaking ? GestureId.APOC_STORM_START : GestureId.APOC_BESTOW;
        }
        // What the server will refuse is not acted: no Wave without ground, no Entomb with nobody to close on, no Portal at the
        // sky, no shield on an empty stomach, nothing at all while he is entombed.
        return id != null && ApocalypseHeroMath.works(ability, sneaking) ? id : null;
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId cooldownGesture(int ability, long left) {
        return switch (ability) {
            case 1 -> ApocalypseHeroMath.isEntomb(left, ApocalypseView.waveCooldown(), ApocalypseView.entombCooldown())
                    ? GestureId.APOC_ENTOMB : GestureId.APOC_SAND_WAVE;
            case 3 -> GestureId.APOC_PORTAL;
            case 5 -> GestureId.APOC_BESTOW;
            default -> null;
        };
    }

    @Override
    @Nullable
    public GestureId flagGesture(GestureRoutes.Flag flag, boolean on) {
        return flag == GestureRoutes.Flag.CHAOS_SHIELD ? (on ? GestureId.APOC_SHIELD_RAISE : GestureId.APOC_SHIELD_DROP) : null;
    }

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return live == Live.SANDSTORM ? GestureId.APOC_STORM_START : null;
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        return id == GestureId.TAKEOFF_APOC;
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        ApocalypseActing.gestureStarted(player, state, id, param);
    }

    // ---- first person and flight ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        ApocalypseActing.firstPerson(s, main, off);
    }

    @Override
    public void flightParticles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density, boolean local) {
        ApocalypseFlight.particles(level, player, st, fx, density, local);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        ApocalypseHeroMath.installCastCheck(ApocalypseActing::canCast);
        ClientLevel level = mc.level;
        if (level != null) {
            ApocalypseActing.tick(level);
            ApocalypseEmpower.tick(level);
        }
        ApocalypseMusic.tick(mc);
    }

    @Override
    public void clear() {
        ApocalypseActing.clear();
        ApocalypseEmpower.clear();
        SandFx.clear();
        ArenaFx.clear();
        ShieldFx.clear();
        ApocalypseMusic.stop();
        ApocalypseBosses.clear();
    }
}
