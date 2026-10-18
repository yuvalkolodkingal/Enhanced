package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.hela.HelaFx;
import com.kolodkingal.enhanced.hela.HelaRules;
import com.kolodkingal.enhanced.network.AsgardFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * How Hela the hero is acted (3.5): her two stances (the blades in her hands while her form is on, the hand held out in
 * Death's Grasp), her hands in first person, the flourishes of her gestures, and the gestures of the keys that the
 * payload of their effect tells ({@link #onFx}: the throw, the rain, the wall, the burst, the dash, Fenris), for everyone
 * who sees her. Her own key press may have started the same gesture a moment before ({@link HelaGestures#predicted}): the
 * payload then does not start it again.
 */
final class HelaActing {
    private static final float PREDICTION_WINDOW = 12.0F;
    private static final float DEDUPE = 4.0F;
    private static final net.minecraft.client.renderer.RenderType EYES =
            net.minecraft.client.renderer.RenderType.eyes(com.kolodkingal.enhanced.Enhanced.id("textures/entity/suit/hela_eyes.png"));

    /** The mask of her glowing eyes (and the line work of her suit). */
    static net.minecraft.client.renderer.RenderType eyes() {
        return EYES;
    }

    /** The gesture a payload of hers stands for when its doer is a player, or null. */
    @Nullable
    static GestureId gestureOf(int kind) {
        return switch (kind) {
            case HelaFx.THROW -> GestureId.HELA_THROW;
            case HelaFx.RAIN -> GestureId.HELA_RAIN;
            case HelaFx.WALL -> GestureId.HELA_WALL;
            case HelaFx.BURST -> GestureId.HELA_BURST;
            case HelaFx.DASH -> GestureId.HELA_DASH;
            case HelaFx.FENRIS -> null; // (the payload names the wolf, not her: see onFx)
            default -> null;
        };
    }

    /** A payload of hers arrived: if its doer is a player in view, he plays the gesture of it. */
    static void onFx(AsgardFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        GestureId id = gestureOf(payload.kind());
        AbstractClientPlayer player = null;
        if (id != null && level.getEntity(payload.entityId()) instanceof AbstractClientPlayer p) {
            player = p;
        } else if (payload.kind() == HelaFx.FENRIS && level.getEntity(payload.entityId()) instanceof com.kolodkingal.enhanced.hela.ArmyMob wolf
                && wolf.isAlly() && !payload.points().isEmpty()) {
            // The wolf rises behind his hero: the nearest player who is Hela points at him.
            Vec3 at = payload.points().get(0);
            double best = 36.0;
            for (AbstractClientPlayer p : level.players()) {
                double d = p.position().distanceToSqr(at);
                if (d < best && com.kolodkingal.enhanced.power.Powers.get(p).acts(com.kolodkingal.enhanced.power.Power.HELA)) {
                    best = d;
                    player = p;
                }
            }
            id = GestureId.HELA_FENRIS;
        }
        if (player == null || id == null) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(player);
        if (s == null) {
            return;
        }
        float age = player.tickCount;
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.id != id) {
                continue;
            }
            float since = age - slot.start;
            if (slot.predicted && since <= PREDICTION_WINDOW) {
                slot.predicted = false;
                return;
            }
            if (since < DEDUPE) {
                return;
            }
        }
        HeroAnimator.start(s, id, 0.0F, age);
        started(player, id);
    }

    // ---- the stances ----

    /** The blades held low (ambient: under the aimed stances and the gestures). */
    static void ambient(PoseContext c) {
        float w = c.st.stance(Stance.HELA_BLADES);
        if (w > 0.001F) {
            c.arm(true, HelaGestures.BLADES_M[0], HelaGestures.BLADES_M[1], HelaGestures.BLADES_M[2], w * 0.8F);
            c.arm(false, HelaGestures.BLADES_O[0], HelaGestures.BLADES_O[1], HelaGestures.BLADES_O[2], w * 0.8F);
        }
    }

    /** The grasp: the hand held out, clawed and trembling a little; the other drawn back. */
    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.HELA_GRASP);
        if (w > 0.001F) {
            c.aim(true, Gestures.Target.LOOK, -4, -4, 0, w);
            c.arm(false, HelaGestures.GRASP_O[0], HelaGestures.GRASP_O[1], HelaGestures.GRASP_O[2], w);
            c.tremble(true, 0.9F, 0.8F, w);
            c.occupy(true, w);
        }
    }

    // ---- first person ----

    /** Her hands at the edges of the screen: the blades held low and out; the grasp thrust forward; her gestures. */
    static void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float grasp = s.stance(Stance.HELA_GRASP);
        if (grasp > 0.001F) {
            m.add(grasp, -0.10F, 0.18F, -0.12F, -30, 8, 0);
            FirstPersonArms.tremble(m, null, grasp, 0.004F, 0.8F, (float) s.time, EnhancedClientConfig.reducedMotion());
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w = slot.weight;
            float t = slot.t;
            float k = (float) Math.sin(Math.PI * Math.min(1.0F, t));
            switch (slot.id) {
                case HELA_THROW -> {
                    m.add(w * k, 0.08F, 0.25F, 0.05F, -50, 10, 0);
                    o.add(w * k, 0.08F, 0.25F, 0.05F, -50, -10, 0);
                }
                case HELA_RAIN -> m.add(w * k, 0.0F, 0.45F, 0.1F, -70, 0, 0);
                case HELA_WALL, HELA_BURST -> m.add(w * k, 0.05F, -0.25F, -0.1F, 30, 0, 0);
                case HELA_RAISE, HELA_FORM_ON -> {
                    m.add(w * k, 0.0F, 0.35F, 0.05F, -40, 0, 0);
                    o.add(w * k, 0.0F, 0.35F, 0.05F, -40, 0, 0);
                }
                case HELA_FENRIS -> m.add(w * k, -0.05F, 0.12F, -0.15F, -20, 0, 0);
                default -> {
                }
            }
        }
    }

    /** The flourish of a gesture: green motes and black smoke at her hands. */
    static void started(AbstractClientPlayer player, GestureId id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int n = switch (id) {
            case HELA_FORM_ON, HELA_RAISE -> 14;
            case HELA_THROW, HELA_BURST, HELA_FENRIS -> 8;
            case HELA_GRASP -> 5;
            default -> 0;
        };
        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).subtract(0.0, 0.4, 0.0);
        for (int i = 0; i < n; i++) {
            level.addParticle(new EnergyParticleOptions(HelaRules.GREEN, 0.6F), hand.x + level.random.nextGaussian() * 0.25,
                    hand.y + level.random.nextGaussian() * 0.25, hand.z + level.random.nextGaussian() * 0.25, 0.0, 0.02, 0.0);
        }
    }

    private HelaActing() {
    }
}
