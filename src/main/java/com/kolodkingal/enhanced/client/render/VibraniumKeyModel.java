package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ultron.VibraniumKeyEntity;
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

import java.util.List;

/**
 * The Vibranium Key: the film's Doomsday Trigger, a flat steel gear drum with a ring of twenty-four teeth and three
 * toothed spokes at a hundred and twenty degrees, in a crater of rubble, with a tower of stacked black gears and
 * cyan seams rising from the hub. About fifty boxes on a 128x64 skin that is cut into one region for each material
 * (steel, rust, black, cyan): every box of a material reads from the corner of its region. The drum turns; the seams
 * glow through {@code vibranium_key_glow.png}.
 *
 * <p>Model space has y pointing down and 0 on the ground the key stands on: what is above the ground has a negative
 * y. The renderer flips it ({@link VibraniumKeyRenderer}).
 */
public class VibraniumKeyModel extends EntityModel<VibraniumKeyEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("vibranium_key"), "main");

    /** A box: the group it belongs to, its place on the skin, its centre, size and the turn about its own middle (degrees). */
    private record Box(String name, String group, int u, int v, float cx, float cy, float cz, int w, float h, int d, float yaw) {
    }

    private static Box box(String name, String group, int u, int v, float cx, float cy, float cz, int w, float h, int d, float yaw) {
        return new Box(name, group, u, v, cx, cy, cz, w, h, d, yaw);
    }

    private static final List<Box> BOXES = List.of(
            box("rubble0", "crater", 90, 0, 23.07F, -1.5F, 4.48F, 7, 3, 7, -11.0F),
            box("rubble1", "crater", 90, 0, 13.14F, -1.5F, 19.48F, 7, 3, 7, -56.0F),
            box("rubble2", "crater", 90, 0, -4.48F, -1.5F, 23.07F, 7, 3, 7, -101.0F),
            box("rubble3", "crater", 90, 0, -19.48F, -1.5F, 13.14F, 7, 3, 7, -146.0F),
            box("rubble4", "crater", 90, 0, -23.07F, -1.5F, -4.48F, 7, 3, 7, -191.0F),
            box("rubble5", "crater", 90, 0, -13.14F, -1.5F, -19.48F, 7, 3, 7, -236.0F),
            box("rubble6", "crater", 90, 0, 4.48F, -1.5F, -23.07F, 7, 3, 7, -281.0F),
            box("rubble7", "crater", 90, 0, 19.48F, -1.5F, -13.14F, 7, 3, 7, -326.0F),
            box("disc0", "drum", 0, 0, 0.0F, -2.0F, -0.1F, 32, 4, 13, 0.0F),
            box("disc1", "drum", 0, 0, 0.0F, -2.0F, -0.1F, 32, 4, 13, 45.0F),
            box("disc2", "drum", 0, 0, 0.0F, -2.0F, -0.1F, 32, 4, 13, 90.0F),
            box("disc3", "drum", 0, 0, 0.0F, -2.0F, -0.1F, 32, 4, 13, 135.0F),
            box("hub", "drum", 0, 17, 0.0F, -7.0F, 0.0F, 8, 6, 8, 0.0F),
            box("hubGlow", "drum", 56, 17, 0.0F, -10.5F, 0.0F, 6, 1, 6, 0.0F),
            box("tooth0", "drum", 0, 0, 17.5F, -3.5F, 0.0F, 3, 7, 3, -0.0F),
            box("tooth1", "drum", 0, 0, 16.9F, -3.5F, 4.53F, 3, 7, 3, -15.0F),
            box("tooth2", "drum", 0, 0, 15.16F, -3.5F, 8.75F, 3, 7, 3, -30.0F),
            box("tooth3", "drum", 0, 0, 12.37F, -3.5F, 12.37F, 3, 7, 3, -45.0F),
            box("tooth4", "drum", 0, 0, 8.75F, -3.5F, 15.16F, 3, 7, 3, -60.0F),
            box("tooth5", "drum", 0, 0, 4.53F, -3.5F, 16.9F, 3, 7, 3, -75.0F),
            box("tooth6", "drum", 0, 0, 0.0F, -3.5F, 17.5F, 3, 7, 3, -90.0F),
            box("tooth7", "drum", 0, 0, -4.53F, -3.5F, 16.9F, 3, 7, 3, -105.0F),
            box("tooth8", "drum", 0, 0, -8.75F, -3.5F, 15.16F, 3, 7, 3, -120.0F),
            box("tooth9", "drum", 0, 0, -12.37F, -3.5F, 12.37F, 3, 7, 3, -135.0F),
            box("tooth10", "drum", 0, 0, -15.16F, -3.5F, 8.75F, 3, 7, 3, -150.0F),
            box("tooth11", "drum", 0, 0, -16.9F, -3.5F, 4.53F, 3, 7, 3, -165.0F),
            box("tooth12", "drum", 0, 0, -17.5F, -3.5F, 0.0F, 3, 7, 3, -180.0F),
            box("tooth13", "drum", 0, 0, -16.9F, -3.5F, -4.53F, 3, 7, 3, -195.0F),
            box("tooth14", "drum", 0, 0, -15.16F, -3.5F, -8.75F, 3, 7, 3, -210.0F),
            box("tooth15", "drum", 0, 0, -12.37F, -3.5F, -12.37F, 3, 7, 3, -225.0F),
            box("tooth16", "drum", 0, 0, -8.75F, -3.5F, -15.16F, 3, 7, 3, -240.0F),
            box("tooth17", "drum", 0, 0, -4.53F, -3.5F, -16.9F, 3, 7, 3, -255.0F),
            box("tooth18", "drum", 0, 0, 0.0F, -3.5F, -17.5F, 3, 7, 3, -270.0F),
            box("tooth19", "drum", 0, 0, 4.53F, -3.5F, -16.9F, 3, 7, 3, -285.0F),
            box("tooth20", "drum", 0, 0, 8.75F, -3.5F, -15.16F, 3, 7, 3, -300.0F),
            box("tooth21", "drum", 0, 0, 12.37F, -3.5F, -12.37F, 3, 7, 3, -315.0F),
            box("tooth22", "drum", 0, 0, 15.16F, -3.5F, -8.75F, 3, 7, 3, -330.0F),
            box("tooth23", "drum", 0, 0, 16.9F, -3.5F, -4.53F, 3, 7, 3, -345.0F),
            box("spoke0", "spoke0", 0, 0, 12.0F, -6.5F, 0.0F, 24, 3, 4, 0.0F),
            box("spokeGlow0", "spoke0", 56, 17, 13.0F, -8.0F, 0.0F, 18, 1, 1, 0.0F),
            box("spoke1", "spoke1", 0, 0, 12.0F, -6.5F, 0.0F, 24, 3, 4, 0.0F),
            box("spokeGlow1", "spoke1", 56, 17, 13.0F, -8.0F, 0.0F, 18, 1, 1, 0.0F),
            box("spoke2", "spoke2", 0, 0, 12.0F, -6.5F, 0.0F, 24, 3, 4, 0.0F),
            box("spokeGlow2", "spoke2", 56, 17, 13.0F, -8.0F, 0.0F, 18, 1, 1, 0.0F),
            box("tower0", "tower", 0, 17, 0.0F, -14.5F, 0.0F, 14, 7, 14, 0.0F),
            box("seam0", "tower", 56, 17, 0.0F, -18.5F, 0.0F, 15, 1, 15, 0.0F),
            box("tower1", "tower", 0, 17, 0.0F, -22.5F, 0.0F, 12, 7, 12, 0.0F),
            box("seam1", "tower", 56, 17, 0.0F, -26.5F, 0.0F, 13, 1, 13, 0.0F),
            box("tower2", "tower", 0, 17, 0.0F, -30.0F, 0.0F, 10, 6, 10, 0.0F),
            box("seam2", "tower", 56, 17, 0.0F, -33.5F, 0.0F, 11, 1, 11, 0.0F),
            box("tower3", "tower", 0, 17, 0.0F, -37.0F, 0.0F, 8, 6, 8, 0.0F),
            box("crown", "tower", 0, 0, 0.0F, -42.0F, 0.0F, 5, 4, 5, 0.0F),
            box("crownGlow", "tower", 56, 17, 0.0F, -44.5F, 0.0F, 3, 1, 3, 0.0F));

    private final ModelPart root;
    private final ModelPart drum;

    public VibraniumKeyModel(ModelPart root) {
        this.root = root;
        this.drum = root.getChild("drum");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition crater = root.addOrReplaceChild("crater", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition tower = root.addOrReplaceChild("tower", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition drum = root.addOrReplaceChild("drum", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition[] spokes = new PartDefinition[3];
        for (int i = 0; i < spokes.length; i++) {
            spokes[i] = drum.addOrReplaceChild("spoke" + i, CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F,
                    Mth.DEG_TO_RAD * 120.0F * i, 0.0F));
        }
        for (Box box : BOXES) {
            PartDefinition group = switch (box.group()) {
                case "crater" -> crater;
                case "tower" -> tower;
                case "drum" -> drum;
                default -> spokes[box.group().charAt(box.group().length() - 1) - '0'];
            };
            group.addOrReplaceChild(box.name(), CubeListBuilder.create().texOffs(box.u(), box.v())
                    .addBox(-box.w() / 2.0F, -box.h() / 2.0F, -box.d() / 2.0F, box.w(), box.h(), box.d()),
                    PartPose.offsetAndRotation(box.cx(), box.cy(), box.cz(), 0.0F, Mth.DEG_TO_RAD * box.yaw(), 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Turns the drum to this angle (radians). */
    public void spin(float angle) {
        drum.yRot = angle;
    }

    @Override
    public void setupAnim(VibraniumKeyEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
