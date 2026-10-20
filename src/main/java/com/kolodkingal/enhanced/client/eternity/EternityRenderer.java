package com.kolodkingal.enhanced.client.eternity;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.EnhancedRenderTypes;
import com.kolodkingal.enhanced.eternity.EternityEntity;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How Eternity is drawn (3.8, PLAN 10.4; {@link EternityModel}'s shape): the silhouette of a seated giant filled with a
 * deep-space starfield, its picture ({@code nebula.png}: purple, blue and magenta nebulae with white stars) mapped by
 * the world position of each point of the body, not by the boxes' own UVs, so the body is a window onto space that does
 * not stretch with the limbs; the picture drifts slowly. The body is unlit (space glows the same from every side). Over
 * it a few bright stars twinkle (light that adds up, facing the camera); under a dark, faceted metal helmet with four
 * bent antenna-horns. He breathes (the chest, arms and head rise over six seconds) and turns his head slowly toward the
 * player who looks at him. Beyond {@link #LOD} blocks only the silhouette and the helmet are drawn.
 */
public class EternityRenderer extends EntityRenderer<EternityEntity> {
    public static final ResourceLocation NEBULA = Enhanced.id("textures/entity/eternity/nebula.png");
    public static final ResourceLocation HELMET = Enhanced.id("textures/entity/eternity/helmet.png");
    /** Blocks of world per repeat of the nebula's picture. */
    static final float NEBULA_BLOCKS = 22.0F;
    /** Beyond this distance the twinkling stars are left out. */
    static final double LOD = 160.0;
    /** One breath (ticks). */
    static final float BREATH_TICKS = 120.0F;
    private static final List<Vector3f> STARS = EternityModel.starPoints();

    private final EternityModel model;
    /** Each Eternity's head turn, eased frame by frame (radians), and the age it was last eased at. */
    private final Map<Integer, float[]> heads = new HashMap<>();

    public EternityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new EternityModel(context.bakeLayer(EternityModel.LAYER));
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EternityEntity entity) {
        return NEBULA;
    }

    @Override
    public boolean shouldRender(EternityEntity entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(EternityEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light) {
        Minecraft mc = Minecraft.getInstance();
        float age = entity.tickCount + partialTick;
        float breath = 0.5F - 0.5F * Mth.cos(age / BREATH_TICKS * Mth.TWO_PI);
        model.pose(breath, headTurn(entity, age, partialTick));
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        boolean mirrored = MirrorReflection.drawing();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        pose.scale(EternityModel.SCALE, EternityModel.SCALE, EternityModel.SCALE);
        VertexConsumer body = new NebulaUv(buffers.getBuffer(RenderType.beaconBeam(NEBULA, false)), camera,
                age * 0.0015F, mirrored);
        model.renderBody(pose, body, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        model.renderHelmet(pose, buffers.getBuffer(RenderType.entitySolid(HELMET)), light,
                OverlayTexture.NO_OVERLAY);
        if (entity.position().distanceTo(camera) < LOD) {
            twinkles(pose, buffers.getBuffer(EnhancedRenderTypes.ENERGY_BEAM), age);
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    /** The bright stars inside his body: little crosses of light that swell and fade, each in its own time. */
    private static void twinkles(PoseStack pose, VertexConsumer vc, float age) {
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < STARS.size(); i++) {
            Vector3f p = STARS.get(i);
            float phase = age * (0.05F + (i % 7) * 0.011F) + i * 1.7F;
            float shine = Mth.clamp(0.5F + 0.5F * Mth.sin(phase), 0.0F, 1.0F);
            shine = shine * shine * shine;
            if (shine < 0.05F) {
                continue;
            }
            float s = 0.8F + 2.6F * shine;
            int alpha = (int) (230 * shine);
            star(vc, m, p.x, p.y, p.z, s, 0.35F * s, alpha);
            star(vc, m, p.x, p.y, p.z, 0.35F * s, s, alpha);
        }
    }

    /** One bar of a star's cross, flat on the front of his body (model pixels). */
    private static void star(VertexConsumer vc, Matrix4f m, float x, float y, float z, float w, float h, int alpha) {
        vc.addVertex(m, x - w, y - h, z).setColor(255, 250, 255, alpha);
        vc.addVertex(m, x + w, y - h, z).setColor(255, 250, 255, alpha);
        vc.addVertex(m, x + w, y + h, z).setColor(255, 250, 255, alpha);
        vc.addVertex(m, x - w, y + h, z).setColor(255, 250, 255, alpha);
    }

    /**
     * His head turns slowly toward the local player while he is before him (at most 40 degrees either way), eased frame
     * by frame.
     */
    private float headTurn(EternityEntity entity, float age, float partialTick) {
        float target = 0.0F;
        Player player = Minecraft.getInstance().player;
        if (player != null && player.level() == entity.level()) {
            Vec3 to = player.getPosition(partialTick).subtract(entity.position());
            if (to.z > 1.0) {
                float yaw = (float) Mth.atan2(to.x, to.z);
                target = Mth.clamp(yaw, -0.7F, 0.7F);
            }
        }
        float start = target;
        float[] state = heads.computeIfAbsent(entity.getId(), id -> new float[]{start, age});
        float dt = Mth.clamp(age - state[1], 0.0F, 5.0F);
        state[1] = age;
        state[0] += (target - state[0]) * (1.0F - (float) Math.exp(-dt / 40.0F));
        return state[0];
    }

    /**
     * Writes the body's vertices with the nebula's coordinates taken from where each point is in the world (the face's
     * plane by its normal: the front and back by x and y, the sides by z and y, the tops by x and z), so the starfield
     * stays put as a window onto space; it drifts by {@code drift}. In the mirror's reflection the height is reflected
     * back, so the reflection shows the same stars.
     */
    static final class NebulaUv implements VertexConsumer {
        private final VertexConsumer inner;
        private final Vec3 camera;
        private final float drift;
        private final boolean mirrored;

        NebulaUv(VertexConsumer inner, Vec3 camera, float drift, boolean mirrored) {
            this.inner = inner;
            this.camera = camera;
            this.drift = drift;
            this.mirrored = mirrored;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light,
                              float nx, float ny, float nz) {
            double wx = x + camera.x;
            double wy = y + camera.y;
            double wz = z + camera.z;
            if (mirrored) {
                wy = 2.0 * (EternityRealm.MIRROR_Y + 1) - wy;
                ny = -ny;
            }
            float ax = Math.abs(nx);
            float ay = Math.abs(ny);
            float az = Math.abs(nz);
            double a;
            double b;
            if (ay >= ax && ay >= az) {
                a = wx;
                b = wz;
            } else if (ax >= az) {
                a = wz;
                b = -wy;
            } else {
                a = wx;
                b = -wy;
            }
            float uu = (float) (a / NEBULA_BLOCKS) + drift;
            float vv = (float) (b / NEBULA_BLOCKS) + drift * 0.37F;
            inner.addVertex(x, y, z, color, uu, vv, overlay, light, nx, ny, nz);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return inner.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return inner.setColor(r, g, b, a);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return inner.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return inner.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return inner.setUv2(u, v);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return inner.setNormal(x, y, z);
        }
    }
}
