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
 * Heimdall's horned helmet (3.5, the look {@code guardian}): the gold of the helmet the skin paints on the head, and over it
 * what stands off it: a crest from brow to nape and the two great horns that sweep out from the temples and curve forward
 * and up. A model of its own on the head (a skin's hat layer is half a pixel deep), drawn with the head's every turn; a
 * helmet in the head slot hides it, and it is not drawn on an invisible player nor in first person (a player's layers never
 * are). The Ragnarok look, hooded, has none.
 */
public class HeimdallHelmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("heimdall"), "helm");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/suit/heimdall_helm.png");
    /** The look that wears the helmet. */
    public static final String LOOK = "guardian";

    private final ModelPart helm;

    public HeimdallHelmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart helm) {
        super(parent);
        this.helm = helm;
    }

    /**
     * The helmet, in the pixels of the head (its top at y = -8, its front at z = -4): the crest along the crown, and on each
     * side a horn of three pieces, each turned further out, then forward, then up, to a point above the brow.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition helm = root.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, -9.4F, -4.8F, 2.0F, 1.6F, 9.6F)
                .texOffs(0, 12).addBox(-4.6F, -8.6F, -4.6F, 9.2F, 1.0F, 9.2F), PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "right" : "left";
            PartDefinition base = helm.addOrReplaceChild(s + "_horn", CubeListBuilder.create()
                    .texOffs(24, 0).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 4.0F),
                    PartPose.offsetAndRotation(side * 4.6F, -6.4F, 0.0F, 0.0F, side * 1.05F, 0.0F));
            PartDefinition mid = base.addOrReplaceChild("mid", CubeListBuilder.create()
                    .texOffs(40, 0).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                    PartPose.offsetAndRotation(0.0F, -0.4F, -1.0F, 0.35F, side * 0.55F, 0.0F));
            PartDefinition upper = mid.addOrReplaceChild("upper", CubeListBuilder.create()
                    .texOffs(40, 8).addBox(-0.75F, -0.75F, -3.5F, 1.5F, 1.5F, 3.5F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, -3.6F, 0.6F, side * 0.25F, 0.0F));
            upper.addOrReplaceChild("tip", CubeListBuilder.create()
                    .texOffs(52, 8).addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 2.5F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, -3.2F, 0.7F, 0.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Whether this player wears the helmet now: Heimdall in the look of the guardian, seen, with nothing on the head. */
    static boolean wears(AbstractClientPlayer player, PowerData data) {
        return data.power() == Power.HEIMDALL && LOOK.equals(Variants.current(data).id()) && !player.isInvisible()
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
        helm.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                LivingEntityRenderer.getOverlayCoords(player, 0.0F), 0xFFFFFFFF);
        poseStack.popPose();
    }
}
