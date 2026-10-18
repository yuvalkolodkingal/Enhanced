package com.kolodkingal.enhanced.client.asgard;

import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.NpcState;
import com.kolodkingal.enhanced.asgard.npc.Role;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;

/**
 * The poses of the Asgardians (3.5), from their synced state: talk gestures, sparring guard, eating, bowing, sitting,
 * kneeling when down, Odin asleep, Heimdall's watch with both hands on Hofund's pommel, the guards' stance, Odin's regal
 * hold of Gungnir. A state's pose eases in over {@link #EASE} ticks from the walking pose, and the state before it eases
 * out, so nothing snaps; everything is interpolated with the partial tick ({@code stateAge}).
 */
public final class AsgardianPoses {
    /** Ticks a pose takes to ease in (and the previous one to ease out). */
    public static final float EASE = 6.0F;

    private AsgardianPoses() {
    }

    /** How far {@code t} (0-1) has eased: smooth at both ends. */
    static float ease(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** How much of the current state's pose shows ({@code 0} just after it changed, {@code 1} after {@link #EASE}). */
    public static float weight(AsgardianEntity npc, float partialTick) {
        return ease(npc.stateAge(partialTick) / EASE);
    }

    /** Whether a state's pose sits the body lower (sitting, kneeling), and by how much (blocks, before the body's scale). */
    static float lowerOf(NpcState state) {
        return switch (state) {
            case SITTING -> 0.62F;
            case DOWN -> 0.3F;
            default -> 0.0F;
        };
    }

    /** How far the body is lowered now, eased between the previous state and this one. */
    public static float lower(AsgardianEntity npc, float partialTick) {
        float w = weight(npc, partialTick);
        return Mth.lerp(w, lowerOf(npc.previousState()), lowerOf(npc.state()));
    }

    /** The vanilla parts the poses change, before {@code setupAnim} runs: sitting is the riding pose, bowing a crouch. */
    static void before(PlayerModel<AsgardianEntity> model, AsgardianEntity npc, float partialTick) {
        NpcState state = npc.state();
        model.riding = state == NpcState.SITTING;
        model.crouching = state == NpcState.BOWING && weight(npc, partialTick) > 0.5F;
    }

    /**
     * After the vanilla {@code setupAnim}: the state's pose over it, blended with the previous state's, then the outer
     * layer (hat, jacket, sleeves, trousers) copied again from the parts.
     */
    static void after(PlayerModel<AsgardianEntity> model, AsgardianEntity npc, float ageInTicks, float partialTick) {
        ModelPart[] parts = {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        PartPose[] walk = new PartPose[parts.length];
        for (int i = 0; i < parts.length; i++) {
            walk[i] = parts[i].storePose();
        }
        float w = weight(npc, partialTick);
        PartPose[] previous = null;
        if (w < 1.0F && npc.previousState() != npc.state()) {
            pose(model, npc, npc.previousState(), ageInTicks, partialTick);
            previous = new PartPose[parts.length];
            for (int i = 0; i < parts.length; i++) {
                previous[i] = parts[i].storePose();
                parts[i].loadPose(walk[i]);
            }
        }
        pose(model, npc, npc.state(), ageInTicks, partialTick);
        if (w < 1.0F) {
            for (int i = 0; i < parts.length; i++) {
                PartPose to = parts[i].storePose();
                PartPose from = previous != null ? previous[i] : walk[i];
                parts[i].loadPose(lerp(from, to, w));
            }
        }
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    static PartPose lerp(PartPose a, PartPose b, float t) {
        return PartPose.offsetAndRotation(Mth.lerp(t, a.x, b.x), Mth.lerp(t, a.y, b.y), Mth.lerp(t, a.z, b.z),
                Mth.rotLerp(t, a.xRot * Mth.RAD_TO_DEG, b.xRot * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD,
                Mth.rotLerp(t, a.yRot * Mth.RAD_TO_DEG, b.yRot * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD,
                Mth.rotLerp(t, a.zRot * Mth.RAD_TO_DEG, b.zRot * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD);
    }

    /** The pose of one state over the walking pose the parts hold now. */
    static void pose(PlayerModel<AsgardianEntity> model, AsgardianEntity npc, NpcState state, float ageInTicks, float partialTick) {
        float age = npc.stateAge(partialTick);
        boolean moving = npc.walkAnimation.speed(partialTick) > 0.05F;
        switch (state) {
            case IDLE -> {
                if (!moving) {
                    idle(model, npc, ageInTicks);
                }
            }
            case TALKING -> talk(model, npc, age);
            case SPARRING -> spar(model, age);
            case SITTING -> {
                model.rightArm.xRot = -0.55F + Mth.sin(ageInTicks * 0.05F) * 0.03F;
                model.leftArm.xRot = -0.55F;
                model.rightArm.zRot = 0.1F;
                model.leftArm.zRot = -0.1F;
                if (npc.role() == Role.FRIGGA || npc.role().slim(npc.variant())) {
                    // Hands folded in the lap.
                    model.rightArm.yRot = -0.35F;
                    model.leftArm.yRot = 0.35F;
                }
            }
            case FEASTING -> feast(model, ageInTicks);
            case SLEEPING -> {
                model.head.xRot = 0.0F;
                model.head.yRot = 0.0F;
                model.rightArm.xRot = -0.35F;
                model.leftArm.xRot = -0.35F;
                model.rightArm.yRot = -0.55F;
                model.leftArm.yRot = 0.55F;
                model.rightArm.zRot = 0.0F;
                model.leftArm.zRot = 0.0F;
                model.rightLeg.xRot = 0.0F;
                model.leftLeg.xRot = 0.0F;
            }
            case BOWING -> {
                model.head.xRot = 0.55F;
                model.head.yRot *= 0.2F;
                model.rightArm.xRot = -1.1F;
                model.rightArm.yRot = -0.55F;
                model.leftArm.xRot = 0.35F;
                model.leftArm.zRot = -0.12F;
            }
            case DOWN -> down(model, ageInTicks);
            case FIGHTING -> {
                // A warrior's guard: the weapon arm ready, the shield arm before him.
                model.rightArm.xRot = model.rightArm.xRot * 0.5F - 0.35F;
                model.leftArm.xRot = model.leftArm.xRot * 0.5F - 0.5F;
                model.leftArm.yRot = 0.3F;
            }
        }
    }

    /** Standing: Heimdall's watch, the guards' stance, Odin's regal hold, the others' breathing. */
    private static void idle(PlayerModel<AsgardianEntity> model, AsgardianEntity npc, float ageInTicks) {
        float breath = Mth.sin(ageInTicks * 0.06F) * 0.02F;
        switch (npc.role()) {
            case HEIMDALL -> {
                // Both hands on Hofund's pommel, the blade planted before him.
                model.rightArm.xRot = -0.62F + breath;
                model.leftArm.xRot = -0.62F + breath;
                model.rightArm.yRot = -0.42F;
                model.leftArm.yRot = 0.42F;
                model.rightArm.zRot = 0.0F;
                model.leftArm.zRot = 0.0F;
            }
            case ODIN -> {
                // Gungnir held upright beside him, the other hand at rest.
                model.rightArm.xRot = -0.28F + breath;
                model.rightArm.zRot = 0.05F;
                model.leftArm.xRot = 0.05F;
                model.leftArm.zRot = -0.08F;
            }
            case EINHERJAR -> {
                // Spear upright, shield before the chest.
                model.rightArm.xRot = -0.32F + breath;
                model.leftArm.xRot = -0.85F;
                model.leftArm.yRot = 0.45F;
            }
            default -> {
                model.rightArm.xRot += breath;
                model.leftArm.xRot -= breath;
            }
        }
    }

    /** Talking: the hands open and move with the words, the head nods; turns pass between the two every 40 ticks. */
    private static void talk(PlayerModel<AsgardianEntity> model, AsgardianEntity npc, float age) {
        boolean speaking = speaking(npc, age);
        float beat = age * (speaking ? 0.22F : 0.08F);
        float amount = speaking ? 1.0F : 0.25F;
        model.head.xRot += Mth.sin(beat * 1.3F) * 0.08F * amount;
        model.rightArm.xRot = -0.45F - Mth.sin(beat) * 0.35F * amount;
        model.rightArm.yRot = -0.25F + Mth.cos(beat * 0.7F) * 0.2F * amount;
        model.rightArm.zRot = 0.1F;
        model.leftArm.xRot = -0.15F - Mth.sin(beat * 0.8F + 1.3F) * 0.2F * amount;
        model.leftArm.zRot = -0.12F;
        if (!speaking) {
            // Listening: arms folded.
            model.rightArm.xRot = -0.9F;
            model.rightArm.yRot = -0.75F;
            model.leftArm.xRot = -0.9F;
            model.leftArm.yRot = 0.75F;
        }
    }

    /** Whether this one of a pair is speaking now (the other listens). */
    public static boolean speaking(AsgardianEntity npc, float age) {
        return ((int) (age / 40.0F) + npc.getId()) % 2 == 0;
    }

    /** Sparring: knees bent, weapon high, shield forward; the vanilla swing plays the blows. */
    private static void spar(PlayerModel<AsgardianEntity> model, float age) {
        float sway = Mth.sin(age * 0.18F) * 0.12F;
        model.rightArm.xRot = Math.min(model.rightArm.xRot, -1.2F + sway);
        model.rightArm.zRot = -0.15F;
        model.leftArm.xRot = -0.9F - sway;
        model.leftArm.yRot = 0.35F;
        model.rightLeg.xRot = -0.35F;
        model.leftLeg.xRot = 0.3F;
        model.body.yRot = 0.2F + sway * 0.5F;
    }

    /** Feasting: a chicken leg to the mouth every second and a half, a happy sway between bites. */
    private static void feast(PlayerModel<AsgardianEntity> model, float ageInTicks) {
        float cycle = (ageInTicks % 30.0F) / 30.0F;
        float bite = cycle < 0.35F ? Mth.sin(cycle / 0.35F * Mth.PI) : 0.0F;
        model.rightArm.xRot = -1.0F - bite * 0.9F;
        model.rightArm.yRot = -0.35F - bite * 0.3F;
        model.head.xRot = -0.1F + bite * 0.15F;
        model.leftArm.xRot = -0.3F;
        model.body.zRot = Mth.sin(ageInTicks * 0.1F) * 0.04F;
    }

    /** Down: on one knee, head bowed, a hand on the ground; breathing hard. */
    private static void down(PlayerModel<AsgardianEntity> model, float ageInTicks) {
        float breath = Mth.sin(ageInTicks * 0.2F) * 0.05F;
        model.head.xRot = 0.6F + breath;
        model.head.yRot = 0.0F;
        model.body.xRot = 0.25F + breath * 0.5F;
        // The right knee up before him, the left knee on the ground behind.
        model.rightLeg.xRot = -1.15F;
        model.rightLeg.yRot = 0.1F;
        model.leftLeg.xRot = 1.05F;
        model.leftLeg.z = 1.5F;
        model.rightArm.xRot = -0.9F;
        model.rightArm.zRot = 0.15F;
        model.leftArm.xRot = -0.35F;
        model.leftArm.zRot = -0.1F;
        model.head.y = 1.5F;
        model.rightArm.y = 3.0F;
        model.leftArm.y = 3.0F;
        model.rightArm.z = -1.0F;
        model.leftArm.z = -1.0F;
    }
}
