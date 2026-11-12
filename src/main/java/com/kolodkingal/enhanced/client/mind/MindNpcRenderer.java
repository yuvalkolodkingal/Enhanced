package com.kolodkingal.enhanced.client.mind;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.mind.MindNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The people of Vision's mind (3.12): the robots, Ultron, the Human Torch and Dr. Horton on the player model, each in
 * his skin; Dum-E as a workshop arm on a base; E.D.I.T.H. and F.R.I.D.A.Y. as glowing holograms with rings.
 */
public class MindNpcRenderer extends LivingEntityRenderer<MindNpcEntity, PlayerModel<MindNpcEntity>> {
    public static final ModelLayerLocation DUM_E = new ModelLayerLocation(Enhanced.id("dum_e"), "main");
    public static final ModelLayerLocation ORB = new ModelLayerLocation(Enhanced.id("mind_orb"), "main");

    private static final ResourceLocation ROBOT = Enhanced.id("textures/entity/mind/riddle_bot.png");
    private static final ResourceLocation ULTRON = Enhanced.id("textures/entity/suit/ultron_human.png");
    private static final ResourceLocation TORCH = Enhanced.id("textures/entity/mind/human_torch.png");
    private static final ResourceLocation TORCH_ABLAZE = Enhanced.id("textures/entity/mind/human_torch_ablaze.png");
    private static final ResourceLocation HORTON = Enhanced.id("textures/entity/mind/horton.png");
    private static final ResourceLocation DUM_E_TEXTURE = Enhanced.id("textures/entity/mind/dum_e.png");
    private static final ResourceLocation EDITH = Enhanced.id("textures/entity/mind/edith.png");
    private static final ResourceLocation FRIDAY = Enhanced.id("textures/entity/mind/friday.png");
    private static final ResourceLocation DUM_E_HUMAN = Enhanced.id("textures/entity/mind/dum_e_human.png");
    private static final ResourceLocation EDITH_HUMAN = Enhanced.id("textures/entity/mind/edith_human.png");
    private static final ResourceLocation FRIDAY_HUMAN = Enhanced.id("textures/entity/mind/friday_human.png");

    private final ModelPart dumE;
    private final ModelPart orb;

    public MindNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new MindPlayerModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.dumE = context.bakeLayer(DUM_E);
        this.orb = context.bakeLayer(ORB);
    }

    /** Dum-E: a base, an upright, an arm and a claw (64x32). */
    public static LayerDefinition createDumE() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 18).addBox(-5.0F, -3.0F, -5.0F, 10.0F, 3.0F, 10.0F), PartPose.ZERO);
        PartDefinition upright = root.addOrReplaceChild("upright", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.5F, -14.0F, -1.5F, 3.0F, 14.0F, 3.0F), PartPose.offset(0.0F, -3.0F, 0.0F));
        PartDefinition arm = upright.addOrReplaceChild("arm", CubeListBuilder.create().texOffs(12, 0)
                .addBox(-1.0F, -1.0F, -12.0F, 2.0F, 2.0F, 12.0F), PartPose.offsetAndRotation(0.0F, -13.0F, 0.0F, 0.5F, 0.0F, 0.0F));
        arm.addOrReplaceChild("claw", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -12.0F, -0.9F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** A hologram's core: one 8-pixel cube (32x16). */
    public static LayerDefinition createOrb() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public void render(MindNpcEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        MindNpcEntity.Role role = entity.role();
        // (3.12.2: the AIs are people, as the series plays them; the robot arm and the orbs are kept for a later use)
        if (false && (role == MindNpcEntity.Role.DUM_E || role.floats())) {
            float time = entity.tickCount + partialTicks;
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot)));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            if (role == MindNpcEntity.Role.DUM_E) {
                // a little nod of the arm, as if he were listening
                dumE.getChild("upright").getChild("arm").xRot = 0.5F + 0.12F * Mth.sin(time * 0.08F);
                dumE.getChild("upright").yRot = 0.25F * Mth.sin(time * 0.03F);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(DUM_E_TEXTURE));
                dumE.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                poseStack.translate(0.0F, -0.25F - 0.08F * Mth.sin(time * 0.1F), 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(time * 2.0F));
                ResourceLocation texture = role == MindNpcEntity.Role.EDITH ? EDITH : FRIDAY;
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
                orb.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                poseStack.scale(1.6F, 0.15F, 1.6F);
                orb.render(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0x80FFFFFF);
            }
            poseStack.popPose();
            return;
        }
        model.setAllVisible(true);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MindNpcEntity entity) {
        return switch (entity.role()) {
            case ROBOT -> ROBOT;
            case ULTRON -> ULTRON;
            case TORCH -> entity.ablaze() ? TORCH_ABLAZE : TORCH;
            case HORTON -> HORTON;
            case EDITH -> EDITH_HUMAN;
            case FRIDAY -> FRIDAY_HUMAN;
            case DUM_E -> DUM_E_HUMAN;
        };
    }

    @Override
    protected void scale(MindNpcEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected boolean shouldShowName(MindNpcEntity entity) {
        return false;
    }
}
