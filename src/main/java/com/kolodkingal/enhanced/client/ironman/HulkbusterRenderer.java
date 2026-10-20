package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.client.render.HandTrackerLayer;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.hulkbuster.Hulkbuster;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;

/**
 * The renderer of an Iron Man inside the Hulkbuster (3.8, lane L4, PLAN 6.4): the player's own body is not drawn
 * (its render type is none), the Hulkbuster's is, part by part ({@link SuitModels#get}: lane L7's model, the skeleton's
 * box suit until it merges) on the limbs of the player model as it moves (the walk, the swing of the punches, the
 * stances), at the player's 3.5× scale (the {@code SCALE} attribute, as for any creature). The suit is lit (its
 * emissive pass) while it works, its thrusters burn while it flies, the fist holds what he holds. {@link
 * HulkbusterRenderEvents} draws him with it instead of the player renderer (after Loki's tricks and the Hulk's; never
 * two). Its first-person forearm is {@link #renderFirstPersonArm}. Made at every resource reload ({@code ClientSetup},
 * region nanotech).
 */
public class HulkbusterRenderer extends PlayerRenderer {
    /** The one made at the last resource reload, or null before it. */
    @Nullable
    private static HulkbusterRenderer instance;

    private final SuitAssembly.Poses still = new SuitAssembly.Poses(SuitKind.HULKBUSTER);

    public HulkbusterRenderer(EntityRendererProvider.Context context) {
        super(context, false);
        this.shadowRadius = 0.6F;
        this.layers.clear();
        addLayer(new Body(this));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new HandTrackerLayer(this));
    }

    public static void set(HulkbusterRenderer renderer) {
        instance = renderer;
    }

    @Nullable
    public static HulkbusterRenderer get() {
        return instance;
    }

    /** Whether this player is drawn as the Hulkbuster now (it is on and grown, or a phase shut the frame on him). */
    public static boolean drawsAsHulkbuster(AbstractClientPlayer player, float partialTick) {
        if (instance == null) {
            return false;
        }
        IronManState state = IronSuits.state(player);
        return Hulkbuster.drawnAsHulkbuster(player, state, player.level().getGameTime(), partialTick);
    }

    /** The player's own body is never drawn (only the suit, by the layer). */
    @Nullable
    @Override
    protected RenderType getRenderType(AbstractClientPlayer player, boolean visible, boolean translucent,
                                       boolean glowing) {
        return null;
    }

    /** The Hulkbuster's parts on the player model's limbs. */
    static final class Body extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final HulkbusterRenderer owner;

        Body(HulkbusterRenderer owner) {
            super(owner);
            this.owner = owner;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (player.isInvisible()) {
                return;
            }
            PlayerModel<AbstractClientPlayer> body = getParentModel();
            PowerData data = Powers.get(player);
            if (data.is(Live.IRON_GRABBING)) {
                // The right fist held up before him with what it grabbed.
                body.rightArm.xRot = -1.65F;
                body.rightArm.yRot = -0.1F;
                body.rightSleeve.copyFrom(body.rightArm);
            }
            SuitModel model = SuitModels.get(SuitKind.HULKBUSTER);
            SuitAssembly.Poses poses = owner.still;
            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(model.texture()));
            for (SuitParts part : SuitParts.VALUES) {
                SuitLayer.drawPart(poseStack, vc, model, body, poses.get(part), part, light, overlay);
            }
            boolean lit = IronSuits.state(player).has(IronManState.POWERED);
            ResourceLocation glow = model.glowTexture();
            if (lit && glow != null) {
                VertexConsumer eyes = buffer.getBuffer(RenderType.eyes(glow));
                for (SuitParts part : SuitParts.VALUES) {
                    SuitLayer.drawPart(poseStack, eyes, model, body, poses.get(part), part, LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY);
                }
            }
            if (lit && data.flying()) {
                SuitLayer.thrusters(poseStack, buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM), body, ageInTicks,
                        1.6F);
            }
        }
    }

    /**
     * His arm in first person: the Hulkbuster's upper arm, forearm and huge gauntlet (no skin), a little further in so
     * the fist stays in view.
     */
    public void renderFirstPersonArm(PoseStack poseStack, MultiBufferSource buffer, int light,
                                     AbstractClientPlayer player, HumanoidArm side) {
        PlayerModel<AbstractClientPlayer> model = getModel();
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        boolean right = side == HumanoidArm.RIGHT;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        arm.xRot = 0.0F;
        SuitModel suit = SuitModels.get(SuitKind.HULKBUSTER);
        SuitParts[] pieces = right ? new SuitParts[]{SuitParts.RIGHT_UPPER_ARM, SuitParts.RIGHT_FOREARM,
                SuitParts.RIGHT_GAUNTLET} : new SuitParts[]{SuitParts.LEFT_UPPER_ARM, SuitParts.LEFT_FOREARM,
                SuitParts.LEFT_GAUNTLET};
        poseStack.pushPose();
        poseStack.translate(right ? -0.06F : 0.06F, 0.0F, 0.0F);
        poseStack.scale(1.35F, 1.35F, 1.35F);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(suit.texture()));
        for (SuitParts part : pieces) {
            SuitLayer.drawPart(poseStack, vc, suit, model, still.get(part), part, light, OverlayTexture.NO_OVERLAY);
        }
        ResourceLocation glow = suit.glowTexture();
        if (glow != null && IronSuits.state(player).has(IronManState.POWERED)) {
            VertexConsumer eyes = buffer.getBuffer(RenderType.eyes(glow));
            for (SuitParts part : pieces) {
                SuitLayer.drawPart(poseStack, eyes, suit, model, still.get(part), part, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY);
            }
        }
        poseStack.popPose();
    }
}
