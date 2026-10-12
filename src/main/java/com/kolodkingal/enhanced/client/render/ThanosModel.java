package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.ThanosBossPoses.Ch;
import com.kolodkingal.enhanced.client.render.ThanosBossPoses.Pose;
import com.kolodkingal.enhanced.thanos.ThanosDefence;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * The body of the Mad Titan (DESIGN section 15.2.7): a custom humanoid 34 pixels tall and twice as broad as a player,
 * drawn at 1.55 times its size, with its own 128 by 128 texture. His shoulders, his jaw and his long arms are what he
 * is; the four plates of his armour (the helmet, the two pauldrons, the breastplate) are parts of their own that are
 * hidden when the plate breaks; the Gauntlet is a forearm, a cuff and a fist whose six sockets are drawn by {@link
 * GauntletGeometry}; the sword is a layer of its own.
 *
 * <p>The model is posed every frame from what the server syncs: his state, his move and its tick, his aim. The pose
 * table is {@link ThanosBossPoses}; this class lays a walk on top of it, blends the change from one pose to the next
 * over a few ticks, and puts the result on the parts. His illusions use the same model (and, for a Thanos near by, his
 * plates and stones, so that nothing tells them from him).
 *
 * @param <T> {@link ThanosEntity}, or his phantom
 */
public class ThanosModel<T extends Monster> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("thanos"), "main");
    /** The size of the texture. */
    public static final int TEXTURE = 128;
    /** The ticks a change of pose takes to blend in. */
    private static final float BLEND_TICKS = 4.0F;

    // ---- the parts ----
    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart[] helmet;
    private final ModelPart[] rightPauldron;
    private final ModelPart[] leftPauldron;
    private final ModelPart breastplate;

    // ---- what the layers read after the pose is set ----
    /** The brightness of each socket of the Gauntlet (see {@link ThanosGlow}). */
    public final float[] sockets = new float[ThanosGlow.SOCKETS];
    /** The whirl of the sword about its grip, in degrees per tick. */
    public float spin;
    /** Which plates of his armour are on (see {@link ThanosDefence}) and how many stones are lit. */
    public int plates = ThanosDefence.ALL_PLATES;
    public int stones = 1;
    /** The glow round the fist while a stone is called on: its colour and how strong (0 to 1). */
    public int auraColor;
    public float auraStrength;

    private final ThanosBossPoses.Out target = new ThanosBossPoses.Out();
    private final Map<Integer, Blend> blends = new HashMap<>();

    /** The last pose an entity was drawn in, to blend away from when it changes. */
    private static final class Blend {
        Pose pose = Pose.STAND;
        final float[] last = new float[Ch.values().length];
        final float[] from = new float[Ch.values().length];
        float changedAt = -1.0E9F;
    }

    public ThanosModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.head = torso.getChild("head");
        this.rightArm = torso.getChild("right_arm");
        this.leftArm = torso.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.helmet = new ModelPart[]{head.getChild("helmet_crown"), head.getChild("helmet_crest"), head.getChild("helmet_cheek_r"),
                head.getChild("helmet_cheek_l"), head.getChild("helmet_back")};
        this.rightPauldron = new ModelPart[]{rightArm.getChild("r_pauldron_top"), rightArm.getChild("r_pauldron_low")};
        this.leftPauldron = new ModelPart[]{leftArm.getChild("l_pauldron_top"), leftArm.getChild("l_pauldron_low")};
        this.breastplate = torso.getChild("body").getChild("breastplate");
    }

    /**
     * The layer: the parts of the table of DESIGN section 15.2.7, each with its box, its pivot and its place in the
     * texture. The torso is a pivot of its own at the hips (the model leans and twists there), and the pivots below it
     * are the table's, taken from the hips.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offset(0.0F, 11.0F, 0.0F));

        PartDefinition head = torso.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(43, 23).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F), PartPose.offset(0.0F, -12.0F, 0.0F));
        head.addOrReplaceChild("chin_beard", CubeListBuilder.create().texOffs(25, 116).addBox(-3.5F, -3.0F, -5.5F, 7.0F, 3.0F, 3.0F), PartPose.ZERO);
        head.addOrReplaceChild("helmet_crown", CubeListBuilder.create().texOffs(41, 75).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 3.0F, 10.0F), PartPose.ZERO);
        head.addOrReplaceChild("helmet_crest", CubeListBuilder.create().texOffs(0, 60).addBox(-1.0F, -15.0F, -4.5F, 2.0F, 5.0F, 9.0F), PartPose.ZERO);
        head.addOrReplaceChild("helmet_cheek_r", CubeListBuilder.create().texOffs(47, 43).addBox(-5.5F, -7.0F, -3.0F, 2.0F, 9.0F, 6.0F), PartPose.ZERO);
        head.addOrReplaceChild("helmet_cheek_l", CubeListBuilder.create().texOffs(64, 43).addBox(3.5F, -7.0F, -3.0F, 2.0F, 9.0F, 6.0F), PartPose.ZERO);
        head.addOrReplaceChild("helmet_back", CubeListBuilder.create().texOffs(0, 116).addBox(-5.0F, -7.0F, 3.5F, 10.0F, 5.0F, 2.0F), PartPose.ZERO);

        PartDefinition body = torso.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 23).addBox(-7.0F, -1.0F, -3.5F, 14.0F, 12.0F, 7.0F), PartPose.offset(0.0F, -11.0F, 0.0F));
        body.addOrReplaceChild("gorget", CubeListBuilder.create().texOffs(23, 60).addBox(-6.0F, -2.0F, -5.0F, 12.0F, 3.0F, 10.0F), PartPose.ZERO);
        body.addOrReplaceChild("breastplate", CubeListBuilder.create().texOffs(0, 43).addBox(-7.5F, -0.5F, -4.5F, 15.0F, 8.0F, 8.0F), PartPose.ZERO);
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(74, 103).addBox(-7.5F, 8.0F, -4.0F, 15.0F, 3.0F, 8.0F), PartPose.ZERO);
        body.addOrReplaceChild("belt_buckle", CubeListBuilder.create().texOffs(84, 116).addBox(-2.0F, 8.0F, -5.0F, 4.0F, 3.0F, 1.0F), PartPose.ZERO);

        PartDefinition rightArm = torso.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -2.0F, -3.5F, 7.0F, 15.0F, 7.0F), PartPose.offset(-9.5F, -10.0F, 0.0F));
        rightArm.addOrReplaceChild("right_bracer", CubeListBuilder.create().texOffs(81, 43).addBox(-4.0F, 6.0F, -4.0F, 8.0F, 6.0F, 8.0F), PartPose.ZERO);
        rightArm.addOrReplaceChild("r_pauldron_top", CubeListBuilder.create().texOffs(68, 60).addBox(-5.0F, -4.5F, -5.0F, 10.0F, 3.0F, 10.0F), PartPose.ZERO);
        rightArm.addOrReplaceChild("r_pauldron_low", CubeListBuilder.create().texOffs(0, 103).addBox(-4.5F, -2.0F, -4.5F, 9.0F, 3.0F, 9.0F), PartPose.ZERO);

        PartDefinition leftArm = torso.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(29, 0).addBox(-3.5F, -2.0F, -3.5F, 7.0F, 15.0F, 7.0F), PartPose.offset(9.5F, -10.0F, 0.0F));
        leftArm.addOrReplaceChild("gauntlet_cuff", CubeListBuilder.create().texOffs(64, 89).addBox(-5.0F, 3.0F, -5.0F, 10.0F, 2.0F, 10.0F), PartPose.ZERO);
        leftArm.addOrReplaceChild("gauntlet_forearm", CubeListBuilder.create().texOffs(80, 23).addBox(-4.5F, 5.0F, -4.5F, 9.0F, 9.0F, 9.0F), PartPose.ZERO);
        leftArm.addOrReplaceChild("gauntlet_hand", CubeListBuilder.create().texOffs(35, 89).addBox(-3.5F, 13.0F, -3.5F, 7.0F, 6.0F, 7.0F), PartPose.ZERO);
        leftArm.addOrReplaceChild("l_pauldron_top", CubeListBuilder.create().texOffs(0, 75).addBox(-5.0F, -4.5F, -5.0F, 10.0F, 3.0F, 10.0F), PartPose.ZERO);
        leftArm.addOrReplaceChild("l_pauldron_low", CubeListBuilder.create().texOffs(37, 103).addBox(-4.5F, -2.0F, -4.5F, 9.0F, 3.0F, 9.0F), PartPose.ZERO);

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(58, 0).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 13.0F, 7.0F), PartPose.offset(-3.6F, 11.0F, 0.0F));
        rightLeg.addOrReplaceChild("knee_plate_r", CubeListBuilder.create().texOffs(46, 116).addBox(-3.5F, 3.5F, -4.5F, 7.0F, 4.0F, 2.0F), PartPose.ZERO);
        rightLeg.addOrReplaceChild("boot_r", CubeListBuilder.create().texOffs(82, 75).addBox(-4.0F, 9.0F, -5.0F, 8.0F, 4.0F, 9.0F), PartPose.ZERO);
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(87, 0).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 13.0F, 7.0F), PartPose.offset(3.6F, 11.0F, 0.0F));
        leftLeg.addOrReplaceChild("knee_plate_l", CubeListBuilder.create().texOffs(65, 116).addBox(-3.5F, 3.5F, -4.5F, 7.0F, 4.0F, 2.0F), PartPose.ZERO);
        leftLeg.addOrReplaceChild("boot_l", CubeListBuilder.create().texOffs(0, 89).addBox(-4.0F, 9.0F, -5.0F, 8.0F, 4.0F, 9.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, TEXTURE, TEXTURE);
    }

    /**
     * Moves the pose stack to the Gauntlet arm's frame (its shoulder, turned as the arm is), for the layer that draws
     * the stones.
     */
    public void translateToGauntlet(PoseStack poseStack) {
        root.translateAndRotate(poseStack);
        torso.translateAndRotate(poseStack);
        leftArm.translateAndRotate(poseStack);
    }

    /** Moves the pose stack to the sword arm's frame, for the layer that puts the sword in its hand. */
    public void translateToSwordArm(PoseStack poseStack) {
        root.translateAndRotate(poseStack);
        torso.translateAndRotate(poseStack);
        rightArm.translateAndRotate(poseStack);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partial = ageInTicks - entity.tickCount;
        target.stand();
        float walk = 1.0F;
        Vec3 aim = null;
        Pose pose = Pose.STAND;
        if (entity instanceof ThanosEntity boss) {
            ThanosState state = boss.state();
            ThanosMove move = boss.currentMove();
            float time = 0.0F;
            int length = 1;
            switch (state) {
                case TELL, ACTIVE -> {
                    if (move != null) {
                        pose = ThanosBossPoses.poseFor(move);
                        length = move.length();
                        time = boss.moveTime(partial);
                        walk = 0.0F;
                    }
                }
                case SURGE -> {
                    pose = Pose.SURGE;
                    length = pose.length();
                    time = boss.stateTime(partial);
                    walk = 0.0F;
                }
                case STAGGERED -> {
                    pose = Pose.STAGGER;
                    length = pose.length();
                    time = boss.stateTime(partial);
                    walk = 0.0F;
                }
                case ARRIVING -> {
                    float landed = boss.landedTime(partial);
                    pose = landed < 0.0F ? Pose.FALL : Pose.ARRIVE;
                    length = pose.length();
                    // The fall is timed from the start of the arrival, the landing from the touch of the ground.
                    time = landed < 0.0F ? boss.stateTime(partial) : landed;
                    walk = 0.0F;
                }
                case DYING -> {
                    pose = Pose.DEATH;
                    length = pose.length();
                    time = boss.stateTime(partial);
                    walk = 0.0F;
                }
                default -> {
                }
            }
            ThanosBossPoses.sample(pose, time, length, target);
            aim = boss.aim();
            ThanosGlow.sockets(sockets, boss.stones(), boss.nextStoneIn(), state, state == ThanosState.TELL || state == ThanosState.ACTIVE ? move : null,
                    boss.moveTime(partial), boss.stateTime(partial), ageInTicks);
            plates = boss.plates();
            stones = boss.stones();
            aura(boss, state, move, boss.moveTime(partial), boss.stateTime(partial));
        } else {
            // An illusion: it walks, and jabs when it strikes.
            float strike = Mth.sin(attackTime * Mth.PI);
            if (attackTime > 0.0F) {
                target.values[Ch.GX.ordinal()] = -90.0F * strike;
                target.values[Ch.TWIST.ordinal()] = 20.0F * strike;
            }
            ThanosEntity real = ThanosClientView.nearestBoss(entity);
            ThanosGlow.sockets(sockets, real == null ? 1 : real.stones(), -1, ThanosState.IDLE, null, 0.0F, 0.0F, ageInTicks);
            plates = real == null ? ThanosDefence.ALL_PLATES : real.plates();
            stones = real == null ? 1 : real.stones();
            auraStrength = 0.0F;
        }
        blend(entity, pose);
        spin = target.get(Ch.SPIN);
        place(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, walk, aim);
    }

    /**
     * The glow round the fist: a stone's colour swelling through a clench, burning while its effect runs, dying away in
     * a surge.
     */
    private void aura(ThanosEntity boss, ThanosState state, ThanosMove move, float moveTime, float stateTime) {
        auraStrength = 0.0F;
        if ((state == ThanosState.TELL || state == ThanosState.ACTIVE) && move != null && move.stone() != null) {
            auraColor = move == ThanosMove.INFINITY_BEAM ? 0xFFFFFF : move.stone().color();
            auraStrength = state == ThanosState.TELL ? Math.min(1.0F, moveTime / Math.max(1.0F, move.tell())) * 0.8F : 0.9F;
        } else if (state == ThanosState.SURGE) {
            auraColor = boss.newestStone().color();
            auraStrength = Math.max(0.0F, 1.0F - stateTime / 40.0F);
        }
    }

    /** Eases from the last pose into this one for a few ticks after the pose changes. */
    private void blend(T entity, Pose pose) {
        if (blends.size() > 64) {
            blends.clear();
        }
        Blend b = blends.computeIfAbsent(entity.getId(), id -> new Blend());
        float[] values = target.values;
        // (The blend runs on the wall clock: the viewer holds a frozen entity's age still, and a pose would never blend
        // in.)
        float clock = System.nanoTime() / 5.0E7F;
        if (b.pose != pose) {
            System.arraycopy(b.last, 0, b.from, 0, b.from.length);
            b.pose = pose;
            b.changedAt = clock;
        }
        float w = AnimMath.ss(0.0F, BLEND_TICKS, clock - b.changedAt);
        if (w < 1.0F) {
            for (int i = 0; i < values.length; i++) {
                values[i] = b.from[i] + (values[i] - b.from[i]) * w;
            }
        }
        System.arraycopy(values, 0, b.last, 0, values.length);
    }

    /** Puts the pose on the parts, with the walk on top of it. */
    private void place(T entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch, float walk, Vec3 aim) {
        float[] v = target.values;
        float stride = Mth.cos(limbSwing * 0.5F) * limbSwingAmount;
        root.y = v[Ch.DROP.ordinal()] + Mth.sin(age * 0.08F) * 0.3F;
        // Standing still the legs rest; walking they swing (a heavy, slow stride), and the arms swing against them.
        rightLeg.xRot = Mth.DEG_TO_RAD * v[Ch.LEG_R.ordinal()] + walk * stride;
        leftLeg.xRot = Mth.DEG_TO_RAD * v[Ch.LEG_L.ordinal()] - walk * stride;
        torso.xRot = Mth.DEG_TO_RAD * v[Ch.LEAN.ordinal()];
        torso.yRot = Mth.DEG_TO_RAD * v[Ch.TWIST.ordinal()];
        rightArm.xRot = Mth.DEG_TO_RAD * v[Ch.RX.ordinal()] - walk * stride * 0.4F;
        rightArm.yRot = Mth.DEG_TO_RAD * v[Ch.RY.ordinal()];
        rightArm.zRot = Mth.DEG_TO_RAD * v[Ch.RZ.ordinal()];
        float gx = Mth.DEG_TO_RAD * v[Ch.GX.ordinal()] + walk * stride * 0.4F;
        float gy = Mth.DEG_TO_RAD * v[Ch.GY.ordinal()];
        float aimed = v[Ch.AIM.ordinal()];
        if (aim != null && aimed > 0.001F) {
            float[] toward = aimedArm(entity, aim);
            gx = Mth.lerp(aimed, gx, toward[0]);
            gy = Mth.lerp(aimed, gy, toward[1]);
        }
        leftArm.xRot = gx;
        leftArm.yRot = gy;
        leftArm.zRot = Mth.DEG_TO_RAD * v[Ch.GZ.ordinal()] * (1.0F - aimed);
        float shake = v[Ch.TREMBLE.ordinal()];
        if (shake > 0.001F) {
            leftArm.xRot += Mth.DEG_TO_RAD * shake * Mth.sin(age * 2.0F);
            leftArm.yRot += Mth.DEG_TO_RAD * shake * Mth.cos(age * 2.0F + 0.7F);
        }
        head.xRot = Mth.DEG_TO_RAD * (Mth.clamp(headPitch, -45.0F, 45.0F) + v[Ch.HEAD_X.ordinal()]);
        head.yRot = Mth.DEG_TO_RAD * (Mth.clamp(netHeadYaw, -45.0F, 45.0F) + v[Ch.HEAD_Y.ordinal()]) - torso.yRot;
        head.zRot = Mth.DEG_TO_RAD * v[Ch.HEAD_Z.ordinal()];
        for (ModelPart part : helmet) {
            part.visible = (plates & ThanosDefence.HELMET) != 0;
        }
        for (ModelPart part : rightPauldron) {
            part.visible = (plates & ThanosDefence.R_PAULDRON) != 0;
        }
        for (ModelPart part : leftPauldron) {
            part.visible = (plates & ThanosDefence.L_PAULDRON) != 0;
        }
        breastplate.visible = (plates & ThanosDefence.BREASTPLATE) != 0;
    }

    /** The {@code xRot, yRot} that point the Gauntlet arm along a world direction, for the way his body faces. */
    private float[] aimedArm(T entity, Vec3 aim) {
        float yaw = Mth.DEG_TO_RAD * Mth.rotLerp(0.0F, entity.yBodyRotO, entity.yBodyRot);
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        Vector3f local = new Vector3f((float) aim.dot(right), (float) aim.y, (float) aim.dot(forward));
        return AimMath.arm(local, 0.0F, 0.0F, 0.0F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /** Forgets every pose being blended (leaving the world). */
    public void clear() {
        blends.clear();
    }
}
