package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EntityGlow;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.odin.OdinAbilities;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client side of Odin, the Allfather (3.5), as a {@link HeroClient} plug-in: his HUD lines and the sight of his ravens
 * ({@link OdinView}), his golden shield, his thirteen gestures ({@link OdinGestures}) and three stances, his first-person
 * hands and flourishes ({@link OdinActing}), his light ({@link OdinFx}), the winged helmet ({@link OdinHelmLayer}), the
 * spear in flight ({@link GungnirRenderer}) and his ravens ({@link RavenRenderer}). He walks with an old king's stride
 * ({@link RunCharacter#ODIN}); he does not fly.
 */
public final class OdinClient implements HeroClient {
    private static final int SHIELD_VEIL = 0xE8B44A;
    private static final int SHIELD_LINES = 0xFFF0C2;
    @Nullable
    private ShieldStyle shield;

    /** Once, from the client's setup: who his ravens see glows for him, and his kinds of the realm's effects are drawn. */
    public static void setup() {
        EntityGlow.register(OdinView::reveals, OdinView::color);
        OdinFx.register();
    }

    @Override
    public Power power() {
        return Power.ODIN;
    }

    // ---- the HUD and the shield ----

    @Override
    @Nullable
    public Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return OdinView.status(player, data, ability, now);
    }

    @Override
    public ShieldStyle shieldStyle() {
        if (shield == null) {
            shield = new ShieldStyle(SHIELD_VEIL, SHIELD_LINES, com.kolodkingal.enhanced.Enhanced.id("textures/entity/hex_wall.png"),
                    (float) OdinAbilities.SHIELD_RADIUS, OdinAbilities::shieldStrength, OdinAbilities::shieldMaxTicks);
        }
        return shield;
    }

    // ---- the animation ----

    @Override
    public void registerGestures() {
        OdinGestures.register();
    }

    @Override
    public void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        OdinActing.stanceTargets(s, p, data, on);
    }

    @Override
    public void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        OdinActing.edges(s, p, data);
    }

    @Override
    public float lean(HeroAnimState st) {
        return OdinActing.lean(st);
    }

    @Override
    public float drop(HeroAnimState st) {
        return OdinActing.drop(st);
    }

    @Override
    public void ambient(PoseContext c) {
        OdinActing.ambient(c);
    }

    @Override
    public void aimed(PoseContext c) {
        OdinActing.aimed(c);
    }

    @Override
    @Nullable
    public GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        // (The local player is read in OdinActing: this plug-in is loaded by the game tests on a server.)
        return OdinActing.predicted(data, ability, now, sneaking);
    }

    // ---- which gesture stands for what ----

    @Override
    @Nullable
    public GestureId cooldownGesture(int ability, long left) {
        return OdinHeroMath.cooldownGesture(ability, left);
    }

    @Override
    @Nullable
    public GestureId flagGesture(GestureRoutes.Flag flag, boolean on) {
        return flag == GestureRoutes.Flag.CHAOS_SHIELD ? (on ? GestureId.ODIN_SHIELD_UP : GestureId.ODIN_SHIELD_DOWN) : null;
    }

    @Override
    @Nullable
    public GestureId liveGesture(Live live) {
        return switch (live) {
            case ODIN_THROWN -> GestureId.ODIN_THROW;
            case ODIN_RAVENS -> GestureId.ODIN_RAVENS;
            default -> null;
        };
    }

    @Override
    public boolean startedElsewhere(GestureId id) {
        // The coronation is started by its payload; the catch also by his spear coming home (his edges); the Heal and
        // Banish by his own press too (their long cooldowns are past the routes' short samples).
        return id == GestureId.ODIN_CROWN || id == GestureId.ODIN_CATCH || id == GestureId.ODIN_HEAL || id == GestureId.ODIN_BANISH;
    }

    @Override
    public void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        OdinActing.gestureStarted(player, state, id, param);
    }

    // ---- first person ----

    @Override
    public void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
        OdinActing.firstPerson(s, main, off);
    }

    // ---- the client's life ----

    @Override
    public void tick(Minecraft mc) {
        OdinView.tick(mc);
        if (mc.level != null) {
            OdinFx.tick(mc.level);
        }
    }

    @Override
    public void clear() {
        OdinView.clear();
        OdinFx.clear();
        OdinActing.clear();
    }
}
