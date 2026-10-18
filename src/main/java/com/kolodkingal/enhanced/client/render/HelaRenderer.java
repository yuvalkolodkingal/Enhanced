package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hela.HelaAttacks;
import com.kolodkingal.enhanced.hela.HelaBrain;
import com.kolodkingal.enhanced.hela.HelaEntity;
import com.kolodkingal.enhanced.hela.HelaFight;
import com.kolodkingal.enhanced.hela.HelaRules;
import com.kolodkingal.enhanced.hela.HelaState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The boss Hela in the world (3.5): {@link HelaModel} in the hero's skin ({@code hela_goddess}) at 1.15 times, a little
 * taller than a player, with:
 *
 * <ul>
 *     <li>her headdress ({@link HelaHeaddressLayer#draw}), which grows from the cowl in the last half of her arrival and
 *     cracks and shrinks away as she dissolves;</li>
 *     <li>a necrosword in each hand (none while her hands are busy with the rain or the catch);</li>
 *     <li>her long cloak, swinging with her walk and her attacks;</li>
 *     <li>the green glow of her eyes and of the line work on her suit (full-bright);</li>
 *     <li>the tell of a volley: the blades hang behind her shoulders, trembling, until they fly.</li>
 * </ul>
 * She is drawn translucent: she forms out of smoke, steps through it and dissolves into it ({@link HelaModel#alpha}); she
 * whirls in the Blade Storm. No name tag; she never falls over (she dissolves).
 */
public class HelaRenderer extends MobRenderer<HelaEntity, HelaModel> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/suit/hela_goddess.png");
    public static final ResourceLocation CLOAK = Enhanced.id("textures/entity/hela_cloak.png");
    public static final ResourceLocation GLOW = Enhanced.id("textures/entity/hela_eyes.png");
    public static final float SCALE = 1.15F;

    private final NecroswordModel blade;
    private final ModelPart headdress;
    private final ModelPart cloak;

    public HelaRenderer(EntityRendererProvider.Context context) {
        super(context, new HelaModel(context.bakeLayer(HelaModel.LAYER)), 0.6F);
        this.blade = new NecroswordModel(context.bakeLayer(NecroswordModel.LAYER));
        this.headdress = context.bakeLayer(HelaHeaddressLayer.LAYER);
        this.cloak = context.bakeLayer(HelaModel.CLOAK).getChild("cloak");
        addLayer(new Cloak(this));
        addLayer(new Glow(this));
        addLayer(new Gear(this));
    }

    @Override
    public ResourceLocation getTextureLocation(HelaEntity hela) {
        return TEXTURE;
    }

    @Override
    protected void scale(HelaEntity hela, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected void setupRotations(HelaEntity hela, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(hela, poseStack, bob, yBodyRot, partialTick, scale);
        if (hela.state() == HelaState.STORM) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-HelaModel.spinAngle(hela.stateTime(partialTick))));
        }
    }

    /** Always translucent (her alpha is the model's); never the outline of a glowing creature only. */
    @Nullable
    @Override
    protected RenderType getRenderType(HelaEntity hela, boolean bodyVisible, boolean translucent, boolean glowing) {
        if (!bodyVisible && !translucent) {
            return glowing ? RenderType.outline(TEXTURE) : null;
        }
        return RenderType.entityTranslucent(TEXTURE);
    }

    @Override
    protected float getFlipDegrees(HelaEntity hela) {
        return 0.0F;
    }

    @Override
    protected boolean shouldShowName(HelaEntity hela) {
        return false;
    }

    @Override
    protected boolean isShaking(HelaEntity hela) {
        return false;
    }

    @Override
    public void render(HelaEntity hela, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(hela, entityYaw, partialTicks, poseStack, buffer, packedLight);
        if (hela.state() == HelaState.VOLLEY && !hela.isInvisible()) {
            volleyTell(hela, partialTicks, poseStack, buffer);
        }
    }

    /** How grown her headdress is: growing from the arrival's {@link HelaFight#FORM_AT} to its boom, gone as she dissolves. */
    static float headdressGrowth(HelaState state, float time) {
        return switch (state) {
            case ARRIVING -> Mth.clamp((time - HelaFight.FORM_AT) / (HelaFight.BOOM_AT - HelaFight.FORM_AT), 0.0F, 1.0F);
            case DYING -> 1.0F - AnimMath.ss(14.0F, 34.0F, time);
            default -> 1.0F;
        };
    }

    /** Whether her hands hold her blades in this state at this time. */
    static boolean holdsBlades(HelaState state, float time) {
        return switch (state) {
            case ARRIVING -> time >= HelaFight.BOOM_AT;
            case RAIN, CATCH, CHANNEL, RISE -> false;
            case DYING -> time < 16.0F;
            default -> true;
        };
    }

    /**
     * The tell of the volley: each blade hangs where the server will throw it from ({@link HelaAttacks#fanPoints}),
     * appearing over the first four ticks, trembling, its point toward her aim.
     */
    private void volleyTell(HelaEntity hela, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        float time = hela.stateTime(partialTicks);
        if (time >= HelaBrain.VOLLEY_THROW) {
            return;
        }
        float appear = AnimMath.ss(0.0F, 4.0F, time);
        float yaw = Mth.rotLerp(partialTicks, hela.yBodyRotO, hela.yBodyRot);
        List<Vec3> points = HelaAttacks.fanPoints(Vec3.ZERO, yaw, hela.getBbHeight(), hela.bladeCount());
        Vec3 origin = hela.getPosition(partialTicks);
        Vec3 aim = hela.aim();
        for (int i = 0; i < points.size(); i++) {
            Vec3 at = points.get(i);
            Vec3 dir = aim.subtract(origin.add(at));
            float y = (float) Math.toDegrees(Mth.atan2(dir.x, dir.z));
            float pitch = (float) Math.toDegrees(Mth.atan2(dir.horizontalDistance(), dir.y));
            poseStack.pushPose();
            poseStack.translate(at.x + Mth.sin(time * 1.9F + i) * 0.02F, at.y, at.z + Mth.cos(time * 1.7F + i) * 0.02F);
            poseStack.mulPose(Axis.YP.rotationDegrees(y));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            float s = 1.25F * appear;
            poseStack.scale(-s, -s, s);
            poseStack.translate(0.0F, NecroswordModel.LENGTH * 0.5F / 16.0F, 0.0F);
            blade.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(NecroswordModel.TEXTURE)), 0xF000F0,
                    OverlayTexture.NO_OVERLAY, EnergyGeometry.argb(0xFFFFFF, appear));
            blade.renderEdges(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(NecroswordModel.TEXTURE)), OverlayTexture.NO_OVERLAY,
                    EnergyGeometry.argb(HelaRules.PALE, appear));
            poseStack.popPose();
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    /** The cloak, from her shoulders, swinging back with her walk and blown out by her attacks. */
    final class Cloak extends RenderLayer<HelaEntity, HelaModel> {
        Cloak(RenderLayerParent<HelaEntity, HelaModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HelaEntity hela, float limbSwing, float limbSwingAmount,
                           float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            HelaModel model = getParentModel();
            if (hela.isInvisible() || model.alpha <= 0.02F) {
                return;
            }
            HelaState state = hela.state();
            float wind = switch (state) {
                case STORM, STEP_IN, VOLLEY -> 0.55F;
                case MELEE, WALL -> 0.35F;
                default -> 0.0F;
            };
            float swing = Math.min(1.0F, limbSwingAmount) * 0.45F + wind + Mth.sin(ageInTicks * 0.09F) * 0.04F;
            poseStack.pushPose();
            model.body.translateAndRotate(poseStack);
            cloak.xRot = 0.08F + swing;
            cloak.getChild("hem").xRot = swing * 0.3F;
            cloak.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(CLOAK)), packedLight, OverlayTexture.NO_OVERLAY,
                    EnergyGeometry.argb(0xFFFFFF, model.alpha));
            poseStack.popPose();
        }
    }

    /** Her eyes and the line work of her suit, green and full-bright, breathing; brighter while she channels. */
    final class Glow extends RenderLayer<HelaEntity, HelaModel> {
        Glow(RenderLayerParent<HelaEntity, HelaModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HelaEntity hela, float limbSwing, float limbSwingAmount,
                           float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            HelaModel model = getParentModel();
            if (hela.isInvisible() || model.alpha <= 0.02F) {
                return;
            }
            float glow = hela.state() == HelaState.CHANNEL ? 1.0F : 0.7F + 0.3F * Mth.sin(ageInTicks * 0.12F);
            float a = model.alpha;
            model.alpha = 1.0F;
            model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)), 0xF00000, OverlayTexture.NO_OVERLAY,
                    EnergyGeometry.argb(0xFFFFFF, glow * a));
            model.alpha = a;
        }
    }

    /** The headdress and the blades in her hands. */
    final class Gear extends RenderLayer<HelaEntity, HelaModel> {
        Gear(RenderLayerParent<HelaEntity, HelaModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HelaEntity hela, float limbSwing, float limbSwingAmount,
                           float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            HelaModel model = getParentModel();
            if (hela.isInvisible() || model.alpha <= 0.02F) {
                return;
            }
            HelaState state = hela.state();
            float time = hela.stateTime(partialTicks);
            int overlay = LivingEntityRenderer.getOverlayCoords(hela, 0.0F);
            int tint = EnergyGeometry.argb(0xFFFFFF, model.alpha);
            float grow = headdressGrowth(state, time);
            HelaHeaddressLayer.draw(poseStack, buffer, packedLight, overlay, model.head, headdress, grow, model.alpha < 0.99F ? tint : 0xFFFFFFFF);
            if (holdsBlades(state, time)) {
                bladeIn(poseStack, buffer, packedLight, model.rightArm, true, model.alpha);
                bladeIn(poseStack, buffer, packedLight, model.leftArm, false, model.alpha);
            }
        }

        /** A necrosword in a hand: the grip in the fist, the blade along the forearm's line and out past the hand. */
        private void bladeIn(PoseStack poseStack, MultiBufferSource buffer, int light, ModelPart arm, boolean right, float alpha) {
            poseStack.pushPose();
            arm.translateAndRotate(poseStack);
            poseStack.translate((right ? -1.0F : 1.0F) / 16.0F, 10.0F / 16.0F, -1.0F / 16.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.95F, 0.95F, 0.95F);
            int tint = EnergyGeometry.argb(0xFFFFFF, alpha);
            blade.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(NecroswordModel.TEXTURE)), light,
                    OverlayTexture.NO_OVERLAY, tint);
            blade.renderEdges(poseStack, buffer.getBuffer(RenderType.entityTranslucentEmissive(NecroswordModel.TEXTURE)), OverlayTexture.NO_OVERLAY,
                    EnergyGeometry.argb(HelaRules.PALE, 0.85F * alpha));
            poseStack.popPose();
        }
    }
}
