package com.kolodkingal.enhanced.client.asgard;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.NpcState;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the Asgardians (3.5): the player model with the skin of the role (and of the Einherjar's or citizen's variant),
 * slim or wide as the skin is; their capes, the Einherjar's crested helmet and whatever headgear another lane registers
 * ({@link #registerHeadgear}: Heimdall's horned helm, Odin's winged one), the props in their hands, the sitting and
 * kneeling bodies lowered, Odin lying asleep, Volstagg big, and a speech bubble over whoever speaks in a talk.
 */
public class AsgardianRenderer extends LivingEntityRenderer<AsgardianEntity, PlayerModel<AsgardianEntity>> {
    private static final ResourceLocation BUBBLE = Enhanced.id("textures/entity/asgardian/bubble.png");
    private static final Map<Role, Headgear> HEADGEAR = new EnumMap<>(Role.class);

    private final AsgardianModel wide;
    private final AsgardianModel slim;

    /** Something worn on the head, drawn in the head's space (after {@code model.head.translateAndRotate}). */
    @FunctionalInterface
    public interface Headgear {
        void render(AsgardianEntity npc, PlayerModel<AsgardianEntity> model, PoseStack poseStack, MultiBufferSource buffers,
                    int packedLight, float partialTick);
    }

    /**
     * Lets another lane draw a role's headgear on its NPC (L2: Heimdall's horned helm; L3: Odin's winged helm), from its
     * client setup. The pose stack is already in the head's space; nothing is drawn while the role has none.
     */
    public static void registerHeadgear(Role role, Headgear headgear) {
        HEADGEAR.put(role, headgear);
    }

    public AsgardianRenderer(EntityRendererProvider.Context context) {
        super(context, new AsgardianModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = (AsgardianModel) model;
        this.slim = new AsgardianModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new CapeLayer(this));
        addLayer(new HelmetLayer(this, new AsgardianHelmetModel(context.bakeLayer(AsgardianHelmetModel.LAYER))));
    }

    /** The skin of a role's variant. Heimdall and Odin wear the skins of the heroes (lanes L2 and L3 draw them). */
    public static ResourceLocation texture(Role role, int variant) {
        return switch (role) {
            case HEIMDALL -> Enhanced.id("textures/entity/suit/heimdall_guardian.png");
            case ODIN -> Enhanced.id("textures/entity/suit/odin_allfather.png");
            case EINHERJAR, CITIZEN -> Enhanced.id("textures/entity/asgardian/" + role.id() + "_" + Math.floorMod(variant, role.skins()) + ".png");
            default -> Enhanced.id("textures/entity/asgardian/" + role.id() + ".png");
        };
    }

    /** The cape of a role, or null. */
    @Nullable
    public static ResourceLocation cape(Role role) {
        return switch (role) {
            case ODIN -> Enhanced.id("textures/entity/suit/odin_cape.png");
            case FRIGGA, VALKYRIE, EINHERJAR, VOLSTAGG, FANDRAL -> Enhanced.id("textures/entity/asgardian/" + role.id() + "_cape.png");
            default -> null;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(AsgardianEntity npc) {
        return texture(npc.role(), npc.variant());
    }

    @Override
    public void render(AsgardianEntity npc, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        model = npc.role().slim(npc.variant()) ? slim : wide;
        model.setAllVisible(true);
        boolean fighting = npc.state() == NpcState.FIGHTING;
        HumanoidModel.ArmPose main = npc.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        HumanoidModel.ArmPose off = npc.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY
                : fighting && npc.getOffhandItem().is(Items.SHIELD) && (npc.tickCount / 30) % 2 == 0 ? HumanoidModel.ArmPose.BLOCK
                : HumanoidModel.ArmPose.ITEM;
        model.rightArmPose = main;
        model.leftArmPose = off;
        super.render(npc, entityYaw, partialTick, poseStack, buffers, packedLight);
        if (npc.state() == NpcState.TALKING && AsgardianPoses.speaking(npc, npc.stateAge(partialTick)) && !npc.isInvisible()) {
            bubble(npc, partialTick, poseStack, buffers, packedLight);
        }
    }

    /** The speech bubble over a speaker's head: a small card that always faces the camera and bobs a little. */
    private void bubble(AsgardianEntity npc, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float age = npc.stateAge(partialTick);
        float pop = Math.min(1.0F, (age % 40.0F) / 4.0F);
        float bob = Mth.sin((npc.tickCount + partialTick) * 0.2F) * 0.03F;
        poseStack.pushPose();
        poseStack.translate(0.0F, npc.getBbHeight() + 0.62F + bob, 0.0F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        float s = 0.42F * pop;
        poseStack.scale(s, s, s);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(BUBBLE));
        vertex(vc, pose, -0.5F, -0.5F, 1.0F, 1.0F, packedLight);
        vertex(vc, pose, 0.5F, -0.5F, 0.0F, 1.0F, packedLight);
        vertex(vc, pose, 0.5F, 0.5F, 0.0F, 0.0F, packedLight);
        vertex(vc, pose, -0.5F, 0.5F, 1.0F, 0.0F, packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
        vc.addVertex(pose, x, y, 0.0F).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    protected void setupRotations(AsgardianEntity npc, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(npc, poseStack, bob, yBodyRot, partialTick, scale);
        float lower = AsgardianPoses.lower(npc, partialTick);
        if (lower > 0.0F) {
            poseStack.translate(0.0F, -lower, 0.0F);
        }
        if (npc.state() == NpcState.SLEEPING) {
            // Odin lies on his back, asleep in the Odinsleep.
            float w = AsgardianPoses.weight(npc, partialTick);
            poseStack.translate(0.0F, 0.2F * w, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F * w));
        }
    }

    /** Like a player: the same size (and Volstagg bigger, by his scale). */
    @Override
    protected void scale(AsgardianEntity npc, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    /** The named ones show their name when looked at (their custom name); the rest never. */
    @Override
    protected boolean shouldShowName(AsgardianEntity npc) {
        return npc.role().named() && super.shouldShowName(npc)
                && (npc.shouldShowName() || npc.hasCustomName() && npc == entityRenderDispatcher.crosshairPickEntity);
    }

    /** A cape swinging behind him as he walks (Odin's red, Frigga's long ivory, Valkyrie's blue, the Einherjar's red). */
    private static final class CapeLayer extends RenderLayer<AsgardianEntity, PlayerModel<AsgardianEntity>> {
        CapeLayer(RenderLayerParent<AsgardianEntity, PlayerModel<AsgardianEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, AsgardianEntity npc, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            ResourceLocation cape = cape(npc.role());
            if (cape == null || npc.isInvisible() || npc.state() == NpcState.SLEEPING) {
                return;
            }
            PlayerModel<AsgardianEntity> model = getParentModel();
            float swing = Math.min(limbSwingAmount, 1.0F);
            float sway = Mth.sin(ageInTicks * 0.067F) * 1.5F;
            float lift = 6.0F + swing * 40.0F + Mth.sin(limbSwing * 0.6662F) * 6.0F * swing + sway;
            if (npc.state() == NpcState.SITTING) {
                lift = 2.0F;
            } else if (model.crouching) {
                lift += 25.0F;
            }
            poseStack.pushPose();
            if (model.crouching) {
                poseStack.translate(0.0F, 0.2F, 0.0F);
            }
            poseStack.translate(0.0F, 0.0F, 0.125F);
            poseStack.mulPose(Axis.XP.rotationDegrees(lift));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(ageInTicks * 0.05F) * 1.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(cape));
            model.renderCloak(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    /** The Einherjar's crested helmet, and the headgear other lanes registered for their roles. */
    private static final class HelmetLayer extends RenderLayer<AsgardianEntity, PlayerModel<AsgardianEntity>> {
        private static final ResourceLocation EINHERJAR = Enhanced.id("textures/entity/asgardian/einherjar_helmet.png");
        private final AsgardianHelmetModel helmet;

        HelmetLayer(RenderLayerParent<AsgardianEntity, PlayerModel<AsgardianEntity>> parent, AsgardianHelmetModel helmet) {
            super(parent);
            this.helmet = helmet;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, AsgardianEntity npc, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (npc.isInvisible()) {
                return;
            }
            Headgear headgear = HEADGEAR.get(npc.role());
            if (npc.role() != Role.EINHERJAR && headgear == null) {
                return;
            }
            poseStack.pushPose();
            getParentModel().head.translateAndRotate(poseStack);
            if (npc.role() == Role.EINHERJAR) {
                helmet.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(EINHERJAR)), packedLight,
                        OverlayTexture.NO_OVERLAY);
            }
            if (headgear != null) {
                headgear.render(npc, getParentModel(), poseStack, buffers, packedLight, partialTick);
            }
            poseStack.popPose();
        }
    }

    /** For the talk screen: the Asgardian of an entity id on this client, or null. */
    @Nullable
    static AsgardianEntity of(@Nullable Entity entity) {
        return entity instanceof AsgardianEntity npc ? npc : null;
    }
}
