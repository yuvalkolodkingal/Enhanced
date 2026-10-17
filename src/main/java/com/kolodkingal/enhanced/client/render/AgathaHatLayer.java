package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Agatha's hat on the Witches' Road (Agatha All Along, the look {@code all_along}): a tall black witch's hat with a wide
 * brim and a band of deep green, its point bent back the way soft felt bends. It is a model of its own on the head (a skin's
 * hat layer is half a pixel deep), drawn with the head's every turn, so it follows the head smoothly; a helmet in the head
 * slot hides it, and it is not drawn on an invisible player. The other looks have no hat.
 */
public class AgathaHatLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("agatha"), "hat");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/suit/agatha_hat.png");
    /** The look that wears the hat. */
    public static final String LOOK = "all_along";

    private final ModelPart hat;

    public AgathaHatLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart hat) {
        super(parent);
        this.hat = hat;
    }

    /**
     * The hat, in the pixels of the head (its top is at y = -8, its front at z = -4): the brim a little above the brow, the
     * band round the crown of the head, then the cone in three steps and a point of three that bend back further each.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, -8.7F, -8.0F, 16.0F, 1.0F, 16.0F)
                .texOffs(0, 17).addBox(-4.5F, -11.2F, -4.5F, 9.0F, 3.0F, 9.0F)
                .texOffs(36, 17).addBox(-3.5F, -14.2F, -3.5F, 7.0F, 3.0F, 7.0F), PartPose.ZERO);
        PartDefinition upper = hat.addOrReplaceChild("upper", CubeListBuilder.create()
                .texOffs(0, 30).addBox(-2.5F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, -14.2F, 0.4F, -0.12F, 0.0F, 0.0F));
        PartDefinition bend = upper.addOrReplaceChild("bend", CubeListBuilder.create()
                .texOffs(20, 30).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.6F, 0.2F, -0.38F, 0.0F, 0.0F));
        PartDefinition tip = bend.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(36, 30).addBox(-1.5F, -2.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -2.6F, 0.2F, -0.55F, 0.0F, 0.0F));
        tip.addOrReplaceChild("point", CubeListBuilder.create()
                .texOffs(48, 30).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -2.2F, 0.1F, -0.6F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Whether this player wears the hat now: Agatha in the look of the Road, seen, with nothing on the head. */
    static boolean wears(AbstractClientPlayer player, PowerData data) {
        return data.power() == Power.AGATHA && LOOK.equals(Variants.current(data).id()) && !player.isInvisible()
                && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!wears(player, Powers.get(player))) {
            return;
        }
        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);
        hat.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                LivingEntityRenderer.getOverlayCoords(player, 0.0F));
        poseStack.popPose();
    }
}
