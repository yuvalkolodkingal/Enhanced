package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The fire of Jim Hammond (3.17): while he flies in flames ({@link Live#JIM_FLAMING}) his own skin glows faintly and one smooth
 * sheath of flame flows up and back along his whole body.
 * <ol>
 *   <li><b>The glow</b>: his current suit (white android, classic red, any look) stays fully visible, drawn again full-bright
 *   with only a faint warm light over it.</li>
 *   <li><b>The sheath</b>: two larger copies of the posed model (head, torso, both arms, both legs) made of slanted sheets of
 *   flame that start at each part and flow away from it up and back along his motion (straight up when he hovers, swept back in
 *   flight). Each sheet carries one soft flame texture that scrolls smoothly (a fixed texture, never a change of frame), fades to
 *   nothing at its top and is see-through, so the suit shows through; the two layers scroll at their own speeds and overlap into one
 *   connected, softly moving sheath, with no separate tongues to pop in and out. It fades in and out as he starts and stops flying.</li>
 * </ol>
 * Unlit and alpha-blended ({@link EnhancedRenderTypes#webDecal}): glowing fire from every angle, never a solid or dark shape. The streaming
 * trail and the embers are {@link JimFx}'s (Source Code Vision has those but not the body's flames). In first person his own body is not
 * drawn, so none of it can block the view.
 */
public class JimFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int FULL_BRIGHT = 0xF000F0;
    /** How visible each player's sheath is now (0 to 1: it fades in and out), by entity id, and when it was last updated. */
    private static final java.util.Map<Integer, float[]> LEVEL = new java.util.HashMap<>();

    private static final ResourceLocation GLOW = tex("glow");
    private static final ResourceLocation FLOW_0 = tex("flow_0");
    private static final ResourceLocation FLOW_1 = tex("flow_1");

    static ResourceLocation tex(String name) {
        return Enhanced.id("textures/entity/jim/" + name + ".png");
    }

    private final PlayerModel<AbstractClientPlayer> glow;

    public JimFlameLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                         PlayerModel<AbstractClientPlayer> glow) {
        super(parent);
        this.glow = glow;
    }

    /** Forgets every player (a world was left). */
    static void clear() {
        LEVEL.clear();
    }

    /** Whether this player's body is in flames now (Jim in flight: Source Code Vision has the trail only, see {@link #burning}). */
    static boolean flames(PowerData data) {
        return data.is(Live.JIM_FLAMING);
    }

    /** Whether this player leaves a burning trail and embers behind: Jim in flames, and Source Code Vision while he flies (no flames on his body). */
    static boolean burning(PowerData data, AbstractClientPlayer player) {
        if (data.is(Live.JIM_FLAMING)) {
            return true;
        }
        // (3.17) Source Code Vision's trail while he flies: G on and in the air (his switch: vision.source_code_flight_fire)
        return data.power() == com.kolodkingal.enhanced.power.Power.VISION && "source_code".equals(data.variant()) && !data.disguise()
                && data.flying() && !player.onGround()
                && (!com.kolodkingal.enhanced.EnhancedConfig.SPEC.isLoaded() || com.kolodkingal.enhanced.EnhancedConfig.SOURCE_CODE_FLIGHT_FIRE.get());
    }

    private void pose(PlayerModel<AbstractClientPlayer> model, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                      float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        model.prepareMobModel(player, limbSwing, limbSwingAmount, partialTicks);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.setAllVisible(true);
        // the second layers of the skin are not part of the fire
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        PowerData data = Powers.get(player);
        float t = player.tickCount + partialTicks;
        // fade in and out (a few ticks), never on and off
        float[] level = LEVEL.computeIfAbsent(player.getId(), k -> new float[]{0.0F, t});
        float dt = Mth.clamp(t - level[1], 0.0F, 4.0F);
        level[1] = t;
        float target = flames(data) && !player.isInvisible() ? 1.0F : 0.0F;
        level[0] = target > level[0] ? Math.min(target, level[0] + dt * 0.12F) : Math.max(target, level[0] - dt * 0.09F);
        float k = level[0];
        if (k <= 0.01F) {
            return;
        }
        // 1. his own skin stays as it is (white stays white, red stays red); only a faint warm light lies over it
        pose(glow, player, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        float pulse = 0.5F + 0.5F * Mth.sin(t * 0.13F);
        int glowAlpha = Math.round(255.0F * (0.010F + 0.010F * pulse) * k);
        glow.renderToBuffer(poseStack, buffer.getBuffer(RenderType.energySwirl(GLOW, (t * 0.003F) % 1.0F, (-t * 0.012F) % 1.0F)),
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glowAlpha << 24 | 0xFF9A30);
        PlayerModel<AbstractClientPlayer> body = getParentModel();
        // 2. the sheath: the way the flames flow is up and back along his motion, in the world; each part turns it into its own frame
        Vec3 velocity = player.getDeltaMovement();
        Vector3f flow = new Vector3f((float) -velocity.x * 2.4F, 1.0F + (float) -velocity.y * 1.2F, (float) -velocity.z * 2.4F);
        if (flow.y < 0.35F) {
            flow.y = 0.35F;
        }
        flow.normalize();
        float slow = 0.88F + 0.12F * Mth.sin(t * 0.06F);
        sheath(poseStack, buffer, body.head, -4, 4, -8, 0, -4, 4, 7.0F, flow, t, 0, k * slow);
        sheath(poseStack, buffer, body.body, -4, 4, 0, 12, -2, 2, 4.0F, flow, t, 1, k * slow);
        sheath(poseStack, buffer, body.rightArm, -3, 1, -2, 10, -2, 2, 5.0F, flow, t, 2, k * slow);
        sheath(poseStack, buffer, body.leftArm, -1, 3, -2, 10, -2, 2, 5.0F, flow, t, 3, k * slow);
        sheath(poseStack, buffer, body.rightLeg, -2, 2, 0, 12, -2, 2, 3.0F, flow, t, 4, k * slow);
        sheath(poseStack, buffer, body.leftLeg, -2, 2, 0, 12, -2, 2, 3.0F, flow, t, 5, k * slow);
    }

    /** Base opacity of the inner and the outer layer of the sheath (their textures' own alpha comes on top). */
    private static final float[] LAYER_ALPHA = {0.34F, 0.22F};

    /**
     * One part's sheath (its box in pixels of the part's own frame): four slanted sheets round it, from its foot to a flame's height
     * above its top ({@code rise} extra pixels), leaning along {@code flow} (a world direction: up and back), in two layers (the outer one
     * wider, taller, slower and fainter). Each sheet is two rows of quads; their vertex alpha rises from a little at the foot to full
     * a third of the way up and to nothing at the top, so the sheet has no edge; the soft flame texture scrolls up it.
     */
    private static void sheath(PoseStack poseStack, MultiBufferSource buffer, ModelPart part, float x0, float x1, float y0, float y1,
                               float z0, float z1, float rise, Vector3f flow, float t, int seed, float level) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        Matrix4f m = poseStack.last().pose();
        Vector3f d = new Matrix4f(m).invert().transformDirection(flow, new Vector3f()).normalize();
        for (int layer = 0; layer < 2; layer++) {
            float gap = layer == 0 ? 0.6F : 1.3F;
            float height = ((y1 - y0) + rise) * (layer == 0 ? 1.0F : 1.45F) / 16.0F;
            float scroll = t * (layer == 0 ? 0.021F : 0.012F) + seed * 0.31F;
            float shift = 0.06F * Mth.sin(t * 0.04F + seed * 1.7F + layer * 2.0F);
            VertexConsumer vc = buffer.getBuffer(EnhancedRenderTypes.webDecal(layer == 0 ? FLOW_0 : FLOW_1));
            int full = Math.round(255.0F * LAYER_ALPHA[layer] * level);
            int foot = Math.round(full * 0.5F);
            float[][] sides = {
                    {x0 - gap, z0 - gap, x1 + gap, z0 - gap}, {x1 + gap, z0 - gap, x1 + gap, z1 + gap},
                    {x1 + gap, z1 + gap, x0 - gap, z1 + gap}, {x0 - gap, z1 + gap, x0 - gap, z0 - gap}};
            float span = layer == 0 ? 0.9F : 0.7F;
            for (int s = 0; s < 4; s++) {
                float[] e = sides[s];
                float u0 = s * 0.25F + layer * 0.31F + shift;
                float u1 = u0 + 0.5F;
                float ax = e[0] / 16.0F, az = e[1] / 16.0F, bx = e[2] / 16.0F, bz = e[3] / 16.0F;
                float by = y1 / 16.0F;
                // rows: the foot (v at the top of the scroll + span), a third up, the top
                float[] frac = {0.0F, 0.34F, 1.0F};
                int[] alpha = {foot, full, 0};
                for (int row = 0; row < 2; row++) {
                    float f0 = frac[row], f1 = frac[row + 1];
                    float va = scroll + span * (1.0F - f0), vb = scroll + span * (1.0F - f1);
                    put(vc, m, ax + d.x * height * f0, by + d.y * height * f0, az + d.z * height * f0, u0, va, alpha[row]);
                    put(vc, m, bx + d.x * height * f0, by + d.y * height * f0, bz + d.z * height * f0, u1, va, alpha[row]);
                    put(vc, m, bx + d.x * height * f1, by + d.y * height * f1, bz + d.z * height * f1, u1, vb, alpha[row + 1]);
                    put(vc, m, ax + d.x * height * f1, by + d.y * height * f1, az + d.z * height * f1, u0, vb, alpha[row + 1]);
                }
            }
        }
        poseStack.popPose();
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int alpha) {
        vc.addVertex(m, x, y, z).setUv(u, v).setColor(Math.min(255, alpha) << 24 | 0xFFFFFF);
    }
}
