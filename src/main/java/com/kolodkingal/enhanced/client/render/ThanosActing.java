package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.ThanosAbilities;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.thanos.StonePowers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * What the client does to act the playing Thanos besides the poses of {@link ThanosPoses} and the keys of
 * {@link ThanosGestures}: the gesture his own key press starts before the server has answered, the landing of a Titan
 * (the animator only knows the landings of fliers), and the movement of the Gauntlet hand in first person. Client only:
 * {@link ThanosClient} hands its arguments on to it, so the plug-in itself names no client class.
 */
final class ThanosActing {
    private ThanosActing() {
    }

    // ---- the key press ----

    /**
     * The gesture the local Thanos's key press starts at once (see {@link ThanosGestures#predicted}), with the guards
     * only a client can see: he is not held by a spell, stone or the Soul World, not starving and not a spectator; a
     * living creature stands where he looks, within the range of the stone he is using; something to step to is in
     * range of the Portal Step; and the Rewind has two seconds of history.
     */
    @Nullable
    static GestureId predicted(PowerData data, int ability, long now, boolean sneaking) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null || me.isSpectator()) {
            return null;
        }
        boolean able = !ThanosAbilities.held(me) && (me.getAbilities().invulnerable || me.getFoodData().getFoodLevel() > 0);
        if (!able) {
            return null;
        }
        boolean creature = false;
        boolean anything = false;
        switch (ability) {
            case 2 -> {
                double range = sneaking ? ThanosConfig.PULL_RANGE.get() : ThanosConfig.SPACE_STEP_RANGE.get();
                creature = creatureAhead(me, range);
                anything = creature || !sneaking && Raycasts.block(me, range, 1.0F).getType() != HitResult.Type.MISS;
            }
            case 3 -> creature = !sneaking && creatureAhead(me, ThanosConfig.TRANSMUTE_RANGE.get());
            case 4 -> creature = !sneaking && creatureAhead(me, ThanosConfig.DRAIN_RANGE.get());
            default -> {
            }
        }
        return ThanosGestures.predicted(data, ability, now, sneaking, able, creature, anything, ThanosHistory.enough());
    }

    /** Whether a creature the stones may touch stands in his sight within {@code range}, in front of the first block. */
    private static boolean creatureAhead(LocalPlayer me, double range) {
        return Raycasts.entity(me, range, 0.3, e -> e instanceof LivingEntity living && living != me && living.isAlive() && !living.isSpectator()
                && !(living instanceof ArmorStand) && !StonePowers.untouchable(living), 1.0F) != null;
    }

    // ---- the edges ----

    /**
     * A Titan's landing: the animator only lands the heroes who fly, so a fall of more than three blocks is found here
     * (by how fast he was falling just before the ground) and played as the soft landing, with a deeper dip than
     * anyone else's and the dust of the ground. A short fall is nothing, as for everyone.
     */
    static void edges(HeroAnimState s, AbstractClientPlayer p) {
        if (!s.wasOnGround && s.onGround && s.prevAirTicks >= 8) {
            float fall = -Math.min(s.dy[0], Math.min(s.dy[1], s.dy[2]));
            if (fall >= ThanosGestures.HARD_LANDING) {
                HeroAnimator.start(s, GestureId.LAND_SOFT, 0.0F, p.tickCount);
                s.drop.v += 1.4F * 0.084F * AnimMath.clamp((fall - 0.15F) / 0.45F, 0.0F, 1.5F);
                FlightEffects.softLanding(p, s, fall);
            }
        }
    }

    // ---- first person ----

    /**
     * The Gauntlet hand at the lower left of the screen, always (the off hand is drawn even when it holds nothing),
     * breathing; and what each stance and gesture does to it. Every row is a movement added to the rest, in a right
     * hand's signs, and the hand stays at the edge of the screen but for the thrust of the Blast and the stamp of the
     * Time Lock, which are over in a few ticks.
     */
    static void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float age = (float) s.time;
        boolean calm = EnhancedClientConfig.reducedMotion();
        if (player.getOffhandItem().isEmpty() && !player.isInvisible()) {
            o.add(1.0F, 0.10F, 0.08F, 0.15F, -8.0F, 0.0F, 20.0F);
            o.ambient(1.0F, 0.0F, 0.005F * Mth.sin(age * 0.08F), 0.0F);
        }
        float w;
        if ((w = s.stance(Stance.THANOS_VEIL)) > 0.001F) {
            // The hand a little forward, as if it held a thin film.
            o.add(w, 0.0F, 0.05F, 0.08F, -6, 0, 4);
            FirstPersonArms.tremble(o, null, w, 0.004F, 1.6F, age, calm);
        }
        if ((w = s.stance(Stance.THANOS_DRAIN)) > 0.001F) {
            // Held out along the look, trembling with the chain.
            o.add(w, 0.10F, 0.12F, 0.16F, -10, 0, 0);
            FirstPersonArms.tremble(o, null, w, 0.010F, 1.9F, age, calm);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (slot.active && slot.weight > 0.001F) {
                gesture(slot.id, slot.weight, slot.t, o, age, calm);
            }
        }
    }

    /** One gesture's rows: the Gauntlet hand is {@code o} (the two-handed ones move both through {@link FirstPersonArms#both}). */
    private static void gesture(GestureId id, float w, float t, FirstPersonArms.Offset o, float age, boolean calm) {
        switch (id) {
            case THANOS_BLAST -> {
                // A thrust over two ticks that holds for four, and a kick back.
                float thrust = ss(0.0F, 1.5F, t) * (1.0F - ss(4.0F, 9.0F, t));
                float kick = Mth.sin(Mth.PI * Mth.clamp((t - 4.0F) / 5.0F, 0.0F, 1.0F));
                o.add(w, 0.0F, 0.06F * thrust, 0.22F * thrust - 0.05F * kick, -14.0F * thrust, 0, 0);
            }
            case THANOS_SLAM -> {
                // Up for six ticks, then down hard.
                float up = ss(0.0F, 6.0F, t) * (1.0F - ss(10.0F, 12.0F, t));
                float down = ss(10.0F, 13.0F, t) * (1.0F - ss(14.0F, 22.0F, t));
                o.add(w, 0.0F, 0.30F * up - 0.25F * down, 0.05F * up + 0.10F * down, 25.0F * down, 0, 0);
            }
            case THANOS_STEP -> {
                // A circle over the twelve ticks of the gesture, through the rest.
                float a = Mth.TWO_PI * t / 12.0F;
                o.add(w, 0.08F * Mth.sin(a), 0.06F * (1.0F - Mth.cos(a)), 0.0F, 0, 0, 0);
            }
            case THANOS_PULL -> {
                // Out, then drawn back.
                float out = ss(0.0F, 5.0F, t) * (1.0F - ss(8.0F, 11.0F, t));
                float back = ss(8.0F, 12.0F, t) * (1.0F - ss(13.0F, 16.0F, t));
                o.add(w, 0.0F, 0.04F * out - 0.02F * back, 0.20F * out - 0.06F * back, 0, 0, 0);
            }
            case THANOS_WARP -> {
                // A flick forward.
                float flick = ss(0.0F, 2.0F, t) * (1.0F - ss(3.0F, 8.0F, t));
                o.add(w, 0.0F, 0.04F * flick, 0.18F * flick, -10.0F * flick, 0, 0);
            }
            case THANOS_VEIL_ON -> {
                // Both hands come up a little and gather in.
                float gather = ss(0.0F, 5.0F, t);
                FirstPersonArms.both(w * gather, -0.06F, 0.08F, 0.06F, -12, 4, 0);
            }
            case THANOS_VEIL_OFF -> {
                float open = ss(0.0F, 4.0F, t) * (1.0F - ss(5.0F, 10.0F, t));
                FirstPersonArms.both(w * open, 0.06F, 0.05F, 0.04F, -8, -4, 0);
            }
            case THANOS_DRAIN_CAST -> {
                // The chain lashes out: the hand thrown forward, then held (the stance has it).
                float lash = ss(0.0F, 2.0F, t) * (1.0F - ss(8.0F, 12.0F, t));
                o.add(w, 0.04F * lash, 0.06F * lash, 0.20F * lash, -12.0F * lash, 0, 0);
            }
            case THANOS_SIGHT -> {
                // The hand rises at the edge of the screen, toward the eye.
                float up = ss(0.0F, 4.0F, t) * (1.0F - ss(9.0F, 14.0F, t));
                o.add(w, 0.04F * up, 0.14F * up, 0.04F * up, -18.0F * up, 0, 10.0F * up);
            }
            case THANOS_REWIND -> {
                // The palm up and trembling; drawn back at the end.
                float up = ss(0.0F, 4.0F, t) * (1.0F - ss(12.0F, 16.0F, t));
                float back = ss(12.0F, 17.0F, t) * (1.0F - ss(18.0F, 22.0F, t));
                o.add(w, 0.06F * up, 0.10F * up, 0.10F * up - 0.06F * back, 0, 0, 25.0F * up);
                FirstPersonArms.tremble(o, null, w * up, 0.008F, 1.8F, age, calm);
            }
            case THANOS_TIMELOCK -> {
                // Both hands pressed down, a stamp.
                float press = ss(4.0F, 10.0F, t) * (1.0F - ss(10.0F, 20.0F, t));
                FirstPersonArms.both(w, 0.0F, -0.10F * press, 0.10F * press, 0, 0, 0);
            }
            default -> {
            }
        }
    }
}
