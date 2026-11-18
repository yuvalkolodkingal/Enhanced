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
 *   <li><b>The fire</b>: on every part (head, torso, both arms, both legs) three {@link FireSheet}s that always face the camera and
 *   rise from the part up and back along his motion (straight up when he hovers, swept back in flight): one through the part, a
 *   wider, taller and slower one behind it, and a fainter one in front of it so flames lick over the body too. Their flame tongues
 *   scroll up continuously (one texture, never a change of frame), so nothing flickers. It fades in and out as he starts and stops
 *   flying.</li>
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
        // (the swirl type is additive and ignores alpha: the faint light is a dim colour, not a low alpha)
        float lightF = (0.07F + 0.05F * pulse) * k;
        int glowColor = 0xFF000000 | Math.round(0xFF * lightF) << 16 | Math.round(0x9A * lightF) << 8 | Math.round(0x30 * lightF);
        glow.renderToBuffer(poseStack, buffer.getBuffer(RenderType.energySwirl(GLOW, (t * 0.003F) % 1.0F, (-t * 0.012F) % 1.0F)),
                FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glowColor);
        PlayerModel<AbstractClientPlayer> body = getParentModel();
        // 2. the fire: sheets of rising flame on every part, always facing the camera, rising up and back along his motion
        Vec3 velocity = player.getDeltaMovement();
        Vector3f flow = new Vector3f((float) -velocity.x * 2.2F, 1.0F + (float) -velocity.y * 1.1F, (float) -velocity.z * 2.2F);
        if (flow.y < 0.3F) {
            flow.y = 0.3F;
        }
        flow.normalize();
        java.util.List<float[]> sheets = new java.util.ArrayList<>();
        part(poseStack, body.head, 0, -4, 0, 0.36F, 0.95F, flow, sheets);
        part(poseStack, body.body, 0, 6, 0, 0.40F, 0.85F, flow, sheets);
        part(poseStack, body.rightArm, -1, 4, 0, 0.22F, 0.7F, flow, sheets);
        part(poseStack, body.leftArm, 1, 4, 0, 0.22F, 0.7F, flow, sheets);
        part(poseStack, body.rightLeg, 0, 6, 0, 0.22F, 0.6F, flow, sheets);
        part(poseStack, body.leftLeg, 0, 6, 0, 0.22F, 0.6F, flow, sheets);
        Matrix4f m = poseStack.last().pose();
        Matrix4f inverse = new Matrix4f(m).invert();
        Vector3f base = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f up = new Vector3f();
        for (int pass = FireSheet.DECAL; pass <= FireSheet.GLOW; pass++) {
            VertexConsumer vc = buffer.getBuffer(FireSheet.type(pass));
            if (pass == FireSheet.DECAL) {
                // a faint wisp of smoke over his head and shoulders, drawn under the flames
                for (int i = 0; i < Math.min(6, sheets.size()); i += 3) {
                    float[] sheet = sheets.get(i);
                    FireSheet.frame(inverse, new Vector3f(sheet[0], sheet[1], sheet[2]), new Vector3f(sheet[3], sheet[4], sheet[5]).mul(1.7F),
                            sheet[6] * 1.3F, base, right, up);
                    FireSheet.draw(FireSheet.SMOKE, m, vc, base, right, up, t, sheet[7] + 2.1F, 0.012F, 0.22F * k);
                }
            }
            for (float[] sheet : sheets) {
                FireSheet.frame(inverse, new Vector3f(sheet[0], sheet[1], sheet[2]), new Vector3f(sheet[3], sheet[4], sheet[5]), sheet[6],
                        base, right, up);
                FireSheet.draw(pass, m, vc, base, right, up, t, sheet[7], sheet[8], sheet[9] * k);
            }
        }
    }

    /**
     * The sheets of one part: its middle ({@code cx, cy, cz} pixels in its own frame) found in camera-relative world space, and from it
     * an inner sheet, a wider and taller outer one that rises slower, and a fainter one in front of the part (so flames lick over the
     * body too). Each is {foot xyz, rise xyz, half-width, seed, speed, strength}.
     */
    private static void part(PoseStack poseStack, ModelPart part, float cx, float cy, float cz, float halfWidth, float height,
                             Vector3f flow, java.util.List<float[]> out) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        Vector3f middle = poseStack.last().pose().transformPosition(new Vector3f(cx / 16.0F, cy / 16.0F, cz / 16.0F));
        poseStack.popPose();
        float seed = out.size() * 0.618F;
        Vector3f foot = new Vector3f(middle).sub(new Vector3f(flow).mul(0.22F));
        Vector3f toCamera = new Vector3f(middle).negate();
        if (toCamera.lengthSquared() > 1.0E-6F) {
            toCamera.normalize();
        }
        Vector3f rise = new Vector3f(flow).mul(height);
        out.add(sheet(foot, rise, halfWidth, seed, 0.020F, 1.0F));
        out.add(sheet(new Vector3f(foot).sub(new Vector3f(flow).mul(0.05F)), new Vector3f(flow).mul(height * 1.35F), halfWidth * 1.4F,
                seed + 0.5F, 0.013F, 0.7F));
        out.add(sheet(new Vector3f(foot).add(new Vector3f(toCamera).mul(0.28F)), new Vector3f(flow).mul(height * 0.8F), halfWidth * 0.85F,
                seed + 0.25F, 0.024F, 0.45F));
    }

    private static float[] sheet(Vector3f foot, Vector3f rise, float halfWidth, float seed, float speed, float strength) {
        return new float[]{foot.x, foot.y, foot.z, rise.x, rise.y, rise.z, halfWidth, seed, speed, strength};
    }

}
