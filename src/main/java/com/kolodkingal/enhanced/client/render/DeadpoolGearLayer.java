package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.deadpool.KatanaModel;
import com.kolodkingal.enhanced.client.deadpool.PistolModel;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * Deadpool's weapons on his body (3.7, PLAN 5.2): two katanas in black scabbards crossed on his back (their hilts over
 * his shoulders) and two chunky cartoon pistols in holsters at his hips, always drawn while he has them; in his empty
 * hands when he uses them ({@link DeadpoolActing#swordsOut}, {@link DeadpoolActing#pistolsOut}): then the scabbards and
 * holsters are empty. While he parries, the swords spin before him round his fists. An item in a hand takes the weapon's
 * place; an elytra hides the scabbards (as capes are hidden). His first-person hands hold them too ({@link
 * #onRenderArm}).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public class DeadpoolGearLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The weapons' sizes against the player model. */
    static final float KATANA_SCALE = 0.85F;
    static final float PISTOL_SCALE = 0.75F;

    private static KatanaModel firstPersonKatana;
    private static PistolModel firstPersonPistol;

    private final KatanaModel katana;
    private final PistolModel pistol;

    public DeadpoolGearLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                             KatanaModel katana, PistolModel pistol) {
        super(parent);
        this.katana = katana;
        this.pistol = pistol;
        firstPersonKatana = katana;
        firstPersonPistol = pistol;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        PowerData data = Powers.get(player);
        if (player.isInvisible() || !DeadpoolActing.armed(player, data)) {
            return;
        }
        int overlay = OverlayTexture.NO_OVERLAY;
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        boolean swords = DeadpoolActing.swordsOut(player, data);
        boolean pistols = DeadpoolActing.pistolsOut(player, data);
        VertexConsumer blades = buffer.getBuffer(RenderType.entityCutoutNoCull(KatanaModel.TEXTURE));
        if (data.has(HeroAbility.DEADPOOL_KATANAS)) {
            if (!player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                for (boolean right : new boolean[]{true, false}) {
                    poseStack.pushPose();
                    model.body.translateAndRotate(poseStack);
                    // The scabbards crossed on his back, the hilts up over his shoulders.
                    poseStack.translate((right ? -2.6F : 2.6F) / 16.0F, 1.2F / 16.0F, 2.9F / 16.0F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + (right ? -32.0F : 32.0F)));
                    poseStack.scale(KATANA_SCALE, KATANA_SCALE, KATANA_SCALE);
                    katana.renderSheathed(poseStack, blades, light, overlay, !swords);
                    poseStack.popPose();
                }
            }
            if (swords) {
                float spin = data.is(Live.DEADPOOL_PARRYING) ? DeadpoolActing.parrySpin(player, partialTick) : 0.0F;
                for (HumanoidArm arm : HumanoidArm.values()) {
                    if (handEmpty(player, arm)) {
                        poseStack.pushPose();
                        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                        part.translateAndRotate(poseStack);
                        swordInHand(poseStack, arm == HumanoidArm.RIGHT, spin);
                        katana.renderSword(poseStack, blades, light, overlay);
                        poseStack.popPose();
                    }
                }
            }
        }
        if (data.has(HeroAbility.DEADPOOL_PISTOLS)) {
            VertexConsumer guns = buffer.getBuffer(RenderType.entityCutoutNoCull(PistolModel.TEXTURE));
            for (boolean right : new boolean[]{true, false}) {
                poseStack.pushPose();
                (right ? model.rightLeg : model.leftLeg).translateAndRotate(poseStack);
                // The holster on the outside of the thigh, the barrel down in it, the grip back.
                poseStack.translate((right ? -2.45F : 2.45F) / 16.0F, 1.6F / 16.0F, 0.4F / 16.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.scale(PISTOL_SCALE, PISTOL_SCALE, PISTOL_SCALE);
                pistol.renderHolster(poseStack, guns, light, overlay);
                if (!pistols) {
                    pistol.renderGun(poseStack, guns, light, overlay);
                }
                poseStack.popPose();
            }
            if (pistols) {
                for (HumanoidArm arm : HumanoidArm.values()) {
                    if (handEmpty(player, arm)) {
                        poseStack.pushPose();
                        (arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm).translateAndRotate(poseStack);
                        pistolInHand(poseStack, arm == HumanoidArm.RIGHT);
                        pistol.renderGun(poseStack, guns, light, overlay);
                        poseStack.popPose();
                    }
                }
            }
        }
    }

    /** Whether the hand on that side holds nothing (the weapon is drawn in it). */
    static boolean handEmpty(AbstractClientPlayer player, HumanoidArm arm) {
        boolean mainSide = player.getMainArm() == arm;
        return (mainSide ? player.getMainHandItem() : player.getOffhandItem()).isEmpty();
    }

    /** From the arm's pivot to a sword in its fist: the blade out of the front of the hand, spun when parrying. */
    static void swordInHand(PoseStack poseStack, boolean right, float spin) {
        poseStack.translate((right ? -1.0F : 1.0F) / 16.0F, 10.0F / 16.0F, -0.5F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        if (spin != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(right ? spin : -spin));
        }
        poseStack.scale(KATANA_SCALE, KATANA_SCALE, KATANA_SCALE);
    }

    /** From the arm's pivot to a pistol in its fist: the barrel along the arm past the hand, the grip in the fist. */
    static void pistolInHand(PoseStack poseStack, boolean right) {
        poseStack.translate((right ? -1.0F : 1.0F) / 16.0F, 10.5F / 16.0F, -0.4F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(PISTOL_SCALE, PISTOL_SCALE, PISTOL_SCALE);
    }

    /**
     * His hands in first person hold his swords or pistols while he uses them (the arm is drawn by the game after this,
     * in the same pose; the weapon is drawn at its fist).
     */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        KatanaModel katana = firstPersonKatana;
        PistolModel pistol = firstPersonPistol;
        if (katana == null || pistol == null || player.isInvisible() || Minecraft.getInstance().level == null) {
            return;
        }
        PowerData data = Powers.get(player);
        boolean swords = DeadpoolActing.swordsOut(player, data);
        boolean pistols = DeadpoolActing.pistolsOut(player, data);
        if (!swords && !pistols || !handEmpty(player, event.getArm())) {
            return;
        }
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // The arm's pivot in the player model (the right arm at x -5, the left at +5, 2 below the neck).
        poseStack.translate((right ? -5.0F : 5.0F) / 16.0F, 2.0F / 16.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(right ? 5.0F : -5.0F));
        int light = event.getPackedLight();
        if (swords) {
            float spin = data.is(Live.DEADPOOL_PARRYING) ? DeadpoolActing.parrySpin(player,
                    Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) : 0.0F;
            swordInHand(poseStack, right, spin);
            katana.renderSword(poseStack, event.getMultiBufferSource().getBuffer(
                    RenderType.entityCutoutNoCull(KatanaModel.TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        } else {
            pistolInHand(poseStack, right);
            pistol.renderGun(poseStack, event.getMultiBufferSource().getBuffer(
                    RenderType.entityCutoutNoCull(PistolModel.TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
    }
}
