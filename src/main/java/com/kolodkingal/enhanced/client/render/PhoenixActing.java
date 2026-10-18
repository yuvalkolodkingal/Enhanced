package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.lerp;
import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * What the client does to act the Phoenix besides the poses of {@link PhoenixPoses} and the keys of
 * {@link PhoenixGestures}: the gesture her own key press starts before the server has answered, the edges the shared
 * animator cannot know are hers (the charge lifts her, it is not a take-off), the movement of her hands in first person
 * and the glints that go with her gestures. Client only: {@link PhoenixClient} hands its arguments on to it.
 */
final class PhoenixActing {
    private PhoenixActing() {
    }

    // ---- the key press ----

    /**
     * The gesture the local Phoenix's key press starts at once (see {@link PhoenixGestures#predicted}), with the two
     * guards only a client can see: she is hungry enough for the Burst (the server turns a hungry Phoenix away), and a
     * living creature stands in her sight for Mind Control (the server answers a cast at nothing with a message and no
     * cooldown, and a hand that reaches out for nothing would be a lie).
     */
    @Nullable
    static GestureId predicted(PowerData data, int ability, long now, boolean sneaking) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null || me.isSpectator()) {
            return null;
        }
        boolean fed = me.getAbilities().invulnerable || me.getFoodData().getFoodLevel() >= PhoenixConfig.burstFoodCost();
        boolean mind = ability == 4 && !sneaking && mindInSight(me);
        return PhoenixGestures.predicted(data, ability, now, sneaking, fed, mind);
    }

    /** Whether Mind Control would find a creature to reach: the same look, range and filter as the server's. */
    private static boolean mindInSight(LocalPlayer me) {
        EntityHitResult hit = Raycasts.entity(me, PhoenixConfig.mindControlRange(), 0.3,
                e -> e instanceof LivingEntity living && living != me && living.isAlive() && !living.isSpectator() && !(living instanceof ArmorStand),
                1.0F);
        return hit != null;
    }

    // ---- the edges ----

    /**
     * The charge lifts her off the ground, which the animator takes for a take-off: she gathers the Burst instead of
     * unfurling her wings, so the take-off gesture is let go of before it can show.
     */
    static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data) {
        if (data.is(Live.PHOENIX_CHARGING)) {
            HeroAnimator.release(s, GestureId.TAKEOFF_PHOENIX, p.tickCount);
        }
    }

    // ---- first person ----

    /**
     * Her hands at the edges of the screen: the V of the charge (up and in, trembling), a hand up near the temple for
     * the gestures of the mind, both hands lifted and then pushed out for the Psionic Field, and thrown wide for the
     * Burst. Where a gesture carries the V itself the stance's row gives way by its weight, as the stance's head does.
     */
    static void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float age = (float) s.time;
        boolean calm = EnhancedClientConfig.reducedMotion();
        float w = s.stance(Stance.PHOENIX_CHARGE) * (1.0F - PhoenixGestures.owningTheV(s));
        if (w > 0.001F) {
            FirstPersonArms.both(w, -0.14F, 0.34F, 0.14F, -46, 10, 0);
            FirstPersonArms.tremble(m, o, w, 0.012F, 1.9F, age, calm);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float gw = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case PHOENIX_READ_ON, PHOENIX_MIND_CAST ->
                        // A hand up near the temple.
                        m.add(gw, -0.12F, 0.30F, 0.10F, -52, 8, 0);
                case PHOENIX_SENSE -> {
                    // Both hands up near the temples, then out like a ripple.
                    float u = ss(0.0F, 5.0F, t);
                    float v = ss(12.0F, 20.0F, t);
                    FirstPersonArms.both(gw, lerp(v, -0.12F * u, -0.20F), lerp(v, 0.30F * u, 0.14F), lerp(v, 0.10F * u, 0.20F),
                            lerp(v, -50 * u, -20), lerp(v, 8 * u, 20), 0);
                }
                case PHOENIX_PSIONIC_CAST -> {
                    // Both hands lifted, then pushed out.
                    float u = ss(0.0F, 6.0F, t);
                    float p = ss(10.0F, 14.0F, t);
                    FirstPersonArms.both(gw, lerp(p, lerp(u, -0.10F, -0.14F), -0.24F), lerp(p, lerp(u, 0.10F, 0.34F), 0.14F),
                            lerp(p, lerp(u, 0.0F, 0.30F), 0.38F), lerp(p, lerp(u, -20, -50), -20), lerp(p, 6, 18), 0);
                }
                case PHOENIX_BURST -> {
                    // From the V of the charge, thrown wide.
                    float u = ss(0.0F, 2.0F, t);
                    FirstPersonArms.both(gw, lerp(u, -0.14F, -0.30F), lerp(u, 0.34F, 0.10F), lerp(u, 0.14F, 0.40F), lerp(u, -46, -20),
                            lerp(u, 10, 30), 0);
                }
                default -> {
                }
            }
        }
    }

    // ---- the glints of the mind ----

    /** A gesture of hers just began: the mind lights up at her forehead. */
    static void started(AbstractClientPlayer player, GestureId id) {
        switch (id) {
            case PHOENIX_READ_ON -> glint(player, 6, HeroAbility.PHOENIX_MIND);
            case PHOENIX_MIND_CAST -> glint(player, 6, HeroAbility.PHOENIX_CONTROL);
            case PHOENIX_SENSE -> glint(player, 10, HeroAbility.PHOENIX_MIND_SENSE);
            default -> {
            }
        }
    }

    /** A few violet motes drifting up off her forehead (small in the local player's own first-person view). */
    private static void glint(AbstractClientPlayer player, int count, HeroAbility row) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Vec3 head = WorldEffectsRenderer.foreheadPosition(mc, player, 0.0F);
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        float size = firstPerson ? 0.4F : 0.9F;
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            int colour = AbilityColors.shade(player, row, i % 3 == 0 ? PhoenixAbilities.PSY_CORE
                    : PhoenixAbilities.PSY_MAIN);
            level.addParticle(new EnergyParticleOptions(colour, size), head.x + (random.nextDouble() - 0.5) * 0.3, head.y + (random.nextDouble() - 0.5) * 0.2,
                    head.z + (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.03, 0.02 + random.nextDouble() * 0.03,
                    (random.nextDouble() - 0.5) * 0.03);
        }
    }
}
