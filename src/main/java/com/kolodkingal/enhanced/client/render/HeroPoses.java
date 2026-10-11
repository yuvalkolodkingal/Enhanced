package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.Gestures.ArmKey;
import com.kolodkingal.enhanced.client.render.Gestures.Mode;
import com.kolodkingal.enhanced.client.render.Gestures.Target;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The body language of the heroes, laid over the vanilla animation every frame. Poses are not assigned, they are
 * blended: each layer has a weight that eases in and out, so nothing snaps. In order (each blends into what the one before
 * left):
 * <ol>
 *     <li>capture what vanilla made, take the swing of an attack out of the arms (it is put back at the end), and see which
 *     arms vanilla holds in a pose of their own (a bow, a shield, an eaten item): they are left alone;</li>
 *     <li>locomotion: the flight layer ({@link FlightPoses}), the speedster's run and everyone's walk and sprint
 *     ({@link RunPoses});</li>
 *     <li>the ambient stances, then the aimed and holding stances ({@link Stances});</li>
 *     <li>the playing gestures ({@link Gestures}), oldest first, and the jab of Loki's Scepter in place of the swing's chop;</li>
 *     <li>the head, which keeps looking where the camera looks on a leaning body;</li>
 *     <li>the swing put back, and the overlay parts (hat, sleeves, jacket, trousers) copied from what they cover.</li>
 * </ol>
 * The state (weights, lean, gestures) is made by {@link HeroAnimator}; the whole body's lean is on the pose stack
 * ({@link BodyLean}). The first-person hand, the inventory preview and every state vanilla owns (swimming, gliding,
 * riding, sleeping) are left as vanilla draws them.
 */
public final class HeroPoses {
    private static final float RAD = Mth.DEG_TO_RAD;
    /**
     * The planting angle of each weapon's arm in a hero landing (degrees of world elevation): with the lead grip the
     * lowest point of the weapon, in the hero's landing stance, touches the ground at this angle (the bigger the
     * weapon, the higher the arm: see {@link GripMath.Kind#scale}).
     */
    public static final float PLANT_MJOLNIR = 46.7F;
    public static final float PLANT_STORMBREAKER = 53.3F;
    private static final float PLANT_BARE = 25.0F;

    private static final PoseContext CTX = new PoseContext();
    private static final float[] V = new float[3];
    private static final float[] TREMBLE = new float[2];
    private static final float[] HEAD = new float[3];
    private static final float[] LEG = new float[1];

    /**
     * The fields vanilla never resets for the player model, which this layer writes: the head's and the body's roll. The
     * model is shared by every player with the same arm width (and by Loki's illusions), so they are cleared on every call.
     */
    public static void reset(PlayerModel<?> model) {
        model.head.zRot = 0.0F;
        model.body.zRot = 0.0F;
    }

    public static void apply(PlayerModel<?> model, LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks) {
        // The first-person hand is drawn with an all-zero animation call: leave it alone.
        if (!(entity instanceof Player player) || ageInTicks == 0.0F) {
            return;
        }
        HeroAnimState st = HeroAnimator.peek(player);
        if (st != null) {
            st.posed = false;
        }
        float pt = ageInTicks - player.tickCount;
        if (st == null || pt >= 1.0F || st.wVanilla > 0.0F || Math.abs(st.time - ageInTicks) > 1.5 || !EnhancedClientConfig.animations()) {
            return;
        }
        st.posed = true;
        PoseContext c = CTX;
        c.begin(model, player, st, ageInTicks, pt);

        // ---- 1. capture ----
        float vanillaTwist = takeOutSwing(c);
        yields(c);
        uprightFromCrouch(c);

        // ---- 2. locomotion ----
        FlightPoses.apply(c);
        RunPoses.apply(c);

        // ---- 3. stances ----
        Stances.ambient(c);
        Stances.aimed(c);

        // ---- 4. gestures ----
        applyGestures(c);

        // ---- 5. the head ----
        head(c);

        // ---- the swing of Loki's Scepter: a touch, not a chop ----
        scepterJab(c);

        // ---- 6. the swing, the shoulders, the overlay parts ----
        putBackSwing(c, vanillaTwist);
        RunPoses.sync(model);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Capture
    // ---------------------------------------------------------------------------------------------------------------

    /** The physical arm that swings for an attack. */
    private static HumanoidArm attackArm(Player player) {
        return player.swingingArm == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    /**
     * Vanilla added the swing of an attack to the arms (and twisted the body); take it out again, exactly, so the pose
     * layers see the arms as they were without it. Returns the twist vanilla gave the body.
     */
    private static float takeOutSwing(PoseContext c) {
        PlayerModel<?> m = c.model;
        float f = m.attackTime;
        if (f <= 0.0F) {
            return 0.0F;
        }
        float twist = m.body.yRot;
        float g = 1.0F - f;
        g = 1.0F - g * g * g * g;
        float f1 = Mth.sin(g * Mth.PI);
        float f2 = Mth.sin(f * Mth.PI) * -(c.lookPitch - 0.7F) * 0.75F;
        ModelPart attack = attackArm(c.player) == HumanoidArm.RIGHT ? m.rightArm : m.leftArm;
        attack.xRot += f1 * 1.2F + f2;
        attack.yRot -= twist * 2.0F;
        attack.zRot -= Mth.sin(f * Mth.PI) * -0.4F;
        m.rightArm.yRot -= twist;
        m.leftArm.yRot -= twist;
        m.leftArm.xRot -= twist;
        m.body.yRot = 0.0F;
        m.rightArm.x = -5.0F;
        m.rightArm.z = 0.0F;
        m.leftArm.x = 5.0F;
        m.leftArm.z = 0.0F;
        return twist;
    }

    /** Arms that hold a pose of their own (a bow, a shield, a spyglass) or an item in use are left to vanilla. */
    private static void yields(PoseContext c) {
        PlayerModel<?> m = c.model;
        Player p = c.player;
        boolean right = !(m.rightArmPose == ArmPose.EMPTY || m.rightArmPose == ArmPose.ITEM);
        boolean left = !(m.leftArmPose == ArmPose.EMPTY || m.leftArmPose == ArmPose.ITEM);
        if (m.rightArmPose == ArmPose.BOW_AND_ARROW || m.rightArmPose == ArmPose.CROSSBOW_CHARGE || m.rightArmPose == ArmPose.CROSSBOW_HOLD
                || m.leftArmPose == ArmPose.BOW_AND_ARROW || m.leftArmPose == ArmPose.CROSSBOW_CHARGE || m.leftArmPose == ArmPose.CROSSBOW_HOLD) {
            right = true;
            left = true;
        }
        if (p.isUsingItem()) {
            HumanoidArm using = p.getUsedItemHand() == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
            if (using == HumanoidArm.RIGHT) {
                right = true;
            } else {
                left = true;
            }
        }
        boolean mRight = c.mSide == HumanoidArm.RIGHT;
        c.yieldM = mRight ? right : left;
        c.yieldO = mRight ? left : right;
    }

    /** In the air vanilla's sneak (the key that descends) must not crouch the body. */
    private static void uprightFromCrouch(PoseContext c) {
        PlayerModel<?> m = c.model;
        float w = c.st.wAir;
        if (!m.crouching || w <= 0.0F) {
            return;
        }
        float keep = 1.0F - w;
        m.body.xRot *= keep;
        m.rightArm.xRot -= 0.4F * w;
        m.leftArm.xRot -= 0.4F * w;
        m.rightLeg.z *= keep;
        m.leftLeg.z *= keep;
        m.rightLeg.y = Mth.lerp(w, m.rightLeg.y, 12.0F);
        m.leftLeg.y = Mth.lerp(w, m.leftLeg.y, 12.0F);
        m.head.y *= keep;
        m.body.y *= keep;
        m.rightArm.y = Mth.lerp(w, m.rightArm.y, 2.0F);
        m.leftArm.y = Mth.lerp(w, m.leftArm.y, 2.0F);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Gestures
    // ---------------------------------------------------------------------------------------------------------------

    /** Turns the keys of a gesture's arm into absolute angles (radians) for this frame's body. */
    private static final Gestures.ArmSolver SOLVER = (key, main, out) -> solve(CTX, key, main, out);

    private static void solve(PoseContext c, ArmKey key, boolean main, float[] out) {
        switch (key.mode) {
            case AIM -> c.aimAngles(main, key.target, key.x, key.y, key.z, out);
            case WORLD -> {
                float phi = key.x + (key.plant ? plantAngle(c.st) : 0.0F);
                out[0] = c.worldX(phi) * RAD;
                out[1] = key.y * c.side * RAD;
                out[2] = key.z * c.side * RAD;
            }
            default -> {
                out[0] = key.x * RAD;
                out[1] = key.y * c.side * RAD;
                out[2] = key.z * c.side * RAD;
            }
        }
    }

    private static float plantAngle(HeroAnimState st) {
        return st.weaponKind == 2 ? PLANT_STORMBREAKER : st.weaponKind == 1 ? PLANT_MJOLNIR : PLANT_BARE;
    }

    /** Plays the gestures, the oldest first, so the newest wins where they overlap. */
    private static void applyGestures(PoseContext c) {
        HeroAnimState.Slot[] slots = c.st.slots;
        // Three slots at most: pick them oldest first (ties by slot) without allocating.
        int done = 0;
        for (int pass = 0; pass < slots.length; pass++) {
            int next = -1;
            for (int i = 0; i < slots.length; i++) {
                HeroAnimState.Slot slot = slots[i];
                if ((done & (1 << i)) == 0 && slot.active && slot.weight > 0.0F && (next < 0 || slot.start < slots[next].start)) {
                    next = i;
                }
            }
            if (next < 0) {
                break;
            }
            done |= 1 << next;
            applyGesture(c, slots[next]);
        }
    }

    private static void applyGesture(PoseContext c, HeroAnimState.Slot slot) {
        Gestures.Def def = Gestures.get(slot.id);
        float t = slot.t;
        float w = slot.weight;
        boolean force = slot.id.force;
        c.forceM = force;
        c.forceO = force;
        for (int arm = 0; arm < 2; arm++) {
            boolean main = arm == 0;
            Mode mode = def.sampleArm(main, t, SOLVER, V, TREMBLE, c.mechanical);
            if (mode == null) {
                continue;
            }
            if (mode == Mode.ADD) {
                c.armAddRad(main, V[0], V[1], V[2], w);
            } else {
                c.armRad(main, V[0], V[1], V[2], w);
                if (force) {
                    c.occupy(main, w);
                }
            }
            if (TREMBLE[0] > 0.0F) {
                c.tremble(main, TREMBLE[0], TREMBLE[1], w);
            }
        }
        if (def.right.sample(t, LEG)) {
            c.legX(true, LEG[0], w);
        }
        if (def.left.sample(t, LEG)) {
            c.legX(false, LEG[0], w);
        }
        if (def.head.sample(t, HEAD)) {
            c.head(HEAD[0], HEAD[1], HEAD[2], w);
        }
        float twist = def.twist.scalar(t, 0.0F);
        if (twist != 0.0F) {
            c.twist(twist, w);
        }
        if (slot.id == GestureId.TOMMY_VIBRATE) {
            shiver(c, w);
        }
        c.forceM = false;
        c.forceO = false;
    }

    /**
     * Tommy's molecules shiver: every limb jitters a few degrees at 30 Hz, a different jitter each, the same every time for
     * a given player (so it does not depend on the frame rate or on who looks).
     */
    private static void shiver(PoseContext c, float w) {
        if (EnhancedClientConfig.reducedMotion()) {
            return;
        }
        long k = (long) Math.floor(c.age * 1.5F);
        int id = c.player.getId();
        c.armAdd(true, 6.0F * HeroAnimator.noise(k, id), 6.0F * HeroAnimator.noise(k + 1, id), 0.0F, w);
        c.armAdd(false, 6.0F * HeroAnimator.noise(k + 2, id), 6.0F * HeroAnimator.noise(k + 3, id), 0.0F, w);
        c.legAdd(true, 6.0F * HeroAnimator.noise(k + 4, id), 0.0F, w);
        c.legAdd(false, 6.0F * HeroAnimator.noise(k + 5, id), 0.0F, w);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Loki's Scepter
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * A swing with Loki's Scepter in the hand is a jab, not a chop: the swinging arm is thrust along the look and the chop
     * of the vanilla swing is muted by the same amount, over the first 60 percent of the swing (a touch is what takes you over).
     */
    private static void scepterJab(PoseContext c) {
        PlayerModel<?> m = c.model;
        float f = m.attackTime;
        if (f <= 0.0F) {
            return;
        }
        Player p = c.player;
        InteractionHand hand = p.swingingArm == null ? InteractionHand.MAIN_HAND : p.swingingArm;
        if (!p.getItemInHand(hand).is(ModItems.LOKIS_SCEPTER.get())) {
            return;
        }
        HumanoidArm attack = attackArm(p);
        boolean main = attack == c.mSide;
        if (main ? c.yieldM : c.yieldO) {
            return;
        }
        float w = Mth.sin(Mth.PI * Math.min(1.0F, f * 1.7F));
        c.aim(main, Target.LOOK, -4, 0, 0, w);
        c.jab = w;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Head
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * The head keeps looking where the camera looks even when the body leans: the angles that point it along the look on
     * this frame's body replace vanilla's, by how much the body leans. What the stances and gestures added to the head stays.
     */
    private static void head(PoseContext c) {
        ModelPart head = c.model.head;
        float tilt = Math.abs(c.pitch) + Math.abs(c.roll);
        float addX = head.xRot - c.lookPitch;
        float addY = head.yRot - c.lookYaw;
        float addZ = head.zRot;
        float x = c.lookPitch;
        float y = c.lookYaw;
        if (tilt > 0.5F || Math.abs(c.yaw) > 0.5F) {
            float a = AnimMath.ss(0.5F, 3.0F, tilt + Math.abs(c.yaw));
            float[] aim = AimMath.head(AimMath.lookDirection(c.lookPitch, c.lookYaw), c.pitch, c.roll, c.yaw * c.bodySide);
            float ax = Mth.clamp(aim[0], -70.0F * RAD, 60.0F * RAD);
            float ay = Mth.clamp(aim[1], -75.0F * RAD, 75.0F * RAD);
            x = Mth.lerp(a, x, ax);
            y = Mth.lerp(a, y, ay);
        }
        // A bank tilts the head back toward upright, a little.
        float counter = Mth.clamp(0.4F * c.roll, -12.0F, 12.0F) * RAD;
        head.xRot = x + addX;
        head.yRot = y + addY;
        head.zRot = addZ + counter;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The swing and the shoulders
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Puts the swing of an attack back on the arms, so a sword swing still reads in flight and in every ambient stance,
     * and is mostly muted while a hand aims a beam; and turns the shoulders with the chest.
     */
    private static void putBackSwing(PoseContext c, float vanillaTwist) {
        PlayerModel<?> m = c.model;
        float f = m.attackTime;
        float twist = c.twist * RAD;
        if (f > 0.0F) {
            float g = 1.0F - f;
            g = 1.0F - g * g * g * g;
            float f1 = Mth.sin(g * Mth.PI);
            float f2 = Mth.sin(f * Mth.PI) * -(c.lookPitch - 0.7F) * 0.75F;
            HumanoidArm attack = attackArm(c.player);
            boolean attackIsM = attack == c.mSide;
            float occupied = attackIsM ? c.occupiedM : c.occupiedO;
            float keep = Math.min(1.0F - 0.75F * Mth.clamp(occupied, 0.0F, 1.0F), 1.0F - c.jab);
            ModelPart arm = attack == HumanoidArm.RIGHT ? m.rightArm : m.leftArm;
            arm.xRot -= (f1 * 1.2F + f2) * keep;
            arm.yRot += vanillaTwist * 2.0F;
            arm.zRot += Mth.sin(f * Mth.PI) * -0.4F * keep;
            m.rightArm.yRot += vanillaTwist;
            m.leftArm.yRot += vanillaTwist;
            m.leftArm.xRot += vanillaTwist;
            twist += vanillaTwist;
        }
        if (twist != 0.0F) {
            m.body.yRot = twist;
            m.rightArm.z = Mth.sin(twist) * 5.0F;
            m.rightArm.x = -Mth.cos(twist) * 5.0F;
            m.leftArm.z = -Mth.sin(twist) * 5.0F;
            m.leftArm.x = Mth.cos(twist) * 5.0F;
        }
    }

    private HeroPoses() {
    }
}
