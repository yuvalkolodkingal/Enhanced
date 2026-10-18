package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.AgathaAbilities;
import com.kolodkingal.enhanced.ability.AgathaShield;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

import static com.kolodkingal.enhanced.client.render.AnimMath.lerp;
import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * How Agatha acts what she does, besides the keys of {@link AgathaGestures}: the row of her shield's stance, what her stance
 * and gestures add to her hands in first person, and the violet motes that go with each gesture (off the fingers of the
 * hex, gathering into the hands of the shield, a puff off the palms that drive the runes down). The big pictures of her
 * abilities (the runes, the stream of the drain, the shards) are {@link AgathaFx}, drawn from the server's payloads. Client
 * only: {@link AgathaClient} hands its arguments on to it.
 */
final class AgathaActing {
    private AgathaActing() {
    }

    /** Whether the local Agatha is fed (the server refuses the shield to an empty stomach, so her arms do not reach for it). */
    static boolean fed() {
        LocalPlayer me = Minecraft.getInstance().player;
        return me != null && !me.isSpectator() && AgathaShield.canAfford(me);
    }

    // ---- the stance ----

    /** The shield: both hands held out before her at the height of the chest, a little apart, the fingers spread. */
    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.AGATHA_SHIELD);
        if (w > 0.001F) {
            c.arm(true, StancePoses.AGATHA_SHIELD_M[0], StancePoses.AGATHA_SHIELD_M[1], StancePoses.AGATHA_SHIELD_M[2], w);
            c.arm(false, StancePoses.AGATHA_SHIELD_O[0], StancePoses.AGATHA_SHIELD_O[1], StancePoses.AGATHA_SHIELD_O[2], w);
            c.head(StancePoses.AGATHA_SHIELD_HEAD, 0, 0, w);
            c.tremble(true, 0.8F, 0.6F, w);
            c.tremble(false, 0.8F, 0.6F, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    // ---- first person ----

    /** Her hands at the edges of the screen: held out for the shield; thrust out and dragged back for the drain; the runes. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w = s.stance(Stance.AGATHA_SHIELD);
        if (w > 0.001F) {
            both(m, o, w, -0.14F, 0.26F, 0.16F, -40, 10, 0);
            FirstPersonArms.tremble(m, o, w, 0.004F, 0.6F, (float) s.time, EnhancedClientConfig.reducedMotion());
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float gw = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case AGATHA_DRAIN -> {
                    // Both hands thrust ahead, then dragged back towards her and down.
                    float u = ss(0.0F, 4.0F, t);
                    float back = ss(10.0F, 16.0F, t);
                    both(m, o, gw, lerp(back, lerp(u, -0.10F, -0.16F), -0.04F), lerp(back, lerp(u, 0.08F, 0.20F), -0.02F),
                            lerp(back, lerp(u, 0.0F, 0.34F), -0.10F), lerp(back, lerp(u, -10, -50), -14), lerp(back, 6, 24), 0);
                    FirstPersonArms.tremble(m, o, gw * (1.0F - back) * u, 0.010F, 2.2F, (float) s.time, EnhancedClientConfig.reducedMotion());
                }
                case AGATHA_HEX -> m.add(gw * ss(0.0F, 4.0F, t), -0.10F, 0.22F, 0.24F, -48, 6, 0);
                case AGATHA_BEAM -> m.add(gw * (1.0F - ss(4.0F, 9.0F, t)), -0.06F, 0.18F, -0.06F, -30, 10, 0);
                case AGATHA_RUNES -> {
                    // The hand circles before her, then both palms are driven down.
                    float u = ss(0.0F, 4.0F, t);
                    float down = ss(12.0F, 15.0F, t);
                    double turn = Math.max(0.0F, Math.min(1.0F, (t - 4.0F) / 8.0F)) * Math.PI * 2.0;
                    m.add(gw * u * (1.0F - down), -0.08F + 0.06F * (float) Math.cos(turn), 0.24F + 0.06F * (float) Math.sin(turn), 0.20F, -46, 4, 0);
                    both(m, o, gw * down, -0.10F, -0.04F, 0.24F, 20, 8, 0);
                }
                default -> {
                }
            }
        }
    }

    private static void both(FirstPersonArms.Offset m, FirstPersonArms.Offset o, float w, float dx, float dy, float dz, float rx, float ry, float rz) {
        m.add(w, dx, dy, dz, rx, ry, rz);
        o.add(w, dx, dy, dz, rx, ry, rz);
    }

    // ---- the edges ----

    /** The take-off (the animator's own condition): a ring of violet motes round her feet as her magic lifts her. */
    static void edges(HeroAnimState s, AbstractClientPlayer p) {
        if (!s.prevFlyingEff && s.flyingEff && (s.onGround || s.airTicks <= 8)) {
            AgathaFlight.takeoff(p);
        }
    }

    // ---- the flourishes ----

    /** A gesture of hers just began: the motes that go with it. */
    static void started(AbstractClientPlayer player, GestureId id) {
        switch (id) {
            case AGATHA_HEX -> motes(player, HumanoidArm.RIGHT, 8, 0.6F, 0.04);
            case AGATHA_DRAIN -> {
                motes(player, HumanoidArm.RIGHT, 6, 0.7F, 0.02);
                motes(player, HumanoidArm.LEFT, 6, 0.7F, 0.02);
            }
            case AGATHA_SHIELD_RAISE -> {
                motes(player, HumanoidArm.RIGHT, 5, 0.6F, 0.01);
                motes(player, HumanoidArm.LEFT, 5, 0.6F, 0.01);
            }
            case AGATHA_RUNES -> motes(player, HumanoidArm.RIGHT, 10, 0.7F, 0.03);
            default -> {
            }
        }
    }

    /** A few violet motes off a hand (smaller in the local player's own first-person view). */
    private static void motes(AbstractClientPlayer player, HumanoidArm side, int count, float size, double speed) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        HumanoidArm arm = side == HumanoidArm.RIGHT ? player.getMainArm() : player.getMainArm().getOpposite();
        Vec3 hand = HandTracker.hand(player, arm, 1.0F);
        if (hand == null) {
            hand = WorldEffectsRenderer.handPosition(mc, player, 1.0F);
        }
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        float scale = firstPerson ? size * 0.45F : size;
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            int colour = i % 3 == 0 ? AgathaAbilities.LILAC : AgathaAbilities.VIOLET;
            level.addParticle(new EnergyParticleOptions(colour, scale), hand.x + (random.nextDouble() - 0.5) * 0.2, hand.y + (random.nextDouble() - 0.5) * 0.2,
                    hand.z + (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * speed, 0.01 + random.nextDouble() * speed,
                    (random.nextDouble() - 0.5) * speed);
        }
    }

    static void clear() {
    }
}
