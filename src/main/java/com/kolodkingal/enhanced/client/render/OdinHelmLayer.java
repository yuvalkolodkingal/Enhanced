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
import net.minecraft.client.model.geom.builders.CubeDeformation;
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
 * Odin's winged helmet (3.5, the look {@code allfather}): a gold cap with a brow band and a nose guard, a low crest, and on
 * each side a wing of three layered feathers swept up and back. A model of its own on the head (a skin's hat layer is half a
 * pixel deep), drawn with the head's every turn; a helmet in the head slot hides it, it is not drawn on an invisible player,
 * and never in first person (the head is not drawn there). The Wanderer wears no helmet.
 *
 * <p>Lane L5's Odin (the Asgardian) wears the same helmet: {@link #drawHelm} draws it on any head, with {@link #LAYER} and
 * {@link #TEXTURE}.
 */
public class OdinHelmLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("odin"), "helm");
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/suit/odin_helm.png");
    /** The look that wears the helmet. */
    public static final String LOOK = "allfather";

    private final ModelPart helm;

    public OdinHelmLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart helm) {
        super(parent);
        this.helm = helm;
    }

    /**
     * The helmet, in the pixels of the head (its top at y = -8, its front at z = -4): the cap over the crown, the band at the
     * brow, the nose guard, the crest along the top, and the two wings at the temples.
     */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition helm = root.addOrReplaceChild("helm", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.6F))
                .texOffs(0, 11).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.7F))
                .texOffs(32, 0).addBox(-0.5F, -5.2F, -4.9F, 1.0F, 3.0F, 1.0F)
                .texOffs(36, 0).addBox(-0.5F, -9.6F, -3.5F, 1.0F, 1.0F, 8.0F), PartPose.ZERO);
        wing(helm, "right_wing", -4.7F, false);
        wing(helm, "left_wing", 4.7F, true);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** One wing: three feathers, longest lowest, fanned up and swept back from the temple. */
    private static void wing(PartDefinition helm, String name, float x, boolean mirror) {
        float out = mirror ? 0.22F : -0.22F;
        PartDefinition wing = helm.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(x, -6.0F, -1.5F, 0.0F, out, 0.0F));
        wing.addOrReplaceChild("feather_low", CubeListBuilder.create().texOffs(0, 20).mirror(mirror)
                .addBox(-0.5F, -1.0F, 0.0F, 1.0F, 2.0F, 6.0F), PartPose.rotation(0.35F, 0.0F, 0.0F));
        wing.addOrReplaceChild("feather_mid", CubeListBuilder.create().texOffs(14, 20).mirror(mirror)
                .addBox(-0.5F, -1.0F, 0.0F, 1.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, -1.2F, 0.5F, 0.75F, 0.0F, 0.0F));
        wing.addOrReplaceChild("feather_high", CubeListBuilder.create().texOffs(26, 20).mirror(mirror)
                .addBox(-0.5F, -1.0F, 0.0F, 1.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -2.2F, 1.0F, 1.15F, 0.0F, 0.0F));
    }

    /** Whether this player wears the helmet now: Odin in the Allfather's look, seen, with nothing on the head. */
    static boolean wears(AbstractClientPlayer player, PowerData data) {
        return data.power() == Power.ODIN && LOOK.equals(Variants.current(data).id()) && !player.isInvisible()
                && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!wears(player, Powers.get(player))) {
            return;
        }
        drawHelm(poseStack, buffer, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), getParentModel().head, helm);
    }

    /** The helmet on a head (the hero's, or the Asgardian Odin's). */
    public static void drawHelm(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, ModelPart head, ModelPart helm) {
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        helm.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
