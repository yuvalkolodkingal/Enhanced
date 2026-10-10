package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.Shapeshifts;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * Loki's illusions, drawn exactly like him: the player model (slim or wide, as his skin is) with his current skin (his
 * look's suit, his own skin, his Frost Giant form), his cape, his armour and whatever he held, and his name.
 */
public class LokiIllusionRenderer extends LivingEntityRenderer<LokiIllusionEntity, PlayerModel<LokiIllusionEntity>> {
    private final PlayerModel<LokiIllusionEntity> wide;
    private final PlayerModel<LokiIllusionEntity> slim;

    public LokiIllusionRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = model;
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        addLayer(new CapeLayer(this));
    }

    /** The caster's skin as it is right now, or (caster out of sight) the default skin for his UUID. */
    public static PlayerSkin skinOf(LokiIllusionEntity illusion) {
        if (illusion.owner() instanceof AbstractClientPlayer owner) {
            return owner.getSkin();
        }
        UUID owner = illusion.ownerId();
        return DefaultPlayerSkin.get(owner != null ? owner : illusion.getUUID());
    }

    @Override
    public void render(LokiIllusionEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        model = skinOf(entity).model() == PlayerSkin.Model.SLIM ? slim : wide;
        model.setAllVisible(true);
        model.crouching = entity.isCrouching();
        HumanoidModel.ArmPose main = entity.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        HumanoidModel.ArmPose off = entity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        model.rightArmPose = entity.isLeftHanded() ? off : main;
        model.leftArmPose = entity.isLeftHanded() ? main : off;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LokiIllusionEntity entity) {
        return skinOf(entity).texture();
    }

    /** The same size as a player. */
    @Override
    protected void scale(LokiIllusionEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    /** Like a player's name tag: only for a copy of someone. */
    @Override
    protected boolean shouldShowName(LokiIllusionEntity entity) {
        return super.shouldShowName(entity) && entity.owner() != null;
    }

    @Override
    protected void renderNameTag(LokiIllusionEntity entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer,
                                 int packedLight, float partialTick) {
        Player owner = entity.owner();
        super.renderNameTag(entity, owner != null ? Shapeshifts.shownName(owner) : displayName, poseStack, buffer, packedLight, partialTick);
    }

    /** His cape, swinging out behind him as he walks. */
    private static class CapeLayer extends RenderLayer<LokiIllusionEntity, PlayerModel<LokiIllusionEntity>> {
        CapeLayer(RenderLayerParent<LokiIllusionEntity, PlayerModel<LokiIllusionEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, LokiIllusionEntity entity, float limbSwing,
                           float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            PlayerSkin skin = skinOf(entity);
            if (entity.isInvisible() || skin.capeTexture() == null || entity.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                return;
            }
            float swing = Math.min(limbSwingAmount, 1.0F);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.125F);
            poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + swing * 40.0F + Mth.sin(limbSwing * 0.6662F) * 6.0F * swing));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(skin.capeTexture()));
            getParentModel().renderCloak(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}
