package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * The Iron Spider's four mechanical legs (DESIGN section 14): gold-and-black, three rigid segments each, unfolding from
 * the gold circle on his back. Drawn on the body's own pose, so they follow every lean, flip and offset of it.
 *
 * <p>Each leg's tip is where the situation wants it, and {@link LegIk} finds the joints: at rest (out, arched over the
 * shoulders and spread, swaying a little); planted on the wall he climbs, on the ceiling he hangs from, or on the
 * ground he walks on, stepping with the crawl or the walk; or striking at a creature, the tip flying out to it in three
 * ticks and back in five. They unfold from lying flat along his spine in fourteen ticks (with a little overshoot) and
 * fold again in eight.
 *
 * <p>Two legs are on each side: the upper pair 3 pixels down from the top of the body, the lower pair 8, both on the
 * back. Legs are numbered as the server's {@code LEG_STRIKE}: upper left, upper right, lower left, lower right.
 */
public class SpiderLegsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = Enhanced.id("textures/entity/spider_legs.png");
    /** The lengths of the three segments, in pixels of the model. */
    private static final double L1 = 9.0;
    private static final double L2 = 10.0;
    private static final double L3 = 7.0;
    /** A pixel of the model in blocks of the pose it is drawn in. */
    private static final double PX = 1.0 / 16.0;
    /** Ticks to unfold and to fold. */
    private static final float UNFOLD_TICKS = 14.0F;
    private static final float FOLD_TICKS = 8.0F;
    /** A strike: the tip goes to its target in this many ticks and back in this many. */
    private static final float STRIKE_OUT = 3.0F;
    private static final float STRIKE_BACK = 5.0F;

    /** Where the roots are on the back, in pixels of the body: x to his left, y down from the top, z to the back. */
    private static final Vec3[] ROOTS = {
            new Vec3(3, 3, 2.5), new Vec3(-3, 3, 2.5), new Vec3(3, 8, 2.5), new Vec3(-3, 8, 2.5)
    };
    /** Lying flat along the spine: the tips near the roots, along the back. */
    private static final Vec3[] FOLDED = {
            new Vec3(3, -9, 3.5), new Vec3(-3, -9, 3.5), new Vec3(3, 20, 3.5), new Vec3(-3, 20, 3.5)
    };
    /** At rest, out: the upper legs arch up and over the shoulders, the lower reach out and a little down. */
    private static final Vec3[] REST = {
            new Vec3(13, -6, -3), new Vec3(-13, -6, -3), new Vec3(22, 14, 6), new Vec3(-22, 14, 6)
    };
    /** Which way each knee bulges (the legs bend up and back). */
    private static final Vec3[] BEND = {
            new Vec3(0.4, -1, 0.7), new Vec3(-0.4, -1, 0.7), new Vec3(0.2, -1, 0.5), new Vec3(-0.2, -1, 0.5)
    };

    /** What a leg remembers between frames. */
    private static final class State {
        float unfold;
        float plant;
        float age = Float.NaN;
    }

    private static final Map<Integer, State> STATES = new HashMap<>();

    private final ModelPart upper = segment((int) L1, 0);
    private final ModelPart middle = segment((int) L2, 8);
    private final ModelPart tip = segment((int) L3, 16);

    public SpiderLegsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /**
     * A segment: a box 2 by 2 pixels across and {@code length} long, standing on +y, textured at (u, 0) of the 32 pixel
     * sheet.
     */
    private static ModelPart segment(int length, int u) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("segment", CubeListBuilder.create().texOffs(u, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, length, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot().getChild("segment");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        // (3.4) Spider-Man, or a custom hero with his legs (in any look).
        if (!data.has(HeroAbility.SPIDER_LEGS) || player.isInvisible()) {
            return;
        }
        State state = STATES.computeIfAbsent(player.getId(), id -> new State());
        boolean out = data.toggle(Toggle.SPIDER_LEGS);
        float dt = Float.isNaN(state.age) ? 0.0F : Mth.clamp(ageInTicks - state.age, 0.0F, 3.0F);
        state.age = ageInTicks;
        state.unfold = out ? Math.min(1.0F, state.unfold + dt / UNFOLD_TICKS) : Math.max(0.0F, state.unfold - dt / FOLD_TICKS);
        if (state.unfold <= 0.0F) {
            return;
        }
        float reach = out ? AnimMath.easeOutBack(state.unfold) : state.unfold;

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        Matrix4f toWorld = new Matrix4f(poseStack.last().pose());
        Matrix4f toModel = new Matrix4f(toWorld).invert();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Plane plane = plane(player, data, partialTicks);
        HeroAnimState anim = HeroAnimator.peek(player);
        float phase = anim != null ? anim.crawlPhase : 0.0F;
        float amp = anim != null ? anim.crawlAmp : 0.0F;
        state.plant = AnimMath.ease(state.plant, plane != null ? 1.0F : 0.0F, dt, 4.0F);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        for (int leg = 0; leg < 4; leg++) {
            Vec3 target = rest(leg, reach, ageInTicks);
            if (plane != null && state.plant > 0.001F) {
                Vec3 planted = planted(plane, leg, toWorld, toModel, camera, phase, amp, limbSwing, limbSwingAmount);
                target = target.lerp(planted, state.plant * reach);
            }
            SpiderEffects.Strike strike = SpiderEffects.strike(player.getId(), leg);
            if (strike != null) {
                float since = (player.level().getGameTime() - strike.born()) + partialTicks;
                float p = since < STRIKE_OUT ? AnimMath.easeOutCubic(since / STRIKE_OUT) : Math.max(0.0F, 1.0F - (since - STRIKE_OUT) / STRIKE_BACK);
                target = target.lerp(toModelPoint(toModel, camera, strike.to()), p);
            }
            LegIk.Pose pose = LegIk.solve(ROOTS[leg], target, BEND[leg], L1, L2, L3);
            if (!finite(pose)) {
                continue;
            }
            draw(poseStack, vc, upper, ROOTS[leg], pose.knee(), packedLight, overlay);
            draw(poseStack, vc, middle, pose.knee(), pose.ankle(), packedLight, overlay);
            draw(poseStack, vc, tip, pose.ankle(), pose.tip(), packedLight, overlay);
        }
        poseStack.popPose();
    }

    /** Where the tip rests: between lying along the spine and out, swaying a little. */
    private static Vec3 rest(int leg, float reach, float age) {
        Vec3 sway = new Vec3(0, 0.8 * Mth.sin(0.07F * age + leg * 1.3F), 0.8 * Mth.cos(0.07F * age + leg * 1.7F));
        return FOLDED[leg].lerp(REST[leg].add(sway), reach);
    }

    // ---- planting ----

    /** A surface the legs plant on, in the world: a point on it and its normal (towards him). */
    private record Plane(Vec3 point, Vec3 normal, Kind kind) {
    }

    private enum Kind {
        WALL, CEILING, GROUND
    }

    /**
     * The surface under his hands and feet right now: the wall, the ceiling, the ground he walks on, or none (in the
     * air).
     */
    private static Plane plane(AbstractClientPlayer player, PowerData data, float partialTicks) {
        Vec3 feet = player.getPosition(partialTicks);
        if (data.is(Live.CLINGING_WALL)) {
            HeroAnimState anim = HeroAnimator.peek(player);
            Vec3 toward = anim != null && anim.wallDir.lengthSqr() > 0.5 ? anim.wallDir : null;
            if (toward == null) {
                return null;
            }
            Vec3 centre = feet.add(0, player.getBbHeight() * 0.5, 0);
            BlockHitResult hit = ((ClientLevel) player.level()).clip(new ClipContext(centre, centre.add(toward), ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player));
            double distance = hit.getType() == HitResult.Type.BLOCK ? centre.distanceTo(hit.getLocation()) : 0.4;
            return new Plane(centre.add(toward.scale(distance)), toward.scale(-1), Kind.WALL);
        }
        if (data.is(Live.CLINGING_CEILING)) {
            return new Plane(feet.add(0, player.getBbHeight() + 0.02, 0), new Vec3(0, -1, 0), Kind.CEILING);
        }
        if (player.onGround() && !player.isInWaterOrBubble()) {
            return new Plane(feet.add(0, 0.01, 0), new Vec3(0, 1, 0), Kind.GROUND);
        }
        return null;
    }

    /**
     * Where a leg's tip goes to touch the surface: a point out beside him in the body's own frame, taken to the world,
     * brought onto the surface along its normal, and taken back. The legs of a diagonal pair step together: the ones
     * whose turn it is lift off the surface a little.
     */
    private static Vec3 planted(Plane plane, int leg, Matrix4f toWorld, Matrix4f toModel, Vec3 camera, float phase, float amp, float limbSwing,
                                float limbSwingAmount) {
        boolean left = leg % 2 == 0;
        boolean upper = leg < 2;
        double side = left ? 1.0 : -1.0;
        boolean pair = upper == left;
        double cycle;
        double stride;
        Vec3 nominal;
        switch (plane.kind()) {
            case WALL -> {
                cycle = Mth.sin(phase + (pair ? 0.0F : Mth.PI)) * amp;
                nominal = new Vec3(12.0 * side, upper ? -2.5 : 15.0, 0.0);
                stride = 0.0;
            }
            case CEILING -> {
                cycle = Mth.sin(limbSwing * 0.6662F + (pair ? 0.0F : Mth.PI)) * limbSwingAmount;
                nominal = new Vec3(11.0 * side, 22.0, upper ? 6.0 : 10.0);
                stride = 2.0 * Mth.cos(limbSwing * 0.6662F + (pair ? 0.0F : Mth.PI)) * limbSwingAmount;
            }
            default -> {
                cycle = Mth.sin(limbSwing * 0.6662F + (pair ? 0.0F : Mth.PI)) * limbSwingAmount;
                nominal = new Vec3(10.0 * side, 24.0, upper ? 5.0 : 11.0);
                stride = 3.0 * Mth.cos(limbSwing * 0.6662F + (pair ? 0.0F : Mth.PI)) * limbSwingAmount;
            }
        }
        // To the world, brought onto the surface, lifted if this leg is in the air of its step, and back to the model.
        Vector3f world = toWorld.transformPosition((float) (nominal.x * PX), (float) (nominal.y * PX), (float) ((nominal.z + stride) * PX), new Vector3f());
        Vec3 at = new Vec3(world.x + camera.x, world.y + camera.y, world.z + camera.z);
        double off = at.subtract(plane.point()).dot(plane.normal());
        double lift = 0.12 * Math.max(0.0, cycle);
        Vec3 onSurface = at.subtract(plane.normal().scale(off - lift));
        return toModelPoint(toModel, camera, onSurface);
    }

    /** A world point as a point of the body, in pixels. */
    private static Vec3 toModelPoint(Matrix4f toModel, Vec3 camera, Vec3 world) {
        Vector3f p = toModel.transformPosition((float) (world.x - camera.x), (float) (world.y - camera.y), (float) (world.z - camera.z), new Vector3f());
        return new Vec3(p.x / PX, p.y / PX, p.z / PX);
    }

    // ---- drawing ----

    /** One segment, from {@code from} to {@code to} (pixels of the body). */
    private static void draw(PoseStack poseStack, VertexConsumer vc, ModelPart part, Vec3 from, Vec3 to, int light, int overlay) {
        Vec3 d = to.subtract(from);
        double length = d.length();
        if (length < 1.0E-6) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(from.x * PX, from.y * PX, from.z * PX);
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), new Vector3f((float) (d.x / length), (float) (d.y / length), (float) (d.z / length))));
        part.render(poseStack, vc, light, overlay);
        poseStack.popPose();
    }

    private static boolean finite(LegIk.Pose pose) {
        return finite(pose.knee()) && finite(pose.ankle()) && finite(pose.tip());
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    /** Forgets the legs of players who are gone. */
    public static void prune(ClientLevel level) {
        STATES.keySet().removeIf(id -> level.getEntity(id) == null);
    }

    public static void clear() {
        STATES.clear();
    }
}
