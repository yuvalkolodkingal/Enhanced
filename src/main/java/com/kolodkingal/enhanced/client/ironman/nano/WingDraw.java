package com.kolodkingal.enhanced.client.ironman.nano;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.BodyLean;
import com.kolodkingal.enhanced.ironman.nano.NanoDroneEntity;
import com.kolodkingal.enhanced.ironman.nano.WingFormation;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Draws the Mark 85's <b>wing blades</b> (3.8.3): where each blade of a nanotech drone is this frame ({@link
 * WingFormation}, interpolated from its Iron Man himself, so they follow him smoothly even in fast flight), the blade
 * ({@link NanoDroneModel}: the metal lit by the world, the cyan panels full bright and glowing), the nanite assembly
 * (each blade grows out of his arm segment by segment, the outer one sliding out of the inner one, with red and gold
 * nanites streaming along) and, after the drones go, the folding away (outer into inner, inner into the arm, the tips
 * first), which the clients draw themselves for {@link WingFormation#RETRACT_TICKS} ticks.
 *
 * <p>In first person his own blades frame the screen: they sit in the camera's frame at its left and right edges, a
 * little low, turned to show their glowing faces, and never cross the middle; their lasers leave from there.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class WingDraw {
    static final ResourceLocation METAL = Enhanced.id("textures/entity/iron_man/wing_drone.png");
    static final ResourceLocation LIGHT = Enhanced.id("textures/entity/iron_man/wing_drone_glow.png");
    /** The extra glow added over the panels (additive). */
    static final int HALO = 0xFF2E4A55;
    /** The red and gold of the nanites. */
    static final int NANITE_RED = 0xB3121B;
    static final int NANITE_GOLD = 0xE8B23A;

    /** First person: how far out, up (from the eye) and forward the inner blade sits, and how big the blades are. */
    static final double FP_OUT = 1.28;
    static final double FP_RANK_OUT = 0.5;
    static final double FP_UP = -0.2;
    static final double FP_FORWARD = 1.2;
    static final float FP_SIZE = 0.52F;
    /** First person: how far the blades turn to show their faces to the camera (degrees). */
    static final float FP_TURN = 42.0F;

    /** The baked blade (the renderer bakes it). */
    @Nullable
    static NanoDroneModel model;

    /** The wings each Iron Man showed lately (owner id): when, and which drone slots (bits). */
    private record Seen(long time, int slots) {
    }

    private static final Map<Integer, Seen> SEEN = new HashMap<>();
    /** The wings folding away now (owner id): since when, and which slots. */
    private static final Map<Integer, Seen> FOLDING = new HashMap<>();

    /**
     * A blade's pose this frame: his frame's origin (feet, or the eye in first person), its yaw and pitch and scale,
     * and the blade's place in it.
     */
    record Pose(Player owner, float partial, Vec3 base, float yaw, float pitch, float scale, Vec3 local,
                boolean firstPerson, int side, int rank) {
        /** The middle of its inner edge in the world (where its laser leaves), moved with his body's lean in flight. */
        Vec3 muzzle() {
            Vec3 upright = at(local);
            return firstPerson ? upright : BodyLean.follow(owner, upright, partial);
        }

        /** A point of this frame in the world (upright). */
        Vec3 at(Vec3 point) {
            return base.add(point.scale(scale).xRot(-pitch * Mth.DEG_TO_RAD).yRot(-yaw * Mth.DEG_TO_RAD));
        }
    }

    /** How much of his flight lean the blades take back (0 none: they lie with him; 1 all: they stay upright). */
    static final float UPRIGHT = 0.75F;

    /**
     * The turn of his body's lean and bank in flight (the player renderer applies it turned half round from this
     * frame), or null when he stands upright.
     */
    @Nullable
    static Quaternionf lean(Player owner) {
        PoseStack lean = new PoseStack();
        lean.mulPose(Axis.YP.rotationDegrees(180.0F));
        BodyLean.transform(lean, owner);
        lean.mulPose(Axis.YP.rotationDegrees(-180.0F));
        Quaternionf q = lean.last().pose().getUnnormalizedRotation(new Quaternionf());
        return Math.abs(q.w) > 0.99999F ? null : q.normalize();
    }

    /** Whether we look out of this Iron Man's eyes. */
    static boolean firstPerson(Player owner) {
        Minecraft mc = Minecraft.getInstance();
        return owner == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    /** First person: a blade's place in the camera's frame. */
    static Vec3 fpLocal(int side, int rank, double time) {
        double phase = side * 1.7 + rank * 2.3;
        double out = FP_OUT + FP_RANK_OUT * rank + 0.02 * Math.sin(time * 0.083 + phase);
        double up = FP_UP - 0.08 * rank + 0.03 * Math.sin(time * 0.12 + phase);
        return new Vec3(WingFormation.sign(side) * out, up, FP_FORWARD - 0.2 * rank);
    }

    /** First person: where the arm is (the blades grow from there). */
    static Vec3 fpArm(int side) {
        return new Vec3(WingFormation.sign(side) * 0.45, -0.55, 0.35);
    }

    private static float ease(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    /** Where a blade of rank {@code rank} on {@code side} is this frame, grown (or left) by {@code shown}. */
    static Pose pose(Player owner, int side, int rank, float partial, float shown) {
        double time = owner.level().getGameTime() + partial;
        boolean fp = firstPerson(owner);
        Vec3 slot = fp ? fpLocal(side, rank, time) : WingFormation.local(side, rank, time);
        Vec3 from;
        if (rank % WingFormation.BLADES == 0) {
            from = fp ? fpArm(side) : WingFormation.arm(side);
        } else {
            from = fp ? fpLocal(side, rank - 1, time) : WingFormation.local(side, rank - 1, time);
        }
        Vec3 local = from.lerp(slot, ease(shown));
        if (fp) {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            return new Pose(owner, partial, camera.getPosition(), camera.getYRot(), camera.getXRot(), 1.0F, local, true,
                    side, rank);
        }
        float yaw = Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot);
        return new Pose(owner, partial, owner.getPosition(partial), yaw, 0.0F, owner.getScale(), local, false, side,
                rank);
    }

    /** Where blade {@code blade} of this drone is drawn now (its laser's start), or null when it has no owner here. */
    @Nullable
    static Vec3 muzzle(NanoDroneEntity drone, int blade, float partial) {
        Player owner = drone.owner();
        if (owner == null) {
            return null;
        }
        int rank = WingFormation.rank(drone.slot(), blade);
        float shown = WingFormation.grown(rank, drone.tickCount + partial);
        return pose(owner, WingFormation.side(drone.slot()), rank, partial, shown).muzzle();
    }

    /** Draws one blade; the pose stack's origin is the world point {@code origin}. */
    static void draw(PoseStack poseStack, MultiBufferSource buffers, Vec3 origin, Pose p, float shown, int light,
                     int tint) {
        NanoDroneModel m = model;
        if (m == null || shown <= 0.02F) {
            return;
        }
        float time = (float) (Minecraft.getInstance().level == null ? 0L
                : Minecraft.getInstance().level.getGameTime()) + Minecraft.getInstance().getTimer()
                .getGameTimeDeltaPartialTick(false);
        double phase = p.side() * 1.7 + p.rank() * 2.3;
        float sway = (float) Math.sin(time * 0.07 + phase);
        float tilt = (float) Math.sin(time * 0.05 + phase * 1.3);
        poseStack.pushPose();
        Vec3 d = p.base().subtract(origin);
        poseStack.translate(d.x, d.y, d.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-p.yaw()));
        if (p.pitch() != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(p.pitch()));
        }
        poseStack.scale(p.scale(), p.scale(), p.scale());
        Quaternionf lean = p.firstPerson() ? null : lean(p.owner());
        if (lean != null) {
            // His body's lean and bank in flight carry the blades along with his arms...
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            BodyLean.transform(poseStack, p.owner());
            poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
        }
        poseStack.translate(p.local().x, p.local().y, p.local().z);
        if (lean != null) {
            // ...but they mostly stay upright, so that from behind they still spread like wings.
            poseStack.mulPose(new Quaternionf().slerp(new Quaternionf(lean).conjugate(), UPRIGHT));
        }
        if (p.side() == 0) {
            // His right: the model's "out" is the frame's -x.
            poseStack.scale(-1.0F, 1.0F, 1.0F);
        }
        float turn = p.firstPerson() ? FP_TURN + 8.0F * Math.min(p.rank(), 1) : 8.0F + 10.0F * Math.min(p.rank(), 1);
        poseStack.mulPose(Axis.YP.rotationDegrees(turn + 4.0F * sway));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-(9.0F + 4.0F * Math.min(p.rank(), 1) + 3.0F * tilt)));
        float size = 0.5F * (0.25F + 0.75F * ease(shown)) * (p.firstPerson() ? FP_SIZE : 1.0F);
        poseStack.scale(size, size, size);
        float reach = shown * 1.15F - 0.08F;
        NanoDroneModel.Shown segments = i -> NanoDroneModel.fromMiddle(i) <= reach;
        boolean pod = !p.firstPerson() && shown > 0.85F;
        m.renderMetal(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(METAL)), light,
                OverlayTexture.NO_OVERLAY, tint, segments, pod);
        m.renderLight(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(LIGHT)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, segments, pod);
        m.renderLight(poseStack, buffers.getBuffer(RenderType.eyes(LIGHT)), LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, HALO, segments, pod);
        poseStack.popPose();
    }

    /** The renderer saw these wings of his this frame (they fold away from here when the drones go). */
    static void seen(Player owner, int slot) {
        long now = owner.level().getGameTime();
        Seen old = SEEN.get(owner.getId());
        int bits = old != null && now - old.time() <= 2 ? old.slots() : 0;
        SEEN.put(owner.getId(), new Seen(now, bits | 1 << Math.min(slot, 30)));
    }

    /** His drones went (the {@code DRONES_IN} effect): the wings he showed fold away. */
    static void fold(int ownerId) {
        ClientLevel level = Minecraft.getInstance().level;
        Seen seen = SEEN.remove(ownerId);
        if (level == null || seen == null || level.getGameTime() - seen.time() > 10) {
            return;
        }
        FOLDING.put(ownerId, new Seen(level.getGameTime(), seen.slots()));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || FOLDING.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack poseStack = new PoseStack();
        for (Map.Entry<Integer, Seen> e : FOLDING.entrySet()) {
            if (!(level.getEntity(e.getKey()) instanceof Player owner)) {
                continue;
            }
            float age = level.getGameTime() - e.getValue().time() + partial;
            for (int slot = 0; slot < 31; slot++) {
                if ((e.getValue().slots() & 1 << slot) == 0) {
                    continue;
                }
                for (int blade = 0; blade < WingFormation.BLADES; blade++) {
                    int rank = WingFormation.rank(slot, blade);
                    float left = WingFormation.left(rank, age);
                    Pose p = pose(owner, WingFormation.side(slot), rank, partial, left);
                    int light = LevelRenderer.getLightColor(level, BlockPos.containing(p.muzzle()));
                    draw(poseStack, buffers, cam, p, left, light, 0xFFFFFFFF);
                }
            }
        }
        buffers.endBatch(RenderType.entityCutoutNoCull(METAL));
        buffers.endBatch(RenderType.entityCutoutNoCull(LIGHT));
        buffers.endBatch(RenderType.eyes(LIGHT));
    }

    /** Every tick: the nanites stream while blades form or fold; old records go. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            SEEN.clear();
            FOLDING.clear();
            return;
        }
        if (mc.isPaused() || mc.player == null) {
            return;
        }
        long now = level.getGameTime();
        SEEN.values().removeIf(s -> now - s.time() > 40 || s.time() > now);
        RandomSource r = level.random;
        for (NanoDroneEntity drone : level.getEntitiesOfClass(NanoDroneEntity.class,
                mc.player.getBoundingBox().inflate(64.0), d -> d.tickCount <= WingFormation.GROW_TICKS
                        + WingFormation.OUTER_DELAY)) {
            Player owner = drone.owner();
            if (owner == null) {
                continue;
            }
            for (int blade = 0; blade < WingFormation.BLADES; blade++) {
                int rank = WingFormation.rank(drone.slot(), blade);
                float shown = WingFormation.grown(rank, drone.tickCount);
                if (shown > 0.0F && shown < 1.0F) {
                    nanites(level, r, pose(owner, WingFormation.side(drone.slot()), rank, 0.0F, shown), 3);
                }
            }
        }
        Iterator<Map.Entry<Integer, Seen>> it = FOLDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Seen> e = it.next();
            float age = now - e.getValue().time();
            if (age > WingFormation.RETRACT_TICKS || age < 0) {
                it.remove();
                continue;
            }
            if (!(level.getEntity(e.getKey()) instanceof Player owner)) {
                continue;
            }
            for (int slot = 0; slot < 31; slot++) {
                if ((e.getValue().slots() & 1 << slot) != 0) {
                    for (int blade = 0; blade < WingFormation.BLADES; blade++) {
                        int rank = WingFormation.rank(slot, blade);
                        float left = WingFormation.left(rank, age);
                        if (left > 0.0F && left < 1.0F) {
                            nanites(level, r, pose(owner, WingFormation.side(slot), rank, 0.0F, left), 4);
                        }
                    }
                }
            }
        }
    }

    /** A few red and gold nanites round a forming (or folding) blade, drifting toward his arm. */
    private static void nanites(ClientLevel level, RandomSource r, Pose p, int count) {
        Vec3 at = p.muzzle();
        Vec3 arm = p.at(p.firstPerson() ? fpArm(p.side()) : WingFormation.arm(p.side()));
        Vec3 toArm = arm.subtract(at).scale(0.06);
        double spread = 0.25 * p.scale() * (p.firstPerson() ? FP_SIZE : 1.0F);
        for (int i = 0; i < count; i++) {
            int color = r.nextInt(3) == 0 ? NANITE_GOLD : NANITE_RED;
            level.addParticle(new EnergyParticleOptions(color, 0.35F), at.x + r.nextGaussian() * spread,
                    at.y + r.nextGaussian() * spread * 1.6, at.z + r.nextGaussian() * spread, toArm.x, toArm.y,
                    toArm.z);
        }
    }

    private WingDraw() {
    }
}
