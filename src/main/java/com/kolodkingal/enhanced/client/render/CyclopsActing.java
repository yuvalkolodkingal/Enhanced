package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.CyclopsAbilities;
import com.kolodkingal.enhanced.ability.CyclopsGlasses;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * How Cyclops acts what he does (3.3): the rows of his two stances (the fingers at the visor while the Optic Blast is held,
 * both hands at his face while the giant beam pours out), what they and his gestures add to his hands in first person,
 * whether a key of the local hero will work (so his arms never reach for what the server refuses), and the small flourishes
 * at his eyes. The beams are drawn by {@link CyclopsBeamFx}, the blasts of the payloads by {@link CyclopsFx}.
 */
final class CyclopsActing {
    private CyclopsActing() {
    }

    // ---- the stances ----

    /** The aimed rows: the Optic Blast (a hand at the visor, the other braced) and the giant beam (both hands at his face). */
    static void aimed(PoseContext c) {
        HeroAnimState st = c.st;
        float w;
        if ((w = st.stance(Stance.CYCLOPS_BLAST)) > 0.001F) {
            c.arm(true, StancePoses.CYCLOPS_BLAST_M[0], StancePoses.CYCLOPS_BLAST_M[1], StancePoses.CYCLOPS_BLAST_M[2], w);
            c.arm(false, StancePoses.CYCLOPS_BLAST_O[0], StancePoses.CYCLOPS_BLAST_O[1], StancePoses.CYCLOPS_BLAST_O[2], w);
            c.tremble(true, 0.8F, 1.6F, w);
            float planted = w * (1.0F - st.wAir);
            c.leg(true, -8, 2, planted);
            c.leg(false, 8, -2, planted);
            c.occupy(true, w);
        }
        if ((w = st.stance(Stance.CYCLOPS_UNLEASHED)) > 0.001F) {
            c.arm(true, StancePoses.CYCLOPS_UNLEASHED_M[0], StancePoses.CYCLOPS_UNLEASHED_M[1], StancePoses.CYCLOPS_UNLEASHED_M[2], w);
            c.arm(false, StancePoses.CYCLOPS_UNLEASHED_O[0], StancePoses.CYCLOPS_UNLEASHED_O[1], StancePoses.CYCLOPS_UNLEASHED_O[2], w);
            // He strains against it: the arms shake and the feet are braced wide.
            c.tremble(true, 2.2F, 2.4F, w);
            c.tremble(false, 2.2F, 2.4F, w);
            float planted = w * (1.0F - st.wAir);
            c.leg(true, -12, 5, planted);
            c.leg(false, 12, -5, planted);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    /** The lean of his stances: back against the giant beam. */
    static float lean(HeroAnimState st) {
        return StancePoses.CYCLOPS_UNLEASHED_LEAN * st.stance(Stance.CYCLOPS_UNLEASHED);
    }

    // ---- first person ----

    /** What his stances and gestures add to his hands in first person. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        float age = (float) s.time;
        if ((w = s.stance(Stance.CYCLOPS_BLAST)) > 0.001F) {
            // The hand rises to the edge of the view, at the temple.
            m.add(w, 0.06F, 0.40F, 0.02F, -62, 14, 6);
        }
        if ((w = s.stance(Stance.CYCLOPS_UNLEASHED)) > 0.001F) {
            // Both hands at the edges of the view, shaking.
            float shake = 0.012F * (float) Math.sin(age * 2.4F);
            m.add(w, 0.08F, 0.42F + shake, 0.0F, -66, 16, 8);
            o.add(w, 0.08F, 0.42F - shake, 0.0F, -66, 16, 8);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w2 = slot.weight;
            switch (slot.id) {
                case CYCLOPS_RICOCHET, CYCLOPS_GLASSES_OFF, CYCLOPS_GLASSES_ON -> m.add(w2, 0.06F, 0.40F, 0.02F, -62, 14, 6);
                case CYCLOPS_CONCUSSION -> {
                    m.add(w2, 0.08F, 0.42F, 0.0F, -66, 16, 8);
                    o.add(w2, 0.08F, 0.42F, 0.0F, -66, 16, 8);
                }
                case CYCLOPS_RALLY -> m.add(w2, -0.04F, 0.50F, 0.10F, -80, 0, 0);
                default -> {
                }
            }
        }
    }

    // ---- what the local hero can do ----

    /**
     * Whether a key of the local hero will work, so far as the client can tell: the blasts of his eyes need the glasses on and
     * nothing pouring out, the Leap ground below him, the glasses' key glasses to put on or to take off. Kept here and not in
     * the plug-in: the plug-in is loaded by the game tests on a server, and it must not need the local player to be verified.
     */
    static boolean canCast(int ability, boolean sneaking) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null || self.isSpectator() || self.hasEffect(ModEffects.ENTOMBED)) {
            return false;
        }
        PowerData data = Powers.get(self);
        boolean aims = CyclopsGlasses.wears(self) && !data.is(Live.CYCLOPS_UNLEASHED);
        return switch (ability) {
            case 1, 2 -> aims;
            case 3 -> aims && hasGround(self);
            case 5 -> sneaking ? CyclopsGlasses.wears(self) : !CyclopsGlasses.wears(self) && self.getInventory().contains(
                    ModItems.RUBY_QUARTZ_GLASSES.get().getDefaultInstance());
            default -> true;
        };
    }

    private static boolean hasGround(LocalPlayer self) {
        Vec3 feet = self.position();
        return self.level().clip(new ClipContext(feet.add(0, 0.1, 0), feet.subtract(0, CyclopsAbilities.LEAP_GROUND_REACH, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, self)).getType() != HitResult.Type.MISS;
    }

    // ---- the flourishes ----

    /** A gesture of his began: a glint at the visor as the hand reaches it, a spark of red when the glasses come off. */
    static void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible()) {
            return;
        }
        RandomSource r = level.random;
        Vec3 eyes = CyclopsBeamFx.eyes(player, 1.0F);
        switch (id) {
            case CYCLOPS_GLASSES_OFF, CYCLOPS_RICOCHET, CYCLOPS_CONCUSSION -> {
                for (int i = 0; i < 6; i++) {
                    level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? CyclopsAbilities.RED : CyclopsAbilities.CORE, 0.6F),
                            eyes.x + (r.nextDouble() - 0.5) * 0.3, eyes.y + (r.nextDouble() - 0.5) * 0.12, eyes.z + (r.nextDouble() - 0.5) * 0.3,
                            (r.nextDouble() - 0.5) * 0.04, 0.01, (r.nextDouble() - 0.5) * 0.04);
                }
            }
            case CYCLOPS_GLASSES_ON -> level.addParticle(new EnergyParticleOptions(CyclopsAbilities.CORE, 0.5F), eyes.x, eyes.y, eyes.z, 0, 0.01, 0);
            default -> {
            }
        }
    }
}
