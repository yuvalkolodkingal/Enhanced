package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;

/**
 * The Infinity Gauntlet on the arm of the playing Thanos (DESIGN section 15.13.2): the cuff, the engraved forearm and
 * the fist of the boss's own Gauntlet, scaled down to fit a player's arm, with its six sockets alight as {@link
 * ThanosHeroGlow} says. It is on the off-hand arm (the left, for a right-handed player, as in the film), over the skin,
 * so it is on every look, "your own skin" too. In first person the same cuboids are put on the hand by an injection
 * into the renderer of the hand ({@link #renderFirstPerson}), since layers do not run there.
 */
public class GauntletLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enhanced.id("thanos"), "gauntlet");
    /** How much smaller than the Titan's the Gauntlet is, to fit the arm of a player (4 pixels wide, 12 long). */
    static final float SCALE = 0.6F;
    /** How far the Gauntlet is turned about the arm in first person, so that its gems face the player (degrees). */
    private static final float FIRST_PERSON_ROLL = 90.0F;

    /** The layer that was made last: the hand in first person uses its model. */
    @Nullable
    private static GauntletLayer instance;

    private final ModelPart gauntlet;

    public GauntletLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart gauntlet) {
        super(parent);
        this.gauntlet = gauntlet;
        instance = this;
    }

    /** The cuff, forearm and hand of the boss's Gauntlet, at the same places in the same texture. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("gauntlet_cuff", CubeListBuilder.create().texOffs(64, 89).addBox(-5.0F, 3.0F, -5.0F, 10.0F, 2.0F, 10.0F), PartPose.ZERO);
        root.addOrReplaceChild("gauntlet_forearm", CubeListBuilder.create().texOffs(80, 23).addBox(-4.5F, 5.0F, -4.5F, 9.0F, 9.0F, 9.0F), PartPose.ZERO);
        root.addOrReplaceChild("gauntlet_hand", CubeListBuilder.create().texOffs(35, 89).addBox(-3.5F, 13.0F, -3.5F, 7.0F, 6.0F, 7.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, ThanosModel.TEXTURE, ThanosModel.TEXTURE);
    }

    /**
     * Whether this player wears the Gauntlet right now: a Thanos (3.4: or a custom hero with any of his stones) who is
     * not invisible or hidden.
     */
    static boolean wears(AbstractClientPlayer player, PowerData data) {
        return data.acts(Power.THANOS) && !player.isInvisible() && !data.is(Live.HIDDEN);
    }

    /** The arm of the off hand. */
    private static ModelPart offArm(PlayerModel<?> model, AbstractClientPlayer player) {
        return player.getMainArm().getOpposite() == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        if (!wears(player, data)) {
            return;
        }
        poseStack.pushPose();
        offArm(getParentModel(), player).translateAndRotate(poseStack);
        draw(poseStack, buffer, packedLight, gauntlet, player, data, ageInTicks, player.level().getGameTime() + partialTicks);
        poseStack.popPose();
    }

    /** Draws the Gauntlet in the frame of the arm the pose stack is in. */
    private static void draw(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ModelPart gauntlet, AbstractClientPlayer player, PowerData data,
                             float age, float time) {
        poseStack.pushPose();
        poseStack.scale(SCALE, SCALE, SCALE);
        gauntlet.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(ThanosRenderer.TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);
        float[] sockets = new float[ThanosGlow.SOCKETS];
        float[] aura = ThanosHeroGlow.sockets(player, data, time, sockets);
        // The engraving glows in the colour of the stone he used last.
        if (aura[1] > 0.02F) {
            int slot = (int) aura[0];
            StoneKind stone = StoneKind.granted().get(slot - 1);
            // (3.5: a custom hero's stone flares in the colour his pick of it chose.)
            HeroAbility row = HeroAbility.at(Power.THANOS, slot);
            int glow = row == null ? stone.color() : AbilityColors.color(player, row, stone.color());
            gauntlet.render(poseStack, buffer.getBuffer(RenderType.eyes(ThanosGlowLayer.GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    EnergyGeometry.argb(glow, 0.4F + 0.6F * aura[1]));
            GauntletGeometry.draw(poseStack, buffer, sockets, age);
            GauntletGeometry.aura(poseStack, buffer, glow, aura[1] * 0.8F, 6.0F);
        } else {
            GauntletGeometry.draw(poseStack, buffer, sockets, age);
        }
        poseStack.popPose();
    }

    /**
     * The Gauntlet on the hand of the local player in first person: called at the end of the renderer of the hand with
     * the arm it just drew.
     */
    public static void renderFirstPerson(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, ModelPart arm,
                                         PlayerRenderer renderer) {
        if (instance == null || player != Minecraft.getInstance().player) {
            return;
        }
        PowerData data = Powers.get(player);
        if (!wears(player, data) || arm != offArm(renderer.getModel(), player)) {
            return;
        }
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        // The gems are on the outer face of the hand, which the first-person arm turns away from the eye: turn the
        // Gauntlet about the arm.
        poseStack.mulPose(Axis.YP.rotationDegrees(FIRST_PERSON_ROLL));
        draw(poseStack, buffer, packedLight, instance.gauntlet, player, data, player.tickCount, player.level().getGameTime());
        poseStack.popPose();
    }
}
