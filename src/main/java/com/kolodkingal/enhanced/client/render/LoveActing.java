package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.love.LoveAbilities;
import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * How Love acts what she does (3.8, for {@link LoveClient}; a class of the client's own, never loaded by the game tests'
 * server): the catch when her axe comes home, the strike's gesture when its payload names her, and the holds of her two
 * beams, eased frame by frame (the arm along the look for the lightning; the chin forward and the fists down for the
 * eyes).
 */
public final class LoveActing {
    /** Each Love's axe in flight at her last tick (entity id, -1 none): its end is the catch. */
    private static final Map<Integer, Integer> AXE = new HashMap<>();
    /** The weights of the two holds (lightning, eyes) per player, and the age they were last eased at. */
    private static final Map<Integer, float[]> HOLDS = new HashMap<>();

    /** The axe came home (her thrown axe is gone and she holds it): the catch. */
    static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data) {
        int flying = data.hammerEntityId();
        Integer before = AXE.put(p.getId(), flying);
        if (before != null && before != -1 && flying == -1 && LoveAbilities.axeHand(p) != null) {
            HeroAnimator.start(s, GestureId.LOVE_CATCH, 0.0F, p.tickCount);
        }
    }

    /** The holds of her beams, over the gestures that start them. */
    static void aimed(PoseContext c) {
        PowerData data = Powers.get(c.player);
        boolean lightning = data.has(HeroAbility.LOVE_LIGHTNING) && data.is(Live.CALLING_LIGHTNING);
        boolean eyes = data.has(HeroAbility.LOVE_EYE_LASER) && data.is(Live.BEAMING);
        float[] w = HOLDS.computeIfAbsent(c.player.getId(), id -> new float[]{0.0F, 0.0F, c.age});
        float dt = Mth.clamp(c.age - w[2], 0.0F, 2.0F);
        w[2] = c.age;
        w[0] = ease(w[0], lightning ? 1.0F : 0.0F, dt, 2.0F);
        w[1] = ease(w[1], eyes ? 1.0F : 0.0F, dt, 2.5F);
        if (w[0] > 0.001F) {
            c.aim(true, Gestures.Target.LOOK, 0, 0, 0, w[0]);
            c.occupy(true, w[0]);
            c.twist(12.0F, w[0]);
        }
        if (w[1] > 0.001F) {
            c.arm(true, LoveGestures.EYES_M[0], LoveGestures.EYES_M[1], LoveGestures.EYES_M[2], w[1] * (1.0F - w[0]));
            c.arm(false, LoveGestures.EYES_O[0], LoveGestures.EYES_O[1], LoveGestures.EYES_O[2], w[1]);
            c.head(LoveGestures.EYES_HEAD, 0, 0, w[1]);
            c.tremble(false, 0.8F, 1.6F, w[1]);
            c.occupy(false, w[1]);
        }
    }

    private static float ease(float value, float target, float dt, float tau) {
        return value + (target - value) * (1.0F - (float) Math.exp(-dt / tau));
    }

    /** The Thunder Strike's payload names her: the axe raised to the sky. */
    public static void strike(EternityFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(payload.entityId());
        if (entity instanceof AbstractClientPlayer player) {
            HeroAnimState s = HeroAnimator.peek(player);
            if (s != null) {
                HeroAnimator.start(s, GestureId.LOVE_STRIKE, 0.0F, player.tickCount);
            }
        }
    }

    static void clear() {
        AXE.clear();
        HOLDS.clear();
    }

    private LoveActing() {
    }
}
