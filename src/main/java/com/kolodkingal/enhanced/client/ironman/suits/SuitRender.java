package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.client.ironman.SuitAssembly;
import com.kolodkingal.enhanced.client.ironman.SuitModel;
import com.kolodkingal.enhanced.client.ironman.SuitModels;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws Iron Man's suits (3.8, PLAN 7): every part of a suit over a humanoid skeleton (the player's model, a display's
 * mannequin, the Hulkbuster's own frame), posed by the suit's {@link SuitAssembly} while a phase plays, in three passes
 * (the armour; the emissive eyes, reactor, repulsors and lights; the nanites' glowing film while a nanotech suit
 * forms), then the energy shapes (the Mark 85's hexagonal shield) and the Mark 5's suitcase: {@link #drawSuit} for
 * whole suits (the displays, the Hulkbuster's frame, the Hulkbuster lane's renderer swap), {@link #drawArm} for one
 * arm, {@link #drawExtras} for a caller that drew the parts itself (the player layer). One that draws parts through
 * the plain {@code SuitModel} call gets the same look from {@link SuitRenderContext}. Everything is interpolated with
 * the partial tick.
 */
public final class SuitRender {
    private static final Map<SuitKind, SuitAssembly.Poses> POSES = new EnumMap<>(SuitKind.class);
    private static final SuitParts[] RIGHT_ARM = {SuitParts.RIGHT_UPPER_ARM, SuitParts.RIGHT_FOREARM,
            SuitParts.RIGHT_GAUNTLET};
    private static final SuitParts[] LEFT_ARM = {SuitParts.LEFT_UPPER_ARM, SuitParts.LEFT_FOREARM,
            SuitParts.LEFT_GAUNTLET};
    /** The nanites' film: a warm glow over a segment while it forms. */
    private static final int FILM = 0xFFB89A5A;

    /**
     * The suit a player shows: the phase's suit while a phase plays, else the one he wears or has deployed ({@link
     * IronSuits#worn}), or null.
     */
    @Nullable
    public static SuitKind shownSuit(Player player) {
        IronManState state = IronSuits.state(player);
        if (state.inPhase() && state.phaseKind() != null) {
            return state.phaseKind();
        }
        return IronSuits.worn(player);
    }

    /** Every part of a suit over a humanoid skeleton, as the look says, and its extras ({@link #drawExtras}). */
    public static void drawSuit(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                                HumanoidModel<?> body, SuitKind kind, SuitLook look) {
        draw(poseStack, buffers, light, overlay, body, kind, look, SuitParts.VALUES);
        drawExtras(poseStack, buffers, light, overlay, body, look);
    }

    /**
     * What a suit shows beside its parts, for a caller that drew the parts itself (the player layer): the nanites'
     * glowing film while a nanotech suit forms, the Mark 5's suitcase while it unfolds, the Mark 85's hexagonal shield.
     */
    public static void drawExtras(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                                  HumanoidModel<?> body, SuitLook look) {
        SuitKind kind = look.kind;
        if (!(SuitModels.get(kind) instanceof SuitArtModel model)) {
            return;
        }
        if (look.inPhase && kind.nanotech()) {
            SuitAssembly.Poses poses = posed(kind, look);
            pass(poseStack, buffers.getBuffer(RenderType.eyes(model.texture())), light, overlay, body, model, look,
                    poses, SuitParts.VALUES, FILM, true);
        }
        if (kind == SuitKind.MARK_5 && look.inPhase) {
            Suitcase.drawAtFeet(poseStack, buffers, light, overlay, look);
        }
        if (kind == SuitKind.MARK_85 && look.shield > 0.01F && SuitArt.ENERGY_GEAR) {
            EnergyShapes.hexShield(poseStack, buffers, body, look);
        }
    }

    /** The poses of a suit at the look's moment (reused frame to frame). */
    private static SuitAssembly.Poses posed(SuitKind kind, SuitLook look) {
        SuitAssembly.Poses poses = POSES.computeIfAbsent(kind, SuitAssembly.Poses::new);
        poses.reset();
        if (look.inPhase) {
            SuitAssembly.of(kind).pose(poses, look.progress, look.up, look.player, look.partialTick);
        }
        return poses;
    }

    /** One arm's pieces (first person, or a caller drawing an arm alone). */
    public static void drawArm(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                               HumanoidModel<?> body, boolean right, SuitLook look) {
        draw(poseStack, buffers, light, overlay, body, look.kind, look, right ? RIGHT_ARM : LEFT_ARM);
    }

    /**
     * One arm of a suit alone and still, lit, at the current origin in the space of the skeleton's arm (Veronica's
     * falling arm of the Hulkbuster: turn and place the pose stack first; pixels as a model part's).
     */
    public static void drawLoneArm(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                                   SuitKind kind, boolean right) {
        if (!(SuitModels.get(kind) instanceof SuitArtModel model)) {
            return;
        }
        SuitParts[] parts = right ? RIGHT_ARM : LEFT_ARM;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture()));
        for (SuitParts part : parts) {
            model.renderStill(part, poseStack, vc, light, overlay, -1);
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(model.glowTexture()));
        for (SuitParts part : parts) {
            model.renderStill(part, poseStack, glow, light, overlay, -1);
        }
    }

    private static void draw(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                             HumanoidModel<?> body, SuitKind kind, SuitLook look, SuitParts[] parts) {
        SuitModel any = SuitModels.get(kind);
        SuitAssembly.Poses poses = posed(kind, look);
        if (!(any instanceof SuitArtModel model)) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(any.texture()));
            for (SuitParts part : parts) {
                SuitAssembly.Pose pose = poses.get(part);
                if (pose.shown > 0.0F) {
                    poseStack.pushPose();
                    place(poseStack, body, part, pose, look.slim);
                    any.renderPart(part, poseStack, vc, light, overlay, -1);
                    poseStack.popPose();
                }
            }
            return;
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(model.texture()));
        pass(poseStack, vc, light, overlay, body, model, look, poses, parts, -1, false);
        if (look.lit > 0.01F) {
            int g = Math.round(255 * Math.min(1.0F, look.lit));
            int glow = 0xFF000000 | g << 16 | g << 8 | g;
            pass(poseStack, buffers.getBuffer(RenderType.eyes(model.glowTexture())), light, overlay, body, model, look,
                    poses, parts, glow, false);
        }
    }

    private static void pass(PoseStack poseStack, VertexConsumer vc, int light, int overlay, HumanoidModel<?> body,
                             SuitArtModel model, SuitLook look, SuitAssembly.Poses poses, SuitParts[] parts,
                             int color, boolean film) {
        for (SuitParts part : parts) {
            SuitAssembly.Pose pose = poses.get(part);
            if (pose.shown <= 0.0F) {
                continue;
            }
            poseStack.pushPose();
            place(poseStack, body, part, pose, look.slim);
            model.renderPart(part, look, poseStack, vc, light, overlay, color, film);
            poseStack.popPose();
        }
    }

    /** Into a part's space: the skeleton's part it rides on, the slim arm's shift, then the choreography's pose. */
    static void place(PoseStack poseStack, HumanoidModel<?> body, SuitParts part, SuitAssembly.Pose pose,
                      boolean slim) {
        anchor(body, part).translateAndRotate(poseStack);
        if (slim && arm(part)) {
            slimShift(poseStack, part);
        }
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
            poseStack.scale(pose.scale, pose.scale, pose.scale);
        }
    }

    /** Whether a part rides on an arm. */
    static boolean arm(SuitParts part) {
        String n = part.name();
        return n.endsWith("ARM") || n.endsWith("GAUNTLET");
    }

    /** A slim (three-pixel) arm: the suit's arm pieces half a pixel in and a little narrower. */
    static void slimShift(PoseStack poseStack, SuitParts part) {
        poseStack.translate((part.right() ? 0.5F : -0.5F) / 16.0F, 0.0F, 0.0F);
        poseStack.scale(0.82F, 1.0F, 1.0F);
    }

    /** The skeleton's part a suit part rides on. */
    static ModelPart anchor(HumanoidModel<?> body, SuitParts part) {
        return switch (part) {
            case HELMET, FACEPLATE -> body.head;
            case CHEST, BACK -> body.body;
            case RIGHT_UPPER_ARM, RIGHT_FOREARM, RIGHT_GAUNTLET -> body.rightArm;
            case LEFT_UPPER_ARM, LEFT_FOREARM, LEFT_GAUNTLET -> body.leftArm;
            case RIGHT_THIGH, RIGHT_SHIN, RIGHT_BOOT -> body.rightLeg;
            case LEFT_THIGH, LEFT_SHIN, LEFT_BOOT -> body.leftLeg;
        };
    }

    /** Whether the player's skin is the slim one (the suit's arms shift in by half a pixel). */
    static boolean slim(AbstractClientPlayer player) {
        return player.getSkin().model() == PlayerSkin.Model.SLIM;
    }

    /** Whether the part shows on the timeline at all (a caller placing things on a part may skip it). */
    public static boolean shown(SuitKind kind, SuitParts part, SuitLook look) {
        return !look.inPhase || SuitTimelines.reveal(kind, part, look.progress, look.up) > 0.0F;
    }

    private SuitRender() {
    }
}
