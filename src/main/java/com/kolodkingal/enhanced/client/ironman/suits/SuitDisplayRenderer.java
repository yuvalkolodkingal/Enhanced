package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.block.SuitDisplayBlock;
import com.kolodkingal.enhanced.block.SuitDisplayBlockEntity;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * A suit display of Iron Man's workshop (3.8, PLAN 7.1, 9.3): the suit it shows standing on its plinth (a steel post
 * holds it up), its eyes and reactor slowly breathing light, a light ring round the plinth; behind it the workshop's
 * gantry, whose robot arms bolt a Mark 3 onto Iron Man when he suits up within reach ({@link GantryArms}). The
 * Hulkbuster stands at 1.75x in its bay, without a gantry. The block's own model (the tower lane's) is the plinth and
 * the post.
 */
public class SuitDisplayRenderer implements BlockEntityRenderer<SuitDisplayBlockEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    /**
     * The suit stands on the plinth (the block's model: 2.5 pixels), a little smaller than life so it fits in the
     * display's two blocks.
     */
    private static final float PLINTH = 2.5F / 16.0F;
    private static final float SUIT_SCALE = 0.92F;
    private static final float FORWARD = 2.0F / 16.0F;
    private static final float HULKBUSTER_SCALE = 1.75F;
    /**
     * Which suit a display shows: the block entity's (lane L6's {@code SuitDisplayBlockEntity}, a public method
     * returning a {@code SuitKind}, found once); until it says, the display's column picks one of the six (so a row of
     * displays shows them all).
     */
    public static Function<SuitDisplayBlockEntity, SuitKind> KIND_OF = SuitDisplayRenderer::kindOf;
    @Nullable
    private static Method kindMethod;
    private static boolean kindLooked;

    private final HumanoidModel<LivingEntity> mannequin;
    private final HulkbusterModel<LivingEntity> hulkbuster;
    private final GantryArms gantry;

    public SuitDisplayRenderer(BlockEntityRendererProvider.Context context) {
        this.mannequin = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER));
        this.hulkbuster = new HulkbusterModel<>(context.bakeLayer(HulkbusterModel.LAYER));
        this.gantry = new GantryArms(context.bakeLayer(SuitArt.GANTRY));
    }

    @Override
    public void render(SuitDisplayBlockEntity display, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = display.getBlockState();
        if (!(state.getBlock() instanceof SuitDisplayBlock)) {
            return;
        }
        SuitKind kind = KIND_OF.apply(display);
        if (kind == null) {
            return;
        }
        Direction facing = state.getValue(SuitDisplayBlock.FACING);
        long gameTime = display.getLevel() == null ? 0L : display.getLevel().getGameTime();
        float time = gameTime + partialTick;
        int lightAbove = display.getLevel() == null ? light
                : net.minecraft.client.renderer.LevelRenderer.getLightColor(display.getLevel(),
                display.getBlockPos().above());
        int lit = Math.max(light, lightAbove);
        boolean hulk = kind == SuitKind.HULKBUSTER;
        float scale = hulk ? HULKBUSTER_SCALE : SUIT_SCALE;

        poseStack.pushPose();
        poseStack.translate(0.5F, PLINTH, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        if (!hulk) {
            Matrix4f toModel = modelToWorld(display.getBlockPos(), facing).invert();
            Player served = display.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel level
                    ? SuitFx.servedBy(level, display.getBlockPos()) : null;
            gantry.render(poseStack, buffers, lit, overlay, toModel, served, partialTick, time);
        }
        poseStack.pushPose();
        // (a little forward of the plinth's back pole, which then holds it from behind)
        poseStack.translate(0.0F, 1.5F, hulk ? 0.0F : -FORWARD);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, -1.5F, 0.0F);
        SuitLook look = SuitLook.still(kind, time);
        look.lit = 0.8F + 0.2F * Mth.sin(time * 0.05F);
        HumanoidModel<?> body;
        if (hulk) {
            hulkbuster.rest(0.0F);
            body = hulkbuster;
        } else {
            for (var part : new net.minecraft.client.model.geom.ModelPart[] {mannequin.head, mannequin.hat,
                    mannequin.body, mannequin.rightArm, mannequin.leftArm, mannequin.rightLeg, mannequin.leftLeg}) {
                part.resetPose();
            }
            mannequin.rightArm.zRot = 0.12F;
            mannequin.leftArm.zRot = -0.12F;
            body = mannequin;
        }
        SuitRender.drawSuit(poseStack, buffers, lit, OverlayTexture.NO_OVERLAY, body, kind, look);
        poseStack.popPose();
        ring(poseStack, buffers, hulk ? 11.0F : 7.0F, time);
        poseStack.popPose();
    }

    /** The light ring round the plinth's top edge, slowly breathing. */
    private static void ring(PoseStack poseStack, MultiBufferSource buffers, float radius, float time) {
        VertexConsumer vc = buffers.getBuffer(RenderType.eyes(WHITE));
        Matrix4f m = poseStack.last().pose();
        PoseStack.Pose normal = poseStack.last();
        float glow = 0.55F + 0.35F * Mth.sin(time * 0.05F);
        int g = Math.round(200 * glow);
        int b = Math.round(255 * glow);
        int r = Math.round(70 * glow);
        float y = 24.0F / 16.0F - 0.002F;
        float r0 = radius / 16.0F;
        float r1 = (radius - 0.8F) / 16.0F;
        int sides = 24;
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides;
            float a1 = Mth.TWO_PI * (i + 1) / sides;
            float[][] q = {{r0 * Mth.cos(a0), r0 * Mth.sin(a0)}, {r0 * Mth.cos(a1), r0 * Mth.sin(a1)},
                    {r1 * Mth.cos(a1), r1 * Mth.sin(a1)}, {r1 * Mth.cos(a0), r1 * Mth.sin(a0)}};
            for (int k = 0; k < 4; k++) {
                vc.addVertex(m, q[k][0], y, q[k][1]).setColor(r, g, b, 255).setUv(0.5F, 0.5F)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(normal, 0, -1, 0);
            }
            for (int k = 3; k >= 0; k--) {
                vc.addVertex(m, q[k][0], y, q[k][1]).setColor(r, g, b, 255).setUv(0.5F, 0.5F)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(normal, 0, 1, 0);
            }
        }
    }

    /** The display's model space (pixels) to the world, as {@link #render} sets it up. */
    private static Matrix4f modelToWorld(BlockPos pos, Direction facing) {
        return new Matrix4f()
                .translate(pos.getX() + 0.5F, pos.getY() + PLINTH, pos.getZ() + 0.5F)
                .rotateY((180.0F - facing.toYRot()) * Mth.DEG_TO_RAD)
                .scale(-1.0F, -1.0F, 1.0F)
                .translate(0.0F, -1.501F, 0.0F)
                .scale(1.0F / 16.0F);
    }

    @Override
    public AABB getRenderBoundingBox(SuitDisplayBlockEntity display) {
        BlockPos pos = display.getBlockPos();
        return new AABB(pos).inflate(5.0, 0.0, 5.0).expandTowards(0.0, 4.0, 0.0);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    /** The block entity's suit (lane L6's accessor, found once), else the column's. */
    private static SuitKind kindOf(SuitDisplayBlockEntity display) {
        if (!kindLooked) {
            kindLooked = true;
            for (String name : new String[] {"kind", "suitKind", "shown", "suit", "getKind", "getSuit"}) {
                try {
                    Method m = display.getClass().getMethod(name);
                    if (m.getReturnType() == SuitKind.class) {
                        kindMethod = m;
                        break;
                    }
                } catch (NoSuchMethodException ignored) {
                    // not this name
                }
            }
        }
        if (kindMethod != null) {
            try {
                return (SuitKind) kindMethod.invoke(display);
            } catch (ReflectiveOperationException e) {
                kindMethod = null;
            }
        }
        BlockPos pos = display.getBlockPos();
        return SuitKind.VALUES[Math.floorMod(pos.getX() + pos.getZ(), SuitKind.VALUES.length)];
    }
}
