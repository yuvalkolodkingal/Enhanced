package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.strange.AstralProjection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * The Cloak of Levitation (3.6): Doctor Strange's red cloak with its high flared collar, drawn on the body as a cloth of
 * its own (a grid of quads, the outside and the lining, and the collar as a strip round the back of the neck), not the
 * vanilla cape. It moves by itself: it billows with the speed of his flight and his steps (the vanilla cape's sway,
 * read from the same motion), lifts in a slow wave while he stands, and its hem flies up over his shoulders while he
 * falls (it catches him). On the spirit of the Astral Projection it is not drawn: the cloak stays on the body ({@code
 * AstralBodyRenderer} draws it there). A custom hero whose Flight is the style "cloak" wears it too. Its textures are
 * {@code strange_<look>_cloak.png} (made by {@code gen_strange.py}): the outside x 0-31, y 0-47, the lining x 32-63,
 * the collar's outside x 0-31, y 48-55, its inside x 32-63.
 */
public class CloakLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The cloak's grid: columns across, rows down. */
    private static final int NX = 8;
    private static final int NY = 10;
    /** Its length and half-widths (pixels of the body), and where it hangs behind the back. */
    private static final float LENGTH = 23.0F;
    private static final float TOP_HALF = 6.0F;
    private static final float HEM_HALF = 7.5F;
    private static final float BACK = 2.3F;
    /** The collar: its height and its radius round the neck, at the bottom and at the flared top. */
    private static final float COLLAR_H = 7.0F;
    private static final float COLLAR_R0 = 5.0F;
    private static final float COLLAR_R1 = 6.3F;
    private static final int COLLAR_SEGS = 10;
    private static final float COLLAR_ARC = (float) Math.toRadians(112);

    public CloakLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** The texture of a look's cloak (the plain one for a look of the player's own skin or a custom hero). */
    public static ResourceLocation texture(String look) {
        String id = switch (look) {
            case "multiverse", "defender" -> look;
            default -> "sorcerer_supreme";
        };
        return Enhanced.id("textures/entity/suit/strange_" + id + "_cloak.png");
    }

    /** Whether this player wears the Cloak now, and in which look (null: none). */
    @Nullable
    static String wornLook(AbstractClientPlayer player, PowerData data) {
        if (player.isInvisible() || AstralProjection.isAstral(player) || player.getItemBySlot(EquipmentSlot.CHEST)
                .is(Items.ELYTRA)) {
            return null;
        }
        if (data.power() == Power.DOCTOR_STRANGE) {
            return Variants.current(data).id();
        }
        if (data.power() == Power.CUSTOM) {
            CustomHero.Pick flight = CustomHeroes.pick(player, PowerModule.FLIGHT);
            return flight != null && "cloak".equals(flight.option()) ? "sorcerer_supreme" : null;
        }
        return null;
    }

    /** How the cloth hangs this frame: how far it swings out behind, sideways, and how much the hem flies up. */
    public record Sway(float billow, float side, float catchUp, float time, boolean calm) {
        /** A cloak that hangs still (on the kneeling body): a little out, the slow wave only. */
        public static Sway still(float time) {
            return new Sway(10.0F, 0.0F, 0.0F, time, EnhancedClientConfig.reducedMotion());
        }
    }

    /** The cloak's sway from a player's motion: the vanilla cape's maths, then the Cloak's own lift and catch. */
    static Sway sway(AbstractClientPlayer p, PowerData data, float partial, float age) {
        double dx = Mth.lerp(partial, p.xCloakO, p.xCloak) - Mth.lerp(partial, p.xo, p.getX());
        double dy = Mth.lerp(partial, p.yCloakO, p.yCloak) - Mth.lerp(partial, p.yo, p.getY());
        double dz = Mth.lerp(partial, p.zCloakO, p.zCloak) - Mth.lerp(partial, p.zo, p.getZ());
        float yaw = Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = -Mth.cos(yaw);
        float up = Mth.clamp((float) dy * 10.0F, -6.0F, 32.0F);
        float forward = Mth.clamp((float) (dx * sin + dz * cos) * 100.0F, 0.0F, 150.0F);
        float side = Mth.clamp((float) (dx * cos - dz * sin) * 100.0F, -20.0F, 20.0F);
        float bob = Mth.lerp(partial, p.oBob, p.bob);
        up += Mth.sin(Mth.lerp(partial, p.walkDistO, p.walkDist) * 6.0F) * 32.0F * bob;
        if (p.isCrouching()) {
            up += 25.0F;
        }
        float billow = Mth.clamp(6.0F + forward / 2.0F + up, 4.0F, 105.0F);
        boolean flying = data.flying() || p.getAbilities().flying;
        if (flying) {
            // It lifts as he floats; at speed it streams out behind him, along his body (which leans into the flight
            // itself), never flipped up flat over his shoulders.
            billow = Mth.clamp(billow, 14.0F, 58.0F);
        }
        float fall = (float) -p.getDeltaMovement().y;
        float catchUp = !flying && !p.onGround() && fall > 0.35F ? Mth.clamp((fall - 0.35F) * 140.0F, 0.0F, 80.0F) : 0;
        return new Sway(billow, side, catchUp, age, EnhancedClientConfig.reducedMotion());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        PowerData data = Powers.get(player);
        String look = wornLook(player, data);
        if (look == null) {
            return;
        }
        draw(poseStack, buffers, light, LivingEntityRenderer.getOverlayCoords(player, 0.0F), texture(look),
                getParentModel().body, sway(player, data, partialTicks, ageInTicks));
    }

    /** Draws the cloak and its collar on {@code body} (the pose stack at the model's root). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                            ResourceLocation texture, ModelPart body, Sway sway) {
        poseStack.pushPose();
        body.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        Vec3[][] grid = cloth(sway);
        for (int j = 0; j < NY; j++) {
            for (int i = 0; i < NX; i++) {
                Vec3 a = grid[j][i];
                Vec3 b = grid[j][i + 1];
                Vec3 c = grid[j + 1][i + 1];
                Vec3 d = grid[j + 1][i];
                Vector3f n = normal(a, b, d);
                float u0 = (float) i / NX * 32.0F / 64.0F;
                float u1 = (float) (i + 1) / NX * 32.0F / 64.0F;
                float v0 = (float) j / NY * 48.0F / 64.0F;
                float v1 = (float) (j + 1) / NY * 48.0F / 64.0F;
                // The outside (seen from behind), and the lining a hair in front of it.
                quad(vc, pose, a, b, c, d, u0, u1, v0, v1, n, light, overlay);
                Vec3 in = new Vec3(n.x(), n.y(), n.z()).scale(-0.12);
                quad(vc, pose, a.add(in), d.add(in), c.add(in), b.add(in), 0.5F + u0, 0.5F + u1, v0, v1,
                        new Vector3f(n).negate(), light, overlay);
            }
        }
        collar(vc, pose, sway, light, overlay);
        poseStack.popPose();
    }

    /**
     * The cloth's grid points (body pixels: x across, y down, z behind): the top row hung along the shoulders and wrapped
     * forward round them, each row below swung out by the sway (more toward the hem), flared wider, with the slow wave of
     * the Cloak's own life and the hem thrown up while he falls.
     */
    static Vec3[][] cloth(Sway sway) {
        Vec3[][] grid = new Vec3[NY + 1][NX + 1];
        float wave = sway.calm() ? 0.0F : 1.0F;
        for (int i = 0; i <= NX; i++) {
            float fx = (float) i / NX * 2.0F - 1.0F;
            float y = 0.0F;
            float z = BACK;
            for (int j = 0; j <= NY; j++) {
                float v = (float) j / NY;
                float half = Mth.lerp(v, TOP_HALF, HEM_HALF + sway.billow() * 0.02F);
                float x = fx * half;
                // Over the shoulders the top rows wrap forward.
                float wrap = Math.max(0.0F, 1.0F - v * 4.0F) * 3.2F * fx * fx;
                grid[j][i] = new Vec3(x + sway.side() * 0.03F * v * 16.0F / 16.0F, y, z - wrap);
                if (j == NY) {
                    break;
                }
                float angle = sway.billow() * (0.3F + 0.7F * v) + sway.catchUp() * v * v
                        + wave * 4.0F * Mth.sin(sway.time() * 0.12F - v * 4.0F + fx * 1.4F)
                        + wave * 2.0F * Mth.sin(sway.time() * 0.07F + fx * 2.3F) * v;
                angle = Mth.clamp(angle, 0.0F, 170.0F) * Mth.DEG_TO_RAD;
                float step = LENGTH / NY;
                y += step * Mth.cos(angle);
                z += step * Mth.sin(angle);
            }
        }
        return grid;
    }

    /** The collar: a strip round the back of the neck, standing up and flaring out, its rim turned back. */
    private static void collar(VertexConsumer vc, PoseStack.Pose pose, Sway sway, int light, int overlay) {
        int rows = 3;
        float flutter = sway.calm() ? 0.0F : 0.25F * Mth.sin(sway.time() * 0.1F);
        for (int s = 0; s < COLLAR_SEGS; s++) {
            float p0 = -COLLAR_ARC + 2.0F * COLLAR_ARC * s / COLLAR_SEGS;
            float p1 = -COLLAR_ARC + 2.0F * COLLAR_ARC * (s + 1) / COLLAR_SEGS;
            for (int r = 0; r < rows; r++) {
                float h0 = (float) r / rows;
                float h1 = (float) (r + 1) / rows;
                Vec3 a = collarPoint(p0, h0, flutter);
                Vec3 b = collarPoint(p1, h0, flutter);
                Vec3 c = collarPoint(p1, h1, flutter);
                Vec3 d = collarPoint(p0, h1, flutter);
                Vector3f n = normal(a, b, d);
                float u0 = (float) s / COLLAR_SEGS * 0.5F;
                float u1 = (float) (s + 1) / COLLAR_SEGS * 0.5F;
                float v0 = (48.0F + 8.0F * (1.0F - h0)) / 64.0F;
                float v1 = (48.0F + 8.0F * (1.0F - h1)) / 64.0F;
                quad(vc, pose, a, b, c, d, u0, u1, v0, v1, n, light, overlay);
                Vec3 in = new Vec3(n.x(), n.y(), n.z()).scale(-0.12);
                quad(vc, pose, a.add(in), d.add(in), c.add(in), b.add(in), 0.5F + u0, 0.5F + u1, v0, v1,
                        new Vector3f(n).negate(), light, overlay);
            }
        }
    }

    /** A point of the collar at {@code phi} round the neck (0: straight behind) and height {@code h} (0-1). */
    private static Vec3 collarPoint(float phi, float h, float flutter) {
        float r = Mth.lerp(h * h, COLLAR_R0, COLLAR_R1 + flutter);
        // The sides stand a little lower than the back, as the film's collar dips toward the chest.
        float height = COLLAR_H * h * (1.0F - 0.35F * Math.abs(phi) / COLLAR_ARC);
        return new Vec3(r * Mth.sin(phi), -height, r * Mth.cos(phi) * 0.9F - 0.6F);
    }

    private static Vector3f normal(Vec3 a, Vec3 b, Vec3 d) {
        Vec3 n = b.subtract(a).cross(d.subtract(a));
        return n.lengthSqr() < 1.0E-8 ? new Vector3f(0, 0, 1) : n.normalize().toVector3f();
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float u1,
                             float v0, float v1, Vector3f n, int light, int overlay) {
        vertex(vc, pose, a, u0, v0, n, light, overlay);
        vertex(vc, pose, b, u1, v0, n, light, overlay);
        vertex(vc, pose, c, u1, v1, n, light, overlay);
        vertex(vc, pose, d, u0, v1, n, light, overlay);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, Vector3f n, int light,
                               int overlay) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(0xFFFFFFFF).setUv(u, v).setOverlay(overlay)
                .setLight(light).setNormal(pose, n.x(), n.y(), n.z());
    }
}
