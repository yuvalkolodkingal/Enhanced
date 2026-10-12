package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.client.render.ApocalypseBossPoses.Ch;
import com.kolodkingal.enhanced.client.render.ApocalypseBossPoses.Pose;
import com.kolodkingal.enhanced.client.render.ApocalypseCubes.Cube;
import com.kolodkingal.enhanced.client.render.ApocalypseCubes.Kind;
import com.kolodkingal.enhanced.client.render.ApocalypseCubes.Pivot;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The body of En Sabah Nur (DESIGN section 17, 6.8): the 36 cubes of {@link ApocalypseCubes} on a biped skeleton, drawn at
 * 1.35 times their size (1.2 in the true form, with the armour cubes hidden). The model is posed every frame from what the
 * server syncs: the state of his controller and the game time it began, the point his attack is aimed at, whether the
 * armour has fallen. The poses are {@link ApocalypseBossPoses}; this class lays the walk, the breathing and the sway of
 * the cloak on top of them, eases from one pose into the next over a few ticks, lets the pose of an attack run on past its
 * tell so that the blow is seen, and, when he dies, takes the cubes away one by one (layer by layer).
 *
 * <p>The hips are the pivot of the body (a {@code torso} part sits there, and the head, the body and the arms hang on it),
 * so a lean or a turn bends him at the waist and a kneeling body sinks as the legs fold.
 */
public class ApocalypseModel extends EntityModel<ApocalypseEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("apocalypse"), "main");
    /** The ticks a change of pose takes to blend in. */
    private static final float BLEND_TICKS = 4.0F;
    /** The hips, in the model's coordinates: the pivot of the torso. */
    private static final float HIP = 12.0F;
    /** How long the body takes to come apart when he dies: from this tick of the death to this one. */
    public static final float DISSOLVE_FROM = 30.0F;
    public static final float DISSOLVE_TO = 150.0F;
    /** The tick of the armour's fall at which the plates leave his body. */
    public static final float PLATES_FLY_AT = 8.0F;

    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    /** Every cube by its name ({@code arm} and {@code arm_l} for the two sides). */
    private final Map<String, ModelPart> cubes = new LinkedHashMap<>();
    private final List<ModelPart> armour = new ArrayList<>();
    private final ModelPart[] cloak = new ModelPart[3];

    // ---- what the layers read after the pose is set ----
    /** The glow of the cracks of the cap (0 to 1). */
    public float glow;
    /** How much of the body has dissolved (0 to 1), and whether it burns away from the surface in (the Phoenix's fire). */
    public float dissolve;
    public boolean burning;

    private final ApocalypseBossPoses.Out target = new ApocalypseBossPoses.Out();
    private final ApocalypseBossPoses.Out follow = new ApocalypseBossPoses.Out();
    private final Map<Integer, Memory> memories = new HashMap<>();

    /** What was drawn of one boss last, to blend away from when its pose changes and to play out a blow. */
    private static final class Memory {
        Pose pose = Pose.STAND;
        float poseSince;
        /** The pose that just ended and goes on for a few ticks, and when it ended. */
        Pose following;
        float followingSince;
        final float[] last = new float[Ch.values().length];
        final float[] from = new float[Ch.values().length];
        float changedAt = -1.0E9F;
    }

    public ApocalypseModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.head = torso.getChild("head");
        this.rightArm = torso.getChild("right_arm");
        this.leftArm = torso.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        for (Cube cube : ApocalypseCubes.ALL) {
            ModelPart part = pivot(cube.pivot()).getChild(cube.name());
            cubes.put(cube.name(), part);
            if (cube.kind() == Kind.ARMOUR) {
                armour.add(part);
            }
            if (cube.mirrored()) {
                ModelPart other = pivot(mirror(cube.pivot())).getChild(cube.name() + "_l");
                cubes.put(cube.name() + "_l", other);
                if (cube.kind() == Kind.ARMOUR) {
                    armour.add(other);
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            cloak[i] = cubes.get("cloak_" + (i + 1));
        }
    }

    private ModelPart pivot(Pivot pivot) {
        return switch (pivot) {
            case HEAD -> head;
            case BODY -> torso.getChild("body");
            case RIGHT_ARM -> rightArm;
            case LEFT_ARM -> leftArm;
            case RIGHT_LEG -> rightLeg;
            case LEFT_LEG -> leftLeg;
        };
    }

    private static Pivot mirror(Pivot pivot) {
        return switch (pivot) {
            case HEAD, BODY -> pivot;
            case RIGHT_ARM -> Pivot.LEFT_ARM;
            case LEFT_ARM -> Pivot.RIGHT_ARM;
            case RIGHT_LEG -> Pivot.LEFT_LEG;
            case LEFT_LEG -> Pivot.RIGHT_LEG;
        };
    }

    /**
     * The layer: the pivots of the skeleton, and every cube of the table hanging on its pivot with its box and its place
     * in the texture. The torso is a part of its own at the hips (the body leans and turns there), and the head, the body
     * and the arms hang on it, offset by the height of the hips.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offset(0.0F, HIP, 0.0F));
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -HIP, 0.0F));
        PartDefinition body = torso.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -HIP, 0.0F));
        PartDefinition rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create(),
                PartPose.offset(Pivot.RIGHT_ARM.x, Pivot.RIGHT_ARM.y - HIP, Pivot.RIGHT_ARM.z));
        PartDefinition leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create(),
                PartPose.offset(Pivot.LEFT_ARM.x, Pivot.LEFT_ARM.y - HIP, Pivot.LEFT_ARM.z));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
                PartPose.offset(Pivot.RIGHT_LEG.x, Pivot.RIGHT_LEG.y, Pivot.RIGHT_LEG.z));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
                PartPose.offset(Pivot.LEFT_LEG.x, Pivot.LEFT_LEG.y, Pivot.LEFT_LEG.z));
        for (Cube cube : ApocalypseCubes.ALL) {
            PartDefinition on = switch (cube.pivot()) {
                case HEAD -> head;
                case BODY -> body;
                case RIGHT_ARM -> rightArm;
                case LEFT_ARM -> leftArm;
                case RIGHT_LEG -> rightLeg;
                case LEFT_LEG -> leftLeg;
            };
            on.addOrReplaceChild(cube.name(), CubeListBuilder.create().texOffs(cube.u(), cube.v())
                    .addBox(cube.x(), cube.y(), cube.z(), cube.w(), cube.h(), cube.d()), PartPose.ZERO);
            if (cube.mirrored()) {
                PartDefinition other = switch (cube.pivot()) {
                    case RIGHT_ARM -> leftArm;
                    case LEFT_ARM -> rightArm;
                    case RIGHT_LEG -> leftLeg;
                    case LEFT_LEG -> rightLeg;
                    default -> on;
                };
                other.addOrReplaceChild(cube.name() + "_l", CubeListBuilder.create().texOffs(cube.u(), cube.v()).mirror()
                        .addBox(ApocalypseCubes.mirroredX(cube.x(), cube.w()), cube.y(), cube.z(), cube.w(), cube.h(), cube.d()), PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, ApocalypseCubes.WIDTH, ApocalypseCubes.HEIGHT);
    }

    /**
     * Whether he still wears his armour: until the third shield breaks, and for the first 8 ticks of the fall (the plates
     * stay on while his arms are crossed, and fly off when they are flung out).
     */
    public static boolean armoured(ApocalypseEntity boss, ApocalypseState state, float time) {
        return !boss.isTrueForm() || state == ApocalypseState.ARMOUR_FALL && time < PLATES_FLY_AT;
    }

    /** The cube called {@code name}, for the renderer's armour plates. */
    public ModelPart cube(String name) {
        return cubes.get(name);
    }

    /** The head, for the layers that need where it is. */
    public ModelPart head() {
        return head;
    }

    @Override
    public void setupAnim(ApocalypseEntity boss, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partial = ageInTicks - boss.tickCount;
        ApocalypseState state = boss.state();
        float time = boss.stateTime(partial);
        Pose pose = ApocalypseBossPoses.poseFor(state);
        Memory memory = memory(boss);
        float now = boss.shownByViewer() ? 0.0F : ageInTicks;

        // The pose of the state, or the end of the blow that has just been struck.
        if (pose != memory.pose) {
            System.arraycopy(memory.last, 0, memory.from, 0, memory.from.length);
            if (pose == Pose.STAND && ApocalypseBossPoses.follows(memory.pose) && !boss.shownByViewer()) {
                memory.following = memory.pose;
                memory.followingSince = now;
            } else {
                memory.following = null;
            }
            memory.pose = pose;
            memory.changedAt = now;
        }
        float blend = boss.shownByViewer() ? 1.0F : AnimMath.ss(0.0F, BLEND_TICKS, now - memory.changedAt);
        if (memory.following != null && now - memory.followingSince <= ApocalypseBossPoses.FOLLOW_TICKS) {
            ApocalypseBossPoses.sample(memory.following, memory.following.length() + (now - memory.followingSince), target);
            blend = AnimMath.ss(0.0F, 2.0F, now - memory.changedAt);
        } else {
            memory.following = null;
            ApocalypseBossPoses.sample(pose, time, target);
        }
        float[] v = target.values;
        if (blend < 1.0F) {
            for (int i = 0; i < v.length; i++) {
                v[i] = memory.from[i] + (v[i] - memory.from[i]) * blend;
            }
        }
        System.arraycopy(v, 0, memory.last, 0, v.length);

        glow = v[Ch.GLOW.ordinal()];
        place(boss, v, state, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        showCubes(boss, state, time);
    }

    private Memory memory(ApocalypseEntity boss) {
        if (memories.size() > 32) {
            memories.clear();
        }
        return memories.computeIfAbsent(boss.getId(), id -> new Memory());
    }

    /** Puts the pose on the parts, with the walk, the breathing and the cloak on top of it. */
    private void place(ApocalypseEntity boss, float[] v, ApocalypseState state, float limbSwing, float limbSwingAmount, float age,
                       float netHeadYaw, float headPitch) {
        boolean walks = state == ApocalypseState.IDLE || state == ApocalypseState.AWAKENING;
        float stride = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
        float breathe = Mth.sin(age * 0.05F);
        root.y = v[Ch.DROP.ordinal()] + (walks ? Mth.sin(age * 0.08F) * 0.25F : 0.0F);
        torso.xRot = Mth.DEG_TO_RAD * (v[Ch.LEAN.ordinal()] + (walks ? 0.4F * breathe : 0.0F));
        torso.yRot = Mth.DEG_TO_RAD * v[Ch.TWIST.ordinal()];
        rightLeg.xRot = Mth.DEG_TO_RAD * v[Ch.LEG_R.ordinal()] + (walks ? 1.1F * stride : 0.0F);
        leftLeg.xRot = Mth.DEG_TO_RAD * v[Ch.LEG_L.ordinal()] - (walks ? 1.1F * stride : 0.0F);

        float swing = walks ? 0.35F * stride : 0.0F;
        float sway = walks ? 0.025F * breathe : 0.0F;
        float aimed = v[Ch.AIM.ordinal()];
        float rx = Mth.DEG_TO_RAD * v[Ch.RX.ordinal()];
        float ry = Mth.DEG_TO_RAD * v[Ch.RY.ordinal()];
        if (aimed > 0.001F) {
            float[] toward = aimedArm(boss);
            rx = Mth.lerp(aimed, rx, toward[0] + rx);
            ry = Mth.lerp(aimed, ry, toward[1] + ry);
        }
        rightArm.xRot = rx - swing;
        rightArm.yRot = ry;
        rightArm.zRot = Mth.DEG_TO_RAD * v[Ch.RZ.ordinal()] + sway * (1.0F - aimed);
        leftArm.xRot = Mth.DEG_TO_RAD * v[Ch.LX.ordinal()] + swing;
        leftArm.yRot = Mth.DEG_TO_RAD * v[Ch.LY.ordinal()];
        leftArm.zRot = Mth.DEG_TO_RAD * v[Ch.LZ.ordinal()] - sway;

        head.xRot = Mth.DEG_TO_RAD * (Mth.clamp(headPitch, -40.0F, 40.0F) * 0.6F + v[Ch.HEAD_X.ordinal()]);
        head.yRot = Mth.DEG_TO_RAD * (Mth.clamp(netHeadYaw, -50.0F, 50.0F) + v[Ch.HEAD_Y.ordinal()]) - torso.yRot;
        head.zRot = Mth.DEG_TO_RAD * v[Ch.HEAD_Z.ordinal()];

        // The three panels of the cloak hang from the shoulders and stream behind him as he walks.
        for (int i = 0; i < cloak.length; i++) {
            cloak[i].xRot = 0.06F + 0.55F * Math.abs(limbSwingAmount) + 0.035F * Mth.sin(age * 0.11F + i * 1.3F)
                    + (v[Ch.LEAN.ordinal()] < 0.0F ? -0.01F * v[Ch.LEAN.ordinal()] : 0.0F);
        }
    }

    /**
     * The {@code xRot, yRot} that point the right arm along the point his attack is aimed at, for the way his body faces
     * (the aim is the world point the server locked; the arm is aimed from the shoulder).
     */
    private float[] aimedArm(ApocalypseEntity boss) {
        Vec3 shoulder = boss.position().add(0.0, boss.getBbHeight() * 0.75, 0.0);
        Vec3 toward = boss.aim().subtract(shoulder);
        if (toward.lengthSqr() < 1.0E-6) {
            return new float[]{-Mth.HALF_PI, 0.0F};
        }
        toward = toward.normalize();
        float yaw = Mth.DEG_TO_RAD * boss.yBodyRot;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        Vector3f local = new Vector3f((float) toward.dot(right), (float) toward.y, (float) toward.dot(forward));
        return AimMath.arm(local, torso.xRot / Mth.DEG_TO_RAD, 0.0F, 0.0F);
    }

    /**
     * Which cubes are drawn: the armour is gone in the true form, and as he dies the body comes apart cube by cube, from
     * the feet up in sand, from the surface in (the armour first) in the Phoenix's fire.
     */
    private void showCubes(ApocalypseEntity boss, ApocalypseState state, float time) {
        boolean frail = !armoured(boss, state, time);
        for (ModelPart part : armour) {
            part.visible = !frail;
        }
        dissolve = 0.0F;
        burning = boss.diesInFire();
        if (state == ApocalypseState.DYING) {
            dissolve = AnimMath.clamp((time - DISSOLVE_FROM) / (DISSOLVE_TO - DISSOLVE_FROM), 0.0F, 1.0F);
        }
        for (Cube cube : ApocalypseCubes.ALL) {
            boolean shown = !(frail && cube.kind() == Kind.ARMOUR);
            if (dissolve > 0.0F && shown) {
                shown = dissolve < threshold(cube, burning);
            }
            cubes.get(cube.name()).visible = shown;
            if (cube.mirrored()) {
                cubes.get(cube.name() + "_l").visible = shown;
            }
        }
    }

    /**
     * The dissolve at which a cube goes: in sand the feet first and the head last, in fire the outermost armour first and
     * the heart of the body last. The spread keeps two neighbours from going at once.
     */
    public static float threshold(Cube cube, boolean fire) {
        float low = switch (cube.name()) {
            case "leg", "greave", "knee" -> 0.0F;
            case "belt", "tasset", "cloak_1", "cloak_2", "cloak_3" -> 0.18F;
            case "body", "chest_plate", "glyph_plate", "collar_low", "collar_back" -> 0.34F;
            case "arm", "pauldron_a", "pauldron_b", "gauntlet" -> 0.5F;
            case "collar_high" -> 0.62F;
            case "head", "cable_1", "cable_2", "cable_3" -> 0.8F;
            default -> 0.9F; // the cap and its crest
        };
        float surface = switch (cube.name()) {
            case "pauldron_a", "pauldron_b", "gauntlet", "greave", "knee", "collar_low", "collar_high", "collar_back" -> 0.0F;
            case "chest_plate", "glyph_plate", "belt", "cloak_1", "cloak_2", "cloak_3", "crest" -> 0.2F;
            case "arm", "leg", "tasset", "cable_1", "cable_2", "cable_3" -> 0.45F;
            case "body" -> 0.65F;
            case "head" -> 0.8F;
            default -> 0.9F;
        };
        float order = fire ? surface : low;
        return 0.12F + order * 0.8F + 0.001F * ((cube.u() + cube.v()) % 7);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /** Forgets every pose being blended (leaving the world). */
    public void clear() {
        memories.clear();
    }
}
