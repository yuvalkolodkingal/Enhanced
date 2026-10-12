package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.client.render.ApocalypseCubes.Cube;
import com.kolodkingal.enhanced.client.render.ApocalypseCubes.Pivot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * En Sabah Nur in the world (DESIGN section 17): the model of {@link ApocalypseModel} at 1.35 times its size (about three
 * blocks tall with the cap), or 1.2 once his armour has fallen, with the glow of {@link ApocalypseEyesLayer} over it.
 * He never falls over when he dies (he kneels and comes apart), has no name tag, and when the third shield breaks the seven
 * plates of his armour (the collar, the two pauldrons, the chest plate, the two gauntlets, the crown of the cap) fly off
 * him and fade: the plates are the model's own cubes, drawn on their own. An invisible boss (while he is away in the
 * Escape) is drawn as nothing.
 */
public class ApocalypseRenderer extends MobRenderer<ApocalypseEntity, ApocalypseModel> {
    public static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/apocalypse_boss.png");
    public static final ResourceLocation TEXTURE_TRUE = Enhanced.id("textures/entity/apocalypse_boss_true.png");

    /** The cubes that fly off, by name ({@code _l} for the left side), and how long they fly. */
    private static final String[] PLATES = {"collar_low", "pauldron_a", "pauldron_a_l", "chest_plate", "gauntlet", "gauntlet_l", "crest"};
    public static final float PLATE_LIFE = 40.0F;

    private final BlockRenderDispatcher blocks;

    public ApocalypseRenderer(EntityRendererProvider.Context context) {
        super(context, new ApocalypseModel(context.bakeLayer(ApocalypseModel.LAYER)), 1.1F);
        this.blocks = context.getBlockRenderDispatcher();
        addLayer(new ApocalypseEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ApocalypseEntity boss) {
        return ApocalypseModel.armoured(boss, boss.state(), boss.stateTime(0.0F)) ? TEXTURE : TEXTURE_TRUE;
    }

    /** How big he is drawn: 1.35, or 1.2 once the plates are off (eased over the six ticks that follow). */
    public static float sizeOf(ApocalypseEntity boss, float time) {
        if (!boss.isTrueForm()) {
            return ApocalypseCubes.SCALE_ARMOURED;
        }
        if (boss.state() == ApocalypseState.ARMOUR_FALL) {
            return Mth.lerp(AnimMath.ss(ApocalypseModel.PLATES_FLY_AT, ApocalypseModel.PLATES_FLY_AT + 6.0F, time), ApocalypseCubes.SCALE_ARMOURED,
                    ApocalypseCubes.SCALE_TRUE);
        }
        return ApocalypseCubes.SCALE_TRUE;
    }

    @Override
    protected void scale(ApocalypseEntity boss, PoseStack poseStack, float partialTick) {
        float size = sizeOf(boss, boss.stateTime(partialTick));
        poseStack.scale(size, size, size);
    }

    /** He kneels and comes apart; he does not fall on his side. */
    @Override
    protected float getFlipDegrees(ApocalypseEntity boss) {
        return 0.0F;
    }

    @Override
    protected boolean isShaking(ApocalypseEntity boss) {
        return false;
    }

    @Override
    protected boolean shouldShowName(ApocalypseEntity boss) {
        return false;
    }

    @Override
    public void render(ApocalypseEntity boss, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(boss, entityYaw, partialTicks, poseStack, buffer, packedLight);
        if (boss.state() == ApocalypseState.TELL_BOULDERS && !boss.isInvisible()) {
            orbit(boss, boss.stateTime(partialTicks), poseStack, buffer);
        }
        if (boss.state() == ApocalypseState.ARMOUR_FALL && !boss.isInvisible()) {
            float fly = boss.stateTime(partialTicks) - ApocalypseModel.PLATES_FLY_AT;
            if (fly >= 0.0F && fly < PLATE_LIFE) {
                plates(boss, fly, partialTicks, poseStack, buffer, packedLight);
            }
        }
    }

    /**
     * The three blocks of sand he tears out of the ground in the Boulder Barrage: they rise from his feet and orbit over his
     * head faster and faster, and at the end of the tell each is where the server throws its boulder from ({@link
     * SandMath#boulder}). They are the block of sand, drawn full-bright.
     */
    private void orbit(ApocalypseEntity boss, float time, PoseStack poseStack, MultiBufferSource buffer) {
        for (int i = 0; i < 3; i++) {
            Vec3 at = SandMath.boulder(Vec3.ZERO, boss.getBbHeight(), time, i);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 17.0F + i * 40.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(time * 23.0F));
            poseStack.scale(0.8F, 0.8F, 0.8F);
            poseStack.translate(-0.5, -0.5, -0.5);
            blocks.renderSingleBlock(Blocks.SAND.defaultBlockState(), poseStack, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    /**
     * The seven plates of the armour, flung outward and up from where they were on his body, spinning, falling, and
     * shrinking away over the last eight ticks of their flight. Each is the model's own cube, drawn alone.
     */
    private void plates(ApocalypseEntity boss, float fly, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float yaw = Mth.rotLerp(partialTicks, boss.yBodyRotO, boss.yBodyRot);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        float life = 1.0F - AnimMath.ss(PLATE_LIFE - 8.0F, PLATE_LIFE, fly);
        float rad = Mth.DEG_TO_RAD * yaw;
        float size = ApocalypseCubes.SCALE_ARMOURED;
        for (int i = 0; i < PLATES.length; i++) {
            String name = PLATES[i];
            boolean left = name.endsWith("_l");
            Cube cube = cubeOf(left ? name.substring(0, name.length() - 2) : name);
            ModelPart part = getModel().cube(name);
            if (cube == null || part == null) {
                continue;
            }
            Pivot pivot = left ? mirror(cube.pivot()) : cube.pivot();
            float cx = left ? ApocalypseCubes.mirroredX(cube.x(), cube.w()) + cube.w() * 0.5F : cube.x() + cube.w() * 0.5F;
            float cy = cube.y() + cube.h() * 0.5F;
            float cz = cube.z() + cube.d() * 0.5F;
            // Where it is on his body (the model's x is his left, z his back), and outward from the middle of him.
            float bodyX = pivot.x + cx;
            float bodyY = pivot.y + cy;
            float bodyZ = pivot.z + cz;
            float outX = -bodyX;
            float outZ = -bodyZ;
            float length = Math.max(0.01F, Mth.sqrt(outX * outX + outZ * outZ));
            float speed = 0.22F + 0.05F * (i % 3);
            // (Local right/forward to the world: forward = (-sin yaw, cos yaw), right = (-cos yaw, -sin yaw).)
            float localRight = outX / length * speed;
            float localForward = outZ / length * speed;
            float vx = -Mth.sin(rad) * localForward - Mth.cos(rad) * localRight;
            float vz = Mth.cos(rad) * localForward - Mth.sin(rad) * localRight;
            float vy = 0.28F + 0.06F * (i % 4);
            float dx = vx * fly;
            float dz = vz * fly;
            float dy = vy * fly - 0.5F * 0.045F * fly * fly;
            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.scale(size * life, size * life, size * life);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            poseStack.translate(pivot.x / 16.0F, pivot.y / 16.0F, pivot.z / 16.0F);
            poseStack.translate(cx / 16.0F, cy / 16.0F, cz / 16.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(fly * (9.0F + 3.0F * i)));
            poseStack.mulPose(Axis.YP.rotationDegrees(fly * (6.0F - 2.0F * (i % 4))));
            poseStack.mulPose(Axis.ZP.rotationDegrees(fly * (4.0F + 5.0F * (i % 3))));
            poseStack.translate(-cx / 16.0F, -cy / 16.0F, -cz / 16.0F);
            boolean was = part.visible;
            part.visible = true;
            part.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY);
            part.visible = was;
            poseStack.popPose();
        }
    }

    private static Cube cubeOf(String name) {
        for (Cube cube : ApocalypseCubes.ALL) {
            if (cube.name().equals(name)) {
                return cube;
            }
        }
        return null;
    }

    private static Pivot mirror(Pivot pivot) {
        return switch (pivot) {
            case RIGHT_ARM -> Pivot.LEFT_ARM;
            case LEFT_ARM -> Pivot.RIGHT_ARM;
            case RIGHT_LEG -> Pivot.LEFT_LEG;
            case LEFT_LEG -> Pivot.RIGHT_LEG;
            default -> pivot;
        };
    }

    /** Forgets every pose being blended (leaving the world). */
    public void clear() {
        getModel().clear();
    }
}
