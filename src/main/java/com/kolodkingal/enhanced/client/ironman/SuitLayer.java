package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.RenderArmEvent;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.EnumMap;
import java.util.Map;

/**
 * Iron Man's suit on the player (3.8, PLAN 5.8, lane L3): each part of the worn or deployed suit ({@link
 * SuitModels#get}: lane L7's models, a box suit until then), drawn on the player model's part it rides on (the helmet
 * on the head, the chest and back on the body, each arm's and leg's pieces on that limb), posed by the suit's {@link
 * SuitAssembly} while a suit-up or a take-off plays (at the phase's progress, interpolated with the partial tick: the
 * same moment on every client, also for one who starts watching mid-phase). Then the emissive pass (eyes, reactor,
 * repulsors) while the suit works, and the thrusters' blue-white cones at the boots and palms while he flies. In first
 * person the arm's pieces are drawn on the first-person arm ({@link #onRenderArm}).
 *
 * <p>The pose of a part ({@link SuitAssembly.Pose}): translated by (x, y, z) pixels in its limb's space, turned about
 * z, y, then x (radians), scaled by {@code scale}; a part whose {@code shown} is 0 or less is not drawn. A player who
 * is not Iron Man (a builder in Creative) wearing a classic suit shows it unlit.
 */
public class SuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** One set of poses per suit, reused frame to frame (the render thread draws one player at a time). */
    private static final Map<SuitKind, SuitAssembly.Poses> POSES = new EnumMap<>(SuitKind.class);
    /** The thrusters' colours: the white-hot core, the blue body. */
    static final int CORE = 0xF2FCFF;
    static final int FLAME = 0x7FD8FF;

    public SuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    // ---- what to draw ----

    /**
     * The suit to draw on this player now (the phase's, the worn one, or a classic item on a non-Iron Man), or null.
     */
    @Nullable
    public static SuitKind drawn(AbstractClientPlayer player) {
        IronManState state = IronSuits.state(player);
        if (Powers.power(player) != com.kolodkingal.enhanced.power.Power.IRON_MAN) {
            // (Not Iron Man: only a classic suit he has on, unlit; never a state left from before.)
            SuitKind chest = IronSuits.kind(player.getItemBySlot(EquipmentSlot.CHEST));
            return chest != null && chest.classic() ? chest : null;
        }
        SuitKind kind = state.phaseKind();
        if (kind == SuitKind.HULKBUSTER || kind == null && SuitKind.of(state.suit()) == SuitKind.HULKBUSTER) {
            // (Lane L4: the Hulkbuster is the renderer swap's, HulkbusterRenderer, and its frame's in a phase.)
            return null;
        }
        if (kind == null) {
            kind = SuitKind.of(state.suit());
        }
        if (kind == null) {
            SuitKind chest = IronSuits.kind(player.getItemBySlot(EquipmentSlot.CHEST));
            if (chest != null && chest.classic()) {
                kind = chest;
            }
        }
        return kind;
    }

    /** The poses of {@code kind} at this frame (the assembly's while a phase plays; everything in place otherwise). */
    static SuitAssembly.Poses poses(AbstractClientPlayer player, SuitKind kind, float partialTick) {
        SuitAssembly.Poses poses = POSES.computeIfAbsent(kind, SuitAssembly.Poses::new);
        poses.reset();
        IronManState state = IronSuits.state(player);
        if (state.inPhase() && state.phaseKind() == kind) {
            float progress = SuitTimelines.progress(player.level().getGameTime(), state.phaseStart(),
                    state.phaseTicks(), partialTick);
            SuitAssembly.of(kind).pose(poses, progress, state.phase() == IronManState.UP, player, partialTick);
        }
        return poses;
    }

    /** Whether the suit's lights are on (it works: the synced POWERED flag). */
    static boolean lit(AbstractClientPlayer player) {
        return IronSuits.state(player).has(IronManState.POWERED);
    }

    // ---- third person ----

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        SuitKind kind = drawn(player);
        if (kind == null) {
            return;
        }
        SuitModel model = SuitModels.get(kind);
        SuitAssembly.Poses poses = poses(player, kind, partialTick);
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        PlayerModel<AbstractClientPlayer> body = getParentModel();
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(model.texture()));
        for (SuitParts part : SuitParts.VALUES) {
            drawPart(poseStack, vc, model, body, poses.get(part), part, packedLight, overlay);
        }
        boolean lit = lit(player);
        ResourceLocation glow = model.glowTexture();
        if (lit && glow != null) {
            VertexConsumer eyes = buffer.getBuffer(RenderType.eyes(glow));
            for (SuitParts part : SuitParts.VALUES) {
                drawPart(poseStack, eyes, model, body, poses.get(part), part, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY);
            }
        }
        if (lit && Powers.get(player).flying() && kind.flies()) {
            thrusters(poseStack, buffer.getBuffer(EnhancedRenderTypes.ENERGY_BEAM), body, ageInTicks,
                    kind == SuitKind.HULKBUSTER ? 1.6F : 1.0F);
        }
    }

    /** The model part a suit part rides on. */
    static ModelPart limb(PlayerModel<?> body, SuitParts part) {
        return switch (part) {
            case HELMET, FACEPLATE -> body.head;
            case CHEST, BACK -> body.body;
            case RIGHT_UPPER_ARM, RIGHT_FOREARM, RIGHT_GAUNTLET -> body.rightArm;
            case LEFT_UPPER_ARM, LEFT_FOREARM, LEFT_GAUNTLET -> body.leftArm;
            case RIGHT_THIGH, RIGHT_SHIN, RIGHT_BOOT -> body.rightLeg;
            case LEFT_THIGH, LEFT_SHIN, LEFT_BOOT -> body.leftLeg;
        };
    }

    /** One part on its limb, posed. */
    static void drawPart(PoseStack poseStack, VertexConsumer vc, SuitModel model, PlayerModel<?> body,
                         SuitAssembly.Pose pose, SuitParts part, int light, int overlay) {
        if (pose.shown <= 0.0F) {
            return;
        }
        ModelPart limb = limb(body, part);
        if (!limb.visible) {
            return;
        }
        poseStack.pushPose();
        limb.translateAndRotate(poseStack);
        applyPose(poseStack, pose);
        model.renderPart(part, poseStack, vc, light, overlay, 0xFFFFFFFF);
        poseStack.popPose();
    }

    /** The assembly's pose on top of the limb's: the offset (pixels), the turn (z, y, x), the scale. */
    static void applyPose(PoseStack poseStack, SuitAssembly.Pose pose) {
        if (pose.x != 0.0F || pose.y != 0.0F || pose.z != 0.0F) {
            poseStack.translate(pose.x / 16.0F, pose.y / 16.0F, pose.z / 16.0F);
        }
        if (pose.zRot != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(pose.zRot));
        }
        if (pose.yRot != 0.0F) {
            poseStack.mulPose(Axis.YP.rotation(pose.yRot));
        }
        if (pose.xRot != 0.0F) {
            poseStack.mulPose(Axis.XP.rotation(pose.xRot));
        }
        if (pose.scale != 1.0F) {
            float k = Math.max(0.01F, pose.scale);
            poseStack.scale(k, k, k);
        }
    }

    // ---- the thrusters ----

    /** Blue-white cones under the boots and from the palms, flickering (interpolated by the age's partial tick). */
    static void thrusters(PoseStack poseStack, VertexConsumer vc, PlayerModel<?> body, float age, float size) {
        float flicker = 0.85F + 0.15F * Mth.sin(age * 2.7F) + 0.06F * Mth.sin(age * 7.3F);
        for (ModelPart leg : new ModelPart[]{body.rightLeg, body.leftLeg}) {
            poseStack.pushPose();
            leg.translateAndRotate(poseStack);
            cone(poseStack, vc, 0.0F, 12.2F, 1.7F * size, 7.0F * size * flicker);
            poseStack.popPose();
        }
        for (ModelPart arm : new ModelPart[]{body.rightArm, body.leftArm}) {
            poseStack.pushPose();
            arm.translateAndRotate(poseStack);
            cone(poseStack, vc, arm == body.rightArm ? -1.0F : 1.0F, 10.3F, 1.1F * size, 4.0F * size * flicker);
            poseStack.popPose();
        }
    }

    /** A cone along the limb's +y from (x, y) in pixels: a bright core inside a wider blue flame. */
    private static void cone(PoseStack poseStack, VertexConsumer vc, float x, float y, float radius, float length) {
        Matrix4f m = poseStack.last().pose();
        pyramid(m, vc, x / 16.0F, y / 16.0F, radius / 16.0F, length / 16.0F, FLAME, 150);
        pyramid(m, vc, x / 16.0F, y / 16.0F, radius * 0.5F / 16.0F, length * 0.6F / 16.0F, CORE, 220);
    }

    private static void pyramid(Matrix4f m, VertexConsumer vc, float x, float y, float r, float length, int rgb,
                                int alpha) {
        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        float[][] ring = {{x - r, -r}, {x + r, -r}, {x + r, r}, {x - r, r}};
        for (int i = 0; i < 4; i++) {
            float[] a = ring[i];
            float[] b = ring[(i + 1) % 4];
            vc.addVertex(m, a[0], y, a[1]).setColor(red, green, blue, alpha);
            vc.addVertex(m, b[0], y, b[1]).setColor(red, green, blue, alpha);
            vc.addVertex(m, x, y + length, 0.0F).setColor(red, green, blue, 0);
            vc.addVertex(m, x, y + length, 0.0F).setColor(red, green, blue, 0);
        }
    }

    // ---- first person ----

    /**
     * His arm in first person: drawn as the game draws it, then the suit's pieces of that arm on it (posed as in third
     * person, so the suit-up plays on his own arms too). Not for a player without a suit.
     */
    public static void onRenderArm(RenderArmEvent event) {
        if (event.isCanceled()) {
            return;
        }
        AbstractClientPlayer player = event.getPlayer();
        SuitKind kind = drawn(player);
        if (kind == null || player.isInvisible()) {
            return;
        }
        EntityRenderer<? super AbstractClientPlayer> any = Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(player);
        if (!(any instanceof PlayerRenderer renderer)) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        // (As PlayerRenderer.renderHand draws it.)
        model.attackTime = 0.0F;
        model.crouching = false;
        model.swimAmount = 0.0F;
        model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        sleeve.visible = player.isModelPartShown(right ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE);
        arm.xRot = 0.0F;
        ResourceLocation skin = player.getSkin().texture();
        arm.render(poseStack, buffers.getBuffer(RenderType.entitySolid(skin)), light, OverlayTexture.NO_OVERLAY);
        sleeve.xRot = 0.0F;
        sleeve.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(skin)), light,
                OverlayTexture.NO_OVERLAY);
        SuitModel suit = SuitModels.get(kind);
        SuitAssembly.Poses poses = poses(player, kind, partialTick);
        SuitParts[] pieces = right ? new SuitParts[]{SuitParts.RIGHT_UPPER_ARM, SuitParts.RIGHT_FOREARM,
                SuitParts.RIGHT_GAUNTLET} : new SuitParts[]{SuitParts.LEFT_UPPER_ARM, SuitParts.LEFT_FOREARM,
                SuitParts.LEFT_GAUNTLET};
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(suit.texture()));
        for (SuitParts part : pieces) {
            drawPart(poseStack, vc, suit, model, poses.get(part), part, light, OverlayTexture.NO_OVERLAY);
        }
        ResourceLocation glow = suit.glowTexture();
        if (lit(player) && glow != null) {
            VertexConsumer eyes = buffers.getBuffer(RenderType.eyes(glow));
            for (SuitParts part : pieces) {
                drawPart(poseStack, eyes, suit, model, poses.get(part), part, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY);
            }
        }
        event.setCanceled(true);
    }

    /** The first-person hook (before the claws', at their LOW: an Iron Man has none). */
    @Subscriber(side = Side.CLIENT)
    public static final class Hooks {
        @Subscribe
        public static void onRenderArm(RenderArmEvent event) {
            SuitLayer.onRenderArm(event);
        }

        private Hooks() {
        }
    }
}
